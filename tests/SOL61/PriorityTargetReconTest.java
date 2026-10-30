package data.scripts.campaign.invasion;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.*;
import data.scripts.IIRT_Omega_ModPlugin;
import org.lwjgl.util.vector.Vector2f;
import java.lang.reflect.*;
import java.util.*;

/** Exercises actual target resolution and the carrier report queue, not source-string assertions. */
public class PriorityTargetReconTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        SectorAPI oldSector=Global.getSector();SettingsAPI oldSettings=Global.getSettings();boolean oldLuna=IIRT_Omega_ModPlugin.hasLunaLib;
        Field registry=PTSDPriorityTargetList.class.getDeclaredField("entries");registry.setAccessible(true);Object oldEntries=registry.get(null);
        try {
            IIRT_Omega_ModPlugin.hasLunaLib=false;
            check(PTSDPrewarReconDirector.regularPhase(PTSDCrisisState.Phase.FORTIFICATION),"Fortification excluded");
            check(PTSDPrewarReconDirector.regularPhase(PTSDCrisisState.Phase.MOBILIZATION),"Mobilization excluded");
            check(!PTSDPrewarReconDirector.regularPhase(PTSDCrisisState.Phase.WAR),"Regular arrivals continued into war");
            check(!PTSDPrewarReconDirector.regularPhase(PTSDCrisisState.Phase.RECON),"Five-day arrivals started too early");
            ModManagerAPI mods=ControlledPatrolRouteTest.proxy(ModManagerAPI.class,(n,a)->n.equals("isModEnabled")?"present".equals(a[0]):null);
            Global.setSettings(NewsSceneMechanicsTest.mock(SettingsAPI.class,n->n.equals("getModManager")?mods:null));
            Constructor<PTSDCrisisState> ctor=PTSDCrisisState.class.getDeclaredConstructor();ctor.setAccessible(true);
            PTSDCrisisState state=ctor.newInstance();state.progressInitialized=true;
            Field repaired=PTSDCrisisState.class.getDeclaredField("runtimeRepaired");repaired.setAccessible(true);repaired.setBoolean(state,true);
            Map<String,Object> data=new HashMap<>();data.put(PTSDCrisisState.PERSISTENT_KEY,state);
            Map<String,SectorEntityToken> entities=new HashMap<>();List<CampaignFleetAPI> fleets=new ArrayList<>();
            StarSystemAPI system=ControlledPatrolRouteTest.proxy(StarSystemAPI.class,(n,a)->n.equals("getId")?"system":n.equals("getFleets")?fleets:null);
            PlanetAPI star=ControlledPatrolRouteTest.proxy(PlanetAPI.class,(n,a)->n.equals("isStar")?true:n.equals("getStarSystem")?system:null);
            PlanetAPI planet=ControlledPatrolRouteTest.proxy(PlanetAPI.class,(n,a)->n.equals("getStarSystem")?system:null);
            entities.put("star",star);entities.put("planet",planet);
            FactionAPI human=NewsSceneMechanicsTest.mock(FactionAPI.class,n->n.equals("getId")?"human":null);
            float[] fp={90f};
            CampaignFleetAPI defender=ControlledPatrolRouteTest.proxy(CampaignFleetAPI.class,(n,a)->n.equals("getFaction")?human:n.equals("getFleetPoints")?Integer.valueOf((int)fp[0]):n.equals("isAlive")?true:n.equals("getContainingLocation")?system:n.equals("getLocation")?new Vector2f(20000,0):null);
            fleets.add(defender);
            CampaignFleetAPI scout=ControlledPatrolRouteTest.proxy(CampaignFleetAPI.class,(n,a)->n.equals("getId")?"carrier":n.equals("getStarSystem")||n.equals("getContainingLocation")?system:n.equals("getLocation")?new Vector2f():n.equals("isAlive")?true:null);
            SectorEntityToken port=ControlledPatrolRouteTest.proxy(SectorEntityToken.class,(n,a)->n.equals("getLocation")?new Vector2f(20000,0):null);
            MarketAPI market=ControlledPatrolRouteTest.proxy(MarketAPI.class,(n,a)->n.equals("getFactionId")?"human":n.equals("getSize")?5:n.equals("getStabilityValue")?8f:n.equals("getPrimaryEntity")?port:null);
            EconomyAPI economy=NewsSceneMechanicsTest.mock(EconomyAPI.class,n->n.equals("getMarkets")?Arrays.asList(market):null);
            CampaignClockAPI clock=NewsSceneMechanicsTest.mock(CampaignClockAPI.class,n->n.equals("getTimestamp")?(long)((100-PTSDCrisisState.CAMPAIGN_DAY_EPOCH_OFFSET)*86400000d):null);
            Global.setSector(ControlledPatrolRouteTest.proxy(SectorAPI.class,(n,a)->n.equals("getEntityById")?entities.get(a[0]):n.equals("getPersistentData")?data:n.equals("getClock")?clock:n.equals("getEconomy")?economy:n.equals("getStarSystems")?Arrays.asList(system):null));
            PTSDPriorityTargetList.Entry e=new PTSDPriorityTargetList.Entry("star","test","present","source","Source");
            check(e.enabled()&&e.system()==system,"Default-on or star resolution failed");
            check(new PTSDPriorityTargetList.Entry("planet","","","source","Source").system()==null,"Planet accepted as starID");
            check(!new PTSDPriorityTargetList.Entry("star","","absent","source","Source").enabled(),"Missing dependency enabled");
            check(e.key.equals(new PTSDPriorityTargetList.Entry("star","changed","present","source","Renamed").key),"Text edit reset switch");
            PTSDPriorityTargetList.Entry other=new PTSDPriorityTargetList.Entry("star","other","","otherMod","Other");
            check(!e.key.equals(other.key),"Mod source keys collided");
            registry.set(null,Arrays.asList(e,other));check(PTSDPriorityTargetList.enabledSystems().size()==1,"Duplicate star scheduled twice");
            PTSDCrisisState.PendingReconReport report=PTSDReconReportDirector.captureCarrierSurvey(state,scout,system,true);
            check(state.pendingReconReports.isEmpty()&&!report.uploaded,"Carrier payload leaked into automatic uploads");
            check(report.fleetStrength==90 && report.marketDefense==249,"Armed survey did not capture distant deployments");
            fp[0]=999;
            check(report.fleetStrength==90,"Payload reads future deployment");
            PTSDReconReportDirector.deliverCarrierSurvey(state,report);
            int count=state.getSystemData("system").evidence.size();
            PTSDReconReportDirector.deliverCarrierSurvey(state,report);
            check(report.uploaded && count==1 && state.getSystemData("system").evidence.size()==count,"Repeated return duplicated evidence");
            check(PTSDReconReportDirector.isSurveyed(state.getSystemData("system"),100),"Armed report did not correct strategic blind spot");
            System.out.println("PriorityTargetReconTest: passed (phase gates, star identity, dependency, source keys, dedup, carrier upload and snapshot)");
        } finally {registry.set(null,oldEntries);IIRT_Omega_ModPlugin.hasLunaLib=oldLuna;Global.setSector(oldSector);Global.setSettings(oldSettings);}
    }
}
