package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.shard.PTSD_BaseShard_Util;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Collections;
import java.util.Map;

/** Sealed interaction for colonies controlled by Psychasthenia. */
public final class PTSDOccupiedColonyInteraction implements InteractionDialogPlugin {
    private static final String BOMBARD = "PTSD_OCC_BOMBARD";
    private static final String FULL_BOMBARD = "PTSD_OCC_FULL_BOMBARD";
    private static final String PROBE = "PTSD_OCC_PROBE";
    private static final String NEGOTIATE = "PTSD_OCC_NEGOTIATE";
    private static final String NEGOTIATE_2 = "PTSD_OCC_NEGOTIATE_2", NEGOTIATE_2_2 = "PTSD_OCC_NEGOTIATE_2_2";
    private static final String NEGOTIATE_END = "PTSD_OCC_NEGOTIATE_END";
    private static final String INTERCEPTOR = "PTSD_OCC_INTERCEPTOR";
    private static final String INTERCEPTOR_2 = "PTSD_OCC_INTERCEPTOR_2";
    private static final String INTERCEPTOR_2Late = "PTSD_OCC_INTERCEPTOR_2_LATE";
    private static final String LEAVE = "PTSD_OCC_LEAVE", WAIT =  "PTSD_OCC_WAIT";

    private InteractionDialogAPI dialog;
    private MarketAPI market;
    private PTSDOccupationAPI.InteractionContext context;
    private float hostileElapsed;
    private boolean intrusionTextCleared;
    private boolean intrusionResolved;

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        SectorEntityToken target = dialog.getInteractionTarget();
        this.market = target == null ? null : target.getMarket();
        this.context = PTSDOccupationAPI.createContext(market);
        if (target != null) {
            // Use the engine's actual spherical planet renderer; planet textures are equirectangular
            // and become a stretched square when drawn directly as a SpriteAPI.
            dialog.getVisualPanel().setVisualFade(0.12f, 0.35f);
            dialog.getVisualPanel().showLargePlanet(target);
        }
        showMain();
    }
    private void baseOption(TextPanelAPI text, OptionPanelAPI options ){
        // text.clear();
        // options.clearOptions();
        int harassmentCost = getHarassmentCost();
        options.addOption("进行远距轰炸（消耗 " + harassmentCost + " 燃料）", BOMBARD,
                "不进入近轨道，在防御网边缘投射高能弹药。这会造成有限破坏，并显著提高目标对本星系的注意。");
        setFuelAvailability(BOMBARD, harassmentCost);

        options.addOption("尝试试探", PROBE,
                "以小规模探测与火控照射测试防御反应。预计会立即招致一支防御舰队。");
        options.addOption("尝试交涉", NEGOTIATE,
                "向地表和轨道设施发送多频段通讯请求，但没有迹象表明对方愿意交流。");

        if (market != null && market.getMemoryWithoutUpdate().getBoolean(
                PTSDOccupationManager.DEFENSE_DEFEATED_MEMORY)) {
            int fullCost = getFullBombardmentCost();
            options.addOption("全面轰炸（消耗 " + fullCost + " 燃料）", FULL_BOMBARD,
                    Color.ORANGE, "防御舰队已被击溃，可以进入近轨道实施高强度全面轰炸。");
            setFuelAvailability(FULL_BOMBARD, fullCost);
        }

        if (context != null) {
            for (PTSDOccupationAPI.InteractionExtension extension : PTSDOccupationAPI.getExtensions()) {
                try {
                    extension.addOptions(context, dialog);
                } catch (Throwable ex) {
                    Global.getLogger(getClass()).warn("Unable to add occupation interaction extension", ex);
                }
            }
        }
    }
    private void showMain() {
        TextPanelAPI text = dialog.getTextPanel();
        OptionPanelAPI options = dialog.getOptionPanel();

        dialog.getTextPanel().addPara("你正在接近一颗明显被未知造物控制的星球——星球那异样的地表就能很清楚的显示这一点。\n但在这片星域彻底安全之前，你无法，也不可能在不受威胁的情况下对这里进行任何操作。",new Color(235, 185, 130));
        dialog.getTextPanel().addPara("轨道内完全没有任何通讯，而这很令人诧异：\n里 面 连 一 点 噪 波 都 没 有\n\n传感器对其发射的波段仿佛被吸收了一样，整个星球在探测仪上只有一个轻微的轮廓。");
        dialog.getTextPanel().addPara("\"长官，我们的探测仪在一个极其少见的频段内，探测到了轻微的人造波，我们怀疑内部可能有某种人造的，或者非人造的设施。\"\n...也许是某种正在等待你先采取行动的防御机制。");
        baseOption(text,options);
        options.addOption("离开", LEAVE);
        options.setShortcut(LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
        dialog.setOptionOnEscape("离开", LEAVE);
    }

    private void setFuelAvailability(Object option, int cost) {
        float fuel = Global.getSector().getPlayerFleet().getCargo().getFuel();
        if (fuel < cost) {
            dialog.getOptionPanel().setEnabled(option, false);
            dialog.getOptionPanel().setTooltip(option,
                    "需要 " + cost + " 燃料；当前只有 " + Math.round(fuel) + "。");
        }
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (LEAVE.equals(optionData)) {
            dialog.dismiss();
            return;
        }
        if (BOMBARD.equals(optionData)) {
            performBombardment(false);
            return;
        }
        if (FULL_BOMBARD.equals(optionData)) {
            performBombardment(true);
            return;
        }
        if (PROBE.equals(optionData)) {
            recordProbe();
            spawnDefense(false);
            return;
        }
        if (NEGOTIATE.equals(optionData)) {
            showNegotiationStart();
            return;
        }
        if (NEGOTIATE_2.equals(optionData)) {
            showNegotiationStart_2();
            return;
        }
        if (NEGOTIATE_2_2.equals(optionData)) {
            showNegotiationAttack();
            return;
        }
        if (INTERCEPTOR.equals(optionData)) {
            showIntercept();
            return;
        }
        if (INTERCEPTOR_2.equals(optionData)) {
            showIntercept_2();
            return;
        }
        if (INTERCEPTOR_2Late.equals(optionData)) {
            showIntercept_2Late();
            return;
        }
        if (NEGOTIATE_END.equals(optionData)) {
            recordNegotiation();
            spawnDefense(true);
            return;
        }
        if (WAIT.equals(optionData)) {
            showIntercept_2Wait();
            return;
        }
        if (context != null) {
            for (PTSDOccupationAPI.InteractionExtension extension : PTSDOccupationAPI.getExtensions()) {
                try {
                    if (extension.optionSelected(context, dialog, optionData)) return;
                } catch (Throwable ex) {
                    Global.getLogger(getClass()).warn("Occupation interaction extension failed", ex);
                }
            }
        }
    }

    private void performBombardment(boolean full) {
        int cost = full ? getFullBombardmentCost() : getHarassmentCost();
        if (Global.getSector().getPlayerFleet().getCargo().getFuel() < cost) {
            showMain();
            return;
        }
        hostileElapsed = 0;
        Global.getSector().getPlayerFleet().getCargo().removeFuel(cost);
        PTSDCrisisState state = PTSDCrisisState.get();
        PTSDCrisisState.OccupationData data = state == null ? null : state.getOccupationData(market.getId());
        PTSDCrisisState.SystemData system = state == null || market.getStarSystem() == null
                ? null : state.getSystemData(market.getStarSystem().getId());

        dialog.getTextPanel().clear();
        dialog.getOptionPanel().clearOptions();
        if (full) {
            dialog.getTextPanel().addPara("舰队压入近轨道。高密度弹幕沿着先前标定的能量节点依次落下，地表的几何光带成片熄灭，又在更深的地层中重新亮起。你造成了真实破坏，但也让整个星系明确知道了你的位置。",
                    Color.ORANGE);
            if (data != null) {
                data.saturationBombardments++;
                data.accumulatedDamage += 0.32f;
            }
            if (system != null) {
                system.omegaControl = Math.max(0.2f, system.omegaControl - 0.16f);
                system.conversionLevel = Math.max(0, system.conversionLevel - 1);
            }
            market.getMemoryWithoutUpdate().unset(PTSDOccupationManager.DEFENSE_DEFEATED_MEMORY);
            PTSDOccupationAPI.addAttention(market, PTSDOccupationAPI.Action.SATURATION_BOMBARDMENT,
                    2.1f, 0.45f);
            if (state != null && market.getStarSystem() != null) {
                state.addEvent(PTSDCrisisState.EventType.GARRISON, PTSDCrisisAPI.SIDE_OMEGA,
                        IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION, state.baseSystemId,
                        market.getStarSystem().getId(), market.getId(), 95f, 5f);
            }
        } else {
            dialog.getTextPanel().addPara("弹道在防御网的外沿展开。大部分弹体被不可见的拦截束蒸发，少数命中在地表留下短暂的黑色空洞。几秒后，更多传感器阵列转向你的舰队——这次轰炸更像一次响亮的宣告。",
                    new Color(235, 185, 130));
            if (data != null) {
                data.harassmentBombardments++;
                data.accumulatedDamage += 0.05f;
            }
            if (system != null) system.omegaControl = Math.max(0.35f, system.omegaControl - 0.03f);
            PTSDOccupationAPI.addAttention(market, PTSDOccupationAPI.Action.HARASSMENT_BOMBARDMENT,
                    0.75f, 0.12f);
        }
        dialog.getTextPanel().addPara("消耗燃料：" + cost, Misc.getHighlightColor());
        dialog.getOptionPanel().addOption("离开轨道", LEAVE);
        dialog.getOptionPanel().setShortcut(LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    private void showNegotiationStart() {
        hostileElapsed -= 10f;
        dialog.getTextPanel().clear();
        dialog.getOptionPanel().clearOptions();
        dialog.getTextPanel().addPara("尽管你的副官并不这么建议你，你还是命令你的通讯官员尝试向对方建立连接。随后，每一个信道都在同一瞬间返回了应答。");
        dialog.getTextPanel().addPara("起初，这一切的回应只有静电噪声和宇宙中不时传入的幽灵讯号经由播音器在舰桥回响，那不时扬起的噪波段让人感到烦躁，但所有人都在屏息以待。",
                new Color(230, 120, 150));
        dialog.getOptionPanel().addOption("等待", NEGOTIATE_2);
    }
    private void showNegotiationStart_2() {
        dialog.getTextPanel().clear();
        dialog.getOptionPanel().clearOptions();
        dialog.getTextPanel().addPara("每一个信道都在同一瞬间返回了应答。");
        dialog.getTextPanel().addPara("那不是语言。那是成千上万个伪造的舰队识别码同时在信道中涌出的尖啸，接收缓存瞬间溢出，通讯官向你播报了大量的恶意通讯攻击讯号，导航数据被替换成互相矛盾的跃迁坐标，甚至连舰内成员的私人终端都开始播放从未录制过的求救讯息。",
                new Color(230, 120, 150));
        dialog.getOptionPanel().addOption("切断外部信道", NEGOTIATE_2_2);
    }

    private void showNegotiationAttack() {
        dialog.getTextPanel().addPara("\"切断通讯信息已经不起作用了！\"，通讯官员的声音在嘈杂的舰桥中回响，\"整个舰桥系统必须重启！\"");
        dialog.getTextPanel().addPara("随着你下达对系统的物理隔离命令生效前的最后一毫秒，一段极短的图像击穿了所有的防火墙，解析器在系统重置的报错中充满恶意的挺立：你的舰队正从某个不存在的角度被观察。图像下方没有文字，只有一串不断闪烁的距离读数。\n\n");
        dialog.getTextPanel().addPara("\"传感器传来未知读数，他们的位置...就在我们下方，他们在星球上！距离正在急剧缩短！\"随着你的面板上也传来了新的相位回波。很清楚的是：你所谓的“交涉”已经得到了回复。",
                Color.ORANGE);
        dialog.getOptionPanel().clearOptions();
        dialog.getOptionPanel().addOption("脱离轨道", NEGOTIATE_END);
    }

    private void showIntercept() {
        dialog.getTextPanel().addPara("\"长官？\"，雷达官的声音和警报一同响起，\"不论您还想干什么，最好都快点！\"");
        dialog.getTextPanel().addPara("雷达对着星球地表狂躁的扫描着，而带来的回馈更是令人感到不可思议：在和近地轨道相重叠的空间内，有着极为危险的高能反射。\n\n没人知道那是什么东西。\n");

    }

    private void showIntercept_2() {
        dialog.getTextPanel().addPara("\"传感器传来高能未知读数，来源是...对方的整个舰船！速度超过...那不重要了，它们就在我们正下方，并且距离正在急剧缩短！\"很明显，你耽搁太久了。",
                Color.ORANGE);
        dialog.getTextPanel().addPara("\"距离61700...52400...39020...\"",
                Color.RED);
        dialog.getOptionPanel().clearOptions();
        dialog.getOptionPanel().addOption("下令舰队立刻脱离轨道", INTERCEPTOR_2Late);
    }
    private void showIntercept_2Late() {
        dialog.getTextPanel().addPara("随着你的下令，舰队开始转向，并尝试逃离，但传感器上越发急促的遇敌信号，以及那发轰鸣在舰队正前方那碎裂的空间洪流。终究还是证明了这一切都是徒劳。");
        dialog.getTextPanel().addPara("\"做不到！它们正在对我们开火！\"",
                Color.RED);
        dialog.getOptionPanel().clearOptions();
        dialog.getOptionPanel().addOption("\"该死，全舰进入奥列夫零级战斗状态！\"", WAIT);
    }
    private void showIntercept_2Idle() {
        dialog.getTextPanel().addPara("\"无意冒犯，"+Global.getSector().getPlayerPerson().getRank()+"，但请尽快下达命令！\"",
                Color.RED);
        dialog.getTextPanel().addPara("你是愣住了？还是在思考对策？你的船员们不知道，但他们在竭力保持冷静，并等待你的决定。");

        dialog.getOptionPanel().clearOptions();
        dialog.getOptionPanel().addOption("\"...\"", WAIT);
    }
    private void showIntercept_2Wait() {
        dialog.getTextPanel().addPara("双方都是死一般的寂静，没人知道战斗会什么时候开始...");
        dialog.getTextPanel().addPara("你 也 一样 \n",Color.RED);

        dialog.getTextPanel().addPara("*提示：你有微弱的机会可以依靠紧急机动来规避敌人舰队，但请默认你基本不会成功",
                Color.GRAY);
        dialog.getOptionPanel().clearOptions();
    }

    private void recordProbe() {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null) state.getOccupationData(market.getId()).probes++;
        PTSDOccupationAPI.addAttention(market, PTSDOccupationAPI.Action.PROBE, 0.9f, 0.08f);
    }

    private void recordNegotiation() {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null) state.getOccupationData(market.getId()).negotiations++;
        PTSDOccupationAPI.addAttention(market, PTSDOccupationAPI.Action.NEGOTIATION, 0.5f, 0.05f);
    }

    private void spawnDefense(boolean negotiationResponse) {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        String activeId = market.getMemoryWithoutUpdate().getString(PTSDOccupationManager.ACTIVE_DEFENSE_MEMORY);
        SectorEntityToken active = activeId == null ? null : Global.getSector().getEntityById(activeId);
        if (active instanceof CampaignFleetAPI) {
            CampaignFleetAPI fleet = (CampaignFleetAPI) active;
            fleet.clearAssignments();
            fleet.addAssignment(FleetAssignment.ATTACK_LOCATION, player, 3f, "反定位信号源");
            dialog.dismiss();
            return;
        }

        float baseStrength = negotiationResponse ? 140f : 450f;
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null && market.getStarSystem() != null) {
            baseStrength += state.getSystemData(market.getStarSystem().getId()).conversionLevel * 7f;
        }
        float severity = intrusionResolved ? 1.05f : (negotiationResponse ? 0.35f : 0.75f);
        float strength = PTSDOmegaFleetScaling.scale(baseStrength, severity);
        Vector2f spawn = Misc.getPointWithinRadius(market.getPrimaryEntity().getLocation(), 900f);
        FleetParamsV3 params = new FleetParamsV3(spawn,
                IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION, 1f,
                FleetTypes.PATROL_MEDIUM, strength, 0f, 0f, 0f, 0f, 0f, 0f);
        params.maxNumShips = Math.max(5, Global.getSettings().getMaxShipsInFleet() / 2);
        CampaignFleetAPI fleet = PTSD_BaseShard_Util.createFleet(params, strength, PTSD_BaseShard_Util.FleetRole.GUARD_ASSAULT);
        if (fleet == null) {
            dialog.dismiss();
            return;
        }
        market.getContainingLocation().addEntity(fleet);
        fleet.setLocation(spawn.x, spawn.y);
        fleet.setName(negotiationResponse ? "丢包处理" : "防火墙系统");
        fleet.setNoFactionInName(true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, false);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        fleet.addEventListener(new PTSDOccupationDefenseListener(market.getId(), negotiationResponse));
        fleet.addAssignment(FleetAssignment.ATTACK_LOCATION, player, 3f, "反定位信号源");
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                market.getPrimaryEntity(), 3f, "请求重构");
        market.getMemoryWithoutUpdate().set(PTSDOccupationManager.ACTIVE_DEFENSE_MEMORY, fleet.getId());
        PTSDOmegaFleetScaling.record(fleet, baseStrength, strength, severity);
        PTSDOccupationAPI.addAttention(market, PTSDOccupationAPI.Action.DEFENSE_SPAWNED,
                negotiationResponse ? 0.2f : 0.35f, 0f);
        PTSDCrisisDevIntel.report("占领区防御舰队生成",
                (negotiationResponse ? "丢包处理" : "防火墙系统") + "；分支 " +
                        PTSD_BaseShard_Util.getFleetBranchName(fleet),
                market.getStarSystem() == null ? null : market.getStarSystem().getId(), fleet.getId());
        dialog.dismiss();
    }

    private int getHarassmentCost() {
        return 25 + (market == null ? 0 : market.getSize() * 5);
    }

    private int getFullBombardmentCost() {
        return 110 + (market == null ? 0 : market.getSize() * 16);
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {
    }

    @Override
    public void advance(float amount) {
        if (market == null || intrusionResolved) return;
        hostileElapsed += Math.max(0f, amount);
        float elapsed = hostileElapsed;
        if (elapsed >= 10f && elapsed - amount < 10f) {
            showIntercept();
            return;
        }
        if (elapsed >= 14f && elapsed - amount < 14f) dialog.getVisualPanel().fadeVisualOut();

        if (elapsed >= 17f && elapsed - amount < 17f) {
            showIntercept_2();
            return;
        }
        if (elapsed >= 22f && elapsed - amount < 22f) {
            showIntercept_2Idle();
            return;
        }
        if (elapsed >= 30f && !intrusionTextCleared) {
            intrusionTextCleared = true;
            // dialog.getTextPanel().clear();
            dialog.getOptionPanel().clearOptions();
            // 需要一个星球白光闪烁化，然后白光越来越近的效果
            PTSDCrisisDevIntel.report("太晚了...", "文字与选项已被抹除；白光扩张",
                    market == null || market.getStarSystem() == null ? null : market.getStarSystem().getId(),
                    market == null ? null : market.getId());
        }
        if (elapsed >= 33.4f) {
            intrusionResolved = true;
            if (market != null) {
                PTSDCrisisState state = PTSDCrisisState.get();
                if (state != null) state.getOccupationData(market.getId()).lastInteraction = "HOSTILE_UI_INTRUSION";
                PTSDOccupationAPI.addAttention(market, PTSDOccupationAPI.Action.PROBE, 0.65f, 0.06f);
                PTSDCrisisDevIntel.report("太晚了...", "强制断开交互并唤醒防护舰队",
                        market.getStarSystem() == null ? null : market.getStarSystem().getId(), market.getId());
                spawnDefense(false);
            } else {
                dialog.dismiss();
            }
        }
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {
    }

    @Override
    public Object getContext() {
        return context;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return Collections.emptyMap();
    }
}
