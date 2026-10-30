package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;

/** Exercises persisted scene decisions and actual investigation callbacks without an OpenGL context. */
public final class NewsSceneMechanicsTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    @SuppressWarnings("unchecked") static <T>T mock(Class<T> type,Function<String,Object> answers){
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},(p,m,a)->{
            Object v=answers.apply(m.getName());if(v!=null)return v;
            Class<?> r=m.getReturnType();if(r==boolean.class)return false;if(r==float.class)return 0f;
            if(r==int.class)return 0;if(r==long.class)return 0L;if(r==double.class)return 0d;
            if(List.class.isAssignableFrom(r))return Collections.emptyList();return null;
        });
    }
    public static void main(String[] args)throws Exception{
        SectorAPI previous=Global.getSector();SettingsAPI old=Global.getSettings();
        try{
            Map<String,Object> persistent=new HashMap<>();
            CampaignClockAPI clock=mock(CampaignClockAPI.class,n->null);
            Global.setSettings(mock(SettingsAPI.class,n->null));
            Global.setSector(mock(SectorAPI.class,n->n.equals("getPersistentData")?persistent:n.equals("getClock")?clock:null));
            Constructor<PTSDCrisisState> ctor=PTSDCrisisState.class.getDeclaredConstructor();ctor.setAccessible(true);
            PTSDCrisisState state=ctor.newInstance();state.progressInitialized=true;persistent.put(PTSDCrisisState.PERSISTENT_KEY,state);
            check(PTSDNewsScene.Kind.values().length==23,"Registry lost a template");
            for(PTSDNewsScene.Kind k:PTSDNewsScene.Kind.values()){
                check(PTSDNewsScene.validTemplates(k.name()),"Unregistered "+k);
                PTSDNewsScene.State s=new PTSDNewsScene.State();s.kind=k;
                for(int b=0;b<3;b++){s.branch=b;check(!PTSDNewsScene.intro(s).isEmpty()&&!PTSDNewsScene.findings(s).isEmpty(),"Missing scene text");}
            }
            check(!PTSDNewsScene.validTemplates("ROUTE_REVERSAL|TYPO"),"Unknown template silently accepted");
            StarSystemAPI empty=mock(StarSystemAPI.class,n->n.equals("getId")?"empty":null);
            check(PTSDNewsScene.unavailable(PTSDNewsScene.Kind.RELAY_PARASITE,state,empty)==null,"Missing utility installation fallback should remain eligible");
            check(PTSDNewsScene.unavailable(PTSDNewsScene.Kind.FORT_DECOY_TRANSIT,state,empty)!=null,"Missing jump pair accepted");
            check(PTSDNewsScene.unavailable(PTSDNewsScene.Kind.FORT_GRAVITY_SURVEY,state,empty)!=null,"Missing black hole accepted");
            PTSDCrisisState.CrisisIncident i=new PTSDCrisisState.CrisisIncident();i.id="test-gravity";i.scene=new PTSDNewsScene.State();
            i.scene.kind=PTSDNewsScene.Kind.FORT_GRAVITY_SURVEY;i.scene.branch=0;i.siteMaterialized=true;
            i.scene.nodes.addAll(Arrays.asList("outer","middle","inner"));
            PTSDNewsScene.sample(state,i,mock(SectorEntityToken.class,n->n.equals("getId")?"outer":null));
            PTSDNewsScene.sample(state,i,mock(SectorEntityToken.class,n->n.equals("getId")?"outer":null));
            check(i.scene.samples==1&&!i.scene.resolved,"Repeated sample counted twice");
            PTSDNewsScene.sample(state,i,mock(SectorEntityToken.class,n->n.equals("getId")?"middle":null));
            check(i.scene.calibrationError&&!i.scene.resolved,"Two-point proof did not allow optional deeper recovery");
            PTSDNewsScene.sample(state,i,mock(SectorEntityToken.class,n->n.equals("getId")?"inner":null));
            check(i.scene.resolved&&i.investigationResult==PTSDTaskRules.InvestigationResult.DISPROVED,"Calibration error not disproved");
            check(state.jeParticipationIds.size()==1,"Samples duplicated Je participation");
            PTSDNewsScene.queueResponse(i,10);PTSDNewsScene.queueResponse(i,20);
            check(Math.abs(i.scene.responseDay-10.35f)<.001f,"Response duplicated or postponed");
            i.scene.ghostRolled=true;i.scene.ghostFleet=true;i.scene.secondsLeft=8.25f;i.scene.towingRecord=true;
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();new ObjectOutputStream(bytes).writeObject(i.scene);
            PTSDNewsScene.State restored=(PTSDNewsScene.State)new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();
            check(restored.ghostRolled&&restored.ghostFleet&&restored.towingRecord&&restored.secondsLeft==8.25f&&restored.samples==3&&restored.calibrationError,"Reload lost branch/timer/rescue state");
            for(long seed=-5000;seed<5000;seed++){
                float f=PTSDStrategicSurvey.estimateMultiplier(seed);check(f>=.4f&&f<=1.8f,"WAR estimate out of range");
                check(f==PTSDStrategicSurvey.estimateMultiplier(seed),"WAR estimate rerolled");
            }
            PTSDCrisisState.SystemData data=state.getSystemData("empty");
            PTSDCrisisState.EvidenceRecord e=new PTSDCrisisState.EvidenceRecord();e.sourceType="UNINHABITED_BASELINE";e.expiresDay=100;
            data.evidence.add(e);check(PTSDReconReportDirector.isSurveyed(data,20),"Phase-completed system remains a blind spot");
            e.sourceType="WAR_OPENING_ESTIMATE";e.remoteEstimate=true;
            check(!PTSDReconReportDirector.isSurveyed(data,20),"Coarse estimate treated as real survey");
            System.out.println("NewsSceneMechanicsTest: passed (23 templates, prerequisites, sample dedup, error verdict, Je dedup, response dedup, persistence, estimate bounds, coverage)");
        }finally{Global.setSector(previous);Global.setSettings(old);}
    }
}
