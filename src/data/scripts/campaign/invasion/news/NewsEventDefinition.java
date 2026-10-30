package data.scripts.campaign.invasion.news;

import org.json.JSONObject;

/**
 * 不再占用 CSV 列的事件专属配置。字段名保留原内部语义，方便核对迁移。
 * 默认不施加数值效果；设置模板不会跳过选址前置条件。
 * 本对象每条新闻独立创建，不能存入战役存档。
 */
public final class NewsEventDefinition {
    /** 原侦察效果配置；新闻不因此直接向敌方提供观测样本。 */
    public float recon;
    /** 核实属实后影响人类认知的基数，沿用框架的核实倍率。 */
    public float awareness;
    /** 原敌方进攻倾向配置，仍受当前阶段与结算入口约束。 */
    public float aggression;
    /** 发布时对报道附近殖民地施加的恐慌基数，证伪后可撤回。 */
    public float panic;
    /** 现实扭曲效果基数，非舰队实际战力。 */
    public float distortion;
    /** 0～1：旧附属物理行动抽签概率；与90%新闻真实性独立。 */
    public float physicalChance;
    /** 旧附属行动的名义自动分；普通独立现场可保持0并自行配置舰队。 */
    public float strength;
    /** 已注册模板名，多个用 | 分隔；有 SiteHandler 时现场由处理器负责。 */
    public String templates = "AUTO";
    /** TRUE/FALSE/AUTO：是否叠加旧武力现场；独立事件建议FALSE。 */
    public String martialSite = "AUTO";
    /** 可选图片资源路径，空串使用现有默认图标。 */
    public String icon = "";
    /** CONDITIONAL 专用，必须是条件导演已支持的触发类型；任意新前置用 unavailableForNews/pick。 */
    public String trigger = "";
    /** 条件行动按钮文字。 */
    public String action = "";
    /** 条件调查远程/超时结束文字；不应包含玩家尚未发现的敌方内部决策。 */
    public String remoteOutcome = "";
    /** 条件事件后续响应描述，展示权限仍由原流程控制。 */
    public String omegaResponse = "";
    /** 条件导演已有行动效果名；全新效果写入独立处理器，不要求扩展本体枚举。 */
    public String effect = "";
    /** 既有 SCRIPTED 卡片选项；独立多层对话使用 createDialog。 */
    public String choice1 = "", choice2 = "";
    /** PUBLIC / INTEL / NONE / DEV_ONLY：既有显示权限。 */
    public String visibility = "PUBLIC";
    /** 当前阶段经过的最少游戏日（既有条件/脚本投放入口使用）。 */
    public float minDay;
    /** 新闻优先级，默认1，2及以上允许更积极推送；不是抽签权重。 */
    public float priority = 1f;
    /** true：靠近目标不自动完成；扩展事件必须调用 Context.finish，超时仍由框架处理。 */
    public boolean manualCompletion;

    /** 发布/核实对应的既有危机效果；这些值不等于舰队编成的实际 FP。 */
    public void effects(float recon, float awareness, float aggression, float panic, float distortion) {
        this.recon=recon; this.awareness=awareness; this.aggression=aggression;
        this.panic=panic; this.distortion=distortion;
    }

    /** 仅供本体加载器桥接旧 Card 结构；扩展类直接填写上方有注释的字段。 */
    public void applyTo(JSONObject row) throws org.json.JSONException {
        float[] numbers={recon,awareness,aggression,panic,distortion,physicalChance,strength,minDay,priority};
        for(float value:numbers) if(Float.isNaN(value)||Float.isInfinite(value))
            throw new IllegalArgumentException("事件配置含非有限数值");
        if(physicalChance<0f||physicalChance>1f||strength<0f||minDay<0f||priority<0f)
            throw new IllegalArgumentException("事件概率/强度/最早时间/优先级越界");
        row.put("recon",recon);row.put("awareness",awareness);row.put("aggression",aggression);
        row.put("panic",panic);row.put("distortion",distortion);
        row.put("physicalChance",physicalChance);row.put("strength",strength);
        row.put("siteTemplates",templates);row.put("martialSite",martialSite);row.put("icon",icon);
        row.put("trigger",trigger);row.put("action",action);row.put("remoteOutcome",remoteOutcome);
        row.put("omegaResponse",omegaResponse);row.put("effect",effect);
        row.put("choice1",choice1);row.put("choice2",choice2);row.put("visibility",visibility);
        row.put("minDay",minDay);row.put("priority",priority);
        row.put("eventManualCompletion",manualCompletion);
    }
}
