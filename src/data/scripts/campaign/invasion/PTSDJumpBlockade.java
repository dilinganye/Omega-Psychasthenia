package data.scripts.campaign.invasion;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import java.io.Serializable;
import java.util.*;

/** P-08: one real paired jump is blocked. A different usable system exit is mandatory. */
public final class PTSDJumpBlockade {
    private static final String OWNER="$PTSD_jump_blockade";
    public static final class State implements Serializable {
        public String inside, outside, cube, escort;
        public List<JumpPointAPI.JumpDestination> outbound=new ArrayList<>(), inbound=new ArrayList<>();
        public float endDay, cancelledDay=-1f, nextSpawn, nextPulse;
        public boolean spawned, restored, seen;
    }
    private PTSDJumpBlockade(){}
    static JumpPointAPI partner(JumpPointAPI inside){
        for(JumpPointAPI.JumpDestination d:inside.getDestinations())if(d.getDestination() instanceof JumpPointAPI&&d.getDestination().isInHyperspace()){
            JumpPointAPI outside=(JumpPointAPI)d.getDestination();
            for(JumpPointAPI.JumpDestination back:outside.getDestinations())if(back.getDestination()==inside)return outside;
        }
        return null;
    }
    static JumpPointAPI pick(StarSystemAPI sys){
        if(sys==null)return null;List<JumpPointAPI> exits=new ArrayList<>();
        for(SectorEntityToken t:sys.getJumpPoints())if(t instanceof JumpPointAPI&&!t.getMemoryWithoutUpdate().contains(OWNER)){
            JumpPointAPI j=(JumpPointAPI)t;JumpPointAPI other=partner(j);
            if(other!=null&&!other.getMemoryWithoutUpdate().contains(OWNER))exits.add(j);
        }
        // Distinct hyperspace-side points: blocking a shared entrance must not seal the system.
        for(JumpPointAPI j:exits)for(JumpPointAPI alternate:exits)if(j!=alternate&&partner(j)!=partner(alternate))return j;
        return null;
    }
    static boolean build(PTSDCrisisState.CrisisIncident i,StarSystemAPI sys){
        if(i.scene.blockade!=null)return !i.scene.blockade.restored;
        JumpPointAPI inside=pick(sys);if(inside==null)return false;JumpPointAPI outside=partner(inside);
        State b=new State();b.inside=inside.getId();b.outside=outside.getId();b.endDay=PTSDCrisisState.getDay()+30;
        // Retain exact destination descriptors, not just IDs/labels; unrelated routes remain intact.
        for(JumpPointAPI.JumpDestination d:new ArrayList<>(inside.getDestinations()))if(d.getDestination()==outside){b.outbound.add(d);inside.removeDestination(outside);}
        for(JumpPointAPI.JumpDestination d:new ArrayList<>(outside.getDestinations()))if(d.getDestination()==inside){b.inbound.add(d);outside.removeDestination(inside);}
        inside.getMemoryWithoutUpdate().set(OWNER,i.id);outside.getMemoryWithoutUpdate().set(OWNER,i.id);
        i.scene.blockade=b;PTSDNewsScene.bind(i,inside);PTSDNewsScene.bind(i,outside);
        i.investigationExpiresDay=Math.max(i.investigationExpiresDay,b.endDay);
        Global.getSector().addScript(new Repulsion(i.id));return true;
    }
    static float deadline(float original,float cancel){return cancel<0?original:Math.min(original,cancel+20);}
    static boolean clearOfOthers(LocationAPI loc,Vector2f point,CampaignFleetAPI player){
        for(CampaignFleetAPI f:loc.getFleets())if(f!=player&&f.isAlive()&&!f.isEmpty()&&Misc.getDistance(f.getLocation(),point)<3000)return false;
        return true;
    }
    static void advance(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,float day){
        State b=i.scene.blockade;if(b==null||i.scene.cleaned)return;
        if(i.investigationAbandoned&&b.cancelledDay<0)b.cancelledDay=day;
        SectorEntityToken inside=PTSDNewsScene.entity(b.inside),outside=PTSDNewsScene.entity(b.outside);
        CampaignFleetAPI player=Global.getSector().getPlayerFleet();
        CampaignFleetAPI existingCube=PTSDNewsScene.fleet(b.cube);
        boolean nearby=player!=null&&((inside!=null&&player.getContainingLocation()==inside.getContainingLocation()&&Misc.getDistance(player,inside)<4500)||
                (outside!=null&&player.getContainingLocation()==outside.getContainingLocation()&&Misc.getDistance(player,outside)<4500));
        if(nearby)b.seen=true;
        if(day>=deadline(b.endDay,b.cancelledDay)||inside==null||outside==null||b.spawned&&(existingCube==null||existingCube.isEmpty()||!existingCube.isAlive())){
            restore(i);
            if(!i.scene.resolved)PTSDNewsScene.finishAs(state,i,b.seen?"反向脉冲停止，跃迁参数恢复。现场记录确认了这次真实封锁。":"封锁窗口已经结束，没有取得完整现场记录。",b.seen?PTSDTaskRules.InvestigationResult.CONFIRMED:PTSDTaskRules.InvestigationResult.INCONCLUSIVE);
            boolean pending=false;
            for(String id:new String[]{b.cube,b.escort}){CampaignFleetAPI f=PTSDNewsScene.fleet(id);if(f!=null){if(f.getBattle()!=null)pending=true;else PTSDNewsScene.retire(f);}}
            i.scene.cleaned=!pending;return;
        }
        if(!b.spawned&&nearby&&day>=b.nextSpawn&&!data.scripts.PTSDDifficultySettings.spawningPaused()){
            b.nextSpawn=day+.2f;
            Vector2f point=null;
            // Search a quiet arc at the real exit. Wait if busy; never spawn into an ongoing NPC battle.
            for(int n=0;n<12;n++){Vector2f candidate=PTSDNewsScene.offset(outside.getLocation(),n*30,950);
                if(clearOfOthers(outside.getContainingLocation(),candidate,player)){point=candidate;break;}}
            if(point==null){i.scene.lastError="超空间出口有第三方舰队，封锁仍在，实体部队等待安全窗口";return;}
            CampaignFleetAPI cube=Global.getFactory().createEmptyFleet(PTSDCrisisProgress.getActiveFactionId(state),"维持封锁的未知构型",true);
            cube.getFleetData().addFleetMember(Global.getFactory().createFleetMember(com.fs.starfarer.api.fleet.FleetMemberType.SHIP,"IIRT_Omega_Cube_Shock"));
            place(cube,outside,point,i);b.cube=cube.getId();
            CampaignFleetAPI escort=PTSDReconScenes.spawn(state,i,inside.getStarSystem(),point);
            if(escort!=null){PTSDCampaignRuntime.leaving(escort);escort.getContainingLocation().removeEntity(escort);outside.getContainingLocation().addEntity(escort);escort.setLocation(point.x+250,point.y+150);PTSDCampaignRuntime.entityAdded(escort);
                placeFlags(escort,i);escort.clearAssignments();escort.addAssignment(FleetAssignment.DEFEND_LOCATION,cube,40,"维持封锁阵位");b.escort=escort.getId();}
            b.spawned=true;i.scene.lastError="";
        }
    }
    private static void placeFlags(CampaignFleetAPI f,PTSDCrisisState.CrisisIncident i){
        f.setTransponderOn(false);f.setNoFactionInName(true);
        f.getMemoryWithoutUpdate().set(PTSDSceneScoutBehavior.CONTROLLED,true);
        f.getMemoryWithoutUpdate().set("$PTSD_encounter_response_installed",true);
        f.getMemoryWithoutUpdate().set("$PTSD_participation_key","incident:"+i.id);
        f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,true);
        f.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS,true);
        f.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS,true);
        // Prevent strategic offscreen engagement. Manual player interaction remains available.
        f.getMemoryWithoutUpdate().set(MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER,true);
    }
    private static void place(CampaignFleetAPI f,SectorEntityToken outside,Vector2f point,PTSDCrisisState.CrisisIncident i){
        placeFlags(f,i);outside.getContainingLocation().addEntity(f);f.setLocation(point.x,point.y);PTSDCampaignRuntime.entityAdded(f);
        for(com.fs.starfarer.api.fleet.FleetMemberAPI m:f.getFleetData().getMembersListCopy())m.getRepairTracker().setCR(m.getRepairTracker().getMaxCR());
        f.addAssignment(FleetAssignment.HOLD,outside.getContainingLocation().createToken(point),40,"维持跃迁封锁");
        i.scene.fleets.put(f.getId(),i.scene.blockade.endDay);
    }
    static void restore(PTSDCrisisState.CrisisIncident i){
        if(i==null||i.scene==null||i.scene.blockade==null||i.scene.blockade.restored)return;
        State b=i.scene.blockade;
        restoreSide(PTSDNewsScene.entity(b.inside),b.outbound,i.id);
        restoreSide(PTSDNewsScene.entity(b.outside),b.inbound,i.id);
        b.restored=true;PTSDNewsScene.unmark(i);
    }
    private static void restoreSide(SectorEntityToken token,List<JumpPointAPI.JumpDestination> saved,String owner){
        if(!(token instanceof JumpPointAPI))return;JumpPointAPI point=(JumpPointAPI)token;
        if(!owner.equals(point.getMemoryWithoutUpdate().getString(OWNER)))return;
        for(JumpPointAPI.JumpDestination d:saved){SectorEntityToken target=d.getDestination();if(target==null||target.isExpired())continue;
            boolean exists=false;for(JumpPointAPI.JumpDestination present:point.getDestinations())if(present.getDestination()==target)exists=true;
            if(!exists)point.addDestination(d);
        }
        point.getMemoryWithoutUpdate().unset(OWNER);
    }
    static InteractionDialogPlugin dialogFor(SectorEntityToken target){
        if(!(target instanceof JumpPointAPI)||target.getMemoryWithoutUpdate()==null||!target.getMemoryWithoutUpdate().contains(OWNER))return null;
        final PTSDCrisisState.CrisisIncident i=PTSDCrisisAPI.getIncident(target.getMemoryWithoutUpdate().getString(OWNER));
        if(i==null||i.scene==null||i.scene.blockade==null||i.scene.blockade.restored)return null;
        return new PTSDReconScenes.SimpleDialog(){
            public void init(InteractionDialogAPI d){super.init(d);i.scene.blockade.seen=true;
                PTSDJeOtloesManager.recordParticipation(PTSDCrisisState.get(),"incident:"+i.id);
                d.getTextPanel().addPara(PTSDReconScenes.intro(i));d.getOptionPanel().addOption("离开跃迁口","leave");}
            public void optionSelected(String text,Object option){dialog.dismiss();}
        };
    }
    private static final class Repulsion implements EveryFrameScript {
        private final String id;private boolean done;private float pulse;
        Repulsion(String id){this.id=id;}
        public boolean isDone(){return done;}public boolean runWhilePaused(){return false;}
        public void advance(float amount){
            PTSDCrisisState.CrisisIncident i=PTSDCrisisAPI.getIncident(id);
            if(i==null||i.scene==null||i.scene.blockade==null||i.scene.blockade.restored){done=true;return;}
            State b=i.scene.blockade;pulse+=amount;
            CampaignFleetAPI player=Global.getSector().getPlayerFleet();if(player==null)return;
            for(String endpoint:new String[]{b.inside,b.outside}){
                SectorEntityToken point=PTSDNewsScene.entity(endpoint);
                if(point==null||player.getContainingLocation()!=point.getContainingLocation())continue;
                if(Misc.getDistance(player,point)<4000&&pulse>=2){Global.getSector().addPing(point,Pings.INTERDICT,java.awt.Color.MAGENTA);pulse=0;}
                // Only the current location; candidate cache avoids a sector-wide per-frame scan.
                for(CampaignFleetAPI f:PTSDCampaignRuntime.fleets(point.getContainingLocation())){
                    if(f.getBattle()!=null||f.isInHyperspaceTransition())continue;
                    float distance=Misc.getDistance(f,point);if(distance>=850)continue;
                    Vector2f away=Vector2f.sub(f.getLocation(),point.getLocation(),null);if(away.lengthSquared()<1)away.set(1,0);away.normalise();
                    float push=Math.min(35,Math.max(0,amount)*180*(1-distance/850));
                    f.setLocation(f.getLocation().x+away.x*push,f.getLocation().y+away.y*push);
                }
            }
        }
    }
}
