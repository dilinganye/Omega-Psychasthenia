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
    transient BaseIntelPlugin owner;
    private int page;
    public void openMapPage(){page=3;}
    public void openReliefPage(){page=4;}

    public static PTSDGeneralStaffIntel ensureIntel() {
        if (Global.getSector() == null) return null;
        Object war=Global.getSector().getIntelManager().getFirstIntel(PTSDWarIntel.class);
        if(war instanceof PTSDWarIntel){
            for(com.fs.starfarer.api.campaign.comm.IntelInfoPlugin legacy:new java.util.ArrayList<com.fs.starfarer.api.campaign.comm.IntelInfoPlugin>(Global.getSector().getIntelManager().getIntel(PTSDGeneralStaffIntel.class)))
                Global.getSector().getIntelManager().removeIntel(legacy);
            return ((PTSDWarIntel)war).coalitionView();
        }
        Object previous = Global.getSector().getIntelManager().getFirstIntel(PTSDGeneralStaffIntel.class);
        if (previous instanceof PTSDGeneralStaffIntel) return (PTSDGeneralStaffIntel) previous;
        PTSDGeneralStaffIntel intel = new PTSDGeneralStaffIntel();
        Global.getSector().getIntelManager().addIntel(intel);
        return intel;
    }

    @Override protected String getName() { return "人类联盟：指挥与战线"; }
    @Override public String getSmallDescriptionTitle() { return getName(); }
    @Override public boolean hasLargeDescription() { return true; }

    @Override public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        populateDescription(info,width,height,true);
    }
    private void populateDescription(TooltipMakerAPI info, float width, float height, boolean navigation) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || state.late == null) return;
        info.addPara("各势力席位依现存规模与已核实贡献计算。个人战绩单列，归属按行动发生时锁定。", 0f);
        if (navigation) {
        info.addButton("席位与贡献", "PAGE:0", HIGHLIGHT, Color.BLACK,
                Math.max(220f, width - 24f), 22f, 6f);
        info.addButton("提案与表决", "PAGE:1", HIGHLIGHT, Color.BLACK,
                Math.max(220f, width - 24f), 22f, 2f);
        info.addButton("战区资源与后备队", "PAGE:2", HIGHLIGHT, Color.BLACK,
                Math.max(220f, width - 24f), 22f, 2f);
        info.addButton("战线与军报地图", "PAGE:3", HIGHLIGHT, Color.BLACK,
                Math.max(220f, width - 24f), 22f, 2f);
        }
        if (page == 0) {
        PTSDReliefState relief=PTSDReliefState.get(state);String playerId=Global.getSector().getPlayerFaction().getId();
        if(relief.playerInvited&&!state.late.coalitionMembers.contains(playerId)){
            info.addPara(relief.playerDeclined?"你仍可重新申请加入联盟。":"联盟邀请你的代表团入席。是否接受由你决定；拒绝不阻止人类救援。",5);
            info.addButton("接受联盟邀请","PLAYER:ACCEPT",width-24,28,4);
            info.addButton("暂时保持独立","PLAYER:DECLINE",width-24,28,3);
        }
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
                    IIRT_Omega_Invasion.WATCHER_FACTION.equals(factionId) || "luddic_path".equals(factionId)) continue;
            com.fs.starfarer.api.campaign.FactionAPI faction = Global.getSector().getFaction(factionId);
            if (faction == null) continue;
            if (state.late.coalitionDiplomaticReplies.containsKey(factionId))
                info.addPara("%s：%s", 3f, HIGHLIGHT, faction.getDisplayName(), state.late.coalitionDiplomaticReplies.get(factionId));
            if (!state.late.coalitionJoinRequests.containsKey(factionId))
                info.addButton("邀请入盟：" + faction.getDisplayName(), "COALITION:JOIN:" + factionId, HIGHLIGHT,
                        Color.BLACK, Math.max(220f, width - 24f), 22f, 2f);
        }
        for (PTSDLateState.Contribution item : state.late.contributions) {
            if (item != null) info.addPara(item.type + " / " + item.actorId + " / " +
                    item.factionAtTime + "：" + Math.round(item.amount), 2f);
        }
        return;
        }
        if (page == 1) {
        info.addSectionHeading("已登记战区资源", HIGHLIGHT, Color.BLACK, com.fs.starfarer.api.ui.Alignment.MID, 10f);
        info.addPara("仍在行动的后备队：" + state.late.reserves.size() +
                "；外围首波目标：" + state.late.selectedBurstCount, 5f);
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
            info.addPara(resolution.sponsorFactionId + " / " + resolution.frontId + "：" +
                    (resolution.resolved ? resolution.outcome : "复核中"), 3f);
            if (resolution.seatSnapshot != null && !resolution.seatSnapshot.isEmpty()) {
                info.addPara("赞成 %s / 反对 %s / 弃权 %s / 待答复 %s", 4f, HIGHLIGHT,
                        String.valueOf(Math.round(PTSDCoalitionVotes.weight(resolution,"YES"))),
                        String.valueOf(Math.round(PTSDCoalitionVotes.weight(resolution,"NO"))),
                        String.valueOf(Math.round(PTSDCoalitionVotes.weight(resolution,"ABSTAIN"))),
                        String.valueOf(Math.round(PTSDCoalitionVotes.weight(resolution,"PENDING"))));
                for (String member : resolution.seatSnapshot.keySet()) {
                    com.fs.starfarer.api.campaign.FactionAPI faction = Global.getSector().getFaction(member);
                    info.addPara("%s · %s席 · %s", 3f, HIGHLIGHT,
                            faction == null ? member : faction.getDisplayName(),
                            String.valueOf(Math.round(resolution.seatSnapshot.get(member))),
                            ballotLabel(resolution.ballots.get(member)));
                }
                if (!resolution.resolved && resolution.seatSnapshot.containsKey(Global.getSector().getPlayerFaction().getId()))
                    for (String choice : new String[]{"YES","NO","ABSTAIN"})
                        info.addButton("我方表决：" + ballotLabel(choice), "VOTE:" + choice + ":" + resolution.id,
                                HIGHLIGHT, Color.BLACK, width - 24f, 24f, 3f);
            }
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
        if(PTSDReliefPanel.action((String)buttonId,PTSDCrisisState.get())){ui.updateUIForItem(owner==null?this:owner);return;}
        if("PLAYER:ACCEPT".equals(buttonId)||"PLAYER:DECLINE".equals(buttonId)){
            PTSDCrisisState s=PTSDCrisisState.get();PTSDReliefState.get(s).playerDeclined="PLAYER:DECLINE".equals(buttonId);
            if(!PTSDReliefState.get(s).playerDeclined)PTSDGeneralStaffDirector.requestMembership(s,Global.getSector().getPlayerFaction().getId());
            else {String player=Global.getSector().getPlayerFaction().getId();s.late.coalitionJoinRequests.remove(player);s.late.coalitionDiplomaticReplies.put(player,"玩家暂不接受邀请，保留重新申请的通道。");}
            ui.updateUIForItem(owner==null?this:owner);return;
        }
        if (((String)buttonId).startsWith("VOTE:")) {
            String[] ballot = ((String)buttonId).split(":",3);
            if (ballot.length == 3) PTSDCoalitionVotes.cast(PTSDCrisisState.get(),ballot[2],ballot[1]);
            ui.updateUIForItem(owner==null?this:owner); return;
        }
        if (PTSDMapCommands.handle(PTSDCrisisState.get(), (String) buttonId)) { ui.updateUIForItem(owner==null?this:owner); return; }
        if (((String) buttonId).startsWith("COALITION:JOIN:")) {
            PTSDGeneralStaffDirector.requestMembership(PTSDCrisisState.get(), ((String)buttonId).substring("COALITION:JOIN:".length()));
            ui.updateUIForItem(owner==null?this:owner); return;
        }
        if (((String) buttonId).startsWith("PAGE:")) {
            try { page = Math.max(0, Math.min(4,
                    Integer.parseInt(((String) buttonId).substring(5)))); }
            catch (NumberFormatException ignored) { return; }
            ui.updateUIForItem(owner==null?this:owner);
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
            ui.updateUIForItem(owner==null?this:owner);
        } else if ("RESERVE".equals(parts[0]) && Global.getSector().getCampaignUI() != null) {
            Global.getSector().getCampaignUI().addMessage(
                    "[总参部] 后备队调令未执行：队伍未授权、仍在行军或目标战区无效");
        }
    }

    @Override public void createLargeDescription(com.fs.starfarer.api.ui.CustomPanelAPI panel,
                                                  float width, float height) {
        final CustomPanelAPI shell=panel.createCustomPanel(width,height,new PTSDCommandSurface(PTSDCommandSurface.INK));
        final CustomPanelAPI[] body={null};
        final CustomPanelAPI[] tabs={null};
        final Runnable[] refresh={null};
        refresh[0]=new Runnable(){public void run(){
            if(body[0]!=null)shell.removeComponent(body[0]);
            if(tabs[0]!=null)shell.removeComponent(tabs[0]);
            tabs[0]=shell.createCustomPanel(width-24f,34f,null);
            String[] names={"成员与入伙","议案与表决","调拨与补给","协同防线","行动与救援"};
            float tabWidth=(width-48f)/5f;
            for(int index=0;index<5;index++){
                final int destination=index;
                tabs[0].addComponent(PTSDCommandSurface.tab(tabs[0],tabWidth,names[index],page==index,
                        new Runnable(){public void run(){page=destination;refresh[0].run();}})).inTL(index*(tabWidth+6f),0f);
            }
            shell.addComponent(tabs[0]).inTL(12f,12f);
            body[0]=shell.createCustomPanel(width-24f,height-68f,null);
            populatePage(body[0],width-24f,height-68f);shell.addComponent(body[0]).inTL(12f,56f);
        }};
        refresh[0].run();panel.addComponent(shell).inTL(0f,0f);
    }
    private void populatePage(CustomPanelAPI panel,float width,float height) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || state.late == null) return;
        PTSDGeneralStaffDirector.registerFounders(state);
        if(page==4){PTSDReliefPanel.populate(panel,width,height,state);return;}
        if (page == 0 || page == 3) {
            float sidebar = Math.min(330f, width*.32f), bodyWidth=width-sidebar-12f;
            TooltipMakerAPI info = panel.createUIElement(sidebar,height,true);
            if(page==0)populateDescription(info,sidebar,height,false);
            else {
                info.addSectionHeading("战区详情",HIGHLIGHT,Color.BLACK,com.fs.starfarer.api.ui.Alignment.MID,0);
                info.addPara(state.late.staffEstablished?"联盟总参部已成立。援军全过程见「行动与救援」。":"联盟正在联络仍有殖民地的创始势力。",6);
                info.addPara("已加入势力："+state.late.coalitionMembers.size()+"；后备队："+state.late.reserves.size(),6);
                info.addPara("点击地图星系查看最近核实信息。右键节点规划航线、设置防御请求。",8);
            }
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
                plugin.setParent(map,info.addPara("尚未选择区域。",10f));
                info.addSpacer(180);
                info.addSectionHeading("正在交战",HIGHLIGHT,Color.BLACK,com.fs.starfarer.api.ui.Alignment.MID,16);
                int active=0;
                for(PTSDLateState.Front front:state.late.fronts.values())if(front!=null&&!front.resolved){
                    com.fs.starfarer.api.campaign.StarSystemAPI sys=state.resolveSystem(front.systemId);
                    info.addPara((sys==null?"失联战区":sys.getName())+"：已承诺防御 "+Math.round(front.defense),5);active++;
                }
                if(active==0)info.addPara("暂无已登记的活动战线。",5);
                panel.addComponent(map).inTL(0f,0f);
            }
            panel.addUIElement(info).inTR(0f,0f); return;
        }
        TooltipMakerAPI info = panel.createUIElement(width, height, true);
        populateDescription(info, width, height,false);
        if(page==2&&owner instanceof PTSDWarIntel)((PTSDWarIntel)owner).addPlayerForces(info,width);
        panel.addUIElement(info).inTL(0f, 0f);
    }
    private static String ballotLabel(String value) {
        return "YES".equals(value) ? "赞成" : "NO".equals(value) ? "反对" : "ABSTAIN".equals(value) ? "弃权" : "待答复";
    }
}
