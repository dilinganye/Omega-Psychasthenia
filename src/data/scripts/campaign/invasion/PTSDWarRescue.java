package data.scripts.campaign.invasion;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.BaseFleetEventListener;
import com.fs.starfarer.api.impl.campaign.fleets.*;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.shard.PTSD_BaseShard_Util;
import data.scripts.IIRT_Omega_ModPlugin;
import data.scripts.PTSDModPluginAltLuna;
import org.lwjgl.util.vector.Vector2f;
import java.io.Serializable;
import java.util.*;

/** Persistent rescue operations; no reward for merely visiting a distress location. */
public final class PTSDWarRescue {
    private static final String KEY="PTSD_war_rescue_operations";
    public static class Operation implements Serializable {
        public String id,systemId;public int kind;public float born;public boolean helped,rewarded,ended;
        public List<String> humans=new ArrayList<String>(),enemies=new ArrayList<String>();
        public Set<String> arrived=new HashSet<String>(),released=new HashSet<String>();
        public Map<String,Float> arrivedFp=new HashMap<String,Float>();
        public Map<String,String> destinations=new HashMap<String,String>();
        public int credits;public float rescuedFp;
    }
    public static class Store implements Serializable {public float nextRoll,nextCheck;public List<Operation> operations=new ArrayList<Operation>();}
    public static Store store(){Map<String,Object> data=Global.getSector().getPersistentData();Object saved=data.get(KEY);if(saved instanceof Store)return (Store)saved;Store s=new Store();data.put(KEY,s);return s;}
    static float setting(String id,float fallback){return PTSDModPluginAltLuna.getLunaFloat(IIRT_Omega_ModPlugin.getModId(),id,fallback);}
    public static void advance(PTSDCrisisState state,float day){
        if(state==null||state.late==null)return;
        Store s=store();if(day<s.nextCheck)return;s.nextCheck=day+.1f;
        for(Operation o:s.operations)if(!o.ended)update(state,o,day);
        CampaignFleetAPI player=Global.getSector().getPlayerFleet();
        if(state.phase!=PTSDCrisisState.Phase.WAR||player==null||!player.isInHyperspace()||day<s.nextRoll)return;
        s.nextRoll=day+Math.max(1,setting("PTSD_rescue_interval",10));
        int active=0;for(Operation o:s.operations)if(!o.ended)active++;
        Random random=new Random();
        if(active>=setting("PTSD_rescue_max_active",3)||random.nextFloat()*100>=setting("PTSD_rescue_chance",50))return;
        spawn(state,random.nextInt(4),day,random);
    }
    static boolean human(MarketAPI m){return m!=null&&!m.isPlanetConditionMarketOnly()&&m.getPrimaryEntity()!=null&&m.getStarSystem()!=null&&!PTSDPrewarReconDirector.omega(m.getFactionId());}
    static boolean allied(PTSDCrisisState s,MarketAPI m){return s.late.coalitionMembers.contains(m.getFactionId());}
    static MarketAPI destination(PTSDCrisisState state,CampaignFleetAPI f,boolean ally,String exclude){
        MarketAPI best=null;float dist=Float.MAX_VALUE;
        for(MarketAPI m:PTSDOccupationManager.getAllMarkets()){
            if(!human(m)||allied(state,m)!=ally||m.getStarSystem().getId().equals(exclude)||PTSDOccupationManager.isOccupied(m))continue;
            if(m.getFaction().isHostileTo(f.getFaction()))continue;
            float d=Misc.getDistance(f.getLocationInHyperspace(),m.getPrimaryEntity().getLocationInHyperspace());
            if(d<dist){dist=d;best=m;}
        }return best;
    }
    public static boolean devSpawn(int kind){PTSDCrisisState s=PTSDCrisisState.get();return Global.getSettings().isDevMode()&&s!=null&&s.phase==PTSDCrisisState.Phase.WAR&&spawn(s,kind,PTSDCrisisState.getDay(),new Random());}
    static boolean spawn(PTSDCrisisState state,int kind,float day,Random random){
        List<MarketAPI> candidates=new ArrayList<MarketAPI>();Set<String> seen=new HashSet<String>();
        for(MarketAPI m:PTSDOccupationManager.getAllMarkets()){
            if(!human(m)||allied(state,m)||PTSDOccupationManager.isOccupied(m)||PTSDProtectedFaction.system(m.getStarSystem()))continue;
            boolean member=false;for(MarketAPI other:Global.getSector().getEconomy().getMarkets(m.getStarSystem()))if(human(other)&&allied(state,other))member=true;
            if(!member&&seen.add(m.getStarSystem().getId()))candidates.add(m);
        }
        if(candidates.isEmpty())return false;
        MarketAPI source=candidates.get(random.nextInt(candidates.size()));StarSystemAPI sys=source.getStarSystem();
        Operation o=new Operation();o.id=UUID.randomUUID().toString();o.systemId=sys.getId();o.kind=kind;o.born=day;
        float low=setting("PTSD_rescue_credit_min",50000),high=Math.max(low,setting("PTSD_rescue_credit_max",150000));
        o.credits=Math.round(low+random.nextFloat()*(high-low));
        int count=kind==2?3+random.nextInt(2):1;
        Vector2f center=new Vector2f(source.getPrimaryEntity().getLocation());center.x+=2400;center.y+=1300;
        for(int n=0;n<count;n++){
            float fp=kind==3?180:kind==2?45:75;
            CampaignFleetAPI f=FleetFactoryV3.createFleet(new FleetParamsV3(center,source.getFactionId(),.7f,FleetTypes.PATROL_LARGE,fp,kind==3?0:15,5,0,0,0,0));
            if(f==null||f.getFleetPoints()<=0){cleanup(o);return false;}
            add(sys,f,center.x+random.nextFloat()*1000-500,center.y+random.nextFloat()*1000-500);
            f.setName(kind==2?"撤离中的求救舰队":"等待援助的幸存舰队");
            f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_ALLOW_PLAYER_BATTLE_JOIN_TOFF,true);
            f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,true);
            f.addEventListener(new Witness(o));o.humans.add(f.getId());Misc.makeImportant(f,"PTSD_rescue_"+o.id);
            f.addAssignment(FleetAssignment.ORBIT_PASSIVE,sys.createToken(f.getLocation()),60,"等待救援窗口");
            if(kind==2)route(state,o,f,false);
        }
        for(int n=0;n<(kind==2?2:1);n++){
            CampaignFleetAPI target=fleet(o.humans.get(n));float fp=kind==3?target.getFleetPoints():kind==1?65:100;
            CampaignFleetAPI enemy=PTSD_BaseShard_Util.createFleet(new FleetParamsV3(center,PTSDCrisisProgress.getActiveFactionId(state),.7f,FleetTypes.PATROL_LARGE,fp,0,0,0,0,0,0),fp,PTSD_BaseShard_Util.FleetRole.GUARD_ASSAULT);
            if(enemy==null||enemy.getFleetPoints()<=0){cleanup(o);return false;}
            if(kind==3&&!matchStrength(target,enemy)){remove(target.getId());enemy.despawn();cleanup(o);return false;}
            add(sys,enemy,center.x-4500-n*700,center.y+1500+n*500);o.enemies.add(enemy.getId());
            enemy.getMemoryWithoutUpdate().set("$PTSD_encounter_response_installed",true);
            enemy.addEventListener(new Witness(o));enemy.addAssignment(FleetAssignment.INTERCEPT,target,60,"追击撤离舰队");
        }
        PTSDMobilizationConvoys.debris(sys,center,random,kind==1?2:kind==3?3:9);
        store().operations.add(o);Global.getSector().getIntelManager().addIntel(new PTSDWarRescueIntel(o));return true;
    }
    private static void add(StarSystemAPI sys,CampaignFleetAPI f,float x,float y){sys.addEntity(f);f.setLocation(x,y);PTSDCampaignRuntime.entityAdded(f);for(com.fs.starfarer.api.fleet.FleetMemberAPI m:f.getFleetData().getMembersListCopy())m.getRepairTracker().setCR(m.getRepairTracker().getMaxCR());}
    static CampaignFleetAPI fleet(String id){return PTSDNewsScene.fleet(id);}
    private static boolean alive(CampaignFleetAPI f){return f!=null&&f.isAlive()&&!f.isDespawning()&&f.getFleetPoints()>0;}
    private static void route(PTSDCrisisState state,Operation o,CampaignFleetAPI f,boolean ally){
        MarketAPI m=destination(state,f,ally,ally?"":o.systemId);
        if(m==null&&!ally)m=destination(state,f,true,"");
        if(m==null)return;
        o.destinations.put(f.getId(),m.getId());f.clearAssignments();
        f.addAssignment(FleetAssignment.GO_TO_LOCATION,m.getPrimaryEntity(),100,"撤离至安全港口");
    }
    static void update(PTSDCrisisState state,Operation o,float day){
        int alive=0;float safeFp=0;boolean allSafe=true;
        for(String id:o.humans){
            if(o.arrived.contains(id)){safeFp+=o.arrivedFp.containsKey(id)?o.arrivedFp.get(id):0;continue;}
            CampaignFleetAPI f=fleet(id);if(!alive(f))continue;alive++;
            boolean threat=false;
            for(String enemyId:o.enemies){CampaignFleetAPI e=fleet(enemyId);if(alive(e)&&e.getContainingLocation()==f.getContainingLocation()&&Misc.getDistance(e,f)<5000)threat=true;}
            boolean safe=f.getBattle()==null&&!threat;
            allSafe &= safe;
            if(safe)safeFp+=f.getFleetPoints();
            if(safe&&o.helped&&o.released.add(id))route(state,o,f,true);
            MarketAPI m=state.resolveMarket(o.destinations.get(id));
            if(o.released.contains(id)&&(m==null||!allied(state,m)||PTSDOccupationManager.isOccupied(m))){route(state,o,f,true);m=state.resolveMarket(o.destinations.get(id));}
            if(m!=null&&f.getBattle()==null&&f.getContainingLocation()==m.getPrimaryEntity().getContainingLocation()&&Misc.getDistance(f,m.getPrimaryEntity())<650){
                if(allied(state,m)){
                    PTSDLateState.Reserve reserve=new PTSDLateState.Reserve();reserve.id="rescue:"+o.id+":"+id;reserve.factionId=f.getFaction().getId();
                    reserve.sourceSystemId=o.systemId;reserve.targetSystemId=m.getStarSystem().getId();reserve.fleetId=id;reserve.strength=f.getFleetPoints();reserve.arrived=true;reserve.authorized=true;reserve.lastDay=day;
                    for(com.fs.starfarer.api.fleet.FleetMemberAPI member:f.getFleetData().getMembersListCopy())reserve.variantIds.add(member.getVariant().getHullVariantId());
                    state.late.reserves.add(reserve);o.arrived.add(id);o.arrivedFp.put(id,(float)f.getFleetPoints());Misc.makeUnimportant(f,"PTSD_rescue_"+o.id);
                    f.clearAssignments();f.addAssignment(FleetAssignment.DEFEND_LOCATION,m.getPrimaryEntity(),10000,"协助联盟防守");
                }else if(o.kind==2){o.destinations.remove(id);route(state,o,f,true);}
            }
        }
        if(o.helped&&!o.rewarded&&safeFp>0&&allSafe){
            o.rewarded=true;o.rescuedFp=safeFp;
            Global.getSector().getPlayerFleet().getCargo().getCredits().add(o.credits);
            Global.getSector().getPlayerStats().addXP(Math.round(safeFp*setting("PTSD_rescue_xp_per_fp",250)));
            Global.getSector().getCampaignUI().addMessage("求救舰队：我们仍然活着。谢谢你，舰长。救援谢礼 "+o.credits+" 星币，获救兵力 "+Math.round(safeFp)+"。",Misc.getPositiveHighlightColor());
        }
        if(alive==0||day>=o.born+90){
            for(String id:o.humans){CampaignFleetAPI f=fleet(id);if(alive(f)&&f.getBattle()!=null)return;}
            for(String id:o.enemies){CampaignFleetAPI f=fleet(id);if(alive(f)&&f.getBattle()!=null)return;}
            o.ended=true;
            for(String id:o.humans)if(!o.arrived.contains(id)){
                CampaignFleetAPI f=fleet(id);if(alive(f)){Misc.makeUnimportant(f,"PTSD_rescue_"+o.id);f.clearAssignments();
                    MarketAPI port=destination(state,f,true,"");if(port==null)port=destination(state,f,false,"");
                    if(port!=null)f.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,port.getPrimaryEntity(),100,"退出求救区域");
                    else f.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,Global.getSector().getHyperspace().createToken(f.getLocationInHyperspace()),30,"撤离失联区域");
                }
            }
for(String id:o.enemies){CampaignFleetAPI e=fleet(id);if(alive(e)&&e.getBattle()==null){PTSDEventFleetReturn.credit(e);PTSDCampaignRuntime.leaving(e);e.despawn();}}}
    }
    /** Match the two physical rosters to their largest shared integer FP budget. */
    static boolean matchStrength(CampaignFleetAPI human,CampaignFleetAPI enemy){
        Map<Integer,List<com.fs.starfarer.api.fleet.FleetMemberAPI>> a=subsets(human),b=subsets(enemy);
        int best=0;for(int n:a.keySet())if(n>best&&b.containsKey(n))best=n;
        if(best<Math.min(human.getFleetPoints(),enemy.getFleetPoints())*.65f)return false;
        for(com.fs.starfarer.api.fleet.FleetMemberAPI m:new ArrayList<com.fs.starfarer.api.fleet.FleetMemberAPI>(human.getFleetData().getMembersListCopy()))if(!a.get(best).contains(m))human.getFleetData().removeFleetMember(m);
        for(com.fs.starfarer.api.fleet.FleetMemberAPI m:new ArrayList<com.fs.starfarer.api.fleet.FleetMemberAPI>(enemy.getFleetData().getMembersListCopy()))if(!b.get(best).contains(m))enemy.getFleetData().removeFleetMember(m);
        return best>0;
    }
    private static Map<Integer,List<com.fs.starfarer.api.fleet.FleetMemberAPI>> subsets(CampaignFleetAPI f){
        Map<Integer,List<com.fs.starfarer.api.fleet.FleetMemberAPI>> result=new HashMap<Integer,List<com.fs.starfarer.api.fleet.FleetMemberAPI>>();result.put(0,new ArrayList<com.fs.starfarer.api.fleet.FleetMemberAPI>());
        for(com.fs.starfarer.api.fleet.FleetMemberAPI member:f.getFleetData().getMembersListCopy()){
            for(int old:new ArrayList<Integer>(result.keySet())){
                int n=old+member.getFleetPointCost();if(n>1000||result.containsKey(n))continue;
                List<com.fs.starfarer.api.fleet.FleetMemberAPI> list=new ArrayList<com.fs.starfarer.api.fleet.FleetMemberAPI>(result.get(old));list.add(member);result.put(n,list);
            }
        }return result;
    }
    private static void cleanup(Operation o){for(String id:o.humans)remove(id);for(String id:o.enemies)remove(id);}
    private static void remove(String id){CampaignFleetAPI f=fleet(id);if(f!=null){f.getMemoryWithoutUpdate().set("$PTSD_dev_discarded",true);PTSDCampaignRuntime.leaving(f);f.despawn();}}
    private static class Witness extends BaseFleetEventListener {
        private Operation o;Witness(Operation o){this.o=o;}
        public void reportBattleOccurred(CampaignFleetAPI f,CampaignFleetAPI winner,BattleAPI battle){
            if(battle==null||!battle.isPlayerInvolved())return;
            if(o.humans.contains(f.getId())&&battle.isOnPlayerSide(f)||o.enemies.contains(f.getId())&&!battle.isOnPlayerSide(f))o.helped=true;
        }
    }
}
