import data.scripts.campaign.invasion.PTSDTaskRules;

/** Run with the temporary compilation directory; no game instance or save required. */
public final class TaskRulesTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        for (PTSDTaskRules.InvestigationResult result : PTSDTaskRules.InvestigationResult.values()) {
            boolean valid = result == PTSDTaskRules.InvestigationResult.CONFIRMED ||
                    result == PTSDTaskRules.InvestigationResult.DISPROVED;
            check(PTSDTaskRules.successfulInvestigation(result) == valid, "Unexpected credit: " + result);
        }
        check(!PTSDTaskRules.successfulInvestigation(null), "Missing legacy result cannot earn credit");
        check(PTSDTaskRules.ordinaryDeadline(100f, 105f) == 160f, "Short remaining lead needs 60 days");
        check(PTSDTaskRules.ordinaryDeadline(100f, 190f) == 190f, "Longer deadline must survive");
        check(PTSDTaskRules.ordinaryDeadline(100f, 0f) == 160f, "Uninitialized deadline");
        check(PTSDTaskRules.rescueWindow(0f) == 15f, "Nearby rescue minimum");
        check(PTSDTaskRules.rescueWindow(30f) == 35f, "Travel plus five days");
        check(PTSDTaskRules.rescueWindow(90f) == 60f, "Distant rescue maximum");
        check("DEFEND".equals(PTSDTaskRules.lateOrder("WR-01", 1)), "Front task classification");
        check("REMOTE".equals(PTSDTaskRules.lateOrder("WR-GS-03", 1)), "Supply order classification");
        check("OBSERVE".equals(PTSDTaskRules.lateOrder("WR-GS-03", 2)), "Forgery investigation classification");
        check("LEAVE".equals(PTSDTaskRules.lateOrder("WR-16", 1)), "Special evacuation classification");
        check(PTSDTaskRules.lateOrder("FT-10", 2) == null, "Protected chain must use its own director");
        check(PTSDTaskRules.lateOrder("WR-unknown", 1) == null, "Unknown task must fail closed");
        check(PTSDTaskRules.lateOrder("WR-01", 3) == null, "Invalid choice must fail closed");
        System.out.println("TaskRulesTest: passed");
    }
}
