package data.scripts.campaign.invasion;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.ai.*;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import java.lang.reflect.*;
import java.util.*;

public class ControlledPatrolRouteTest {
    interface Call {Object invoke(String name,Object[] args);}
    @SuppressWarnings("unchecked") static <T>T proxy(Class<T> type,Call call){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},(p,m,a)->{
        if(m.getName().equals("equals"))return p==a[0];if(m.getName().equals("hashCode"))return System.identityHashCode(p);
        Object v=call.invoke(m.getName(),a);if(v!=null)return v;
        if(m.getReturnType()==boolean.class)return false;if(m.getReturnType()==float.class)return 0f;
        if(m.getReturnType()==int.class)return 0;return null;
    });}
    static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
    public static void main(String[] args)throws Exception{
        FactionAPI faction=proxy(FactionAPI.class,(n,a)->n.equals("getId")?IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION:null);
        PlanetAPI planet=proxy(PlanetAPI.class,(n,a)->n.equals("getId")?"occupied":n.equals("getFaction")?faction:null);
        PlanetAPI star=proxy(PlanetAPI.class,(n,a)->n.equals("isStar")?true:null);
        SectorEntityToken jump=proxy(SectorEntityToken.class,(n,a)->null);
        SectorEntityToken facility=proxy(SectorEntityToken.class,(n,a)->n.equals("hasTag")?Tags.COMM_RELAY.equals(a[0]):null);
        SectorEntityToken terrain=proxy(SectorEntityToken.class,(n,a)->null);
        StarSystemAPI system=proxy(StarSystemAPI.class,(n,a)->{
            if(n.equals("getJumpPoints"))return Arrays.asList(jump);
            if(n.equals("getPlanets"))return Arrays.asList(star,planet);
            if(n.equals("getAllEntities"))return Arrays.asList(jump,star,planet,facility,terrain);
            if(n.equals("getEntityById"))return planet;
            if(n.equals("createToken"))throw new AssertionError("Random patrol token created");return null;
        });
        List<SectorEntityToken> anchors=PTSDControlledPatrolDirector.patrolAnchors(system);
        check(anchors.size()==3&&anchors.contains(jump)&&anchors.contains(planet)&&anchors.contains(facility),"Wrong node set or duplicate nodes");
        List<Object[]> orders=new ArrayList<Object[]>();boolean[] busy={false};
        FleetAssignmentDataAPI assignment=proxy(FleetAssignmentDataAPI.class,(n,a)->null);
        CampaignFleetAIAPI ai=proxy(CampaignFleetAIAPI.class,(n,a)->n.equals("getCurrentAssignment")&&busy[0]?assignment:null);
        CampaignFleetAPI fleet=proxy(CampaignFleetAPI.class,(n,a)->{if(n.equals("getAI"))return ai;if(n.equals("addAssignment"))orders.add(a);if(n.equals("clearAssignments"))throw new AssertionError("Active orders cleared");return null;});
        Method route=PTSDControlledPatrolDirector.class.getDeclaredMethod("route",PTSDCrisisState.class,StarSystemAPI.class,PTSDLateState.ControlledPatrol.class,CampaignFleetAPI.class,float.class);route.setAccessible(true);
        PTSDLateState.ControlledPatrol slot=new PTSDLateState.ControlledPatrol();
        route.invoke(null,null,system,slot,fleet,1f);check(orders.size()==2&&orders.get(0)[1]==jump,"First route not actual jump point");
        busy[0]=true;route.invoke(null,null,system,slot,fleet,100f);check(orders.size()==2,"In-progress trip interrupted");
        busy[0]=false;route.invoke(null,null,system,slot,fleet,101f);check(orders.get(2)[1]==planet,"Next route not planet");
        orders.clear();slot.guardedPlanetId="occupied";route.invoke(null,null,system,slot,fleet,102f);
        check(orders.size()==1&&orders.get(0)[0]==FleetAssignment.ORBIT_AGGRESSIVE&&orders.get(0)[1]==planet,"Planet guard leaves its post");
        System.out.println("ControlledPatrolRouteTest: passed (real nodes, no terrain/random point, uninterrupted transit, close guard)");
    }
}
