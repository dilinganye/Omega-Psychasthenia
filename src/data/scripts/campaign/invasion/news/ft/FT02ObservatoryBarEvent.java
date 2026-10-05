package data.scripts.campaign.invasion.news.ft;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson;
import data.scripts.campaign.invasion.*;
import java.util.Map;

/** The withdrawn-curve witness is an actual portside bar event at the saved core market. */
public final class FT02ObservatoryBarEvent extends BaseBarEventWithPerson {
    @Override public boolean isAlwaysShow() { return true; }
    @Override public boolean shouldShowAtMarket(MarketAPI market) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || state.late == null || market == null) return false;
        PTSDLateState.FTChain chain = state.late.ftChains.get("FT-02");
        return chain != null && !chain.resolved && market.getId().equals(chain.marketId) &&
                ("WITHDRAWN_CURVE".equals(chain.node) || "ARCHIVE_REGION".equals(chain.node) ||
                 "BAR_EVADING".equals(chain.node));
    }
    @Override public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        regen(dialog.getInteractionTarget().getMarket());
        dialog.getTextPanel().addPara("一名穿着观测台维修服的工人独坐角落。他把一张旧星图折了又折。");
        dialog.getOptionPanel().addOption("询问被撤回的辐射曲线", this);
    }
    @Override public void init(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        super.init(dialog, memoryMap);
        PTSDCrisisState state = PTSDCrisisState.get();
        PTSDLateState.FTChain chain = state == null ? null : state.late.ftChains.get("FT-02");
        if (chain == null) { done = true; return; }
        regen(dialog.getInteractionTarget().getMarket());
        dialog.getVisualPanel().showPersonInfo(person, true);
        if (!"BAR_EVADING".equals(chain.node))
            PTSDFTDirector.choose(state, "FT-02", "与观测员交谈");
        render();
    }
    private void render() {
        PTSDLateState.FTChain chain = PTSDCrisisState.get().late.ftChains.get("FT-02");
        options.clearOptions();
        text.addPara(chain.outcome);
        if ("BAR_EVADING".equals(chain.node)) {
            options.addOption("支付10000星币", "支付10000星币");
            options.addOption("花费1故事点施压", "花费1故事点施压");
        }
        options.addOption("离开酒馆", "leave");
    }
    @Override public void optionSelected(String optionText, Object optionData) {
        if ("leave".equals(optionData)) { done = true; noContinue = true; return; }
        if (optionData instanceof String && PTSDFTDirector.choose(PTSDCrisisState.get(),
                "FT-02", (String) optionData)) {
            render();
            PTSDLateState.FTChain chain = PTSDCrisisState.get().late.ftChains.get("FT-02");
            if ("EXACT_COORDINATES".equals(chain.node)) done = true;
        } else text.addPara("你尚无法承担这项代价；观测员重新低头看向酒杯。");
    }
}
