package data.scripts;

import com.fs.starfarer.api.Global;
import lunalib.lunaSettings.LunaSettings;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Builds advanced settings only while DevMode is active and removes them when it is closed. */
public final class PTSDLunaConfigVisibility {
    private static final Set<String> DEV_IDS = new HashSet<String>();
    private static Boolean lastDevMode;
    private static boolean syncing;

    static {
        String[] ids = {
                "PTSDDevHeader", "PTSDDevText",
                "PTSD_start_stage_time", "PTSD_collect_data_time", "PTSD_invade_time", "PTSD_repair_time",
                "PTSD_scout_spawn_radius", "PTSD_scout_max_active", "PTSD_warning_encounter_threshold",
                "PTSD_strategic_update_interval", "PTSD_hidden_materialization_range", "PTSD_expansion_interval",
                "PTSD_max_black_hole_fortresses", "PTSD_front_turn_min_interval", "PTSD_front_turn_max_interval",
                "PTSD_max_guard_fleets", "PTSD_final_invasion_max_strength", "PTSD_DefStat_onNewGame",
                "PTSD_phase_dormant_enabled", "PTSD_phase_recon_enabled", "PTSD_phase_expansion_enabled",
                "PTSD_phase_fortification_enabled", "PTSD_phase_war_enabled"
        };
        for (String id : ids) DEV_IDS.add(id);
    }

    private PTSDLunaConfigVisibility() { }

    public static void sync() {
        if (!IIRT_Omega_ModPlugin.hasLunaLib || Global.getSettings() == null || syncing) return;
        boolean dev = Global.getSettings().isDevMode();
        if (lastDevMode != null && lastDevMode.booleanValue() == dev) return;
        String modId = IIRT_Omega_ModPlugin.getModId();
        syncing = true;
        try {
            if (dev) addDevSettings(modId);
            else removeDevSettings(modId);
            LunaSettings.SettingsCreator.refresh(modId);
            IIRT_Omega_ModPlugin.reloadLunaSettingsAfterVisibilityChange();
            lastDevMode = dev;
        } catch (Throwable ex) {
            Global.getLogger(PTSDLunaConfigVisibility.class).warn(
                    "Unable to synchronize Psychasthenia DevMode Luna settings visibility", ex);
        } finally {
            syncing = false;
        }
    }

    private static void addDevSettings(String modId) {
        LunaSettings.SettingsCreator.addHeader(modId, "PTSDDevHeader", "开发者阶段与内部参数", "开发");
        LunaSettings.SettingsCreator.addText(modId, "PTSDDevText",
                "这些设置仅在 DevMode 中显示。阶段开关只影响 DevMode 测试；关闭 DevMode 后所有阶段恢复启用。", "开发");

        addInt(modId, "PTSD_start_stage_time", "沉寂阶段时长（天）", "暗流持续到公开侦察开始前的时间。", 65, 1, 365, "阶段");
        addInt(modId, "PTSD_collect_data_time", "侦察阶段时长（天）", "包含常规观察与后半段火力侦察。", 60, 1, 730, "阶段");
        addInt(modId, "PTSD_invade_time", "据点营建时长（天）", "前哨母星的初始建设时间。", 30, 1, 365, "阶段");
        addInt(modId, "PTSD_repair_time", "要塞建设时长（天）", "全面进攻阈值前的封锁建设时间。", 30, 1, 365, "阶段");

        addInt(modId, "PTSD_scout_spawn_radius", "侦察目标附近半径", "侦察任务在目标附近的基础活动尺度。", 300, 100, 3000, "侦察");
        addInt(modId, "PTSD_scout_max_active", "同时存在侦察队上限", "仅限制实体舰队，不限制远程事件记录。", 3, 1, 12, "侦察");
        addInt(modId, "PTSD_warning_encounter_threshold", "提前预警接触次数", "玩家接触达到该次数后显示危机预警。", 4, 1, 20, "侦察");

        addInt(modId, "PTSD_strategic_update_interval", "权重更新间隔（天）", "重新估算各星系权重的周期。", 7, 1, 60, "战略");
        addInt(modId, "PTSD_hidden_materialization_range", "事件实体化距离", "玩家接近后才投影舰队和战场。", 5000, 1000, 20000, "战略");
        addInt(modId, "PTSD_expansion_interval", "行星异化间隔（天）", "向同星系其他行星扩张的平均周期。", 14, 3, 90, "战略");
        addInt(modId, "PTSD_max_black_hole_fortresses", "黑洞要塞上限", "允许同时存在的黑洞要塞数量。", 3, 0, 10, "战略");
        addInt(modId, "PTSD_front_turn_min_interval", "战线回合最短间隔", "双方战略部署的最短间隔。", 6, 2, 60, "战争");
        addInt(modId, "PTSD_front_turn_max_interval", "战线回合最长间隔", "双方战略部署的最长间隔。", 12, 2, 90, "战争");
        addInt(modId, "PTSD_max_guard_fleets", "最大卫戍事件数", "实控区可并行维护的卫戍部署。", 10, 0, 30, "战争");
        addInt(modId, "PTSD_final_invasion_max_strength", "单次入侵基础强度上限", "倍率、Flat 与事件严重度应用前的上限。", 200, 50, 5000, "战争");

        LunaSettings.SettingsCreator.addRadio(modId, "PTSD_DefStat_onNewGame", "新开局初始阶段",
                "Sar=暗流；Cod=侦察；Inv=营建；Rep=要塞；FuA=战争；End=结束。",
                "Sar", "Sar,Cod,Inv,Rep,FuA,End", "阶段开关");
        addBool(modId, "PTSD_phase_dormant_enabled", "启用：暗流阶段", "关闭后测试存档会跳过暗流。", true);
        addBool(modId, "PTSD_phase_recon_enabled", "启用：侦察/火力侦察阶段", "关闭后测试存档会跳过侦察。", true);
        addBool(modId, "PTSD_phase_expansion_enabled", "启用：沉寂营建阶段", "关闭后测试存档会跳过前哨营建。", true);
        addBool(modId, "PTSD_phase_fortification_enabled", "启用：封锁与要塞阶段", "关闭后测试存档会跳过封锁建设。", true);
        addBool(modId, "PTSD_phase_war_enabled", "启用：全面战争阶段", "关闭后测试存档会在战争前结束。", true);
    }

    private static void addInt(String modId, String id, String name, String text,
                               int value, int min, int max, String tab) {
        LunaSettings.SettingsCreator.addInt(modId, id, name, text, value, min, max, tab);
    }

    private static void addBool(String modId, String id, String name, String text, boolean value) {
        LunaSettings.SettingsCreator.addBoolean(modId, id, name, text, value, "阶段开关");
    }

    @SuppressWarnings("unchecked")
    private static void removeDevSettings(String modId) throws Exception {
        Class<?> loader = Class.forName("lunalib.backend.ui.settings.LunaSettingsLoader");
        Method getter = loader.getMethod("getSettingsData");
        Object raw = getter.invoke(null);
        if (!(raw instanceof List)) return;
        Iterator<Object> iterator = ((List<Object>) raw).iterator();
        while (iterator.hasNext()) {
            Object data = iterator.next();
            Method getModId = data.getClass().getMethod("getModID");
            Method getFieldId = data.getClass().getMethod("getFieldID");
            Object dataMod = getModId.invoke(data);
            Object field = getFieldId.invoke(data);
            if (modId.equals(dataMod) && field instanceof String && DEV_IDS.contains(field)) iterator.remove();
        }
    }
}
