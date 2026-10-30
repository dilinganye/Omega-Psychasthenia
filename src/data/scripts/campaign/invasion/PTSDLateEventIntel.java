package data.scripts.campaign.invasion;

import com.fs.starfarer.api.util.Misc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.Color;

/** Expiring action desk. All card reports also appear in the existing news Intel stream. */
public final class PTSDLateEventIntel extends BaseIntelPlugin {
    private static final long serialVersionUID = 1L;
    private static final Color CYAN = new Color(166, 214, 222);

    public static PTSDLateEventIntel ensureIntel() {
        if (Global.getSector() == null) return null;
        Object old = Global.getSector().getIntelManager().getFirstIntel(PTSDLateEventIntel.class);
        if (old instanceof PTSDLateEventIntel) return (PTSDLateEventIntel) old;
        PTSDLateEventIntel intel = new PTSDLateEventIntel();
        Global.getSector().getIntelManager().addIntel(intel);
        return intel;
    }

    @Override protected String getName() { return "危机事件簿：待决命令"; }
    @Override public String getSmallDescriptionTitle() { return getName(); }
    @Override public boolean hasLargeDescription() { return true; }

    @Override public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || state.late == null) return;
        info.addPara("公开报道与已下达命令在此归档。报道须另行核查，未收到的后续记录不会补作结论。", 0f);
        info.addButton("查看联盟行动与Je战区求援", "OPEN_RELIEF", width-24, 32, 8);
        int shown = 0;
        for (int i = state.late.eventCards.size() - 1; i >= 0 && shown < 20; i--) {
            PTSDLateState.EventCard card = state.late.eventCards.get(i);
            if (card == null || card.shortCard || (card.id != null && card.id.startsWith("FT-"))) continue;
            boolean revisedWar = card.id != null && (card.id.startsWith("MB-") || card.id.startsWith("WR-"));
            PTSDCrisisIncidentManager.ScriptedTemplate template = revisedWar ?
                    PTSDCrisisIncidentManager.getTemplate(card.id) : null;
            info.addSectionHeading(card.id + "《" + (template == null ? card.title : template.title) + "》", CYAN, Color.BLACK,
                    com.fs.starfarer.api.ui.Alignment.MID, 10f);
            info.addPara("%s", 4f, Misc.getTextColor(), "公开报道：" + (template == null ? card.report : template.report));
            if (!revisedWar) info.addPara("核实记录：" + card.intercept, 3f);
            else info.addPara("补充资料仍需核验；报道中的推测不代表已确认的敌方部署。", 3f);
            if (card.systemId != null && state.resolveSystem(card.systemId) != null)
                info.addPara("目标：" + state.resolveSystem(card.systemId).getName(), 3f);
            if (card.resolved) info.addPara(revisedWar ? "本次命令已归档。己方资金与物资收支以实际回执为准；敌方后续变化尚待观测。" : "结算：" + card.outcome, 3f);
            else if (card.accepted) {
                info.addPara(revisedWar ? "行动已下达，尚待现场完成回执。" : "执行中：" + card.outcome, 3f);
                SectorEntityToken target = card.targetEntityId == null ? null :
                        Global.getSector().getEntityById(card.targetEntityId);
                if (target != null) info.addPara("现场对象：" + target.getName(), 3f);
                info.addPara("剩余约 " + Math.max(0, Math.round(card.orderDeadlineDay -
                        PTSDCrisisState.getDay())) + " 日；完成现场行动后自动结算。", 3f);
                info.addButton("导航至任务星系", "GO:" + card.id, CYAN, Color.BLACK,
                        Math.max(220f, width - 24f), 22f, 2f);
            }
            else {
                if (PTSDCrisisState.getDay() >= card.expiresDay)
                    info.addPara("命令已过期；等待下一次战役结算归档。", 3f);
                else {
                    info.addPara("剩余约 " + Math.max(0, Math.round(card.expiresDay - PTSDCrisisState.getDay())) +
                            " 日可下达命令。", 3f);
                    info.addButton(card.firstChoice, "LATE:1:" + card.id, CYAN, Color.BLACK,
                            Math.max(220f, width - 24f), 22f, 3f);
                    info.addButton(card.secondChoice, "LATE:2:" + card.id, CYAN, Color.BLACK,
                            Math.max(220f, width - 24f), 22f, 2f);
                }
            }
            shown++;
        }
    }

    @Override public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        PTSDCommandSurface.document(panel,width,height,getName(),"战役档案 / 行动回执",PTSDCommandSurface.AMBER,
                new PTSDCommandSurface.Content(){public void draw(TooltipMakerAPI info,float w,float h){createSmallDescription(info,w,h);}});
    }

    @Override public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (!(buttonId instanceof String)) return;
        String input = (String) buttonId;
        if("OPEN_RELIEF".equals(input)){
            PTSDWarIntel war=PTSDWarIntel.ensureIntel();war.coalitionView().openReliefPage();
            Global.getSector().getCampaignUI().showCoreUITab(com.fs.starfarer.api.campaign.CoreUITabId.INTEL,war);return;
        }
        if (input.startsWith("GO:")) {
            PTSDCrisisState state = PTSDCrisisState.get();
            if (state == null || state.late == null) return;
            for (PTSDLateState.EventCard card : state.late.eventCards) {
                if (card == null || !input.substring(3).equals(card.id)) continue;
                SectorEntityToken target = card.targetEntityId == null ? null :
                        Global.getSector().getEntityById(card.targetEntityId);
                if (target == null && state.resolveSystem(card.systemId) != null)
                    target = state.resolveSystem(card.systemId).getHyperspaceAnchor();
                if (target != null) Global.getSector().layInCourseFor(target);
                return;
            }
        }
        String[] part = input.split(":", 3);
        if (part.length != 3 || !"LATE".equals(part[0])) return;
        if (PTSDLateEventDirector.choose(PTSDCrisisState.get(), part[2],
                "1".equals(part[1]) ? 1 : 2)) ui.updateUIForItem(this);
    }
}
