package data.scripts.campaign.invasion;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.combat.StatBonus;
import org.lwjgl.util.vector.Vector2f;
import java.util.*;
import java.lang.reflect.Constructor;

public final class ReconWorldSceneTest {
    static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    static PTSDCrisisState.CrisisIncident item(PTSDCrisisState state,PTSDNewsScene.Kind kind){
        PTSDCrisisState.CrisisIncident i=new PTSDCrisisState.CrisisIncident();i.id="test-"+state.incidents.size();i.cardId="P-09";
        i.scene=new PTSDNewsScene.State();i.scene.kind=kind;i.scene.recon=new PTSDReconScenes.Progress();i.scene.recon.enemy="enemy";i.scene.recon.station="station";i.scene.recon.observedOnce=true;i.investigationExpiresDay=PTSDCrisisState.getDay()+1000;
        state.incidents.add(i);return i;
    }
    public static void main(String[] args)throws Exception{
        SectorAPI old=Global.getSector();SettingsAPI settings=Global.getSettings();
        try{
            Constructor<PTSDCrisisState> ctor=PTSDCrisisState.class.getDeclaredConstructor();ctor.setAccessible(true);
            PTSDCrisisState state=ctor.newInstance();state.progressInitialized=true;
            Map<String,Object> persistent=new HashMap<>();persistent.put(PTSDCrisisState.PERSISTENT_KEY,state);
            Map<String,SectorEntityToken> entities=new HashMap<>();
            StatBonus sensor=new StatBonus();MutableFleetStatsAPI stats=NewsSceneMechanicsTest.mock(MutableFleetStatsAPI.class,n->n.equals("getSensorRangeMod")?sensor:null);
            CampaignClockAPI clock=NewsSceneMechanicsTest.mock(CampaignClockAPI.class,n->null);
            StarSystemAPI sys=NewsSceneMechanicsTest.mock(StarSystemAPI.class,n->null);
            final boolean[] fighting={false},visible={true},detected={false};
            final Vector2f enemyPosition=new Vector2f(2000,0);
            com.fs.starfarer.api.campaign.rules.MemoryAPI memory=SceneRevisionTest.memory(new HashMap<>());
            BattleAPI battle=NewsSceneMechanicsTest.mock(BattleAPI.class,n->null);
            CampaignFleetAPI station=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,n->n.equals("getContainingLocation")?sys:n.equals("getLocation")?new Vector2f():n.equals("getMemoryWithoutUpdate")?memory:n.equals("getStats")?stats:null);
            CampaignFleetAPI enemy=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,n->n.equals("getId")?"enemy":n.equals("getContainingLocation")||n.equals("getStarSystem")?sys:n.equals("getLocation")?enemyPosition:n.equals("getBattle")&&fighting[0]?battle:n.equals("isVisibleToPlayerFleet")?visible[0]:null);
            CampaignFleetAPI player=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,n->n.equals("getContainingLocation")?sys:n.equals("getLocation")?new Vector2f():n.equals("isVisibleToSensorsOf")?detected[0]:null);
            entities.put("station",station);entities.put("enemy",enemy);
            Global.setSettings(NewsSceneMechanicsTest.mock(SettingsAPI.class,n->null));
            Global.setSector(ControlledPatrolRouteTest.proxy(SectorAPI.class,(n,a)->n.equals("getPersistentData")?persistent:n.equals("getEntityById")?entities.get(a[0]):n.equals("getClock")?clock:n.equals("getPlayerFleet")?player:null));
            PTSDCrisisState.CrisisIncident entry=item(state,PTSDNewsScene.Kind.DARK_TRANSIT);entry.recordedByPlayer=true;entry.scene.nodes.add("enemy");
            check(PTSDNewsScene.dialogForIncident(entry,enemy)==null,"Enemy battle dialog replaced with generic sampling");
            entry.scene.kind=PTSDNewsScene.Kind.RECON_SURRENDER;entry.scene.recon.enemy="other";
            check(PTSDNewsScene.dialogForIncident(entry,enemy) instanceof PTSDReconScenes.SimpleDialog,"Rules re-entry lost physical escort dialog");
            entry.investigationResolved=true;
            check(PTSDNewsScene.dialogForIncident(entry,enemy)==null,"Completed encounter re-opened from rules menu");
            PTSDCrisisState.CrisisIncident i=item(state,PTSDNewsScene.Kind.STATION_BLACKOUT);
            PTSDReconScenes.PressureClock pressure=new PTSDReconScenes.PressureClock(i.id);
            enemyPosition.x=4000;pressure.advance(35);check(i.scene.recon.seconds==0,"Remote ship suppresses station");
            enemyPosition.x=2000;pressure.advance(35);check(i.scene.recon.seconds==35&&sensor.computeEffective(100)==10,"Physical pressure missing");
            pressure.advance(35);check(pressure.isDone()&&i.scene.resolved&&sensor.computeEffective(100)==100,"Seventy-second pressure not restored");
            pressure.advance(35);check(sensor.computeEffective(100)==100,"Repeated clock re-applies pressure");
            i=item(state,PTSDNewsScene.Kind.STATION_BLACKOUT);pressure=new PTSDReconScenes.PressureClock(i.id);pressure.advance(12);
            fighting[0]=true;pressure.advance(1);check(pressure.isDone()&&i.scene.resolved&&sensor.computeEffective(100)==100,"Intercept failed to stop pressure");fighting[0]=false;
            i=item(state,PTSDNewsScene.Kind.STATION_BLACKOUT);pressure=new PTSDReconScenes.PressureClock(i.id);pressure.advance(12);
            i.investigationAbandoned=true;pressure.advance(1);check(sensor.computeEffective(100)==100&&pressure.isDone(),"Abandon left permanent modifier");
            i=item(state,PTSDNewsScene.Kind.DARK_TRANSIT);i.cardId="P-12";
            visible[0]=false;PTSDReconScenes.advance(state,i,1);check(i.scene.recon.observed==0,"Invisible target creates track evidence");
            visible[0]=true;PTSDReconScenes.advance(state,i,1.1f);check(i.scene.recon.observed>0&&!i.scene.resolved,"Visible covert tracking not accumulated");
            float before=i.scene.recon.observed;visible[0]=false;PTSDReconScenes.advance(state,i,5);check(i.scene.recon.observed==before,"Absent interval fabricates evidence");
            visible[0]=true;PTSDReconScenes.advance(state,i,5.1f);PTSDReconScenes.advance(state,i,5.2f);check(i.scene.resolved,"Continuous covert track cannot conclude");
            check(PTSDSceneDevIntel.branches("P-08").length==2&&PTSDSceneDevIntel.branches("P-04").length==3,"DEV queues nonexistent duplicate branches");
            System.out.println("ReconWorldSceneTest: passed (station proximity, seventy seconds, interruption, abandonment, covert visibility and no catch-up)");
        }finally{Global.setSector(old);Global.setSettings(settings);}
    }
}
