package data.scripts.campaign.invasion.news;

/**
 * 新闻 CSV 的 eventLoc 扩展入口。填写实现类全名；需要 public 无参构造器。
 * 游戏通过 SettingsAPI.getInstanceOfScript 创建实例，DLC 无需修改本体注册表。
 *
 * 实例只保存不变配置，不进入存档，也不可保存某一次任务的抽签或计时。
 * 每次任务的数据放在 Context.data()，舰队、地点只保存 ID；重入不得重新抽签。
 * configure 仅加载表格时调用，禁止在这里生成实体或修改战役状态。
 *
 * 可同时实现 PTSDCrisisNewsAPI.CustomNewsHandler（精确选址）或
 * PTSDNewsSiteAPI.SiteHandler（独立现场）；适配器会自动连接这些接口。
 * 不实现它们时，继续使用通用 target 和 Definition.templates 对应的现场机制。
 */
public interface NewsEvent {
    void configure(NewsEventDefinition definition);

    /** 返回 null 表示可选；返回原因表示正常等待。不得在候选筛选时修改世界。 */
    default String unavailableForNews(data.scripts.campaign.invasion.PTSDCrisisState state) { return null; }

    /**
     * 仅在已接取的调查目标/绑定实体上调用。返回 null 保留正常交互；非 null 优先显示。
     * 对话中应提供正常设施功能或退出入口。每次进入创建新的对话实例。
     * 此工厂可能用于共享设施的任务列表：禁止在工厂中发奖/攻击；实际进入行为放在 init。
     */
    default com.fs.starfarer.api.campaign.InteractionDialogPlugin createDialog(
            NewsEventContext context, com.fs.starfarer.api.campaign.SectorEntityToken target) { return null; }

    /** 新闻已建立；尚未接取。这里只适合设置持久状态，不应泄露真假分支。 */
    default void onPublished(NewsEventContext context) { }

    /** 第一次接取时调用一次，早于现场创建与交互；真假分支已写入 incident。 */
    default void onAccepted(NewsEventContext context) { }

    /** 随危机后台周期运行，不是逐帧回调；自行用游戏日控制较慢任务。 */
    default void advanceIncident(NewsEventContext context) { }

    /** 完整调查结束、放弃或到期；不要在此再次发放标准调查报告。 */
    default void onFinished(NewsEventContext context) { }

    /** DEV只读摘要。不得造舰或改变预算；null 表示没有额外状态。 */
    default String devStatus(NewsEventContext context) { return null; }
    /** DEV实际舰队清单，返回现存实体ID。不能为预览创建舰队。 */
    default java.util.List<String> devFleetIds(NewsEventContext context) { return java.util.Collections.emptyList(); }
    /** DEV快捷操作的持久键与按钮文字；只声明，不执行。普通玩家不显示。 */
    default java.util.Map<String,String> devActions(NewsEventContext context) { return java.util.Collections.emptyMap(); }
    /** 显式点击DEV按钮才调用。须再次检查前置；返回结果说明，不冒充玩家实际完成调查。 */
    default String runDevAction(NewsEventContext context,String action) { return "此事件没有该调试操作。"; }
}
