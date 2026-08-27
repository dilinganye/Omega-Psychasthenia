# 精神创伤危机进度与势力时代接口

## 变量

全部连续变量使用 `0–100`：

| 枚举 | 含义 |
| --- | --- |
| `RECON_CONFIDENCE` | 第四窥视掌握人类星域情报的可信程度 |
| `HUMAN_AWARENESS` | 人类确认危机存在的程度 |
| `WATCHER_AGGRESSION` | 第四窥视从观察转向试探和报复的倾向 |
| `NEST_DEVELOPMENT` | 前哨母星、工业与复制能力发展程度 |
| `BLOCKADE_DENSITY` | 母星周边、跳跃点和超空间封锁密度 |
| `OMEGA_ESCALATION` | 全面战争后精神创伤升级程度 |
| `HUMAN_COHESION` | 人类方面共享情报和协同防御的能力 |
| `GLOBAL_PANIC` | 全局恐慌加成；默认 0，仅供特殊剧情事件修改 |
| `PUBLIC_PANIC` | 已弃用兼容别名，读写同一个 `GLOBAL_PANIC` |
| `REALITY_DISTORTION` | 行星改造、黑洞要塞与异常空间影响程度 |

全面进攻准备度是派生值：

```text
侦察置信 × 0.30 + 巢穴发展 × 0.35 + 封锁密度 × 0.35
```

要塞化阶段计时结束且准备度达到 `55` 后，才会正式发动全面进攻。

## 势力时代

| Era | 危机对外势力 |
| --- | --- |
| `WATCHER_PRE_INVASION` | `Omega_Watcher` / 第四窥视 |
| `PSYCHASTHENIA_POST_INVASION` | `Omega_Psychasthenia` / 精神创伤 |
| `AFTERMATH` | 精神创伤残余；不会恢复为第四窥视 |

第四窥视和精神创伤是同一个危机主体的两个时代。侦察、火力试探、沉寂建设和封锁阶段使用第四窥视；进入 WAR 时，舰队、危机实体、市场、人物和战略事件统一移交精神创伤。WAR 期间会持续校正后来加载或生成的第四窥视资产。

## 读取

```java
PTSDCrisisProgressAPI.Snapshot snapshot = PTSDCrisisProgressAPI.getSnapshot();
float recon = snapshot.reconConfidence;
float readiness = snapshot.invasionReadiness;
String factionId = snapshot.activeFactionId;

float globalPanic = PTSDCrisisProgressAPI.get(
        PTSDCrisisProgress.Variable.GLOBAL_PANIC);
float localPanic = PTSDLocalPanicAPI.getMarketPanic(market);
```

## 添加事件贡献

```java
PTSDCrisisProgressAPI.add(
        PTSDCrisisProgress.Variable.HUMAN_AWARENESS,
        4f,
        "my_mod_unknown_ship_report",
        targetSystem.getId());
```

`sourceId` 应使用稳定且带模组前缀的标识；`systemId` 可以为 `null`。返回值是经过 `0–100` 限制后实际生效的增量。

`set()` 主要用于剧情节点或开发工具，不建议普通随机事件频繁使用：

```java
PTSDCrisisProgressAPI.set(
        PTSDCrisisProgress.Variable.GLOBAL_PANIC,
        20f,
        "my_mod_major_broadcast",
        null);
```

## 监听

运行时监听器不进入存档，其他模组应在每次游戏载入时注册：

```java
PTSDCrisisProgressAPI.registerListener("my_mod", new PTSDCrisisProgressAPI.BaseListener() {
    @Override
    public void reportProgressChanged(PTSDCrisisProgressAPI.Change change,
                                      PTSDCrisisProgressAPI.Snapshot snapshot) {
        // 不要在这里再次无条件修改同一个变量，以免形成递归通知。
    }

    @Override
    public void reportFactionEraChanged(PTSDCrisisProgress.Era previous,
                                        PTSDCrisisProgress.Era current,
                                        PTSDCrisisProgressAPI.Snapshot snapshot) {
        // 可在进入后入侵时代时转换外部模组自己的危机资产。
    }
});
```

卸载或禁用相关内容时可调用：

```java
PTSDCrisisProgressAPI.unregisterListener("my_mod");
```

## 当前自动贡献

- 侦察抵达、战力采样、被玩家目击和成功逃脱。
- 玩家追逐第四窥视。
- 核心前哨建立、行星扩张改造和黑洞要塞完成。
- 占领区轰炸、试探、交涉、防御舰队生成与击败。
- 精神创伤进攻成功、失败学习以及人类防御成功。
- 各阶段随时间发生的自然战略变量增长；全局恐慌不会自然增长，局部事件恐慌在长期无新刺激时缓慢衰减。

进度已经参与侦察精度、Omega 攻击权重、人类防御权重、舰队动态 Flat 和全面进攻阈值，而不是仅用于 UI 显示。

## DevMode

Dev 监视器显示全部变量、当前 Era、活动势力和准备度。显式事件贡献产生“危机进度变化”记录，包含变量、实际增量、结果、来源 ID 和星系位置；自然逐日增长不会刷屏。

## 新闻调查与目击接口

- `PTSDCrisisAPI.recordNewsIncident(incidentId)`：将可调查新闻记录进“边缘失联信号”，并启动最长 30 日的后台调查。
- `PTSDCrisisAPI.reportFleetSighting(systemId, fleetId, label)`：新增或刷新一个持续 10 日的异常舰队目击标签。
- `PTSDCrisisAPI.getActiveSignalTraces()`：读取当前未过期的目击标签。
- `PTSDCrisisAPI.getIncident(incidentId)` / `resolveIncidentTarget(incident)`：读取新闻状态并解析其具体设施、跳跃点或行星目标。

这些方法优先扩展既有危机 API；独立新闻 Intel 仅负责显示和十日过期，不保存战略真相。

## 对抗学习与新闻数据接口

- `PTSDCrisisAPI.recordOmegaDefeat(factionId, playerInvolved, systemId, defeatedStrength)`：供外部危机舰队或剧情战役提交一次精神创伤战败。内部会按损失规模增加目标势力对抗值，并在玩家参与时增加记恨值；同一战略事件应自行防止重复提交。
- `PTSDCrisisAPI.getFactionResistance(factionId)`：读取该势力当前 `0–100` 对抗值。危机标准进攻会把它换算为额外基础 FP。
- `PTSDCrisisAPI.getPlayerGrudge()`：读取玩家 `0–100` 记恨值。正式战争后的消耗袭扰和低补给处决舰队使用此值。
- 新闻定义统一位于 `data/config/PTSD_crisis_news.csv`。其 `id` 为合并键，`phases` 使用 `DORMANT|RECON` 形式声明适用阶段，`investigable` 控制调查入口，`filler` 控制是否进入随阶段递减的普通新闻池。

外部模组可通过合并同路径 CSV 扩展内容；新闻的战略变量经 `PTSDCrisisProgress`，新闻恐慌则经 `PTSDLocalPanicAPI` 写入目标附近殖民地，不再修改全局恐慌。

## 全局恐慌与殖民地局部恐慌

- `GLOBAL_PANIC` 是叠加到所有殖民地实际恐慌上的特殊剧情值，默认及当前正常流程均为 `0`。普通新闻、侦察目击、占领区活动和战斗不会修改它。
- 实际恐慌记录位于 `PTSDCrisisState.SystemData.colonyPanic`，与 Omega 的星系侦察、舰队强度采样和攻击权重处于同一份星系战略信息表。
- `ColonyPanicData.eventPanic` 保存新闻与局部事件贡献；`proximityPanic` 由殖民地至母星/精神创伤控制区的距离和危机阶段定期计算；实际值为二者加全局加成并限制到 `0–100`。
- 新闻以自己的 `targetSystemId` 为中心，在 `PTSDLocalPanicAPI.NEWS_RADIUS` 内按距离衰减传播，并把每个市场的实际贡献保存在 `CrisisIncident.panicByMarket`，从而允许 Je Otloes 只削减某一事件造成的影响。

```java
float marketPanic = PTSDLocalPanicAPI.getMarketPanic(market);
float systemPanic = PTSDLocalPanicAPI.getSystemPanic(system.getId());
float applied = PTSDLocalPanicAPI.addAtMarket(
        market.getId(), 8f, "my_mod_special_incident");
Map<String, Float> spread = PTSDLocalPanicAPI.spreadFromSystem(
        system.getId(), 12f, 18000f, "my_mod_local_news");
```

当前标准影响：本地恐慌每点降低约 `0.65%` 市场流通度，上限 `-65%`；星系最高恐慌会压低该星系所有殖民地的战斗舰队规模倍率，最低至 `25%`；星系恐慌达到约 `30` 后有低频、全局数量上限为 4 的趁乱海盗活动。