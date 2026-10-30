package data.scripts.campaign.invasion.news;

import data.scripts.campaign.invasion.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** 每次回调的只读引用容器。实际修改 incident 的持久字段，不保存 Context 本身。 */
public final class NewsEventContext {
    public final PTSDCrisisState state;
    public final PTSDCrisisState.CrisisIncident incident;
    public final float day;
    public NewsEventContext(PTSDCrisisState state, PTSDCrisisState.CrisisIncident incident, float day) {
        this.state=state;this.incident=incident;this.day=day;
    }
    /** 字符串键值随 incident 保存；建议使用 DLC 自己的前缀，值中保存实体 ID。 */
    public Map<String,String> data() {
        if(incident.eventData==null)incident.eventData=new LinkedHashMap<String,String>();
        return incident.eventData;
    }
    /** 已实际获得一项证据后调用；仅进入港口、阅读新闻或绑定目标不算取得证据。 */
    public void recordEvidence() { incident.eventEvidenceCollected=true; }
    /** 绑定既有设施供 createDialog 使用；加黄色任务标记，不取得该设施的删除权。 */
    public void bind(com.fs.starfarer.api.campaign.SectorEntityToken entity) {
        if(entity==null||entity.getId()==null)throw new IllegalArgumentException("绑定目标必须有持久 ID");
        if(incident.eventInteractionIds==null)incident.eventInteractionIds=new java.util.ArrayList<String>();
        if(!incident.eventInteractionIds.contains(entity.getId()))incident.eventInteractionIds.add(entity.getId());
        com.fs.starfarer.api.util.Misc.makeImportant(entity,"PTSD_event_"+incident.id);
    }
    /**
     * 独立现场完成调查的统一出口。只有 CONFIRMED / DISPROVED 领取完整报告，
     * INCONCLUSIVE 需要 evidence=true 才有部分报告；超时、无证据放弃不奖励。
     * 结果与奖励幂等，重复对话回调不会复制报告。
     */
    public void finish(PTSDTaskRules.InvestigationResult result, boolean evidence) {
        if(result==null||result==PTSDTaskRules.InvestigationResult.PENDING)
            throw new IllegalArgumentException("finish 需要终态调查结果");
        if(incident.investigationResolved)return;
        if(PTSDTaskRules.successfulInvestigation(result)&&!evidence&&!PTSDInvestigationRewards.hasEvidence(incident))
            throw new IllegalArgumentException("确认或证伪必须有已取得的现场证据");
        incident.eventEvidenceCollected|=evidence;
        incident.investigationResult=result;
        incident.investigationResolved=true;
        // 显式接管结算的独立条件事件须在实际行动完成后才调用 finish。
        if(incident.eventManualCompletion&&incident.scenarioTrigger!=null)incident.scenarioResolved=true;
        incident.investigationReal=result==PTSDTaskRules.InvestigationResult.CONFIRMED;
        if(result==PTSDTaskRules.InvestigationResult.CONFIRMED)
            PTSDNewsSiteManager.confirm(state,incident);
        else if(result==PTSDTaskRules.InvestigationResult.DISPROVED)
            PTSDCrisisIncidentManager.disproveIncident(incident);
        PTSDInvestigationRewards.complete(incident);
    }
}
