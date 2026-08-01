package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;

import static data.scripts.IIRT_Omega_ModPlugin.omega_fleet_strength_multiplier;
import static data.scripts.IIRT_Omega_ModPlugin.unknown_event_strength_flat;

/** Single balancing entry point for every physical Psychasthenia fleet. */
public final class PTSDOmegaFleetScaling {
    public static final String BASE_FP_MEMORY = "$PTSD_base_fleet_fp";
    public static final String FINAL_FP_MEMORY = "$PTSD_scaled_fleet_fp";
    public static final String FLAT_FP_MEMORY = "$PTSD_dynamic_flat_fp";
    public static final String SEVERITY_MEMORY = "$PTSD_event_severity";

    private PTSDOmegaFleetScaling() { }

    /** Severity is normally 0..1; exceptional endgame encounters may deliberately exceed 1. */
    public static float scale(float baseFp, float severity) {
        float flat = getDynamicFlat();
        float severityMult = 1f + Math.max(0f, severity) * 0.65f;
        return Math.max(5f, baseFp * clamp(omega_fleet_strength_multiplier, 0.5f, 10f) * severityMult + flat);
    }

    public static float severityFor(PTSDCrisisState.EventType type, float strategicStrength) {
        float result = Math.min(1.5f, Math.max(0f, strategicStrength) / 250f);
        if (type == PTSDCrisisState.EventType.GARRISON) result += 0.2f;
        if (type == PTSDCrisisState.EventType.FORTRESS_PATROL) result += 0.45f;
        if (type == PTSDCrisisState.EventType.ATTACK) result += 0.3f;
        if (type == PTSDCrisisState.EventType.FIRE_PROBE) result += 0.1f;
        return result;
    }

    /** Progress is permanent; recent hostile player activity fades over roughly 120 campaign days. */
    public static float getDynamicFlat() {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return 0f;
        float progress;
        switch (state.phase) {
            case RECON: progress = 15f; break;
            case EXPANSION: progress = 45f; break;
            case FORTIFICATION: progress = 85f; break;
            case WAR: progress = 125f; break;
            case ENDED: progress = 150f; break;
            default: progress = 0f;
        }
        progress = Math.max(progress, state.omegaEscalation * 0.65f + state.nestDevelopment * 0.25f);
        float recent = 0f;
        float now = PTSDCrisisState.getDay();
        if (state.occupations != null) {
            for (PTSDCrisisState.OccupationData data : state.occupations.values()) {
                if (data == null) continue;
                float age = Math.max(0f, now - data.lastInteractionDay);
                float freshness = Math.max(0f, 1f - age / 120f);
                float actions = data.harassmentBombardments * 8f + data.saturationBombardments * 24f +
                        data.probes * 6f + data.negotiations * 4f + data.defenseVictories * 10f;
                recent += (actions + Math.max(0f, data.omegaAttention) * 7f) * freshness;
            }
        }
        recent += Math.min(30f, state.totalOmegaEncounters * 2f);
        return clamp(progress + recent, 0f, 200f) + clamp(unknown_event_strength_flat, 0f, 200f);
    }

    public static void record(com.fs.starfarer.api.campaign.CampaignFleetAPI fleet,
                              float baseFp, float finalFp, float severity) {
        if (fleet == null) return;
        fleet.getMemoryWithoutUpdate().set(BASE_FP_MEMORY, baseFp);
        fleet.getMemoryWithoutUpdate().set(FINAL_FP_MEMORY, finalFp);
        fleet.getMemoryWithoutUpdate().set(FLAT_FP_MEMORY, getDynamicFlat());
        fleet.getMemoryWithoutUpdate().set(SEVERITY_MEMORY, severity);
        if (Global.getSettings().isDevMode()) {
            PTSDCrisisDevIntel.report("精神创伤舰队强度",
                    "基础 " + Math.round(baseFp) + " / 倍率 " + omega_fleet_strength_multiplier +
                            " / Flat +" + Math.round(getDynamicFlat()) + "（设置额外 +" + Math.round(unknown_event_strength_flat) + "） / 严重度 " + severity +
                            " / 最终 " + Math.round(finalFp),
                    fleet.getStarSystem() == null ? null : fleet.getStarSystem().getId(), fleet.getId());
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
