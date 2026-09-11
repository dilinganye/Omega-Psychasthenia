package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Optional interoperability surface for other mods. Providers are runtime objects and should
 * register on game load; queued events and the strategic state itself remain save-persistent.
 */
public final class PTSDCrisisAPI {
    public static final String SIDE_OMEGA = "omega";
    public static final String SIDE_HUMAN = "human";

    public interface InterventionProvider {
        List<ForceContribution> getForceContributions(CrisisSnapshot snapshot);
        float modifySystemWeight(String systemId, String side, float currentWeight, CrisisSnapshot snapshot);
        void reportStrategicEventResolved(EventResult result);
    }

    public static final class ForceContribution {
        public String id;
        public String providerId;
        public String side;
        public String factionId;
        public String systemId;
        public float strength;
        public String description;

        public ForceContribution(String id, String side, String factionId,
                                 String systemId, float strength, String description) {
            this.id = id;
            this.side = side;
            this.factionId = factionId;
            this.systemId = systemId;
            this.strength = strength;
            this.description = description;
        }
    }

    public static final class CrisisSnapshot {
        public final PTSDCrisisState.Phase phase;
        public final int sightings;
        public final int encounters;
        public final String baseSystemId;

        private CrisisSnapshot(PTSDCrisisState state) {
            this.phase = state.phase;
            this.sightings = state.totalScoutSightings;
            this.encounters = state.totalOmegaEncounters;
            this.baseSystemId = state.baseSystemId;
        }
    }

    public static final class EventResult {
        public final String eventId;
        public final PTSDCrisisState.EventType type;
        public final String targetSystemId;
        public final String side;
        public final float strength;
        public final boolean successful;

        private EventResult(PTSDCrisisState.StrategicEvent event) {
            this.eventId = event.id;
            this.type = event.type;
            this.targetSystemId = event.targetSystemId;
            this.side = event.side;
            this.strength = event.strength;
            this.successful = event.successful;
        }
    }

    private static final Map<String, InterventionProvider> PROVIDERS =
            new LinkedHashMap<String, InterventionProvider>();

    private PTSDCrisisAPI() {
    }

    public static void registerProvider(String providerId, InterventionProvider provider) {
        if (providerId == null || provider == null) return;
        PROVIDERS.put(providerId, provider);
    }

    public static void unregisterProvider(String providerId) {
        if (providerId != null) PROVIDERS.remove(providerId);
    }

    public static List<ForceContribution> getForceContributions() {
        List<ForceContribution> result = new ArrayList<ForceContribution>();
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return result;
        CrisisSnapshot snapshot = new CrisisSnapshot(state);
        for (Map.Entry<String, InterventionProvider> entry :
                new ArrayList<Map.Entry<String, InterventionProvider>>(PROVIDERS.entrySet())) {
            try {
                List<ForceContribution> supplied = entry.getValue().getForceContributions(snapshot);
                if (supplied == null) continue;
                for (ForceContribution contribution : supplied) {
                    if (contribution == null || contribution.systemId == null || contribution.strength <= 0f) continue;
                    contribution.providerId = entry.getKey();
                    result.add(contribution);
                }
            } catch (Throwable ex) {
                if (Global.getLogger(PTSDCrisisAPI.class) != null) {
                    Global.getLogger(PTSDCrisisAPI.class).warn("Crisis provider failed: " + entry.getKey(), ex);
                }
            }
        }
        return result;
    }

    public static float modifyWeight(String systemId, String side, float weight) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return weight;
        CrisisSnapshot snapshot = new CrisisSnapshot(state);
        float result = weight;
        for (Map.Entry<String, InterventionProvider> entry :
                new ArrayList<Map.Entry<String, InterventionProvider>>(PROVIDERS.entrySet())) {
            try {
                result = entry.getValue().modifySystemWeight(systemId, side, result, snapshot);
            } catch (Throwable ex) {
                Global.getLogger(PTSDCrisisAPI.class).warn("Crisis weight provider failed: " + entry.getKey(), ex);
            }
        }
        if (Float.isNaN(result) || Float.isInfinite(result)) return weight;
        return Math.max(0.05f, result);
    }

    public static PTSDCrisisState.StrategicEvent queueExternalEvent(
            String side, String factionId, String sourceSystemId,
            String targetSystemId, float strength, float delayDays,
            String description) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || targetSystemId == null || strength <= 0f) return null;
        PTSDCrisisState.StrategicEvent event = state.addEvent(
                PTSDCrisisState.EventType.EXTERNAL, side, factionId,
                sourceSystemId, targetSystemId, null, strength, delayDays);
        event.description = description;
        event.playerRelevant = true;
        return event;
    }

    public static PTSDCrisisState.CrisisIncident getIncident(String incidentId) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || incidentId == null) return null;
        for (PTSDCrisisState.CrisisIncident incident : state.incidents) {
            if (incident != null && incidentId.equals(incident.id)) return incident;
        }
        return null;
    }

    /** Records a news lead without revealing whether the source is true. */
    public static boolean recordNewsIncident(String incidentId) {
        PTSDCrisisState.CrisisIncident incident = getIncident(incidentId);
        if (incident == null || !incident.investigable) return false;
        if (!incident.recordedByPlayer) {
            incident.recordedByPlayer = true;
            incident.investigationExpiresDay = PTSDCrisisState.getDay() + 30f;
            java.util.Random seeded = new java.util.Random(incident.id.hashCode() * 31L + 0x50545344L);
            float roll = seeded.nextFloat();
            PTSDCrisisState state = PTSDCrisisState.get();
            // The 5% Fourth Watch tracker branch does not exist before reconnaissance begins.
            incident.investigationOutcome = state != null && state.phase == PTSDCrisisState.Phase.DORMANT
                    ? (roll < .25f ? 1 : 2)
                    : (roll < .25f ? 1 : (roll < .95f ? 2 : 3));
            PTSDCrisisIntel.ensureIntel();
            PTSDCrisisDevIntel.report("新闻线索记录", "调查结果池 " + incident.investigationOutcome,
                    incident.targetSystemId, null);
        }
        return true;
    }

    /** Adds or refreshes a ten-day player-known fleet sighting in the crisis Intel. */
    public static void reportFleetSighting(String systemId, String fleetId, String label) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || systemId == null) return;
        float now = PTSDCrisisState.getDay();
        for (PTSDCrisisState.SignalTrace trace : state.signalTraces) {
            if (trace != null && systemId.equals(trace.systemId) &&
                    ((fleetId == null && trace.fleetId == null) || (fleetId != null && fleetId.equals(trace.fleetId)))) {
                trace.createdDay = now; trace.expiresDay = now + 10f; trace.label = label; trace.confirmed = true;
                PTSDCrisisIntel.ensureIntel(); return;
            }
        }
        PTSDCrisisState.SignalTrace trace = new PTSDCrisisState.SignalTrace();
        trace.id = "PTSD_trace_" + com.fs.starfarer.api.util.Misc.genUID();
        trace.systemId = systemId; trace.fleetId = fleetId;
        trace.label = label == null ? "\u672a\u77e5\u8230\u961f\u76ee\u51fb" : label;
        trace.createdDay = now; trace.expiresDay = now + 10f; trace.confirmed = true;
        state.signalTraces.add(trace);
        PTSDCrisisIntel.ensureIntel();
    }

    public static List<PTSDCrisisState.SignalTrace> getActiveSignalTraces() {
        List<PTSDCrisisState.SignalTrace> result = new ArrayList<PTSDCrisisState.SignalTrace>();
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return result;
        float now = PTSDCrisisState.getDay();
        for (PTSDCrisisState.SignalTrace trace : state.signalTraces) {
            if (trace != null && trace.expiresDay > now) result.add(trace);
        }
        return result;
    }

    public static com.fs.starfarer.api.campaign.SectorEntityToken resolveIncidentTarget(
            PTSDCrisisState.CrisisIncident incident) {
        if (incident == null || Global.getSector() == null) return null;
        if (incident.targetEntityId != null) {
            com.fs.starfarer.api.campaign.SectorEntityToken entity =
                    Global.getSector().getEntityById(incident.targetEntityId);
            if (entity != null) return entity;
        }
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null && incident.targetMarketId != null) {
            com.fs.starfarer.api.campaign.econ.MarketAPI market = state.resolveMarket(incident.targetMarketId);
            if (market != null && market.getPrimaryEntity() != null) return market.getPrimaryEntity();
        }
        com.fs.starfarer.api.campaign.StarSystemAPI system = state == null ? null : state.resolveSystem(incident.targetSystemId);
        return system == null ? null : system.getHyperspaceAnchor();
    }
    public static String getSystemName(String systemId) {
        PTSDCrisisState state = PTSDCrisisState.get();
        com.fs.starfarer.api.campaign.StarSystemAPI system = state == null ? null : state.resolveSystem(systemId);
        return system == null ? "\u672a\u77e5\u4f4d\u7f6e" : system.getName();
    }
    /** Returns the latest learned Omega attack weight for a system. */
    public static float getAttackWeight(String systemId) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || systemId == null) return 0f;
        return state.getSystemData(systemId).attackWeight;
    }

    /** True when current intelligence classifies an empty system as a future occupation target. */
    public static boolean isOccupationSuggested(String systemId) {
        PTSDCrisisState state = PTSDCrisisState.get();
        return state != null && systemId != null && state.getSystemData(systemId).occupationSuggested;
    }

    /** Separate colonisation value retained for the future full-invasion occupation planner. */
    public static float getOccupationWeight(String systemId) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || systemId == null) return 0f;
        return state.getSystemData(systemId).occupationWeight;
    }
    /** Records a lost Omega engagement as persistent strategic learning. */
    public static void recordOmegaDefeat(String opponentFactionId, boolean playerInvolved,
                                         String systemId, float defeatedStrength) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return;
        String factionId = opponentFactionId;
        if (playerInvolved && Global.getSector().getPlayerFaction() != null) {
            factionId = Global.getSector().getPlayerFaction().getId();
        }
        if (factionId == null) factionId = "unknown";
        float lesson = Math.max(1f, Math.min(15f, 2f + Math.max(0f, defeatedStrength) / 35f));
        Float old = state.factionResistance.get(factionId);
        state.factionResistance.put(factionId, Math.min(100f, (old == null ? 0f : old) + lesson));
        if (playerInvolved || (Global.getSector().getPlayerFaction() != null &&
                factionId.equals(Global.getSector().getPlayerFaction().getId()))) {
            state.playerGrudge = Math.min(100f, state.playerGrudge + lesson * 1.35f);
        }
        PTSDCrisisDevIntel.report("对抗模型更新",
                factionId + " +" + Math.round(lesson * 10f) / 10f +
                        "，记恨值 " + Math.round(state.playerGrudge * 10f) / 10f,
                systemId, null);
    }

    public static float getFactionResistance(String factionId) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || factionId == null) return 0f;
        Float value = state.factionResistance.get(factionId);
        return value == null ? 0f : value;
    }

    public static float getPlayerGrudge() {
        PTSDCrisisState state = PTSDCrisisState.get();
        return state == null ? 0f : state.playerGrudge;
    }

    /**
     * Adds an expiring observation to Omega's belief model. Negative numeric values and -1
     * presence signals mean "not observed". This is the only supported route for teaching a
     * system assessment; callers must not copy ground-truth fields into the belief directly.
     */
    public static PTSDCrisisState.EvidenceRecord recordEvidence(
            String systemId, String sourceType, String sourceId,
            float fleetStrength, float marketDefense, float strategicValue,
            int colonySignal, int fleetSignal, float confidence,
            float lifetimeDays, boolean playerContaminated, float weightBias) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || systemId == null) return null;
        PTSDCrisisState.SystemData data = state.getSystemData(systemId);
        PTSDCrisisState.EvidenceRecord evidence = new PTSDCrisisState.EvidenceRecord();
        evidence.id = "PTSD_evidence_" + Misc.genUID();
        evidence.sourceType = sourceType == null ? "UNKNOWN" : sourceType;
        evidence.sourceId = sourceId;
        evidence.fleetStrength = finiteOrUnknown(fleetStrength);
        evidence.marketDefense = finiteOrUnknown(marketDefense);
        evidence.strategicValue = finiteOrUnknown(strategicValue);
        evidence.colonySignal = clampSignal(colonySignal);
        evidence.fleetSignal = clampSignal(fleetSignal);
        evidence.confidence = clamp01(confidence);
        evidence.weightBias = Math.max(-.75f, Math.min(2f, weightBias));
        evidence.createdDay = PTSDCrisisState.getDay();
        evidence.expiresDay = evidence.createdDay + Math.max(1f, lifetimeDays);
        evidence.playerContaminated = playerContaminated;
        data.evidence.add(evidence);
        while (data.evidence.size() > 64) data.evidence.remove(0);
        refreshBelief(data, evidence.createdDay);
        return evidence;
    }

    /** Adds a durable incident bias which remains an input after attack weights are recomputed. */
    public static void addIncidentWeightBias(String systemId, float additiveMultiplier) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || systemId == null || Float.isNaN(additiveMultiplier) ||
                Float.isInfinite(additiveMultiplier)) return;
        PTSDCrisisState.SystemData data = state.getSystemData(systemId);
        data.incidentWeightBias = Math.max(-.75f,
                Math.min(2f, data.incidentWeightBias + additiveMultiplier));
    }

    /** Rebuilds the current belief exclusively from unexpired evidence. */
    public static void refreshBelief(PTSDCrisisState.SystemData data, float day) {
        if (data == null) return;
        if (data.evidence == null) data.evidence = new ArrayList<PTSDCrisisState.EvidenceRecord>();
        float fleet = 0f, marketTotal = 0f, marketWeight = 0f, valueTotal = 0f, valueWeight = 0f;
        float combinedUncertainty = 1f;
        float colonyScore = 0f, colonyWeight = 0f, fleetScore = 0f, fleetWeight = 0f;
        for (int i = data.evidence.size() - 1; i >= 0; i--) {
            PTSDCrisisState.EvidenceRecord item = data.evidence.get(i);
            if (item == null || item.expiresDay <= day) { data.evidence.remove(i); continue; }
            float life = Math.max(1f, item.expiresDay - item.createdDay);
            float freshness = Math.max(.08f, Math.min(1f, (item.expiresDay - day) / life));
            float trust = clamp01(item.confidence) * freshness * (item.playerContaminated ? .72f : 1f);
            combinedUncertainty *= 1f - Math.min(.95f, trust);
            if (item.fleetStrength >= 0f) fleet = Math.max(fleet, item.fleetStrength * (.55f + .45f * trust));
            if (item.marketDefense >= 0f) { marketTotal += item.marketDefense * trust; marketWeight += trust; }
            if (item.strategicValue >= 0f) { valueTotal += item.strategicValue * trust; valueWeight += trust; }
            if (item.colonySignal >= 0) { colonyScore += item.colonySignal * trust; colonyWeight += trust; }
            if (item.fleetSignal >= 0) { fleetScore += item.fleetSignal * trust; fleetWeight += trust; }
        }
        data.beliefConfidence = clamp01(1f - combinedUncertainty);
        data.observedFleetStrength = fleet;
        data.observedMarketDefense = marketWeight <= .001f ? 0f : marketTotal / marketWeight;
        data.strategicValue = valueWeight <= .001f ? 1f : Math.max(1f, valueTotal / valueWeight);
        data.hasNonCrisisColony = colonyWeight > .08f && colonyScore / colonyWeight >= .5f;
        data.hasNonCrisisFleet = fleetWeight > .08f && fleetScore / fleetWeight >= .5f;
    }

    /**
     * Finds a collision-safe campaign point. It never returns an unchecked fallback: null means
     * the caller should skip or retry spawning the entity.
     */
    public static Vector2f findSafePoint(LocationAPI location, SectorEntityToken focus,
                                         float minRadius, float maxRadius, Random random) {
        if (location == null || focus == null) return null;
        return findSafePoint(location, focus.getLocation(), minRadius, maxRadius, random);
    }

    public static Vector2f findSafePoint(LocationAPI location, Vector2f center,
                                         float minRadius, float maxRadius, Random random) {
        if (location == null || center == null) return null;
        Random rng = random == null ? new Random() : random;
        float min = Math.max(200f, minRadius);
        float max = Math.max(min + 1f, maxRadius);
        for (int attempt = 0; attempt < 64; attempt++) {
            Vector2f point = Misc.getPointAtRadius(center, min + rng.nextFloat() * (max - min));
            if (isSafePoint(location, point)) return point;
        }
        // Deterministic expanding rings make dense systems reliable without accepting an unsafe point.
        for (int ring = 0; ring < 5; ring++) {
            float radius = max + ring * Math.max(1200f, (max - min) * .5f);
            for (int step = 0; step < 24; step++) {
                Vector2f direction = Misc.getUnitVectorAtDegreeAngle(step * 15f + ring * 7.5f);
                direction.scale(radius);
                Vector2f point = Vector2f.add(center, direction, null);
                if (isSafePoint(location, point)) return point;
            }
        }
        return null;
    }

    /** Whether newly disclosed news should create a lower-left Intel notification. */
    public static boolean isNewsIntelSubscribed() {
        PTSDCrisisState state = PTSDCrisisState.get();
        return state == null || state.newsIntelSubscribed;
    }

    /**
     * Changes the per-save news subscription. Articles and their effects continue to exist when
     * unsubscribed; only the lower-left Intel push is suppressed.
     */
    public static void setNewsIntelSubscribed(boolean subscribed) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null) state.newsIntelSubscribed = subscribed;
    }

    public static boolean isSafePoint(LocationAPI location, Vector2f point) {
        if (location == null || point == null) return false;
        if (!(location instanceof StarSystemAPI)) return true;
        StarSystemAPI system = (StarSystemAPI) location;
        for (PlanetAPI planet : system.getPlanets()) {
            if (planet == null) continue;
            float clearance = Math.max(1600f, planet.getRadius() + 1200f);
            if (Misc.getDistance(point, planet.getLocation()) < clearance) return false;
        }
        for (SectorEntityToken jump : system.getJumpPoints()) {
            if (jump == null) continue;
            float clearance = Math.max(1300f, jump.getRadius() + 900f);
            if (Misc.getDistance(point, jump.getLocation()) < clearance) return false;
        }
        return true;
    }

    private static int clampSignal(int value) { return value < 0 ? -1 : (value > 0 ? 1 : 0); }
    private static float clamp01(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return 0f;
        return Math.max(0f, Math.min(1f, value));
    }
    private static float finiteOrUnknown(float value) {
        return Float.isNaN(value) || Float.isInfinite(value) ? -1f : value;
    }

    static void notifyResolved(PTSDCrisisState.StrategicEvent event) {
        PTSDCrisisDevIntel.reportEventResolved(event);
        EventResult result = new EventResult(event);
        for (Map.Entry<String, InterventionProvider> entry :
                new ArrayList<Map.Entry<String, InterventionProvider>>(PROVIDERS.entrySet())) {
            try {
                entry.getValue().reportStrategicEventResolved(result);
            } catch (Throwable ex) {
                Global.getLogger(PTSDCrisisAPI.class).warn("Crisis result provider failed: " + entry.getKey(), ex);
            }
        }
    }
}
