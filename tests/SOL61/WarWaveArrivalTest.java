package data.scripts.campaign.invasion;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.*;
import org.lwjgl.util.vector.Vector2f;
import java.lang.reflect.*;

/** Transit, arrival and combat gates run against the actual wave coordinator. */
public final class WarWaveArrivalTest {
    public static void main(String[] args)throws Exception{
        SectorAPI previous=Global.getSector();
        com.fs.starfarer.api.SettingsAPI oldSettings=Global.getSettings();
        try{
            Global.setSettings(NewsSceneMechanicsTest.mock(com.fs.starfarer.api.SettingsAPI.class,n->null));
            Constructor<PTSDCrisisState> ctor=PTSDCrisisState.class.getDeclaredConstructor();ctor.setAccessible(true);
            PTSDCrisisState state=ctor.newInstance();
            StarSystemAPI system=NewsSceneMechanicsTest.mock(StarSystemAPI.class,n->n.equals("getId")?"target":null);
            SectorEntityToken target=NewsSceneMechanicsTest.mock(SectorEntityToken.class,n->n.equals("getContainingLocation")?system:n.equals("getLocation")?new Vector2f():null);
            MarketAPI market=NewsSceneMechanicsTest.mock(MarketAPI.class,n->n.equals("getPrimaryEntity")?target:null);
            EconomyAPI economy=NewsSceneMechanicsTest.mock(EconomyAPI.class,n->n.equals("getMarket")?market:null);
            Vector2f position=new Vector2f(6000,0);boolean[] battle={false};float[] fp={80};
            BattleAPI combat=NewsSceneMechanicsTest.mock(BattleAPI.class,n->null);
            CampaignFleetAPI fleet=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,n->n.equals("getContainingLocation")?system:n.equals("getLocation")?position:n.equals("getFleetPoints")?(int)fp[0]:n.equals("getBattle")&&battle[0]?combat:null);
            Global.setSector(NewsSceneMechanicsTest.mock(SectorAPI.class,n->n.equals("getEconomy")?economy:n.equals("getEntityById")?fleet:null));
            IIRT_Omega_Invasion invasion=new IIRT_Omega_Invasion();Field index=IIRT_Omega_Invasion.class.getDeclaredField("eventFleetIndexReady");index.setAccessible(true);index.set(invasion,true);
            Method update=IIRT_Omega_Invasion.class.getDeclaredMethod("updateWarWave",PTSDCrisisState.class,PTSDCrisisState.StrategicEvent.class,float.class);update.setAccessible(true);
            PTSDCrisisState.StrategicEvent e=new PTSDCrisisState.StrategicEvent();e.type=PTSDCrisisState.EventType.ATTACK;e.status=PTSDCrisisState.EventStatus.MATERIALIZED;
            e.physicalWarWave=true;e.targetMarketId="market";e.materializedFleetIds.add("fleet");e.strength=100;e.musterUntilDay=3;e.resolveDay=83;
            update.invoke(invasion,state,e,10f);
            NewsSceneMechanicsTest.check(e.arrivalDay==0&&e.resolveDay==83&&e.strength==80,"Transit advanced siege or losses ignored");
            position.x=500;update.invoke(invasion,state,e,12f);
            NewsSceneMechanicsTest.check(e.arrivalDay==12&&e.resolveDay==17,"Arrival failed to start five-day siege");
            fp[0]=30;battle[0]=true;update.invoke(invasion,state,e,17f);
            NewsSceneMechanicsTest.check(e.strength==30&&e.resolveDay>=18,"Ongoing battle resolved or survivors ignored");
            e.type=PTSDCrisisState.EventType.FIRE_PROBE;e.arrivalDay=0;battle[0]=false;update.invoke(invasion,state,e,20f);
            NewsSceneMechanicsTest.check(e.resolveDay==22,"Feint did not use shorter contact window");
            NewsSceneMechanicsTest.check(PTSDWarWaveReturn.refund(300,100)==50&&PTSDWarWaveReturn.refund(30,100)==15&&PTSDWarWaveReturn.refund(0,100)==0,"War return exceeded paid/surviving share");
            System.out.println("WarWaveArrivalTest: passed (transit gate, actual FP losses, arrival clock, battle postponement, feint window)");
        }finally{Global.setSector(previous);Global.setSettings(oldSettings);}
    }
}
