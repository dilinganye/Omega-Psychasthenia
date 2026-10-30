package data.scripts.campaign.invasion;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import data.scripts.campaign.invasion.news.*;
import java.lang.reflect.*;
import java.util.*;

/** Exercises the production router with two DLC cases and a built-in port case on one station. */
public final class NewsInteractionRoutingTest {
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    static int accepted,opened,finished,cleaned;
    public static final class Event implements NewsEvent,PTSDNewsSiteAPI.SiteHandler {
        public void configure(NewsEventDefinition d){d.manualCompletion=true;}
        public void onAccepted(NewsEventContext c){accepted++;c.data().put("ready","true");}
        public void onFinished(NewsEventContext c){finished++;}
        public PTSDNewsSiteAPI.SiteResult materialize(PTSDNewsSiteAPI.SiteContext c){return null;}
        public void onExpired(PTSDNewsSiteAPI.SiteContext c){cleaned++;}
        public InteractionDialogPlugin createDialog(NewsEventContext c,SectorEntityToken target){
            check(c.incident.eventAccepted&&"true".equals(c.data().get("ready")),"Dialog factory ran before acceptance");
            return ControlledPatrolRouteTest.proxy(InteractionDialogPlugin.class,(n,a)->{if(n.equals("init"))opened++;return null;});
        }
    }
    public static final class Broken implements NewsEvent {
        static int calls;public void configure(NewsEventDefinition d){}
        public void onAccepted(NewsEventContext c){calls++;throw new IllegalStateException("expected");}
    }
    public static void main(String[] args)throws Exception {
        SectorAPI old=Global.getSector();SettingsAPI settings=Global.getSettings();
        try{
            Global.setSettings(ControlledPatrolRouteTest.proxy(SettingsAPI.class,(n,a)->{
                if(n.equals("getInstanceOfScript"))try{return Class.forName((String)a[0]).getDeclaredConstructor().newInstance();}catch(Exception e){throw new RuntimeException(e);}
                return null;
            }));
            Constructor<PTSDCrisisState> ctor=PTSDCrisisState.class.getDeclaredConstructor();ctor.setAccessible(true);
            PTSDCrisisState state=ctor.newInstance();state.progressInitialized=true;
            Map<String,Object> persistent=new HashMap<>();persistent.put(PTSDCrisisState.PERSISTENT_KEY,state);
            SectorEntityToken primary=NewsSceneMechanicsTest.mock(SectorEntityToken.class,n->n.equals("getId")?"home":null);
            MarketAPI market=NewsSceneMechanicsTest.mock(MarketAPI.class,n->n.equals("getPrimaryEntity")?primary:null);
            CampaignFleetAPI station=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,n->n.equals("getId")?"station":n.equals("getMarket")?market:n.equals("isStationMode")?true:null);
            final EveryFrameScript[] reopen={null};final boolean[] reopened={false},inBattle={true};final int[] removed={0};
            StarSystemAPI sys=ControlledPatrolRouteTest.proxy(StarSystemAPI.class,(n,a)->{if(n.equals("removeEntity"))removed[0]++;return n.equals("getId")?"system":null;});
            CampaignFleetAPI fighter=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,n->n.equals("getId")?"fighter":n.equals("getContainingLocation")?sys:
                    n.equals("getBattle")&&inBattle[0]?NewsSceneMechanicsTest.mock(BattleAPI.class,k->null):null);
            CampaignUIAPI ui=ControlledPatrolRouteTest.proxy(CampaignUIAPI.class,(n,a)->{
                if(n.equals("showInteractionDialog")){check(NewsEventLoader.dialogFor(station)==null&&PTSDNewsScene.dialogFor(station)==null,"Normal affairs intercepted again");reopened[0]=true;}return null;
            });
            Global.setSector(ControlledPatrolRouteTest.proxy(SectorAPI.class,(n,a)->{
                if(n.equals("getPersistentData"))return persistent;if(n.equals("getCampaignUI"))return ui;
                if(n.equals("getStarSystem"))return sys;if(n.equals("getEntityById"))return "fighter".equals(a[0])?fighter:primary;
                if(n.equals("addTransientScript"))reopen[0]=(EveryFrameScript)a[0];return null;
            }));
            for(int n=0;n<3;n++){
                PTSDCrisisState.CrisisIncident i=new PTSDCrisisState.CrisisIncident();i.id="task"+n;i.cardId=n==2?"R2-03":"DLC_"+n;i.headline=i.id;
                i.targetEntityId="home";i.targetSystemId="system";i.recordedByPlayer=true;i.investigationExpiresDay=30;
                i.siteMaterialized=true;
                if(n<2)i.eventLoc=Event.class.getName();else{i.scene=new PTSDNewsScene.State();i.scene.kind=PTSDNewsScene.Kind.PORT_RECORDS;i.scene.nodes.add("home");}
                state.incidents.add(i);
            }
            InteractionDialogPlugin menu=NewsEventLoader.dialogFor(station);check(menu instanceof PTSDNewsInteractionMenu,"Shared DLC/built-in menu absent");
            check(accepted==2&&opened==0,"Unselected dialog initialized or acceptance missing");
            List<Object> options=new ArrayList<>();final InteractionDialogPlugin[] switched={null};
            OptionPanelAPI panel=ControlledPatrolRouteTest.proxy(OptionPanelAPI.class,(n,a)->{if(n.equals("addOption"))options.add(a[1]);return null;});
            InteractionDialogAPI dialog=ControlledPatrolRouteTest.proxy(InteractionDialogAPI.class,(n,a)->{
                if(n.equals("setPlugin"))switched[0]=(InteractionDialogPlugin)a[0];
                return n.equals("getOptionPanel")?panel:n.equals("getTextPanel")?NewsSceneMechanicsTest.mock(TextPanelAPI.class,k->null):n.equals("getInteractionTarget")?station:null;
            });
            menu.init(dialog);check(options.containsAll(Arrays.asList("news:0","news:1","news:2","normal","leave")),"Cases missing");
            menu.optionSelected("", "news:1");check(opened==1&&switched[0]!=null,"Selected child not opened");
            NewsEventLoader.dialogFor(station);check(accepted==2,"Reentry repeated acceptance");
            menu.optionSelected("","normal");check(reopen[0]!=null,"Normal callback missing");reopen[0].advance(.1f);check(reopened[0],"Normal affairs unreachable");
            check(NewsEventLoader.dialogFor(station)!=null,"Normal bypass persisted across visits");
            PTSDCrisisState.CrisisIncident expired=state.incidents.get(0);expired.investigationResolved=true;expired.scenarioTrigger="PENDING_CHAIN";expired.investigationResult=PTSDTaskRules.InvestigationResult.EXPIRED;
            NewsEventLoader.advance(state,31);NewsEventLoader.advance(state,32);check(expired.eventFinished&&finished==1,"Expired conditional callback lost/repeated");
            PTSDCrisisState.CrisisIncident fighting=state.incidents.get(1);fighting.siteHandlerExpression=Event.class.getName();fighting.siteEntityIds.add("fighter");
            PTSDNewsSiteAPI.registerHandler(Event.class.getName(),(Event)NewsEventLoader.resolve(Event.class.getName()));
            PTSDNewsSiteManager.cleanup(state,fighting,true);check(removed[0]==0&&cleaned==0&&fighting.siteMaterialized,"Battle cleanup deleted fleet");
            inBattle[0]=false;PTSDNewsSiteManager.cleanup(state,fighting,true);PTSDNewsSiteManager.cleanup(state,fighting,true);
            check(removed[0]==1&&cleaned==1&&!fighting.siteMaterialized,"Deferred cleanup lost/duplicated");
            PTSDCrisisState.CrisisIncident broken=new PTSDCrisisState.CrisisIncident();broken.eventLoc=Broken.class.getName();broken.recordedByPlayer=true;
            NewsEventLoader.accepted(state,broken,1);NewsEventLoader.accepted(state,broken,2);check(Broken.calls==1&&broken.eventError!=null,"Failed callback retried");
            System.out.println("NewsInteractionRoutingTest: passed (DLC/built-in coexistence, lazy child init, normal bypass, acceptance once, expiry, battle cleanup)");
        }finally{Global.setSector(old);Global.setSettings(settings);}
    }
}
