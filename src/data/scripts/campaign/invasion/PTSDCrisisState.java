package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The save-persistent strategic layer for the entire Psychasthenia crisis.
 * Physical fleets are only projections of this data; they are not the source of truth.
 */
public final class PTSDCrisisState implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String PERSISTENT_KEY = "$PTSD_crisis_state_v2";
    public static final int CURRENT_VERSION = 2;

    public enum Phase {
        DORMANT,
        RECON,
        EXPANSION,
        FORTIFICATION,
        WAR,
        ENDED
    }

    public enum EventType {
        SCOUT,
        ATTACK,
        DEFENSE,
        MERCENARY_DEFENSE,
        CONSTRUCTION,
        GARRISON,
        FORTRESS_PATROL,
        PLAYER_TASK_FORCE,
        EXTERNAL
    }

    public enum EventStatus {
        PLANNED,
        MATERIALIZED,
        RESOLVED,
        CANCELLED
    }

    public static final class SystemData implements Serializable {
        private static final long serialVersionUID = 1L;

        public String systemId;
        public int scoutVisits;
        public int playerSightings;
        public int hostileContacts;
        public float observedFleetStrength;
        public float observedMarketDefense;
        public float strategicValue;
        public float attackWeight = 1f;
        public float humanDefenseWeight = 1f;
        public float learningMultiplier = 1f;
        public float omegaControl;
        public float humanControl = 1f;
        public float lastObservedDay;
        public float lastWeightUpdateDay;
        public int successfulOmegaAttacks;
        public int failedOmegaAttacks;
        public int conversionLevel;
        public boolean blackHoleFortress;
        public boolean knownToPlayer;

        public SystemData() {
        }

        public SystemData(String systemId) {
            this.systemId = systemId;
        }
    }

    public static final class StrategicEvent implements Serializable {
        private static final long serialVersionUID = 1L;

        public String id;
        public EventType type;
        public EventStatus status = EventStatus.PLANNED;
        public String side;
        public String factionId;
        public String sourceSystemId;
        public String targetSystemId;
        public String targetMarketId;
        public float strength;
        public float createdDay;
        public float resolveDay;
        public String materializedFleetId;
        public float materializedDay;
        public float projectionExpiresDay;
        public float nextProjectionDay;
        public boolean aftermathProjected;
        public String description;
        public String referenceId;
        public boolean playerRelevant;
        public boolean successful;

        public StrategicEvent() {
        }

        public StrategicEvent(EventType type, String side, String factionId,
                              String sourceSystemId, String targetSystemId,
                              String targetMarketId, float strength,
                              float createdDay, float resolveDay) {
            this.id = "PTSD_event_" + Misc.genUID();
            this.type = type;
            this.side = side;
            this.factionId = factionId;
            this.sourceSystemId = sourceSystemId;
            this.targetSystemId = targetSystemId;
            this.targetMarketId = targetMarketId;
            this.strength = strength;
            this.createdDay = createdDay;
            this.resolveDay = resolveDay;
        }
    }

    public static final class PlayerMarker implements Serializable {
        private static final long serialVersionUID = 1L;

        public String systemId;
        public String type;
        public float weight;
        public float placedDay;

        public PlayerMarker() {
        }

        public PlayerMarker(String systemId, String type, float weight, float placedDay) {
            this.systemId = systemId;
            this.type = type;
            this.weight = weight;
            this.placedDay = placedDay;
        }
    }

    public static final class PlayerTaskForce implements Serializable {
        private static final long serialVersionUID = 1L;

        public String id;
        public String name;
        public String sourceMarketId;
        public String assignedSystemId;
        public String specialization;
        public float strength;
        public float deploymentWeight = 1f;
        public int productionCost;
        public float createdDay;
        public float nextRedeployDay;
        public boolean destroyed;

        public PlayerTaskForce() {
        }
    }

    public static final class OccupationData implements Serializable {
        private static final long serialVersionUID = 1L;

        public String marketId;
        public int harassmentBombardments;
        public int saturationBombardments;
        public int probes;
        public int negotiations;
        public int defenseVictories;
        public float omegaAttention;
        public float humanAttention;
        public float accumulatedDamage;
        public float lastInteractionDay;
        public String lastInteraction;

        public OccupationData() {
        }

        public OccupationData(String marketId) {
            this.marketId = marketId;
        }
    }

    public int version = CURRENT_VERSION;
    public Phase phase = Phase.DORMANT;
    public float phaseStartedDay;
    public float nextScoutDay;
    public float nextWeightUpdateDay;
    public float nextOmegaTurnDay;
    public float nextHumanTurnDay;
    public float nextExpansionDay;
    public float nextFortressDay;
    public float lastSimulationDay;

    public int totalScoutSightings;
    public int totalScoutEscapes;
    public int totalOmegaEncounters;
    public int visibleStage;
    public boolean softWarningShown;
    public boolean hardWarningShown;
    public boolean watcherTransferred;
    public boolean preWarIntelCreated;
    public boolean warIntelCreated;

    public String baseSystemId;
    public String baseMarketId;

    public Map<String, SystemData> systems = new LinkedHashMap<String, SystemData>();
    public List<StrategicEvent> events = new ArrayList<StrategicEvent>();
    public Map<String, PlayerMarker> playerMarkers = new LinkedHashMap<String, PlayerMarker>();
    public List<PlayerTaskForce> playerTaskForces = new ArrayList<PlayerTaskForce>();
    public Map<String, Integer> committedProduction = new LinkedHashMap<String, Integer>();
    public Map<String, OccupationData> occupations = new LinkedHashMap<String, OccupationData>();
    public Map<String, Float> aftermathCooldowns = new LinkedHashMap<String, Float>();

    private PTSDCrisisState() {
        float day = getDay();
        phaseStartedDay = day;
        nextScoutDay = day + 5f;
        nextWeightUpdateDay = day + 1f;
        nextOmegaTurnDay = day + 5f;
        nextHumanTurnDay = day + 8f;
        nextExpansionDay = day + 5f;
        nextFortressDay = day + 10f;
        lastSimulationDay = day;
    }

    public static PTSDCrisisState get() {
        if (Global.getSector() == null) return null;
        Object existing = Global.getSector().getPersistentData().get(PERSISTENT_KEY);
        if (existing instanceof PTSDCrisisState) {
            PTSDCrisisState state = (PTSDCrisisState) existing;
            state.repairCollections();
            return state;
        }
        PTSDCrisisState state = new PTSDCrisisState();
        state.migrateLegacyMemory();
        Global.getSector().getPersistentData().put(PERSISTENT_KEY, state);
        return state;
    }

    private void migrateLegacyMemory() {
        Object legacy = Global.getSector().getMemoryWithoutUpdate().get(IIRT_Omega_Invasion.stage_id);
        String name = null;
        if (legacy instanceof Enum) name = ((Enum<?>) legacy).name();
        else if (legacy instanceof String) name = (String) legacy;
        if ("COLLECT_DATA".equals(name)) phase = Phase.RECON;
        else if ("INVADE".equals(name)) phase = Phase.EXPANSION;
        else if ("REPAIR".equals(name)) phase = Phase.FORTIFICATION;
        else if ("FULL_ATTACK".equals(name)) phase = Phase.WAR;
        else if ("END".equals(name)) phase = Phase.ENDED;
        else if (name == null) {
            String setting = data.scripts.IIRT_Omega_ModPlugin.PTSD_DefStat_onNewGame;
            if ("Cod".equals(setting)) phase = Phase.RECON;
            else if ("Inv".equals(setting)) phase = Phase.EXPANSION;
            else if ("Rep".equals(setting)) phase = Phase.FORTIFICATION;
            else if ("FuA".equals(setting)) phase = Phase.WAR;
            else if ("End".equals(setting)) phase = Phase.ENDED;
        }
        Object legacyBaseSystem = Global.getSector().getMemoryWithoutUpdate().get(IIRT_Omega_Invasion.baseSystem_id);
        Object legacyBaseMarket = Global.getSector().getMemoryWithoutUpdate().get(IIRT_Omega_Invasion.baseMarket_id);
        if (legacyBaseSystem instanceof String) baseSystemId = (String) legacyBaseSystem;
        if (legacyBaseMarket instanceof String) baseMarketId = (String) legacyBaseMarket;
        phaseStartedDay = getDay();
    }

    public static float getDay() {
        if (Global.getSector() == null || Global.getSector().getClock() == null) return 0f;
        return Global.getSector().getClock().getTimestamp() / 86400000f;
    }

    public void repairCollections() {
        if (systems == null) systems = new LinkedHashMap<String, SystemData>();
        if (events == null) events = new ArrayList<StrategicEvent>();
        if (playerMarkers == null) playerMarkers = new LinkedHashMap<String, PlayerMarker>();
        if (playerTaskForces == null) playerTaskForces = new ArrayList<PlayerTaskForce>();
        if (committedProduction == null) committedProduction = new LinkedHashMap<String, Integer>();
        if (occupations == null) occupations = new LinkedHashMap<String, OccupationData>();
        if (aftermathCooldowns == null) aftermathCooldowns = new LinkedHashMap<String, Float>();
        version = CURRENT_VERSION;
    }

    public SystemData getSystemData(String systemId) {
        if (systemId == null) return null;
        SystemData result = systems.get(systemId);
        if (result == null) {
            result = new SystemData(systemId);
            systems.put(systemId, result);
        }
        return result;
    }

    public StrategicEvent getEvent(String eventId) {
        if (eventId == null) return null;
        for (StrategicEvent event : events) {
            if (eventId.equals(event.id)) return event;
        }
        return null;
    }

    public StrategicEvent addEvent(EventType type, String side, String factionId,
                                   String sourceSystemId, String targetSystemId,
                                   String targetMarketId, float strength,
                                   float delayDays) {
        float day = getDay();
        StrategicEvent event = new StrategicEvent(type, side, factionId,
                sourceSystemId, targetSystemId, targetMarketId,
                strength, day, day + Math.max(1f, delayDays));
        events.add(event);
        trimResolvedEvents();
        PTSDCrisisDevIntel.reportEventCreated(event);
        return event;
    }

    public List<StrategicEvent> getActiveEvents() {
        List<StrategicEvent> result = new ArrayList<StrategicEvent>();
        for (StrategicEvent event : events) {
            if (event.status == EventStatus.PLANNED || event.status == EventStatus.MATERIALIZED) {
                result.add(event);
            }
        }
        return result;
    }

    public int countActiveEvents(EventType type) {
        int result = 0;
        for (StrategicEvent event : events) {
            if (event.type == type && (event.status == EventStatus.PLANNED || event.status == EventStatus.MATERIALIZED)) {
                result++;
            }
        }
        return result;
    }

    public int countActiveTaskForces() {
        int result = 0;
        for (PlayerTaskForce force : playerTaskForces) {
            if (!force.destroyed) result++;
        }
        return result;
    }

    public int getCommittedProduction(String marketId) {
        Integer result = committedProduction.get(marketId);
        return result == null ? 0 : result;
    }

    public PlayerTaskForce getTaskForce(String forceId) {
        if (forceId == null) return null;
        for (PlayerTaskForce force : playerTaskForces) if (forceId.equals(force.id)) return force;
        return null;
    }

    public OccupationData getOccupationData(String marketId) {
        if (marketId == null) return null;
        OccupationData data = occupations.get(marketId);
        if (data == null) {
            data = new OccupationData(marketId);
            occupations.put(marketId, data);
        }
        return data;
    }

    public void releaseCommittedProduction(String marketId, int amount) {
        if (marketId == null || amount <= 0) return;
        int remaining = Math.max(0, getCommittedProduction(marketId) - amount);
        if (remaining == 0) committedProduction.remove(marketId);
        else committedProduction.put(marketId, remaining);
    }

    public void addCommittedProduction(String marketId, int amount) {
        if (marketId == null || amount <= 0) return;
        committedProduction.put(marketId, getCommittedProduction(marketId) + amount);
    }

    public void putMarker(String systemId, String type, float weight) {
        if (systemId == null) return;
        playerMarkers.put(systemId, new PlayerMarker(systemId, type, weight, getDay()));
        SystemData data = getSystemData(systemId);
        if (data != null) data.knownToPlayer = true;
    }

    public void clearMarker(String systemId) {
        if (systemId != null) playerMarkers.remove(systemId);
    }

    public StarSystemAPI resolveSystem(String systemId) {
        if (systemId == null || Global.getSector() == null) return null;
        return Global.getSector().getStarSystem(systemId);
    }

    public MarketAPI resolveMarket(String marketId) {
        if (marketId == null || Global.getSector() == null || Global.getSector().getEconomy() == null) return null;
        return Global.getSector().getEconomy().getMarket(marketId);
    }

    private void trimResolvedEvents() {
        if (events.size() <= 160) return;
        List<StrategicEvent> kept = new ArrayList<StrategicEvent>();
        for (StrategicEvent event : events) {
            if (event.status == EventStatus.PLANNED || event.status == EventStatus.MATERIALIZED) {
                kept.add(event);
            }
        }
        for (int i = events.size() - 1; i >= 0 && kept.size() < 120; i--) {
            StrategicEvent event = events.get(i);
            if (!kept.contains(event)) kept.add(event);
        }
        events = kept;
    }
}