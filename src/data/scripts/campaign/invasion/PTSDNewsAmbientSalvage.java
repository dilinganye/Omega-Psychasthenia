package data.scripts.campaign.invasion;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import org.lwjgl.util.vector.Vector2f;
import java.util.ArrayList;
import java.util.Random;

/** N-09 is a visible civilian activity, not an accept-to-spawn investigation. */
public final class PTSDNewsAmbientSalvage {
    private PTSDNewsAmbientSalvage() { }
    static boolean eligible(PTSDCrisisState.CrisisIncident item,float day,String playerSystem){
        return PTSDNewsPresentation.ambientSite(item)&&!item.siteMaterialized&&
                (item.siteEntityIds==null||item.siteEntityIds.isEmpty())&&
                day<item.createdDay+30f&&item.targetSystemId!=null&&item.targetSystemId.equals(playerSystem);
    }
    static void advance(PTSDCrisisState state,PTSDCrisisState.CrisisIncident item,float day){
        CampaignFleetAPI player=Global.getSector().getPlayerFleet();
        if(player==null||player.getStarSystem()==null||!eligible(item,day,player.getStarSystem().getId()))return;
        StarSystemAPI system=player.getStarSystem();Random rng=new Random(item.id.hashCode());
        Vector2f point=PTSDCrisisAPI.findSafePoint(system,system.getCenter(),7000,11000,rng);
        if(point==null)return;
        CampaignFleetAPI salvagers=FleetFactoryV3.createFleet(new FleetParamsV3(point,Factions.INDEPENDENT,.4f,
                FleetTypes.SCAVENGER_MEDIUM,25,15,5,0,0,0,0));
        if(salvagers==null)return;
        if(item.siteEntityIds==null)item.siteEntityIds=new ArrayList<String>();
        float remaining=Math.max(1,item.createdDay+30-day);
        SectorEntityToken field=PTSDNewsSiteManager.addDebris(system,"公开打捞作业区",point,rng,item.siteEntityIds,remaining);
        if(field==null)return;
        for(int n=0;n<2;n++)PTSDNewsSiteManager.addDebris(system,"打捞作业散落残片",
                new Vector2f(point.x+(n==0?-650:700),point.y+400),rng,item.siteEntityIds,remaining);
        system.addEntity(salvagers);PTSDCampaignRuntime.entityAdded(salvagers);
        salvagers.setLocation(point.x+500,point.y);salvagers.setName("现场打捞船队");
        salvagers.getMemoryWithoutUpdate().set("$PTSD_ambient_news",item.id);
        salvagers.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,true);
        salvagers.addAssignment(FleetAssignment.ORBIT_PASSIVE,field,remaining,"检查残骸并整理打捞货物");
        salvagers.addScript(new Retirement(salvagers,item.createdDay+25,item.createdDay+35));
        item.siteEntityIds.add(salvagers.getId());item.targetEntityId=field.getId();
        item.siteMaterialized=true;item.siteTitle="大型打捞作业区";
        item.siteDescription="残骸区内有民用打捞队作业。报道中的设备已由发现者回收，现场不保证仍有同类物资。";
    }
    /** Attached to the fleet so removal of an old news record cannot orphan it. */
    public static final class Retirement implements EveryFrameScript {
        private final CampaignFleetAPI fleet;private final float departDay,endDay;
        private float next;private boolean departing,done;
        Retirement(CampaignFleetAPI fleet,float departDay,float endDay){this.fleet=fleet;this.departDay=departDay;this.endDay=endDay;}
        public void advance(float amount){
            if(done)return;float day=PTSDCrisisState.getDay();if(day<next)return;next=day+.25f;
            if(fleet.getContainingLocation()==null||!fleet.isAlive()){done=true;return;}
            if(fleet.getBattle()!=null)return;
            if(!departing&&day>=departDay){departing=true;StarSystemAPI sys=fleet.getStarSystem();
                if(sys!=null&&!sys.getJumpPoints().isEmpty()){fleet.clearAssignments();fleet.addAssignment(FleetAssignment.GO_TO_LOCATION,sys.getJumpPoints().get(0),10,"结束打捞，撤离作业星系");}}
            if(day>=endDay){PTSDCampaignRuntime.leaving(fleet);fleet.despawn();done=true;}
        }
        public boolean isDone(){return done;}public boolean runWhilePaused(){return false;}
    }
}
