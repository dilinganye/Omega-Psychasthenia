package data.scripts.campaign.invasion;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.util.Misc;

/** Budgeted war fleets physically withdraw; only survivors reaching the rear replenish assets. */
public final class PTSDWarWaveReturn implements EveryFrameScript {
    private final CampaignFleetAPI fleet;
    private final String sourceId;
    private final float deadline;
    private final float paidShare;
    private float refundMultiplier = .5f;
    private float next;
    private boolean done;
    private PTSDWarWaveReturn(CampaignFleetAPI fleet,String source,float paidShare){this.fleet=fleet;sourceId=source;this.paidShare=paidShare;deadline=PTSDCrisisState.getDay()+80;}
    static void begin(CampaignFleetAPI fleet,String source,float paidShare){
        begin(fleet,source,paidShare,.5f);
    }
    static void begin(CampaignFleetAPI fleet,String source,float paidShare,float multiplier){
        if(fleet.getMemoryWithoutUpdate().getBoolean("$PTSD_wave_return"))return;
        fleet.getMemoryWithoutUpdate().set("$PTSD_wave_return",true);
        SectorEntityToken destination=anchor(PTSDCrisisState.get(),source);
        if(destination!=null){fleet.clearAssignments();fleet.addAssignment(FleetAssignment.GO_TO_LOCATION,destination,80,"后撤整备");}
        PTSDWarWaveReturn script=new PTSDWarWaveReturn(fleet,source,paidShare);
        script.refundMultiplier=multiplier;fleet.addScript(script);
    }
    public void advance(float amount){
        float day=PTSDCrisisState.getDay();if(day<next)return;next=day+.2f;
        if(fleet.getContainingLocation()==null||fleet.getFleetPoints()<=0){done=true;return;}
        if(fleet.getBattle()!=null)return;
        PTSDCrisisState state=PTSDCrisisState.get();StarSystemAPI source=state.resolveSystem(sourceId);
        SectorEntityToken destination=anchor(state,sourceId);
        boolean arrived=source!=null&&destination!=null&&fleet.getContainingLocation()==source&&Misc.getDistance(fleet,destination)<1500;
        if(!arrived) {
            if(destination!=null && fleet.getCurrentAssignment()==null)
                fleet.addAssignment(FleetAssignment.GO_TO_LOCATION,destination,80,"继续返航整备");
            return;
        }
        PTSDLateWarDirector.creditStrategicAssets(state,"war-wave-return:"+fleet.getId(),
                Math.max(0,Math.min(fleet.getFleetPoints(),paidShare))*refundMultiplier,"部队抵达后方",sourceId);
        PTSDCampaignRuntime.leaving(fleet);fleet.despawn();done=true;
    }
    public boolean isDone(){return done;}public boolean runWhilePaused(){return false;}
    static float refund(float surviving,float paid){return Math.max(0,Math.min(surviving,paid))*.5f;}
    static SectorEntityToken anchor(PTSDCrisisState state,String sourceId) {
        StarSystemAPI source=state.resolveSystem(sourceId);if(source==null)return null;
        for(PTSDLateState.Fortress f:state.late.fortresses.values()) {
            if(f.destroyed||!sourceId.equals(f.currentSystemId)||f.fleetId==null)continue;
            SectorEntityToken station=Global.getSector().getEntityById(f.fleetId);
            if(station!=null&&station.getContainingLocation()==source)return station;
        }
        for(PlanetAPI p:source.getPlanets())if(!p.isStar()&&p.getFaction()!=null&&
                (IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(p.getFaction().getId())||
                IIRT_Omega_Invasion.WATCHER_FACTION.equals(p.getFaction().getId())))return p;
        return source.getJumpPoints().isEmpty()?source.getCenter():source.getJumpPoints().get(0);
    }
}
