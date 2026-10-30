package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import java.util.Map;

/** Real-time dialogue clock; reopening uses the incident's remaining time. */
public final class PTSDNewsSceneDialog implements InteractionDialogPlugin {
    private final PTSDCrisisState.CrisisIncident incident;
    private final SectorEntityToken node;
    private InteractionDialogAPI dialog;
    private int displayedSecond=-1;
    public PTSDNewsSceneDialog(PTSDCrisisState.CrisisIncident incident,SectorEntityToken node){this.incident=incident;this.node=node;}
    public void init(InteractionDialogAPI dialog){this.dialog=dialog;PTSDJeOtloesManager.recordParticipation(PTSDCrisisState.get(),"incident:"+incident.id);show();}
    private void show(){
        PTSDNewsScene.State s=incident.scene;dialog.getOptionPanel().clearOptions();
        dialog.getTextPanel().addPara(PTSDExpansionScenes.handles(incident.cardId)?PTSDExpansionScenes.intro(incident):PTSDNewsScene.intro(s));
        if(incident.jeExclusiveScene&&!s.resolved)dialog.getTextPanel().addPara(PTSDNewsSceneText.DIALOG_01);
        if(!s.result.isEmpty())dialog.getTextPanel().addPara(s.result);
        if(s.resolved||s.locked){
            if(!s.owned.contains(node.getId()))dialog.getOptionPanel().addOption("使用此处原有功能","normal");
            dialog.getOptionPanel().addOption(PTSDNewsSceneText.DIALOG_02,"leave");return;
        }
        if(s.timerArmed)dialog.getTextPanel().addPara(PTSDNewsSceneText.DIALOG_03+Math.max(0,(int)Math.ceil(s.secondsLeft))+PTSDNewsSceneText.DIALOG_04);
        dialog.getOptionPanel().addOption(PTSDExpansionScenes.contested(incident)?"远距核对残骸与传输片段":PTSDNewsSceneText.DIALOG_05,"sample");
        dialog.getOptionPanel().setEnabled("sample",!s.visited.contains(node.getId()));
        dialog.getOptionPanel().addOption(PTSDNewsSceneText.DIALOG_06,"finish");
        dialog.getOptionPanel().setEnabled("finish",s.samples>0||s.recordTaken);
        if(s.escort)dialog.getOptionPanel().setEnabled("finish",false);
        if(PTSDExpansionScenes.signalOptions(incident)&&(s.kind==PTSDNewsScene.Kind.RELAY_PARASITE||s.kind==PTSDNewsScene.Kind.SIGNAL_PURSUIT||
                s.kind==PTSDNewsScene.Kind.FORT_GRAVITY_SURVEY||s.kind==PTSDNewsScene.Kind.SPATIAL_ECHO||"E-02".equals(incident.cardId))){
            if(s.kind!=PTSDNewsScene.Kind.FORT_GRAVITY_SURVEY||s.branch==2){
                dialog.getOptionPanel().addOption(PTSDNewsSceneText.DIALOG_07,"download");
                dialog.getOptionPanel().addOption(PTSDNewsSceneText.DIALOG_08,"destroy");}
        }
        if(s.kind==PTSDNewsScene.Kind.FACILITY_INTERDICTION||s.kind==PTSDNewsScene.Kind.FORT_SUPPLY_WINDOW||s.kind==PTSDNewsScene.Kind.FORT_SENSOR_LATTICE){
            dialog.getOptionPanel().addOption(PTSDNewsSceneText.DIALOG_09,"disrupt");
            dialog.getOptionPanel().setEnabled("disrupt",s.operationStart<0&&Global.getSector().getPlayerFleet().getCargo().getSupplies()>=10);
        }
        if(s.kind==PTSDNewsScene.Kind.MISSING_CONVOY||s.kind==PTSDNewsScene.Kind.FORT_SALVAGE_INTERCEPTION){
            if(s.branch==2&&s.samples>0)dialog.getOptionPanel().addOption(PTSDNewsSceneText.DIALOG_10,"expose");
            if(PTSDNewsScene.fleet(s.civilian)!=null){dialog.getOptionPanel().addOption(PTSDNewsSceneText.DIALOG_11,"escort");dialog.getOptionPanel().addOption(PTSDNewsSceneText.DIALOG_12,"abandon");}
            dialog.getOptionPanel().addOption(PTSDNewsSceneText.DIALOG_13,"recover");
        }
        if(PTSDExpansionScenes.contested(incident)){
            if(!s.nodes.isEmpty()&&s.nodes.get(0).equals(node.getId())){
                dialog.getOptionPanel().addOption("E-05".equals(incident.cardId)?"回收补给账簿原件":"回收战斗记录舱","expansionRecover");
                dialog.getOptionPanel().setEnabled("expansionRecover",PTSDExpansionScenes.canRecover(incident,node));
                if(!s.recordTaken&&!s.recordLost&&!PTSDExpansionScenes.canRecover(incident,node))
                    dialog.getTextPanel().addPara("未知舰影仍在记录舱近旁；接驳艇无法安全靠近。你可以引开它，或通过交战清除威胁。");
            }
            if("E-06".equals(incident.cardId)&&PTSDNewsScene.fleet(s.civilian)!=null&&!s.escort){
                dialog.getOptionPanel().addOption("护送巡逻幸存者携记录撤往跃迁口","escort");
                dialog.getOptionPanel().addOption("让巡逻幸存者放下拖载，优先撤离","abandon");
            }
        }
        if(!s.owned.contains(node.getId()))dialog.getOptionPanel().addOption("使用此处原有功能","normal");
        dialog.getOptionPanel().addOption(PTSDNewsSceneText.DIALOG_14,"leave");
    }
    public void optionSelected(String text,Object data){
        String action=String.valueOf(data);PTSDNewsScene.State s=incident.scene;PTSDCrisisState state=PTSDCrisisState.get();float day=PTSDCrisisState.getDay();
        if("normal".equals(action)){PTSDPortInvestigation.openNormal(dialog,node);return;}
        if("leave".equals(action)){dialog.dismiss();return;}if(s.resolved||s.locked)return;
        s.responseNodeId=node.getId();

        if("expansionRecover".equals(action)){if(PTSDExpansionScenes.recover(incident,node,day))PTSDJeOtloesManager.recordParticipation(state,"incident:"+incident.id);}
        else if("sample".equals(action)){PTSDNewsScene.sample(state,incident,node);dialog.getTextPanel().addPara(PTSDNewsSceneText.sampling(s));}
        else if("finish".equals(action)&&!s.escort&&PTSDExpansionScenes.handles(incident.cardId))PTSDExpansionScenes.conclude(state,incident);
        else if("finish".equals(action)&&!s.escort)PTSDNewsScene.finishAs(state,incident,s.calibrationError?PTSDNewsSceneText.DIALOG_15:s.recordTaken?PTSDNewsSceneText.DIALOG_16:PTSDNewsSceneText.DIALOG_17+s.samples+PTSDNewsSceneText.DIALOG_18,s.calibrationError?PTSDTaskRules.InvestigationResult.DISPROVED:s.recordTaken?PTSDTaskRules.InvestigationResult.CONFIRMED:PTSDTaskRules.InvestigationResult.INCONCLUSIVE);
        else if("expose".equals(action)&&s.samples>0)PTSDNewsScene.finish(state,incident,PTSDNewsSceneText.DIALOG_19,true);
        else if("download".equals(action)){s.timerArmed=true;s.downloading=true;PTSDNewsScene.sample(state,incident,node);dialog.getTextPanel().addPara(PTSDNewsSceneText.DIALOG_20);}
        else if("destroy".equals(action)){s.disrupted=true;PTSDNewsScene.queueResponse(incident,day);PTSDNewsScene.finish(state,incident,PTSDNewsSceneText.DIALOG_21,true);}
        else if("disrupt".equals(action)&&s.operationStart<0&&Global.getSector().getPlayerFleet().getCargo().getSupplies()>=10){Global.getSector().getPlayerFleet().getCargo().removeSupplies(10);s.operationStart=day;s.operationNodeId=node.getId();dialog.getTextPanel().addPara(PTSDNewsSceneText.DIALOG_22);}
        else if(("escort".equals(action)||"abandon".equals(action))&&!s.escort){
            CampaignFleetAPI civilian=PTSDNewsScene.fleet(s.civilian);if(civilian!=null&&civilian.getStarSystem()!=null&&!civilian.getStarSystem().getJumpPoints().isEmpty()){
                s.escort=true;s.towingRecord="escort".equals(action);civilian.clearAssignments();civilian.addAssignment(FleetAssignment.GO_TO_LOCATION,civilian.getStarSystem().getJumpPoints().get(0),20,PTSDNewsSceneText.DIALOG_23);PTSDNewsScene.queueResponse(incident,day);dialog.getTextPanel().addPara(PTSDNewsSceneText.DIALOG_24);}}
        else if("recover".equals(action)){
            CampaignFleetAPI guard=PTSDNewsScene.fleet(s.reference);
            if(s.kind==PTSDNewsScene.Kind.FORT_SALVAGE_INTERCEPTION&&s.branch==1&&guard!=null&&guard.getBattle()==null&&guard.getContainingLocation()==node.getContainingLocation()&&com.fs.starfarer.api.util.Misc.getDistance(guard,node)<1200){
                dialog.getTextPanel().addPara(PTSDNewsSceneText.DIALOG_25);
            }
            else if(s.branch==2){s.timerArmed=true;s.downloading=true;dialog.getTextPanel().addPara(PTSDNewsSceneText.DIALOG_26);}
            else if(s.recordLost)dialog.getTextPanel().addPara(PTSDNewsSceneText.DIALOG_27);
            else{s.recordTaken=true;PTSDNewsScene.queueResponse(incident,day);dialog.getTextPanel().addPara(PTSDNewsSceneText.DIALOG_28);}}
        show();
    }
    public void advance(float amount){
        PTSDNewsScene.State s=incident.scene;if(!s.timerArmed||s.resolved||s.locked)return;
        s.secondsLeft-=Math.max(0,amount);
        if(s.downloading){s.downloadProgress+=Math.max(0,amount);if(s.downloadProgress>=12f){s.downloading=false;s.recordTaken=true;
            dialog.getTextPanel().addPara(PTSDNewsSceneText.DIALOG_29);show();}}
        int second=(int)Math.ceil(s.secondsLeft);
        if(second!=displayedSecond){displayedSecond=second;dialog.setPromptText(PTSDNewsSceneText.DIALOG_30+Math.max(0,second)+PTSDNewsSceneText.DIALOG_31);}
        if(s.secondsLeft<=0){PTSDNewsScene.timeout(incident,node);dialog.dismiss();}
    }
    public void optionMousedOver(String text,Object data){}public void backFromEngagement(EngagementResultAPI result){}
    public Object getContext(){return incident;}public Map<String,MemoryAPI> getMemoryMap(){return null;}
}
