package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.Set;

/** A low-certainty news stream; exact targets and true reports remain DevMode-only. */
public final class PTSDCrisisNewsIntel extends BaseIntelPlugin {
    private static final long serialVersionUID = 1L;

    public PTSDCrisisNewsIntel() {
        setImportant(false);
    }

    public static PTSDCrisisNewsIntel ensureIntel() {
        if (Global.getSector() == null) return null;
        Object existing = Global.getSector().getIntelManager().getFirstIntel(PTSDCrisisNewsIntel.class);
        if (existing instanceof PTSDCrisisNewsIntel) return (PTSDCrisisNewsIntel) existing;
        PTSDCrisisNewsIntel intel = new PTSDCrisisNewsIntel();
        Global.getSector().getIntelManager().addIntel(intel);
        return intel;
    }

    public static void report(PTSDCrisisState.CrisisIncident incident) {
        if (incident == null || !incident.disclosed || Global.getSector() == null) return;
        PTSDCrisisNewsIntel intel = ensureIntel();
        if (intel != null) intel.sendUpdateIfPlayerHasIntel(incident, false, false);
    }

    @Override
    protected String getName() {
        return "边缘新闻：未归档信号";
    }

    @Override
    public String getSmallDescriptionTitle() {
        return getName();
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(getName(), getTitleColor(mode), 0f);
        PTSDCrisisState.CrisisIncident latest = latestDisclosed();
        if (latest != null) info.addPara(latest.headline, 3f, Misc.getGrayColor(), latest.headline);
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        PTSDCrisisState state = PTSDCrisisState.get();
        info.addPara("以下条目来自港务日志、民用频道、地方军方和匿名航路简报。它们未经统一核实，也不保证彼此描述的是同一种现象。", 10f);
        if (state == null) return;
        int shown = 0;
        for (int i = state.incidents.size() - 1; i >= 0 && shown < 14; i--) {
            PTSDCrisisState.CrisisIncident incident = state.incidents.get(i);
            if (incident == null || !incident.disclosed) continue;
            Color color = "火力侦察".equals(incident.category) ?
                    new Color(238, 151, 105) : new Color(170, 158, 188);
            info.addPara("%s　[%s / %s]", shown == 0 ? 10f : 8f, color,
                    incident.headline, incident.sourceLabel, age(incident.createdDay));
            info.addPara(incident.publicText, 2f, Misc.getTextColor(), new String[0]);
            if (Global.getSettings().isDevMode()) {
                info.addPara("[DEV真实记录] %s @ %s｜%s", 2f, Misc.getHighlightColor(),
                        incident.trueText, location(incident.targetSystemId), incident.effectSummary);
            }
            shown++;
        }
        if (shown == 0) info.addPara("目前没有足以进入公开汇编的记录。", 10f, Misc.getGrayColor(), new String[0]);
    }

    @Override
    public String getIcon() {
        FactionAPI faction = getFactionForUIColors();
        return faction == null ? null : faction.getCrest();
    }

    @Override
    public FactionAPI getFactionForUIColors() {
        FactionAPI independent = Global.getSector() == null ? null : Global.getSector().getFaction("independent");
        return independent == null ? super.getFactionForUIColors() : independent;
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add("危机");
        tags.add("外交通讯");
        return tags;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        PTSDCrisisState.CrisisIncident latest = latestDisclosed();
        if (latest == null || !latest.playerRelevant) return null;
        PTSDCrisisState state = PTSDCrisisState.get();
        StarSystemAPI system = state == null ? null : state.resolveSystem(latest.targetSystemId);
        return system == null ? null : system.getHyperspaceAnchor();
    }


    private static PTSDCrisisState.CrisisIncident latestDisclosed() {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return null;
        for (int i = state.incidents.size() - 1; i >= 0; i--) {
            PTSDCrisisState.CrisisIncident incident = state.incidents.get(i);
            if (incident != null && incident.disclosed) return incident;
        }
        return null;
    }

    private static String location(String systemId) {
        PTSDCrisisState state = PTSDCrisisState.get();
        StarSystemAPI system = state == null ? null : state.resolveSystem(systemId);
        return system == null ? "未知位置" : system.getName();
    }

    private static String age(float day) {
        float elapsed = Math.max(0f, PTSDCrisisState.getDay() - day);
        if (elapsed < 1f) return "今天";
        return Math.round(elapsed) + "天前";
    }
}
