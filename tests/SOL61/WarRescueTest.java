package data.scripts.campaign.invasion;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.lwjgl.util.vector.Vector2f;
import java.util.*;
import java.lang.reflect.*;
public class WarRescueTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static CampaignFleetAPI roster(final int... points){
  final List<FleetMemberAPI> list=new ArrayList<FleetMemberAPI>();
  for(final int fp:points)list.add(ControlledPatrolRouteTest.proxy(FleetMemberAPI.class,(n,a)->n.equals("getFleetPointCost")?fp:null));
  FleetDataAPI data=ControlledPatrolRouteTest.proxy(FleetDataAPI.class,(n,a)->{if(n.equals("getMembersListCopy"))return new ArrayList<FleetMemberAPI>(list);if(n.equals("removeFleetMember")){list.remove(a[0]);return null;}return null;});
  return ControlledPatrolRouteTest.proxy(CampaignFleetAPI.class,(n,a)->{if(n.equals("getFleetData"))return data;if(n.equals("getFleetPoints")){int sum=0;for(FleetMemberAPI m:list)sum+=m.getFleetPointCost();return sum;}return null;});
 }
 public static void main(String[] args)throws Exception{
  CampaignFleetAPI a=roster(40,30,20,10),b=roster(35,25,20,10);
  check(PTSDWarRescue.matchStrength(a,b),"Physical equal-FP matching failed");
  check(a.getFleetPoints()==b.getFleetPoints()&&a.getFleetPoints()==90,"Matching created unequal rosters");
  a=roster(100);b=roster(9,9);check(!PTSDWarRescue.matchStrength(a,b),"Invalid match should decline generation");
  SectorAPI old=Global.getSector();SettingsAPI settings=Global.getSettings();
  try{
   Constructor<PTSDCrisisState> c=PTSDCrisisState.class.getDeclaredConstructor();c.setAccessible(true);PTSDCrisisState state=c.newInstance();state.late=new PTSDLateState();state.phase=PTSDCrisisState.Phase.WAR;
   Map<String,Object> persistent=new HashMap<String,Object>();
   CampaignFleetAPI player=ControlledPatrolRouteTest.proxy(CampaignFleetAPI.class,(n,x)->n.equals("isInHyperspace")?false:null);
   Global.setSector(ControlledPatrolRouteTest.proxy(SectorAPI.class,(n,x)->n.equals("getPersistentData")?persistent:n.equals("getPlayerFleet")?player:null));
   PTSDWarRescue.advance(state,50);check(PTSDWarRescue.store().nextRoll==0,"In-system visit consumed hyper-only probability window");
   StarSystemAPI sys=ControlledPatrolRouteTest.proxy(StarSystemAPI.class,(n,x)->n.equals("getId")?"ally":null);
   FactionAPI faction=ControlledPatrolRouteTest.proxy(FactionAPI.class,(n,x)->n.equals("getId")?"independent":null);
   SectorEntityToken port=ControlledPatrolRouteTest.proxy(SectorEntityToken.class,(n,x)->n.equals("getContainingLocation")?sys:n.equals("getLocation")?new Vector2f():null);
   MarketAPI market=ControlledPatrolRouteTest.proxy(MarketAPI.class,(n,x)->n.equals("getPrimaryEntity")?port:n.equals("getStarSystem")?sys:n.equals("getFactionId")?"independent":n.equals("getMemoryWithoutUpdate")?SceneRevisionTest.memory(new HashMap<String,Object>()):null);
   EconomyAPI economy=ControlledPatrolRouteTest.proxy(EconomyAPI.class,(n,x)->n.equals("getMarket")?market:null);
   final Vector2f location=new Vector2f(3000,0);
   FleetDataAPI fd=ControlledPatrolRouteTest.proxy(FleetDataAPI.class,(n,x)->n.equals("getMembersListCopy")?new ArrayList<FleetMemberAPI>():null);
   CampaignFleetAPI survivor=ControlledPatrolRouteTest.proxy(CampaignFleetAPI.class,(n,x)->n.equals("getId")?"survivor":n.equals("getLocation")?location:n.equals("getContainingLocation")?sys:n.equals("getFaction")?faction:n.equals("getFleetData")?fd:n.equals("isAlive")?true:n.equals("getFleetPoints")?45:n.equals("getMemoryWithoutUpdate")?SceneRevisionTest.memory(new HashMap<String,Object>()):null);
   Global.setSector(ControlledPatrolRouteTest.proxy(SectorAPI.class,(n,x)->n.equals("getEconomy")?economy:n.equals("getEntityById")?survivor:null));
   Global.setSettings(ControlledPatrolRouteTest.proxy(SettingsAPI.class,(n,x)->null));
   state.late.coalitionMembers.add("independent");PTSDWarRescue.Operation o=new PTSDWarRescue.Operation();o.id="test";o.systemId="source";o.humans.add("survivor");o.destinations.put("survivor","port");
   PTSDWarRescue.update(state,o,1);check(state.late.reserves.isEmpty(),"Defense granted before arrival");
   location.x=0;PTSDWarRescue.update(state,o,2);check(state.late.reserves.size()==1&&state.late.reserves.get(0).strength==45,"Arrival did not preserve surviving FP");
   PTSDWarRescue.update(state,o,3);check(state.late.reserves.size()==1&&!o.rewarded,"Repeated arrival duplicated defense or rewarded spectator");
   System.out.println("WarRescueTest passed: equal physical FP, hyper gate, real arrival, survivor strength, dedup and no spectator reward");
  }finally{Global.setSector(old);Global.setSettings(settings);}
 }
}
