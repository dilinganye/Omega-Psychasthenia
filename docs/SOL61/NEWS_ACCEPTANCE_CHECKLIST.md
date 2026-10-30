> 2026-10-21补充：E-05/E-06现已接入账簿争夺、巡逻救援与记录回收的三分支；离线通过、实机未验。见PORT_EXPANSION_20261021.md。

> 2026-10-21：七项港务与 E-01～04已补专属流程，源码/离线通过，实机仍未测；E2全部保持纯报道。详见 [本批覆盖与测试](PORT_EXPANSION_20261021.md)。

# 全量新闻逐条验收登记表

基准：2026-10-18 当前新闻 CSV，共 174 条有效记录。此表由当前配置生成；所有结果初始为未测，生成表不代表事件通过。

操作方法见 [完整手册](FULL_ACCEPTANCE_GUIDE.md)。每条先按 delivery/JeOnly 找入口；CONDITIONAL 缺前置应记录等待，SCRIPTED 必须走原链，不能把强制投放成功当作剧情完成。保护原文不因本次测试而重写。

每条通过必须核对：标题/来源/报道 → 地点与实体是否相符 → 可调查时真假结果 → 选择实际后果 → 结束与清理。纯报道标“现场不适用”，不要求造现场。多模板必须分别记录实际抽到的模板；没有跑到的分支标未测。

|ID|标题|阶段|入口 / JeOnly|目标与前置|模板 / 处理器|结果登记|
|---|---|---|---|---|---|---|
|N-01|标签错误|ALL|AMBIENT / FALSE|POPULATED ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|N-02|争抢残骸|ALL|AMBIENT / FALSE|EDGE_MARKET ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|N-03|短暂失控|ALL|AMBIENT / FALSE|POPULATED ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|N-04|错误广播|ALL|AMBIENT / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|N-05|电台情歌|ALL|AMBIENT / FALSE|PIRATE ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|N-06|重复浮标|ALL|AMBIENT / FALSE|WILDERNESS ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|N-07|掷出窗外|ALL|AMBIENT / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|N-08|一次交易|ALL|AMBIENT / FALSE|MILITARY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|N-09|大型打捞|ALL|AMBIENT / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|N-10|野史|ALL|AMBIENT / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|N-11|粗制滥造|ALL|AMBIENT / FALSE|PIRATE ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|D-01|被动翘班|DORMANT / RECON|AMBIENT / FALSE|EDGE_MARKET ; |WRECK_RECONSTRUCTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|D-02|赏金取消|DORMANT / RECON|AMBIENT / FALSE|PIRATE ; |WRECK_RECONSTRUCTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|D-03|校验错误|DORMANT / RECON|AMBIENT / FALSE|RELAY ; |RELAY_PARASITE ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|D-04|空舱漂流|DORMANT / RECON|AMBIENT / FALSE|ANY ; |CREW_MISSING / DISTORTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|D-05|未知回波|DORMANT / RECON|AMBIENT / FALSE|ANY ; |DISTORTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|D-06|信号不好|DORMANT / RECON|AMBIENT / FALSE|RELAY ; |ROUTE / COMMUNICATION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|D-07|静默打捞|DORMANT / RECON|AMBIENT / FALSE|ANY ; |WRECK_RECONSTRUCTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|D-08|未知突袭|DORMANT / RECON|AMBIENT / FALSE|PIRATE ; |CREW_MISSING ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|D-09|余晖游荡|DORMANT / RECON|AMBIENT / FALSE|POPULATED ; |DISTORTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|D-10|超高延迟|DORMANT / RECON|AMBIENT / FALSE|POPULATED ; |COMMUNICATION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|D-11|错误识别|DORMANT / RECON|AMBIENT / FALSE|POPULATED ; |COMMUNICATION / DISTORTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|D-12|静默打捞|DORMANT / RECON|AMBIENT / FALSE|WILDERNESS ; |WRECK_RECONSTRUCTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|P-01|遇袭|RECON / EXPANSION|AMBIENT / FALSE|MILITARY ; |WRECK_RECONSTRUCTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|P-02|火控照射|RECON / EXPANSION|AMBIENT / FALSE|MILITARY ; |SPATIAL_ECHO ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|P-03|战区|RECON / EXPANSION|AMBIENT / FALSE|EDGE_MARKET ; |WRECK_RECONSTRUCTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|P-04|无故而降|RECON|AMBIENT / FALSE|PIRATE ; |SURRENDERED_FLEET ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|P-05|战争火苗|RECON / EXPANSION|AMBIENT / FALSE|MILITARY ; |WRECK_RECONSTRUCTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|P-06|错误增援|RECON|AMBIENT / FALSE|POPULATED ; |ROUTE_DIVERSION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|P-07|传奇终止|RECON|AMBIENT / FALSE|MILITARY ; |MISSING_CONVOY ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|P-08|跃迁崩坏|RECON|AMBIENT / FALSE|RELAY ; |ROUTE_REVERSAL ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|P-09|大断电|RECON|AMBIENT / FALSE|POPULATED ; |SPATIAL_ECHO ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|P-10|武器残片|RECON / EXPANSION|AMBIENT / FALSE|ANY ; |WRECK_RECONSTRUCTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|P-11|疑似遇袭|RECON / EXPANSION|AMBIENT / FALSE|EDGE_MARKET ; |WRECK_RECONSTRUCTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|P-12|隐秘信号|RECON / EXPANSION|AMBIENT / FALSE|RELAY ; |TRAFFIC_OBSERVATION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|P-13|频道空白|RECON|AMBIENT / FALSE|RELAY ; |RELAY_PARASITE ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E-01|测绘失败|EXPANSION|AMBIENT / FALSE|WILDERNESS ; |MISSING_CONVOY ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E-02|疑似施工|EXPANSION / FORTIFICATION|AMBIENT / FALSE|WILDERNESS ; |FACILITY_INTERDICTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E-03|光谱漂移|EXPANSION / FORTIFICATION|AMBIENT / FALSE|POPULATED ; |SPATIAL_ECHO ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E-04|中继致盲|EXPANSION / FORTIFICATION|AMBIENT / FALSE|RELAY ; |RELAY_PARASITE ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E-05|海盗亡巢|EXPANSION|AMBIENT / FALSE|PIRATE ; |WRECK_RECONSTRUCTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E-06|边境冲突|EXPANSION|AMBIENT / FALSE|MILITARY ; |WRECK_RECONSTRUCTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E-07|航道警告|EXPANSION|AMBIENT / FALSE|MILITARY ; |COMMUNICATION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F-01|规则脉冲|FORTIFICATION|AMBIENT / FALSE|WILDERNESS ; |FORT_GRAVITY_SURVEY ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F-02|禁止航行|FORTIFICATION|AMBIENT / FALSE|RELAY ; |ROUTE_DIVERSION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F-03|保费之外|FORTIFICATION|AMBIENT / FALSE|EDGE_MARKET ; |ROUTE_REVERSAL ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F-04|边境换防|FORTIFICATION|AMBIENT / FALSE|MILITARY ; |MISSING_CONVOY ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F-05|夜空弧线|FORTIFICATION|AMBIENT / FALSE|POPULATED ; |SPATIAL_ECHO ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F-06|联合会议|FORTIFICATION|AMBIENT / FALSE|ANY ; |FORT_SUPPLY_WINDOW ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F-07|未知传言|FORTIFICATION|AMBIENT / FALSE|ANY ; |COMMUNICATION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F-08|未知传言|FORTIFICATION|AMBIENT / FALSE|ANY ; |COMMUNICATION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|FT-01|许可暂缓|FORTIFICATION|SCRIPTED / FALSE|CUSTOM(data.scripts.campaign.invasion.news.ft.FT01Handler) ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|FT-02|这可没有|FORTIFICATION|SCRIPTED / FALSE|CUSTOM(data.scripts.campaign.invasion.news.ft.FT02Handler) ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|FT-03|超级闷棍|FORTIFICATION|SCRIPTED / FALSE|CUSTOM(data.scripts.campaign.invasion.news.ft.FT03Handler) ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|FT-04|母星外的等待|FORTIFICATION|SCRIPTED / FALSE|CUSTOM(data.scripts.campaign.invasion.news.ft.FT04Handler) ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|FT-05|幸存归来|FORTIFICATION|SCRIPTED / FALSE|CUSTOM(data.scripts.campaign.invasion.news.ft.FT05Handler) ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|FT-06|边界事发|FORTIFICATION|SCRIPTED / FALSE|CUSTOM(data.scripts.campaign.invasion.news.ft.FT06Handler) ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|FT-07|航路索赔|FORTIFICATION|SCRIPTED / FALSE|CUSTOM(data.scripts.campaign.invasion.news.ft.FT07Handler) ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|FT-08|倒爷的座|FORTIFICATION|SCRIPTED / FALSE|CUSTOM(data.scripts.campaign.invasion.news.ft.FT08Handler) ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|FT-09|欢喜冤家-事件|FORTIFICATION|SCRIPTED / FALSE|CUSTOM(data.scripts.campaign.invasion.news.ft.FT09Handler) ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|FT-10|一次临时定义|FORTIFICATION / MOBILIZATION|SCRIPTED / FALSE|CUSTOM(data.scripts.campaign.invasion.news.ft.FT10Handler) ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|W-01|新闻属实|WAR|AMBIENT / FALSE|MILITARY ; |BATTLE_AFTERMATH / ROUTE ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|W-02|准备撤离|WAR|AMBIENT / FALSE|POPULATED ; |CREW_MISSING / ROUTE ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|W-03|疑似胜利|WAR|AMBIENT / FALSE|MILITARY ; |BATTLE_AFTERMATH ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|W-04|信号毁坏|WAR|AMBIENT / FALSE|EDGE_MARKET ; |CREW_MISSING / BATTLE_AFTERMATH ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|W-05|信道崩坏|WAR|AMBIENT / FALSE|RELAY ; |COMMUNICATION / DISTORTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|W-06|海盗联盟|WAR|AMBIENT / FALSE|PIRATE ; |BATTLE_AFTERMATH / ROUTE ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|W-07|未知立场|WAR|AMBIENT / FALSE|WILDERNESS ; |FACILITY / DISTORTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|W-08|阵线崩溃|WAR|AMBIENT / FALSE|MILITARY ; |BATTLE_AFTERMATH / ROUTE ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|W-09|难民潮|WAR|AMBIENT / FALSE|POPULATED ; |CREW_MISSING / ROUTE ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|W-10|严禁打捞|WAR|AMBIENT / FALSE|ANY ; |BATTLE_AFTERMATH / DISTORTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|W-11|战时状态|WAR|AMBIENT / FALSE|ANY ; |BATTLE_AFTERMATH / DISTORTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|N-CUSTOM-01|没油逃犯|ALL|AMBIENT / FALSE|CUSTOM(data.scripts.campaign.invasion.news.FugitiveShuttleNewsHandler) ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F-HEG-01|空白征令|ALL|AMBIENT / FALSE|FACTION("hegemony") ; |PORT_RECORDS ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F-HEG-02|退役服役|ALL|AMBIENT / FALSE|FACTION("hegemony") ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F-TT-01|死亡与赋税|ALL|AMBIENT / FALSE|FACTION("tritachyon") ; |PORT_RECORDS ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F-TT-02|购买绩效|ALL|AMBIENT / FALSE|FACTION("tritachyon") ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F-LC-01|朝圣迟到|ALL|AMBIENT / FALSE|FACTION("luddic_church") ; |SPATIAL_ECHO ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F-LC-02|三祝谷物|ALL|AMBIENT / FALSE|FACTION("luddic_church") ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F-LP-01|布道不止|ALL|AMBIENT / FALSE|FACTION("luddic_path") ; |ABANDONED_FACILITY ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F-LP-02|反应堆售罄|ALL|AMBIENT / FALSE|FACTION("luddic_path") ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F-PIR-01|悬赏自己|ALL|AMBIENT / FALSE|FACTION("pirates") ; |ABANDONED_FACILITY ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F-PIR-02|九九新|ALL|AMBIENT / FALSE|FACTION("pirates") ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F-PL-01|延期表决|ALL|AMBIENT / FALSE|FACTION("persean") ; |PORT_RECORDS ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F-PL-02|战争债券|ALL|AMBIENT / FALSE|FACTION("persean") ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|JE-01|失声航路|ALL|AMBIENT / TRUE|EDGE_MARKET ; |RELAY_PARASITE / SIGNAL_PURSUIT ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|MB-01|红色草案|MOBILIZATION|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|MB-02|七处求援|MOBILIZATION|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|MB-03|末班离港|MOBILIZATION|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|MB-04|航道让行|MOBILIZATION|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|MB-05|和平拨款|MOBILIZATION|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-01|名单失准|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-02|军需空单|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-03|双印争签|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-04|捷报待核|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-05|补给候签|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-06|孤星求援|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-07|远征获准|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-08|空域失守|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-09|名单有名|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-10|断港留账|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-11|伤船候准|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-12|商路遇袭|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-13|损报相左|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-14|外围告急|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-15|视界迁移|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-16|视界危急|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-GS-01|席位待签|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-GS-02|后备启程|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-GS-03|供货缺页|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-GS-04|越级获签|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|WR-JE-01|无章勘验|WAR|SCRIPTED / FALSE|ANY ; | ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|R2-01|中继重放|RECON|CONDITIONAL / FALSE|ANY ; RELAY|RELAY_PARASITE ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|R2-02|航图删改|RECON|CONDITIONAL / FALSE|ANY ; JUMP|ROUTE_REVERSAL ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|R2-03|港口漏登|RECON|CONDITIONAL / FALSE|ANY ; MARKET|PORT_RECORDS ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|R2-04|静默拖船|RECON|CONDITIONAL / FALSE|ANY ; SCOUT|MISSING_CONVOY ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|R2-05|假求救|RECON|CONDITIONAL / FALSE|ANY ; WILDERNESS|SIGNAL_PURSUIT ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|R2-06|上传浮标|RECON|CONDITIONAL / FALSE|ANY ; REPORT|SIGNAL_PURSUIT ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|R2-07|踪迹标记|RECON|CONDITIONAL / FALSE|ANY ; PLAYER_TRAIL|ROUTE_REVERSAL ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|R2-08|燃料购买异常|RECON|CONDITIONAL / FALSE|ANY ; MARKET|PORT_RECORDS ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|R2-09|跃迁点假灯|RECON|CONDITIONAL / FALSE|ANY ; JUMP|ROUTE_REVERSAL ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|R2-10|星图批注|RECON|CONDITIONAL / FALSE|ANY ; RELAY|RELAY_PARASITE ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|R2-11|好死不送|RECON|CONDITIONAL / FALSE|ANY ; WILDERNESS|ABANDONED_FACILITY ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|R2-12|倒填关税单|RECON|CONDITIONAL / FALSE|ANY ; MARKET|PORT_RECORDS ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|R2-13|传感器盲扫|RECON|CONDITIONAL / FALSE|ANY ; SCOUT|SPATIAL_ECHO ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|R2-14|短促火力测试|RECON|CONDITIONAL / FALSE|ANY ; SCOUT|WRECK_RECONSTRUCTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|R2-15|伪造气象记录|RECON|CONDITIONAL / FALSE|ANY ; WILDERNESS|SPATIAL_ECHO ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E2-01|残骸索价|EXPANSION|CONDITIONAL / FALSE|ANY ; MARKET| ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E2-02|临行改约|EXPANSION|CONDITIONAL / FALSE|ANY ; MARKET| ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E2-03|一船双号|EXPANSION|CONDITIONAL / FALSE|ANY ; MARKET| ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E2-04|悬赏叠贴|EXPANSION|CONDITIONAL / FALSE|ANY ; MARKET| ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E2-05|明日两价|EXPANSION|CONDITIONAL / FALSE|ANY ; MARKET| ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E2-06|空泊有主|EXPANSION|CONDITIONAL / FALSE|ANY ; MARKET| ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E2-07|归船失保|EXPANSION|CONDITIONAL / FALSE|ANY ; MARKET| ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E2-08|决赛跳票|EXPANSION|CONDITIONAL / FALSE|ANY ; MARKET| ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E2-09|替身疑照|EXPANSION|CONDITIONAL / FALSE|ANY ; MARKET| ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E2-10|彩带入舱|EXPANSION|CONDITIONAL / FALSE|ANY ; MARKET| ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E2-11|疑罪从有|EXPANSION|CONDITIONAL / FALSE|ANY ; MARKET| ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E2-12|持械领号|EXPANSION|CONDITIONAL / FALSE|ANY ; MARKET| ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E2-13|安全副驾|EXPANSION|CONDITIONAL / FALSE|ANY ; MARKET| ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E2-14|空间断拍|EXPANSION|CONDITIONAL / FALSE|ANY ; BLACK_HOLE| ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|E2-15|辐峰无证|EXPANSION|CONDITIONAL / FALSE|ANY ; BLACK_HOLE| ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F2-01|你不许说|FORTIFICATION|CONDITIONAL / FALSE|ANY ; FORTRESS|FORT_GRAVITY_SURVEY ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F2-02|你不许说|FORTIFICATION|CONDITIONAL / FALSE|ANY ; FORT_SMALL|FORT_PATROL_GAP ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F2-03|护航错拍|FORTIFICATION|CONDITIONAL / FALSE|ANY ; TRANSPORT|FORT_SUPPLY_WINDOW ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F2-04|旧路无声|FORTIFICATION|CONDITIONAL / FALSE|ANY ; FORT_BLOCKED|FORT_DECOY_TRANSIT ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F2-05|黑洞回声|FORTIFICATION|CONDITIONAL / FALSE|ANY ; FORT_STABLE|SIGNAL_PURSUIT ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F2-06|黑洞回声|FORTIFICATION|CONDITIONAL / FALSE|ANY ; FORT_MEDIUM|FORT_SALVAGE_INTERCEPTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F2-07|双重日期|FORTIFICATION|CONDITIONAL / FALSE|ANY ; FORTRESS|SIGNAL_PURSUIT ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F2-08|删除之影|FORTIFICATION|CONDITIONAL / FALSE|ANY ; JUMP|FORT_DECOY_TRANSIT ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F2-09|恢复正常|FORTIFICATION|CONDITIONAL / FALSE|ANY ; FORTRESS|FORT_PATROL_GAP ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F2-10|引力失拍|FORTIFICATION|CONDITIONAL / FALSE|ANY ; FORT_EDEN|FORT_GRAVITY_SURVEY ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F2-11|航路销签|FORTIFICATION|CONDITIONAL / FALSE|ANY ; RELAY|PORT_RECORDS ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F2-12|未知单元|FORTIFICATION|CONDITIONAL / FALSE|ANY ; OUTPOST|TRAFFIC_OBSERVATION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F2-13|武装打捞|FORTIFICATION|CONDITIONAL / FALSE|ANY ; FORT_EDEN|FORT_SALVAGE_INTERCEPTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F2-14|双口同影|FORTIFICATION|CONDITIONAL / FALSE|ANY ; JUMP|FORT_DECOY_TRANSIT ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|F2-15|许可未至|FORTIFICATION|CONDITIONAL / FALSE|ANY ; MARKET|PORT_RECORDS ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|SITE-01|航迹折返|DORMANT / RECON|AMBIENT / FALSE|ANY ; |ROUTE_REVERSAL ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|SITE-02|空船引路|DORMANT / RECON|AMBIENT / FALSE|ANY ; |ROUTE_DIVERSION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|SITE-03|重放余音|RECON|AMBIENT / FALSE|ANY ; |RELAY_PARASITE ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|SITE-04|无人应答|RECON|AMBIENT / FALSE|ANY ; |SIGNAL_PURSUIT ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|SITE-05|残骸异向|RECON / EXPANSION|AMBIENT / FALSE|ANY ; |WRECK_RECONSTRUCTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|SITE-06|拖船失联|RECON / EXPANSION|AMBIENT / FALSE|ANY ; |MISSING_CONVOY ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|SITE-07|疑似光照|EXPANSION / FORTIFICATION|AMBIENT / FALSE|ANY ; |FACILITY_INTERDICTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|SITE-08|秒针三响|RECON / EXPANSION|AMBIENT / FALSE|ANY ; |SPATIAL_ECHO ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|SITE-09|货影交班|FORTIFICATION|AMBIENT / FALSE|ANY ; |FORT_SUPPLY_WINDOW ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|SITE-10|散点同向|FORTIFICATION|AMBIENT / FALSE|ANY ; |FORT_SENSOR_LATTICE ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|SITE-11|巡迹间断|FORTIFICATION|AMBIENT / FALSE|ANY ; |FORT_PATROL_GAP ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|SITE-12|双口异重|FORTIFICATION|AMBIENT / FALSE|ANY ; |FORT_DECOY_TRANSIT ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|SITE-13|引力错拍|FORTIFICATION|AMBIENT / FALSE|ANY ; |FORT_GRAVITY_SURVEY ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|SITE-14|打捞求援|FORTIFICATION|AMBIENT / FALSE|ANY ; |FORT_SALVAGE_INTERCEPTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|JE-02|回声追索|RECON / EXPANSION|AMBIENT / TRUE|ANY ; |SIGNAL_PURSUIT ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|JE-03|峰值之外|FORTIFICATION / MOBILIZATION|AMBIENT / TRUE|ANY ; |FORT_GRAVITY_SURVEY ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|
|JE-04|残录争夺|FORTIFICATION / MOBILIZATION|AMBIENT / TRUE|ANY ; |FORT_SALVAGE_INTERCEPTION ; |未测；文案□ 地点□ 真假/选项□ 后果□ 清理□|

## 记录方法

对失败项追加：`ID / 分支 / 测试档 / 实际现象 / 预期 / 日志时刻`。无需把174条全部测完才反馈；第一轮优先完整手册的P0/P1批次。
