package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.IIRT_Omega_ModPlugin;
import org.lwjgl.util.vector.Vector2f;
import java.util.*;

/** Regional operations use persistent travel records; visibility never advances their clock. */
public final class PTSDWarCampaignDirector {
    private PTSDWarCampaignDirector() { }
    public static final float EDGE_BAND = 3000f, REGION_RADIUS = 12000f;

    static List<Vector2f> hull(List<Vector2f> points) {
        List<Vector2f> p = new ArrayList<Vector2f>(points);
        Collections.sort(p, new Comparator<Vector2f>() {
            public int compare(Vector2f a, Vector2f b) {
                int c = Float.compare(a.x,b.x); return c != 0 ? c : Float.compare(a.y,b.y);
            }
        });
        List<Vector2f> unique = new ArrayList<Vector2f>();
        for (Vector2f v : p) if (unique.isEmpty() || !v.equals(unique.get(unique.size()-1))) unique.add(v);
        if (unique.size() <= 2) return unique;
        List<Vector2f> result = new ArrayList<Vector2f>();
        for (Vector2f v : unique) {
            while (result.size() >= 2 && cross(result.get(result.size()-2),result.get(result.size()-1),v) <= 0)
                result.remove(result.size()-1);
            result.add(v);
        }
        int lower = result.size();
        for (int i=unique.size()-2;i>=0;i--) {
            Vector2f v=unique.get(i);
            while (result.size()>lower && cross(result.get(result.size()-2),result.get(result.size()-1),v)<=0)
                result.remove(result.size()-1);
            result.add(v);
        }
        result.remove(result.size()-1); return result;
    }
    private static float cross(Vector2f a,Vector2f b,Vector2f c) {
        return (b.x-a.x)*(c.y-a.y)-(b.y-a.y)*(c.x-a.x);
    }
    static boolean onEdge(Vector2f p,List<Vector2f> hull,float band) {
        if (hull.size()<=2) return true;
        for(int i=0;i<hull.size();i++) {
            Vector2f a=hull.get(i),b=hull.get((i+1)%hull.size());
            float dx=b.x-a.x,dy=b.y-a.y;
            float t=Math.max(0,Math.min(1,((p.x-a.x)*dx+(p.y-a.y)*dy)/Math.max(1,dx*dx+dy*dy)));
            float ex=p.x-a.x-t*dx,ey=p.y-a.y-t*dy;
            if(ex*ex+ey*ey<=band*band)return true;
        }
        return false;
    }
    static List<MarketAPI> targets(PTSDCrisisState state,boolean edgeOnly) {
        List<MarketAPI> all=new ArrayList<MarketAPI>();List<Vector2f> points=new ArrayList<Vector2f>();
        for(MarketAPI m:PTSDOccupationManager.getAllMarkets()) {
            if(m==null||m.isPlanetConditionMarketOnly()||m.getStarSystem()==null||m.getPrimaryEntity()==null)continue;
            if(PTSDProtectedFaction.system(m.getStarSystem()))continue;
            String f=m.getFactionId();
            if(IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(f)||IIRT_Omega_Invasion.WATCHER_FACTION.equals(f)||"omega".equals(f))continue;
            PTSDCrisisFactionModifierAPI.Effects effects=PTSDCrisisFactionModifierAPI.get(f,m.getStarSystem().getId());
            if(effects.immune||effects.targetWeight<=0)continue;
            all.add(m);points.add(m.getStarSystem().getLocation());
        }
        if(!edgeOnly)return all;
        List<Vector2f> outline=hull(points);List<MarketAPI> edge=new ArrayList<MarketAPI>();
        for(MarketAPI m:all)if(onEdge(m.getStarSystem().getLocation(),outline,EDGE_BAND))edge.add(m);
        return edge;
    }
    static float observedDefense(PTSDCrisisState state,MarketAPI m) {
        PTSDCrisisState.SystemData d=state.getSystemData(m.getStarSystem().getId());
        return Math.max(60f,(d.observedMarketDefense>0?d.observedMarketDefense:m.getSize()*m.getSize()*9f)+Math.max(0,d.observedFleetStrength));
    }
    static float medianDefense(PTSDCrisisState state,List<MarketAPI> targets) {
        List<Float> values=new ArrayList<Float>();for(MarketAPI m:targets)values.add(observedDefense(state,m));
        if(values.isEmpty())return 60f;Collections.sort(values);int n=values.size();
        return n%2==0?(values.get(n/2-1)+values.get(n/2))*.5f:values.get(n/2);
    }
    static boolean singularity(float defense,float median) {
        return defense>median*IIRT_Omega_ModPlugin.warSingularityMultiplier &&
                defense>Math.max(600f,IIRT_Omega_ModPlugin.final_invasion_max_strength);
    }
    static float force(float defense,boolean opening) { return Math.max(90f,defense*(opening?2.8f:1.25f)); }
    // Suspended balance rule: opening cost was nominal * .5 (and half war-point cost).
    static float points(float nominal,boolean opening) { return opening?0:Math.max(5f,(float)Math.ceil(nominal/80f)*10f); }
    // Suspended: return free?0:Math.max(0,surviving)*(opening?.75f:.5f);
    static float refund(float surviving,boolean opening,boolean free) { return free||opening?0:Math.max(0,surviving)*.5f; }

    public static void prepare(PTSDCrisisState state,float day,boolean forceRefresh) {
        if(!forceRefresh&&day<state.late.nextOpeningPlanDay)return;
        state.late.nextOpeningPlanDay=day+15;
        List<MarketAPI> edge=targets(state,true);float median=medianDefense(state,edge),assets=0,points=0;
        float weakest=Float.MAX_VALUE;
        for(MarketAPI m:edge) {
            float defense=observedDefense(state,m);
            float strength=force(defense,false)*data.scripts.PTSDDifficultySettings.scale(state,day);
            if(strength<weakest){weakest=strength;assets=strength;points=points(strength,false);}
        }
        state.late.openingAssetsRequired=assets;state.late.openingPointsRequired=points;
    }
    public static boolean ready(PTSDCrisisState state) {
        boolean surveyed=state.warOpeningSurveyCompleted;
        PTSDStrategicSurvey.prepareWarOpening(state,PTSDCrisisState.getDay());
        prepare(state,PTSDCrisisState.getDay(),!surveyed);
        boolean sourceAvailable=false;
        for(PTSDCrisisState.SystemData d:state.systems.values())
            if(d.omegaControl>=.5f && state.resolveSystem(d.systemId)!=null){sourceAvailable=true;break;}
        if(!sourceAvailable)return false;
        if(state.late.strategicReserve+.01f<state.late.openingAssetsRequired ||
                state.late.warPoints+.01f<state.late.openingPointsRequired)return false;
        // Final check uses today's complete target set; a new colony must not produce a partial opening.
        prepare(state,PTSDCrisisState.getDay(),true);
        return state.late.strategicReserve+.01f>=state.late.openingAssetsRequired &&
                state.late.warPoints+.01f>=state.late.openingPointsRequired;
    }
    public static void start(PTSDCrisisState state) {
        if(state.late.firstWaveSelected)return;
        if(state.phase!=PTSDCrisisState.Phase.WAR && !ready(state))return;
        float day=PTSDCrisisState.getDay();List<MarketAPI> edge=targets(state,true);
        float median=medianDefense(state,edge);int index=0;
        PTSDPriorityTargetDirector.launchOpening(state,day);
        for(MarketAPI m:edge) if(launch(state,m,true,median,day+(index++%6),"opening"))state.late.selectedBurstCount++;
        state.late.firstWaveSelected=true;state.late.warStartedDay=day;state.late.nextRegionalOffensiveDay=day+30;
        PTSDWarIntel.ensureIntel();
    }
    static StarSystemAPI source(PTSDCrisisState state,StarSystemAPI target) {
        StarSystemAPI best=null;float distance=Float.MAX_VALUE;
        for(PTSDCrisisState.SystemData d:state.systems.values())if(d.omegaControl>=.5f) {
            StarSystemAPI s=state.resolveSystem(d.systemId);if(s==null)continue;
            float v=Misc.getDistance(s.getLocation(),target.getLocation());
            if(v<distance){distance=v;best=s;}
        }
        return best;
    }
    private static boolean launch(PTSDCrisisState state,MarketAPI market,boolean opening,float median,float day,String wave) {
        return launch(state,market,market.getStarSystem(),opening,median,day,wave,null);
    }
    static boolean launchPriority(PTSDCrisisState state,MarketAPI market,StarSystemAPI system,float day,String star) {
        return launch(state,market,system,true,0,day,"priority:"+system.getId(),star);
    }
    private static boolean launch(PTSDCrisisState state,MarketAPI market,StarSystemAPI target,boolean opening,float median,float day,String wave,String priorityStar) {
        if(data.scripts.PTSDDifficultySettings.scale(state,day)<=0)return false;
        if(PTSDProtectedFaction.system(target) || (market!=null && PTSDProtectedFaction.isGamma(market.getFactionId())))return false;
        for(PTSDCrisisState.StrategicEvent e:state.getActiveEvents())
            if(e.simulatedWarOperation && (market!=null?market.getId().equals(e.targetMarketId):target.getId().equals(e.targetSystemId)&&e.targetMarketId==null))return false;
        StarSystemAPI origin=source(state,target);if(origin==null)return false;
        float defense=market==null?Math.max(60,state.getSystemData(target.getId()).observedFleetStrength):observedDefense(state,market),strength=force(defense,opening)*data.scripts.PTSDDifficultySettings.scale(state,day);
        boolean free=opening || priorityStar!=null;float assets=free?0:strength,wp=free?0:points(strength,opening);
        if(state.late.strategicReserve+.01f<assets||state.late.warPoints+.01f<wp)return false;
        PTSDCrisisState.StrategicEvent e=state.addEvent(PTSDCrisisState.EventType.ATTACK,PTSDCrisisAPI.SIDE_OMEGA,
                IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION,origin.getId(),target.getId(),market==null?null:market.getId(),strength,100f);
        e.priorityStarId=priorityStar;
        e.simulatedWarOperation=true;e.physicalWarWave=true;e.openingOperation=opening;e.singularityOperation=free;
        e.operationNominalFp=strength;e.musterUntilDay=day;e.operationWaveId=opening?"opening:"+target.getId():wave;
        e.opponentFactionId=market==null?null:market.getFactionId();e.operationOriginX=origin.getLocation().x;e.operationOriginY=origin.getLocation().y;
        e.operationTargetX=target.getLocation().x;e.operationTargetY=target.getLocation().y;
        e.operationArrivalDay=day+Math.max(2,Math.min(opening?24:40,Misc.getDistance(origin.getLocation(),target.getLocation())/1800f));
        e.resolveDay=e.operationArrivalDay+5;
        Random r=new Random(e.id.hashCode());double angle=Math.atan2(e.operationOriginY-e.operationTargetY,e.operationOriginX-e.operationTargetX)+(r.nextFloat()-.5)*Math.toRadians(60);
        float length=2200+r.nextFloat()*2200;e.approachX=e.operationTargetX+(float)Math.cos(angle)*length;e.approachY=e.operationTargetY+(float)Math.sin(angle)*length;
        state.late.strategicReserve=Math.max(0,state.late.strategicReserve-assets);state.late.warPoints=Math.max(0,state.late.warPoints-wp);state.late.warPointBelief=Math.max(0,state.late.warPointBelief-wp);
        PTSDLateWarDirector.recordSpending(state,(priorityStar!=null?"名单免费部署 ":free?"特异点免费部署 ":opening?"首波免费部署 ":"区域攻势 ")+e.id,wp,assets);
        return true;
    }
    public static Vector2f position(PTSDCrisisState.StrategicEvent e,float day) {
        float start=e.operationReturnScheduled?e.resolveDay:e.musterUntilDay;
        float end=e.operationReturnScheduled?e.operationReturnDay:e.operationArrivalDay;
        float t=Math.max(0,Math.min(1,(day-start)/Math.max(.01f,end-start)));
        if(e.operationReturnScheduled)t=1-t;
        return new Vector2f(e.operationOriginX+(e.operationTargetX-e.operationOriginX)*t,
                e.operationOriginY+(e.operationTargetY-e.operationOriginY)*t);
    }
    public static boolean near(PTSDCrisisState.StrategicEvent e,CampaignFleetAPI player,float day) {
        if(player==null)return false;
        if(!e.operationReturnScheduled && day<e.musterUntilDay)return false;
        if(player.isInHyperspace())return Misc.getDistance(player.getLocation(),position(e,day))<4500;
        if(player.getStarSystem()==null)return false;
        if(e.operationReturnScheduled)return e.sourceSystemId.equals(player.getStarSystem().getId())&&day>=e.operationReturnDay;
        return e.targetSystemId.equals(player.getStarSystem().getId())&&day>=e.operationArrivalDay && !e.operationReturnScheduled;
    }
    public static void arrival(PTSDCrisisState state,PTSDCrisisState.StrategicEvent e,float day) {
        if(e.operationArrived)return;e.operationArrived=true;e.operationContactDay=day;
        MarketAPI market=state.resolveMarket(e.targetMarketId);
        if(market!=null&&market.isPlayerOwned()) {
            CampaignFleetAPI player=Global.getSector().getPlayerFleet();float travel=0;
            if(player!=null)travel=Misc.getDistanceLY(player.getLocationInHyperspace(),market.getLocationInHyperspace())/
                    Math.max(.01f,Misc.getLYPerDayAtBurn(player,Math.max(1,player.getFleetData().getMinBurnLevel())));
            e.operationRescueDeadline=day+PTSDTaskRules.rescueWindow(travel);
            e.resolveDay=Math.max(e.resolveDay,e.operationRescueDeadline);
        }
        PTSDLateWarDirector.enqueueAlert(state,"arrival:"+e.id,e.targetSystemId,
                (market==null?"星系航路":market.getName())+"：侦测到未知舰船袭击。外围警戒正在接敌，各航路立即避让。",day);
        PTSDLateState.Front front=new PTSDLateState.Front();front.id="operation:"+e.id;front.operationId=e.id;
        front.systemId=e.targetSystemId;front.marketId=e.targetMarketId;front.attackBudget=e.strength;
        front.warningDay=day;front.nextStrikeDay=e.resolveDay;front.burst=e.openingOperation;
        front.playerRescueWarningDay=day;front.playerRescueDeadline=e.operationRescueDeadline;
        state.late.fronts.put(front.id,front);
        if(e.openingOperation && market!=null) {
            PTSDLateWarDirector.enqueueAlert(state,"arrival-disruption:"+e.id,e.targetSystemId,
                    "轨道告急｜"+market.getName()+"：港口暂停全部民用出航。防务频段内多处呼叫无人应答。",day+.15f);
            PTSDLateWarDirector.enqueueAlert(state,"arrival-evacuation:"+e.id,e.targetSystemId,
                    "航路中断｜"+market.getName()+"：撤离船队请求护航。远端监听站仍在报告新的武装接触。",day+.4f);
        }
    }
    static void closeFront(PTSDCrisisState state,PTSDCrisisState.StrategicEvent event) {
        PTSDLateState.Front front=state.late.fronts.get("operation:"+event.id);
        if(front!=null){front.resolved=true;front.lost=event.successful;front.attackBudget=event.strength;}
    }
    public static void advance(PTSDCrisisState state,float day,IIRT_Omega_Invasion host) {
        if(!state.late.firstWaveSelected)return;
        if(day>=state.late.nextRegionalOffensiveDay) {
            List<MarketAPI> edge=targets(state,true),all=targets(state,false);
            final PTSDCrisisState planningState=state;
            Collections.sort(edge,new Comparator<MarketAPI>() {
                public int compare(MarketAPI a,MarketAPI b){return Float.compare(observedDefense(planningState,a),observedDefense(planningState,b));}
            });
            boolean launched=false;float median=medianDefense(state,edge);
            float cheapestAssets=Float.MAX_VALUE,cheapestPoints=Float.MAX_VALUE;
            for(MarketAPI seed:edge) {
                List<MarketAPI> region=new ArrayList<MarketAPI>();float assets=0,wp=0;
                for(MarketAPI m:all) {
                    if(Misc.getDistance(m.getStarSystem().getLocation(),seed.getStarSystem().getLocation())>REGION_RADIUS)continue;
                    boolean assigned=false;
                    for(PTSDCrisisState.StrategicEvent e:state.getActiveEvents())
                        if(e.simulatedWarOperation&&m.getId().equals(e.targetMarketId)){assigned=true;break;}
                    if(assigned)continue;
                    region.add(m);float defense=observedDefense(state,m);
                    {float strength=force(defense,false)*data.scripts.PTSDDifficultySettings.scale(state,day);assets+=strength;wp+=points(strength,false);}
                }
                if(region.isEmpty())continue;
                if(assets<cheapestAssets){cheapestAssets=assets;cheapestPoints=wp;}
                if(assets>state.late.strategicReserve||wp>state.late.warPoints)continue;
                String wave="region:"+(++state.late.regionalOffensiveNumber);
                for(MarketAPI m:region)launch(state,m,false,median,day,wave);
                launched=true;break;
            }
            if(cheapestAssets<Float.MAX_VALUE){state.late.regionalAssetsRequired=cheapestAssets;state.late.regionalPointsRequired=cheapestPoints;}
            // Retry a funded whole region, never pretend a partly launched region is complete.
            state.late.nextRegionalOffensiveDay=day+(launched?Math.min(30f,30f/Math.max(.5f,IIRT_Omega_ModPlugin.lateOffensiveTempo)):1);
        }
        for(PTSDCrisisState.StrategicEvent e:new ArrayList<PTSDCrisisState.StrategicEvent>(state.events)) {
            if(!e.simulatedWarOperation||e.operationReturned)continue;
            if(e.status==PTSDCrisisState.EventStatus.RESOLVED && e.materializedDay>0) {
                boolean alive=false;
                for(String id:e.operationReturningFleetIds) {
                    SectorEntityToken token=Global.getSector().getEntityById(id);
                    if(token instanceof CampaignFleetAPI && ((CampaignFleetAPI)token).isAlive()){alive=true;break;}
                }
                if(!alive)e.operationReturned=true;
            }
            if(e.status==PTSDCrisisState.EventStatus.RESOLVED && e.materializedDay<=0) {
                if(!e.operationReturnScheduled) {
                    e.operationReturnScheduled=true;e.operationReturnDay=day+Math.max(2,e.operationArrivalDay-e.musterUntilDay);
                    e.resolveDay=day;
                }
                if(near(e,Global.getSector().getPlayerFleet(),day)) {
                    host.materializeEvent(state,e);e.status=PTSDCrisisState.EventStatus.RESOLVED;host.despawnEventFleets(e);
                } else if(day>=e.operationReturnDay) {
                    PTSDLateWarDirector.creditStrategicAssets(state,"virtual-return:"+e.id,refund(e.strength,e.openingOperation,e.singularityOperation),"模拟部队返抵来源地",e.sourceSystemId);
                    e.operationReturned=true;
                }
            }
        }
    }
}
