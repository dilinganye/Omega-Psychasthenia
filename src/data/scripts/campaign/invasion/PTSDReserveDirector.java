package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

/** A reserve has one ship roster in save state; the nearby fleet is only its physical projection. */
public final class PTSDReserveDirector {
    private PTSDReserveDirector() { }

    public static void advance(PTSDCrisisState state, float day) {
        if (state == null || state.late == null) return;
        for (PTSDLateState.Reserve reserve : state.late.reserves) {
            if (reserve == null || reserve.destroyed || !reserve.authorized) continue;
            CampaignFleetAPI live = PTSDFortressDirector.findFleet(reserve.fleetId);
            if (live != null) {
                if (live.getFleetData().getMembersListCopy().isEmpty()) {
                    reserve.destroyed = true;
                    reserve.fleetId = null;
                }
                continue;
            }
            if (reserve.fleetId != null) {
                reserve.destroyed = true;
                continue;
            }
            if (reserve.targetSystemId == null) continue;
            StarSystemAPI source = state.resolveSystem(reserve.sourceSystemId);
            StarSystemAPI target = state.resolveSystem(reserve.targetSystemId);
            if (source == null || target == null) continue;
            float distance = Misc.getDistance(source.getLocation(), target.getLocation());
            reserve.distance = Math.max(1f, distance);
            reserve.travelled = Math.min(reserve.distance,
                    reserve.travelled + Math.max(0f, Math.min(30f, day - reserve.lastDay)) * reserve.speed * 120f);
            reserve.lastDay = day;
            reserve.arrived = reserve.travelled >= reserve.distance;
            CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            if (player == null || player.getContainingLocation() != Global.getSector().getHyperspace()) continue;
            Vector2f where = getPosition(state, reserve);
            if (where != null && Misc.getDistance(player.getLocation(), where) < 5000f) materialize(reserve, where);
        }
    }

    public static Vector2f getPosition(PTSDCrisisState state, PTSDLateState.Reserve reserve) {
        StarSystemAPI source = state.resolveSystem(reserve.sourceSystemId);
        StarSystemAPI target = state.resolveSystem(reserve.targetSystemId);
        if (source == null || target == null) return null;
        float part = reserve.distance <= 0f ? 0f : Math.max(0f,
                Math.min(1f, reserve.travelled / reserve.distance));
        return new Vector2f(source.getLocation().x +
                (target.getLocation().x - source.getLocation().x) * part,
                source.getLocation().y + (target.getLocation().y - source.getLocation().y) * part);
    }

    private static void materialize(PTSDLateState.Reserve reserve, Vector2f where) {
        CampaignFleetAPI fleet;
        if (reserve.variantIds.isEmpty()) {
            FleetParamsV3 params = new FleetParamsV3(where, reserve.factionId, 1f,
                    FleetTypes.TASK_FORCE, reserve.strength, 0f, 0f, 0f, 0f, 0f, 0f);
            fleet = FleetFactoryV3.createFleet(params);
        } else {
            fleet = Global.getFactory().createEmptyFleet(reserve.factionId, "联合总参部后备队", true);
            for (String id : reserve.variantIds) {
                try { fleet.getFleetData().addFleetMember(
                        Global.getFactory().createFleetMember(FleetMemberType.SHIP, id)); }
                catch (Throwable ex) { Global.getLogger(PTSDReserveDirector.class).warn("Reserve variant lost: " + id, ex); }
            }
        }
        if (fleet == null || fleet.getFleetData().getMembersListCopy().isEmpty()) return;
        if (reserve.variantIds.isEmpty()) for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            reserve.variantIds.add(member.getVariant().getHullVariantId());
        }
        fleet.setName("联合总参部后备队");
        fleet.getMemoryWithoutUpdate().set("$PTSD_reserve_id", reserve.id);
        Global.getSector().getHyperspace().addEntity(fleet);
        fleet.setLocation(where.x, where.y);
        StarSystemAPI target = Global.getSector().getStarSystem(reserve.targetSystemId);
        if (target != null && target.getHyperspaceAnchor() != null) {
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, target.getHyperspaceAnchor(), 100f,
                    "前往总参部指定战区");
        }
        reserve.fleetId = fleet.getId();
    }

    public static void dematerialize(PTSDLateState.Reserve reserve, CampaignFleetAPI fleet) {
        if (reserve == null || fleet == null || reserve.destroyed) return;
        reserve.variantIds.clear();
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            reserve.variantIds.add(member.getVariant().getHullVariantId());
        }
        if (reserve.variantIds.isEmpty()) reserve.destroyed = true;
        fleet.getContainingLocation().removeEntity(fleet);
        reserve.fleetId = null;
    }
}
