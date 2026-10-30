# 调查报告与工程装配台

2026-10-28：源码、数据和离线验证完成；未替换游戏加载 JAR，界面与实机存取货待验收。

## 已确认规则

| 情形 | 调查报告 |
| --- | --- |
| 玩家完整确认或有证据证伪一条证据链 | 3份 |
| 已取得至少一项现场证据，主动放弃或提前结束为不完整结论 | 1份 |
| Je代查完成并交接 | 1份 |
| 刚接取就放弃、纯过期、目标失效、单纯跟踪信号 | 不给 |

每条调查实例只结算一次，不按新闻ID全局锁死。基本取证完成但条件后续行动仍未完成的，等待整条链结束；此时放弃且已有证据可给1份。没有历史补发或旧档迁移。部分报告不会被记为成功调查，也不自动提升原调查判定。

报告ID：`PTSD_investigation_report`，暂用原版 `graphics/icons/cargo/survey_data.png`。证据包含已确认现场、采样、现场记录、港务资料、救出船只，以及已实际记录的侦察/封锁现象；仅生成现场不算证据。

## 右键与首次事件

`PTSD_core_fragment` 和调查报告共用货舱右键入口。首次进入独立的工程主管/首席科学家三段交谈，最后选择批准后才持久解锁；中途退出不扣材料、不跳过未读剧情。已有FT-09等事件链保持独立。

后续右键直接进入装配台。显示五类材料库存与单批价格，武器使用原版帷幕物品同类的 `showCustomProductionPicker`。列表中一个选择单位是一批，每次最多100批；选择后另页显示实际产量和材料总价。此批数上限不是材料预算。确认时重新检查真实货舱来源，通过后同步扣货并交付成品；取消、缺货和重复确认不扣货。不假装多个材料可以折算为同一种通货。

普通hullmod兑换为原版 `modspec` 可学习插件，不直接永久解锁；可携带、出售或右键学习。配方数量同样适用。当前没有随意加入尚未指定的hullmod配方。

## 专用配方CSV

文件：`data/config/ptsdCSV/PTSD_workshop_recipes.csv`。

| 列 | 含义 |
| --- | --- |
| itemId | 武器ID；兑换武器时填写 |
| itemQuantity | 单批成品数量，hullmod同样使用此列 |
| hullmodId | hullmod ID；与itemId二选一 |
| protocolFragments | 创伤协议残片需求 |
| investigationReports | 调查报告需求 |
| supplies | 补给需求 |
| heavyMachinery | 重型装备需求，原版 `heavy_machinery` |
| heavyArmaments | 重型武器需求，原版 `hand_weapons` |

启用首个配方：`PTSD_Photonspreador ×1`，残片1、报告2、补给50、重型装备10、重型武器0。

必须保留8列表头。数量必须为正整数；价格为非负整数，至少一种材料非零。空输出、双输出、未知武器/hullmod、重复输出、负数/小数和不匹配的组件接入会记录明确错误并禁用该行，不提供免费替代品。`itemId`以`#`开头可保留禁用样例。数量上限10000、单种单批费用上限1000000。文件在打开装配台时读取，不增加每帧扫描。

## 按船占用的特殊改装组件

用户确认：安装占用1件，拆下返还；保存方案/预览不扣货；不足时禁止安装。

采用原版 `HullModItemManager` / `HullModEffect.getRequiredItem()`，无需自建高频监听器。后续新hullmod的效果类须继承 `data.scripts.campaign.cargo.PTSDItemBoundHullMod`，并添加标签 `ptsd_item_bound`。继承类照常实现自身改装效果。**只给任意已有hullmod加标签不足以接入原版接口**，CSV会拒绝这种错误配置。

这类配方交付 `PTSD_hullmod_module`，其SpecialItemData.data保存具体hullmod ID，各改装的组件不会混为一堆。右键登记该改装设计，不消耗组件；实际安装由原版占用一件，拆卸由原版返还。普通插件与组件路线分开，避免学习时先消耗掉安装所需的唯一组件。

尚未指定实际组件型hullmod，故本轮提供可复用系统与验证，不给现有武器或hullmod擅自加标签。原版还会读取停靠地点可用仓储；组件物品不足时遵从其安装限制。

## 实现入口

- `src/data/scripts/campaign/invasion/PTSDInvestigationRewards.java`：奖励规则与每实例结算；CrisisIncident.investigationReportsAwarded持久去重。
- `PTSDNewsScene.finishAs`、`PTSDCrisisIncidentManager.advanceInvestigations`、`PTSDConditionalNewsDirector.completePlayerOperation`：完整/部分调查结算。
- `PTSDCrisisIntel.abandon`：有现场证据的主动放弃。
- `PTSDJeOtloesManager.completeAgentReport`：Je交接；允许未亲自接取的私下调查得到1份。
- `news/FugitiveShuttleNewsHandler.resolveIncident`：特殊现场接触路径，补全确认判定并调用奖励。
- `src/data/scripts/campaign/cargo/PTSDCoreFragmentItemPlugin.java`：两种材料的右键入口。
- `PTSDEngineeringDialog.java`：首次事件、原版武器选择与确认页、分页插件选择。
- `PTSDWorkshopRecipes.java`：CSV校验、总价核对与真实货舱交换。
- `PTSDItemBoundHullMod.java` / `PTSDHullmodModulePlugin.java`：按船占用与设计登记。
- `data/campaign/special_items.csv`：调查报告和装配组件注册。

## 验证与实机步骤

Java8临时编译涵盖危机全目录、cargo目录、其源代码依赖和SOL61测试。临时产物：`C:/Users/12862/AppData/Local/Temp/ptsd-workshop-4202e45bdbb9452fbd345085abdff046`。

新增 `EngineeringWorkshopTest` 覆盖数据校验、总价、缺货、确认时库存变化、实际原版选择回调、取消、重复确认、奖励去重、Je私下交接、过期不奖、首次剧情解锁以及空data图鉴组件。

新增 `ItemBoundHullmodTest` 调用真实原版管理器，验证预览不扣、保存占用、重复保存不重扣、另一艘船缺货禁装、拆下返还、重复拆卸不复制。其余相关回归包括 InvestigationNavigationTest、NewsSceneMechanicsTest、BlackoutSearchTest、ReconWorldSceneTest、TaskRulesTest。

实机仍需：

1. 编入测试版本并重启；取得两种入口材料，分别验证首次中途退出与最后批准，重新开档/读档后不重复完整首读。
2. 真正走完调查链/证伪，领取3份；取得一项证据后主动终止，领取1份；无证据放弃不给。Je交接1份，不二次领取。
3. 材料不足、恰好足够、多批武器订单、取消和连续点击确认；确认成品进入实际打开的货舱来源，存取仓储场景也核对。
4. 确认调查报告图标、中文换行、原版选择器批次数与确认页可读性。
5. 指定首个实际组件hullmod并添加效果继承及标签后，检查原版装配、自动装配、取消预览、船只切换、停靠仓储、拆卸、存取船和存读档。离线管理器验证不等同整套游戏UI/船只生命周期实测。

加载JAR SHA256：`F86447C019F2BFB3EC22EDA800993E6C95BE1899AC47BCA0068B529AD55892B6`。本轮未覆盖它。
