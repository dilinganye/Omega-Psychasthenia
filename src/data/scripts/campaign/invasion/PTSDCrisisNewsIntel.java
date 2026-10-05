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
    private static final String TOGGLE_SUBSCRIPTION = "PTSD_NEWS_TOGGLE_SUBSCRIPTION";
    private static final String E2_ACTION_0 = "PTSD_E2_ACTION_0";
    private static final String E2_ACTION_1 = "PTSD_E2_ACTION_1";
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
        // The boolean is forceNoMessage. Unsubscribed articles remain available in the News
        // category and keep all simulation effects, but do not generate the lower-left push.
        Global.getSector().getIntelManager().addIntel(intel,
                !PTSDCrisisAPI.isNewsIntelSubscribed() || incident.silentNews);
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
        return item == null ? "过期的边缘新闻" : PTSDPlayerNewsText.headline(item);
    }
    @Override public String getSmallDescriptionTitle() { return getName(); }
    @Override public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        PTSDCrisisState.CrisisIncident item = incident();
        info.addPara(getName(), getTitleColor(mode), 0f);
        if (item != null) info.addPara("内容来源：" + PTSDPlayerNewsText.source(item), 3f, Misc.getGrayColor());
    }
    @Override public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        PTSDCrisisState.CrisisIncident item = incident();
        if (item == null) { info.addPara("该条目的原始记录已不存在。", 10f); return; }
        item.readByPlayer = true;
        Color news = "火力侦察".equals(item.category) ? new Color(238,151,105) : new Color(170,158,188);
        boolean subscribed = PTSDCrisisAPI.isNewsIntelSubscribed();
        info.addPara("新闻推送：%s。关闭后新闻仍会发生并进入新闻分类，但不会再弹出左下角 Intel 提示。",
                8f, subscribed ? Misc.getPositiveHighlightColor() : Misc.getGrayColor(),
                subscribed ? "已订阅" : "未订阅");
        info.addButton(subscribed ? "停止订阅新闻推送" : "订阅新闻推送",
                TOGGLE_SUBSCRIPTION, getFactionForUIColors().getBaseUIColor(),
                getFactionForUIColors().getDarkUIColor(), width, 24f, 3f);
        info.addPara("[%s / %s / %s]", 10f, news, PTSDPlayerNewsText.source(item),
                PTSDCrisisAPI.getSystemName(item.targetSystemId), remaining(item));
        info.addPara("%s", 8f, Misc.getTextColor(), PTSDPlayerNewsText.report(item));
        boolean e2News = item.cardId != null && item.cardId.startsWith("E2-");
        boolean e2Chain = e2News && ("E2-01".equals(item.cardId) ||
                "E2-02".equals(item.cardId) || "E2-03".equals(item.cardId));
        if (item.scenarioTrigger != null && !e2News) {
            info.addPara("目标：%s；干预窗口约剩 %s 日。", 6f, Misc.getHighlightColor(),
                    PTSDCrisisAPI.getSystemName(item.targetSystemId),
                    String.valueOf(Math.max(0, Math.round(item.scenarioDeadline - PTSDCrisisState.getDay()))));
            info.addPara("%s", 4f, Misc.getTextColor(), item.scenarioResolved ?
                    "归档：" + PTSDPlayerNewsText.summary(item) :
                    "可选行动：" + PTSDPlayerNewsText.action(item) + "。结果须在现场核查后判断。");
        } else if (e2Chain) {
            info.addPara("所涉港口：%s；约剩 %s 日核查。", 6f, Misc.getHighlightColor(),
                    PTSDCrisisAPI.getSystemName(item.targetSystemId),
                    String.valueOf(Math.max(0, Math.round(item.scenarioDeadline - PTSDCrisisState.getDay()))));
            if (item.scenarioResolved) info.addPara("%s", 4f, Misc.getTextColor(), "归档：" + PTSDPlayerNewsText.summary(item));
        } else if (e2News && item.scenarioResolved && item.effectSummary != null) {
            info.addPara("%s", 4f, Misc.getTextColor(), "归档：" + PTSDPlayerNewsText.summary(item));
        }
        if (e2News) {
            String status = PTSDE2Director.status(item);
            if (!status.isEmpty()) info.addPara("地方事件：" + status, 6f);
            String[] actions = PTSDE2Director.choices(item);
            if (actions != null) {
                info.addButton(actions[0], E2_ACTION_0, getFactionForUIColors().getBaseUIColor(),
                        getFactionForUIColors().getDarkUIColor(), width, 24f, 4f);
                info.addButton(actions[1], E2_ACTION_1, getFactionForUIColors().getBaseUIColor(),
                        getFactionForUIColors().getDarkUIColor(), width, 24f, 4f);
            }
        }
        if (item.investigable && !item.recordedByPlayer && !item.scenarioResolved) {
            info.addPara(item.scenarioTrigger == null ?
                    "报告中仍有一组可以定位的航路参数。记录后，位置将转入“边缘失联信号”。" :
                    "可记录线索并前往现场核查。基础取证后，是否执行后续行动由你决定。", 8f, Misc.getHighlightColor(), "记录");
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
        } else if (e2Chain) {
            info.addPara("前往所涉港口可核查当事船只；是否介入由现场情况决定。",
                    8f, Misc.getGrayColor());
        } else if (e2News) {
            info.addPara("地方短讯，无现场干预目标。报道仍可能有误。", 8f, Misc.getGrayColor());
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
        if (E2_ACTION_0.equals(buttonId) || E2_ACTION_1.equals(buttonId)) {
            if (!PTSDE2Director.choose(incident(), E2_ACTION_0.equals(buttonId) ? 0 : 1))
                Global.getSector().getCampaignUI().addMessage(
                        "该地方事件的目标、期限或费用条件已不满足。", Misc.getGrayColor());
            ui.updateUIForItem(this);
            return;
        }
        if (TOGGLE_SUBSCRIPTION.equals(buttonId)) {
            PTSDCrisisAPI.setNewsIntelSubscribed(!PTSDCrisisAPI.isNewsIntelSubscribed());
            ui.updateUIForItem(this);
            return;
        }
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
    @Override public String getIcon() {
        PTSDCrisisState.CrisisIncident item = incident();
        if (item != null && item.iconPath != null && item.iconPath.trim().length() > 0) {
            String path = item.iconPath.trim();
            try {
                Global.getSettings().getSprite(path);
                return path;
            } catch (Throwable invalidCustomIcon) {
                Global.getLogger(PTSDCrisisNewsIntel.class).warn(
                        "Invalid crisis news icon for " + item.cardId + ": " + path,
                        invalidCustomIcon);
                item.iconPath = "";
            }
        }
        FactionAPI f=getFactionForUIColors(); return f==null?null:f.getCrest();
    }
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
