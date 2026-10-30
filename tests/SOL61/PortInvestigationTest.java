package data.scripts.campaign.invasion;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import java.io.*;
import java.util.*;
import java.lang.reflect.Constructor;

/** Evidence survives save/load; market entry aliases cannot bypass or duplicate the case. */
public final class PortInvestigationTest {
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        for(String id:Arrays.asList("R2-03","R2-08","R2-12","F-HEG-01","F-PL-01","F2-11","F2-15")){
            check(PTSDPortInvestigation.supports(id),"Missing port "+id);
            check(new HashSet<String>(Arrays.asList(PTSDPortInvestigationText.get(id).actions)).size()==3,"Duplicate actions "+id);
        }
        check(!PTSDPortInvestigation.supports("F-TT-01"),"Pure report became investigation");
        for(int branch=0;branch<3;branch++){
            PTSDNewsScene.State s=new PTSDNewsScene.State();s.branch=branch;s.nodes.addAll(Arrays.asList("home","other"));
            check(!PTSDPortInvestigation.collect(s,2,"home"),"Conclusion without sources");
            check(!PTSDPortInvestigation.collect(s,0,"other"),"Wrong port accepted original");
            check(PTSDPortInvestigation.collect(s,0,"home"),"Original missing");
            check(!PTSDPortInvestigation.collect(s,0,"home"),"Duplicate evidence");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();new ObjectOutputStream(bytes).writeObject(s);
            s=(PTSDNewsScene.State)new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();
            check(s.samples==1&&!PTSDPortInvestigation.available(s,0,"home"),"Reload reset evidence");
            check(PTSDPortInvestigation.collect(s,1,"other"),"Independent source unavailable");
            check(!PTSDPortInvestigation.collect(s,2,"other"),"Wrong port accepted analysis");
            check(PTSDPortInvestigation.collect(s,2,"home")&&s.samples==3,"Conclusion unavailable");
            s.resolved=true;check(!PTSDPortInvestigation.collect(s,0,"home"),"Resolved case changed");
        }
        PTSDNewsScene.State single=new PTSDNewsScene.State();single.nodes.add("only");single.portEvidence=null;
        for(int n=0;n<3;n++)check(PTSDPortInvestigation.collect(single,n,"only"),"Single port fallback failed");
        SectorAPI previous=Global.getSector();SettingsAPI old=Global.getSettings();
        try{
            Constructor<PTSDCrisisState> ctor=PTSDCrisisState.class.getDeclaredConstructor();ctor.setAccessible(true);
            PTSDCrisisState state=ctor.newInstance();state.progressInitialized=true;
            Map<String,Object> persistent=new HashMap<>();persistent.put(PTSDCrisisState.PERSISTENT_KEY,state);
            Global.setSettings(NewsSceneMechanicsTest.mock(SettingsAPI.class,n->null));
            Global.setSector(NewsSceneMechanicsTest.mock(SectorAPI.class,n->n.equals("getPersistentData")?persistent:null));
            SectorEntityToken primary=NewsSceneMechanicsTest.mock(SectorEntityToken.class,n->n.equals("getId")?"home":null);
            MarketAPI market=NewsSceneMechanicsTest.mock(MarketAPI.class,n->n.equals("getPrimaryEntity")?primary:null);
            SectorEntityToken station=NewsSceneMechanicsTest.mock(SectorEntityToken.class,n->n.equals("getId")?"station":n.equals("getMarket")?market:null);
            for(int n=0;n<2;n++){
                PTSDCrisisState.CrisisIncident i=new PTSDCrisisState.CrisisIncident();i.id="port"+n;i.cardId=n==0?"R2-03":"R2-08";
                i.recordedByPlayer=true;i.siteMaterialized=true;i.scene=new PTSDNewsScene.State();i.scene.kind=PTSDNewsScene.Kind.PORT_RECORDS;i.scene.nodes.add("home");
                state.incidents.add(i);check(PTSDNewsScene.boundNode(i,station)==primary,"Station entry not canonical");
            }
            InteractionDialogPlugin plugin=PTSDNewsScene.dialogFor(station);
            check(plugin instanceof PTSDNewsInteractionMenu,"Priority shared investigation menu missing");
            List<Object> options=new ArrayList<>();
            OptionPanelAPI panel=ControlledPatrolRouteTest.proxy(OptionPanelAPI.class,(n,a)->{if(n.equals("addOption"))options.add(a[1]);return null;});
            plugin.init(NewsSceneMechanicsTest.mock(InteractionDialogAPI.class,n->n.equals("getOptionPanel")?panel:n.equals("getTextPanel")?NewsSceneMechanicsTest.mock(TextPanelAPI.class,k->null):null));
            check(options.containsAll(Arrays.asList("news:0","news:1","normal","leave")),"Multi-case/normal menu missing");
            final com.fs.starfarer.api.EveryFrameScript[] reopen=new com.fs.starfarer.api.EveryFrameScript[1];
            final boolean[] normalOpened={false};
            CampaignUIAPI ui=ControlledPatrolRouteTest.proxy(CampaignUIAPI.class,(n,a)->{
                if(n.equals("showInteractionDialog")){check(PTSDNewsScene.dialogFor(station)==null,"Normal entry recursively intercepted");normalOpened[0]=true;}
                return null;
            });
            Global.setSector(ControlledPatrolRouteTest.proxy(SectorAPI.class,(n,a)->{
                if(n.equals("addTransientScript"))reopen[0]=(com.fs.starfarer.api.EveryFrameScript)a[0];
                return n.equals("getPersistentData")?persistent:n.equals("getCampaignUI")?ui:null;
            }));
            PTSDPortInvestigation.openNormal(NewsSceneMechanicsTest.mock(InteractionDialogAPI.class,n->null),station);
            check(reopen[0]!=null,"Normal reopen not scheduled");reopen[0].advance(.01f);
            check(normalOpened[0]&&!PTSDPortInvestigation.openingNormal(),"Normal reopen/bypass cleanup failed");
            check(PTSDNewsScene.dialogFor(station) instanceof PTSDNewsInteractionMenu,"Bypass remained on next visit");
            state.incidents.get(0).scene.resolved=true;check(!PTSDNewsScene.hasActiveEntry(state.incidents.get(0),station),"Completed case stayed active");
            check(PTSDNewsScene.hasActiveEntry(state.incidents.get(1),station),"Completing one removed another");
            for(String id:Arrays.asList("E-01","E-02","E-03","E-04")){
                for(int branch=0;branch<3;branch++){
                    PTSDCrisisState.CrisisIncident i=new PTSDCrisisState.CrisisIncident();i.id=id+branch;i.cardId=id;
                    i.scene=new PTSDNewsScene.State();i.scene.kind=PTSDNewsScene.Kind.SPATIAL_ECHO;i.scene.branch=branch;
                    i.scene.nodes.addAll(Arrays.asList("home","other"));
                    PTSDNewsScene.sample(state,i,primary);PTSDNewsScene.sample(state,i,primary);
                    check(i.scene.samples==1,"Expansion repeated node counted twice");
                    if(id.equals("E-01")||id.equals("E-02"))check(!i.scene.resolved,"Choice scene auto-ended");
                    SectorEntityToken other=NewsSceneMechanicsTest.mock(SectorEntityToken.class,n->n.equals("getId")?"other":null);
                    PTSDNewsScene.sample(state,i,other);
                    if(id.equals("E-01")&&branch<2)check(i.scene.recordTaken&&!i.scene.resolved,"Survey recovery lost player choice");
                    if(id.equals("E-03")&&branch==0)check(i.investigationResult==PTSDTaskRules.InvestigationResult.DISPROVED,"Calibration not disproved");
                    if(id.equals("E-03")&&branch==2)check(i.scene.timerArmed&&!i.scene.resolved,"Reverse location prematurely concluded");
                    if(id.equals("E-04"))check(i.scene.resolved,"Relay comparison did not conclude");
                    if(id.equals("E-04")&&branch==2)check(i.scene.responseQueued,"Relay upload omitted response");
                }
            }
        }finally{Global.setSector(previous);Global.setSettings(old);}
        check(!PTSDExpansionScenes.handles("E2-01")&&!PTSDExpansionScenes.handles("E2-15"),"E2 report gained scene");
        check(!PTSDExpansionScenes.accepts("E-03",NewsSceneMechanicsTest.mock(StarSystemAPI.class,n->null)),"Planetless albedo site accepted");
        System.out.println("PortInvestigationTest: passed");
    }
}
