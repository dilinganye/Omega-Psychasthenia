import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.*;
import data.scripts.campaign.invasion.*;
import java.lang.reflect.*;
import java.util.*;

/** Resource affordability, old-system IDs, hidden-market lookup and actual support accounting. */
public final class LateRuntimeRegressionTest {
    interface Calls { Object call(String name); }
    @SuppressWarnings("unchecked") static <T>T mock(Class<T> type,Calls calls){
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(self,method,args)->{
            if(method.getName().equals("equals"))return self==args[0];
            if(method.getName().equals("hashCode"))return System.identityHashCode(self);
            Object value=calls.call(method.getName());if(value!=null||!method.getReturnType().isPrimitive())return value;
            if(method.getReturnType()==boolean.class)return false;
            if(method.getReturnType()==float.class)return 0f;
            if(method.getReturnType()==int.class)return 0;
            if(method.getReturnType()==long.class)return 0L;
            return null;
        });
    }
    static void check(boolean condition,String text){if(!condition)throw new AssertionError(text);}
    public static void main(String[]args)throws Exception{
        Constructor<PTSDCrisisState> constructor=PTSDCrisisState.class.getDeclaredConstructor();constructor.setAccessible(true);
        PTSDCrisisState state=constructor.newInstance();
        int real=0,tracked=0;
        for(int i=0;i<10000;i++){
            float roll=(i+.5f)/10000f;
            if(PTSDTaskRules.ordinaryNewsOutcome(roll,false)==1)real++;
            if(PTSDTaskRules.ordinaryNewsOutcome(roll,false)==3)tracked++;
            check(PTSDTaskRules.ordinaryNewsOutcome(roll,true)!=3,"Dormant tracking branch enabled");
        }
        check(real==9000&&tracked==0,"90% truth partition changed");
        state.late.strategicReserve=240f;state.late.warPoints=20f;
        check(PTSDLateWarDirector.affordableAttackStrength(state)==160f,"Attack ignored preparation ceiling");
        state.late.strategicReserve=65f;
        check(PTSDLateWarDirector.affordableAttackStrength(state)==65f,"Attack ignored actual assets");
        check(PTSDLateWarDirector.reserveAttack(state,"funded",65f),"Affordable small deployment rejected");
        check(state.late.strategicReserve==0f && state.late.warPoints==10f,"Deployment budget not debited exactly once");
        check(!PTSDLateWarDirector.reserveAttack(state,"funded",65f),"Deployment charged twice");
        state.late.warPoints=9f;
        check(PTSDLateWarDirector.affordableAttackStrength(state)==0f,"Unfunded attack became affordable");
        PTSDLateState.Resolution vote=new PTSDLateState.Resolution();
        vote.seatSnapshot.put("a",50f);vote.seatSnapshot.put("b",50f);
        vote.ballots.put("a","YES");vote.ballots.put("b","PENDING");
        check(!PTSDCoalitionVotes.passed(vote),"Tie or missing ballot treated as majority");
        vote.ballots.put("b","YES");check(PTSDCoalitionVotes.passed(vote),"Majority rejected");
        vote.ballots.put("a","NO");check(PTSDCoalitionVotes.weight(vote,"NO")==50f,"Per-member weight lost");
        PTSDLateState.Front front=new PTSDLateState.Front();front.id="front";front.systemId="destination";front.defense=20f;
        state.late.fronts.put(front.id,front);
        PTSDLateState.Reserve reserve=new PTSDLateState.Reserve();reserve.targetSystemId=front.systemId;reserve.strength=60f;
        Method support=PTSDReserveDirector.class.getDeclaredMethod("support",PTSDCrisisState.class,PTSDLateState.Reserve.class);support.setAccessible(true);
        support.invoke(null,state,reserve);support.invoke(null,state,reserve);
        check(front.defense==80f,"Repeated arrival duplicated defense");
        reserve.strength=35f;support.invoke(null,state,reserve);check(front.defense==55f,"Loss did not reduce support");
        PTSDReserveDirector.withdrawSupport(state,reserve);check(front.defense==20f,"Withdraw did not restore baseline");
        Object blackSpec=mock(PlanetAPI.class.getMethod("getSpec").getReturnType(),name->name.equals("isBlackHole")?true:null);
        PlanetAPI companion=mock(PlanetAPI.class,name->name.equals("isStar")?true:name.equals("getSpec")?blackSpec:null);
        StarSystemAPI binary=mock(StarSystemAPI.class,name->name.equals("getPlanets")?Collections.singletonList(companion):null);
        Method blackHole=PTSDFortressDirector.class.getDeclaredMethod("blackHole",StarSystemAPI.class);blackHole.setAccessible(true);
        check(blackHole.invoke(null,binary)==companion,"Companion black hole excluded from fortress sites");
        SectorAPI previous=Global.getSector();
        try{
            final MarketAPI[] attached={null};final int[] scans={0};
            StarSystemAPI system=mock(StarSystemAPI.class,name->name.equals("getId")?"persisted-name":null);
            SectorEntityToken planet=mock(SectorEntityToken.class,name->name.equals("getMarket")?attached[0]:name.equals("getContainingLocation")?system:null);
            MarketAPI market=mock(MarketAPI.class,name->name.equals("getId")?"hidden":name.equals("getPrimaryEntity")?planet:null);
            attached[0]=market;
            LocationAPI location=mock(LocationAPI.class,name->{if(name.equals("getAllEntities")){scans[0]++;return Collections.singletonList(planet);}return null;});
            EconomyAPI economy=mock(EconomyAPI.class,name->null);
            Global.setSector(mock(SectorAPI.class,name->name.equals("getEconomy")?economy:name.equals("getStarSystems")?
                    Collections.singletonList(system):name.equals("getAllLocations")?Collections.singletonList(location):null));
            check(state.resolveSystem("persisted-name")==system,"Unique-ID lookup hid persisted system ID");
            for(int i=0;i<1000;i++)check(state.resolveMarket("hidden")==market,"Hidden market lost");
            check(scans[0]==1,"Hidden market still scans whole sector per read");
            attached[0]=null;check(state.resolveMarket("hidden")==null,"Detached market served from cache");
        }finally{Global.setSector(previous);}
        System.out.println("LateRuntimeRegressionTest: passed (90% truth, finite budgets, ballots, support once, hidden-market cache, legacy system ID)");
    }
}
