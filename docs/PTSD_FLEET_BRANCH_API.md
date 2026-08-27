# 精神创伤舰队分支生成接口

## 设计约束

每支第四窥视/精神创伤舰队在生成前只选择一个分支。生成器把该分支的装配列表写入 `FleetParamsV3.addShips`，并把原版自动船池的 combat/freighter/tanker/transport/liner/utility 点数全部归零，因此同一舰队不会混入另一分支或势力默认船池。

内置分支：

- `ectoplasm_intervention`（介入灵质）：侦察 `RECON` 权重 5，其余任务权重 1。
- `entropy_transport`（熵级运载）：运输、建设和工程 `LOGISTICS_ENGINEERING` 权重 5，其余任务权重 1。
- `network_wraith`（网络冥魂）：守卫和打击 `GUARD_ASSAULT` 权重 5，其余任务权重 1。
- `garbled_carcass`（乱码尸骸）：所有任务权重始终为 1，不获得任务加成。

普通 `GENERAL` 任务四支等权。任务偏好只影响整支舰队选择哪个分支，不会在舰队内部按权重混船。

## 新增分支

```java
Map<ShipAPI.HullSize, PTSD_BaseShard_Util.ShardTypeVariants> ships = new HashMap<>();
// 按现有 TranvariantData/CubevariantData/WebvariantData/BugvariantData 的格式填充 ships。

PTSD_BaseShard_Util.BranchDefinition branch =
        new PTSD_BaseShard_Util.BranchDefinition("my_branch", "新分支", ships)
                .setWeight(PTSD_BaseShard_Util.FleetRole.RECON, 2f)
                .setWeight(PTSD_BaseShard_Util.FleetRole.GUARD_ASSAULT, 4f);
PTSD_BaseShard_Util.registerBranch(branch);
```

简化注册可使用：

```java
PTSD_BaseShard_Util.registerBranch("my_branch", "新分支", ships);
```

简化注册会令所有任务权重默认为 1。相同 ID 再次注册会替换旧定义；可用 `unregisterBranch(id)` 撤销。

## 创建舰队

推荐统一入口：

```java
CampaignFleetAPI fleet = PTSD_BaseShard_Util.createFleet(
        params,
        intendedFleetPoints,
        PTSD_BaseShard_Util.FleetRole.GUARD_ASSAULT,
        random
);
```

如果调用方必须自行调用 `FleetFactoryV3`：

```java
PTSD_BaseShard_Util.BranchDefinition selected =
        PTSD_BaseShard_Util.prepareFleetParams(params, intendedFleetPoints,
                PTSD_BaseShard_Util.FleetRole.LOGISTICS_ENGINEERING, random);
CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
PTSD_BaseShard_Util.tagFleet(fleet, selected);
```

也可把任务角色换成明确的 `branchId`，强制生成指定分支。

## 运行时标记与 Dev 检查

成功生成后舰队 Memory 中会写入：

- `$PTSD_shard_branch`：稳定的分支 ID。
- `$PTSD_shard_branch_name`：用于显示和 Dev 检查的中文名称。

侦察舰队使用 `RECON`；战略打击、占领区防御、后备进攻、要塞/母星巡逻使用 `GUARD_ASSAULT`。当前 `CONSTRUCTION` 是纯后台战略事件，不物化舰队；未来建设舰队必须使用 `LOGISTICS_ENGINEERING`。

新增精神创伤舰队入口时，不要直接调用 `FleetFactoryV3.createFleet(params)`；应调用本文件中的统一入口，或严格执行 `prepareFleetParams -> createFleet -> tagFleet`。

固定空间站本体（例如 IIRT_Omega_Station_Common 和黑洞要塞）属于单体设施，不参与随机分支；由设施生成的巡逻舰队必须参与分支选择。预设的热寂边界守卫已按普通守卫舰队处理。