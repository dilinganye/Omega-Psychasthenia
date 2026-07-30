package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.Set;

/** Intel deliberately reveals only the crisis stage that the player has personally brushed against. */
public final class PTSDCrisisIntel extends BaseIntelPlugin {
    private static final long serialVersionUID = 1L;
    private boolean devOnlyPreview;

    public PTSDCrisisIntel() {
        setImportant(true);
    }

    public static PTSDCrisisIntel ensureIntel() {
        if (Global.getSector() == null) return null;
        Object existing = Global.getSector().getIntelManager().getFirstIntel(PTSDCrisisIntel.class);
        if (existing instanceof PTSDCrisisIntel) {
            PTSDCrisisIntel intel = (PTSDCrisisIntel) existing;
            intel.devOnlyPreview = false;
            PTSDCrisisState state = PTSDCrisisState.get();
            if (state != null) state.preWarIntelCreated = true;
            return intel;
        }
        PTSDCrisisIntel intel = new PTSDCrisisIntel();
        Global.getSector().getIntelManager().addIntel(intel);
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null) state.preWarIntelCreated = true;
        return intel;
    }

    public static PTSDCrisisIntel ensureDevPreview() {
        if (Global.getSector() == null || !Global.getSettings().isDevMode()) return null;
        Object existing = Global.getSector().getIntelManager().getFirstIntel(PTSDCrisisIntel.class);
        if (existing instanceof PTSDCrisisIntel) return (PTSDCrisisIntel) existing;
        PTSDCrisisIntel intel = new PTSDCrisisIntel();
        intel.devOnlyPreview = true;
        Global.getSector().getIntelManager().addIntel(intel, true);
        return intel;
    }

    public static void removeDevPreview() {
        if (Global.getSector() == null) return;
        for (com.fs.starfarer.api.campaign.comm.IntelInfoPlugin plugin :
                new java.util.ArrayList<com.fs.starfarer.api.campaign.comm.IntelInfoPlugin>(
                        Global.getSector().getIntelManager().getIntel(PTSDCrisisIntel.class))) {
            PTSDCrisisIntel intel = (PTSDCrisisIntel) plugin;
            if (intel.devOnlyPreview) Global.getSector().getIntelManager().removeIntel(intel);
        }
    }
    public void replaceWithWarIntel() {
        if (!isEnding() && !isEnded()) endImmediately();
    }

    @Override
    protected String getName() {
        return devOnlyPreview ? "[DEV预览] 边缘失联信号" : "边缘失联信号";
    }

    @Override
    public boolean shouldRemoveIntel() {
        return (devOnlyPreview && !Global.getSettings().isDevMode()) || super.shouldRemoveIntel();
    }

    @Override
    public String getSmallDescriptionTitle() {
        return getName();
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        PTSDCrisisState state = PTSDCrisisState.get();
        float opad = 10f;
        Color h = Misc.getHighlightColor();
        Color omega = getFactionForUIColors().getBaseUIColor();

        info.addPara("一些来源不明的失联记录正从边缘星系累积。没有任何一条报告足以独立证明异常，但它们呈现出令人不安的重复性。", opad);
        if (state == null || state.visibleStage <= 0) {
            info.addPara("当前没有可靠的阶段判断。只有亲自遭遇相关舰队或事件，新的情报才会被拼入记录。", opad, Misc.getGrayColor(), "亲自遭遇");
            return;
        }

        if (state.visibleStage == 1) {
            info.addPara("推测阶段：%s。少量无法识别的舰队似乎在通讯设施附近短暂停留，受到接近后会立即撤离。", opad, h, "远距侦察");
        } else if (state.visibleStage == 2) {
            info.addPara("推测阶段：%s。若干星体的光谱和环境参数开始偏离历史记录，但信息仍然零散。", opad, h, "据点营建");
        } else {
            info.addPara("推测阶段：%s。异常活动正围绕若干星系固化，部分航路已经不再适合常规舰队深入。", opad, omega, "大规模行动准备");
        }
        info.addPara("已确认的异常接触：%s；被目击的侦察单位：%s。", opad, h,
                String.valueOf(state.totalOmegaEncounters), String.valueOf(state.totalScoutSightings));
    }

    @Override
    public String getIcon() {
        FactionAPI faction = getFactionForUIColors();
        return faction == null ? null : faction.getCrest();
    }

    @Override
    public FactionAPI getFactionForUIColors() {
        FactionAPI watcher = Global.getSector() == null ? null : Global.getSector().getFaction(IIRT_Omega_Invasion.WATCHER_FACTION);
        if (watcher != null) return watcher;
        return super.getFactionForUIColors();
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(IIRT_Omega_Invasion.WATCHER_FACTION);
        tags.add("危机");
        return tags;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return null;
        PTSDCrisisState.SystemData best = null;
        for (PTSDCrisisState.SystemData data : state.systems.values()) {
            if (!data.knownToPlayer) continue;
            if (best == null || data.lastObservedDay > best.lastObservedDay) best = data;
        }
        if (best == null) return null;
        StarSystemAPI system = state.resolveSystem(best.systemId);
        return system == null ? null : system.getHyperspaceAnchor();
    }
}