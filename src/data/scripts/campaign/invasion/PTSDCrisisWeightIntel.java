package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Dev-only audit of learned system strength and strategic attack/occupation weights. */
public final class PTSDCrisisWeightIntel extends BaseIntelPlugin {
    private static final long serialVersionUID = 1L;
    private static final String SHOW_COLONIZED = "PTSD_WEIGHT_COLONIZED";
    private static final String SHOW_ALL = "PTSD_WEIGHT_ALL";
    private static final String BACK = "PTSD_WEIGHT_BACK";
    private boolean showAll;

    public PTSDCrisisWeightIntel() { setImportant(true); }

    public static PTSDCrisisWeightIntel ensure(boolean allSystems) {
        if (Global.getSector() == null || !Global.getSettings().isDevMode()) return null;
        Object found = Global.getSector().getIntelManager().getFirstIntel(PTSDCrisisWeightIntel.class);
        PTSDCrisisWeightIntel intel;
        if (found instanceof PTSDCrisisWeightIntel) intel = (PTSDCrisisWeightIntel) found;
        else {
            intel = new PTSDCrisisWeightIntel();
            Global.getSector().getIntelManager().addIntel(intel, true);
        }
        intel.showAll = allSystems;
        return intel;
    }

    public static void removeWhenNotDev() {
        if (Global.getSector() == null || Global.getSettings().isDevMode()) return;
        for (IntelInfoPlugin intel : new ArrayList<IntelInfoPlugin>(
                Global.getSector().getIntelManager().getIntel(PTSDCrisisWeightIntel.class))) {
            Global.getSector().getIntelManager().removeIntel(intel);
        }
    }

    @Override protected String getName() {
        return showAll ? "[DEV] 全星系攻击权数" : "[DEV] 殖民星域攻击权重";
    }
    @Override public String getSmallDescriptionTitle() { return getName(); }
    @Override public boolean hasLargeDescription() { return true; }
    @Override public boolean shouldRemoveIntel() {
        return !Global.getSettings().isDevMode() || super.shouldRemoveIntel();
    }
    @Override public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        info.addPara("显示第四窥视/精神创伤内部星系评分。关闭 DevMode 后自动移除。", 10f);
    }

    @Override public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        TooltipMakerAPI info = panel.createUIElement(width, height, true);
        FactionAPI faction = getFactionForUIColors();
        Color base = faction == null ? Misc.getBasePlayerColor() : faction.getBaseUIColor();
        Color dark = faction == null ? Misc.getDarkPlayerColor() : faction.getDarkUIColor();
        PTSDCrisisState state = PTSDCrisisState.get();
        info.addSectionHeading(getName(), base, dark, com.fs.starfarer.api.ui.Alignment.MID, 0f);
        info.addPara("权重越高，全面战争中越可能成为部署方向。侦察强度是侦察舰每日样本的最高值；估计防御越低，攻击权重越高。", 8f);
        info.addPara("“建议占领”表示该星系无非危机殖民地、无其他势力舰队，且尚未被精神创伤控制。", 5f);
        info.addButton("仅显示有殖民地的星域", SHOW_COLONIZED, base, dark, width, 24f, 8f);
        info.addButton("显示所有星系攻击权数", SHOW_ALL, base, dark, width, 24f, 2f);
        info.addButton("返回危机监视器", BACK, base, dark, width, 24f, 2f);
        if (state == null) {
            info.addPara("危机状态尚未初始化。", 10f);
            panel.addUIElement(info).inTL(0f, 0f); return;
        }
        List<PTSDCrisisState.SystemData> rows = new ArrayList<PTSDCrisisState.SystemData>(state.systems.values());
        Collections.sort(rows, new Comparator<PTSDCrisisState.SystemData>() {
            @Override public int compare(PTSDCrisisState.SystemData a, PTSDCrisisState.SystemData b) {
                return Float.compare(b.attackWeight, a.attackWeight);
            }
        });
        int rank = 0;
        for (PTSDCrisisState.SystemData data : rows) {
            if (!showAll && !data.hasNonCrisisColony) continue;
            StarSystemAPI system = state.resolveSystem(data.systemId);
            if (system == null) continue;
            rank++;
            float confidence = Math.min(1f, 0.12f + data.scoutVisits * 0.16f + data.playerSightings * 0.05f + state.reconConfidence / 250f);
            Color color = data.occupationSuggested ? Color.ORANGE : (data.attackWeight >= 25f ? Misc.getNegativeHighlightColor() : Misc.getHighlightColor());
            info.addPara("#%s %s | 攻击权重 %s | 侦察舰队强度 %s | 市场防御 %s | 置信 %s%%",
                    rank == 1 ? 9f : 6f, color, String.valueOf(rank), system.getName(), f(data.attackWeight),
                    f(data.observedFleetStrength), f(data.observedMarketDefense), String.valueOf(Math.round(confidence * 100f)));
            String role = data.occupationSuggested ? "建议占领 / 占领权重 " + f(data.occupationWeight) : "常规进攻评分";
            info.addPara("当日最高 %s (报告 %s) | 历史日样本 %s | 殖民地 %s | 非危机舰队 %s | Omega控制 %s | %s",
                    1f, Misc.getGrayColor(), f(data.reconDailyMax), String.valueOf(data.reconDailyReports),
                    String.valueOf(data.reconStrengthHistory == null ? 0 : data.reconStrengthHistory.size()),
                    yn(data.hasNonCrisisColony), yn(data.hasNonCrisisFleet), f(data.omegaControl), role);
        }
        if (rank == 0) info.addPara("当前视图没有可显示的星系，请等待下一次权重更新。", 10f);
        panel.addUIElement(info).inTL(0f, 0f);
    }

    @Override public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (!Global.getSettings().isDevMode()) return;
        if (SHOW_COLONIZED.equals(buttonId)) { showAll = false; ui.updateUIForItem(this); return; }
        if (SHOW_ALL.equals(buttonId)) { showAll = true; ui.updateUIForItem(this); return; }
        if (BACK.equals(buttonId)) {
            PTSDCrisisDevIntel monitor = PTSDCrisisDevIntel.ensureIntel();
            if (monitor != null) { ui.updateIntelList(true); ui.selectItem(monitor); }
            return;
        }
        super.buttonPressConfirmed(buttonId, ui);
    }
    @Override public String getIcon() {
        FactionAPI faction = getFactionForUIColors();
        return faction == null ? null : faction.getCrest();
    }
    @Override public FactionAPI getFactionForUIColors() {
        FactionAPI faction = Global.getSector() == null ? null : Global.getSector().getFaction(IIRT_Omega_Invasion.WATCHER_FACTION);
        return faction == null ? super.getFactionForUIColors() : faction;
    }
    @Override public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map); tags.add("DEV"); tags.add("危机"); return tags;
    }
    private static String f(float value) { return String.valueOf(Math.round(value * 10f) / 10f); }
    private static String yn(boolean value) { return value ? "是" : "否"; }
}