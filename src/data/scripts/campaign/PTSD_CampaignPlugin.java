//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package data.scripts.campaign;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.BattleAutoresolverPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.CampaignPlugin.PickPriority;
import data.scripts.campaign.cargo.PTSD_Omega_AIOfficerPlugin;
import data.scripts.campaign.invasion.PTSDOccupationManager;
import data.scripts.campaign.invasion.PTSDCrisisState;
import data.scripts.campaign.invasion.PTSDJeOtloesDialog;
import data.scripts.campaign.invasion.PTSDJeOtloesManager;
import data.scripts.campaign.invasion.PTSDOccupiedColonyInteraction;

public class PTSD_CampaignPlugin extends BaseCampaignPlugin {
    public PTSD_CampaignPlugin() {
    }

    public String getId() {
        return "PTSD_CampaignPlugin";
    }

    public boolean isTransient() {
        return true;
    }

    public PluginPick<AICoreOfficerPlugin> pickAICoreOfficerPlugin(String commodityId) {
        return commodityId.equalsIgnoreCase("PTSD_core") ? new PluginPick(new PTSD_Omega_AIOfficerPlugin(), PickPriority.MOD_SPECIFIC) : super.pickAICoreOfficerPlugin(commodityId);
    }

    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {
        if (interactionTarget != null && PTSDOccupationManager.isOccupied(interactionTarget.getMarket())) {
            return new PluginPick<InteractionDialogPlugin>(
                    new PTSDOccupiedColonyInteraction(), PickPriority.MOD_SPECIFIC);
        }
        if (interactionTarget != null && interactionTarget.getMarket() != null &&
                PTSDJeOtloesManager.eligibleMarket(interactionTarget.getMarket())) {
            PTSDCrisisState state = PTSDCrisisState.get();
            if (state != null && state.jeMeetingReady &&
                    interactionTarget.getMarket().getId().equals(state.jeMeetingMarketId)) {
                state.jePendingMeetingDialog = false;
                return new PluginPick<InteractionDialogPlugin>(
                        new PTSDJeOtloesDialog(PTSDJeOtloesDialog.Mode.REPORT,
                                interactionTarget.getMarket()), PickPriority.MOD_SPECIFIC);
            }
            if (state != null && !state.jeIntroCompleted &&
                    PTSDJeOtloesManager.triggerUnlocked(state)) {
                state.jePendingIntroMarketId = null;
                return new PluginPick<InteractionDialogPlugin>(
                        new PTSDJeOtloesDialog(PTSDJeOtloesDialog.Mode.INTRO,
                                interactionTarget.getMarket()), PickPriority.MOD_SPECIFIC);
            }
        }
        return super.pickInteractionDialogPlugin(interactionTarget);
    }
}
