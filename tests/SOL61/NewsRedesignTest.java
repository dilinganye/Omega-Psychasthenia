package data.scripts.campaign.invasion;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import java.util.*;
import java.lang.reflect.*;
import java.io.*;

/** Exercises routing edits and restoration against live destination lists, not a copied formula. */
public final class NewsRedesignTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static final Map<String,SectorEntityToken> entities=new HashMap<>();
    static JumpPointAPI jump(String id,boolean hyper,StarSystemAPI system,List<JumpPointAPI.JumpDestination> destinations){
        com.fs.starfarer.api.campaign.rules.MemoryAPI memory=SceneRevisionTest.memory(new HashMap<>());
        JumpPointAPI j=ControlledPatrolRouteTest.proxy(JumpPointAPI.class,(name,args)->{
            if(name.equals("getId"))return id;
            if(name.equals("isInHyperspace"))return hyper;
            if(name.equals("getStarSystem")||name.equals("getContainingLocation"))return system;
            if(name.equals("getMemoryWithoutUpdate"))return memory;
            if(name.equals("getLocation"))return new Vector2f();
            if(name.equals("getDestinations"))return destinations;
            if(name.equals("removeDestination")){destinations.removeIf(d->d.getDestination()==args[0]);return null;}
            if(name.equals("addDestination")){destinations.add((JumpPointAPI.JumpDestination)args[0]);return null;}
            return null;
        });entities.put(id,j);return j;
    }
    public static void main(String[] args)throws Exception{
        check(PTSDCrisisIncidentManager.eventWeight("")==1&&PTSDCrisisIncidentManager.eventWeight("  ")==1,"Blank multiplier changed");
        check(PTSDCrisisIncidentManager.eventWeight("0")==0&&PTSDCrisisIncidentManager.eventWeight(".25")==.25f,"Weights not parsed");
        for(String bad:new String[]{"-1","NaN","Infinity","invalid"}){try{PTSDCrisisIncidentManager.eventWeight(bad);throw new AssertionError("Invalid weight accepted");}catch(IllegalArgumentException expected){}}
        int trueCount=0;for(int n=0;n<10000;n++)if(PTSDTaskRules.ordinaryNewsOutcome((n+.5f)/10000f,.9f)==1)trueCount++;
        check(trueCount==9000,"Truth probability not ninety percent");
        check(PTSDTaskRules.ordinaryNewsOutcome(0f,0f)==2&&PTSDTaskRules.ordinaryNewsOutcome(.999f,1f)==1,"Truth difficulty endpoints");
        check(data.scripts.PTSDDifficultySettings.managed("PTSD_news_true_chance"),"Truth excluded from difficulty snapshots");
        PTSDCrisisNewsIntel intel=new PTSDCrisisNewsIntel();Field auto=PTSDCrisisNewsIntel.class.getDeclaredField("autoOpenInvestigation");auto.setAccessible(true);
        check(!auto.getBoolean(intel),"New news defaults to auto-navigation");
        final int[] refresh={0};IntelUIAPI ui=ControlledPatrolRouteTest.proxy(IntelUIAPI.class,(n,a)->{if(n.equals("updateUIForItem"))refresh[0]++;return null;});
        intel.buttonPressConfirmed("PTSD_NEWS_AUTO_OPEN",ui);check(auto.getBoolean(intel)&&refresh[0]==1,"Toggle did not enable");
        intel.buttonPressConfirmed("PTSD_NEWS_AUTO_OPEN",ui);check(!auto.getBoolean(intel)&&refresh[0]==2,"Toggle did not disable");
        check(PTSDJumpBlockade.deadline(30,-1)==30&&PTSDJumpBlockade.deadline(30,5)==25&&PTSDJumpBlockade.deadline(30,29)==30,"Cancel extends blockade");
        SectorAPI previous=Global.getSector();SettingsAPI settings=Global.getSettings();
        try{
            Constructor<PTSDCrisisState> ctor=PTSDCrisisState.class.getDeclaredConstructor();ctor.setAccessible(true);PTSDCrisisState state=ctor.newInstance();state.progressInitialized=true;
            Map<String,Object> persistent=new HashMap<>();persistent.put(PTSDCrisisState.PERSISTENT_KEY,state);
            Global.setSettings(NewsSceneMechanicsTest.mock(SettingsAPI.class,n->null));
            CampaignClockAPI clock=NewsSceneMechanicsTest.mock(CampaignClockAPI.class,n->null);
            Global.setSector(ControlledPatrolRouteTest.proxy(SectorAPI.class,(n,a)->n.equals("getEntityById")?entities.get(a[0]):n.equals("getClock")?clock:n.equals("getPersistentData")?persistent:null));
            List<SectorEntityToken> jumps=new ArrayList<>();StarSystemAPI sys=NewsSceneMechanicsTest.mock(StarSystemAPI.class,n->n.equals("getJumpPoints")?jumps:null);
            List<JumpPointAPI.JumpDestination> a=new ArrayList<>(),b=new ArrayList<>(),c=new ArrayList<>(),d=new ArrayList<>();
            JumpPointAPI in=jump("in",false,sys,a),out=jump("out",true,sys,b),alternate=jump("alternate",false,sys,c),outside2=jump("outside2",true,sys,d);
            JumpPointAPI.JumpDestination original=new JumpPointAPI.JumpDestination(out,"Original label");original.setMinDistFromToken(123);original.setMaxDistFromToken(456);
            a.add(original);b.add(new JumpPointAPI.JumpDestination(in,"In"));jumps.add(in);
            check(PTSDJumpBlockade.pick(sys)==null,"Single-exit system accepted");
            c.add(new JumpPointAPI.JumpDestination(outside2,"Other"));d.add(new JumpPointAPI.JumpDestination(alternate,"Back"));jumps.add(alternate);
            check(PTSDJumpBlockade.pick(sys)==in,"Valid paired jump rejected");
            PTSDCrisisState.CrisisIncident i=new PTSDCrisisState.CrisisIncident();i.id="blockade";i.scene=new PTSDNewsScene.State();i.scene.kind=PTSDNewsScene.Kind.JUMP_BLOCKADE;
            check(PTSDJumpBlockade.build(i,sys),"Blockade build failed");
            check(a.isEmpty()&&b.isEmpty()&&c.size()==1&&d.size()==1,"Wrong destinations blocked");
            check(i.scene.owned.isEmpty(),"Blockade took ownership of world jumps");
            check(PTSDJumpBlockade.pick(sys)==null,"Concurrent blockade could seal last exit");
            // Another mod adding a destination during the blockade must survive restoration.
            JumpPointAPI.JumpDestination foreign=new JumpPointAPI.JumpDestination(alternate,"New route");a.add(foreign);
            PTSDJumpBlockade.restore(i);PTSDJumpBlockade.restore(i);
            check(a.size()==2&&a.contains(original)&&a.contains(foreign)&&b.size()==1,"Restore duplicated or discarded a route");
            check(original.getMinDistFromToken()==123&&original.getMaxDistFromToken()==456,"Destination parameters lost");
            check(!Misc.isImportantForReason(in.getMemoryWithoutUpdate(),"PTSD_scene_blockade"),"Completed jump retained scene marker");
            List<CampaignFleetAPI> traffic=new ArrayList<>();LocationAPI loc=NewsSceneMechanicsTest.mock(LocationAPI.class,n->n.equals("getFleets")?traffic:null);
            check(PTSDJumpBlockade.clearOfOthers(loc,new Vector2f(),null),"Empty exit rejected");
            CampaignFleetAPI npc=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,n->n.equals("isAlive")?true:n.equals("getLocation")?new Vector2f(200,0):null);traffic.add(npc);
            check(!PTSDJumpBlockade.clearOfOthers(loc,new Vector2f(),null),"NPC spawn collision accepted");
            check(PTSDJumpBlockade.clearOfOthers(loc,new Vector2f(),npc),"Player blocks materialization");
            PTSDReconScenes.Progress progress=new PTSDReconScenes.Progress();progress.seconds=35;progress.observed=.15f;progress.escaping=true;progress.enemy="fleet-id";
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();new ObjectOutputStream(bytes).writeObject(progress);
            PTSDReconScenes.Progress restored=(PTSDReconScenes.Progress)new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();
            check(restored.seconds==35&&restored.observed==.15f&&restored.escaping&&"fleet-id".equals(restored.enemy),"Encounter progress reset on serialization");
        }finally{Global.setSector(previous);Global.setSettings(settings);}
        System.out.println("NewsRedesignTest: passed (weights, truth, UI toggle, jump prerequisites/blockade/restore, spawn safety, persistence)");
    }
}
