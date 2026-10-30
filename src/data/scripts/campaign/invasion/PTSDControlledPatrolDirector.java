package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.listeners.BaseFleetEventListener;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import data.hullmods.shard.PTSD_BaseShard_Util;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Free physical projections of saved, per-system defense slots. Fortress escorts are separate. */
public final class PTSDControlledPatrolDirector {
    private static final String TAG = "PTSD_controlled_system_patrol";
    private static final Random RANDOM = new Random();
    private PTSDControlledPatrolDirector() { }

    private static final class Ceiling {
        final int fleets, ships;
        final float totalFp, singleFp;
        final ShipAPI.HullSize hull;
        Ceiling(int fleets, float totalFp, float singleFp, int ships, ShipAPI.HullSize hull) {
            this.fleets = fleets; this.totalFp = totalFp; this.singleFp = singleFp;
            this.ships = ships; this.hull = hull;
        }
    }

    private static Ceiling ceiling(PTSDCrisisState.Phase phase) {
        if (phase == PTSDCrisisState.Phase.RECON)
            return new Ceiling(2, 100f, 60f, 3, ShipAPI.HullSize.DESTROYER);
        if (phase == PTSDCrisisState.Phase.EXPANSION ||
                phase == PTSDCrisisState.Phase.FORTIFICATION)
            return new Ceiling(4, 600f, 200f, 8, ShipAPI.HullSize.CRUISER);
        return new Ceiling(10, 1000f, 800f, 12, ShipAPI.HullSize.CAPITAL_SHIP);
    }

    public static void advance(PTSDCrisisState state, float day) {
        if (state == null || state.late == null || Global.getSector() == null ||
                state.phase == PTSDCrisisState.Phase.DORMANT ||
                state.phase == PTSDCrisisState.Phase.ENDED) return;
        state.ensureRuntimeReady();
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        StarSystemAPI current = player == null ? null : player.getStarSystem();
        for (PTSDCrisisState.SystemData data : state.systems.values()) {
            if (data == null || data.omegaLocalAlert <= 0f) continue;
            if (data.omegaLocalAlertDay > 0f)
                data.omegaLocalAlert = Math.max(0f,
                        data.omegaLocalAlert - Math.max(0f, day - data.omegaLocalAlertDay) / 30f);
            data.omegaLocalAlertDay = day;
        }
        Ceiling limit = ceiling(state.phase);
        if (current != null && state.getSystemData(current.getId()).omegaControl >= .5f) {
            state.late.controlledPatrolAwayDay.remove(current.getId());
            float each = Math.min(limit.singleFp, limit.totalFp / limit.fleets);
            List<SectorEntityToken> guarded = occupiedPlanets(current);
            for (int index = 0; index < limit.fleets + guarded.size(); index++) {
                SectorEntityToken planet = index < limit.fleets ? null : guarded.get(index - limit.fleets);
                String id = planet == null ? current.getId() + ":" + index :
                        current.getId() + ":planet:" + planet.getId();
                PTSDLateState.ControlledPatrol slot = state.late.controlledPatrols.get(id);
                if (slot == null) {
                    slot = new PTSDLateState.ControlledPatrol();
                    slot.id = id; slot.systemId = current.getId(); slot.index = index;
                    slot.waypointIndex = index;
                    state.late.controlledPatrols.put(id, slot);
                }
                slot.guardedPlanetId = planet == null ? null : planet.getId();
                float previousNominal = slot.nominalFp;
                slot.nominalFp = each;
                // A stage increase may upgrade an intact slot, but leaving and re-entering
                // must never repair a damaged fleet before its actual replenishment day.
                if (slot.fleetId == null && day >= slot.replenishDay) {
                    if (slot.survivingFp <= 0f || slot.replenishDay > 0f ||
                            Math.abs(slot.survivingFp - previousNominal) < .01f)
                        slot.survivingFp = each;
                    slot.replenishDay = 0f;
                }
                CampaignFleetAPI fleet = fleet(slot.fleetId);
                if (fleet == null && slot.fleetId != null) {
                    // An unverified disappearance is never silently restored by re-entering.
                    slot.fleetId = null;
                    slot.survivingFp = 0f;
                    slot.replenishDay = Math.max(slot.replenishDay, day + 30f);
                    PTSDCrisisDevIntel.report("实控巡逻对象丢失",
                            id + "：等待30日复核，未凭空结算战果", slot.systemId, null);
                }
                if (fleet == null && slot.survivingFp >= 8f)
                    fleet = spawn(state, current, slot, limit);
                if (fleet != null && fleet.getBattle() == null)
                    route(state, current, slot, fleet, day);
            }
        }
        for (PTSDLateState.ControlledPatrol slot :
                new ArrayList<PTSDLateState.ControlledPatrol>(state.late.controlledPatrols.values())) {
            if (slot == null || slot.systemId == null) continue;
            if (current != null && slot.systemId.equals(current.getId()) &&
                    (slot.guardedPlanetId != null ? isStillOccupied(current, slot.guardedPlanetId) :
                            slot.index < limit.fleets) &&
                    state.getSystemData(slot.systemId).omegaControl >= .5f) continue;
            Float away = state.late.controlledPatrolAwayDay.get(slot.systemId);
            if (away == null) {
                state.late.controlledPatrolAwayDay.put(slot.systemId, day);
                continue;
            }
            if (day - away < 8f) continue;
            CampaignFleetAPI fleet = fleet(slot.fleetId);
            if (fleet == null || fleet.getBattle() != null) continue;
            slot.survivingFp = Math.min(slot.nominalFp, Math.max(0f, fleet.getFleetPoints()));
            { data.scripts.campaign.invasion.PTSDCampaignRuntime.leaving(fleet); fleet.despawn(FleetDespawnReason.OTHER, null); }
            slot.fleetId = null;
            PTSDCrisisDevIntel.report("实控巡逻卸载",
                    slot.id + "：离开8日，保存 " + Math.round(slot.survivingFp) + " FP",
                    slot.systemId, null);
        }
    }

    private static CampaignFleetAPI spawn(PTSDCrisisState state, StarSystemAPI system,
                                           PTSDLateState.ControlledPatrol slot, Ceiling limit) {
        List<SectorEntityToken> anchors = patrolAnchors(system);
        SectorEntityToken anchor = slot.guardedPlanetId == null ?
                (anchors.isEmpty() ? null : anchors.get(Math.floorMod(slot.index, anchors.size()))) :
                system.getEntityById(slot.guardedPlanetId);
        if (anchor == null) return null;
        Vector2f point = PTSDCrisisAPI.findSafePoint(system, anchor, 300f, 700f, RANDOM);
        if (point == null) return null;
        float budget = Math.min(limit.singleFp, Math.max(8f, slot.survivingFp));
        FleetParamsV3 params = new FleetParamsV3(point,
                PTSDCrisisProgress.getActiveFactionId(state), .5f, FleetTypes.PATROL_LARGE,
                budget, 0f, 0f, 0f, 0f, 0f, 0f);
        params.maxNumShips = limit.ships;
        CampaignFleetAPI fleet = PTSD_BaseShard_Util.createFleet(params, budget,
                PTSD_BaseShard_Util.FleetRole.GUARD_ASSAULT, RANDOM,
                new PTSD_BaseShard_Util.FleetLimits(budget, limit.ships, limit.hull));
        if (fleet == null || fleet.getFleetPoints() > budget + .01f) return null;
        { system.addEntity(fleet); data.scripts.campaign.invasion.PTSDCampaignRuntime.entityAdded(fleet); }
        fleet.setLocation(point.x, point.y);
        fleet.setName(state.phase == PTSDCrisisState.Phase.RECON ?
                "未知编队" : slot.guardedPlanetId == null ? "精神创伤巡弋单元" : "精神创伤近卫单元");
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(false);
        fleet.addTag(TAG);
        fleet.addTag("PTSD_crisis_fleet");
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        fleet.getEventListeners().add(new PTSDStrategicFleetListener(null, fleet.getFleetPoints()));
        fleet.getEventListeners().add(new LossListener(slot.id));
        slot.fleetId = fleet.getId();
        slot.survivingFp = fleet.getFleetPoints();
        slot.nextWaypointDay = 0f;
        PTSDCrisisDevIntel.report("实控巡逻实体化",
                slot.id + " / " + Math.round(slot.survivingFp) + " FP / 免费投影",
                system.getId(), fleet.getId());
        return fleet;
    }

    private static void route(PTSDCrisisState state, StarSystemAPI system,
                              PTSDLateState.ControlledPatrol slot,
                              CampaignFleetAPI fleet, float day) {
        // Let the current trip finish. Reissuing every few days made distant nodes unreachable.
        if (fleet.getAI() != null && fleet.getAI().getCurrentAssignment() != null) return;
        if (slot.guardedPlanetId != null) {
            SectorEntityToken planet = system.getEntityById(slot.guardedPlanetId);
            if (planet == null || !isStillOccupied(system, slot.guardedPlanetId)) return;
            fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, planet, 2f, "在占领区近旁警戒");
            return;
        }
        List<SectorEntityToken> anchors = patrolAnchors(system);
        if (anchors.isEmpty()) return;
        int index = Math.floorMod(slot.waypointIndex++, anchors.size());
        SectorEntityToken anchor = anchors.get(index);
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, anchor, 60f, "巡查星球、设施与跃迁航路");
        fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, anchor, .4f, "检查航路节点");
    }

    static List<SectorEntityToken> patrolAnchors(StarSystemAPI system) {
        List<SectorEntityToken> anchors = new ArrayList<SectorEntityToken>();
        anchors.addAll(system.getJumpPoints());
        for (SectorEntityToken planet : system.getPlanets())
            if (planet != null && !planet.isStar()) anchors.add(planet);
        for (SectorEntityToken entity : system.getAllEntities()) {
            if (entity == null || anchors.contains(entity)) continue;
            if (entity.hasTag(Tags.STATION) || entity.hasTag(Tags.COMM_RELAY) ||
                    entity.hasTag(Tags.SENSOR_ARRAY) || entity.hasTag(Tags.NAV_BUOY) ||
                    (entity instanceof CampaignFleetAPI && ((CampaignFleetAPI) entity).isStationMode()))
                anchors.add(entity);
        }
        return anchors;
    }

    /** Existing fortress escorts retain transit orders; only free system roaming is replaced. */
    public static void installNodePatrol(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getMemoryWithoutUpdate().getBoolean("$PTSD_node_patrol")) return;
        fleet.getMemoryWithoutUpdate().set("$PTSD_node_patrol", true);
        fleet.addScript(new NodePatrol(fleet));
    }

    private static final class NodePatrol implements com.fs.starfarer.api.EveryFrameScript {
        private final CampaignFleetAPI fleet;
        private int next;
        private float elapsed;
        NodePatrol(CampaignFleetAPI fleet) { this.fleet = fleet; }
        public boolean isDone() { return fleet == null || !fleet.isAlive(); }
        public boolean runWhilePaused() { return false; }
        public void advance(float amount) {
            elapsed += Global.getSector().getClock().convertToDays(amount);
            if (elapsed < .1f || isDone()) return;
            elapsed = 0f;
            if (fleet.getBattle() != null || fleet.getStarSystem() == null) return;
            com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI order = fleet.getCurrentAssignment();
            if (order != null && order.getAssignment() != FleetAssignment.PATROL_SYSTEM) return;
            List<SectorEntityToken> anchors = patrolAnchors(fleet.getStarSystem());
            if (anchors.isEmpty()) return;
            if (order != null) fleet.clearAssignments();
            SectorEntityToken anchor = anchors.get(Math.floorMod(next++, anchors.size()));
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, anchor, 60f, "沿设施与跃迁航路巡查");
            fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, anchor, .4f, "检查航路节点");
        }
    }

    private static List<SectorEntityToken> occupiedPlanets(StarSystemAPI system) {
        List<SectorEntityToken> result = new ArrayList<SectorEntityToken>();
        for (SectorEntityToken planet : system.getPlanets()) {
            if (planet == null || planet.isStar() || planet.getFaction() == null) continue;
            String faction = planet.getFaction().getId();
            if (IIRT_Omega_Invasion.WATCHER_FACTION.equals(faction) ||
                    IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(faction)) result.add(planet);
        }
        return result;
    }

    private static boolean isStillOccupied(StarSystemAPI system, String planetId) {
        for (SectorEntityToken planet : occupiedPlanets(system))
            if (planetId.equals(planet.getId())) return true;
        return false;
    }

    private static CampaignFleetAPI fleet(String id) {
        if (id == null || Global.getSector() == null) return null;
        Object found = Global.getSector().getEntityById(id);
        return found instanceof CampaignFleetAPI ? (CampaignFleetAPI) found : null;
    }

    private static final class LossListener extends BaseFleetEventListener {
        private final String slotId;
        LossListener(String slotId) { this.slotId = slotId; }
        @Override public void reportBattleOccurred(CampaignFleetAPI fleet,
                CampaignFleetAPI primaryWinner, BattleAPI battle) {
            if (fleet == null || battle == null || !battle.isDone()) return;
            PTSDCrisisState state = PTSDCrisisState.get();
            if (state == null || state.late == null) return;
            PTSDLateState.ControlledPatrol slot = state.late.controlledPatrols.get(slotId);
            if (slot == null) return;
            boolean defeated = battle.wasFleetDefeated(fleet, primaryWinner);
            if (!defeated) {
                slot.survivingFp = Math.min(slot.nominalFp, Math.max(0f, fleet.getFleetPoints()));
                if (slot.survivingFp < slot.nominalFp)
                    slot.replenishDay = Math.max(slot.replenishDay, PTSDCrisisState.getDay() + 30f);
                return;
            }
            if (!state.late.committedIds.add("controlled-patrol-loss:" + fleet.getId())) return;
            float defeatedFp = Math.max(slot.survivingFp, Math.max(0f, fleet.getFleetPoints()));
            slot.fleetId = null;
            slot.survivingFp = 0f;
            slot.replenishDay = PTSDCrisisState.getDay() + 30f;
            float cost = Math.min(20f, Math.max(1f, (float) Math.ceil(defeatedFp / 50f)));
            state.late.strategicReserve = Math.max(0f, state.late.strategicReserve - cost);
            PTSDLateWarDirector.recordSpending(state, "实控巡逻战损 " + slotId, 0f, cost);
            PTSDCrisisState.SystemData local = state.getSystemData(slot.systemId);
            local.omegaLocalAlert = Math.min(1f, local.omegaLocalAlert + .2f);
            local.omegaLocalAlertDay = PTSDCrisisState.getDay();
            PTSDCrisisDevIntel.report("实控巡逻战损",
                    slotId + "：战略资产-" + cost + "，30日补员，当地警戒+" + .2f,
                    slot.systemId, fleet.getId());
        }
    }
}
