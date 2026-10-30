package data.scripts.campaign.invasion;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.shard.PTSD_BaseShard_Util;
import org.lwjgl.util.vector.Vector2f;
import java.util.Random;

/** Sensor-gated reactions. Each physical detachment can fund a single pair of reinforcements. */
public final class PTSDWarFleetResponse implements EveryFrameScript {
    private final CampaignFleetAPI fleet;
    private final String eventId;
    private boolean called, reacting, fleeing;
    private int pulses;
    private float nextCheck, nextPulse, nextSupportAttempt, nextSweepCheck;
    private PTSDWarFleetResponse(CampaignFleetAPI f,String id){fleet=f;eventId=id;}
    static void attach(CampaignFleetAPI fleet,String eventId) {
        // Suppress the general encounter controller; two controllers must not overwrite orders.
        fleet.getMemoryWithoutUpdate().set("$PTSD_encounter_response_installed",true);
        fleet.addScript(new PTSDWarFleetResponse(fleet,eventId));
    }
    public boolean isDone(){return fleet==null||!fleet.isAlive();}
    public boolean runWhilePaused(){return false;}
    public void advance(float amount) {
        float day=PTSDCrisisState.getDay();if(day<nextCheck||isDone())return;nextCheck=day+.05f;
        if(fleet.getBattle()!=null)return;
        CampaignFleetAPI player=Global.getSector().getPlayerFleet();
        PTSDCrisisState state=PTSDCrisisState.get();PTSDCrisisState.StrategicEvent event=state.getEvent(eventId);
        if(event==null)return;
        boolean detected=player!=null&&player.getContainingLocation()==fleet.getContainingLocation()&&
                player.getVisibilityLevelTo(fleet)!=SectorEntityToken.VisibilityLevel.NONE;
        if(!detected){if(reacting){resume(state,event);reacting=false;}
            if(day>=nextSweepCheck && !fleet.getMemoryWithoutUpdate().getBoolean("$PTSD_wave_return")){nextSweepCheck=day+.5f;PTSDPriorityTargetDirector.hunt(fleet,event);}return;}
        boolean stronger=player.getFleetPoints()>fleet.getFleetPoints();
        if(!reacting||stronger!=fleeing||fleet.getCurrentAssignment()==null) {
            reacting=true;fleeing=stronger;fleet.clearAssignments();
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,stronger);
            if(stronger) {
                Vector2f direction=Vector2f.sub(fleet.getLocation(),player.getLocation(),null);
                if(direction.lengthSquared()<1)direction.set(1,0);direction.normalise();direction.scale(12000);
                Vector2f.add(direction,fleet.getLocation(),direction);
                fleet.addAssignment(FleetAssignment.GO_TO_LOCATION,fleet.getContainingLocation().createToken(direction),5,"紧急脱离追踪");
                if(fleet.getAbility(Abilities.EMERGENCY_BURN)==null)fleet.addAbility(Abilities.EMERGENCY_BURN);
                if(fleet.getAbility(Abilities.EMERGENCY_BURN)!=null&&fleet.getAbility(Abilities.EMERGENCY_BURN).isUsable())
                    fleet.getAbility(Abilities.EMERGENCY_BURN).activate();
            } else fleet.addAssignment(FleetAssignment.INTERCEPT,player,3,"迎击已发现目标");
        }
        if(stronger) {
            if(pulses<3&&day>=nextPulse&&Misc.getDistance(fleet,player)<3500) {
                PTSDNewsScene.pulse(fleet,false);pulses++;nextPulse=day+.2f;
            }
            if(!called && day>=nextSupportAttempt){nextSupportAttempt=day+1;callPair(state,event);}
        }
    }
    private void resume(PTSDCrisisState state,PTSDCrisisState.StrategicEvent e) {
        SectorEntityToken target=null;
        if(fleet.getMemoryWithoutUpdate().getBoolean("$PTSD_wave_return")) {
            target=PTSDWarWaveReturn.anchor(state,e.sourceSystemId);
        } else {
            com.fs.starfarer.api.campaign.econ.MarketAPI market=state.resolveMarket(e.targetMarketId);
            if(market!=null)target=market.getPrimaryEntity();
        }
        if(target==null)return;
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,false);
        fleet.clearAssignments();fleet.addAssignment(FleetAssignment.GO_TO_LOCATION,target,80,"恢复既定航路");
        if(!fleet.getMemoryWithoutUpdate().getBoolean("$PTSD_wave_return"))
            fleet.addAssignment(FleetAssignment.ATTACK_LOCATION,target,20,"执行战区任务");
    }
    private void callPair(PTSDCrisisState state,PTSDCrisisState.StrategicEvent e) {
        StarSystemAPI source=state.resolveSystem(e.sourceSystemId);if(source==null)return;
        PTSDCrisisFleetLimits limit=PTSDCrisisFleetLimits.forPhase(state,PTSDCrisisState.getDay());
        float budget=Math.max(30,Math.min(limit.perFleetFp,fleet.getFleetPoints()));
        if(PTSDLateWarDirector.affordableAttackStrength(state)<budget*2)return;
        // Build both candidates before paying or adding either to the campaign.
        CampaignFleetAPI[] pair=new CampaignFleetAPI[2];float total=0;
        for(int i=0;i<2;i++) {
            FleetParamsV3 params=new FleetParamsV3(source.getLocation(),IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION,1f,
                    FleetTypes.TASK_FORCE,budget,0,0,0,0,0,0);
            pair[i]=PTSD_BaseShard_Util.createFleet(params,budget,PTSD_BaseShard_Util.FleetRole.GUARD_ASSAULT,new Random(),limit.forFleet(budget));
            if(pair[i]==null||pair[i].getFleetPoints()<=0)return;total+=pair[i].getFleetPoints();
        }
        if(!PTSDLateWarDirector.reserveAttack(state,"support-pair:"+fleet.getId(),total))return;
        called=true;
        for(int i=0;i<2;i++) {
            CampaignFleetAPI support=pair[i];support.setName("精神创伤响应编队");support.setNoFactionInName(true);
            support.addTag("PTSD_crisis_fleet");support.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY,true);
            support.getMemoryWithoutUpdate().set("$PTSD_encounter_response_installed",true);
            source.addEntity(support);PTSDCampaignRuntime.entityAdded(support);
            support.setLocation(source.getCenter().getLocation().x+600+i*200,source.getCenter().getLocation().y);
            // The destination is the reported position, not an omniscient moving player reference.
            SectorEntityToken reported=fleet.getContainingLocation().createToken(new Vector2f(fleet.getLocation()));
            support.addAssignment(FleetAssignment.GO_TO_LOCATION,reported,30,"前往遇袭编队最后报告位置");
            support.addAssignment(FleetAssignment.DEFEND_LOCATION,reported,4,"搜索与掩护撤离");
            support.getEventListeners().add(new PTSDStrategicFleetListener(null,support.getFleetPoints()));
            support.addScript(new SupportReturn(support,e.sourceSystemId,PTSDCrisisState.getDay()+34));
        }
    }
    private static final class SupportReturn implements EveryFrameScript {
        final CampaignFleetAPI fleet;final String source;final float deadline;boolean done;
        SupportReturn(CampaignFleetAPI f,String s,float d){fleet=f;source=s;deadline=d;}
        public void advance(float amount){if(PTSDCrisisState.getDay()>=deadline&&fleet.getBattle()==null){PTSDWarWaveReturn.begin(fleet,source,fleet.getFleetPoints());done=true;}}
        public boolean isDone(){return done||!fleet.isAlive();}public boolean runWhilePaused(){return false;}
    }
}
