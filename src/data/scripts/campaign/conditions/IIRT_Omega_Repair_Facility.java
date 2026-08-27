package data.scripts.campaign.conditions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.IIRT_Omega_ModPlugin;
import data.scripts.campaign.invasion.PTSDOccupationManager;

/**
 * Core controller for a crisis-held planet. The market is intentionally outside EconomyAPI;
 * this condition supplies repair/defence metadata while garrisons come from crisis events.
 */
public class IIRT_Omega_Repair_Facility extends BaseMarketConditionPlugin {
    public static final String FACILITY_MEMORY = "$PTSD_repair_facility_active";
    protected float elapsed = 0f;
    protected static final float qualityBonus = 3f;

    @Override public void apply(String id) {
        if (market == null) return;
        market.getMemoryWithoutUpdate().set(FACILITY_MEMORY, true);
        market.getMemoryWithoutUpdate().set(PTSDOccupationManager.STRATEGIC_SHELL_MEMORY, true);
        float progress = Math.min(1f, elapsed / Math.max(1f, IIRT_Omega_ModPlugin.repair_time));
        market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD)
                .modifyFlat(id, qualityBonus * progress, "Omega reconstruction lattice");
        market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD)
                .modifyFlat(id, 1f + qualityBonus * progress, "Omega repair facility");
        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD)
                .modifyMult(id, 3f + progress * 5f, "Embedded defence architecture");
    }

    @Override public void unapply(String id) {
        if (market == null) return;
        market.getMemoryWithoutUpdate().unset(FACILITY_MEMORY);
        market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).unmodify(id);
        market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).unmodify(id);
        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodify(id);
    }

    @Override public void advance(float amount) {
        if (!IIRT_Omega_ModPlugin.isInvasionEnabled() || market == null) return;
        elapsed = Math.min(Math.max(1f, IIRT_Omega_ModPlugin.repair_time),
                elapsed + Global.getSector().getClock().convertToDays(amount));
        market.getMemoryWithoutUpdate().set(FACILITY_MEMORY, true);
    }
}