package data.scripts.campaign.invasion;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import java.lang.reflect.*;
import java.util.*;

public class FirstReconInvestigationTest {
    static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
    public static void main(String[] args)throws Exception{
        Constructor<PTSDCrisisState> ctor=PTSDCrisisState.class.getDeclaredConstructor();ctor.setAccessible(true);
        PTSDCrisisState s=ctor.newInstance();s.progressInitialized=true;s.phase=PTSDCrisisState.Phase.RECON;
        s.totalScoutEscapes=1;
        check(!PTSDJeOtloesManager.triggerUnlocked(s),"Escape alone unlocks Je");
        s.totalScoutEscapes=100;s.totalOmegaEncounters=100;
        check(!PTSDJeOtloesManager.triggerUnlocked(s),"Background encounter/escape totals unlock Je");
        s.totalPlayerOmegaBattles=1;check(PTSDJeOtloesManager.triggerUnlocked(s),"One actual battle must qualify");s.totalPlayerOmegaBattles=0;
        for(int n=0;n<3;n++)PTSDJeOtloesManager.recordParticipation(s,"incident:"+n);
        check(!PTSDJeOtloesManager.triggerUnlocked(s),"Three participations qualify");
        PTSDJeOtloesManager.recordParticipation(s,"incident:2");check(s.jeParticipationIds.size()==3,"Duplicate counted");
        PTSDJeOtloesManager.recordParticipation(s,"incident:3");check(PTSDJeOtloesManager.triggerUnlocked(s),"Four participations fail");s.jeParticipationIds.clear();
        PTSDCrisisState.CrisisIncident i=new PTSDCrisisState.CrisisIncident();i.id="first";i.cardId="SITE-01";i.investigable=true;i.recordedByPlayer=true;i.acceptedDuringRecon=true;i.investigationExpiresDay=60;
        for(int outcome=1;outcome<=3;outcome++){i.investigationOutcome=outcome;check(PTSDFirstReconInvestigation.eligible(s,i,1),"Guarantee wrongly depends on truth");}
        s.firstReconInvestigationId="first";check(!PTSDFirstReconInvestigation.eligible(s,i,1),"Re-entry repeats guarantee");s.firstReconInvestigationId=null;
        i.acceptedDuringRecon=false;check(!PTSDFirstReconInvestigation.eligible(s,i,1),"Pre-RECON acceptance qualifies");i.acceptedDuringRecon=true;
        check(!PTSDFirstReconInvestigation.eligible(s,i,60),"Expired lead qualifies");
        s.phase=PTSDCrisisState.Phase.WAR;check(!PTSDFirstReconInvestigation.eligible(s,i,1),"WAR qualifies");
        SectorAPI previous=Global.getSector();SettingsAPI old=Global.getSettings();
        try{
            Map<String,Object> data=new HashMap<>();data.put(PTSDCrisisState.PERSISTENT_KEY,s);
            LocationAPI here=NewsSceneMechanicsTest.mock(LocationAPI.class,n->null),remote=NewsSceneMechanicsTest.mock(LocationAPI.class,n->null);
            LocationAPI[] place={remote};boolean[] visible={false};
            FactionAPI faction=NewsSceneMechanicsTest.mock(FactionAPI.class,n->n.equals("getId")?IIRT_Omega_Invasion.WATCHER_FACTION:null);
            MemoryAPI memory=NewsSceneMechanicsTest.mock(MemoryAPI.class,n->null);
            CampaignFleetAPI player=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,n->n.equals("getContainingLocation")?here:null);
            CampaignFleetAPI fleet=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,n->n.equals("getContainingLocation")?place[0]:n.equals("isVisibleToPlayerFleet")?visible[0]:n.equals("getFaction")?faction:n.equals("getMemoryWithoutUpdate")?memory:n.equals("getId")?"scout":null);
            Global.setSettings(NewsSceneMechanicsTest.mock(SettingsAPI.class,n->null));
            Global.setSector(NewsSceneMechanicsTest.mock(SectorAPI.class,n->n.equals("getPlayerFleet")?player:n.equals("getPersistentData")?data:n.equals("getClock")?NewsSceneMechanicsTest.mock(CampaignClockAPI.class,k->null):null));
            PTSDJeOtloesManager.recordFleetEncounter(fleet);check(s.jeParticipationIds.isEmpty(),"Remote plugin query counted encounter");
            place[0]=here;PTSDJeOtloesManager.recordFleetEncounter(fleet);check(s.jeParticipationIds.isEmpty(),"Invisible plugin query counted encounter");
            visible[0]=true;PTSDJeOtloesManager.recordFleetEncounter(fleet);PTSDJeOtloesManager.recordFleetEncounter(fleet);
            check(s.jeParticipationIds.size()==1,"Visible real encounter not deduplicated");
            // Exercise the dedicated observer controller: no retreat before detection,
            // retreat on detection, and no removal until player visibility is lost.
            boolean[] detects={false},alive={true};int[] orders={0},despawns={0};
            SectorEntityToken waypoint=NewsSceneMechanicsTest.mock(SectorEntityToken.class,n->n.equals("getLocation")?new org.lwjgl.util.vector.Vector2f(20000,0):null);
            LocationAPI location=NewsSceneMechanicsTest.mock(LocationAPI.class,n->n.equals("createToken")?waypoint:null);
            com.fs.starfarer.api.combat.StatBonus burn=new com.fs.starfarer.api.combat.StatBonus();
            MutableFleetStatsAPI stats=NewsSceneMechanicsTest.mock(MutableFleetStatsAPI.class,n->n.equals("getFleetwideMaxBurnMod")?burn:null);
            CampaignFleetAPI observer=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,n->{
                if(n.equals("isAlive"))return alive[0];if(n.equals("isVisibleToPlayerFleet"))return visible[0];
                if(n.equals("getContainingLocation"))return location;if(n.equals("getStats"))return stats;
                if(n.equals("getLocation"))return new org.lwjgl.util.vector.Vector2f(100,0);
                if(n.equals("addAssignment"))orders[0]++;
                if(n.equals("despawn")){despawns[0]++;alive[0]=false;}return null;
            });
            CampaignFleetAPI nearby=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,n->n.equals("getContainingLocation")?location:n.equals("isVisibleToSensorsOf")?detects[0]:n.equals("getLocation")?new org.lwjgl.util.vector.Vector2f(0,0):null);
            Global.setSector(NewsSceneMechanicsTest.mock(SectorAPI.class,n->n.equals("getPlayerFleet")?nearby:n.equals("getPersistentData")?data:null));
            Constructor<PTSDFirstReconInvestigation> controller=PTSDFirstReconInvestigation.class.getDeclaredConstructor(CampaignFleetAPI.class);controller.setAccessible(true);
            PTSDFirstReconInvestigation script=controller.newInstance(observer);
            Field sighted=PTSDFirstReconInvestigation.class.getDeclaredField("sighted");sighted.setAccessible(true);sighted.setBoolean(script,true);
            visible[0]=false;script.advance(.1f);check(orders[0]==0&&despawns[0]==0,"Unseen observing fleet prematurely removed");
            detects[0]=true;visible[0]=true;script.advance(.1f);check(orders[0]==1&&despawns[0]==0,"Detection did not immediately trigger retreat");
            script.advance(.1f);check(despawns[0]==0,"Visible fleeing observer removed");
            visible[0]=false;script.advance(.1f);script.advance(.1f);check(despawns[0]==1,"Lost observer not removed exactly once");
        }finally{Global.setSector(previous);Global.setSettings(old);}
        System.out.println("FirstReconInvestigationTest: passed (Je qualification, remote/invisible query rejection, first-arrival eligibility and dedup)");
    }
}
