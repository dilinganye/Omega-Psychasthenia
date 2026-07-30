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
import data.scripts.campaign.invasion.PTSDOccupiedColonyInteraction;

public class PTSD_CampaignPlugin extends BaseCampaignPlugin {
    public PTSD_CampaignPlugin() {
    }

    public String getId() {
        return "AL_CampaignPlugin";
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
        return super.pickInteractionDialogPlugin(interactionTarget);
    }
}
