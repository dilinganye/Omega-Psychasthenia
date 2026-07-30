# 精神创伤危机：DevMode 观察与触发指南

本文用于验证 `Omega_PTSD_Invasion` 的阶段推进、侦察、扩张、要塞化和全面战争。所有 `[DEV]` 内容只在 Starsector DevMode 开启时存在；关闭 DevMode 后，调试监视器和仅为调试补建的 Intel 会自动移除，正常的“偶然目击—逐步揭示”逻辑不受影响。

## 1. 进入监视器

1. 用项目当前采用的方式开启 Starsector DevMode，然后载入存档或按 F8 重载脚本。
2. 打开 Intel 界面，找到 **`[DEV] 精神创伤危机监视器`**。
3. 监视器顶部显示：
   - 当前内部阶段和阶段开始日；
   - 玩家正常可见阶段；
   - 接触、侦察目击和逃脱次数；
   - 下一次侦察、权重更新、扩张、要塞、Omega 回合和人类回合的日期；
   - 核心基地、两级警告和第四窥视资产移交状态；
   - 攻击权重最高的十个星系。
4. “战略事件”区显示远方隐藏推演事件和已实体化事件；“即时触发记录”区显示阶段切换、侦察生成、警告、改造、要塞等瞬时动作。

每条有有效位置的事件都有两个按钮：

- **前往**：调用原版 `SectorAPI.layInCourseFor()`，为玩家舰队铺设前往目标的航线。
- **到达**：把玩家舰队转移到目标所在 Location，并放置在目标附近。该操作会真实触发“玩家靠近后实体化”、侦察目击等距离逻辑。

“直接访问情报”中的两个按钮可在 DevMode 下打开战前危机 Intel 和战区态势 Intel。若剧情尚未正常解锁，它们带有 `[DEV预览]` 前缀；若随后满足正式解锁条件，预览会原位升级为正式 Intel。

## 2. 推荐的 LunaLib 测试参数

在 LunaLib 的模组设置中缩短以下参数，可快速跑完一个测试周期：

| 设置 ID | 建议测试值 | 用途 |
| --- | ---: | --- |
| `PTSD_start_stage_time` | 1 | DORMANT → RECON |
| `PTSD_collect_data_time` | 5 | RECON → EXPANSION |
| `PTSD_invade_time` | 5 | EXPANSION → FORTIFICATION |
| `PTSD_repair_time` | 5 | FORTIFICATION → WAR |
| `PTSD_scout_min_interval` | 1 | 侦察最短刷新间隔 |
| `PTSD_scout_max_interval` | 2 | 侦察最长刷新间隔 |
| `PTSD_scout_max_active` | 3 | 同时存在的侦察舰队上限 |
| `PTSD_warning_encounter_threshold` | 1 | 一次有效接触即可触发软警告 |
| `PTSD_strategic_update_interval` | 1 | 权重表更新间隔 |
| `PTSD_expansion_interval` | 3 | 行星扩张改造间隔 |
| `PTSD_front_turn_min_interval` | 2 | 战线回合最短间隔 |
| `PTSD_front_turn_max_interval` | 3 | 战线回合最长间隔 |
| `PTSD_hidden_materialization_range` | 10000 | 便于观察隐藏事件实体化 |

`PTSD_DefStat_onNewGame` 可让**新游戏**从指定阶段开始：

| 值 | 阶段 |
| --- | --- |
| `Sar` | DORMANT / 起始等待 |
| `Cod` | RECON / 侦察 |
| `Inv` | EXPANSION / 建设扩张 |
| `Rep` | FORTIFICATION / 要塞化 |
| `FuA` | WAR / 全面进攻 |
| `End` | ENDED / 结束 |

现有存档应以监视器中的“当前阶段”为准；改变新游戏默认阶段不会倒写已有危机状态。

## 3. 各事件的触发与观察目标

| 事件 | 触发办法 | DevMode 中应看到 | 发展状态应发生的变化 |
| --- | --- | --- | --- |
| 阶段切换 | 等待对应阶段计时结束；测试时缩短四个阶段时间 | 即时记录“阶段切换”，顶部阶段和阶段开始日更新 | 下一类阶段计时被初始化 |
| 侦察舰队生成 | 进入 RECON，等待 `nextScoutDay`；保证活动侦察数低于上限 | 左下角 `[DEV危机]` 消息；记录目标设施、星系和舰队；可前往/到达 | 目标星系 `scoutVisits` 增加 |
| 侦察被目击 | 点击侦察记录的“到达”，让侦察进入玩家可见范围 | “侦察单位被目击”记录 | `totalScoutSightings`、`totalOmegaEncounters` 和该星系目击数增加 |
| 侦察撤离/逃脱 | 靠近或追逐侦察舰队，等待其逃离并消失 | “侦察单位脱离”记录 | `totalScoutEscapes` 增加；若摆脱玩家，异常接触数也增加 |
| 软警告 | 让有效接触数达到 `PTSD_warning_encounter_threshold` | “软警告触发”；战前 Intel 正式出现 | `softWarningShown=true`，玩家正常可见阶段提升 |
| 核心据点建立 | 进入 EXPANSION；系统首次执行 `ensureBase()` | “核心据点建立”，可直接到达被改造行星 | `baseSystemId/baseMarketId` 写入；Omega 控制=1；行星大幅改造 |
| 行星扩张改造 | EXPANSION/FORTIFICATION/WAR 中等待 `nextExpansionDay` | “行星扩张改造”，显示行星和改造等级 | 目标星系 `conversionLevel` 递增；产生建设事件 |
| 黑洞要塞建立 | 进入 FORTIFICATION/WAR，等待 `nextFortressDay`；确保要塞数未达上限 | “黑洞要塞建立”，可到达空间站附近 | `blackHoleFortress=true`；Omega 控制=1；产生要塞巡逻事件 |
| 战略事件生成 | 建设、卫戍、要塞巡逻、Omega 进攻、人类防御、佣兵、玩家舰队或外部 API 排队 | “战略事件生成”；监视器显示类型、阵营、强度、目标和结算日 | 事件进入 `PLANNED` |
| 隐藏事件实体化 | 点击某战略事件“到达”，或正常进入其物化距离 | “战略事件实体化”，按钮优先跟踪实际舰队 | 状态 `PLANNED → MATERIALIZED`，写入 `materializedFleetId` |
| 实体卸载 | 离开目标星系/物化范围，且事件未临近结算、舰队未交战 | “战略事件卸载” | 保存剩余强度，状态回到 `PLANNED`，远方继续隐藏推演 |
| 全面入侵警告 | 进入 WAR（正常流程最终必然发生） | “全面入侵警告触发”；第四窥视资产移交；战区 Intel 正式出现 | `hardWarningShown=true`、`watcherTransferred=true` |
| Omega 战线回合 | WAR 中等待 `nextOmegaTurnDay` | 新的 ATTACK 战略事件和相应目标位置 | 按侦察权重优先选择薄弱点 |
| 人类战线回合 | WAR 中等待 `nextHumanTurnDay` | DEFENSE / MERCENARY_DEFENSE 等事件 | 按人类防御权重部署，受大局上限和玩家标记影响 |
| 战略事件结算 | 等待 `resolveDay`，或在实体化后摧毁相应舰队 | “战略事件结算”，显示成功/失败 | 事件进入 `RESOLVED`；进攻结果更新学习倍率、控制权和后续建设 |
| 殖民地摧毁与占领 | 让 Omega ATTACK 成功结算 | 后续建设、卫戍事件出现在监视器 | 殖民地被摧毁并划为精神创伤实控区；目标行星进一步改造 |
| 玩家特化舰队 | WAR Intel 中使用殖民地产能组建舰队 | PLAYER_TASK_FORCE 事件 | 最多 8 支；占用产能并以独立部署权重影响战线 |
| 外部模组干涉 | 由其他模组调用 `PTSDCrisisAPI.queueExternalEvent()` 或注册 provider | EXTERNAL 事件、外部兵力标记/权重变化 | 进入统一隐藏事件列表并参与战线结算 |

## 4. 一次完整验收流程

1. 开启 DevMode，载入测试档，确认 `[DEV]` 监视器存在。
2. 将阶段时间和事件间隔改成上表建议值。
3. 在 RECON 等待侦察生成，点击“到达”，观察目击与撤离。
4. 确认软警告和正式战前 Intel 只在阈值满足后出现。
5. 等待 EXPANSION，验证基地和至少一次行星扩张。
6. 等待 FORTIFICATION，验证黑洞要塞和巡逻事件。
7. 等待 WAR，确认第四窥视资产移交、正式战区 Intel、Omega/Human 错峰回合。
8. 对一个 `PLANNED` 事件点击“到达”，验证实体化；离开后验证卸载；再等待结算。
9. 关闭 DevMode：确认监视器与 `[DEV预览]` Intel 消失；正式解锁的 Intel 保留，未正式解锁的 Intel 不应残留。

## 5. 注意事项

- “到达”是有状态的测试操作，会改变玩家所在 Location，并可能立刻触发物化、接触、警告或战斗；使用前建议另存测试档。
- Dev 监视器只记录 **DevMode 开启期间** 的即时触发；战略事件列表本身来自存档状态，所以能显示开启 DevMode 前已经排队的事件。
- 普通模式不会发送 `[DEV危机]` 消息、不会创建 Dev 监视器，也不会提前补建任何危机 Intel。
- `src/data/missions` 未参与本功能改动，仍保持原位置和打包方式。

## 6. 占领区、殖民地互动与临时战线投影

### 原版地图隐藏

- DevMode 关闭时，精神创伤 faction 的市场以及带有 `$PTSD_controlled_territory` 的市场会设置为 hidden，不参与原版大地图殖民地标注。
- DevMode 开启时会临时取消 hidden，方便开发定位；关闭 DevMode 后观察器会自动恢复隐藏。
- 占领区不会从危机战略状态中删除。正式战区 Intel 仍通过控制色、战线和箭头提供大致位置推断。
- Dev 监视器的“精神创伤占领区”章节会列出每个市场的实际隐藏状态、双方注意力、累计轰炸损伤、防御舰队胜利次数，并提供“前往/到达”。

### 殖民地互动触发表

| 选项 | 触发条件 | 实际效果 | Dev 记录 |
| --- | --- | --- | --- |
| 远距轰炸 | 拥有 `25 + 市场规模 × 5` 燃料 | 有限降低 Omega 控制；增加精神创伤注意力和少量人类关注 | `HARASSMENT_BOMBARDMENT` |
| 尝试试探 | 无额外资源条件 | 生成占领区防御单元，主动攻击玩家；三天后返航并消失 | `PROBE`、`DEFENSE_SPAWNED` |
| 尝试交涉 | 无额外资源条件 | 播放恶意信道攻击剧情；剧情结束生成较小响应舰队 | `NEGOTIATION`、`DEFENSE_SPAWNED` |
| 全面轰炸 | 玩家击败该殖民地的防御舰队，且拥有 `110 + 市场规模 × 16` 燃料 | 显著增加精神创伤注意力；提高人类关注；降低控制和改造等级；排队新的卫戍事件 | `DEFENSE_DEFEATED`、`SATURATION_BOMBARDMENT` |

防御胜利通过实际战斗监听器判定，不会因为舰队正常返航或超时消失而误判。全面轰炸后会消耗这次“防御已击溃”状态，需要再次击败新的防御反应才能再次进行。

所有互动结果保存在 `PTSDCrisisState.OccupationData`：轰炸次数、试探次数、交涉次数、防御胜利、双方注意力、累计损伤和最后互动类型都可供后续剧情使用。

外部剧情和特殊战役可通过 `PTSDOccupationAPI.registerExtension()` 注册 `InteractionExtension`，追加选项、处理选择并接收所有占领区行动回调。

### 战线性能与投影验证

- 战略层仍以本地权重和伪随机结算为事实来源，实体舰队不是战果来源。
- 普通战线投影同时最多存在 5 支，单个星系最多 2 支。
- 每次投影最多持续约 2.5 游戏日；离开物化范围或期限结束后会卸载，强度写回战略事件。
- 同一事件卸载后至少 5 游戏日才允许重新投影；一次实体化后设置 7 游戏日重复冷却。
- 玩家进入近期发生过远程结算的星系时，可以生成一处持续约 5 天的战斗残骸。每个星系至少间隔 4 天才投影下一处残骸。
- Dev 记录名称分别为“战略事件实体化”“战略事件卸载”和“战线残骸投影”，用于确认实体生命周期。