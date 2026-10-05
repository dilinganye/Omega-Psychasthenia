package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.invasion.news.ft.AbstractFTHandler;
import java.awt.Color;

/** Player-facing journal of active FT leads. Internal values stay in Dev Intel. */
public final class PTSDFTIntel extends BaseIntelPlugin {
    private static final long serialVersionUID = 1L;
    public static PTSDFTIntel ensureIntel() {
        Object existing = Global.getSector().getIntelManager().getFirstIntel(PTSDFTIntel.class);
        if (existing instanceof PTSDFTIntel) return (PTSDFTIntel) existing;
        PTSDFTIntel intel = new PTSDFTIntel();
        Global.getSector().getIntelManager().addIntel(intel);
        return intel;
    }
    @Override protected String getName() { return "待核实航路线索"; }
    @Override public String getSmallDescriptionTitle() { return getName(); }
    @Override public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || state.late == null) return;
        for (PTSDLateState.FTChain chain : state.late.ftChains.values()) {
            if (chain == null || "FT-07".equals(chain.id) || "FT-08".equals(chain.id) ||
                    "FT-04".equals(chain.id)) continue;
            PTSDCrisisIncidentManager.ScriptedTemplate template = PTSDCrisisIncidentManager.getTemplate(chain.id);
            if (template == null) continue;
            info.addSectionHeading("FT-09".equals(chain.id) ? "匿名残片定位" : template.title,
                    new Color(171, 204, 222), Color.BLACK,
                    com.fs.starfarer.api.ui.Alignment.MID, 8f);
            if ("INTEL".equals(template.visibility))
                info.addPara("一份匿名残片定位记录已转入舰内调查。", 4f);
            else if (!"DEV_ONLY".equals(template.visibility)) info.addPara(template.report, 4f);
            Object handler = PTSDCrisisNewsAPI.resolveHandler(template.targetExpression.substring(7,
                    template.targetExpression.length() - 1));
            if (handler instanceof AbstractFTHandler) {
                AbstractFTHandler ft = (AbstractFTHandler) handler;
                info.addPara(PTSDFTDirector.playerText(ft.status(chain)), 4f);
                if (!chain.resolved && "FT-10".equals(chain.id))
                    info.addButton("与舰内研究组交谈", "DIALOG:" + chain.id,
                            new Color(171, 204, 222), Color.BLACK,
                            Math.max(230f, width - 24f), 22f, 3f);
                else if (!chain.resolved) for (String action : ft.actions(state, chain))
                    info.addButton(action, "FT:" + chain.id + ":" + action,
                            new Color(171, 204, 222), Color.BLACK,
                            Math.max(230f, width - 24f), 22f, 3f);
            }
            boolean exact = !"FT-02".equals(chain.id) ||
                    "EXACT_COORDINATES".equals(chain.node) || "CONVOY_SIGHTED".equals(chain.node);
            if (chain.systemId != null && state.resolveSystem(chain.systemId) != null && exact) {
                info.addPara("线索区域：" + state.resolveSystem(chain.systemId).getName(), 4f);
                if (!chain.resolved) info.addButton("设置航线", "GO:" + chain.id,
                        new Color(171, 204, 222), Color.BLACK,
                        Math.max(230f, width - 24f), 22f, 3f);
            }
            if ("FT-02".equals(chain.id) && !exact && chain.marketId != null &&
                    state.resolveMarket(chain.marketId) != null) {
                info.addPara("可询问的观测员曾在 " + state.resolveMarket(chain.marketId).getName() +
                        " 的酒馆出现。", 4f);
                info.addButton("导航至酒馆所在港口", "GO_BAR:FT-02",
                        new Color(171, 204, 222), Color.BLACK,
                        Math.max(230f, width - 24f), 22f, 3f);
            }
        }
    }
    @Override public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (!(buttonId instanceof String)) return;
        String value = (String) buttonId;
        if (value.startsWith("GO_BAR:")) {
            PTSDCrisisState state = PTSDCrisisState.get();
            PTSDLateState.FTChain chain = state == null ? null : state.late.ftChains.get(value.substring(7));
            if (chain != null && state.resolveMarket(chain.marketId) != null)
                Global.getSector().layInCourseFor(state.resolveMarket(chain.marketId).getPrimaryEntity());
        } else if (value.startsWith("GO:")) {
            PTSDCrisisState state = PTSDCrisisState.get();
            PTSDLateState.FTChain chain = state == null ? null : state.late.ftChains.get(value.substring(3));
            SectorEntityToken target = PTSDFTDirector.target(chain);
            if (target == null && chain != null && state.resolveSystem(chain.systemId) != null)
                target = state.resolveSystem(chain.systemId).getHyperspaceAnchor();
            if (target != null) Global.getSector().layInCourseFor(target);
        } else if (value.startsWith("DIALOG:")) {
            PTSDFTDialog.open(value.substring(7));
        } else if (value.startsWith("FT:")) {
            String[] parts = value.split(":", 3);
            if (parts.length == 3 && PTSDFTDirector.choose(PTSDCrisisState.get(), parts[1], parts[2])) {
                ui.updateUIForItem(this);
                if ("FT-09".equals(parts[1]) ||
                        ("FT-05".equals(parts[1]) && "联络幸存舰".equals(parts[2])))
                    PTSDFTDialog.open(parts[1]);
            }
        }
    }
}
