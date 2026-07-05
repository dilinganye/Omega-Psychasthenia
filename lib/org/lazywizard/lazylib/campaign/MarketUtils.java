package org.lazywizard.lazylib.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.ImmigrationPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat.StatMod;

// TODO: Test, Javadoc, add to changelog
class MarketUtils
{
    // Use Misc.getRoundedValue() if you want to match vanilla's displayed number exactly
    /*public static float calculateGrowthPercentage(MarketAPI market)
    {
        final ImmigrationPlugin plugin = Global.getSector().getPluginPicker().pickImmigrationPlugin(market);
        float total = 0f;
        for (StatMod mod : plugin.computeIncoming().getWeight().getFlatMods().values())
        {
            total += mod.getValue();
        }

        return plugin.getFractionForPopulationPoints(total) * 100f;
    }*/

    private MarketUtils()
    {
    }
}
