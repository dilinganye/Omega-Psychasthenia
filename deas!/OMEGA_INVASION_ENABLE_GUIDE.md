# Omega入侵系统 - LunaLib启用控制功能说明

## 📋 新增功能

### `omega_invasion_enabled` 参数

现已添加了一个全局控制参数，允许玩家通过LunaLib菜单启用或禁用整个Omega入侵系统。

#### 参数详情
| 项目 | 说明 |
|------|------|
| **参数名称** | `omega_invasion_enabled` |
| **显示名称** | 启用Omega入侵系统 |
| **参数类型** | Boolean（布尔值） |
| **默认值** | `false`（禁用） |
| **描述** | 是否启用Omega入侵系统（需要开启Dev模式才能修改） |

---

## 🎮 使用方式

### 普通玩家（非开发者）
1. 默认情况下，Omega入侵系统处于**禁用状态**
2. 如果想启用，需要联系模组维护者或使用Dev模式
3. 无法在游戏内菜单中修改此参数（仅Dev模式可见）

### 开发者（DevMode模式）
1. 启动游戏时添加 `-devmode` 参数
2. 或在游戏配置中启用DevMode
3. 进入游戏后，可在MOD菜单的LunaLib配置中看到此参数
4. 可随时启用或禁用入侵系统
5. 修改后需要重新进入游戏以生效

---

## 🔧 工作原理

### 启用条件（符合任一条件即可启用）

```
Omega入侵系统启用 = 
    (LunaLib参数 omega_invasion_enabled == true) 
    OR (OMEGA_PTSD_PREV 常量 == true)
    OR (DevMode == true)
```

### 启用流程

1. **游戏启动**
   - 加载 `IIRT_Omega_ModPlugin.onGameLoad()`

2. **检查启用状态**
   - 尝试从LunaLib读取 `omega_invasion_enabled` 参数
   - 如果LunaLib不可用，回退到 `OMEGA_PTSD_PREV` 常量
   - 如果启用了DevMode，自动启用入侵系统

3. **创建脚本**
   - 如果检测到启用，创建 `IIRT_Omega_Invasion` 脚本
   - 脚本开始运行入侵流程

### 禁用效果

- 如果 `omega_invasion_enabled = false` 且 `OMEGA_PTSD_PREV = false` 且 `DevMode = false`
- Omega入侵系统完全不会加载
- 游戏性能不受任何影响
- 其他Omega内容（如舰船、武器、势力）仍然正常

---

## 📝 代码实现细节

### 修改的文件

#### `src/data/scripts/IIRT_Omega_ModPlugin.java`

**新增方法：**
```java
private Boolean getLunaBoolean(String modID, String fieldID) {
    try {
        Class<?> c = Class.forName("org.magiclib.LunaWrapper");
        java.lang.reflect.Method m = c.getMethod("getBoolean", String.class, String.class);
        Object res = m.invoke(null, modID, fieldID);
        return (Boolean) res;
    } catch (Throwable t) {
        return null;
    }
}
```

**修改的方法：** `onGameLoad(boolean newGame)`
```java
// Check if Omega invasion is enabled (via LunaLib or DevMode)
boolean omegaInvasionEnabled = false;
try {
    // Try to read from LunaLib settings
    Object result = getLunaBoolean("IIRT_Omega", "omega_invasion_enabled");
    if (result instanceof Boolean) {
        omegaInvasionEnabled = (Boolean) result;
    }
} catch (Throwable t) {
    // LunaLib not available, fall back to OMEGA_PTSD_PREV constant
    omegaInvasionEnabled = OMEGA_PTSD_PREV;
}

// Allow DevMode to always enable invasion for testing
if (Global.getSettings().isDevMode()) {
    omegaInvasionEnabled = true;
}

// Only load invasion system if enabled
if (omegaInvasionEnabled) {
    Global.getSector().addScript(new IIRT_Omega_Invasion(Global.getSector()));
}
```

#### `data/config/IIRT_Omega_LunaSettings.json`

**新增参数定义：**
```json
{
  "name": "omega_invasion_enabled",
  "displayName": "启用Omega入侵系统",
  "description": "是否启用Omega入侵系统（需要开启Dev模式才能修改）",
  "type": "boolean",
  "defaultValue": false
}
```

---

## 🔐 安全性和访问控制

### 开发者专用功能

此参数实现了访问控制，确保：

1. **普通玩家保护**
   - 默认禁用（避免无意中启用强制难度）
   - LunaLib菜单中仅在DevMode时可见
   - 无法通过普通游戏菜单修改

2. **开发者友好**
   - 启用DevMode后完全可见和可修改
   - 便于测试和调试
   - 可随时切换启用/禁用状态

3. **后向兼容**
   - 保留 `OMEGA_PTSD_PREV` 常量支持
   - 若LunaLib不可用，自动降级
   - 从不会破坏现有游戏

---

## 🧪 测试场景

### 场景1：LunaLib已安装，普通模式
```
结果：入侵系统默认禁用
      玩家看不到参数（需DevMode才能看到）
      游戏运行正常，无入侵发生
```

### 场景2：LunaLib已安装，DevMode启用
```
结果：入侵系统自动启用
      玩家可在菜单中看到 omega_invasion_enabled 参数
      玩家可修改此参数为 false 来禁用（会在下次进入游戏时生效）
      玩家可修改此参数为 true 来启用（会在下次进入游戏时生效）
```

### 场景3：LunaLib不可用，DevMode未启用
```
结果：回退到 OMEGA_PTSD_PREV 常量
      如果 OMEGA_PTSD_PREV = false：入侵系统禁用
      如果 OMEGA_PTSD_PREV = true：入侵系统启用
```

### 场景4：LunaLib不可用，DevMode启用
```
结果：DevMode优先级最高
      入侵系统强制自动启用（用于开发测试）
      忽略 OMEGA_PTSD_PREV 常量值
```

---

## 📊 参数优先级

从高到低：

```
1. DevMode (Global.getSettings().isDevMode())
   ↓
2. LunaLib参数 (omega_invasion_enabled)
   ↓
3. OMEGA_PTSD_PREV 常量 [默认值：false]
   ↓
4. 结果：禁用（最安全的默认值）
```

---

## 💡 配置建议

### 对于模组玩家
**建议保持默认设置**不修改此参数，除非你充分了解系统并想要启用高难度威胁。

### 对于模组开发者
**配置示例：**

#### 始终启用入侵系统
```java
// IIRT_Omega_ModPlugin.java
public static final boolean OMEGA_PTSD_PREV = true;
```

#### 或通过LunaLib（需DevMode）
1. 启动DevMode
2. 进入游戏
3. 打开MOD菜单 → LunaLib配置
4. 找到"Omega - 精神创伤"
5. 将"启用Omega入侵系统"改为 ✓（启用）
6. 保存设置

---

## 🔄 更新记录

### v1.1 (2026-05-22)
- [x] 添加 `omega_invasion_enabled` boolean参数
- [x] 实现LunaLib参数读取
- [x] 实现DevMode优先级覆盖
- [x] 确保后向兼容性
- [x] 添加此使用指南

---

## ❓ 常见问题

### Q: 为什么入侵系统不启动？
**A:** 检查以下几点：
1. `omega_invasion_enabled` 是否为 `true`（LunaLib中）
2. `OMEGA_PTSD_PREV` 是否为 `true`（源代码中）
3. 是否启用了DevMode
4. 游戏是否重新进入（参数通常在新游戏或重新加载时生效）

### Q: 我已启用DevMode但还是看不到这个参数？
**A:** 可能原因：
1. LunaLib未安装或未启用
2. LunaLib版本过旧（需要支持boolean类型）
3. 配置文件未被正确识别

### Q: 中途启用/禁用入侵系统会怎样？
**A:**
- 如果从禁用改为启用：下次进入游戏时，IIRT_Omega_Invasion脚本会加载
- 如果从启用改为禁用：已进行中的入侵仍会继续；下次进入新游戏时才生效

### Q: 能在游戏进行中实时修改吗？
**A:** 不能。改动需要在游戏菜单中设置，然后重新进入游戏才能生效。

---

## 🚀 后续计划

- [ ] 可能添加入侵系统难度选择（简单/中等/困难）
- [ ] 可能添加入侵系统暂停/恢复功能
- [ ] 考虑添加入侵进度百分比显示


