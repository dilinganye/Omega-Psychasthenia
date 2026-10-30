package data.scripts.campaign.invasion;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.shard.PTSD_BaseShard_Util;
import org.lwjgl.util.vector.Vector2f;
import java.util.*;

/** Physical two-stop reconnaissance. Samples remain aboard until an owned planet is reached. */
public final class PTSDPrewarReconDirector {
    private PTSDPrewarReconDirector(){}
    static boolean regularPhase(PTSDCrisisState.Phase phase){
        return phase==PTSDCrisisState.Phase.FORTIFICATION || phase==PTSDCrisisState.Phase.MOBILIZATION;
    }
    public static void advance(PTSDCrisisState state,float day){
        if(!regularPhase(state.phase))return;
        if(state.late.nextCarrierReconDay==0){state.late.nextCarrierReconDay=day+5;return;}
        if(day<state.late.nextCarrierReconDay)return;
        state.late.nextCarrierReconDay=day+5; // No catch-up burst after a load or time skip.
        List<StarSystemAPI> all=inhabited();
        if(all.isEmpty())return;
        com.fs.starfarer.api.util.WeightedRandomPicker<StarSystemAPI> picker=new com.fs.starfarer.api.util.WeightedRandomPicker<StarSystemAPI>();
        for(StarSystemAPI s:all)picker.add(s,s.hasTag(Tags.THEME_CORE)?3f:1f);
        StarSystemAPI start=picker.pick();
        List<StarSystemAPI> others=new ArrayList<StarSystemAPI>(all);others.remove(start);
        StarSystemAPI next=others.isEmpty()?null:others.get(new Random().nextInt(others.size()));
        spawn(state,start,next,false,false);
    }
    static List<StarSystemAPI> inhabited(){
        Set<StarSystemAPI> result=new LinkedHashSet<StarSystemAPI>();
        for(MarketAPI m:PTSDOccupationManager.getAllMarkets())if(m!=null&&!m.isPlanetConditionMarketOnly() &&
                m.getStarSystem()!=null&&!omega(m.getFactionId()))result.add(m.getStarSystem());
        return new ArrayList<StarSystemAPI>(result);
    }
    static boolean omega(String id){return IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(id)||
            IIRT_Omega_Invasion.WATCHER_FACTION.equals(id)||"omega".equals(id);}
    static SectorEntityToken observationPoint(StarSystemAPI system){
        for(MarketAPI m:Global.getSector().getEconomy().getMarkets(system))
            if(!m.isPlanetConditionMarketOnly()&&!omega(m.getFactionId())&&m.getPrimaryEntity()!=null)return m.getPrimaryEntity();
        return system.getJumpPoints().isEmpty()?system.getCenter():system.getJumpPoints().get(0);
    }
    static boolean controlled(String id){return IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(id)||IIRT_Omega_Invasion.WATCHER_FACTION.equals(id);}
    static PlanetAPI home(CampaignFleetAPI fleet){
        PlanetAPI best=null;float distance=Float.MAX_VALUE;boolean bestLocal=false;
        for(StarSystemAPI s:Global.getSector().getStarSystems())for(PlanetAPI p:s.getPlanets())
            if(!p.isStar()&&p.getFaction()!=null&&controlled(p.getFaction().getId())) {
                boolean local=fleet.getContainingLocation()==p.getContainingLocation();
                float d=local?Misc.getDistance(fleet,p):Misc.getDistance(fleet.getLocationInHyperspace(),p.getLocationInHyperspace());
                if(best==null || local&&!bestLocal || local==bestLocal&&d<distance){distance=d;best=p;bestLocal=local;}
            }
        return best;
    }
    static CampaignFleetAPI spawn(PTSDCrisisState state,StarSystemAPI start,StarSystemAPI next,boolean armed,boolean harassment){
        PTSDCrisisFleetLimits limits=PTSDCrisisFleetLimits.forPhase(state,PTSDCrisisState.getDay());
        float fp=limits.perFleetFp;
        FleetParamsV3 params=new FleetParamsV3(start.getLocation(),IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION,1f,
                FleetTypes.MERC_SCOUT,fp,0,0,0,0,0,0);
        CampaignFleetAPI fleet=PTSD_BaseShard_Util.createFleet(params,fp,armed?PTSD_BaseShard_Util.FleetRole.GUARD_ASSAULT:
                PTSD_BaseShard_Util.FleetRole.RECON,new Random(),limits.forFleet(fp));
        if(fleet==null)return null;
        SectorEntityToken anchor=start.getJumpPoints().isEmpty()?observationPoint(start):start.getJumpPoints().get(0);
        Vector2f point=PTSDCrisisAPI.findSafePoint(start,anchor,500,1200,new Random());
        if(point==null)return null;
        fleet.setName(harassment?"精神创伤武装骚扰队":armed?"精神创伤强行侦察队":"精神创伤巡回侦察队");
        fleet.setNoFactionInName(true);fleet.setTransponderOn(false);fleet.addTag("PTSD_crisis_fleet");
        fleet.getMemoryWithoutUpdate().set("$PTSD_encounter_response_installed",true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY,true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,!armed);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE,armed);
        start.addEntity(fleet);fleet.setLocation(point.x,point.y);PTSDCampaignRuntime.entityAdded(fleet);
        fleet.addScript(new Carrier(fleet,start,next,armed,harassment));
        PTSDCrisisDevIntel.report("巡回侦察出航",start.getName()+" → "+(next==null?"返航":next.getName())+"；返抵控制星球后上传",start.getId(),fleet.getId());
        return fleet;
    }
    private static final class Carrier implements EveryFrameScript {
        final CampaignFleetAPI fleet;final List<String> stops=new ArrayList<String>();
        final List<PTSDCrisisState.PendingReconReport> payload=new ArrayList<PTSDCrisisState.PendingReconReport>();
        final boolean armed,harassment;int index;float nextCheck,stationUntil,deadline;boolean done;
        SectorEntityToken destination;float nextHomeCheck;
        Carrier(CampaignFleetAPI f,StarSystemAPI first,StarSystemAPI second,boolean a,boolean h){
            fleet=f;armed=a;harassment=h;stops.add(first.getId());if(second!=null)stops.add(second.getId());
            deadline=PTSDCrisisState.getDay()+120;
            destination=observationPoint(first);order();
        }
        public boolean runWhilePaused(){return false;}
        public boolean isDone(){return done||!fleet.isAlive()||fleet.isExpired();}
        private void order(){
            fleet.clearAssignments();if(destination!=null)fleet.addAssignment(FleetAssignment.GO_TO_LOCATION,destination,120,"前往侦察航点");
        }
        public void advance(float amount){
            if(isDone())return;float day=PTSDCrisisState.getDay();if(day<nextCheck)return;nextCheck=day+.1f;
            if(fleet.isVisibleToPlayerFleet())PTSDJeOtloesManager.recordFleetEncounter(fleet);
            if(fleet.getBattle()!=null)return;
            PTSDCrisisState state=PTSDCrisisState.get();
            if(day>=deadline && index<stops.size()){index=stops.size();destination=null;}
            if(index>=stops.size()) {
                if(destination==null || !controlled(destination.getFaction().getId()) || day>=nextHomeCheck) {
                    PlanetAPI planet=home(fleet);nextHomeCheck=day+5;
                    if(planet!=destination){destination=planet;order();}
                }
                if(destination!=null && fleet.getContainingLocation()==destination.getContainingLocation() && Misc.getDistance(fleet,destination)<700) {
                    for(PTSDCrisisState.PendingReconReport report:payload)PTSDReconReportDirector.deliverCarrierSurvey(state,report);
                    // Prewar event carriers follow the existing surviving-FP event retirement rule.
                    PTSDEventFleetReturn.credit(fleet);
                    done=true;PTSDCampaignRuntime.leaving(fleet);fleet.despawn();return;
                }
                if(fleet.getCurrentAssignment()==null)order();return;
            }
            StarSystemAPI system=state.resolveSystem(stops.get(index));
            if(system==null){index++;destination=null;return;}
            if(destination==null){destination=observationPoint(system);order();}
            if(fleet.getContainingLocation()==system && Misc.getDistance(fleet,destination)<1800) {
                if(stationUntil==0) {
                    stationUntil=day+(harassment?5:1);fleet.clearAssignments();
                    fleet.addAssignment(armed?FleetAssignment.ATTACK_LOCATION:FleetAssignment.ORBIT_PASSIVE,destination,
                            harassment?5:1,armed?"测定防区火力":"采集区域资料");
                }
                if(day>=stationUntil){
                    payload.add(PTSDReconReportDirector.captureCarrierSurvey(state,fleet,system,armed));
                    index++;stationUntil=0;destination=index<stops.size()?observationPoint(state.resolveSystem(stops.get(index))):home(fleet);order();
                }
            } else if(fleet.getCurrentAssignment()==null)order();
        }
    }
}
