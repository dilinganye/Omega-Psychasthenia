package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * DevMode-only crisis monitor. It deliberately does not alter discovery flags or normal Intel
 * visibility; switching DevMode off removes this Intel and any dev-only previews.
 */
public final class PTSDCrisisDevIntel extends BaseIntelPlugin {
    private static final long serialVersionUID = 1L;
    private static final int MAX_LOG = 80;
    private static final int MAX_EVENT_BUTTONS = 18;
    private static final String OPEN_PREWAR = "PTSD_DEV_OPEN_PREWAR";
    private static final String OPEN_WAR = "PTSD_DEV_OPEN_WAR";
    private static final String REFRESH = "PTSD_DEV_REFRESH";
    private static final String FORCE_DARK = "PTSD_DEV_FORCE_DARK";
    private static final String FORCE_PROBE = "PTSD_DEV_FORCE_PROBE";
    private static final String OPEN_COLONY_WEIGHTS = "PTSD_DEV_OPEN_COLONY_WEIGHTS";
    private static final String OPEN_ALL_WEIGHTS = "PTSD_DEV_OPEN_ALL_WEIGHTS";
    private static final String OPEN_CONTROL = "PTSD_DEV_OPEN_CONTROL";

    public static final class DevRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id;
        public float day;
        public String kind;
        public String description;
        public String systemId;
        public String entityId;
    }

    private static final class DevButton {
        public final boolean arrive;
        public final String eventId;
        public final String systemId;
        public final String entityId;

        private DevButton(boolean arrive, String eventId, String systemId, String entityId) {
            this.arrive = arrive;
            this.eventId = eventId;
            this.systemId = systemId;
            this.entityId = entityId;
        }
    }

    private final List<DevRecord> records = new ArrayList<DevRecord>();

    public PTSDCrisisDevIntel() {
        setImportant(true);
    }

    public static void sync() {
        if (Global.getSector() == null || Global.getSettings() == null) return;
        if (Global.getSettings().isDevMode()) {
            ensureIntel();
            PTSDCrisisDevControlIntel.ensureIntel();
            PTSDCrisisState state = PTSDCrisisState.get();
            if (state != null && state.legacyTimelineMigrated && !state.timelineMigrationReported) {
                state.timelineMigrationReported = true;
                report("状态迁移", "旧存档负数时间轴已整体迁移至稳定的正数战役日；既有事件间隔保持不变。", null, null);
            }
            return;
        }
        for (IntelInfoPlugin intel : new ArrayList<IntelInfoPlugin>(
                Global.getSector().getIntelManager().getIntel(PTSDCrisisDevIntel.class))) {
            Global.getSector().getIntelManager().removeIntel(intel);
        }
        PTSDCrisisIntel.removeDevPreview();
        PTSDWarIntel.removeDevPreview();
        PTSDCrisisWeightIntel.removeWhenNotDev();
        PTSDCrisisDevControlIntel.removeWhenNotDev();
    }

    public static PTSDCrisisDevIntel ensureIntel() {
        if (Global.getSector() == null || !Global.getSettings().isDevMode()) return null;
        Object existing = Global.getSector().getIntelManager().getFirstIntel(PTSDCrisisDevIntel.class);
        if (existing instanceof PTSDCrisisDevIntel) return (PTSDCrisisDevIntel) existing;
        PTSDCrisisDevIntel intel = new PTSDCrisisDevIntel();
        Global.getSector().getIntelManager().addIntel(intel, true);
        return intel;
    }

    public static void report(String kind, String description, String systemId, String entityId) {
        if (Global.getSector() == null || !Global.getSettings().isDevMode()) return;
        PTSDCrisisDevIntel intel = ensureIntel();
        if (intel == null) return;
        DevRecord record = new DevRecord();
        record.id = "PTSD_dev_" + Misc.genUID();
        record.day = PTSDCrisisState.getDay();
        record.kind = kind == null ? "危机事件" : kind;
        record.description = description == null ? "" : description;
        record.systemId = systemId;
        record.entityId = entityId;
        intel.records.add(record);
        while (intel.records.size() > MAX_LOG) intel.records.remove(0);
        String location = intel.locationName(systemId, entityId);
        Global.getSector().getCampaignUI().addMessage(
                "[DEV危机] " + record.kind + " @ " + location + "：" + record.description,
                new Color(255, 190, 90));
        intel.sendUpdateIfPlayerHasIntel(record, false, true);
    }

    public static void reportEventCreated(PTSDCrisisState.StrategicEvent event) {
        if (event == null) return;
        report("战略事件生成", describeEvent(event), event.targetSystemId, event.materializedFleetId);
    }

    public static void reportEventResolved(PTSDCrisisState.StrategicEvent event) {
        if (event == null) return;
        report("战略事件结算",
                describeEvent(event) + "，结果=" + (event.successful ? "成功" : "失败"),
                event.targetSystemId, event.materializedFleetId);
    }

    private static String describeEvent(PTSDCrisisState.StrategicEvent event) {
        String text = event.type + " / " + event.side + " / 强度 " + Math.round(event.strength);
        if (event.description != null && !event.description.isEmpty()) text += " / " + event.description;
        return text;
    }

    @Override
    protected String getName() {
        return "[DEV] 精神创伤危机监视器";
    }

    @Override
    public String getSmallDescriptionTitle() {
        return getName();
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) {
            info.addPara("危机状态尚未初始化。", Misc.getNegativeHighlightColor(), 10f);
            return;
        }
        info.addPara("当前阶段：%s；活动战略事件：%s；侦察目击/逃脱：%s/%s。",
                10f, Misc.getHighlightColor(), state.phase.name(),
                String.valueOf(state.getActiveEvents().size()),
                String.valueOf(state.totalScoutSightings), String.valueOf(state.totalScoutEscapes));
        info.addPara("该条目和 Dev 预览在关闭 DevMode 后自动移除，不会替玩家解锁正常危机情报。", 8f);
    }

    @Override
    public boolean hasLargeDescription() {
        return true;
    }

    @Override
    public void createLargeDescription(com.fs.starfarer.api.ui.CustomPanelAPI panel, float width, float height) {
        TooltipMakerAPI info = panel.createUIElement(width, height, true);
        PTSDCrisisState state = PTSDCrisisState.get();
        Color base = getFactionForUIColors().getBaseUIColor();
        Color dark = getFactionForUIColors().getDarkUIColor();
        float opad = 10f;

        info.addSectionHeading("DevMode 危机监视", base, dark,
                com.fs.starfarer.api.ui.Alignment.MID, 0f);
        if (state == null) {
            info.addPara("危机状态尚未初始化。", Misc.getNegativeHighlightColor(), opad);
            panel.addUIElement(info).inTL(0f, 0f);
            return;
        }

        info.addPara("阶段 %s（已持续 %s）｜可见阶段 %s｜接触 %s｜侦察目击 %s｜逃脱 %s",
                opad, Misc.getHighlightColor(), state.phase.name(),
                elapsed(state.phaseStartedDay), String.valueOf(state.visibleStage),
                String.valueOf(state.totalOmegaEncounters), String.valueOf(state.totalScoutSightings),
                String.valueOf(state.totalScoutEscapes));
        info.addPara("时代 %s｜当前危机势力 %s｜全面进攻准备度 %s/%s",
                3f, Misc.getHighlightColor(), PTSDCrisisProgress.getEra(state).name(),
                PTSDCrisisProgress.getActiveFactionId(state),
                String.valueOf(Math.round(PTSDCrisisProgress.getInvasionReadiness(state))),
                String.valueOf(Math.round(PTSDCrisisProgress.INVASION_READINESS_THRESHOLD)));
        info.addPara("侦察置信 %s｜人类认知 %s｜第四窥视攻击性 %s｜巢穴发展 %s｜封锁密度 %s",
                3f, Misc.getHighlightColor(), String.valueOf(Math.round(state.reconConfidence)),
                String.valueOf(Math.round(state.humanAwareness)), String.valueOf(Math.round(state.watcherAggression)),
                String.valueOf(Math.round(state.nestDevelopment)), String.valueOf(Math.round(state.blockadeDensity)));
        info.addPara("精神创伤升级 %s｜人类凝聚 %s｜全局恐慌加成 %s｜现实扭曲 %s",
                3f, Misc.getHighlightColor(), String.valueOf(Math.round(state.omegaEscalation)),
                String.valueOf(Math.round(state.humanCohesion)), String.valueOf(Math.round(state.globalPanic)),
                String.valueOf(Math.round(state.realityDistortion)));
        info.addPara("Je Otloes：侦察目击 %s/4｜真实交战 %s/1｜接触 %s｜实地委托 %s｜代查 %s｜待会面 %s",
                3f, Misc.getHighlightColor(), String.valueOf(state.totalScoutSightings),
                String.valueOf(state.totalPlayerOmegaBattles), yesNo(state.jeIntroCompleted),
                state.jePlayerTaskIncidentId == null ? "无" : state.jePlayerTaskIncidentId,
                state.jeAgentIncidentId == null ? "无" : state.jeAgentIncidentId,
                yesNo(state.jeMeetingReady));
        info.addPara("下一未知事件 %s｜下一侦察 %s｜权重更新 %s｜扩张 %s｜要塞 %s｜Omega 回合 %s｜人类回合 %s",
                3f, Misc.getHighlightColor(), remaining(state.nextIncidentDay), remaining(state.nextScoutDay), remaining(state.nextWeightUpdateDay),
                remaining(state.nextExpansionDay), remaining(state.nextFortressDay),
                remaining(state.nextOmegaTurnDay), remaining(state.nextHumanTurnDay));
        String baseName = locationName(state.baseSystemId, null);
        info.addPara("基地：%s｜软警告 %s｜全面警告 %s｜第四窥视移交 %s",
                3f, Misc.getHighlightColor(), baseName,
                yesNo(state.softWarningShown), yesNo(state.hardWarningShown), yesNo(state.watcherTransferred));

        info.addPara("舰队强度：全局倍率 %s｜动态 Flat +%s（上限 +200）",
                3f, Misc.getHighlightColor(),
                String.valueOf(data.scripts.IIRT_Omega_ModPlugin.omega_fleet_strength_multiplier),
                String.valueOf(Math.round(PTSDOmegaFleetScaling.getDynamicFlat())));
        info.addPara("玩家记恨 " + format(state.playerGrudge) + "｜下次记恨袭扰 " + remaining(state.nextGrudgeRaidDay) +
                "｜战前截获 " + (state.prewarHunterSpawned ? (state.prewarHunterResolved ? "已结束" : "进行中/锁定入侵") : "未触发") +
                "｜全面战争承诺 " + (state.warCommitDay > 0f ? remaining(state.warCommitDay) : "未建立"), 3f);
        if (!state.factionResistance.isEmpty()) {
            StringBuilder resistance = new StringBuilder("对抗值：");
            int resistanceShown = 0;
            for (java.util.Map.Entry<String, Float> entry : state.factionResistance.entrySet()) {
                if (resistanceShown++ > 0) resistance.append("｜");
                resistance.append(entry.getKey()).append(" ").append(format(entry.getValue()));
                if (resistanceShown >= 6) break;
            }
            info.addPara(resistance.toString(), 3f, Misc.getGrayColor(), new String[0]);
        }
        info.addSectionHeading("直接访问情报", base, dark,
                com.fs.starfarer.api.ui.Alignment.MID, opad);
        info.addPara("以下预览只在 DevMode 中补建；若剧情已正常解锁，则会自动转为正式情报。", 5f);
        info.addButton("打开变量与事件控制台", OPEN_CONTROL, Color.ORANGE, new Color(80, 45, 20), width, 24f, 4f);
        info.addButton("查看战前危机情报", OPEN_PREWAR, base, dark, width, 24f, 2f);
        info.addButton("查看战区态势情报", OPEN_WAR, base, dark, width, 24f, 2f);
        info.addButton("查看殖民星域攻击权重", OPEN_COLONY_WEIGHTS, base, dark, width, 24f, 2f);
        info.addButton("查看全部星系攻击权数", OPEN_ALL_WEIGHTS, base, dark, width, 24f, 2f);
        info.addButton("刷新监视器", REFRESH, base, dark, width, 24f, 2f);
        info.addButton("强制抽取：暗流事件", FORCE_DARK, base, dark, width, 24f, 2f);
        info.addButton("强制抽取：火力侦察", FORCE_PROBE, Color.ORANGE, new Color(80, 45, 20), width, 24f, 2f);

        addWeights(info, state, base, dark, opad);
        addLocalPanic(info, state, base, dark, opad);
        addOccupations(info, state, width, base, dark, opad);
        addIncidents(info, state, width, base, dark, opad);
        addStrategicEvents(info, state, width, base, dark, opad);
        addRecords(info, width, base, dark, opad);
        panel.addUIElement(info).inTL(0f, 0f);
    }

    private void addWeights(TooltipMakerAPI info, PTSDCrisisState state, Color base, Color dark, float opad) {
        info.addSectionHeading("攻击权重最高的星系", base, dark,
                com.fs.starfarer.api.ui.Alignment.MID, opad);
        List<PTSDCrisisState.SystemData> data = new ArrayList<PTSDCrisisState.SystemData>(state.systems.values());
        Collections.sort(data, new Comparator<PTSDCrisisState.SystemData>() {
            @Override
            public int compare(PTSDCrisisState.SystemData a, PTSDCrisisState.SystemData b) {
                return Float.compare(b.attackWeight, a.attackWeight);
            }
        });
        int shown = 0;
        for (PTSDCrisisState.SystemData item : data) {
            StarSystemAPI system = state.resolveSystem(item.systemId);
            if (system == null) continue;
            info.addPara("%s：攻击 %s｜人类防御 %s｜控制 Ω %s / H %s｜侦察 %s",
                    3f, Misc.getHighlightColor(), system.getName(), format(item.attackWeight),
                    format(item.humanDefenseWeight), format(item.omegaControl), format(item.humanControl),
                    String.valueOf(item.scoutVisits));
            if (++shown >= 10) break;
        }
    }

    private void addLocalPanic(TooltipMakerAPI info, PTSDCrisisState state,
                               Color base, Color dark, float opad) {
        info.addSectionHeading("殖民地局部恐慌（侦察信息表）", base, dark,
                com.fs.starfarer.api.ui.Alignment.MID, opad);
        List<com.fs.starfarer.api.campaign.econ.MarketAPI> markets =
                new ArrayList<com.fs.starfarer.api.campaign.econ.MarketAPI>();
        for (com.fs.starfarer.api.campaign.econ.MarketAPI market :
                Global.getSector().getEconomy().getMarketsCopy()) {
            if (market == null || market.isPlanetConditionMarketOnly() || market.getStarSystem() == null) continue;
            if (IIRT_Omega_Invasion.WATCHER_FACTION.equals(market.getFactionId()) ||
                    IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(market.getFactionId())) continue;
            markets.add(market);
        }
        Collections.sort(markets, new Comparator<com.fs.starfarer.api.campaign.econ.MarketAPI>() {
            @Override
            public int compare(com.fs.starfarer.api.campaign.econ.MarketAPI a,
                               com.fs.starfarer.api.campaign.econ.MarketAPI b) {
                return Float.compare(PTSDLocalPanicAPI.getMarketPanic(b),
                        PTSDLocalPanicAPI.getMarketPanic(a));
            }
        });
        int shown = 0;
        for (com.fs.starfarer.api.campaign.econ.MarketAPI market : markets) {
            PTSDCrisisState.SystemData system = state.getSystemData(market.getStarSystem().getId());
            PTSDCrisisState.ColonyPanicData local = system.colonyPanic.get(market.getId());
            float eventPanic = local == null ? 0f : local.eventPanic;
            float proximityPanic = local == null ? 0f : local.proximityPanic;
            info.addPara("%s @ %s：实际 %s｜新闻/事件 %s｜邻近压迫 %s｜全局 +%s｜来源 %s",
                    shown == 0 ? 5f : 3f, Misc.getHighlightColor(), market.getName(),
                    market.getStarSystem().getName(), format(PTSDLocalPanicAPI.getMarketPanic(market)),
                    format(eventPanic), format(proximityPanic), format(state.globalPanic),
                    local == null || local.lastSource == null ? "无" : local.lastSource);
            if (++shown >= 12) break;
        }
        if (shown == 0) info.addPara("当前没有可记录恐慌的殖民地。", 5f);
    }
    private void addOccupations(TooltipMakerAPI info, PTSDCrisisState state, float width,
                                Color base, Color dark, float opad) {
        info.addSectionHeading("精神创伤占领区", base, dark,
                com.fs.starfarer.api.ui.Alignment.MID, opad);
        int shown = 0;
        for (com.fs.starfarer.api.campaign.econ.MarketAPI market :
                PTSDOccupationManager.getAllMarkets()) {
            if (!PTSDOccupationManager.isOccupied(market)) continue;
            PTSDCrisisState.OccupationData data = state.getOccupationData(market.getId());
            info.addPara("%s @ %s｜原版地图隐藏 %s｜Omega注意 %s｜人类关注 %s｜损伤 %s｜防御胜利 %s",
                    shown == 0 ? 5f : 7f, Misc.getHighlightColor(), market.getName(),
                    market.getStarSystem() == null ? "未知星系" : market.getStarSystem().getName(),
                    yesNo(market.isHidden()), format(data.omegaAttention), format(data.humanAttention),
                    format(data.accumulatedDamage), String.valueOf(data.defenseVictories));
            SectorEntityToken entity = market.getPrimaryEntity();
            if (entity != null) {
                addNavigationButtons(info, width, base, dark,
                        new DevButton(false, null,
                                market.getStarSystem() == null ? null : market.getStarSystem().getId(), entity.getId()),
                        new DevButton(true, null,
                                market.getStarSystem() == null ? null : market.getStarSystem().getId(), entity.getId()),
                        market.getName());
            }
            shown++;
        }
        if (shown == 0) info.addPara("尚无精神创伤占领区。", 5f);
    }

    private void addIncidents(TooltipMakerAPI info, PTSDCrisisState state, float width,
                              Color base, Color dark, float opad) {
        info.addSectionHeading("随机事件卡（公开报道 / 真实记录）", base, dark,
                com.fs.starfarer.api.ui.Alignment.MID, opad);
        int shown = 0;
        for (int i = state.incidents.size() - 1; i >= 0 && shown < MAX_EVENT_BUTTONS; i--) {
            PTSDCrisisState.CrisisIncident incident = state.incidents.get(i);
            if (incident == null) continue;
            String where = locationName(incident.targetSystemId, null);
            info.addPara("[%s] 分支 %s / %s / %s @ %s", shown == 0 ? 6f : 8f,
                    Misc.getHighlightColor(), incident.cardId, String.valueOf(incident.randomBranch),
                    incident.category, incident.headline, where);
            info.addPara("公开：" + incident.publicText, 1f);
            info.addPara("真实：" + incident.trueText + "｜" + incident.effectSummary, 1f,
                    Misc.getGrayColor(), new String[0]);
            addNavigationButtons(info, width, base, dark,
                    new DevButton(false, incident.linkedEventId, incident.targetSystemId, null),
                    new DevButton(true, incident.linkedEventId, incident.targetSystemId, null), where);
            shown++;
        }
        if (shown == 0) info.addPara("尚无随机事件卡。", 5f);
    }
    private void addStrategicEvents(TooltipMakerAPI info, PTSDCrisisState state, float width,
                                    Color base, Color dark, float opad) {
        info.addSectionHeading("战略事件（含远方隐藏事件）", base, dark,
                com.fs.starfarer.api.ui.Alignment.MID, opad);
        int shown = 0;
        for (int i = state.events.size() - 1; i >= 0 && shown < MAX_EVENT_BUTTONS; i--) {
            PTSDCrisisState.StrategicEvent event = state.events.get(i);
            SectorEntityToken target = resolveDisplayTarget(event.targetSystemId, event.targetMarketId,
                    event.materializedFleetId);
            String where = target == null ? locationName(event.targetSystemId, null) : target.getFullName();
            info.addPara("[%s] %s / %s / 强度 %s / 结算日 %s @ %s",
                    shown == 0 ? 6f : 8f, colorForStatus(event.status),
                    event.status.name(), event.type.name(), event.side,
                    String.valueOf(Math.round(event.strength)), format(event.resolveDay), where);
            if (event.description != null && !event.description.isEmpty()) info.addPara(event.description, 1f);
            addNavigationButtons(info, width, base, dark,
                    new DevButton(false, event.id, event.targetSystemId, event.materializedFleetId),
                    new DevButton(true, event.id, event.targetSystemId, event.materializedFleetId), where);
            shown++;
        }
        if (shown == 0) info.addPara("尚无战略事件。", 5f);
    }

    private void addRecords(TooltipMakerAPI info, float width, Color base, Color dark, float opad) {
        info.addSectionHeading("即时触发记录", base, dark,
                com.fs.starfarer.api.ui.Alignment.MID, opad);
        int shown = 0;
        for (int i = records.size() - 1; i >= 0 && shown < MAX_EVENT_BUTTONS; i--) {
            DevRecord record = records.get(i);
            String where = locationName(record.systemId, record.entityId);
            info.addPara("%s前 [%s] %s @ %s", shown == 0 ? 6f : 8f,
                    Misc.getHighlightColor(), elapsed(record.day), record.kind, record.description, where);
            addNavigationButtons(info, width, base, dark,
                    new DevButton(false, null, record.systemId, record.entityId),
                    new DevButton(true, null, record.systemId, record.entityId), where);
            shown++;
        }
        if (shown == 0) info.addPara("DevMode 启用后尚无即时触发。", 5f);
    }

    private void addNavigationButtons(TooltipMakerAPI info, float width, Color base, Color dark,
                                      DevButton go, DevButton arrive, String where) {
        ButtonAPI goButton = info.addButton("前往：" + where, go, base, dark, width, 22f, 2f);
        ButtonAPI arriveButton = info.addButton("到达：" + where, arrive, Color.ORANGE,
                new Color(80, 45, 20), width, 22f, 1f);
        boolean available = resolveTarget(go.eventId, go.systemId, go.entityId) != null;
        goButton.setEnabled(available);
        arriveButton.setEnabled(available);
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (!Global.getSettings().isDevMode()) return;
        if (OPEN_CONTROL.equals(buttonId)) {
            PTSDCrisisDevControlIntel intel = PTSDCrisisDevControlIntel.ensureIntel();
            if (intel != null) { ui.updateIntelList(true); ui.selectItem(intel); }
            return;
        }
        if (OPEN_PREWAR.equals(buttonId)) {
            PTSDCrisisIntel intel = PTSDCrisisIntel.ensureDevPreview();
            if (intel != null) { ui.updateIntelList(true); ui.selectItem(intel); }
            return;
        }
        if (OPEN_WAR.equals(buttonId)) {
            PTSDWarIntel intel = PTSDWarIntel.ensureDevPreview();
            if (intel != null) { ui.updateIntelList(true); ui.selectItem(intel); }
            return;
        }
        if (OPEN_COLONY_WEIGHTS.equals(buttonId) || OPEN_ALL_WEIGHTS.equals(buttonId)) {
            PTSDCrisisWeightIntel intel = PTSDCrisisWeightIntel.ensure(OPEN_ALL_WEIGHTS.equals(buttonId));
            if (intel != null) { ui.updateIntelList(true); ui.selectItem(intel); }
            return;
        }
        if (REFRESH.equals(buttonId)) {
            ui.updateUIForItem(this);
            return;
        }
        if (FORCE_DARK.equals(buttonId) || FORCE_PROBE.equals(buttonId)) {
            String category = FORCE_PROBE.equals(buttonId) ? "火力侦察" : "暗流";
            PTSDCrisisIncidentManager.forceRandomCategory(category);
            ui.updateUIForItem(this);
            return;
        }
        if (buttonId instanceof DevButton) {
            DevButton data = (DevButton) buttonId;
            SectorEntityToken target = resolveTarget(data.eventId, data.systemId, data.entityId);
            if (target != null) {
                if (data.arrive) arriveAt(target);
                else Global.getSector().layInCourseFor(target);
            }
            ui.updateUIForItem(this);
            return;
        }
        super.buttonPressConfirmed(buttonId, ui);
    }

    private static SectorEntityToken resolveTarget(String eventId, String systemId, String entityId) {
        if (Global.getSector() == null) return null;
        if (entityId != null) {
            SectorEntityToken entity = Global.getSector().getEntityById(entityId);
            if (entity != null) return entity;
        }
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null && eventId != null) {
            PTSDCrisisState.StrategicEvent event = state.getEvent(eventId);
            if (event != null) {
                if (event.materializedFleetId != null) {
                    SectorEntityToken fleet = Global.getSector().getEntityById(event.materializedFleetId);
                    if (fleet != null) return fleet;
                }
                if (event.targetMarketId != null) {
                    com.fs.starfarer.api.campaign.econ.MarketAPI market = state.resolveMarket(event.targetMarketId);
                    if (market != null && market.getPrimaryEntity() != null) return market.getPrimaryEntity();
                }
                systemId = event.targetSystemId;
            }
        }
        StarSystemAPI system = state == null ? Global.getSector().getStarSystem(systemId) : state.resolveSystem(systemId);
        if (system == null) return null;
        if (system.getCenter() != null) return system.getCenter();
        return system.getHyperspaceAnchor();
    }

    private static SectorEntityToken resolveDisplayTarget(String systemId, String marketId, String entityId) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (entityId != null && Global.getSector() != null) {
            SectorEntityToken entity = Global.getSector().getEntityById(entityId);
            if (entity != null) return entity;
        }
        if (state != null && marketId != null) {
            com.fs.starfarer.api.campaign.econ.MarketAPI market = state.resolveMarket(marketId);
            if (market != null && market.getPrimaryEntity() != null) return market.getPrimaryEntity();
        }
        return resolveTarget(null, systemId, null);
    }

    private static void arriveAt(SectorEntityToken target) {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        LocationAPI destination = target.getContainingLocation();
        if (player == null || destination == null) return;
        if (player.getContainingLocation() != destination) {
            if (player.getContainingLocation() != null) player.getContainingLocation().removeEntity(player);
            destination.addEntity(player);
            Global.getSector().setCurrentLocation(destination);
        }
        Vector2f offset = Misc.getUnitVectorAtDegreeAngle((float) (Math.random() * 360f));
        offset.scale(Math.max(500f, target.getRadius() + player.getRadius() + 250f));
        player.setLocation(target.getLocation().x + offset.x, target.getLocation().y + offset.y);
        player.setNoEngaging(2f);
        player.clearAssignments();
        player.addAssignment(FleetAssignment.GO_TO_LOCATION, target, 1f);
    }

    private String locationName(String systemId, String entityId) {
        SectorEntityToken target = resolveTarget(null, systemId, entityId);
        if (target != null) {
            StarSystemAPI system = target.getStarSystem();
            if (system != null) return target.getFullName() + " / " + system.getName();
            return target.getFullName();
        }
        PTSDCrisisState state = PTSDCrisisState.get();
        StarSystemAPI system = state == null ? null : state.resolveSystem(systemId);
        return system == null ? "未知位置" : system.getName();
    }

    private static String remaining(float targetDay) {
        float days = targetDay - PTSDCrisisState.getDay();
        if (days <= 0f) return "已到期";
        return format(days) + " 天后";
    }

    private static String elapsed(float startDay) {
        float days = Math.max(0f, PTSDCrisisState.getDay() - startDay);
        return format(days) + " 天";
    }
    private static String format(float value) {
        return String.valueOf(Math.round(value * 10f) / 10f);
    }

    private static String yesNo(boolean value) {
        return value ? "是" : "否";
    }

    private static Color colorForStatus(PTSDCrisisState.EventStatus status) {
        if (status == PTSDCrisisState.EventStatus.MATERIALIZED) return Color.ORANGE;
        if (status == PTSDCrisisState.EventStatus.RESOLVED) return new Color(130, 210, 150);
        if (status == PTSDCrisisState.EventStatus.CANCELLED) return Misc.getGrayColor();
        return Misc.getHighlightColor();
    }

    @Override
    public boolean shouldRemoveIntel() {
        return !Global.getSettings().isDevMode() || super.shouldRemoveIntel();
    }

    @Override
    public boolean isHidden() {
        return !Global.getSettings().isDevMode();
    }

    @Override
    public String getIcon() {
        FactionAPI faction = getFactionForUIColors();
        return faction == null ? null : faction.getCrest();
    }

    @Override
    public FactionAPI getFactionForUIColors() {
        FactionAPI faction = Global.getSector() == null ? null :
                Global.getSector().getFaction(IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION);
        return faction == null ? super.getFactionForUIColors() : faction;
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_IMPORTANT);
        tags.add("危机");
        tags.add("DEV");
        return tags;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return null;
        StarSystemAPI base = state.resolveSystem(state.baseSystemId);
        if (base != null) return base.getHyperspaceAnchor();
        List<PTSDCrisisState.StrategicEvent> active = state.getActiveEvents();
        if (!active.isEmpty()) {
            StarSystemAPI target = state.resolveSystem(active.get(active.size() - 1).targetSystemId);
            if (target != null) return target.getHyperspaceAnchor();
        }
        return null;
    }
}
