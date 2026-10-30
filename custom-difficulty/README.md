# 自定义难度保存位置（2026-10-19 修正）

游戏脚本不能直接写入模组目录，旧实现被游戏安全检查拒绝。自动快照改用原版保存接口，实际位置为：

- `D:/Game/Starsector_098/saves/common/PTSD-custom-difficulty/latest.json.data`
- `D:/Game/Starsector_098/saves/common/PTSD-custom-difficulty/history/时间戳-标识.json.data`

`.data` 是游戏自动添加的后缀，文件内容仍是可读JSON，包括非DEV隐藏的完整难度参数。每次自定义参数实际变化并保存后创建历史；无变化保存不会新增。

本目录原有 `latest.json` 和 `history` 是此前手动恢复的文件，保留供参考，不再是自动更新位置。新位置在安装修复版本、进入战役并保存自定义参数后生成。

将某份快照的JSON对象复制到 `data/config/ptsdCSV/PTSD_difficulty_presets.json` 中指定难度的对象内，即可更新该预设；不要用平面快照直接覆盖包含四个难度名称的整个文件。
