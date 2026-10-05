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
import com.fs.starfarer.api.util.Misc;
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
        state.late.repair();
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
            for (int index = 0; index < limit.fleets; index++) {
                String id = current.getId() + ":" + index;
                PTSDLateState.ControlledPatrol slot = state.late.controlledPatrols.get(id);
                if (slot == null) {
                    slot = new PTSDLateState.ControlledPatrol();
                    slot.id = id; slot.systemId = current.getId(); slot.index = index;
                    state.late.controlledPatrols.put(id, slot);
                }
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
                    slot.index < limit.fleets &&
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
            fleet.despawn(FleetDespawnReason.OTHER, null);
            slot.fleetId = null;
            PTSDCrisisDevIntel.report("实控巡逻卸载",
                    slot.id + "：离开8日，保存 " + Math.round(slot.survivingFp) + " FP",
                    slot.systemId, null);
        }
    }

    private static CampaignFleetAPI spawn(PTSDCrisisState state, StarSystemAPI system,
                                           PTSDLateState.ControlledPatrol slot, Ceiling limit) {
        Vector2f point = PTSDCrisisAPI.findSafePoint(system, system.getCenter(),
                4500f, 12000f, RANDOM);
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
        system.addEntity(fleet);
        fleet.setLocation(point.x, point.y);
        fleet.setName(state.phase == PTSDCrisisState.Phase.RECON ?
                "未知编队" : "精神创伤巡弋单元");
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
        if (day < slot.nextWaypointDay && fleet.getAI() != null &&
                fleet.getAI().getCurrentAssignment() != null) return;
        List<SectorEntityToken> anchors = new ArrayList<SectorEntityToken>();
        anchors.addAll(system.getJumpPoints());
        for (SectorEntityToken planet : system.getPlanets())
            if (planet != null && !planet.isStar()) anchors.add(planet);
        anchors.addAll(system.getTerrainCopy());
        if (anchors.isEmpty()) anchors.add(system.getCenter());
        int index = Math.abs(slot.waypointIndex++ % anchors.size());
        SectorEntityToken anchor = anchors.get(index);
        Vector2f point = PTSDCrisisAPI.findSafePoint(system, anchor, 1800f, 4800f, RANDOM);
        if (point == null) point = Misc.getPointAtRadius(anchor.getLocation(), 3000f);
        SectorEntityToken waypoint = system.createToken(point);
        fleet.clearAssignments();
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, waypoint, 8f,
                "沿星系航道漫游巡查");
        float alert = state.getSystemData(system.getId()).omegaLocalAlert;
        slot.nextWaypointDay = day + (alert > .5f ? 2f : 4f);
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
