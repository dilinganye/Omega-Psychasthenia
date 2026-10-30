package data.scripts.campaign.invasion.news.defaults;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.shard.PTSD_BaseShard_Util;
import data.scripts.campaign.invasion.*;
import data.scripts.campaign.invasion.news.*;
import org.lwjgl.util.vector.Vector2f;
import java.util.*;

/**
 * P-10：抢先回收、隐蔽尾随实际交接、截击载运者。三条路线由玩家行为决定。
 * 仅使用公开 eventLoc 接口；所有任务状态放入 eventData，不在共享处理器内保存。
 * 原报道/核查文字保留在 CSV；以下只包含新增现场对话和行为。
 */
public final class P10Event implements NewsEvent, PTSDCrisisNewsAPI.CustomNewsHandler, PTSDNewsSiteAPI.SiteHandler {
    private static final String[] ROLES={"collector","receiver","pursuer"};
    public void configure(NewsEventDefinition d) {
        d.recon=1.2f;d.awareness=3;d.aggression=1.4f;d.panic=2;d.distortion=.8f;
        d.strength=20;d.physicalChance=0; // 独立现场自己造舰，不叠加旧附属行动。
        d.templates="WRECK_RECONSTRUCTION";d.martialSite="FALSE";d.manualCompletion=true;
    }
    public PTSDCrisisNewsAPI.TargetSelection pick(PTSDCrisisNewsAPI.PickContext c) {
        List<StarSystemAPI> systems=new ArrayList<StarSystemAPI>();
        for(StarSystemAPI s:Global.getSector().getStarSystems())if(!s.getJumpPoints().isEmpty()&&
                !s.hasTag(Tags.THEME_HIDDEN)&&!s.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)&&!PTSDProtectedFaction.system(s))systems.add(s);
        if(systems.isEmpty())return null;
        StarSystemAPI s=systems.get(c.random.nextInt(systems.size()));
        return new PTSDCrisisNewsAPI.TargetSelection(s,null,s.getJumpPoints().get(c.random.nextInt(s.getJumpPoints().size())));
    }
    static String get(PTSDCrisisState.CrisisIncident i,String key){return i.eventData==null?null:i.eventData.get("p10."+key);}
    static void set(PTSDCrisisState.CrisisIncident i,String key,Object value){
        if(i.eventData==null)i.eventData=new LinkedHashMap<String,String>();i.eventData.put("p10."+key,String.valueOf(value));
    }
    static float num(PTSDCrisisState.CrisisIncident i,String key){try{return Float.parseFloat(get(i,key));}catch(Exception absent){return 0;}}
    static boolean yes(PTSDCrisisState.CrisisIncident i,String key){return "true".equals(get(i,key));}
    static SectorEntityToken entity(String id){return id==null||id.isEmpty()?null:Global.getSector().getEntityById(id);}
    static CampaignFleetAPI fleet(PTSDCrisisState.CrisisIncident i,String role){SectorEntityToken e=entity(get(i,role));return e instanceof CampaignFleetAPI?(CampaignFleetAPI)e:null;}
    static boolean alive(CampaignFleetAPI f){return f!=null&&f.isAlive()&&f.getFleetPoints()>0&&f.getContainingLocation()!=null;}
    static boolean near(SectorEntityToken a,SectorEntityToken b,float radius){return a!=null&&b!=null&&a.getContainingLocation()==b.getContainingLocation()&&Misc.getDistance(a,b)<radius;}
    public void onAccepted(NewsEventContext c){set(c.incident,"mode","WAITING");}
    private static SectorEntityToken wreck(StarSystemAPI sys,Vector2f at,Random rng,String name){
        DerelictShipEntityPlugin.DerelictShipData spec=DerelictShipEntityPlugin.createRandom(Factions.INDEPENDENT,null,rng,0);
        spec.durationDays=45;
        SectorEntityToken e=BaseThemeGenerator.addSalvageEntity(rng,sys,Entities.WRECK,Factions.INDEPENDENT,spec);
        e.setLocation(at.x,at.y);e.setFacing(rng.nextFloat()*360);e.setName(name);e.addTag(Tags.UNRECOVERABLE);return e;
    }
    private static CampaignFleetAPI spawn(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,StarSystemAPI sys,Vector2f point,String role){
        PTSDCrisisFleetLimits limits=PTSDCrisisFleetLimits.forPhase(state,PTSDCrisisState.getDay());
        float active=0;for(String r:ROLES){CampaignFleetAPI f=fleet(i,r);if(alive(f))active+=f.getFleetPoints();}
        float fp=Math.min(limits.perFleetFp,limits.totalFp-active);if(fp<3)return null;
        for(CampaignFleetAPI f:sys.getFleets())if(f!=Global.getSector().getPlayerFleet()&&!PTSDJeOtloesManager.isOmegaFleet(f)&&Misc.getDistance(f.getLocation(),point)<2500)return null;
        FleetParamsV3 params=new FleetParamsV3(point,PTSDCrisisProgress.getActiveFactionId(state),.5f,FleetTypes.PATROL_SMALL,fp,0,0,0,0,0,0);
        CampaignFleetAPI f=PTSD_BaseShard_Util.createFleet(params,fp,PTSD_BaseShard_Util.FleetRole.RECON,new Random((i.id+role).hashCode()),limits.forFleet(fp));
        if(f==null||f.getFleetPoints()<=0)return null;
        sys.addEntity(f);PTSDCampaignRuntime.entityAdded(f);f.setLocation(point.x,point.y);f.setNoFactionInName(true);f.setTransponderOn(false);
        f.setName(role.equals("collector")?"正在回收残片的未知编队":role.equals("receiver")?"等待交接的未知编队":"搜寻残片的未知编队");
        f.getMemoryWithoutUpdate().set("$PTSD_participation_key","incident:"+i.id);
        f.getEventListeners().add(new PTSDStrategicFleetListener(null,f.getFleetPoints()));
        f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,true);
        for(com.fs.starfarer.api.fleet.FleetMemberAPI m:f.getFleetData().getMembersListCopy())m.getRepairTracker().setCR(m.getRepairTracker().getMaxCR());
        set(i,role,f.getId());set(i,role+"End",PTSDCrisisState.getDay()+12);return f;
    }
    public PTSDNewsSiteAPI.SiteResult materialize(PTSDNewsSiteAPI.SiteContext c){
        if(c.system==null||c.system.getJumpPoints().isEmpty())return null;
        PTSDCrisisState.CrisisIncident i=c.incident;
        SectorEntityToken exit=c.system.getJumpPoints().get(Math.floorMod(i.id.hashCode(),c.system.getJumpPoints().size()));
        Vector2f field=PTSDCrisisAPI.findSafePoint(c.system,exit,4200,6500,c.random);if(field==null)return null;
        Vector2f start=PTSDCrisisAPI.findSafePoint(c.system,field,5200,6500,c.random);if(start==null)return null;
        CampaignFleetAPI collector=spawn(c.state,i,c.system,start,"collector");if(collector==null)return null;
        List<String> owned=new ArrayList<String>();owned.add(collector.getId());
        try{
            SectorEntityToken anchor=wreck(c.system,field,c.random,"被切开的武器舱残骸");owned.add(anchor.getId());
            for(int n=0;n<2;n++){
                Vector2f p=PTSDCrisisAPI.findSafePoint(c.system,field,350,1000,c.random);
                if(p!=null)owned.add(wreck(c.system,p,c.random,"散落的熔蚀船体").getId());
            }
            set(i,"anchor",anchor.getId());set(i,"exit",exit.getId());set(i,"mode","APPROACH");set(i,"cargoRole","collector");
            set(i,"readyDay",PTSDCrisisState.getDay()+.8f);set(i,"lastDay",PTSDCrisisState.getDay());
            new NewsEventContext(c.state,i,PTSDCrisisState.getDay()).bind(anchor);
            move(collector,anchor,"接近残片");Global.getSector().addScript(new Runtime(i));
            return new PTSDNewsSiteAPI.SiteResult(anchor,"武器残片争夺","无应答编队正向残片靠近。",
                    "抢先打捞、隐蔽尾随交接，或截击载运者。",owned);
        }catch(RuntimeException failure){for(String id:owned)remove(id);set(i,"collector","");throw failure;}
    }
    private static void move(CampaignFleetAPI f,SectorEntityToken target,String text){
        if(f==null||target==null)return;f.clearAssignments();f.addAssignment(FleetAssignment.GO_TO_LOCATION,target,30,text);
        f.addAssignment(FleetAssignment.ORBIT_PASSIVE,target,30,text);
    }
    static boolean readyToCollect(float day,float ready,boolean atSite,boolean fighting,boolean taken){return day>=ready&&atSite&&!fighting&&!taken;}
    static boolean canTail(boolean visible,boolean detected,float distance){return visible&&!detected&&distance>=900&&distance<=6000;}
    static boolean queuePursuit(PTSDCrisisState.CrisisIncident i,float day){
        if(yes(i,"queued")||yes(i,"spawned"))return false;set(i,"queued",true);set(i,"pursuitDay",day+.3f);return true;
    }
    static String resultFor(String route){
        if("RACE".equals(route))return "打捞组抢先取回了残片。隔离箱内的结构仍在缓慢折叠，熔蚀面与报道相符。附近的陌生回波开始向残骸区靠拢。";
        if("INTERCEPT".equals(route))return "载运编队已失去携走残片的能力。回收的封装仍有余温，内部结构与原船体的切口相吻合；一段未发完的收讫信号被一并保存。";
        return "两支无应答编队在跃迁口附近短暂并行，质量读数从一侧转移到另一侧。你取得了连续航迹与交接记录，没有取得实物，也无法据此推断它们更远处的部署。";
    }
    static void finish(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,String route){
        if(i.investigationResolved)return;set(i,"route",route);i.effectSummary=resultFor(route);
        new NewsEventContext(state,i,PTSDCrisisState.getDay()).finish(PTSDTaskRules.InvestigationResult.CONFIRMED,true);
        i.effectSummary=resultFor(route); // 战略效果确认会写账本摘要，玩家仍应看到实际取得的证据。
        if(Global.getSector().getCampaignUI()!=null)Global.getSector().getCampaignUI().addMessage(i.effectSummary);
    }
    /** 只追踪已被传感器发现的玩家；失联后取消拦截，搜索最后已知位置。 */
    private static void search(PTSDCrisisState.CrisisIncident i,CampaignFleetAPI f,CampaignFleetAPI player){
        if(!alive(f)||f.getBattle()!=null)return;
        boolean detected=player!=null&&player.getContainingLocation()==f.getContainingLocation()&&player.isVisibleToSensorsOf(f);
        if(detected){set(i,"searchX",player.getLocation().x);set(i,"searchY",player.getLocation().y);
            if(!yes(i,"chasing")||f.getCurrentAssignment()==null){f.clearAssignments();f.addAssignment(FleetAssignment.INTERCEPT,player,1,"追回残片");set(i,"chasing",true);}
        }else if(yes(i,"chasing")||f.getCurrentAssignment()==null){
            SectorEntityToken p=f.getContainingLocation().createToken(new Vector2f(num(i,"searchX"),num(i,"searchY")));
            f.clearAssignments();f.addAssignment(FleetAssignment.GO_TO_LOCATION,p,3,"检查最后接触位置");
            f.addAssignment(FleetAssignment.PATROL_SYSTEM,p,8,"搜索残片信号");set(i,"chasing",false);
        }
    }
    static void tick(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,float day){
        // 仅每0.05游戏日检查本事件的三支舰队；没有逐帧全星区扫描或离线补算尾随。
        if(day<num(i,"nextTick"))return;
        float elapsed=Math.max(0,Math.min(.1f,day-num(i,"lastDay")));set(i,"lastDay",day);set(i,"nextTick",day+.05f);
        StarSystemAPI sys=state.resolveSystem(i.targetSystemId);if(sys==null)return;
        CampaignFleetAPI player=Global.getSector().getPlayerFleet(),collector=fleet(i,"collector"),cargo=fleet(i,get(i,"cargoRole"));
        SectorEntityToken anchor=entity(get(i,"anchor")),exit=entity(get(i,"exit"));String mode=get(i,"mode");
        if(alive(cargo)){set(i,"lastX",cargo.getLocation().x);set(i,"lastY",cargo.getLocation().y);}
        if(!i.investigationResolved&&!alive(cargo)&&("CARRYING".equals(mode)||"TRANSFERRED".equals(mode))){
            // 装载后的载运者实际损失才留下封装；自然离场先写 LOST，避免刷第二份样品。
            Vector2f at=new Vector2f(num(i,"lastX"),num(i,"lastY"));
            if(!PTSDCrisisAPI.isSafePoint(sys,at))at=anchor==null?null:anchor.getLocation();
            if(at!=null){SectorEntityToken drop=wreck(sys,at,new Random(i.id.hashCode()),"载运队遗落的残片封装");set(i,"drop",drop.getId());new NewsEventContext(state,i,day).bind(drop);}
            set(i,"mode","DROPPED");
        }
        if(!alive(collector)&&"APPROACH".equals(mode))set(i,"mode","UNGUARDED");
        if(!i.investigationResolved&&alive(collector)&&"APPROACH".equals(get(i,"mode"))&&
                readyToCollect(day,num(i,"readyDay"),near(collector,anchor,500),collector.getBattle()!=null,yes(i,"taken"))){
            set(i,"mode","CARRYING");move(collector,exit,"携残片前往交接");
        }
        if(!i.investigationResolved&&"CARRYING".equals(get(i,"mode"))&&alive(collector)){
            boolean visible=near(player,collector,6000)&&collector.isVisibleToPlayerFleet();
            if(canTail(visible,player!=null&&player.isVisibleToSensorsOf(collector),player==null?Float.MAX_VALUE:Misc.getDistance(player,collector))){set(i,"tail",num(i,"tail")+elapsed);i.eventEvidenceCollected=true;}
            if(near(collector,exit,650)&&collector.getBattle()==null){
                if(num(i,"arrival")==0)set(i,"arrival",day);
                CampaignFleetAPI receiver=fleet(i,"receiver");
                if(!yes(i,"receiverCreated")){
                    Vector2f at=PTSDCrisisAPI.findSafePoint(sys,exit,3500,5000,new Random(i.id.hashCode()+9));
                    receiver=at==null?null:spawn(state,i,sys,at,"receiver");
                    if(receiver!=null){set(i,"receiverCreated",true);move(receiver,exit,"接近交接点");}
                }
                if(alive(receiver)&&receiver.getBattle()==null&&near(receiver,collector,900)){
                    // 两支实体舰队接近并保持接触0.25日才转移；玩家须亲眼记录且未被侦测。
                    if(num(i,"handoff")==0)set(i,"handoff",day+.25f);
                    if(day>=num(i,"handoff")){
                        set(i,"mode","TRANSFERRED");set(i,"cargoRole","receiver");set(i,"collectorEnd",day+1);set(i,"receiverEnd",day+1);
                        if(visible&&receiver.isVisibleToPlayerFleet()&&num(i,"tail")>=.15f&&
                                !player.isVisibleToSensorsOf(collector)&&!player.isVisibleToSensorsOf(receiver))finish(state,i,"TAIL");
                    }
                }else set(i,"handoff",0);
                if(day-num(i,"arrival")>3&&"CARRYING".equals(get(i,"mode"))){set(i,"mode","LOST");set(i,"collectorEnd",day);}
            }
        }
        // 成功夺取后即使任务已结算，仍保留追索和实体退场。零强度时等待，不绕过难度曲线。
        if(yes(i,"queued")&&!yes(i,"spawned")&&day>=num(i,"pursuitDay")){
            Vector2f at=PTSDCrisisAPI.findSafePoint(sys,anchor==null?sys.getCenter():anchor,6000,8000,new Random(i.id.hashCode()+41));
            CampaignFleetAPI response=at==null?null:spawn(state,i,sys,at,"pursuer");
            if(response!=null){set(i,"spawned",true);response.getMemoryWithoutUpdate().unset(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE);
                if(!yes(i,"searchKnown")&&anchor!=null){set(i,"searchX",anchor.getLocation().x);set(i,"searchY",anchor.getLocation().y);}}
        }
        if(!yes(i,"pursuerReturning"))search(i,fleet(i,"pursuer"),player);
        for(String role:ROLES){CampaignFleetAPI f=fleet(i,role);if(!alive(f)||f.getBattle()!=null)continue;
            if(day>=num(i,role+"End")){
                if(role.equals(get(i,"cargoRole"))&&("CARRYING".equals(get(i,"mode"))||"TRANSFERRED".equals(get(i,"mode"))))set(i,"mode","LOST");
                if(!yes(i,role+"Returning")){move(f,exit,"撤离残片区域");set(i,role+"Returning",true);}
                if(near(f,exit,500)||exit==null&&!f.isVisibleToPlayerFleet()){PTSDEventFleetReturn.credit(f);PTSDCampaignRuntime.leaving(f);f.despawn();}
            }
        }
    }
    private static void remove(String id){SectorEntityToken e=entity(id);if(e!=null&&e.getContainingLocation()!=null){PTSDCampaignRuntime.leaving(e);e.getContainingLocation().removeEntity(e);}}
    public void onExpired(PTSDNewsSiteAPI.SiteContext c){
        if(c.incident.eventData!=null&&"true".equals(c.incident.eventData.get("framework.devDiscard"))){
            for(String role:ROLES){CampaignFleetAPI f=fleet(c.incident,role);if(f!=null&&f.getBattle()==null){f.getMemoryWithoutUpdate().set("$PTSD_dev_discarded",true);remove(f.getId());}}
            if(c.incident.siteEntityIds!=null)for(String id:new ArrayList<String>(c.incident.siteEntityIds))remove(id);
            remove(get(c.incident,"drop"));
            set(c.incident,"queued",false);set(c.incident,"spawned",true);
        }
        // 通用清理不能删除交战舰队；每队保留 Runtime 的真实撤离与单次返还。
        for(String role:ROLES)if(c.incident.siteEntityIds!=null)c.incident.siteEntityIds.remove(get(c.incident,role));
    }
    public void onFinished(NewsEventContext c){if(!yes(c.incident,"queued"))set(c.incident,"collectorEnd",c.day);}
    static boolean recoverable(PTSDCrisisState.CrisisIncident i,SectorEntityToken target){
        if(target==null||i.investigationOutcome!=1||!i.siteMaterialized||yes(i,"taken")||i.investigationResolved)return false;String mode=get(i,"mode");
        if("DROPPED".equals(mode))return target.getId().equals(get(i,"drop"));
        return ("APPROACH".equals(mode)||"UNGUARDED".equals(mode))&&target.getId().equals(get(i,"anchor"))&&!near(fleet(i,"collector"),target,1200);
    }
    public InteractionDialogPlugin createDialog(NewsEventContext c,SectorEntityToken target){
        // 历史通用现场继续走旧对话；属实现场未显现时不伪造可打捞的残片。
        return target instanceof CampaignFleetAPI||c.incident.scene!=null||c.incident.investigationOutcome==1&&!c.incident.siteMaterialized?null:new FragmentDialog(c,target);
    }
    private static final class FragmentDialog implements InteractionDialogPlugin {
        final NewsEventContext c;final SectorEntityToken target;InteractionDialogAPI dialog;
        FragmentDialog(NewsEventContext c,SectorEntityToken target){this.c=c;this.target=target;}
        public void init(InteractionDialogAPI d){dialog=d;d.getOptionPanel().clearOptions();
            if(c.incident.investigationOutcome!=1){d.getTextPanel().addPara("打捞组回传的原始谱线来自常规推进器残料。报告中的周期性抽动与传感器校准周期相同，校正后不再出现。");d.getOptionPanel().addOption("保存原始谱线与校准记录","disprove");}
            else{String mode=get(c.incident,"mode");
                d.getTextPanel().addPara("CARRYING".equals(mode)?"切割边缘仍在发热，残片已不在原位。附近留下了一段朝跃迁口延伸的航迹。":
                        "TRANSFERRED".equals(mode)||"LOST".equals(mode)?"凹槽已经空了。残留质量记录在此中断，没有证据指出下一段航程。":
                        "DROPPED".equals(mode)?"破裂的封装漂在载运队失去信号的位置。内部结构仍在抽动。":
                        "金属在牵引光束里缩成一个结。它没有发光，仪器却坚持把它画成一道亮线。远处，无应答舰影正在靠近。");
                d.getOptionPanel().addOption("回收关键残片","recover");d.getOptionPanel().setEnabled("recover",recoverable(c.incident,target));
                d.getOptionPanel().addOption("只封存残留记录，结束调查","partial");}
            d.getOptionPanel().addOption("返回航行，观察或截击回收队","leave");
        }
        public void optionSelected(String text,Object option){
            if(c.incident.investigationResolved){dialog.dismiss();return;}
            if("recover".equals(option)&&recoverable(c.incident,target)){
                String route="DROPPED".equals(get(c.incident,"mode"))?"INTERCEPT":"RACE";
                set(c.incident,"taken",true);set(c.incident,"mode","RECOVERED");
                set(c.incident,"searchX",target.getLocation().x);set(c.incident,"searchY",target.getLocation().y);set(c.incident,"searchKnown",true);
                queuePursuit(c.incident,PTSDCrisisState.getDay());finish(c.state,c.incident,route);
                dialog.getTextPanel().addPara(resultFor(route));dialog.getOptionPanel().clearOptions();dialog.getOptionPanel().addOption("离开","leave");
            }else if("partial".equals(option)){
                c.incident.effectSummary="只封存了熔蚀面与残留质量读数，没有取得关键残片或连续交接证据。";
                c.finish(PTSDTaskRules.InvestigationResult.INCONCLUSIVE,true);dialog.dismiss();
            }else if("disprove".equals(option)&&c.incident.investigationOutcome!=1){
                c.incident.investigationDisproofEvidence="原始谱线与常规推进器残料一致，周期性抽动随校准修正消失。";
                c.finish(PTSDTaskRules.InvestigationResult.DISPROVED,true);dialog.dismiss();
            }else dialog.dismiss();
        }
        public void advance(float amount){}public void optionMousedOver(String t,Object o){}public void backFromEngagement(EngagementResultAPI r){}
        public Object getContext(){return c.incident;}public Map<String,MemoryAPI> getMemoryMap(){return null;}
    }
    /** 保存 incident 引用，历史列表裁剪也不会丢失已经排定的追兵和退场账本。 */
    public static final class Runtime implements EveryFrameScript {
        private final PTSDCrisisState.CrisisIncident incident;private boolean done;
        public Runtime(PTSDCrisisState.CrisisIncident incident){this.incident=incident;}
        public boolean isDone(){return done;}public boolean runWhilePaused(){return false;}
        public void advance(float amount){
            PTSDCrisisState state=PTSDCrisisState.get();if(state==null)return;
            try{tick(state,incident,PTSDCrisisState.getDay());}
            catch(RuntimeException error){incident.eventError="P-10现场推进："+error.getMessage();Global.getLogger(P10Event.class).error(incident.eventError,error);done=true;return;}
            boolean live=false;for(String role:ROLES)if(alive(fleet(incident,role)))live=true;
            done=incident.investigationResolved&&!live&&(!yes(incident,"queued")||yes(incident,"spawned"));
        }
    }
    public static String status(PTSDCrisisState.CrisisIncident i){return "P-10 状态 "+get(i,"mode")+" / 路线 "+get(i,"route")+" / 隐蔽尾随 "+num(i,"tail")+"日 / 追索已生成 "+yes(i,"spawned");}
    public static List<CampaignFleetAPI> fleets(PTSDCrisisState.CrisisIncident i){List<CampaignFleetAPI> result=new ArrayList<CampaignFleetAPI>();for(String r:ROLES){CampaignFleetAPI f=fleet(i,r);if(alive(f))result.add(f);}return result;}
    @Override public String devStatus(NewsEventContext c){return status(c.incident);}
    @Override public List<String> devFleetIds(NewsEventContext c){List<String> ids=new ArrayList<String>();for(CampaignFleetAPI f:fleets(c.incident))ids.add(f.getId());return ids;}
    @Override public Map<String,String> devActions(NewsEventContext c){
        Map<String,String> actions=new LinkedHashMap<String,String>();
        actions.put("load","P-10：直接进入已装载运输段（测试尾随/截击）");
        actions.put("carrier","P-10：导航实际载运者（DEV）");return actions;
    }
    @Override public String runDevAction(NewsEventContext c,String action){
        if(!Global.getSettings().isDevMode())return "仅DEV可用。";
        if("load".equals(action)){
            CampaignFleetAPI collector=fleet(c.incident,"collector");SectorEntityToken exit=entity(get(c.incident,"exit"));
            if(c.incident.investigationResolved||!alive(collector)||collector.getBattle()!=null||exit==null||!"APPROACH".equals(get(c.incident,"mode")))return "需要尚未装载、未交战的属实现场。";
            set(c.incident,"mode","CARRYING");set(c.incident,"cargoRole","collector");move(collector,exit,"携残片前往交接");return "进入运输段；未赠送尾随证据或调查奖励。";
        }
        if("carrier".equals(action)){CampaignFleetAPI carrier=fleet(c.incident,get(c.incident,"cargoRole"));if(!alive(carrier))return "载运者已不在场。";Global.getSector().layInCourseFor(carrier);return "已导航实际载运者；仅用于DEV验收。";}
        return "未知操作。";
    }
}
