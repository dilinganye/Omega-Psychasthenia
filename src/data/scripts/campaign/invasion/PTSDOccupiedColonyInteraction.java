package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
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
    private static final String LEAVE = "PTSD_OCC_LEAVE";

    private InteractionDialogAPI dialog;
    private MarketAPI market;
    private PTSDOccupationAPI.InteractionContext context;
    private PTSDOccupationHostileVisual hostileVisual;
    private boolean intrusionTextCleared;
    private boolean intrusionResolved;

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        SectorEntityToken target = dialog.getInteractionTarget();
        this.market = target == null ? null : target.getMarket();
        this.context = PTSDOccupationAPI.createContext(market);
        if (target instanceof PlanetAPI) {
            hostileVisual = new PTSDOccupationHostileVisual((PlanetAPI) target);
            dialog.getVisualPanel().showCustomPanel(810f, 540f, hostileVisual);
        } else if (target != null) {
            dialog.getVisualPanel().showLargePlanet(target);
        }
        showMain();
    }

    private void showMain() {
        TextPanelAPI text = dialog.getTextPanel();
        OptionPanelAPI options = dialog.getOptionPanel();
        text.clear();
        options.clearOptions();
        text.addPara("你正在接近一颗被未知造物控制的星球。在这片星域安全之前，你无法，也不可能在安全的情况下对这里进行任何操作。",
                new Color(230, 170, 150));
        text.addPara("被重写的轨道设施没有回应识别请求。传感器只能确认地表与近轨道之间存在持续的数据交换，以及某种正在等待你先采取行动的防御机制。");

        int harassmentCost = getHarassmentCost();
        options.addOption("进行远距轰炸（消耗 " + harassmentCost + " 燃料）", BOMBARD,
                "不进入近轨道，在防御网边缘投射高能弹药。这会造成有限破坏，并显著提高精神创伤对本星系的注意。");
        setFuelAvailability(BOMBARD, harassmentCost);

        options.addOption("尝试试探", PROBE,
                "以小规模探测与火控照射测试防御反应。预计会立即招致一支防御舰队。");
        options.addOption("尝试交涉", NEGOTIATE,
                "向地表和轨道设施发送多频段通讯请求。没有迹象表明对方愿意交流。");

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
        if (NEGOTIATE_END.equals(optionData)) {
            recordNegotiation();
            spawnDefense(true);
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
        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
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
                negotiationResponse ? "丢包处理" : "防火墙系统",
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
        if (hostileVisual == null || intrusionResolved) return;
        float elapsed = hostileVisual.getElapsed();
        if (elapsed >= 11.2f && !intrusionTextCleared) {
            intrusionTextCleared = true;
            dialog.getTextPanel().clear();
            dialog.getOptionPanel().clearOptions();
            PTSDCrisisDevIntel.report("占领区恶意界面侵蚀", "文字与选项已被抹除；白光正在扩张",
                    market == null || market.getStarSystem() == null ? null : market.getStarSystem().getId(),
                    market == null ? null : market.getId());
        }
        if (elapsed >= 13.4f) {
            intrusionResolved = true;
            if (market != null) {
                PTSDCrisisState state = PTSDCrisisState.get();
                if (state != null) state.getOccupationData(market.getId()).lastInteraction = "HOSTILE_UI_INTRUSION";
                PTSDOccupationAPI.addAttention(market, PTSDOccupationAPI.Action.PROBE, 0.65f, 0.06f);
                PTSDCrisisDevIntel.report("占领区恶意界面侵蚀完成", "强制断开交互并唤醒防护舰队",
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
