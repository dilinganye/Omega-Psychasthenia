package data.scripts.campaign.invasion;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.PerShipData;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.ShipCondition;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldSource;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.hullmods.shard.PTSD_BaseShard_Util;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static data.scripts.IIRT_Omega_ModPlugin.*;

/**
 * Save-compatible coordinator for the rewritten Psychasthenia crisis.
 * The persistent strategic state is authoritative; physical fleets are nearby projections of it.
 */
public class IIRT_Omega_Invasion implements EveryFrameScript {
    public enum STAGE { START, COLLECT_DATA, INVADE, REPAIR, FULL_ATTACK, END }

    public static final String stage_id = "$IIRT_Omega_Invasion_Stage";
    public static final String baseSystem_id = "$IIRT_Omega_Base_System";
    public static final String baseMarket_id = "$IIRT_Omega_Base_Market";
    public static final String WATCHER_FACTION = "Omega_Watcher";
    public static final String PSYCHASTHENIA_FACTION = "Omega_Psychasthenia";
    public static final String IIRT_Omega_Faction = PSYCHASTHENIA_FACTION;
    public static final String GAMMA_LEGION_FACTION = "Gamma_Legion";

    private static final String SCOUT_TAG = "IIRT_Omega_Scout";
    private static final String CRISIS_FLEET_TAG = "PTSD_crisis_fleet";
    private static final String EVENT_MEMORY = "$PTSD_strategic_event";
    private static final String FORTRESS_MEMORY = "$PTSD_black_hole_fortress";
    private static final Color NOTICE_COLOR = new Color(238, 165, 143, 255);
    private static final Color DANGER_COLOR = new Color(255, 92, 92, 255);

    protected STAGE currStage;
    protected float stageElapsed;
    protected float elapsed;
    protected float temp_time;
    protected StarSystemAPI baseSystem;
    protected MarketAPI baseMarket;
    protected int inv_interval = 14;

    private IntervalUtil heartbeat = new IntervalUtil(0.08f, 0.16f);
    private Random random = new Random();

    public void setStage(STAGE stage) {
        currStage = stage;
        if (Global.getSector() != null) Global.getSector().getMemoryWithoutUpdate().set(stage_id, stage);
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null) transition(state, phaseForStage(stage));
    }

    @Override
    public boolean isDone() {
        PTSDCrisisState state = PTSDCrisisState.get();
        return state != null && state.phase == PTSDCrisisState.Phase.ENDED;
    }

    @Override
    public boolean runWhilePaused() { return false; }

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null || Global.getSector().getClock() == null) return;
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || !isInvasionEnabled()) return;
        ensureRuntimeFields();
        initializeOrMigrate(state);
        normalizeDisabledPhase(state);
        if (state.phase == PTSDCrisisState.Phase.ENDED) return;
        heartbeat.advance(Global.getSector().getClock().convertToDays(amount));
        if (!heartbeat.intervalElapsed()) return;

        float day = PTSDCrisisState.getDay();
        PTSDCrisisProgress.advance(state, day);
        PTSDLocalPanicManager.advance(state, day, random);
        PTSDCrisisIncidentManager.advance(state, day, random);
        enforceCrisisDiplomacy(state);
        if (day >= state.nextIsolationSyncDay) {
            PTSDOccupationManager.syncMapVisibility();
            ensureCrisisFleetListeners();
            state.nextIsolationSyncDay = day + 1f;
        }
        if (PTSDCrisisProgress.getEra(state) != PTSDCrisisProgress.Era.WATCHER_PRE_INVASION) {
            transferWatcherToPsychasthenia(state);
        } else {
            synchronizePreInvasionWatcherFaction(state);
        }
        stageElapsed = Math.max(0f, day - state.phaseStartedDay);
        currStage = stageForPhase(state.phase);
        Global.getSector().getMemoryWithoutUpdate().set(stage_id, currStage);
        revealNearbyCrisisActivity(state);
        processMaterializationAndEncounters(state, day);
        resolveDueEvents(state, day);
        if (day >= state.nextWeightUpdateDay) {
            updateSystemWeights(state, day);
            state.nextWeightUpdateDay = day + Math.max(1f, strategic_update_interval);
        }

        switch (state.phase) {
            case DORMANT:
                if (stageElapsed >= start_stage_time) transitionToEnabled(state, PTSDCrisisState.Phase.RECON);
                break;
            case RECON:
                PTSDReconPlayerEvents.advance(state, day);
                runRecon(state, day);
                if (stageElapsed >= collect_data_time) transitionToEnabled(state, PTSDCrisisState.Phase.EXPANSION);
                break;
            case EXPANSION:
                ensureBase(state);
                runExpansion(state, day);
                if (stageElapsed >= invade_time) transitionToEnabled(state, PTSDCrisisState.Phase.FORTIFICATION);
                break;
            case FORTIFICATION:
                ensureBase(state);
                runExpansion(state, day);
                runFortification(state, day);
                if (stageElapsed >= repair_time && PTSDCrisisProgress.isReadyForInvasion(state)) advancePrewarGate(state, day);
                break;
            case WAR:
                ensureBase(state);
                runWar(state, day);
                break;
            default:
                break;
        }
    }

    private void ensureRuntimeFields() {
        if (heartbeat == null) heartbeat = new IntervalUtil(0.08f, 0.16f);
        if (random == null) random = new Random();
    }

    private void initializeOrMigrate(PTSDCrisisState state) {
        if (Global.getSector().getMemoryWithoutUpdate().contains(baseSystem_id) && state.baseSystemId == null) {
            state.baseSystemId = Global.getSector().getMemoryWithoutUpdate().getString(baseSystem_id);
        }
        if (Global.getSector().getMemoryWithoutUpdate().contains(baseMarket_id) && state.baseMarketId == null) {
            state.baseMarketId = Global.getSector().getMemoryWithoutUpdate().getString(baseMarket_id);
        }
        baseSystem = state.resolveSystem(state.baseSystemId);
        baseMarket = state.resolveMarket(state.baseMarketId);
        if (state.phase != PTSDCrisisState.Phase.DORMANT || state.phaseStartedDay != 0f) {
            currStage = stageForPhase(state.phase);
            return;
        }
        STAGE saved = readSavedStage();
        if (saved == null) saved = stageFromSetting();
        state.phase = phaseForStage(saved);
        state.phaseStartedDay = PTSDCrisisState.getDay() - Math.max(0f, stageElapsed);
        currStage = saved;
        Global.getSector().getMemoryWithoutUpdate().set(stage_id, saved);
    }

    private STAGE readSavedStage() {
        if (!Global.getSector().getMemoryWithoutUpdate().contains(stage_id)) return null;
        Object value = Global.getSector().getMemoryWithoutUpdate().get(stage_id);
        if (value instanceof STAGE) return (STAGE) value;
        if (value instanceof String) {
            try { return STAGE.valueOf((String) value); } catch (Throwable ignored) { }
        }
        return null;
    }

    private STAGE stageFromSetting() {
        if ("Cod".equals(PTSD_DefStat_onNewGame)) return STAGE.COLLECT_DATA;
        if ("Inv".equals(PTSD_DefStat_onNewGame)) return STAGE.INVADE;
        if ("Rep".equals(PTSD_DefStat_onNewGame)) return STAGE.REPAIR;
        if ("FuA".equals(PTSD_DefStat_onNewGame)) return STAGE.FULL_ATTACK;
        if ("End".equals(PTSD_DefStat_onNewGame)) return STAGE.END;
        return STAGE.START;
    }

    private static PTSDCrisisState.Phase phaseForStage(STAGE stage) {
        if (stage == STAGE.COLLECT_DATA) return PTSDCrisisState.Phase.RECON;
        if (stage == STAGE.INVADE) return PTSDCrisisState.Phase.EXPANSION;
        if (stage == STAGE.REPAIR) return PTSDCrisisState.Phase.FORTIFICATION;
        if (stage == STAGE.FULL_ATTACK) return PTSDCrisisState.Phase.WAR;
        if (stage == STAGE.END) return PTSDCrisisState.Phase.ENDED;
        return PTSDCrisisState.Phase.DORMANT;
    }

    private static STAGE stageForPhase(PTSDCrisisState.Phase phase) {
        if (phase == PTSDCrisisState.Phase.RECON) return STAGE.COLLECT_DATA;
        if (phase == PTSDCrisisState.Phase.EXPANSION) return STAGE.INVADE;
        if (phase == PTSDCrisisState.Phase.FORTIFICATION) return STAGE.REPAIR;
        if (phase == PTSDCrisisState.Phase.WAR) return STAGE.FULL_ATTACK;
        if (phase == PTSDCrisisState.Phase.ENDED) return STAGE.END;
        return STAGE.START;
    }

    private static void transition(PTSDCrisisState state, PTSDCrisisState.Phase next) {
        if (state.phase == next) return;
        PTSDCrisisState.Phase previous = state.phase;
        state.phase = next;
        state.phaseStartedDay = PTSDCrisisState.getDay();
        PTSDCrisisProgress.onPhaseChanged(state, previous, next);
        Global.getSector().getMemoryWithoutUpdate().set(stage_id, stageForPhase(next));
        float day = state.phaseStartedDay;
        if (next == PTSDCrisisState.Phase.RECON) state.nextScoutDay = day + 1f;
        if (next == PTSDCrisisState.Phase.EXPANSION) state.nextExpansionDay = day + 1f;
        if (next == PTSDCrisisState.Phase.FORTIFICATION) state.nextFortressDay = day + 1f;
        if (next == PTSDCrisisState.Phase.WAR) {
            state.nextOmegaTurnDay = day + 1f;
            state.nextHumanTurnDay = day + 3f;
        }
        PTSDCrisisDevIntel.report("阶段切换", previous.name() + " → " + next.name(),
                state.baseSystemId, null);
    }

    /** Dev console entry point; preserves all normal phase-transition side effects. */
    public static void devTransition(PTSDCrisisState.Phase next) {
        if (Global.getSector() == null || !Global.getSettings().isDevMode()) return;
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null && next != null) transition(state, next);
    }

    public static void devTriggerSoftWarning() {
        if (Global.getSettings().isDevMode()) showSoftWarning(PTSDCrisisState.get());
    }

    public static void devTriggerPrewarRedAlert() {
        if (Global.getSettings().isDevMode()) showPrewarRedAlert(PTSDCrisisState.get());
    }

    public static void devTriggerHardWarning() {
        if (Global.getSettings().isDevMode()) showHardWarning(PTSDCrisisState.get());
    }
    private static void transitionToEnabled(PTSDCrisisState state, PTSDCrisisState.Phase requested) {
        PTSDCrisisState.Phase target = requested;
        while (target != PTSDCrisisState.Phase.ENDED && !isPhaseEnabled(target)) {
            target = nextPhase(target);
        }
        transition(state, target);
    }

    private static void normalizeDisabledPhase(PTSDCrisisState state) {
        if (state == null || state.phase == PTSDCrisisState.Phase.ENDED || isPhaseEnabled(state.phase)) return;
        PTSDCrisisState.Phase disabled = state.phase;
        PTSDCrisisState.Phase target = nextPhase(disabled);
        while (target != PTSDCrisisState.Phase.ENDED && !isPhaseEnabled(target)) target = nextPhase(target);
        transition(state, target);
        PTSDCrisisDevIntel.report("阶段已由Dev开关跳过", disabled.name() + " → " + target.name(),
                state.baseSystemId, null);
    }

    private static PTSDCrisisState.Phase nextPhase(PTSDCrisisState.Phase phase) {
        if (phase == PTSDCrisisState.Phase.DORMANT) return PTSDCrisisState.Phase.RECON;
        if (phase == PTSDCrisisState.Phase.RECON) return PTSDCrisisState.Phase.EXPANSION;
        if (phase == PTSDCrisisState.Phase.EXPANSION) return PTSDCrisisState.Phase.FORTIFICATION;
        if (phase == PTSDCrisisState.Phase.FORTIFICATION) return PTSDCrisisState.Phase.WAR;
        return PTSDCrisisState.Phase.ENDED;
    }
    private void runRecon(PTSDCrisisState state, float day) {
        if (state.totalScoutSightings >= warning_encounter_threshold) showSoftWarning(state);
        if (day < state.nextScoutDay) return;
        state.nextScoutDay = day + frequencyAdjusted(randomBetween(scout_min_interval, scout_max_interval));
        if (countTaggedFleets(SCOUT_TAG) >= Math.max(1, Math.round(scout_max_active))) return;
        spawnScout(state);
    }

    private static final class ScoutMissionPlan {
        final IIRT_Omega_ScoutAI.MissionType type;
        final SectorEntityToken target;
        final String systemId;
        final float stationDays;

        ScoutMissionPlan(IIRT_Omega_ScoutAI.MissionType type, SectorEntityToken target,
                         String systemId, float stationDays) {
            this.type = type;
            this.target = target;
            this.systemId = systemId;
            this.stationDays = stationDays;
        }
    }

    /** Invalid mod-added targets are retried instead of aborting the campaign heartbeat. */
    private void spawnScout(PTSDCrisisState state) {
        Throwable lastFailure = null;
        for (int attempt = 1; attempt <= 8; attempt++) {
            try {
                ScoutMissionPlan plan = pickScoutMission(state);
                if (plan != null && createScout(plan)) return;
            } catch (Throwable ex) {
                lastFailure = ex;
                Global.getLogger(IIRT_Omega_Invasion.class).warn(
                        "Failed to prepare PTSD scout mission; retrying candidate " + attempt, ex);
            }
        }
        state.nextScoutDay = Math.min(state.nextScoutDay, PTSDCrisisState.getDay() + 2f);
        PTSDCrisisDevIntel.report("侦察任务生成失败",
                lastFailure == null ? "未找到有效目标；两日后重试" :
                        lastFailure.getClass().getSimpleName() + "；两日后重试", null, null);
    }

    private ScoutMissionPlan pickScoutMission(PTSDCrisisState state) {
        WeightedRandomPicker<IIRT_Omega_ScoutAI.MissionType> types =
                new WeightedRandomPicker<IIRT_Omega_ScoutAI.MissionType>(random);
        types.add(IIRT_Omega_ScoutAI.MissionType.RELAY, 8f);
        types.add(IIRT_Omega_ScoutAI.MissionType.HYPERSPACE_WATCH, 4f);
        types.add(IIRT_Omega_ScoutAI.MissionType.WILDERNESS_ROAM, 3f);
        if (state.totalScoutSightings >= Math.max(2, warning_encounter_threshold / 2)) {
            types.add(IIRT_Omega_ScoutAI.MissionType.COLONY_INFILTRATION,
                    2f + Math.min(6f, state.totalScoutSightings));
        }
        IIRT_Omega_ScoutAI.MissionType type = types.pick();
        if (type == null) return null;
        WeightedRandomPicker<SectorEntityToken> targets = new WeightedRandomPicker<SectorEntityToken>(random);
        String selectedSystemId = null;

        if (type == IIRT_Omega_ScoutAI.MissionType.COLONY_INFILTRATION) {
            for (MarketAPI market : PTSDOccupationManager.getAllMarkets()) {
                if (market == null || market.isPlanetConditionMarketOnly() || market.getPrimaryEntity() == null ||
                        market.getStarSystem() == null || market.getFactionId() == null ||
                        WATCHER_FACTION.equals(market.getFactionId()) ||
                        PSYCHASTHENIA_FACTION.equals(market.getFactionId())) continue;
                PTSDCrisisState.SystemData data = state.getSystemData(market.getStarSystem().getId());
                targets.add(market.getPrimaryEntity(),
                        (2f + market.getSize()) / (1f + data.scoutVisits * 0.3f));
            }
        } else {
            WeightedRandomPicker<StarSystemAPI> systems = new WeightedRandomPicker<StarSystemAPI>(random);
            for (StarSystemAPI system : Global.getSector().getStarSystems()) {
                if (system == null || system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)) continue;
                PTSDCrisisState.SystemData data = state.getSystemData(system.getId());
                float weight = (system.isEnteredByPlayer() ? 1.2f : 1f) /
                        (1f + data.scoutVisits * 0.28f);
                if (type == IIRT_Omega_ScoutAI.MissionType.RELAY) {
                    for (SectorEntityToken relay : system.getEntitiesWithTag(Tags.COMM_RELAY)) {
                        targets.add(relay, 8f * weight);
                    }
                } else if (type == IIRT_Omega_ScoutAI.MissionType.WILDERNESS_ROAM) {
                    boolean populated = false;
                    for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system)) {
                        if (market != null && !market.isPlanetConditionMarketOnly()) { populated = true; break; }
                    }
                    if (!populated) systems.add(system, weight);
                } else {
                    systems.add(system, weight);
                }
            }
            if (type != IIRT_Omega_ScoutAI.MissionType.RELAY) {
                StarSystemAPI system = systems.pick();
                if (system != null) {
                    selectedSystemId = system.getId();
                    if (type == IIRT_Omega_ScoutAI.MissionType.HYPERSPACE_WATCH) {
                        Vector2f point = new Vector2f(system.getLocation());
                        Vector2f offset = Misc.getUnitVectorAtDegreeAngle(random.nextFloat() * 360f);
                        offset.scale(1800f + random.nextFloat() * 4200f);
                        Vector2f.add(point, offset, point);
                        targets.add(Global.getSector().getHyperspace().createToken(point), 1f);
                    } else {
                        for (PlanetAPI planet : system.getPlanets()) {
                            if (planet != null && !planet.isStar()) targets.add(planet, 2f);
                        }
                        for (SectorEntityToken jump : system.getJumpPoints()) targets.add(jump, 1f);
                    }
                }
            }
        }
        SectorEntityToken target = targets.pick();
        if (target == null) return null;
        if (selectedSystemId == null && target.getStarSystem() != null) selectedSystemId = target.getStarSystem().getId();
        float days = type == IIRT_Omega_ScoutAI.MissionType.HYPERSPACE_WATCH ?
                16f + random.nextFloat() * 28f :
                (type == IIRT_Omega_ScoutAI.MissionType.WILDERNESS_ROAM ?
                        24f + random.nextFloat() * 36f : 4f + random.nextFloat() * 8f);
        return new ScoutMissionPlan(type, target, selectedSystemId, days);
    }

    private boolean createScout(ScoutMissionPlan plan) {
        Vector2f destination = plan.target.getLocationInHyperspace();
        if (destination == null) return false;
        Vector2f source = Misc.getUnitVectorAtDegreeAngle(random.nextFloat() * 360f);
        source.scale(18000f + random.nextFloat() * 26000f);
        Vector2f.add(destination, source, source);
        FleetParamsV3 params = new FleetParamsV3(source, WATCHER_FACTION, 0.4f,
                FleetTypes.MERC_SCOUT, 12f + random.nextFloat() * 14f, 0f, 0f, 0f, 0f, 0f, 0f);
        params.maxNumShips = Math.max(3, Global.getSettings().getMaxShipsInFleet() / 4);
        CampaignFleetAPI scout = PTSD_BaseShard_Util.createFleet(params, params.combatPts,
                PTSD_BaseShard_Util.FleetRole.RECON, params.random);
        if (scout == null) return false;
        Global.getSector().getHyperspace().addEntity(scout);
        scout.setLocation(source.x, source.y);
        scout.setName("无法识别的微弱信号");
        scout.setNoFactionInName(true);
        scout.setSensorProfile(120f);
        scout.setTransponderOn(false);
        scout.addTag(SCOUT_TAG);
        scout.addTag(CRISIS_FLEET_TAG);
        scout.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF, true);
        scout.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
        scout.getMemoryWithoutUpdate().set(MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);
        scout.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        scout.addScript(new IIRT_Omega_ScoutAI(scout, plan.target, plan.type, plan.systemId, plan.stationDays));
        PTSDCrisisDevIntel.report("侦察舰队启航",
                "任务 " + plan.type.name() + "；目标 " + plan.target.getFullName() +
                        "；驻留 " + Math.round(plan.stationDays) + " 日；航行时间不计入驻留；分支 " +
                        PTSD_BaseShard_Util.getFleetBranchName(scout),
                plan.systemId, scout.getId());
        return true;
    }

    /** Materializes the rare 5% news-investigation observer: one very fast ship that shadows then flees. */
    public static CampaignFleetAPI spawnNewsTracker(String systemId, SectorEntityToken target) {
        if (Global.getSector() == null || target == null || target.getContainingLocation() == null) return null;
        try {
            Random seeded = new Random(Misc.genUID().hashCode());
            Vector2f hyper = target.getLocationInHyperspace();
            FleetParamsV3 params = new FleetParamsV3(hyper, WATCHER_FACTION, 0.15f,
                    FleetTypes.MERC_SCOUT, 7f, 0f, 0f, 0f, 0f, 0f, 0f);
            params.maxNumShips = 1;
            CampaignFleetAPI scout = PTSD_BaseShard_Util.createFleet(params, params.combatPts,
                PTSD_BaseShard_Util.FleetRole.RECON, params.random);
            if (scout == null) return null;
            target.getContainingLocation().addEntity(scout);
            Vector2f offset = Misc.getUnitVectorAtDegreeAngle(seeded.nextFloat() * 360f);
            offset.scale(4200f + seeded.nextFloat() * 2600f);
            Vector2f point = Vector2f.add(target.getLocation(), offset, null);
            scout.setLocation(point.x, point.y);
            scout.setName("无法识别的单舰信号");
            scout.setNoFactionInName(true); scout.setSensorProfile(70f); scout.setTransponderOn(false);
            scout.addTag(SCOUT_TAG); scout.addTag(CRISIS_FLEET_TAG); scout.addTag(Tags.SALVAGE_ENTITY_NO_DEBRIS);
            scout.getStats().getFleetwideMaxBurnMod().modifyFlat("PTSD_news_tracker", 6f, "异常推进特征");
            scout.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF, true);
            scout.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
            scout.getMemoryWithoutUpdate().set(MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);
            scout.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
            CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            SectorEntityToken follow = player == null ? target : player;
            scout.addScript(new IIRT_Omega_ScoutAI(scout, follow,
                    IIRT_Omega_ScoutAI.MissionType.COLONY_INFILTRATION, systemId, 10f));
            PTSDCrisisDevIntel.report("新闻跟踪舰生成", "单舰高速观察单元；分支 " +
                    PTSD_BaseShard_Util.getFleetBranchName(scout), systemId, scout.getId());
            return scout;
        } catch (Throwable ex) {
            Global.getLogger(IIRT_Omega_Invasion.class).warn("Failed to spawn news tracker", ex);
            return null;
        }
    }
    public static void reportScoutMissionStage(String systemId, String fleetId, String missionType, String stage) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null && systemId != null && "抵达任务区".equals(stage)) {
            PTSDCrisisState.SystemData data = state.getSystemData(systemId);
            data.scoutVisits++;
            data.lastObservedDay = PTSDCrisisState.getDay();
            PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.RECON_CONFIDENCE,
                    0.6f, "SCOUT_ARRIVAL", systemId);
        }
        PTSDCrisisDevIntel.report("侦察任务阶段", missionType + "：" + stage, systemId, fleetId);
    }

    public static void reportScoutSighting(String systemId) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return;
        state.totalScoutSightings++;
        state.totalOmegaEncounters++;
        PTSDCrisisState.SystemData data = state.getSystemData(systemId);
        if (data != null) {
            data.playerSightings++;
            data.knownToPlayer = true;
            data.lastObservedDay = PTSDCrisisState.getDay();
        }
        state.visibleStage = Math.max(state.visibleStage, visibleStageForPhase(state.phase));
        PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.RECON_CONFIDENCE, 1.5f, "SCOUT_SIGHTING", systemId);
        PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.HUMAN_AWARENESS, 4f, "SCOUT_SIGHTING", systemId);
        PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.WATCHER_AGGRESSION, 1f, "SCOUT_SIGHTING", systemId);
        PTSDLocalPanicAPI.spreadFromSystem(systemId, 1.2f, 12000f, "SCOUT_SIGHTING");
        if (state.totalScoutSightings >= warning_encounter_threshold) showSoftWarning(state);
        else if (state.softWarningShown) PTSDCrisisIntel.ensureIntel();
        PTSDCrisisAPI.reportFleetSighting(systemId, null, "第四窥视舰队目击");
        PTSDCrisisDevIntel.report("侦察单位被目击",
                "累计目击 " + state.totalScoutSightings + "；累计异常接触 " + state.totalOmegaEncounters,
                systemId, null);
    }

    public static void reportScoutEscape(String systemId, boolean escapedPlayer) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return;
        state.totalScoutEscapes++;
        PTSDCrisisState.SystemData data = state.getSystemData(systemId);
        if (data != null) data.hostileContacts++;
        PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.RECON_CONFIDENCE, 0.8f, "SCOUT_ESCAPE", systemId);
        if (escapedPlayer) {
            state.totalOmegaEncounters++;
            state.visibleStage = Math.max(state.visibleStage, visibleStageForPhase(state.phase));
            PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.WATCHER_AGGRESSION, 1.5f, "PLAYER_PURSUIT", systemId);
        }
        PTSDCrisisDevIntel.report("侦察单位脱离",
                escapedPlayer ? "成功摆脱玩家追逐" : "完成常规撤离", systemId, null);
    }

    /** Legacy entry point retained for external callers; current scouts submit explicit daily maxima. */
    public static void reportReconSample(String systemId, float fleetStrength) {
        reportReconDailyMaximum(systemId, fleetStrength, null, (int) Math.floor(PTSDCrisisState.getDay()));
    }

    public static void reportReconDailyMaximum(String systemId, float fleetStrength,
                                               String fleetId, int dayBucket) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || systemId == null) return;
        PTSDCrisisState.SystemData data = state.getSystemData(systemId);
        float strength = Math.max(0f, fleetStrength);
        boolean newDay = data.reconDailyBucket != dayBucket;
        if (newDay) {
            data.reconDailyBucket = dayBucket;
            data.reconDailyMax = strength;
            data.reconDailyReports = 1;
            data.reconStrengthHistory.add(strength);
            while (data.reconStrengthHistory.size() > 30) data.reconStrengthHistory.remove(0);
        } else {
            data.reconDailyReports++;
            if (strength <= data.reconDailyMax) return;
            data.reconDailyMax = strength;
            if (!data.reconStrengthHistory.isEmpty()) {
                data.reconStrengthHistory.set(data.reconStrengthHistory.size() - 1, strength);
            }
        }
        data.lastReconSampleDay = PTSDCrisisState.getDay();
        data.observedFleetStrength = Math.max(data.observedFleetStrength * 0.92f, data.reconDailyMax);
        data.lastObservedDay = data.lastReconSampleDay;
        if (newDay) {
            PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.RECON_CONFIDENCE,
                    Math.min(0.5f, 0.05f + strength / 1000f), "RECON_DAILY_MAX", systemId);
            PTSDCrisisDevIntel.report("侦察日结",
                    "当日最高舰队强度 " + Math.round(strength) +
                            (fleetId == null ? "" : " / 提交舰队 " + fleetId), systemId, fleetId);
        }
    }
    private static int visibleStageForPhase(PTSDCrisisState.Phase phase) {
        if (phase == PTSDCrisisState.Phase.RECON || phase == PTSDCrisisState.Phase.DORMANT) return 1;
        if (phase == PTSDCrisisState.Phase.EXPANSION) return 2;
        return 3;
    }

    private static void showSoftWarning(PTSDCrisisState state) {
        if (!state.softWarningShown) {
            state.softWarningShown = true;
            Global.getSector().getCampaignUI().addMessage("一场不为人知的动荡似乎正要发生", NOTICE_COLOR);
            PTSDCrisisDevIntel.report("软警告触发", "一场不为人知的动荡似乎正要发生",
                    state.baseSystemId, null);
        }
        PTSDCrisisIntel.ensureIntel();
    }

    private void advancePrewarGate(PTSDCrisisState state, float day) {
        if (!state.prewarHunterSpawned) {
            StarSystemAPI targetSystem = findPlayerTargetSystem();
            CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            if (targetSystem == null || player == null) return;
            PTSDCrisisState.StrategicEvent hunter = state.addEvent(
                    PTSDCrisisState.EventType.PREWAR_HUNTER, PTSDCrisisAPI.SIDE_OMEGA,
                    WATCHER_FACTION, state.baseSystemId, targetSystem.getId(), null,
                    32f + Math.min(28f, state.playerGrudge * .35f), 10f + random.nextFloat() * 4f);
            hunter.targetEntityId = player.getId();
            hunter.opponentFactionId = Global.getSector().getPlayerFaction().getId();
            hunter.playerRelevant = true;
            hunter.description = "第四窥视对高价值目标的最终截获测试；该行动结束前，全面入侵进度被锁定。";
            state.prewarHunterSpawned = true;
            state.prewarHunterEventId = hunter.id;
            PTSDCrisisDevIntel.report("战前截获行动", "正在截获高价值目标；全面入侵暂时锁定",
                    targetSystem.getId(), null);
            return;
        }
        PTSDCrisisState.StrategicEvent hunter = state.getEvent(state.prewarHunterEventId);
        if (!state.prewarHunterResolved && hunter != null && hunter.status == PTSDCrisisState.EventStatus.RESOLVED) {
            state.prewarHunterResolved = true;
            state.prewarHunterResolvedDay = day;
            if (state.warCommitDay <= day) state.warCommitDay = day + 1f + random.nextFloat() * 7f;
        }
        if (!state.prewarHunterResolved) return;
        showPrewarRedAlert(state);
        if (day >= state.warCommitDay) beginWar(state);
    }

    private static void showPrewarRedAlert(PTSDCrisisState state) {
        if (state.prewarRedAlertShown) return;
        state.prewarRedAlertShown = true;
        Global.getSector().getCampaignUI().addMessage(
                "多当局联合发布红色警报：未知舰体相关报道已确认属实。边缘航路进入最高戒备。", DANGER_COLOR);
        PTSDCrisisDevIntel.report("战前红色警报", "未知舰体新闻已由多当局联合确认为真实；八日内进入全面战争",
                state.baseSystemId, null);
        PTSDCrisisIntel.ensureIntel();
    }

    private StarSystemAPI findPlayerTargetSystem() {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null) return null;
        if (player.getStarSystem() != null) return player.getStarSystem();
        StarSystemAPI best = null;
        float distance = Float.MAX_VALUE;
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            float curr = Misc.getDistance(player.getLocationInHyperspace(), system.getLocation());
            if (curr < distance) { distance = curr; best = system; }
        }
        return best;
    }
    private void beginWar(PTSDCrisisState state) {
        if (!isPhaseEnabled(PTSDCrisisState.Phase.WAR)) {
            transition(state, PTSDCrisisState.Phase.ENDED);
            PTSDCrisisDevIntel.report("全面战争阶段已禁用", "危机由要塞阶段直接结束", state.baseSystemId, null);
            return;
        }
        transition(state, PTSDCrisisState.Phase.WAR);
        showHardWarning(state);
        configureWarFaction();
    }

    private static void showHardWarning(PTSDCrisisState state) {
        if (!state.hardWarningShown) {
            state.hardWarningShown = true;
            Global.getSector().getCampaignUI().addMessage("边缘星系侦测到大量失联信号，危险警告！", DANGER_COLOR);
            PTSDCrisisDevIntel.report("全面入侵警告触发",
                    "边缘星系侦测到大量失联信号，第四窥视资产即将移交", state.baseSystemId, null);
        }
        transferWatcherToPsychasthenia(state);
        PTSDCrisisIntel pre = (PTSDCrisisIntel) Global.getSector().getIntelManager().getFirstIntel(PTSDCrisisIntel.class);
        if (pre != null) pre.replaceWithWarIntel();
        PTSDWarIntel.ensureIntel();
    }

    private static void synchronizePreInvasionWatcherFaction(PTSDCrisisState state) {
        boolean firstSync = !state.preInvasionFactionSynchronized;
        for (LocationAPI location : Global.getSector().getAllLocations()) {
            for (CampaignFleetAPI fleet : new ArrayList<CampaignFleetAPI>(location.getFleets())) {
                boolean crisisAsset = fleet.hasTag(CRISIS_FLEET_TAG) || fleet.hasTag(SCOUT_TAG) ||
                        fleet.getMemoryWithoutUpdate().getBoolean(FORTRESS_MEMORY);
                if (crisisAsset && fleet.getFaction() != null &&
                        PSYCHASTHENIA_FACTION.equals(fleet.getFaction().getId())) {
                    fleet.setFaction(WATCHER_FACTION, true);
                }
            }
        }
        for (MarketAPI market : PTSDOccupationManager.getAllMarkets()) {
            boolean crisisMarket = (state.baseMarketId != null && state.baseMarketId.equals(market.getId())) ||
                    market.getMemoryWithoutUpdate().getBoolean("$PTSD_controlled_territory");
            if (crisisMarket && PSYCHASTHENIA_FACTION.equals(market.getFactionId())) {
                market.setFactionId(WATCHER_FACTION);
                if (market.getPrimaryEntity() != null) market.getPrimaryEntity().setFaction(WATCHER_FACTION);
                for (PersonAPI person : market.getPeopleCopy()) person.setFaction(WATCHER_FACTION);
            }
        }
        for (PTSDCrisisState.StrategicEvent event : state.events) {
            if (PTSDCrisisAPI.SIDE_OMEGA.equals(event.side) && PSYCHASTHENIA_FACTION.equals(event.factionId)) {
                event.factionId = WATCHER_FACTION;
            }
        }
        state.preInvasionFactionSynchronized = true;
        if (firstSync) {
            PTSDCrisisDevIntel.report("前入侵势力同步",
                    "侦察、沉寂建设与封锁阶段的危机资产统一归属第四窥视", state.baseSystemId, null);
        }
    }
    private static void transferWatcherToPsychasthenia(PTSDCrisisState state) {
        boolean firstTransfer = !state.watcherTransferred;
        for (LocationAPI location : Global.getSector().getAllLocations()) {
            for (CampaignFleetAPI fleet : new ArrayList<CampaignFleetAPI>(location.getFleets())) {
                if (fleet.getFaction() != null && WATCHER_FACTION.equals(fleet.getFaction().getId())) {
                    fleet.setFaction(PSYCHASTHENIA_FACTION, true);
                }
            }
            for (SectorEntityToken entity : new ArrayList<SectorEntityToken>(location.getAllEntities())) {
                if (!(entity instanceof CampaignFleetAPI) && entity.getFaction() != null &&
                        WATCHER_FACTION.equals(entity.getFaction().getId())) entity.setFaction(PSYCHASTHENIA_FACTION);
            }
        }
        for (MarketAPI market : PTSDOccupationManager.getAllMarkets()) {
            if (WATCHER_FACTION.equals(market.getFactionId())) {
                market.setFactionId(PSYCHASTHENIA_FACTION);
                if (market.getPrimaryEntity() != null) market.getPrimaryEntity().setFaction(PSYCHASTHENIA_FACTION);
                for (PersonAPI person : market.getPeopleCopy()) person.setFaction(PSYCHASTHENIA_FACTION);
            }
        }
        for (PTSDCrisisState.StrategicEvent event : state.events) {
            if (WATCHER_FACTION.equals(event.factionId)) event.factionId = PSYCHASTHENIA_FACTION;
        }
        state.watcherTransferred = true;
        if (firstTransfer) {
            PTSDCrisisDevIntel.report("势力时代切换",
                    "第四窥视全部资产已转为精神创伤；后续载入资产将持续校正", state.baseSystemId, null);
        }
    }
    /** Reasserted every heartbeat so diplomacy events and other mods cannot unlock these relations. */
    private static void enforceCrisisDiplomacy(PTSDCrisisState state) {
        if (Global.getSector() == null) return;
        FactionAPI watcher = Global.getSector().getFaction(WATCHER_FACTION);
        FactionAPI psych = Global.getSector().getFaction(PSYCHASTHENIA_FACTION);
        if (state != null && !state.diplomacyLockedReported) {
            state.diplomacyLockedReported = true;
            PTSDCrisisDevIntel.report("危机外交锁定", "除Gamma_Legion及危机内部别名外全部锁定敌对", null, null);
        }
        for (FactionAPI crisis : new FactionAPI[] { watcher, psych }) {
            if (crisis == null) continue;
            for (FactionAPI other : Global.getSector().getAllFactions()) {
                if (other == null) continue;
                String id = other.getId();
                if (id.equals(crisis.getId())) continue;
                if (WATCHER_FACTION.equals(id) || PSYCHASTHENIA_FACTION.equals(id)) {
                    crisis.setRelationship(id, 1f);
                    other.setRelationship(crisis.getId(), 1f);
                } else if (GAMMA_LEGION_FACTION.equals(id)) {
                    crisis.setRelationship(id, 0f);
                    other.setRelationship(crisis.getId(), 0f);
                } else {
                    crisis.setRelationship(id, -1f);
                    other.setRelationship(crisis.getId(), -1f);
                }
            }
        }
    }
    private void configureWarFaction() {
        FactionAPI omega = Global.getSector().getFaction(PSYCHASTHENIA_FACTION);
        if (omega == null) return;
        omega.getDoctrine().setNumShips(5);
        omega.getDoctrine().setOfficerQuality(5);
        omega.getDoctrine().setShipQuality(5);
        omega.getDoctrine().setAggression(5);
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            String id = faction.getId();
            if (!PSYCHASTHENIA_FACTION.equals(id) && !WATCHER_FACTION.equals(id) &&
                    !GAMMA_LEGION_FACTION.equals(id)) omega.setRelationship(id, -1f);
        }
    }

    private void ensureBase(PTSDCrisisState state) {
        baseSystem = state.resolveSystem(state.baseSystemId);
        baseMarket = state.resolveMarket(state.baseMarketId);
        if (baseSystem != null && baseMarket != null) return;
        WeightedRandomPicker<StarSystemAPI> systems = new WeightedRandomPicker<StarSystemAPI>(random);
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (!system.isProcgen() || system.getPlanets().size() <= 1 || system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)) continue;
            int populated = 0;
            int usable = 0;
            for (PlanetAPI planet : system.getPlanets()) {
                if (planet.isStar()) continue;
                usable++;
                MarketAPI market = planet.getMarket();
                if (market != null && !market.isPlanetConditionMarketOnly() && market.getSize() > 1) populated++;
            }
            if (usable == 0 || populated > 0) continue;
            float weight = 1f + Math.min(4f, system.getLocation().length() / 12000f);
            if (system.isEnteredByPlayer()) weight *= 0.25f;
            if (system.getStar() != null && system.getStar().getSpec().isBlackHole()) weight *= 1.5f;
            systems.add(system, weight);
        }
        if (systems.isEmpty()) {
            for (StarSystemAPI system : Global.getSector().getStarSystems()) {
                if (system.isProcgen() && system.getPlanets().size() > 1) systems.add(system);
            }
        }
        baseSystem = systems.pick();
        if (baseSystem == null) return;
        WeightedRandomPicker<PlanetAPI> planets = new WeightedRandomPicker<PlanetAPI>(random);
        for (PlanetAPI planet : baseSystem.getPlanets()) {
            if (planet.isStar()) continue;
            MarketAPI market = planet.getMarket();
            if (market != null && !market.isPlanetConditionMarketOnly() && market.getSize() > 1) continue;
            planets.add(planet, planet.isGasGiant() ? 0.7f : 1.4f);
        }
        PlanetAPI planet = planets.pick();
        if (planet == null) return;
        baseMarket = planet.getMarket();
        if (baseMarket == null) {
            baseMarket = Global.getFactory().createMarket("PTSD_core_" + planet.getId(), planet.getName(), 7);
            baseMarket.setPrimaryEntity(planet);
            planet.setMarket(baseMarket);
        }
        String activeFactionId = PTSDCrisisProgress.getActiveFactionId(state);
        baseMarket.setFactionId(activeFactionId);
        baseMarket.setPlayerOwned(false);
        baseMarket.setPrimaryEntity(planet);
        planet.setFaction(activeFactionId);
        PTSDOccupationManager.prepareStrategicShell(baseMarket);
        state.baseSystemId = baseSystem.getId();
        state.baseMarketId = baseMarket.getId();
        Global.getSector().getMemoryWithoutUpdate().set(baseSystem_id, state.baseSystemId);
        Global.getSector().getMemoryWithoutUpdate().set(baseMarket_id, state.baseMarketId);
        PTSDCrisisState.SystemData data = state.getSystemData(baseSystem.getId());
        data.omegaControl = 1f;
        data.humanControl = 0f;
        data.conversionLevel = Math.max(1, data.conversionLevel);
        applyPlanetMutation(planet, 4);
        PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.NEST_DEVELOPMENT,
                8f, "CORE_OUTPOST_ESTABLISHED", state.baseSystemId);
        PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.REALITY_DISTORTION,
                4f, "CORE_OUTPOST_ESTABLISHED", state.baseSystemId);
        PTSDCrisisDevIntel.report("核心据点建立",
                baseMarket.getName() + " 已完成初始大幅改造", state.baseSystemId, planet.getId());
    }

    private static void ensureIndustry(MarketAPI market, String industryId) {
        if (!market.hasIndustry(industryId)) market.addIndustry(industryId);
    }

    private void runExpansion(PTSDCrisisState state, float day) {
        if (day < state.nextExpansionDay) return;
        state.nextExpansionDay = day + Math.max(2f, frequencyAdjusted(expansion_interval));
        expandOnePlanet(state);
    }

    private void expandOnePlanet(PTSDCrisisState state) {
        WeightedRandomPicker<PlanetAPI> picker = new WeightedRandomPicker<PlanetAPI>(random);
        for (PTSDCrisisState.SystemData data : state.systems.values()) {
            if (data.omegaControl < 0.5f) continue;
            StarSystemAPI system = state.resolveSystem(data.systemId);
            if (system == null) continue;
            for (PlanetAPI planet : system.getPlanets()) {
                if (planet.isStar() || planet.hasTag("PTSD_mutated_planet")) continue;
                MarketAPI market = planet.getMarket();
                if (market != null && !market.isPlanetConditionMarketOnly() && !PSYCHASTHENIA_FACTION.equals(market.getFactionId()) && !WATCHER_FACTION.equals(market.getFactionId())) continue;
                picker.add(planet, planet.isGasGiant() ? 0.65f : 1.2f);
            }
        }
        PlanetAPI planet = picker.pick();
        if (planet == null) return;
        String activeFactionId = PTSDCrisisProgress.getActiveFactionId(state);
        PTSDCrisisState.SystemData data = state.getSystemData(planet.getStarSystem().getId());
        data.conversionLevel = Math.min(5, data.conversionLevel + 1);
        applyPlanetMutation(planet, Math.max(1, data.conversionLevel));
        PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.NEST_DEVELOPMENT,
                3f + data.conversionLevel, "PLANET_EXPANSION", planet.getStarSystem().getId());
        PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.REALITY_DISTORTION,
                1.5f + data.conversionLevel * 0.5f, "PLANET_EXPANSION", planet.getStarSystem().getId());
        PTSDCrisisDevIntel.report("行星扩张改造",
                planet.getName() + " 改造等级 " + data.conversionLevel,
                planet.getStarSystem().getId(), planet.getId());
        planet.setFaction(activeFactionId);
        if (planet.getMarket() != null && planet.getMarket().isPlanetConditionMarketOnly()) {
            planet.getMarket().setFactionId(activeFactionId);
            planet.getMarket().getMemoryWithoutUpdate().set("$PTSD_mutation_level", data.conversionLevel);
        }
        state.addEvent(PTSDCrisisState.EventType.CONSTRUCTION, PTSDCrisisAPI.SIDE_OMEGA,
                activeFactionId, state.baseSystemId, planet.getStarSystem().getId(),
                planet.getMarket() == null ? null : planet.getMarket().getId(), 35f + data.conversionLevel * 12f, 7f);
    }

    private static void applyPlanetMutation(PlanetAPI planet, int level) {
        if (planet == null || planet.getSpec() == null) return;
        int clamped = Math.max(1, Math.min(5, level));
        int pulse = 24 * clamped;
        planet.addTag("PTSD_mutated_planet");
        planet.getSpec().addTag("PTSD_mutated_planet");
        planet.getSpec().setPlanetColor(new Color(Math.min(255, 88 + pulse), Math.max(12, 104 - clamped * 13), Math.min(255, 120 + pulse / 2)));
        planet.getSpec().setAtmosphereColor(new Color(Math.min(255, 130 + pulse), 22 + clamped * 9, Math.min(255, 170 + pulse / 2), 150 + clamped * 18));
        planet.getSpec().setCloudColor(new Color(74 + clamped * 20, Math.max(12, 90 - clamped * 12), 130 + clamped * 18, 180));
        planet.getSpec().setGlowColor(new Color(190 + clamped * 10, 34 + clamped * 8, 225, 210));
        planet.getSpec().setIconColor(new Color(210, 55 + clamped * 12, 235));
        planet.getSpec().setUseReverseLightForGlow(true);
        planet.getSpec().setRotation(planet.getSpec().getRotation() + 0.35f * clamped);
        planet.applySpecChanges();
    }

    private void runFortification(PTSDCrisisState state, float day) {
        if (day < state.nextFortressDay) return;
        String activeFactionId = PTSDCrisisProgress.getActiveFactionId(state);
        state.nextFortressDay = day + Math.max(4f, frequencyAdjusted(expansion_interval * 1.5f));
        if (countFortresses(state) < Math.max(0, Math.round(max_black_hole_fortresses))) createBlackHoleFortress(state);
        if (state.baseSystemId != null && state.countActiveEvents(PTSDCrisisState.EventType.GARRISON) < max_guard_fleets) {
            state.addEvent(PTSDCrisisState.EventType.GARRISON, PTSDCrisisAPI.SIDE_OMEGA,
                    activeFactionId, state.baseSystemId, state.baseSystemId, state.baseMarketId, 85f, 12f);
        }
        for (PTSDCrisisState.SystemData data : state.systems.values()) {
            if (!data.blackHoleFortress || hasActiveEventFor(state, PTSDCrisisState.EventType.FORTRESS_PATROL, data.systemId)) continue;
            state.addEvent(PTSDCrisisState.EventType.FORTRESS_PATROL, PTSDCrisisAPI.SIDE_OMEGA,
                    activeFactionId, data.systemId, data.systemId, null, 135f, 18f);
        }
    }

    private boolean hasActiveEventFor(PTSDCrisisState state, PTSDCrisisState.EventType type, String systemId) {
        for (PTSDCrisisState.StrategicEvent event : state.getActiveEvents()) {
            if (event.type == type && systemId != null && systemId.equals(event.targetSystemId)) return true;
        }
        return false;
    }

    private int countFortresses(PTSDCrisisState state) {
        int result = 0;
        for (PTSDCrisisState.SystemData data : state.systems.values()) if (data.blackHoleFortress) result++;
        return result;
    }

    private void createBlackHoleFortress(PTSDCrisisState state) {
        WeightedRandomPicker<StarSystemAPI> picker = new WeightedRandomPicker<StarSystemAPI>(random);
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system.getStar() == null || !system.getStar().getSpec().isBlackHole()) continue;
            PTSDCrisisState.SystemData data = state.getSystemData(system.getId());
            if (data.blackHoleFortress) continue;
            float weight = (system.isEnteredByPlayer() ? 0.35f : 1.2f) * (1f + system.getLocation().length() / 18000f);
            picker.add(system, weight);
        }
        StarSystemAPI system = picker.pick();
        if (system == null) return;
        String activeFactionId = PTSDCrisisProgress.getActiveFactionId(state);
        CampaignFleetAPI fortress = Global.getFactory().createEmptyFleet(activeFactionId, "癫狂视界要塞", true);
        FleetMemberAPI member;
        try { member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "IIRT_Omega_Station_Stable"); }
        catch (Throwable ex) { Global.getLogger(getClass()).warn("Unable to create Omega black-hole station variant", ex); return; }
        fortress.getFleetData().addFleetMember(member);
        fortress.setStationMode(true);
        fortress.setNoFactionInName(true);
        fortress.addTag(CRISIS_FLEET_TAG);
        fortress.getMemoryWithoutUpdate().set(FORTRESS_MEMORY, true);
        fortress.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        fortress.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        fortress.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        // createEmptyFleet() may not have initialized its ability map yet; stations need no campaign abilities.
        system.addEntity(fortress);

        fortress.setCircularOrbitWithSpin(system.getStar(), random.nextFloat() * 360f,
                Math.max(850f, system.getStar().getRadius() + 550f), 18f, 1f, 2f);
        system.getStar().getSpec().setGlowColor(new Color(225, 35, 255));
        system.getStar().getSpec().setCoronaColor(new Color(175, 20, 230, 210));
        system.getStar().applySpecChanges();
        PTSDCrisisState.SystemData data = state.getSystemData(system.getId());
        data.blackHoleFortress = true;
        data.omegaControl = 1f;
        data.humanControl = 0f;
        data.conversionLevel = Math.max(data.conversionLevel, 4);
        state.addEvent(PTSDCrisisState.EventType.FORTRESS_PATROL, PTSDCrisisAPI.SIDE_OMEGA,
                activeFactionId, system.getId(), system.getId(), null, 150f, 18f);
        PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.BLOCKADE_DENSITY,
                10f, "BLACK_HOLE_FORTRESS", system.getId());
        PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.REALITY_DISTORTION,
                7f, "BLACK_HOLE_FORTRESS", system.getId());
        PTSDCrisisDevIntel.report("黑洞要塞建立",
                system.getName() + " 的黑洞已转化为欧米伽要塞", system.getId(), fortress.getId());
    }
    private void runWar(PTSDCrisisState state, float day) {
        showHardWarning(state);
        configureWarFaction();
        runExpansion(state, day);
        runFortification(state, day);
        scheduleGrudgeRaid(state, day);
        if (day >= state.nextOmegaTurnDay) {
            scheduleOmegaTurn(state);
            state.nextOmegaTurnDay = day + Math.max(2f, frequencyAdjusted(randomBetween(front_turn_min_interval, front_turn_max_interval)));
        }
        if (day >= state.nextHumanTurnDay) {
            scheduleHumanTurn(state);
            state.nextHumanTurnDay = day + Math.max(2f, frequencyAdjustedHuman(randomBetween(front_turn_min_interval, front_turn_max_interval))) + 1.5f;
        }
        resolveDueEvents(state, day);
        if (Global.getSector().getMemoryWithoutUpdate().getBoolean("$IIRT_omega_Invasion_End")) {
            transition(state, PTSDCrisisState.Phase.ENDED);
            if (baseMarket != null && baseMarket.hasCondition("IIRT_Omega_Repair_Facility")) baseMarket.removeCondition("IIRT_Omega_Repair_Facility");
        }
    }

    private void scheduleGrudgeRaid(PTSDCrisisState state, float day) {
        if (day < state.nextGrudgeRaidDay || state.playerGrudge <= 0f) return;
        float grudge = Math.min(100f, state.playerGrudge);
        state.nextGrudgeRaidDay = day + Math.max(1.5f, 8f - grudge * .06f) + random.nextFloat() * 3f;
        if (random.nextFloat() > .20f + grudge * .0075f) return;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        StarSystemAPI targetSystem = findPlayerTargetSystem();
        if (player == null || targetSystem == null) return;
        float supplies = player.getCargo() == null ? 99999f : player.getCargo().getSupplies();
        float lowThreshold = Math.max(45f, player.getFleetPoints() * .8f);
        boolean execution = supplies < lowThreshold;
        float strength = execution ? 160f + grudge * 2.2f : 24f + grudge * .5f;
        PTSDCrisisState.StrategicEvent raid = state.addEvent(PTSDCrisisState.EventType.GRUDGE_RAID,
                PTSDCrisisAPI.SIDE_OMEGA, PSYCHASTHENIA_FACTION, state.baseSystemId,
                targetSystem.getId(), null, Math.min(final_invasion_max_strength, strength),
                execution ? 14f : 8f);
        raid.targetEntityId = player.getId();
        raid.opponentFactionId = Global.getSector().getPlayerFaction().getId();
        raid.playerRelevant = true;
        raid.description = execution ? "供应崩溃已被确认；执行高强度追杀。" : "以低成本袭扰持续消耗目标补给。";
        PTSDCrisisDevIntel.report(execution ? "记恨追杀舰队" : "记恨消耗舰队",
                "记恨值 " + Math.round(grudge) + " / 目标补给 " + Math.round(supplies), targetSystem.getId(), null);
    }
    private void scheduleOmegaTurn(PTSDCrisisState state) {
        MarketAPI target = pickAttackTarget(state);
        if (target == null || target.getStarSystem() == null) return;
        StarSystemAPI source = pickOmegaSource(state, target.getStarSystem());
        PTSDCrisisState.SystemData targetData = state.getSystemData(target.getStarSystem().getId());
        float confidence = Math.min(1f, 0.15f + targetData.scoutVisits * 0.16f + targetData.playerSightings * 0.08f);
        float resistance = PTSDCrisisAPI.getFactionResistance(target.getFactionId());
        float strength = Math.min(final_invasion_max_strength,
                Math.max(55f, 75f + targetData.attackWeight * 1.8f + confidence * 65f + resistance * 2.5f));
        PTSDCrisisState.StrategicEvent event = state.addEvent(PTSDCrisisState.EventType.ATTACK,
                PTSDCrisisAPI.SIDE_OMEGA, PSYCHASTHENIA_FACTION,
                source == null ? state.baseSystemId : source.getId(), target.getStarSystem().getId(),
                target.getId(), strength, 9f + random.nextFloat() * 8f);
        event.description = "根据侦察权重选择的突破行动；目标防御越薄弱，部署优先级越高。";
        event.playerRelevant = target.isPlayerOwned();
        event.opponentFactionId = target.isPlayerOwned() ? Global.getSector().getPlayerFaction().getId() : target.getFactionId();
    }

    private MarketAPI pickAttackTarget(PTSDCrisisState state) {
        WeightedRandomPicker<MarketAPI> picker = new WeightedRandomPicker<MarketAPI>(random);
        for (MarketAPI market : PTSDOccupationManager.getAllMarkets()) {
            if (market.isPlanetConditionMarketOnly() || market.getStarSystem() == null || market.getPrimaryEntity() == null) continue;
            String factionId = market.getFactionId();
            if (PSYCHASTHENIA_FACTION.equals(factionId) || WATCHER_FACTION.equals(factionId) || Factions.OMEGA.equals(factionId)) continue;
            PTSDCrisisState.SystemData data = state.getSystemData(market.getStarSystem().getId());
            float weight = Math.max(0.05f, data.attackWeight);
            if (market.isPlayerOwned()) weight *= 1.25f;
            PTSDCrisisState.PlayerMarker marker = state.playerMarkers.get(data.systemId);
            if (marker != null && "OMEGA_BASE".equals(marker.type)) weight *= Math.min(1.25f, marker.weight);
            picker.add(market, PTSDCrisisAPI.modifyWeight(data.systemId, PTSDCrisisAPI.SIDE_OMEGA, weight));
        }
        return picker.pick();
    }

    private StarSystemAPI pickOmegaSource(PTSDCrisisState state, StarSystemAPI target) {
        StarSystemAPI best = state.resolveSystem(state.baseSystemId);
        float bestScore = -1f;
        for (PTSDCrisisState.SystemData data : state.systems.values()) {
            if (data.omegaControl < 0.5f) continue;
            StarSystemAPI system = state.resolveSystem(data.systemId);
            if (system == null) continue;
            float score = data.omegaControl * 100000f / (5000f + Misc.getDistance(system.getLocation(), target.getLocation()));
            if (score > bestScore) { bestScore = score; best = system; }
        }
        return best;
    }

    private void scheduleHumanTurn(PTSDCrisisState state) {
        rebalancePlayerTaskForces(state);
        PTSDCrisisState.StrategicEvent attack = null;
        for (PTSDCrisisState.StrategicEvent event : state.getActiveEvents()) {
            if (event.type == PTSDCrisisState.EventType.ATTACK && PTSDCrisisAPI.SIDE_OMEGA.equals(event.side) && !hasDefenseFor(state, event.targetSystemId)) {
                attack = event;
                break;
            }
        }
        if (attack == null) return;
        MarketAPI market = state.resolveMarket(attack.targetMarketId);
        if (market == null || market.getStarSystem() == null) return;
        PTSDCrisisState.SystemData data = state.getSystemData(attack.targetSystemId);
        float baseDefense = Math.max(35f, data.observedMarketDefense * 0.38f + market.getSize() * 9f);
        PTSDCrisisState.PlayerMarker marker = state.playerMarkers.get(attack.targetSystemId);
        if (marker != null && "DEFEND".equals(marker.type)) baseDefense *= Math.min(1.35f, marker.weight);
        baseDefense = PTSDCrisisAPI.modifyWeight(attack.targetSystemId, PTSDCrisisAPI.SIDE_HUMAN, baseDefense);
        float delay = Math.max(2f, attack.resolveDay - PTSDCrisisState.getDay());
        StarSystemAPI humanSource = pickHumanSource(market.getStarSystem(), market.getFactionId());
        String humanSourceId = humanSource == null ? market.getStarSystem().getId() : humanSource.getId();
        PTSDCrisisState.StrategicEvent defense = state.addEvent(PTSDCrisisState.EventType.DEFENSE,
                PTSDCrisisAPI.SIDE_HUMAN, market.getFactionId(), humanSourceId,
                attack.targetSystemId, market.getId(), baseDefense, delay);
        defense.description = market.getName() + " 自发组织的殖民地防卫舰队。";
        PTSDCrisisState.StrategicEvent mercenary = state.addEvent(PTSDCrisisState.EventType.MERCENARY_DEFENSE,
                PTSDCrisisAPI.SIDE_HUMAN, market.getFactionId(), humanSourceId, attack.targetSystemId, market.getId(),
                Math.max(20f, baseDefense * 0.35f), delay);
        mercenary.description = "自由联盟与地方雇佣兵组成的临时增援。";
    }

    private void rebalancePlayerTaskForces(PTSDCrisisState state) {
        float day = PTSDCrisisState.getDay();
        for (PTSDCrisisState.PlayerTaskForce force : state.playerTaskForces) {
            if (force.destroyed || day < force.nextRedeployDay) continue;
            MarketAPI source = state.resolveMarket(force.sourceMarketId);
            if (source == null || !source.isPlayerOwned() || source.getStarSystem() == null) {
                force.destroyed = true;
                state.releaseCommittedProduction(force.sourceMarketId, force.productionCost);
                continue;
            }
            WeightedRandomPicker<PTSDCrisisState.SystemData> picker = new WeightedRandomPicker<PTSDCrisisState.SystemData>(random);
            for (PTSDCrisisState.SystemData data : state.systems.values()) {
                StarSystemAPI system = state.resolveSystem(data.systemId);
                if (system == null) continue;
                float weight = 0.1f + data.attackWeight;
                boolean underAttack = false;
                for (PTSDCrisisState.StrategicEvent event : state.getActiveEvents()) {
                    if (event.type == PTSDCrisisState.EventType.ATTACK && data.systemId.equals(event.targetSystemId)) {
                        underAttack = true;
                        weight += event.strength / 20f;
                    }
                }
                PTSDCrisisState.PlayerMarker marker = state.playerMarkers.get(data.systemId);
                if (marker != null && "DEFEND".equals(marker.type)) weight *= Math.min(1.35f, marker.weight);
                if ("INTERCEPTOR".equals(force.specialization) && underAttack) weight *= 1.45f;
                if ("LINE".equals(force.specialization)) weight *= 0.8f + Math.min(1.5f, data.humanDefenseWeight / 80f);
                if ("STRIKE".equals(force.specialization) && data.omegaControl >= 0.5f) weight *= 1.8f;
                int alreadyAssigned = 0;
                for (PTSDCrisisState.PlayerTaskForce other : state.playerTaskForces) {
                    if (!other.destroyed && data.systemId.equals(other.assignedSystemId)) alreadyAssigned++;
                }
                weight /= 1f + alreadyAssigned * 0.75f;
                picker.add(data, Math.max(0.02f, weight));
            }
            PTSDCrisisState.SystemData target = picker.pick();
            if (target == null) continue;
            boolean changed = !target.systemId.equals(force.assignedSystemId);
            force.assignedSystemId = target.systemId;
            force.nextRedeployDay = day + 10f + random.nextFloat() * 8f;
            if (changed) {
                PTSDCrisisState.StrategicEvent move = state.addEvent(PTSDCrisisState.EventType.PLAYER_TASK_FORCE,
                        PTSDCrisisAPI.SIDE_HUMAN, Global.getSector().getPlayerFaction().getId(),
                        source.getStarSystem().getId(), target.systemId, null,
                        force.strength * force.deploymentWeight, 5f);
                move.referenceId = force.id;
                move.description = force.name + " 正在重新部署以填补战线。";
            }
        }
    }

    private StarSystemAPI pickHumanSource(StarSystemAPI target, String factionId) {
        StarSystemAPI best = null;
        float bestScore = -1f;
        for (MarketAPI candidate : Global.getSector().getEconomy().getMarketsCopy()) {
            if (candidate.isPlanetConditionMarketOnly() || candidate.getStarSystem() == null || candidate.getStarSystem() == target) continue;
            if (!candidate.getFactionId().equals(factionId) && !candidate.isPlayerOwned()) continue;
            float distance = Misc.getDistance(candidate.getStarSystem().getLocation(), target.getLocation());
            float score = candidate.getSize() * 10000f / (3000f + distance);
            if (score > bestScore) { bestScore = score; best = candidate.getStarSystem(); }
        }
        return best;
    }

    private boolean hasDefenseFor(PTSDCrisisState state, String targetSystemId) {
        for (PTSDCrisisState.StrategicEvent event : state.getActiveEvents()) {
            if (targetSystemId.equals(event.targetSystemId) && (event.type == PTSDCrisisState.EventType.DEFENSE ||
                    event.type == PTSDCrisisState.EventType.MERCENARY_DEFENSE)) return true;
        }
        return false;
    }

    private void resolveSpecialPursuits(PTSDCrisisState state, float day) {
        for (PTSDCrisisState.StrategicEvent event : state.events) {
            if (event == null || (event.type != PTSDCrisisState.EventType.PREWAR_HUNTER &&
                    event.type != PTSDCrisisState.EventType.GRUDGE_RAID) ||
                    (event.status != PTSDCrisisState.EventStatus.PLANNED && event.status != PTSDCrisisState.EventStatus.MATERIALIZED)) continue;
            List<CampaignFleetAPI> fleets = findEventFleets(event);
            CampaignFleetAPI fleet = fleets.isEmpty() ? null : fleets.get(0);
            boolean vanished = event.status == PTSDCrisisState.EventStatus.MATERIALIZED && !hasEventFleet(event);
            if (event.type == PTSDCrisisState.EventType.PREWAR_HUNTER && vanished && day < event.resolveDay) {
                event.status = PTSDCrisisState.EventStatus.PLANNED;
                event.materializedFleetId = null;
                if (event.materializedFleetIds != null) event.materializedFleetIds.clear();
                event.defeatLearningRecorded = false;
                event.nextProjectionDay = day + .5f;
                PTSDCrisisDevIntel.report("战前截获单元补位", "非玩家因素使截获单元失效；门槛保持锁定并重新投放",
                        event.targetSystemId, null);
                continue;
            }
            if (!vanished && day < event.resolveDay) continue;
            if (fleet != null && fleet.getBattle() == null) despawnEventFleets(event);
            event.successful = false;
            event.status = PTSDCrisisState.EventStatus.RESOLVED;
            if (event.type == PTSDCrisisState.EventType.PREWAR_HUNTER) {
                state.prewarHunterResolved = true;
                state.prewarHunterResolvedDay = day;
                state.warCommitDay = day + 1f + random.nextFloat() * 7f;
                event.aftermathKind = null;
                PTSDCrisisDevIntel.report("战前截获行动结束", "长时间未能接触目标，截获舰队脱离",
                        event.targetSystemId, null);
            }
            PTSDCrisisAPI.notifyResolved(event);
        }
    }
    private void resolveDueEvents(PTSDCrisisState state, float day) {
        resolveSpecialPursuits(state, day);
        for (PTSDCrisisState.StrategicEvent event : state.events) {
            if (event.status != PTSDCrisisState.EventStatus.MATERIALIZED ||
                    day >= event.resolveDay || PTSDCrisisAPI.SIDE_OMEGA.equals(event.side)) continue;
            if (!hasEventFleet(event)) {
                event.successful = false;
                event.status = PTSDCrisisState.EventStatus.RESOLVED;
                if (event.type == PTSDCrisisState.EventType.PLAYER_TASK_FORCE) {
                    PTSDCrisisState.PlayerTaskForce force = state.getTaskForce(event.referenceId);
                    if (force != null && !force.destroyed) {
                        force.destroyed = true;
                        state.releaseCommittedProduction(force.sourceMarketId, force.productionCost);
                    }
                }
                PTSDCrisisAPI.notifyResolved(event);
            }
        }
        for (PTSDCrisisState.StrategicEvent event : new ArrayList<PTSDCrisisState.StrategicEvent>(state.events)) {
            if (event.status != PTSDCrisisState.EventStatus.PLANNED && event.status != PTSDCrisisState.EventStatus.MATERIALIZED) continue;
            if (event.type != PTSDCrisisState.EventType.ATTACK || !PTSDCrisisAPI.SIDE_OMEGA.equals(event.side)) continue;
            if (event.status == PTSDCrisisState.EventStatus.MATERIALIZED &&
                    !hasEventFleet(event) && day < event.resolveDay) {
                finishAttack(state, event, false);
            } else if (day >= event.resolveDay) {
                resolveAttack(state, event);
            }
        }
        for (PTSDCrisisState.StrategicEvent event : state.events) {
            if ((event.status != PTSDCrisisState.EventStatus.PLANNED && event.status != PTSDCrisisState.EventStatus.MATERIALIZED) ||
                    day < event.resolveDay || event.type == PTSDCrisisState.EventType.ATTACK) continue;
            if (event.type == PTSDCrisisState.EventType.CONSTRUCTION || event.type == PTSDCrisisState.EventType.GARRISON ||
                    event.type == PTSDCrisisState.EventType.FORTRESS_PATROL || event.type == PTSDCrisisState.EventType.PLAYER_TASK_FORCE ||
                    event.type == PTSDCrisisState.EventType.EXTERNAL || event.type == PTSDCrisisState.EventType.DEFENSE ||
                    event.type == PTSDCrisisState.EventType.MERCENARY_DEFENSE ||
                    event.type == PTSDCrisisState.EventType.FIRE_PROBE || event.type == PTSDCrisisState.EventType.GRUDGE_RAID) {
                event.successful = true;
                event.status = PTSDCrisisState.EventStatus.RESOLVED;
                despawnEventFleets(event);
                PTSDCrisisAPI.notifyResolved(event);
            }
        }
    }

    private void resolveAttack(PTSDCrisisState state, PTSDCrisisState.StrategicEvent attack) {
        PTSDCrisisState.SystemData data = state.getSystemData(attack.targetSystemId);
        float attackStrength = attack.strength;
        float defenseStrength = Math.max(20f, data.observedMarketDefense * 0.55f);
        for (PTSDCrisisState.StrategicEvent event : state.getActiveEvents()) {
            if (!attack.targetSystemId.equals(event.targetSystemId) || event == attack) continue;
            if (PTSDCrisisAPI.SIDE_HUMAN.equals(event.side)) defenseStrength += event.strength;
            else if (PTSDCrisisAPI.SIDE_OMEGA.equals(event.side)) attackStrength += event.strength * 0.45f;
        }
        for (PTSDCrisisState.PlayerTaskForce force : state.playerTaskForces) {
            if (!force.destroyed && attack.targetSystemId.equals(force.assignedSystemId)) {
                defenseStrength += force.strength * force.deploymentWeight * ("LINE".equals(force.specialization) ? 1.2f : 1f);
            }
        }
        for (PTSDCrisisAPI.ForceContribution force : PTSDCrisisAPI.getForceContributions()) {
            if (!attack.targetSystemId.equals(force.systemId)) continue;
            if (PTSDCrisisAPI.SIDE_OMEGA.equals(force.side)) attackStrength += force.strength;
            else defenseStrength += force.strength;
        }
        attackStrength *= 0.86f + random.nextFloat() * 0.3f;
        defenseStrength *= 0.86f + random.nextFloat() * 0.3f;
        finishAttack(state, attack, attackStrength > defenseStrength);
    }

    private void finishAttack(PTSDCrisisState state, PTSDCrisisState.StrategicEvent attack, boolean success) {
        attack.successful = success;
        attack.status = PTSDCrisisState.EventStatus.RESOLVED;
        despawnEventFleets(attack);
        PTSDCrisisState.SystemData data = state.getSystemData(attack.targetSystemId);
        if (success) {
            data.successfulOmegaAttacks++;
            attack.aftermathKind = "HUMAN_DEFEAT";
            data.learningMultiplier = Math.min(2.5f, data.learningMultiplier * 1.08f + 0.03f);
            data.omegaControl = 1f;
            data.humanControl = 0f;
            MarketAPI market = state.resolveMarket(attack.targetMarketId);
            if (market != null) destroyAndClaimColony(state, market);
            PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.OMEGA_ESCALATION,
                    2.5f, "OMEGA_ATTACK_SUCCESS", attack.targetSystemId);
            PTSDLocalPanicAPI.spreadFromSystem(attack.targetSystemId, 3f, 24000f,
                    "OMEGA_ATTACK_SUCCESS");
        } else {
            data.failedOmegaAttacks++;
            attack.aftermathKind = "OMEGA_DEFEAT";
            if (!attack.defeatLearningRecorded) {
                attack.defeatLearningRecorded = true;
                PTSDCrisisAPI.recordOmegaDefeat(attack.opponentFactionId, attack.playerRelevant,
                        attack.targetSystemId, attack.strength);
            }
            data.learningMultiplier = Math.max(0.28f, data.learningMultiplier * 0.78f);
            data.attackWeight *= 0.72f;
            PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.OMEGA_ESCALATION,
                    1.2f, "OMEGA_ATTACK_FAILURE_LEARNING", attack.targetSystemId);
            PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.HUMAN_COHESION,
                    2f, "HUMAN_DEFENSE_SUCCESS", attack.targetSystemId);
        }
        for (PTSDCrisisState.StrategicEvent event : state.events) {
            if (event == attack || event.status == PTSDCrisisState.EventStatus.RESOLVED || !attack.targetSystemId.equals(event.targetSystemId)) continue;
            if (event.type == PTSDCrisisState.EventType.DEFENSE || event.type == PTSDCrisisState.EventType.MERCENARY_DEFENSE) {
                event.successful = !success;
                event.status = PTSDCrisisState.EventStatus.RESOLVED;
                despawnEventFleets(event);
                PTSDCrisisAPI.notifyResolved(event);
            }
        }
        PTSDCrisisAPI.notifyResolved(attack);
    }

    private void destroyAndClaimColony(PTSDCrisisState state, MarketAPI market) {
        int oldSize = market.getSize();
        market.setAdmin(null);
        market.setPlayerOwned(false);
        for (SectorEntityToken entity : new ArrayList<SectorEntityToken>(market.getConnectedEntities())) entity.setFaction(PSYCHASTHENIA_FACTION);
        if (market.getPrimaryEntity() != null) market.getPrimaryEntity().setFaction(PSYCHASTHENIA_FACTION);
        market.setFactionId(PSYCHASTHENIA_FACTION);
        market.getCommDirectory().clear();
        for (PersonAPI person : new ArrayList<PersonAPI>(market.getPeopleCopy())) market.removePerson(person);
        market.clearCommodities();
        for (Industry industry : new ArrayList<Industry>(market.getIndustries())) market.removeIndustry(industry.getId(), null, false);
        for (SubmarketAPI submarket : new ArrayList<SubmarketAPI>(market.getSubmarketsCopy())) market.removeSubmarket(submarket.getSpecId());
        for (MarketConditionAPI condition : new ArrayList<MarketConditionAPI>(market.getConditions())) {
            if (condition.getSpec().isDecivRemove()) market.removeSpecificCondition(condition.getIdForPluginModifications());
        }
        market.setPlanetConditionMarketOnly(true);
        market.setSize(1);
        if (!market.hasCondition(Conditions.DECIVILIZED)) market.addCondition(Conditions.DECIVILIZED);
        market.removeCondition(Conditions.RUINS_SCATTERED);
        market.removeCondition(Conditions.RUINS_WIDESPREAD);
        market.removeCondition(Conditions.RUINS_EXTENSIVE);
        market.removeCondition(Conditions.RUINS_VAST);
        if (oldSize <= 3) market.addCondition(Conditions.RUINS_SCATTERED);
        else if (oldSize <= 4) market.addCondition(Conditions.RUINS_WIDESPREAD);
        else if (oldSize <= 6) market.addCondition(Conditions.RUINS_EXTENSIVE);
        else market.addCondition(Conditions.RUINS_VAST);
        market.getMemoryWithoutUpdate().set("$PTSD_destroyed_colony", true);
        market.getMemoryWithoutUpdate().set("$PTSD_controlled_territory", true);
        PTSDOccupationManager.prepareStrategicShell(market);
        if (market.getPlanetEntity() != null) applyPlanetMutation(market.getPlanetEntity(), 5);
        if (market.getStarSystem() != null) {
            String systemId = market.getStarSystem().getId();
            state.addEvent(PTSDCrisisState.EventType.CONSTRUCTION, PTSDCrisisAPI.SIDE_OMEGA,
                    PSYCHASTHENIA_FACTION, state.baseSystemId, systemId, market.getId(), 70f, 8f);
            state.addEvent(PTSDCrisisState.EventType.GARRISON, PTSDCrisisAPI.SIDE_OMEGA,
                    PSYCHASTHENIA_FACTION, state.baseSystemId, systemId, market.getId(), 95f, 10f);
        }
    }
    private void updateSystemWeights(PTSDCrisisState state, float day) {
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            PTSDCrisisState.SystemData data = state.getSystemData(system.getId());
            float fleetDefense = 0f;
            float marketDefense = 0f;
            float value = 1f;
            boolean hasNonCrisisFleet = false;
            boolean hasNonCrisisColony = false;
            for (CampaignFleetAPI fleet : system.getFleets()) {
                if (fleet.getFaction() == null) continue;
                String factionId = fleet.getFaction().getId();
                if (!PSYCHASTHENIA_FACTION.equals(factionId) && !WATCHER_FACTION.equals(factionId)) {
                    hasNonCrisisFleet = true;
                    fleetDefense += Math.max(0f, fleet.getFleetPoints());
                }
            }
            for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system)) {
                if (market.isPlanetConditionMarketOnly() || PSYCHASTHENIA_FACTION.equals(market.getFactionId()) ||
                        WATCHER_FACTION.equals(market.getFactionId())) continue;
                hasNonCrisisColony = true;
                float size = market.getSize();
                value += size * size * 12f;
                if (market.isPlayerOwned()) value += 55f;
                marketDefense += size * size * 9f + market.getStabilityValue() * 3f;
                if (market.hasIndustry(Industries.PATROLHQ)) marketDefense += 18f;
                if (market.hasIndustry(Industries.MILITARYBASE)) marketDefense += 45f;
                if (market.hasIndustry(Industries.HIGHCOMMAND)) marketDefense += 80f;
                if (market.hasIndustry(Industries.ORBITALSTATION)) marketDefense += 28f;
                if (market.hasIndustry(Industries.BATTLESTATION)) marketDefense += 60f;
                if (market.hasIndustry(Industries.STARFORTRESS)) marketDefense += 105f;
                if (market.hasIndustry(Industries.HEAVYINDUSTRY) || market.hasIndustry(Industries.ORBITALWORKS)) value += 45f;
            }
            float trueDefense = fleetDefense + marketDefense;
            float confidence = Math.min(1f, 0.12f + data.scoutVisits * 0.16f + data.playerSightings * 0.05f + state.reconConfidence / 250f);
            float uncertainty = 0.72f + seededNoise(system.getId(), day) * 0.56f;
            // Fleet strength now comes primarily from scout daily maxima instead of omniscient live totals.
            data.observedFleetStrength *= 0.96f;
            if (data.reconStrengthHistory.isEmpty()) {
                data.observedFleetStrength = Math.max(data.observedFleetStrength,
                        fleetDefense * confidence * uncertainty * 0.18f);
            }
            data.observedMarketDefense = Math.max(3f,
                    marketDefense * (0.28f + confidence * 0.72f) * uncertainty);
            data.strategicValue = value;
            data.hasNonCrisisColony = hasNonCrisisColony;
            data.hasNonCrisisFleet = hasNonCrisisFleet;
            boolean omegaOccupied = data.omegaControl >= 0.5f || data.conversionLevel > 0;
            data.occupationSuggested = !omegaOccupied && !hasNonCrisisColony && !hasNonCrisisFleet;
            int nonStarPlanets = 0;
            for (PlanetAPI planet : system.getPlanets()) if (planet != null && !planet.isStar()) nonStarPlanets++;
            data.occupationWeight = data.occupationSuggested ?
                    Math.max(1f, 3f + nonStarPlanets * 1.4f + confidence * 4f) : 0f;

            float scoutedDefense = data.observedFleetStrength + data.observedMarketDefense;
            float weakness = Math.max(0.12f, Math.min(3.5f, 90f / (25f + scoutedDefense)));
            if (omegaOccupied) {
                data.attackWeight = 0.05f;
            } else if (hasNonCrisisColony) {
                float strategicPull = 1f + (float) Math.sqrt(Math.max(1f, value)) / 8f;
                data.attackWeight = Math.max(0.05f, strategicPull * weakness *
                        (0.25f + confidence * 0.75f) * Math.max(0.25f, data.learningMultiplier) * 22f);
            } else if (data.occupationSuggested) {
                // This weight is an occupation recommendation, not a colony assault target.
                data.attackWeight = data.occupationWeight;
            } else {
                // Uncolonized systems containing remnant/pirate/wild fleets retain a cleanup weight.
                data.attackWeight = Math.max(0.1f, weakness * (0.2f + confidence * 0.8f) * 6f);
            }
            data.humanDefenseWeight = Math.max(1f, value / 18f + trueDefense / 12f);
            float occupationAttention = 0f;
            float humanOccupationAttention = 0f;
            for (PTSDCrisisState.OccupationData occupation : state.occupations.values()) {
                MarketAPI occupied = state.resolveMarket(occupation.marketId);
                if (occupied == null || occupied.getStarSystem() != system) continue;
                occupationAttention += occupation.omegaAttention;
                humanOccupationAttention += occupation.humanAttention;
            }
            data.attackWeight *= 1f + Math.min(0.8f, occupationAttention * 0.08f);
            data.attackWeight *= 1f + state.omegaEscalation / 250f;
            data.humanDefenseWeight *= 1f + Math.min(0.4f, humanOccupationAttention * 0.05f);
            data.humanDefenseWeight *= 1f + state.humanCohesion / 300f;
            PTSDCrisisState.PlayerMarker marker = state.playerMarkers.get(system.getId());
            if (marker != null && "DEFEND".equals(marker.type)) data.humanDefenseWeight *= Math.min(1.35f, marker.weight);
            data.attackWeight = PTSDCrisisAPI.modifyWeight(system.getId(), PTSDCrisisAPI.SIDE_OMEGA, data.attackWeight);
            data.humanDefenseWeight = PTSDCrisisAPI.modifyWeight(system.getId(), PTSDCrisisAPI.SIDE_HUMAN, data.humanDefenseWeight);
            data.lastWeightUpdateDay = day;
        }
    }

    private float seededNoise(String id, float day) {
        long bucket = (long) Math.floor(day / Math.max(1f, strategic_update_interval));
        return new Random(31L * id.hashCode() + bucket * 7919L).nextFloat();
    }

    private void processMaterializationAndEncounters(PTSDCrisisState state, float day) {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null) return;
        for (PTSDCrisisState.StrategicEvent event : state.getActiveEvents()) {
            boolean near = shouldMaterialize(state, event, player);
            if (near) event.lastPlayerNearDay = day;
            if (event.status == PTSDCrisisState.EventStatus.PLANNED && near) {
                materializeEvent(state, event);
            }
            if (event.status != PTSDCrisisState.EventStatus.MATERIALIZED) continue;

            List<CampaignFleetAPI> fleets = findEventFleets(event);
            boolean persistentPursuit = event.type == PTSDCrisisState.EventType.PREWAR_HUNTER ||
                    event.type == PTSDCrisisState.EventType.GRUDGE_RAID;
            boolean awayForTenDays = !near && event.lastPlayerNearDay > 0f &&
                    day >= event.lastPlayerNearDay + 10f;
            if (!persistentPursuit && awayForTenDays && day + 1f < event.resolveDay) {
                float remaining = 0f;
                for (CampaignFleetAPI fleet : fleets) {
                    if (fleet != null) remaining += Math.max(0f, fleet.getFleetPoints());
                }
                if (remaining > 0f) event.strength = Math.max(1f, Math.min(event.strength, remaining));
                despawnEventFleets(event);
                event.status = PTSDCrisisState.EventStatus.PLANNED;
                event.nextProjectionDay = Math.max(event.nextProjectionDay, day + 5f);
                PTSDCrisisDevIntel.report("战略事件卸载",
                        event.type + "：玩家离开渲染范围已满10日，回归隐藏推演",
                        event.targetSystemId, null);
                continue;
            }

            if (PTSDCrisisAPI.SIDE_OMEGA.equals(event.side)) {
                for (CampaignFleetAPI fleet : fleets) {
                    if (fleet == null || !fleet.isVisibleToPlayerFleet() ||
                            fleet.getMemoryWithoutUpdate().getBoolean("$PTSD_player_reported_contact")) continue;
                    fleet.getMemoryWithoutUpdate().set("$PTSD_player_reported_contact", true);
                    state.totalOmegaEncounters++;
                    state.visibleStage = Math.max(state.visibleStage, visibleStageForPhase(state.phase));
                    PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.HUMAN_AWARENESS,
                            2f, "MATERIALIZED_CONTACT", event.targetSystemId);
                    PTSDLocalPanicAPI.spreadFromSystem(event.targetSystemId, .5f, 10000f,
                            "MATERIALIZED_CONTACT");
                    PTSDCrisisState.SystemData data = state.getSystemData(event.targetSystemId);
                    if (data != null) { data.knownToPlayer = true; data.lastObservedDay = day; }
                    if (state.phase != PTSDCrisisState.Phase.WAR) {
                        if (state.totalOmegaEncounters >= warning_encounter_threshold) showSoftWarning(state);
                        if (state.softWarningShown) PTSDCrisisIntel.ensureIntel();
                    }
                }
            }
        }
        projectRecentAftermath(state, day, player);
    }

    private void projectRecentAftermath(PTSDCrisisState state, float day, CampaignFleetAPI player) {
        StarSystemAPI current = player.getStarSystem();
        if (current == null) return;
        Float cooldown = state.aftermathCooldowns.get(current.getId());
        if (cooldown != null && day < cooldown) return;
        for (int i = state.events.size() - 1; i >= 0; i--) {
            PTSDCrisisState.StrategicEvent event = state.events.get(i);
            if (event.status != PTSDCrisisState.EventStatus.RESOLVED || event.aftermathProjected ||
                    event.aftermathKind == null || !current.getId().equals(event.targetSystemId) ||
                    day > event.resolveDay + 60f) continue;
            MarketAPI market = state.resolveMarket(event.targetMarketId);
            SectorEntityToken focus = market != null && market.getPrimaryEntity() != null ?
                    market.getPrimaryEntity() : current.getCenter();
            if (focus == null) return;

            boolean omegaDefeat = "OMEGA_DEFEAT".equals(event.aftermathKind);
            int debrisCount = omegaDefeat ? 5 : 3;
            for (int n = 0; n < debrisCount; n++) {
                DebrisFieldParams params = new DebrisFieldParams(
                        Math.max(180f, Math.min(520f, 170f + event.strength * (0.8f + n * .08f))),
                        -1f, 60f, omegaDefeat ? 1.5f : 2.5f);
                params.source = DebrisFieldSource.BATTLE;
                params.baseSalvageXP = Math.max(0, Math.round(event.strength * 1.5f));
                SectorEntityToken debris = Misc.addDebrisField(current, params, random);
                Vector2f location = findSafePoint(current, focus, 1800f + n * 350f, 5200f + n * 500f);
                debris.setLocation(location.x, location.y);
                debris.setDiscoverable(null);
                debris.setDiscoveryXP(null);
            }
            if (omegaDefeat) {
                spawnOmegaWreck(current, focus, "IIRT_Omega_Arrow_Only");
                if (event.strength >= 80f) spawnOmegaWreck(current, focus, "IIRT_Omega_Antitrack_Only");
                spawnHumanWreck(current, focus, event.opponentFactionId);
                if (event.strength >= 100f) spawnHumanWreck(current, focus, event.opponentFactionId);
            } else {
                spawnHumanWreck(current, focus, event.opponentFactionId);
                if (event.strength >= 90f) spawnHumanWreck(current, focus, event.opponentFactionId);
            }
            event.aftermathProjected = true;
            state.aftermathCooldowns.put(current.getId(), day + 4f);
            PTSDCrisisDevIntel.report("战线残骸投影",
                    event.type + " / " + (omegaDefeat ? "人类惨胜与不可回收欧米伽残舰" : "人类防线损毁"),
                    current.getId(), null);
            break;
        }
    }

    private void spawnOmegaWreck(StarSystemAPI system, SectorEntityToken focus, String variantId) {
        try {
            PerShipData ship = new PerShipData(variantId, ShipCondition.WRECKED, PSYCHASTHENIA_FACTION, 0f);
            ship.addDmods = true;
            ship.pruneWeapons = true;
            DerelictShipEntityPlugin.DerelictShipData data =
                    new DerelictShipEntityPlugin.DerelictShipData(ship, false);
            data.durationDays = 60f;
            SectorEntityToken wreck = BaseThemeGenerator.addSalvageEntity(random, system, Entities.WRECK,
                    PSYCHASTHENIA_FACTION, data);
            Vector2f point = findSafePoint(system, focus, 2200f, 5200f);
            wreck.setLocation(point.x, point.y);
            wreck.setName("无法修复的精神创伤残舰");
            wreck.addTag(Tags.UNRECOVERABLE);
            wreck.addTag(Tags.NO_BATTLE_SALVAGE);
            wreck.getDropRandom().clear();
            wreck.getDropValue().clear();
        } catch (Throwable ex) {
            Global.getLogger(getClass()).warn("Unable to project Omega wreck " + variantId, ex);
        }
    }

    private void spawnHumanWreck(StarSystemAPI system, SectorEntityToken focus, String factionId) {
        try {
            if (factionId == null || Global.getSector().getFaction(factionId) == null) factionId = Factions.INDEPENDENT;
            DerelictShipEntityPlugin.DerelictShipData data =
                    DerelictShipEntityPlugin.createRandom(factionId, null, random, 0f);
            data.durationDays = 60f;
            SectorEntityToken wreck = BaseThemeGenerator.addSalvageEntity(random, system, Entities.WRECK, factionId, data);
            Vector2f point = findSafePoint(system, focus, 1800f, 6000f);
            wreck.setLocation(point.x, point.y);
            wreck.setName(Global.getSector().getFaction(factionId).getDisplayName() + "战损舰体");
        } catch (Throwable ex) {
            Global.getLogger(getClass()).warn("Unable to project human wreck", ex);
        }
    }
    private boolean shouldMaterialize(PTSDCrisisState state, PTSDCrisisState.StrategicEvent event, CampaignFleetAPI player) {
        if (event.type == PTSDCrisisState.EventType.CONSTRUCTION) return false;
        if (event.type == PTSDCrisisState.EventType.PREWAR_HUNTER || event.type == PTSDCrisisState.EventType.GRUDGE_RAID) return true;
        float day = PTSDCrisisState.getDay();
        if (event.status == PTSDCrisisState.EventStatus.PLANNED && day < event.nextProjectionDay) return false;
        if (event.status == PTSDCrisisState.EventStatus.PLANNED) {
            int global = 0;
            int local = 0;
            for (PTSDCrisisState.StrategicEvent other : state.getActiveEvents()) {
                if (other.status != PTSDCrisisState.EventStatus.MATERIALIZED) continue;
                global++;
                if (event.targetSystemId != null && event.targetSystemId.equals(other.targetSystemId)) local++;
            }
            if (global >= 5 || local >= 2) return false;
        }
        StarSystemAPI target = state.resolveSystem(event.targetSystemId);
        if (target == null) return false;
        if (player.getStarSystem() == target) return true;
        return Misc.getDistance(player.getLocationInHyperspace(), target.getLocation()) <= hidden_materialization_range;
    }

    private Vector2f findSafePoint(StarSystemAPI system, SectorEntityToken focus, float minRadius, float maxRadius) {
        Vector2f center = focus == null ? system.getCenter().getLocation() : focus.getLocation();
        for (int attempt = 0; attempt < 30; attempt++) {
            float radius = minRadius + random.nextFloat() * Math.max(1f, maxRadius - minRadius);
            Vector2f point = Misc.getPointAtRadius(center, radius);
            boolean safe = true;
            for (PlanetAPI planet : system.getPlanets()) {
                float clearance = Math.max(1400f, planet.getRadius() + 1100f);
                if (Misc.getDistance(point, planet.getLocation()) < clearance) { safe = false; break; }
            }
            if (safe) return point;
        }
        if (!system.getJumpPoints().isEmpty()) {
            SectorEntityToken jump = system.getJumpPoints().get(0);
            return Misc.getPointAtRadius(jump.getLocation(), Math.max(1000f, jump.getRadius() + 700f));
        }
        return Misc.getPointAtRadius(center, Math.max(8000f, maxRadius));
    }
    private List<CampaignFleetAPI> findEventFleets(PTSDCrisisState.StrategicEvent event) {
        List<CampaignFleetAPI> result = new ArrayList<CampaignFleetAPI>();
        if (event == null) return result;
        if (event.materializedFleetIds == null) event.materializedFleetIds = new ArrayList<String>();
        if (event.materializedFleetId != null && !event.materializedFleetIds.contains(event.materializedFleetId)) {
            event.materializedFleetIds.add(0, event.materializedFleetId);
        }
        for (String id : new ArrayList<String>(event.materializedFleetIds)) {
            CampaignFleetAPI fleet = findFleet(id);
            if (fleet == null) event.materializedFleetIds.remove(id);
            else if (!result.contains(fleet)) result.add(fleet);
        }
        for (LocationAPI location : Global.getSector().getAllLocations()) {
            for (CampaignFleetAPI fleet : location.getFleets()) {
                if (fleet == null || !event.id.equals(fleet.getMemoryWithoutUpdate().getString(EVENT_MEMORY))) continue;
                if (!result.contains(fleet)) result.add(fleet);
                if (!event.materializedFleetIds.contains(fleet.getId())) event.materializedFleetIds.add(fleet.getId());
            }
        }
        event.materializedFleetId = result.isEmpty() ? null : result.get(0).getId();
        return result;
    }

    private boolean hasEventFleet(PTSDCrisisState.StrategicEvent event) {
        return !findEventFleets(event).isEmpty();
    }

    private void despawnEventFleets(PTSDCrisisState.StrategicEvent event) {
        for (CampaignFleetAPI fleet : findEventFleets(event)) {
            if (fleet != null && fleet != Global.getSector().getPlayerFleet() && fleet.getBattle() == null) {
                fleet.despawn(FleetDespawnReason.OTHER, event);
            }
        }
        if (event.materializedFleetIds != null) event.materializedFleetIds.clear();
        event.materializedFleetId = null;
    }

    private SectorEntityToken pickProjectionTarget(StarSystemAPI system, SectorEntityToken primary) {
        WeightedRandomPicker<SectorEntityToken> picker = new WeightedRandomPicker<SectorEntityToken>(random);
        if (primary != null) picker.add(primary, 5f);
        for (SectorEntityToken jump : system.getJumpPoints()) picker.add(jump, 2f);
        for (PlanetAPI planet : system.getPlanets()) {
            if (planet != null && !planet.isStar()) picker.add(planet, 1.5f);
        }
        SectorEntityToken picked = picker.pick();
        return picked == null ? primary : picked;
    }

    private void materializeEvent(PTSDCrisisState state, PTSDCrisisState.StrategicEvent event) {
        StarSystemAPI system = state.resolveSystem(event.targetSystemId);
        if (system == null) return;
        MarketAPI market = state.resolveMarket(event.targetMarketId);
        boolean pursuit = event.type == PTSDCrisisState.EventType.PREWAR_HUNTER ||
                event.type == PTSDCrisisState.EventType.GRUDGE_RAID;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        SectorEntityToken primaryTarget = pursuit && player != null ? player :
                (market != null && market.getPrimaryEntity() != null ? market.getPrimaryEntity() : system.getCenter());
        LocationAPI spawnLocation = pursuit && player != null && player.getContainingLocation() != null ?
                player.getContainingLocation() : system;
        String factionId = event.factionId;
        if (factionId == null || Global.getSector().getFaction(factionId) == null) {
            factionId = PTSDCrisisAPI.SIDE_OMEGA.equals(event.side) ? PSYCHASTHENIA_FACTION : Factions.INDEPENDENT;
        }

        float baseCombat = Math.max(20f, Math.min(final_invasion_max_strength, event.strength));
        float severity = PTSDCrisisAPI.SIDE_OMEGA.equals(event.side)
                ? PTSDOmegaFleetScaling.severityFor(event.type, event.strength) : 0f;
        float totalCombat = PTSDCrisisAPI.SIDE_OMEGA.equals(event.side)
                ? PTSDOmegaFleetScaling.scale(baseCombat, severity) : baseCombat;
        int maxGroups = pursuit ? 1 : Math.max(1, Math.min(3, (int) Math.floor(totalCombat / 24f)));
        int groupCount = pursuit ? 1 : 1 + random.nextInt(maxGroups);
        float[] shares = new float[groupCount];
        float shareTotal = 0f;
        for (int i = 0; i < groupCount; i++) {
            shares[i] = .75f + random.nextFloat() * .5f;
            shareTotal += shares[i];
        }

        if (event.materializedFleetIds == null) event.materializedFleetIds = new ArrayList<String>();
        event.materializedFleetIds.clear();
        event.materializedFleetId = null;
        boolean playerInsideTarget = player != null && player.getStarSystem() == system;
        for (int index = 0; index < groupCount; index++) {
            SectorEntityToken target = pursuit ? primaryTarget : pickProjectionTarget(system, primaryTarget);
            SectorEntityToken spawnFocus = playerInsideTarget ? player : target;
            Vector2f spawn = pursuit ? Misc.getPointAtRadius(primaryTarget.getLocation(),
                    4200f + random.nextFloat() * 1800f) :
                    findSafePoint(system, spawnFocus, playerInsideTarget ? 2200f : 5500f,
                            playerInsideTarget ? 3800f : 8000f);
            float combat = Math.max(10f, totalCombat * shares[index] / shareTotal);
            float baseShare = Math.max(10f, baseCombat * shares[index] / shareTotal);
            String fleetType = event.type == PTSDCrisisState.EventType.MERCENARY_DEFENSE ?
                    FleetTypes.MERC_ARMADA : FleetTypes.TASK_FORCE;
            FleetParamsV3 params = new FleetParamsV3(spawn, factionId, 1f, fleetType,
                    combat, 0f, 0f, 0f, 0f, 0f, 0f);
            params.maxNumShips = Math.max(8, Math.round(Global.getSettings().getMaxShipsInFleet() * 1.25f));
            CampaignFleetAPI fleet;
            if (PTSDCrisisAPI.SIDE_OMEGA.equals(event.side)) {
                fleet = PTSD_BaseShard_Util.createFleet(params, combat,
                        PTSD_BaseShard_Util.FleetRole.GUARD_ASSAULT, random);
            } else {
                fleet = FleetFactoryV3.createFleet(params);
            }
            if (fleet == null) continue;
            if (PTSDCrisisAPI.SIDE_OMEGA.equals(event.side)) {
                PTSDOmegaFleetScaling.record(fleet, baseShare, combat, severity);
            }
            spawnLocation.addEntity(fleet);
            fleet.setLocation(spawn.x, spawn.y);
            fleet.addTag(CRISIS_FLEET_TAG);
            fleet.getMemoryWithoutUpdate().set(EVENT_MEMORY, event.id);
            fleet.getMemoryWithoutUpdate().set("$PTSD_projection_group", index);
            fleet.getEventListeners().add(new PTSDStrategicFleetListener(event.id, combat));
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY,
                    PTSDCrisisAPI.SIDE_OMEGA.equals(event.side));
            if (PTSDCrisisAPI.SIDE_OMEGA.equals(event.side)) {
                if (event.type == PTSDCrisisState.EventType.FIRE_PROBE) fleet.setName("第四窥视火控试探单元");
                else fleet.setName(event.type == PTSDCrisisState.EventType.FORTRESS_PATROL ?
                        "要塞巡弋单元" : "精神创伤战区单元");
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
            } else {
                fleet.setName(event.type == PTSDCrisisState.EventType.MERCENARY_DEFENSE ?
                        "自由联盟雇佣舰队" : "殖民地联合防卫队");
            }
            if (groupCount > 1) fleet.setName(fleet.getName() + "·分遣" + (index + 1));
            fleet.clearAssignments();
            float assignmentDays = Math.max(5f, event.resolveDay - PTSDCrisisState.getDay());
            if (event.type == PTSDCrisisState.EventType.PREWAR_HUNTER) {
                fleet.setName("第四窥视截获单元");
                fleet.addAssignment(FleetAssignment.INTERCEPT, target, assignmentDays, "正在截获高价值目标");
            } else if (event.type == PTSDCrisisState.EventType.GRUDGE_RAID) {
                fleet.setName(event.strength >= 150f ? "精神创伤处决集群" : "精神创伤消耗单元");
                fleet.addAssignment(FleetAssignment.INTERCEPT, target, assignmentDays, "追踪高价值目标");
            } else if (event.type == PTSDCrisisState.EventType.ATTACK) {
                fleet.addAssignment(FleetAssignment.ATTACK_LOCATION, target, assignmentDays,
                        "突破 " + target.getName());
            } else if (event.type == PTSDCrisisState.EventType.FIRE_PROBE) {
                fleet.addAssignment(FleetAssignment.ATTACK_LOCATION, target,
                        Math.min(assignmentDays, 7f), "测量火力响应");
            } else if (event.type == PTSDCrisisState.EventType.FORTRESS_PATROL ||
                    event.type == PTSDCrisisState.EventType.GARRISON) {
                fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, target,
                        Math.max(8f, assignmentDays), "封锁星系");
            } else {
                fleet.addAssignment(FleetAssignment.DEFEND_LOCATION, target, assignmentDays, "阻滞进攻");
            }
            if (PTSDCrisisAPI.SIDE_OMEGA.equals(event.side)) {
                PTSDCrisisDevIntel.report("精神创伤分支舰队生成",
                        event.type + " / 分支 " + PTSD_BaseShard_Util.getFleetBranchName(fleet) +
                                " / 强度 " + Math.round(combat),
                        event.targetSystemId, fleet.getId());
            }
            event.materializedFleetIds.add(fleet.getId());
            if (event.materializedFleetId == null) event.materializedFleetId = fleet.getId();
        }
        if (event.materializedFleetIds.isEmpty()) return;
        event.materializedDay = PTSDCrisisState.getDay();
        event.lastPlayerNearDay = event.materializedDay;
        event.projectionExpiresDay = event.materializedDay + 10f;
        event.nextProjectionDay = event.materializedDay + 7f;
        event.status = PTSDCrisisState.EventStatus.MATERIALIZED;
        PTSDCrisisDevIntel.report("战略事件实体化",
                event.type + " / " + event.side + " / " + event.materializedFleetIds.size() +
                        "支分遣舰队 / 总强度 " + Math.round(totalCombat),
                event.targetSystemId, event.materializedFleetId);
    }

    private void revealNearbyCrisisActivity(PTSDCrisisState state) {
        if (state.phase == PTSDCrisisState.Phase.DORMANT || state.phase == PTSDCrisisState.Phase.RECON ||
                state.phase == PTSDCrisisState.Phase.WAR || Global.getSector().getPlayerFleet() == null) return;
        StarSystemAPI current = Global.getSector().getPlayerFleet().getStarSystem();
        if (current == null) return;
        PTSDCrisisState.SystemData data = state.getSystemData(current.getId());
        if ((data.omegaControl > 0f || data.conversionLevel > 0 || data.blackHoleFortress) && !data.knownToPlayer) {
            data.knownToPlayer = true;
            data.lastObservedDay = PTSDCrisisState.getDay();
            state.totalOmegaEncounters++;
            state.visibleStage = Math.max(state.visibleStage, visibleStageForPhase(state.phase));
            PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.HUMAN_AWARENESS,
                    5f, "OCCUPIED_ZONE_DISCOVERY", current.getId());
            PTSDLocalPanicAPI.spreadFromSystem(current.getId(), 2f, 18000f,
                    "OCCUPIED_ZONE_DISCOVERY");
            if (state.totalOmegaEncounters >= warning_encounter_threshold) showSoftWarning(state);
            if (state.softWarningShown) PTSDCrisisIntel.ensureIntel();
        }
    }
    private CampaignFleetAPI findFleet(String fleetId) {
        if (fleetId == null || Global.getSector() == null) return null;
        SectorEntityToken entity = Global.getSector().getEntityById(fleetId);
        return entity instanceof CampaignFleetAPI ? (CampaignFleetAPI) entity : null;
    }

    private static void ensureCrisisFleetListeners() {
        for (LocationAPI location : Global.getSector().getAllLocations()) {
            for (CampaignFleetAPI fleet : location.getFleets()) {
                if (fleet == null || fleet.getFaction() == null) continue;
                String factionId = fleet.getFaction().getId();
                if (!WATCHER_FACTION.equals(factionId) && !PSYCHASTHENIA_FACTION.equals(factionId)) continue;
                boolean found = false;
                for (com.fs.starfarer.api.campaign.listeners.FleetEventListener listener : fleet.getEventListeners()) {
                    if (listener instanceof PTSDStrategicFleetListener) { found = true; break; }
                }
                if (!found) fleet.getEventListeners().add(new PTSDStrategicFleetListener(null, fleet.getFleetPoints()));
            }
        }
    }
    private int countTaggedFleets(String tag) {
        int result = 0;
        for (LocationAPI location : Global.getSector().getAllLocations()) {
            for (CampaignFleetAPI fleet : location.getFleets()) if (fleet.hasTag(tag)) result++;
        }
        return result;
    }

    private float frequencyAdjusted(float days) {
        return days / Math.max(0.1f, unknown_event_frequency *
                PTSDCrisisDetectorAbility.getEventFrequencyMultiplier());
    }
    private float frequencyAdjustedHuman(float days) {
        return days / Math.max(0.1f, unknown_event_frequency);
    }
    private float randomBetween(float min, float max) {
        float low = Math.min(min, max);
        float high = Math.max(min, max);
        return low + random.nextFloat() * Math.max(0f, high - low);
    }
}