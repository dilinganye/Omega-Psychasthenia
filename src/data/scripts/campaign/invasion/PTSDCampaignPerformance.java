package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import java.util.LinkedHashMap;
import java.util.Map;

/** Opt-in developer profiling. No timers or allocations while disabled. */
public final class PTSDCampaignPerformance {
    private static boolean enabled;
    private static long lastSummary;
    private static final Map<String, long[]> samples = new LinkedHashMap<String, long[]>();
    private PTSDCampaignPerformance() { }
    public static boolean isEnabled() { return enabled; }
    public static void reset() { enabled = false; samples.clear(); lastSummary = 0L; }
    public static void toggle() {
        if (Global.getSettings() == null || !Global.getSettings().isDevMode()) { reset(); return; }
        enabled = !enabled; samples.clear(); lastSummary = System.nanoTime();
    }
    public static long begin() {
        if (!enabled) return 0L;
        if (Global.getSettings() == null || !Global.getSettings().isDevMode()) { reset(); return 0L; }
        return System.nanoTime();
    }
    public static void end(String name, long start) {
        if (start == 0L || !enabled) return;
        long now = System.nanoTime(), elapsed = Math.max(0L, now - start);
        long[] value = samples.get(name);
        if (value == null) { value = new long[3]; samples.put(name, value); }
        value[0]++; value[1] += elapsed; value[2] = Math.max(value[2], elapsed);
        if (now - lastSummary < 15000000000L) return;
        StringBuilder out = new StringBuilder("PTSD performance / 15 real seconds");
        for (Map.Entry<String, long[]> entry : samples.entrySet()) {
            long[] s = entry.getValue();
            out.append(" | ").append(entry.getKey()).append(": calls=").append(s[0])
                    .append(", totalMs=").append(s[1] / 1000000d)
                    .append(", meanMs=").append(s[1] / (1000000d * s[0]))
                    .append(", maxMs=").append(s[2] / 1000000d);
        }
        Global.getLogger(PTSDCampaignPerformance.class).info(out.toString());
        samples.clear(); lastSummary = now;
    }
}
