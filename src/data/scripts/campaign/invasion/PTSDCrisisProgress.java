package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;

/**
 * Authoritative continuous progression model for the crisis. Values use a 0..100 scale.
 * Event implementations should contribute through PTSDCrisisAPI instead of editing fields directly.
 */
public final class PTSDCrisisProgress {
    public enum Variable {
        RECON_CONFIDENCE,
        HUMAN_AWARENESS,
        WATCHER_AGGRESSION,
        NEST_DEVELOPMENT,
        BLOCKADE_DENSITY,
        OMEGA_ESCALATION,
        HUMAN_COHESION,
        PUBLIC_PANIC,
        REALITY_DISTORTION
    }

    public enum Era {
        WATCHER_PRE_INVASION,
        PSYCHASTHENIA_POST_INVASION,
        AFTERMATH
    }

    public static final float INVASION_READINESS_THRESHOLD = 55f;

    private PTSDCrisisProgress() { }

    public static void advance(PTSDCrisisState state, float now) {
        if (state == null) return;
        state.ensureProgressInitialized();
        if (state.lastProgressUpdateDay <= 0f) state.lastProgressUpdateDay = now;
        float days = Math.max(0f, Math.min(30f, now - state.lastProgressUpdateDay));
        state.lastProgressUpdateDay = now;
        if (days <= 0f) return;

        switch (state.phase) {
            case DORMANT:
                addRaw(state, Variable.RECON_CONFIDENCE, 0.02f * days);
                break;
            case RECON:
                addRaw(state, Variable.RECON_CONFIDENCE, 0.35f * days);
                addRaw(state, Variable.WATCHER_AGGRESSION, 0.05f * days);
                break;
            case EXPANSION:
                addRaw(state, Variable.RECON_CONFIDENCE, 0.08f * days);
                addRaw(state, Variable.WATCHER_AGGRESSION, 0.08f * days);
                addRaw(state, Variable.NEST_DEVELOPMENT, 0.85f * days);
                addRaw(state, Variable.REALITY_DISTORTION, 0.08f * days);
                break;
            case FORTIFICATION:
                addRaw(state, Variable.WATCHER_AGGRESSION, 0.12f * days);
                addRaw(state, Variable.NEST_DEVELOPMENT, 0.45f * days);
                addRaw(state, Variable.BLOCKADE_DENSITY, 1.15f * days);
                addRaw(state, Variable.REALITY_DISTORTION, 0.12f * days);
                break;
            case WAR:
                addRaw(state, Variable.OMEGA_ESCALATION, 0.30f * days);
                addRaw(state, Variable.BLOCKADE_DENSITY, 0.06f * days);
                addRaw(state, Variable.HUMAN_COHESION, 0.04f * days);
                addRaw(state, Variable.PUBLIC_PANIC, 0.05f * days);
                addRaw(state, Variable.REALITY_DISTORTION, 0.10f * days);
                break;
            case ENDED:
                addRaw(state, Variable.PUBLIC_PANIC, -0.12f * days);
                addRaw(state, Variable.HUMAN_COHESION, -0.03f * days);
                break;
        }
        if (state.phase != PTSDCrisisState.Phase.WAR) {
            addRaw(state, Variable.PUBLIC_PANIC, -0.04f * days);
        }
    }

    public static void onPhaseChanged(PTSDCrisisState state, PTSDCrisisState.Phase previous,
                                      PTSDCrisisState.Phase next) {
        if (state == null) return;
        state.ensureProgressInitialized();
        state.applyProgressFloors(next);
        state.lastProgressUpdateDay = PTSDCrisisState.getDay();
        PTSDCrisisProgressAPI.notifyPhaseChanged(previous, next);
    }

    public static float get(PTSDCrisisState state, Variable variable) {
        if (state == null || variable == null) return 0f;
        state.ensureProgressInitialized();
        switch (variable) {
            case RECON_CONFIDENCE: return state.reconConfidence;
            case HUMAN_AWARENESS: return state.humanAwareness;
            case WATCHER_AGGRESSION: return state.watcherAggression;
            case NEST_DEVELOPMENT: return state.nestDevelopment;
            case BLOCKADE_DENSITY: return state.blockadeDensity;
            case OMEGA_ESCALATION: return state.omegaEscalation;
            case HUMAN_COHESION: return state.humanCohesion;
            case PUBLIC_PANIC: return state.publicPanic;
            case REALITY_DISTORTION: return state.realityDistortion;
            default: return 0f;
        }
    }

    public static float add(PTSDCrisisState state, Variable variable, float amount,
                            String sourceId, String systemId) {
        if (state == null || variable == null || Float.isNaN(amount) || Float.isInfinite(amount)) return 0f;
        float before = get(state, variable);
        addRaw(state, variable, amount);
        float after = get(state, variable);
        float applied = after - before;
        if (Math.abs(applied) >= 0.001f) {
            state.lastProgressSource = sourceId;
            state.lastProgressSystemId = systemId;
            state.lastProgressChangeDay = PTSDCrisisState.getDay();
            PTSDCrisisProgressAPI.notifyProgressChanged(variable, before, after, sourceId, systemId);
            if (Global.getSettings().isDevMode()) {
                PTSDCrisisDevIntel.report("危机进度变化",
                        variable.name() + " " + signed(applied) + " → " + Math.round(after) +
                                (sourceId == null ? "" : " / " + sourceId), systemId, null);
            }
        }
        return applied;
    }

    private static void addRaw(PTSDCrisisState state, Variable variable, float amount) {
        switch (variable) {
            case RECON_CONFIDENCE: state.reconConfidence = clamp(state.reconConfidence + amount); break;
            case HUMAN_AWARENESS: state.humanAwareness = clamp(state.humanAwareness + amount); break;
            case WATCHER_AGGRESSION: state.watcherAggression = clamp(state.watcherAggression + amount); break;
            case NEST_DEVELOPMENT: state.nestDevelopment = clamp(state.nestDevelopment + amount); break;
            case BLOCKADE_DENSITY: state.blockadeDensity = clamp(state.blockadeDensity + amount); break;
            case OMEGA_ESCALATION: state.omegaEscalation = clamp(state.omegaEscalation + amount); break;
            case HUMAN_COHESION: state.humanCohesion = clamp(state.humanCohesion + amount); break;
            case PUBLIC_PANIC: state.publicPanic = clamp(state.publicPanic + amount); break;
            case REALITY_DISTORTION: state.realityDistortion = clamp(state.realityDistortion + amount); break;
        }
    }

    public static float getInvasionReadiness(PTSDCrisisState state) {
        if (state == null) return 0f;
        return clamp(get(state, Variable.RECON_CONFIDENCE) * 0.30f +
                get(state, Variable.NEST_DEVELOPMENT) * 0.35f +
                get(state, Variable.BLOCKADE_DENSITY) * 0.35f);
    }

    public static boolean isReadyForInvasion(PTSDCrisisState state) {
        return getInvasionReadiness(state) >= INVASION_READINESS_THRESHOLD;
    }

    public static Era getEra(PTSDCrisisState state) {
        if (state == null || state.phase == PTSDCrisisState.Phase.DORMANT ||
                state.phase == PTSDCrisisState.Phase.RECON || state.phase == PTSDCrisisState.Phase.EXPANSION ||
                state.phase == PTSDCrisisState.Phase.FORTIFICATION) return Era.WATCHER_PRE_INVASION;
        if (state.phase == PTSDCrisisState.Phase.ENDED) return Era.AFTERMATH;
        return Era.PSYCHASTHENIA_POST_INVASION;
    }

    public static String getActiveFactionId(PTSDCrisisState state) {
        return getEra(state) == Era.WATCHER_PRE_INVASION
                ? IIRT_Omega_Invasion.WATCHER_FACTION : IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION;
    }

    private static float clamp(float value) { return Math.max(0f, Math.min(100f, value)); }
    private static String signed(float value) { return (value >= 0f ? "+" : "") + Math.round(value * 10f) / 10f; }
}
