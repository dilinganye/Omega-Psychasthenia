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
        return showAll ? "[DEV] \u5168\u661f\u7cfb\u653b\u51fb\u6743\u6570" : "[DEV] \u6b96\u6c11\u661f\u57df\u653b\u51fb\u6743\u91cd";
    }
    @Override public String getSmallDescriptionTitle() { return getName(); }
    @Override public boolean hasLargeDescription() { return true; }
    @Override public boolean shouldRemoveIntel() {
        return !Global.getSettings().isDevMode() || super.shouldRemoveIntel();
    }
    @Override public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        info.addPara("\u663e\u793a\u7b2c\u56db\u7aa5\u89c6/\u7cbe\u795e\u521b\u4f24\u5185\u90e8\u661f\u7cfb\u8bc4\u5206\u3002\u5173\u95ed DevMode \u540e\u81ea\u52a8\u79fb\u9664\u3002", 10f);
    }

    @Override public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        TooltipMakerAPI info = panel.createUIElement(width, height, true);
        FactionAPI faction = getFactionForUIColors();
        Color base = faction == null ? Misc.getBasePlayerColor() : faction.getBaseUIColor();
        Color dark = faction == null ? Misc.getDarkPlayerColor() : faction.getDarkUIColor();
        PTSDCrisisState state = PTSDCrisisState.get();
        info.addSectionHeading(getName(), base, dark, com.fs.starfarer.api.ui.Alignment.MID, 0f);
        info.addPara("\u6743\u91cd\u8d8a\u9ad8\uff0c\u5168\u9762\u6218\u4e89\u4e2d\u8d8a\u53ef\u80fd\u6210\u4e3a\u90e8\u7f72\u65b9\u5411\u3002\u4fa6\u5bdf\u5f3a\u5ea6\u662f\u4fa6\u5bdf\u8230\u6bcf\u65e5\u6837\u672c\u7684\u6700\u9ad8\u503c\uff1b\u4f30\u8ba1\u9632\u5fa1\u8d8a\u4f4e\uff0c\u653b\u51fb\u6743\u91cd\u8d8a\u9ad8\u3002", 8f);
        info.addPara("\u201c\u5efa\u8bae\u5360\u9886\u201d\u8868\u793a\u8be5\u661f\u7cfb\u65e0\u975e\u5371\u673a\u6b96\u6c11\u5730\u3001\u65e0\u5176\u4ed6\u52bf\u529b\u8230\u961f\uff0c\u4e14\u5c1a\u672a\u88ab\u7cbe\u795e\u521b\u4f24\u63a7\u5236\u3002", 5f);
        info.addButton("\u4ec5\u663e\u793a\u6709\u6b96\u6c11\u5730\u7684\u661f\u57df", SHOW_COLONIZED, base, dark, width, 24f, 8f);
        info.addButton("\u663e\u793a\u6240\u6709\u661f\u7cfb\u653b\u51fb\u6743\u6570", SHOW_ALL, base, dark, width, 24f, 2f);
        info.addButton("\u8fd4\u56de\u5371\u673a\u76d1\u89c6\u5668", BACK, base, dark, width, 24f, 2f);
        if (state == null) {
            info.addPara("\u5371\u673a\u72b6\u6001\u5c1a\u672a\u521d\u59cb\u5316\u3002", 10f);
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
            info.addPara("#%s %s | \u653b\u51fb\u6743\u91cd %s | \u4fa6\u5bdf\u8230\u961f\u5f3a\u5ea6 %s | \u5e02\u573a\u9632\u5fa1 %s | \u7f6e\u4fe1 %s%%",
                    rank == 1 ? 9f : 6f, color, String.valueOf(rank), system.getName(), f(data.attackWeight),
                    f(data.observedFleetStrength), f(data.observedMarketDefense), String.valueOf(Math.round(confidence * 100f)));
            String role = data.occupationSuggested ? "\u5efa\u8bae\u5360\u9886 / \u5360\u9886\u6743\u91cd " + f(data.occupationWeight) : "\u5e38\u89c4\u8fdb\u653b\u8bc4\u5206";
            info.addPara("\u5f53\u65e5\u6700\u9ad8 %s (\u62a5\u544a %s) | \u5386\u53f2\u65e5\u6837\u672c %s | \u6b96\u6c11\u5730 %s | \u975e\u5371\u673a\u8230\u961f %s | Omega\u63a7\u5236 %s | %s",
                    1f, Misc.getGrayColor(), f(data.reconDailyMax), String.valueOf(data.reconDailyReports),
                    String.valueOf(data.reconStrengthHistory == null ? 0 : data.reconStrengthHistory.size()),
                    yn(data.hasNonCrisisColony), yn(data.hasNonCrisisFleet), f(data.omegaControl), role);
        }
        if (rank == 0) info.addPara("\u5f53\u524d\u89c6\u56fe\u6ca1\u6709\u53ef\u663e\u793a\u7684\u661f\u7cfb\uff0c\u8bf7\u7b49\u5f85\u4e0b\u4e00\u6b21\u6743\u91cd\u66f4\u65b0\u3002", 10f);
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
        Set<String> tags = super.getIntelTags(map); tags.add("DEV"); tags.add("\u5371\u673a"); return tags;
    }
    private static String f(float value) { return String.valueOf(Math.round(value * 10f) / 10f); }
    private static String yn(boolean value) { return value ? "\u662f" : "\u5426"; }
}