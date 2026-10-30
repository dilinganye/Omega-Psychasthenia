package data.scripts.campaign.invasion;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.*;
import org.lwjgl.util.vector.Vector2f;
import java.lang.reflect.*;
import java.util.*;

public class VirtualWarLifecycleTest {
    static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
    public static void main(String[] args)throws Exception {
        SectorAPI oldSector=Global.getSector();SettingsAPI oldSettings=Global.getSettings();
        try {
            Global.setSettings(NewsSceneMechanicsTest.mock(SettingsAPI.class,n->null));
            Constructor<PTSDCrisisState> ctor=PTSDCrisisState.class.getDeclaredConstructor();ctor.setAccessible(true);
            PTSDCrisisState s=ctor.newInstance();s.progressInitialized=true;s.late.firstWaveSelected=true;s.late.nextRegionalOffensiveDay=Float.MAX_VALUE;
            Map<String,Object> persistent=new HashMap<>();persistent.put(PTSDCrisisState.PERSISTENT_KEY,s);
            float[] day={100};CampaignFleetAPI[] player={null};
            CampaignClockAPI clock=NewsSceneMechanicsTest.mock(CampaignClockAPI.class,n->n.equals("getTimestamp")?(long)((day[0]-PTSDCrisisState.CAMPAIGN_DAY_EPOCH_OFFSET)*86400000d):null);
            MarketAPI market=NewsSceneMechanicsTest.mock(MarketAPI.class,n->n.equals("getName")?"test colony":null);
            EconomyAPI economy=NewsSceneMechanicsTest.mock(EconomyAPI.class,n->n.equals("getMarket")?market:null);
            Global.setSector(NewsSceneMechanicsTest.mock(SectorAPI.class,n->n.equals("getPersistentData")?persistent:n.equals("getClock")?clock:n.equals("getPlayerFleet")?player[0]:n.equals("getEconomy")?economy:null));
            PTSDCrisisState.StrategicEvent e=new PTSDCrisisState.StrategicEvent();e.id="virtual";e.simulatedWarOperation=true;e.physicalWarWave=true;e.openingOperation=true;
            e.targetSystemId="target";e.targetMarketId="market";e.sourceSystemId="source";e.musterUntilDay=90;e.operationArrivalDay=100;
            e.operationOriginX=0;e.operationTargetX=20000;e.resolveDay=105;e.strength=40;s.events.add(e);
            Method update=IIRT_Omega_Invasion.class.getDeclaredMethod("updateWarWave",PTSDCrisisState.class,PTSDCrisisState.StrategicEvent.class,float.class);update.setAccessible(true);
            IIRT_Omega_Invasion host=new IIRT_Omega_Invasion();
            update.invoke(host,s,e,99f);check(!e.operationArrived&&s.late.alerts.isEmpty(),"Warning or arrival before travel completes");
            update.invoke(host,s,e,100f);update.invoke(host,s,e,101f);
            check(e.operationArrived&&e.arrivalDay==100&&s.late.alerts.size()==3,"Arrival warning missing or duplicated");
            check(s.late.fronts.containsKey("operation:"+e.id),"Arrival not connected to coalition defense");
            StarSystemAPI target=NewsSceneMechanicsTest.mock(StarSystemAPI.class,n->n.equals("getId")?"target":null);
            player[0]=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,n->n.equals("getStarSystem")?target:null);
            check(!PTSDWarCampaignDirector.near(e,player[0],99)&&PTSDWarCampaignDirector.near(e,player[0],100),"Target player materializes army before arrival");
            player[0]=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,n->n.equals("isInHyperspace")?true:n.equals("getLocation")?new Vector2f(10000,0):null);
            check(PTSDWarCampaignDirector.near(e,player[0],95)&&!PTSDWarCampaignDirector.near(e,player[0],85),"Hyperspace encounter ignores travel time");
            player[0]=null;e.status=PTSDCrisisState.EventStatus.RESOLVED;float before=s.late.strategicReserve;
            day[0]=105;PTSDWarCampaignDirector.advance(s,105,null);
            check(e.operationReturnScheduled&&e.operationReturnDay==115&&s.late.strategicReserve==before,"Return paid before reaching source");
            StarSystemAPI origin=NewsSceneMechanicsTest.mock(StarSystemAPI.class,n->n.equals("getId")?"source":null);
            CampaignFleetAPI waiting=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,n->n.equals("getStarSystem")?origin:null);
            check(!PTSDWarCampaignDirector.near(e,waiting,114)&&PTSDWarCampaignDirector.near(e,waiting,115),"Player waiting at return destination cannot intercept on arrival");
            day[0]=115;PTSDWarCampaignDirector.advance(s,115,null);PTSDWarCampaignDirector.advance(s,115,null);
            check(e.operationReturned&&s.late.strategicReserve==before,"Opening survivor created assets");
            e.operationReturned=false;e.operationReturnScheduled=false;e.status=PTSDCrisisState.EventStatus.RESOLVED;
            for(int i=0;i<170;i++){PTSDCrisisState.StrategicEvent old=new PTSDCrisisState.StrategicEvent();old.id="old"+i;old.status=PTSDCrisisState.EventStatus.RESOLVED;s.events.add(old);}
            Method trim=PTSDCrisisState.class.getDeclaredMethod("trimResolvedEvents");trim.setAccessible(true);trim.invoke(s);
            check(s.events.contains(e)&&!s.getActiveEvents().contains(e),"Returning operation pruned or reactivated for another battle");
            System.out.println("VirtualWarLifecycleTest: passed (arrival, one alert, timed proximity, return delay, exact survivor credit, history retention)");
        } finally {Global.setSector(oldSector);Global.setSettings(oldSettings);}
    }
}
