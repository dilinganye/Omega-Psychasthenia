# Omega入侵系统 - 完整实现指南

## 概述

本文档详细说明了欧米伽（Omega）入侵系统的完整实现流程，包括四个主要阶段。

## 系统架构

### 核心类
- `IIRT_Omega_Invasion.java` - 主入侵管理脚本（EveryFrameScript）
- `IIRT_Omega_ScoutAI.java` - 侦察队智能AI系统

### 可配置参数（LunaLib）

所有时间参数单位为**游戏天数**，可通过LunaLib在主菜单的MOD设置中调整：

| 参数 | 默认值 | 范围 | 说明 |
|------|--------|------|------|
| `start_stage_time` | 65天 | 1-365 | 初始准备阶段持续时间 |
| `collect_data_time` | 60天 | 1-365 | 侦察阶段持续时间 |
| `invade_time` | 30天 | 1-365 | 初期入侵持续时间 |
| `repair_time` | 30天 | 1-365 | 核心建设持续时间 |
| `scout_min_interval` | 10天 | 1-60 | 侦察队生成最小间隔 |
| `scout_max_interval` | 25天 | 1-60 | 侦察队生成最大间隔 |
| `scout_spawn_radius` | 300 | 100-2000 | 侦察队在目标附近的生成半径 |
| `max_guard_fleets` | 10 | 1-20 | 核心建设期间的最大守卫舰队数 |
| `final_invasion_max_strength` | 200 | 50-500 | 最终入侵阶段的最大攻击强度 |

---

## 入侵阶段详解

### 第一阶段：START（初始阶段）
**持续时间：** `start_stage_time`天

**行为：**
- 系统准备并检查初始状态
- 不生成任何舰队或进行任何操作
- 时间流逝后自动转入COLLECT_DATA阶段

**内存标记：**
- `$IIRT_Omega_Invasion_Stage` - 当前阶段状态

---

### 第二阶段：COLLECT_DATA（侦察阶段）
**持续时间：** `collect_data_time`天

**目标：** 在舰队接近时被发现，逐渐增加威胁感知

**行为：**

1. **侦察队生成**
   - 每 `scout_min_interval` ~ `scout_max_interval` 天生成一支侦察队
   - 侦察目标：所有非过程生成星系中的通讯中继站
   - 侦察队规模：约12个战斗力单位

2. **侦察队AI行为**（IIRT_Omega_ScoutAI）
   - **40%概率：** 被动轨道（ORBIT_PASSIVE）- 在目标周围盘旋，低调侦察
   - **40%概率：** 主动轨道（ORBIT_AGGRESSIVE）- 在目标周围巡逻，可能与玩家接触
   - **20%概率：** 隐形模式 - 关闭应答器，仅在玩家直接扫描时可见

3. **逃离机制**
   - 侦察队每0.5-1秒检查周围10000单位内是否有敌对舰队
   - 如果玩家舰队靠近，激活应急燃烧和传感器脉冲
   - 立即执行逃离命令并驶入超空间

4. **生成位置**
   - 优先从已发现的星门生成
   - 若无星门，在目标周围 `scout_spawn_radius` 范围内随机生成
   - 可在超空间或系统内部生成

**内存标记：**
- `$IIRT_Omega_Invasion_Stage` - "COLLECT_DATA"
- 侦察队标签：`"IIRT_Omega_Scout"`

---

### 第三阶段：INVADE（初期入侵阶段）
**持续时间：** `invade_time`天

**目标：** 建立Omega的主要基地星系

**行为：**

1. **基地星系选择**
   - 选择条件：
     * 过程生成星系（不是手工设计的）
     * 拥有蓝巨星（BLUE_GIANT）或蓝超巨星（BLUE_SUPERGIANT）
     * 玩家未凭空进入或非主要据点
     * 倾向于无人定居点
   - 如果没有蓝巨星系统，降级要求选择任意过程生成星系

2. **基地行星准备**
   - 选择非恒星、倾向非气态巨行星的行星
   - 改变行星外观（灰色调，象征被感染）
   - 清除原有势力市场
   - 创建Omega_Psychasthenia阵营的市场

3. **市场配置**
   - 添加条件：`"IIRT_Omega_Repair_Facility"` - 维修设施
   - 添加产业：轨道建造、行星盾、高级指挥部
   - 储存容量等级：7

4. **入侵舰队快速部署**
   - 每2天生成一支巡逻级大型舰队
   - 规模：战斗尺寸×1.5
   - 位置：基地行星周围300单位
   - 任务：防守基地位置
   - AI等级：OMEGA级

**内存标记：**
- `$IIRT_Omega_Base_System` - 基地星系ID
- `$IIRT_Omega_Base_Market` - 基地市场ID
- 星系标记：`"$IIRT_Omega_Invaded"`

---

### 第四阶段：REPAIR（核心建设阶段）
**持续时间：** `repair_time`天

**目标：** 逐步增强Omega势力，构建防御力量和后备据点

**行为：**

1. **守卫舰队逐步部署**
   - **目标数量：** 最多 `max_guard_fleets` 支
   - **生成间隔：** 每5天生成一支
   - **部署位置：** 从基地向外扩展（半径：300 + 1000×舰队编号）
   - **舰队规模：** 战斗尺寸×（1 + 舰队编号×0.1）
   - **任务：** 防守基地位置

2. **势力升级准备**
   - 在此阶段末期，Omega势力进行全面升级：
     * 舰队数量上限 → 5级
     * 军官素质 → 5级
     * 舰船质量 → 5级
     * 攻击倾向 → 5级

3. **战略通知和后备星域选择**
   - 在阶段完成时显示通知：`"警报：检测到Omega在远方建立了后备据点（星系名）。"`
   - 自动选择玩家从未进入、绿色区域（非过程生成）的星系
   - 将此星系ID保存到：`$IIRT_Omega_ReserveSystem`

**内存标记：**
- `$IIRT_Omega_spawnedGuards` - 已生成守卫舰队数
- `$IIRT_Omega_lastGuardSpawnCheck` - 上次检查生成时间
- `$IIRT_Omega_ReserveSystem` - 后备星系ID

---

### 第五阶段：FULL_ATTACK（全面进攻阶段）
**持续时间：** 无限（直到手工结束或游戏结束）

**目标：** 对玩家势力及其盟友进行持续军事打击

**行为：**

1. **进攻间隔**
   - 每 10-20 天发动一次进攻
   - 间隔时间随机

2. **目标选择**
   - 随机选择任一 `isShowInIntelTab()` 的势力
   - 从该势力的市场中随机选择一个

3. **进攻编队组成**
   - **攻击舰队：** 
     * 从基地出发
     * 规模：战斗尺寸×2 + 4艘护卫舰
     * 舰队点数上限：战斗尺寸×1.5
     * 命令：前往目标并攻击
   - **防卫舰队：**
     * 留守基地
     * 规模：战斗尺寸
     * 舰队点数上限：战斗尺寸×1.5
     * 命令：防守基地

4. **关系管理**
   - Omega与所有其他势力维持敌对关系
   - 定期重置关系等级确保敌对

**内存标记：**
- `$IIRT_omega_Invasion_End` - 外部信号，用于结束入侵

---

## 技术细节

### 计时系统
所有时间转换使用：
```java
float days = Global.getSector().getClock().convertToDays(amount);
```

### LunaLib集成
使用反射方式避免硬依赖：
```java
private Integer lunaGetInt(String modID, String fieldID) {
    try {
        Class<?> c = Class.forName("org.magiclib.LunaWrapper");
        java.lang.reflect.Method m = c.getMethod("getInt", String.class, String.class);
        Object res = m.invoke(null, modID, fieldID);
        return (Integer) res;
    } catch (Throwable t) {
        return null;
    }
}
```

### 舰队生成
使用FleetFactoryV3创建舰队，配置参数：
- 阵营：`"Omega_Psychasthenia"`
- 禁用船舶回收：`MEMORY_KEY_NO_SHIP_RECOVERY`
- AI核心质量：`OfficerQuality.AI_OMEGA` / `AI_GAMMA`等

### 消息和通知
通过CampaignUI显示消息：
```java
Global.getSector().getCampaignUI().getMessageDisplay().addMessage(message, color);
```

---

## 扩展要点

### 后续开发建议

1. **后备据点入侵**
   - 利用 `$IIRT_Omega_ReserveSystem` 在后期生成新的入侵波次
   - 可从此处对远方殖民地进行攻击

2. **动态舰队强度增长**
   - 根据入侵进度调整入侵舰队配置
   - 参考：`final_invasion_max_strength` 参数

3. **玩家反击机制**
   - 摧毁关键舰队可加速（减慢）入侵进度
   - 占领基地可直接结束入侵

4. **特殊事件触发**
   - 可监听特殊敌遇创建事件
   - 在玩家与Omega舰队交战时触发特殊剧情

---

## 调试提示

### Dev模式功能
在 `Global.getSettings().isDevMode()` 启用时：
- 显示侦察队生成信息
- 舰队位置显示为浮动文本
- 所有舰队自动导航至调试目标

### 检查入侵进度
```java
STAGE currentStage = (STAGE) Global.getSector().getMemoryWithoutUpdate().get("$IIRT_Omega_Invasion_Stage");
```

### 舰队缩放测试
修改参数值快速测试不同的入侵强度和压力级别。

---

## 配置文件位置

- **设置定义：** `data/config/IIRT_Omega_LunaSettings.json`
- **时间配置：** 通过游戏内LunaLib菜单调整
- **舰队配置：** `data/world/factions/Omega_Psychasthenia.faction`


