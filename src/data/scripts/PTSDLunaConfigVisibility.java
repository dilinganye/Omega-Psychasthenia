package data.scripts;

import com.fs.starfarer.api.Global;
import lunalib.lunaSettings.LunaSettings;

import lunalib.backend.ui.settings.LunaSettingsLoader;
import lunalib.backend.ui.settings.LunaSettingsData;
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
                "PTSDDevHeader", "PTSDDevText", "PTSD_news_true_chance", "PTSD_rescue_interval", "PTSD_rescue_chance", "PTSD_rescue_max_active", "PTSD_rescue_credit_min", "PTSD_rescue_credit_max", "PTSD_rescue_xp_per_fp",
                "PTSD_collapse_hit_chance","PTSD_collapse_cr_min","PTSD_collapse_cr_max","PTSD_collapse_cr_floor","PTSD_collapse_dmod_threshold","PTSD_collapse_dmod_chance",
                "PTSD_start_stage_time", "PTSD_collect_data_time", "PTSD_invade_time", "PTSD_repair_time",
                "PTSD_scout_spawn_radius", "PTSD_scout_max_active", "PTSD_warning_encounter_threshold",
                "PTSD_strategic_update_interval", "PTSD_hidden_materialization_range", "PTSD_expansion_interval",
                "PTSD_max_black_hole_fortresses", "PTSD_front_turn_min_interval", "PTSD_front_turn_max_interval",
                "PTSD_max_guard_fleets", "PTSD_final_invasion_max_strength", "PTSD_DefStat_onNewGame",
                "PTSD_phase_dormant_enabled", "PTSD_phase_recon_enabled", "PTSD_phase_expansion_enabled",
                "PTSD_phase_fortification_enabled", "PTSD_phase_war_enabled",
                "PTSDLateDevHeader", "PTSD_late_production_interval", "PTSD_late_base_income",
                "PTSD_late_outpost_income", "PTSD_late_small_income", "PTSD_late_stable_income",
                "PTSD_late_medium_income", "PTSD_late_eden_income", "PTSD_late_turn_cap",
                "PTSD_late_storage_cap", "PTSD_late_mobilization_threshold",
                "PTSD_late_surprise_threshold", "PTSD_late_surprise_minimum",
                "PTSD_late_fortification_deadline", "PTSD_late_small_work",
                "PTSD_late_stable_work", "PTSD_late_medium_work",
                "PTSD_late_small_transport", "PTSD_late_stable_transport",
                "PTSD_late_medium_transport", "PTSD_late_major_card_cooldown",
                "PTSD_late_short_card_cooldown", "PTSD_late_task_deadline",
                "PTSD_late_strike_min", "PTSD_late_strike_max"
                ,"PTSDReconDevHeader", "PTSD_recon_report_player", "PTSD_recon_report_three",
                "PTSD_recon_report_two", "PTSD_recon_report_interval", "PTSD_recon_upload_min",
                "PTSD_recon_upload_max", "PTSD_recon_remote_error", "PTSD_recon_remote_lifetime",
                "PTSD_recon_low_fleet_fp", "PTSD_recon_empty_reroute_min",
                "PTSD_recon_empty_reroute_max", "PTSD_recon_post_phase_multiplier",
                "PTSD_news_recon_density", "PTSD_news_expansion_density",
                "PTSD_news_fortress_density", "PTSD_news_recon_false",
                "PTSD_news_expansion_false", "PTSD_news_fortress_false"
        };
        for (String id : ids) DEV_IDS.add(id);
    }

    private PTSDLunaConfigVisibility() { }

    public static void sync() {
        if (!IIRT_Omega_ModPlugin.hasLunaLib || Global.getSettings() == null || syncing) return;
        boolean dev = Global.getSettings().isDevMode();
        String modId = IIRT_Omega_ModPlugin.getModId();
        syncing = true;
        try {
            if (lastDevMode == null || lastDevMode.booleanValue() != dev) {
                data.scripts.campaign.invasion.PTSDPriorityTargetList.addSettings();
                addDevSettings(modId);
                PTSDDifficultySettings.register(modId);
                PTSDDifficultySettings.registerCurves(modId);
                PTSDDifficultySettings.capture(modId);
                PTSDDifficultySettings.regroup(modId);
                // Materialize hidden defaults before hiding metadata; presets still own these values.
                LunaSettings.SettingsCreator.refresh(modId);
                if (!dev) {removeSettings(modId, DEV_IDS);PTSDDifficultySettings.hideCurves(modId);}
                IIRT_Omega_ModPlugin.reloadLunaSettingsAfterVisibilityChange();
                lastDevMode = dev;
            }
        } catch (Throwable ex) {
            // Do not retry a broken integration on every campaign frame.
            lastDevMode = dev;
            Global.getLogger(PTSDLunaConfigVisibility.class).warn(
                    "Unable to synchronize Psychasthenia DevMode Luna settings visibility", ex);
        } finally {
            syncing = false;
        }
    }

    private static void addDevSettings(String modId) {
        data.scripts.campaign.invasion.PTSDReliefSettings.register(modId,DEV_IDS);
        addInt(modId,"PTSD_news_true_chance","普通调查属实概率","百分比；仅影响之后接取的普通调查。固定剧情及条件事件保持自己的真假规则。",90,0,100,"自定义难度");
        addInt(modId,"PTSD_rescue_interval","战时求救判定间隔日","仅玩家在超空间时判定。",10,1,120,"自定义难度");
        addInt(modId,"PTSD_rescue_chance","战时求救触发概率","每个窗口最多判定一次。",50,0,100,"自定义难度");
        addInt(modId,"PTSD_rescue_max_active","战时求救同时事件上限","限制未结束事件。",3,0,10,"自定义难度");
        addInt(modId,"PTSD_rescue_credit_min","战时求救谢礼下限","每起一次。",50000,0,1000000,"自定义难度");
        addInt(modId,"PTSD_rescue_credit_max","战时求救谢礼上限","每起一次。",150000,0,2000000,"自定义难度");
        addInt(modId,"PTSD_rescue_xp_per_fp","战时求救每存活FP经验","脱险兵力乘此值。",250,0,2000,"自定义难度");
        addInt(modId,"PTSD_collapse_hit_chance","破裂波面影响舰船概率％","每个实际扫过的波面逐舰判定；开始演出时固定参数。",33,0,100,"自定义难度");
        addInt(modId,"PTSD_collapse_cr_min","波面CR损失下限百分点","不产生直接舰体伤害。",5,0,50,"自定义难度");
        addInt(modId,"PTSD_collapse_cr_max","波面CR损失上限百分点","不得低于损失下限。",10,0,50,"自定义难度");
        addInt(modId,"PTSD_collapse_cr_floor","波面CR保护底线％","低于此值的舰船不会被事件进一步降CR，也不凭空修复。",10,0,100,"自定义难度");
        addInt(modId,"PTSD_collapse_dmod_threshold","波面D插风险CR阈值％","冲击前CR低于此值才可能新增D插。",30,0,100,"自定义难度");
        addInt(modId,"PTSD_collapse_dmod_chance","低CR波面D插概率％","每舰整场最多新增一个；战斗内记录、战后结算。",15,0,100,"自定义难度");
        LunaSettings.SettingsCreator.addHeader(modId, "PTSDDevHeader", "开发者阶段与内部参数", "开发");
        LunaSettings.SettingsCreator.addText(modId, "PTSDDevText",
                "这些设置仅在 DevMode 中显示。阶段开关只影响 DevMode 测试；关闭 DevMode 后所有阶段恢复启用。", "开发");

        addInt(modId, "PTSD_start_stage_time", "沉寂阶段时长（天）", "暗流持续到公开侦察开始前的时间。", 150, 1, 365, "阶段");
        //addInt(modId, "PTSD_collect_data_time", "侦察阶段时长（天）", "包含常规观察与后半段火力侦察。", 730, 1, 1000, "阶段");
        addInt(modId, "PTSD_collect_data_time", "侦察基础时长（实际×2）", "包含常规观察与后半段火力侦察；实际阶段时长为此数值两倍，兼容既有设置。", 300, 150, 3600, "阶段");
        addInt(modId, "PTSD_invade_time", "营建最短时长（天）", "营建结束后进入要塞化；战争准备点需由实际设施产出。", 365, 30, 1825, "阶段");
        addInt(modId, "PTSD_repair_time", "要塞阶段参考时长（天）", "实际离场由准备点、情报及最迟期限共同决定。", 200, 30, 1825, "阶段");

        addInt(modId, "PTSD_scout_spawn_radius", "侦察目标附近半径", "窥视的尺度。", 300, 100, 3000, "侦察");
        addInt(modId, "PTSD_scout_max_active", "同时存在侦察队上限", "仅限制实体舰队，不限制远程事件记录。", 12, 3, 24, "侦察");
        addInt(modId, "PTSD_warning_encounter_threshold", "提前预警接触次数", "玩家接触达到该次数后显示危机预警。", 4, 1, 20, "侦察");
        LunaSettings.SettingsCreator.addHeader(modId, "PTSDReconDevHeader", "侦察上传与条件新闻", "侦察调试");
        addInt(modId, "PTSD_recon_report_player", "玩家线索附报概率％", "仅在已有有效上传的玩家踪迹时附报；超额概率会按顺序截断。", 20, 0, 100, "侦察调试");
        addInt(modId, "PTSD_recon_report_three", "周边三星系附报概率％", "不含本地；四分支概率合计最多100％。", 25, 0, 100, "侦察调试");
        addInt(modId, "PTSD_recon_report_two", "周边双星系附报概率％", "未分配概率仅汇报本地。", 40, 0, 100, "侦察调试");
        addInt(modId, "PTSD_recon_report_interval", "批量报告间隔（天）", "抵达立即报告，之后按此间隔再次报告。", 5, 1, 20, "侦察调试");
        addInt(modId, "PTSD_recon_upload_min", "上传最短窗口（天）", "上传前截击会丢失舰载样本。", 1, 1, 10, "侦察调试");
        addInt(modId, "PTSD_recon_upload_max", "上传最长窗口（天）", "不得短于最短窗口。", 3, 1, 15, "侦察调试");
        addInt(modId, "PTSD_recon_remote_error", "远距估测误差％", "殖民地存在误判率与舰队强度噪声。", 25, 0, 60, "侦察调试");
        addInt(modId, "PTSD_recon_remote_lifetime", "远距报告有效期（天）", "到期后低活动地区重新纳入候选。", 45, 5, 180, "侦察调试");
        addInt(modId, "PTSD_recon_low_fleet_fp", "低活动舰队估值上限", "无殖民地且远距舰队估值不高于此值时暂缓实地复查。", 30, 0, 150, "侦察调试");
        addInt(modId, "PTSD_recon_empty_reroute_min", "无人改道最短等待（天）", "实地确认无活跃殖民地后开始计时。", 2, 1, 15, "侦察调试");
        addInt(modId, "PTSD_recon_empty_reroute_max", "无人改道最长等待（天）", "不得短于最短等待。", 4, 1, 20, "侦察调试");
        addInt(modId, "PTSD_recon_post_phase_multiplier", "营建后侦察间隔倍率", "仍有战略盲点时才继续补查。", 3, 1, 10, "侦察调试");
        addInt(modId, "PTSD_news_recon_density", "侦察条件新闻密度％", "调整下一次事件抽取间隔。", 100, 10, 300, "侦察调试");
        addInt(modId, "PTSD_news_expansion_density", "营建条件新闻密度％", "调整下一次事件抽取间隔。", 100, 10, 300, "侦察调试");
        addInt(modId, "PTSD_news_fortress_density", "要塞条件新闻密度％", "调整下一次事件抽取间隔。", 100, 10, 300, "侦察调试");
        addInt(modId, "PTSD_news_recon_false", "侦察误报率％", "仅作用于后续新条件事件。", 30, 0, 60, "侦察调试");
        addInt(modId, "PTSD_news_expansion_false", "营建误报率％", "仅作用于后续新条件事件。", 18, 0, 50, "侦察调试");
        addInt(modId, "PTSD_news_fortress_false", "要塞误报率％", "仅作用于后续新条件事件。", 8, 0, 35, "侦察调试");

        addInt(modId, "PTSD_strategic_update_interval", "权重更新间隔（天）", "重新估算各星系权重的周期。", 7, 1, 60, "战略");
        addInt(modId, "PTSD_hidden_materialization_range", "事件实体化距离", "玩家接近后才投影舰队和战场。", 8000, 1000, 20000, "战略");
        addInt(modId, "PTSD_expansion_interval", "行星异化间隔（天）", "向同星系其他行星扩张的平均周期。", 30, 10, 90, "战略");
        addInt(modId, "PTSD_max_black_hole_fortresses", "黑洞要塞上限", "允许同时存在的黑洞要塞数量。", 3, 0, 10, "战略");
        addInt(modId, "PTSD_front_turn_min_interval", "战线回合最短间隔", "双方战略部署的最短间隔。", 6, 2, 60, "战争");
        addInt(modId, "PTSD_front_turn_max_interval", "战线回合最长间隔", "双方战略部署的最长间隔。", 12, 2, 90, "战争");
        addInt(modId, "PTSD_max_guard_fleets", "最大卫戍事件数", "实控区可并行维护的卫戍部署。", 16, 0, 30, "战争");
        addInt(modId, "PTSD_final_invasion_max_strength", "单次入侵基础强度上限", "倍率、Flat 与事件严重度应用前的上限。", 200, 50, 5000, "战争");
        LunaSettings.SettingsCreator.addHeader(modId, "PTSDLateDevHeader", "后期危机精确账本", "后期调试");
        addInt(modId, "PTSD_late_production_interval", "生产周期（天）", "下一次结算后采用；不补发历史生产。", 10, 2, 30, "后期调试");
        addInt(modId, "PTSD_late_base_income", "母星每期产出", "有效母星生产源。", 4, 1, 20, "后期调试");
        addInt(modId, "PTSD_late_outpost_income", "据点每期产出", "有控制权且已完成改造的据点。", 2, 0, 10, "后期调试");
        addInt(modId, "PTSD_late_small_income", "Small每期产出", "运输受阻期间减半。", 1, 0, 10, "后期调试");
        addInt(modId, "PTSD_late_stable_income", "Stable每期产出", "运输受阻期间减半。", 2, 0, 10, "后期调试");
        addInt(modId, "PTSD_late_medium_income", "Medium每期产出", "运输受阻期间减半。", 3, 0, 10, "后期调试");
        addInt(modId, "PTSD_late_eden_income", "EDEN每期产出", "完整要塞有效产出。", 5, 0, 15, "后期调试");
        addInt(modId, "PTSD_late_turn_cap", "单期产出上限", "所有设施合计后生效。", 20, 1, 100, "后期调试");
        addInt(modId, "PTSD_late_storage_cap", "准备点储存上限", "不得低于单期产出上限。", 200, 45, 500, "后期调试");
        addInt(modId, "PTSD_late_mobilization_threshold", "常规动员门槛", "需同时满足真实与估计准备点及航路情报。", 60, 15, 200, "后期调试");
        addInt(modId, "PTSD_late_surprise_threshold", "直接突袭估计门槛", "仍需至少两个进攻来源与真实资源下限。", 120, 30, 300, "后期调试");
        addInt(modId, "PTSD_late_surprise_minimum", "误判突袭真实下限", "估计值再高也不得在真实资源不足时突袭。", 45, 10, 150, "后期调试");
        addInt(modId, "PTSD_late_fortification_deadline", "要塞基础期限（实际×2）", "实际最迟期限为此数值两倍；达到期限会带着实际资源缺口进入动员。", 180, 30, 600, "后期调试");
        addInt(modId, "PTSD_late_small_work", "Small工程需求", "仅作用于之后的运输入站结算。", 20, 5, 100, "后期调试");
        addInt(modId, "PTSD_late_stable_work", "Stable工程需求", "仅作用于之后的运输入站结算。", 30, 5, 150, "后期调试");
        addInt(modId, "PTSD_late_medium_work", "Medium工程需求", "仅作用于之后的运输入站结算。", 40, 5, 200, "后期调试");
        addInt(modId, "PTSD_late_small_transport", "Small运输间隔", "下一次发船起生效。", 8, 2, 30, "后期调试");
        addInt(modId, "PTSD_late_stable_transport", "Stable运输间隔", "下一次发船起生效。", 7, 2, 30, "后期调试");
        addInt(modId, "PTSD_late_medium_transport", "Medium运输间隔", "下一次发船起生效。", 5, 2, 30, "后期调试");
        addInt(modId, "PTSD_late_major_card_cooldown", "核心事件抽取间隔", "下一张核心事件起生效。", 5, 1, 30, "后期调试");
        addInt(modId, "PTSD_late_short_card_cooldown", "短卡抽取间隔", "下一张短卡起生效。", 3, 1, 30, "后期调试");
        addInt(modId, "PTSD_late_task_deadline", "现场任务期限", "接受新任务时写入，不改变已有任务。", 12, 3, 40, "后期调试");
        addInt(modId, "PTSD_late_strike_min", "战区炮击最短间隔", "下一轮炮击安排起生效。", 3, 1, 15, "后期调试");
        addInt(modId, "PTSD_late_strike_max", "战区炮击最长间隔", "下一轮炮击安排起生效，不得短于最短间隔。", 5, 1, 20, "后期调试");
        if (false) {
            LunaSettings.SettingsCreator.addRadio(modId, "PTSD_DefStat_onNewGame", "新开局初始阶段",
                    "Sar=暗流；Cod=侦察；Inv=营建；Rep=要塞；FuA=战争；End=结束。",
                    "Sar", "Sar,Cod,Inv,Rep,FuA,End", "阶段开关");
            addBool(modId, "PTSD_phase_dormant_enabled", "启用：暗流阶段", "关闭后测试存档会跳过暗流。", true);
            addBool(modId, "PTSD_phase_recon_enabled", "启用：侦察/火力侦察阶段", "关闭后测试存档会跳过侦察。", true);
            addBool(modId, "PTSD_phase_expansion_enabled", "启用：沉寂营建阶段", "关闭后测试存档会跳过前哨营建。", false);
            addBool(modId, "PTSD_phase_fortification_enabled", "启用：封锁与要塞阶段", "关闭后测试存档会跳过封锁建设。", false);
            addBool(modId, "PTSD_phase_war_enabled", "启用：全面战争阶段", "关闭后测试存档会在战争前结束。", false);
        }
    }

    private static void addInt(String modId, String id, String name, String text,
                               int value, int min, int max, String tab) {
        LunaSettings.SettingsCreator.addInt(modId, id, name, text, value, min, max, tab);
    }

    private static void addBool(String modId, String id, String name, String text, boolean value) {
        LunaSettings.SettingsCreator.addBoolean(modId, id, name, text, value, "阶段开关");
    }

    private static void removeSettings(String modId, Set<String> ids) {
        Iterator<LunaSettingsData> iterator = LunaSettingsLoader.getSettingsData().iterator();
        while (iterator.hasNext()) {
            LunaSettingsData data = iterator.next();
            if (modId.equals(data.getModID()) && ids.contains(data.getFieldID())) iterator.remove();
        }
    }
}
