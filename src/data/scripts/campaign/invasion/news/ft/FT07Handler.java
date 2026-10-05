package data.scripts.campaign.invasion.news.ft;

import data.scripts.campaign.invasion.*;

public final class FT07Handler extends AbstractFTHandler {
    protected String id() { return "FT-07"; }
    @Override public void start(PTSDCrisisState state, PTSDLateState.FTChain chain) {
        super.start(state, chain);
        if (chain.node != null) finish(chain,
                "保险商赔付了船体和货物；失联航路不被认定为财产。港口和船员家属仍各持一份相反通知。");
    }
}
