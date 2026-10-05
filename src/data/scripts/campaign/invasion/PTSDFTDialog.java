package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import data.scripts.campaign.invasion.news.ft.AbstractFTHandler;
import java.util.Collections;
import java.util.Map;

/** Native multi-step conversation for the crew and witnesses. Choices settle in the card handler. */
public final class PTSDFTDialog implements InteractionDialogPlugin {
    private final String id;
    private InteractionDialogAPI dialog;
    public PTSDFTDialog(String id) { this.id = id; }
    public static void open(String id) {
        if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null ||
                Global.getSector().getCampaignUI().getCurrentInteractionDialog() != null) return;
        Global.getSector().getCampaignUI().showInteractionDialog(new PTSDFTDialog(id),
                Global.getSector().getPlayerFleet());
    }
    @Override public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        render();
    }
    private void render() {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || state.late == null) { dialog.dismiss(); return; }
        PTSDLateState.FTChain chain = state.late.ftChains.get(id);
        if (chain == null) { dialog.dismiss(); return; }
        dialog.getOptionPanel().clearOptions();
        dialog.getTextPanel().addPara(chain.outcome == null ? "记录暂时中断。" :
                PTSDFTDirector.playerText(chain.outcome));
        PTSDCrisisIncidentManager.ScriptedTemplate template = PTSDCrisisIncidentManager.getTemplate(id);
        Object resolved = template == null || !template.targetExpression.startsWith("CUSTOM(") ? null :
                PTSDCrisisNewsAPI.resolveHandler(template.targetExpression.substring(7,
                        template.targetExpression.length() - 1));
        if (!chain.resolved && resolved instanceof AbstractFTHandler)
            for (String action : ((AbstractFTHandler) resolved).actions(state, chain))
                dialog.getOptionPanel().addOption(action, action);
        dialog.getOptionPanel().addOption("结束交谈", "close");
    }
    @Override public void optionSelected(String optionText, Object optionData) {
        if ("close".equals(optionData)) { dialog.dismiss(); return; }
        if (optionData instanceof String && PTSDFTDirector.choose(PTSDCrisisState.get(),
                id, (String) optionData)) render();
        else dialog.getTextPanel().addPara("条件尚未满足；没有支付费用，也没有改变目标状态。");
    }
    @Override public void optionMousedOver(String optionText, Object optionData) { }
    @Override public void advance(float amount) { }
    @Override public void backFromEngagement(EngagementResultAPI result) { }
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return Collections.emptyMap(); }
}
