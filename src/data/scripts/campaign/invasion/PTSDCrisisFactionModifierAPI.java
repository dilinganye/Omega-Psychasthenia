package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;

import java.util.LinkedHashMap;
import java.util.Map;

/** Optional-mod hook for the first-wave faction-specific resistance and targeting. */
public final class PTSDCrisisFactionModifierAPI {
    public interface Modifier {
        Effects modify(String factionId, String systemId, Effects base);
    }

    public static final class Effects {
        public boolean immune;
        public float targetWeight = 1f;
        public float omegaStrength = 1f;
        public float humanStrength = 1f;

        public Effects copy() {
            Effects result = new Effects();
            result.immune = immune;
            result.targetWeight = targetWeight;
            result.omegaStrength = omegaStrength;
            result.humanStrength = humanStrength;
            return result;
        }
    }

    private static final Map<String, Modifier> MODIFIERS = new LinkedHashMap<String, Modifier>();

    private PTSDCrisisFactionModifierAPI() { }

    public static synchronized void register(String factionId, Modifier modifier) {
        if (factionId == null || factionId.isEmpty() || modifier == null) return;
        MODIFIERS.put(factionId, modifier);
    }

    public static synchronized void unregister(String factionId) {
        MODIFIERS.remove(factionId);
    }

    public static Effects get(String factionId, String systemId) {
        Effects result = new Effects();
        if (factionId == null) return result;
        if ("Gamma_Legion".equals(factionId)) result.immune = true;
        if ("SGB".equals(factionId)) {
            result.targetWeight = 2f;
            result.omegaStrength = 1.75f;
            result.humanStrength = 1.75f;
        }
        Modifier modifier;
        synchronized (PTSDCrisisFactionModifierAPI.class) {
            modifier = MODIFIERS.get(factionId);
        }
        if (modifier != null) {
            try {
                Effects candidate = modifier.modify(factionId, systemId, result.copy());
                if (candidate != null) result = candidate;
            } catch (Throwable ex) {
                Global.getLogger(PTSDCrisisFactionModifierAPI.class).warn(
                        "First-wave faction modifier failed for " + factionId, ex);
            }
        }
        if ("Gamma_Legion".equals(factionId)) result.immune = true;
        result.targetWeight = clamp(result.targetWeight, 0f, 5f);
        result.omegaStrength = clamp(result.omegaStrength, .25f, 5f);
        result.humanStrength = clamp(result.humanStrength, .25f, 5f);
        return result;
    }

    private static float clamp(float value, float min, float max) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return 1f;
        return Math.max(min, Math.min(max, value));
    }
}
