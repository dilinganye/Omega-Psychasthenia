package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/** Keeps crisis territory as strategic planet-attached shells, outside the vanilla economy. */
public final class PTSDOccupationManager {
    public static final String CONTROLLED_MEMORY = "$PTSD_controlled_territory";
    public static final String MAP_HIDDEN_MEMORY = "$PTSD_occupation_map_hidden";
    public static final String DEFENSE_DEFEATED_MEMORY = "$PTSD_occupation_defense_defeated";
    public static final String ACTIVE_DEFENSE_MEMORY = "$PTSD_occupation_active_defense";
    public static final String STRATEGIC_SHELL_MEMORY = "$PTSD_strategic_market_shell";
    private static final String REPAIR_FACILITY = "IIRT_Omega_Repair_Facility";
    private static final String SCOUT_TAG = "IIRT_Omega_Scout";
    private static final String EVENT_MEMORY = "$PTSD_strategic_event";
    private static final String FORTRESS_MEMORY = "$PTSD_black_hole_fortress";
    private PTSDOccupationManager() { }

    public static boolean isOccupied(MarketAPI market) {
        if (market == null) return false;
        if (IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(market.getFactionId()) ||
                IIRT_Omega_Invasion.WATCHER_FACTION.equals(market.getFactionId())) return true;
        return market.getMemoryWithoutUpdate().getBoolean(CONTROLLED_MEMORY);
    }

    /** Converts a market into a non-economic strategic shell while preserving planet interaction/state IDs. */
    public static void prepareStrategicShell(MarketAPI market) {
        if (market == null) return;
        boolean newlyIsolated = !market.getMemoryWithoutUpdate().getBoolean(STRATEGIC_SHELL_MEMORY);
        market.getMemoryWithoutUpdate().set(CONTROLLED_MEMORY, true);
        market.getMemoryWithoutUpdate().set(STRATEGIC_SHELL_MEMORY, true);
        market.setPlayerOwned(false);
        market.setAdmin(null);
        market.setPlanetConditionMarketOnly(true);
        market.setSize(1);
        market.getCommDirectory().clear();
        for (PersonAPI person : new ArrayList<PersonAPI>(market.getPeopleCopy())) market.removePerson(person);
        market.clearCommodities();
        for (Industry industry : new ArrayList<Industry>(market.getIndustries())) market.removeIndustry(industry.getId(), null, false);
        for (SubmarketAPI submarket : new ArrayList<SubmarketAPI>(market.getSubmarketsCopy())) market.removeSubmarket(submarket.getSpecId());
        if (!market.hasCondition(REPAIR_FACILITY)) market.addCondition(REPAIR_FACILITY);
        if (Global.getSector() != null && Global.getSector().getEconomy() != null && market.isInEconomy()) {
            Global.getSector().getEconomy().removeMarket(market);
        }
        market.setHidden(!Global.getSettings().isDevMode());
        market.getMemoryWithoutUpdate().set(MAP_HIDDEN_MEMORY, true);
        if (newlyIsolated) {
            PTSDCrisisDevIntel.report("占领市场经济隔离", "移出EconomyAPI并启用重构设施",
                    market.getStarSystem() == null ? null : market.getStarSystem().getId(), market.getId());
        }
    }

    public static void syncMapVisibility() {
        if (Global.getSector() == null || Global.getSector().getEconomy() == null) return;
        boolean dev = Global.getSettings().isDevMode();
        Set<MarketAPI> markets = getAllMarkets();
        for (MarketAPI market : markets) {
            boolean occupied = isOccupied(market);
            boolean managed = market.getMemoryWithoutUpdate().getBoolean(MAP_HIDDEN_MEMORY);
            if (occupied) {
                prepareStrategicShell(market);
                market.setHidden(!dev);
            } else if (managed) {
                market.setHidden(false);
                market.getMemoryWithoutUpdate().unset(MAP_HIDDEN_MEMORY);
            }
        }
        normalizeLegacyFleets(markets);
    }

    public static Set<MarketAPI> getAllMarkets() {
        Set<MarketAPI> markets = new LinkedHashSet<MarketAPI>(Global.getSector().getEconomy().getMarketsCopy());
        for (LocationAPI location : Global.getSector().getAllLocations()) {
            for (SectorEntityToken entity : location.getAllEntities()) if (entity.getMarket() != null) markets.add(entity.getMarket());
        }
        return markets;
    }

    /** Removes legacy economy traders and turns any unclassified crisis fleet into a local guard. */
    private static void normalizeLegacyFleets(Set<MarketAPI> markets) {
        for (LocationAPI location : Global.getSector().getAllLocations()) {
            SectorEntityToken guardTarget = null;
            for (MarketAPI market : markets) {
                if (isOccupied(market) && market.getContainingLocation() == location && market.getPrimaryEntity() != null) {
                    guardTarget = market.getPrimaryEntity(); break;
                }
            }
            for (CampaignFleetAPI fleet : new ArrayList<CampaignFleetAPI>(location.getFleets())) {
                if (fleet == null || fleet.getFaction() == null) continue;
                String factionId = fleet.getFaction().getId();
                if (!IIRT_Omega_Invasion.WATCHER_FACTION.equals(factionId) &&
                        !IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(factionId)) continue;
                if (fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_TRADE_FLEET)) {
                    if (fleet.getBattle() == null) fleet.despawn(com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason.OTHER, null);
                    continue;
                }
                if (fleet.isStationMode() || fleet.hasTag(SCOUT_TAG) ||
                        fleet.getMemoryWithoutUpdate().contains(EVENT_MEMORY) ||
                        fleet.getMemoryWithoutUpdate().getBoolean(FORTRESS_MEMORY) || guardTarget == null) continue;
                fleet.clearAssignments();
                fleet.setName("\"轰鸣\"");
                fleet.setNoFactionInName(true);
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
                fleet.addAssignment(FleetAssignment.DEFEND_LOCATION, guardTarget, 9999f, "似乎是你吸引太久注意力的代价");
            }
        }
    }
}