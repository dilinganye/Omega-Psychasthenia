package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.shard.PTSD_BaseShard_Util;
import org.lwjgl.util.vector.Vector2f;
import java.io.Serializable;
import java.util.*;

/** Explicit CSV scene registry. State belongs to the incident, not the UI or handler instance. */
public final class PTSDNewsScene {
    public enum Kind {
        ROUTE_REVERSAL, ROUTE_DIVERSION, RELAY_PARASITE, SIGNAL_PURSUIT,
        WRECK_RECONSTRUCTION, MISSING_CONVOY, FACILITY_INTERDICTION, SPATIAL_ECHO,
        FORT_SUPPLY_WINDOW, FORT_SENSOR_LATTICE, FORT_PATROL_GAP, FORT_DECOY_TRANSIT,
        FORT_GRAVITY_SURVEY, FORT_SALVAGE_INTERCEPTION,
        PORT_RECORDS, ABANDONED_FACILITY, SURRENDERED_FLEET, TRAFFIC_OBSERVATION,
        RECON_FIRE_CONTROL, RECON_SURRENDER, JUMP_BLOCKADE, STATION_BLACKOUT, DARK_TRANSIT
    }
    public static final String INCIDENT = "$PTSD_scene_incident";
    public static final class State implements Serializable {
        private static final long serialVersionUID = 1L;
        public Kind kind;
        public PTSDReconScenes.Progress recon;
        public PTSDJumpBlockade.State blockade;
        public int branch, samples;
        public String wreckMode;
        public boolean wreckResponseCreated;
        public List<String> nodes = new ArrayList<String>();
        public List<String> owned = new ArrayList<String>();
        public Map<String, Float> fleets = new LinkedHashMap<String, Float>();
        public Set<String> visited = new LinkedHashSet<String>();
        public Set<String> portEvidence = new LinkedHashSet<String>();
        public Set<String> pulsed = new LinkedHashSet<String>();
        public String reference, civilian, ghostId, sourceSystemId, operationNodeId, responseNodeId, result = "", lastError = "";
        public int deliveredBefore, lostBefore;
        public boolean towingRecord;
        public int rescuedShips;
        public boolean rewardSettled;
        public boolean calibrationError;
        public boolean resolved, cleaned, ghostRolled, ghostFleet, responseQueued, responseSpawned;
        public boolean timerArmed, locked, damaged, disrupted, escort, recordTaken, recordLost, downloading;
        public float downloadProgress;
        public float recordCollectionDay;
        public float secondsLeft = 24f, responseDay, lockUntil, operationStart = -1f, lastDay, nextTick;
        public int interference;

        /**
         * 旧存档没有后来新增的集合时，反序列化不会可靠执行字段初始化器。
         * 只补空集合，不重抽差分、不重置计时或已取得的证据。由每次加载的一次性修补调用。
         */
        public void repairCollections() {
            if (nodes == null) nodes = new ArrayList<String>();
            if (owned == null) owned = new ArrayList<String>();
            if (fleets == null) fleets = new LinkedHashMap<String, Float>();
            if (visited == null) visited = new LinkedHashSet<String>();
            if (portEvidence == null) portEvidence = new LinkedHashSet<String>();
            if (pulsed == null) pulsed = new LinkedHashSet<String>();
        }
    }
    private PTSDNewsScene() { }
    public static Kind parse(String key) {
        if (key == null) return null;
        try { return Kind.valueOf(key.trim().toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException ex) { return null; }
    }
    public static boolean manages(PTSDCrisisState.CrisisIncident i) {
        if (i == null) return false;
        if (i.scene != null) return true;
        // 自定义现场优先。保留 template 作为文案/DEV 信息，不再吞掉 DLC 的处理器。
        if (i.siteHandlerExpression != null && !i.siteHandlerExpression.trim().isEmpty()) return false;
        if (i.siteTemplate != null) for (String key : i.siteTemplate.split("\\|")) if (parse(key) != null) return true;
        return false;
    }
    public static boolean validTemplates(String expression) {
        if (expression == null || expression.trim().isEmpty()) return true;
        for (String key : expression.split("\\|")) if (parse(key) == null &&
                !Arrays.asList("AUTO","COMMUNICATION","ROUTE","BATTLE_AFTERMATH","CREW_MISSING","FACILITY","DISTORTION").contains(key.trim())) return false;
        return true;
    }
    static SectorEntityToken entity(String id) {
        return id == null || id.isEmpty() || Global.getSector() == null ? null : Global.getSector().getEntityById(id);
    }
    static CampaignFleetAPI fleet(String id) { SectorEntityToken e=entity(id); return e instanceof CampaignFleetAPI?(CampaignFleetAPI)e:null; }
    static PlanetAPI hole(StarSystemAPI system) {
        for (PlanetAPI p:system.getPlanets()) if(p.isStar() && p.getSpec()!=null && p.getSpec().isBlackHole())return p;
        return null;
    }
    static PTSDLateState.Fortress fort(PTSDCrisisState state,String system) {
        if(state.late!=null)for(PTSDLateState.Fortress f:state.late.fortresses.values())
            if(f!=null&&!f.destroyed&&!f.inTransit&&system!=null&&system.equals(f.currentSystemId))return f;
        return null;
    }
    public static String unavailable(Kind k,PTSDCrisisState state,StarSystemAPI sys) {
        if(sys==null)return PTSDNewsSceneText.SCENE_001;
        if(PTSDReconScenes.handles(k))return PTSDReconScenes.unavailable(k,sys);
        if(k==Kind.JUMP_BLOCKADE)return PTSDJumpBlockade.pick(sys)==null?"需要至少两个出口、双向连通且没有其他封锁的跃迁点":null;
        // Relay scenes use an existing utility installation where possible; a missing installation may be created.
        if((k==Kind.ROUTE_REVERSAL||k==Kind.FORT_DECOY_TRANSIT) && sys.getJumpPoints().size()<(k==Kind.FORT_DECOY_TRANSIT?2:1))return PTSDNewsSceneText.SCENE_002;
        if(k==Kind.FORT_PATROL_GAP&&sys.getJumpPoints().isEmpty())return "缺少真实换防出口";
        if(k==Kind.FORT_GRAVITY_SURVEY && hole(sys)==null)return PTSDNewsSceneText.SCENE_003;
        if((k==Kind.PORT_RECORDS||k==Kind.SURRENDERED_FLEET)&&publicMarket(sys)==null)return PTSDNewsSceneText.SCENE_004;
        if(k==Kind.TRAFFIC_OBSERVATION&&traffic(sys,null)==null)return PTSDNewsSceneText.SCENE_005;
        if(k==Kind.FORT_SENSOR_LATTICE){boolean neighbor=false;for(StarSystemAPI other:Global.getSector().getStarSystems())
            if(other!=sys&&!other.hasTag(Tags.THEME_HIDDEN)&&!other.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)){neighbor=true;break;}
            if(!neighbor)return PTSDNewsSceneText.SCENE_006;}
        if(k.name().startsWith("FORT_") && k!=Kind.FORT_DECOY_TRANSIT && k!=Kind.FORT_GRAVITY_SURVEY && fort(state,sys.getId())==null)return PTSDNewsSceneText.SCENE_007;
        if(k.name().startsWith("FORT_")&&k!=Kind.FORT_DECOY_TRANSIT&&k!=Kind.FORT_GRAVITY_SURVEY&&entity(fort(state,sys.getId()).fleetId)==null)return PTSDNewsSceneText.SCENE_008;
        if((k==Kind.MISSING_CONVOY||k==Kind.FORT_SALVAGE_INTERCEPTION)&&sys.getJumpPoints().isEmpty())return PTSDNewsSceneText.SCENE_009;
        if(k==Kind.FORT_SUPPLY_WINDOW) {
            PTSDLateState.Fortress f=fort(state,sys.getId());boolean live=false;
            if(f!=null)for(String id:f.transportIds)if(fleet(id)!=null)live=true;
            if(!live)return PTSDNewsSceneText.SCENE_010;
        }
        if(k==Kind.FACILITY_INTERDICTION && fort(state,sys.getId())==null) {
            boolean found=false;for(CampaignFleetAPI f:sys.getFleets())if(PTSDJeOtloesManager.isOmegaFleet(f)&&f.isStationMode())found=true;
            if(!found)return PTSDNewsSceneText.SCENE_011;
        }
        return null;
    }
    private static MarketAPI publicMarket(StarSystemAPI sys){
        for(MarketAPI market:Global.getSector().getEconomy().getMarkets(sys))if(!market.isPlanetConditionMarketOnly()&&market.getPrimaryEntity()!=null&&
                !IIRT_Omega_Invasion.WATCHER_FACTION.equals(market.getFactionId())&&!IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(market.getFactionId()))return market;
        return null;
    }
    private static CampaignFleetAPI traffic(StarSystemAPI sys,String preferred){
        CampaignFleetAPI candidate=fleet(preferred);if(candidate!=null&&candidate.getStarSystem()==sys&&!candidate.isStationMode())return candidate;
        for(CampaignFleetAPI f:sys.getFleets())if(f!=Global.getSector().getPlayerFleet()&&!f.isStationMode()&&f.getFleetPoints()>0)return f;
        return null;
    }
    public static boolean accepts(String expression,PTSDCrisisState state,StarSystemAPI sys) {
        if(expression==null)return true;boolean specialized=false;
        for(String key:expression.split("\\|")){Kind k=parse(key);if(k==null)continue;specialized=true;if(unavailable(k,state,sys)==null)return true;}
        return !specialized;
    }
    static void preparePublicLocation(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i){
        if(i.siteTemplate==null||!i.siteTemplate.contains("FORT_SENSOR_LATTICE"))return;
        StarSystemAPI source=state.resolveSystem(i.targetSystemId);if(source==null)return;
        StarSystemAPI closest=null;float distance=Float.MAX_VALUE;
        for(StarSystemAPI sys:Global.getSector().getStarSystems())if(sys!=source&&!sys.hasTag(Tags.THEME_HIDDEN)&&!sys.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)){
            float d=Misc.getDistance(sys.getLocation(),source.getLocation());if(d<distance){distance=d;closest=sys;}}
        if(closest!=null){i.sceneSourceSystemId=source.getId();i.targetSystemId=closest.getId();i.targetEntityId=closest.getCenter().getId();i.targetMarketId=null;}
    }
    static Vector2f offset(Vector2f base,float angle,float distance) {
        Vector2f d=Misc.getUnitVectorAtDegreeAngle(angle);d.scale(distance);return Vector2f.add(base,d,null);
    }
    static SectorEntityToken node(PTSDCrisisState.CrisisIncident i,StarSystemAPI system,String name,Vector2f point,String type) {
        SectorEntityToken n=PTSDNewsSiteManager.addEntity(system,name,type,point,i.scene.owned);
        if(n!=null){n.getMemoryWithoutUpdate().set(INCIDENT,i.id);i.scene.nodes.add(n.getId());Misc.makeImportant(n,"PTSD_scene_"+i.id);}
        return n;
    }
    static SectorEntityToken wreck(PTSDCrisisState.CrisisIncident i,StarSystemAPI sys,Vector2f point,Random rng) {
        SectorEntityToken n=PTSDNewsSiteManager.addWreck(sys,point,PTSDNewsSiteManager.nearbyFaction(sys),rng,i.scene.owned,
                Math.max(1f,i.investigationExpiresDay-PTSDCrisisState.getDay()));
        if(n!=null){n.getMemoryWithoutUpdate().set(INCIDENT,i.id);i.scene.nodes.add(n.getId());Misc.makeImportant(n,"PTSD_scene_"+i.id);}return n;
    }
    static boolean wildernessWreckSite(StarSystemAPI sys){
        if(sys==null || publicMarket(sys)!=null)return false;
        for(CampaignTerrainAPI terrain:sys.getTerrainCopy())
            if(Terrain.ASTEROID_BELT.equals(terrain.getType())||Terrain.ASTEROID_FIELD.equals(terrain.getType()))return !sys.getAsteroids().isEmpty();
        return false;
    }
    static String wreckMode(String card,int branch){
        if("D-01".equals(card))return "TWIN_SCOUTS";
        if("D-02".equals(card))return "SALVAGERS";
        if("D-07".equals(card))return "LATE_SCOUT";
        if("D-12".equals(card))return "BELT_OBSERVER";
        return new String[]{"SALVAGERS","BELT_OBSERVER","LATE_SCOUT"}[Math.floorMod(branch,3)];
    }
    static boolean spawnRareWreck(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,StarSystemAPI sys,Vector2f point,boolean heatdeath){
        if(data.scripts.PTSDDifficultySettings.spawningPaused())return false;
        String variant=heatdeath?"IIRT_Omega_Heatdeath_Normal":"IIRT_Omega_Tangento_Only_3";
        if(!Global.getSettings().doesVariantExist(variant))return false;
        CampaignFleetAPI large=Global.getFactory().createEmptyFleet(PTSDCrisisProgress.getActiveFactionId(state),heatdeath?PTSDNewsSceneText.RARE_HEAT:PTSDNewsSceneText.RARE_TRANSPORT,true);
        large.getFleetData().addFleetMember(Global.getFactory().createFleetMember(com.fs.starfarer.api.fleet.FleetMemberType.SHIP,variant));
        large.getMemoryWithoutUpdate().set("$PTSD_encounter_response_installed",true);
        large.getMemoryWithoutUpdate().set("$PTSD_participation_key","incident:"+i.id);
        large.setTransponderOn(false);large.setNoFactionInName(true);
        sys.addEntity(large);large.setLocation(point.x+1800,point.y+600);PTSDCampaignRuntime.entityAdded(large);
        for(com.fs.starfarer.api.fleet.FleetMemberAPI member:large.getFleetData().getMembersListCopy())member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
        i.scene.fleets.put(large.getId(),PTSDCrisisState.getDay()+12);
        PTSDSceneScoutBehavior.install(large,i.scene.nodes,false,true);
        if(!heatdeath){
            i.scene.fleets.remove(large.getId()); // Rare carrier is the explicit early-cap exception; its escort still uses normal limits.
            CampaignFleetAPI escort=spawnEnemy(state,i,sys,new Vector2f(point.x+2100,point.y+400),12,12,false);
            if(escort==null){remove(large.getId());i.scene.fleets.remove(large.getId());return false;}
            escort.getMemoryWithoutUpdate().set(PTSDSceneScoutBehavior.CONTROLLED,true);
            escort.getMemoryWithoutUpdate().set("$PTSD_encounter_response_installed",true);
            escort.addAssignment(FleetAssignment.DEFEND_LOCATION,large,12,PTSDNewsSceneText.RARE_ESCORT);
            i.scene.fleets.put(large.getId(),PTSDCrisisState.getDay()+12);
        }
        return true;
    }
    private static void spawnWreckObserver(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,StarSystemAPI sys,Vector2f point,boolean pursuitOnly){
        CampaignFleetAPI scout=spawnEnemy(state,i,sys,point,PTSDCrisisFleetLimits.forPhase(state,PTSDCrisisState.getDay()).perFleetFp,30,false);
        if(scout!=null){
            List<String> route=new ArrayList<String>(i.scene.nodes);
            if(!route.isEmpty())Collections.rotate(route,-Math.max(0,i.scene.fleets.size()-1));
            PTSDSceneScoutBehavior.install(scout,route,false,pursuitOnly);
        }
    }
    static SectorEntityToken existingInstallation(StarSystemAPI sys){
        for(String tag:new String[]{Tags.COMM_RELAY,Tags.SENSOR_ARRAY,Tags.NAV_BUOY})
            if(!sys.getEntitiesWithTag(tag).isEmpty())return sys.getEntitiesWithTag(tag).get(0);
        return null;
    }
    static void bind(PTSDCrisisState.CrisisIncident i,SectorEntityToken target){
        if(!i.scene.nodes.contains(target.getId()))i.scene.nodes.add(target.getId());
        // Never claim ownership or replace the world's memory with one incident ID.
        Misc.makeImportant(target,"PTSD_scene_"+i.id);
    }
    static void unmark(PTSDCrisisState.CrisisIncident i){
        if(i.scene==null)return;
        for(String id:i.scene.nodes){SectorEntityToken n=entity(id);if(n!=null)Misc.makeUnimportant(n,"PTSD_scene_"+i.id);}
    }
    public static boolean materialize(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,StarSystemAPI sys,boolean dev) {
        if(i.siteMaterialized)return true;
        if(i.sceneSourceSystemId!=null){sys=state.resolveSystem(i.sceneSourceSystemId);if(sys==null)return false;}
        if(i.scene==null){
            State s=new State();List<Kind> eligible=new ArrayList<Kind>();
            for(String key:i.siteTemplate.split("\\|")){Kind k=parse(key);if(k!=null&&unavailable(k,state,sys)==null)eligible.add(k);}
            if(eligible.isEmpty())return false;
            Random rng=new Random(i.id.hashCode());s.kind=eligible.get(rng.nextInt(eligible.size()));s.branch=Math.floorMod(i.randomBranch,3);i.scene=s;
        }
        State s=i.scene;String reason=unavailable(s.kind,state,sys);
        if(reason!=null){s.lastError=reason;return false;}
        s.sourceSystemId=sys.getId();
        Random rng=new Random(i.id.hashCode());Vector2f base=sys.getCenter().getLocation();
        SectorEntityToken original=entity(i.targetEntityId);if(original!=null&&original.getContainingLocation()==sys)base=original.getLocation();
        PTSDLateState.Fortress f=fort(state,sys.getId());
        if(f!=null){s.reference=f.fleetId;SectorEntityToken station=entity(f.fleetId);if(station!=null)base=station.getLocation();}
        if(PTSDReconScenes.handles(s.kind)) {
            if(!PTSDReconScenes.build(state,i,sys,rng))return false;
        } else if(s.kind==Kind.JUMP_BLOCKADE) {
            if(!PTSDJumpBlockade.build(i,sys))return false;
        } else if(PTSDExpansionScenes.handles(i.cardId)) {
            if(PTSDCrisisState.getDay()<s.nextTick)return false;
            if(!PTSDExpansionScenes.build(state,i,sys,rng)){s.nextTick=PTSDCrisisState.getDay()+.5f;return false;}
        } else switch(s.kind) {
            case PORT_RECORDS:
                MarketAPI port=state.resolveMarket(i.targetMarketId);
                if(port==null||port.getPrimaryEntity()==null||port.getPrimaryEntity().getContainingLocation()!=sys)port=publicMarket(sys);
                s.reference=port.getPrimaryEntity().getId();
                bind(i,port.getPrimaryEntity());
                for(MarketAPI candidate:Global.getSector().getEconomy().getMarkets(sys)) {
                    if(candidate.isPlanetConditionMarketOnly()||candidate.getPrimaryEntity()==null||PTSDPrewarReconDirector.omega(candidate.getFactionId()))continue;
                    bind(i,candidate.getPrimaryEntity());
                    if(s.nodes.size()>=2)break;
                }
                break;
            case ABANDONED_FACILITY:
                MarketAPI nearby=publicMarket(sys);if(nearby!=null)base=nearby.getPrimaryEntity().getLocation();
                node(i,sys,PTSDNewsSceneText.SCENE_012,offset(base,100,3200),Entities.MAKESHIFT_STATION);
                node(i,sys,PTSDNewsSceneText.SCENE_013,offset(base,100,4000),Entities.GENERIC_PROBE);break;
            case SURRENDERED_FLEET:
                MarketAPI pirate=state.resolveMarket(i.targetMarketId);if(pirate==null)pirate=publicMarket(sys);
                base=pirate.getPrimaryEntity().getLocation();spawnCivilian(state,i,sys,offset(base,90,1500));
                CampaignFleetAPI surrendered=fleet(s.civilian);if(surrendered!=null){surrendered.setFaction(pirate.getFactionId());surrendered.setName(PTSDNewsSceneText.SCENE_014);}
                node(i,sys,PTSDNewsSceneText.SCENE_015,offset(base,90,1900),Entities.GENERIC_PROBE);
                node(i,sys,PTSDNewsSceneText.SCENE_016,offset(base,120,2200),Entities.GENERIC_PROBE);break;
            case TRAFFIC_OBSERVATION:
                CampaignFleetAPI contact=traffic(sys,i.targetEntityId);s.reference=contact.getId();base=contact.getLocation();
                node(i,sys,PTSDNewsSceneText.SCENE_017,offset(base,30,1200),Entities.GENERIC_PROBE);
                node(i,sys,PTSDNewsSceneText.SCENE_018,offset(base,210,1400),Entities.GENERIC_PROBE);break;
            case ROUTE_REVERSAL:
                base=sys.getJumpPoints().get(0).getLocation();
                for(int n=0;n<3;n++)node(i,sys,new String[]{PTSDNewsSceneText.SCENE_019,PTSDNewsSceneText.SCENE_020,PTSDNewsSceneText.SCENE_021}[n],offset(base,35f,800+n*1100),Entities.GENERIC_PROBE);
                break;
            case ROUTE_DIVERSION:
                base=offset(sys.getCenter().getLocation(),rng.nextFloat()*360,Math.max(9000,sys.getMaxRadiusInHyperspace()*3));
                node(i,sys,PTSDNewsSceneText.SCENE_022,base,Entities.GENERIC_PROBE_ACTIVE);
                for(int n=0;n<3;n++)wreck(i,sys,offset(base,rng.nextFloat()*360,250+rng.nextFloat()*850),rng);
                break;
            case RELAY_PARASITE:
            case SIGNAL_PURSUIT:
                SectorEntityToken relay=existingInstallation(sys);
                if(relay!=null){s.reference=relay.getId();bind(i,relay);}
                else node(i,sys,PTSDNewsSceneText.SCENE_023,offset(base,120,2200),Entities.COMM_RELAY_MAKESHIFT);
                break;
            case WRECK_RECONSTRUCTION:
                s.wreckMode=wreckMode(i.cardId,s.branch);
                if("BELT_OBSERVER".equals(s.wreckMode)) {
                    for(CampaignTerrainAPI terrain:sys.getTerrainCopy())if(Terrain.ASTEROID_BELT.equals(terrain.getType())||Terrain.ASTEROID_FIELD.equals(terrain.getType())){
                        if(!sys.getAsteroids().isEmpty())base=sys.getAsteroids().get(rng.nextInt(sys.getAsteroids().size())).getLocation();
                        else base=terrain.getLocation();break;
                    }
                }else base=offset(base,rng.nextFloat()*360,2700);
                for(int n=0;n<3;n++) {
                    Vector2f point=offset(base,rng.nextFloat()*360,200+rng.nextFloat()*1100);
                    wreck(i,sys,point,rng);
                    PTSDNewsSiteManager.addDebris(sys,PTSDNewsSceneText.SCENE_024,point,rng,s.owned,Math.max(1,i.investigationExpiresDay-PTSDCrisisState.getDay()));
                }
                float rareRoll=i.devForced&&i.randomBranch==100?0:i.devForced&&i.randomBranch==101?.01f:new Random((i.id+":rare-wreck").hashCode()).nextFloat();
                if(rareRoll<.005f && spawnRareWreck(state,i,sys,base,true))s.wreckMode="HEATDEATH";
                else if(rareRoll<.025f && spawnRareWreck(state,i,sys,base,false))s.wreckMode="ENTROPY_CONVOY";
                if("SALVAGERS".equals(s.wreckMode)){spawnCivilian(state,i,sys,offset(base,80,900));CampaignFleetAPI salvager=fleet(s.civilian);
                    if(salvager!=null){salvager.setName(PTSDNewsSceneText.SCENE_025);salvager.clearAssignments();for(String id:s.nodes)salvager.addAssignment(FleetAssignment.GO_TO_LOCATION,entity(id),5,PTSDNewsSceneText.SCENE_026);}}
                if("TWIN_SCOUTS".equals(s.wreckMode)||"BELT_OBSERVER".equals(s.wreckMode)) {
                    int count="TWIN_SCOUTS".equals(s.wreckMode)?2:1;
                    for(int n=0;n<count;n++)spawnWreckObserver(state,i,sys,offset(base,n*170,1300),"BELT_OBSERVER".equals(s.wreckMode));
                    s.wreckResponseCreated=true;
                }
                break;
            case MISSING_CONVOY:
                base=sys.getJumpPoints().isEmpty()?base:sys.getJumpPoints().get(0).getLocation();
                wreck(i,sys,offset(base,80,1700),rng);node(i,sys,PTSDNewsSceneText.SCENE_027,offset(base,80,2800),Entities.GENERIC_PROBE);
                if(s.branch==0)spawnCivilian(state,i,sys,offset(base,80,2000));break;
            case FACILITY_INTERDICTION:
                if(s.reference==null)for(CampaignFleetAPI existing:sys.getFleets())if(PTSDJeOtloesManager.isOmegaFleet(existing)&&existing.isStationMode()){s.reference=existing.getId();base=existing.getLocation();break;}
                node(i,sys,PTSDNewsSceneText.SCENE_028,offset(base,110,2300),Entities.GENERIC_PROBE);break;
            case SPATIAL_ECHO:
                for(int n=0;n<3;n++)node(i,sys,PTSDNewsSceneText.SCENE_029+(n+1),offset(base,n*120,3000),Entities.GENERIC_PROBE);break;
            case FORT_SUPPLY_WINDOW:
                s.deliveredBefore=f.deliveredTransports;s.lostBefore=f.lostTransports;
                for(String id:f.transportIds)if(fleet(id)!=null){s.reference=id;break;}
                SectorEntityToken transfer=node(i,sys,s.branch==2?PTSDNewsSceneText.SCENE_030:PTSDNewsSceneText.SCENE_031,offset(base,45,s.branch==2?4800:2400),Entities.GENERIC_PROBE);
                CampaignFleetAPI shipping=fleet(s.reference);
                if(s.branch==2&&shipping!=null&&transfer!=null){shipping.clearAssignments();shipping.addAssignment(FleetAssignment.GO_TO_LOCATION,transfer,20,PTSDNewsSceneText.SCENE_032);shipping.addAssignment(FleetAssignment.ORBIT_PASSIVE,transfer,1,PTSDNewsSceneText.SCENE_033);shipping.addAssignment(FleetAssignment.GO_TO_LOCATION,entity(f.fleetId),35,PTSDNewsSceneText.SCENE_034);}
                if(s.branch==1&&shipping!=null){CampaignFleetAPI lateEscort=spawnEnemy(state,i,sys,offset(base,250,7000),35,20,false);
                    if(lateEscort!=null){lateEscort.addAssignment(FleetAssignment.ORBIT_PASSIVE,sys.getCenter(),2,PTSDNewsSceneText.SCENE_035);lateEscort.addAssignment(FleetAssignment.DEFEND_LOCATION,shipping,18,PTSDNewsSceneText.SCENE_036);}}
                break;
            case FORT_SENSOR_LATTICE:
                List<StarSystemAPI> near=new ArrayList<StarSystemAPI>(Global.getSector().getStarSystems());
                final StarSystemAPI origin=sys;
                Collections.sort(near,(a,b)->Float.compare(Misc.getDistance(a.getLocation(),origin.getLocation()),Misc.getDistance(b.getLocation(),origin.getLocation())));
                int count=0;for(StarSystemAPI region:near){if(region==sys||region.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)||region.hasTag(Tags.THEME_HIDDEN))continue;
                    node(i,region,PTSDNewsSceneText.SCENE_037,offset(region.getCenter().getLocation(),rng.nextFloat()*360,6500),Entities.GENERIC_PROBE_ACTIVE);if(++count>=3)break;}
                if(s.nodes.isEmpty())node(i,sys,PTSDNewsSceneText.SCENE_037,offset(base,180,5000),Entities.GENERIC_PROBE_ACTIVE);break;
            case FORT_PATROL_GAP:
                if(!PTSDPatrolWindow.build(state,i,sys,base))return false;
                break;
            case FORT_DECOY_TRANSIT:
                for(int n=0;n<2;n++)node(i,sys,PTSDNewsSceneText.SCENE_039+(n+1),offset(sys.getJumpPoints().get(n).getLocation(),90,700),Entities.GENERIC_PROBE_ACTIVE);break;
            case FORT_GRAVITY_SURVEY:
                PlanetAPI bh=hole(sys);float safe=Math.max(4000,bh.getRadius()+2500);
                for(int n=0;n<3;n++)node(i,sys,new String[]{PTSDNewsSceneText.SCENE_040,PTSDNewsSceneText.SCENE_041,PTSDNewsSceneText.SCENE_042}[n],offset(bh.getLocation(),30+n*18,safe+(2-n)*2200),Entities.GENERIC_PROBE);break;
            case FORT_SALVAGE_INTERCEPTION:
                base=offset(base,150,4500);for(int n=0;n<3;n++)wreck(i,sys,offset(base,rng.nextFloat()*360,200+rng.nextFloat()*950),rng);
                if(s.branch==0)spawnCivilian(state,i,sys,offset(base,90,400));
                if(s.branch==1){CampaignFleetAPI collector=spawnEnemy(state,i,sys,offset(base,230,2500),45,16,false);s.reference=collector==null?null:collector.getId();}
                break;
        }
        if(s.kind==Kind.FORT_SUPPLY_WINDOW&&f!=null&&f.eventEscortLevel>0){
            CampaignFleetAPI escort=spawnEnemy(state,i,sys,offset(base,270,3500),30+f.eventEscortLevel*20,20,false);
            SectorEntityToken transport=entity(s.reference);if(escort!=null&&transport!=null)escort.addAssignment(FleetAssignment.DEFEND_LOCATION,transport,20,PTSDNewsSceneText.SCENE_043);
        }
        if(s.nodes.isEmpty()){s.lastError=PTSDNewsSceneText.SCENE_044;return false;}
        i.siteMaterialized=true;i.siteEntityIds.clear();i.siteEntityIds.addAll(s.owned);i.targetEntityId=s.nodes.get(0);
        SectorEntityToken first=entity(i.targetEntityId);if(first!=null&&first.getStarSystem()!=null)i.targetSystemId=first.getStarSystem().getId();
        i.siteTitle=title(s.kind);i.siteDescription=PTSDExpansionScenes.handles(i.cardId)?PTSDExpansionScenes.intro(i):intro(s);i.siteConfirmationHint=PTSDNewsSceneText.SCENE_045;
        if(PTSDReconScenes.handles(s.kind)||s.kind==Kind.JUMP_BLOCKADE){i.siteDescription=PTSDReconScenes.intro(i);i.siteConfirmationHint="在大地图观察现场行动；无需逐点采样。";}
        if(s.kind==Kind.FORT_PATROL_GAP)i.siteConfirmationHint="观察巡逻队离站，在设施外围停留取证；深入前留意返航方向。";
        s.lastDay=PTSDCrisisState.getDay();s.lastError="";
        return true;
    }
    static CampaignFleetAPI spawnEnemy(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,StarSystemAPI sys,Vector2f point,float requested,float lifetime,boolean fleeing) {
        State s=i.scene;float day=PTSDCrisisState.getDay();PTSDCrisisFleetLimits limits=PTSDCrisisFleetLimits.forPhase(state,day);
        float active=0;for(String id:s.fleets.keySet()){CampaignFleetAPI f=fleet(id);if(f!=null && f.getContainingLocation()==sys)active+=f.getFleetPoints();}
        float fp=Math.min(requested,Math.min(limits.perFleetFp,limits.totalFp-active));if(fp<3)return null;
        FleetParamsV3 params=new FleetParamsV3(point,PTSDCrisisProgress.getActiveFactionId(state),.5f,FleetTypes.PATROL_SMALL,fp,0,0,0,0,0,0);
        CampaignFleetAPI f=PTSD_BaseShard_Util.createFleet(params,fp,PTSD_BaseShard_Util.FleetRole.RECON,new Random((i.id+s.fleets.size()).hashCode()),limits.forFleet(fp));
        if(f==null||f.getFleetPoints()<=0)return null;
        sys.addEntity(f);PTSDCampaignRuntime.entityAdded(f);f.setLocation(point.x,point.y);f.setName(PTSDNewsSceneText.SCENE_046);f.setNoFactionInName(true);f.setTransponderOn(false);
        f.getMemoryWithoutUpdate().set("$PTSD_participation_key","incident:"+i.id);
        for(com.fs.starfarer.api.fleet.FleetMemberAPI m:f.getFleetData().getMembersListCopy())m.getRepairTracker().setCR(m.getRepairTracker().getMaxCR());
        s.fleets.put(f.getId(),day+lifetime);
        if(fleeing){f.addTag("PTSD_scene_fleeing");f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,true);}
        return f;
    }
    static void spawnCivilian(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,StarSystemAPI sys,Vector2f point) {
        CampaignFleetAPI f=FleetFactoryV3.createFleet(new FleetParamsV3(point,PTSDNewsSiteManager.nearbyFaction(sys),.2f,FleetTypes.TRADE_SMALL,8,20,0,0,0,0,0));
        if(f==null)return;sys.addEntity(f);PTSDCampaignRuntime.entityAdded(f);f.setLocation(point.x,point.y);f.setName(PTSDNewsSceneText.SCENE_047);
        f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,true);i.scene.civilian=f.getId();i.scene.owned.add(f.getId());
        f.addAssignment(FleetAssignment.ORBIT_PASSIVE,sys.getCenter(),30,PTSDNewsSceneText.SCENE_048);
    }
    public static InteractionDialogPlugin dialogFor(SectorEntityToken target) {
        return PTSDNewsInteractionMenu.choose(target,dialogEntries(target));
    }
    /** 供统一入口合并本体与 DLC 的调查；不再让第一个命中的任务遮住同一设施的其他任务。 */
    public static List<PTSDNewsInteractionMenu.Entry> dialogEntries(SectorEntityToken target) {
        List<PTSDNewsInteractionMenu.Entry> entries=new ArrayList<PTSDNewsInteractionMenu.Entry>();
        if(target==null||PTSDPortInvestigation.openingNormal())return entries;
        InteractionDialogPlugin blockade=PTSDJumpBlockade.dialogFor(target);
        if(blockade!=null){entries.add(new PTSDNewsInteractionMenu.Entry(null,"跃迁封锁",blockade));return entries;}
        PTSDCrisisState state=PTSDCrisisState.get();if(state==null)return entries;
        for(PTSDCrisisState.CrisisIncident i:state.incidents) {
            if(i==null || !i.recordedByPlayer || i.investigationAbandoned || i.investigationResolved)continue;
            if(!i.siteMaterialized && i.investigationOutcome==1 && target.getStarSystem()!=null &&
                    target.getStarSystem().getId().equals(i.targetSystemId))
                PTSDNewsSiteManager.materialize(state,i,new Random(i.id.hashCode()),false,false);
            if(!hasActiveEntry(i,target))continue;
            InteractionDialogPlugin plugin=dialogForIncident(i,target);
            if(plugin!=null)entries.add(new PTSDNewsInteractionMenu.Entry(i,PTSDPlayerNewsText.headline(i),plugin));
        }
        return entries;
    }
    static boolean hasActiveEntry(PTSDCrisisState.CrisisIncident i,SectorEntityToken target) {
        return i!=null && target!=null && i.recordedByPlayer && !i.investigationAbandoned &&
                !i.investigationResolved && i.scene!=null && !i.scene.resolved &&
                boundNode(i,target)!=null;
    }

    /** Rules-menu re-entry must use the same physical encounter as first contact. */
    public static InteractionDialogPlugin dialogForIncident(PTSDCrisisState.CrisisIncident i,SectorEntityToken target) {
        if(!hasActiveEntry(i,target))return null;
        if(i.scene.kind==Kind.JUMP_BLOCKADE)return PTSDJumpBlockade.dialogFor(target);
        if(PTSDReconScenes.handles(i.scene.kind))return PTSDReconScenes.dialog(i,target);
        if(i.scene.kind==Kind.FORT_PATROL_GAP)return PTSDPatrolWindow.dialog(i,target);
        if(i.scene.kind==Kind.PORT_RECORDS&&PTSDPortInvestigation.supports(i.cardId))return new PTSDPortInvestigation(target,i.id);
        return new PTSDNewsSceneDialog(i,boundNode(i,target));
    }

    /** Market-linked stations and the colony's primary entity share one port investigation. */
    public static SectorEntityToken boundNode(PTSDCrisisState.CrisisIncident incident,SectorEntityToken target) {
        if(incident==null||incident.scene==null||target==null)return null;
        if(incident.scene.nodes.contains(target.getId()))return target;
        if(incident.scene.kind!=Kind.PORT_RECORDS||target.getMarket()==null||
                target instanceof CampaignFleetAPI&&!((CampaignFleetAPI)target).isStationMode())return null;
        SectorEntityToken primary=target.getMarket().getPrimaryEntity();
        return primary!=null&&incident.scene.nodes.contains(primary.getId())?primary:null;
    }

    public static String title(Kind k){if(k==Kind.JUMP_BLOCKADE)return "跃迁封锁";if(PTSDReconScenes.handles(k))return PTSDReconScenes.title(k);return new String[]{PTSDNewsSceneText.SCENE_049,PTSDNewsSceneText.SCENE_050,PTSDNewsSceneText.SCENE_051,PTSDNewsSceneText.SCENE_052,PTSDNewsSceneText.SCENE_053,PTSDNewsSceneText.SCENE_054,PTSDNewsSceneText.SCENE_055,PTSDNewsSceneText.SCENE_056,PTSDNewsSceneText.SCENE_057,PTSDNewsSceneText.SCENE_058,PTSDNewsSceneText.SCENE_059,PTSDNewsSceneText.SCENE_060,PTSDNewsSceneText.SCENE_061,PTSDNewsSceneText.SCENE_062,PTSDNewsSceneText.SCENE_063,PTSDNewsSceneText.SCENE_064,PTSDNewsSceneText.SCENE_065,PTSDNewsSceneText.SCENE_066}[k.ordinal()];}
    static String intro(State s){return PTSDNewsSceneText.intro(s);}
    public static void queueResponse(PTSDCrisisState.CrisisIncident i,float day) {
        if(i.scene.responseQueued||i.scene.responseSpawned)return;i.scene.responseQueued=true;i.scene.responseDay=day+.35f;
    }
    static void finish(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,String result,boolean supported) {
        finishAs(state,i,result,supported?PTSDTaskRules.InvestigationResult.CONFIRMED:
                result.contains(PTSDNewsSceneText.SCENE_067)?PTSDTaskRules.InvestigationResult.EXPIRED:PTSDTaskRules.InvestigationResult.DISPROVED);
    }
    static void finishAs(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,String result,PTSDTaskRules.InvestigationResult verdict) {
        State s=i.scene;if(s.resolved)return;s.resolved=true;s.result=result;unmark(i);
        boolean supported=verdict==PTSDTaskRules.InvestigationResult.CONFIRMED;
        i.investigationResolved=true;i.investigationReal=supported;i.siteConfirmed=supported;
        i.investigationResult=verdict;
        i.siteDescription=result;i.effectSummary=result;
        if(supported)PTSDCrisisIncidentManager.confirmTrueEffects(state,i);
        PTSDConditionalNewsDirector.onInvestigated(state,i);
        // The scene text is evidence, not the director's internal operation summary.
        i.effectSummary=result;
        PTSDInvestigationRewards.complete(i);
        if(!s.rewardSettled&&(s.kind==Kind.MISSING_CONVOY||s.kind==Kind.FORT_SALVAGE_INTERCEPTION||"E-06".equals(i.cardId))){
            s.rewardSettled=true;int reward=s.rescuedShips*750+(s.recordTaken?1500:0);
            CampaignFleetAPI player=Global.getSector().getPlayerFleet();
            if(player!=null&&reward>0){player.getCargo().getCredits().add(reward);s.result+=PTSDNewsSceneText.SCENE_068+reward+PTSDNewsSceneText.SCENE_069;i.effectSummary=s.result;}
        }
    }
    static void sample(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,SectorEntityToken node) {
        State s=i.scene;if(s.resolved||s.locked)return;
        if(!s.visited.add(node.getId()))return;s.samples=s.visited.size();
        s.responseNodeId=node.getId();
        if(PTSDExpansionScenes.handles(i.cardId)){PTSDExpansionScenes.sampled(state,i,node);return;}
        if(s.kind==Kind.WRECK_RECONSTRUCTION && "LATE_SCOUT".equals(s.wreckMode) && !s.wreckResponseCreated){
            s.wreckResponseCreated=true;spawnWreckObserver(state,i,node.getStarSystem(),offset(node.getLocation(),210,7000),false);
        }
        PTSDJeOtloesManager.recordParticipation(state,"incident:"+i.id);
        if(i.jeExclusiveScene)queueResponse(i,PTSDCrisisState.getDay());
        if(s.kind==Kind.SPATIAL_ECHO&&s.branch==2&&s.nodes.indexOf(node.getId())!=s.samples-1){
            pulse(node,false);queueResponse(i,PTSDCrisisState.getDay());s.result=PTSDNewsSceneText.SCENE_070;
        }
        if(s.kind==Kind.RELAY_PARASITE||s.kind==Kind.SIGNAL_PURSUIT||(s.kind==Kind.FORT_GRAVITY_SURVEY&&s.branch==2&&s.samples>=2))s.timerArmed=true;
        if(s.kind==Kind.FORT_GRAVITY_SURVEY&&s.branch==0&&s.samples>=2){
            s.calibrationError=true;s.result=PTSDNewsSceneText.SCENE_071;
            if(s.samples>=s.nodes.size())finish(state,i,PTSDNewsSceneText.SCENE_072,false);return;
        }
        if(s.samples>=s.nodes.size()&&(s.nodes.size()>1||s.kind==Kind.PORT_RECORDS)&&s.kind!=Kind.FORT_SALVAGE_INTERCEPTION&&s.kind!=Kind.MISSING_CONVOY){
            if(s.kind==Kind.FORT_GRAVITY_SURVEY&&s.branch==2)queueResponse(i,PTSDCrisisState.getDay());
            if((s.kind==Kind.ROUTE_REVERSAL||s.kind==Kind.WRECK_RECONSTRUCTION)&&s.branch==2)queueResponse(i,PTSDCrisisState.getDay());
            finish(state,i,findings(s),true);
        }
    }
    static String findings(State s){return PTSDNewsSceneText.findings(s);}
    static void pulse(SectorEntityToken source,boolean damage) {
        CampaignFleetAPI player=Global.getSector().getPlayerFleet();if(player==null||source==null||player.getContainingLocation()!=source.getContainingLocation())return;
        Global.getSector().addPing(source,Pings.INTERDICT,java.awt.Color.RED);
        player.addScript(new com.fs.starfarer.api.impl.campaign.ghosts.SpeedReduction(player,.5f));
        if(damage)for(com.fs.starfarer.api.fleet.FleetMemberAPI member:player.getFleetData().getMembersListCopy())
            member.getRepairTracker().setCR(Math.max(0,member.getRepairTracker().getCR()-.08f));
    }
    static void retire(CampaignFleetAPI fleet){if(fleet==null||fleet.getContainingLocation()==null||fleet.getBattle()!=null)return;PTSDEventFleetReturn.credit(fleet);PTSDCampaignRuntime.leaving(fleet);fleet.despawn();}
    static void remove(String id){SectorEntityToken e=entity(id);if(e!=null&&e.getContainingLocation()!=null){PTSDCampaignRuntime.leaving(e);e.getContainingLocation().removeEntity(e);}}
    public static void advance(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,float day) {
        CampaignFleetAPI player=Global.getSector().getPlayerFleet();
        if(i.scene==null||!i.siteMaterialized){
            if(i.investigationExpiresDay>0&&day>=i.investigationExpiresDay){
                i.investigationResolved=true;i.investigationResult=PTSDTaskRules.InvestigationResult.EXPIRED;
                i.effectSummary=PTSDNewsSceneText.SCENE_073;
                PTSDConditionalNewsDirector.onExpired(state,i,i.effectSummary);return;
            }
            if(!i.investigationResolved&&player!=null){
                StarSystemAPI targetSystem=state.resolveSystem(i.targetSystemId);
                if(player.getStarSystem()!=null&&player.getStarSystem().getId().equals(i.targetSystemId))materialize(state,i,player.getStarSystem(),false);
                else if("JUMP_BLOCKADE".equals(i.siteTemplate)&&player.isInHyperspace()&&targetSystem!=null&&targetSystem.getHyperspaceAnchor()!=null&&Misc.getDistance(player,targetSystem.getHyperspaceAnchor())<5000)materialize(state,i,targetSystem,false);
            }
            if(i.scene==null||!i.siteMaterialized)return;
        }
        State s=i.scene;
        if(s.kind==Kind.JUMP_BLOCKADE){PTSDJumpBlockade.advance(state,i,day);return;}
        if(PTSDReconScenes.handles(s.kind)){PTSDReconScenes.advance(state,i,day);return;}
        if(s.cleaned&&s.fleets.isEmpty()&&(!s.responseQueued||s.responseSpawned))return;
        if(day<s.nextTick)return;float elapsed=Math.max(0,day-s.lastDay);s.lastDay=day;s.nextTick=day+.05f;
        if(day>=i.investigationExpiresDay&&!s.resolved)finish(state,i,PTSDNewsSceneText.SCENE_074,false);
        SectorEntityToken anchor=s.nodes.isEmpty()?null:entity(s.nodes.get(0));
        SectorEntityToken responseAnchor=entity(s.responseNodeId);if(responseAnchor==null)responseAnchor=anchor;
        if(s.responseQueued&&!s.responseSpawned&&day>=s.responseDay&&responseAnchor!=null){
            StarSystemAPI sys=responseAnchor.getStarSystem();if(sys!=null){CampaignFleetAPI response=spawnEnemy(state,i,sys,offset(responseAnchor.getLocation(),210,7500),PTSDCrisisFleetLimits.forPhase(state,day).perFleetFp,30,false);
                if(response!=null){CampaignFleetAPI rescueTarget=s.escort?fleet(s.civilian):null;
                    if(rescueTarget!=null)response.addAssignment(FleetAssignment.INTERCEPT,rescueTarget,3,PTSDNewsSceneText.SCENE_075);
                    else response.addAssignment(FleetAssignment.GO_TO_LOCATION,responseAnchor,3,PTSDNewsSceneText.SCENE_076);
                    response.addAssignment(FleetAssignment.PATROL_SYSTEM,sys.getCenter(),30,PTSDNewsSceneText.SCENE_077);s.responseSpawned=true;
                    if(i.jeExclusiveScene){CampaignFleetAPI wing=spawnEnemy(state,i,sys,offset(responseAnchor.getLocation(),120,8200),PTSDCrisisFleetLimits.forPhase(state,day).perFleetFp,30,false);
                        if(wing!=null){wing.addAssignment(FleetAssignment.GO_TO_LOCATION,responseAnchor,3,PTSDNewsSceneText.SCENE_078);wing.addAssignment(FleetAssignment.PATROL_SYSTEM,sys.getCenter(),30,PTSDNewsSceneText.SCENE_079);}}
                }}
        }
        for(String id:new ArrayList<String>(s.fleets.keySet())){
            CampaignFleetAPI f=fleet(id);if(f==null){s.fleets.remove(id);continue;}
            if(f.hasTag("PTSD_scene_fleeing") && f.getBattle()==null){
                String key="$PTSD_scene_unseen_since";
                if(f.isVisibleToPlayerFleet())f.getMemoryWithoutUpdate().unset(key);
                else if(!f.getMemoryWithoutUpdate().contains(key))f.getMemoryWithoutUpdate().set(key,day);
                else if(day-f.getMemoryWithoutUpdate().getFloat(key)>=.2f){retire(f);s.fleets.remove(id);continue;}
            }
            if(day>=s.fleets.get(id)){if(f.getBattle()==null){retire(f);s.fleets.remove(id);}continue;}
            if(!f.getMemoryWithoutUpdate().getBoolean(PTSDSceneScoutBehavior.CONTROLLED)&&!f.hasTag("PTSD_scene_fleeing")&&f.getStarSystem()!=null&&f.getBattle()==null&&player!=null&&player.getContainingLocation()==f.getContainingLocation()&&player.isVisibleToSensorsOf(f)){
                if(f.getCurrentAssignment()==null||f.getCurrentAssignment().getAssignment()!=FleetAssignment.INTERCEPT){f.clearAssignments();f.addAssignment(FleetAssignment.INTERCEPT,player,.3f,PTSDNewsSceneText.SCENE_080);f.addAssignment(FleetAssignment.PATROL_SYSTEM,f.getStarSystem().getCenter(),3,PTSDNewsSceneText.SCENE_081);}
            }
        }
        if(s.timerArmed&&!s.locked&&!s.resolved&&Global.getSector().getCampaignUI().getCurrentInteractionDialog()==null){
            s.secondsLeft-=elapsed*10f; // Offline/paused time is absent; outside the dialog this is campaign-day time.
            if(s.secondsLeft<=0)timeout(i,anchor);
        }
        if(s.operationStart>=0&&!s.disrupted&&!s.resolved){
            SectorEntityToken operationNode=entity(s.operationNodeId);if(operationNode==null)operationNode=anchor;
            if(operationNode==null||player==null||player.getContainingLocation()!=operationNode.getContainingLocation()||Misc.getDistance(player,operationNode)>1800)s.operationStart=day;
            else if(day-s.operationStart>=1){s.disrupted=true;queueResponse(i,day);PTSDLateState.Fortress f=fort(state,s.sourceSystemId);
                if(f!=null){f.blockedUntilDay=Math.max(f.blockedUntilDay,day+4);f.eventEscortLevel=Math.min(3,f.eventEscortLevel+1);}
                else if(s.kind==Kind.FACILITY_INTERDICTION)PTSDLateWarDirector.disruptProduction(state,i.id,3f,0f,s.sourceSystemId);
                finish(state,i,PTSDNewsSceneText.SCENE_082,true);}
        }
        if(s.kind==Kind.ROUTE_DIVERSION)advanceGhost(state,i,player,anchor);
        if(s.locked&&day>=s.lockUntil&&!s.resolved){s.locked=false;s.secondsLeft=24f;}
        if(!s.resolved&&player!=null){
            for(String id:new ArrayList<String>(s.nodes)){SectorEntityToken n=entity(id);if(n==null||n.getContainingLocation()!=player.getContainingLocation())continue;
                if(Misc.getDistance(n,player)<1600&&s.kind==Kind.FORT_SENSOR_LATTICE&&s.pulsed.add(id)){
                    PTSDJeOtloesManager.recordParticipation(state,"incident:"+i.id);
                    pulse(n,false);CampaignFleetAPI response=spawnEnemy(state,i,n.getStarSystem(),offset(n.getLocation(),180,7500),PTSDCrisisFleetLimits.forPhase(state,day).perFleetFp,30,false);
                    if(response!=null){response.addAssignment(FleetAssignment.GO_TO_LOCATION,n,3,PTSDNewsSceneText.SCENE_083);response.addAssignment(FleetAssignment.PATROL_SYSTEM,n.getStarSystem().getCenter(),30,PTSDNewsSceneText.SCENE_084);}}
                if(s.kind==Kind.FORT_DECOY_TRANSIT&&id.equals(s.nodes.get(s.branch%2))&&Misc.getDistance(n,player)<1500&&s.pulsed.add(id)){
                    PTSDJeOtloesManager.recordParticipation(state,"incident:"+i.id);
                    pulse(n,true);wreck(i,n.getStarSystem(),n.getLocation(),new Random(i.id.hashCode()));n.setName(PTSDNewsSceneText.SCENE_085);}
            }
            if(s.kind==Kind.FORT_PATROL_GAP&&s.branch==2&&anchor!=null&&player.getContainingLocation()==anchor.getContainingLocation()&&Misc.getDistance(player,anchor)<1800)queueResponse(i,day);
        }
        if(s.kind==Kind.FORT_SUPPLY_WINDOW&&!s.resolved&&entity(s.reference)==null){
            PTSDLateState.Fortress f=fort(state,s.sourceSystemId);
            if(f!=null&&f.lostTransports>s.lostBefore){f.eventEscortLevel=Math.min(3,f.eventEscortLevel+1);queueResponse(i,day);finish(state,i,PTSDNewsSceneText.SCENE_086,true);}
            else if(f!=null&&f.deliveredTransports>s.deliveredBefore)finish(state,i,PTSDNewsSceneText.SCENE_087,true);
        }
        if(s.kind==Kind.FORT_PATROL_GAP)PTSDPatrolWindow.advance(state,i,elapsed);
        if(!s.resolved)PTSDExpansionScenes.advance(i,day);
        if((s.kind==Kind.FORT_SALVAGE_INTERCEPTION||s.kind==Kind.MISSING_CONVOY||"E-06".equals(i.cardId))&&!s.resolved)advanceRescue(state,i,day);
        if(s.resolved&&day>=i.investigationExpiresDay&&!s.cleaned&&(!s.responseQueued||s.responseSpawned)){
            boolean pending=false;for(String id:s.owned){SectorEntityToken e=entity(id);if(e instanceof CampaignFleetAPI){CampaignFleetAPI cf=(CampaignFleetAPI)e;if(cf.getBattle()!=null){pending=true;continue;}if(PTSDJeOtloesManager.isOmegaFleet(cf)){retire(cf);continue;}}remove(id);}s.cleaned=!pending;
        }
    }
    static void timeout(PTSDCrisisState.CrisisIncident i,SectorEntityToken anchor){
        State s=i.scene;if(s.locked)return;s.locked=true;s.timerArmed=false;s.lockUntil=PTSDCrisisState.getDay()+3;queueResponse(i,PTSDCrisisState.getDay());
        if(s.kind==Kind.RELAY_PARASITE&&!s.damaged){s.damaged=true;pulse(anchor,true);}s.result=PTSDNewsSceneText.SCENE_088;
    }
    private static void advanceRescue(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,float day){
        State s=i.scene;if(s.resolved)return;CampaignFleetAPI civilian=fleet(s.civilian);
        if(civilian!=null&&civilian.getFleetPoints()<=0)civilian=null;
        if("E-06".equals(i.cardId)&&s.escort&&civilian==null){
            finishAs(state,i,s.recordTaken?"巡逻队未能抵达撤离口；你保住了先前回收的战斗记录。":"巡逻队的识别信号在撤离途中消失。没有确认其脱险，残留观测不足以还原最后一段交战。",s.recordTaken?PTSDTaskRules.InvestigationResult.CONFIRMED:PTSDTaskRules.InvestigationResult.INCONCLUSIVE);return;
        }
        if(s.escort&&civilian==null){finish(state,i,s.recordTaken?PTSDNewsSceneText.SCENE_089:PTSDNewsSceneText.SCENE_090,s.recordTaken);return;}
        if(s.escort&&civilian!=null&&civilian.getBattle()==null){StarSystemAPI sys=civilian.getStarSystem();if(sys!=null&&!sys.getJumpPoints().isEmpty()&&Misc.getDistance(civilian,sys.getJumpPoints().get(0))<600){
            if(s.towingRecord)s.recordTaken=true;
            s.rescuedShips=civilian.getFleetData().getMembersListCopy().size();
            finish(state,i,PTSDNewsSceneText.SCENE_091+s.rescuedShips+PTSDNewsSceneText.SCENE_092+(s.recordTaken?PTSDNewsSceneText.SCENE_093:PTSDNewsSceneText.SCENE_094),true);remove(civilian.getId());}}
        if(s.kind==Kind.FORT_SALVAGE_INTERCEPTION&&s.branch==1&&!s.recordTaken&&!s.recordLost){CampaignFleetAPI collector=fleet(s.reference);SectorEntityToken record=s.nodes.isEmpty()?null:entity(s.nodes.get(0));
            if(collector!=null&&record!=null){if(collector.getCurrentAssignment()==null)collector.addAssignment(FleetAssignment.GO_TO_LOCATION,record,4,PTSDNewsSceneText.SCENE_095);
                if(Misc.getDistance(collector,record)<450){s.recordLost=true;collector.clearAssignments();collector.addAssignment(FleetAssignment.GO_TO_LOCATION,collector.getStarSystem().getCenter(),8,PTSDNewsSceneText.SCENE_096);s.result=PTSDNewsSceneText.SCENE_097;}}}
    }
    private static void advanceGhost(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,CampaignFleetAPI player,SectorEntityToken anchor){
        State s=i.scene;
        if(player==null||anchor==null||s.resolved||player.getContainingLocation()!=anchor.getContainingLocation()){remove(s.ghostId);s.ghostId=null;return;}
        if(!s.ghostRolled&&entity(s.ghostId)==null){
            CustomCampaignEntityAPI g=anchor.getContainingLocation().addCustomEntity(null,null,Entities.SENSOR_GHOST,Factions.NEUTRAL);
            g.setSensorProfile(1800f);g.setDiscoverable(true);g.setRadius(30);g.setLocation(player.getLocation().x+5000,player.getLocation().y+1800);
            s.ghostId=g.getId();g.addScript(new PTSDSceneGhostMotion(i.id,g));
        }
        if(!s.ghostRolled&&Misc.getDistance(player,anchor)<1200){s.ghostRolled=true;s.ghostFleet=new Random((i.id+":ghost").hashCode()).nextBoolean();
            PTSDJeOtloesManager.recordParticipation(state,"incident:"+i.id);
            SectorEntityToken g=entity(s.ghostId);Vector2f location=g==null?offset(anchor.getLocation(),45,1800):new Vector2f(g.getLocation());remove(s.ghostId);s.ghostId=null;
            if(s.ghostFleet){CampaignFleetAPI scout=spawnEnemy(state,i,anchor.getStarSystem(),location,40,12,true);if(scout!=null){
                Vector2f away=Vector2f.sub(location,player.getLocation(),null);if(away.lengthSquared()<1)away=new Vector2f(1,0);away.normalise();away.scale(20000);Vector2f.add(location,away,away);
                scout.addAssignment(FleetAssignment.GO_TO_LOCATION,anchor.getContainingLocation().createToken(away),12,PTSDNewsSceneText.SCENE_098);
                if(scout.getAbility(Abilities.EMERGENCY_BURN)!=null)scout.getAbility(Abilities.EMERGENCY_BURN).activate();}}
        }
    }
}
