package data.scripts.campaign.invasion;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import java.lang.reflect.Constructor;
import java.io.*;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Real handler calls: observation, guarded retrieval, collection, persistence and deduplication. */
public final class ExpansionContestedTest {
    static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    static PTSDCrisisState.CrisisIncident incident(String card,int branch){
        PTSDCrisisState.CrisisIncident i=new PTSDCrisisState.CrisisIncident();i.id=card+branch;i.cardId=card;i.scene=new PTSDNewsScene.State();
        i.scene.kind=PTSDNewsScene.Kind.WRECK_RECONSTRUCTION;i.scene.branch=branch;i.scene.nodes.add("record");i.scene.nodes.add("fragment");
        return i;
    }
    public static void main(String[] args)throws Exception{
        SectorAPI previous=Global.getSector();SettingsAPI old=Global.getSettings();
        try{
            Constructor<PTSDCrisisState> ctor=PTSDCrisisState.class.getDeclaredConstructor();ctor.setAccessible(true);
            PTSDCrisisState state=ctor.newInstance();state.progressInitialized=true;
            Map<String,Object> persistent=new HashMap<>();persistent.put(PTSDCrisisState.PERSISTENT_KEY,state);
            Map<String,SectorEntityToken> entities=new HashMap<>();
            Global.setSettings(NewsSceneMechanicsTest.mock(SettingsAPI.class,n->null));
            Global.setSector(ControlledPatrolRouteTest.proxy(SectorAPI.class,(n,a)->n.equals("getPersistentData")?persistent:n.equals("getEntityById")?entities.get(a[0]):null));
            StarSystemAPI sys=NewsSceneMechanicsTest.mock(StarSystemAPI.class,n->null);
            com.fs.starfarer.api.campaign.rules.MemoryAPI memory=SceneRevisionTest.memory(new HashMap<String,Object>());
            SectorEntityToken record=NewsSceneMechanicsTest.mock(SectorEntityToken.class,n->n.equals("getMemoryWithoutUpdate")?memory:n.equals("getId")?"record":n.equals("getLocation")?new Vector2f():n.equals("getContainingLocation")?sys:null);
            SectorEntityToken fragment=NewsSceneMechanicsTest.mock(SectorEntityToken.class,n->n.equals("getId")?"fragment":null);
            entities.put("record",record);
            for(String card:Arrays.asList("E-05","E-06"))for(int branch=0;branch<3;branch++){
                PTSDCrisisState.CrisisIncident i=incident(card,branch);
                PTSDNewsScene.sample(state,i,record);PTSDNewsScene.sample(state,i,fragment);
                check(!i.scene.recordTaken&&!i.scene.responseQueued&&!i.scene.resolved,"Observation caused capture/pursuit");
                PTSDExpansionScenes.conclude(state,i);
                check(i.investigationResult==PTSDTaskRules.InvestigationResult.INCONCLUSIVE,"All scans fabricated key evidence");
                i=incident(card,branch);
                check(!PTSDExpansionScenes.recover(i,fragment,10),"Wrong wreck gave record");
                check(PTSDExpansionScenes.recover(i,record,10),"Record unavailable");
                check(i.scene.responseQueued==(branch!=0),"Wrong response branch");
                float first=i.scene.responseDay;
                check(!PTSDExpansionScenes.recover(i,record,99)&&i.scene.responseDay==first,"Duplicate retrieval requeued");
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();new ObjectOutputStream(bytes).writeObject(i.scene);
                i.scene=(PTSDNewsScene.State)new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();
                check(!PTSDExpansionScenes.recover(i,record,100),"Reload renewed reward");
                PTSDExpansionScenes.conclude(state,i);
                check(i.investigationResult==(("E-05".equals(card)&&branch==2)?PTSDTaskRules.InvestigationResult.INCONCLUSIVE:PTSDTaskRules.InvestigationResult.CONFIRMED),"Wrong evidence verdict");
            }
            Vector2f position=new Vector2f(400,0);
            CampaignFleetAPI collector=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,n->n.equals("getId")?"collector":n.equals("getLocation")?position:n.equals("getContainingLocation")||n.equals("getStarSystem")?sys:n.equals("getFleetPoints")?20:null);
            entities.put("collector",collector);
            PTSDCrisisState.CrisisIncident i=incident("E-05",1);i.scene.reference="collector";i.scene.recordCollectionDay=5;
            check(!PTSDExpansionScenes.canRecover(i,record),"Nearby collector did not block");
            PTSDExpansionScenes.advance(i,4);check(!i.scene.recordLost,"Collection ignored deadline");
            position.x=1600;check(PTSDExpansionScenes.canRecover(i,record),"Drawing collector away did not open access");
            PTSDExpansionScenes.advance(i,6);check(!i.scene.recordLost,"Remote collector stole record");
            position.x=400;PTSDExpansionScenes.advance(i,6);check(i.scene.recordLost,"Arrived collector failed to take record");
            entities.remove("collector");check(!PTSDExpansionScenes.canRecover(i,record),"Lost record magically respawned");
            PTSDExpansionScenes.advance(i,20);check(i.scene.recordLost,"Lost state reset");
            JumpPointAPI exit=NewsSceneMechanicsTest.mock(JumpPointAPI.class,n->n.equals("getLocation")?new Vector2f():null);
            final int[] removals={0};final boolean[] fighting={true};
            StarSystemAPI rescueSystem=ControlledPatrolRouteTest.proxy(StarSystemAPI.class,(n,a)->{
                if(n.equals("removeEntity"))removals[0]++;
                return n.equals("getJumpPoints")?Arrays.asList(exit):null;
            });
            FleetDataAPI data=NewsSceneMechanicsTest.mock(FleetDataAPI.class,n->n.equals("getMembersListCopy")?Arrays.asList(NewsSceneMechanicsTest.mock(com.fs.starfarer.api.fleet.FleetMemberAPI.class,k->null)):null);
            BattleAPI battle=NewsSceneMechanicsTest.mock(BattleAPI.class,n->null);
            CampaignFleetAPI survivors=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,n->n.equals("getId")?"survivors":n.equals("getFleetPoints")?12:n.equals("getLocation")?new Vector2f():n.equals("getStarSystem")||n.equals("getContainingLocation")?rescueSystem:n.equals("getFleetData")?data:n.equals("getBattle")&&fighting[0]?battle:null);
            entities.put("survivors",survivors);
            java.lang.reflect.Method rescue=PTSDNewsScene.class.getDeclaredMethod("advanceRescue",PTSDCrisisState.class,PTSDCrisisState.CrisisIncident.class,float.class);rescue.setAccessible(true);
            i=incident("E-06",0);i.scene.escort=true;i.scene.towingRecord=true;i.scene.civilian="survivors";
            rescue.invoke(null,state,i,10f);check(!i.scene.resolved&&removals[0]==0,"Fleet vanished during battle at exit");
            fighting[0]=false;rescue.invoke(null,state,i,11f);
            check(i.scene.resolved&&i.scene.rescuedShips==1&&i.scene.recordTaken&&i.scene.rewardSettled&&removals[0]==1,"Actual rescue not settled");
            rescue.invoke(null,state,i,12f);check(removals[0]==1&&i.scene.rescuedShips==1,"Repeated rescue settlement");
            i=incident("E-06",2);i.scene.escort=true;i.scene.civilian="destroyed";
            rescue.invoke(null,state,i,12f);check(i.investigationResult==PTSDTaskRules.InvestigationResult.INCONCLUSIVE&&i.scene.rescuedShips==0,"Lost patrol invented rescue or disproved battle");
            System.out.println("ExpansionContestedTest: passed (6 branches, partial evidence, guard distance, physical collection, retrieval dedup, save/load, verdicts)");
        }finally{Global.setSector(previous);Global.setSettings(old);}
    }
}
