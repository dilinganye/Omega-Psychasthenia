package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.characters.PersonAPI;
import java.util.*;

/** Campaign-local caches, never serialized and never an intelligence source. */
public final class PTSDCampaignRuntime {
    private static SectorAPI sector;
    private static LocationAPI playerLocation;
    private static final Map<LocationAPI, Entry> fleets = new IdentityHashMap<LocationAPI, Entry>();
    private static List<LocationAPI> auditLocations;
    private static List<MarketAPI> auditMarkets;
    private static int auditBatch;
    private static float nextAuditDay;
    private static final class Entry {
        float expires;
        List<CampaignFleetAPI> values;
    }
    private PTSDCampaignRuntime() { }
    public static void reset() {
        sector = null; playerLocation = null; fleets.clear();
        auditLocations = null; auditMarkets = null; auditBatch = 0; nextAuditDay = 0f;
        PTSDCampaignPerformance.reset();
    }
    private static void ensureSector() {
        if (sector != Global.getSector()) { reset(); sector = Global.getSector(); }
        CampaignFleetAPI player = sector == null ? null : sector.getPlayerFleet();
        LocationAPI current = player == null ? null : player.getContainingLocation();
        if (current != playerLocation) {
            fleets.remove(playerLocation); fleets.remove(current); playerLocation = current;
        }
    }
    public static List<CampaignFleetAPI> fleets(LocationAPI location) {
        if (location == null) return Collections.emptyList();
        ensureSector();
        float day = PTSDCrisisState.getDay();
        Entry entry = fleets.get(location);
        if (entry == null || day >= entry.expires) {
            entry = new Entry();
            entry.expires = day + (location == playerLocation ? .1f : .5f);
            // Vanilla returns a live read-only typed view. Keep it live: external spawns/removals
            // are visible immediately without caching membership, positions or sensor results.
            entry.values = Collections.unmodifiableList(location.getFleets());
            fleets.put(location, entry);
        }
        return entry.values;
    }
    public static void invalidate(LocationAPI location) { if (location != null) fleets.remove(location); }
    public static void entityAdded(SectorEntityToken entity) {
        if (entity == null) return;
        invalidate(entity.getContainingLocation());
        if (entity instanceof CampaignFleetAPI) {
            CampaignFleetAPI fleet = (CampaignFleetAPI)entity;
            if (fleet.getFaction() != null && crisis(fleet.getFaction().getId())) {
                String desired = PTSDCrisisProgress.getActiveFactionId(PTSDCrisisState.get());
                if (!desired.equals(fleet.getFaction().getId())) fleet.setFaction(desired, true);
                normalizeFleet(PTSDCrisisState.get(), fleet);
            }
        }
    }
    public static void leaving(SectorEntityToken entity) {
        if (entity != null) invalidate(entity.getContainingLocation());
    }
    private static boolean crisis(String id) {
        return IIRT_Omega_Invasion.WATCHER_FACTION.equals(id) || IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(id);
    }
    public static void normalizeFleet(PTSDCrisisState state, CampaignFleetAPI fleet) {
        if (state == null || fleet == null || !fleet.isAlive() || fleet.isExpired() ||
                fleet.getFaction() == null || !crisis(fleet.getFaction().getId())) return;
        String desired = PTSDCrisisProgress.getActiveFactionId(state);
        if (!desired.equals(fleet.getFaction().getId())) fleet.setFaction(desired, true);
        for (FleetEventListener listener : fleet.getEventListeners())
            if (listener instanceof PTSDStrategicFleetListener) return;
        fleet.getEventListeners().add(new PTSDStrategicFleetListener(null, fleet.getFleetPoints()));
    }
    public static void audit(PTSDCrisisState state, float day) {
        ensureSector();
        if (sector == null || day < nextAuditDay) return;
        nextAuditDay = day + .5f;
        if (auditLocations == null || auditBatch >= 10) {
            auditLocations = new ArrayList<LocationAPI>(sector.getAllLocations());
            auditMarkets = new ArrayList<MarketAPI>(sector.getEconomy().getMarketsCopy());
            auditBatch = 0;
        }
        int start = auditLocations.size() * auditBatch / 10;
        int end = auditLocations.size() * (auditBatch + 1) / 10;
        String desired = PTSDCrisisProgress.getActiveFactionId(state);
        for (int i = start; i < end; i++) {
            LocationAPI location = auditLocations.get(i);
            for (CampaignFleetAPI fleet : fleets(location)) normalizeFleet(state, fleet);
            for (SectorEntityToken entity : location.getAllEntities()) {
                if (IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(desired) &&
                        !(entity instanceof CampaignFleetAPI) && entity.getFaction() != null &&
                        IIRT_Omega_Invasion.WATCHER_FACTION.equals(entity.getFaction().getId())) entity.setFaction(desired);
                normalizeMarket(state, entity.getMarket(), desired);
            }
        }
        start = auditMarkets.size() * auditBatch / 10;
        end = auditMarkets.size() * (auditBatch + 1) / 10;
        for (int i = start; i < end; i++) {
            normalizeMarket(state, auditMarkets.get(i), desired);
        }
        if (auditBatch == 0) for (PTSDCrisisState.StrategicEvent event : state.events)
            if (event != null && PTSDCrisisAPI.SIDE_OMEGA.equals(event.side) && crisis(event.factionId)) event.factionId = desired;
        auditBatch++;
    }
    private static void normalizeMarket(PTSDCrisisState state, MarketAPI market, String desired) {
        if (market == null || !crisis(market.getFactionId()) || desired.equals(market.getFactionId())) return;
        boolean asset = IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(desired) ||
                market.getId().equals(state.baseMarketId) || market.getMemoryWithoutUpdate().getBoolean("$PTSD_controlled_territory");
        if (!asset) return;
        market.setFactionId(desired);
        if (market.getPrimaryEntity() != null) market.getPrimaryEntity().setFaction(desired);
        for (PersonAPI person : market.getPeopleCopy()) person.setFaction(desired);
    }
}
