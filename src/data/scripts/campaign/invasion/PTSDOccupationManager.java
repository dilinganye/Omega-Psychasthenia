package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import java.util.LinkedHashSet;
import java.util.Set;

/** Keeps occupied markets off the vanilla map while retaining them as strategic objects. */
public final class PTSDOccupationManager {
    public static final String CONTROLLED_MEMORY = "$PTSD_controlled_territory";
    public static final String MAP_HIDDEN_MEMORY = "$PTSD_occupation_map_hidden";
    public static final String DEFENSE_DEFEATED_MEMORY = "$PTSD_occupation_defense_defeated";
    public static final String ACTIVE_DEFENSE_MEMORY = "$PTSD_occupation_active_defense";

    private PTSDOccupationManager() {
    }

    public static boolean isOccupied(MarketAPI market) {
        if (market == null) return false;
        if (IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(market.getFactionId()) ||
                IIRT_Omega_Invasion.WATCHER_FACTION.equals(market.getFactionId())) return true;
        return market.getMemoryWithoutUpdate().getBoolean(CONTROLLED_MEMORY);
    }

    public static void syncMapVisibility() {
        if (Global.getSector() == null || Global.getSector().getEconomy() == null) return;
        boolean dev = Global.getSettings().isDevMode();
        Set<MarketAPI> markets = new LinkedHashSet<MarketAPI>(
                Global.getSector().getEconomy().getMarketsCopy());
        for (LocationAPI location : Global.getSector().getAllLocations()) {
            for (SectorEntityToken entity : location.getAllEntities()) {
                if (entity.getMarket() != null) markets.add(entity.getMarket());
            }
        }
        for (MarketAPI market : markets) {
            boolean occupied = isOccupied(market);
            boolean managed = market.getMemoryWithoutUpdate().getBoolean(MAP_HIDDEN_MEMORY);
            if (occupied) {
                market.getMemoryWithoutUpdate().set(MAP_HIDDEN_MEMORY, true);
                market.setHidden(!dev);
            } else if (managed) {
                market.setHidden(false);
                market.getMemoryWithoutUpdate().unset(MAP_HIDDEN_MEMORY);
            }
        }
    }
}
