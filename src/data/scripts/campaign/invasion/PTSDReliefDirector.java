package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.*;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import java.util.*;
import static data.scripts.campaign.invasion.PTSDReliefState.Stage.*;

/** Quarter-day campaign scheduler. Each task owns one fleet and one defense contribution. */
public final class PTSDReliefDirector {
    public static final String UNIT="$PTSD_relief_unit", PATH="luddic_path";
    private static SectorAPI marketSector;private static float marketDay=-1;private static List<MarketAPI> marketSnapshot;
    static List<MarketAPI> markets(){SectorAPI current=Global.getSector();float day=PTSDCrisisState.getDay();
        if(current!=marketSector||marketSnapshot==null||day<marketDay||day>=marketDay+1){marketSector=current;marketDay=day;marketSnapshot=new ArrayList<MarketAPI>(PTSDOccupationManager.getAllMarkets());}
        return marketSnapshot;}
    static int cfg(String id){return PTSDReliefSettings.value(id);}
    static boolean omega(String f){return PTSDPrewarReconDirector.omega(f);}
    static boolean port(MarketAPI m){return m!=null&&!m.isPlanetConditionMarketOnly()&&m.getPrimaryEntity()!=null&&m.getStarSystem()!=null&&!omega(m.getFactionId());}
    static float capacity(MarketAPI m){return m.getSize()*40f;}
    public static float available(PTSDCrisisState s,MarketAPI m){
        float spent=PTSDReliefState.get(s).sourceSpent.containsKey(m.getId())?PTSDReliefState.get(s).sourceSpent.get(m.getId()):0;
        return Math.max(0,capacity(m)*(1-Math.min(100,cfg("relief_keep"))/100f)-spent);
    }
    static void charge(PTSDCrisisState s,String market,float fp){PTSDReliefState r=PTSDReliefState.get(s);Float old=r.sourceSpent.get(market);r.sourceSpent.put(market,Math.max(0,(old==null?0:old)+fp));}
    public static CampaignFleetAPI generate(String faction,MarketAPI source,float budget){
        if(budget<3)return null;
        FleetParamsV3 p=new FleetParamsV3(source,null,faction,.7f,FleetTypes.PATROL_LARGE,budget,0,0,0,0,0,0);
        CampaignFleetAPI f=FleetFactoryV3.createFleet(p);if(f==null)return null;
        List<FleetMemberAPI> members=f.getFleetData().getMembersListCopy();
        for(int n=members.size()-1;n>=0&&f.getFleetPoints()>budget+.01f;n--)f.getFleetData().removeFleetMember(members.get(n));
        if(f.isEmpty())return null;
        for(FleetMemberAPI m:f.getFleetData().getMembersListCopy())m.getRepairTracker().setCR(m.getRepairTracker().getMaxCR());
        f.setTransponderOn(true);return f;
    }
    static PTSDReliefState.Unit unit(PTSDCrisisState s,String kind,MarketAPI source,StarSystemAPI start,StarSystemAPI target,String market,String group,float budget,boolean physical){
        String faction=kind.startsWith("PATH")?PATH:source.getFactionId();
        CampaignFleetAPI f=generate(faction,source,budget);if(f==null)return null;
        PTSDReliefState.Unit u=new PTSDReliefState.Unit();u.id="relief-"+Misc.genUID();u.kind=kind;u.faction=faction;u.fleet=f;
        u.sourceSystem=start.getId();u.sourceMarket=source==null?null:source.getId();u.targetSystem=target.getId();u.targetMarket=market;u.group=group;
        u.stageDay=u.lastDay=PTSDCrisisState.getDay();u.x=start.getLocation().x;u.y=start.getLocation().y;u.fromX=u.x;u.fromY=u.y;
        f.getMemoryWithoutUpdate().set(UNIT,u.id);f.setName(kind.startsWith("PATH")?"赴难誓师舰队":kind.equals("SCOUT")?"联盟联合侦察队":"联盟远征援军");
        PTSDReliefState.get(s).units.add(u);
        if(source!=null){charge(s,source.getId(),f.getFleetPoints());u.borrowed=true;}
        if(physical){start.addEntity(f);f.setLocation(source.getPrimaryEntity().getLocation().x,source.getPrimaryEntity().getLocation().y);PTSDCampaignRuntime.entityAdded(f);u.physical=true;
            f.addAssignment(FleetAssignment.ORBIT_PASSIVE,source.getPrimaryEntity(),cfg("relief_prepare"),"等待整备完成");}
        return u;
    }
    static List<MarketAPI> sources(final PTSDCrisisState s,final Vector2f point,float maxLy){
        List<MarketAPI> list=new ArrayList<MarketAPI>();for(MarketAPI m:markets())
            if(port(m)&&s.late.coalitionMembers.contains(m.getFactionId())&&Misc.getDistance(m.getStarSystem().getLocation(),point)<=maxLy*2000f)list.add(m);
        Collections.sort(list,new Comparator<MarketAPI>(){public int compare(MarketAPI a,MarketAPI b){return Float.compare(Misc.getDistance(a.getStarSystem().getLocation(),point),Misc.getDistance(b.getStarSystem().getLocation(),point));}});return list;
    }
    public static void advance(PTSDCrisisState s,float day){
        PTSDReliefState r=PTSDReliefState.get(s);if(day<r.nextDay)return;r.nextDay=day+.25f;
        PTSDCombatRelief.settleCampaign(s);
        PTSDJeWarExpedition.advance(s,day);
        relations(s);
        if(s.phase==PTSDCrisisState.Phase.WAR){
            if(s.late.staffEstablished){
                replenish(s,day);
                if(!r.playerInvited){r.playerInvited=true;for(String f:new String[]{"independent","persean","sindrian_diktat","luddic_church"})if(PTSDGeneralStaffDirector.hasColony(f))PTSDGeneralStaffDirector.requestMembership(s,f);}
                for(PTSDLateState.Front front:s.late.fronts.values())if((front.warned||front.operationId!=null)&&!front.resolved){
                    if(day<front.warningDay+cfg("relief_delay"))r.requestNotes.put(front.id,"警报已签收，响应剩余约 "+Math.ceil(front.warningDay+cfg("relief_delay")-day)+"日");
                    else if(r.alerts.add(front.id))organize(s,front,day);
                }
                if(day>=r.nextRecon){r.nextRecon=day+Math.max(1,cfg("relief_recon"));scout(s);}
            }
            path(s,day);
        }
        for(PTSDReliefState.Unit u:new ArrayList<PTSDReliefState.Unit>(r.units)){
            if(s.phase!=PTSDCrisisState.Phase.WAR&&u.stage!=DONE&&u.kind.startsWith("PATH")&&u.fleet!=null&&u.fleet.getBattle()==null){
                if(!"PATH_SURVIVOR".equals(u.kind)){finish(s,u,false);continue;}
                if(u.stage!=RETURNING)returnHome(s,u,day);
            }
            advanceUnit(s,u,day);
        }
        int completed=0;for(int n=r.units.size()-1;n>=0;n--)if(r.units.get(n).stage==DONE&&++completed>40)r.units.remove(n);
    }
    static void organize(PTSDCrisisState s,PTSDLateState.Front front,float day){
        MarketAPI target=s.resolveMarket(front.marketId);if(!port(target))return;
        PTSDReliefState ledger=PTSDReliefState.get(s);ledger.requestNotes.put(front.id,"来源兵力或支援距离不足，未编成援军");
        Random random=new Random(front.id.hashCode());
        if(!s.late.coalitionMembers.contains(target.getFactionId())&&random.nextFloat()*100>=cfg("relief_nonmember")){ledger.requestNotes.put(front.id,"非成员求援未获本轮出兵承诺");return;}
        List<MarketAPI> candidates=sources(s,target.getStarSystem().getLocation(),cfg("relief_range"));if(candidates.isEmpty())return;
        MarketAPI muster=candidates.get(0);int count=0;
        for(MarketAPI source:candidates){if(source.getId().equals(target.getId())||available(s,source)<15)continue;
            PTSDReliefState.Unit u=unit(s,"ALLIANCE",source,source.getStarSystem(),target.getStarSystem(),target.getId(),front.id,Math.min(120,available(s,source)),true);
            if(u!=null){u.frontId=front.id;u.musterMarket=muster.getId();count++;}if(count>=3)break;
        }
        if(count>0){ledger.requestNotes.put(front.id,count+"支分队已开始原港整备");PTSDLateWarDirector.enqueueAlert(s,"relief:"+front.id,front.systemId,"联合总参部已签发驰援令。各港正在整备，编队将在集结后共同出发。",day);}
    }
    public static boolean requestAssistance(PTSDCrisisState s,PTSDLateState.Front front){
        if(front==null||front.resolved||!s.late.staffEstablished)return false;
        PTSDReliefState r=PTSDReliefState.get(s);for(PTSDReliefState.Unit u:r.units)if(front.id.equals(u.group)&&u.stage!=DONE)return true;
        if(PTSDCrisisState.getDay()<front.warningDay+cfg("relief_delay")){r.alerts.remove(front.id);return true;}
        int before=r.units.size();organize(s,front,PTSDCrisisState.getDay());return r.units.size()>before;
    }
    static void devPath(PTSDCrisisState s){
        if(s.phase!=PTSDCrisisState.Phase.WAR||!Global.getSettings().isDevMode())return;
        for(PTSDLateState.Front f:s.late.fronts.values())if(!f.resolved){StarSystemAPI target=s.resolveSystem(f.systemId);if(target==null)continue;StarSystemAPI source=emptyOrigin(target);if(source==null)continue;
            PTSDReliefState.Unit u=unit(s,"PATH_RESCUE",null,source,target,f.marketId,region(PTSDReliefState.get(s),target),cfg("path_fp_min"),false);
            if(u!=null){u.frontId=f.id;startTravel(s,u,TRAVELLING,target.getId(),PTSDCrisisState.getDay());}return;
        }
    }
    static void scout(PTSDCrisisState s){
        PTSDReliefState r=PTSDReliefState.get(s);int count=0;for(PTSDReliefState.Unit u:r.units)if(u.stage!=DONE&&"SCOUT".equals(u.kind))count++;if(count>=3)return;
        for(MarketAPI source:sources(s,Global.getSector().getPlayerFleet().getLocationInHyperspace(),1000)){
            if(available(s,source)<15)continue;StarSystemAPI best=null;float oldest=Float.MAX_VALUE;
            for(StarSystemAPI sys:Global.getSector().getStarSystems()){
                if(sys==source.getStarSystem()||sys.hasTag(Tags.THEME_HIDDEN)||Misc.getDistance(sys.getLocation(),source.getStarSystem().getLocation())>20000)continue;
                boolean assigned=false;for(PTSDReliefState.Unit u:r.units)if(u.stage!=DONE&&"SCOUT".equals(u.kind)&&sys.getId().equals(u.targetSystem))assigned=true;if(assigned)continue;
                PTSDCrisisState.HumanReport report=s.humanWarReports.get("coalition-survey:"+sys.getId());float stamp=report==null?-10000:report.day;
                if(stamp<oldest){oldest=stamp;best=sys;}
            }
            if(best==null)continue;
            PTSDReliefState.Unit u=unit(s,"SCOUT",source,source.getStarSystem(),best,null,"scout",Math.min(30,available(s,source)),true);
            if(u!=null){u.fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,true);return;}
        }
    }
    static void survey(PTSDCrisisState s,PTSDReliefState.Unit u,float day){
        if(u.reported)return;u.reported=true;final StarSystemAPI target=s.resolveSystem(u.targetSystem);if(target==null)return;
        report(s,target,u,day,false);
        List<StarSystemAPI> nearby=new ArrayList<StarSystemAPI>();for(StarSystemAPI sys:Global.getSector().getStarSystems())
            if(sys!=target&&!sys.hasTag(Tags.THEME_HIDDEN)&&Misc.getDistance(sys.getLocation(),target.getLocation())<=20000)nearby.add(sys);
        Collections.sort(nearby,new Comparator<StarSystemAPI>(){public int compare(StarSystemAPI a,StarSystemAPI b){return Float.compare(Misc.getDistance(a.getLocation(),target.getLocation()),Misc.getDistance(b.getLocation(),target.getLocation()));}});
        for(int n=0;n<Math.min(3,nearby.size());n++)report(s,nearby.get(n),u,day,true);
    }
    static void report(PTSDCrisisState s,StarSystemAPI sys,PTSDReliefState.Unit u,float day,boolean remote){
        PTSDCrisisState.HumanReport h=new PTSDCrisisState.HumanReport();h.id="coalition-survey:"+sys.getId();h.systemId=sys.getId();h.day=day;h.factionId=u.faction;
        h.source=remote?"联盟周边通信汇总":"联盟侦察队现场上传";h.text=remote?"周边航路已建立联系；隐蔽部署尚未核实。":"现场航迹已上传，侦察队无需返港交付。";
        if(!remote)for(CampaignFleetAPI f:PTSDCampaignRuntime.fleets(sys))if(PTSDJeOtloesManager.isOmegaFleet(f)&&f.isVisibleToSensorsOf(u.fleet)){h.strength+=Math.round(f.getFleetPoints()/10)*10;h.omega=true;}
        s.humanWarReports.put(h.id,h);
    }
    static void relations(PTSDCrisisState s){
        FactionAPI path=Global.getSector().getFaction(PATH);if(path==null)return;PTSDReliefState r=PTSDReliefState.get(s);
        if(s.phase!=PTSDCrisisState.Phase.WAR){for(Map.Entry<String,Float> e:r.relations.entrySet())if(Global.getSector().getFaction(e.getKey())!=null)path.setRelationship(e.getKey(),e.getValue());r.relations.clear();return;}
        for(FactionAPI f:Global.getSector().getAllFactions())if(!PATH.equals(f.getId())){
            if(!r.relations.containsKey(f.getId()))r.relations.put(f.getId(),path.getRelationship(f.getId()));
            path.setRelationship(f.getId(),omega(f.getId())?-1f:s.late.coalitionMembers.contains(f.getId())?.5f:0f);
        }
    }
    static String region(PTSDReliefState r,StarSystemAPI sys){
        for(Map.Entry<String,float[]> e:r.regionCenters.entrySet())if(Misc.getDistance(sys.getLocation(),new Vector2f(e.getValue()[0],e.getValue()[1]))<=cfg("path_region")*2000)return e.getKey();
        r.regionCenters.put(sys.getId(),new float[]{sys.getLocation().x,sys.getLocation().y});return sys.getId();
    }
    static void path(PTSDCrisisState s,float day){
        if(Global.getSector().getFaction(PATH)==null)return;PTSDReliefState r=PTSDReliefState.get(s);Set<String> checked=new HashSet<String>();
        for(PTSDLateState.Front front:s.late.fronts.values())if((front.warned||front.operationId!=null)&&!front.resolved){StarSystemAPI target=s.resolveSystem(front.systemId);if(target==null)continue;
            String area=region(r,target);if(!checked.add(area)||r.regions.containsKey(area)&&r.regions.get(area)>day)continue;
            r.regions.put(area,day+Math.max(1,cfg("path_interval")));int count=0;for(PTSDReliefState.Unit u:r.units)if(u.stage!=DONE&&"PATH_RESCUE".equals(u.kind)&&area.equals(u.group))count++;
            if(count>=cfg("path_region_cap")||Math.random()*100>=cfg("path_chance"))continue;
            StarSystemAPI origin=emptyOrigin(target);if(origin==null)continue;
            float strength=Math.max(cfg("path_fp_min"),Math.min(Math.max(cfg("path_fp_min"),cfg("path_fp_max")),front.attackBudget*.5f));
            PTSDReliefState.Unit u=unit(s,"PATH_RESCUE",null,origin,target,front.marketId,area,strength,false);
            if(u!=null){u.frontId=front.id;startTravel(s,u,TRAVELLING,target.getId(),day);}
        }
        if(day<r.nextRaid)return;r.nextRaid=day+Math.max(1,cfg("path_raid_interval"));int count=0;for(PTSDReliefState.Unit u:r.units)if(u.stage!=DONE&&"PATH_RAID".equals(u.kind))count++;
        if(count>=cfg("path_raid_cap")||Math.random()*100>=cfg("path_raid_chance"))return;
        for(PTSDCrisisState.HumanReport h:s.humanWarReports.values())if(h.omega&&day-h.day<45){StarSystemAPI target=s.resolveSystem(h.systemId);if(target==null||PTSDProtectedFaction.system(target))continue;
            MarketAPI market=null;for(MarketAPI m:Global.getSector().getEconomy().getMarkets(target))if(!m.isPlanetConditionMarketOnly()&&omega(m.getFactionId())){market=m;break;}
            if(market==null)continue;StarSystemAPI origin=emptyOrigin(target);if(origin==null)continue;
            float budget=cfg("path_raid_min")+(float)Math.random()*Math.max(0,cfg("path_raid_max")-cfg("path_raid_min"));
            PTSDReliefState.Unit u=unit(s,"PATH_RAID",null,origin,target,market.getId(),"raid",budget,false);if(u!=null)startTravel(s,u,TRAVELLING,target.getId(),day);return;
        }
    }
    static StarSystemAPI emptyOrigin(StarSystemAPI target){
        List<StarSystemAPI> list=new ArrayList<StarSystemAPI>();for(StarSystemAPI sys:Global.getSector().getStarSystems()){
            if(sys==target||sys.hasTag(Tags.THEME_HIDDEN)||sys.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)||Misc.getDistance(sys.getLocation(),target.getLocation())>24000)continue;
            boolean inhabited=false;for(MarketAPI m:Global.getSector().getEconomy().getMarkets(sys))if(!m.isPlanetConditionMarketOnly())inhabited=true;if(!inhabited)list.add(sys);
        }return list.isEmpty()?null:list.get(new Random().nextInt(list.size()));
    }
    static void startTravel(PTSDCrisisState s,PTSDReliefState.Unit u,PTSDReliefState.Stage stage,String target,float day){
        withdraw(s,u);u.stage=stage;u.stageDay=day;u.fromX=u.x;u.fromY=u.y;u.targetSystem=target;
        u.status=stage==RETURNING?"幸存舰船正在返航":stage==MUSTERING?"正在前往集结点":"正在前往行动目标";
        StarSystemAPI sys=s.resolveSystem(target);u.travelDays=sys==null?1:Math.max(1,Misc.getDistance(new Vector2f(u.x,u.y),sys.getLocation())/900f);
        if(u.physical&&u.fleet.getBattle()==null&&sys!=null){u.fleet.clearAssignments();SectorEntityToken dest=sys.getCenter();
            MarketAPI market=s.resolveMarket(stage==MUSTERING?u.musterMarket:u.targetMarket);if(market!=null&&market.getStarSystem()==sys)dest=market.getPrimaryEntity();
            u.fleet.addAssignment(FleetAssignment.GO_TO_LOCATION,dest,300,"前往联合行动目标");}
    }
    static boolean at(PTSDCrisisState s,PTSDReliefState.Unit u){StarSystemAPI target=s.resolveSystem(u.targetSystem);if(target==null)return false;
        if(!u.physical)return PTSDCrisisState.getDay()>=u.stageDay+u.travelDays;
        SectorEntityToken destination=target.getCenter();MarketAPI m=s.resolveMarket(u.stage==MUSTERING?u.musterMarket:u.targetMarket);if(m!=null&&m.getStarSystem()==target)destination=m.getPrimaryEntity();
        return u.fleet.getContainingLocation()==target&&Misc.getDistance(u.fleet,destination)<1200;
    }
    static void advanceUnit(PTSDCrisisState s,PTSDReliefState.Unit u,float day){
        if(u.stage==DONE)return;float dt=Math.min(1,Math.max(0,day-u.lastDay));u.lastDay=day;
        if(u.fleet==null||u.fleet.isEmpty()||u.physical&&!u.fleet.isAlive()){finish(s,u,false);return;}
        if(u.fleet.getBattle()!=null)return;
        if(u.physical){Vector2f p=u.fleet.getLocationInHyperspace();u.x=p.x;u.y=p.y;}
        if(u.stage==PREPARING){MarketAPI source=s.resolveMarket(u.sourceMarket);if(!port(source)||!u.faction.equals(source.getFactionId())){returnHome(s,u,day);return;}if(day<u.stageDay+cfg("relief_prepare"))return;
            if("ALLIANCE".equals(u.kind)){MarketAPI muster=s.resolveMarket(u.musterMarket);if(!port(muster)){returnHome(s,u,day);return;}startTravel(s,u,MUSTERING,muster.getStarSystem().getId(),day);}
            else startTravel(s,u,TRAVELLING,u.targetSystem,day);return;}
        if((u.stage==MUSTERING||u.stage==ASSEMBLED)&&!port(s.resolveMarket(u.musterMarket))){returnHome(s,u,day);return;}
        if(u.stage==MUSTERING&&at(s,u)){u.stage=ASSEMBLED;u.stageDay=day;u.fleet.clearAssignments();u.fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE,s.resolveMarket(u.musterMarket).getPrimaryEntity(),30,"等待联合编队集结");
            u.status="等待其余分队；超过集结上限则先行出发";
            if(!PTSDReliefState.get(s).groups.containsKey(u.group))PTSDReliefState.get(s).groups.put(u.group,day);}
        if(u.stage==ASSEMBLED){boolean ready=true;for(PTSDReliefState.Unit other:PTSDReliefState.get(s).units)if(u.group.equals(other.group)&&(other.stage==PREPARING||other.stage==MUSTERING))ready=false;
            if(ready||day>=PTSDReliefState.get(s).groups.get(u.group)+cfg("relief_muster")){MarketAPI target=s.resolveMarket(u.targetMarket);if(!port(target)){garrison(s,u,day);return;}startTravel(s,u,TRAVELLING,target.getStarSystem().getId(),day);}return;}
        if(u.stage==TRAVELLING||u.stage==RETURNING){StarSystemAPI target=s.resolveSystem(u.targetSystem);if(target==null){returnHome(s,u,day);return;}
            if(u.stage==RETURNING){MarketAPI destination=s.resolveMarket(u.targetMarket);if(!port(destination)||Global.getSector().getFaction(u.faction).isHostileTo(destination.getFactionId())){returnHome(s,u,day);return;}}
            if(u.stage==TRAVELLING&&"ALLIANCE".equals(u.kind)&&!port(s.resolveMarket(u.targetMarket))){garrison(s,u,day);return;}
            if(!u.physical){float t=Math.min(1,Math.max(0,(day-u.stageDay)/u.travelDays));u.x=u.fromX+(target.getLocation().x-u.fromX)*t;u.y=u.fromY+(target.getLocation().y-u.fromY)*t;materialize(s,u,target,t);}
            if(!at(s,u))return;
            if(u.stage==RETURNING){finish(s,u,true);return;}
            if("SCOUT".equals(u.kind)){survey(s,u,day);returnHome(s,u,day);return;}
            if("ALLIANCE".equals(u.kind)&&!port(s.resolveMarket(u.targetMarket))){garrison(s,u,day);return;}
            u.stage="ALLIANCE_GARRISON".equals(u.kind)?GARRISON:ACTIVE;u.stageDay=day;u.status="已抵达，展开防御";
            if(u.physical){u.fleet.clearAssignments();u.fleet.addAssignment(FleetAssignment.PATROL_SYSTEM,target.getCenter(),60,"展开战区支援");}
        }
        if(u.stage==ACTIVE||u.stage==GARRISON){
            if(u.targetSystem==null||s.resolveSystem(u.targetSystem)==null){returnHome(s,u,day);return;}
            if(!u.physical)materialize(s,u,s.resolveSystem(u.targetSystem),1);
            PTSDLateState.Front front=s.late.fronts.get(u.frontId);
            if(front==null||front.resolved){withdraw(s,u);for(PTSDLateState.Front f:s.late.fronts.values())if(!f.resolved&&u.targetSystem.equals(f.systemId)){front=f;u.frontId=f.id;break;}}
            if(front!=null&&!front.resolved){float fp=u.fleet.getFleetPoints();front.defense=Math.max(0,front.defense+fp-u.committed);u.committed=fp;}
            if(u.kind.startsWith("PATH")&&!u.physical)virtualFight(s,u,dt);
            float duration=u.stage==GARRISON?cfg("relief_garrison"):"PATH_SURVIVOR".equals(u.kind)?cfg("path_survivor_days"):u.kind.startsWith("PATH")?cfg("path_operation_days"):15;
            if(day>=u.stageDay+duration||front!=null&&front.resolved&&!front.lost&&!"PATH_SURVIVOR".equals(u.kind)){
                if(u.kind.startsWith("PATH")&&!"PATH_SURVIVOR".equals(u.kind))finish(s,u,false);
                else if("ALLIANCE".equals(u.kind)&&u.stage!=GARRISON&&!port(s.resolveMarket(u.targetMarket)))garrison(s,u,day);
                else returnHome(s,u,day);
            }
        }
    }
    static void materialize(PTSDCrisisState s,PTSDReliefState.Unit u,StarSystemAPI target,float t){
        CampaignFleetAPI player=Global.getSector().getPlayerFleet();if(player==null)return;
        if(t>=1&&player.getStarSystem()==target){target.addEntity(u.fleet);u.fleet.setLocation(target.getCenter().getLocation().x,target.getCenter().getLocation().y);}
        else if(player.isInHyperspace()&&Misc.getDistance(player.getLocation(),new Vector2f(u.x,u.y))<5000){Global.getSector().getHyperspace().addEntity(u.fleet);u.fleet.setLocation(u.x,u.y);}
        else return;
        u.physical=true;PTSDCampaignRuntime.entityAdded(u.fleet);u.fleet.addAssignment(FleetAssignment.GO_TO_LOCATION,target.getCenter(),300,"驰援受袭星系");
        u.fleet.addAssignment(FleetAssignment.PATROL_SYSTEM,target.getCenter(),60,"迎击未知舰船");
    }
    static void virtualFight(PTSDCrisisState s,PTSDReliefState.Unit u,float dt){
        // Never also simulate a materialized battle. Losses remove actual roster members.
        float enemy=0;for(PTSDCrisisState.StrategicEvent e:s.getActiveEvents())if(u.targetSystem.equals(e.targetSystemId)&&PTSDCrisisAPI.SIDE_OMEGA.equals(e.side)&&e.simulatedWarOperation&&e.materializedDay<=0&&e.arrivalDay>0&&e.arrivalDay<=PTSDCrisisState.getDay()){
            enemy+=e.strength;e.strength=Math.max(0,e.strength-u.fleet.getFleetPoints()*.025f*dt);}
        if(enemy>0)damageRoster(u.fleet,enemy*.015f*dt);
        if(enemy<=0){StarSystemAPI sys=s.resolveSystem(u.targetSystem);CampaignFleetAPI player=Global.getSector().getPlayerFleet();
            if(sys!=null&&(player==null||player.getStarSystem()!=sys))for(CampaignFleetAPI defender:new ArrayList<CampaignFleetAPI>(PTSDCampaignRuntime.fleets(sys)))
                if(PTSDJeOtloesManager.isOmegaFleet(defender)&&defender.getBattle()==null){float fp=defender.getFleetPoints();damageRoster(defender,u.fleet.getFleetPoints()*.025f*dt);damageRoster(u.fleet,fp*.015f*dt);break;}}
    }
    static void damageRoster(CampaignFleetAPI f,float damage){
        if(damage<=0||f.isEmpty())return;FleetMemberAPI m=f.getFleetData().getMembersListCopy().get(0);
        float remaining=m.getStatus().getHullFraction()-damage/Math.max(1,m.getFleetPointCost());
        if(remaining<=0)f.getFleetData().removeFleetMember(m);else m.getStatus().setHullFraction(remaining);
    }
    static void garrison(PTSDCrisisState s,PTSDReliefState.Unit u,float day){
        StarSystemAPI lost=s.resolveSystem(u.targetSystem);MarketAPI m=nearestSafe(s,lost==null?new Vector2f(u.x,u.y):lost.getLocation(),u.faction);if(m==null){returnHome(s,u,day);return;}
        withdraw(s,u);u.targetMarket=m.getId();u.frontId=null;u.kind="ALLIANCE_GARRISON";startTravel(s,u,TRAVELLING,m.getStarSystem().getId(),day);
        u.status="原目标失守，转驻 "+m.getName();
    }
    public static MarketAPI nearestSafe(PTSDCrisisState s,Vector2f point,String faction){MarketAPI best=null;float distance=Float.MAX_VALUE;
        for(MarketAPI m:markets())if(port(m)&&!Global.getSector().getFaction(faction).isHostileTo(m.getFactionId())){
            float d=Misc.getDistance(point,m.getStarSystem().getLocation());if(d<distance){distance=d;best=m;}}
        return best;
    }
    static void returnHome(PTSDCrisisState s,PTSDReliefState.Unit u,float day){
        MarketAPI home=s.resolveMarket(u.sourceMarket);if(!port(home)||Global.getSector().getFaction(u.faction).isHostileTo(home.getFactionId()))home=nearestSafe(s,new Vector2f(u.x,u.y),u.faction);
        if(home==null){withdraw(s,u);u.stage=RETURNING;u.targetSystem=null;u.status="等待安全返航港";return;}
        u.targetMarket=home.getId();startTravel(s,u,RETURNING,home.getStarSystem().getId(),day);
        if(u.kind.startsWith("PATH"))u.fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,true);
        if(u.physical){if(u.fleet.getAbility(Abilities.EMERGENCY_BURN)==null)u.fleet.addAbility(Abilities.EMERGENCY_BURN);u.fleet.getAbility(Abilities.EMERGENCY_BURN).activate();}
    }
    static void withdraw(PTSDCrisisState s,PTSDReliefState.Unit u){PTSDLateState.Front front=s.late.fronts.get(u.frontId);if(front!=null)front.defense=Math.max(0,front.defense-u.committed);u.committed=0;}
    static void finish(PTSDCrisisState s,PTSDReliefState.Unit u,boolean returned){if(u.stage==DONE)return;withdraw(s,u);
        if(returned&&u.borrowed&&!u.settled)charge(s,u.sourceMarket,-u.fleet.getFleetPoints());u.settled=true;
        if(u.physical&&u.fleet!=null&&u.fleet.getContainingLocation()!=null&&u.fleet.getBattle()==null){PTSDCampaignRuntime.leaving(u.fleet);u.fleet.despawn();}
        u.stage=DONE;u.status=returned?"已到港，幸存兵力归建":"行动结束";
    }
    static float replenishedSpent(float spent,float locked,float capacity,float fraction){return Math.max(locked,spent-Math.max(0,capacity*fraction));}
    static void replenish(PTSDCrisisState s,float day){
        PTSDReliefState r=PTSDReliefState.get(s);if(r.nextReplenish<=0){r.nextReplenish=day+Math.max(1,cfg("relief_replenish_days"));return;}
        if(day<r.nextReplenish)return;r.nextReplenish=day+Math.max(1,cfg("relief_replenish_days"));
        for(String source:new ArrayList<String>(r.sourceSpent.keySet())){MarketAPI market=s.resolveMarket(source);if(!port(market)||!s.late.coalitionMembers.contains(market.getFactionId()))continue;
            float locked=0;for(PTSDReliefState.Unit u:r.units)if(u.stage!=DONE&&u.borrowed&&source.equals(u.sourceMarket)&&u.fleet!=null)locked+=u.fleet.getFleetPoints();
            for(PTSDReliefState.BattleRecord b:r.battles.values())if(!b.settled){List<FleetMemberAPI> pending=b.pending.get(source),arrived=b.arrived.get(source);
                if(pending!=null)for(FleetMemberAPI m:pending)locked+=m.getFleetPointCost();
                if(arrived!=null)for(FleetMemberAPI m:arrived)if(!b.dead.contains(m.getId()))locked+=m.getFleetPointCost();}
            r.sourceSpent.put(source,replenishedSpent(r.sourceSpent.get(source),locked,capacity(market)*(1-Math.min(100,cfg("relief_keep"))/100f),Math.min(100,cfg("relief_replenish_percent"))/100f));
        }
    }
}
