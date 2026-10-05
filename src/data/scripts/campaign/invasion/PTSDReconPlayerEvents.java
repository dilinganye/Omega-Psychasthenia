package data.scripts.campaign.invasion;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldSource;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.shard.PTSD_BaseShard_Util;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Player-centred RECON-stage ambient events. One persisted roll is made per campaign day. */
public final class PTSDReconPlayerEvents {
    public static final String TAG = "PTSD_recon_player_event";
    public static final String REACTIVE_RETREAT = "$PTSD_reactive_retreat";
    public static final String RETREAT_TRIGGERED = "$PTSD_retreat_triggered";
    private static final String WATCHER = IIRT_Omega_Invasion.WATCHER_FACTION;
    private static final Random RANDOM = new Random();

    private PTSDReconPlayerEvents() { }

    public static void advance(PTSDCrisisState state, float day) {
        if (state == null || state.phase != PTSDCrisisState.Phase.RECON || Global.getSector() == null) return;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null) return;
        int bucket = (int)Math.floor(day);
        if (state.reconPlayerEventDayBucket != bucket) {
            state.reconPlayerEventDayBucket = bucket;
            rollDaily(state, player, day);
        }
        checkTrapArrival(state, player, day);
    }

    private static void rollDaily(PTSDCrisisState state, CampaignFleetAPI player, float day) {
        if (RANDOM.nextFloat() < .01f) spawnMatchedManeuver(player);
        if (RANDOM.nextFloat() < .02f) armTrapFromNavigation(state, player, day);
        if (RANDOM.nextFloat() < .03f) spawnUnreachableObserver(player);
        if (RANDOM.nextFloat() < .025f) spawnRouteWrecks(player);
        if (player.isInHyperspace() && RANDOM.nextFloat() < .02f) spawnReactivePursuer(player);
    }

    private static CampaignFleetAPI createFleet(LocationAPI location, Vector2f point, float fp,
                                                  PTSD_BaseShard_Util.FleetRole role, String explicitBranch) {
        if (location == null || point == null) return null;
        // These ambient RECON encounters never use larger than destroyer hulls, even
        // when the player has an endgame fleet or a strategic difficulty setting is raised.
        float budget = Math.min(65f, Math.max(12f, fp));
        PTSD_BaseShard_Util.FleetLimits limits = new PTSD_BaseShard_Util.FleetLimits(
                budget, 3, com.fs.starfarer.api.combat.ShipAPI.HullSize.DESTROYER);
        FleetParamsV3 params = new FleetParamsV3(point, WATCHER, .5f, FleetTypes.PATROL_LARGE,
                budget, 0f, 0f, 0f, 0f, 0f, 0f);
        params.maxNumShips = 3;
        PTSD_BaseShard_Util.BranchDefinition branch;
        if (explicitBranch == null) {
            branch = PTSD_BaseShard_Util.prepareFleetParams(params, budget, role, RANDOM, limits);
        } else {
            branch = PTSD_BaseShard_Util.prepareFleetParams(params, budget, explicitBranch, RANDOM, limits);
        }
        if (branch == null || params.addShips == null || params.addShips.isEmpty()) return null;
        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
        if (!PTSD_BaseShard_Util.isWithinLimits(fleet, limits)) return null;
        PTSD_BaseShard_Util.tagFleet(fleet, branch);
        location.addEntity(fleet);
        fleet.setLocation(point.x, point.y);
        fleet.setTransponderOn(false);
        fleet.setNoFactionInName(true);
        fleet.addTag(TAG);
        fleet.addTag("PTSD_crisis_fleet");
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        return fleet;
    }

    private static Vector2f around(SectorEntityToken target, float min, float max) {
        if (target == null || target.getContainingLocation() == null) return null;
        return PTSDCrisisAPI.findSafePoint(target.getContainingLocation(), target, min, max, RANDOM);
    }

    private static void spawnMatchedManeuver(CampaignFleetAPI player) {
        float fp = Math.max(7f, player.getFleetPoints());
        CampaignFleetAPI fleet = createFleet(player.getContainingLocation(), around(player, 7000f, 11000f), fp,
                PTSD_BaseShard_Util.FleetRole.RECON, null);
        if (fleet == null) return;
        fleet.setName("未知舰队");
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        fleet.addAssignment(FleetAssignment.INTERCEPT, player, 20f, "逼近目标");
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, escapeToken(player), 30f, "脱离观测区");
        PTSDOmegaEncounterResponse.attachPursuer(fleet, player);
        PTSDCrisisDevIntel.report("每日事件：有限火力机动舰队", "玩家舰队 " + Math.round(fp) +
                " FP；本队实有 " + Math.round(fleet.getFleetPoints()) + " FP；分支 " + PTSD_BaseShard_Util.getFleetBranchName(fleet),
                player.getStarSystem() == null ? null : player.getStarSystem().getId(), fleet.getId());
    }

    private static void armTrapFromNavigation(PTSDCrisisState state, CampaignFleetAPI player, float day) {
        if (state.reconTrapSystemId != null && day < state.reconTrapExpiresDay) return;
        // A plotted navigation assignment is private player information. A scout which actually
        // sees the player may extrapolate a nearby empty system, but cannot read that assignment.
        boolean observed = false;
        for (CampaignFleetAPI fleet : player.getContainingLocation().getFleets()) {
            if (fleet == player || fleet.getFaction() == null ||
                    !WATCHER.equals(fleet.getFaction().getId())) continue;
            if (Misc.getDistance(fleet.getLocation(), player.getLocation()) < 4000f) {
                observed = true;
                break;
            }
        }
        if (!observed) return;
        StarSystemAPI system = null;
        float best = Float.MAX_VALUE;
        Vector2f origin = player.getLocationInHyperspace();
        for (StarSystemAPI candidate : Global.getSector().getStarSystems()) {
            if (!validTrapSystem(candidate, player)) continue;
            float distance = Misc.getDistance(origin, candidate.getLocation());
            if (distance < best) { best = distance; system = candidate; }
        }
        if (system == null) return;
        state.reconTrapSystemId = system.getId();
        state.reconTrapTargetEntityId = null;
        state.reconTrapExpiresDay = day + 45f;
        PTSDCrisisDevIntel.report("每日事件：陷阱判定已埋设", "侦察舰目击后的邻近星系预测；45日后失效", system.getId(), null);
    }

    private static boolean validTrapSystem(StarSystemAPI system, CampaignFleetAPI player) {
        if (system == null || system == player.getStarSystem()) return false;
        if (system.hasTag(Tags.THEME_HIDDEN) || system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)) return false;
        for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system)) {
            if (market != null && !market.isPlanetConditionMarketOnly()) return false;
        }
        return true;
    }

    private static void checkTrapArrival(PTSDCrisisState state, CampaignFleetAPI player, float day) {
        if (state.reconTrapSystemId == null) return;
        if (day >= state.reconTrapExpiresDay) { clearTrap(state); return; }
        StarSystemAPI current = player.getStarSystem();
        if (current == null || !state.reconTrapSystemId.equals(current.getId())) return;
        spawnTrapFormation(current, player);
        clearTrap(state);
    }

    private static void clearTrap(PTSDCrisisState state) {
        state.reconTrapSystemId = null;
        state.reconTrapTargetEntityId = null;
        state.reconTrapExpiresDay = 0f;
    }

    private static void spawnTrapFormation(StarSystemAPI system, CampaignFleetAPI player) {
        SectorEntityToken focus = pickSystemFocus(system);
        boolean inhabited = false;
        for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system)) {
            if (market != null && !market.isPlanetConditionMarketOnly()) {
                inhabited = true;
                break;
            }
        }
        // Shared actual-FP ceiling, including any generated escorts.
        float[] budgets = inhabited ? new float[] {35f, 25f} : new float[] {40f, 30f, 20f};
        float actualFp = 0f;
        int actualGroups = 0;
        CampaignFleetAPI leader = null;
        for (int i = 0; i < budgets.length; i++) {
            CampaignFleetAPI fleet = createFleet(system,
                    i == 0 ? safeAround(focus, 5000f, 8500f) :
                            safeAround(leader == null ? focus : leader, 1300f, 4200f),
                    Math.min(budgets[i], (inhabited ? 60f : 90f) - actualFp),
                    i == 0 ? PTSD_BaseShard_Util.FleetRole.RECON :
                            PTSD_BaseShard_Util.FleetRole.GUARD_ASSAULT, null);
            if (fleet == null) continue;
            if (leader == null) leader = fleet;
            fleet.setName("未知编队");
            if (inhabited) {
                fleet.setSensorProfile(20f);
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FLEET_TYPE, "未知编队");
            }
            makeTrapNeutral(fleet);
            fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, system.getCenter(), 30f, "保持静默机动");
            actualFp += fleet.getFleetPoints();
            actualGroups++;
        }
        if (leader == null) return;
        Global.getSector().getCampaignUI().addMessage("传感器上出现了极端的能量反应", new Color(255, 105, 125));
        try { Global.getSector().addPing(leader, "sensor_burst", new Color(220, 90, 255)); } catch (Throwable ignored) { }
        PTSDCrisisDevIntel.report("每日事件：陷阱舰群显现", actualGroups +
                "支中立编队；" + (inhabited ? "有人/60" : "无人/90") +
                " FP；实有自动分 " + Math.round(actualFp),
                system.getId(), leader.getId());
    }

    private static void makeTrapNeutral(CampaignFleetAPI fleet) {
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);
        fleet.addTag(Tags.SALVAGE_ENTITY_NO_DEBRIS);
    }

    private static void spawnUnreachableObserver(CampaignFleetAPI player) {
        CampaignFleetAPI fleet = createFleet(player.getContainingLocation(), around(player, 3600f, 5200f), 5f + RANDOM.nextFloat() * 9f,
                PTSD_BaseShard_Util.FleetRole.RECON, PTSD_BaseShard_Util.BRANCH_TRAN);
        if (fleet == null) return;
        fleet.setName("无法建立接触的侦察回波");
        makeTrapNeutral(fleet);
        fleet.setSensorProfile(45f);
        fleet.getStats().getFleetwideMaxBurnMod().modifyFlat("PTSD_unreachable_observer", 8f, "异常距离控制");
        fleet.addScript(new DistanceControlScript(fleet, player, 12f));
        PTSDCrisisDevIntel.report("每日事件：近距不可接触侦察", "介入灵质；主动维持接触距离", player.getStarSystem() == null ? null : player.getStarSystem().getId(), fleet.getId());
    }

    private static void spawnRouteWrecks(CampaignFleetAPI player) {
        LocationAPI location = player.getContainingLocation();
        if (location == null) return;
        Vector2f direction = new Vector2f(player.getVelocity());
        if (direction.lengthSquared() < 1f) direction = Misc.getUnitVectorAtDegreeAngle(player.getFacing());
        else direction.normalise();
        direction.scale(3200f + RANDOM.nextFloat() * 3200f);
        Vector2f center = Vector2f.add(player.getLocation(), direction, null);
        String faction = pickNearbyFaction(player);
        int count = 1 + RANDOM.nextInt(2);
        int made = 0;
        SectorEntityToken investigationAnchor = null;
        for (int i = 0; i < count; i++) {
            try {
                Vector2f off = Misc.getUnitVectorAtDegreeAngle(RANDOM.nextFloat() * 360f);
                off.scale(250f + RANDOM.nextFloat() * 1400f);
                Vector2f p = Vector2f.add(center, off, null);
                p = PTSDCrisisAPI.findSafePoint(location, p, 200f, 1800f, RANDOM);
                if (p == null) continue;
                DerelictShipEntityPlugin.DerelictShipData data = DerelictShipEntityPlugin.createRandom(faction, null, RANDOM, 0f);
                data.durationDays = 30f;
                SectorEntityToken wreck = BaseThemeGenerator.addSalvageEntity(RANDOM, location, Entities.WRECK, faction, data);
                wreck.setLocation(p.x, p.y);
                wreck.setName("? ? ?");
                // These are scenery from a violent interception, not a reliable source of free
                // recoverable hulls. A small minority remains repairable for exploration value.
                if (RANDOM.nextFloat() < .85f) wreck.addTag(Tags.UNRECOVERABLE);
                if (investigationAnchor == null) investigationAnchor = wreck;
                made++;
            } catch (Throwable ex) { Global.getLogger(PTSDReconPlayerEvents.class).warn("Unable to create route wreck", ex); }
        }
        int debrisMade = 0;
        int debrisCount = 2 + RANDOM.nextInt(3);
        for (int i = 0; i < debrisCount; i++) {
            try {
                Vector2f off = Misc.getUnitVectorAtDegreeAngle(RANDOM.nextFloat() * 360f);
                off.scale(180f + RANDOM.nextFloat() * 1900f);
                Vector2f p = PTSDCrisisAPI.findSafePoint(location,
                        Vector2f.add(center, off, null), 180f, 1600f, RANDOM);
                if (p == null) continue;
                DebrisFieldParams params = new DebrisFieldParams(
                        90f + RANDOM.nextFloat() * 100f, -1f, 30f, .12f + RANDOM.nextFloat() * .23f);
                params.source = DebrisFieldSource.BATTLE;
                params.baseSalvageXP = 5 + RANDOM.nextInt(16);
                SectorEntityToken debris = Misc.addDebrisField(location, params, RANDOM);
                debris.setLocation(p.x, p.y);
                debris.setName("正在散逸的航路残骸区");
                if (investigationAnchor == null) investigationAnchor = debris;
                debrisMade++;
            } catch (Throwable ex) { Global.getLogger(PTSDReconPlayerEvents.class).warn("Unable to create route debris", ex); }
        }
        boolean scoutSpawned = investigationAnchor != null && RANDOM.nextFloat() < .7f &&
                spawnRouteWreckScout(location, investigationAnchor, player);
        PTSDCrisisDevIntel.report("每日事件：航路残骸群", made + " 艘残舰 / " + debrisMade +
                        " 片小型临时残骸区；势力 " + faction + "；调查侦察队 " +
                        (scoutSpawned ? "已出现" : "未出现") + "；持续30日",
                player.getStarSystem() == null ? null : player.getStarSystem().getId(), null);
    }

    private static boolean spawnRouteWreckScout(LocationAPI location, SectorEntityToken anchor,
                                                CampaignFleetAPI player) {
        Vector2f spawn = PTSDCrisisAPI.findSafePoint(location, anchor, 900f, 1900f, RANDOM);
        CampaignFleetAPI scout = createFleet(location, spawn, 7f + RANDOM.nextFloat() * 11f,
                PTSD_BaseShard_Util.FleetRole.RECON, PTSD_BaseShard_Util.BRANCH_TRAN);
        if (scout == null) return false;
        scout.setName("未知舰队");
        makeTrapNeutral(scout);
        scout.setSensorProfile(35f);
        scout.addAssignment(FleetAssignment.ORBIT_PASSIVE, anchor, 7f + RANDOM.nextFloat() * 8f,
                "逐段扫描散逸残骸");
        scout.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, escapeToken(player), 20f,
                "结束取样并远离航路");
        PTSDOmegaEncounterResponse.attachSurveyScout(scout);
        return true;
    }

    private static String pickNearbyFaction(CampaignFleetAPI player) {
        List<String> ids = new ArrayList<String>();
        if (player.getContainingLocation() != null) for (CampaignFleetAPI fleet : player.getContainingLocation().getFleets()) {
            if (fleet == null || fleet == player || WATCHER.equals(fleet.getFaction().getId()) || IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(fleet.getFaction().getId())) continue;
            String id = fleet.getFaction().getId(); if (id != null && !ids.contains(id)) ids.add(id);
        }
        return ids.isEmpty() ? Factions.INDEPENDENT : ids.get(RANDOM.nextInt(ids.size()));
    }

    private static void spawnReactivePursuer(CampaignFleetAPI player) {
        float fp = Math.max(15f, player.getFleetPoints() * (.35f + RANDOM.nextFloat() * .35f));
        CampaignFleetAPI fleet = createFleet(player.getContainingLocation(), around(player, 4200f, 7000f), fp,
                PTSD_BaseShard_Util.FleetRole.RECON, null);
        if (fleet == null) return;
        fleet.setName("正在截获航迹的未知舰队");
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
        fleet.getMemoryWithoutUpdate().set(REACTIVE_RETREAT, true);
        if (fleet.getAbility(Abilities.INTERDICTION_PULSE) == null) fleet.addAbility(Abilities.INTERDICTION_PULSE);
        if (fleet.getAbility(Abilities.SENSOR_BURST) == null) fleet.addAbility(Abilities.SENSOR_BURST);
        if (fleet.getAbility(Abilities.EMERGENCY_BURN) == null) fleet.addAbility(Abilities.EMERGENCY_BURN);
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, player, 5f, "接近并扫描未知航迹");
        fleet.addScript(new ReactivePulseScript(fleet, player));
        PTSDCrisisDevIntel.report("每日事件：超空间试触追击",
                "接近后发动原版阻断脉冲和主动扫描，再加力脱离；本队实有 " +
                        Math.round(fleet.getFleetPoints()) + " FP", null, fleet.getId());
    }

    private static final class ReactivePulseScript implements EveryFrameScript {
        private final CampaignFleetAPI fleet, player;
        private int stage;
        private float elapsed;
        private float observedFp;
        private String observedSystemId;
        private SectorEntityToken exit;
        private boolean scanActivated;
        ReactivePulseScript(CampaignFleetAPI fleet, CampaignFleetAPI player) {
            this.fleet = fleet;
            this.player = player;
        }
        public boolean isDone() { return fleet == null || !fleet.isAlive() || fleet.isDespawning() || stage == 3; }
        public boolean runWhilePaused() { return false; }
        public void advance(float amount) {
            if (isDone() || Global.getSector() == null || fleet.getBattle() != null) return;
            float days = Global.getSector().getClock().convertToDays(amount);
            elapsed += days;
            if (fleet.getMemoryWithoutUpdate().getBoolean(RETREAT_TRIGGERED) && stage < 2) beginEscape();
            if (fleet.getContainingLocation() != player.getContainingLocation()) {
                if (stage == 2) upload();
                fleet.despawn();
                stage = 3;
                return;
            }
            if (stage == 0) {
                if (elapsed > 4f) { beginEscape(); return; }
                if (Misc.getDistance(fleet, player) >
                        com.fs.starfarer.api.impl.campaign.abilities.InterdictionPulseAbility.getRange(fleet) * .8f)
                    return;
                com.fs.starfarer.api.characters.AbilityPlugin pulse =
                        fleet.getAbility(Abilities.INTERDICTION_PULSE);
                if (pulse != null && pulse.isUsable()) {
                    pulse.activate();
                    stage = 1;
                    elapsed = 0f;
                } else {
                    beginScan();
                }
            } else if (stage == 1) {
                // The vanilla ability applies its own range, hit and cooldown rules.
                if (fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.JUST_DID_INTERDICTION_PULSE) ||
                        elapsed > .15f) beginScan();
            } else if (stage == 4) {
                tryScan();
            } else if (stage == 2) {
                tryEmergencyBurn();
                if (!fleet.isVisibleToPlayerFleet()) {
                    upload();
                    fleet.despawn();
                    stage = 3;
                } else if (exit == null || Misc.getDistance(fleet, exit) < 1800f) {
                    exit = escapeToken(player);
                    fleet.clearAssignments();
                    fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, exit, 4f, "加力脱离扫描区");
                }
            }
        }
        private void beginScan() {
            stage = 4;
            elapsed = 0f;
            tryScan();
        }
        private void tryScan() {
            com.fs.starfarer.api.characters.AbilityPlugin scanner = fleet.getAbility(Abilities.SENSOR_BURST);
            if (scanner != null && scanner.isUsable()) {
                scanner.activate();
                scanActivated = true;
            }
            if (!scanActivated && elapsed < .5f) return;
            if (scanActivated && Misc.getDistance(fleet, player) <= 6500f) {
                observedFp = player.getFleetPoints();
                if (player.getStarSystem() != null) observedSystemId = player.getStarSystem().getId();
                else observedSystemId = nearestSystemTo(player);
            }
            beginEscape();
        }
        private void beginEscape() {
            stage = 2;
            elapsed = 0f;
            fleet.getMemoryWithoutUpdate().set(RETREAT_TRIGGERED, true);
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, false);
            fleet.clearAssignments();
            exit = escapeToken(player);
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, exit, 8f, "紧急加力撤离");
            tryEmergencyBurn();
        }
        private void tryEmergencyBurn() {
            com.fs.starfarer.api.characters.AbilityPlugin burn = fleet.getAbility(Abilities.EMERGENCY_BURN);
            if (burn != null && burn.isUsable() && !burn.isActive()) burn.activate();
        }
        private String nearestSystemTo(CampaignFleetAPI observed) {
            StarSystemAPI nearest = null;
            float best = Float.MAX_VALUE;
            for (StarSystemAPI candidate : Global.getSector().getStarSystems()) {
                if (candidate == null) continue;
                float distance = Misc.getDistance(candidate.getLocation(), observed.getLocationInHyperspace());
                if (distance < best) { best = distance; nearest = candidate; }
            }
            return nearest == null ? null : nearest.getId();
        }
        private void upload() {
            if (observedFp <= 0f || observedSystemId == null) return;
            PTSDThreatDirector.recordAction(PTSDCrisisState.get(), "FRONT",
                    "reactive-scan:" + fleet.getId(), observedSystemId,
                    Math.min(100f, observedFp / 5f), .65f, true, 35f);
            observedFp = 0f;
        }
    }

    private static SectorEntityToken escapeToken(SectorEntityToken from) {
        Vector2f p = new Vector2f(from.getLocation());
        Vector2f off = Misc.getUnitVectorAtDegreeAngle(RANDOM.nextFloat() * 360f); off.scale(18000f); Vector2f.add(p, off, p);
        return from.getContainingLocation().createToken(p);
    }

    private static SectorEntityToken pickSystemFocus(StarSystemAPI system) {
        for (PlanetAPI p : system.getPlanets()) if (p != null && !p.isStar()) return p;
        for (SectorEntityToken p : system.getJumpPoints()) if (p != null) return p;
        return system.getCenter();
    }

    private static Vector2f safeAround(SectorEntityToken focus, float min, float max) { return around(focus, min, max); }

    private static final class DistanceControlScript implements EveryFrameScript {
        private final CampaignFleetAPI fleet, player; private float days, checkElapsed;
        DistanceControlScript(CampaignFleetAPI fleet, CampaignFleetAPI player, float days) { this.fleet=fleet; this.player=player; this.days=days; }
        public boolean isDone() { return fleet == null || !fleet.isAlive() || days <= 0f; }
        public boolean runWhilePaused() { return false; }
        public void advance(float amount) {
            if (isDone() || Global.getSector() == null) return;
            float d = Global.getSector().getClock().convertToDays(amount); days -= d; checkElapsed += d;
            if (fleet.getContainingLocation() != player.getContainingLocation()) { fleet.despawn(); return; }
            float range = Misc.getDistance(fleet, player);
            if (range < 1200f) {
                Vector2f away = Vector2f.sub(fleet.getLocation(), player.getLocation(), null);
                if (away.lengthSquared() < 1f) away = Misc.getUnitVectorAtDegreeAngle(RANDOM.nextFloat() * 360f);
                else away.normalise();
                away.scale(3000f);
                Vector2f corrected = Vector2f.add(player.getLocation(), away, null);
                fleet.setLocation(corrected.x, corrected.y);
                range = 3000f;
            }
            if (checkElapsed < .04f) return;
            checkElapsed = 0f;
            fleet.clearAssignments();
            if (range < 2600f) fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, escapeToken(player), .4f, "回避接触");
            else if (range > 5200f) fleet.addAssignment(FleetAssignment.FOLLOW, player, .4f, "维持遥测距离");
            else fleet.addAssignment(FleetAssignment.HOLD, fleet, .4f, "保持距离");
            if (days <= 0f) { fleet.clearAssignments(); fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, escapeToken(player), 10f, "信号消失"); }
        }
    }

    public static void devTrigger(String id) {
        PTSDCrisisState state = PTSDCrisisState.get(); CampaignFleetAPI player = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        if (state == null || player == null) return;
        if ("MATCHED".equals(id)) spawnMatchedManeuver(player);
        else if ("TRAP_ARM".equals(id)) armTrapFromNavigation(state, player, PTSDCrisisState.getDay());
        else if ("TRAP_NOW".equals(id) && player.getStarSystem() != null) spawnTrapFormation(player.getStarSystem(), player);
        else if ("OBSERVER".equals(id)) spawnUnreachableObserver(player);
        else if ("WRECKS".equals(id)) spawnRouteWrecks(player);
        else if ("PURSUER".equals(id)) spawnReactivePursuer(player);
    }
}
