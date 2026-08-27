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
    public static final int CURRENT_VERSION = 9;
    public static final float CAMPAIGN_DAY_EPOCH_OFFSET = 700000f;

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
        PREWAR_HUNTER,
        GRUDGE_RAID,
        FIRE_PROBE,
        EXTERNAL
    }

    public enum EventStatus {
        PLANNED,
        MATERIALIZED,
        RESOLVED,
        CANCELLED
    }

    public static final class ColonyPanicData implements Serializable {
        private static final long serialVersionUID = 1L;

        public String marketId;
        /** Persistent contribution from news, encounters and player-facing events. */
        public float eventPanic;
        /** Recomputed from distance to Psychasthenia-controlled space and the current phase. */
        public float proximityPanic;
        public float lastChangedDay;
        public String lastSource;

        public ColonyPanicData() { }

        public ColonyPanicData(String marketId) {
            this.marketId = marketId;
        }
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
        public float lastReconSampleDay;
        public int reconDailyBucket = -1;
        public float reconDailyMax;
        public int reconDailyReports;
        public List<Float> reconStrengthHistory = new ArrayList<Float>();
        public boolean hasNonCrisisColony;
        public boolean hasNonCrisisFleet;
        public boolean occupationSuggested;
        public float occupationWeight;
        public float lastWeightUpdateDay;
        public int successfulOmegaAttacks;
        public int failedOmegaAttacks;
        public int conversionLevel;
        public boolean blackHoleFortress;
        public boolean knownToPlayer;
        /** Per-colony panic records stored alongside Omega reconnaissance data. */
        public Map<String, ColonyPanicData> colonyPanic = new LinkedHashMap<String, ColonyPanicData>();
        public float systemPanic;

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
        public String targetEntityId;
        public float strength;
        public float createdDay;
        public float resolveDay;
        public String materializedFleetId;
        /** All temporary physical groups currently projecting this strategic event. */
        public List<String> materializedFleetIds = new ArrayList<String>();
        /** Last campaign day the player was inside this event's materialization range. */
        public float lastPlayerNearDay;
        public float materializedDay;
        public float projectionExpiresDay;
        public float nextProjectionDay;
        public boolean aftermathProjected;
        public String description;
        public String referenceId;
        public boolean playerRelevant;
        public boolean successful;
        /** Faction whose defenses this Omega action is testing or attacking. */
        public String opponentFactionId;
        /** Prevents physical and strategic resolution from recording the same defeat twice. */
        public boolean defeatLearningRecorded;
        /** PYRRHIC_HUMAN, OMEGA_DEFEAT, or HUMAN_DEFEAT. */
        public String aftermathKind;

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

    /**
     * A narrative crisis card. Public reports may be wrong; the truth and exact target are
     * retained for DevMode and later declassification without leaking them to normal Intel.
     */
    public static final class CrisisIncident implements Serializable {
        private static final long serialVersionUID = 1L;

        public String id;
        public String cardId;
        public String category;
        public int randomBranch;
        public Phase phase;
        public String targetSystemId;
        public String targetMarketId;
        public String targetEntityId;
        public String linkedEventId;
        public float createdDay;
        public float expiresDay;
        public String sourceLabel;
        public String headline;
        public String publicText;
        public String trueText;
        public String effectSummary;
        public boolean disclosed;
        public boolean playerRelevant;
        public boolean devForced;
        public boolean investigable;
        public boolean readByPlayer;
        public boolean recordedByPlayer;
        /** 0 unresolved, 1 matching evidence, 2 false report, 3 Fourth Watch tracker. */
        public int investigationOutcome;
        public boolean investigationResolved;
        public boolean investigationReal;
        public float newsExpiresDay;
        public float investigationExpiresDay;
        /** Exact per-market panic contribution caused by this news item. */
        public Map<String, Float> panicByMarket = new LinkedHashMap<String, Float>();
        public float panicMitigationRatio = 1f;
        /** CSV-driven true-site template(s), optional custom handler, and persisted physical scene. */
        public String siteTemplate = "";
        public String siteHandlerExpression = "";
        public String siteTitle = "";
        public String siteDescription = "";
        public String siteConfirmationHint = "";
        public List<String> siteEntityIds = new ArrayList<String>();
        public boolean siteMaterialized;
        public boolean siteConfirmed;
        public boolean martialSiteEligible;
        public boolean martialSiteSpawned;
        public float siteHardExpireDay;
        public float siteCleanupDay;

        public CrisisIncident() { }
    }

    public static final class SignalTrace implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id;
        public String systemId;
        public String fleetId;
        public String label;
        public float createdDay;
        public float expiresDay;
        public boolean confirmed;
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
    public float lastProgressUpdateDay;
    public float lastLocalPanicUpdateDay;
    public float nextPanicPirateDay;

    // Persisted player-centred RECON ambient-event scheduler and pending cross-system trap.
    public int reconPlayerEventDayBucket = -1;
    public String reconTrapSystemId;
    public String reconTrapTargetEntityId;
    public float reconTrapExpiresDay;

    // Continuous crisis variables; all use a stable 0..100 scale.
    public float reconConfidence;
    public float humanAwareness;
    public float watcherAggression;
    public float nestDevelopment;
    public float blockadeDensity;
    public float omegaEscalation;
    public float humanCohesion;
    /** Stage-wide additive panic. Defaults to zero and changes only through explicit special events. */
    public float globalPanic;
    /** Deprecated binary/save compatibility alias for globalPanic. */
    @Deprecated public float publicPanic;
    public float realityDistortion;
    public boolean progressInitialized;
    public String lastProgressSource;
    public String lastProgressSystemId;
    public float lastProgressChangeDay;
    public float nextIncidentDay;
    public float nextIsolationSyncDay;
    public boolean pandoraInitialized;
    public boolean pandoraOpened;

    public int totalScoutSightings;
    public int totalScoutEscapes;
    public int totalOmegaEncounters;
    /** Player-involved battles against either crisis-era faction; sightings do not count. */
    public int totalPlayerOmegaBattles;
    public int visibleStage;
    public boolean softWarningShown;
    public boolean hardWarningShown;
    public boolean watcherTransferred;
    public boolean preInvasionFactionSynchronized;
    public boolean preWarIntelCreated;
    public boolean warIntelCreated;
    public boolean legacyTimelineMigrated;
    public boolean timelineMigrationReported;
    public boolean diplomacyLockedReported;

    /** Persistent anti-faction learning accumulated from lost Omega engagements. */
    public Map<String, Float> factionResistance = new LinkedHashMap<String, Float>();
    /** Player-specific hostility; drives post-war attrition and pursuit actions. */
    public float playerGrudge;
    public float nextGrudgeRaidDay;
    public String prewarHunterEventId;
    public boolean prewarHunterSpawned;
    public boolean prewarHunterResolved;
    public boolean prewarRedAlertShown;
    public float prewarHunterResolvedDay;
    public float warCommitDay;

    // Je Otloes independent contact/event state.
    public boolean jeIntroCompleted;
    public boolean jeResistedOnce;
    public String jePendingIntroMarketId;
    public String jePersonId;
    public String jePlayerTaskIncidentId;
    public String jeAgentIncidentId;
    public float jeAgentReturnDay;
    public String jeMeetingMarketId;
    public boolean jeMeetingReady;
    public boolean jePendingMeetingDialog;
    public int jeCompletedInvestigations;
    /** Campaign-day cooldowns and persisted contact outcomes; opening the dialog cannot reroll these. */
    public float jeLastTaskAcceptedDay;
    public float jePostTaskBusyUntilDay;
    public float jeNextTaskAvailableDay;
    public int jeContactDayBucket = -1;
    public int jeContactAttemptsToday;
    public float jeContactBlockedUntilDay;
    public int jeMissedContactsSinceSuccess;
    public boolean jeMissedContactApologyPending;
    public boolean jeSpamComplaintPending;
    public boolean jeDetectorGranted;
    /** DevMode one-shot contact outcome: 0 random, 1 connected, 2 missed, 3 blocked for the day. */
    public int jeDevNextContactOutcome;

    public String baseSystemId;
    public String baseMarketId;

    public Map<String, SystemData> systems = new LinkedHashMap<String, SystemData>();
    public List<StrategicEvent> events = new ArrayList<StrategicEvent>();
    public Map<String, PlayerMarker> playerMarkers = new LinkedHashMap<String, PlayerMarker>();
    public List<PlayerTaskForce> playerTaskForces = new ArrayList<PlayerTaskForce>();
    public Map<String, Integer> committedProduction = new LinkedHashMap<String, Integer>();
    public Map<String, OccupationData> occupations = new LinkedHashMap<String, OccupationData>();
    public Map<String, Float> aftermathCooldowns = new LinkedHashMap<String, Float>();
    public Map<String, Float> incidentCooldowns = new LinkedHashMap<String, Float>();
    public List<CrisisIncident> incidents = new ArrayList<CrisisIncident>();
    public List<SignalTrace> signalTraces = new ArrayList<SignalTrace>();

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
        lastProgressUpdateDay = day;
        nextIncidentDay = day + 4f;
        nextIsolationSyncDay = day;
        nextGrudgeRaidDay = day + 8f;
        nextPanicPirateDay = day + 10f;
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
        return (float) (Global.getSector().getClock().getTimestamp() / 86400000d +
                CAMPAIGN_DAY_EPOCH_OFFSET);
    }

    public void repairCollections() {
        if (systems == null) systems = new LinkedHashMap<String, SystemData>();
        if (events == null) events = new ArrayList<StrategicEvent>();
        if (playerMarkers == null) playerMarkers = new LinkedHashMap<String, PlayerMarker>();
        if (playerTaskForces == null) playerTaskForces = new ArrayList<PlayerTaskForce>();
        if (committedProduction == null) committedProduction = new LinkedHashMap<String, Integer>();
        if (occupations == null) occupations = new LinkedHashMap<String, OccupationData>();
        if (aftermathCooldowns == null) aftermathCooldowns = new LinkedHashMap<String, Float>();
        if (incidentCooldowns == null) incidentCooldowns = new LinkedHashMap<String, Float>();
        if (incidents == null) incidents = new ArrayList<CrisisIncident>();
        if (signalTraces == null) signalTraces = new ArrayList<SignalTrace>();
        if (factionResistance == null) factionResistance = new LinkedHashMap<String, Float>();
        for (SystemData data : systems.values()) {
            if (data == null) continue;
            if (data.colonyPanic == null) data.colonyPanic =
                    new LinkedHashMap<String, ColonyPanicData>();
        }
        for (CrisisIncident incident : incidents) {
            if (incident == null) continue;
            if (incident.panicByMarket == null) incident.panicByMarket =
                    new LinkedHashMap<String, Float>();
            if (incident.panicMitigationRatio <= 0f) incident.panicMitigationRatio = 1f;
        }
        for (StrategicEvent event : events) {
            if (event == null) continue;
            if (event.materializedFleetIds == null) {
                event.materializedFleetIds = new ArrayList<String>();
            }
            if (event.materializedFleetId != null &&
                    !event.materializedFleetIds.contains(event.materializedFleetId)) {
                event.materializedFleetIds.add(event.materializedFleetId);
            }
        }
        if (version < 7) {
            // The removed public panic mixed news, passive drift and phase floors; it cannot be
            // mapped to a real colony without inventing information, so the new global modifier
            // deliberately starts at zero.
            globalPanic = 0f;
            publicPanic = 0f;
        } else {
            publicPanic = globalPanic;
        }
        if (nextIncidentDay <= 0f) nextIncidentDay = getDay() + 2f;
        if (nextIsolationSyncDay <= 0f) nextIsolationSyncDay = getDay();
        if (nextGrudgeRaidDay <= 0f) nextGrudgeRaidDay = getDay() + 4f;
        if (nextPanicPirateDay <= 0f) nextPanicPirateDay = getDay() + 6f;
        if (version < 3) migrateLegacyTimeline();
        // Any pre-v5 state necessarily existed while the crisis system was already running.
        if (version < 5) {
            pandoraInitialized = true;
            pandoraOpened = true;
        }
        ensureProgressInitialized();
        version = CURRENT_VERSION;
    }

    public void ensureProgressInitialized() {
        if (progressInitialized) return;
        progressInitialized = true;
        applyProgressFloors(phase);
        lastProgressUpdateDay = getDay();
    }

    /** Migration and phase transitions only raise floors; earned progress is never reduced. */
    public void applyProgressFloors(Phase target) {
        if (target == null) target = Phase.DORMANT;
        switch (target) {
            case RECON:
                reconConfidence = Math.max(reconConfidence, 15f);
                humanCohesion = Math.max(humanCohesion, 10f);
                break;
            case EXPANSION:
                reconConfidence = Math.max(reconConfidence, 55f);
                humanAwareness = Math.max(humanAwareness, 15f);
                watcherAggression = Math.max(watcherAggression, 25f);
                nestDevelopment = Math.max(nestDevelopment, 12f);
                humanCohesion = Math.max(humanCohesion, 15f);
                break;
            case FORTIFICATION:
                reconConfidence = Math.max(reconConfidence, 70f);
                humanAwareness = Math.max(humanAwareness, 25f);
                watcherAggression = Math.max(watcherAggression, 35f);
                nestDevelopment = Math.max(nestDevelopment, 45f);
                blockadeDensity = Math.max(blockadeDensity, 10f);
                humanCohesion = Math.max(humanCohesion, 20f);
                realityDistortion = Math.max(realityDistortion, 15f);
                break;
            case WAR:
                reconConfidence = Math.max(reconConfidence, 85f);
                humanAwareness = Math.max(humanAwareness, 70f);
                watcherAggression = Math.max(watcherAggression, 80f);
                nestDevelopment = Math.max(nestDevelopment, 75f);
                blockadeDensity = Math.max(blockadeDensity, 65f);
                omegaEscalation = Math.max(omegaEscalation, 35f);
                humanCohesion = Math.max(humanCohesion, 35f);
                realityDistortion = Math.max(realityDistortion, 35f);
                break;
            case ENDED:
                reconConfidence = Math.max(reconConfidence, 90f);
                humanAwareness = Math.max(humanAwareness, 90f);
                humanCohesion = Math.max(humanCohesion, 40f);
                break;
            default:
                humanCohesion = Math.max(humanCohesion, 5f);
                break;
        }
    }
    private void migrateLegacyTimeline() {
        phaseStartedDay = migrateDay(phaseStartedDay);
        nextScoutDay = migrateDay(nextScoutDay);
        nextWeightUpdateDay = migrateDay(nextWeightUpdateDay);
        nextOmegaTurnDay = migrateDay(nextOmegaTurnDay);
        nextHumanTurnDay = migrateDay(nextHumanTurnDay);
        nextExpansionDay = migrateDay(nextExpansionDay);
        nextFortressDay = migrateDay(nextFortressDay);
        lastSimulationDay = migrateDay(lastSimulationDay);
        for (SystemData data : systems.values()) {
            data.lastObservedDay = migrateDay(data.lastObservedDay);
            data.lastWeightUpdateDay = migrateDay(data.lastWeightUpdateDay);
        }
        for (StrategicEvent event : events) {
            event.createdDay = migrateDay(event.createdDay);
            event.resolveDay = migrateDay(event.resolveDay);
            event.materializedDay = migrateDay(event.materializedDay);
            event.projectionExpiresDay = migrateDay(event.projectionExpiresDay);
            event.nextProjectionDay = migrateDay(event.nextProjectionDay);
        }
        for (PlayerMarker marker : playerMarkers.values()) marker.placedDay = migrateDay(marker.placedDay);
        for (PlayerTaskForce force : playerTaskForces) {
            force.createdDay = migrateDay(force.createdDay);
            force.nextRedeployDay = migrateDay(force.nextRedeployDay);
        }
        for (OccupationData occupation : occupations.values()) {
            occupation.lastInteractionDay = migrateDay(occupation.lastInteractionDay);
        }
        for (Map.Entry<String, Float> entry : aftermathCooldowns.entrySet()) {
            entry.setValue(migrateDay(entry.getValue()));
        }
        legacyTimelineMigrated = true;
    }

    private static float migrateDay(float day) {
        return day < -100000f ? day + CAMPAIGN_DAY_EPOCH_OFFSET : day;
    }
    public SystemData getSystemData(String systemId) {
        if (systemId == null) return null;
        SystemData result = systems.get(systemId);
        if (result == null) {
            result = new SystemData(systemId);
            systems.put(systemId, result);
        }
        if (result.reconStrengthHistory == null) result.reconStrengthHistory = new ArrayList<Float>();
        if (result.colonyPanic == null) result.colonyPanic = new LinkedHashMap<String, ColonyPanicData>();
        return result;
    }

    public ColonyPanicData getColonyPanic(String systemId, String marketId) {
        if (systemId == null || marketId == null) return null;
        SystemData system = getSystemData(systemId);
        ColonyPanicData result = system.colonyPanic.get(marketId);
        if (result == null) {
            result = new ColonyPanicData(marketId);
            system.colonyPanic.put(marketId, result);
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
        if (marketId == null || Global.getSector() == null) return null;
        if (Global.getSector().getEconomy() != null) {
            MarketAPI market = Global.getSector().getEconomy().getMarket(marketId);
            if (market != null) return market;
        }
        // Crisis colonies deliberately live outside EconomyAPI; resolve their planet-attached shell directly.
        for (com.fs.starfarer.api.campaign.LocationAPI location : Global.getSector().getAllLocations()) {
            for (com.fs.starfarer.api.campaign.SectorEntityToken entity : location.getAllEntities()) {
                MarketAPI market = entity.getMarket();
                if (market != null && marketId.equals(market.getId())) return market;
            }
        }
        return null;
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