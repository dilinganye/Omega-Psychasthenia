# Omega-精神创伤：GPT-6 Sol 工作交接

更新日期：2026-10-05。工作区：`D:\Game\Starsector_098\mods\Omega-Psychasthenia`。

本文根据本对话可见的执行记录及当前工作区文件整理，供 GPT-6.1 Sol 接续开发。它区分**当前源码可核对的实现**、**已完成的静态验证**和**尚未完成的游戏内验证**；不能把策划稿中的要求自动视为运行成果。

快速入口：[新闻主表](data/config/ptsdCSV/PTSD_crisis_news.csv) · [前期机制覆盖稿](docs/OVERWRITE/CRISIS_MECHANICS_OVERWRITE.md) · [后期机制稿](docs/OVERWRITE/FORTRESS_WAR_MECHANICS.md) · [FT 事件链](docs/OVERWRITE/FT_EVENT_CHAINS_V2.md) · [危机每日入口](src/data/scripts/campaign/invasion/IIRT_Omega_Invasion.java)。

## 1. 最重要的当前状态

- `mod_info.json` 的模组 ID 为 `Omega_PTSD`，游戏加载 `jars/Omega Psychasthenia.jar`，游戏版本标为 `0.98a-RC8`。最近几轮用户明确要求**只修改源码、CSV 和必要文档，在临时目录编译，不替换该 JAR**。因此当前游戏进程和磁盘上的已加载 JAR 不一定包含下述新行为。
- 当前危机存档版本 `PTSDCrisisState.CURRENT_VERSION = 15`，阶段枚举为 `DORMANT → RECON → EXPANSION → FORTIFICATION → MOBILIZATION → WAR → ENDED`。主要持久状态在 `PTSDCrisisState` 和 `PTSDLateState`；每日入口在 `IIRT_Omega_Invasion`。
- 工作树存在大量尚未提交的修改及未跟踪文件，含早于最近一轮的工作。**不要执行 `git reset`、`git clean` 或批量覆盖**。`data/console/commands.csv` 等也有已有改动，最近一轮没有以它们为目标。
- 最近一轮的 Java 临时编译结果是 `javacExit=0`；只有 Java 8 选项已过时、弃用 API 和未检查泛型等警告。编译目录在系统临时目录生成并清理；`git diff --check` 通过。新闻 CSV 用 Python `csv.DictReader` 检查为 **165 行、15 条唯一 E2、字段对齐**，E2 中文内容按 Unicode 码点确认。**没有把当前代码打包进 JAR，也没有完成游戏内运行验收**。

## 2. 本对话中的工作脉络与产物

### 2.1 设计和机制稿

曾根据用户对精神创伤的构想，整理后期剧情与数值契约，文件均在 `docs/OVERWRITE/`：

| 文件 | 内容与优先关系 |
| --- | --- |
| `CRISIS_MECHANICS_OVERWRITE.md` | 沉寂、侦察、证据、恐慌及跨阶段接口；覆盖更早的同主题设计。 |
| `FINAL_FORTIFICATION_WAR.md` | 营建、要塞、公开动员、战争的剧情与事件文案。 |
| `FORTRESS_WAR_MECHANICS.md` | 玩家档案、战略资产和准备点、施工与突袭、首月战争、总参部等机制。后期数值以此稿为基础。 |
| `FT_EVENT_CHAINS_V2.md` | FT-01～FT-10 的新版特殊事件链；覆盖旧 FT 卡片。 |
| `RECON_NEWS_RUNTIME.md` | 批量侦察报告、条件新闻 CSV 接口和 LunaLib 运行规则。其文中“存档版本 14”是写稿时的记录，**现行源码已是 15**。 |
| `STRATEGIC_ASSET_INCOME_RUNTIME.md` | 侦察任务奖励、舰队返还、实控星球产线与战略资产规则；与战争准备点分账。 |

这些文档部分仍保留“当时只撰写规格”“当前 Alpha 未实现后期”等历史表述。后续判断**现有实现**时应以源码、CSV、存档字段和实际游戏测试为准；判断**设计意图**时再看上述文档。旧 `docs/` 文件仍保留作前期剧情与素材参考。

### 2.2 早于最近一轮、当前工作区可见的实现

当前源码已包含后期阶段状态、有限战略资产与真实/估计战争准备点、营建生产账、要塞四级工程、动员与战争导演、战区、总参部、玩家威胁证据、后备队、FT 特殊事件处理器、条件新闻和 DEV/LunaLib 控制。核心文件如下：

| 领域 | 主要文件 |
| --- | --- |
| 阶段与存档 | `src/data/scripts/campaign/invasion/PTSDCrisisState.java`、`PTSDLateState.java`、`IIRT_Omega_Invasion.java` |
| 战争生产、战区、威胁 | `PTSDLateWarDirector.java`、`PTSDThreatDirector.java`、`PTSDWarIntel.java`、`PTSDCrisisFleetLimits.java` |
| 要塞、撤离和运输 | `PTSDFortressDirector.java`、`PTSDFortressEscapeScript.java`、`PTSDEntropyTransportFactory.java`、`src/data/scripts/everyframe/IIRT_Omega_PluginStarter.java` |
| FT 事件 | `PTSDFTDirector.java`、`PTSDFTDialog.java`、`PTSDFTIntel.java`、`src/data/scripts/campaign/invasion/news/ft/` |
| 新闻与侦察报告 | `PTSDCrisisIncidentManager.java`、`PTSDCrisisNewsAPI.java`、`PTSDConditionalNewsDirector.java`、`PTSDReconReportDirector.java`、`PTSDNewsSiteManager.java`、`data/config/ptsdCSV/PTSD_crisis_news.csv` |
| 总参部/后备队 | `PTSDGeneralStaffDirector.java`、`PTSDGeneralStaffIntel.java`、`PTSDReserveDirector.java` |
| 开发与设置 | `PTSDCrisisDevControlIntel.java`、`PTSDCrisisDevIntel.java`、`src/data/scripts/IIRT_Omega_ModPlugin.java`、`data/config/LunaSettings.csv` |

这些是**文件和入口存在**的结论，不等于每条游戏内分支都已通过实测。历史反馈曾包括 DEV 提示格式异常、战争准备点卡在 0、部分事件按钮立即“完成”却无实体结算、FT 处理器被脚本沙箱拒绝反射实例化、侦察舰队超编、要塞击毁没有演出等。当前代码有对应修订，但旧存档与游戏内回调仍需逐项复测。

### 2.3 最近一轮实施：侦察舰队、要塞、巡逻、E2

本轮用户确认并实施的源码/CSV 变化：

1. **RECON 导航陷阱**：`PTSDReconPlayerEvents.spawnTrapFormation` 不再一次生成五支高强度编队。有活跃市场的星系至多 2 队、合计 60 FP；无人星系至多 3 队、合计 90 FP。共同走最多 3 舰、最高驱逐舰的生成约束；舰队名称统一为“未知编队”。自然触发和 DEV 强制触发走同一方法。
2. **火力侦察实体化**：`IIRT_Omega_Invasion.materializeEvent` 计算同一星系已存在的 `FIRE_PROBE` 实体队数与实际 FP，RECON 默认共同占用最多 3 队、150 FP；占额满则延后实体化。`PTSDCrisisFleetLimits` 默认 RECON 单队 65 FP／最多 3 驱逐级；EXPANSION 210/90、FORTIFICATION 270/105、MOBILIZATION 325/120；WAR 从 325/120 随开战 240 日线性升至 600/400。游戏性难度提高时可放宽战略火侦限制；显示用告警密度不参与难度。
3. **受击即撤与残骸测绘**：`PTSDReconPlayerEvents.ReactivePulseScript` 改为接近玩家后依次尝试原版阻断脉冲、主动传感器扫描、紧急加力；能力暂时不可用时有重试/等待。只在扫描实际发动且舰队逃脱后上传玩家资料。`PTSDOmegaEncounterResponse` 让 R2-13 及共用调查舰持续朝远离威胁的点位移动，离开玩家视野后卸载；途中仍可追击。第三方大型舰队逼近时会按实力选择暂退/伏击，而非只感知玩家。
4. **FT 处理器**：`PTSDCrisisNewsAPI.registerBuiltinHandlers()` 显式注册 FT01～FT10 和逃犯新闻处理器，`IIRT_Omega_ModPlugin` 在应用加载及读档时调用，以避开游戏脚本沙箱禁止动态构造造成的“未找到事件专属处理器”。外部注册 API 仍保留。
5. **要塞运输与毁灭**：`PTSDFortressDirector.sendTransport` 从超空间生成真实熵级运输并指向工程站；选址优先无人黑洞星系。DEV 新增“模拟推进现存要塞至完整体”和详细击毁失败理由。完整体的实际舰船损失、战斗回调及 DEV 模拟收敛到一次性毁灭入口；星系内坍缩有跨战斗/战役地图保存的 180 秒倒计时，超空间毁灭另建残址。未完成要塞不触发完整体爆炸。
6. **实控星系巡逻**：新增 `PTSDControlledPatrolDirector`。玩家进入 `omegaControl >= 0.5` 星系时，按持久槽实体化全星系漫游舰队；RECON 为 2 队/总 100/单队 60，EXPANSION 与 FORTIFICATION 为 4/600/200，MOBILIZATION 与 WAR 为 10/1000/800。离开 8 日后卸载；重入保持受损舰队实际 FP，整队损失需 30 日补员。生成不扣战略资产，整队战损扣 `ceil(战损前 FP/50)`、最多 20，并提高当地短期警戒。专属要塞护卫另算。
7. **条件新闻与 Je**：`PTSDConditionalNewsDirector` 的 R2/E2/F2 自然抽取在每次合格抽取时有 10% 通过门槛，落空不标记已见；沿用分阶段误报率。DEV 强制投放跳过抽取和误报，但仍检查现存目标并生成真实现场。Je 玩家亲自接取的委托可取当阶段 R2/F2 正常权重或旧专案 0.1 权重，E2 不进入池；该线索私下交给调查任务，不重复发公开新闻。Je 代理调查维持原流程。
8. **E2 新闻**：CSV 的 E2-01～E2-15 全部重写。`PTSDE2Director` 管理前三条人类地方事件链，分别为海盗打捞索价、佣兵护航改约、商船与港务重复登记；真实事件生成相关人类舰队并按到场、战斗或到期结算，整条假报道不造舰或奖赏。E2-04～13 是人类短讯，E2-14～15 是不可现场调查的黑洞遥测短讯。`PTSDCrisisNewsIntel` 已区分前三条港口事件与无现场行动的短讯。

上述 1～8 是本轮源码中的具体改动；其**游戏内能力发动、战斗回调、跨场景倒计时、旧档迁移**仍未通过实机确认。

## 3. 上一轮的实际检查和输出

- Java 编译：使用 Starsector core 与已安装模组的 JAR 组成 classpath，把 `src/data/scripts/campaign/invasion` 全部 Java、`IIRT_Omega_PluginStarter.java` 和 `IIRT_Omega_ModPlugin.java` 编入唯一临时目录。最终输出 `javacExit=0`。临时目录在 `finally` 中核对位于系统 TEMP 下后递归清理；没有把 `.class` 留在工作树。
- CSV 检查：`data/config/ptsdCSV/PTSD_crisis_news.csv` 可被 `csv.DictReader` 完整解析；当时共 165 行，E2 15 个唯一编号，字段无错位，E2-01 标题是有效中文 Unicode。终端曾出现乱码，仅是控制台编码显示，Python 码点检查正常。
- Git 检查：最终 `git -c core.safecrlf=false diff --check` 没有输出错误。普通 `git diff --check` 曾仅给出 LF/CRLF 将转换的提示。
- 没有执行游戏内自动化测试、没有用新源码替换 `jars/Omega Psychasthenia.jar`、没有修改 `mod_info.json` 的 JAR 路径。DEV 按钮已经写入源码，不等于当前游戏加载的旧 JAR 里可见。

## 4. 接手时应立即关注的差异与风险

1. **源码和运行 JAR 不一致**：先确认下一轮用户是否继续要求“不更新 JAR”。若仍要求仅源码/临时编译，就不要为了验证擅自替换已加载文件。游戏内验收需之后打包或使用经用户允许的测试副本。
2. **旧设计稿的历史句子**：`FINAL_FORTIFICATION_WAR.md` 仍有“Alpha 以 RECON 为终点”，`RECON_NEWS_RUNTIME.md` 写版本 14。这些描述不能用来否定现行源码。修改设计稿时要保留剧情意图，并明确当前源码版本 15。
3. **阶段进度的规格与代码不同**：`CRISIS_MECHANICS_OVERWRITE.md` 规定 DORMANT 120 日、RECON 最短 365 日且最迟 800 日按证据推进；现行 `IIRT_Omega_ModPlugin` 默认 DORMANT 90 日、RECON 365 日、EXPANSION 30 日，`IIRT_Omega_Invasion` 仍按这些时间参数切换前三段。早期 `FINAL_DORMANT.md` 写 90 日。交接稿应同时保留设计目标和代码事实，不能把覆盖稿的 120 日或证据门槛写成已运行机制。用户此前要求保留现有侦察阶段时长；若后续要改门槛，先核对最新要求和 Luna 设置。
4. **DEV 提示格式历史异常**：用户曾贴出 `UnknownFormatConversionException`，定位 `PTSDCrisisDevIntel.addIncidents` 的 `addPara`。当前该方法第 369 行仍将动态事件文本拼入 `addPara` 字符串。虽然之前有修订，运行时含 `%` 的新闻仍值得特意复测，不能因编译成功而视为排除。
5. **要塞演出与保存**：战斗识别使用持久要塞舰队 ID 和舰船成员 ID；战斗内效果由 `IIRT_Omega_PluginStarter` 推进，战役地图由 `PTSDFortressEscapeScript` 接力。重点实测战斗结束顺序、暂停不计时、离开/重入星系、重复回调、旧档缺失对象、超空间残址仅生成一次，以及市场/天体引用仍可用。
6. **新闻与人类链条**：E2 三条链的真实舰队、超时结算、重复支付和假报道都只有静态检查；特别复测 E2-02 商船先于玩家签约抵达时的 UI 与结局。FT 十张卡和逃犯处理器虽已显式注册，仍需用打包后的 DEV 面板逐卡触发验证。
7. **巡逻运行成本与刷新**：静态检查已修复“离开再进入恢复满编”漏洞；仍须在游戏内检验 8 日卸载、30 日整队补员、战斗伤亡监听器读档后是否继续存在，以及阶段升级时槽位是否按预期增加。
8. **条件新闻概率**：当前是抽取前的绝对 10% 门槛，不是只把同池相对权重乘 0.1。DEV 强制和 Je 私下委托绕过该门槛；确认这与后续用户需求一致后再调整。
9. **旧存档迁移**：`PTSDCrisisState` 和 `PTSDLateState.repair()` 为新增字段做容错；但不能从历史新闻、旧 `playerGrudge` 或不存在的舰队伪造敌方证据、资源与奖励。需要用真实旧档载入测试，尤其是已出现要塞、已投放 FT/E2、在途运输和战斗倒计时状态。

## 5. 建议的后续验证顺序

1. 先记录 `git status --short` 与本文件，区分本任务和原有改动；不要清理未跟踪的源码、文档和资源。
2. 校验 `mod_info.json` 实际加载的 JAR 是否包含最近编译的类；按用户当轮授权决定是否制作测试副本。若不能加载新类，只做静态与临时编译，不宣称游戏内已通过。
3. 按 DEV 顺序测试：RECON 有人/无人陷阱和同星系多火侦 → R2-13 与受击即撤 → FT 十卡注册/目标/后果 → 要塞运输路线与 Small/Stable/Medium/EDEN → 两种完整体毁灭和 180 秒跨地图撤离 → 巡逻离开/重入及战损 → E2 真假新闻与 Je 池。
4. 每项记录**世界实体、实际 FP、存档 ID、真实准备点、敌方估计、战略资产、Intel 文案**。重复点击、重复战斗回调和读档后再触发都应保持一次性。
5. 用新档和真实旧 Alpha/后期存档各验一遍；没有证据支持的历史数据保持未知，并在 DEV 记录迁移原因。

## 6. 用户已确认的边界

- 本系列最近一次开发计划规定：保留现有未提交改动；仅改源码、新闻 CSV 和必要 DEV 说明；编译仅进临时目录并清理，**不替换游戏加载 JAR**。后续若用户改变这一点，以最新指示为准。
- 玩家在 `WAR` 前不应被新闻和选项直接告知精神创伤工程的真正用途；FT、E2 文案采用可由人类机构得知的局部事实和逐步调查。
- 恐慌的基本含义固定；战略资产与战争准备点分账。玩家资料只可由实际观察且成功上传的证据构成；公开新闻、玩家单方面目击、后台真实状态均不能直接变成敌方情报。
- 重要决策和结算应有可追查的目标、资源支出与其他战线变化；DEV 可见精确账本，普通玩家以合理的新闻、Intel 和现场征兆理解结果。

## 7. 资料、源码与参考路径

以下路径已在 2026-10-05 只读核对。相对路径均以本工作区为根；目录大小写按磁盘实际名称填写。引用 API 时优先查看公开接口，再用反编译源码理解原版行为；反编译内容只能作为实现参考，不能假设所有内部类都是稳定的公开 API。

| 用途 | 位置与使用方式 |
| --- | --- |
| 模组源码 | `src/`；危机主体在 `src/data/scripts/campaign/invasion/`，加载入口在 `src/data/scripts/IIRT_Omega_ModPlugin.java`，战斗插件在 `src/data/scripts/everyframe/IIRT_Omega_PluginStarter.java`。 |
| 游戏实际加载物 | `mod_info.json` 指向 `jars/Omega Psychasthenia.jar`。最近修改的源码没有重新装入该 JAR；要区分源码行为和游戏当前行为。 |
| 原版公开 API 源码 | `D:\Game\Starsector_098\starsector-core\starfarer.api\com\fs\starfarer\api\`；可配合 `D:\Game\Starsector_098\starsector-core\starfarer.api.jar` 编译。 |
| 原版反编译源码 | `D:\Game\Starsector_098\starsector-core\deobf\sources\sources\`；用于核对原版能力、战斗/战役地图回调、舰队 AI 和 UI 行为。 |
| 原版游戏资源 | `D:\Game\Starsector_098\starsector-core\data\`；其中 `data\campaign\rules.csv` 可参考原版对话规则与交互链。 |
| BoxUtil | `D:\Game\Starsector_098\mods\BoxUtil\jars\BoxUtilMod.jar`；同时有 `src\` 可阅读。 |
| GraphicsLib | `D:\Game\Starsector_098\mods\GraphicsLib\jars\Graphics.jar`；本地有 `javadoc\`。 |
| LunaLib | `D:\Game\Starsector_098\mods\Lunalib\jars\LunaLib.jar`、`Lunalib-Kotlin.jar` 等；当前磁盘未见独立 `src\`，设置入口是本模组的 `data/config/LunaSettings.csv`。 |
| MagicLib | `D:\Game\Starsector_098\mods\MagicLib\jars\MagicLib.jar`、`MagicLib-Kotlin.jar`；同时有 `src\`。 |
| LazyLib | `D:\Game\Starsector_098\mods\LazyLib\jars\LazyLib.jar`、`LazyLib-Kotlin.jar`；当前磁盘未见独立 `src\`。 |
| 剧情新闻主表 | `data/config/ptsdCSV/PTSD_crisis_news.csv`：卡片 ID、阶段、来源、标题、公开报道、后台真实文本、目标、投放模式和处理器。写作和触发规则须同时核对 CSV 与 Java。 |
| 模组对话规则 | `data/campaign/rules.csv`：模组内原版规则式对话与交互入口。FT 专属的代码对话还要查看 `news/ft/`、`PTSDFTDialog.java` 等。 |
| 设计优先级 | 前期剧情参考 `docs/FINAL_DORMANT.md`、`docs/FINAL_RECON.md`；冲突数值以 `docs/OVERWRITE/` 的机制稿为设计意图，FT 卡以 `FT_EVENT_CHAINS_V2.md` 为准；`docs/INVATION_IDEAS.md` 是较老的素材池。 |

新闻表当前可解析 **165 行**；可识别前缀计数为 R2/E2/F2 各 15、FT 10、MB 5、WR 21。投放模式计数为 AMBIENT 76、CONDITIONAL 45、SCRIPTED 36；其余普通新闻也在同一表内。数字描述的是 CSV 条目，不代表每张卡都完成了游戏内验证。

## 8. MOD 整体目标与写作准则

### 8.1 体验曲线

`mod_info.json` 的早期公开简介给出最初前提：英仙座之外，对一批 Omega 的销毁行动被不明波段打断；它们发生自我演化、摧毁押送者后消失。这个前提可以作为模组名称、舰船美术与危机的共同起点，**不是已确认的最终幕后真相**。简介中“暂时没有生涯事件”的句子属于旧版本状态；现行工作树已经有战役危机源码，是否在玩家当前游戏中生效仍取决于加载的 JAR。

这个 MOD 的核心是让“正常英仙座社会”在可核查的小事中逐步失序。最初仍是地方新闻、海盗闹剧、商业争执和文书错误；侦察期出现无法串成统一解释的异常；营建与要塞化让交通、生产、保险和撤离制度开始扭曲；公开动员承认共同威胁，却因各势力权责和利益产生摩擦；战争把这些细小延误变成战线损失。玩家既能在战斗中作出贡献，也能利用情报、后勤、政治与误导改变敌方选择。全面战争仍会到来，但开局轴线、损失和后续攻防应因前期行动而不同。

参考 TNO 一类作品的制度失序与战时荒诞气质，**不照搬其世界观或句式**。优先写可感的行政动作和社会代价：撤回公告、补签许可、重复回执、保险拒赔、舰队调令与港口关闭。冷酷的公文与局部人的窘迫并置，恐怖由“报告说得很平静，世界却已变样”产生。宏大奇观先改变世界对象、经济、航路或战斗，再进入新闻；不要反复用“不可名状”“现实崩坏”替代具体后果。

### 8.2 情报、敌手与玩家

- 第四窥视和精神创伤是同一主体的前后时代，正式全面进攻后才公开切换到精神创伤。战前普通公众、新闻作者和玩家不能因作者知道后台真相，就在标题、选项或对白中直接说出“欧米伽正在建造什么”。
- 敌人像一名有局部视野的对手：侦察舰实际观测、上传、过期，才形成可用证据；估错、受骗、被截断都要改变部署。侦察、工程、驻防、追击和针对玩家的反应竞争有限资源。调离某防区的舰队须在该区留下可核对的空缺。
- 玩家从火力侦察起形成有来源的多维威胁档案。玩家越积极截运、破坏要塞、援救战线或误导敌方，越可能吸引相应应对；但敌方不能偷读玩家当前货舱、未被观测的航线或后台地图真相。玩家可选择亲自承担火力，也可劝其他势力行动；贡献与总参部话语权按实际战果和当时归属结算。
- 恐慌通常造成经济和防线损失，并可被假消息放大；只有孤立且仍有指挥/互助能力的局部战线，才可能短暂把高恐慌转成背水抵抗。它不无条件增加舰船，也不抹去民生损失。
- 深层目的仅以互相矛盾的航迹、物质流量、非领土目标和异常部署埋线。现有资料没有经用户确认的完整最终真相；**不要在交接、游戏新闻或 Je 对话中自行补造一个官方解释**。Je 可以带来更早、更便宜或更私人化的判断，但其缺席或死亡不得截断必需信息。

### 8.3 各文本渠道的权限

公开新闻只写来源能看见或愿意声称的事实，允许假报道；新闻发表本身不替敌人生成侦察证据。危机 Intel 写玩家已核实的线索、置信度和可行动位置；战区 Intel 写实际战况与估算的不确定性；即时提示限于玩家附近的危险、行动反馈或有理由传来的联合告警；DEV 才展示真值、证据链与资源账本。敌方主要通过舰队调动、命令碎片、异常数据和留下的空缺“发声”，不持续向玩家人格化自述。

事件应是有中间环节的链：公开触发 → 取得有限线索 → 前往酒馆、现场或舰内询问 → 真实舰队/设施行动 → 成功、失败、超时或误报结算 → 后续社会与敌方反应。玩家按钮只能基于其当时合理知道的内容；不能从一条运输回执直接跳到“公布敌方假账”或“阻止要塞生产”。

## 9. 用户直接给出的文案标尺

以下是用户此前在对话中给出的**原文案例**，用于锁定语气和事件节奏；不是要重新覆盖当前 CSV，也不是已经游戏内验收的证明。CSV 中的 `truth` 是后台/调查素材，不能自动成为公开报道。

### 9.1 `N-CUSTOM-01「没油逃犯」`：地方小事也有真实生命周期

```csv
N-CUSTOM-01,ALL,普通新闻,CUSTOM(data.scripts.campaign.invasion.news.FugitiveShuttleNewsHandler),4,22,0,0.2,0,0.1,0,0,0,true,true,边缘航路治安通报,没油逃犯,一殖民地的逃犯夺取了穿梭机，最终因穿梭机燃料不足搁浅。当地警备部门尚未找到失窃舰船。,...我不太清楚你为什么当时要让我去处理这事儿，我就是个搞情报的。我的的确确帮你请了一些佣兵去对付那个家伙，但逃犯会在任何舰队靠近时立刻逃跑，也许他仍藏在目标附近，也许他早就逃之夭夭了。,,,,,,,,,,,,,,
```

关键不在于事件规模，而在于新闻、信息人的有限口吻和现场逃犯舰各有行为：读到报道并不意味着玩家自动抓到人，接近时目标还会逃走。此例同时是 `CUSTOM(...)` 处理器管理实体生命周期的参考。当前主新闻 CSV 中仍有同 ID 行；实现参考 `FugitiveShuttleNewsHandler`。

### 9.2 用户改写的 `FT-02「这可没有」`：从异常报道走向调查

```csv
FT-02,FORTIFICATION,要塞与动员,ANY,0,12,0,0,0,0,0,0,0,false,false,星轨航道台,这可没有,观测台于近日暂停并撤回了一条曾发布的一颗黑洞的辐射曲线，具体原因众说纷纭。,有人说他们观测到了不干净的东西，但是幸好他们没有过多的宣扬这些奇奇怪怪的消息，要不然鬼知道民众会自发过去然后引发什么奇奇怪怪的后果。,,,FALSE,,SCRIPTED,,,,,,按历史记录前往目标天体,记下工程位置,7,1
```

这段原文要求：先是人类观测台撤稿和流言；玩家通过旧数据、当班观测员及现场逐步定位；选择不能让玩家先知敌方工程目的。**当前 CSV 已改为 `CUSTOM(...FT02Handler)` 且公开报道、`truth` 和选项文字不完全等同于上述用户原稿**，应把此例当作文案目标，当前文件当作实际投放内容分别检查。新版链的酒馆询问、费用或故事点、候选星系和真实运载见 `FT_EVENT_CHAINS_V2.md` 与 FT02 处理器。

### 9.3 用户给出的对话intel剧情文本案例
- 工程主管和首席科学家把一小块回收物固定在工位上，正隔着一张投影图争吵。你进门时，两人都没有回头。

"你有看过那个该死的装甲数值吗？"工程主管敲了敲台面，"同样厚的一块东西，不可能称出这个重量。"

"装甲？"科学家把剖面图拉到他面前，"里面是空的。你先别拿它和船壳比。断口附近那组读数，你又打算怎么解释？"

"好，那就叫它'外壳'。"工程主管注意到你，他的明显愣了愣，而科学家则没好气的继续调侃他："现在不知道怎么编了？所以我说..."而随着他也转身看到了你，二人愣在了原地。

"$playerSirOrMadam！"二人慌慌张张的向你行礼，而你只是示意他们继续说下去。

"是的，$playerSirOrMadam，所以..."工程主管把投影转了过来，而科学家则继续说到："根据我们目前的 讨 论..."他在最后两个字上加了重音，工程主管则没好气的翻了个白眼。

"我们能确定它很密，里面曾有强得离谱的能量反应。但这种仪器的外壳被破坏，或者准确说，有一个设计之外的裸露的洞口后，这种能量反应就会倾向于消失，但物质总要有一个出路..."

工程主管打断了科学家的解释，直接说了结果："所以，内部的能量已经泄露干净了，现在残留到底是压缩机的一些残余结构，还是内部的残余物质，我们不得而知。"

科学家点了点头。"是的，现有的数据和推理都能证明这些。至于里面原先有什么——可能某种东西被压进了这个空腔，变成了能量态的场；也可能我们把完全不同的现象当成了一件事。我倾向于先写'高密度空腔体'，别写'运载器械'。毕竟我们还没见过完整的这种东西"
- 你的穿梭机混在几艘获准进入地表的小艇中，沿着一条窄得不太必要的航道下降，缓慢的前往地表。那几艘不知是矿石运输艇还是打捞拖船的工业小艇似乎堵了一艘无标记的医疗驳船好一路，而后者很快便焦急的离开了航道，相当野蛮的从那几只慢悠悠的工业艇缝隙间开了过去。

你命令你的穿梭艇驾驶员缓慢行进，你们并不着急，更何况也并没有医疗人员的那种特殊身份。

硫磺色的云层在加厚窗外被掀开，而后又在一瞬间消逝，利莫特堡上空的红色护盾每隔几秒便在云层里闪一下-这是因为那庞大、琐碎而又高速的云层所致-乍看起来像是有维修员从行星另一侧用巨大的焊接枪短促地用火星点亮天空。

透过逐渐清晰的云层下方，你看见地表被军防设施、小型船坞、一些略大的未知设施和矿坑出口切成许多不规则的色块。你最初以为远处那一排伫立在山缝和深渊间的黑色立柱是轨道电梯的基座；直到导航员将放大图像调取到你的Tripad上后你才意识到，那些是链戍修建的大陆架于地面上的检修段。 资料把它们归在"民用承载设施"下，和供水管线、垃圾处理站列在同一页，但也没有任何更详细的说明了。


### 9.3 用户提出的战争入口场景

用户提出的目标体验是：“精神创伤憋的足够了就拉一大堆乱七八糟的直接边缘星系自动模拟，然后左下角蹦几十条警告（那几天会故意刷屏，各个殖民地都报警），然后玩家会发现在星系边缘的势力集体消逝”。它表达的是**短期、集中、来源各异的灾难告警高峰**：先失联，再收到轨道打击、撤离、稳定度和殖民地毁灭记录；事后可在战区 Intel 回看。远方可战略结算，玩家接近才实体化，避免为演出同时生成全图实体舰队。当前源码有首波警报与远方结算入口，但这段完整体验尚未经过游戏内验收。

## 10. 各阶段目标与当前实现状态

下表中“源码入口”只表示可在当前工作树中找到相应类、阶段分支或 CSV 行；“待实机”表示没有在装载新 JAR 的战役中完成验收。不要把阶段设计、Java 编译和用户实际能玩的版本混为一谈。

| 阶段 | 目标体验、敌方判断与玩家反制 | 当前源码/内容入口 | 已知差距与验证级别 |
| --- | --- | --- | --- |
| **DORMANT／沉寂** | 正常社会基线：地方新闻和真实但非欧米伽的小现场建立参照。敌方不凭普通新闻或距离暗中获取侦察证据；玩家可调查新闻，尚不面对统一危机。 | `IIRT_Omega_Invasion` 的 DORMANT 分支、新闻 CSV 的 N/D/势力普通行、新闻现场系统。 | 代码默认 90 日；覆盖机制稿设计为 120 日，旧 `FINAL_DORMANT.md` 为 90 日。现有早期流程可见于源码；本次没有重新做游戏内验证。 |
| **RECON／侦察与火力试探** | 异常混入普通新闻。侦察队用带来源、上传延迟和有效期的局部报告推断航路、殖民地与舰队；火力试探开始记录玩家反应。玩家可追击、截断上传、污染判断、调查误报并提前预警。 | `IIRT_Omega_ScoutAI`、`PTSDReconReportDirector`、`PTSDReconPlayerEvents`、`PTSDCrisisFleetLimits`、R2 15 条和既有侦察新闻。 | 代码默认 365 日后转营建；覆盖稿提出最短 365/最迟 800 日证据门槛，尚未成为当前转换条件。最近舰队上限与撤离修改仅临时编译通过；上传、重复火侦及 DEV 触发待实机。 |
| **EXPANSION／营建** | 侦察舰减少，母星和据点逐步转为产能；人类仍主要看到运输、港务和产业错账。玩家可找出建设链、截断运输、污染判断和提前布防。E2 以人类地方新闻为主，不让玩家直接从欧米伽视角看工程。 | `IIRT_Omega_Invasion.runExpansion`、`PTSDLateWarDirector` 的生产账、E2 15 条与 `PTSDE2Director` 的前三条小链。 | 代码默认该阶段 30 日，母星建成后营建期可生产准备点；E2 新链与旧档编号去重仅静态检查，实际结算和 UI 待实机。 |
| **FORTIFICATION／要塞化** | 黑洞附近工程、运载、巡逻、封锁和错误进攻轴线逐步显形。敌方在护航、施工、驻防与针对玩家的行动间争用预算；玩家可截运、逼其回防、调查真假集结，提前攻击会改写计划而非结束危机。 | `PTSDFortressDirector`、`PTSDEntropyTransportFactory`、`PTSDControlledPatrolDirector`、FT 10 条专属处理器、F2 15 条。Small → Stable → Medium → EDEN 四级在源码中存在。 | 运输现从超空间真实出发，选址优先无人星系；完整体毁灭与 180 秒逃离入口已写入源码。四级施工、运输路径、FT 全链、两种地点的爆炸及读档幂等均待实机。 |
| **MOBILIZATION／公开动员** | 常规约 30 日，各势力公开确认威胁、撤离和布防；玩家辨别可能轴线并影响首波损失。真实准备极高或有下限约束的错误自信可直接突袭而跳过动员。 | `IIRT_Omega_Invasion` 的 MOBILIZATION 分支、`PTSDLateWarDirector.checkFortificationExit`、`PTSDMobilizationIntel`、MB 5 条；第 22 日后有战前截获入口。 | 条件分支和 Intel 入口可见；真假轴线、到期截获、错判突袭的资源缺口及旧档转换待实机核对。 |
| **WAR／全面战争** | 数日密集告警、边缘星系优先遭受强攻，此后多战区持续攻防。敌方按真实准备点、有限资产和实际新证据调兵；玩家可救援、诱敌、截补给、参与总参部决议与后备队调令。战局应因前期行动出现可追踪差分。 | `PTSDLateWarDirector`、`PTSDWarIntel`、`PTSDGeneralStaffDirector/Intel`、`PTSDReserveDirector`、WR 21 条；完整体移动要塞与跨场景毁灭另由要塞系统承载。 | 战争舰队上限随 WAR 240 日从合计 325/单队 120 增至 600/400。四爆发点、首月殖民地损失、警报刷屏、议决与后备队反复实体化等须在装载新 JAR 的战役中核对；不能因代码和策划稿存在而标为完成。 |

所有阶段均遵守同一结算原则：**真实世界对象、玩家目击、人类公开报道、敌方已上传证据是四种不同信息**。远方可先由战略层推进，玩家接近再物化；物化不应复制兵力。无论玩家是否认识 Je、是否提前攻打母星、是否守住某个边缘殖民地，主线信息都应有替代渠道并继续推进。文案要解释玩家可观察到的因果，不在单周目提前给出完整幕后真相。

## 11. 交接范围之外仍有关联的模组内容

本文件重点交接生涯危机、舰队与新闻叙事；模组同时有舰船、武器、视觉效果与舰队分支。继续开发这些内容时，先看 `docs/PTSD_FLEET_BRANCH_API.md`、`docs/POLAR_RIFT_WEAPON_API.md`、`docs/NEWS_EXTENSION_API.md` 及各武器测试文档，再核对 `data/hulls/`、`data/variants/`、`data/weapons/`、`graphics/`、`src/data/hullmods/` 和对应源码。新闻或危机事件生成欧米伽舰队时，要使用现行舰队分支及阶段上限，不凭剧情文字另造免费战力。上述非危机系统的完整规格、测试状态并未在本次交接中逐舰逐武器审计。

