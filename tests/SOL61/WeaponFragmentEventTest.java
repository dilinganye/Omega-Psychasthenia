package data.scripts.campaign.invasion.news.defaults;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import data.scripts.campaign.invasion.*;
import data.scripts.campaign.invasion.news.*;
import org.lwjgl.util.vector.Vector2f;
import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.util.function.BiFunction;

/** P-10 world-state and dialogue regression, including actual handoff tick and award callbacks. */
public final class WeaponFragmentEventTest {
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    @SuppressWarnings("unchecked") static <T>T mock(Class<T> type,BiFunction<String,Object[],Object> f){
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},(p,m,a)->{
            Object v=f.apply(m.getName(),a);if(v!=null)return v;
            if(m.getReturnType()==boolean.class)return false;if(m.getReturnType()==float.class)return 0f;
            if(m.getReturnType()==int.class)return 0;if(m.getReturnType()==long.class)return 0L;
            if(List.class.isAssignableFrom(m.getReturnType()))return Collections.emptyList();return null;
        });
    }
    static PTSDCrisisState.CrisisIncident incident(String id){
        PTSDCrisisState.CrisisIncident i=new PTSDCrisisState.CrisisIncident();i.id=id;i.cardId="P-10";i.targetSystemId="system";i.targetEntityId="anchor";
        i.recordedByPlayer=true;i.investigable=true;i.investigationOutcome=1;i.eventManualCompletion=true;i.siteMaterialized=true;
        i.investigationExpiresDay=40;P10Event.set(i,"anchor","anchor");P10Event.set(i,"exit","exit");
        P10Event.set(i,"collector","collector");P10Event.set(i,"receiver","receiver");P10Event.set(i,"cargoRole","collector");
        P10Event.set(i,"receiverCreated",true);P10Event.set(i,"collectorEnd",100);P10Event.set(i,"receiverEnd",100);return i;
    }
    public static void main(String[] args)throws Exception {
        SectorAPI old=Global.getSector();SettingsAPI settings=Global.getSettings();
        try{
            Global.setSettings(mock(SettingsAPI.class,(n,a)->null));
            Constructor<PTSDCrisisState> ctor=PTSDCrisisState.class.getDeclaredConstructor();ctor.setAccessible(true);PTSDCrisisState state=ctor.newInstance();state.progressInitialized=true;
            Map<String,Object> persistent=new HashMap<>();persistent.put(PTSDCrisisState.PERSISTENT_KEY,state);
            StarSystemAPI system=mock(StarSystemAPI.class,(n,a)->n.equals("getId")?"system":null);
            Map<String,SectorEntityToken> entities=new HashMap<>();
            SectorEntityToken anchor=mock(SectorEntityToken.class,(n,a)->n.equals("getId")?"anchor":n.equals("getContainingLocation")||n.equals("getStarSystem")?system:n.equals("getLocation")?new Vector2f(6000,0):null);
            SectorEntityToken exit=mock(SectorEntityToken.class,(n,a)->n.equals("getId")?"exit":n.equals("getContainingLocation")||n.equals("getStarSystem")?system:n.equals("getLocation")?new Vector2f():null);
            final boolean[] detected={false},fighting={false};final int[] reports={0};
            CampaignFleetAPI collector=mock(CampaignFleetAPI.class,(n,a)->n.equals("getId")?"collector":n.equals("isAlive")||n.equals("isVisibleToPlayerFleet")?true:
                    n.equals("getFleetPoints")?Integer.valueOf(12):n.equals("getContainingLocation")||n.equals("getStarSystem")?system:n.equals("getLocation")?new Vector2f():
                    n.equals("getBattle")&&fighting[0]?mock(BattleAPI.class,(x,y)->null):null);
            CampaignFleetAPI receiver=mock(CampaignFleetAPI.class,(n,a)->n.equals("getId")?"receiver":n.equals("isAlive")||n.equals("isVisibleToPlayerFleet")?true:
                    n.equals("getFleetPoints")?Integer.valueOf(12):n.equals("getContainingLocation")||n.equals("getStarSystem")?system:n.equals("getLocation")?new Vector2f(100,0):null);
            CargoAPI cargo=mock(CargoAPI.class,(n,a)->{if(n.equals("addSpecial"))reports[0]+=((Number)a[1]).intValue();return null;});
            CampaignFleetAPI player=mock(CampaignFleetAPI.class,(n,a)->n.equals("getCargo")?cargo:n.equals("getContainingLocation")?system:
                    n.equals("getLocation")?new Vector2f(3000,0):n.equals("isVisibleToSensorsOf")?detected[0]:null);
            entities.put("collector",collector);entities.put("receiver",receiver);entities.put("anchor",anchor);entities.put("exit",exit);
            Global.setSector(mock(SectorAPI.class,(n,a)->n.equals("getEntityById")?entities.get(a[0]):n.equals("getStarSystem")?system:
                    n.equals("getPlayerFleet")?player:n.equals("getPersistentData")?persistent:null));
            check(!P10Event.readyToCollect(1,.8f,false,false,false),"Remote collection");
            check(!P10Event.readyToCollect(1,.8f,true,true,false),"Collection during combat");
            check(P10Event.readyToCollect(1,.8f,true,false,false),"Valid arrival blocked");
            check(!P10Event.canTail(true,true,3000)&&!P10Event.canTail(false,false,3000)&&P10Event.canTail(true,false,3000),"Tail ignores visibility/detection");
            P10Event event=new P10Event();
            PTSDCrisisState.CrisisIncident tail=incident("tail");P10Event.set(tail,"mode","CARRYING");P10Event.set(tail,"tail",.2);P10Event.set(tail,"lastDay",9.9);
            P10Event.tick(state,tail,10);check(!tail.investigationResolved&&P10Event.num(tail,"handoff")>10,"Handoff completed without waiting");
            P10Event.tick(state,tail,10.3f);check(tail.investigationResolved&&"TAIL".equals(P10Event.get(tail,"route"))&&reports[0]==3,"Observed handoff lost");
            check(!P10Event.yes(tail,"queued")&&tail.effectSummary.contains("连续航迹"),"Tail fabricated theft/pursuit or lost narrative");
            P10Event.finish(state,tail,"RACE");check(reports[0]==3&&"TAIL".equals(P10Event.get(tail,"route")),"Repeat finish changed reward/route");
            PTSDCrisisState.CrisisIncident exposed=incident("exposed");P10Event.set(exposed,"mode","CARRYING");P10Event.set(exposed,"tail",.2);detected[0]=true;
            P10Event.tick(state,exposed,10);P10Event.tick(state,exposed,10.3f);
            check(!exposed.investigationResolved&&"TRANSFERRED".equals(P10Event.get(exposed,"mode")),"Detected player awarded covert route");detected[0]=false;
            InteractionDialogAPI dialog=mock(InteractionDialogAPI.class,(n,a)->n.equals("getOptionPanel")?mock(OptionPanelAPI.class,(x,y)->null):n.equals("getTextPanel")?mock(TextPanelAPI.class,(x,y)->null):null);
            PTSDCrisisState.CrisisIncident race=incident("race");P10Event.set(race,"mode","APPROACH");
            InteractionDialogPlugin plugin=event.createDialog(new NewsEventContext(state,race,1),anchor);plugin.init(dialog);plugin.optionSelected("","recover");
            check(race.investigationResolved&&"RACE".equals(P10Event.get(race,"route"))&&P10Event.yes(race,"queued")&&reports[0]==6,"Race route missing");
            check(!P10Event.queuePursuit(race,2),"Second pursuit queued");plugin.optionSelected("","recover");check(reports[0]==6,"Duplicate report");
            PTSDCrisisState.CrisisIncident intercept=incident("intercept");P10Event.set(intercept,"mode","CARRYING");
            check(!P10Event.recoverable(intercept,anchor),"Loaded sample still in original wreck");
            P10Event.set(intercept,"mode","DROPPED");P10Event.set(intercept,"drop","anchor");
            plugin=event.createDialog(new NewsEventContext(state,intercept,1),anchor);plugin.init(dialog);plugin.optionSelected("","recover");
            check("INTERCEPT".equals(P10Event.get(intercept,"route"))&&reports[0]==9&&P10Event.yes(intercept,"queued"),"Intercept route missing");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();new ObjectOutputStream(bytes).writeObject(intercept);
            intercept=(PTSDCrisisState.CrisisIncident)new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();
            check(!P10Event.queuePursuit(intercept,3)&&intercept.investigationReportsAwarded==3,"Reload reset pursuit/reward");
            PTSDCrisisState.CrisisIncident falseLead=incident("false");falseLead.investigationOutcome=2;falseLead.siteMaterialized=false;
            plugin=event.createDialog(new NewsEventContext(state,falseLead,1),anchor);plugin.init(dialog);plugin.optionSelected("","disprove");
            check(falseLead.investigationResult==PTSDTaskRules.InvestigationResult.DISPROVED&&reports[0]==12&&!P10Event.yes(falseLead,"queued"),"False report not disproved cleanly");
            System.out.println("WeaponFragmentEventTest: passed (arrival gates, actual handoff, detection, three routes, false report, one pursuit, rewards and persistence)");
        }finally{Global.setSector(old);Global.setSettings(settings);}
    }
}
