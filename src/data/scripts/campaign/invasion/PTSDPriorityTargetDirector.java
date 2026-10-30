package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import java.util.*;

/** Explicit targets bypass ordinary faction immunity only inside the selected star's system. */
public final class PTSDPriorityTargetDirector {
    private PTSDPriorityTargetDirector(){}
    public static void advance(PTSDCrisisState state,float day){
        if(day<state.late.nextPriorityTargetDay)return;state.late.nextPriorityTargetDay=day+1;
        if(state.late.priorityReconDays==null)state.late.priorityReconDays=new LinkedHashMap<String,Float>();
        if(state.late.priorityOpeningLaunched==null)state.late.priorityOpeningLaunched=new LinkedHashSet<String>();
        if(state.phase==PTSDCrisisState.Phase.WAR){launchOpening(state,day);return;}
        boolean harassment=PTSDPrewarReconDirector.regularPhase(state.phase);
        if(state.phase!=PTSDCrisisState.Phase.RECON&&!harassment)return;
        for(Map.Entry<String,StarSystemAPI> entry:PTSDPriorityTargetList.enabledSystems().entrySet()) {
            String key=entry.getKey()+":"+(harassment?"harass":"recon");
            Float next=state.late.priorityReconDays.get(key);if(next!=null&&day<next)continue;
            CampaignFleetAPI fleet=PTSDPrewarReconDirector.spawn(state,entry.getValue(),null,true,harassment);
            state.late.priorityReconDays.put(key,day+(fleet==null?5:PTSDPriorityTargetList.interval(harassment)));
        }
    }
    public static void launchOpening(PTSDCrisisState state,float day){
        if(state.late.priorityOpeningLaunched==null)state.late.priorityOpeningLaunched=new LinkedHashSet<String>();
        for(StarSystemAPI s:PTSDPriorityTargetList.enabledSystems().values()) {
            if(state.late.priorityOpeningLaunched.contains(s.getId()+":complete"))continue;
            boolean complete=true;
            List<MarketAPI> markets=new ArrayList<MarketAPI>();
            for(MarketAPI m:PTSDOccupationManager.getAllMarkets())if(m.getStarSystem()==s&&!m.isPlanetConditionMarketOnly() &&
                    !PTSDPrewarReconDirector.omega(m.getFactionId())&&m.getPrimaryEntity()!=null)markets.add(m);
            // One fleet sweep also covers configured systems without colonies.
            String sweep=s.getId()+":sweep";
            if(!state.late.priorityOpeningLaunched.contains(sweep) && PTSDWarCampaignDirector.launchPriority(state,null,s,day,starId(s)))
                state.late.priorityOpeningLaunched.add(sweep);
            if(!state.late.priorityOpeningLaunched.contains(sweep))complete=false;
            for(MarketAPI m:markets) {
                String key=s.getId()+":"+m.getId();if(state.late.priorityOpeningLaunched.contains(key))continue;
                if(PTSDWarCampaignDirector.launchPriority(state,m,s,day,starId(s))){state.late.priorityOpeningLaunched.add(key);state.late.selectedBurstCount++;}
                else complete=false;
            }
            if(complete)state.late.priorityOpeningLaunched.add(s.getId()+":complete");
        }
    }
    private static String starId(StarSystemAPI s){return s.getStar()==null?s.getId():s.getStar().getId();}
    static List<CampaignFleetAPI> defenders(StarSystemAPI system){
        List<CampaignFleetAPI> result=new ArrayList<CampaignFleetAPI>();
        if(system==null)return result;
        for(CampaignFleetAPI f:PTSDCampaignRuntime.fleets(system))if(f!=null && f.isAlive() && !f.isExpired() && !f.isDespawning() &&
                f.getContainingLocation()==system && f.getFaction()!=null && !f.isPlayerFleet() &&
                !PTSDPrewarReconDirector.omega(f.getFaction().getId()) && !PTSDProtectedFaction.isGamma(f.getFaction().getId()) && f.getFleetPoints()>0)result.add(f);
        return result;
    }
    /** Returns true only once the sweep is finished; a beaten wave never respawns for free. */
    static boolean resolveSweep(PTSDCrisisState state,PTSDCrisisState.StrategicEvent e,float day){
        StarSystemAPI system=state.resolveSystem(e.targetSystemId);
        List<CampaignFleetAPI> targets=defenders(system);
        if(targets.isEmpty() || e.strength<=0 || day>=e.operationArrivalDay+30)return true;
        if(e.materializedDay>0) {e.resolveDay=day+1;return false;} // Physical fleets must actually fight.
        CampaignFleetAPI player=Global.getSector().getPlayerFleet();
        if(player!=null && player.getStarSystem()==system){e.resolveDay=day+1;return false;}
        if(day<e.priorityNextCombatDay){e.resolveDay=day+1;return false;}
        e.priorityNextCombatDay=day+1;
        // One bounded offscreen engagement per day, with losses on both sides. Fleets that leave escape it.
        CampaignFleetAPI defender=targets.get(0);
        if(defender.getBattle()!=null){e.resolveDay=day+1;return false;}
        float defense=defender.getFleetPoints();
        float damage=Math.min(defense,Math.max(1,e.strength*.25f));
        String key="$PTSD_priority_damage_"+e.id;
        float accumulated=defender.getMemoryWithoutUpdate().getFloat(key)+damage;
        for(FleetMemberAPI member: new ArrayList<FleetMemberAPI>(defender.getFleetData().getMembersListCopy())) {
            if(accumulated<member.getFleetPointCost())continue;
            accumulated-=member.getFleetPointCost();defender.getFleetData().removeFleetMember(member);
        }
        defender.getMemoryWithoutUpdate().set(key,accumulated,35);
        e.strength=Math.max(0,e.strength-Math.max(1,defense*.15f));
        if(defender.getFleetData().getMembersListCopy().isEmpty()) {
            PTSDCampaignRuntime.leaving(defender);
            defender.despawn(CampaignEventListener.FleetDespawnReason.DESTROYED_BY_BATTLE,null);
        }
        e.resolveDay=day+1;return defenders(system).isEmpty()||e.strength<=0;
    }
    static boolean isSweep(PTSDCrisisState.StrategicEvent e){return e.priorityStarId!=null&&e.targetMarketId==null;}
    static SectorEntityToken sweepAnchor(StarSystemAPI s){return s.getJumpPoints().isEmpty()?s.getCenter():s.getJumpPoints().get(0);}
    static void hunt(CampaignFleetAPI fleet,PTSDCrisisState.StrategicEvent e){
        if(!isSweep(e)||fleet.getBattle()!=null || fleet.getStarSystem()==null || !e.targetSystemId.equals(fleet.getStarSystem().getId()))return;
        CampaignFleetAPI closest=null;float distance=Float.MAX_VALUE;
        for(CampaignFleetAPI target:defenders(fleet.getStarSystem())) {
            if(target.getVisibilityLevelTo(fleet)==SectorEntityToken.VisibilityLevel.NONE)continue;
            float d=Misc.getDistance(fleet,target);if(d<distance){distance=d;closest=target;}
        }
        if(closest!=null && closest.getBattle()==null && Misc.getDistance(fleet,closest)<250f) {
            Global.getFactory().createBattle(fleet,closest);return;
        }
        if(closest!=null && (fleet.getCurrentAssignment()==null||fleet.getCurrentAssignment().getTarget()!=closest)) {
            fleet.clearAssignments();fleet.addAssignment(FleetAssignment.INTERCEPT,closest,3,"清剿已发现舰队");
        } else if(closest==null && fleet.getCurrentAssignment()==null) {
            fleet.addAssignment(FleetAssignment.PATROL_SYSTEM,sweepAnchor(fleet.getStarSystem()),2,"搜索指定星系");
            PTSDControlledPatrolDirector.installNodePatrol(fleet);
        }
    }
}
