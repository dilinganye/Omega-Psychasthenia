package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import java.util.*;

/** The patrol gap is measured from a moving guard and a real facility, not three probes. */
final class PTSDPatrolWindow {
    static boolean build(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,StarSystemAPI sys,Vector2f base){
        PTSDNewsScene.State s=i.scene;SectorEntityToken facility=PTSDNewsScene.entity(s.reference);
        if(facility==null||sys.getJumpPoints().isEmpty()){s.lastError="巡防现场缺少设施或真实出口";return false;}
        SectorEntityToken exit=sys.getJumpPoints().get(0);CampaignFleetAPI guard=null;
        for(CampaignFleetAPI f:sys.getFleets())if(!f.isStationMode()&&PTSDJeOtloesManager.isOmegaFleet(f)&&f.getBattle()==null&&f.getCurrentAssignment()!=null&&f.getCurrentAssignment().getAssignment()==FleetAssignment.PATROL_SYSTEM){guard=f;break;}
        boolean own=guard==null;
        if(own)guard=PTSDReconScenes.spawn(state,i,sys,PTSDNewsScene.offset(base,200,3400));
        if(guard==null){s.lastError="没有可用巡逻队；等待生成额度恢复";return false;}
        s.operationNodeId=guard.getId();PTSDNewsScene.bind(i,exit);
        if(own){
            guard.clearAssignments();
            // Real locations, with a readable interval between departure and return.
            guard.addAssignment(FleetAssignment.GO_TO_LOCATION,exit,8,"驶向换防出口");
            guard.addAssignment(FleetAssignment.ORBIT_PASSIVE,exit,s.branch==1?1f:.3f,"等待交接");
            guard.addAssignment(FleetAssignment.GO_TO_LOCATION,facility,8,"返回设施外围");
            guard.addAssignment(FleetAssignment.PATROL_SYSTEM,facility,15,"巡视设施与航路");
        }
        i.siteConfirmationHint="观察巡逻队离站，在设施外围停留取证；深入前留意返航方向。";
        return true;
    }
    static void advance(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,float elapsed){
        PTSDNewsScene.State s=i.scene;if(s.resolved)return;
        SectorEntityToken facility=PTSDNewsScene.entity(s.reference);CampaignFleetAPI guard=PTSDNewsScene.fleet(s.operationNodeId),player=Global.getSector().getPlayerFleet();
        if(facility==null){PTSDNewsScene.finishAs(state,i,"设施已离开原位，巡防窗口无法继续确认。",PTSDTaskRules.InvestigationResult.TARGET_LOST);return;}
        if(player==null||player.getContainingLocation()!=facility.getContainingLocation())return;
        float distance=Misc.getDistance(player,facility);
        boolean gap=guard==null||guard.getContainingLocation()!=facility.getContainingLocation()||Misc.getDistance(guard,facility)>3000||guard.getBattle()!=null;
        if(distance>1600&&distance<3600&&gap){
            s.downloadProgress+=Math.min(.1f,Math.max(0,elapsed));
            PTSDJeOtloesManager.recordParticipation(state,"incident:"+i.id);
            if(s.branch==2&&!s.responseQueued){PTSDNewsScene.queueResponse(i,PTSDCrisisState.getDay());
                if(guard!=null&&guard.getBattle()==null&&s.fleets.containsKey(guard.getId())){guard.clearAssignments();guard.addAssignment(FleetAssignment.GO_TO_LOCATION,player.getContainingLocation().createToken(player.getLocation()),2,"封闭巡防缺口");}}
            if(s.downloadProgress>=.25f)PTSDNewsScene.finish(state,i,s.branch==2?"外围记录已经保存，返回舰影开始封闭来路。你没有等待包围完成。":"换防期间的设施活动已被连续记录。证据来自实际巡逻窗口，不能据此认定防区永久空缺。",true);
        }
        // Leaving the corridor preserves partial evidence but never advances the observation clock.
    }
    static InteractionDialogPlugin dialog(final PTSDCrisisState.CrisisIncident i,final SectorEntityToken target){
        return new PTSDReconScenes.SimpleDialog(){
            public void init(InteractionDialogAPI d){super.init(d);d.getTextPanel().addPara("换防队沿真实出口与设施往返。待它离开设施三千距离以外，再于设施外围约一千六至三千六的距离连续观测四分之一日。这里没有需要逐个接驳的采样探针；返航舰影与附近敌军仍会构成威胁。");d.getOptionPanel().addOption("使用原有跃迁功能","normal");d.getOptionPanel().addOption("离开","leave");}
            public void optionSelected(String text,Object data){if("normal".equals(data))PTSDPortInvestigation.openNormal(dialog,target);else dialog.dismiss();}
        };
    }
}
