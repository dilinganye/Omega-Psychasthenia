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

/** A one-time observer, independent of the report's truth roll and site cleanup. */
public final class PTSDFirstReconInvestigation implements EveryFrameScript {
    private final CampaignFleetAPI fleet;
    private boolean escaping, sighted;
    private SectorEntityToken waypoint;
    private PTSDFirstReconInvestigation(CampaignFleetAPI fleet){this.fleet=fleet;}

    static boolean eligible(PTSDCrisisState state, PTSDCrisisState.CrisisIncident i,float day){
        return state.phase==PTSDCrisisState.Phase.RECON && state.firstReconInvestigationId==null &&
                i!=null && i.acceptedDuringRecon && i.recordedByPlayer && !i.investigationResolved &&
                day<i.investigationExpiresDay && PTSDNewsPresentation.canInvestigate(i);
    }
    public static void advanceSites(PTSDCrisisState state){
        if(state==null || state.phase!=PTSDCrisisState.Phase.RECON || state.firstReconInvestigationId!=null)return;
        CampaignFleetAPI player=Global.getSector().getPlayerFleet();
        if(player==null || player.getStarSystem()==null)return;
        for(PTSDCrisisState.CrisisIncident i:state.incidents){
            if(!eligible(state,i,PTSDCrisisState.getDay()))continue;
            if(i.scene!=null&&i.scene.kind==PTSDNewsScene.Kind.STATION_BLACKOUT){
                // This scene owns its delayed scout. Never add the generic first-arrival scout.
                CampaignFleetAPI staged=i.scene.recon==null?null:PTSDNewsScene.fleet(i.scene.recon.enemy);
                if(staged!=null&&staged.isAlive()&&staged.isVisibleToPlayerFleet()){
                    state.firstReconInvestigationId=i.id;state.firstReconInvestigationScoutId=staged.getId();return;
                }
                continue;
            }
            if(i.scene!=null && "HEATDEATH".equals(i.scene.wreckMode))continue; // Rare solitary scene; keep the scout guarantee for the next eligible investigation.
            SectorEntityToken site=PTSDCrisisAPI.resolveIncidentTarget(i);
            if(site==null || site.getContainingLocation()!=player.getContainingLocation() ||
                    !player.getStarSystem().getId().equals(i.targetSystemId) || Misc.getDistance(player,site)>1800f)continue;
            // True template sites must exist before claiming their first arrival.
            if(i.investigationOutcome==1 && !i.siteMaterialized)continue;
            if(i.scene!=null&&"TWIN_SCOUTS".equals(i.scene.wreckMode)){
                for(String fleetId:i.scene.fleets.keySet()){
                    CampaignFleetAPI existing=PTSDNewsScene.fleet(fleetId);
                    if(existing!=null&&existing.isAlive()&&existing.getMemoryWithoutUpdate().getBoolean(PTSDSceneScoutBehavior.CONTROLLED)){
                        state.firstReconInvestigationId=i.id;state.firstReconInvestigationScoutId=fleetId;return;
                    }
                }
            }
            Random random=new Random();
            FleetParamsV3 params=new FleetParamsV3(site.getLocation(),IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION,
                    .25f,FleetTypes.MERC_SCOUT,24f,0,0,0,0,0,0);
            params.maxNumShips=3;
            CampaignFleetAPI scout=PTSD_BaseShard_Util.createFleet(params,24f,PTSD_BaseShard_Util.FleetRole.RECON,
                    random,new PTSD_BaseShard_Util.FleetLimits(24f,3,com.fs.starfarer.api.combat.ShipAPI.HullSize.DESTROYER));
            if(scout==null)return; // Retry; failed generation does not consume the guarantee.
            Vector2f point=PTSDCrisisAPI.findSafePoint(player.getStarSystem(),site,300,650,random);
            if(point==null)return;
            scout.setName("正在核查现场的未知侦察队");scout.setNoFactionInName(true);scout.setTransponderOn(false);
            scout.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,true);
            scout.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY,true);
            scout.getMemoryWithoutUpdate().set("$PTSD_participation_key","incident:"+i.id);
            // Keep generic pursuer maintenance from installing a conflicting controller.
            scout.getMemoryWithoutUpdate().set("$PTSD_encounter_response_installed",true);
            player.getStarSystem().addEntity(scout);scout.setLocation(point.x,point.y);
            PTSDCampaignRuntime.entityAdded(scout);
            scout.addAssignment(FleetAssignment.ORBIT_PASSIVE,site,100000f,"核查现场信号");
            scout.addScript(new PTSDFirstReconInvestigation(scout));
            state.firstReconInvestigationId=i.id;state.firstReconInvestigationScoutId=scout.getId();
            PTSDCrisisDevIntel.report("首次侦察调查遭遇","现场侦察队已生成",i.targetSystemId,scout.getId());
            return;
        }
    }
    public boolean isDone(){return fleet==null||!fleet.isAlive()||fleet.isExpired()||fleet.isDespawning();}
    public boolean runWhilePaused(){return false;}
    public void advance(float amount){
        if(isDone()||Global.getSector()==null||fleet.getBattle()!=null)return;
        CampaignFleetAPI player=Global.getSector().getPlayerFleet();
        if(player==null)return;
        if(!sighted&&fleet.isVisibleToPlayerFleet()){
            sighted=true;
            PTSDJeOtloesManager.recordFleetEncounter(fleet);
            IIRT_Omega_Invasion.reportScoutSighting(fleet.getStarSystem()==null?null:fleet.getStarSystem().getId());
        }
        if(!escaping){
            if(player.getContainingLocation()!=fleet.getContainingLocation()||!player.isVisibleToSensorsOf(fleet))return;
            escaping=true;
            fleet.getStats().getFleetwideMaxBurnMod().modifyFlat("PTSD_first_recon_escape",8f);
            flee(player);
        }else if(!fleet.isVisibleToPlayerFleet()){
            PTSDEventFleetReturn.credit(fleet);PTSDCampaignRuntime.leaving(fleet);fleet.despawn();return;
        }
        if(waypoint==null||Misc.getDistance(fleet,waypoint)<1500f)flee(player);
    }
    private void flee(CampaignFleetAPI player){
        Vector2f away=Vector2f.sub(fleet.getLocation(),player.getLocation(),null);
        if(away.lengthSquared()<1)away=new Vector2f(1,0);
        away.normalise();away.scale(15000);Vector2f.add(away,fleet.getLocation(),away);
        waypoint=fleet.getContainingLocation().createToken(away);
        fleet.clearAssignments();fleet.addAssignment(FleetAssignment.GO_TO_LOCATION,waypoint,100000f,"撤离接触区域");
    }
}
