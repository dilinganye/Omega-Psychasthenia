package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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