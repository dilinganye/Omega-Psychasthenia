package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldSource;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.shard.PTSD_BaseShard_Util;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Materializes, confirms and cleans up true-news physical scenes. */
public final class PTSDNewsSiteManager {
    public static final String AUTO = "AUTO";
    public static final String COMMUNICATION = "COMMUNICATION";
    public static final String ROUTE = "ROUTE";
    public static final String BATTLE = "BATTLE_AFTERMATH";
    public static final String CREW = "CREW_MISSING";
    public static final String FACILITY = "FACILITY";
    public static final String DISTORTION = "DISTORTION";

    private static final String[][] SCENES = {
            {"被寄生的通讯浮标","十二秒之后的回声","不存在的第二座基站","全频道的单字回复","军民识别码互换区"},
            {"被移动的跳跃点航标","航迹突然折返","自动导航拒绝区","延迟抵达的跃迁残光","沿航路排列的空救生舱"},
            {"只缺少武器接口的战场","没有弹道来源的贯穿孔","被迫排成战列线的残骸","仍在执行命令的断舰","一轮齐射的烧蚀墙","伪造的交战双方"},
            {"完整用餐却无人存在的穿梭艇","黑匣子组成的监听阵列","没有接收者的求救信号","被排练过的幸存者证词","漂流的无人葬礼船"},
            {"被拆开又复原的采矿站","自己建造自己的维修无人机","多出来的一段轨道设施","反向工作的传感器阵列","无材料来源的施工框架"},
            {"不照亮物体的白光","固定在背景上的舰影","反向扩散的碎片云","缺少一块星空的区域","重复发生的三秒钟","只对武器系统响应的空间裂纹"}
    };
    private static final String[][] DETAILS = {
            {"未知节点贴伏在基站结构阴影内，每隔数秒向无人方向发射窄束校验码。","应答器会延迟十二秒复述玩家刚刚发出的识别和能力信号。","传感器同时显示两座重合基站，其中一座没有实体碰撞却提前广播数据。","不同设备用不同信道发送请求，却在同一时间戳收到同一个字符。","民船残骸被标成军舰，军事节点被标成货柜，而实体自身的识别码没有改变。"},
            {"合法航标被拖离原轨道，复制品仍在向自动导航发送错误跃迁参数。","发动机尾迹在没有转弯半径的情况下原路折返，折返点藏有响应计时器。","环形低功率节点持续诱导自动驾驶重新规划一片空无一物的区域。","跳跃点反复出现没有舰队抵达的跃迁残光，第二轮会复现玩家的接近角度。","来自不同失踪舰队的空救生舱被等距排列成一把航路测距标尺。"},
            {"不同势力残骸结构完整，唯独武器接口和传感器阵列被在有动力时切除。","残骸块沿一条笔直射线排列，远端一次性节点记录了贯穿射击。","溃散舰体被拖成标准战列线，舰首全部指向同一无物方向。","断成数段的舰体仍被外接节点驱动，周期性尝试完成最后一次转向。","装甲碎片形成一面平面烧蚀墙，证明齐射在击穿反应堆前主动停止。","两组公开敌对势力的残骸都受到来自同一第三方向的攻击。"},
            {"穿梭艇生命维持和餐食加热仍在运行，人员与个人终端却全部消失。","多艘失踪舰船的黑匣子围绕未知核心接线，组合成完整交通图。","求救信标会根据接近舰队的势力和规模改写措辞，并记得玩家曾经离开。","幸存者使用完全相同的停顿和错误描述遭遇，救援前曾反复收到同一音频。","无人葬礼船播放尚未失踪人员的阵亡名单，排程时间早于相关舰队抵达。"},
            {"采矿站曾被拆成标准模块后原样焊回，只缺少制造误差和应力记录。","无母舰维修无人机不断拆解损坏个体，再制造外形逐轮偏离的替代品。","空间站外环多出无门舱段，持续耗能并监听靠港舰船，却没有施工记录。","传感器阵列反向测量自己被发现所需的时间，并随玩家隐蔽状态改变响应。","轨道骨架没有材料运输来源，并缓慢改变形状让自己始终背向玩家。"},
            {"强烈白光不照亮任何附近物体，能量读数反而随接近不断下降。","巨大舰影固定在恒星背景上没有视差，距离读数却持续逼近零。","碎片云反向汇聚到空无中心，最终短暂勾勒出未知舰体轮廓。","一小块恒星背景完全缺失，并随玩家改变自动导航目标而转向。","浮标、碎片和噪声每隔三秒回到旧状态，只有玩家位置继续前进。","空间裂纹只对炮塔转动、锁定和开火响应，并复制武器朝向产生延迟闪光。"}
    };    private static final Random RANDOM = new Random();
    private PTSDNewsSiteManager() { }

    public static void advance(PTSDCrisisState state, float day, Random random) {
        if (state == null || Global.getSector() == null) return;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        for (PTSDCrisisState.CrisisIncident incident : state.incidents) {
            if (incident == null || !incident.recordedByPlayer || incident.investigationOutcome != 1) continue;
            if (!incident.siteMaterialized && !incident.investigationResolved && player != null &&
                    player.getStarSystem() != null && incident.targetSystemId != null &&
                    incident.targetSystemId.equals(player.getStarSystem().getId())) {
                materialize(state, incident, random == null ? RANDOM : random, false, false);
            }
            if (incident.siteMaterialized && incident.siteHandlerExpression != null && !incident.siteHandlerExpression.isEmpty()) {
                PTSDNewsSiteAPI.SiteHandler handler = PTSDNewsSiteAPI.resolveHandler(incident.siteHandlerExpression);
                if (handler != null) try { handler.advance(context(state, incident, random)); }
                catch (Throwable ex) { Global.getLogger(PTSDNewsSiteManager.class).warn("Site advance failed", ex); }
            }
            if (incident.siteMaterialized && incident.siteCleanupDay > 0f && day >= incident.siteCleanupDay) cleanup(state, incident, false);
            if (!incident.investigationResolved && incident.investigationExpiresDay > 0f && day >= incident.investigationExpiresDay) cleanup(state, incident, true);
        }
    }

    public static boolean materialize(PTSDCrisisState state, PTSDCrisisState.CrisisIncident incident,
                                      Random random, boolean forceMartial, boolean devForced) {
        if (state == null || incident == null || incident.siteMaterialized) return false;
        StarSystemAPI system = state.resolveSystem(incident.targetSystemId);
        if (system == null) return false;
        SectorEntityToken oldTarget = PTSDCrisisAPI.resolveIncidentTarget(incident);
        MarketAPI market = state.resolveMarket(incident.targetMarketId);
        if (oldTarget == null || oldTarget.getContainingLocation() != system) oldTarget = market != null ? market.getPrimaryEntity() : system.getCenter();
        Random rng = random == null ? new Random(incident.id == null ? 1L : incident.id.hashCode()) : random;
        PTSDNewsSiteAPI.SiteResult result = null;
        if (incident.siteHandlerExpression != null && !incident.siteHandlerExpression.trim().isEmpty()) {
            PTSDNewsSiteAPI.SiteHandler handler = PTSDNewsSiteAPI.resolveHandler(incident.siteHandlerExpression);
            if (handler != null) try { result = handler.materialize(new PTSDNewsSiteAPI.SiteContext(state, incident, system, market, oldTarget, rng)); }
            catch (Throwable ex) { Global.getLogger(PTSDNewsSiteManager.class).warn("Custom site materialization failed", ex); }
        }
        if (result == null) result = materializeBuiltIn(state, incident, system, oldTarget, rng);
        if (result == null || result.anchor == null) return false;
        incident.siteMaterialized = true;
        incident.siteTitle = result.title;
        incident.siteDescription = result.description;
        incident.siteConfirmationHint = result.confirmationHint;
        if (incident.siteEntityIds == null) incident.siteEntityIds = new ArrayList<String>();
        incident.siteEntityIds.clear(); incident.siteEntityIds.addAll(result.entityIds);
        if (!incident.siteEntityIds.contains(result.anchor.getId())) incident.siteEntityIds.add(result.anchor.getId());
        incident.targetEntityId = result.anchor.getId();
        incident.siteHardExpireDay = incident.investigationExpiresDay > 0f ? incident.investigationExpiresDay : PTSDCrisisState.getDay() + 30f;
        if ((forceMartial || incident.martialSiteEligible) && (forceMartial || rng.nextFloat() < .5f)) spawnMartialScene(incident, system, result.anchor, rng);
        PTSDCrisisDevIntel.report("新闻属实现场显现", result.title + " / " + result.description + (incident.martialSiteSpawned ? " / 武力调查场景" : ""), system.getId(), result.anchor.getId());
        if (devForced || Global.getSettings().isDevMode()) Global.getSector().getCampaignUI().addMessage("[DEV现场] " + result.title, new Color(206,142,255));
        return true;
    }

    private static PTSDNewsSiteAPI.SiteResult materializeBuiltIn(PTSDCrisisState state, PTSDCrisisState.CrisisIncident incident,
                                                                  StarSystemAPI system, SectorEntityToken target, Random rng) {
        String family = pickFamily(incident.siteTemplate, incident.id);
        int familyIndex = familyIndex(family);
        String[] choices = SCENES[familyIndex];
        int choice = (((incident.id == null ? rng.nextInt() : incident.id.hashCode()) + incident.randomBranch) & 0x7fffffff) % choices.length;
        String title = choices[choice];
        Vector2f point = findSafePoint(system, target, rng, 1800f, 5200f);
        List<String> ids = new ArrayList<String>();
        SectorEntityToken anchor;
        if (COMMUNICATION.equals(family)) {
            anchor = addEntity(system, title, choice % 2 == 0 ? Entities.SENSOR_ARRAY_MAKESHIFT : Entities.GENERIC_PROBE_ACTIVE, point, ids);
            addEntity(system, "窄束校验节点", Entities.GENERIC_PROBE, nearby(point, rng, 260f, 720f), ids);
        } else if (ROUTE.equals(family)) {
            anchor = addEntity(system, title, Entities.NAV_BUOY_MAKESHIFT, point, ids);
            int count = choice == 4 ? 8 : 3;
            for (int i=0;i<count;i++) addEntity(system, choice==4?"空救生舱":"航迹记录节点", Entities.GENERIC_PROBE, nearby(point,rng,320f,1500f),ids);
        } else if (BATTLE.equals(family)) {
            anchor = addDebris(system, title, point, rng, ids, 30f);
            for (int i=0;i<3+rng.nextInt(4);i++) addWreck(system, nearby(point,rng,350f,1800f), Factions.INDEPENDENT, rng, ids, 45f);
        } else if (CREW.equals(family)) {
            anchor = addWreck(system, point, Factions.INDEPENDENT, rng, ids, 30f);
            if (anchor != null) anchor.setName(title);
            for (int i=0;i<(choice==1?5:2);i++) addEntity(system,"失去记录的黑匣子",Entities.GENERIC_PROBE,nearby(point,rng,180f,900f),ids);
        } else if (FACILITY.equals(family)) {
            String type = choice == 0 ? Entities.STATION_MINING : (choice == 3 ? Entities.SENSOR_ARRAY_MAKESHIFT : Entities.MAKESHIFT_STATION);
            anchor = addEntity(system, title, type, point, ids);
            addEntity(system,"无登记施工节点",Entities.GENERIC_PROBE_ACTIVE,nearby(point,rng,500f,1300f),ids);
        } else {
            anchor = addEntity(system, title, Entities.GENERIC_PROBE_ACTIVE, point, ids);
            for (int i=0;i<5+rng.nextInt(5);i++) addEntity(system,"无法稳定测距的碎片",Entities.GENERIC_PROBE,nearby(point,rng,250f,1400f),ids);
            try { Global.getSector().addPing(anchor,"sensor_burst",new Color(220,210,255)); } catch (Throwable ignored) { }
        }
        if (anchor == null) return null;
        anchor.setDiscoverable(true);
        String description = descriptionFor(family, title);
        return new PTSDNewsSiteAPI.SiteResult(anchor,title,description,"抵近至约1800距离并完成传感器确认",ids);
    }

    private static String descriptionFor(String family, String title) {
        int fi=familyIndex(family);for(int i=0;i<SCENES[fi].length;i++)if(SCENES[fi][i].equals(title))return DETAILS[fi][i];
        return "现场证据与公开报道相符，但其排列方式明显是为观察和采样而设计。";
    }

    public static void confirm(PTSDCrisisState state, PTSDCrisisState.CrisisIncident incident) {
        if (state == null || incident == null) return;
        incident.investigationResolved = true; incident.investigationReal = true; incident.siteConfirmed = true;
        incident.siteCleanupDay = PTSDCrisisState.getDay() + 1f + new Random(incident.id.hashCode()).nextFloat() * 4f;
        if (incident.siteHandlerExpression != null && !incident.siteHandlerExpression.isEmpty()) {
            PTSDNewsSiteAPI.SiteHandler handler = PTSDNewsSiteAPI.resolveHandler(incident.siteHandlerExpression);
            if (handler != null) try { handler.onConfirmed(context(state,incident,RANDOM)); } catch (Throwable ignored) { }
        }
        PTSDCrisisDevIntel.report("新闻现场确认", incident.siteTitle + " / 动态实体将在 " + Math.round(incident.siteCleanupDay-PTSDCrisisState.getDay()) + " 日内清理", incident.targetSystemId, incident.targetEntityId);
    }

    public static void resolveRemotely(PTSDCrisisState state, PTSDCrisisState.CrisisIncident incident) {
        if (state == null || incident == null) return;
        if (incident.siteTemplate == null || incident.siteTemplate.isEmpty()) incident.siteTemplate = DISTORTION;
        String family=pickFamily(incident.siteTemplate, incident.id); String[] choices=SCENES[familyIndex(family)];
        incident.siteTitle=choices[(incident.id.hashCode()&0x7fffffff)%choices.length];
        incident.siteDescription=descriptionFor(family,incident.siteTitle);
        incident.siteConfirmed=true;
    }

    public static void cleanup(PTSDCrisisState state, PTSDCrisisState.CrisisIncident incident, boolean expired) {
        if (incident == null || Global.getSector() == null) return;
        if (incident.siteHandlerExpression != null && !incident.siteHandlerExpression.isEmpty()) {
            PTSDNewsSiteAPI.SiteHandler handler=PTSDNewsSiteAPI.resolveHandler(incident.siteHandlerExpression);
            if(handler!=null)try{handler.onExpired(context(state,incident,RANDOM));}catch(Throwable ignored){}
        }
        if (incident.siteEntityIds == null) incident.siteEntityIds = new ArrayList<String>();
        for(String id:new ArrayList<String>(incident.siteEntityIds)){
            SectorEntityToken entity=Global.getSector().getEntityById(id);
            if(entity!=null&&entity.getContainingLocation()!=null){if(Entities.WRECK.equals(entity.getCustomEntityType())||entity instanceof CampaignTerrainAPI)continue;try{entity.getContainingLocation().removeEntity(entity);}catch(Throwable ignored){}}
        }
        incident.siteEntityIds.clear(); incident.siteMaterialized=false; incident.siteCleanupDay=0f;
        if(expired){incident.investigationResolved=true;incident.investigationReal=false;}
    }

    private static PTSDNewsSiteAPI.SiteContext context(PTSDCrisisState state, PTSDCrisisState.CrisisIncident incident, Random random){
        StarSystemAPI system=state.resolveSystem(incident.targetSystemId);MarketAPI market=state.resolveMarket(incident.targetMarketId);
        return new PTSDNewsSiteAPI.SiteContext(state,incident,system,market,PTSDCrisisAPI.resolveIncidentTarget(incident),random==null?RANDOM:random);
    }

    private static void spawnMartialScene(PTSDCrisisState.CrisisIncident incident, StarSystemAPI system, SectorEntityToken anchor, Random rng){
        int wrecks=2+rng.nextInt(5);String faction=nearbyFaction(system);
        for(int i=0;i<wrecks;i++)addWreck(system,nearby(anchor.getLocation(),rng,450f,2200f),faction,rng,incident.siteEntityIds,45f+rng.nextFloat()*20f);
        float fp=12f+rng.nextFloat()*28f;
        FleetParamsV3 params=new FleetParamsV3(anchor.getLocation(),IIRT_Omega_Invasion.WATCHER_FACTION,.25f,FleetTypes.MERC_SCOUT,fp,0f,0f,0f,0f,0f,0f);
        params.maxNumShips=2+rng.nextInt(4);
        CampaignFleetAPI fleet=PTSD_BaseShard_Util.createFleet(params,fp,PTSD_BaseShard_Util.FleetRole.RECON,rng);
        if(fleet!=null){system.addEntity(fleet);Vector2f p=nearby(anchor.getLocation(),rng,900f,2100f);fleet.setLocation(p.x,p.y);fleet.setName(rng.nextBoolean()?"正在测绘残骸的未知编队":"沉默的战场调查单元");fleet.setNoFactionInName(true);fleet.setTransponderOn(false);fleet.addTag("PTSD_omega_scout");fleet.addTag(Tags.SALVAGE_ENTITY_NO_DEBRIS);fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,true);fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF,true);fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY,true);fleet.addAssignment(rng.nextBoolean()?FleetAssignment.ORBIT_PASSIVE:FleetAssignment.PATROL_SYSTEM,anchor,8f+rng.nextFloat()*7f,rng.nextBoolean()?"逐段扫描战损舰体":"重建最后一次交火");incident.siteEntityIds.add(fleet.getId());incident.martialSiteSpawned=true;}
    }

    private static String nearbyFaction(StarSystemAPI system){for(MarketAPI m:Global.getSector().getEconomy().getMarkets(system))if(m!=null&&!m.isPlanetConditionMarketOnly()&&!IIRT_Omega_Invasion.WATCHER_FACTION.equals(m.getFactionId())&&!IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(m.getFactionId()))return m.getFactionId();return Factions.INDEPENDENT;}
    private static SectorEntityToken addEntity(StarSystemAPI system,String name,String type,Vector2f point,List<String>ids){try{SectorEntityToken e=system.addCustomEntity(null,name,type,Factions.NEUTRAL);e.setLocation(point.x,point.y);e.addTag("PTSD_news_true_site");ids.add(e.getId());return e;}catch(Throwable ex){Global.getLogger(PTSDNewsSiteManager.class).warn("Unable to add site entity "+type,ex);return null;}}
    private static SectorEntityToken addDebris(StarSystemAPI system,String name,Vector2f point,Random rng,List<String>ids,float days){DebrisFieldParams p=new DebrisFieldParams(220f,-1f,days,.4f);p.source=DebrisFieldSource.BATTLE;p.baseSalvageXP=25;SectorEntityToken e=Misc.addDebrisField(system,p,rng);e.setLocation(point.x,point.y);e.setName(name);ids.add(e.getId());return e;}
    private static SectorEntityToken addWreck(StarSystemAPI system,Vector2f point,String faction,Random rng,List<String>ids,float days){try{DerelictShipEntityPlugin.DerelictShipData d=DerelictShipEntityPlugin.createRandom(faction,null,rng,0f);d.durationDays=days;SectorEntityToken e=BaseThemeGenerator.addSalvageEntity(rng,system,Entities.WRECK,faction,d);e.setLocation(point.x,point.y);e.addTag(Tags.UNRECOVERABLE);ids.add(e.getId());return e;}catch(Throwable ex){return null;}}
    private static Vector2f nearby(Vector2f point,Random rng,float min,float max){Vector2f p=new Vector2f(point);Vector2f off=Misc.getUnitVectorAtDegreeAngle(rng.nextFloat()*360f);off.scale(min+rng.nextFloat()*(max-min));return Vector2f.add(p,off,p);}
    private static Vector2f findSafePoint(StarSystemAPI system,SectorEntityToken anchor,Random rng,float min,float max){Vector2f base=anchor==null?system.getCenter().getLocation():anchor.getLocation();for(int a=0;a<40;a++){Vector2f p=nearby(base,rng,min,max);boolean safe=true;for(PlanetAPI planet:system.getPlanets()){if(planet!=null&&Misc.getDistance(p,planet.getLocation())<Math.max(1800f,planet.getRadius()+1200f)){safe=false;break;}}if(safe)for(SectorEntityToken jump:system.getJumpPoints()){if(Misc.getDistance(p,jump.getLocation())<Math.max(1200f,jump.getRadius()+800f)){safe=false;break;}}if(safe)return p;}return nearby(base,rng,6500f,8000f);}
    private static String pickFamily(String value,String seed){if(value==null||value.trim().isEmpty()||AUTO.equalsIgnoreCase(value.trim()))return DISTORTION;String[]parts=value.split("\\|");int hash=(seed==null?value:seed).hashCode();return parts[(hash&0x7fffffff)%parts.length].trim().toUpperCase();}
    public static String normalizeFamily(String value){if(value==null||value.trim().isEmpty()||AUTO.equalsIgnoreCase(value.trim()))return DISTORTION;String[]parts=value.split("\\|");return parts[Math.abs(value.hashCode())%parts.length].trim().toUpperCase();}
    private static int familyIndex(String f){if(COMMUNICATION.equals(f))return 0;if(ROUTE.equals(f))return 1;if(BATTLE.equals(f))return 2;if(CREW.equals(f))return 3;if(FACILITY.equals(f))return 4;return 5;}

    public static void devMaterializeLatest(String family,boolean martial){PTSDCrisisState s=PTSDCrisisState.get();if(s==null)return;for(int i=s.incidents.size()-1;i>=0;i--){PTSDCrisisState.CrisisIncident in=s.incidents.get(i);if(in==null||!in.investigable)continue;if(in.siteEntityIds==null)in.siteEntityIds=new ArrayList<String>();in.recordedByPlayer=true;in.investigationOutcome=1;in.investigationExpiresDay=PTSDCrisisState.getDay()+30f;in.siteTemplate=family;cleanup(s,in,false);materialize(s,in,new Random(Misc.genUID().hashCode()),martial,true);return;}}
}