package data.scripts.campaign.invasion;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.loading.CampaignPingSpec;
import org.lwjgl.util.vector.Vector2f;
import java.lang.reflect.*;
import java.io.*;
import java.util.*;

/** Delayed materialization, search navigation and visible cues without requiring a fleet factory. */
public final class BlackoutSearchTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    static SectorEntityToken token(StarSystemAPI sys,Vector2f at){
        Vector2f position=new Vector2f(at);
        return ControlledPatrolRouteTest.proxy(SectorEntityToken.class,(name,args)->{
            if(name.equals("getLocation"))return position;
            if(name.equals("getContainingLocation")||name.equals("getStarSystem"))return sys;
            if(name.equals("setLocation")){position.set((Float)args[0],(Float)args[1]);return null;}
            return null;
        });
    }
    public static void main(String[] args)throws Exception{
        SectorAPI old=Global.getSector();SettingsAPI oldSettings=Global.getSettings();
        try{
            Map<String,Object> data=new HashMap<>();Map<String,SectorEntityToken> entities=new HashMap<>();
            List<CampaignPingSpec> pings=new ArrayList<>();List<EveryFrameScript> scripts=new ArrayList<>();
            List<CampaignFleetAPI> fleets=new ArrayList<>();List<MarketAPI> markets=new ArrayList<>();
            StarSystemAPI[] place={null};
            SectorEntityToken exit=token(null,new Vector2f(10000,0));
            StarSystemAPI sys=ControlledPatrolRouteTest.proxy(StarSystemAPI.class,(name,a)->{
                if(name.equals("getId"))return "blackout-system";
                if(name.equals("getFleets"))return fleets;
                if(name.equals("getJumpPoints"))return Collections.singletonList(exit);
                if(name.equals("getPlanets")||name.equals("getAllEntities"))return Collections.emptyList();
                if(name.equals("createToken"))return token(place[0],(Vector2f)a[0]);
                return null;
            });place[0]=sys;
            Vector2f playerPosition=new Vector2f(12000,0);
            CampaignFleetAPI player=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,name->
                    name.equals("getLocation")?playerPosition:name.equals("getContainingLocation")||name.equals("getStarSystem")?sys:null);
            MemoryAPI memory=SceneRevisionTest.memory(new HashMap<>());
            com.fs.starfarer.api.fleet.MutableFleetStatsAPI stationStats=NewsSceneMechanicsTest.mock(
                    com.fs.starfarer.api.fleet.MutableFleetStatsAPI.class,name->
                    name.equals("getSensorRangeMod")?new com.fs.starfarer.api.combat.StatBonus():null);
            CampaignFleetAPI station=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,name->
                    name.equals("getId")?"station":name.equals("isAlive")?true:
                    name.equals("getLocation")?new Vector2f():name.equals("getContainingLocation")||name.equals("getStarSystem")?sys:
                    name.equals("getMemoryWithoutUpdate")?memory:name.equals("getStats")?stationStats:null);
            Map<String,Object> stationData=new HashMap<>();stationData.put(MemFlags.STATION_FLEET,station);
            SectorEntityToken connected=ControlledPatrolRouteTest.proxy(SectorEntityToken.class,(name,a)->
                    name.equals("hasTag")?Tags.STATION.equals(a[0]):name.equals("getMemoryWithoutUpdate")?SceneRevisionTest.memory(stationData):null);
            markets.add(NewsSceneMechanicsTest.mock(MarketAPI.class,name->name.equals("getFactionId")?Factions.INDEPENDENT:
                    name.equals("getConnectedEntities")?Collections.singleton(connected):null));
            EconomyAPI economy=NewsSceneMechanicsTest.mock(EconomyAPI.class,name->name.equals("getMarkets")?markets:null);
            CampaignClockAPI clock=NewsSceneMechanicsTest.mock(CampaignClockAPI.class,name->null);
            Global.setSettings(NewsSceneMechanicsTest.mock(SettingsAPI.class,name->null));
            Global.setSector(ControlledPatrolRouteTest.proxy(SectorAPI.class,(name,a)->{
                if(name.equals("getPlayerFleet"))return player;
                if(name.equals("getPersistentData"))return data;
                if(name.equals("getClock"))return clock;
                if(name.equals("getStarSystem"))return sys;
                if(name.equals("getEntityById"))return entities.get(a[0]);
                if(name.equals("getEconomy"))return economy;
                if(name.equals("addScript")){scripts.add((EveryFrameScript)a[0]);return null;}
                if(name.equals("addPing")){check(a[1] instanceof CampaignPingSpec,"Cue did not use native custom ping");pings.add((CampaignPingSpec)a[1]);}
                return null;
            }));
            Constructor<PTSDCrisisState> ctor=PTSDCrisisState.class.getDeclaredConstructor();ctor.setAccessible(true);
            PTSDCrisisState state=ctor.newInstance();state.progressInitialized=true;state.phase=PTSDCrisisState.Phase.RECON;
            data.put(PTSDCrisisState.PERSISTENT_KEY,state);entities.put("station",station);fleets.add(station);
            PTSDCrisisState.CrisisIncident item=new PTSDCrisisState.CrisisIncident();item.id="blackout";item.cardId="P-09";
            item.recordedByPlayer=true;item.investigable=true;item.acceptedDuringRecon=true;item.investigationOutcome=1;item.targetSystemId="blackout-system";
            item.investigationExpiresDay=PTSDCrisisState.getDay()+60;item.scene=new PTSDNewsScene.State();
            item.scene.kind=PTSDNewsScene.Kind.STATION_BLACKOUT;state.incidents.add(item);
            check(PTSDReconScenes.build(state,item,sys,new Random(4)),"Blackout pre-arrival build failed");
            check(item.scene.recon.waitingForArrival&&item.scene.recon.enemy==null&&item.scene.fleets.isEmpty(),"Fleet spawned before arrival");
            check(scripts.size()==1&&scripts.get(0) instanceof PTSDReconScenes.PressureClock,"Staged clock not registered");
            item.siteMaterialized=true;item.targetEntityId="station";
            SectorEntityToken navigation=PTSDSearchAreaNavigation.resolve(item);
            check(navigation!=station&&Vector2f.sub(navigation.getLocation(),station.getLocation(),null).length()>=2000,
                    "Search navigation still points at station");
            check(PTSDCrisisAPI.resolveIncidentTarget(item)==station,"Navigation changed simulation target");
            check(PTSDReconScenes.dialog(item,station)==null,"Search scene hijacks the station dialog");
            PTSDReconScenes.PressureClock pressure=(PTSDReconScenes.PressureClock)scripts.get(0);
            check(!pressure.runWhilePaused(),"Pulses run while paused");
            pressure.advance(1);pressure.advance(10);
            check(pings.size()==2&&pings.get(0).getRange()>=18000&&pings.get(1).getRange()>=18000,"Pre-arrival cues missing or too small");
            check(pings.get(0).getDuration()==3.5f&&pings.get(1).getDuration()==3.5f,"Pre-arrival cue became an active interdiction pulse");
            check(pings.get(0).getColor().equals(PTSDReconScenes.BLACKOUT_BEIGE)&&
                    pings.get(1).getColor().equals(PTSDReconScenes.BLACKOUT_PURPLE),"Cue colors not beige/purple");
            check(item.scene.recon.seconds==0&&item.scene.recon.enemy==null&&!item.scene.resolved,
                    "Pre-arrival clock suppresses station or resolves missing enemy");
            PTSDReconScenes.advance(state,item,PTSDCrisisState.getDay());
            PTSDFirstReconInvestigation.advanceSites(state);
            check(state.firstReconInvestigationId==null&&item.scene.recon.enemy==null&&!item.scene.resolved,
                    "Background encounter or first-investigation guarantee bypassed staging");
            // A patrol covering every candidate must defer, rather than force a visible spawn.
            float[] patrolRange={100000f};boolean[] actuallyVisible={true};
            CampaignFleetAPI patrol=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,name->name.equals("isAlive")?true:
                    name.equals("getLocation")?new Vector2f():name.equals("getMaxSensorRangeToDetect")?patrolRange[0]:null);
            fleets.add(patrol);PTSDCampaignRuntime.invalidate(sys);
            playerPosition.set(item.scene.recon.searchX,item.scene.recon.searchY);pressure.advance(4);
            check(item.scene.recon.arrivedAtSearch&&item.scene.recon.waitingForArrival&&item.scene.fleets.isEmpty(),
                    "Covered patrol area forced a spawn");
            SectorEntityToken candidate=token(sys,new Vector2f(2000,0));
            patrolRange[0]=500f;check(PTSDReconScenes.hiddenFromPatrols(sys,candidate,player,station,false),"Safe candidate rejected");
            CampaignFleetAPI hidden=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,name->name.equals("isVisibleToSensorsOf")?actuallyVisible[0]:
                    name.equals("getContainingLocation")?sys:name.equals("getLocation")?new Vector2f(2000,0):null);
            check(!PTSDReconScenes.hiddenFromPatrols(sys,hidden,player,station,true),"Actual sensor visibility ignored");
            actuallyVisible[0]=false;check(PTSDReconScenes.hiddenFromPatrols(sys,hidden,player,station,true),"Hidden fleet rejected");
            int before=pings.size();PTSDReconScenes.activeBlackoutPulse(hidden,player);
            check(pings.size()==before+2,"Invisible source failed to emit both player-visible cues");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();new ObjectOutputStream(bytes).writeObject(item.scene.recon);
            PTSDReconScenes.Progress saved=(PTSDReconScenes.Progress)new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();
            check(saved.waitingForArrival&&saved.arrivedAtSearch&&saved.beaconSequence==item.scene.recon.beaconSequence&&
                    saved.searchX==item.scene.recon.searchX,"Reload loses staged position or pulse sequence");
            check(PTSDSearchAreaNavigation.searches(PTSDNewsScene.Kind.DARK_TRANSIT)&&
                    !PTSDSearchAreaNavigation.searches(PTSDNewsScene.Kind.PORT_RECORDS)&&
                    !PTSDSearchAreaNavigation.searches(PTSDNewsScene.Kind.RECON_SURRENDER),"Navigation changed interactive scene types");
            entities.put("exit",exit);item.scene.reference="station";item.scene.kind=PTSDNewsScene.Kind.FORT_PATROL_GAP;item.targetEntityId="exit";
            SectorEntityToken window=PTSDSearchAreaNavigation.resolve(item);
            float fromFacility=Vector2f.sub(window.getLocation(),station.getLocation(),null).length();
            check(fromFacility>1600&&fromFacility<3600,"Patrol-window navigation points near jump instead of facility");
            item.scene.kind=PTSDNewsScene.Kind.STATION_BLACKOUT;item.targetEntityId="station";
            item.investigationAbandoned=true;pressure.advance(1);check(pressure.isDone(),"Abandoned search keeps emitting");
            System.out.println("BlackoutSearchTest: passed (deferred build, area navigation, remote cues, patrol exclusion, visible pulses, reload, cleanup)");
        }finally{PTSDCampaignRuntime.reset();Global.setSector(old);Global.setSettings(oldSettings);}
    }
}
