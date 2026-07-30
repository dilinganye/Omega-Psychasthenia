package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ArrowData;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** The formal war room: safe custom hyperspace map, fronts, orders and player task forces. */
public final class PTSDWarIntel extends BaseIntelPlugin {
    private static final long serialVersionUID = 1L;
    private boolean devOnlyPreview;

    private static final String BUTTON_CLEAR = "PTSD_CLEAR_MARKERS";
    private static final String BUTTON_MARK_DEFEND = "PTSD_MARK_DEFEND:";
    private static final String BUTTON_MARK_BASE = "PTSD_MARK_BASE:";
    private static final String BUTTON_BUILD_INTERCEPTOR = "PTSD_BUILD:INTERCEPTOR";
    private static final String BUTTON_BUILD_LINE = "PTSD_BUILD:LINE";
    private static final String BUTTON_BUILD_STRIKE = "PTSD_BUILD:STRIKE";

    public PTSDWarIntel() {
        setImportant(true);
    }

    public static PTSDWarIntel ensureIntel() {
        if (Global.getSector() == null) return null;
        Object existing = Global.getSector().getIntelManager().getFirstIntel(PTSDWarIntel.class);
        if (existing instanceof PTSDWarIntel) {
            PTSDWarIntel intel = (PTSDWarIntel) existing;
            intel.devOnlyPreview = false;
            PTSDCrisisState state = PTSDCrisisState.get();
            if (state != null) state.warIntelCreated = true;
            return intel;
        }
        PTSDWarIntel intel = new PTSDWarIntel();
        Global.getSector().getIntelManager().addIntel(intel);
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null) state.warIntelCreated = true;
        return intel;
    }

    public static PTSDWarIntel ensureDevPreview() {
        if (Global.getSector() == null || !Global.getSettings().isDevMode()) return null;
        Object existing = Global.getSector().getIntelManager().getFirstIntel(PTSDWarIntel.class);
        if (existing instanceof PTSDWarIntel) return (PTSDWarIntel) existing;
        PTSDWarIntel intel = new PTSDWarIntel();
        intel.devOnlyPreview = true;
        Global.getSector().getIntelManager().addIntel(intel, true);
        return intel;
    }

    public static void removeDevPreview() {
        if (Global.getSector() == null) return;
        for (com.fs.starfarer.api.campaign.comm.IntelInfoPlugin plugin :
                new java.util.ArrayList<com.fs.starfarer.api.campaign.comm.IntelInfoPlugin>(
                        Global.getSector().getIntelManager().getIntel(PTSDWarIntel.class))) {
            PTSDWarIntel intel = (PTSDWarIntel) plugin;
            if (intel.devOnlyPreview) Global.getSector().getIntelManager().removeIntel(intel);
        }
    }
    @Override
    protected String getName() {
        return devOnlyPreview ? "[DEV预览] 精神创伤危机：战区态势" : "精神创伤危机：战区态势";
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
    public boolean hasLargeDescription() {
        return true;
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        PTSDCrisisState state = PTSDCrisisState.get();
        float opad = 10f;
        info.addPara("这是一张由不完整通讯、殖民地报告与战场估算拼合的全星域战线图。舰队只在你靠近相关星系时实体化；远方战况由战略事件异步结算。", opad);
        if (state == null) return;
        info.addPara("当前战线：%s 个活动；精神创伤实控星系：%s；玩家特化舰队：%s/8。", opad,
                Misc.getHighlightColor(), String.valueOf(state.getActiveEvents().size()),
                String.valueOf(countOmegaSystems(state)), String.valueOf(state.countActiveTaskForces()));
    }

    @Override
    public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        PTSDCrisisState state = PTSDCrisisState.get();
        Color omega = getFactionForUIColors().getBaseUIColor();
        Color human = Global.getSector().getPlayerFaction().getBaseUIColor();
        float gap = 10f;
        float sideWidth = Math.max(285f, Math.min(340f, width * 0.29f));
        float mapWidth = width - sideWidth - gap;

        PTSDFrontMapPanel mapPlugin = new PTSDFrontMapPanel();
        CustomPanelAPI mapPanel = panel.createCustomPanel(mapWidth, height, mapPlugin);
        TooltipMakerAPI mapHeader = mapPanel.createUIElement(mapWidth - 24f, 74f, false);
        mapHeader.addSectionHeading("全星域战线", omega, getFactionForUIColors().getDarkUIColor(),
                com.fs.starfarer.api.ui.Alignment.MID, 0f);
        LabelAPI help = mapHeader.addPara("滚轮缩放；按住左键拖动；右键重置视野。战线、控制区与推进箭头均为战略推演。", 5f);
        help.setColor(new Color(180, 190, 205));
        mapPanel.addUIElement(mapHeader).inTL(12f, 8f);

        TooltipMakerAPI hoverInfo = mapPanel.createUIElement(mapWidth - 24f, 48f, false);
        LabelAPI hoverLabel = hoverInfo.addPara("将鼠标移到推进箭头、战线边缘或星系节点上查看局部态势。", 0f);
        hoverLabel.setColor(new Color(180, 190, 205));
        mapPanel.addUIElement(hoverInfo).inBL(12f, 8f);
        mapPlugin.setHoverLabel(hoverLabel);
        panel.addComponent(mapPanel).inTL(0f, 0f);

        TooltipMakerAPI side = panel.createUIElement(sideWidth, height, true);
        side.addSectionHeading("战区态势控制台", omega, getFactionForUIColors().getDarkUIColor(),
                com.fs.starfarer.api.ui.Alignment.MID, 0f);
        side.addPara("远方战况由战略层异步结算；舰队仅在玩家接近相关星系时临时实体化。", 8f);
        if (state == null) {
            side.addPara("战略数据尚未初始化。", Misc.getNegativeHighlightColor(), 10f);
        } else {
            side.addPara("活动战线 %s　精神创伤实控 %s　玩家舰队 %s/8", 6f,
                    Misc.getHighlightColor(), String.valueOf(state.getActiveEvents().size()),
                    String.valueOf(countOmegaSystems(state)), String.valueOf(state.countActiveTaskForces()));
            addFrontSummary(side, state, sideWidth, 10f, omega, human);
            addOrderControls(side, state, sideWidth, 10f, omega, human);
            addTaskForceControls(side, state, sideWidth, 10f, human);
        }
        panel.addUIElement(side).inTR(0f, 0f);
    }
    private void addFrontSummary(TooltipMakerAPI info, PTSDCrisisState state, float width, float opad,
                                 Color omega, Color human) {
        info.addSectionHeading("活动战线", omega, getFactionForUIColors().getDarkUIColor(),
                com.fs.starfarer.api.ui.Alignment.MID, opad);
        List<PTSDCrisisState.StrategicEvent> events = state.getActiveEvents();
        Collections.sort(events, new Comparator<PTSDCrisisState.StrategicEvent>() {
            @Override
            public int compare(PTSDCrisisState.StrategicEvent a, PTSDCrisisState.StrategicEvent b) {
                return Float.compare(b.strength, a.strength);
            }
        });
        if (events.isEmpty()) {
            info.addPara("暂时没有可确认的推进箭头。战略层仍会按既定周期更新。", opad);
            return;
        }
        int shown = 0;
        for (PTSDCrisisState.StrategicEvent event : events) {
            if (shown >= 9) break;
            StarSystemAPI target = state.resolveSystem(event.targetSystemId);
            if (target == null) continue;
            String side = PTSDCrisisAPI.SIDE_OMEGA.equals(event.side) ? "精神创伤推进" : "人类侧阻滞";
            String status = event.status == PTSDCrisisState.EventStatus.MATERIALIZED ? "已实体化" : "远程推演";
            Color color = PTSDCrisisAPI.SIDE_OMEGA.equals(event.side) ? omega : human;
            info.addPara("%s → %s　强度 %s　[%s]", 3f, color,
                    side, target.getName(), String.valueOf(Math.round(event.strength)), status);
            if (event.description != null && !event.description.isEmpty()) {
                info.addPara(event.description, 1f, Misc.getGrayColor(), new String[0]);
            }
            shown++;
        }
    }

    private void addOrderControls(TooltipMakerAPI info, PTSDCrisisState state, float width, float opad,
                                  Color omega, Color human) {
        info.addSectionHeading("玩家战场标记", human, Global.getSector().getPlayerFaction().getDarkUIColor(),
                com.fs.starfarer.api.ui.Alignment.MID, opad);
        info.addPara("标记会影响人类侧权重，但每个标记的贡献受到大局上限约束，不会让全部防卫事件挤到一个星系。橙色表示报告敌方据点，青色表示重点防御。", opad);

        List<PTSDCrisisState.SystemData> fronts = getFrontCandidates(state);
        int shown = 0;
        for (PTSDCrisisState.SystemData data : fronts) {
            if (shown >= 4) break;
            StarSystemAPI system = state.resolveSystem(data.systemId);
            if (system == null) continue;
            ButtonAPI defend = info.addButton("重点防御：" + system.getName(), BUTTON_MARK_DEFEND + data.systemId,
                    human, Global.getSector().getPlayerFaction().getDarkUIColor(), width * 0.49f, 22f, 4f);
            defend.setEnabled(true);
            ButtonAPI base = info.addButton("报告据点：" + system.getName(), BUTTON_MARK_BASE + data.systemId,
                    omega, getFactionForUIColors().getDarkUIColor(), width * 0.49f, 22f, 2f);
            base.setEnabled(true);
            shown++;
        }
        if (!state.playerMarkers.isEmpty()) {
            info.addButton("清除全部战场标记", BUTTON_CLEAR, human,
                    Global.getSector().getPlayerFaction().getDarkUIColor(), width, 22f, opad);
        }
    }

    private void addTaskForceControls(TooltipMakerAPI info, PTSDCrisisState state, float width, float opad, Color human) {
        info.addSectionHeading("殖民地特化舰队", human, Global.getSector().getPlayerFaction().getDarkUIColor(),
                com.fs.starfarer.api.ui.Alignment.MID, opad);
        int available = getTotalAvailableProduction(state);
        info.addPara("每支舰队占用 1 点玩家殖民地舰船产能，并保留独立部署权重。现有 %s/8 支，可用未承诺产能 %s。", opad,
                Misc.getHighlightColor(), String.valueOf(state.countActiveTaskForces()), String.valueOf(available));
        boolean enabled = state.countActiveTaskForces() < 8 && available > 0;
        ButtonAPI interceptor = info.addButton("组建截击舰队", BUTTON_BUILD_INTERCEPTOR,
                human, Global.getSector().getPlayerFaction().getDarkUIColor(), width, 22f, 4f);
        interceptor.setEnabled(enabled);
        ButtonAPI line = info.addButton("组建战列阻滞舰队", BUTTON_BUILD_LINE,
                human, Global.getSector().getPlayerFaction().getDarkUIColor(), width, 22f, 2f);
        line.setEnabled(enabled);
        ButtonAPI strike = info.addButton("组建纵深打击舰队", BUTTON_BUILD_STRIKE,
                human, Global.getSector().getPlayerFaction().getDarkUIColor(), width, 22f, 2f);
        strike.setEnabled(enabled);

        for (PTSDCrisisState.PlayerTaskForce force : state.playerTaskForces) {
            if (force.destroyed) continue;
            StarSystemAPI system = state.resolveSystem(force.assignedSystemId);
            info.addPara("%s　%s　部署：%s　估算强度 %s", 3f, human,
                    force.name, force.specialization,
                    system == null ? "待命" : system.getName(), String.valueOf(Math.round(force.strength)));
        }
    }

    @Override
    public List<ArrowData> getArrowData(SectorMapAPI map) {
        PTSDCrisisState state = PTSDCrisisState.get();
        List<ArrowData> result = new ArrayList<ArrowData>();
        if (state == null) return result;
        for (PTSDCrisisState.StrategicEvent event : state.getActiveEvents()) {
            StarSystemAPI target = state.resolveSystem(event.targetSystemId);
            StarSystemAPI source = state.resolveSystem(event.sourceSystemId);
            if (target == null) continue;
            SectorEntityToken from = source == null ? getMapLocation(map) : source.getHyperspaceAnchor();
            if (from == null || from == target.getHyperspaceAnchor()) continue;
            FactionAPI faction = event.factionId == null ? null : Global.getSector().getFaction(event.factionId);
            Color color;
            if (faction != null) color = faction.getBaseUIColor();
            else if (PTSDCrisisAPI.SIDE_OMEGA.equals(event.side)) color = getFactionForUIColors().getBaseUIColor();
            else color = Global.getSector().getPlayerFaction().getBaseUIColor();
            ArrowData arrow = new ArrowData(Math.max(6f, Math.min(24f, 5f + event.strength / 16f)),
                    from, target.getHyperspaceAnchor(), color);
            arrow.alphaMult = event.status == PTSDCrisisState.EventStatus.MATERIALIZED ? 0.9f : 0.52f;
            result.add(arrow);
        }
        return result;
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || !(buttonId instanceof String)) {
            super.buttonPressConfirmed(buttonId, ui);
            return;
        }
        String id = (String) buttonId;
        if (BUTTON_CLEAR.equals(id)) {
            state.playerMarkers.clear();
        } else if (id.startsWith(BUTTON_MARK_DEFEND)) {
            state.putMarker(id.substring(BUTTON_MARK_DEFEND.length()), "DEFEND", 1.35f);
        } else if (id.startsWith(BUTTON_MARK_BASE)) {
            state.putMarker(id.substring(BUTTON_MARK_BASE.length()), "OMEGA_BASE", 1.2f);
        } else if (id.startsWith("PTSD_BUILD:")) {
            createTaskForce(state, id.substring("PTSD_BUILD:".length()));
        }
        ui.updateUIForItem(this);
    }

    private void createTaskForce(PTSDCrisisState state, String specialization) {
        if (state.countActiveTaskForces() >= 8) return;
        MarketAPI source = pickProductionMarket(state);
        PTSDCrisisState.SystemData target = pickPlayerTarget(state);
        if (source == null || target == null) return;

        PTSDCrisisState.PlayerTaskForce force = new PTSDCrisisState.PlayerTaskForce();
        force.id = "PTSD_player_force_" + Misc.genUID();
        force.sourceMarketId = source.getId();
        force.assignedSystemId = target.systemId;
        force.specialization = specialization;
        force.productionCost = 1;
        force.createdDay = PTSDCrisisState.getDay();
        force.nextRedeployDay = force.createdDay + 10f;
        int number = state.countActiveTaskForces() + 1;
        if ("INTERCEPTOR".equals(specialization)) {
            force.name = "边区截击群 " + number;
            force.strength = 58f;
            force.deploymentWeight = 1.25f;
        } else if ("LINE".equals(specialization)) {
            force.name = "战列阻滞群 " + number;
            force.strength = 82f;
            force.deploymentWeight = 1.05f;
        } else {
            force.name = "纵深打击群 " + number;
            force.strength = 70f;
            force.deploymentWeight = 0.9f;
        }
        state.playerTaskForces.add(force);
        state.addCommittedProduction(source.getId(), 1);
        PTSDCrisisState.StrategicEvent deployment = state.addEvent(PTSDCrisisState.EventType.PLAYER_TASK_FORCE,
                PTSDCrisisAPI.SIDE_HUMAN, Global.getSector().getPlayerFaction().getId(),
                source.getStarSystem().getId(), target.systemId, null, force.strength, 6f);
        deployment.referenceId = force.id;
        deployment.description = force.name + " 正在前往指定战线。";
    }

    private MarketAPI pickProductionMarket(PTSDCrisisState state) {
        MarketAPI best = null;
        int bestAvailable = 0;
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (!market.isPlayerOwned() || market.getStarSystem() == null) continue;
            CommodityOnMarketAPI ships = market.getCommodityData(Commodities.SHIPS);
            if (ships == null) continue;
            int available = ships.getMaxSupply() - state.getCommittedProduction(market.getId());
            if (available > bestAvailable) {
                bestAvailable = available;
                best = market;
            }
        }
        return bestAvailable > 0 ? best : null;
    }

    private int getTotalAvailableProduction(PTSDCrisisState state) {
        int result = 0;
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (!market.isPlayerOwned() || market.getStarSystem() == null) continue;
            CommodityOnMarketAPI ships = market.getCommodityData(Commodities.SHIPS);
            if (ships == null) continue;
            result += Math.max(0, ships.getMaxSupply() - state.getCommittedProduction(market.getId()));
        }
        return result;
    }

    private PTSDCrisisState.SystemData pickPlayerTarget(PTSDCrisisState state) {
        PTSDCrisisState.PlayerMarker bestMarker = null;
        for (PTSDCrisisState.PlayerMarker marker : state.playerMarkers.values()) {
            if (bestMarker == null || marker.weight > bestMarker.weight) bestMarker = marker;
        }
        if (bestMarker != null) return state.getSystemData(bestMarker.systemId);
        List<PTSDCrisisState.SystemData> candidates = getFrontCandidates(state);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private List<PTSDCrisisState.SystemData> getFrontCandidates(PTSDCrisisState state) {
        List<PTSDCrisisState.SystemData> result = new ArrayList<PTSDCrisisState.SystemData>();
        Set<String> activeTargets = new LinkedHashSet<String>();
        for (PTSDCrisisState.StrategicEvent event : state.getActiveEvents()) {
            if (event.targetSystemId != null) activeTargets.add(event.targetSystemId);
        }
        for (PTSDCrisisState.SystemData data : state.systems.values()) {
            if (activeTargets.contains(data.systemId) || data.omegaControl > 0f || data.attackWeight > 2f) result.add(data);
        }
        Collections.sort(result, new Comparator<PTSDCrisisState.SystemData>() {
            @Override
            public int compare(PTSDCrisisState.SystemData a, PTSDCrisisState.SystemData b) {
                return Float.compare(b.attackWeight, a.attackWeight);
            }
        });
        return result;
    }

    private static int countOmegaSystems(PTSDCrisisState state) {
        int result = 0;
        for (PTSDCrisisState.SystemData data : state.systems.values()) {
            if (data.omegaControl >= 0.5f) result++;
        }
        return result;
    }

    @Override
    public String getIcon() {
        FactionAPI faction = getFactionForUIColors();
        return faction == null ? null : faction.getCrest();
    }

    @Override
    public FactionAPI getFactionForUIColors() {
        FactionAPI faction = Global.getSector() == null ? null : Global.getSector().getFaction(IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION);
        return faction == null ? super.getFactionForUIColors() : faction;
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION);
        tags.add("危机");
        return tags;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return null;
        StarSystemAPI base = state.resolveSystem(state.baseSystemId);
        if (base != null) return base.getHyperspaceAnchor();
        for (PTSDCrisisState.StrategicEvent event : state.getActiveEvents()) {
            StarSystemAPI target = state.resolveSystem(event.targetSystemId);
            if (target != null) return target.getHyperspaceAnchor();
        }
        return null;
    }
}