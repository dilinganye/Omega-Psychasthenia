package data.scripts.campaign.invasion;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.io.Serializable;
import java.util.*;

/** Stores actual rosters even while traveling virtually; projection never rolls a new fleet. */
public final class PTSDReliefState implements Serializable {
    public enum Stage { PREPARING, MUSTERING, ASSEMBLED, TRAVELLING, ACTIVE, GARRISON, RETURNING, DONE }
    public static final class Unit implements Serializable {
        public String id,kind,faction,sourceMarket,sourceSystem,targetSystem,targetMarket,musterMarket,frontId,group;
        public Stage stage=Stage.PREPARING;
        public CampaignFleetAPI fleet;
        public float stageDay,lastDay,fromX,fromY,x,y,travelDays,committed;
        public boolean physical,reported,borrowed,settled;
        public String status="整备";
    }
    public static final class BattleRecord implements Serializable {
        public String id,system; public float x,y,pathBudget,pathSpent,elapsed,nextPath,requestTime;
        public boolean requested,settled,pathExhausted;
        public Map<String,String> sourceFactions=new LinkedHashMap<String,String>();
        public Map<String,List<FleetMemberAPI>> pending=new LinkedHashMap<String,List<FleetMemberAPI>>();
        public Map<String,List<FleetMemberAPI>> arrived=new LinkedHashMap<String,List<FleetMemberAPI>>();
        public Map<String,Float> arrivalSeconds=new LinkedHashMap<String,Float>();
        public Set<String> dead=new HashSet<String>();
        public Set<String> enemyIds=new HashSet<String>();
    }
    public List<Unit> units=new ArrayList<Unit>();
    public Set<String> alerts=new HashSet<String>();
    public Map<String,Float> sourceSpent=new LinkedHashMap<String,Float>();
    public Map<String,Float> groups=new LinkedHashMap<String,Float>();
    public Map<String,Float> regions=new LinkedHashMap<String,Float>();
    public Map<String,float[]> regionCenters=new LinkedHashMap<String,float[]>();
    public Map<String,String> requestNotes=new LinkedHashMap<String,String>();
    public Map<String,Float> relations=new LinkedHashMap<String,Float>();
    public Map<String,BattleRecord> battles=new LinkedHashMap<String,BattleRecord>();
    public float nextDay,nextRecon,nextRaid,nextReplenish;
    public PTSDJeWarExpedition.Mission jeExpedition;
    public boolean playerInvited,playerDeclined;
    public static PTSDReliefState get(PTSDCrisisState s){if(s.late.relief==null)s.late.relief=new PTSDReliefState();return s.late.relief;}
}
