package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
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

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
        if (Global.getSector()==null || !Global.getSettings().isDevMode()) return null;
        Object existing=Global.getSector().getIntelManager().getFirstIntel(PTSDCrisisDevControlIntel.class);
        if (existing instanceof PTSDCrisisDevControlIntel) return (PTSDCrisisDevControlIntel)existing;
        PTSDCrisisDevControlIntel intel=new PTSDCrisisDevControlIntel();
        intel.setImportant(true);
        Global.getSector().getIntelManager().addIntel(intel,true);
        return intel;
    }
    public static void removeWhenNotDev() {
        if (Global.getSector()==null || Global.getSettings().isDevMode()) return;
        for (IntelInfoPlugin item:new ArrayList<IntelInfoPlugin>(Global.getSector().getIntelManager().getIntel(PTSDCrisisDevControlIntel.class)))
            Global.getSector().getIntelManager().removeIntel(item);
    }

    @Override protected String getName(){return "[DEV] 危机变量与事件控制台";}
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
        addProgress(info,s,width,base,dark);
        addCounters(info,s,width,base,dark);
        addPhases(info,s,width,base,dark);
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
    private void addCounters(TooltipMakerAPI i,PTSDCrisisState s,float w,Color b,Color d){
        i.addSectionHeading("全局计数器",b,d,Alignment.MID,12f);
        counter(i,w,b,d,"SCOUT_SIGHTINGS","第四窥视目击",s.totalScoutSightings);
        counter(i,w,b,d,"SCOUT_ESCAPES","侦察队脱离",s.totalScoutEscapes);
        counter(i,w,b,d,"OMEGA_ENCOUNTERS","欧米伽遭遇",s.totalOmegaEncounters);
        counter(i,w,b,d,"PLAYER_BATTLES","玩家与危机交战",s.totalPlayerOmegaBattles);
        counter(i,w,b,d,"VISIBLE_STAGE","玩家可见阶段",s.visibleStage);
        counter(i,w,b,d,"JE_TASKS","Je 已完成任务",s.jeCompletedInvestigations);
        i.addPara("玩家记恨：%s",5f,Misc.getHighlightColor(),fmt(s.playerGrudge));buttons(i,w,b,d,"GRUDGE",10f);
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
        if(s.phase==PTSDCrisisState.Phase.FORTIFICATION&&PTSDCrisisProgress.isReadyForInvasion(s)&&!s.prewarHunterSpawned)sectionButton(i,w,b,d,"大事件：战前高价值目标截获","创建锁住全面入侵进度的第四窥视截获舰队。","EVENT:PREWAR_HUNTER");
        if(s.prewarHunterResolved&&!s.prewarRedAlertShown)sectionButton(i,w,b,d,"大事件：多当局联合红色警报","确认未知舰体新闻属实并启动战争倒计时。","EVENT:RED_ALERT");
        if(s.phase==PTSDCrisisState.Phase.WAR&&!s.hardWarningShown)sectionButton(i,w,b,d,"大事件：边缘失联信号危险警告","发布全面战争硬警告并切换正式危机情报。","EVENT:HARD_WARNING");        if(s.phase==PTSDCrisisState.Phase.RECON){
            sectionButton(i,w,b,d,"大战略事件：火力侦察","在玩家当前星系生成一项火控试探战略事件。","EVENT:FIRE_PROBE");
            sectionButton(i,w,b,d,"侦察每日事件：等强机动","生成与玩家当前FP一致并以玩家为目标的舰队。","EVENT:RECON_MATCHED");
            sectionButton(i,w,b,d,"侦察每日事件：跨星系陷阱判定","按玩家当前自动导航目标埋设陷阱；目标必须是另一无人非隐藏星系。","EVENT:RECON_TRAP_ARM");
            sectionButton(i,w,b,d,"侦察每日事件：立即显现陷阱舰群","在玩家当前星系直接生成五支中立陷阱编队。","EVENT:RECON_TRAP_NOW");
            sectionButton(i,w,b,d,"侦察每日事件：不可接触观察单元","在玩家附近生成主动控制距离的介入灵质侦察舰。","EVENT:RECON_OBSERVER");
            sectionButton(i,w,b,d,"侦察每日事件：航路残骸群","在当前行进方向生成6至12艘持续30日的非危机势力残骸。","EVENT:RECON_WRECKS");
            sectionButton(i,w,b,d,"侦察每日事件：受击即撤追击舰队","生成特殊追击舰队；战斗受击后立即全体撤离。","EVENT:RECON_PURSUER");
            sectionButton(i,w,b,d,"新闻现场：通讯与传感器","将最新可调查新闻设为属实并投影通讯类现场。","EVENT:SITE_COMMUNICATION");
            sectionButton(i,w,b,d,"新闻现场：航路与导航","将最新可调查新闻设为属实并投影航路类现场。","EVENT:SITE_ROUTE");
            sectionButton(i,w,b,d,"新闻现场：战斗与残骸","将最新可调查新闻设为属实并投影战场类现场。","EVENT:SITE_BATTLE");
            sectionButton(i,w,b,d,"新闻现场：人员失踪","将最新可调查新闻设为属实并投影人员类现场。","EVENT:SITE_CREW");
            sectionButton(i,w,b,d,"新闻现场：设施与工程","将最新可调查新闻设为属实并投影设施类现场。","EVENT:SITE_FACILITY");
            sectionButton(i,w,b,d,"新闻现场：空间异常","将最新可调查新闻设为属实并投影扭曲类现场。","EVENT:SITE_DISTORTION");
            sectionButton(i,w,b,d,"新闻现场：武力调查组合","将最新可调查新闻投影为战场现场，并强制生成残骸与第四窥视调查小队。","EVENT:SITE_MARTIAL");
        }
        if(s.phase==PTSDCrisisState.Phase.WAR){sectionButton(i,w,b,d,"大战略事件：精神创伤进攻","创建一次精神创伤进攻推演。","EVENT:ATTACK");sectionButton(i,w,b,d,"大战略事件：人类防御","创建一支人类防卫事件。","EVENT:DEFENSE");}
    }
    private void addRegistered(TooltipMakerAPI i,PTSDCrisisState s,float w,Color b,Color d){
        for(Action a:REGISTERED.values())if(a.condition==null||a.condition.isAvailable(s)){i.addSectionHeading(a.header==null?a.label:a.header,b,d,Alignment.MID,12f);if(a.description!=null)i.addPara(a.description,5f);i.addButton(a.label,"REG:"+a.id,b,d,w,24f,4f);}
    }
    private void addCsvEvents(TooltipMakerAPI i,PTSDCrisisState s,float w,Color b,Color d){
        for(PTSDCrisisIncidentManager.DevCard c:PTSDCrisisIncidentManager.getDevCardsForCurrentPhase()){i.addSectionHeading("子事件："+c.category+" / "+c.id,b,d,Alignment.MID,12f);i.addPara(c.headline+"（阶段条件："+c.phases+"）",5f);i.addButton("触发 "+c.id,"NEWS:"+c.id,b,d,w,22f,3f);}
    }
    private void sectionButton(TooltipMakerAPI i,float w,Color b,Color d,String h,String text,String id){i.addSectionHeading(h,b,d,Alignment.MID,12f);i.addPara(text,5f);i.addButton("触发",id,b,d,w,24f,4f);}

    @Override public void buttonPressConfirmed(Object buttonId,IntelUIAPI ui){
        if(!Global.getSettings().isDevMode()||!(buttonId instanceof String))return;String id=(String)buttonId;PTSDCrisisState s=PTSDCrisisState.get();if(s==null)return;
        if(REFRESH.equals(id)){ui.updateUIForItem(this);return;}
        if(id.startsWith("VAR:")){String[] p=id.split(":");PTSDCrisisProgress.add(s,PTSDCrisisProgress.Variable.valueOf(p[1]),Float.parseFloat(p[2]),"DEV_CONTROL",null);}
        else if(id.startsWith("COUNT:")){adjustCount(s,id);}
        else if(id.startsWith("GRUDGE:")){s.playerGrudge=Math.max(0f,Math.min(200f,s.playerGrudge+Float.parseFloat(id.substring(7))));}
        else if(id.startsWith("PHASE:")){IIRT_Omega_Invasion.devTransition(PTSDCrisisState.Phase.valueOf(id.substring(6)));}
        else if(id.startsWith("NEWS:")){PTSDCrisisIncidentManager.force(id.substring(5));}
        else if(id.startsWith("REG:")){Action a=REGISTERED.get(id.substring(4));if(a!=null&&(a.condition==null||a.condition.isAvailable(s)))a.executor.execute(s,ui);}
        else if(id.startsWith("EVENT:")){executeBuiltin(id.substring(6),s,ui);}
        ui.updateIntelList(true);ui.updateUIForItem(this);
    }
    private void adjustCount(PTSDCrisisState s,String id){String[] p=id.split(":");int n=Math.round(Float.parseFloat(p[2]));if("SCOUT_SIGHTINGS".equals(p[1]))s.totalScoutSightings=Math.max(0,s.totalScoutSightings+n);else if("SCOUT_ESCAPES".equals(p[1]))s.totalScoutEscapes=Math.max(0,s.totalScoutEscapes+n);else if("OMEGA_ENCOUNTERS".equals(p[1]))s.totalOmegaEncounters=Math.max(0,s.totalOmegaEncounters+n);else if("PLAYER_BATTLES".equals(p[1]))s.totalPlayerOmegaBattles=Math.max(0,s.totalPlayerOmegaBattles+n);else if("VISIBLE_STAGE".equals(p[1]))s.visibleStage=Math.max(0,Math.min(4,s.visibleStage+n));else if("JE_TASKS".equals(p[1]))s.jeCompletedInvestigations=Math.max(0,s.jeCompletedInvestigations+n);}
    private void executeBuiltin(String key,PTSDCrisisState s,IntelUIAPI ui){
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
        StarSystemAPI sys=currentSystem();if(sys==null)return;PTSDCrisisState.EventType type="ATTACK".equals(key)?PTSDCrisisState.EventType.ATTACK:("DEFENSE".equals(key)?PTSDCrisisState.EventType.DEFENSE:PTSDCrisisState.EventType.FIRE_PROBE);String side=type==PTSDCrisisState.EventType.DEFENSE?PTSDCrisisAPI.SIDE_HUMAN:PTSDCrisisAPI.SIDE_OMEGA;String faction=type==PTSDCrisisState.EventType.DEFENSE?"independent":PTSDCrisisProgress.getActiveFactionId(s);PTSDCrisisState.StrategicEvent e=s.addEvent(type,side,faction,null,sys.getId(),null,type==PTSDCrisisState.EventType.ATTACK?120f:65f,8f);e.playerRelevant=true;e.description="DEV 控制台触发";
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