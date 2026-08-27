# 精神创伤新闻扩展接口

新闻表位于 `data/config/ptsdCSV/PTSD_crisis_news.csv`。加载器会遍历所有启用模组中的同路径表格；第三方模组只需在自己的模组目录放置同列结构的 CSV，即可按新 `id` 追加、按已有 `id` 覆盖新闻，并通过 `target` 列使用以下扩展目标。

## FACTION

```csv
"FACTION(""tritachyon"")"
```

也接受 `FACTION(tritachyon)`。事件只会从该势力当前控制、拥有有效主要实体的市场中选址；精神创伤与第四窥视市场始终被排除。势力失去全部市场时，该卡本轮选址失败并由新闻管理器改抽其他卡，不会报错。

## CUSTOM

```csv
CUSTOM(their.mod.news.SomeNewsHandler)
```

类必须：

- 有公开的无参构造器；
- 实现 `data.scripts.campaign.invasion.PTSDCrisisNewsAPI.CustomNewsHandler`；
- 对加载失败、无可用目标等情况从 `pick()` 返回 `null`，新闻系统会改抽而不是终止。

也可以在每次读档时注册短别名：

```java
PTSDCrisisNewsAPI.registerHandler("my_event", new SomeNewsHandler());
```

随后 CSV 使用 `CUSTOM(my_event)`。

## 生命周期

`pick(PickContext)` 负责独特选址，并返回 `TargetSelection`：

- `system` 必填；
- `market` 可空；
- `targetLocation` 是公开的精确位置接口，可为星球、跳跃点、通讯设施或自定义实体。

`onIncidentCreated(IncidentContext)` 在新闻卡创建并应用表格数值效果后调用。它可创建舰队或实体、修改公开的危机状态，并可返回一个实体作为调查目标。

`advance(AdvanceContext)` 是全局效果接口。处理器被任意新闻卡加载后，每个危机 heartbeat 调用一次。处理器对象不会写入存档，因此实现应把长期状态存进 `PTSDCrisisState`、sector memory 或自己的可序列化脚本，而不要只存在处理器字段里。

所有处理器异常会被隔离并写入日志，不会中止整个新闻轮次。

## 内置示例

`CUSTOM(data.scripts.campaign.invasion.news.FugitiveShuttleNewsHandler)` 对应 `N-CUSTOM-01`：

- 在随机殖民地附近选择安全的 `targetLocation`；
- 创建名为“逃犯”的单舰 Kite 护卫舰，初始战备 20%；
- 舰队保持静默且避免主动交战，玩家靠近后逃离；
- 接触后 2–4 日内自动清理，未接触也不会存在超过 30 日；
- 舰船被击毁后目标自然消失，且不可回收。

同目录 `src/data/scripts/campaign/invasion/news/` 用于保存本模组新闻专用处理器；第三方模组不需要修改本模组源码，只需让自己的实现类进入其 JAR 并在合并 CSV 中引用。

## 属实现场列

新闻CSV现额外提供三列：

| 列 | 用途 |
|---|---|
| `siteTemplates` | 属实调查的现场族。可填 `COMMUNICATION`、`ROUTE`、`BATTLE_AFTERMATH`、`CREW_MISSING`、`FACILITY`、`DISTORTION`；使用 `|` 提供多个候选。空值或 `AUTO` 会依据目标类型、新闻分类和卡片ID自动推断。 |
| `siteHandler` | 独立于 `target=CUSTOM(...)` 的现场代码窗口。可填 `CUSTOM(完整类名)` 或注册别名。它只负责玩家抵达后的实体化、确认与清理，不改变新闻最初的选址。 |
| `martialSite` | `TRUE`、`FALSE` 或 `AUTO`。AUTO会把火力侦察以及强度/攻击性足够高的新闻视为武力新闻；属实现场有50%概率追加战损残骸与第四窥视调查小队。 |

因此两种CUSTOM互不冲突：

- `target=CUSTOM(...)` 决定新闻发生在哪里、创建新闻时做什么；
- `siteHandler=CUSTOM(...)` 决定报道属实后，玩家在现场会看到什么。

现场处理器需实现 `PTSDNewsSiteAPI.SiteHandler`：

```java
public final class MySite implements PTSDNewsSiteAPI.SiteHandler {
    public PTSDNewsSiteAPI.SiteResult materialize(PTSDNewsSiteAPI.SiteContext context) {
        // 在 context.targetLocation 附近寻找安全点并创建实体。
        // 返回的 anchor 会成为新的调查目标；entityIds 用于过期清理。
        return new PTSDNewsSiteAPI.SiteResult(anchor, "现场名称", "现场描述",
                "确认条件提示", entityIds);
    }
    public void advance(PTSDNewsSiteAPI.SiteContext context) { }
    public void onConfirmed(PTSDNewsSiteAPI.SiteContext context) { }
    public void onExpired(PTSDNewsSiteAPI.SiteContext context) { }
}
```

也可在读档时注册别名：

```java
PTSDNewsSiteAPI.registerHandler("my_site", new MySite());
```

CSV随后填写 `CUSTOM(my_site)`。自定义处理器对象不序列化，持久状态必须写入 `CrisisIncident`扩展数据、sector memory或处理器自己创建的可序列化脚本。异常会被隔离记录；若`materialize()`返回空，系统会回退到通用现场族。

## 通用现场生命周期

1. 玩家记录新闻时仍按普通新闻进行25%属实、70%假消息、5%跟踪舰差分；Je专属实地任务固定拥有可验证现场。
2. 属实调查在玩家进入目标星系时才实体化，避免远方活动实体造成性能压力。
3. 玩家抵近现场约1800距离后确认，新闻真相、现场名称和战略影响写入Intel。
4. 动态实体在确认后1–5日内清理；未确认现场与调查无论如何不超过30日。
5. 武力场景的调查小队只使用一个精神创伤分支，保持非主动攻击并围绕残骸扫描；残骸编成、数量、舰队FP、名称与任务文本均有差分。