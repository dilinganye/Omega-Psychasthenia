package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldSource;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static data.scripts.IIRT_Omega_ModPlugin.*;

/** Random, save-persistent narrative cards for the dark-current and fire-probe eras. */
public final class PTSDCrisisIncidentManager {
    private enum TargetKind { ANY, EDGE_MARKET, PIRATE, RELAY, POPULATED, MILITARY, WILDERNESS }

    private static final class Card {
        final String id;
        final String category;
        final TargetKind target;
        final float weight;
        final float cooldown;
        final float recon;
        final float awareness;
        final float aggression;
        final float panic;
        final float distortion;
        final float physicalChance;
        final float strength;
        final String source;
        final String[] headlines;
        final String[] reports;
        final String[] truths;

        Card(String id, String category, TargetKind target, float weight, float cooldown,
             float recon, float awareness, float aggression, float panic, float distortion,
             float physicalChance, float strength, String source,
             String[] headlines, String[] reports, String[] truths) {
            this.id = id;
            this.category = category;
            this.target = target;
            this.weight = weight;
            this.cooldown = cooldown;
            this.recon = recon;
            this.awareness = awareness;
            this.aggression = aggression;
            this.panic = panic;
            this.distortion = distortion;
            this.physicalChance = physicalChance;
            this.strength = strength;
            this.source = source;
            this.headlines = headlines;
            this.reports = reports;
            this.truths = truths;
        }
    }

    private static final class Target {
        final StarSystemAPI system;
        final MarketAPI market;

        Target(StarSystemAPI system, MarketAPI market) {
            this.system = system;
            this.market = market;
        }
    }

    private static final List<Card> DARK = new ArrayList<Card>();
    private static final List<Card> PROBE = new ArrayList<Card>();
    private static final Color WHISPER_COLOR = new Color(182, 164, 198);

    static {
        dark("D-01", TargetKind.EDGE_MARKET, 8, 35, .8f, 0, 0, .2f, 0, .35f,
                "少了一班船", "港务局将一艘逾期补给船登记为导航事故。搜救航次没有找到求救信标。",
                "补给船被第四窥视无声截停；货舱和航行日志被取走，残骸被推离航路。", "边缘港务局");
        dark("D-02", TargetKind.PIRATE, 7, 40, 1.3f, 0, .5f, .2f, 0, 0,
                "没有海盗认领", "数支海盗舰队在同一航段失联。附近电台罕见地一致否认对此负责。",
                "第四窥视用海盗舰队校准了对低纪律编队的火控与追逃模型。", "匿名航路简报");
        dark("D-03", TargetKind.RELAY, 9, 28, 1.4f, .2f, .1f, .1f, 0, .25f,
                "第七码", "一座通讯基站每天在同一毫秒产生额外校验码。维护程序会在记录生成后将其删除。",
                "基站正在被窄束监听；额外校验码是观测单元确认数据完整性的回执。", "基站维护日志");
        dark("D-04", TargetKind.ANY, 6, 42, .3f, .1f, 0, 1.1f, .2f, 0,
                "空舱漂流", "救援队找到一艘生命维持仍在运行的穿梭艇。乘员、日志和个人终端均不在船上。",
                "乘员和信息载体被完整带走；第四窥视没有留下可供归因的战斗损伤。", "民用救援频道");
        dark("D-05", TargetKind.ANY, 5, 45, .2f, 0, 0, .2f, .4f, 0,
                "星图上不存在的回波", "数名领航员报告星图曾短暂显示一颗不存在的恒星。重新校准后，记录彼此无法对应。",
                "分布在超空间的观测单元用同步脉冲测量了民用导航系统的纠错过程。", "领航员互助网");
        dark("D-06", TargetKind.RELAY, 7, 38, .7f, .1f, .1f, .5f, .1f, 0,
                "边缘灯塔熄灭", "数座导航设施依照相同顺序关闭，又在数日后自行恢复。管理方称原因是设备老化。",
                "关闭顺序标出了航路在失去引导时的真实流量；恢复动作来自设施外部。", "航路管理通告");
        dark("D-07", TargetKind.ANY, 6, 45, 1.1f, .2f, .2f, .3f, .2f, .5f,
                "被整理过的残骸", "一处旧战场的残骸被排列成同心圆，所有武器接口都遭到整齐切除。",
                "第四窥视对交战双方完成了舰体取样，并把无价值碎片作为空间标尺重新排列。", "打捞者留言板");
        dark("D-08", TargetKind.PIRATE, 5, 55, 1.2f, .1f, .5f, .3f, 0, 0,
                "零伤亡的失守", "一处非法补给点被完整清空。现场没有尸体，也没有足以解释撤离的武器痕迹。",
                "观测单元切断生命维持并接管内部网络；人员在设施恢复供能前自行逃离。", "海盗电台");
        dark("D-09", TargetKind.POPULATED, 4, 60, 0, .1f, 0, .8f, .8f, 0,
                "相同的梦", "互不相识的通信员报告了相同梦境：一支舰队从屏幕边缘驶过，却无法被转向观察。",
                "一次低强度的认知信道注入被用于测量人口世界的通信岗位轮换与恢复时间。", "地方医疗公报");
        dark("D-10", TargetKind.POPULATED, 6, 42, 1.0f, .3f, .1f, .4f, 0, 0,
                "延迟十二秒", "公开频道出现统一的十二秒延迟，军事频道未受影响。中继机构称服务已经恢复。",
                "第四窥视由优先级差异反推出军民通信拓扑，并确认了战时信道的保留容量。", "中继网络状态页");
        dark("D-11", TargetKind.POPULATED, 5, 35, .4f, .2f, .1f, .3f, .2f, 0,
                "错误的识别灯", "一支友军舰队在传感器上短暂显示为未知目标。双方设备检查均未发现识别码变更。",
                "观测单元篡改了接收端的分类结果，并记录附近舰队在误判后的武器解锁延迟。", "舰队事故记录");
        dark("D-12", TargetKind.WILDERNESS, 7, 36, 1.5f, .1f, .3f, .2f, 0, .45f,
                "静默打捞者", "探索者在无人星系拍到一组移动亮点。抵近后，只剩被拆解了一半的旧时代残骸。",
                "第四窥视正在回收旧舰体与武器接口；发现观察者后，它把撤离本身也当作一次测试。", "探索者协会");

        probe("P-01", TargetKind.MILITARY, 8, 28, 2.0f, 1.2f, 2.4f, 1.0f, .2f, .85f, 34,
                "一轮齐射", "一支边缘巡逻队报告旗舰推进器遭精确击毁。未知舰船没有扩大战果。",
                "第四窥视只进行了一轮齐射，用于记录编队失去指挥后的恢复顺序。", "地方巡逻司令部");
        probe("P-02", TargetKind.MILITARY, 7, 24, 1.5f, .8f, 1.8f, .7f, .2f, .15f, 24,
                "火控照射", "多支舰队报告遭到高强度火控雷达照射。锁定只持续数秒，没有攻击到来。",
                "照射脉冲提取了电子战、护盾响应和目标优先级数据。", "军用频段截获");
        probe("P-03", TargetKind.EDGE_MARKET, 6, 35, 1.8f, 1.2f, 2.0f, 1.6f, .2f, .65f, 30,
                "炮火越过舰桥", "运输舰舰桥前方出现实弹烧蚀轨迹。攻击者在护航舰完成转向前已经离开。",
                "射击故意偏离目标；第四窥视测量了民用编队遭到致命威胁时的分散模式。", "商船公会警报");
        probe("P-04", TargetKind.PIRATE, 6, 38, 2.2f, .8f, 2.3f, .7f, .1f, .7f, 32,
                "失去武器的海盗", "一支海盗舰队仍能航行，但所有武器均被从安装座上切除。幸存者拒绝说明经过。",
                "第四窥视在不摧毁舰体的前提下完成了解除武装测试。", "海盗悬赏频道");
        probe("P-05", TargetKind.MILITARY, 8, 32, 2.8f, 2.0f, 3.2f, 1.8f, .3f, .9f, 48,
                "三分钟战争", "一场高强度交战在三分钟内开始并结束。未知方主动脱离，双方战损尚未公布。",
                "第四窥视完成了第一次完整实战采样，并在对方援军抵达前按计划退出。", "未经核实的战斗报告");
        probe("P-06", TargetKind.POPULATED, 6, 38, 2.0f, 1.2f, 2.2f, 1.0f, .2f, .25f, 30,
                "错误的增援方向", "殖民地求援信标曾指向错误跳跃点。增援抵达后，轨道防线记录出现短暂空白。",
                "第四窥视伪造求援方位，从无人防守的一侧完成了抵近测绘。", "殖民地安全通告");
        probe("P-07", TargetKind.MILITARY, 4, 52, 2.8f, 1.5f, 3.0f, 1.4f, .3f, .7f, 58,
                "无意义的决斗", "一艘未知舰船持续照射编队旗舰。短暂交火后，它拒绝追击并离开。",
                "所谓决斗只是为了隔离并记录高质量军官的指挥习惯。", "佣兵内部简报");
        probe("P-08", TargetKind.RELAY, 7, 32, 2.5f, 1.4f, 2.8f, 1.2f, .2f, .8f, 42,
                "跃迁点夺秒", "主要跳跃点遭到短暂封锁。未知舰船在第一支增援出现后立即撤离。",
                "封锁持续时间由各势力实际响应速度决定；该星系已被标为可突破目标。", "航路紧急通知");
        probe("P-09", TargetKind.POPULATED, 6, 44, 2.6f, 1.8f, 3.0f, 1.5f, .4f, .65f, 50,
                "防御平台停电", "轨道站主武器离线七十秒。一艘未知舰船进入有效射程，随后自行退出。",
                "第四窥视验证了电子战窗口，并完成对平台备用供能和人工接管速度的测量。", "轨道设施事故报告");
        probe("P-10", TargetKind.ANY, 5, 45, 1.2f, 3.0f, 1.4f, 2.0f, .8f, .2f, 20,
                "武器残片", "打捞者发现无法归类的烧蚀残片。样本离开真空后开始缓慢失去结构。",
                "残片由第四窥视有意留下，用于观察人类的回收、运输和研究链条。", "研究悬赏摘要");
        probe("P-11", TargetKind.EDGE_MARKET, 4, 65, 2.0f, 2.0f, 4.0f, 2.2f, .4f, .95f, 65,
                "第一次报复", "一处外围设施遭到短促袭击。攻击与近期针对未知舰船的追捕行动存在模糊关联。",
                "第四窥视首次把玩家行为、关联设施和惩罚性火力编入同一组因果模型。", "加密事故汇编");
        probe("P-12", TargetKind.RELAY, 3, 90, 1.8f, 3.5f, 2.0f, 3.2f, 1.0f, 0, 0,
                "全频道空白", "多个边缘星系同时失联数分钟。通信恢复后，没有机构能够解释空白期间发生了什么。",
                "第四窥视已完成本轮测量；观察单位正沿不同航线撤向同一片未公开星域。", "全星区中继汇总");
    }

    private PTSDCrisisIncidentManager() { }

    private static void dark(String id, TargetKind target, float weight, float cooldown,
                             float recon, float awareness, float aggression, float panic, float distortion,
                             float physicalChance, String headline, String report, String truth, String source) {
        DARK.add(card(id, "暗流", target, weight, cooldown, recon, awareness, aggression, panic,
                distortion, physicalChance, 0f, headline, report, truth, source));
    }

    private static void probe(String id, TargetKind target, float weight, float cooldown,
                              float recon, float awareness, float aggression, float panic, float distortion,
                              float physicalChance, float strength, String headline, String report,
                              String truth, String source) {
        PROBE.add(card(id, "火力侦察", target, weight, cooldown, recon, awareness, aggression, panic,
                distortion, physicalChance, strength, headline, report, truth, source));
    }

    private static Card card(String id, String category, TargetKind target, float weight, float cooldown,
                             float recon, float awareness, float aggression, float panic, float distortion,
                             float physicalChance, float strength, String headline, String report,
                             String truth, String source) {
        return new Card(id, category, target, weight, cooldown, recon, awareness, aggression, panic,
                distortion, physicalChance, strength, source,
                variants(headline), variants(report), variants(truth));
    }

    /** Adds small, deterministic wording differences without changing the card's factual core. */
    private static String[] variants(String base) {
        return new String[] { base, base, base };
    }

    public static void advance(PTSDCrisisState state, float day, Random random) {
        if (state == null || random == null || day < state.nextIncidentDay) return;
        if (state.phase != PTSDCrisisState.Phase.DORMANT && state.phase != PTSDCrisisState.Phase.RECON) return;

        float frequency = Math.max(.1f, unknown_event_frequency);
        state.nextIncidentDay = day + between(random, unknown_event_min_interval,
                Math.max(unknown_event_min_interval, unknown_event_max_interval)) / frequency;

        boolean fireProbe = state.phase == PTSDCrisisState.Phase.RECON &&
                (state.watcherAggression >= 22f || state.reconConfidence >= 36f ||
                        day - state.phaseStartedDay >= Math.max(8f, collect_data_time * .42f));
        List<Card> pool = fireProbe ? PROBE : (state.phase == PTSDCrisisState.Phase.DORMANT ? DARK : null);
        if (pool == null) return;

        for (int attempt = 0; attempt < 12; attempt++) {
            Card card = pick(pool, state, day, random);
            if (card == null) return;
            Target target = pickTarget(card.target, state, random);
            if (target == null || target.system == null) continue;
            createIncident(state, card, target, day, random, false);
            return;
        }
        state.nextIncidentDay = Math.min(state.nextIncidentDay, day + 2f);
        PTSDCrisisDevIntel.report("未知事件抽取失败", "候选目标均失效；两日后重试", null, null);
    }

    public static boolean forceRandomCategory(String category) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return false;
        List<Card> pool = "火力侦察".equals(category) ? PROBE : DARK;
        Random random = new Random(Misc.genUID().hashCode());
        WeightedRandomPicker<Card> picker = new WeightedRandomPicker<Card>(random);
        for (Card card : pool) picker.add(card, card.weight);
        for (int attempt = 0; attempt < 12; attempt++) {
            Card card = picker.pick();
            if (card == null) return false;
            Target target = pickTarget(card.target, state, random);
            if (target == null) continue;
            createIncident(state, card, target, PTSDCrisisState.getDay(), random, true);
            return true;
        }
        return false;
    }
    public static boolean force(String cardId) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || cardId == null) return false;
        Card card = find(cardId);
        if (card == null) return false;
        Random random = new Random(Misc.genUID().hashCode());
        Target target = pickTarget(card.target, state, random);
        if (target == null) return false;
        createIncident(state, card, target, PTSDCrisisState.getDay(), random, true);
        return true;
    }

    private static Card find(String id) {
        for (Card card : DARK) if (card.id.equalsIgnoreCase(id)) return card;
        for (Card card : PROBE) if (card.id.equalsIgnoreCase(id)) return card;
        return null;
    }

    private static Card pick(List<Card> pool, PTSDCrisisState state, float day, Random random) {
        WeightedRandomPicker<Card> picker = new WeightedRandomPicker<Card>(random);
        for (Card card : pool) {
            Float until = state.incidentCooldowns.get(card.id);
            if (until != null && day < until) continue;
            float weight = card.weight;
            if ("P-11".equals(card.id)) weight *= 1f + state.totalScoutEscapes * .18f;
            if ("P-12".equals(card.id)) weight *= Math.max(.15f, state.watcherAggression / 55f);
            picker.add(card, weight);
        }
        return picker.pick();
    }

    private static Target pickTarget(TargetKind kind, PTSDCrisisState state, Random random) {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player != null && player.getStarSystem() != null && random.nextFloat() < .18f) {
            MarketAPI playerMarket = bestMarket(player.getStarSystem());
            if (matches(kind, player.getStarSystem(), playerMarket)) {
                return new Target(player.getStarSystem(), playerMarket);
            }
        }

        WeightedRandomPicker<Target> picker = new WeightedRandomPicker<Target>(random);
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system == null || system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)) continue;
            List<MarketAPI> markets = Global.getSector().getEconomy().getMarkets(system);
            boolean populated = false;
            for (MarketAPI market : markets) {
                if (market != null && !market.isPlanetConditionMarketOnly() && !isCrisisMarket(market)) {
                    populated = true;
                    break;
                }
            }
            if (!populated && (kind == TargetKind.WILDERNESS || kind == TargetKind.ANY)) {
                picker.add(new Target(system, null), 2f);
            }
            for (MarketAPI market : markets) {
                if (market == null || market.isPlanetConditionMarketOnly() || isCrisisMarket(market)) continue;
                if (!matches(kind, system, market)) continue;
                float edge = Math.max(1f, system.getLocation().length() / 12000f);
                float weight = kind == TargetKind.EDGE_MARKET ? edge * (2f + market.getSize()) : 2f + market.getSize();
                picker.add(new Target(system, market), weight);
            }
            if (kind == TargetKind.RELAY && !system.getEntitiesWithTag(Tags.COMM_RELAY).isEmpty()) {
                picker.add(new Target(system, bestMarket(system)), 7f);
            }
        }
        return picker.pick();
    }

    private static boolean matches(TargetKind kind, StarSystemAPI system, MarketAPI market) {
        if (kind == TargetKind.ANY) return true;
        if (kind == TargetKind.WILDERNESS) return market == null;
        if (kind == TargetKind.RELAY) return system != null && !system.getEntitiesWithTag(Tags.COMM_RELAY).isEmpty();
        if (market == null) return false;
        if (kind == TargetKind.PIRATE) return Factions.PIRATES.equals(market.getFactionId());
        if (kind == TargetKind.POPULATED || kind == TargetKind.EDGE_MARKET) return market.getSize() >= 3;
        if (kind == TargetKind.MILITARY) {
            return market.hasIndustry("militarybase") || market.hasIndustry("highcommand") || market.hasIndustry("patrolhq");
        }
        return true;
    }

    private static boolean isCrisisMarket(MarketAPI market) {
        String id = market.getFactionId();
        return IIRT_Omega_Invasion.WATCHER_FACTION.equals(id) ||
                IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(id);
    }

    private static MarketAPI bestMarket(StarSystemAPI system) {
        MarketAPI best = null;
        for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system)) {
            if (market == null || market.isPlanetConditionMarketOnly() || isCrisisMarket(market)) continue;
            if (best == null || market.getSize() > best.getSize()) best = market;
        }
        return best;
    }

    private static void createIncident(PTSDCrisisState state, Card card, Target target,
                                       float day, Random random, boolean forced) {
        int branch = random.nextInt(3);
        float branchMult = branch == 0 ? .85f : (branch == 2 ? 1.18f : 1f);
        PTSDCrisisState.CrisisIncident incident = new PTSDCrisisState.CrisisIncident();
        incident.id = "PTSD_incident_" + Misc.genUID();
        incident.cardId = card.id;
        incident.category = card.category;
        incident.randomBranch = branch;
        incident.phase = state.phase;
        incident.targetSystemId = target.system.getId();
        incident.targetMarketId = target.market == null ? null : target.market.getId();
        incident.createdDay = day;
        incident.expiresDay = day + card.cooldown;
        incident.sourceLabel = sourceVariant(card.source, branch);
        incident.headline = headlineVariant(card.headlines[branch], branch);
        incident.publicText = reportVariant(card.reports[branch], branch);
        incident.trueText = truthVariant(card.truths[branch], branch);
        incident.disclosed = forced || card.category.equals("火力侦察") || random.nextFloat() < .72f;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        incident.playerRelevant = player != null && player.getStarSystem() == target.system;
        incident.devForced = forced;
        incident.effectSummary = applyEffects(state, card, target.system.getId(), branchMult);

        if (card.physicalChance > 0f && (forced || random.nextFloat() < card.physicalChance)) {
            if (card.category.equals("火力侦察") && card.strength > 0f) {
                PTSDCrisisState.StrategicEvent event = state.addEvent(
                        PTSDCrisisState.EventType.FIRE_PROBE, PTSDCrisisAPI.SIDE_OMEGA,
                        IIRT_Omega_Invasion.WATCHER_FACTION, null, target.system.getId(),
                        target.market == null ? null : target.market.getId(),
                        Math.max(18f, card.strength * branchMult), between(random, 4f, 9f));
                event.description = card.id + "：" + incident.trueText;
                event.playerRelevant = incident.playerRelevant;
                event.referenceId = incident.id;
                incident.linkedEventId = event.id;
            } else if (incident.playerRelevant && ("D-01".equals(card.id) || "D-07".equals(card.id))) {
                projectDebris(target.system, target.market, card.id, random);
            } else if ("D-12".equals(card.id)) {
                state.nextScoutDay = Math.min(state.nextScoutDay, day + .25f);
            }
        }

        state.incidents.add(incident);
        while (state.incidents.size() > 100) state.incidents.remove(0);
        state.incidentCooldowns.put(card.id, day + card.cooldown);
        PTSDCrisisNewsIntel.report(incident);
        PTSDCrisisDevIntel.report("随机事件 " + card.id,
                "分支 " + branch + "｜公开：" + incident.publicText + "｜真实：" + incident.trueText +
                        "｜影响：" + incident.effectSummary,
                incident.targetSystemId, null);

        if (incident.playerRelevant) {
            Global.getSector().getCampaignUI().addMessage(incident.headline + "：" + incident.publicText,
                    WHISPER_COLOR);
        }
    }

    private static String applyEffects(PTSDCrisisState state, Card card, String systemId, float mult) {
        add(state, PTSDCrisisProgress.Variable.RECON_CONFIDENCE, card.recon * mult, card.id, systemId);
        add(state, PTSDCrisisProgress.Variable.HUMAN_AWARENESS, card.awareness * mult, card.id, systemId);
        add(state, PTSDCrisisProgress.Variable.WATCHER_AGGRESSION, card.aggression * mult, card.id, systemId);
        add(state, PTSDCrisisProgress.Variable.PUBLIC_PANIC, card.panic * mult, card.id, systemId);
        add(state, PTSDCrisisProgress.Variable.REALITY_DISTORTION, card.distortion * mult, card.id, systemId);
        PTSDCrisisState.SystemData data = state.getSystemData(systemId);
        data.lastObservedDay = PTSDCrisisState.getDay();
        data.attackWeight *= 1f + Math.min(.22f, (card.recon + card.aggression) * .018f * mult);
        if (card.category.equals("火力侦察")) data.hostileContacts++;
        return "侦察+" + round(card.recon * mult) + "，认知+" + round(card.awareness * mult) +
                "，攻击性+" + round(card.aggression * mult) + "，恐慌+" + round(card.panic * mult);
    }

    private static void add(PTSDCrisisState state, PTSDCrisisProgress.Variable variable,
                            float amount, String source, String systemId) {
        if (amount > 0f) PTSDCrisisProgress.add(state, variable, amount, source, systemId);
    }

    private static void projectDebris(StarSystemAPI system, MarketAPI market, String cardId, Random random) {
        SectorEntityToken focus = market != null && market.getPrimaryEntity() != null ?
                market.getPrimaryEntity() : system.getCenter();
        if (focus == null) return;
        DebrisFieldParams params = new DebrisFieldParams(180f, -1f, 5f, .25f);
        params.source = DebrisFieldSource.BATTLE;
        params.baseSalvageXP = 20;
        SectorEntityToken debris = Misc.addDebrisField(system, params, random);
        Vector2f point = Misc.getPointWithinRadius(focus.getLocation(), 1600f);
        debris.setLocation(point.x, point.y);
        debris.setName("无法归类的微小碎片");
        PTSDCrisisDevIntel.report("暗流实体投影 " + cardId, "仅在玩家已位于目标星系时生成", system.getId(), debris.getId());
    }

    private static String sourceVariant(String source, int branch) {
        if (branch == 0) return source;
        if (branch == 1) return source + "（转述）";
        return "未经核实 / " + source;
    }

    private static String headlineVariant(String text, int branch) {
        if (branch == 0) return text;
        if (branch == 1) return text + "：后续记录";
        return "未证实：" + text;
    }

    private static String reportVariant(String text, int branch) {
        if (branch == 0) return text;
        if (branch == 1) return text + " 当地机构称没有理由发布进一步警告。";
        return text + " 不同来源对时间和方位的描述并不一致。";
    }

    private static String truthVariant(String text, int branch) {
        if (branch == 0) return text + " 行动比预定时间提前结束。";
        if (branch == 1) return text;
        return text + " 误差本身也被写入了下一轮观测参数。";
    }

    private static float between(Random random, float min, float max) {
        float low = Math.min(min, max);
        return low + random.nextFloat() * Math.max(0f, Math.max(min, max) - low);
    }

    private static String round(float value) {
        return String.valueOf(Math.round(value * 10f) / 10f);
    }
}
