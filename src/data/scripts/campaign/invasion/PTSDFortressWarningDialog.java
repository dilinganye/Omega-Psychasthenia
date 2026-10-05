package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;

import java.util.Map;

/** No blast spoiler before first contact; both choices return to the campaign map. */
public final class PTSDFortressWarningDialog implements InteractionDialogPlugin {
    private final String fortressId;
    private InteractionDialogAPI dialog;

    public PTSDFortressWarningDialog(String fortressId) { this.fortressId = fortressId; }

    @Override public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        dialog.getTextPanel().addPara("未知主力要塞已进入交战距离。当地记录无法估计它的完整火力。当前可以返回星图手动存档，再决定是否接战。");
        dialog.getOptionPanel().addOption("返回星图，稍后决定", "return");
        dialog.getOptionPanel().addOption("继续接近", "continue");
    }

    @Override public void optionSelected(String optionText, Object optionData) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null && state.late != null) {
            PTSDLateState.Fortress fortress = state.late.fortresses.get(fortressId);
            if (fortress != null) fortress.autoSavePending = true;
        }
        dialog.dismiss();
    }

    @Override public void optionMousedOver(String optionText, Object optionData) { }
    @Override public void advance(float amount) { }
    @Override public void backFromEngagement(EngagementResultAPI battleResult) { }
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return java.util.Collections.emptyMap(); }
}
