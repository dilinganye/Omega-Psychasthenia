package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.Set;

/** One ten-day news entry. Recorded leads are copied into the persistent crisis Intel. */
public final class PTSDCrisisNewsIntel extends BaseIntelPlugin {
    private static final long serialVersionUID = 2L;
    private static final String RECORD = "PTSD_NEWS_RECORD";
    private String incidentId;

    public PTSDCrisisNewsIntel() { }
    private PTSDCrisisNewsIntel(String incidentId) { this.incidentId = incidentId; setImportant(false); }

    public static void report(PTSDCrisisState.CrisisIncident incident) {
        if (incident == null || !incident.disclosed || Global.getSector() == null) return;
        // Remove the legacy aggregate stream if an old save still contains it.
        for (com.fs.starfarer.api.campaign.comm.IntelInfoPlugin plugin :
                Global.getSector().getIntelManager().getIntel(PTSDCrisisNewsIntel.class)) {
            PTSDCrisisNewsIntel old = (PTSDCrisisNewsIntel) plugin;
            if (old.incidentId == null) Global.getSector().getIntelManager().removeIntel(old);
            else if (incident.id.equals(old.incidentId)) return;
        }
        PTSDCrisisNewsIntel intel = new PTSDCrisisNewsIntel(incident.id);
        Global.getSector().getIntelManager().addIntel(intel, true);
    }

    private PTSDCrisisState.CrisisIncident incident() { return PTSDCrisisAPI.getIncident(incidentId); }
    @Override protected String getName() {
        PTSDCrisisState.CrisisIncident item = incident();
        return item == null ? "\u8fc7\u671f\u7684\u8fb9\u7f18\u65b0\u95fb" : "\u8fb9\u7f18\u65b0\u95fb\uff1a" + item.headline;
    }
    @Override public String getSmallDescriptionTitle() { return getName(); }
    @Override public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        PTSDCrisisState.CrisisIncident item = incident();
        info.addPara(getName(), getTitleColor(mode), 0f);
        if (item != null) info.addPara(item.sourceLabel + " / " + remaining(item), 3f, Misc.getGrayColor());
    }
    @Override public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        PTSDCrisisState.CrisisIncident item = incident();
        if (item == null) { info.addPara("\u8be5\u6761\u76ee\u7684\u539f\u59cb\u8bb0\u5f55\u5df2\u4e0d\u5b58\u5728\u3002", 10f); return; }
        item.readByPlayer = true;
        Color news = "\u706b\u529b\u4fa6\u5bdf".equals(item.category) ? new Color(238,151,105) : new Color(170,158,188);
        info.addPara("[%s / %s]", 10f, news, item.sourceLabel, remaining(item));
        info.addPara(item.publicText, 8f);
        if (item.investigable && !item.recordedByPlayer) {
            info.addPara("\u62a5\u544a\u4e2d\u4ecd\u6709\u4e00\u7ec4\u53ef\u4ee5\u5b9a\u4f4d\u7684\u822a\u8def\u53c2\u6570\u3002\u8bb0\u5f55\u540e\uff0c\u4f4d\u7f6e\u5c06\u8f6c\u5165\u201c\u8fb9\u7f18\u5931\u8054\u4fe1\u53f7\u201d\u3002", 8f, Misc.getHighlightColor(), "\u8bb0\u5f55");
            info.addButton("\u8bb0\u5f55\u8c03\u67e5\u7ebf\u7d22", RECORD, getFactionForUIColors().getBaseUIColor(),
                    getFactionForUIColors().getDarkUIColor(), width, 24f, 8f);
        } else if (item.recordedByPlayer) {
            info.addPara("\u8be5\u6761\u76ee\u5df2\u8bb0\u5165\u201c\u8fb9\u7f18\u5931\u8054\u4fe1\u53f7\u201d\u3002", 8f, Misc.getHighlightColor(), "\u5df2\u8bb0\u5165");
        } else {
            info.addPara("\u8be5\u6761\u62a5\u9053\u6ca1\u6709\u8db3\u591f\u7684\u5b9a\u4f4d\u4fe1\u606f\uff0c\u53ea\u4f5c\u4e3a\u666e\u901a\u65b0\u95fb\u4fdd\u7559\u3002", 8f, Misc.getGrayColor());
        }
        if (Global.getSettings().isDevMode()) {
            info.addPara("[DEV] %s @ %s | %s | outcome=%s", 8f, Misc.getHighlightColor(), item.trueText,
                    PTSDCrisisAPI.getSystemName(item.targetSystemId), item.effectSummary, String.valueOf(item.investigationOutcome));
        }
    }
    @Override public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (RECORD.equals(buttonId)) { PTSDCrisisAPI.recordNewsIncident(incidentId); ui.updateIntelList(true); ui.updateUIForItem(this); return; }
        super.buttonPressConfirmed(buttonId, ui);
    }
    @Override public boolean shouldRemoveIntel() {
        PTSDCrisisState.CrisisIncident item = incident();
        return item == null || PTSDCrisisState.getDay() >= item.newsExpiresDay || super.shouldRemoveIntel();
    }
    @Override public SectorEntityToken getMapLocation(SectorMapAPI map) {
        PTSDCrisisState.CrisisIncident item = incident();
        if (item == null || !item.recordedByPlayer || item.investigationResolved && !item.investigationReal) return null;
        PTSDCrisisState state = PTSDCrisisState.get();
        StarSystemAPI system = state == null ? null : state.resolveSystem(item.targetSystemId);
        return system == null ? null : system.getHyperspaceAnchor();
    }
    @Override public String getIcon() { FactionAPI f=getFactionForUIColors(); return f==null?null:f.getCrest(); }
    @Override public FactionAPI getFactionForUIColors() {
        FactionAPI f=Global.getSector()==null?null:Global.getSector().getFaction("independent"); return f==null?super.getFactionForUIColors():f;
    }
    @Override public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags=super.getIntelTags(map); tags.add("\u65b0\u95fb"); tags.add("\u5916\u4ea4\u901a\u8baf"); return tags;
    }
    private static String remaining(PTSDCrisisState.CrisisIncident item) {
        return "\u5269\u4f59 " + Math.max(0, (int)Math.ceil(item.newsExpiresDay-PTSDCrisisState.getDay())) + " \u5929";
    }
}