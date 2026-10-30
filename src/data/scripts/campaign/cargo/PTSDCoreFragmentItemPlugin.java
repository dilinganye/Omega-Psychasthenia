package data.scripts.campaign.cargo;

import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * Reward left behind by a destroyed Psychasthenia/Watcher command personality.
 *
 * Shared cargo-origin workshop entry for protocol fragments and investigation reports.
 */
public class PTSDCoreFragmentItemPlugin extends BaseSpecialItemPlugin {
    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded,
                              CargoTransferHandlerAPI transferHandler, Object stackSource) {
        if (!com.fs.starfarer.api.Global.CODEX_TOOLTIP_MODE) tooltip.addTitle(getName());
        if (spec.getDesc() != null && !spec.getDesc().isEmpty()) {
            tooltip.addPara(spec.getDesc(), Misc.getTextColor(), 10f);
        }
        tooltip.addPara("右键交给工程主管与首席科学家，查看分析与装配项目。打开界面不会消耗材料。",
                Misc.getGrayColor(), 10f);
        addCostLabel(tooltip, 10f, transferHandler, stackSource);
    }

    @Override
    public boolean hasRightClickAction() {
        return true;
    }

    @Override
    public boolean shouldRemoveOnRightClickAction() {
        return false;
    }

    @Override
    public void performRightClickAction(RightClickActionHelper helper) {
        PTSDEngineeringDialog.open(helper);
    }
}
