package data.scripts.campaign.invasion;

import data.scripts.IIRT_Omega_ModPlugin;
import data.scripts.PTSDModPluginAltLuna;
import lunalib.lunaSettings.LunaSettings;
import java.util.*;

/** All new military-policy knobs are DEV-only, captured by the existing difficulty schema. */
public final class PTSDReliefSettings {
    public static final Map<String,Integer> DEFAULTS=new LinkedHashMap<String,Integer>();
    private static final Map<String,String> LABELS=new LinkedHashMap<String,String>();
    static {
        add("relief_delay",3,"联盟响应延迟日");add("relief_prepare",5,"联盟原地整备日");
        add("relief_muster",10,"联盟集结等待上限日");add("relief_range",10,"联盟附近支援范围光年");
        add("relief_keep",50,"联盟来源最低留守百分比");add("relief_nonmember",75,"联盟援助非成员概率百分比");
        add("relief_garrison",30,"联盟失守转驻日");add("relief_recon",10,"联盟侦察组织间隔日");
        add("relief_near_seconds",60,"联盟战斗近援首批秒");add("relief_far_seconds",120,"联盟战斗远援首批秒");
        add("relief_wave_seconds",45,"联盟战斗批次间隔秒");add("relief_far_chance",50,"联盟远方战斗支援概率百分比");
        add("path_interval",10,"左径战略救援判定间隔日");add("path_chance",50,"左径战略救援概率百分比");
        add("path_region",10,"左径救援区域合并光年");add("path_region_cap",2,"左径区域同时救援队上限");
        add("path_fp_min",60,"左径救援舰队最低FP");add("path_fp_max",180,"左径救援舰队最高FP");
        add("path_raid_interval",10,"左径反攻判定间隔日");add("path_raid_chance",25,"左径反攻概率百分比");
        add("path_raid_cap",2,"左径同时反攻队上限");add("path_raid_min",120,"左径反攻最低FP");add("path_raid_max",240,"左径反攻最高FP");
        add("path_operation_days",15,"左径战略到场行动日上限");add("path_first_seconds",30,"左径战斗首轮抵达秒");
        add("path_wave_seconds",90,"左径战斗后续间隔秒");add("path_ratio_min",40,"左径战斗敌方FP比例下限百分比");
        add("path_ratio_max",60,"左径战斗敌方FP比例上限百分比");add("path_total_min",60,"左径战斗总额度最低FP");
        add("path_total_max",300,"左径战斗总额度最高FP");add("path_wave_fp",60,"左径战斗单轮FP上限");
        add("path_survivor_days",10,"左径战斗幸存者星系驻防日");
        add("relief_replenish_days",30,"联盟战损补员周期日");add("relief_replenish_percent",15,"联盟战损补员上限百分比");
        add("je_rescue_days",10,"Je战区求援期限日");add("je_escort_fp",80,"Je调查护航FP");
        add("je_pursuit_fp",120,"Je调查追兵FP");add("je_missing_chance",30,"Je救援失败失联概率百分比");
    }
    private static void add(String id,int value,String label){DEFAULTS.put("PTSD_"+id,value);LABELS.put("PTSD_"+id,label);}
    public static int value(String key){String id="PTSD_"+key;Integer fallback=DEFAULTS.get(id);if(fallback==null)throw new IllegalArgumentException(key);
        return Math.max(0,PTSDModPluginAltLuna.getLunaInt(IIRT_Omega_ModPlugin.getModId(),id,fallback));}
    public static void register(String mod,Set<String> devIds){for(String id:DEFAULTS.keySet()){
        devIds.add(id);LunaSettings.SettingsCreator.addInt(mod,id,LABELS.get(id),"联盟、左径及Je战争规则；仅开发模式可编辑，随难度预设与自定义快照保存。",DEFAULTS.get(id),0,id.contains("chance")||id.contains("ratio")||id.endsWith("percent")||id.endsWith("keep")||id.endsWith("nonmember")?100:2000,"自定义难度");}}
}
