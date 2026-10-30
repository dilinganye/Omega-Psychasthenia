> 2026-10-19 更正：自动快照改为游戏目录 saves/common/PTSD-custom-difficulty/latest.json.data 与 history/*.json.data。下文旧模组目录路径已废弃，详见 CURVE_EDITOR_FIX_TESTING.md。

# 军官等级曲线测试包

退出游戏后备份现用JAR、LunaSettings.csv和PTSD_difficulty_presets.json，将本包jars与data内文件对应替换。加载JAR未自动覆盖。本包包含此前三曲线、新闻入口与风暴侦察修改。

## 变更
LunaLib战役内DEV“曲线编辑”新增第四张图：军官等级。曲线为绝对等级，2～20，平滑采样后四舍五入；不叠加旧基础、加成、阶段额外等级或±2随机浮动。只影响新生成军官，现有军官不追溯重建。
四档预设初始值：沉寂8、侦察8、营建9、要塞10、动员11、战争12，各阶段暂为水平线，可自由改成多点曲线。
旧PTSD_omega_officer_level_bonus从CSV、默认预设、读取逻辑与舰队预算影响中移除；历史Luna存储中残留的键不再参与计算或新快照。
曲线ID：PTSD_curve_officer_dormant/recon/expansion/fortification/mobilization/war。
默认配置：data/config/ptsdCSV/PTSD_difficulty_presets.json。
自定义保存：custom-difficulty/latest.json与history目录。

## 测试
1. 载入战役并开启DEV，打开LunaLib→精神创伤→曲线编辑。确认第四张图和底部保存按钮可滚动到达，旧军官加成项已消失。
2. 侦察军官曲线设2,10,20，应用；将RECON进度分别置于0、50%、100%，新生成舰队军官应为2、10、20级。同一进度多次生成不再上下浮动。
3. 切换营建阶段，默认新生军官为9级，不沿用侦察末值。原舰队军官应保持原级。
4. 检查快照包含军官曲线且不含旧设置；切预设再切自定义，确认曲线恢复。
5. 在数值栏输入0,30应保留上次合法值并记录警告，不生成越界军官。

编译及DifficultyProfileTest、DifficultyCurveTest、ReconFleetCompositionTest、LunaSettingsFormatTest通过。实际Luna布局、游戏内舰队生成仍待实测。
