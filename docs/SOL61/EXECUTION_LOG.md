> 本次独立测试包：`build/sol61-fortress-collapse-20261019`。临时编译及19项离线回归通过，加载JAR未覆盖；结果见 `FORTRESS_COLLAPSE_REGRESSION.txt`。实际渲染/战斗衔接/性能待游戏验收。

## 2026-10-19 黑洞毁灭演出与DEV清理

- [x] 源码：90秒自绘逆向吸入/局部反色/错位裂环/叠底压暗/猛烈爆裂碎片；无伤害DEV预览与停止按钮，正式战斗/战役接入。
- [x] 源码：伤害跟随演出波面，33％逐舰判定、5～10百分点CR损失、10％保护底线；低于30％CR时15％概率新增D插，整场每舰至多一次。新增参数只在DEV显示且纳入难度预设/导出。
- [x] 删除旧事件全舰队删除与强制全军撤退；存读档墙钟不计离线时间；战斗到战役延续进度与结算去重。
- [x] DEV自建现场安全清理，保留共享对象与真实世界设施，交战中拒绝，不返还测试资产。
- [ ] 实机：渲染观感、战斗衔接、低CR/D插、世界废墟、镜头缩放及性能；手册见 FORTRESS_COLLAPSE_TESTING.md。
- [ ] 历史新闻/Je/联盟等其余缺口继续保留，不宣称全部完成。

## 2026-10-19 左径额度与幸存者生命周期确认

- [x] 设计确认：战斗总额度＝开战敌方FP×每场一次40％～60％随机比例，初始总下限60FP/上限300FP、每轮最多60FP；相关范围和额度均可在DEV自定义难度调整。抽签及额度持久记录，不随每轮或UI刷新重抽。
- [x] 本地API核对：LazyLib实际接口为 MathUtils.getRandomNumberInRange(float, float)，正确示例参数0.40f、0.60f。
- [x] 设计确认：战斗额外援军幸存者按原舰船与战损转战役队；星系内驻防10日后出超空间加力前往最近友好/中立人类殖民地，抵达卸载；超空间战斗直接前往殖民地。无合法目标则等待重选。
- [x] 设计确认：战略左径队靠近玩家才按理论位置实体化，实体化后实际执行任务，完成且脱战后立即卸载，无需离开玩家视野。
- [ ] 运行实现仍待联盟整体方案确认；本次文档更新不改变当前测试版本。详见 ALLIANCE_SYSTEM_DISCUSSION.md 第六节。

## 2026-10-19 支援节奏与未来Luna参数通用约定

- [x] 设计确认：左径战斗支援首轮30秒、后续90秒，按未暂停战斗时间计；持续多轮，以每场总兵力额度限制，不使用固定3轮上限。
- [x] 设计确认：反攻每10日25％概率，全星域最多2队；独立于救援区域名额，参数纳入DEV自定义难度。
- [x] 设计确认：离屏战场仅概率战略援军，不模拟额外战斗必到队。
- **通用开发约束**：后续所有新增LunaLib调整项，用户未特别要求时默认仅DEVMODE可修改，并纳入难度预设/自定义保存/修改转自定义/导出。此约束适用于整个模组，不仅本轮联盟；保留已有明确公开参数的可见性。
- [ ] 待确认：战斗总兵力与每轮规模、战略队规模、战后去向；联盟战斗可达范围与进场技术。
- [ ] 本轮仅更新方案及交接，未更改测试版本。

## 2026-10-19 左径救世与主动反攻方向

- [x] 设计确认：援助所有人类，覆盖正面战场，不限玩家；概率冲击精神创伤占领区，形成有益于人类的混沌变量。
- [x] 设计确认：战略救援使用10日判定间隔（低频测试版本）；战斗援军允许多轮，否决原单轮建议。
- [x] 文档补充：实际反攻、占领区牵制、敌方有证据的真实调防、宗教狂热通信样稿；均明确提案与已确认范围。
- [ ] 待确认：多轮时间/总额度、主动反攻与救援名额是否独立、离屏独立战斗援军是否必到。
- [ ] 尚未实现：本次更新设计文档，没有修改当前测试运行逻辑。见 ALLIANCE_SYSTEM_DISCUSSION.md 第六节。

## 2026-10-19 左径支援规则确认

- [x] 设计确认：战斗必到与概率战略援军独立并存，不替代、不互相扣额度。
- [x] 设计确认：敌对包括第四窥视及全部精神创伤危机身份。
- [x] 设计确认：战略援军免费但限频限量，实际航行、可被击毁；不扣联盟额度，不返还精神创伤资产。
- [ ] 待确认：战斗适用范围、单场到场轮次/秒数、战略概率/区域/刷新间隔/数量及舰队规模。
- [ ] 运行实现：尚未开始；本轮仅更新联盟讨论稿。不能将设计确认视为源码完成。


## 2026-10-19 联盟讨论与测试效率补足

- 新建 `ALLIANCE_SYSTEM_DISCUSSION.md`：联盟成立/邀请、3日响应＋5日整备＋集结、失守方向30日转驻、TAB求援、实体1＋虚拟3侦察及独立左径援军。明确现有实现、已确认规则、待确认规则与建议。
- 已确认：霸主/速子优先发起及英仙/教会接续；独立、英仙、辛达、教会必加入；左径独立支援不加入联盟、不占联盟额度。左径具体友好/战斗范围和兵力来源待答复。
- DEV源码补任意编号/差分输入、模板过滤、只读前置检查、无副作用跳过；保留文本框与滚动位置。临时编译及SceneRevisionTest通过，未替换加载JAR。
- 新增增量测试包 `build/sol61-20261019-devtools`，依赖上一完整测试包的规则与资源。正在测试的旧包保持不变。
- 联盟新机制尚未实施；应待讨论确认后实施，不影响当前测试基线。


## 2026-10-19 更新：现场修订与自定义难度

- [x] 源码：共享设施/殖民地调查入口、独立叹号标记、采样文案集中、取消调查与扫描线、四类残骸差分、风暴接触、动员运输、首波免费且不返还、Gamma保护。
- [x] 源码：五档预设/自定义快照；DEV隐藏参数仍可由预设应用；完整自定义导出；多点等距无越界平滑曲线、两点线性、阶段独立、零值停发。
- [x] 离线：临时编译；现场共享入口/取消/结算及难度快照/曲线等回归验证。未覆盖加载JAR。
- [ ] 实机：Luna保存与切页、场景菜单、地图与按钮布局、风暴及追逐、彩蛋、运输到货、战争推进、帧率。
- [ ] 后续：全部174条新闻逐条专属化与EXPANSION差分、旧泛化模板的剩余世界物体绑定；历史Je情报网、联盟争端、完整后勤与殖民地重建等仍未完成。
- 测试与配置说明：`DIFFICULTY_CURVES.md`、`OCT18_SCENE_REVISION.md`；完整历史缺口仍看 `REMAINING_WORK.md`。切勿把源码完成勾选当作游戏内验收。

# SOL61 实施记录

## 2026-10-05：第一批任务结算修复

最新用户授权“写入md并执行”。现有工作树大量修改与未跟踪文件保留；没有暂存、提交、回滚或覆盖加载JAR。

### 源码改动

- 新增 `PTSDTaskRules`：调查终态与有效结论判断、普通任务最低30日期限、MB/WR的21条非短讯事件共42个选项的明确行动映射。未注册选项拒绝执行，FT继续使用原专属导演。
- `PTSDLateEventDirector`：不再根据中文选项猜测行为；协防移除到场成功，要求玩家击败关联星系的进攻/火侦/截获事件；超过执行期限不再领奖。普通护航先等待接应，玩家抵近后再启程。普通任务至少30日；WR-16保留特殊撤离安排，不修改要塞爆炸。
- `PTSDCrisisState`：版本16；增加调查终态、充分证伪证据引用、行动执行状态和Je亲自成功/私人网络资格。旧档模糊的失败历史不迁移成证伪，不凭旧总计数授予网络资格。
- `PTSDCrisisAPI`：普通线索首次记录时给至少30日；仅调整该任务的调查/条件/现场有效期，不延全局战役、运输上传或真实战斗。已结束/过期条件线索不重复接取。
- `PTSDCrisisIncidentManager/NewsSiteManager`：区别真实确认、充分证伪、结论不足、追踪信号、超时和目标消失；单纯现场空缺不撤销恐慌、不计成功。保留有效调查结论，后续行动过期不将其改成调查失败。
- `PTSDConditionalNewsDirector/PTSDCrisisIntel`：非E2基础取证不再自动执行干扰等后续效果；明确主动下达。共享/协调用通信提交；其他当前普通现场行动采用用户确认的10补给、连续驻留1日，离开现场重置驻留。效果和敌方有成本反应在行动完成后各执行一次。现有Intel暂加操作入口，仅提供机制可操作性，不是最终视觉重写。
- `PTSDJeOtloesManager`：超时/失踪/结论不足不刷亲自任务数或降恐慌；不强制覆盖任务真假值；有效结论自动登记，首次亲自成功记录私人网络资格。原介绍/委托对白保留。
- `PTSDE2Director`：调查状态反馈不把无充分证据的空缺说成证伪；地方剧情专属处理保留。
- `PTSDLateWarDirector/IIRT_Omega_Invasion`：防御信标不再直接增加真实防御、虚拟防御权重或降低敌方选区评分。实际调兵与请求队列仍待接入联盟机制。

### 验证

两次临时编译。最终一轮将危机目录全部Java、ModPlugin、PluginStarter和 `tests/SOL61/TaskRulesTest.java` 编入系统TEMP的唯一目录；`javac_exit=0`，`TaskRulesTest: passed`，`test_exit=0`。使用原版与已安装模组JAR作依赖，源/目标Java8；现有过时API/unchecked警告保留。

测试覆盖有效调查终态、旧状态无凭据不领奖、最低30日与更长期限保留、关键事件分类、特殊撤离、保护事件交还专属导演、未知事件/无效选择拒绝。未在游戏中加载新类、运行存档或验收鼠标交互；编译成功不等于游戏内通过。

### 尚未完成及新发现

1. 首波Front尚无完整实体进攻事件关联，协防现在不会靠到场假成功，但部分首波任务可能没有可登记的关联战果。下一批要衔接真实攻击预算与实体投影；不能伪造目标舰队或另算一份攻击资源。
2. 充分证伪证据字段已建立，但当前多数误报没有成对记录/签名回执等独立证据提供者。现按“结论不足”结束，逐事件补证据后才能成为有效证伪；尚不能宣称证伪内容完成。
3. E2实体地方事件有自己的存续/结算流程，部分实体寿命仍可能短于接取窗口；没有为了满足30日去冻结世界。需要逐卡区分任务专属联系人与真实参与舰队。
4. Je代理报告流程、可干预死亡链、私人面板尚未全面改造；资格只是状态字段，面板尚未上线。
5. 普通现场效果暂沿用既有工程/协调数值。不同事件的独立成本、网络容量、通信条件和外交反制仍待深化。
6. 防御信标免费加成已移除，盟军优先请求/实际答复尚未接通；这段过渡状态需在测试副本中特别检验。
7. 阶段完成度、90/245/30准备阶段、联盟成员与逐势力票、持续战争、兵力账本、允许重写的完整剧情、地图UI与Je专属UI仍在TODO，不能据本批测试打勾。

待专项检阅方案：[UI_PROPOSAL.md](UI_PROPOSAL.md)。仍待回答的数值/方向问题：否决权兵力占比的20%取得/15%撤销初始门槛；霸主/速子缺席时是否允许替代牵头势力。

## 2026-10-05：文案与已批准UI的第二批源码

用户确认UI_PROPOSAL的战线/联盟方案并追加线条规范：不同颜色、半透明、低饱和度虚线，精神创伤/人类总体线条更宽。Je终端风格尚未收到答复。

### 文案

MB/WR共24条新闻的标题、公开报道和截获/补充记录完成第一轮重写，使用新闻、公文、现场记录各自语气；保留既有事实、事件ID、效果字段和选项行为。WR-16紧急撤离与WR-JE-01本批保留。逐行写入检查74条受保护类别的原CSV字段未改；Je原介绍/对白段落未重写；随后只增加代理任务失败时的状态分支，不让原成功对白覆盖失败结论。这只是允许重写内容的一批，不包含全部R2/E2/F2与对话。

### 玩家地图与联盟图表

- 状态版本17增加独立 `HumanReport` 快照，来源仅为本舰可见接触、成员/玩家已签收调令、已送达告警。没有从敌方证据池或Je私密调查导入数据。
- `PTSDHumanWarReports` 定期采集真实可见接触；敌方失联后停止更新旧报告，显示记录日期及过期状态。敌方移动连线仅在同一舰队先后被实际观测于不同星系时生成，不能读取真实出发地代替观测。
- `PTSDFrontMapPanel` 改为快照驱动，去除后台攻击/防卫权重、控制百分比与未知要塞标记；已知星系可悬停/点击报告。虚线为半透明低饱和度，阵营宽底线加势力细线；使用原版UI标签、按钮、边框盒。加入地图绘制裁切及GL状态恢复。非成员有人区域不显示军报详情，无人区域不显示兵力数字。
- 右键菜单接入防御信标、单点撤销、规划航线、当前战区增援申请、授权后备队调动；空白处不下达命令。删除WarIntel侧栏的旧标记按钮区。`PTSDMapCommands` 共用于战线/联盟地图。
- `PTSDWarIntel` 大描述、简介、星图箭头与地图定位均改用人类报告；不再拿敌方母星当默认地图定位，不再向玩家展示后台部署数量/权重。
- `PTSDCoalitionCharts` 四张成员图：已承诺兵力、有效殖民地、累计贡献、近90日贡献；原版标签/势力色/总量/日期，空统计不补虚构数字。玩家个人功绩不成为额外切片，佣金历史归属保持。
- `PTSDGeneralStaffIntel` 初版成员图/地图页、原有提案/后备队页共存。成员图已有源码，完整逐势力表决UI、地图图层过滤与全面情报刷新机制尚未完成。

### 成员与普通殖民地窗口

- 霸主/速子有实际殖民地时登记为牵头成员；其他势力必须收到面板邀请并答复，不自动全部加入。初始答复延迟2日，意愿参考实际受威胁状况与外交关系；这是一版待实测政策，不是用户逐项确认的最终外交数值。玩家须主动申请自身身份入盟。
- 入盟时保存成员两两旧关系，再将低于-40者提高到-40；较高关系保留，不清除其他政治旗标。退出/旧敌摩擦/调解与新外交变化保护尚未完整实现，不能声称联盟外交完成。
- 首波玩家殖民地：首次可靠炮击报告送达时锁定当前舰队正常航程估计（原版 `Misc.getLYPerDayAtBurn` 与 `getDistanceLY`）+5日，夹在15～60日。只延常规永久废弃结算窗口，不停止炮击或世界时间，不刷新换船后的期限；要塞爆炸不动。旧档在本次以后收到的首份可靠炮击报告可开始锁定，不凭空补历史。

### 后续重点

- 席位50/50与历史贡献半额、动态否决/提交快照、真实逐势力表决尚未接入，现有总参部随机总表决仍需替换。
- 已承诺兵力饼图基于当前资源登记，完整来源/战损唯一归账尚待A-06，不把图表上线当成兵力模拟完成。
- 首波实体攻击关联、证伪凭据、E2联系人寿命与Je死亡链仍待完成；随后补齐了多殖民地精确协防绑定和代理已失败线索不计功。
- 游戏鼠标菜单、文本换行、图表遮挡、分辨率、不同日期过期样式与跨窗口刷新均待加载测试副本后实机验收；没有替换加载JAR。

### 收尾修复与最终验证

协防进一步要求实际进攻目标市场或Front引用相符；同星系另一殖民地的战斗不再计功。代理调查不重复接取正在由玩家亲自处理的同一事件；已失败的代理线索不降恐慌、不记完成，原对白段落保留，仅增加失败状态分支。每条地图路线在所属势力色范围内作稳定的小幅色差，方便区分不同战线。

最终回读：`javac_exit=0`、`TaskRulesTest: passed` / `task_rules_test_exit=0`、`HumanReportStateTest: passed` / `human_report_test_exit=0`。最后版本测试还覆盖同星系另一殖民地不计协防、对应目标可计、已结束战区不计。曾遇到测试构造器参数不符，已改为公开空构造器并重跑全部编译/测试；不是隐藏失败。临时目录已按TEMP范围验证后清理。未生成加载JAR或游戏内验收结论。

文档与CSV收尾检查通过：UTF-8/行尾空白/Markdown链接有效；CSV无额外列，实际事件ID唯一（原有重复注释分隔行不作为事件），MB/WR行动映射覆盖21条/42项。此前第一次CSV唯一性检查将注释行也纳入，已纠正检查范围后重跑通过，未改动这些原注释行。
## 2026-10-05：Omega遭遇响应撤退崩溃修复

用户报告 `PTSDOmegaEncounterResponse.advance:92` 在原版 `getEntityById` 内因空字符串ID引用而崩溃。根因：等待伏击满12日的分支将状态改为撤退，却没有设置 `escapeThreatId`；下一次更新直接以null查询实体。

源码同时修复两处：伏击超时转入撤退时保存当前威胁ID；撤退查询先排除null/空白ID，目标消失或不在当前地点时沿用玩家作为威胁。后者兼容旧档中已经保存的异常撤退状态，不要求重新开档。没有修改剧情、资源数值或状态版本。

新增 `tests/SOL61/EncounterResponseRegressionTest.java`，通过公开API代理执行实际 `advance()`，覆盖null、空字符串、空白字符串、已消失目标和现存目标；断言无空ID传入实体查询。使用原版及安装模组JAR作依赖，将本次响应类与测试编译到系统TEMP：Java8目标，`javac_exit=0`；`EncounterResponseRegressionTest: passed (null, blank, missing, existing threat)`，`regression_exit=0`。未进行游戏内加载验证。

按此前约定未替换 `jars/Omega Psychasthenia.jar`。修复需编入加载JAR并重启游戏后生效；源代码修改不会自动更新已加载类。


## 2026-10-05：Luna设置百分号崩溃与侦察逃离卸载

用户报告 `UnknownFormatConversionException: Conversion = '%'`，调用点为Luna设置面板名称 `addPara`。核对本地Luna源码：名称和说明进入原版Formatter。开发设置中10个名称以ASCII百分号结尾，另有“100%。”说明；已改为全角“％”，ID、数值、概率和取值范围保持不变。公开LunaSettings.csv没有ASCII百分号。

用户追加并明确范围：仅设计上已经进入逃离阶段的侦察类舰队，失去玩家可见性后卸载；普通旅行/观察/采样侦察队不受影响，陷阱保留诱导与后续交战。

- `IIRT_Omega_ScoutAI`：仅ESCAPE阶段、非战斗、不可见时完成撤离；移除18日/18000距离/曾目击的额外门槛。保留原有撤离结束的样本上传与现存FP返还，不改任务中侦察队。
- `PTSDOmegaEncounterResponse`：只为surveyScout的escaping状态移除6500距离门槛；战斗追击者保留原距离/地点规则。等待伏击状态不清理；`PTSD_late_trap`豁免不可见卸载。保留上一轮空ID防护。
- `PTSDReconPlayerEvents.ReactivePulseScript`：只有stage=2逃离时，失去可见性便沿原上传/卸载流程结束；扫描/接近不改变。6500扫描取证范围保留。
- `PTSDFortressEscapeScript`：取消后期陷阱不可见5秒直接removeEntity；原接敌、加力与任务生命周期保留。未将其他战争舰队统一纳入隐身卸载。

验证：危机目录全部Java、ModPlugin、PluginStarter、Luna可见性类及两个回归测试临时编译通过，Java8目标。`EncounterResponseRegressionTest`执行实际响应/试触逃离类，覆盖旧档空ID、近距离不可见撤离、普通观察、等待伏击、陷阱豁免和普通战斗追击者保留；通过。`LunaSettingsFormatTest`将339个源字符串及公开CSV交给String.format；通过。未实机加载验证；按此前约定未替换加载JAR，新源码需编入JAR并重启后生效。

临时编译输出：`C:/Users/12862/AppData/Local/Temp/ptsd-luna-escape-cbfe6c2c09194ff3be03829eef9c4809`。保留于TEMP，不作为加载版本。

## 2026-10-05：F2/E2/MB/WR新闻视角、标题与来源纠正

用户指出F2仍使用精神创伤视角。核查确认上一批只完成24条MB/WR，F2未重写。泄露路径不止CSV：条件事件把applyPlayerEffect/applyResponse中的敌方工程账本、预算与调令拼入effectSummary；过期时把未观测的remoteOutcome当确定结局；后期事件簿把truth直接标为核实记录；Je对白直接读旧trueText。

追加标准与命名范围：新闻Headline优先四字；source尽量复用CSV前95行已有名称，减少重复和原创；report参考前70行，使用据悉、据称、具体记录/消息来源等归因，区分目击、转述、推测。用户确认WB是MB的笔误。用户称F系列时只指F-数字，不自动包括F-HEG等派生系列；F2/E2/MB/WR各自单独识别。这一命名规则适用于后续任务，但不撤销此前受保护类别的文案保护。

### 数据

本批56条：F2 15条、E2 15条、MB 5条、WR 21条。标题全部四字；来源全部来自前95行已有名称，共31种，最大重复4次；报道逐条增加消息归因。F2删除公开/核查文本中的工程真实进度、秘密调兵目的、敌方资源容量与设施后台引用，改为可观测航迹/采样及其限制。E2保留地方港口/合同/登记争议，不把人类地方事故直接判为与精神创伤无关。MB/WR保留原事件事实与选择，不以改写新闻改变战争机制；WR-16仅改标题/来源/报道，没有改变要塞爆炸。WR-JE新闻修辞调整，不编Je隐藏身世，不重写原人物介绍对白。WR-10～14按原核查资料复核为港口结算、伤员准入、贸易走廊袭击、损失电文冲突、外围防守选择，避免错配主题。

仅改上述行的叙事列；ID、阶段、数值、触发、effect、choice等机制列保留。受保护N-/D-/P-/E-/F-/FT-/N-CUSTOM-/F-FACTION及其他本批无关行保留（第二轮检查107条其他行未变化）。omegaResponse继续作为内部模拟/开发叙述存在，不再直接向玩家展示。

### 显示与结算

新增PTSDPlayerNewsText：条件事件的玩家摘要由调查/行动状态重建，不读取旧effectSummary、scenarioRemoteOutcome或scenarioOmegaResponse；完成行动不自动获知敌方后续。F2/E2/MB/WR旧档显示优先用当前模板标题、来源、报道、行动文字，避免旧缓存继续泄露。E2实际缴费/合同回执保留，旧元叙事解释隐藏。

PTSDCrisisNewsIntel、PTSDCrisisIntel接入；新闻不再承诺抵近即自动执行行动。PTSDJeOtloesDialog使用核查记录并按调查状态区分，不能直接朗读条件事件内部trueText。PTSDLateEventIntel对MB/WR不提前显示truth为已核实记录，旧内部outcome不再直接展示；行动结算的己方收支仍以实际游戏账户为准。目前这是一层保守显示边界，逐事件追加有来源/时刻的丰富完成回执仍待后续。

PTSDConditionalNewsDirector继续执行原有玩家效果和有成本敌方应对，内部细节只进入Dev记录；过期事件不向玩家声明未来远端结局。PTSDE2Director修正未核查就区分真假、scenarioFalse直接声称证伪/无欧米伽调兵的文字，证伪须依据DISPROVED调查状态。

### 验证与边界

全危机目录、ModPlugin、PluginStarter临时Java8编译通过；PlayerNewsBoundaryTest覆盖旧缓存内部预算/真相/未来、调查终态、完成行动后未知应对、E2旧元叙事与实际5000付款回执保留；TaskRulesTest及HumanReportStateTest均通过。56条标题长度/来源/报道归因检查通过。没有实机验收、未替换加载JAR；源码需重新编入加载版本并重启。正常玩家摘要不展示开发信息；显式[DEV]区域仍用于开发诊断。

未完成：R2及其他允许重写系列全面修订、逐事件独立取证/证伪凭据、带来源时刻的后续观察与详细己方结算回执。不能据本批将整个情报系统标为完全无泄露。

临时验证目录：C:/Users/12862/AppData/Local/Temp/ptsd-news-boundary-30b9b8fadd704c0e8c835550ce8c9dfc。

## 2026-10-05：第一阶段新闻池为空与旧版兼容

用户发现第一阶段没有新闻，并要求尽量兼容旧版。定位：旧N/D等CSV行保留了空白delivery列，optString仅对缺失键默认AMBIENT，对现存空字符串仍返回空；Card构造器原样保存，selectPool要求精确AMBIENT，导致旧空白行被排除。

最小修复位于PTSDCrisisIncidentManager.Card构造器：null、空字符串及纯空白delivery归一为AMBIENT；非空显式类型trim后使用Locale.ROOT转大写，CONDITIONAL/SCRIPTED仍保留原投放方式。不批量更改旧CSV、不增加存档字段或改状态版本、不清除已有新闻/调查/冷却、不补造漏掉的历史新闻、不要求新开档。替换为新加载代码后，下一轮正常抽取可恢复新闻。

DormantNewsPoolTest读取真实CSV，经实际Card构造器及selectPool验证：37条第一阶段兼容记录，其中24条普通新闻+12条暗流为36条随机候选；1条专项调查N-CUSTOM仍不进入随机池。测试null/空/纯空白和显式AMBIENT/CONDITIONAL/SCRIPTED归一化。临时Java8编译通过，pool_test_exit=0。未替换加载JAR，未实机载入旧档；不把源码修复等同当前游戏已生效。

临时验证目录：C:/Users/12862/AppData/Local/Temp/ptsd-dormant-news-8161c3d598b343e6827e63fc81484150。
## 2026-10-06：新闻投放检查与战役航行性能修复

实施用户批准计划，保留附近威胁、侦察报告节奏/上传期限/每日最大值/阶段公式、逃离失联卸载、陷阱交战与要塞撤离。新闻文案和保护系列没有重写。状态版本17不变，新增标记transient、缓存与统计为运行时数据。

主要修复：PTSDCrisisState.get不再反复遍历全状态，每次加载重建初始化标记并完成原迁移，显式repairCollections保留；新调查初始化PENDING。IIRT主循环势力全同步仅首次/阶段转换，后续PTSDCampaignRuntime按0.5日/10批普查；实体创建/卸载接入缓存失效，保留专属战略监听器，避免先装通用监听器产生双重结算。第一次加载兼容普查仍需要完成，后续分散开销。

候选视图缓存本地0.1日/远端0.5日，复用原版BaseLocation.getFleets实时只读typed list；外部模组增删也实时可见，不缓存位置/可见性/战果。原版依据来自starsector-core/deobf的BaseLocation实现。首阶段没有侦察报告/低活动记录时跳过相应后台逻辑。性能摘要仅Dev可选，每15实秒输出调用数及耗时，重新加载清空，不是完整帧时间采集。

157条新闻审计：76 AMBIENT、45 CONDITIONAL、36 SCRIPTED。空白投放方式继续兼容；未知显式类型、目标表达式、条件触发类型作为数据错误记录。随机池首类失败后试另一类，候选每轮移除，避免重复无效抽取；两类不可用保留冷却/两日内重试。条件对象缺失正常等待，订阅不阻止生成，专项调查仅接触入口。没有补旧档历史。

验证：全危机目录、ModPlugin/PluginStarter入口及SOL61测试以Java8目标临时编译成功。TaskRulesTest、HumanReportStateTest、PlayerNewsBoundaryTest、EncounterResponseRegressionTest、LunaSettingsFormatTest、DormantNewsPoolTest、NewsDeliveryAuditTest、CampaignRuntimeTest全部通过。新增覆盖1000次读取不深修、重新加载一次修补、新调查PENDING、旧模糊失败不证伪、TTL及本/外部增删与地点进入、十批普查/监听器去重、条件缺目标等待、日最大值合并不改期限、上传前保留/失效舰队不产生证据。完整成功上传、跨日进度与实机显示/帧时间尚未验收。

临时产物位于`C:/Users/12862/AppData/Local/Temp/ptsd-performance-2fb1de377f8c4e33b8a442e67366e9de`，没有打包/覆盖加载JAR。新闻CSV SHA256 `05F9A62D560C9522D14AABECFC94349DED797B2A6B53186D42A23EAB8D64D157`；加载JAR SHA256 `B7CF19146CF07FDAB28DA8B4A996570A75868F6FC7712D54B4672DB3E5821DFA`，本轮保持不变。实机对照及未完成项见[PERFORMANCE_TEST_PROTOCOL.md](PERFORMANCE_TEST_PROTOCOL.md)与TODO第15节，不宣称已解决实际掉帧。

## 2026-10-12：战争生成/自绘玩家Intel/性能第二轮

用户要求与已确认UI范围、源码变化、旧档处理、未答复设计项与实机边界集中记录在[OCT12_IMPLEMENTATION.md](OCT12_IMPLEMENTATION.md)，目标表第16节和HANDOFF已同步。原剧情CSV未改，加载JAR未打包/写入，保留所有原有未提交与PSD等无关文件。

新增/变化重点：WAR后期侦察、有限预算进攻、任务舰队不被整理器改令、首要塞资源优先、多星黑洞及建设重试；接取聚焦导航、44%普通真实线索、侦察/要塞时间门槛翻倍；PTSDCommandSurface、逐势力票/席位快照、实际行军增援与防线支持账本；移除Luna反射与异常风暴、DevWatcher高频全区整理、隐藏市场重复深搜、事件舰队重复普查、战争警告的重复势力移交。

临时Java8编译通过；TaskRulesTest、HumanReportStateTest、PlayerNewsBoundaryTest、EncounterResponseRegressionTest、LunaSettingsFormatTest、DormantNewsPoolTest、NewsDeliveryAuditTest、CampaignRuntimeTest、LateRuntimeRegressionTest共9项回归通过。Luna文本格式检查334段；157行新闻审计仍通过。临时产物`C:/Users/12862/AppData/Local/Temp/ptsd-oct12-runtime`。没有游戏窗口的自绘UI验证或FPS/帧时间新数据；既有日志更新时间10月5日，不能标为10月12日新版本实测。

## 2026-10-14：接取新闻调查窗口30→60日

按用户要求修改PTSDTaskRules统一期限（60日最低值，较长现有期限保留），接取API标记新期限版本，调查回退/DEV生成/玩家说明同步。新普通新闻现场主残骸和废船按剩余调查期限设置寿命，装饰碎片及回收舰响应不扩改。旧档加载修补将尚未到期、未结束已接取调查延长30日一次，条件/现场期限及仍存在的主证据自然寿命联动，版本标记防重载叠加；已过期/结束不重开，缺期限先按旧规则判断。结果、新闻文案和独立事件链期限不重写。

Java8临时编译及TaskRulesTest、HumanReportStateTest、PlayerNewsBoundaryTest、EncounterResponseRegressionTest、LunaSettingsFormatTest、DormantNewsPoolTest、NewsDeliveryAuditTest、CampaignRuntimeTest、LateRuntimeRegressionTest共9项回归全部通过。期限/迁移专项覆盖60日最低、较长窗口、条件/现场期限联动、一次迁移、已过期/完成/缺期限旧记录不复活；原版API核对DerelictShipData.durationDays、DebrisFieldParams.lastsDays。实机尚未验证。

临时编译目录：C:/Users/12862/AppData/Local/Temp/ptsd-news-60days-f7bb2593e38b4a1080ce87b5f201368a。没有替换加载JAR（本轮SHA256起点582BF8BFD60AFCB313BDA92710B0D9B6AB90AE3D72735E3F563BD0303C18BE6F），保留此前未提交开发成果。需求替代与后续实测见TODO第17节。

### 2026-10-14后续纠正：不处理旧存档

用户明确“不需要管老存档”，撤销上节新增的旧调查延期方案：删除期限版本字段、加载延期、旧主证据寿命扩展和对应迁移专项测试。保留新接取至少60日、较长现有窗口、普通现场主证据剩余寿命、DEV及提示。已有调查不自动延期；既有其他功能迁移没有清除。

最终Java8临时编译及TaskRulesTest、HumanReportStateTest、CampaignRuntimeTest通过。加载JAR未替换；上节旧档延期说明仅为已撤销过程留痕，不再描述最终实现。

## 2026-10-14：Je第三次联系必回复

新增MAX_CONSECUTIVE_CONTACT_ATTEMPTS=3，利用原jeMissedContactsSinceSuccess判定连续两次失败后的第三次必接通，放在当天拒接与随机抽签之前；清除当天拒接，保留原抱怨/道歉并由成功对白重置计数。跨日继续连续计数，同日达到三次才设置当天抱怨；不改变任务冷却、代查联系限制或死亡。未改原对白/未新增旧档迁移；DEV强制失败也不能越过第三次兜底。

实际rollContactAttempt回归JeContactGuaranteeTest通过：当天连续拒接后第三次成功、跨日成功、计数与提示复位、200轮随机联系均不超三次、Je死亡不复活。全危机源码与ModPlugin/PluginStarter入口Java8临时编译成功，产物C:/Users/12862/AppData/Local/Temp/ptsd-je-contact-ae11590741cb4f3995073ce1e55c8f94。加载JAR未写入，实机联系对话仍待构建后验证。

## 2026-10-14 新闻模板扩充基础批次（全案尚未完成）

已批准方案见当前对话；实施边界与完整待办见[OCT14_NEWS_IMPLEMENTATION.md](OCT14_NEWS_IMPLEMENTATION.md)。本批接入JeOnly、修复Je首次交战记录链、部分永久舰队回收、新闻/Je自绘位置图。十四模板和战争扩充尚未实现，不得标完成。原CSV文本保持，未覆盖加载JAR。后续直接继续已授权范围，无需重新询问是否实施。

## 2026-10-14 续做验证结果

完成十四类批准模板及四类旧报道语义模板接入、CSV17条新增、阶段情报、DEV逐项差分与实体战争波次。最终Java8目标临时编译通过（JDK21仅报告旧source/target与既有弃用/unchecked警告），以下13项回归全部通过：TaskRulesTest、HumanReportStateTest、PlayerNewsBoundaryTest、EncounterResponseRegressionTest、LunaSettingsFormatTest、DormantNewsPoolTest、NewsDeliveryAuditTest、CampaignRuntimeTest、LateRuntimeRegressionTest、JeContactGuaranteeTest、JeBattleEntryTest、NewsSceneMechanicsTest、WarWaveArrivalTest。

使用本轮修改前news-input.json逐字段对照原157行，source/headline/report/truth全部保持；新CSV共174条有效行。git diff --check通过，仅提示工作区既有LF/CRLF转换规则。回归输出位于C:/Users/12862/AppData/Local/Temp/ptsd-oct14-events/regression-results.txt。

加载jars/Omega Psychasthenia.jar未写入；当前SHA256为582BF8BFD60AFCB313BDA92710B0D9B6AB90AE3D72735E3F563BD0303C18BE6F。当前无游戏内验证结果，不宣称布局、遭遇节奏或超空间帧率已经验收。全部状态与待核查范围见OCT14_NEWS_IMPLEMENTATION.md。

## 2026-10-14 新闻显示策略与Ping修复

用户要求无定位且无实际现场的新闻不显示地图；F-TT-01明确视为纯报道，覆盖此前PORT_RECORDS自动现场映射。PTSDNewsPresentation统一UI/接取/生成策略，随机目标系统不再被当作公共位置；普通调查显示不参考真假抽签，E2实体链保留地图。旧F-TT未生成现场的误接任务可结束为INCONCLUSIVE。

N-09在当前源码缺少专属生成入口，按用户补充授权新增PTSDNewsAmbientSalvage：免接取、目标星系进入后实体化，3处残骸+独立打捞队，按事件持久标识防重；30日现场窗口，25日准备撤离/35日结束，战斗期间推迟退场。已有同条新闻现场不重复创建；不额外发放已被发现者拿走的纳米煅炉。

崩溃来自PTSDNewsScene.pulse的错误Ping ID interdiction；原版Pings.java与data/campaign/pings.json注册的是interdict，已改Pings.INTERDICT。不是屏蔽异常，原减速/战备效果保留。NewsPresentationRegressionTest调用实际pulse并核对本机原版Ping配置，同时验证地图/按钮/真伪边界及现场去重窗口。

本轮临时编译与5项相关测试通过，未覆盖加载JAR，未进行游戏内验收。临时产物沿用C:/Users/12862/AppData/Local/Temp/ptsd-oct14-events。


## 2026-10-18：全量测试说明与未完成项核对

按用户要求对照最新源码、TODO、OVERWRITE交接及10月12/14/18实施记录，新增FULL_ACCEPTANCE_GUIDE.md、REMAINING_WORK.md与由当前174行新闻配置生成的NEWS_ACCEPTANCE_CHECKLIST.md。清单区分源码存在、实机待验、真实缺口、历史规则冲突与可选设计。复核了DEV真实按钮、Je旧即时死亡、联盟50/50与关系底线、后备队variant快照、RECON时长入口等；修正旧快速手册的四处首波/3日集结指引。没有新增游戏功能或修改加载JAR，本轮不重跑游戏测试；文档链接/数据行数/编码另作检查。

## 2026-10-22：174条新闻内容/事件树/同质化审阅

按用户请求读取当前主CSV和调用关系，历史OVERWRITE/SOL61仅用于意图与前序差异；实际174条新闻、23组（保留F-数字与各势力派生区别），同质化是操作设计审阅，不冒充实机评测。主报告NEWS_EVENT_AUDIT_20261022.md包含所有编号的报道大意、阶段/入口/前置、树状态、重做建议、物理CSV行号和源码链接；CSV/XLSX共174记录26列，增加叙事风险、操作风险、相似条目、原因和优先级。xlsx提供概览、逐条可筛选表、18类事件树，冻结标题及编号区。

用户追加反疲劳与Je乏味反馈后，重点标记Je亲自任务池复用公开R2/F2、JE私有仍共享模板；建议分成互不相同的通信/指挥/救援/谈判核心，不用新道具与更多坐标假装差异。高风险条目逐条给不同保留/合并/压缩/重做方向。28极高、61高、29中、56低；E2纯报道的后台旧链残留另列P0，不把纯报道当成缺事件树。WR-JE-01目前仍立即30%死亡，建议可干预但未实施。

验证：174唯一编号与原表一一匹配；全部174主表行号反查命中；全部引用代码文件存在、位置由当前源码取得；CSV导出回读174条；三页工作簿结构与概览渲染检查通过。原新闻CSV哈希89cc340922f0ffe9b128351922f5f6a1b7eb9e8caaf255cc84963cfa28064318前后不变。仅新增审阅文件及更新目标/交接，未改游戏逻辑/文案、未打包/覆盖JAR、无实机或存档状态验证。

## 2026-10-28：边缘失联信号由操作模板提示改为新闻描述

按用户截图反馈，在PTSDCrisisIntel调查条目标题下使用新闻公开正文（PTSDPlayerNewsText.report），同时覆盖普通调查与Je高能信号。DevMode且siteMaterialized时保留原“现场：siteTitle/siteConfirmationHint”；其余显示新闻正文，缺失时为中性占位。未使用siteDescription/trueText/effectSummary回填正文；固定%s格式避免正文百分号引发Formatter异常。导航、放弃、条件行动及任务生命周期未变。

Java8临时编译全危机目录/ModPlugin/PluginStarter及现有SOL61测试成功；InvestigationNavigationTest（实际渲染按钮/导航）、PlayerNewsBoundaryTest、NewsPresentationRegressionTest均通过。临时产物C:/Users/12862/AppData/Local/Temp/ptsd-investigation-description-a90883c10e024c7bb0d9c331e126959f。加载JAR未写入，本轮起点SHA256 F86447C019F2BFB3EC22EDA800993E6C95BE1899AC47BCA0068B529AD55892B6；实机长文本排版/模式切换仍待验证。

## 2026-10-28：大断电两阶段预兆与搜索导航

实施用户要求：单独PTSDSearchAreaNavigation给搜索/观察任务创建无交互LocationToken，不改实际目标；CrisisIntel导航按钮/地图三处接入。ReconScenes的STATION_BLACKOUT build只建立搜索区域/保留站体引用与PressureClock，不造敌舰、不给站体任务交互标记。等待阶段巨幅原版自定义扫描米色/紫色交替，6～10未暂停推进秒；1400到达门槛后尝试实际生成，使用巡逻传感候选筛选与真实舰队复核，无窗口3秒重试。舰队处较粗紫色阻断与较细米色扫描每约6秒提示，原版ActionIndicator不依赖舰队识别，因而可循脉冲找舰影。旧70秒站体压制/中断恢复保留，生成帧不算等待；首次调查额外观察舰不提前生成。没有改新闻CSV或新增旧档迁移。

核对原版公开loading/CampaignPingSpec、SectorAPI.addPing、SensorBurstAbility与InterdictionPulseAbility，以及deobf/sources/sources的ActionIndicator/PingScript/BaseLocation.LocationToken。远距波是视觉预兆，不模拟不存在舰队或伤害玩家；站外波形使用原版绘制，实际压制仍由本事件机制承担。现场搜索说明已更新，普通新闻正文显示规则保留。

全危机目录、入口和SOL61测试Java8临时编译成功。BlackoutSearchTest、ReconWorldSceneTest、InvestigationNavigationTest、FirstReconInvestigationTest、NewsPresentationRegressionTest、NewsSceneMechanicsTest、SceneRevisionTest、ControlledPatrolRouteTest、PlayerNewsBoundaryTest共9项通过。修正旧压制测试的绝对过期日1000为当前日+1000，使新期限检查有效。新增测试不依赖真实舰队工厂，因此实际生成与AI/视觉仍未验收，步骤见BLACKOUT_SEARCH_TESTING_20261028.md。

临时产物C:/Users/12862/AppData/Local/Temp/ptsd-blackout-4db90df2ee7244f2bb53ea3c01f4bf14；加载JAR SHA256 F86447C019F2BFB3EC22EDA800993E6C95BE1899AC47BCA0068B529AD55892B6保持未写入。同文件存在并行手工对白修改，本轮保留其内容，仅对必要入口局部补丁；最终回归期间ReconScenes哈希稳定。没有回滚其他开发成果。

## 2026-10-28 调查奖励与工程装配台

阅读docs/OVERWRITE/ALL_DO_GPT_6_SOL.md和现有工程师FT-09风格，依据原版ShroudedSubstratePlugin/DwellerCMD接入cargo右键与CustomProductionPicker；采用原版HullModItemManager/getRequiredItem实现按船占用组件。用户回答确认完整3、部分1、Je1和拆下返还，首配方辉光体1/2/50/10/0。新增奖励、首读事件、8列配方解析、确认时库存复核、普通插件/组件双路线，不改保护系列剧情，不补发历史，不加后台轮询。

全危机/cargo目录、依赖和SOL61测试Java8临时编译成功。EngineeringWorkshopTest、ItemBoundHullmodTest、InvestigationNavigationTest、NewsSceneMechanicsTest、BlackoutSearchTest、ReconWorldSceneTest、TaskRulesTest共7项通过。新增测试覆盖真实UI选择回调与实际原版组件管理器，但尚未运行游戏；没有宣称实机验收完成。临时产物C:/Users/12862/AppData/Local/Temp/ptsd-workshop-4202e45bdbb9452fbd345085abdff046。加载JAR未写入，SHA256 F86447C019F2BFB3EC22EDA800993E6C95BE1899AC47BCA0068B529AD55892B6。详细规则/代码入口/验收步骤见ENGINEERING_WORKSHOP_20261028.md，TODO/HANDOFF同步。

## 2026-10-29 用户原文：工程师首读剧情

按用户提供原文替换此前自行创作的三段首读对话。原文独立留档于ENGINEERING_DIALOGUE_REFERENCE_20261029.md并生成PTSDEngineeringDialogueText.java常量，保留“不敢=能”“科学主席”“打引”等所有原有表达与100%字样。仅替换称呼token、按原有问答节点分段；新增武器化/逆向两个实际选项分支，各显示用户指定回应，再以最终报告要求和“是！”回应结束。首读最终选项完成后解锁，随后可查看装配项目或结束交谈。未改变材料、兑换、奖励、等待时间或既有已读标记。

更新现有EngineeringWorkshopTest的首读路径；Java8临时编译及该回归通过。产物C:/Users/12862/AppData/Local/Temp/ptsd-engineering-dialogue-43f61809f3ac4f0e90b98dad3bdcc542。未修改加载JAR；本次只读记录其当前SHA256为4D6B116960F2CB3D61EE32D09473A64905AB129D29313019B7EAFF5BE9B78604，与上次任务记录不同，不将期间其他构建归因于本次操作。原版实际滚动展示仍待实机验收。

## 2026-10-29 新闻左下角正文推送

核对原版IntelInfoPlugin.ListInfoMode.MESSAGES和BaseIntelPlugin颜色/通知绘制入口，在PTSDCrisisNewsIntel.createIntelInfo中增加仅MESSAGES显示的公开report正文，不截断。保留标题、来源、原版文本颜色/换行和Intel列表紧凑布局，不改订阅、silentNews、已读与接取状态。固定%s把正文作为参数传入，避免100%等成为格式指令。扩展现有NewsPresentationRegressionTest验证通知正文、换行/百分号、后台字段隔离、不自动已读/接取、列表紧凑与空正文。

临时Java8编译与NewsPresentationRegressionTest、PlayerNewsBoundaryTest通过；产物C:/Users/12862/AppData/Local/Temp/ptsd-news-popup-c36234930f7c4798a7e1e94875539cc8。未写加载JAR；实际游戏长消息的尺寸和停留时间仍待编入后验收。
