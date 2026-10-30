package data.scripts.campaign.invasion;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import java.util.*;

/** Event observer, deliberately not a strategic recon controller. No report or upload is produced. */
public final class PTSDSceneScoutBehavior implements EveryFrameScript {
    public static final String CONTROLLED="$PTSD_scene_scout_controller";
    private final CampaignFleetAPI fleet;
    private final List<String> nodes;
    private final boolean onlyPursuit;
    private boolean escaping,sighted,boosted;
    private boolean requireSightingBeforeRetirement;
    private float departureDelay;
    private int index;
    private float unseenSince=-1, nextCheck, expires, nextNode;
    private SectorEntityToken waypoint;
    private final Set<String> scanned=new HashSet<String>();
    private PTSDSceneScoutBehavior(CampaignFleetAPI f,List<String> nodes,boolean escape,boolean pursuit){
        fleet=f;this.nodes=nodes==null?Collections.<String>emptyList():new ArrayList<String>(nodes);
        escaping=escape;onlyPursuit=pursuit;expires=PTSDCrisisState.getDay()+30;
    }
    public static void install(CampaignFleetAPI f,List<String> nodes,boolean escape,boolean pursuit){
        f.getMemoryWithoutUpdate().set(CONTROLLED,true);f.getMemoryWithoutUpdate().set("$PTSD_encounter_response_installed",true);
        f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,true);
        f.addScript(new PTSDSceneScoutBehavior(f,nodes,escape,pursuit));
    }
    /** Storm contact must be presented before its lost-contact retirement can begin. */
    public static void installStorm(CampaignFleetAPI f){
        f.getMemoryWithoutUpdate().set(CONTROLLED,true);
        f.getMemoryWithoutUpdate().set("$PTSD_encounter_response_installed",true);
        f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,true);
        PTSDSceneScoutBehavior script=new PTSDSceneScoutBehavior(f,null,true,true);
        script.departureDelay=2f;script.requireSightingBeforeRetirement=true;
        f.clearAssignments();
        f.addAssignment(FleetAssignment.HOLD,f.getContainingLocation().createToken(f.getLocation()),1f,"扫描风暴中的接触");
        f.addScript(script);
    }
    public boolean isDone(){return fleet==null || !fleet.isAlive() || fleet.isExpired() || fleet.isDespawning();}
    public boolean runWhilePaused(){return false;}
    public void advance(float amount){
        if(isDone() || fleet.getBattle()!=null)return;
        // Seconds, before the slower campaign-day AI checks. No offline or paused time.
        if(departureDelay>0){
            if(fleet.isVisibleToPlayerFleet()&&!sighted){sighted=true;PTSDJeOtloesManager.recordFleetEncounter(fleet);}
            fleet.getVelocity().set(0,0);
            departureDelay=Math.max(0,departureDelay-Math.max(0,amount));
            if(departureDelay>0)return;
            fleet.clearAssignments();
        }
        float day=PTSDCrisisState.getDay();if(day<nextCheck)return;nextCheck=day+.025f;
        CampaignFleetAPI player=Global.getSector().getPlayerFleet();if(player==null)return;
        if(fleet.isVisibleToPlayerFleet()&&!sighted){sighted=true;PTSDJeOtloesManager.recordFleetEncounter(fleet);}
        boolean pursuit=Global.getSector().getCampaignUI().getUltimateCourseTarget()==fleet || player.getInteractionTarget()==fleet;
        if(!escaping && (pursuit || !onlyPursuit && player.getContainingLocation()==fleet.getContainingLocation() && player.isVisibleToSensorsOf(fleet)))escaping=true;
        if(escaping){
            if(!pursuit && !boosted && fleet.getAbility(Abilities.EMERGENCY_BURN)!=null)fleet.getAbility(Abilities.EMERGENCY_BURN).setCooldownLeft(.1f);
            if(pursuit && !boosted){boosted=true;
                if(fleet.getAbility(Abilities.EMERGENCY_BURN)==null)fleet.addAbility(Abilities.EMERGENCY_BURN);
                if(fleet.getAbility(Abilities.EMERGENCY_BURN)!=null){fleet.getAbility(Abilities.EMERGENCY_BURN).setCooldownLeft(0);fleet.getAbility(Abilities.EMERGENCY_BURN).activate();}
            }
            if(fleet.isVisibleToPlayerFleet() || requireSightingBeforeRetirement && !sighted)unseenSince=-1;
            else if(unseenSince<0)unseenSince=day;
            else if(day-unseenSince>=.2f){retire();return;}
            if(waypoint==null || Misc.getDistance(fleet,waypoint)<1500 || fleet.getCurrentAssignment()==null){
                Vector2f away=player.getContainingLocation()==fleet.getContainingLocation()?Vector2f.sub(fleet.getLocation(),player.getLocation(),null):new Vector2f(1,0);
                if(away.lengthSquared()<1)away.set(1,0);away.normalise();away.scale(16000);Vector2f.add(away,fleet.getLocation(),away);
                waypoint=fleet.getContainingLocation().createToken(away);fleet.clearAssignments();fleet.addAssignment(FleetAssignment.GO_TO_LOCATION,waypoint,100,"撤离接触区域");
            }
        }else if(!nodes.isEmpty() && day>=nextNode && (fleet.getCurrentAssignment()==null || fleet.getCurrentAssignment().getAssignment()==FleetAssignment.ORBIT_PASSIVE)){
            nextNode=day+.5f;
            if(fleet.getCurrentAssignment()!=null){SectorEntityToken previous=fleet.getCurrentAssignment().getTarget();
                if(previous!=null && scanned.add(previous.getId())){
                    if(fleet.getAbility(Abilities.SENSOR_BURST)==null)fleet.addAbility(Abilities.SENSOR_BURST);
                    if(fleet.getAbility(Abilities.SENSOR_BURST)!=null)fleet.getAbility(Abilities.SENSOR_BURST).activate();
                }}
            SectorEntityToken node=PTSDNewsScene.entity(nodes.get(index++%nodes.size()));
            if(node!=null){fleet.clearAssignments();fleet.addAssignment(FleetAssignment.GO_TO_LOCATION,node,3,"检查漂流残片");fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE,node,.3f,"扫描残片");}
        }
        if(day>=expires)retire();
    }
    private void retire(){PTSDEventFleetReturn.credit(fleet);PTSDCampaignRuntime.leaving(fleet);fleet.despawn();}
}
