package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import java.util.*;

/** A port case owns evidence, not a colony or a replacement market. */
public final class PTSDPortInvestigation implements InteractionDialogPlugin {
    private final SectorEntityToken target;
    private InteractionDialogAPI dialog;
    private PTSDCrisisState.CrisisIncident selected;
    private String initialCaseId;
    private static boolean openingNormal;
    public static boolean openingNormal(){return openingNormal;}
    public static boolean supports(String cardId){return PTSDPortInvestigationText.supports(cardId);}
    public PTSDPortInvestigation(SectorEntityToken target){this.target=target;}
    public PTSDPortInvestigation(SectorEntityToken target,String incidentId){this.target=target;this.initialCaseId=incidentId;}
    static Set<String> evidence(PTSDNewsScene.State s){if(s.portEvidence==null)s.portEvidence=new LinkedHashSet<String>();return s.portEvidence;}
    static int sourcePort(PTSDNewsScene.State s,int source){return source==1&&s.nodes.size()>1?1:0;}
    static boolean available(PTSDNewsScene.State s,int source,String node){
        if(s.resolved||source<0||source>2||s.nodes.isEmpty()||!s.nodes.get(sourcePort(s,source)).equals(node)||evidence(s).contains("source:"+source))return false;
        if(source==2)return evidence(s).contains("source:0")&&evidence(s).contains("source:1");
        return source!=1||s.branch==0||evidence(s).contains("source:0");
    }
    static boolean collect(PTSDNewsScene.State s,int source,String node){
        if(!available(s,source,node))return false;evidence(s).add("source:"+source);s.samples=evidence(s).size();s.visited.add(node);return true;
    }
    public void init(InteractionDialogAPI dialog){
        this.dialog=dialog;
        if(initialCaseId!=null)for(PTSDCrisisState.CrisisIncident i:cases())if(initialCaseId.equals(i.id)){selected=i;show();return;}
        menu();
    }
    private List<PTSDCrisisState.CrisisIncident> cases(){
        List<PTSDCrisisState.CrisisIncident> result=new ArrayList<PTSDCrisisState.CrisisIncident>();
        for(PTSDCrisisState.CrisisIncident i:PTSDCrisisState.get().incidents)
            if(PTSDNewsScene.hasActiveEntry(i,target)&&i.scene.kind==PTSDNewsScene.Kind.PORT_RECORDS&&PTSDPortInvestigationText.supports(i.cardId))result.add(i);
        return result;
    }
    private void menu(){
        selected=null;dialog.getOptionPanel().clearOptions();dialog.getTextPanel().addPara("新闻调查接待：你可以查阅已接取事件的当地线索，也可以先办理正常港口事务。");
        for(PTSDCrisisState.CrisisIncident i:cases())dialog.getOptionPanel().addOption("调查："+PTSDPlayerNewsText.headline(i),"case:"+i.id);
        dialog.getOptionPanel().addOption("办理正常港口事务","normal");dialog.getOptionPanel().addOption("离开","leave");
    }
    private void show(){
        PTSDNewsScene.State s=selected.scene;PTSDPortInvestigationText.Case text=PTSDPortInvestigationText.get(selected.cardId);
        dialog.getOptionPanel().clearOptions();dialog.getTextPanel().addPara(text.intro);
        dialog.getTextPanel().addPara("已保存证据 "+evidence(s).size()+" / 3。资料随调查保留，离港不会重置。");
        SectorEntityToken node=PTSDNewsScene.boundNode(selected,target);
        for(int source=0;source<3;source++){
            int owner=sourcePort(s,source);SectorEntityToken port=PTSDNewsScene.entity(s.nodes.get(owner));
            boolean local=node!=null&&s.nodes.get(owner).equals(node.getId());
            dialog.getOptionPanel().addOption(text.actions[source]+(evidence(s).contains("source:"+source)?"（已记录）":local?"":"（另一处港口保管）"),"source:"+source);
            dialog.getOptionPanel().setEnabled("source:"+source,node!=null&&available(s,source,node.getId()));
            if(!local&&!evidence(s).contains("source:"+source)&&port!=null)dialog.getTextPanel().addPara("独立副本由"+port.getName()+"保管；请自行前往核对。");
        }
        if(s.branch!=0&&!evidence(s).contains("source:0"))dialog.getTextPanel().addPara("对方要求先出示原始受理编号，才同意查找独立副本。");
        dialog.getOptionPanel().addOption("核对并提交完整调查","finish");dialog.getOptionPanel().setEnabled("finish",evidence(s).size()==3);
        dialog.getOptionPanel().addOption("只提交已有证据，结束调查","partial");dialog.getOptionPanel().setEnabled("partial",!evidence(s).isEmpty());
        dialog.getOptionPanel().addOption("返回调查列表","back");dialog.getOptionPanel().addOption("办理正常港口事务","normal");dialog.getOptionPanel().addOption("暂时离港","leave");
    }
    public void optionSelected(String text,Object value){
        String action=String.valueOf(value);
        if("leave".equals(action)){dialog.dismiss();return;}
        if("normal".equals(action)){normal();return;}
        if("back".equals(action)){menu();return;}
        if(action.startsWith("case:")){
            for(PTSDCrisisState.CrisisIncident i:cases())if(i.id.equals(action.substring(5))){selected=i;show();return;}menu();return;
        }
        if(selected==null||!PTSDNewsScene.hasActiveEntry(selected,target)){menu();return;}
        PTSDNewsScene.State s=selected.scene;
        if(action.startsWith("source:")){
            int source;try{source=Integer.parseInt(action.substring(7));}catch(NumberFormatException ex){return;}
            SectorEntityToken node=PTSDNewsScene.boundNode(selected,target);
            if(node!=null&&collect(s,source,node.getId())){
                PTSDJeOtloesManager.recordParticipation(PTSDCrisisState.get(),"incident:"+selected.id);
                dialog.getTextPanel().addPara(PTSDPortInvestigationText.get(selected.cardId).evidence[source]);
                if(source==2)dialog.getTextPanel().addPara(PTSDPortInvestigationText.get(selected.cardId).outcomes[Math.floorMod(s.branch,3)]);
            }show();return;
        }
        if("finish".equals(action)&&evidence(s).size()==3){
            String outcome=PTSDPortInvestigationText.get(selected.cardId).outcomes[Math.floorMod(s.branch,3)];
            PTSDNewsScene.finishAs(PTSDCrisisState.get(),selected,outcome,s.branch==2?PTSDTaskRules.InvestigationResult.INCONCLUSIVE:PTSDTaskRules.InvestigationResult.CONFIRMED);
            dialog.getTextPanel().addPara(outcome);menu();
        }else if("partial".equals(action)&&!evidence(s).isEmpty()){
            PTSDNewsScene.finishAs(PTSDCrisisState.get(),selected,"已封存"+evidence(s).size()+"份当地资料；未取得的原件保持未知，没有据此确认报道中的全部推断。",PTSDTaskRules.InvestigationResult.INCONCLUSIVE);menu();
        }
    }
    private void normal(){openNormal(dialog,target);}
    public static void openNormal(InteractionDialogAPI dialog,final SectorEntityToken target){
        dialog.dismiss();
        Global.getSector().addTransientScript(new com.fs.starfarer.api.EveryFrameScript(){
            boolean done;public boolean isDone(){return done;}public boolean runWhilePaused(){return true;}
            public void advance(float amount){
                CampaignUIAPI ui=Global.getSector().getCampaignUI();if(ui.isShowingDialog()){done=true;return;}
                done=true;openingNormal=true;try{ui.showInteractionDialog(target);}finally{openingNormal=false;}
            }
        });
    }
    public void advance(float amount){}public void optionMousedOver(String t,Object d){}public void backFromEngagement(EngagementResultAPI r){}
    public Object getContext(){return selected;}public Map<String,MemoryAPI> getMemoryMap(){return null;}
}
