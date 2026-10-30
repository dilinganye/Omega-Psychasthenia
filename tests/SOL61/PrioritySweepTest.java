package data.scripts.campaign.invasion;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.*;
import org.lwjgl.util.vector.Vector2f;
import java.lang.reflect.*;
import java.util.*;

public class PrioritySweepTest {
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    public static void main(String[] args)throws Exception {
        SectorAPI old=Global.getSector();SettingsAPI settings=Global.getSettings();
        try {
            Global.setSettings(NewsSceneMechanicsTest.mock(SettingsAPI.class,n->null));
            Constructor<PTSDCrisisState> c=PTSDCrisisState.class.getDeclaredConstructor();c.setAccessible(true);PTSDCrisisState s=c.newInstance();
            List<CampaignFleetAPI> fleets=new ArrayList<>();List<FleetMemberAPI> ships=new ArrayList<>();
            StarSystemAPI system=ControlledPatrolRouteTest.proxy(StarSystemAPI.class,(n,a)->n.equals("getId")?"target":n.equals("getFleets")?fleets:n.equals("getLocation")?new Vector2f():null);
            CampaignFleetAPI[] player={null};
            CampaignClockAPI clock=NewsSceneMechanicsTest.mock(CampaignClockAPI.class,n->null);
            Global.setSector(ControlledPatrolRouteTest.proxy(SectorAPI.class,(n,a)->n.equals("getStarSystem")?system:n.equals("getClock")?clock:n.equals("getPlayerFleet")?player[0]:null));
            FactionAPI human=NewsSceneMechanicsTest.mock(FactionAPI.class,n->n.equals("getId")?"testFaction":null);
            Map<String,Float> memory=new HashMap<>();
            MemoryAPI mem=ControlledPatrolRouteTest.proxy(MemoryAPI.class,(n,a)->{
                if(n.equals("getFloat"))return memory.containsKey(a[0])?memory.get(a[0]):0f;
                if(n.equals("set")){memory.put((String)a[0],((Number)a[1]).floatValue());return null;}return null;
            });
            FleetMemberAPI ship=ControlledPatrolRouteTest.proxy(FleetMemberAPI.class,(n,a)->n.equals("getFleetPointCost")?50:null);ships.add(ship);
            FleetDataAPI fd=ControlledPatrolRouteTest.proxy(FleetDataAPI.class,(n,a)->{
                if(n.equals("getMembersListCopy"))return new ArrayList<FleetMemberAPI>(ships);
                if(n.equals("removeFleetMember")){ships.remove(a[0]);return null;}return null;
            });
            boolean[] alive={true};
            CampaignFleetAPI target=ControlledPatrolRouteTest.proxy(CampaignFleetAPI.class,(n,a)->{
                if(n.equals("getFleetData"))return fd;if(n.equals("getMemoryWithoutUpdate"))return mem;
                if(n.equals("getFaction"))return human;if(n.equals("getContainingLocation"))return system;
                if(n.equals("getFleetPoints"))return ships.size()*50;if(n.equals("isAlive"))return alive[0];
                if(n.equals("despawn")){alive[0]=false;return null;}return null;
            });fleets.add(target);
            PTSDCrisisState.StrategicEvent e=new PTSDCrisisState.StrategicEvent();e.id="sweep";e.targetSystemId="target";e.priorityStarId="star";e.strength=1000;e.operationArrivalDay=100;
            check(PTSDPriorityTargetDirector.isSweep(e),"Fleet-only operation not recognized");
            e.materializedDay=101;
            check(!PTSDPriorityTargetDirector.resolveSweep(s,e,105)&&ships.size()==1,"Physical enemy deleted by virtual settlement");
            e.materializedDay=0;player[0]=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,n->n.equals("getStarSystem")?system:null);
            check(!PTSDPriorityTargetDirector.resolveSweep(s,e,106)&&ships.size()==1,"Player present during offscreen damage");
            player[0]=null;
            check(PTSDPriorityTargetDirector.resolveSweep(s,e,107)&&ships.isEmpty()&&!alive[0]&&e.strength<1000,"Offscreen battle did not consume both forces");
            check(PTSDWarCampaignDirector.refund(e.strength,true,true)==0,"Free target wave returned assets");
            // A free operation can be queued with no budget and uses the normal persistent travel record.
            s.getSystemData("target").omegaControl=1;s.late.strategicReserve=0;s.late.warPoints=0;
            check(PTSDWarCampaignDirector.launchPriority(s,null,system,110,"star"),"Free wave required resources");
            PTSDCrisisState.StrategicEvent launched=s.events.get(0);
            check(launched.priorityStarId.equals("star")&&launched.simulatedWarOperation&&launched.singularityOperation&&
                    launched.operationArrivalDay>110&&s.late.strategicReserve==0&&s.late.warPoints==0,"Free launch bypassed travel or charged budget");
            check(!PTSDWarCampaignDirector.launchPriority(s,null,system,110,"star"),"Active target wave duplicated");
            System.out.println("PrioritySweepTest: passed (physical/player protection, simulated casualties, free travel, dedup, zero refund)");
        }finally{Global.setSector(old);Global.setSettings(settings);}
    }
}
