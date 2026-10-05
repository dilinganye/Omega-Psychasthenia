package data.scripts.campaign.invasion;

/** Player-facing records never use the simulation's enemy response or cached effect ledger. */
public final class PTSDPlayerNewsText {
    private PTSDPlayerNewsText() { }

    private static boolean revised(PTSDCrisisState.CrisisIncident item) {
        return item != null && item.cardId != null && (item.cardId.startsWith("F2-") ||
                item.cardId.startsWith("E2-") || item.cardId.startsWith("MB-") || item.cardId.startsWith("WR-"));
    }

    private static PTSDCrisisIncidentManager.ScriptedTemplate revisedTemplate(PTSDCrisisState.CrisisIncident item) {
        return revised(item) ?
                PTSDCrisisIncidentManager.getTemplate(item.cardId) : null;
    }

    public static String source(PTSDCrisisState.CrisisIncident item) {
        PTSDCrisisIncidentManager.ScriptedTemplate template = revisedTemplate(item);
        return template != null ? template.source : item.sourceLabel;
    }

    public static String headline(PTSDCrisisState.CrisisIncident item) {
        PTSDCrisisIncidentManager.ScriptedTemplate template = revisedTemplate(item);
        return template != null ? template.title : item.headline;
    }

    public static String report(PTSDCrisisState.CrisisIncident item) {
        PTSDCrisisIncidentManager.ScriptedTemplate template = revisedTemplate(item);
        return template != null ? template.report : revised(item) ?
                "观测摘录暂不可用；尚不能据此判断施工进度或守备调动。" : item.publicText;
    }

    public static String action(PTSDCrisisState.CrisisIncident item) {
        PTSDCrisisIncidentManager.ScriptedTemplate template = revisedTemplate(item);
        return template != null ? template.action : revised(item) ?
                "复核现场记录" : item.scenarioAction;
    }

    public static String summary(PTSDCrisisState.CrisisIncident item) {
        if (item.cardId != null && item.cardId.startsWith("E2-")) {
            String cached = item.effectSummary;
            if (cached != null && !cached.contains("欧米伽资料") && !cached.contains("精神创伤有关") &&
                    !cached.contains("人类地方事件") && !cached.contains("现存船只和合同状态")) return cached;
            return "地方报道已归档；当事人后续活动仍需新的记录核查。";
        }
        if (item.scenarioTrigger == null)
            return item.effectSummary == null ? "现场记录已归档。" : item.effectSummary;
        // Rebuild at display time: old saves may contain already-concatenated enemy budgets.
        if (item.playerOperationCompleted)
            return "行动记录已归档。后续施工、运输与警戒变化须由新的现场观测确认。";
        if (item.investigationResult == PTSDTaskRules.InvestigationResult.DISPROVED)
            return "取得了足以证伪报道的记录，调查已归档。";
        if (item.investigationResult == PTSDTaskRules.InvestigationResult.INCONCLUSIVE)
            return "现有记录不足以确认或证伪报道，调查已归档。";
        if (item.investigationResult == PTSDTaskRules.InvestigationResult.TARGET_LOST)
            return "已失去现场目标，无法确认此后的去向或活动。";
        if (item.investigationResult == PTSDTaskRules.InvestigationResult.TRACKER)
            return "现场发现了跟踪信号，其来源与目的尚未确定。";
        if (item.scenarioResolved || item.investigationResult == PTSDTaskRules.InvestigationResult.EXPIRED)
            return "本次核查窗口已结束；此后的情况尚无可核验记录。";
        if (item.playerOperationAccepted)
            return "行动已下达，尚待完成回执；现场行动离开目标后需重新累计驻留时间。";
        if (item.investigationResult == PTSDTaskRules.InvestigationResult.CONFIRMED)
            return "现场记录与报道所述异常相符；后续行动仍需另行下达。";
        return "尚待现场核查；报道本身不能证明敌方计划。";
    }

    public static String investigativeNote(PTSDCrisisState.CrisisIncident item) {
        if (item.scenarioTrigger == null && !revised(item)) return item.trueText;
        if (item.investigationResult != PTSDTaskRules.InvestigationResult.CONFIRMED)
            return summary(item);
        PTSDCrisisIncidentManager.ScriptedTemplate template = revisedTemplate(item);
        return template == null ? "调查记录支持报道中描述的异常。对方的任务安排与下一步行动尚无法确认。" :
                template.intercept;
    }
}
