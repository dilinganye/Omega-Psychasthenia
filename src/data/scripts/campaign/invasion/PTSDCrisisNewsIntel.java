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

    public String getIncidentId() { return incidentId; }
    public static PTSDCrisisNewsIntel find(String id) {
        if (id == null || Global.getSector() == null) return null;
        for (com.fs.starfarer.api.campaign.comm.IntelInfoPlugin plugin : Global.getSector().getIntelManager().getIntel(PTSDCrisisNewsIntel.class)) {
            PTSDCrisisNewsIntel intel = (PTSDCrisisNewsIntel) plugin;
            if (id.equals(intel.incidentId)) return intel;
        }
        return null;
    }
    private PTSDCrisisState.CrisisIncident incident() { return PTSDCrisisAPI.getIncident(incidentId); }
    @Override protected String getName() {
        PTSDCrisisState.CrisisIncident item = incident();
        return item == null ? "过期的边缘新闻" : "边缘新闻：" + item.headline;
    }
    @Override public String getSmallDescriptionTitle() { return getName(); }
    @Override public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        PTSDCrisisState.CrisisIncident item = incident();
        info.addPara(getName(), getTitleColor(mode), 0f);
        if (item != null) info.addPara(item.sourceLabel + " / " +
                PTSDCrisisAPI.getSystemName(item.targetSystemId) + " / " + remaining(item),
                3f, Misc.getGrayColor());
    }
    @Override public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        PTSDCrisisState.CrisisIncident item = incident();
        if (item == null) { info.addPara("该条目的原始记录已不存在。", 10f); return; }
        item.readByPlayer = true;
        Color news = "火力侦察".equals(item.category) ? new Color(238,151,105) : new Color(170,158,188);
        info.addPara("[%s / %s / %s]", 10f, news, item.sourceLabel,
                PTSDCrisisAPI.getSystemName(item.targetSystemId), remaining(item));
        info.addPara(item.publicText, 8f);
        if (item.investigable && !item.recordedByPlayer) {
            info.addPara("报告中仍有一组可以定位的航路参数。记录后，位置将转入“边缘失联信号”。", 8f, Misc.getHighlightColor(), "记录");
            info.addButton("记录调查线索", RECORD, getFactionForUIColors().getBaseUIColor(),
                    getFactionForUIColors().getDarkUIColor(), width, 24f, 8f);
        } else if (item.recordedByPlayer) {
            info.addPara("该条目已记入“边缘失联信号”。", 8f, Misc.getHighlightColor(), "已记入");
            if (item.siteMaterialized) {
                info.addPara("现场投影：%s。%s", 6f, new Color(206,142,255),
                        item.siteTitle == null ? "未分类异常" : item.siteTitle,
                        item.siteConfirmationHint == null ? "抵近并扫描现场。" : item.siteConfirmationHint);
            } else if (!item.investigationResolved) {
                info.addPara("进入目标星系后，现场证据会按新闻类型进行实体化；最长保留30日。", 6f, Misc.getGrayColor());
            }
        } else {
            info.addPara("该条报道没有足够的定位信息，只作为普通新闻保留。", 8f, Misc.getGrayColor());
        }
        if (Global.getSettings().isDevMode()) {
            info.addPara("[DEV] %s @ %s | %s | outcome=%s", 8f, Misc.getHighlightColor(), item.trueText,
                    PTSDCrisisAPI.getSystemName(item.targetSystemId), item.effectSummary, String.valueOf(item.investigationOutcome));
            info.addPara("[DEV现场] template=%s | handler=%s | martial=%s/%s | spawned=%s", 3f,
                    Misc.getHighlightColor(), String.valueOf(item.siteTemplate), String.valueOf(item.siteHandlerExpression),
                    String.valueOf(item.martialSiteEligible), String.valueOf(item.martialSiteSpawned), String.valueOf(item.siteMaterialized));
        }
    }
    @Override public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (RECORD.equals(buttonId)) { PTSDCrisisAPI.recordNewsIncident(incidentId); PTSDCrisisIntel task = PTSDCrisisIntel.ensureIntel(); ui.updateIntelList(true); ui.selectItem(task); return; }
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
        Set<String> tags=super.getIntelTags(map);
        tags.add("新闻");
        // tags.add("外交通讯");
        return tags;
    }
    private static String remaining(PTSDCrisisState.CrisisIncident item) {
        return "剩余 " + Math.max(0, (int)Math.ceil(item.newsExpiresDay-PTSDCrisisState.getDay())) + " 天";
    }
}