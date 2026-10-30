package data.scripts.campaign.invasion;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.impl.campaign.rulecmd.PTSDNewsSceneEntry;
import java.util.*;
import java.lang.reflect.*;

public final class SceneRevisionTest {
    static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    static MemoryAPI memory(Map<String,Object> values){return ControlledPatrolRouteTest.proxy(MemoryAPI.class,(n,a)->{
        if(n.equals("get")||n.equals("getString"))return values.get(a[0]);
        if(n.equals("contains"))return values.containsKey(a[0]);
        if(n.equals("getBoolean"))return Boolean.TRUE.equals(values.get(a[0]));
        if(n.equals("set")){values.put((String)a[0],a[1]);return null;}
        if(n.equals("unset")){values.remove(a[0]);return null;}
        return null;
    });}
    public static void main(String[] args)throws Exception{
        check("D-03:2".equals(PTSDSceneDevIntel.normalizeRequest("d-03：2")),"Direct scene request normalization");
        check("JE-01:0".equals(PTSDSceneDevIntel.normalizeRequest("JE-01")),"Default branch");
        check(PTSDSceneDevIntel.normalizeRequest("D-01:100")==null,"Rare branch leaked to other card");
        check(PTSDSceneDevIntel.normalizeRequest("D-01:3")==null,"Unsupported branch accepted");
        check(PTSDSceneDevIntel.normalizeRequest("D-01:")==null,"Empty branch accepted");
        SectorAPI old=Global.getSector();SettingsAPI settings=Global.getSettings();
        try{
            Constructor<PTSDCrisisState> constructor=PTSDCrisisState.class.getDeclaredConstructor();constructor.setAccessible(true);
            PTSDCrisisState state=constructor.newInstance();state.progressInitialized=true;
            Map<String,Object> persistent=new HashMap<>();persistent.put(PTSDCrisisState.PERSISTENT_KEY,state);
            Map<String,SectorEntityToken> entities=new HashMap<>();
            List<MarketAPI> markets=new ArrayList<>();
            StarSystemAPI sys=NewsSceneMechanicsTest.mock(StarSystemAPI.class,n->n.equals("getName")?"Test sector":null);
            EconomyAPI economy=NewsSceneMechanicsTest.mock(EconomyAPI.class,n->n.equals("getMarkets")?markets:null);
            Global.setSettings(NewsSceneMechanicsTest.mock(SettingsAPI.class,n->null));
            CampaignClockAPI clock=NewsSceneMechanicsTest.mock(CampaignClockAPI.class,n->null);
            Global.setSector(ControlledPatrolRouteTest.proxy(SectorAPI.class,(n,a)->n.equals("getClock")?clock:n.equals("getPersistentData")?persistent:n.equals("getEntityById")?entities.get(a[0]):n.equals("getEconomy")?economy:null));
            MemoryAPI mem=memory(new HashMap<>());
            SectorEntityToken relay=ControlledPatrolRouteTest.proxy(SectorEntityToken.class,(n,a)->n.equals("getId")?"relay":n.equals("getStarSystem")?sys:n.equals("getMemoryWithoutUpdate")?mem:null);entities.put("relay",relay);
            PTSDCrisisState.CrisisIncident first=new PTSDCrisisState.CrisisIncident();first.id="one";first.cardId="D-03";first.headline="A";first.recordedByPlayer=true;first.investigationOutcome=1;first.siteMaterialized=true;
            first.scene=new PTSDNewsScene.State();first.scene.kind=PTSDNewsScene.Kind.RELAY_PARASITE;
            PTSDNewsScene.bind(first,relay);state.incidents.add(first);
            PTSDCrisisState.CrisisIncident second=new PTSDCrisisState.CrisisIncident();second.id="two";second.cardId="JE-01";second.headline="B";second.recordedByPlayer=true;second.investigationOutcome=1;second.siteMaterialized=true;
            second.scene=new PTSDNewsScene.State();second.scene.kind=PTSDNewsScene.Kind.SIGNAL_PURSUIT;
            PTSDNewsScene.bind(second,relay);state.incidents.add(second);
            check(PTSDNewsScene.dialogFor(relay)!=null,"Shared relay lacks priority entry without rules");
            check(PTSDNewsScene.hasActiveEntry(first,relay),"Active shared node rejected");
            first.scene.visited.add("relay");
            check(PTSDNewsScene.hasActiveEntry(first,relay),"Sampling prematurely removed priority entry");
            first.scene.visited.clear();
            check(first.scene.owned.isEmpty()&&second.scene.owned.isEmpty(),"World object became cleanup-owned");
            check(Misc.isImportantForReason(mem,"PTSD_scene_one")&&Misc.isImportantForReason(mem,"PTSD_scene_two"),"Task importance reasons collided");
            List<Object> options=new ArrayList<>();
            OptionPanelAPI panel=ControlledPatrolRouteTest.proxy(OptionPanelAPI.class,(n,a)->{if(n.equals("addOption"))options.add(a[1]);return null;});
            InteractionDialogAPI dialog=ControlledPatrolRouteTest.proxy(InteractionDialogAPI.class,(n,a)->n.equals("getInteractionTarget")?relay:n.equals("getOptionPanel")?panel:null);
            Map<String,MemoryAPI> map=new HashMap<>();map.put("local",memory(new HashMap<>()));
            new PTSDNewsSceneEntry().execute("test",dialog,Misc.tokenize("populate"),map);
            check(options.contains("PTSD_scene_entry:one")&&options.contains("PTSD_scene_entry:two"),"Coexisting scene menu lost an entry");
            PTSDCrisisIntel.abandon(first);
            check(!PTSDCrisisIntel.canTrackIncident(first)&&first.investigationResult==PTSDTaskRules.InvestigationResult.INCONCLUSIVE,"Abandon treated as success or retained navigation");
            check(!Misc.isImportantForReason(mem,"PTSD_scene_one")&&Misc.isImportantForReason(mem,"PTSD_scene_two"),"Cancelling one scene erased the other marker");
            check(!PTSDNewsScene.hasActiveEntry(first,relay),"Abandoned investigation still overrides world dialog");
            check(PTSDNewsScene.dialogFor(relay)!=null,"Abandoning one task hides another on shared facility");
            second.scene.nodes.add("other");second.scene.visited.add("relay");second.scene.samples=1;
            check(PTSDNewsSceneText.sampling(second.scene).contains("1 个目标"),"Remaining sample count wrong");
            check(PTSDSceneDevIntel.cleanSelected(state,second).contains("仅清理"),"Natural scene cleanup allowed");
            second.devForced=true;second.scene.owned.add("relay");
            String cleaned=PTSDSceneDevIntel.cleanSelected(state,second);
            check(second.scene.cleaned&&cleaned.contains("保留被其他事件引用对象 1"),"Shared entity not protected by cleanup");
            check(entities.containsKey("relay"),"Cleanup deleted shared facility");
            check(!PTSDNewsScene.wreckMode("D-01",0).equals(PTSDNewsScene.wreckMode("D-02",0)),"Distinct wreck cases share behavior");
            MarketAPI gamma=NewsSceneMechanicsTest.mock(MarketAPI.class,n->n.equals("getFactionId")?"Gamma_Legion":null);markets.add(gamma);
            check(PTSDProtectedFaction.system(sys),"Gamma colony did not protect shared system");
            PTSDMobilizationConvoys.Record record=new PTSDMobilizationConvoys.Record();record.fleetId="delivery";record.assets=10;record.points=5;
            float before=state.late.strategicReserve,wp=state.late.warPoints;
            PTSDMobilizationConvoys.settle(state,record,true);PTSDMobilizationConvoys.settle(state,record,true);
            check(state.late.strategicReserve==before+10&&state.late.warPoints==wp+5,"Arrival not exactly once");
            PTSDMobilizationConvoys.Record lost=new PTSDMobilizationConvoys.Record();lost.fleetId="lost";lost.assets=999;lost.points=999;
            PTSDMobilizationConvoys.settle(state,lost,false);check(state.late.strategicReserve==before+10&&state.late.warPoints==wp+5,"Lost supply convoy credited");
            System.out.println("SceneRevisionTest: passed (shared world entries, owned cleanup isolation, independent markers, abandonment, sampling, Gamma, convoy settlement)");
        }finally{Global.setSector(old);Global.setSettings(settings);}
    }
}
