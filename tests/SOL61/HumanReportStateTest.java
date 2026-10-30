import data.scripts.campaign.invasion.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

/** Save migration and report attribution without a running campaign. */
public final class HumanReportStateTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        Constructor<PTSDCrisisState> constructor = PTSDCrisisState.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        PTSDCrisisState state = constructor.newInstance();
        PTSDCrisisState.CrisisIncident legacy = new PTSDCrisisState.CrisisIncident();
        legacy.investigationResult = null; // Field absent in an old save.
        legacy.investigationResolved = true; legacy.investigationReal = false;
        state.incidents.add(legacy);
        Method repair = PTSDCrisisState.class.getDeclaredMethod("repairCollections");
        repair.setAccessible(true); repair.invoke(state);
        check(legacy.investigationResult == PTSDTaskRules.InvestigationResult.LEGACY_UNVERIFIED,
                "Ambiguous old failures cannot become proof");
        check(!state.jePrivateNetworkQualified, "Old completion totals cannot unlock private network");
        check(PTSDHumanWarReports.latest(state,"unknown") == null, "No fallback to enemy world truth");
        PTSDCrisisState.HumanReport first = new PTSDCrisisState.HumanReport();
        first.id="first"; first.systemId="known"; first.day=2f; first.strength=30f;
        state.humanWarReports.put(first.id,first);
        PTSDCrisisState.HumanReport second = new PTSDCrisisState.HumanReport();
        second.id="second"; second.systemId="known"; second.day=5f;
        state.humanWarReports.put(second.id,second);
        check(PTSDHumanWarReports.latest(state,"known") == second,"Most recent sourced report wins");
        check(first.strength == 30f,"Old report is retained as recorded");
        state.late.coalitionMembers.add("member");
        PTSDLateState.Reserve reserve = new PTSDLateState.Reserve();
        reserve.factionId="member"; reserve.strength=40f; state.late.reserves.add(reserve);
        PTSDLateState.Reserve outsider = new PTSDLateState.Reserve();
        outsider.factionId="outsider"; outsider.strength=90f; state.late.reserves.add(outsider);
        Map<String,Float> troops=PTSDCoalitionCharts.values(state,0);
        check(troops.size()==1 && troops.get("member")==40f,"Nonmembers excluded from troops pie");
        PTSDLateState.Contribution old = new PTSDLateState.Contribution();
        old.factionAtTime="member"; old.actorId="player"; old.amount=25f; old.day=-100f;
        state.late.contributions.add(old);
        check(PTSDCoalitionCharts.values(state,2).get("member")==25f,"Locked affiliation credited once");
        check(PTSDCoalitionCharts.values(state,3).get("member")==0f,"Old contribution absent from recent pie");
        check(!PTSDCoalitionCharts.values(state,2).containsKey("player"),"Personal record is not a second faction slice");
        state.phase=PTSDCrisisState.Phase.WAR;
        PTSDLateState.Front front=new PTSDLateState.Front();
        front.id="front"; front.systemId="known"; front.marketId="colony-a";
        state.late.fronts.put(front.id,front);
        PTSDLateState.EventCard card=new PTSDLateState.EventCard();
        card.orderKind="DEFEND"; card.systemId="known"; card.targetFrontId=front.id;
        PTSDCrisisState.StrategicEvent attack=new PTSDCrisisState.StrategicEvent();
        attack.type=PTSDCrisisState.EventType.ATTACK; attack.targetMarketId="colony-b";
        Method defense=PTSDLateEventDirector.class.getDeclaredMethod("matchesDefenseVictory",PTSDCrisisState.class,
                PTSDLateState.EventCard.class,String.class,PTSDCrisisState.StrategicEvent.class);
        defense.setAccessible(true);
        check(!(Boolean)defense.invoke(null,state,card,"known",attack),"Same system's other colony cannot satisfy defense");
        attack.targetMarketId="colony-a";
        check((Boolean)defense.invoke(null,state,card,"known",attack),"Target colony's attack is relevant defense");
        front.resolved=true;
        check(!(Boolean)defense.invoke(null,state,card,"known",attack),"Finished front cannot earn defense credit");
        System.out.println("HumanReportStateTest: passed");
    }
}
