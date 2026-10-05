# SOL61 初步实现审计与证据

日期：2026-10-05。性质：只读源码审计；没有游戏内复现、没有本轮Java编译或运行测试。

需求与优先级以 [TODO.md](TODO.md) 为准。这里记录可复查事实，避免在后续交接中把疑点、旧稿和已实现混为一谈。行号是本次审阅位置，后续代码改变时先按类/方法定位。

## 1. 运行与工作树事实

- `mod_info.json`：模组ID `Omega_PTSD`，标注游戏版本 `0.98a-RC8`，加载 `jars/Omega Psychasthenia.jar`。
- 工作树已有大量源码、CSV、资源、文档修改及未跟踪文件。它们早于本次写入；没有清理、重置、暂存或提交。
- 前序交接记录存档版本15、临时Java编译成功与新闻表165行等数字；本轮没有重新编译或逐行CSV统计，不能称为本轮新验证结果。
- 前序源码未打入加载JAR。本轮也不更新JAR；源码行为和实际游戏加载行为须分开。
- 2026-10-05写入前 `docs/SOL61/TODO.md` 为用户创建的0字节文件；本轮此前规划期间未对其写入。
- 本工作区检索未发现 `AGENTS.md`；`docs` 中未发现字面名为 `设计.md` 的文件，实际设计入口为OVERWRITE及FINAL等文档。

## 2. 已核实源码缺口

### BUG-01：中文文案耦合行动判定

位置：`src/data/scripts/campaign/invasion/PTSDLateEventDirector.java`，`choose()`及 `orderKind()`（本次约349行）。

事实：先以部分固定ID处理，再用选项字符串的“护送/护航/救出”“攻击/摧毁/反攻”“记录/核实”“守/赶赴”等关键词返回 `ESCORT/BATTLE/OBSERVE/DEFEND`。

影响：仅改文案可能把行动改成另一种，或落入远程结算。必须先将行为从显示文字分离。FT在该choose入口已被拒绝并交独立导演，因此不能假定所有死代码FT分支仍实际运行。

### BUG-02：进入防区即算防御成功

位置：同文件 `advanceOrder()`（约485行起）。

事实：`DEFEND` 在玩家位于目标星系且相应WAR战区存在时调用成功结算；非WAR条件更宽。这里没有要求实际救援、守备持续时间或有效战果。

影响：剧情若承诺“保住殖民地”“完成协防”，当前判断不能证明结果。需区别抵达与目标完成；具体任务按承诺设计，不给所有事件盲加相同守备时长。

### BUG-03：调查与行动直接串结算

位置：`PTSDCrisisIncidentManager.advanceInvestigations()`（约789行起）；`PTSDConditionalNewsDirector.onInvestigated()`（约254行起）。

事实：目标附近的记录调查会置 `investigationResolved` 并调用条件事件调查入口。该入口对非E2真实卡直接 `applyPlayerEffect()` 和 `applyResponse()`；E2交给独立导演。

影响：调查确认可直接成为干扰/协调/设施延缓及敌方反应，缺独特行动过程。不是所有短讯都需要行动；应逐卡盘点。

### BUG-04：Je超时可能刷完成计数

位置：`PTSDJeOtloesManager.advanceTasks()`（约240～246行）；`PTSDCrisisIncidentManager.advanceInvestigations()`（约800～802行）。

事实：Je玩家任务只要 `incident.investigationResolved` 即处理恐慌缓解并增加 `jeCompletedInvestigations`。通用调查超时也置该标记，同时置 `investigationReal=false`。

结论：当前入口未区分成功与超时。新面板还要求“亲自成功并交付”，不能复用旧总计数直接解锁。代理调查在另一完成报告入口也增加该总计数，因此计数不代表全部亲自任务。

### BUG-05：Je固定概率即时死亡

位置：`PTSDLateEventDirector`，`WR-JE-01` 条件（约146/198行）及 `settleRemote()`（约623～633行）。

事实：触发条件涉及Je在场与全局恐慌；选第二项后直接 `Math.random() < .3`，设置 `jeDead` 并清除若干任务引用。

影响：没有在该死亡判定中体现实际涉险推进、局部恐慌/暴露、玩家排险或确认死亡记录。与用户确认的风险链不符；需按真实事件结算而非只换死讯文案。

### BUG-06/07：总参部不是完整成员表决体系

位置：`PTSDGeneralStaffDirector.seats()`（约61行）；`propose()`；`advance()`（约175行）；`PTSDLateState.Resolution`。

事实：

- 席位从所有合格市场及贡献汇总，排除危机势力，但没有实际入盟成员过滤。
- 提案挑一个赞助势力，计算整体 `voteChance`，预留60点额度，约2日后单次随机通过或返还。
- `Resolution`保存赞助方、整体概率等，没有各成员赞成/反对/弃权或否决资格快照。
- 总参部约WAR第10日自动成立，与旧稿事件/多方证据成立条件也有差异。

影响：必须先实现成员、真实投票及资源承诺，不能用饼图把不存在的投票记录包装出来。旧稿55%多数和席位公式是参考候选，用户最新动态否决要求优先。

### BUG-08：地图可见性没有充分隔离

位置：`PTSDFrontMapPanel` 的 `getEdges()`、`drawSystemNodes()`及 `updateHover()`（约326行）；`PTSDWarIntel`。

事实：边从后台活动事件构造；节点颜色/控制区读实际控制，未知节点仍会因为控制/事件相关而突出；悬停显示攻击权重、防卫权重、控制率及要塞活动。

影响：读 `knownToPlayer` 不等于所有显示已经过滤。需单独的人类报告/玩家可见快照，非成员区域和无人区域按新标准处理。己方已报告精确数值与敌方估计不应一刀切。

### BUG-09：防御信标可凭空改变结算倍率

位置：`PTSDWarIntel` 的标记按钮与 `buttonPressConfirmed()`；`IIRT_Omega_Invasion.updateSystemWeights()`（约1603行）；`PTSDLateWarDirector.advanceFronts()`（约530行）。

事实：标记提高人类防卫权重；首波判断中 `DEFEND` 标记直接使defense乘1.4。地图右键目前重置视野（`PTSDFrontMapPanel`约72行），标记按钮在侧栏。

影响：“协调请求”必须引发资源受理/调度，而不是直接提高真实防御。右键菜单替换需要和视野重置交互一起设计。

### BUG-10/11：后备队投影与战损持久化不完整

位置：`PTSDReserveDirector.advance()`（约19行）、`materialize()`及 `dematerialize()`（约93行）；`PTSDLateState.Reserve`。

事实：

- 虚拟行军沿源/目标星系直线推进，达到总距离置 `arrived`。
- 当前靠近投影入口要求玩家在超空间且距虚拟位置约5000以内；投影舰队前往目标超空间锚点。
- 该入口没有完整目标星系投影/实际战区参战安排。
- 保存/重建主要使用 `variantIds`，不是舰体、CR、军官、修理状态等完整成员快照。
- 活实体存在时该推进循环直接continue；其实际行军与虚拟状态同步需进一步核对。

影响：不可认为已实现“抵达参战、卸载保留舰况、反复靠近不恢复”。后两项已列为目标；缺实体是否真实被毁也须区分正常卸载、外部移除和战损。

## 3. 时间与持续战争差异

### GAP-01/03：阶段仍主要按时间与旧参数推进

位置：`IIRT_Omega_Invasion.advance()`（约110～168行）和 `IIRT_Omega_ModPlugin` 阶段参数；OVERWRITE交接第4节。

事实：DORMANT、RECON、EXPANSION按各自时间参数切换；当前交接默认90/365/30日。FORTIFICATION调用后期出口判定，MOBILIZATION第22日后进入战前门槛。

差异：本轮365～840日证据完成度、后期365日组合基线、主动延缓单独记账、1,800日总兜底和提前突袭完整要塞条件需要新适配。不能简单把RECON参数改成450日即称完成。

### GAP-02：首波Front与持续战略战争是不同入口

位置：`PTSDLateWarDirector.advanceFronts()`（约513～566行）；`IIRT_Omega_Invasion.runWar()`及 `resolveDueEvents()`。

事实：首波Front打击满足6轮或开战满30日即结束；伤害在若干轮后可以造成市场永久废弃。主循环WAR仍另行 `runWar()`，通用ATTACK事件另有结算。

结论：不能写“游戏30日后完全停止战争”。需要统一/衔接首波与通用战争的资源、战损、持续攻防及可见报告；新持续战争体系尚未完整形成。

### GAP-04：玩家动态窗口未实现

位置：同 `advanceFronts()`。

事实：按打击次数、火力/防御比较和barrage达到阈值判废弃；本轮所要求的预警时合理航程+5日、15～60日窗口未在该判断中体现。

注意：`groundTruthMarketDefense` 被用于结算器防御判断本身不自动构成敌方偷读。真实战斗结算可使用实际防御，敌方选择目标才受证据限制，二者要分开。

## 4. 疑点与暂缓事项

### RISK-01：读取玩家CR的含义

位置：`PTSDThreatDirector.spawnTrap()`（约173～181行）。

事实：平均CR低于0.35时生成距离3800～5200，否则1700～3000。低CR时更远出现，可能是减轻突袭伤害的生成保护，不必然是敌方使用实时CR选择战略目标。

后续：核对战略决策与世界投影边界，保持敌方证据限制；必要游戏性保护可在内部使用，但不能制造敌方“已知玩家CR”的证据或剧情。

### RISK-02～05：继承交接的未验证项

来源：`docs/OVERWRITE/ALL_DO_GPT_6_SOL.md`。

- 侦察陷阱额度/火力侦察上限、能力真正发动、撤退、上传与第三方威胁处理。
- FT显式处理器注册、十卡生命周期与实体结算；E2前三链的到场、时间和支付；Je任务池。
- 巡逻损伤保持、8日卸载、整队30日补员和读档监听器。
- DEV动态 `addPara` 含百分号：目前约369行仍拼动态公开文本，需实际输入复现，未声称本轮已触发异常。
- 完整要塞毁灭、跨战斗/战役地图180秒倒计时、幂等世界变化与旧档：用户本轮明确暂不动，保留风险，不变成待立即修复的任务。

## 5. 原版与库的核对记录

- 公开API `CampaignClockAPI` 提供cycle/month/day、elapsedDays、seconds转换与GregorianCalendar。
- 反编译 `com/fs/starfarer/campaign/CampaignClock.java`：cycle对应GregorianCalendar年；通常365日；`convertToMonths()`以30日换算。后续写“游戏日”避免误解。
- `CustomPanelAPI`、`TooltipMakerAPI`可构造自定义面板/UI元素和tooltip；现有地图已使用 `BaseCustomUIPanelPlugin`、输入事件与GL11绘制。右键菜单的具体组件、焦点、裁剪与刷新仍须实现前核验。
- `FactionAPI.setRelationship()`及 `RepLevel`可用于关系覆盖；不能只因存在set接口就承诺与外部外交系统完全兼容。
- BoxUtil等本地库路径由用户指定并由前序交接核对；本轮未完整审计每个库的可用组件，不把内部反编译类视为稳定公开接口。

## 6. 本轮验证范围

本次文档落盘只做文件内容、链接、编号、状态和差异核对。源码、CSV、JAR与游戏存档没有改动；没有对MOD重新编译或运行测试。后续新增审计记录：日期、文件/方法、实际证据、影响、核实级别、关联TODO编号。

文档验证结果：三份文件非空且UTF-8回读正常，无Unicode替换字符或行尾空白；目标表的表格编号无重复，本目录Markdown交叉链接全部有效。该验证不覆盖MOD运行行为。

## 审计之后的实操变更

上面的源码结论是首次实操前的基线。最新用户已授权实施，任务结算、MB/WR明确映射、独立人类报告、地图操作、成员图、邀请与关系保存、玩家常规救援窗口等已开始修改；具体已修复范围和仍未完成项见 [EXECUTION_LOG.md](EXECUTION_LOG.md)。旧证据中的行号/缺口不自动代表修改后仍存在，尤其不能继续说“本轮仅文档”。游戏内验收尚未进行。
