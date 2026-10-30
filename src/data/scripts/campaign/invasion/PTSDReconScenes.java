package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.fleets.*;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import java.io.Serializable;
import java.util.*;
import java.awt.Color;
import com.fs.starfarer.api.loading.CampaignPingSpec;

/** Campaign encounters, not repeated interaction-dialog samples. All clocks/choices persist. */
public final class PTSDReconScenes {
    public static final class Progress implements Serializable {
        public String enemy, destination, station;
        public int step;
        public float observed, seconds, nextPulse, nextTick, born;
        public boolean escaping, pressure, closed, observedOnce;
        public boolean waitingForArrival, arrivedAtSearch, searchReady;
        public float searchX, searchY, beaconIn, spawnRetryIn;
        public int beaconSequence;
    }
    private PTSDReconScenes() { }
    static boolean handles(PTSDNewsScene.Kind kind) {
        return kind==PTSDNewsScene.Kind.RECON_FIRE_CONTROL || kind==PTSDNewsScene.Kind.RECON_SURRENDER ||
                kind==PTSDNewsScene.Kind.STATION_BLACKOUT || kind==PTSDNewsScene.Kind.DARK_TRANSIT;
    }
    static CampaignFleetAPI station(StarSystemAPI sys) {
        for(MarketAPI m:Global.getSector().getEconomy().getMarkets(sys)) {
            if(PTSDPrewarReconDirector.omega(m.getFactionId()))continue;
            CampaignFleetAPI f=Misc.getStationFleet(m);
            if(f!=null && f.isAlive() && !f.isEmpty() && f.getBattle()==null&&!f.getMemoryWithoutUpdate().contains("$PTSD_station_pressure"))return f;
        }
        return null;
    }
    static String unavailable(PTSDNewsScene.Kind kind,StarSystemAPI sys) {
        if(PTSDProtectedFaction.system(sys))return "该星系受危机保护，不生成主动压制或攻击事件";
        if(sys.getJumpPoints().isEmpty())return "缺少真实跃迁出口";
        if(kind==PTSDNewsScene.Kind.STATION_BLACKOUT && station(sys)==null)return "需要一座现存且未在交战的空间站";
        return null;
    }
    static String title(PTSDNewsScene.Kind kind) {
        switch(kind) {
            case RECON_FIRE_CONTROL:return "火控试探";
            case RECON_SURRENDER:return "弃械舰队";
            case STATION_BLACKOUT:return "站外压制";
            default:return "无应答航迹";
        }
    }
    static String intro(PTSDCrisisState.CrisisIncident i) {
        switch(i.scene.kind) {
            case JUMP_BLOCKADE:return "跃迁口每次聚拢，便被一道反向脉冲推散。导航员已排除这条出口；其他出口仍可通行。超空间一侧有持续的高能接触，切勿把它当作普通航标。可观察、等待封锁结束，或自行挑战封锁部队。";
            case RECON_FIRE_CONTROL:return "远处的舰影将火控信号压在你的舰队轮廓上，却迟迟没有开火。保持距离可记录照射周期；迫近会改变它的行动。";
            case RECON_SURRENDER:return "这支海盗舰队与你在新闻里看到的描述近乎相符。而对方似乎也并没有向其他海盗一样高高兴兴的找架打，恰恰相反，舰队的炮塔全部都瞄向了一个和你的航向近乎相反的点位，对方舰队的发动机也停在低功率。看起来它们不是很愿意说出他们想做什么，只请求一条离开这里的路线。你可以掩护撤离，也可以离开；营救按实际脱险结算。";
            case STATION_BLACKOUT:return "空间站周围不时掠过大范围扫描回波。请在站外空域搜寻脉冲来源，无需与空间站交互；抵近后留意其他方向的阻断与扫描闪光。";
            default:return "无应答舰影正沿真实出口航行。保持隐蔽、保持接触，才能记录它的路线；一旦被发现，它可能加速、改道，或转向你。";
        }
    }
    static CampaignFleetAPI spawn(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,StarSystemAPI sys,Vector2f at) {
        CampaignFleetAPI f=PTSDNewsScene.spawnEnemy(state,i,sys,at,PTSDCrisisFleetLimits.forPhase(state,PTSDCrisisState.getDay()).perFleetFp,30,false);
        if(f!=null){
            f.getMemoryWithoutUpdate().set(PTSDSceneScoutBehavior.CONTROLLED,true);
            f.getMemoryWithoutUpdate().set("$PTSD_encounter_response_installed",true);
            f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,true);
        }
        return f;
    }
    static boolean build(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,StarSystemAPI sys,Random rng) {
        PTSDNewsScene.State s=i.scene;
        if(s.recon!=null)return !s.nodes.isEmpty();
        if(data.scripts.PTSDDifficultySettings.spawningPaused()){s.lastError="难度曲线为0，等待恢复生成";return false;}
        SectorEntityToken exit=sys.getJumpPoints().get(rng.nextInt(sys.getJumpPoints().size()));
        CampaignFleetAPI st=s.kind==PTSDNewsScene.Kind.STATION_BLACKOUT?station(sys):null;
        if(s.kind==PTSDNewsScene.Kind.STATION_BLACKOUT&&st==null)return false;
        Vector2f base=st==null?PTSDNewsScene.offset(exit.getLocation(),170,3200):PTSDNewsScene.offset(st.getLocation(),rng.nextFloat()*360,2400);
        if(st!=null) {
            Vector2f search=PTSDCrisisAPI.findSafePoint(sys,st,2000f,2800f,rng);
            if(search==null){s.lastError="空间站附近没有安全搜索空域";return false;}
            Progress p=new Progress();p.station=st.getId();p.destination=exit.getId();p.born=PTSDCrisisState.getDay();
            p.searchReady=true;p.searchX=search.x;p.searchY=search.y;p.waitingForArrival=true;s.recon=p;
            st.getMemoryWithoutUpdate().set("$PTSD_station_pressure",i.id);
            // Keep the station as a simulation anchor, without a quest/interact marker on it.
            if(!s.nodes.contains(st.getId()))s.nodes.add(st.getId());
            Global.getSector().addScript(new PressureClock(i.id));
            return true;
        }
        CampaignFleetAPI enemy=spawn(state,i,sys,PTSDNewsScene.offset(base,65,2200));
        if(enemy==null){s.lastError="阶段预算未能组成现场队，等待后再试";return false;}
        Progress p=new Progress();p.enemy=enemy.getId();p.destination=exit.getId();p.born=PTSDCrisisState.getDay();s.recon=p;
        enemy.setName(s.kind==PTSDNewsScene.Kind.DARK_TRANSIT?"无应答的穿行舰影":"持续测定中的未知舰影");
        if(s.kind==PTSDNewsScene.Kind.RECON_SURRENDER) {
            CampaignFleetAPI pirates=FleetFactoryV3.createFleet(new FleetParamsV3(base,Factions.PIRATES,.4f,FleetTypes.PATROL_LARGE,70,0,0,0,0,0,0));
            if(pirates==null||pirates.isEmpty()){PTSDNewsScene.remove(enemy.getId());s.fleets.remove(enemy.getId());s.recon=null;return false;}
            sys.addEntity(pirates);pirates.setLocation(base.x,base.y);PTSDCampaignRuntime.entityAdded(pirates);
            pirates.setName("海盗舰队");pirates.setTransponderOn(true);
            pirates.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,true);
            pirates.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS,true);
            pirates.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS,true);
            pirates.addAssignment(FleetAssignment.HOLD,sys.createToken(base),30,"等待");
            for(com.fs.starfarer.api.fleet.FleetMemberAPI m:pirates.getFleetData().getMembersListCopy())m.getRepairTracker().setCR(.12f);
            s.civilian=pirates.getId();s.owned.add(pirates.getId());PTSDNewsScene.bind(i,pirates);
            enemy.addAssignment(FleetAssignment.HOLD,sys.createToken(enemy.getLocation()),30,"观察目标");
        }else{
            PTSDNewsScene.bind(i,enemy);
            enemy.addAssignment(s.kind==PTSDNewsScene.Kind.DARK_TRANSIT?FleetAssignment.GO_TO_LOCATION:FleetAssignment.HOLD,
                    s.kind==PTSDNewsScene.Kind.DARK_TRANSIT?exit:sys.createToken(enemy.getLocation()),30,"测定航路");
        }
        return true;
    }
    static boolean observing(CampaignFleetAPI player,CampaignFleetAPI enemy,float max){
        return player!=null&&enemy!=null&&player.getContainingLocation()==enemy.getContainingLocation()&&enemy.isVisibleToPlayerFleet()&&Misc.getDistance(player,enemy)<max;
    }
    static void advance(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,float day){
        PTSDNewsScene.State s=i.scene;Progress p=s.recon;if(p==null||s.cleaned)return;
        if(day<p.nextTick)return;float dt=Math.min(.1f,Math.max(0,day-s.lastDay));s.lastDay=day;p.nextTick=day+.025f;
        CampaignFleetAPI enemy=PTSDNewsScene.fleet(p.enemy), player=Global.getSector().getPlayerFleet(), civil=PTSDNewsScene.fleet(s.civilian);
        if(i.investigationAbandoned||day>=i.investigationExpiresDay){close(i);if(!s.resolved)PTSDNewsScene.finishAs(state,i,"调查窗口已结束，保留已有接触记录。",PTSDTaskRules.InvestigationResult.EXPIRED);}
        if(s.resolved){
            close(i);
            boolean pending=false;
            for(String id:new ArrayList<String>(s.fleets.keySet())){CampaignFleetAPI f=PTSDNewsScene.fleet(id);
                if(f==null){s.fleets.remove(id);continue;}if(f.getBattle()!=null){pending=true;continue;}
                if(!p.escaping){PTSDSceneScoutBehavior.install(f,null,true,true);p.escaping=true;}
                if(day>=p.born+30){PTSDNewsScene.retire(f);s.fleets.remove(id);}else pending=true;
            }
            if(civil!=null&&civil.getBattle()==null){SectorEntityToken exit=PTSDNewsScene.entity(p.destination);
                if(!p.closed&&exit!=null){civil.clearAssignments();civil.addAssignment(FleetAssignment.GO_TO_LOCATION,exit,30,"撤离现场");p.closed=true;}
                if(exit==null||civil.getContainingLocation()!=exit.getContainingLocation()||Misc.getDistance(civil,exit)<500||day>=p.born+30){PTSDNewsScene.remove(civil.getId());}else pending=true;}
            s.cleaned=!pending;return;
        }
        boolean seen=observing(player,enemy,3200);if(seen){p.observedOnce=true;PTSDJeOtloesManager.recordParticipation(state,"incident:"+i.id);}
        if(s.kind==PTSDNewsScene.Kind.RECON_SURRENDER){
            if(civil==null||civil.isEmpty()){PTSDNewsScene.finishAs(state,i,"弃械船队的识别信号消失，没有确认任何船只脱险。",PTSDTaskRules.InvestigationResult.TARGET_LOST);return;}
            if(s.escort){SectorEntityToken exit=PTSDNewsScene.entity(p.destination);
                if(civil.getBattle()==null&&exit!=null&&Misc.getDistance(civil,exit)<500){s.rescuedShips=civil.getFleetData().getMembersListCopy().size();PTSDNewsScene.finish(state,i,"弃械船队抵达出口。最后一艘船的通讯员只说了谢谢，随后关闭应答器。",true);return;}
                if(enemy!=null&&enemy.getBattle()==null&&!p.escaping&&p.step==0&&Misc.getDistance(enemy,civil)<2200){
                    p.step=1;
                    if(s.branch==2){enemy.getMemoryWithoutUpdate().unset(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE);enemy.clearAssignments();enemy.addAssignment(FleetAssignment.INTERCEPT,civil,5,"截断撤离");}
                    else {scan(enemy);escape(i,enemy);}
                }
            }
            return;
        }
        if(s.kind==PTSDNewsScene.Kind.STATION_BLACKOUT && p.waitingForArrival)return;
        if(enemy==null||enemy.isEmpty()){
            close(i);PTSDNewsScene.finishAs(state,i,p.observedOnce?"未知接触已消失。先前记录与报道相符，无法仅凭失联断言其已被摧毁。":"到达时未知舰影已离场，未获得完整观测。",p.observedOnce?PTSDTaskRules.InvestigationResult.CONFIRMED:PTSDTaskRules.InvestigationResult.INCONCLUSIVE);return;
        }
        if(s.kind==PTSDNewsScene.Kind.STATION_BLACKOUT)return; // Seconds handled by PressureClock, including interrupted cleanup.
        if(s.kind==PTSDNewsScene.Kind.RECON_FIRE_CONTROL){
            if(seen && enemy.getBattle()==null){
                if(day>=p.nextPulse){p.nextPulse=day+.12f;scan(enemy);if(player.isVisibleToSensorsOf(enemy)){Global.getSector().addPing(player,Pings.INTERDICT,java.awt.Color.RED);p.observed+=1;}}
                if(Misc.getDistance(player,enemy)<1000&&!p.escaping&&p.step==0){
                    p.step=1;
                    if(s.branch==2){enemy.getMemoryWithoutUpdate().unset(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE);enemy.clearAssignments();enemy.addAssignment(FleetAssignment.INTERCEPT,player,1,"接近火控目标");}
                    else {if(s.branch==1)PTSDNewsScene.pulse(enemy,false);escape(i,enemy);}
                }
                if(p.observed>=3){PTSDNewsScene.finish(state,i,"三轮照射的起止均被记录；每次锁定都足以发射，来袭警报却始终没有响起。",true);}
            }
        }else{
            // Tracking needs actual visibility and distance; leaving/reloading cannot fabricate samples.
            if(seen&&enemy.getBattle()==null&&!player.isVisibleToSensorsOf(enemy))p.observed+=dt;
            if(seen&&player.isVisibleToSensorsOf(enemy)&&p.step==0){p.step=1;scan(enemy);
                if(s.branch==2){enemy.getMemoryWithoutUpdate().unset(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE);enemy.clearAssignments();enemy.addAssignment(FleetAssignment.INTERCEPT,player,2,"转向接触来源");}
                else if(s.branch==1&&enemy.getStarSystem()!=null&&enemy.getStarSystem().getJumpPoints().size()>1){
                    for(SectorEntityToken turn:enemy.getStarSystem().getJumpPoints())if(!turn.getId().equals(p.destination)){p.destination=turn.getId();enemy.clearAssignments();enemy.addAssignment(FleetAssignment.GO_TO_LOCATION,turn,20,"变更离场航路");break;}
                }else escape(i,enemy);
            }
            SectorEntityToken exit=PTSDNewsScene.entity(p.destination);
            if(p.observed>=.2f)PTSDNewsScene.finish(state,i,"你保持隐蔽记录了一段连续航程。无应答目标的引擎特征与走私商船不符，真实出口已写入观测日志。",true);
            else if(enemy.getBattle()==null&&exit!=null&&Misc.getDistance(enemy,exit)<500){PTSDNewsScene.finishAs(state,i,"舰影进入出口前切断了尾迹。你只保留不连续的航行片段。",PTSDTaskRules.InvestigationResult.INCONCLUSIVE);escape(i,enemy);}
        }
    }
    static void scan(CampaignFleetAPI f){if(f.getAbility(Abilities.SENSOR_BURST)==null)f.addAbility(Abilities.SENSOR_BURST);if(f.getAbility(Abilities.SENSOR_BURST)!=null){f.getAbility(Abilities.SENSOR_BURST).setCooldownLeft(0);f.getAbility(Abilities.SENSOR_BURST).activate();}}
    static void escape(PTSDCrisisState.CrisisIncident i,CampaignFleetAPI f){if(i.scene.recon.escaping)return;i.scene.recon.escaping=true;f.clearAssignments();PTSDSceneScoutBehavior.install(f,null,true,true);}
    static void close(PTSDCrisisState.CrisisIncident i){
        if(i==null||i.scene==null||i.scene.recon==null)return;
        CampaignFleetAPI station=PTSDNewsScene.fleet(i.scene.recon.station);
        if(station!=null){station.getStats().getSensorRangeMod().unmodify("PTSD_pressure_"+i.id);
            if(i.id.equals(station.getMemoryWithoutUpdate().getString("$PTSD_station_pressure")))station.getMemoryWithoutUpdate().unset("$PTSD_station_pressure");}
        i.scene.recon.pressure=false;
    }
    static final Color BLACKOUT_BEIGE=new Color(224,211,178);
    static final Color BLACKOUT_PURPLE=new Color(176,145,209);
    static CampaignPingSpec blackoutPing(float range,boolean scan){
        CampaignPingSpec spec=new CampaignPingSpec();
        spec.setUseFactionColor(false);spec.setColor(scan?BLACKOUT_BEIGE:BLACKOUT_PURPLE);
        spec.setWidth(scan?9f:20f);spec.setRange(range);spec.setDuration(scan?3.5f:1.3f);
        spec.setAlphaMult(.85f);spec.setInFraction(.08f);spec.setNum(1);
        return spec;
    }
    static boolean patrolObserver(CampaignFleetAPI f,CampaignFleetAPI player,CampaignFleetAPI station){
        return f!=null&&f!=player&&f!=station&&f.isAlive()&&!f.isExpired()&&!f.isStationMode()&&
                !PTSDJeOtloesManager.isOmegaFleet(f);
    }
    static boolean hiddenFromPatrols(StarSystemAPI sys,SectorEntityToken candidate,
                                      CampaignFleetAPI player,CampaignFleetAPI station,boolean actualFleet){
        for(CampaignFleetAPI other:PTSDCampaignRuntime.fleets(sys))if(patrolObserver(other,player,station)){
            if(actualFleet){if(candidate.isVisibleToSensorsOf(other))return false;}
            else if(Misc.getDistance(candidate,other)<Math.max(1000f,other.getMaxSensorRangeToDetect(candidate))+250f)return false;
        }
        return true;
    }
    static Vector2f blackoutSpawnPoint(StarSystemAPI sys,CampaignFleetAPI station,CampaignFleetAPI player,
                                       SectorEntityToken candidate,boolean actualFleet,int seed){
        for(int ring=0;ring<3;ring++)for(int step=0;step<24;step++){
            float distance=Math.max(station.getRadius()+600f,1400f)+ring*500f;
            if(distance>2800f)continue;
            Vector2f point=PTSDNewsScene.offset(station.getLocation(),Math.floorMod(seed,360)+step*15f,distance);
            if(!PTSDCrisisAPI.isSafePoint(sys,point)||Misc.getDistance(player.getLocation(),point)<1000f)continue;
            candidate.setLocation(point.x,point.y);
            if(hiddenFromPatrols(sys,candidate,player,station,actualFleet))return point;
        }
        return null;
    }
    static void distantBlackoutPulse(PTSDCrisisState.CrisisIncident i,CampaignFleetAPI station,CampaignFleetAPI player){
        Progress p=i.scene.recon;StarSystemAPI sys=station.getStarSystem();
        if(sys==null)return;
        Random rng=new Random((i.id+":blackout-pulse:"+p.beaconSequence).hashCode());
        Vector2f center=sys.getCenter()==null?station.getLocation():sys.getCenter().getLocation();
        Vector2f point=PTSDNewsScene.offset(center,rng.nextFloat()*360f,7000f+rng.nextFloat()*9000f);
        float range=Math.max(18000f+rng.nextFloat()*8000f,Misc.getDistance(point,player.getLocation())+5000f);
        CampaignPingSpec spec=blackoutPing(range,true);
        spec.setColor(p.beaconSequence++%2==0?BLACKOUT_BEIGE:BLACKOUT_PURPLE);
        Global.getSector().addPing(sys.createToken(point),spec);
        p.beaconIn=6f+rng.nextFloat()*4f;
    }
    static void activeBlackoutPulse(CampaignFleetAPI enemy,CampaignFleetAPI player){
        if(player==null||player.getContainingLocation()!=enemy.getContainingLocation())return;
        float range=Math.max(6000f,Misc.getDistance(enemy,player)+1500f);
        // Vanilla campaign pings render independently of fleet visibility. Show the source's
        // pulse without revealing its identity, changing sensors or spawning a fake fleet.
        Global.getSector().addPing(enemy,blackoutPing(range,false));
        Global.getSector().addPing(enemy,blackoutPing(range,true));
    }
    static boolean stageBlackout(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,
                                  CampaignFleetAPI station,CampaignFleetAPI player){
        Progress p=i.scene.recon;StarSystemAPI sys=station.getStarSystem();
        SectorEntityToken probe=sys.createToken(station.getLocation());probe.setSensorProfile(400f);
        Vector2f at=blackoutSpawnPoint(sys,station,player,probe,false,i.id.hashCode());
        if(at==null){i.scene.lastError="巡逻队仍覆盖站外空域，等待隐蔽位置";return false;}
        CampaignFleetAPI enemy=spawn(state,i,sys,at);
        if(enemy==null){i.scene.lastError="阶段额度未能组成侦察队，等待后再试";return false;}
        // Recheck with the real fleet profile and current sensor rules before releasing it.
        at=blackoutSpawnPoint(sys,station,player,enemy,true,i.id.hashCode());
        if(at==null){PTSDNewsScene.remove(enemy.getId());i.scene.fleets.remove(enemy.getId());
            i.scene.lastError="实际侦察队无法避开巡逻传感器，等待下一窗口";return false;}
        p.enemy=enemy.getId();p.waitingForArrival=false;p.born=PTSDCrisisState.getDay();
        enemy.setName("持续测定中的未知舰影");
        enemy.addAssignment(FleetAssignment.HOLD,sys.createToken(at),30,"测定轨道设施回波");
        i.scene.lastError="";scan(enemy);activeBlackoutPulse(enemy,player);p.beaconIn=6f;
        return true;
    }
    static final class PressureClock implements EveryFrameScript {
        private final String id; private boolean done;
        PressureClock(String id){this.id=id;}
        public boolean isDone(){return done;} public boolean runWhilePaused(){return false;}
        public void advance(float amount){
            if(done)return;
            PTSDCrisisState.CrisisIncident i=PTSDCrisisAPI.getIncident(id);
            if(i==null||i.scene==null||i.scene.recon==null){done=true;return;}
            Progress p=i.scene.recon;CampaignFleetAPI enemy=PTSDNewsScene.fleet(p.enemy),st=PTSDNewsScene.fleet(p.station);
            if(i.scene.resolved||i.investigationAbandoned||PTSDCrisisState.getDay()>=i.investigationExpiresDay){
                close(i);done=true;
                if(!i.scene.resolved)PTSDNewsScene.finishAs(PTSDCrisisState.get(),i,"调查窗口已结束，保留已有接触记录。",PTSDTaskRules.InvestigationResult.EXPIRED);
                return;
            }
            CampaignFleetAPI player=Global.getSector().getPlayerFleet();
            if(p.waitingForArrival && st!=null){
                if(player==null||player.getContainingLocation()!=st.getContainingLocation())return;
                float elapsed=Math.max(0,amount);p.beaconIn-=elapsed;p.spawnRetryIn-=elapsed;
                boolean arrived=p.searchReady&&Misc.getDistance(player.getLocation(),new Vector2f(p.searchX,p.searchY))<=1400f;
                if(arrived){p.arrivedAtSearch=true;
                    if(p.spawnRetryIn<=0f&&st.getBattle()==null){p.spawnRetryIn=3f;stageBlackout(PTSDCrisisState.get(),i,st,player);}}
                if(p.waitingForArrival){if(p.beaconIn<=0f)distantBlackoutPulse(i,st,player);return;}
                // The approach frame must not count as already elapsed station pressure.
                return;
            }
            if(i.scene.resolved||i.investigationAbandoned||enemy==null||st==null||enemy.getBattle()!=null||enemy.getContainingLocation()!=st.getContainingLocation()){
                close(i);done=true;if(!i.scene.resolved)PTSDNewsScene.finishAs(PTSDCrisisState.get(),i,p.seconds>0?"施扰接触被打断，空间站的本地回波重新出现。":"施扰舰已离场，没有取得完整压制记录。",p.seconds>0?PTSDTaskRules.InvestigationResult.CONFIRMED:PTSDTaskRules.InvestigationResult.INCONCLUSIVE);return;
            }
            if(Misc.getDistance(enemy,st)>2800)return;
            if(st.getBattle()!=null){close(i);return;}
            p.pressure=true;st.getStats().getSensorRangeMod().modifyMult("PTSD_pressure_"+id,.1f,"未知电子压制");
            p.seconds+=Math.max(0,amount);p.beaconIn-=Math.max(0,amount);
            if(observing(player,enemy,6000))p.observedOnce=true;
            if(p.beaconIn<=0f){p.beaconIn=6f;activeBlackoutPulse(enemy,player);}
            if(p.seconds>=70){close(i);done=true;PTSDNewsScene.finishAs(PTSDCrisisState.get(),i,p.observedOnce?"站方回波在七十秒后恢复。未知舰影结束传输，开始离场。记录没有显示任何射弹。":"空间站已恢复应答；你未在现场完整观察本次压制。",p.observedOnce?PTSDTaskRules.InvestigationResult.CONFIRMED:PTSDTaskRules.InvestigationResult.INCONCLUSIVE);}
        }
    }
    static abstract class SimpleDialog implements InteractionDialogPlugin {
        protected InteractionDialogAPI dialog;
        public void init(InteractionDialogAPI d){dialog=d;d.getOptionPanel().clearOptions();}
        public void optionMousedOver(String text,Object data){} public void advance(float amount){}
        public void backFromEngagement(com.fs.starfarer.api.combat.EngagementResultAPI result){}
        public Object getContext(){return null;}
        public java.util.Map<String,com.fs.starfarer.api.campaign.rules.MemoryAPI> getMemoryMap(){return null;}
    }
    static InteractionDialogPlugin dialog(final PTSDCrisisState.CrisisIncident i,final SectorEntityToken target){
        if(i.scene.kind==PTSDNewsScene.Kind.STATION_BLACKOUT)return null;
        // Enemy fleets always retain the real battle interaction instead of a scene-only dialog.
        if(i.scene.recon!=null&&target.getId().equals(i.scene.recon.enemy))return null;
        return new SimpleDialog(){
            public void init(InteractionDialogAPI d){super.init(d);d.getTextPanel().addPara(intro(i));
                if(i.scene.kind==PTSDNewsScene.Kind.RECON_SURRENDER&&!i.scene.escort){
                    d.getOptionPanel().addOption("开启火力照射，把他们逼出这个稳定点，看看他们究竟在害怕什么","escort");}
                if(! (i.scene.kind==PTSDNewsScene.Kind.RECON_SURRENDER)){
                    d.getOptionPanel().addOption("尝试正常交流","normal");}
                d.getOptionPanel().addOption("离开","leave");}
            public void optionSelected(String text,Object option){
                if("normal".equals(option)){PTSDPortInvestigation.openNormal(dialog,target);return;}
                if("escort".equals(option)){CampaignFleetAPI civilian=PTSDNewsScene.fleet(i.scene.civilian),enemy=PTSDNewsScene.fleet(i.scene.recon.enemy);SectorEntityToken exit=PTSDNewsScene.entity(i.scene.recon.destination);
                    if(civilian!=null&&exit!=null&&!i.scene.escort){i.scene.escort=true;civilian.getMemoryWithoutUpdate().unset(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS);civilian.clearAssignments();civilian.addAssignment(FleetAssignment.GO_TO_LOCATION,exit,30,"撤离封锁区域");
                        if(enemy!=null){enemy.clearAssignments();enemy.addAssignment(FleetAssignment.GO_TO_LOCATION,civilian,20,"跟随撤离舰队");}PTSDJeOtloesManager.recordParticipation(PTSDCrisisState.get(),"incident:"+i.id);}}
                dialog.dismiss();}
        };
    }
}
