package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.shard.PTSD_BaseShard_Util;
import java.io.Serializable;

/** Je is sent on a real voyage. No mortality roll is allowed at the dispatch button. */
public final class PTSDJeWarExpedition {
    public static boolean unavailable(PTSDCrisisState s){Mission m=s==null||s.late==null||s.late.relief==null?null:s.late.relief.jeExpedition;return m!=null&&(!m.done||m.missing);}
    public static final class Mission implements Serializable {
        public String system,source,status="航行调查",outcome;
        public CampaignFleetAPI escort,pursuer;
        public float started,deadline,returnDeadline;
        public boolean distress,playerEngaged,returning,done,missing,rolled;
        public float failureRoll;
    }
    public static String start(PTSDCrisisState s,PTSDLateState.EventCard card){
        PTSDReliefState r=PTSDReliefState.get(s);
        if(r.jeExpedition!=null&&!r.jeExpedition.done)return "Je的调查航程仍在进行，可在联盟行动页追踪。";
        if(s.jeDead||PTSDJeOtloesManager.isPostTaskBusy(s)||s.jeAgentIncidentId!=null||s.jeMeetingReady)return null;
        StarSystemAPI target=s.resolveSystem(card.systemId);
        if(target==null)for(PTSDLateState.Front front:s.late.fronts.values())if(!front.resolved){target=s.resolveSystem(front.systemId);if(target!=null)break;}
        if(target==null||PTSDProtectedFaction.system(target))return null;
        MarketAPI source=PTSDReliefDirector.nearestSafe(s,target.getLocation(),"independent");if(source==null)return null;
        CampaignFleetAPI escort=PTSDReliefDirector.generate("independent",source,PTSDReliefDirector.cfg("je_escort_fp"));if(escort==null)return null;
        Mission m=new Mission();m.system=target.getId();m.source=source.getId();m.escort=escort;m.started=PTSDCrisisState.getDay();r.jeExpedition=m;
        escort.setName("Je的调查护航队");source.getStarSystem().addEntity(escort);escort.setLocation(source.getPrimaryEntity().getLocation().x,source.getPrimaryEntity().getLocation().y);
        escort.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,true);PTSDCampaignRuntime.entityAdded(escort);Misc.makeImportant(escort,"PTSD_je_war");
        escort.addAssignment(FleetAssignment.GO_TO_LOCATION,target.getCenter(),300,"前往Je指定的调查战区");
        card.systemId=target.getId();card.targetEntityId=escort.getId();PTSDLateEventIntel.ensureIntel();
        return "Je的护航队已从"+source.getName()+"出发。调查进展与可能的求援将在联盟行动页持续更新。";
    }
    static boolean alive(CampaignFleetAPI f){return f!=null&&f.isAlive()&&!f.isEmpty()&&!f.isDespawning();}
    public static void recordBattle(PTSDCrisisState s,CampaignFleetAPI enemy){
        if(s==null||s.late==null||s.late.relief==null)return;Mission m=s.late.relief.jeExpedition;
        if(m!=null&&!m.done&&m.pursuer!=null&&enemy!=null&&m.pursuer.getId().equals(enemy.getId()))m.playerEngaged=true;
    }
    static boolean missing(float roll,int chance){return roll*100<chance;}
    static void advance(PTSDCrisisState s,float day){
        Mission m=PTSDReliefState.get(s).jeExpedition;if(m==null||m.done)return;
        if(m.escort!=null&&m.escort.getBattle()!=null)return;
        if(!alive(m.escort)){fail(s,m,day,"护航队失去联系");return;}
        if(m.returning){MarketAPI source=s.resolveMarket(m.source);
            if(!PTSDReliefDirector.port(source)||Global.getSector().getFaction("independent").isHostileTo(source.getFactionId())){source=PTSDReliefDirector.nearestSafe(s,m.escort.getLocationInHyperspace(),"independent");if(source!=null){m.source=source.getId();routeHome(m,source);}else return;}
            if(m.escort.getStarSystem()==source.getStarSystem()&&Misc.getDistance(m.escort,source.getPrimaryEntity())<1000){retire(m.escort,false);finish(s,m,day,false,m.outcome+"；调查队已返港。");}return;}
        StarSystemAPI target=s.resolveSystem(m.system);if(target==null){fail(s,m,day,"调查战区无法继续访问");return;}
        if(!m.distress){
            if(day-m.started>180){home(s,m,day,"调查航程受阻，Je取消了此次调查");return;}
            if(m.escort.getStarSystem()!=target||Misc.getDistance(m.escort,target.getCenter())>1500)return;
            CampaignFleetAPI enemy=PTSD_BaseShard_Util.createFleet(new FleetParamsV3(target.getLocation(),PTSDCrisisProgress.getActiveFactionId(s),.7f,FleetTypes.PATROL_LARGE,PTSDReliefDirector.cfg("je_pursuit_fp"),0,0,0,0,0,0),PTSDReliefDirector.cfg("je_pursuit_fp"),PTSD_BaseShard_Util.FleetRole.GUARD_ASSAULT);
            if(enemy==null||enemy.isEmpty()){home(s,m,day,"调查结束，没有形成可追踪的拦截");return;}
            m.pursuer=enemy;target.addEntity(enemy);enemy.setLocation(m.escort.getLocation().x+6500,m.escort.getLocation().y+1800);PTSDCampaignRuntime.entityAdded(enemy);
            enemy.getMemoryWithoutUpdate().set("$PTSD_encounter_response_installed",true);
            enemy.addAssignment(FleetAssignment.INTERCEPT,m.escort,Math.max(1,PTSDReliefDirector.cfg("je_rescue_days"))+3,"截击调查护航队");
            m.escort.clearAssignments();m.escort.addAssignment(FleetAssignment.ORBIT_PASSIVE,target.createToken(m.escort.getLocation()),100,"等待接应窗口");
            m.distress=true;m.deadline=day+PTSDReliefDirector.cfg("je_rescue_days");m.status="求援：追兵接近";
            PTSDLateWarDirector.enqueueAlert(s,"je-war-distress:"+m.started,m.system,"Je：有东西跟着我们进来了。坐标已经上传，预计还能坚持约"+PTSDReliefDirector.cfg("je_rescue_days")+"日。",day);return;
        }
        if(m.pursuer!=null&&m.pursuer.getBattle()!=null)return;
        if(!alive(m.pursuer)){
            if(m.playerEngaged)PTSDGeneralStaffDirector.recordContribution(s,"je-war-rescue:"+m.started,"SUPPORT","player",m.system,15);
            home(s,m,day,m.playerEngaged?"追兵已被击退，Je带着调查记录脱险":"追兵被其他力量消灭，Je正在脱离战区");return;
        }
        if(day>=m.deadline)fail(s,m,day,"救援窗口结束");
    }
    static void fail(PTSDCrisisState s,Mission m,float day,String reason){
        if(!m.rolled){m.failureRoll=(float)Math.random();m.rolled=true;}
        if(!alive(m.escort)||missing(m.failureRoll,PTSDReliefDirector.cfg("je_missing_chance"))){retire(m.escort,false);retire(m.pursuer,true);finish(s,m,day,true,reason+"；Je失联，尚无证据确认其死亡。");}
        else home(s,m,day,reason+"；Je放弃记录，自行摆脱追踪");
    }
    static void home(PTSDCrisisState s,Mission m,float day,String text){
        m.returning=true;m.status="撤离返港";m.outcome=text;retire(m.pursuer,true);
        MarketAPI port=s.resolveMarket(m.source);if(!PTSDReliefDirector.port(port))port=PTSDReliefDirector.nearestSafe(s,m.escort.getLocationInHyperspace(),"independent");
        if(port!=null){m.source=port.getId();routeHome(m,port);}
        PTSDLateWarDirector.enqueueAlert(s,"je-war-return:"+m.started,m.system,text,day);
    }
    private static void routeHome(Mission m,MarketAPI port){m.escort.clearAssignments();m.escort.addAssignment(FleetAssignment.GO_TO_LOCATION,port.getPrimaryEntity(),300,"携带Je的调查记录返港");}
    private static void retire(CampaignFleetAPI fleet,boolean refund){if(fleet==null||fleet.getBattle()!=null)return;if(refund)PTSDEventFleetReturn.credit(fleet);if(fleet.getContainingLocation()!=null){PTSDCampaignRuntime.leaving(fleet);fleet.despawn();}}
    static void finish(PTSDCrisisState s,Mission m,float day,boolean missing,String text){m.done=true;m.missing=missing;m.status=missing?"失联，尚无死亡证据":"已返港";m.outcome=text;
        s.jePostTaskBusyUntilDay=day;s.jeNextTaskAvailableDay=day;
        PTSDLateWarDirector.enqueueAlert(s,"je-war-end:"+m.started,m.system,text,day);
    }
}
