package data.scripts.campaign.cargo;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.DataForEncounterSide;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.FleetMemberData;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.Status;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfigGen;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;

/** Shared officer/salvage hooks for Fourth Watch and Psychasthenia fleets. */
public final class PTSDOmegaFleetSupport {
    public static final String CORE_ID = "PTSD_core";
    public static final String FRAGMENT_ITEM_ID = "PTSD_core_fragment";
    public static final String WATCHER_FACTION_ID = "Omega_Watcher";
    public static final String PSYCHASTHENIA_FACTION_ID = "Omega_Psychasthenia";

    private PTSDOmegaFleetSupport() {
    }

    public static boolean isSupportedFleet(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getFaction() == null) return false;
        String factionId = fleet.getFaction().getId();
        return WATCHER_FACTION_ID.equals(factionId) || PSYCHASTHENIA_FACTION_ID.equals(factionId);
    }

    /**
     * Installs the reward-only encounter config when a fleet has no more specialized config.
     * Seeded fortress fleets keep their own combat rules and call addCoreFragments() from there.
     */
    public static void installSalvageConfigIfAbsent(CampaignFleetAPI fleet) {
        if (!isSupportedFleet(fleet)) return;
        if (fleet.getMemoryWithoutUpdate().contains(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN)) return;
        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN,
                new CoreFragmentFIDConfigGen());
    }

    public static void addCoreFragments(InteractionDialogAPI dialog,
                                        FleetEncounterContext context,
                                        CargoAPI salvage) {
        if (!(dialog.getInteractionTarget() instanceof CampaignFleetAPI)) return;
        CampaignFleetAPI fleet = (CampaignFleetAPI) dialog.getInteractionTarget();
        if (!isSupportedFleet(fleet)) return;
        DataForEncounterSide data = context.getDataFor(fleet);
        if (data == null) return;

        int fragments = 0;
        for (FleetMemberData casualty : data.getOwnCasualties()) {
            if (casualty.getStatus() != Status.DESTROYED && casualty.getStatus() != Status.DISABLED) continue;
            FleetMemberAPI member = casualty.getMember();
            if (member == null || member.isFighterWing() || member.getCaptain() == null) continue;
            if (!CORE_ID.equals(member.getCaptain().getAICoreId())) continue;
            fragments += member.isStation() ? 3 : 1;
        }
        if (fragments > 0) {
            salvage.addSpecial(new SpecialItemData(FRAGMENT_ITEM_ID, null), fragments);
            if (Global.getSettings().isDevMode()) {
                Global.getSector().getCampaignUI().addMessage(
                        "[DEV核心奖励] " + fleet.getName() + "：创伤协议残片 ×" + fragments);
            }
        }
    }

    public static final class CoreFragmentFIDConfigGen implements FIDConfigGen {
        @Override
        public FIDConfig createConfig() {
            FIDConfig config = new FIDConfig();
            config.showTransponderStatus = false;
            config.delegate = new BaseFIDDelegate() {
                @Override
                public void postPlayerSalvageGeneration(InteractionDialogAPI dialog,
                                                        FleetEncounterContext context,
                                                        CargoAPI salvage) {
                    addCoreFragments(dialog, context, salvage);
                }
            };
            return config;
        }
    }
}
