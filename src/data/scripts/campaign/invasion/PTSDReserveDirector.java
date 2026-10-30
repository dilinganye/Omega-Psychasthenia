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
                    withdrawSupport(state, reserve);
                } else {
                    reserve.strength = Math.max(0f, live.getFleetPoints());
                    reserve.arrived = live.getStarSystem() != null && live.getStarSystem().getId().equals(reserve.targetSystemId);
                    if (reserve.arrived) support(state, reserve);
                    else if (reserve.committedDefense > 0f) withdrawSupport(state, reserve);
                }
                continue;
            }
            if (reserve.fleetId != null) {
                reserve.destroyed = true;
                withdrawSupport(state, reserve);
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
            if (reserve.arrived) support(state, reserve);
            CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            if (player != null && reserve.arrived && player.getStarSystem() == target) {
                materialize(reserve, target.getCenter().getLocation()); continue;
            }
            if (player == null || player.getContainingLocation() != Global.getSector().getHyperspace()) continue;
            Vector2f where = getPosition(state, reserve);
            if (where != null && Misc.getDistance(player.getLocation(), where) < 5000f) materialize(reserve, where);
        }
    }

    public static void withdrawSupport(PTSDCrisisState state, PTSDLateState.Reserve reserve) {
        PTSDLateState.Front old = state.late.fronts.get(reserve.supportingFrontId);
        if (old != null && !old.resolved) old.defense = Math.max(0f, old.defense - reserve.committedDefense);
        reserve.committedDefense = 0f; reserve.supportingFrontId = null;
    }
    private static void support(PTSDCrisisState state, PTSDLateState.Reserve reserve) {
        for (PTSDLateState.Front front : state.late.fronts.values()) {
            if (front.resolved || !front.systemId.equals(reserve.targetSystemId)) continue;
            if (!front.id.equals(reserve.supportingFrontId)) withdrawSupport(state, reserve);
            front.defense = Math.max(0f, front.defense + reserve.strength - reserve.committedDefense);
            reserve.supportingFrontId = front.id; reserve.committedDefense = reserve.strength;
            if(reserve.id!=null && reserve.factionId!=null)
                PTSDGeneralStaffDirector.recordContribution(state,"support:"+reserve.id+":"+front.id,
                        "SUPPORT",reserve.factionId,front.systemId,reserve.strength/10f);
            return;
        }
        withdrawSupport(state, reserve);
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
        if(reserve.members!=null&&!reserve.members.isEmpty()){
            fleet=Global.getFactory().createEmptyFleet(reserve.factionId,"联合总参部后备队",true);
            for(FleetMemberAPI member:reserve.members)fleet.getFleetData().addFleetMember(member);
        } else if (reserve.variantIds.isEmpty()) {
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
        // Doctrine multipliers must not turn a funded reserve into unbudgeted extra ships.
        java.util.List<FleetMemberAPI> members=new java.util.ArrayList<FleetMemberAPI>(fleet.getFleetData().getMembersListCopy());
        for(int index=members.size()-1;index>=0 && fleet.getFleetPoints()>reserve.strength+.01f;index--)
            fleet.getFleetData().removeFleetMember(members.get(index));
        if(fleet.getFleetData().getMembersListCopy().isEmpty())return;
        reserve.strength=Math.min(reserve.strength,fleet.getFleetPoints());
        reserve.members=new java.util.ArrayList<FleetMemberAPI>(fleet.getFleetData().getMembersListCopy());
        if (reserve.variantIds.isEmpty()) for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            reserve.variantIds.add(member.getVariant().getHullVariantId());
        }
        fleet.setName("联合总参部后备队");
        fleet.getMemoryWithoutUpdate().set("$PTSD_reserve_id", reserve.id);
        StarSystemAPI target = PTSDCrisisState.get().resolveSystem(reserve.targetSystemId);
        com.fs.starfarer.api.campaign.LocationAPI location = reserve.arrived && target != null ? target : Global.getSector().getHyperspace();
        location.addEntity(fleet); PTSDCampaignRuntime.entityAdded(fleet);
        fleet.setLocation(where.x, where.y);
        if (target != null && target.getHyperspaceAnchor() != null) {
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, target.getCenter(), 100f,
                    "前往总参部指定战区");
            fleet.addAssignment(FleetAssignment.DEFEND_LOCATION, target.getCenter(), 1000f, "协同防守战区");
        }
        reserve.fleetId = fleet.getId();
    }

    public static void dematerialize(PTSDLateState.Reserve reserve, CampaignFleetAPI fleet) {
        if (reserve == null || fleet == null || reserve.destroyed || fleet.getBattle()!=null) return;
        reserve.members=new java.util.ArrayList<FleetMemberAPI>(fleet.getFleetData().getMembersListCopy());
        reserve.strength=fleet.getFleetPoints();
        reserve.variantIds.clear();
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            reserve.variantIds.add(member.getVariant().getHullVariantId());
        }
        if (reserve.variantIds.isEmpty()) reserve.destroyed = true;
        PTSDCampaignRuntime.leaving(fleet);
        if(fleet.getContainingLocation()!=null)fleet.getContainingLocation().removeEntity(fleet);
        reserve.fleetId = null;
    }
}
