package data.scripts.campaign.invasion.news.ft;

import data.scripts.campaign.invasion.*;

public class FT08Handler extends AbstractFTHandler {
    protected String id() { return "FT-08"; }
    @Override public String unavailableReason(PTSDCrisisState state) {
        PTSDLateState.FTChain permit = state.late.ftChains.get("FT-01");
        return permit != null && permit.permitObtained ? "可触发" : "尚未取得私改许可";
    }
    @Override public void start(PTSDCrisisState state, PTSDLateState.FTChain chain) {
        PTSDLateState.FTChain permit = state.late.ftChains.get("FT-01");
        if (permit == null) return;
        chain.systemId = permit.systemId;
        chain.marketId = permit.marketId;
        chain.targetId = permit.targetId;
        chain.node = "UNREST";
        finish(chain, "外围撤离资格被转卖；排队者与地方警备发生冲突。没有可由玩家直接结算的目标。");
        if (chain.marketId != null) {
            PTSDCrisisState.ColonyPanicData local = state.getColonyPanic(chain.systemId, chain.marketId);
            if (local != null) {
                local.eventPanic = Math.min(100f, local.eventPanic + 8f);
                local.lastChangedDay = PTSDCrisisState.getDay();
                local.lastSource = "FT-08 倒卖撤离资格";
            }
        }
    }
}
