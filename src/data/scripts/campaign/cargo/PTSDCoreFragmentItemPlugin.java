package data.scripts.campaign.cargo;

import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * Reward left behind by a destroyed Psychasthenia/Watcher command personality.
 *
 * The right-click hook is intentionally dormant. A later story implementation can switch
 * hasRightClickAction() to true and open a cargo-origin interaction dialog from
 * performRightClickAction(RightClickActionHelper), following ShroudedSubstratePlugin.
 */
public final class PTSDCoreFragmentItemPlugin extends BaseSpecialItemPlugin {
    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded,
                              CargoTransferHandlerAPI transferHandler, Object stackSource) {
        if (!com.fs.starfarer.api.Global.CODEX_TOOLTIP_MODE) tooltip.addTitle(getName());
        if (spec.getDesc() != null && !spec.getDesc().isEmpty()) {
            tooltip.addPara(spec.getDesc(), Misc.getTextColor(), 10f);
        }
        tooltip.addPara("其中封存着无法用常规方法解译的敌对协议。目前没有可用的分析手段。",
                Misc.getGrayColor(), 10f);
        addCostLabel(tooltip, 10f, transferHandler, stackSource);
    }

    @Override
    public boolean hasRightClickAction() {
        return false;
    }

    @Override
    public boolean shouldRemoveOnRightClickAction() {
        return false;
    }

    @Override
    public void performRightClickAction(RightClickActionHelper helper) {
        // FUTURE STORY HOOK:
        // create RuleBasedInteractionDialogPluginImpl, pass helper through setCustom1(),
        // then call CampaignUIAPI.showInteractionDialogFromCargo(). Do not enable until
        // the analysis rules and persistent story state are implemented.
    }
}
