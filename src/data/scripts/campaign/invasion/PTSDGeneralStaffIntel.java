package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;

import java.awt.Color;
import java.util.Map;

/** Separate command ledger; the existing War Intel remains the operational theater map. */
public final class PTSDGeneralStaffIntel extends BaseIntelPlugin {
    private static final long serialVersionUID = 1L;
    private static final Color HIGHLIGHT = new Color(155, 210, 240);
    private int page;

    public static PTSDGeneralStaffIntel ensureIntel() {
        if (Global.getSector() == null) return null;
        Object previous = Global.getSector().getIntelManager().getFirstIntel(PTSDGeneralStaffIntel.class);
        if (previous instanceof PTSDGeneralStaffIntel) return (PTSDGeneralStaffIntel) previous;
        PTSDGeneralStaffIntel intel = new PTSDGeneralStaffIntel();
        Global.getSector().getIntelManager().addIntel(intel);
        return intel;
    }

    @Override protected String getName() { return "联合总参部"; }
    @Override public String getSmallDescriptionTitle() { return getName(); }
    @Override public boolean hasLargeDescription() { return true; }

    @Override public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || state.late == null) return;
        info.addPara("各势力席位依现存规模与已核实贡献计算。个人战绩单列，归属按行动发生时锁定。", 0f);
        info.addButton("席位与贡献", "PAGE:0", HIGHLIGHT, Color.BLACK,
                Math.max(220f, width - 24f), 22f, 6f);
        info.addButton("提案与表决", "PAGE:1", HIGHLIGHT, Color.BLACK,
                Math.max(220f, width - 24f), 22f, 2f);
        info.addButton("战区资源与后备队", "PAGE:2", HIGHLIGHT, Color.BLACK,
                Math.max(220f, width - 24f), 22f, 2f);
        info.addButton("战线与军报地图", "PAGE:3", HIGHLIGHT, Color.BLACK,
                Math.max(220f, width - 24f), 22f, 2f);
        if (page == 0) {
        info.addSectionHeading("席位与贡献", HIGHLIGHT, Color.BLACK, com.fs.starfarer.api.ui.Alignment.MID, 10f);
        info.addPara("玩家个人：%s", 8f, HIGHLIGHT,
                String.valueOf(Math.round(PTSDGeneralStaffDirector.playerScore(state))));
        for (Map.Entry<String, Float> entry : PTSDGeneralStaffDirector.seats(state).entrySet()) {
            info.addPara(entry.getKey() + "：" + Math.round(entry.getValue()), 3f);
        }
        java.util.Set<String> candidates = new java.util.LinkedHashSet<String>();
        candidates.add(Global.getSector().getPlayerFaction().getId());
        for (com.fs.starfarer.api.campaign.econ.MarketAPI market : PTSDOccupationManager.getAllMarkets())
            if (market != null && !market.isPlanetConditionMarketOnly()) candidates.add(market.getFactionId());
        for (String factionId : candidates) {
            if (state.late.coalitionMembers.contains(factionId) ||
                    IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(factionId) ||
                    IIRT_Omega_Invasion.WATCHER_FACTION.equals(factionId)) continue;
            com.fs.starfarer.api.campaign.FactionAPI faction = Global.getSector().getFaction(factionId);
            if (faction == null) continue;
            if (state.late.coalitionDiplomaticReplies.containsKey(factionId))
                info.addPara("%s：%s", 3f, HIGHLIGHT, faction.getDisplayName(), state.late.coalitionDiplomaticReplies.get(factionId));
            if (!state.late.coalitionJoinRequests.containsKey(factionId))
                info.addButton("邀请入盟：" + faction.getDisplayName(), "COALITION:JOIN:" + factionId, HIGHLIGHT,
                        Color.BLACK, Math.max(220f, width - 24f), 22f, 2f);
        }
        for (PTSDLateState.Contribution item : state.late.contributions) {
            if (item != null) info.addPara(item.type + " / " + item.actorId + " → " +
                    item.factionAtTime + "：" + Math.round(item.amount), 2f);
        }
        return;
        }
        if (page == 1) {
        info.addSectionHeading("已登记战区资源", HIGHLIGHT, Color.BLACK, com.fs.starfarer.api.ui.Alignment.MID, 10f);
        info.addPara("仍在行动的后备队：" + state.late.reserves.size() +
                "；四处主爆发点：" + state.late.selectedBurstCount, 5f);
        for (PTSDLateState.Front front : state.late.fronts.values()) {
            if (front == null || front.resolved) continue;
            info.addSectionHeading("增援提案：" + front.systemId, HIGHLIGHT, Color.BLACK,
                    com.fs.starfarer.api.ui.Alignment.MID, 10f);
            info.addPara("当前已承诺防御额度：" + Math.round(front.defense), 3f);
            info.addButton("提交战绩与情报", "STAFF:EVIDENCE:" + front.id, HIGHLIGHT,
                    Color.BLACK, Math.max(220f, width - 24f), 22f, 4f);
            info.addButton("外交施压（1故事点）", "STAFF:PRESSURE:" + front.id, HIGHLIGHT,
                    Color.BLACK, Math.max(220f, width - 24f), 22f, 2f);
            info.addButton("提供60单位补给", "STAFF:SUPPLIES:" + front.id, HIGHLIGHT,
                    Color.BLACK, Math.max(220f, width - 24f), 22f, 2f);
            info.addButton("承诺殖民地补给出口30日", "STAFF:EXPORT:" + front.id, HIGHLIGHT,
                    Color.BLACK, Math.max(220f, width - 24f), 22f, 2f);
        }
        for (PTSDLateState.Resolution resolution : state.late.resolutions) {
            info.addPara(resolution.sponsorFactionId + " → " + resolution.frontId + "：" +
                    (resolution.resolved ? resolution.outcome : "复核中"), 3f);
        }
        return;
        }
        info.addSectionHeading("后备队与可用额度", HIGHLIGHT, Color.BLACK,
                com.fs.starfarer.api.ui.Alignment.MID, 10f);
        for (Map.Entry<String, Float> entry : state.late.staffAvailableByFaction.entrySet()) {
            info.addPara(entry.getKey() + "：可调拨 " + Math.round(entry.getValue()), 3f);
        }
        for (PTSDLateState.Reserve reserve : state.late.reserves) {
            if (reserve == null || reserve.destroyed || !reserve.authorized) continue;
            info.addSectionHeading("后备队：" + reserve.id, HIGHLIGHT, Color.BLACK,
                    com.fs.starfarer.api.ui.Alignment.MID, 10f);
            info.addPara("当前命令：" + (reserve.targetSystemId == null ? "待命" : reserve.targetSystemId) +
                    "；预计行军进度 " + Math.round(reserve.distance <= 0f ? 0f :
                    reserve.travelled / reserve.distance * 100f) + "%", 3f);
            for (PTSDLateState.Front front : state.late.fronts.values()) {
                if (front == null || front.resolved) continue;
                info.addButton("调往 " + front.systemId, "RESERVE:" + reserve.id + ":" + front.systemId,
                        HIGHLIGHT, Color.BLACK, Math.max(220f, width - 24f), 22f, 2f);
            }
        }
    }

    @Override public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (!(buttonId instanceof String)) return;
        if (PTSDMapCommands.handle(PTSDCrisisState.get(), (String) buttonId)) { ui.updateUIForItem(this); return; }
        if (((String) buttonId).startsWith("COALITION:JOIN:")) {
            PTSDGeneralStaffDirector.requestMembership(PTSDCrisisState.get(), ((String)buttonId).substring("COALITION:JOIN:".length()));
            ui.updateUIForItem(this); return;
        }
        if (((String) buttonId).startsWith("PAGE:")) {
            try { page = Math.max(0, Math.min(3,
                    Integer.parseInt(((String) buttonId).substring(5)))); }
            catch (NumberFormatException ignored) { return; }
            ui.updateUIForItem(this);
            return;
        }
        String[] parts = ((String) buttonId).split(":", 3);
        if (parts.length != 3) return;
        boolean success = false;
        if ("STAFF".equals(parts[0])) success =
                PTSDGeneralStaffDirector.propose(PTSDCrisisState.get(), parts[2], parts[1]);
        else if ("RESERVE".equals(parts[0])) success =
                PTSDGeneralStaffDirector.orderReserve(PTSDCrisisState.get(), parts[1], parts[2]);
        if (success) {
            ui.updateUIForItem(this);
        } else if ("RESERVE".equals(parts[0]) && Global.getSector().getCampaignUI() != null) {
            Global.getSector().getCampaignUI().addMessage(
                    "[总参部] 后备队调令未执行：队伍未授权、仍在行军或目标战区无效");
        }
    }

    @Override public void createLargeDescription(com.fs.starfarer.api.ui.CustomPanelAPI panel,
                                                  float width, float height) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || state.late == null) return;
        PTSDGeneralStaffDirector.registerFounders(state);
        if (page == 0 || page == 3) {
            float sidebar = Math.min(330f, width*.32f), bodyWidth=width-sidebar-12f;
            TooltipMakerAPI info = panel.createUIElement(sidebar,height,true);
            createSmallDescription(info,sidebar,height);
            if (page == 0) {
                CustomPanelAPI charts = panel.createCustomPanel(bodyWidth,height,null);
                PTSDCoalitionCharts.populate(charts,bodyWidth,height,state);
                panel.addComponent(charts).inTL(0f,0f);
            } else {
                PTSDFrontMapPanel plugin = new PTSDFrontMapPanel();
                CustomPanelAPI map = panel.createCustomPanel(bodyWidth,height,plugin);
                TooltipMakerAPI header = map.createUIElement(bodyWidth-24f,65f,false);
                header.addSectionHeading("联合战区地图", HIGHLIGHT,Color.BLACK,com.fs.starfarer.api.ui.Alignment.MID,0f);
                header.addPara("滚轮缩放；左键拖动；右键星系下达请求。",5f);
                map.addUIElement(header).inTL(12f,8f);
                TooltipMakerAPI hover = map.createUIElement(bodyWidth-24f,48f,false);
                plugin.setHoverLabel(hover.addPara("悬停查看最近报告及来源。",0f));
                map.addUIElement(hover).inBL(12f,8f);
                plugin.setParent(map,info.addPara("点击星系固定军报。",10f));
                panel.addComponent(map).inTL(0f,0f);
            }
            panel.addUIElement(info).inTR(0f,0f); return;
        }
        TooltipMakerAPI info = panel.createUIElement(width, height, true);
        createSmallDescription(info, width, height);
        panel.addUIElement(info).inTL(0f, 0f);
    }
}
