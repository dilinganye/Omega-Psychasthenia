package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.IIRT_Omega_ModPlugin;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DevMode-only state/event console. External and future crisis modules may register actions here
 * instead of modifying this Intel class. Conditions are checked both while rendering and executing.
 */
public final class PTSDCrisisDevControlIntel extends BaseIntelPlugin {
    private static final long serialVersionUID = 1L;
    private static final String REFRESH = "PTSD_CTRL_REFRESH";

    public interface Condition { boolean isAvailable(PTSDCrisisState state); }
    public interface Executor { void execute(PTSDCrisisState state, IntelUIAPI ui); }
    public static final class Action {
        public final String header, id, label, description;
        public final Condition condition;
        public final Executor executor;
        private Action(String header, String id, String label, String description,
                       Condition condition, Executor executor) {
            this.header=header; this.id=id; this.label=label; this.description=description;
            this.condition=condition; this.executor=executor;
        }
    }
    private static final Map<String, Action> REGISTERED = new LinkedHashMap<String, Action>();

    public static void registerAction(String header, String id, String label, String description,
                                      Condition condition, Executor executor) {
        if (id == null || executor == null) return;
        REGISTERED.put(id, new Action(header, id, label, description, condition, executor));
    }
    public static void unregisterAction(String id) { REGISTERED.remove(id); }

    public static PTSDCrisisDevControlIntel ensureIntel() {
        if (Global.getSector()==null || !Global.getSettings().isDevMode() ||
                IIRT_Omega_ModPlugin.OMEGA_PTSD_is_Alpha) return null;
        Object existing=Global.getSector().getIntelManager().getFirstIntel(PTSDCrisisDevControlIntel.class);
        if (existing instanceof PTSDCrisisDevControlIntel) return (PTSDCrisisDevControlIntel)existing;
        PTSDCrisisDevControlIntel intel=new PTSDCrisisDevControlIntel();
        intel.setImportant(true);
        Global.getSector().getIntelManager().addIntel(intel,true);
        return intel;
    }
    public static void removeWhenUnavailable() {
        if (Global.getSector()==null) return;
        if (Global.getSettings().isDevMode() && !IIRT_Omega_ModPlugin.OMEGA_PTSD_is_Alpha) return;
        for (IntelInfoPlugin item:new ArrayList<IntelInfoPlugin>(Global.getSector().getIntelManager().getIntel(PTSDCrisisDevControlIntel.class)))
            Global.getSector().getIntelManager().removeIntel(item);
    }

    @Override protected String getName(){return "[DEV] 危机阶段与后期流程控制台";}
    @Override public String getSmallDescriptionTitle(){return getName();}
    @Override public boolean hasLargeDescription(){return true;}
    @Override public void createSmallDescription(TooltipMakerAPI info,float width,float height){
        PTSDCrisisState s=PTSDCrisisState.get();
        info.addPara(s==null?"危机尚未初始化。":"阶段 "+s.phase+"；此处可修改全局状态并按正常条件触发事件。",10f);
    }
    @Override public void createLargeDescription(CustomPanelAPI panel,float width,float height){
        TooltipMakerAPI info=panel.createUIElement(width,height,true);
        PTSDCrisisState s=PTSDCrisisState.get(); Color base=getFactionForUIColors().getBaseUIColor(),dark=getFactionForUIColors().getDarkUIColor();
        info.addSectionHeading("全局变量控制",base,dark,Alignment.MID,0f);
        if(s==null){info.addPara("危机状态尚未初始化。",Misc.getNegativeHighlightColor(),10f);panel.addUIElement(info).inTL(0,0);return;}
        info.addPara("这里有意不包含逐星系侦察、攻击权重和星球恐慌表。所有修改都会写入当前存档。",8f);
        addPhases(info,s,width,base,dark);
        addReconControls(info,s,width,base,dark);
        if (s.late != null) addLateControls(info,s,width,base,dark);
        addProgress(info,s,width,base,dark);
        addCounters(info,s,width,base,dark);
        addBuiltinEvents(info,s,width,base,dark);
        addRegistered(info,s,width,base,dark);
        addCsvEvents(info,s,width,base,dark);
        info.addButton("刷新控制台",REFRESH,base,dark,width,24f,10f);
        panel.addUIElement(info).inTL(0,0);
    }
    private void addProgress(TooltipMakerAPI i,PTSDCrisisState s,float w,Color b,Color d){
        String[][] vars={{"RECON_CONFIDENCE","侦察置信"},{"HUMAN_AWARENESS","人类认知"},{"WATCHER_AGGRESSION","第四窥视攻击性"},{"NEST_DEVELOPMENT","前哨发展"},{"BLOCKADE_DENSITY","封锁密度"},{"OMEGA_ESCALATION","精神创伤升级"},{"HUMAN_COHESION","人类凝聚"},{"GLOBAL_PANIC","全局恐慌加成"},{"REALITY_DISTORTION","现实扭曲"}};
        for(String[] v:vars){PTSDCrisisProgress.Variable x=PTSDCrisisProgress.Variable.valueOf(v[0]);i.addPara(v[1]+"：%s",5f,Misc.getHighlightColor(),fmt(PTSDCrisisProgress.get(s,x)));buttons(i,w,b,d,"VAR:"+v[0],10f);}
    }
    private void addReconControls(TooltipMakerAPI i,PTSDCrisisState s,float w,Color b,Color d) {
        i.addSectionHeading("侦察报告与盲点",b,d,Alignment.MID,12f);
        int officerPhase = Math.max(0, Math.min(4,
                s.phase.ordinal() - PTSDCrisisState.Phase.RECON.ordinal()));
        int officerBase = 2 + IIRT_Omega_ModPlugin.omegaOfficerLevelBonus + officerPhase;
        i.addPara("新生欧米伽军官：基础2 + Luna难度加成"+
                IIRT_Omega_ModPlugin.omegaOfficerLevelBonus+" + 阶段"+officerPhase+
                " = "+officerBase+"级；随机范围 "+Math.max(2,officerBase-2)+"～"+
                Math.min(20,officerBase+2)+"级。仅影响之后生成的军官。",3f);
        i.addPara(PTSDReconReportDirector.coverageSummary(s),5f);
        i.addPara("批量报告概率 玩家/三星系/双星系/仅本地："+
                Math.round(IIRT_Omega_ModPlugin.reconReportPlayerChance)+"/"+
                Math.round(IIRT_Omega_ModPlugin.reconReportThreeChance)+"/"+
                Math.round(IIRT_Omega_ModPlugin.reconReportTwoChance)+"/"+
                Math.round(100f-IIRT_Omega_ModPlugin.reconReportPlayerChance-
                        IIRT_Omega_ModPlugin.reconReportThreeChance-IIRT_Omega_ModPlugin.reconReportTwoChance)+
                "%；间隔 "+fmt(IIRT_Omega_ModPlugin.reconReportInterval)+" 日；上传 "+
                fmt(IIRT_Omega_ModPlugin.reconUploadMin)+"～"+fmt(IIRT_Omega_ModPlugin.reconUploadMax)+" 日",3f);
        i.addPara("远距误差 "+fmt(IIRT_Omega_ModPlugin.reconRemoteError*100f)+
                "%；有效 "+fmt(IIRT_Omega_ModPlugin.reconRemoteLifetime)+
                " 日；无人改道 "+fmt(IIRT_Omega_ModPlugin.reconEmptyRerouteMin)+"～"+
                fmt(IIRT_Omega_ModPlugin.reconEmptyRerouteMax)+" 日；来源："+
                (Global.getSettings().isDevMode() && IIRT_Omega_ModPlugin.hasLunaLib?
                        "LunaLib DevMode（缺项用默认）":"内置默认"),3f);
        i.addPara("条件新闻密度 侦察/营建/要塞："+
                fmt(IIRT_Omega_ModPlugin.reconNewsDensity)+"/"+
                fmt(IIRT_Omega_ModPlugin.expansionNewsDensity)+"/"+
                fmt(IIRT_Omega_ModPlugin.fortressNewsDensity)+
                "；误报率："+fmt(IIRT_Omega_ModPlugin.reconNewsFalseRate*100f)+"/"+
                fmt(IIRT_Omega_ModPlugin.expansionNewsFalseRate*100f)+"/"+
                fmt(IIRT_Omega_ModPlugin.fortressNewsFalseRate*100f)+
                "%；R2/E2/F2自然投放×0.1，DEV固定属实",3f);
        i.addPara("最近改道："+(s.lastReconRoutingReason==null?"无":s.lastReconRoutingReason),3f);
        int shown=0;
        for(PTSDCrisisState.PendingReconReport report:s.pendingReconReports)
            if(report!=null&&shown++<10)i.addPara("待上传 "+report.kind+" / "+report.targetSystemId+
                    " / 源 "+report.sourceFleetId+" / 剩余 "+fmt(Math.max(0f,report.uploadDay-PTSDCrisisState.getDay()))+" 日",2f);
    }
    private void addCounters(TooltipMakerAPI i,PTSDCrisisState s,float w,Color b,Color d){
        i.addSectionHeading("全局计数器",b,d,Alignment.MID,12f);
        counter(i,w,b,d,"SCOUT_SIGHTINGS","第四窥视目击",s.totalScoutSightings);
        counter(i,w,b,d,"SCOUT_ESCAPES","侦察队脱离",s.totalScoutEscapes);
        counter(i,w,b,d,"OMEGA_ENCOUNTERS","欧米伽遭遇",s.totalOmegaEncounters);
        counter(i,w,b,d,"PLAYER_BATTLES","玩家与危机交战",s.totalPlayerOmegaBattles);
        counter(i,w,b,d,"VISIBLE_STAGE","玩家可见阶段",s.visibleStage);
        counter(i,w,b,d,"JE_TASKS","Je 已完成任务",s.jeCompletedInvestigations);
        i.addPara("玩家记恨：%s",5f,Misc.getHighlightColor(),fmt(s.playerGrudge));buttons(i,w,b,d,"GRUDGE",10f);
        PTSDCrisisFleetLimits fleetLimits=PTSDCrisisFleetLimits.forPhase(s,PTSDCrisisState.getDay());
        i.addPara("当前战略实体舰队上限：合计 "+fmt(fleetLimits.totalFp)+
                " / 单队 "+fmt(fleetLimits.perFleetFp)+" FP；单队最多 "+fleetLimits.maxShips+
                " 艘；最高舰种 "+fleetLimits.maxHullSize+
                "；难度上调×"+fmt(PTSDCrisisFleetLimits.difficultyIncrease()),3f);
    }
    private void addLateControls(TooltipMakerAPI i, PTSDCrisisState s, float w, Color b, Color d) {
        PTSDLateState late=s.late;
        i.addSectionHeading("后期危机：真实账本与执行入口",b,d,Alignment.MID,12f);
        i.addPara("准备点 真实/估计 "+fmt(late.warPoints)+" / "+fmt(late.warPointBelief)+
                "；机动额度 "+fmt(late.strategicReserve)+"；联合防务额度 "+fmt(late.humanResponseBudget)+
                "；玩家威胁 "+fmt(PTSDThreatDirector.getThreat(s,PTSDCrisisState.getDay())),5f);
        i.addPara("要塞 "+late.fortresses.size()+"；首波 "+late.selectedBurstCount+
                "/4；战区 "+late.fronts.size()+"；总参部 "+(late.staffEstablished?"已成立":"未成立")+
                "；后备队 "+late.reserves.size(),3f);
        i.addPara("生产："+(late.lastProductionBreakdown==null?"母星建成后开始校准":late.lastProductionBreakdown),3f);
        i.addPara("战略资产上限5000；最近任务回收："+
                (late.lastAssetCreditCause==null?"无":late.lastAssetCreditCause+
                        "，实际+"+fmt(late.lastAssetCreditAmount)+
                        "，第"+fmt(late.lastAssetCreditDay)+"日"),3f);
        i.addPara("账本版本 "+late.warEconomyVersion+"；距下次常规生产约 "+
                Math.max(0,Math.round(late.nextProductionDay-PTSDCrisisState.getDay()))+
                " 日；本期已支出准备 "+fmt(late.spentWarPointsSinceProduction)+
                " / 战略资产 "+fmt(late.spentStrategicReserveSinceProduction),3f);
        i.addPara("当前Luna参数：产出×"+fmt(data.scripts.IIRT_Omega_ModPlugin.lateProductionMultiplier)+
                "，建设×"+fmt(data.scripts.IIRT_Omega_ModPlugin.lateConstructionSpeed)+
                "，攻势×"+fmt(data.scripts.IIRT_Omega_ModPlugin.lateOffensiveTempo)+
                "，追击×"+fmt(data.scripts.IIRT_Omega_ModPlugin.latePlayerResponse)+
                "，告警×"+fmt(data.scripts.IIRT_Omega_ModPlugin.lateAlertDensity)+
                "；来源："+(data.scripts.IIRT_Omega_ModPlugin.hasLunaLib?
                "LunaLib（缺项使用内置默认）":"内置默认"),3f);
        buttons(i,w,b,d,"LATE_WAR_POINTS",20f);
        buttons(i,w,b,d,"LATE_BELIEF",20f);
        buttons(i,w,b,d,"LATE_RESERVE",40f);
        if(s.phase==PTSDCrisisState.Phase.EXPANSION||s.phase==PTSDCrisisState.Phase.FORTIFICATION||s.phase==PTSDCrisisState.Phase.MOBILIZATION||
                s.phase==PTSDCrisisState.Phase.WAR)
            lateButton(i,w,b,d,"结算一次正常生产","LATE:PRODUCE");
        if(s.phase==PTSDCrisisState.Phase.FORTIFICATION)
            lateButton(i,w,b,d,"检查要塞化离场门槛","LATE:CHECK_GATE");
        if(s.phase==PTSDCrisisState.Phase.FORTIFICATION||s.phase==PTSDCrisisState.Phase.MOBILIZATION||
                s.phase==PTSDCrisisState.Phase.WAR){
            lateButton(i,w,b,d,"在黑洞开始一座有成本的要塞工程","LATE:BUILD_FORTRESS");
            lateButton(i,w,b,d,"使一艘已付费运输舰到站","LATE:DELIVER_TRANSPORT");
            lateButton(i,w,b,d,"模拟推进现存要塞至完整体（不补造运输）","LATE:COMPLETE_FORTRESS");
            lateButton(i,w,b,d,"截断一艘现存施工运输舰","LATE:INTERCEPT_TRANSPORT");
            lateButton(i,w,b,d,"命令完整要塞执行有成本调动","LATE:MOVE_FORTRESS");
            lateButton(i,w,b,d,"到达首座现存要塞旁","LATE:GO_FORTRESS");
            lateButton(i,w,b,d,"模拟星系内完整要塞被击毁","LATE:DESTROY_LOCAL");
            lateButton(i,w,b,d,"模拟超空间完整要塞被击毁","LATE:DESTROY_HYPER");
        }
        if(s.phase==PTSDCrisisState.Phase.FORTIFICATION||s.phase==PTSDCrisisState.Phase.MOBILIZATION){
            i.addPara("四处爆发点测试：先提供侦察回执和真实准备点，再切换 WAR；首波只结算一次。",5f);
            lateButton(i,w,b,d,"向四处边缘殖民地写入模拟侦察回执","LATE:FOUR_ROUTES");
        }
        if(s.phase==PTSDCrisisState.Phase.MOBILIZATION)
            lateButton(i,w,b,d,"打开公开动员情报","LATE:OPEN_MOBILIZATION");
        if(s.phase==PTSDCrisisState.Phase.WAR){
            if(!late.firstWaveSelected)lateButton(i,w,b,d,"按真实预算结算首波四处爆发点","LATE:FIRST_WAVE");
            lateButton(i,w,b,d,"立即推进一轮战区结算","LATE:FRONT_TURN");
            if(!late.staffEstablished)lateButton(i,w,b,d,"建立联合总参部","LATE:STAFF");
            else lateButton(i,w,b,d,"打开联合总参部情报","LATE:OPEN_STAFF");
            lateButton(i,w,b,d,"录入180点玩家战绩并检查后备队","LATE:PLAYER_RECORD");
            lateButton(i,w,b,d,"按现有席位与资源结算总参部表决","LATE:VOTE_SETTLE");
            lateButton(i,w,b,d,"命令现有后备队前往当前星系","LATE:RESERVE_CURRENT");
            lateButton(i,w,b,d,"打开战区态势情报","LATE:OPEN_WAR");
        }
        lateButton(i,w,b,d,"注入当前星系已上传的玩家战力样本","LATE:EVIDENCE_FRONT");
        lateButton(i,w,b,d,"按真实兵力尝试高威胁伏击","LATE:THREAT_TRAP");
        for(PTSDLateState.Fortress f:late.fortresses.values())if(f!=null)
            i.addPara("要塞 "+f.id+"：等级 "+f.tier+"；工程 "+fmt(f.tierProgress)+
                    "；到站/损失 "+f.deliveredTransports+"/"+f.lostTransports+
                    "；位置 "+f.currentSystemId+(f.inTransit?" → "+f.destinationSystemId:""),2f);
        for(PTSDLateState.Fortress f:late.fortresses.values())if(f!=null)
            for(String transportId:f.transportIds){
                com.fs.starfarer.api.campaign.SectorEntityToken transport =
                        Global.getSector().getEntityById(transportId);
                i.addPara("施工运输 "+transportId+"："+
                        (transport==null?"对象已消失":
                                transport.getContainingLocation()==null?"无位置":
                                        transport.getContainingLocation().getName())+
                        " → "+f.currentSystemId,2f);
            }
        StarSystemAPI localSystem=currentSystem();
        if(localSystem!=null){
            int patrols=0,probing=0;
            float patrolFp=0f,probeFp=0f;
            for(PTSDLateState.ControlledPatrol slot:late.controlledPatrols.values())
                if(slot!=null&&localSystem.getId().equals(slot.systemId)){
                    com.fs.starfarer.api.campaign.SectorEntityToken projected=
                            slot.fleetId==null?null:Global.getSector().getEntityById(slot.fleetId);
                    if(projected instanceof CampaignFleetAPI){
                        patrols++;
                        patrolFp+=((CampaignFleetAPI)projected).getFleetPoints();
                    }
                    i.addPara("实控巡逻 "+slot.id+"："+
                            (projected instanceof CampaignFleetAPI?"现场 "+Math.round(((CampaignFleetAPI)projected).getFleetPoints())+" FP":"未实体化")+
                            "；补员 "+fmt(Math.max(0f,slot.replenishDay-PTSDCrisisState.getDay()))+" 日",2f);
                }
            for(PTSDCrisisState.StrategicEvent event:s.events)
                if(event!=null&&event.type==PTSDCrisisState.EventType.FIRE_PROBE&&
                        localSystem.getId().equals(event.targetSystemId)&&
                        event.materializedFleetIds!=null)
                    for(String id:event.materializedFleetIds){
                        com.fs.starfarer.api.campaign.SectorEntityToken entity=
                                Global.getSector().getEntityById(id);
                        if(entity instanceof CampaignFleetAPI){
                            probing++;
                            probeFp+=((CampaignFleetAPI)entity).getFleetPoints();
                        }
                    }
            i.addPara("当前星系实体占额：实控巡逻 "+patrols+" 队 / "+fmt(patrolFp)+
                    " FP；火力侦察 "+probing+" 队 / "+fmt(probeFp)+
                    " FP；当地警戒 "+fmt(s.getSystemData(localSystem.getId()).omegaLocalAlert),3f);
        }
        for(PTSDLateState.Front f:late.fronts.values())if(f!=null)
            i.addPara("战区 "+f.id+"：进攻 "+fmt(f.attackBudget)+"；防御 "+fmt(f.defense)+
                    "；打击 "+f.strikes+"；证据 "+f.evidenceIds.size()+(f.resolved?"；已结束":""),2f);
        int shown=0;
        for(PTSDLateState.ThreatEvidence e:late.threatEvidence)if(e!=null&&shown++<16)
            i.addPara("玩家证据 "+e.id+" / "+e.domain+" / "+e.uploadState+
                    " / 有效至 "+fmt(e.expiresDay),2f);
        i.addSectionHeading("后期事件卡（按当前阶段强制投放）",b,d,Alignment.MID,12f);
        for(Map.Entry<String,String> card:PTSDLateEventDirector.devCardsForPhase(s.phase).entrySet()){
            String reason=PTSDLateEventDirector.unavailableReason(s,card.getKey());
            i.addPara(card.getKey()+" · "+card.getValue()+"："+reason,2f);
            i.addButton("投放 "+card.getKey(),"LATE:CARD:"+card.getKey(),
                    "可触发".equals(reason)?b:Misc.getGrayColor(),d,w,22f,2f)
                    .setEnabled("可触发".equals(reason));
        }
        if(!late.eventCards.isEmpty())lateButton(i,w,b,d,"打开后期事件簿","LATE:OPEN_EVENTS");
    }
    private void lateButton(TooltipMakerAPI i,float w,Color b,Color d,String label,String id){
        i.addButton(label,id,b,d,w,22f,2f);
    }
    private void buttons(TooltipMakerAPI i,float w,Color b,Color d,String key,float step){
        i.addButton("-"+Math.round(step),key+":"+(-step),b,d,w,22f,2f);
        i.addButton("-1",key+":-1",b,d,w,22f,1f);
        i.addButton("+1",key+":1",b,d,w,22f,1f);
        i.addButton("+"+Math.round(step),key+":"+step,b,d,w,22f,1f);
    }
    private void counter(TooltipMakerAPI i,float w,Color b,Color d,String key,String name,int value){i.addPara(name+"：%s",5f,Misc.getHighlightColor(),String.valueOf(value));buttons(i,w,b,d,"COUNT:"+key,5f);}
    private void addPhases(TooltipMakerAPI i,PTSDCrisisState s,float w,Color b,Color d){
        i.addSectionHeading("大阶段切换（当前 "+s.phase+"）",b,d,Alignment.MID,12f);
        for(PTSDCrisisState.Phase p:PTSDCrisisState.Phase.values())i.addButton("切换到 "+p,"PHASE:"+p.name(),p==s.phase?Misc.getGrayColor():b,d,w,22f,2f).setEnabled(p!=s.phase);
    }
    private void addBuiltinEvents(TooltipMakerAPI i,final PTSDCrisisState s,float w,Color b,Color d){
        if(PTSDJeOtloesManager.triggerUnlocked(s)&&!s.jeIntroCompleted)sectionButton(i,w,b,d,"特殊事件：Je Otloes 初次拘留","在最近的友好港口直接打开初次对话。","EVENT:JE_INTRO");
        if(s.jeIntroCompleted&&s.jeAgentIncidentId==null&&!s.jeMeetingReady)sectionButton(i,w,b,d,"特殊事件：Je 联系人对话","直接打开联系人通讯。","EVENT:JE_CONTACT");
        if(s.jeIntroCompleted&&s.jePlayerTaskIncidentId==null)sectionButton(i,w,b,d,"特殊事件：Je 实地调查委托","创建专属较高强度调查。","EVENT:JE_PLAYER");
        if(s.jeIntroCompleted&&s.jeAgentIncidentId==null&&!s.jeMeetingReady)sectionButton(i,w,b,d,"特殊事件：Je 代为调查","令 Je 离线并调查一条可调查新闻。","EVENT:JE_AGENT");
        if(s.jeMeetingReady)sectionButton(i,w,b,d,"特殊事件：Je 调查结果会面","直接打开已满足条件的报告对话。","EVENT:JE_REPORT");
        if(s.jeIntroCompleted){
            sectionButton(i,w,b,d,"Je 测试：刷新任务与通讯状态","清除3日忙碌、12日任务冷却及当日通讯阻断。当前剩余 "+PTSDJeOtloesManager.daysUntilNextTask(s)+" 日。","EVENT:JE_RESET_COOLDOWN");
            sectionButton(i,w,b,d,"Je 测试：下一次必定接通","下一次联系人对话跳过75%失联随机。","EVENT:JE_FORCE_CONNECTED");
            sectionButton(i,w,b,d,"Je 测试：下一次普通失联","下一次联系人对话显示2秒无回应演出，但不锁住全天。","EVENT:JE_FORCE_MISSED");
            sectionButton(i,w,b,d,"Je 测试：下一次失联并阻断当天","下一次联系人对话失联，并令当天后续请求继续失败。","EVENT:JE_FORCE_BLOCKED");
            if(!s.jeDetectorGranted)sectionButton(i,w,b,d,"Je 大事件：授予危机信号相关器","直接授予并测试新生涯能力。","EVENT:JE_GRANT_DETECTOR");
        }
        if(!s.softWarningShown&&(s.totalScoutSightings>=data.scripts.IIRT_Omega_ModPlugin.warning_encounter_threshold||s.totalOmegaEncounters>=data.scripts.IIRT_Omega_ModPlugin.warning_encounter_threshold))sectionButton(i,w,b,d,"大事件：不为人知的动荡","发布软警告并建立危机 Intel。","EVENT:SOFT_WARNING");
        if(s.phase==PTSDCrisisState.Phase.MOBILIZATION&&!s.prewarHunterSpawned)sectionButton(i,w,b,d,"大事件：战前高价值目标截获","创建有预算的第四窥视截获舰队。","EVENT:PREWAR_HUNTER");
        if(s.prewarHunterResolved&&!s.prewarRedAlertShown)sectionButton(i,w,b,d,"大事件：多当局联合红色警报","确认未知舰体新闻属实并启动战争倒计时。","EVENT:RED_ALERT");
        if(s.phase==PTSDCrisisState.Phase.WAR&&!s.hardWarningShown)sectionButton(i,w,b,d,"大事件：边缘失联信号危险警告","发布全面战争硬警告并切换正式危机情报。","EVENT:HARD_WARNING");        if(s.phase==PTSDCrisisState.Phase.RECON){
            sectionButton(i,w,b,d,"大战略事件：火力侦察","在当前星系生成火力侦察；默认单队65、合计150自动分上限。","EVENT:FIRE_PROBE");
            sectionButton(i,w,b,d,"侦察每日事件：等强机动","按玩家威胁选择目标，但本队至多65自动分、三艘驱逐级舰船。","EVENT:RECON_MATCHED");
            sectionButton(i,w,b,d,"侦察每日事件：跨星系陷阱判定","仅依据附近侦察舰目击与公开航路推定无人星系，不读取玩家自动导航目标。","EVENT:RECON_TRAP_ARM");
            sectionButton(i,w,b,d,"侦察每日事件：立即显现陷阱舰群","有人星系最多两队/60， 无人星系最多三队/90；均为未知编队和驱逐级以下。","EVENT:RECON_TRAP_NOW");
            sectionButton(i,w,b,d,"侦察每日事件：不可接触观察单元","在玩家附近生成主动控制距离的介入灵质侦察舰。","EVENT:RECON_OBSERVER");
            sectionButton(i,w,b,d,"侦察每日事件：航路残骸群","生成1至2艘残舰与2至4片小型残骸区；70%概率附带介入灵质调查小队。","EVENT:RECON_WRECKS");
            sectionButton(i,w,b,d,"侦察每日事件：受击即撤追击舰队","生成最多65自动分、三艘驱逐级舰船的追击队；受击后撤离。","EVENT:RECON_PURSUER");
            sectionButton(i,w,b,d,"新闻现场：通讯与传感器","将最新可调查新闻设为属实并投影通讯类现场。","EVENT:SITE_COMMUNICATION");
            sectionButton(i,w,b,d,"新闻现场：航路与导航","将最新可调查新闻设为属实并投影航路类现场。","EVENT:SITE_ROUTE");
            sectionButton(i,w,b,d,"新闻现场：战斗与残骸","将最新可调查新闻设为属实并投影战场类现场。","EVENT:SITE_BATTLE");
            sectionButton(i,w,b,d,"新闻现场：人员失踪","将最新可调查新闻设为属实并投影人员类现场。","EVENT:SITE_CREW");
            sectionButton(i,w,b,d,"新闻现场：设施与工程","将最新可调查新闻设为属实并投影设施类现场。","EVENT:SITE_FACILITY");
            sectionButton(i,w,b,d,"新闻现场：空间异常","将最新可调查新闻设为属实并投影扭曲类现场。","EVENT:SITE_DISTORTION");
            sectionButton(i,w,b,d,"新闻现场：武力调查组合","将最新可调查新闻投影为战场现场，并强制生成残骸与第四窥视调查小队。","EVENT:SITE_MARTIAL");
        }
        if(s.phase==PTSDCrisisState.Phase.WAR){sectionButton(i,w,b,d,"大战略事件：精神创伤进攻","创建一次精神创伤进攻推演。","EVENT:ATTACK");sectionButton(i,w,b,d,"大战略事件：人类防御","创建一支人类防卫事件。","EVENT:DEFENSE");}
        i.addSectionHeading("未满足的既有事件条件",b,d,Alignment.MID,12f);
        String[] known={"JE_INTRO","JE_CONTACT","JE_PLAYER","JE_AGENT","JE_REPORT",
                "SOFT_WARNING","PREWAR_HUNTER","RED_ALERT","HARD_WARNING",
                "FIRE_PROBE","ATTACK","DEFENSE","SITE_COMMUNICATION"};
        for(String key:known){String reason=builtinUnavailableReason(key,s);
            if(reason!=null)i.addPara(key+"："+reason,2f);}
    }
    private void addRegistered(TooltipMakerAPI i,PTSDCrisisState s,float w,Color b,Color d){
        for(Action a:REGISTERED.values())if(a.condition==null||a.condition.isAvailable(s)){i.addSectionHeading(a.header==null?a.label:a.header,b,d,Alignment.MID,12f);if(a.description!=null)i.addPara(a.description,5f);i.addButton(a.label,"REG:"+a.id,b,d,w,24f,4f);}
    }
    private void addCsvEvents(TooltipMakerAPI i,PTSDCrisisState s,float w,Color b,Color d){
        for(PTSDCrisisIncidentManager.DevCard c:PTSDCrisisIncidentManager.getDevCardsForCurrentPhase()){
            String reason=PTSDCrisisIncidentManager.unavailableReason(c.id);
            boolean allowed="可触发".equals(reason)||reason.contains("DEV 强制投放可绕过");
            i.addSectionHeading("子事件："+c.category+" / "+c.id,b,d,Alignment.MID,12f);
            i.addPara(c.headline+"（"+c.delivery+"；"+reason+"）",5f);
            i.addButton("触发 "+c.id,"NEWS:"+c.id,allowed?b:Misc.getGrayColor(),d,w,22f,3f).setEnabled(allowed);
        }
    }
    private void sectionButton(TooltipMakerAPI i,float w,Color b,Color d,String h,String text,String id){i.addSectionHeading(h,b,d,Alignment.MID,12f);i.addPara(text,5f);i.addButton("触发",id,b,d,w,24f,4f);}

    @Override public void buttonPressConfirmed(Object buttonId,IntelUIAPI ui){
        if(!Global.getSettings().isDevMode()||!(buttonId instanceof String))return;String id=(String)buttonId;PTSDCrisisState s=PTSDCrisisState.get();if(s==null)return;
        if(REFRESH.equals(id)){ui.updateUIForItem(this);return;}
        if(id.startsWith("VAR:")){String[] p=id.split(":");PTSDCrisisProgress.add(s,PTSDCrisisProgress.Variable.valueOf(p[1]),Float.parseFloat(p[2]),"DEV_CONTROL",null);}
        else if(id.startsWith("COUNT:")){adjustCount(s,id);}
        else if(id.startsWith("GRUDGE:")){s.playerGrudge=Math.max(0f,Math.min(200f,s.playerGrudge+Float.parseFloat(id.substring(7))));}
        else if(id.startsWith("LATE_WAR_POINTS:")){s.late.warPoints=Math.max(0f,Math.min(200f,s.late.warPoints+Float.parseFloat(id.substring(16))));}
        else if(id.startsWith("LATE_BELIEF:")){s.late.warPointBelief=Math.max(0f,Math.min(200f,s.late.warPointBelief+Float.parseFloat(id.substring(12))));}
        else if(id.startsWith("LATE_RESERVE:")){s.late.strategicReserve=Math.max(0f,Math.min(700f,s.late.strategicReserve+Float.parseFloat(id.substring(13))));}
        else if(id.startsWith("LATE:")){executeLate(id.substring(5),s,ui);}
        else if(id.startsWith("PHASE:")){IIRT_Omega_Invasion.devTransition(PTSDCrisisState.Phase.valueOf(id.substring(6)));}
        else if(id.startsWith("NEWS:")){
            String cardId=id.substring(5);
            if(!PTSDCrisisIncidentManager.force(cardId))
                Global.getSector().getCampaignUI().addMessage("[DEV] "+cardId+"："+
                        PTSDCrisisIncidentManager.unavailableReason(cardId),Misc.getGrayColor());
        }
        else if(id.startsWith("REG:")){Action a=REGISTERED.get(id.substring(4));if(a!=null&&(a.condition==null||a.condition.isAvailable(s)))a.executor.execute(s,ui);}
        else if(id.startsWith("EVENT:")){executeBuiltin(id.substring(6),s,ui);}
        ui.updateIntelList(true);ui.updateUIForItem(this);
    }
    private void adjustCount(PTSDCrisisState s,String id){String[] p=id.split(":");int n=Math.round(Float.parseFloat(p[2]));if("SCOUT_SIGHTINGS".equals(p[1]))s.totalScoutSightings=Math.max(0,s.totalScoutSightings+n);else if("SCOUT_ESCAPES".equals(p[1]))s.totalScoutEscapes=Math.max(0,s.totalScoutEscapes+n);else if("OMEGA_ENCOUNTERS".equals(p[1]))s.totalOmegaEncounters=Math.max(0,s.totalOmegaEncounters+n);else if("PLAYER_BATTLES".equals(p[1]))s.totalPlayerOmegaBattles=Math.max(0,s.totalPlayerOmegaBattles+n);else if("VISIBLE_STAGE".equals(p[1]))s.visibleStage=Math.max(0,Math.min(4,s.visibleStage+n));else if("JE_TASKS".equals(p[1]))s.jeCompletedInvestigations=Math.max(0,s.jeCompletedInvestigations+n);}
    private void executeLate(String action, PTSDCrisisState s, IntelUIAPI ui) {
        float day=PTSDCrisisState.getDay();
        boolean done=false;
        if("PRODUCE".equals(action))done=PTSDLateWarDirector.devProduce(s);
        else if("CHECK_GATE".equals(action)){
            PTSDCrisisState.Phase old=s.phase;
            PTSDLateWarDirector.checkFortificationExit(s,day-s.phaseStartedDay,day);
            done=old!=s.phase;
        }
        else if("BUILD_FORTRESS".equals(action))done=PTSDFortressDirector.devStartNext(s,currentSystem());
        else if("DELIVER_TRANSPORT".equals(action))done=PTSDFortressDirector.devDeliverOne(s);
        else if("COMPLETE_FORTRESS".equals(action))done=PTSDFortressDirector.devCompleteOne(s);
        else if("INTERCEPT_TRANSPORT".equals(action))done=PTSDFortressDirector.devInterceptOne(s);
        else if("MOVE_FORTRESS".equals(action))done=PTSDFortressDirector.devOrderMove(s);
        else if("GO_FORTRESS".equals(action)){
            for(PTSDLateState.Fortress fortress:s.late.fortresses.values()){
                if(fortress==null||fortress.destroyed||
                        Global.getSector().getEntityById(fortress.fleetId)==null)continue;
                done=PTSDCrisisDevIntel.devArriveAt(fortress.fleetId,fortress.currentSystemId);
                if(done)break;
            }
        }
        else if("DESTROY_LOCAL".equals(action))done=PTSDFortressDirector.devDestroyOne(s,false);
        else if("DESTROY_HYPER".equals(action))done=PTSDFortressDirector.devDestroyOne(s,true);
        else if("FOUR_ROUTES".equals(action))done=devSeedFourRoutes(s)>0;
        else if("FIRST_WAVE".equals(action)&&s.phase==PTSDCrisisState.Phase.WAR){
            boolean before=s.late.firstWaveSelected;
            PTSDLateWarDirector.onWarStarted(s);
            done=!before&&s.late.firstWaveSelected;
        }
        else if("FRONT_TURN".equals(action))done=PTSDLateWarDirector.devAdvanceOneFront(s);
        else if("STAFF".equals(action))done=PTSDGeneralStaffDirector.devEstablish(s);
        else if("PLAYER_RECORD".equals(action)){
            String key="DEV:PLAYER_RECORD";
            PTSDGeneralStaffDirector.recordContribution(s,key,"BATTLE","player",s.baseSystemId,180f);
            PTSDGeneralStaffDirector.advance(s,day);
            done=true;
        }
        else if("VOTE_SETTLE".equals(action))done=PTSDGeneralStaffDirector.devResolveNextVote(s);
        else if("RESERVE_CURRENT".equals(action)){
            StarSystemAPI system=currentSystem();
            if(system!=null)for(PTSDLateState.Reserve reserve:s.late.reserves){
                if(reserve==null||reserve.destroyed||!reserve.authorized)continue;
                done=PTSDGeneralStaffDirector.orderReserve(s,reserve.id,system.getId());
                if(done)break;
            }
        }
        else if("EVIDENCE_FRONT".equals(action)){
            StarSystemAPI system=currentSystem();
            if(system!=null){PTSDThreatDirector.recordAction(s,"FRONT","DEV:"+Misc.genUID(),
                    system.getId(),80f,.9f,true,60f);done=true;}
        }
        else if("THREAT_TRAP".equals(action)){
            StarSystemAPI system=currentSystem();
            if(system!=null){
                PTSDThreatDirector.recordAction(s,"FRONT","DEV:"+Misc.genUID(),
                        system.getId(),90f,.95f,true,60f);
                s.late.previousPlayerVictoryFP=0f;
                s.late.recentPlayerVictoryFP=300f;
                s.late.nextTrapDay=day;
                float before=s.late.nextTrapDay;
                PTSDThreatDirector.advance(s,day);
                done=s.late.nextTrapDay>before;
            }
        }
        else if(action.startsWith("CARD:")){
            String card=action.substring(5);
            done=PTSDLateEventDirector.force(s,card);
        }
        else if("OPEN_MOBILIZATION".equals(action)){
            PTSDMobilizationIntel intel=PTSDMobilizationIntel.ensureIntel();
            if(intel!=null){ui.selectItem(intel);done=true;}
        }
        else if("OPEN_WAR".equals(action)){
            PTSDWarIntel intel=PTSDWarIntel.ensureIntel();
            if(intel!=null){ui.selectItem(intel);done=true;}
        }
        else if("OPEN_STAFF".equals(action)){
            PTSDGeneralStaffIntel intel=PTSDGeneralStaffIntel.ensureIntel();
            if(intel!=null){ui.selectItem(intel);done=true;}
        }
        else if("OPEN_EVENTS".equals(action)){
            PTSDLateEventIntel intel=PTSDLateEventIntel.ensureIntel();
            if(intel!=null){ui.selectItem(intel);done=true;}
        }
        String result=done?"已执行":lateFailureReason(action,s);
        PTSDCrisisDevIntel.report("DEV 后期操作",action+"："+result,s.baseSystemId,null);
        if(!done)Global.getSector().getCampaignUI().addMessage("[DEV] "+action+"："+result,Misc.getGrayColor());
    }
    private String lateFailureReason(String action,PTSDCrisisState s){
        if("PRODUCE".equals(action))return "母星尚未建成或控制权不符；没有可结算的生产源";
        if("CHECK_GATE".equals(action)){
            int routes=0;
            for(PTSDCrisisState.SystemData data:s.systems.values())
                if(data!=null&&data.hasNonCrisisColony&&
                        PTSDLateWarDirector.hasValidEvidence(data,PTSDCrisisState.getDay()))routes++;
            return "要塞化已过 "+Math.round(PTSDCrisisState.getDay()-s.phaseStartedDay)+
                    " 日；真实/估计准备 "+fmt(s.late.warPoints)+"/"+fmt(s.late.warPointBelief)+
                    "；有效航路 "+routes+"；期限 "+
                    Math.round(data.scripts.IIRT_Omega_ModPlugin.lateFortificationDeadline)+" 日";
        }
        if("BUILD_FORTRESS".equals(action))return "需要至少10准备点、30战略资产、未占用的黑洞星系及剩余要塞名额";
        if("COMPLETE_FORTRESS".equals(action))return "没有可模拟升级的现存、未毁、未在途要塞；检查完整体变体资源";
        if("DESTROY_LOCAL".equals(action))return PTSDFortressDirector.devDestroyReason(s,false);
        if("DESTROY_HYPER".equals(action))return PTSDFortressDirector.devDestroyReason(s,true);
        if("DELIVER_TRANSPORT".equals(action)||"INTERCEPT_TRANSPORT".equals(action))
            return "没有可用的实际施工运输舰；检查要塞等级、战略资产和运输冷却";
        if("MOVE_FORTRESS".equals(action))return "没有驻留且可支付调动成本的完整要塞";
        if("FRONT_TURN".equals(action))return "没有仍在交战的战区";
        if("VOTE_SETTLE".equals(action))return "没有待表决议案";
        if("RESERVE_CURRENT".equals(action))return "当前星系无有效目标或没有已授权后备队";
        if(action.startsWith("CARD:"))return PTSDLateEventDirector.unavailableReason(s,action.substring(5));
        return "当前阶段、目标或预算条件不满足；查看本页账本及最近事件记录";
    }
    private int devSeedFourRoutes(PTSDCrisisState state) {
        List<MarketAPI> markets=new ArrayList<MarketAPI>(PTSDOccupationManager.getAllMarkets());
        Collections.sort(markets,new Comparator<MarketAPI>(){
            @Override public int compare(MarketAPI a,MarketAPI b){
                float ad=a==null||a.getStarSystem()==null?0f:a.getStarSystem().getLocation().length();
                float bd=b==null||b.getStarSystem()==null?0f:b.getStarSystem().getLocation().length();
                return Float.compare(bd,ad);
            }
        });
        Set<String> used=new LinkedHashSet<String>();
        for(MarketAPI market:markets){
            if(market==null||market.isPlanetConditionMarketOnly()||market.getStarSystem()==null||
                    market.getPrimaryEntity()==null||
                    IIRT_Omega_Invasion.WATCHER_FACTION.equals(market.getFactionId())||
                    IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(market.getFactionId())||
                    PTSDCrisisFactionModifierAPI.get(market.getFactionId(),market.getStarSystem().getId()).immune||
                    !used.add(market.getStarSystem().getId()))continue;
            PTSDCrisisAPI.recordEvidence(market.getStarSystem().getId(),"DEV_SIMULATED_SCOUT",
                    "DEV:"+market.getId(),-1f,-1f,50f,1,-1,.8f,90f,false,0f);
            if(used.size()>=4)break;
        }
        return used.size();
    }
    private void executeBuiltin(String key,PTSDCrisisState s,IntelUIAPI ui){
        String blocked=builtinUnavailableReason(key,s);
        if(blocked!=null){
            Global.getSector().getCampaignUI().addMessage("[DEV] "+key+"："+blocked,Misc.getGrayColor());
            PTSDCrisisDevIntel.report("DEV 事件未执行",key+"："+blocked,s.baseSystemId,null);
            return;
        }
        if("JE_RESET_COOLDOWN".equals(key)){PTSDJeOtloesManager.resetTaskCooldownForDev();return;}
        if("JE_FORCE_CONNECTED".equals(key)){PTSDJeOtloesManager.devForceNextContact(1);return;}
        if("JE_FORCE_MISSED".equals(key)){PTSDJeOtloesManager.devForceNextContact(2);return;}
        if("JE_FORCE_BLOCKED".equals(key)){PTSDJeOtloesManager.devForceNextContact(3);return;}
        if("JE_GRANT_DETECTOR".equals(key)){PTSDJeOtloesManager.grantDetector();return;}
        if("JE_INTRO".equals(key)&&PTSDJeOtloesManager.triggerUnlocked(s)&&!s.jeIntroCompleted){MarketAPI m=nearestMarket();if(m!=null)ui.showDialog(m.getPrimaryEntity(),new PTSDJeOtloesDialog(PTSDJeOtloesDialog.Mode.INTRO,m));return;}
        if("JE_CONTACT".equals(key)&&s.jeIntroCompleted&&s.jeAgentIncidentId==null&&!s.jeMeetingReady){ui.showDialog(Global.getSector().getPlayerFleet(),new PTSDJeOtloesDialog(PTSDJeOtloesDialog.Mode.CONTACT,null));return;}
        if("JE_PLAYER".equals(key)&&s.jeIntroCompleted&&s.jePlayerTaskIncidentId==null){PTSDJeOtloesManager.startPlayerInvestigation(null);PTSDCrisisIntel task=PTSDCrisisIntel.ensureIntel();ui.updateIntelList(true);ui.selectItem(task);return;}
        if("JE_AGENT".equals(key)&&s.jeIntroCompleted&&s.jeAgentIncidentId==null&&!s.jeMeetingReady){PTSDJeOtloesManager.startAgentInvestigation(null);return;}
        if("JE_REPORT".equals(key)&&s.jeMeetingReady){MarketAPI m=s.resolveMarket(s.jeMeetingMarketId);if(m!=null)ui.showDialog(m.getPrimaryEntity(),new PTSDJeOtloesDialog(PTSDJeOtloesDialog.Mode.REPORT,m));return;}
        if("SOFT_WARNING".equals(key)){IIRT_Omega_Invasion.devTriggerSoftWarning();return;}
        if("RED_ALERT".equals(key)){IIRT_Omega_Invasion.devTriggerPrewarRedAlert();return;}
        if("HARD_WARNING".equals(key)){IIRT_Omega_Invasion.devTriggerHardWarning();return;}
        if(key.startsWith("RECON_")){PTSDReconPlayerEvents.devTrigger(key.substring(6));return;}
        if(key.startsWith("SITE_")){
            String family=key.substring(5);boolean martial="MARTIAL".equals(family);
            if(martial)family=PTSDNewsSiteManager.BATTLE;
            PTSDNewsSiteManager.devMaterializeLatest(family,martial);return;
        }
        if("PREWAR_HUNTER".equals(key)){StarSystemAPI target=currentSystem();if(target==null)return;PTSDCrisisState.StrategicEvent e=s.addEvent(PTSDCrisisState.EventType.PREWAR_HUNTER,PTSDCrisisAPI.SIDE_OMEGA,IIRT_Omega_Invasion.WATCHER_FACTION,s.baseSystemId,target.getId(),null,32f+Math.min(28f,s.playerGrudge*.35f),12f);if(Global.getSector().getPlayerFleet()!=null)e.targetEntityId=Global.getSector().getPlayerFleet().getId();e.playerRelevant=true;e.description="第四窥视对高价值目标的最终截获测试；该行动结束前，全面入侵进度被锁定。";s.prewarHunterSpawned=true;s.prewarHunterEventId=e.id;return;}
        if(!"ATTACK".equals(key)&&!"DEFENSE".equals(key)&&!"FIRE_PROBE".equals(key))return;
        StarSystemAPI sys=currentSystem();if(sys==null)return;PTSDCrisisState.EventType type="ATTACK".equals(key)?PTSDCrisisState.EventType.ATTACK:("DEFENSE".equals(key)?PTSDCrisisState.EventType.DEFENSE:PTSDCrisisState.EventType.FIRE_PROBE);String side=type==PTSDCrisisState.EventType.DEFENSE?PTSDCrisisAPI.SIDE_HUMAN:PTSDCrisisAPI.SIDE_OMEGA;String faction=type==PTSDCrisisState.EventType.DEFENSE?"independent":PTSDCrisisProgress.getActiveFactionId(s);PTSDCrisisState.StrategicEvent e=s.addEvent(type,side,faction,null,sys.getId(),null,type==PTSDCrisisState.EventType.ATTACK?120f:65f,8f);e.playerRelevant=true;e.description="DEV 控制台触发";
    }
    private String builtinUnavailableReason(String key,PTSDCrisisState s){
        if(key.startsWith("RECON_")||"FIRE_PROBE".equals(key))
            return s.phase==PTSDCrisisState.Phase.RECON?null:"需要侦察阶段";
        if("ATTACK".equals(key)||"DEFENSE".equals(key))
            return s.phase==PTSDCrisisState.Phase.WAR?null:"需要全面战争阶段";
        if("PREWAR_HUNTER".equals(key))
            return s.phase!=PTSDCrisisState.Phase.MOBILIZATION?"需要公开动员阶段":
                    s.prewarHunterSpawned?"高价值目标截获已经部署":null;
        if("RED_ALERT".equals(key))return !s.prewarHunterResolved?"高价值目标截获尚未结束":
                s.prewarRedAlertShown?"红色警报已发布":null;
        if("HARD_WARNING".equals(key))return s.phase!=PTSDCrisisState.Phase.WAR?"需要全面战争阶段":
                s.hardWarningShown?"全面警告已发布":null;
        if("SOFT_WARNING".equals(key))return s.softWarningShown?"软警告已发布":
                s.totalScoutSightings<IIRT_Omega_ModPlugin.warning_encounter_threshold&&
                s.totalOmegaEncounters<IIRT_Omega_ModPlugin.warning_encounter_threshold?
                        "侦察目击与异常接触次数均未达预警门槛":null;
        if("JE_INTRO".equals(key))return s.jeIntroCompleted?"Je 已完成初次接触":
                PTSDJeOtloesManager.triggerUnlocked(s)?null:"Je 接触剧情尚未解锁";
        if("JE_CONTACT".equals(key))return !s.jeIntroCompleted?"Je 尚未成为联系人":
                s.jeAgentIncidentId!=null?"Je 正在代查":s.jeMeetingReady?"请先完成会面":null;
        if("JE_PLAYER".equals(key))return !s.jeIntroCompleted?"Je 尚未成为联系人":
                s.jePlayerTaskIncidentId!=null?"已有玩家调查委托":null;
        if("JE_AGENT".equals(key))return !s.jeIntroCompleted?"Je 尚未成为联系人":
                s.jeAgentIncidentId!=null?"Je 正在代查":s.jeMeetingReady?"报告会面待完成":null;
        if("JE_REPORT".equals(key))return s.jeMeetingReady?null:"Je 的调查报告尚未待会面";
        if(key.startsWith("SITE_")){
            for(PTSDCrisisState.CrisisIncident incident:s.incidents)
                if(incident!=null&&incident.investigable)return null;
            return "没有现存可调查新闻";
        }
        return null;
    }
    private StarSystemAPI currentSystem(){if(Global.getSector().getPlayerFleet()!=null&&Global.getSector().getPlayerFleet().getStarSystem()!=null)return Global.getSector().getPlayerFleet().getStarSystem();MarketAPI m=nearestMarket();return m==null?null:m.getStarSystem();}
    private MarketAPI nearestMarket(){MarketAPI best=null;float dist=Float.MAX_VALUE;for(MarketAPI m:Global.getSector().getEconomy().getMarketsCopy()){if(!PTSDJeOtloesManager.eligibleMarket(m)||m.getStarSystem()==null)continue;float d=Global.getSector().getPlayerFleet()==null?0f:Misc.getDistance(Global.getSector().getPlayerFleet().getLocationInHyperspace(),m.getStarSystem().getLocation());if(d<dist){dist=d;best=m;}}return best;}
    private static String fmt(float v){return String.valueOf(Math.round(v*10f)/10f);}
    @Override public boolean shouldRemoveIntel(){return !Global.getSettings().isDevMode()||super.shouldRemoveIntel();}
    @Override public boolean isHidden(){return !Global.getSettings().isDevMode();}
    @Override public String getIcon(){FactionAPI f=getFactionForUIColors();return f==null?null:f.getCrest();}
    @Override public FactionAPI getFactionForUIColors(){FactionAPI f=Global.getSector()==null?null:Global.getSector().getFaction(IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION);return f==null?super.getFactionForUIColors():f;}
    @Override public Set<String> getIntelTags(SectorMapAPI map){Set<String> t=super.getIntelTags(map);t.add(Tags.INTEL_IMPORTANT);t.add("危机");t.add("DEV");return t;}
}
