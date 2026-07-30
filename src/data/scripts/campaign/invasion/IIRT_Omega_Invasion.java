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
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldSource;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
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
        if (Global.getSector() == null || Global.getSector().getClock() == null || !isInvasionEnabled()) return;
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return;
        ensureRuntimeFields();
        initializeOrMigrate(state);
        heartbeat.advance(Global.getSector().getClock().convertToDays(amount));
        if (!heartbeat.intervalElapsed()) return;

        float day = PTSDCrisisState.getDay();
        stageElapsed = Math.max(0f, day - state.phaseStartedDay);
        currStage = stageForPhase(state.phase);
        Global.getSector().getMemoryWithoutUpdate().set(stage_id, currStage);
        revealNearbyCrisisActivity(state);
        processMaterializationAndEncounters(state, day);
        if (day >= state.nextWeightUpdateDay) {
            updateSystemWeights(state, day);
            state.nextWeightUpdateDay = day + Math.max(1f, strategic_update_interval);
        }

        switch (state.phase) {
            case DORMANT:
                if (stageElapsed >= start_stage_time) transition(state, PTSDCrisisState.Phase.RECON);
                break;
            case RECON:
                runRecon(state, day);
                if (stageElapsed >= collect_data_time) transition(state, PTSDCrisisState.Phase.EXPANSION);
                break;
            case EXPANSION:
                ensureBase(state);
                runExpansion(state, day);
                if (stageElapsed >= invade_time) transition(state, PTSDCrisisState.Phase.FORTIFICATION);
                break;
            case FORTIFICATION:
                ensureBase(state);
                runExpansion(state, day);
                runFortification(state, day);
                if (stageElapsed >= repair_time) beginWar(state);
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

    private void runRecon(PTSDCrisisState state, float day) {
        if (state.totalScoutSightings >= warning_encounter_threshold) showSoftWarning(state);
        if (day < state.nextScoutDay) return;
        state.nextScoutDay = day + randomBetween(scout_min_interval, scout_max_interval);
        if (countTaggedFleets(SCOUT_TAG) >= Math.max(1, Math.round(scout_max_active))) return;
        spawnScout(state);
    }

    private void spawnScout(PTSDCrisisState state) {
        WeightedRandomPicker<SectorEntityToken> targets = new WeightedRandomPicker<SectorEntityToken>(random);
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system == null || system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)) continue;
            PTSDCrisisState.SystemData data = state.getSystemData(system.getId());
            float systemWeight = (system.isEnteredByPlayer() ? 1.15f : 1f) / (1f + data.scoutVisits * 0.28f);
            for (SectorEntityToken relay : system.getEntitiesWithTag(Tags.COMM_RELAY)) targets.add(relay, 8f * systemWeight);
            for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system)) {
                if (!market.isPlanetConditionMarketOnly() && market.getPrimaryEntity() != null) {
                    targets.add(market.getPrimaryEntity(), (1.5f + market.getSize()) * systemWeight);
                }
            }
            if (system.getCenter() != null) targets.add(system.getCenter(), 0.45f * systemWeight);
        }
        SectorEntityToken target = targets.pick();
        if (target == null || target.getStarSystem() == null) return;
        float radius = Math.max(900f, scout_spawn_radius + random.nextFloat() * scout_spawn_radius * 2.5f);
        Vector2f location = Misc.getUnitVectorAtDegreeAngle(random.nextFloat() * 360f);
        location.scale(radius);
        Vector2f.add(target.getLocation(), location, location);
        FleetParamsV3 params = new FleetParamsV3(location, WATCHER_FACTION, 0.4f,
                FleetTypes.MERC_SCOUT, 12f + random.nextFloat() * 14f, 0f, 0f, 0f, 0f, 0f, 0f);
        params.maxNumShips = Math.max(3, Global.getSettings().getMaxShipsInFleet() / 4);
        CampaignFleetAPI scout = FleetFactoryV3.createFleet(params);
        if (scout == null) return;
        target.getStarSystem().addEntity(scout);
        scout.setLocation(location.x, location.y);
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
        scout.addScript(new IIRT_Omega_ScoutAI(scout, target));
        PTSDCrisisState.SystemData data = state.getSystemData(target.getStarSystem().getId());
        data.scoutVisits++;
        data.lastObservedDay = PTSDCrisisState.getDay();
        PTSDCrisisDevIntel.report("侦察舰队生成",
                "目标 " + target.getFullName() + "；侦察次数 " + data.scoutVisits,
                target.getStarSystem().getId(), scout.getId());
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
        if (state.totalScoutSightings >= warning_encounter_threshold) showSoftWarning(state);
        else if (state.softWarningShown) PTSDCrisisIntel.ensureIntel();
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
        if (escapedPlayer) {
            state.totalOmegaEncounters++;
            state.visibleStage = Math.max(state.visibleStage, visibleStageForPhase(state.phase));
        }
        PTSDCrisisDevIntel.report("侦察单位脱离",
                escapedPlayer ? "成功摆脱玩家追逐" : "完成常规撤离", systemId, null);
    }

    public static void reportReconSample(String systemId, float fleetStrength) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || systemId == null) return;
        PTSDCrisisState.SystemData data = state.getSystemData(systemId);
        data.observedFleetStrength = Math.max(data.observedFleetStrength * 0.82f, fleetStrength);
        data.lastObservedDay = PTSDCrisisState.getDay();
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

    private void beginWar(PTSDCrisisState state) {
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

    private static void transferWatcherToPsychasthenia(PTSDCrisisState state) {
        if (state.watcherTransferred) return;
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
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (WATCHER_FACTION.equals(market.getFactionId())) {
                market.setFactionId(PSYCHASTHENIA_FACTION);
                if (market.getPrimaryEntity() != null) market.getPrimaryEntity().setFaction(PSYCHASTHENIA_FACTION);
                for (PersonAPI person : market.getPeopleCopy()) person.setFaction(PSYCHASTHENIA_FACTION);
            }
        }
        state.watcherTransferred = true;
    }

    private void configureWarFaction() {
        FactionAPI omega = Global.getSector().getFaction(PSYCHASTHENIA_FACTION);
        if (omega == null) return;
        omega.getDoctrine().setNumShips(5);
        omega.getDoctrine().setOfficerQuality(5);
        omega.getDoctrine().setShipQuality(5);
        omega.getDoctrine().setAggression(5);
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (!PSYCHASTHENIA_FACTION.equals(faction.getId())) omega.setRelationship(faction.getId(), -1f);
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
        baseMarket.setPlanetConditionMarketOnly(false);
        baseMarket.setSize(7);
        baseMarket.setFactionId(PSYCHASTHENIA_FACTION);
        baseMarket.setPlayerOwned(false);
        baseMarket.setPrimaryEntity(planet);
        planet.setFaction(PSYCHASTHENIA_FACTION);
        if (!baseMarket.hasCondition("IIRT_Omega_Repair_Facility")) baseMarket.addCondition("IIRT_Omega_Repair_Facility");
        ensureIndustry(baseMarket, Industries.POPULATION);
        ensureIndustry(baseMarket, Industries.MEGAPORT);
        ensureIndustry(baseMarket, Industries.ORBITALWORKS);
        ensureIndustry(baseMarket, Industries.HIGHCOMMAND);
        ensureIndustry(baseMarket, Industries.PLANETARYSHIELD);
        if (!baseMarket.isInEconomy()) Global.getSector().getEconomy().addMarket(baseMarket, false);
        state.baseSystemId = baseSystem.getId();
        state.baseMarketId = baseMarket.getId();
        Global.getSector().getMemoryWithoutUpdate().set(baseSystem_id, state.baseSystemId);
        Global.getSector().getMemoryWithoutUpdate().set(baseMarket_id, state.baseMarketId);
        PTSDCrisisState.SystemData data = state.getSystemData(baseSystem.getId());
        data.omegaControl = 1f;
        data.humanControl = 0f;
        data.conversionLevel = Math.max(1, data.conversionLevel);
        applyPlanetMutation(planet, 4);
        PTSDCrisisDevIntel.report("核心据点建立",
                baseMarket.getName() + " 已完成初始大幅改造", state.baseSystemId, planet.getId());
    }

    private static void ensureIndustry(MarketAPI market, String industryId) {
        if (!market.hasIndustry(industryId)) market.addIndustry(industryId);
    }

    private void runExpansion(PTSDCrisisState state, float day) {
        if (day < state.nextExpansionDay) return;
        state.nextExpansionDay = day + Math.max(3f, expansion_interval);
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
                if (market != null && !market.isPlanetConditionMarketOnly() && !PSYCHASTHENIA_FACTION.equals(market.getFactionId())) continue;
                picker.add(planet, planet.isGasGiant() ? 0.65f : 1.2f);
            }
        }
        PlanetAPI planet = picker.pick();
        if (planet == null) return;
        PTSDCrisisState.SystemData data = state.getSystemData(planet.getStarSystem().getId());
        data.conversionLevel = Math.min(5, data.conversionLevel + 1);
        applyPlanetMutation(planet, Math.max(1, data.conversionLevel));
        PTSDCrisisDevIntel.report("行星扩张改造",
                planet.getName() + " 改造等级 " + data.conversionLevel,
                planet.getStarSystem().getId(), planet.getId());
        planet.setFaction(PSYCHASTHENIA_FACTION);
        if (planet.getMarket() != null && planet.getMarket().isPlanetConditionMarketOnly()) {
            planet.getMarket().setFactionId(PSYCHASTHENIA_FACTION);
            planet.getMarket().getMemoryWithoutUpdate().set("$PTSD_mutation_level", data.conversionLevel);
        }
        state.addEvent(PTSDCrisisState.EventType.CONSTRUCTION, PTSDCrisisAPI.SIDE_OMEGA,
                PSYCHASTHENIA_FACTION, state.baseSystemId, planet.getStarSystem().getId(),
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
        state.nextFortressDay = day + Math.max(12f, expansion_interval * 1.5f);
        if (countFortresses(state) < Math.max(0, Math.round(max_black_hole_fortresses))) createBlackHoleFortress(state);
        if (state.baseSystemId != null && state.countActiveEvents(PTSDCrisisState.EventType.GARRISON) < max_guard_fleets) {
            state.addEvent(PTSDCrisisState.EventType.GARRISON, PTSDCrisisAPI.SIDE_OMEGA,
                    PSYCHASTHENIA_FACTION, state.baseSystemId, state.baseSystemId, state.baseMarketId, 85f, 12f);
        }
        for (PTSDCrisisState.SystemData data : state.systems.values()) {
            if (!data.blackHoleFortress || hasActiveEventFor(state, PTSDCrisisState.EventType.FORTRESS_PATROL, data.systemId)) continue;
            state.addEvent(PTSDCrisisState.EventType.FORTRESS_PATROL, PTSDCrisisAPI.SIDE_OMEGA,
                    PSYCHASTHENIA_FACTION, data.systemId, data.systemId, null, 135f, 18f);
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
        CampaignFleetAPI fortress = Global.getFactory().createEmptyFleet(PSYCHASTHENIA_FACTION, "癫狂视界要塞", true);
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
        fortress.clearAbilities();
        fortress.addAbility(Abilities.TRANSPONDER);
        if (fortress.getAbility(Abilities.TRANSPONDER) != null) fortress.getAbility(Abilities.TRANSPONDER).activate();
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
                PSYCHASTHENIA_FACTION, system.getId(), system.getId(), null, 150f, 18f);
        PTSDCrisisDevIntel.report("黑洞要塞建立",
                system.getName() + " 的黑洞已转化为欧米伽要塞", system.getId(), fortress.getId());
    }
    private void runWar(PTSDCrisisState state, float day) {
        showHardWarning(state);
        configureWarFaction();
        runExpansion(state, day);
        runFortification(state, day);
        if (day >= state.nextOmegaTurnDay) {
            scheduleOmegaTurn(state);
            state.nextOmegaTurnDay = day + randomBetween(front_turn_min_interval, front_turn_max_interval);
        }
        if (day >= state.nextHumanTurnDay) {
            scheduleHumanTurn(state);
            state.nextHumanTurnDay = day + randomBetween(front_turn_min_interval, front_turn_max_interval) + 1.5f;
        }
        resolveDueEvents(state, day);
        if (Global.getSector().getMemoryWithoutUpdate().getBoolean("$IIRT_omega_Invasion_End")) {
            transition(state, PTSDCrisisState.Phase.ENDED);
            if (baseMarket != null && baseMarket.hasCondition("IIRT_Omega_Repair_Facility")) baseMarket.removeCondition("IIRT_Omega_Repair_Facility");
        }
    }

    private void scheduleOmegaTurn(PTSDCrisisState state) {
        MarketAPI target = pickAttackTarget(state);
        if (target == null || target.getStarSystem() == null) return;
        StarSystemAPI source = pickOmegaSource(state, target.getStarSystem());
        PTSDCrisisState.SystemData targetData = state.getSystemData(target.getStarSystem().getId());
        float confidence = Math.min(1f, 0.15f + targetData.scoutVisits * 0.16f + targetData.playerSightings * 0.08f);
        float strength = Math.min(final_invasion_max_strength,
                Math.max(55f, 75f + targetData.attackWeight * 1.8f + confidence * 65f));
        PTSDCrisisState.StrategicEvent event = state.addEvent(PTSDCrisisState.EventType.ATTACK,
                PTSDCrisisAPI.SIDE_OMEGA, PSYCHASTHENIA_FACTION,
                source == null ? state.baseSystemId : source.getId(), target.getStarSystem().getId(),
                target.getId(), strength, 9f + random.nextFloat() * 8f);
        event.description = "根据侦察权重选择的突破行动；目标防御越薄弱，部署优先级越高。";
        event.playerRelevant = target.isPlayerOwned();
    }

    private MarketAPI pickAttackTarget(PTSDCrisisState state) {
        WeightedRandomPicker<MarketAPI> picker = new WeightedRandomPicker<MarketAPI>(random);
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
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

    private void resolveDueEvents(PTSDCrisisState state, float day) {
        for (PTSDCrisisState.StrategicEvent event : state.events) {
            if (event.status != PTSDCrisisState.EventStatus.MATERIALIZED || event.materializedFleetId == null ||
                    day >= event.resolveDay || PTSDCrisisAPI.SIDE_OMEGA.equals(event.side)) continue;
            if (findFleet(event.materializedFleetId) == null) {
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
            if (event.status == PTSDCrisisState.EventStatus.MATERIALIZED && event.materializedFleetId != null &&
                    findFleet(event.materializedFleetId) == null && day < event.resolveDay) {
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
                    event.type == PTSDCrisisState.EventType.MERCENARY_DEFENSE) {
                event.successful = true;
                event.status = PTSDCrisisState.EventStatus.RESOLVED;
                CampaignFleetAPI fleet = findFleet(event.materializedFleetId);
                if (fleet != null) fleet.despawn(FleetDespawnReason.OTHER, event);
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
        CampaignFleetAPI physical = findFleet(attack.materializedFleetId);
        if (physical != null && physical != Global.getSector().getPlayerFleet()) physical.despawn(FleetDespawnReason.OTHER, attack);
        PTSDCrisisState.SystemData data = state.getSystemData(attack.targetSystemId);
        if (success) {
            data.successfulOmegaAttacks++;
            data.learningMultiplier = Math.min(2.5f, data.learningMultiplier * 1.08f + 0.03f);
            data.omegaControl = 1f;
            data.humanControl = 0f;
            MarketAPI market = state.resolveMarket(attack.targetMarketId);
            if (market != null) destroyAndClaimColony(state, market);
        } else {
            data.failedOmegaAttacks++;
            data.learningMultiplier = Math.max(0.28f, data.learningMultiplier * 0.78f);
            data.attackWeight *= 0.72f;
        }
        for (PTSDCrisisState.StrategicEvent event : state.events) {
            if (event == attack || event.status == PTSDCrisisState.EventStatus.RESOLVED || !attack.targetSystemId.equals(event.targetSystemId)) continue;
            if (event.type == PTSDCrisisState.EventType.DEFENSE || event.type == PTSDCrisisState.EventType.MERCENARY_DEFENSE) {
                event.successful = !success;
                event.status = PTSDCrisisState.EventStatus.RESOLVED;
                CampaignFleetAPI fleet = findFleet(event.materializedFleetId);
                if (fleet != null) fleet.despawn(FleetDespawnReason.OTHER, event);
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
            for (CampaignFleetAPI fleet : system.getFleets()) {
                if (fleet.getFaction() == null) continue;
                String factionId = fleet.getFaction().getId();
                if (!PSYCHASTHENIA_FACTION.equals(factionId) && !WATCHER_FACTION.equals(factionId)) {
                    fleetDefense += Math.max(0f, fleet.getFleetPoints());
                }
            }
            for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system)) {
                if (market.isPlanetConditionMarketOnly()) continue;
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
            float confidence = Math.min(1f, 0.12f + data.scoutVisits * 0.16f + data.playerSightings * 0.05f);
            float uncertainty = 0.72f + seededNoise(system.getId(), day) * 0.56f;
            data.observedFleetStrength = Math.max(data.observedFleetStrength * 0.75f, fleetDefense * confidence * uncertainty);
            data.observedMarketDefense = Math.max(8f, trueDefense * (0.35f + confidence * 0.65f) * uncertainty);
            data.strategicValue = value;
            float vulnerability = value / (35f + data.observedMarketDefense);
            data.attackWeight = Math.max(0.05f, vulnerability * 34f * (0.2f + confidence * 0.8f) * Math.max(0.25f, data.learningMultiplier));
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
            data.humanDefenseWeight *= 1f + Math.min(0.4f, humanOccupationAttention * 0.05f);
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
            if (event.status == PTSDCrisisState.EventStatus.PLANNED && shouldMaterialize(state, event, player)) materializeEvent(state, event);
            if (event.status == PTSDCrisisState.EventStatus.MATERIALIZED && event.materializedFleetId != null) {
                CampaignFleetAPI fleet = findFleet(event.materializedFleetId);
                boolean projectionExpired = event.projectionExpiresDay > 0f && day >= event.projectionExpiresDay;
                if (fleet != null && (projectionExpired || !shouldMaterialize(state, event, player)) &&
                        fleet.getBattle() == null && day + 1f < event.resolveDay) {
                    event.strength = Math.max(1f, Math.min(event.strength, fleet.getFleetPoints()));
                    fleet.despawn(FleetDespawnReason.OTHER, event);
                    event.materializedFleetId = null;
                    event.status = PTSDCrisisState.EventStatus.PLANNED;
                    event.nextProjectionDay = Math.max(event.nextProjectionDay, day + 5f);
                    PTSDCrisisDevIntel.report("战略事件卸载", event.type + " 已离开玩家附近，回归隐藏推演",
                            event.targetSystemId, null);
                    continue;
                }
                if (fleet != null && PTSDCrisisAPI.SIDE_OMEGA.equals(event.side) && fleet.isVisibleToPlayerFleet() &&
                        !fleet.getMemoryWithoutUpdate().getBoolean("$PTSD_player_reported_contact")) {
                    fleet.getMemoryWithoutUpdate().set("$PTSD_player_reported_contact", true);
                    state.totalOmegaEncounters++;
                    state.visibleStage = Math.max(state.visibleStage, visibleStageForPhase(state.phase));
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
                    !current.getId().equals(event.targetSystemId) || day > event.resolveDay + 10f) continue;
            MarketAPI market = state.resolveMarket(event.targetMarketId);
            SectorEntityToken focus = market != null && market.getPrimaryEntity() != null
                    ? market.getPrimaryEntity() : current.getCenter();
            if (focus == null) return;
            DebrisFieldParams params = new DebrisFieldParams(
                    Math.max(180f, Math.min(450f, 150f + event.strength * 1.2f)),
                    -1f, 5f, event.successful ? 2f : 1f);
            params.source = DebrisFieldSource.BATTLE;
            params.baseSalvageXP = Math.max(0, Math.round(event.strength * 3f));
            SectorEntityToken debris = Misc.addDebrisField(current, params, random);
            Vector2f location = Misc.getPointWithinRadius(focus.getLocation(), 900f);
            debris.setLocation(location.x, location.y);
            debris.setDiscoverable(null);
            debris.setDiscoveryXP(null);
            event.aftermathProjected = true;
            state.aftermathCooldowns.put(current.getId(), day + 4f);
            PTSDCrisisDevIntel.report("战线残骸投影",
                    event.type + " 远程结算痕迹 / " + (event.successful ? "进攻方占优" : "防御方占优"),
                    current.getId(), debris.getId());
            break;
        }
    }

    private boolean shouldMaterialize(PTSDCrisisState state, PTSDCrisisState.StrategicEvent event, CampaignFleetAPI player) {
        if (event.type == PTSDCrisisState.EventType.CONSTRUCTION) return false;
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

    private void materializeEvent(PTSDCrisisState state, PTSDCrisisState.StrategicEvent event) {
        StarSystemAPI system = state.resolveSystem(event.targetSystemId);
        if (system == null) return;
        MarketAPI market = state.resolveMarket(event.targetMarketId);
        SectorEntityToken target = market != null && market.getPrimaryEntity() != null ? market.getPrimaryEntity() : system.getCenter();
        String factionId = event.factionId;
        if (factionId == null || Global.getSector().getFaction(factionId) == null) {
            factionId = PTSDCrisisAPI.SIDE_OMEGA.equals(event.side) ? PSYCHASTHENIA_FACTION : Factions.INDEPENDENT;
        }
        float combat = Math.max(20f, Math.min(final_invasion_max_strength, event.strength));
        String fleetType = event.type == PTSDCrisisState.EventType.MERCENARY_DEFENSE ? FleetTypes.MERC_ARMADA : FleetTypes.TASK_FORCE;
        Vector2f spawn = Misc.getPointWithinRadius(target.getLocation(), 5500f + random.nextFloat() * 2500f);
        FleetParamsV3 params = new FleetParamsV3(spawn, factionId, 1f, fleetType, combat, 0f, 0f, 0f, 0f, 0f, 0f);
        params.maxNumShips = Math.max(8, Math.round(Global.getSettings().getMaxShipsInFleet() * 1.25f));
        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
        if (fleet == null) return;
        system.addEntity(fleet);
        fleet.setLocation(spawn.x, spawn.y);
        fleet.addTag(CRISIS_FLEET_TAG);
        fleet.getMemoryWithoutUpdate().set(EVENT_MEMORY, event.id);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, PTSDCrisisAPI.SIDE_OMEGA.equals(event.side));
        if (PTSDCrisisAPI.SIDE_OMEGA.equals(event.side)) {
            fleet.setName(event.type == PTSDCrisisState.EventType.FORTRESS_PATROL ? "要塞巡弋单元" : "精神创伤战区单元");
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        } else {
            fleet.setName(event.type == PTSDCrisisState.EventType.MERCENARY_DEFENSE ? "自由联盟雇佣舰队" : "殖民地联合防卫队");
        }
        fleet.clearAssignments();
        float assignmentDays = Math.max(5f, event.resolveDay - PTSDCrisisState.getDay());
        if (event.type == PTSDCrisisState.EventType.ATTACK) {
            fleet.addAssignment(FleetAssignment.ATTACK_LOCATION, target, assignmentDays, "突破 " + target.getName());
        } else if (event.type == PTSDCrisisState.EventType.FORTRESS_PATROL || event.type == PTSDCrisisState.EventType.GARRISON) {
            fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, target, Math.max(8f, assignmentDays), "封锁星系");
        } else {
            fleet.addAssignment(FleetAssignment.DEFEND_LOCATION, target, assignmentDays, "阻滞进攻");
        }
        event.materializedFleetId = fleet.getId();
        event.materializedDay = PTSDCrisisState.getDay();
        event.projectionExpiresDay = Math.min(event.resolveDay, event.materializedDay + 2.5f);
        event.nextProjectionDay = event.materializedDay + 7f;
        event.status = PTSDCrisisState.EventStatus.MATERIALIZED;
        PTSDCrisisDevIntel.report("战略事件实体化",
                event.type + " / " + event.side + " / 强度 " + Math.round(event.strength),
                event.targetSystemId, fleet.getId());
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
            if (state.totalOmegaEncounters >= warning_encounter_threshold) showSoftWarning(state);
            if (state.softWarningShown) PTSDCrisisIntel.ensureIntel();
        }
    }

    private CampaignFleetAPI findFleet(String fleetId) {
        if (fleetId == null || Global.getSector() == null) return null;
        SectorEntityToken entity = Global.getSector().getEntityById(fleetId);
        return entity instanceof CampaignFleetAPI ? (CampaignFleetAPI) entity : null;
    }

    private int countTaggedFleets(String tag) {
        int result = 0;
        for (LocationAPI location : Global.getSector().getAllLocations()) {
            for (CampaignFleetAPI fleet : location.getFleets()) if (fleet.hasTag(tag)) result++;
        }
        return result;
    }

    private float randomBetween(float min, float max) {
        float low = Math.min(min, max);
        float high = Math.max(min, max);
        return low + random.nextFloat() * Math.max(0f, high - low);
    }
}