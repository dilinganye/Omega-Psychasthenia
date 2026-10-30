# 2026-10-26 新闻权重与实施覆盖表

2026-10-29更新：174条有效CSV逐条登记；P-10完成三路线独立现场后恢复为0.8。现有50条暂停正常投放，33条填写正倍率，其余91条沿用空白=1。倍率乘原权重，并非绝对发生概率；条件入口乘原优先级。

0权重保留原文、编号与DEV定向入口；不清理旧存档已接取调查。恢复条件：先确认独有大地图操作，完成对应实体/生命周期/结算，再配置非零倍率并完成测试。WAR专属行不改；ALL行仍按统一倍率影响其各阶段。

|编号|标题|阶段|当前模板|原表同质化判断|倍率|本批状态 / 待办|
|---|---|---|---|---|---|---|
|N-01|标签错误|ALL|原入口|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|N-02|争抢残骸|ALL|原入口|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|N-03|短暂失控|ALL|原入口|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|N-04|错误广播|ALL|原入口|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|N-05|电台情歌|ALL|原入口|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|N-06|重复浮标|ALL|原入口|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|N-07|掷出窗外|ALL|原入口|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|N-08|一次交易|ALL|原入口|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|N-09|大型打捞|ALL|原入口|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|N-10|野史|ALL|原入口|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|N-11|粗制滥造|ALL|原入口|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|D-01|被动翘班|DORMANT / RECON|WRECK_RECONSTRUCTION|高|0.6|保留既有差分并调低/整理投放；实机待验|
|D-02|赏金取消|DORMANT / RECON|WRECK_RECONSTRUCTION|高|0.3|保留既有差分并调低/整理投放；实机待验|
|D-03|校验错误|DORMANT / RECON|RELAY_PARASITE|高|0.45|保留既有差分并调低/整理投放；实机待验|
|D-04|空舱漂流|DORMANT / RECON|CREW_MISSING / DISTORTION|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|D-05|未知回波|DORMANT / RECON|DISTORTION|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|D-06|信号不好|DORMANT / RECON|ROUTE / COMMUNICATION|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|D-07|静默打捞|DORMANT / RECON|WRECK_RECONSTRUCTION|高|0.25|保留既有差分并调低/整理投放；实机待验|
|D-08|未知突袭|DORMANT / RECON|CREW_MISSING|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|D-09|余晖游荡|DORMANT / RECON|DISTORTION|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|D-10|超高延迟|DORMANT / RECON|COMMUNICATION|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|D-11|错误识别|DORMANT / RECON|COMMUNICATION / DISTORTION|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|D-12|静默打捞|DORMANT / RECON|WRECK_RECONSTRUCTION|高|0.5|保留既有差分并调低/整理投放；实机待验|
|P-01|遇袭|RECON / EXPANSION|WRECK_RECONSTRUCTION|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|P-02|火控照射|RECON / EXPANSION|RECON_FIRE_CONTROL|极高|1|本批：真实火控试探、迫近反应|
|P-03|战区|RECON / EXPANSION|WRECK_RECONSTRUCTION|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|P-04|无故而降|RECON|RECON_SURRENDER|中|0.8|本批：真实弃械舰队撤离与拦截|
|P-05|战争火苗|RECON / EXPANSION|WRECK_RECONSTRUCTION|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|P-06|错误增援|RECON|ROUTE_DIVERSION|中|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|P-07|传奇终止|RECON|MISSING_CONVOY|中|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|P-08|跃迁崩坏|RECON|JUMP_BLOCKADE|高|1.2|本批：真实双向跃迁封锁、超晶格、安全实体化、原路恢复|
|P-09|大断电|RECON|STATION_BLACKOUT|极高|0.8|本批：真实空间站70秒电子压制、可打断|
|P-10|武器残片|RECON / EXPANSION|独立P10Event，模板仅作分类|高（旧实现）|0.8|2026-10-29：抢先回收/隐蔽尾随实体交接/截击载运队；差异证据、一次追索及真实撤离，离线通过，实机待验|
|P-11|疑似遇袭|RECON / EXPANSION|WRECK_RECONSTRUCTION|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|P-12|隐秘信号|RECON / EXPANSION|DARK_TRANSIT|高|0.8|本批：可见且隐蔽的真实舰队跟踪|
|P-13|频道空白|RECON|RELAY_PARASITE|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|E-01|测绘失败|EXPANSION|MISSING_CONVOY|中|0.8|保留既有差分并调低/整理投放；实机待验|
|E-02|疑似施工|EXPANSION / FORTIFICATION|FACILITY_INTERDICTION|中|1|保留既有差分并调低/整理投放；实机待验|
|E-03|光谱漂移|EXPANSION / FORTIFICATION|SPATIAL_ECHO|高|0.35|保留既有差分并调低/整理投放；实机待验|
|E-04|中继致盲|EXPANSION / FORTIFICATION|RELAY_PARASITE|高|0.35|保留既有差分并调低/整理投放；实机待验|
|E-05|海盗亡巢|EXPANSION|WRECK_RECONSTRUCTION|高|0.8|保留已实现账簿争夺，实机待验|
|E-06|边境冲突|EXPANSION|WRECK_RECONSTRUCTION|中|1|保留已实现巡逻救援与记录回收，实机待验|
|E-07|航道警告|EXPANSION|COMMUNICATION|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|F-01|规则脉冲|FORTIFICATION|FORT_GRAVITY_SURVEY|极高|0.5|保留既有差分并调低/整理投放；实机待验|
|F-02|禁止航行|FORTIFICATION|ROUTE_DIVERSION|中|0.7|保留既有差分并调低/整理投放；实机待验|
|F-03|保费之外|FORTIFICATION|ROUTE_REVERSAL|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|F-04|边境换防|FORTIFICATION|MISSING_CONVOY|中|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|F-05|夜空弧线|FORTIFICATION|SPATIAL_ECHO|极高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|F-06|联合会议|FORTIFICATION|FORT_SUPPLY_WINDOW|中|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|F-07|未知传言|FORTIFICATION|COMMUNICATION|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|F-08|未知传言|FORTIFICATION|COMMUNICATION|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|FT-01|许可暂缓|FORTIFICATION|原入口|低|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|FT-02|这可没有|FORTIFICATION|原入口|中|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|FT-03|超级闷棍|FORTIFICATION|原入口|低|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|FT-04|母星外的等待|FORTIFICATION|原入口|低|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|FT-05|幸存归来|FORTIFICATION|原入口|中|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|FT-06|边界事发|FORTIFICATION|原入口|低|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|FT-07|航路索赔|FORTIFICATION|原入口|低|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|FT-08|倒爷的座|FORTIFICATION|原入口|低|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|FT-09|欢喜冤家-事件|FORTIFICATION|原入口|中|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|FT-10|一次临时定义|FORTIFICATION / MOBILIZATION|原入口|低|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|W-01|新闻属实|WAR|BATTLE_AFTERMATH / ROUTE|高|1（空白）|WAR专属：本批暂跳过|
|W-02|准备撤离|WAR|CREW_MISSING / ROUTE|高|1（空白）|WAR专属：本批暂跳过|
|W-03|疑似胜利|WAR|BATTLE_AFTERMATH|高|1（空白）|WAR专属：本批暂跳过|
|W-04|信号毁坏|WAR|CREW_MISSING / BATTLE_AFTERMATH|高|1（空白）|WAR专属：本批暂跳过|
|W-05|信道崩坏|WAR|COMMUNICATION / DISTORTION|高|1（空白）|WAR专属：本批暂跳过|
|W-06|海盗联盟|WAR|BATTLE_AFTERMATH / ROUTE|高|1（空白）|WAR专属：本批暂跳过|
|W-07|未知立场|WAR|FACILITY / DISTORTION|高|1（空白）|WAR专属：本批暂跳过|
|W-08|阵线崩溃|WAR|BATTLE_AFTERMATH / ROUTE|高|1（空白）|WAR专属：本批暂跳过|
|W-09|难民潮|WAR|CREW_MISSING / ROUTE|高|1（空白）|WAR专属：本批暂跳过|
|W-10|严禁打捞|WAR|BATTLE_AFTERMATH / DISTORTION|高|1（空白）|WAR专属：本批暂跳过|
|W-11|战时状态|WAR|BATTLE_AFTERMATH / DISTORTION|低|1（空白）|WAR专属：本批暂跳过|
|N-CUSTOM-01|没油逃犯|ALL|原入口|中|1（空白）|保留既有专属实现/规则；本批不改|
|F-HEG-01|空白征令|ALL|PORT_RECORDS|极高|0.15|保留既有差分并调低/整理投放；实机待验|
|F-HEG-02|退役服役|ALL|原入口|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|F-TT-01|死亡与赋税|ALL|PORT_RECORDS|低|1（空白）|保留既有专属实现/规则；本批不改|
|F-TT-02|购买绩效|ALL|原入口|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|F-LC-01|朝圣迟到|ALL|SPATIAL_ECHO|极高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|F-LC-02|三祝谷物|ALL|原入口|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|F-LP-01|布道不止|ALL|ABANDONED_FACILITY|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|F-LP-02|反应堆售罄|ALL|原入口|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|F-PIR-01|悬赏自己|ALL|ABANDONED_FACILITY|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|F-PIR-02|九九新|ALL|原入口|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|F-PL-01|延期表决|ALL|PORT_RECORDS|极高|0.15|保留既有差分并调低/整理投放；实机待验|
|F-PL-02|战争债券|ALL|原入口|低|1（空白）|纯报道/原自有事件保持；不强制调查化|
|JE-01|失声航路|ALL|RELAY_PARASITE / SIGNAL_PURSUIT|极高|0.15|保留既有差分并调低/整理投放；实机待验|
|MB-01|红色草案|MOBILIZATION|原入口|高|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|MB-02|七处求援|MOBILIZATION|原入口|高|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|MB-03|末班离港|MOBILIZATION|原入口|中|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|MB-04|航道让行|MOBILIZATION|原入口|中|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|MB-05|和平拨款|MOBILIZATION|原入口|低|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-01|名单失准|WAR|原入口|高|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-02|军需空单|WAR|原入口|高|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-03|双印争签|WAR|原入口|高|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-04|捷报待核|WAR|原入口|高|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-05|补给候签|WAR|原入口|高|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-06|孤星求援|WAR|原入口|高|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-07|远征获准|WAR|原入口|高|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-08|空域失守|WAR|原入口|中|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-09|名单有名|WAR|原入口|高|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-10|断港留账|WAR|原入口|低|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-11|伤船候准|WAR|原入口|低|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-12|商路遇袭|WAR|原入口|低|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-13|损报相左|WAR|原入口|低|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-14|外围告急|WAR|原入口|高|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-15|视界迁移|WAR|原入口|中|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-16|视界危急|WAR|原入口|中|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-GS-01|席位待签|WAR|原入口|高|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-GS-02|后备启程|WAR|原入口|高|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-GS-03|供货缺页|WAR|原入口|高|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-GS-04|越级获签|WAR|原入口|高|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|WR-JE-01|无章勘验|WAR|原入口|极高|1（空白）|原固定链保留；不以普通概率或通用现场替换|
|R2-01|中继重放|RECON|RELAY_PARASITE|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|R2-02|航图删改|RECON|ROUTE_REVERSAL|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|R2-03|港口漏登|RECON|PORT_RECORDS|极高|0.25|保留既有差分并调低/整理投放；实机待验|
|R2-04|静默拖船|RECON|MISSING_CONVOY|中|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|R2-05|假求救|RECON|SIGNAL_PURSUIT|极高|0.4|保留既有差分并调低/整理投放；实机待验|
|R2-06|上传浮标|RECON|SIGNAL_PURSUIT|极高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|R2-07|踪迹标记|RECON|ROUTE_REVERSAL|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|R2-08|燃料购买异常|RECON|PORT_RECORDS|极高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|R2-09|跃迁点假灯|RECON|ROUTE_REVERSAL|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|R2-10|星图批注|RECON|RELAY_PARASITE|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|R2-11|好死不送|RECON|ABANDONED_FACILITY|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|R2-12|倒填关税单|RECON|PORT_RECORDS|极高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|R2-13|传感器盲扫|RECON|SPATIAL_ECHO|极高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|R2-14|短促火力测试|RECON|WRECK_RECONSTRUCTION|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|R2-15|伪造气象记录|RECON|SPATIAL_ECHO|极高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|E2-01|残骸索价|EXPANSION|原入口|低|1（空白）|纯报道；不启用旧E2现场链|
|E2-02|临行改约|EXPANSION|原入口|低|1（空白）|纯报道；不启用旧E2现场链|
|E2-03|一船双号|EXPANSION|原入口|低|1（空白）|纯报道；不启用旧E2现场链|
|E2-04|悬赏叠贴|EXPANSION|原入口|低|1（空白）|纯报道；不启用旧E2现场链|
|E2-05|明日两价|EXPANSION|原入口|低|1（空白）|纯报道；不启用旧E2现场链|
|E2-06|空泊有主|EXPANSION|原入口|低|1（空白）|纯报道；不启用旧E2现场链|
|E2-07|归船失保|EXPANSION|原入口|低|1（空白）|纯报道；不启用旧E2现场链|
|E2-08|决赛跳票|EXPANSION|原入口|低|1（空白）|纯报道；不启用旧E2现场链|
|E2-09|替身疑照|EXPANSION|原入口|低|1（空白）|纯报道；不启用旧E2现场链|
|E2-10|彩带入舱|EXPANSION|原入口|低|1（空白）|纯报道；不启用旧E2现场链|
|E2-11|疑罪从有|EXPANSION|原入口|低|1（空白）|纯报道；不启用旧E2现场链|
|E2-12|持械领号|EXPANSION|原入口|低|1（空白）|纯报道；不启用旧E2现场链|
|E2-13|安全副驾|EXPANSION|原入口|低|1（空白）|纯报道；不启用旧E2现场链|
|E2-14|空间断拍|EXPANSION|原入口|低|1（空白）|纯报道；不启用旧E2现场链|
|E2-15|辐峰无证|EXPANSION|原入口|低|1（空白）|纯报道；不启用旧E2现场链|
|F2-01|你不许说|FORTIFICATION|FORT_GRAVITY_SURVEY|极高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|F2-02|你不许说|FORTIFICATION|FORT_PATROL_GAP|中|0.8|本批：真实巡防窗口与封口响应|
|F2-03|护航错拍|FORTIFICATION|FORT_SUPPLY_WINDOW|中|1|保留既有差分并调低/整理投放；实机待验|
|F2-04|旧路无声|FORTIFICATION|FORT_DECOY_TRANSIT|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|F2-05|黑洞回声|FORTIFICATION|SIGNAL_PURSUIT|极高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|F2-06|黑洞回声|FORTIFICATION|FORT_SALVAGE_INTERCEPTION|中|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|F2-07|双重日期|FORTIFICATION|SIGNAL_PURSUIT|极高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|F2-08|删除之影|FORTIFICATION|FORT_DECOY_TRANSIT|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|F2-09|恢复正常|FORTIFICATION|FORT_PATROL_GAP|中|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|F2-10|引力失拍|FORTIFICATION|FORT_GRAVITY_SURVEY|极高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|F2-11|航路销签|FORTIFICATION|PORT_RECORDS|极高|0.2|保留既有差分并调低/整理投放；实机待验|
|F2-12|未知单元|FORTIFICATION|TRAFFIC_OBSERVATION|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|F2-13|武装打捞|FORTIFICATION|FORT_SALVAGE_INTERCEPTION|中|1|保留既有差分并调低/整理投放；实机待验|
|F2-14|双口同影|FORTIFICATION|FORT_DECOY_TRANSIT|高|0.8|保留既有差分并调低/整理投放；实机待验|
|F2-15|许可未至|FORTIFICATION|PORT_RECORDS|极高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|SITE-01|航迹折返|DORMANT / RECON|ROUTE_REVERSAL|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|SITE-02|空船引路|DORMANT / RECON|ROUTE_DIVERSION|中|0.8|保留既有差分并调低/整理投放；实机待验|
|SITE-03|重放余音|RECON|RELAY_PARASITE|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|SITE-04|无人应答|RECON|SIGNAL_PURSUIT|极高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|SITE-05|残骸异向|RECON / EXPANSION|WRECK_RECONSTRUCTION|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|SITE-06|拖船失联|RECON / EXPANSION|MISSING_CONVOY|中|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|SITE-07|疑似光照|EXPANSION / FORTIFICATION|FACILITY_INTERDICTION|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|SITE-08|秒针三响|RECON / EXPANSION|SPATIAL_ECHO|极高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|SITE-09|货影交班|FORTIFICATION|FORT_SUPPLY_WINDOW|中|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|SITE-10|散点同向|FORTIFICATION|FORT_SENSOR_LATTICE|极高|1|保留既有差分并调低/整理投放；实机待验|
|SITE-11|巡迹间断|FORTIFICATION|FORT_PATROL_GAP|中|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|SITE-12|双口异重|FORTIFICATION|FORT_DECOY_TRANSIT|高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|SITE-13|引力错拍|FORTIFICATION|FORT_GRAVITY_SURVEY|极高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|SITE-14|打捞求援|FORTIFICATION|FORT_SALVAGE_INTERCEPTION|中|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|JE-02|回声追索|RECON / EXPANSION|SIGNAL_PURSUIT|极高|0.3|保留既有差分并调低/整理投放；实机待验|
|JE-03|峰值之外|FORTIFICATION / MOBILIZATION|FORT_GRAVITY_SURVEY|极高|0|暂禁：共用采样/展示或相近机制尚未专属化；保留DEV，独特玩法验收后再恢复|
|JE-04|残录争夺|FORTIFICATION / MOBILIZATION|FORT_SALVAGE_INTERCEPTION|高|0.2|保留既有差分并调低/整理投放；实机待验|

## 暂禁编号（51条）

P-01, P-03, P-05, P-06, P-07, P-10, P-11, P-13, F-03, F-04, F-05, F-06, F-LC-01, F-LP-01, F-PIR-01, R2-01, R2-02, R2-04, R2-06, R2-07, R2-08, R2-09, R2-10, R2-11, R2-12, R2-13, R2-14, R2-15, F2-01, F2-04, F2-05, F2-06, F2-07, F2-08, F2-09, F2-10, F2-12, F2-15, SITE-01, SITE-03, SITE-04, SITE-05, SITE-06, SITE-07, SITE-08, SITE-09, SITE-11, SITE-12, SITE-13, SITE-14, JE-03

注：原审计表是设计输入，不是实现完成证明。本表“保留”不意味着已通过游戏测试；新实现、既有实现、暂禁分别按状态核对。
