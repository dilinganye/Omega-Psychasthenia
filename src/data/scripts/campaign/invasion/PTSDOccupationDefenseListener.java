package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.BaseFleetEventListener;

import java.io.Serializable;

/** Records a real player victory without making the physical response fleet persistent. */
public final class PTSDOccupationDefenseListener extends BaseFleetEventListener implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String marketId;
    private final boolean negotiationResponse;

    public PTSDOccupationDefenseListener(String marketId, boolean negotiationResponse) {
        this.marketId = marketId;
        this.negotiationResponse = negotiationResponse;
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (fleet == null || battle == null || !battle.isPlayerInvolved()) return;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null || !battle.wasFleetVictorious(player, primaryWinner)) return;
        if (!battle.wasFleetDefeated(fleet, primaryWinner)) return;
        MarketAPI market = resolveMarket();
        if (market == null) return;
        market.getMemoryWithoutUpdate().set(PTSDOccupationManager.DEFENSE_DEFEATED_MEMORY, true);
        clearActive(market, fleet);
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null) {
            PTSDCrisisState.OccupationData data = state.getOccupationData(marketId);
            data.defenseVictories++;
        }
        PTSDOccupationAPI.addAttention(market, PTSDOccupationAPI.Action.DEFENSE_DEFEATED,
                negotiationResponse ? 0.35f : 0.55f, 0.2f);
    }

    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
        MarketAPI market = resolveMarket();
        if (market != null) clearActive(market, fleet);
    }

    private MarketAPI resolveMarket() {
        if (Global.getSector() == null || Global.getSector().getEconomy() == null) return null;
        return Global.getSector().getEconomy().getMarket(marketId);
    }

    private void clearActive(MarketAPI market, CampaignFleetAPI fleet) {
        String active = market.getMemoryWithoutUpdate().getString(PTSDOccupationManager.ACTIVE_DEFENSE_MEMORY);
        if (fleet == null || fleet.getId().equals(active)) {
            market.getMemoryWithoutUpdate().unset(PTSDOccupationManager.ACTIVE_DEFENSE_MEMORY);
        }
    }
}
