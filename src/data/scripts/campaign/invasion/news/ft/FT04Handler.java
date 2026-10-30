package data.scripts.campaign.invasion.news.ft;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.invasion.*;

/** No public news. Direct interaction skips the ambient warning sequence. */
public class FT04Handler extends AbstractFTHandler {
    protected String id() { return "FT-04"; }
    @Override public String unavailableReason(PTSDCrisisState state) {
        return state.baseSystemId == null || state.baseMarketId == null ?
                "母星实体尚未建立" : "可触发";
    }
    @Override public void start(PTSDCrisisState state, PTSDLateState.FTChain chain) {
        chain.systemId = state.baseSystemId;
        chain.marketId = state.baseMarketId;
        com.fs.starfarer.api.campaign.econ.MarketAPI market = state.resolveMarket(state.baseMarketId);
        chain.targetId = market == null || market.getPrimaryEntity() == null ? null :
                market.getPrimaryEntity().getId();
        chain.node = "WATCHING";
        chain.outcome = "";
        chain.expiresDay = Float.MAX_VALUE;
    }
    @Override public void advanceChain(PTSDCrisisState state, PTSDLateState.FTChain chain, float day) {
        CampaignFleetAPI player = player();
        SectorEntityToken target = PTSDFTDirector.target(chain);
        if (player == null || target == null) return;
        if (player.getStarSystem() != target.getStarSystem()) {
            if (day - chain.nextDay > 15f && chain.steps > 0) chain.steps--;
            return;
        }
        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        if ("DIRECT_CONTACT".equals(chain.node) && dialog == null && !chain.triggered) {
            encounter(state, chain, player, target);
            return;
        }
        boolean explicit = dialog != null && dialog.getInteractionTarget() == target;
        if (explicit && !chain.triggered) {
            encounter(state, chain, player, target);
            return;
        }
        if (Misc.getDistance(player, target) < 3800f && day - chain.nextDay >= 2f) {
            chain.steps++;
            chain.nextDay = day;
            if (chain.steps == 1) {
                chain.outcome = "航迹边缘有数个碎屑反射点同时停住。它们仍未靠近。";
                Global.getSector().getCampaignUI().addMessage(chain.outcome);
            } else if (chain.steps == 2) {
                chain.outcome = "第二次扫描时，反射点已经位于舰队后方。";
                Global.getSector().getCampaignUI().addMessage(chain.outcome);
            } else if (!chain.triggered) encounter(state, chain, player, target);
        }
    }
    private void encounter(PTSDCrisisState state, PTSDLateState.FTChain chain,
                           CampaignFleetAPI player, SectorEntityToken target) {
        if (state.late.strategicReserve < 20f) return;
        CampaignFleetAPI fleet = spawn(state, chain, "未知守卫编队", "Omega_Scrap_Heap_Garbage",
                player.getLocation().x + 2800f, player.getLocation().y + 1000f,
                20f, "$PTSD_ft04");
        if (fleet == null) return;
        fleet.addAssignment(FleetAssignment.INTERCEPT, player, 20f, "排除近轨道接触");
        PTSDOmegaEncounterResponse.attachPursuer(fleet, player);
        chain.triggered = true;
        finish(chain, "母星近轨道守卫已实体出动；其兵力从当地防区扣除");
    }
}
