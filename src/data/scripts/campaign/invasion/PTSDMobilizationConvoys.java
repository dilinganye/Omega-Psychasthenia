package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.*;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.shard.PTSD_BaseShard_Util;
import org.lwjgl.util.vector.Vector2f;
import java.io.Serializable;
import java.util.*;

/** Physical last-month supply, never an arrival-by-timer or a war-start gate. */
public final class PTSDMobilizationConvoys {
    public static final class Record implements Serializable {
        private static final long serialVersionUID=1L;
        public String fleetId,escortId,targetId;
        public float createdDay,assets,points,nextCheck,nextDebris;
        public boolean settled,delivered;
    }
    private PTSDMobilizationConvoys(){}
    public static void advance(PTSDCrisisState state,float day){
        if(state.late.mobilizationConvoys==null)state.late.mobilizationConvoys=new ArrayList<Record>();
        if(state.phase==PTSDCrisisState.Phase.MOBILIZATION && day<state.phaseStartedDay+30) {
            if(!state.late.convoyRequirementLocked){
                PTSDWarCampaignDirector.prepare(state,day,true);
                state.late.convoyRequiredAssets=state.late.openingAssetsRequired;
                state.late.convoyRequiredPoints=state.late.openingPointsRequired;
                state.late.convoyRequirementLocked=true;
                state.late.nextMobilizationConvoyDay=day;
            }
            if(day>=state.late.nextMobilizationConvoyDay){
                state.late.nextMobilizationConvoyDay=day+5;spawn(state,day);
            }
        }
        for(Record record:state.late.mobilizationConvoys)if(!record.settled && day>=record.nextCheck){
            record.nextCheck=day+.1f;
            CampaignFleetAPI transport=PTSDNewsScene.fleet(record.fleetId);
            if(transport==null || !transport.isAlive() || transport.isDespawning() || transport.getFleetPoints()<=0){settle(state,record,false);continue;}
            if(transport.getBattle()!=null)continue;
            SectorEntityToken target=PTSDNewsScene.entity(record.targetId);
            if(target==null || target.getFaction()==null || !PTSDPrewarReconDirector.omega(target.getFaction().getId())){
                List<SectorEntityToken> alternatives=destinations(state);
                if(alternatives.isEmpty()){if(day>=record.createdDay+60){settle(state,record,false);remove(transport);}continue;}
                target=alternatives.get(0);record.targetId=target.getId();transport.clearAssignments();
            }
            if(transport.getContainingLocation()==target.getContainingLocation() && Misc.getDistance(transport,target)<900){settle(state,record,true);remove(transport);continue;}
            if(transport.getCurrentAssignment()==null)transport.addAssignment(FleetAssignment.GO_TO_LOCATION,target,60,"向据点转运物资");
            if(day>=record.nextDebris){record.nextDebris=day+2;
                Random random=new Random((record.fleetId+":"+(int)day).hashCode());
                if(random.nextFloat()<.3f)debris(transport.getContainingLocation(),transport.getLocation(),random,2);
            }
            if(day>=record.createdDay+60){settle(state,record,false);remove(transport);}
        }
    }
    static List<SectorEntityToken> destinations(PTSDCrisisState state){
        List<SectorEntityToken> result=new ArrayList<SectorEntityToken>();
        for(PTSDLateState.Fortress f:state.late.fortresses.values()){
            SectorEntityToken entity=PTSDNewsScene.entity(f.fleetId);if(!f.destroyed && entity!=null && entity.getFaction()!=null && PTSDPrewarReconDirector.omega(entity.getFaction().getId()))result.add(entity);
        }
        for(PTSDCrisisState.SystemData data:state.systems.values())if(data.omegaControl>=.5f){
            StarSystemAPI sys=state.resolveSystem(data.systemId);if(sys==null)continue;
            for(PlanetAPI planet:sys.getPlanets())if(!planet.isStar() && planet.getFaction()!=null && PTSDPrewarReconDirector.omega(planet.getFaction().getId()))result.add(planet);
        }
        return result;
    }
    static boolean spawn(PTSDCrisisState state,float day){
        List<SectorEntityToken> targets=destinations(state);if(targets.isEmpty())return false;
        if(!state.late.convoyRequirementLocked){
            PTSDWarCampaignDirector.prepare(state,day,true);
            state.late.convoyRequiredAssets=state.late.openingAssetsRequired;
            state.late.convoyRequiredPoints=state.late.openingPointsRequired;
            state.late.convoyRequirementLocked=true;
            state.late.nextMobilizationConvoyDay=day+5;
        }
        Random random=new Random();SectorEntityToken target=targets.get(state.late.mobilizationConvoys.size()%targets.size());
        Vector2f point=Misc.getUnitVectorAtDegreeAngle(random.nextFloat()*360);point.scale(10000+random.nextFloat()*7000);Vector2f.add(target.getLocationInHyperspace(),point,point);
        CampaignFleetAPI transport=PTSDEntropyTransportFactory.create(state,Global.getSector().getHyperspace(),point.x,point.y,0,"战前补给输入");
        if(transport==null)return false;
        transport.getMemoryWithoutUpdate().set("$PTSD_encounter_response_installed",true);
        transport.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,true);
        transport.setName("携带拖曳残片的未知运输队");
        transport.addAssignment(FleetAssignment.GO_TO_LOCATION,target,60,"向据点转运物资");
        PTSDCrisisFleetLimits limits=PTSDCrisisFleetLimits.forPhase(state,day);
        float fp=Math.min(80,limits.perFleetFp);
        CampaignFleetAPI escort=PTSD_BaseShard_Util.createFleet(new FleetParamsV3(point,PTSDCrisisProgress.getActiveFactionId(state),.5f,FleetTypes.PATROL_MEDIUM,fp,0,0,0,0,0,0),fp,PTSD_BaseShard_Util.FleetRole.GUARD_ASSAULT,random,limits.forFleet(fp));
        if(escort==null){remove(transport);return false;}
        if(escort!=null){
            escort.getMemoryWithoutUpdate().set("$PTSD_encounter_response_installed",true);
            Global.getSector().getHyperspace().addEntity(escort);escort.setLocation(point.x+250,point.y-200);PTSDCampaignRuntime.entityAdded(escort);
            escort.addAssignment(FleetAssignment.DEFEND_LOCATION,transport,60,"押运物资");
        }
        for(CampaignFleetAPI fleet:new CampaignFleetAPI[]{transport,escort})if(fleet!=null)
            for(com.fs.starfarer.api.fleet.FleetMemberAPI member:fleet.getFleetData().getMembersListCopy())member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
        Record record=new Record();record.fleetId=transport.getId();record.escortId=escort==null?null:escort.getId();record.targetId=target.getId();record.createdDay=day;
        record.assets=state.late.convoyRequiredAssets*.1f;record.points=state.late.convoyRequiredPoints*.1f;record.nextDebris=day+2;
        state.late.mobilizationConvoys.add(record);debris(transport.getContainingLocation(),point,random,6);
        PTSDUnknownConvoyIntel.ensure();return true;
    }
    static void settle(PTSDCrisisState state,Record r,boolean delivered){
        if(r.settled)return;r.settled=true;r.delivered=delivered;
        if(delivered){
            PTSDLateWarDirector.creditStrategicAssets(state,"mobilization-convoy:"+r.fleetId,r.assets,"运输抵达补充战前物资",null);
            state.late.warPoints+=r.points;state.late.warPointBelief+=r.points;
        }
        CampaignFleetAPI escort=PTSDNewsScene.fleet(r.escortId);
        SectorEntityToken target=PTSDNewsScene.entity(r.targetId);
        if(escort!=null && target!=null && target.getStarSystem()!=null)
            PTSDWarWaveReturn.begin(escort,target.getStarSystem().getId(),0,0); // Free supply escort never mints assets.
        else if(escort!=null && escort.getBattle()==null)remove(escort);
    }
    private static void remove(CampaignFleetAPI fleet){PTSDCampaignRuntime.leaving(fleet);fleet.despawn();}
    static void debris(LocationAPI location,Vector2f center,Random random,int count){
        for(int n=0;n<count;n++){
            DebrisFieldParams params=new DebrisFieldParams(180+random.nextFloat()*180,-1,20,.2f);params.source=DebrisFieldSource.BATTLE;
            SectorEntityToken field=Misc.addDebrisField(location,params,random);
            Vector2f offset=Misc.getUnitVectorAtDegreeAngle(random.nextFloat()*360);offset.scale(random.nextFloat()*1500);
            field.setLocation(center.x+offset.x,center.y+offset.y);field.setName("正在散逸的残骸带");
        }
    }
}
