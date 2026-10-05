import data.scripts.campaign.invasion.PTSDCrisisState;
import data.scripts.campaign.invasion.PTSDPlayerNewsText;
import data.scripts.campaign.invasion.PTSDTaskRules;

/** Old saved narrative strings must never override player-facing investigation state. */
public final class PlayerNewsBoundaryTest {
    public static void main(String[] args) {
        PTSDCrisisState.CrisisIncident item = new PTSDCrisisState.CrisisIncident();
        item.cardId = "F2-06";
        item.scenarioTrigger = "FORT_MEDIUM";
        item.effectSummary = "SECRET_BUDGET：准备点-10；战略资产-30；增派护航并牺牲其他航路覆盖";
        item.trueText = "SECRET_TRUTH：敌方工程需求增加";
        item.scenarioRemoteOutcome = "SECRET_FUTURE：物资入站后工程继续";
        item.scenarioOmegaResponse = "SECRET_RESPONSE";
        for (PTSDTaskRules.InvestigationResult result : PTSDTaskRules.InvestigationResult.values()) {
            item.investigationResult = result;
            String text = PTSDPlayerNewsText.summary(item);
            check(text);
            if (result == PTSDTaskRules.InvestigationResult.INCONCLUSIVE && !text.contains("不足"))
                throw new AssertionError("Inconclusive investigation acquired certainty");
            if (result == PTSDTaskRules.InvestigationResult.TARGET_LOST && !text.contains("无法确认"))
                throw new AssertionError("Lost contact acquired a future outcome");
        }
        item.playerOperationCompleted = true;
        check(PTSDPlayerNewsText.summary(item));
        if (!PTSDPlayerNewsText.summary(item).contains("新的现场观测"))
            throw new AssertionError("Completed action reveals unseen response");
        item.playerOperationCompleted = false;
        item.cardId = "R2-01";
        check(PTSDPlayerNewsText.summary(item));
        item.cardId = "E2-01";
        item.effectSummary = "现场证伪；未产生欧米伽资料或调兵";
        check(PTSDPlayerNewsText.summary(item));
        if (PTSDPlayerNewsText.summary(item).contains("欧米伽资料"))
            throw new AssertionError("Legacy E2 metagame explanation leaked");
        item.effectSummary = "已支付5000星币；打捞船获准离开";
        if (!PTSDPlayerNewsText.summary(item).equals(item.effectSummary))
            throw new AssertionError("Actual human payment receipt disappeared");
        System.out.println("PlayerNewsBoundaryTest: passed (old saved secrets, investigation states, operation, E2 receipt)");
    }
    private static void check(String text) {
        if (text.contains("SECRET_") || text.contains("战略资产") || text.contains("准备点") ||
                text.contains("牺牲其他航路") || text.contains("工程需求增加"))
            throw new AssertionError("Enemy-private narrative reached player: " + text);
    }
}
