# 曲线保存与Alt取整修复（2026-10-21）

源码、临时编译与离线回归通过；实机待验收。新包 build/sol61-curve-save-20261021，包含上一批港务及E-01～06实现。未覆盖加载JAR。

## 原因与修改

LunaLib会缓存此前页签的ChangedSetting，点击保存设置时重新覆盖JSON。旧图形编辑器只写底层数据，未同步这些缓存，旧的“中等”选择会再次触发预设恢复，旧曲线字段也可能覆盖新值。

现在应用曲线显式写入自定义模式并同步本模组曲线、增长封顶和难度选择的待保存记录/可见控件。保留其他参数未保存修改和其他模组草稿。随后主动选择其他预设仍正常生效，切回自定义恢复保存曲线。未初始化时明确报错，避免假成功。

四张曲线支持Alt＋左键拖动四舍五入取整；正常拖动保留小数；军官仍限制2～20，其余值0～100。此操作只影响拖动点，不将所有控制点强制改为整数。

代码：src/data/scripts/PTSDDifficultySettings.java（applyCurves、syncLunaDraft）；PTSDDifficultyCurveEditor.java（processInput、dragValue）。

## 安装与快速验收

1. 退出游戏并备份现有JAR。若已使用上一版r2，替换本包jars中的JAR即可；较旧版本还需对照本包data及原测试包说明同步配套文件。不要覆盖自己的难度快照。重启游戏，使用测试档。
2. DEV模式进入LunaLib，先浏览“难度”和“自定义难度”页签，再进入“曲线编辑”。这一步特意制造曾导致覆盖的旧页签缓存。
3. 将侦察基础曲线首点调为5，增长率、资产产速和军官各改一个点，并改变一个增长封顶选项。
4. 点击图内“应用并保存”，再点LunaLib“保存设置”。离开该页签再回来、关闭设置再打开，确认所有修改保留。
5. 检查 saves/common/PTSD-custom-difficulty/latest.json.data 的 PTSD_curve_recon 等字段；对应历史快照应新增，不能只有UI变了而文件没变。
6. 选择“中等”并保存，确认恢复预设；选择“自定义”并保存，确认刚才的曲线回来。重启游戏再检查。
7. 按住Alt左键拖任意点，状态栏应显示整数数值（可能格式为5.000）；松开Alt继续拖动可产生小数。军官不能低于2或超过20。
8. 若先改了其他参数但未保存，应用曲线后再保存设置，那些修改仍应生效；其他模组的待保存参数不应被清除。

## 离线验证

DifficultyProfileTest增加了旧页签缓存→图形应用→Luna保存覆盖→重新读取→导出检查→预设切换/自定义恢复的回归，以及无关参数保留、Alt整数/正常小数和军官边界。DifficultyCurveTest验证曲线插值与边界。均通过，不能替代实际鼠标交互验收。

## 2026-10-21 IDEA依赖补齐

PTSDDifficultyCurveEditor使用的lunalib.lunaUI.panel.LunaBaseCustomPanelPlugin确实存在，但位于Lunalib-Kotlin.jar，OpenCustomPanelFromDialog位于LunaLib.jar。已在.idea/libraries/LunaLib.xml的CLASSES和SOURCES补充Lunalib-Kotlin.jar。当前modules.xml启用的Omega Psychasthenia.iml已引用该LunaLib库，无需改包名或改用反射。javap核实实际签名、XML路径检查和临时编译通过；IDEA须重新加载项目配置。未覆盖游戏JAR。
