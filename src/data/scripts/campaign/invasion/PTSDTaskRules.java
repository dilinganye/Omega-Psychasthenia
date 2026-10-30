package data.scripts.campaign.invasion;

/** Stable task semantics, independent of presentation text and campaign APIs. */
public final class PTSDTaskRules {
    public static final float NEWS_INVESTIGATION_DAYS = 60f;
    private PTSDTaskRules() { }
    public static int ordinaryNewsOutcome(float roll, boolean dormant) {
        return ordinaryNewsOutcome(roll, data.scripts.IIRT_Omega_ModPlugin.news_true_chance);
    }
    public static int ordinaryNewsOutcome(float roll, float trueChance) {
        return roll < Math.max(0f, Math.min(1f, trueChance)) ? 1 : 2;
    }

    public enum InvestigationResult {
        PENDING, CONFIRMED, DISPROVED, INCONCLUSIVE, TRACKER, EXPIRED, TARGET_LOST, LEGACY_UNVERIFIED
    }

    public static boolean successfulInvestigation(InvestigationResult result) {
        return result == InvestigationResult.CONFIRMED || result == InvestigationResult.DISPROVED;
    }

    public static float ordinaryDeadline(float now, float existing) {
        return Math.max(now + NEWS_INVESTIGATION_DAYS, existing);
    }

    public static float rescueWindow(float travelDays) {
        return Math.max(15f, Math.min(60f, Math.max(0f, travelDays) + 5f));
    }

    /** null is an unsupported choice; REMOTE is an explicitly registered remote action. */
    public static String lateOrder(String id, int choice) {
        if (id == null || choice < 1 || choice > 2) return null;
        String[] ids = {"MB-01", "MB-02", "MB-03", "MB-04",
                "WR-01", "WR-02", "WR-03", "WR-04", "WR-05", "WR-06", "WR-07",
                "WR-08", "WR-09", "WR-14", "WR-15", "WR-16",
                "WR-GS-01", "WR-GS-02", "WR-GS-03", "WR-GS-04", "WR-JE-01"};
        String[][] orders = {{"REMOTE", "REMOTE"}, {"OBSERVE", "REMOTE"},
                {"ESCORT", "DEFEND"}, {"HUNTER", "LEAVE"},
                {"DEFEND", "BATTLE"}, {"BATTLE", "DEFEND"}, {"REMOTE", "ESCORT"},
                {"REMOTE", "BATTLE"}, {"REMOTE", "ESCORT"}, {"DEFEND", "ESCORT"},
                {"BATTLE", "DEFEND"}, {"OBSERVE", "BATTLE"}, {"OBSERVE", "REMOTE"},
                {"ESCORT", "DEFEND"}, {"FORTRESS", "DEFEND"}, {"LEAVE", "ESCORT"},
                {"REMOTE", "REMOTE"}, {"REMOTE", "REMOTE"}, {"REMOTE", "OBSERVE"},
                {"REMOTE", "REMOTE"}, {"REMOTE", "REMOTE"}};
        for (int i = 0; i < ids.length; i++) if (ids[i].equals(id)) return orders[i][choice - 1];
        return null;
    }
}
