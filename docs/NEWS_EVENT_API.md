# 新闻 eventLoc 扩展接口

2026-10-29更新：补齐共享设施多调查入口、轨道站入口、立即接取回调、DEV扩展钩子和参战舰队延迟清理。P-10已改为独立大地图事件，是比教学港口更完整的实现示例。游戏内独立DLC加载、设施交互和存读档仍须验收。仅修改CSV而仍使用旧JAR，无法加载新接口，必须使用同批测试版本。

## 先看这些文件

- 完整可复制参考：`src/data/scripts/campaign/invasion/news/NewsEventReference.java`。包含配置、前置、选址、发布、接取、现场创建、两类后台回调、证据结算、结束清理及对话实现，每个入口均有批注。
- 最小接口：`NewsEvent.java`。只复用现有模板时，实现 `configure` 即可。
- 专属参数：`NewsEventDefinition.java`。原特殊列迁入这里的类型化字段；各字段有默认值、范围和用途。
- 任务状态与结算：`NewsEventContext.java`。`data()`、`bind()`、`recordEvidence()`、`finish()`。
- 内置入口：`src/data/scripts/campaign/invasion/news/defaults/`。Java保留字不能用作包名，所以不是 `news.default`。
- 独立示例源模组：`docs/examples/ptsd-news-dlc/`。构建好的示例在 `build/sol61-news-api-20261028/dlc-example/`，未自动启用。
- 逐条对应关系：`docs/SOL61/NEWS_EVENTLOC_INDEX.md`。

## DLC目录与加载

```text
YourNewsDLC/
  mod_info.json                     # dependencies 中声明 Omega_PTSD
  jars/your-news.jar                 # 自己的Java类；不得把本体API重新打进来
  data/config/ptsdCSV/PTSD_crisis_news.csv
```

自己的CSV只写新增行，不复制整张本体表。ID建议带模组前缀，例如 `MYMOD_NEWS_01`。eventLoc填写 `my.mod.news.SomeEvent`，不带 `.java`、`CUSTOM()` 或引号。类须public、具有public无参构造器，实现本体的 `NewsEvent`。

本体使用原版 `getMergedSpreadsheetDataForMod("id", path, "Omega_PTSD")` 合并一次，使用 `getInstanceOfScript` 创建处理器。不需要DLC ModPlugin，不需要向本体的类表注册，也不需要修改本体JAR。

原版合并允许DLC同ID覆盖本体行，但多个DLC互相覆盖同ID时选择顺序没有保证；正常扩展请使用独立ID。类名错误、接口错误、非法模板和非法参数会拒绝该行并记录日志。DEV定向检查可显示本行配置错误，不生成占位探针。

原有 `CUSTOM(...)` 和旧列读取暂留兼容；非空eventLoc的Java配置优先。既有存档中的旧处理器类名保留。新增任务使用新入口；不回填历史调查奖励。

## 主表16列

|列|含义与默认|
|---|---|
|id|唯一编号；推荐DLC前缀|
|phases|允许阶段，使用 `RECON` 或 `RECON|EXPANSION` 等；ALL表示全部|
|category|新闻类别，同时参与候选池路由：火力侦察→PROBE，普通新闻→NEWS，其余→DARK；不只是显示标签|
|target|通用目标筛选，如ANY、POPULATED、RELAY；实现CustomNewsHandler后由pick接管|
|weight|同一合格候选池内的相对抽取权重，默认1；不是百分比，也不是统一禁用开关|
|cooldown|默认12游戏日；同编号重复投放冷却，同时用于条件新闻初始行动期限。普通新闻展示10日、接取后通常至少30日，不能用本列统一替代|
|investigable|true允许接取，false为报道|
|filler|填充新闻池标记，默认false；系统先选择填充/主要事件池，再按权重选取。不表示真假|
|source|新闻来源|
|headline|标题；本体优先四字|
|report|公开报道，不能写敌方内部决策|
|truth|核查文本字符串，不是真实概率或布尔值，不能当作即时公开情报。普通调查90%真实性由自定义难度控制|
|delivery|空白或AMBIENT为随机新闻；CONDITIONAL使用既有条件导演；SCRIPTED使用既有脚本链|
|JeOnly|TRUE仅由Je入口取得；空白/FALSE允许公开入口|
|eventWeight|空白1、0禁用普通/条件/Je投放，DEV仍可定向检查；乘在原weight上|
|eventLoc|事件实现类全名；无需特殊逻辑的纯报道可留空|

2026-10-28接口迁移由36列减为16列，共移除21个实现细节列并新增eventLoc。所有174条有效记录保留；161行引用158个入口类，13行允许留空。JE-02/03/04与SITE-04/13/14共用处理器，JeOnly及每次事件的状态仍分别保存。

被移除列包括：recon、awareness、aggression、panic、distortion、physicalChance、strength、siteTemplates、siteHandler、martialSite、icon、trigger、action、remoteOutcome、omegaResponse、effect、choice1、choice2、minDay、priority、visibility。它们的有效值已迁入专属Java配置，迁移时原文、权重和效果未重新设计；2026-10-29另经用户确认重做P-10三路线并恢复其eventWeight为0.8。

## 选择适合的接入方式

1. **已有机制的变种**：只实现NewsEvent，在configure填写templates、效果和特殊文本；CSV仍负责标题、阶段等。变种共用类，通过Context的cardId、Je标记及自己的持久数据区分，不能把任务状态保存在处理器成员字段。
2. **独立大地图事件树**：同时实现CustomNewsHandler与SiteHandler。pick选择真实目标；materialize返回现场；createDialog提供互动；manualCompletion=true；通过Context.finish结算。完整参考类就是这种方式。
3. **自定义前置**：unavailableForNews返回原因表示等待；pick返回null表示找不到合法目标。不要为满足配置临时伪造港口、设施或运输。正常等待与异常分开。
4. **既有CONDITIONAL/FT链**：configure保留已有trigger/effect或继承现存链处理器。独立DLC不要填一个本体未支持的trigger/effect名字期待它自动获得机制；新机制用方式2自行实现。SCRIPTED仍是现有后期导演/FT链的接口，本轮没有把后期政治导演改成任意脚本调度器。

## 生命周期与状态契约

1. 加载主表时实例化类并configure。此时可能没有存档，禁止访问玩家/制造舰队。配置对象每行独立，不写存档。
2. 检查unavailableForNews、pick，选择合法目标。选址检查无副作用。
3. 发布生成incident并onPublished。未接取不得提前把现场真假、伏击和敌方部署展示给玩家。
4. 接取时立即调用onAccepted，先保存真假，再进入回调，然后才创建现场。现场创建/交互/后台均有幂等补漏，回调异常也不会重复执行。DEV指定真假同样先保存，不能在回调后才改结果。默认90%真实性仍由自定义难度控制，不是physicalChance字段。
5. 属实调查进入目标星系后materialize。自定义SiteHandler优先于模板，不再混入旧随机回收舰或无关护卫。
6. advanceIncident是每事件后台周期；SiteHandler.advance只在现场存在时运行；CustomNewsHandler.advance是已加载实例的全局周期。它们都不是逐帧回调，不要全星区遍历。秒级特效另用原版脚本并控制作用地点。
7. 交互调用createDialog。共享设施会合并本体与DLC的任务列表，只有选中的子对话才init。因此createDialog只是工厂，禁止在其中发奖、开火或开始上传；进入时的动作放在init。使用bind把其他真实设施列入调查节点并添加黄色任务标记；任务结束解除标记。借用不等于取得删除权。
8. 自己控制结局时使用finish。CONFIRMED会执行核实效果和现场确认回调，DISPROVED撤回恐慌；完整调查3份报告，已有证据但无完整结论1份，超时等无奖励。实际取得证据时调用recordEvidence，才能在之后主动放弃时保留部分报告。
9. onFinished处理调查终态或未接取新闻过期；onExpired在现场清理前调用一次，包含正常完成，不仅是超时。成功回调、分支、奖励、已创建舰队都应幂等。

Context.data仅存字符串；保存抽签结果、实体ID和游戏日即可。不要把处理器、对话对象、Random或渲染对象写进存档。读档重新解析类，任务状态仍在incident；禁止在configure/重入对话时重新投骰。

返回SiteResult时，entityIds只填本事件创建的对象。锚点可以是借用的原版中继或殖民地，但不必、也不应因此加入entityIds。若需保留残局或安排舰队返航，清理前自行移出删除列表并交给自己的生命周期脚本；战斗中的舰队不能直接销毁。

独立现场的 owned 列表中仍有参战舰队时，框架延后整个 onExpired 和实体清理，脱战后重试。自行接管退场的舰队仍由事件负责检查战斗、去重返还与卸载。不能以此替代事件自己的生命周期管理。

## DEV接入（无需向本体面板加编号分支）

- `devStatus(context)`：返回只读现场摘要。
- `devFleetIds(context)`：返回已经存在的舰队ID，面板显示其实际编成；不能为预览造舰。
- `devActions(context)`：返回操作键到按钮文字的映射，默认空集合。
- `runDevAction(context, action)`：仅在DEV显式点击时执行，验证前置后返回结果说明。不能伪造玩家救援成功或发放标准报酬。
- DEV清理会检查 owned 与 devFleetIds 中的参战舰队，再设置 `eventData["framework.devDiscard"]="true"` 并调用清理。处理器据此取消排队响应，自己的额外舰队也必须无奖励/无资产返还地清理。只读显示不应修改任务状态。

完整批注参考类已包含上述四个默认接口。实际示例 P10Event 通过这些接口显示载运者/接收队/追索队，提供进入运输段及导航载运者的按钮。

`target` 支持：ANY（通用）、EDGE_MARKET（偏向外围的有人殖民地）、PIRATE（海盗）、RELAY（有中继）、POPULATED（规模至少3的殖民地）、MILITARY（巡逻总部/军事基地/最高指挥部）、WILDERNESS（无人星系）、FACTION(势力ID)、CUSTOM(处理器)。eventLoc同时实现CustomNewsHandler时由其pick接管目标筛选，CSV内通用target不再决定最终落点。

回调异常会记录到日志和incident.eventError，并停止该事件的普通后台推进。DEV选中现场显示类路径与错误。不要在异常分支悄悄退回“调查成功”，也不要重发报酬。修正代码后重启并使用新测试任务验证。

接口后续扩展优先增加带默认实现的方法，保持现有DLC可编译/加载；本参考类列的是已经接入的接口，不以空占位方法声称实现了未来系统。

## 快速验证

1. 先编入本批源码并使用新CSV；独立测试包在build/sol61-news-api-20261028/package。当前加载JAR没有自动覆盖。
2. 将构建好的dlc-example作为独立模组安装并启用；不要放入本体目录。进入RECON测试存档。
3. 打开DEV「事件现场与舰队测试」，填写 `DLC_EXAMPLE_01:0`，先检查前置再生成；无需等待随机刷新。
4. 导航到目标殖民地。应有调查入口；“正常港口事务”可进入原港口；重新进入仍可调查。核对资料后获得3份报告，再进入不重复。
5. 新生成 `DLC_EXAMPLE_01:-1`，验证不凭空出现敌方现场，原始记录可以证伪。另存档在取得证据后、结束前读档，检查证据不重抽。
6. 同一任务重复结算、完成后等待清理：原殖民地/设施仍存在，没有旧通用回收舰吞掉目标；标准报告只发一次。
7. 在DLC开发副本中拼错eventLoc，重启后DEV检查应明确报告配置错误，其他新闻继续可用。恢复类名后再测。
8. 报告/碎片第一次右键：剧情中途退出不扣材料，不应解锁兑换；读完批准后再右键直接进兑换。材料不足、取消、重复确认不应额外扣料。

19项离线回归包含单独编译和加载示例DLC JAR、174条配置逐字段对照、存档序列化、现场所有权、完整/部分报告去重、旧港口与RECON场景及工坊结算。它们不替代实际游戏脚本沙箱、UI或跨模组回调验收。
