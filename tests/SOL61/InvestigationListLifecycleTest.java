package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Regression for stale dossier entries, actual list ordering and missing legacy scene fields. */
public final class InvestigationListLifecycleTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static PTSDCrisisState.CrisisIncident incident(String id,float day,String system){
        PTSDCrisisState.CrisisIncident i=new PTSDCrisisState.CrisisIncident();
        i.id=id;i.cardId="SITE-01";i.createdDay=day;i.targetSystemId=system;
        i.investigable=true;i.recordedByPlayer=true;i.investigationExpiresDay=50f;return i;
    }
    @SuppressWarnings("unchecked") static <T>T reload(T value)throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        new ObjectOutputStream(bytes).writeObject(value);
        return (T)new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();
    }
    public static void main(String[] args)throws Exception {
        SectorAPI old=Global.getSector();SettingsAPI oldSettings=Global.getSettings();
        try {
            Global.setSettings(NewsSceneMechanicsTest.mock(SettingsAPI.class,n->null));
            CampaignFleetAPI player=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,n->n.equals("getLocationInHyperspace")?new Vector2f(0,0):null);
            Map<String,StarSystemAPI> systems=new HashMap<>();
            systems.put("near",NewsSceneMechanicsTest.mock(StarSystemAPI.class,n->n.equals("getLocation")?new Vector2f(3,4):null));
            systems.put("far",NewsSceneMechanicsTest.mock(StarSystemAPI.class,n->n.equals("getLocation")?new Vector2f(30,40):null));
            Global.setSector(ControlledPatrolRouteTest.proxy(SectorAPI.class,(n,a)->{
                if(n.equals("getPlayerFleet"))return player;
                if(n.equals("getStarSystem"))return systems.get(a[0]);
                if(n.equals("getStarSystems"))return new ArrayList<StarSystemAPI>(systems.values());
                return null;
            }));
            Constructor<PTSDCrisisState> constructor=PTSDCrisisState.class.getDeclaredConstructor();constructor.setAccessible(true);
            PTSDCrisisState state=constructor.newInstance();state.progressInitialized=true;
            PTSDCrisisState.CrisisIncident near=incident("near",1,"near"),far=incident("far",3,"far"),unknown=incident("unknown",2,"missing");
            state.incidents.addAll(Arrays.asList(near,unknown,far));
            check(PTSDInvestigationList.incidents(state,PTSDInvestigationList.Order.NEWEST,5).get(0)==far,"Newest order");
            check(PTSDInvestigationList.incidents(state,PTSDInvestigationList.Order.OLDEST,5).get(0)==near,"Oldest order");
            check(PTSDInvestigationList.incidents(state,PTSDInvestigationList.Order.NEAREST,5).get(0)==near,"Nearest order");
            List<PTSDCrisisState.CrisisIncident> sorted=PTSDInvestigationList.incidents(state,PTSDInvestigationList.Order.FARTHEST,5);
            check(sorted.get(0)==far&&sorted.get(2)==unknown,"Farthest/unknown order");
            check(state.incidents.get(0)==near&&state.incidents.get(1)==unknown,"Sorting mutated simulation order");
            near.investigationResolved=true;near.investigationReal=true;near.investigationResult=PTSDTaskRules.InvestigationResult.CONFIRMED;
            near.scenarioTrigger="TEST";near.scenarioDeadline=60;
            check(PTSDInvestigationList.visible(near,55),"Unfinished follow-up lost to base deadline");
            check(!PTSDInvestigationList.visible(near,60),"Expired follow-up remains");
            near.investigationReportsAwarded=3;check(!PTSDInvestigationList.visible(near,5),"Rewarded remains");
            near.investigationReportsAwarded=0;near.investigationResult=PTSDTaskRules.InvestigationResult.TARGET_LOST;
            check(!PTSDInvestigationList.visible(near,5),"Lost target remains");
            near.investigationResult=PTSDTaskRules.InvestigationResult.EXPIRED;
            check(!PTSDInvestigationList.visible(near,5),"Expired result remains");
            far.investigationAbandoned=true;check(!PTSDInvestigationList.visible(far,5),"Abandoned remains");

            // Reproduce the supplied crash before the state loader has had a chance to repair it.
            PTSDCrisisState.CrisisIncident legacy=incident("legacy",0,"near");
            legacy.scene=new PTSDNewsScene.State();legacy.scene.portEvidence=null;
            legacy.scene.nodes=null;legacy.scene.owned=null;legacy.scene.fleets=null;legacy.scene.pulsed=null;
            legacy.scene.visited.add("kept");legacy.scene.branch=2;legacy.scene.secondsLeft=7;
            legacy=reload(legacy);
            check(!PTSDInvestigationRewards.hasEvidence(legacy),"Missing evidence fabricated or crashed");
            check(!PTSDInvestigationRewards.hasEvidence(null),"Null incident");
            state.incidents.add(legacy);state.invalidateRuntime();state.ensureRuntimeReady();
            check(legacy.scene.portEvidence!=null&&legacy.scene.nodes!=null&&legacy.scene.owned!=null&&
                    legacy.scene.fleets!=null&&legacy.scene.pulsed!=null,"Nested migration incomplete");
            check(legacy.scene.visited.contains("kept")&&legacy.scene.branch==2&&legacy.scene.secondsLeft==7,"Migration reset progress");
            Set<String> evidence=legacy.scene.portEvidence;evidence.add("record");state.ensureRuntimeReady();
            check(legacy.scene.portEvidence==evidence&&PTSDInvestigationRewards.hasEvidence(legacy),"Repair discarded evidence");
            legacy=reload(legacy);check(legacy.scene.portEvidence.contains("record"),"Evidence not persistent");
            PTSDCrisisIntel intel=new PTSDCrisisIntel();
            java.lang.reflect.Field sort=PTSDCrisisIntel.class.getDeclaredField("sortOrder");sort.setAccessible(true);
            sort.set(intel,PTSDInvestigationList.Order.FARTHEST);
            check(!java.lang.reflect.Modifier.isTransient(sort.getModifiers())&&
                    reload(sort.get(intel))==PTSDInvestigationList.Order.FARTHEST,"Sort choice cannot be persisted");
            // Exercise the reported abandon path, rather than only calling the evidence helper.
            legacy.scene.portEvidence=null;legacy.scene.samples=0;legacy.scene.visited.clear();
            PTSDCrisisIntel.abandon(legacy);
            check(legacy.investigationAbandoned&&!PTSDInvestigationList.visible(legacy,5),"Legacy abandon failed");
            System.out.println("InvestigationListLifecycleTest: passed (four orders, terminal filtering, follow-up, legacy null fields, persistence)");
        } finally {Global.setSector(old);Global.setSettings(oldSettings);}
    }
}
