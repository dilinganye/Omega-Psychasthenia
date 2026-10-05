package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.Color;

/** Public mobilization desk: three finite orders against a deliberately incomplete threat map. */
public final class PTSDMobilizationIntel extends BaseIntelPlugin {
    private static final long serialVersionUID = 1L;
    private static final Color INK = new Color(180, 205, 225);

    public static PTSDMobilizationIntel ensureIntel() {
        if (Global.getSector() == null) return null;
        Object old = Global.getSector().getIntelManager().getFirstIntel(PTSDMobilizationIntel.class);
        if (old instanceof PTSDMobilizationIntel) return (PTSDMobilizationIntel) old;
        PTSDMobilizationIntel intel = new PTSDMobilizationIntel();
        Global.getSector().getIntelManager().addIntel(intel);
        return intel;
    }

    @Override protected String getName() { return "公开动员：边缘防务草案"; }
    @Override public String getSmallDescriptionTitle() { return getName(); }
    @Override public boolean hasLargeDescription() { return true; }

    @Override public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || state.late == null) return;
        info.addPara("联合报告仅覆盖已公开的航路和殖民地。布防与撤离命令须在警报成真前下达。", 0f);
        info.addPara("剩余跨势力命令：%s/3", 5f, INK,
                String.valueOf(Math.max(0, 3 - state.late.mobilizationOrdersUsed)));
        int count = 0;
        for (MarketAPI market : PTSDOccupationManager.getAllMarkets()) {
            if (count >= 12) break;
            if (market == null || market.getStarSystem() == null || market.isPlanetConditionMarketOnly()) continue;
            if (PTSDCrisisFactionModifierAPI.get(market.getFactionId(),
                    market.getStarSystem().getId()).immune) continue;
            PTSDCrisisState.SystemData data = state.getSystemData(market.getStarSystem().getId());
            if (data == null || !data.hasNonCrisisColony) continue;
            info.addSectionHeading(market.getName() + " / " + market.getStarSystem().getName(),
                    INK, Color.BLACK, com.fs.starfarer.api.ui.Alignment.MID, 8f);
            info.addButton("预设防御", "MOB:DEFEND:" + market.getStarSystem().getId(),
                    INK, Color.BLACK, Math.max(210f, width - 22f), 22f, 3f);
            info.addButton("组织撤离", "MOB:EVACUATE:" + market.getStarSystem().getId(),
                    INK, Color.BLACK, Math.max(210f, width - 22f), 22f, 2f);
            count++;
        }
    }

    @Override public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        TooltipMakerAPI info = panel.createUIElement(width, height, true);
        createSmallDescription(info, width, height);
        panel.addUIElement(info).inTL(0f, 0f);
    }

    @Override public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (!(buttonId instanceof String)) return;
        String[] part = ((String) buttonId).split(":", 3);
        if (part.length != 3 || !"MOB".equals(part[0])) return;
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || state.late == null || state.phase != PTSDCrisisState.Phase.MOBILIZATION ||
                state.late.mobilizationOrdersUsed >= 3) return;
        if (!"DEFEND".equals(part[1]) && !"EVACUATE".equals(part[1])) return;
        if (state.playerMarkers.containsKey(part[2])) {
            Global.getSector().getCampaignUI().addMessage("[公开动员] 此星系已有部署命令");
            return;
        }
        if (state.late.humanResponseBudget < 15f) {
            Global.getSector().getCampaignUI().addMessage("[公开动员] 联合防务额度不足15点");
            return;
        }
        state.late.humanResponseBudget -= 15f;
        state.putMarker(part[2], part[1], 1.4f);
        state.late.mobilizationOrdersUsed++;
        PTSDCrisisDevIntel.report("公开动员部署", part[1] + " @ " + part[2] +
                "；联合防务额度-15", part[2], null);
        ui.updateUIForItem(this);
    }
}
