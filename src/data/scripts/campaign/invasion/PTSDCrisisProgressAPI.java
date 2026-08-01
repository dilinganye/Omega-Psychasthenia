package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Binary-isolated interoperability surface for crisis progress and faction-era changes. */
public final class PTSDCrisisProgressAPI {
    public interface Listener {
        void reportProgressChanged(Change change, Snapshot snapshot);
        void reportPhaseChanged(PTSDCrisisState.Phase previous, PTSDCrisisState.Phase current, Snapshot snapshot);
        void reportFactionEraChanged(PTSDCrisisProgress.Era previous, PTSDCrisisProgress.Era current, Snapshot snapshot);
    }

    public static class BaseListener implements Listener {
        public void reportProgressChanged(Change change, Snapshot snapshot) { }
        public void reportPhaseChanged(PTSDCrisisState.Phase previous, PTSDCrisisState.Phase current, Snapshot snapshot) { }
        public void reportFactionEraChanged(PTSDCrisisProgress.Era previous, PTSDCrisisProgress.Era current, Snapshot snapshot) { }
    }

    public static final class Snapshot {
        public final PTSDCrisisState.Phase phase;
        public final PTSDCrisisProgress.Era era;
        public final String activeFactionId;
        public final String baseSystemId;
        public final float reconConfidence, humanAwareness, watcherAggression, nestDevelopment;
        public final float blockadeDensity, omegaEscalation, humanCohesion, publicPanic, realityDistortion;
        public final float invasionReadiness;

        private Snapshot(PTSDCrisisState state) {
            phase = state.phase;
            era = PTSDCrisisProgress.getEra(state);
            activeFactionId = PTSDCrisisProgress.getActiveFactionId(state);
            baseSystemId = state.baseSystemId;
            reconConfidence = state.reconConfidence;
            humanAwareness = state.humanAwareness;
            watcherAggression = state.watcherAggression;
            nestDevelopment = state.nestDevelopment;
            blockadeDensity = state.blockadeDensity;
            omegaEscalation = state.omegaEscalation;
            humanCohesion = state.humanCohesion;
            publicPanic = state.publicPanic;
            realityDistortion = state.realityDistortion;
            invasionReadiness = PTSDCrisisProgress.getInvasionReadiness(state);
        }

        public float get(PTSDCrisisProgress.Variable variable) {
            switch (variable) {
                case RECON_CONFIDENCE: return reconConfidence;
                case HUMAN_AWARENESS: return humanAwareness;
                case WATCHER_AGGRESSION: return watcherAggression;
                case NEST_DEVELOPMENT: return nestDevelopment;
                case BLOCKADE_DENSITY: return blockadeDensity;
                case OMEGA_ESCALATION: return omegaEscalation;
                case HUMAN_COHESION: return humanCohesion;
                case PUBLIC_PANIC: return publicPanic;
                case REALITY_DISTORTION: return realityDistortion;
                default: return 0f;
            }
        }
    }

    public static final class Change {
        public final PTSDCrisisProgress.Variable variable;
        public final float before, after, delta, day;
        public final String sourceId, systemId;
        private Change(PTSDCrisisProgress.Variable variable, float before, float after,
                       String sourceId, String systemId) {
            this.variable = variable; this.before = before; this.after = after; this.delta = after - before;
            this.sourceId = sourceId; this.systemId = systemId; this.day = PTSDCrisisState.getDay();
        }
    }

    private static final Map<String, Listener> LISTENERS = new LinkedHashMap<String, Listener>();
    private PTSDCrisisProgressAPI() { }

    public static void registerListener(String id, Listener listener) { if (id != null && listener != null) LISTENERS.put(id, listener); }
    public static void unregisterListener(String id) { if (id != null) LISTENERS.remove(id); }

    public static Snapshot getSnapshot() {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return null;
        state.ensureProgressInitialized();
        return new Snapshot(state);
    }

    public static float get(PTSDCrisisProgress.Variable variable) { return PTSDCrisisProgress.get(PTSDCrisisState.get(), variable); }
    public static float add(PTSDCrisisProgress.Variable variable, float amount, String sourceId, String systemId) {
        return PTSDCrisisProgress.add(PTSDCrisisState.get(), variable, amount, sourceId, systemId);
    }
    public static float set(PTSDCrisisProgress.Variable variable, float value, String sourceId, String systemId) {
        PTSDCrisisState state = PTSDCrisisState.get();
        return state == null ? 0f : PTSDCrisisProgress.add(state, variable,
                value - PTSDCrisisProgress.get(state, variable), sourceId, systemId);
    }
    public static float getInvasionReadiness() { return PTSDCrisisProgress.getInvasionReadiness(PTSDCrisisState.get()); }
    public static boolean isReadyForInvasion() { return PTSDCrisisProgress.isReadyForInvasion(PTSDCrisisState.get()); }
    public static PTSDCrisisProgress.Era getEra() { return PTSDCrisisProgress.getEra(PTSDCrisisState.get()); }
    public static String getActiveFactionId() { return PTSDCrisisProgress.getActiveFactionId(PTSDCrisisState.get()); }
    public static boolean isPostInvasionEra() { return getEra() != PTSDCrisisProgress.Era.WATCHER_PRE_INVASION; }

    static void notifyProgressChanged(PTSDCrisisProgress.Variable variable, float before, float after,
                                      String sourceId, String systemId) {
        Change change = new Change(variable, before, after, sourceId, systemId);
        Snapshot snapshot = getSnapshot();
        for (Map.Entry<String, Listener> entry : new ArrayList<Map.Entry<String, Listener>>(LISTENERS.entrySet())) {
            try { entry.getValue().reportProgressChanged(change, snapshot); }
            catch (Throwable ex) { log("Progress listener failed: " + entry.getKey(), ex); }
        }
    }

    static void notifyPhaseChanged(PTSDCrisisState.Phase previous, PTSDCrisisState.Phase current) {
        Snapshot snapshot = getSnapshot();
        PTSDCrisisProgress.Era oldEra = eraForPhase(previous), newEra = eraForPhase(current);
        for (Map.Entry<String, Listener> entry : new ArrayList<Map.Entry<String, Listener>>(LISTENERS.entrySet())) {
            try {
                entry.getValue().reportPhaseChanged(previous, current, snapshot);
                if (oldEra != newEra) entry.getValue().reportFactionEraChanged(oldEra, newEra, snapshot);
            } catch (Throwable ex) { log("Phase listener failed: " + entry.getKey(), ex); }
        }
    }

    private static PTSDCrisisProgress.Era eraForPhase(PTSDCrisisState.Phase phase) {
        if (phase == PTSDCrisisState.Phase.WAR) return PTSDCrisisProgress.Era.PSYCHASTHENIA_POST_INVASION;
        if (phase == PTSDCrisisState.Phase.ENDED) return PTSDCrisisProgress.Era.AFTERMATH;
        return PTSDCrisisProgress.Era.WATCHER_PRE_INVASION;
    }
    private static void log(String message, Throwable ex) {
        if (Global.getLogger(PTSDCrisisProgressAPI.class) != null) Global.getLogger(PTSDCrisisProgressAPI.class).warn(message, ex);
    }
}
