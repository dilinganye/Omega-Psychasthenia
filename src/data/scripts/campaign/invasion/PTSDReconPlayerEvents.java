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
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
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
        updateReactiveRetreatFleets(player);
    }

    private static void rollDaily(PTSDCrisisState state, CampaignFleetAPI player, float day) {
        if (RANDOM.nextFloat() < .01f) spawnMatchedManeuver(player);
        if (RANDOM.nextFloat() < .02f) armTrapFromNavigation(state, player, day);
        if (RANDOM.nextFloat() < .03f) spawnUnreachableObserver(player);
        if (RANDOM.nextFloat() < .05f) spawnRouteWrecks(player);
        if (player.isInHyperspace() && RANDOM.nextFloat() < .02f) spawnReactivePursuer(player);
    }

    private static CampaignFleetAPI createFleet(LocationAPI location, Vector2f point, float fp,
                                                  PTSD_BaseShard_Util.FleetRole role, String explicitBranch) {
        if (location == null || point == null) return null;
        FleetParamsV3 params = new FleetParamsV3(point, WATCHER, .5f, FleetTypes.PATROL_LARGE,
                fp, 0f, 0f, 0f, 0f, 0f, 0f);
        params.maxNumShips = Global.getSettings().getMaxShipsInFleet();
        PTSD_BaseShard_Util.BranchDefinition branch;
        if (explicitBranch == null) {
            branch = PTSD_BaseShard_Util.prepareFleetParams(params, fp, role, RANDOM);
        } else {
            branch = PTSD_BaseShard_Util.prepareFleetParams(params, fp, explicitBranch, RANDOM);
        }
        if (branch == null || params.addShips == null || params.addShips.isEmpty()) return null;
        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
        if (fleet == null) return null;
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
        Vector2f result = new Vector2f(target.getLocation());
        Vector2f offset = Misc.getUnitVectorAtDegreeAngle(RANDOM.nextFloat() * 360f);
        offset.scale(min + RANDOM.nextFloat() * Math.max(1f, max - min));
        return Vector2f.add(result, offset, result);
    }

    private static void spawnMatchedManeuver(CampaignFleetAPI player) {
        float fp = Math.max(7f, player.getFleetPoints());
        CampaignFleetAPI fleet = createFleet(player.getContainingLocation(), around(player, 7000f, 11000f), fp,
                PTSD_BaseShard_Util.FleetRole.RECON, null);
        if (fleet == null) return;
        fleet.setName("正在修正航向的未知舰队");
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        fleet.addAssignment(FleetAssignment.INTERCEPT, player, 20f, "以未知参数逼近目标");
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, escapeToken(player), 30f, "脱离观测区");
        PTSDCrisisDevIntel.report("每日事件：等强机动舰队", "目标玩家；基准自动分 " + Math.round(fp) + "；分支 " + PTSD_BaseShard_Util.getFleetBranchName(fleet),
                player.getStarSystem() == null ? null : player.getStarSystem().getId(), fleet.getId());
    }

    private static void armTrapFromNavigation(PTSDCrisisState state, CampaignFleetAPI player, float day) {
        if (state.reconTrapSystemId != null && day < state.reconTrapExpiresDay) return;
        CampaignFleetAIAPI ai = player.getAI();
        FleetAssignmentDataAPI assignment = ai == null ? null : ai.getCurrentAssignment();
        SectorEntityToken target = assignment == null ? null : assignment.getTarget();
        StarSystemAPI system = target == null ? null : target.getStarSystem();
        if (!validTrapSystem(system, player)) return;
        state.reconTrapSystemId = system.getId();
        state.reconTrapTargetEntityId = target.getId();
        state.reconTrapExpiresDay = day + 45f;
        PTSDCrisisDevIntel.report("每日事件：陷阱判定已埋设", "等待玩家跨星系进入导航目标；45日后失效", system.getId(), target.getId());
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
        CampaignFleetAPI leader = createFleet(system, safeAround(focus, 5000f, 8500f), 500f,
                PTSD_BaseShard_Util.FleetRole.GUARD_ASSAULT, PTSD_BaseShard_Util.BRANCH_WEB);
        if (leader == null) return;
        leader.setName("静默的网络冥魂领队");
        makeTrapNeutral(leader);
        leader.addAssignment(FleetAssignment.PATROL_SYSTEM, system.getCenter(), 30f, "在星系内保持静默机动");
        CampaignFleetAPI transport = createFleet(system, safeAround(leader, 600f, 1200f), 300f,
                PTSD_BaseShard_Util.FleetRole.LOGISTICS_ENGINEERING, PTSD_BaseShard_Util.BRANCH_CUBE);
        if (transport != null) {
            transport.setName("静默的熵级运载编队"); makeTrapNeutral(transport);
            transport.addAssignment(FleetAssignment.FOLLOW, leader, 30f, "跟随领队舰");
        }
        for (int i = 0; i < 3; i++) {
            CampaignFleetAPI escort = createFleet(system, safeAround(leader, 1300f, 4200f), 110f + RANDOM.nextFloat() * 150f,
                    PTSD_BaseShard_Util.FleetRole.GUARD_ASSAULT, null);
            if (escort == null) continue;
            escort.setName("静默的漫游编队"); makeTrapNeutral(escort);
            escort.addAssignment(i == 0 ? FleetAssignment.FOLLOW : FleetAssignment.PATROL_SYSTEM,
                    i == 0 ? leader : system.getCenter(), 30f, "在异常信号间漫游");
        }
        Global.getSector().getCampaignUI().addMessage("传感器上出现了极端的能量反应", new Color(255, 105, 125));
        try { Global.getSector().addPing(leader, "sensor_burst", new Color(220, 90, 255)); } catch (Throwable ignored) { }
        PTSDCrisisDevIntel.report("每日事件：陷阱舰群显现", "五支中立编队；网络冥魂500 FP领队；熵级运载300 FP跟随",
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
        PTSDCrisisDevIntel.report("每日事件：近距不可接触侦察", "介入灵质观察单元；主动维持接触距离", player.getStarSystem() == null ? null : player.getStarSystem().getId(), fleet.getId());
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
        int count = 6 + RANDOM.nextInt(7);
        int made = 0;
        for (int i = 0; i < count; i++) {
            try {
                DerelictShipEntityPlugin.DerelictShipData data = DerelictShipEntityPlugin.createRandom(faction, null, RANDOM, 0f);
                data.durationDays = 30f;
                SectorEntityToken wreck = BaseThemeGenerator.addSalvageEntity(RANDOM, location, Entities.WRECK, faction, data);
                Vector2f off = Misc.getUnitVectorAtDegreeAngle(RANDOM.nextFloat() * 360f);
                off.scale(250f + RANDOM.nextFloat() * 1400f);
                Vector2f p = Vector2f.add(center, off, null);
                wreck.setLocation(p.x, p.y);
                wreck.setName("近期战损舰体");
                made++;
            } catch (Throwable ex) { Global.getLogger(PTSDReconPlayerEvents.class).warn("Unable to create route wreck", ex); }
        }
        PTSDCrisisDevIntel.report("每日事件：航路残骸群", made + " 艘；势力 " + faction + "；持续30日",
                player.getStarSystem() == null ? null : player.getStarSystem().getId(), null);
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
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        fleet.getMemoryWithoutUpdate().set(REACTIVE_RETREAT, true);
        fleet.addAssignment(FleetAssignment.INTERCEPT, player, 20f, "立即追击异常航迹");
        fleet.addScript(new PostBattleEscapeScript(fleet, player));
        PTSDCrisisDevIntel.report("每日事件：超空间试触追击", "受击后全舰撤退；返回生涯后高速脱离；FP " + Math.round(fp), null, fleet.getId());
    }

    private static void updateReactiveRetreatFleets(CampaignFleetAPI player) {
        LocationAPI location = player.getContainingLocation(); if (location == null) return;
        for (CampaignFleetAPI fleet : location.getFleets()) {
            if (fleet != null && fleet.getMemoryWithoutUpdate().getBoolean(RETREAT_TRIGGERED) && !fleet.isDespawning()) {
                fleet.clearAssignments();
                fleet.getStats().getFleetwideMaxBurnMod().modifyFlat("PTSD_reactive_escape", 8f, "受击后紧急撤离");
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
                fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, escapeToken(player), 20f, "紧急脱离接触");
            }
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
            else fleet.addAssignment(FleetAssignment.HOLD, fleet, .4f, "保持不可接触距离");
            if (days <= 0f) { fleet.clearAssignments(); fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, escapeToken(player), 10f, "信号消失"); }
        }
    }

    private static final class PostBattleEscapeScript implements EveryFrameScript {
        private final CampaignFleetAPI fleet, player;
        PostBattleEscapeScript(CampaignFleetAPI fleet, CampaignFleetAPI player) { this.fleet=fleet; this.player=player; }
        public boolean isDone() { return fleet == null || !fleet.isAlive() || fleet.isDespawning(); }
        public boolean runWhilePaused() { return false; }
        public void advance(float amount) {
            if (isDone() || fleet.getBattle() != null || !fleet.getMemoryWithoutUpdate().getBoolean(RETREAT_TRIGGERED)) return;
            fleet.clearAssignments(); fleet.getStats().getFleetwideMaxBurnMod().modifyFlat("PTSD_reactive_escape", 8f, "受击后紧急撤离");
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, escapeToken(player), 20f, "紧急脱离接触");
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