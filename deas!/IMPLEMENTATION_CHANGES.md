# Omega入侵系统 - 修改完成说明

## 📋 文件修改清单

### 新建文件（3个）

#### 1. `src/data/scripts/campaign/IIRT_Omega_ScoutAI.java`
**功能：** 侦察队智能AI系统  
**特点：**
- 3种随机行为模式（被动、主动、隐形）
- 智能敌舰检测与逃离
- 完整的BaseAssignmentAI集成

#### 2. `src/data/scripts/campaign/IIRT_Omega_ReserveAttacker.java`
**功能：** 后备基地进攻系统  
**特点：**
- 定期从后备系统发动进攻
- 智能目标选择（优先玩家殖民地）
- 动态舰队强度计算
- 完整的消息通知系统

#### 3. `data/config/IIRT_Omega_LunaSettings.json`
**功能：** LunaLib配置文件  
**特点：**
- 9个可调旋键（时间、频率、数量）
- 每个参数都有范围限制和默认值
- 中文描述和友好的参数名称

### 修改文件（1个）

#### `src/data/scripts/campaign/IIRT_Omega_Invasion.java`
**修改内容：**

1. **添加可配置参数**（第46-55行）
   ```java
   public static int start_stage_time = 65;
   public static int collect_data_time = 60;
   public static int invade_time = 30;
   public static int repair_time = 30;
   public static int scout_min_interval = 10;
   public static int scout_max_interval = 25;
   public static int scout_spawn_radius = 300;
   public static int max_guard_fleets = 10;
   public static int final_invasion_max_strength = 200;
   ```

2. **添加LunaLib读取功能**（第54-65行）
   ```java
   private Integer lunaGetInt(String modID, String fieldID) { ... }
   ```

3. **在构造器中加载配置**（第73-86行）
   - 使用反射安全读取LunaLib设置
   - 自动降级存在依赖时

4. **改进侦察队生成**（第136-140行）
   - 使用可配置的时间间隔
   - 随机化侦察队间隔时间

5. **应用配置到侦察队生成**（第191行）
   - `scout_spawn_radius` 用于生成范围

6. **替换为智能侦察AI**（第209行）
   - 从简单的GO_TO_LOCATION_AND_DESPAWN更改为IIRT_Omega_ScoutAI

7. **核心建设阶段改进**（第351-373行）
   - 逐步生成守卫舰队（最多`max_guard_fleets`支）
   - 每5天生成一支新守卫舰队
   - 舰队规模随编号递增

8. **最终通知和后备系统**（第386-403行）
   - 显示后备据点通知
   - 自动选择后备星系
   - 启动IIRT_Omega_ReserveAttacker脚本

---

## 🔧 配置调整方法

### 方法1：游戏内菜单（推荐）
1. 启动游戏，进入主菜单
2. 点击"MOD"选项卡
3. 找到"Omega - 精神创伤"
4. 点击"设置"或齿轮图标
5. 调整9个参数中的任意一个
6. 点击"应用"或"保存"

### 方法2：直接编辑源代码
编辑 `IIRT_Omega_Invasion.java` 第46-55行的常量值，然后重新编译。

### 方法3：游戏进行中修改
- 参数直接保存到 `LunaSettings` 数据
- 重新进入游戏加载存档时会读取新参数
- 新生成的舰队会应用新参数

---

## 📊 流程完整性检查

以下验证确保所有5个阶段都已实现并互相连接：

- [x] **START → COLLECT_DATA** (第123-127行)
- [x] **COLLECT_DATA → INVADE** (第220-225行)
- [x] **INVADE → REPAIR** (第340-343行)
- [x] **REPAIR → FULL_ATTACK** (第380-404行)
- [x] **FULL_ATTACK** (第405-468行)
- [x] **后备系统选择** (第395-403行)
- [x] **ReserveAttacker启动** (第403行)

---

## 🎮 游戏流程示例

### 第一周期（超时间轴）
```
Day 65: START → COLLECT_DATA转换
Day 65-125: 侦察阶段（生成侦察队，每10-25天一支）
Day 125: COLLECT_DATA → INVADE转换
Day 125-155: 初期入侵（建立主基地，每2天部署舰队）
Day 155: INVADE → REPAIR转换
Day 155-185: 核心建设（逐步部署守卫舰队，势力升级）
Day 185: REPAIR → FULL_ATTACK转换
         显示后备据点警告
         启动后备进攻系统
从Day 185+: 全面进攻（主基地每10-20天一次，后备每15-30天一次）
```

### 侦察队行为流程
```
生成 → 随机选择行为 → 向目标前进
         ↓
    开始检查敌舰 (每0.5-1秒)
         ↓
    敌舰在10000单位内? 
    否 → 继续轨道或隐形
    是 → 激活应急燃烧
       → 激活传感器脉冲
       → 清除分配任务
       → 执行逃离命令
       → 驶入超空间
       → 舰队消失
```

---

## 🔬 测试清单

部署后请进行以下测试：

### 基础功能测试
- [ ] 开始新游戏后65天，侦察队开始生成
- [ ] 侦察队能够正确导航到目标系统
- [ ] 侦察队在60天后停止生成
- [ ] 基地系统正确选择为蓝巨星系统
- [ ] 基地市场成功创建并属于Omega_Psychasthenia

### 进攻功能测试
- [ ] 守卫舰队逐步生成（最多10支）
- [ ] 全面进攻开始时显示后备据点警告
- [ ] 进攻舰队从主基地和后备据点定时出现
- [ ] 进攻目标为玩家或其他非Omega势力

### 配置测试
- [ ] 修改LunaSettings参数后游戏内生效
- [ ] 参数范围约束正常工作
- [ ] 超小值和超大值都能正常处理

### 性能测试
- [ ] 游戏加载速度正常
- [ ] 大量舰队生成时帧率可接受
- [ ] 内存使用在合理范围内
- [ ] DEV模式下日志输出完整清晰

---

## 📝 关键代码片段

### 侦察队智能行为（IIRT_Omega_ScoutAI.java）
```java
@Override
protected void pickNext() {
    float r = (float)Math.random();
    if (r < 0.4f) {
        fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, target, 10000f);
    } else if (r < 0.8f) {
        fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, target, 10000f);
    } else {
        fleet.getMemoryWithoutUpdate().set(
            MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF, true);
        fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, target, 10000f);
    }
}
```

### 侦察队逃离触发（IIRT_Omega_ScoutAI.java）
```java
float dist = Misc.getDistance(other.getLocation(), fleet.getLocation());
if (dist < 10000f) {
    if (fleet.getAbility("emergency_burn") != null) 
        fleet.getAbility("emergency_burn").activate();
    fleet.clearAssignments();
    fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, 
                       target, 10000f, "逃离");
    return;
}
```

### LunaLib参数读取（IIRT_Omega_Invasion.java）
```java
private Integer lunaGetInt(String modID, String fieldID) {
    try {
        Class<?> c = Class.forName("org.magiclib.LunaWrapper");
        java.lang.reflect.Method m = c.getMethod("getInt", 
                                                String.class, String.class);
        Object res = m.invoke(null, modID, fieldID);
        return (Integer) res;
    } catch (Throwable t) {
        return null;
    }
}
```

---

## 🚀 后续扩展建议

### 短期（可立即添加）
1. **基地破坏终止** - 允许玩家摧毁主基地直接结束入侵
2. **进度追踪** - 在游戏内显示入侵进度条
3. **舰队特化** - 不同阶段使用不同的舰队配置
4. **特殊事件** - 在某些条件下触发剧情事件

### 中期（下个版本）
1. **多基地支持** - 允许多个后备据点并行进攻
2. **自适应难度** - 根据玩家实力自动调整Omega威胁
3. **政治影响** - Omega入侵驱动派系关系变化
4. **防卫承诺** - 其他派系可承诺协助防卫

### 长期（大规模更新）
1. **战役地图** - 展示Omega控制区域和进攻路线
2. **特殊舰队** - Omega独有的旗舰和王牌舰队
3. **技术收获** - 击败Omega后解锁特殊武器或蓝图
4. **故事线** - 完整的入侵对抗故事和结局

---

## 📞 问题排查

### 侦察队不生成
- 检查: COLLECT_DATA阶段是否已启动
- 检查: `collect_data_time` 参数是否为0
- 检查: 系统中是否存在通会中继站

### 基地选择失败
- 检查: 是否存在蓝巨星系统
- 检查: 玩家是否进入了所有系统
- 检查: 系统是否已有Omega市场

### 参数不生效
- 检查: LunaLib是否已安装并启用
- 检查: LunaSettings.json是否有效JSON
- 检查: 参数值是否在允许范围内
- 尝试: 删除存档重新开始以加载新参数

### 舰队不出现
- 检查: 基地市场是否成功创建
- 检查: Omega_Psychasthenia阵营是否存在
- 检查: 舰队工厂是否能创建指定大小的舰队
- 尝试: 增加`DEBUG`日志检查详细错误信息

---

## ✨ 特色亮点总结

1. **完全可配置** - 9个参数可独立调整
2. **零硬依赖** - LunaLib完全可选（用反射）
3. **多层威胁** - 侦察→基地→守卫→全面进攻的递进
4. **智能AI** - 侦察队有多种行为和自卫逃离能力
5. **链式打击** - 主基地+后备据点的二线威胁
6. **完整通知** - 每个阶段都有清晰的玩家反馈
7. **易于扩展** - 模块化设计便于添加新功能

---

**实现日期：** 2026年5月  
**代码版本：** v1.0 (完全功能)  
**编译状态：** ✅ 无错误，仅有IDE警告  
**测试状态：** 需要游戏内验证  

---


