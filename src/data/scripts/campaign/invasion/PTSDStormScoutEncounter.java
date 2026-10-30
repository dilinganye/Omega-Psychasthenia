package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.shard.PTSD_BaseShard_Util;
import org.lwjgl.util.vector.Vector2f;
import java.util.Random;

/** One persisted roll per 30-day RECON window, consumed only by travel through a live storm. */
public final class PTSDStormScoutEncounter {
    private PTSDStormScoutEncounter(){}
    public static void advance(PTSDCrisisState state,float day){advance(state,day,false);}
    public static void devSpawn(){if(Global.getSettings().isDevMode())advance(PTSDCrisisState.get(),PTSDCrisisState.getDay(),true);}
    private static void advance(PTSDCrisisState state,float day,boolean forced){
        if(state.phase!=PTSDCrisisState.Phase.RECON)return;
        CampaignFleetAPI player=Global.getSector().getPlayerFleet();
        if(player==null || !player.isInHyperspace() || !forced && player.getVelocity().lengthSquared()<1)return;
        int month=(int)Math.floor(Math.max(0,day-state.phaseStartedDay)/30f);
        if(!forced && state.stormScoutWindow!=null && state.stormScoutWindow==month)return;
        if(!forced){
        HyperspaceTerrainPlugin hyper=Misc.getHyperspaceTerrainPlugin();
        if(hyper==null || !hyper.isInClouds(player))return;
        HyperspaceTerrainPlugin.CellStateTracker cell=hyper.getCellAt(player,0);
        if(cell==null || !cell.isStorming())return;
        state.stormScoutWindow=month;
        }
        Random random=new Random();if(!forced && random.nextFloat()>=.5f)return;
        PTSDCrisisFleetLimits limits=PTSDCrisisFleetLimits.forPhase(state,day);
        Vector2f point=Misc.getUnitVectorAtDegreeAngle(random.nextFloat()*360);point.scale(600+random.nextFloat()*300);Vector2f.add(player.getLocation(),point,point);
        FleetParamsV3 params=new FleetParamsV3(point,PTSDCrisisProgress.getActiveFactionId(state),.5f,FleetTypes.MERC_SCOUT,limits.perFleetFp,0,0,0,0,0,0);
        CampaignFleetAPI scout=PTSD_BaseShard_Util.createFleet(params,limits.perFleetFp,PTSD_BaseShard_Util.FleetRole.RECON,random,limits.forFleet(limits.perFleetFp));
        if(scout==null || scout.getFleetPoints()<=0)return;
        scout.setName("风暴中的未知接触");scout.setNoFactionInName(true);scout.setTransponderOn(false);
        scout.getMemoryWithoutUpdate().set("$PTSD_encounter_response_installed",true);
        scout.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,true);
        player.getContainingLocation().addEntity(scout);scout.setLocation(point.x,point.y);PTSDCampaignRuntime.entityAdded(scout);
        for(com.fs.starfarer.api.fleet.FleetMemberAPI m:scout.getFleetData().getMembersListCopy())m.getRepairTracker().setCR(m.getRepairTracker().getMaxCR());
        if(scout.getAbility(Abilities.SENSOR_BURST)==null)scout.addAbility(Abilities.SENSOR_BURST);
        if(scout.getAbility(Abilities.SENSOR_BURST)!=null){scout.getAbility(Abilities.SENSOR_BURST).setCooldownLeft(0);scout.getAbility(Abilities.SENSOR_BURST).activate();}
        PTSDSceneScoutBehavior.installStorm(scout);
        PTSDCrisisDevIntel.report("风暴接触","本月事件接触已生成；不产生侦察报告",null,scout.getId());
    }
}
