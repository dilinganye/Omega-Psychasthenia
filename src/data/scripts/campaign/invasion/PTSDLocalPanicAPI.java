package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public local-panic surface. Colony records live inside the corresponding SystemData so panic,
 * reconnaissance and invasion weights remain part of the same strategic information table.
 */
public final class PTSDLocalPanicAPI {
    public static final float NEWS_RADIUS = 18000f;

    private PTSDLocalPanicAPI() { }

    public static float getGlobalPanic() {
        PTSDCrisisState state = PTSDCrisisState.get();
        return state == null ? 0f : state.globalPanic;
    }

    public static float getMarketPanic(MarketAPI market) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || market == null || market.getStarSystem() == null) return 0f;
        PTSDCrisisState.ColonyPanicData data =
                state.getColonyPanic(market.getStarSystem().getId(), market.getId());
        return clamp(data.eventPanic + data.proximityPanic + state.globalPanic);
    }

    public static float getSystemPanic(String systemId) {
        PTSDCrisisState state = PTSDCrisisState.get();
        PTSDCrisisState.SystemData data = state == null ? null : state.getSystemData(systemId);
        return data == null ? 0f : clamp(data.systemPanic + state.globalPanic);
    }

    public static float addAtMarket(String marketId, float amount, String sourceId) {
        PTSDCrisisState state = PTSDCrisisState.get();
        MarketAPI market = state == null ? null : state.resolveMarket(marketId);
        if (state == null || market == null || market.getStarSystem() == null) return 0f;
        return addRaw(state, market, amount, sourceId);
    }

    public static Map<String, Float> spreadFromSystem(String sourceSystemId, float amount,
                                                      float radius, String sourceId) {
        PTSDCrisisState state = PTSDCrisisState.get();
        Map<String, Float> applied = new LinkedHashMap<String, Float>();
        StarSystemAPI source = state == null ? null : state.resolveSystem(sourceSystemId);
        if (state == null || source == null || Math.abs(amount) < .001f) return applied;
        float effectiveRadius = Math.max(1000f, radius);
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (!isEligible(market)) continue;
            StarSystemAPI target = market.getStarSystem();
            float distance = Misc.getDistance(source.getLocation(), target.getLocation());
            if (distance > effectiveRadius) continue;
            float falloff = 1f - distance / effectiveRadius;
            float localAmount = amount * (.15f + .85f * falloff * falloff);
            float delta = addRaw(state, market, localAmount, sourceId);
            if (Math.abs(delta) >= .001f) applied.put(market.getId(), delta);
        }
        return applied;
    }

    public static float mitigateIncident(PTSDCrisisState.CrisisIncident incident,
                                         float remainingRatio, String sourceId) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || incident == null || incident.panicByMarket == null) return 0f;
        float ratio = Math.max(.05f, Math.min(.75f, remainingRatio));
        float previousRatio = incident.panicMitigationRatio <= 0f ? 1f : incident.panicMitigationRatio;
        if (ratio >= previousRatio) return 0f;
        float removed = 0f;
        float relativeRemoval = (previousRatio - ratio) / previousRatio;
        for (Map.Entry<String, Float> entry : incident.panicByMarket.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0f) continue;
            MarketAPI market = state.resolveMarket(entry.getKey());
            if (!isEligible(market)) continue;
            float delta = entry.getValue() * relativeRemoval;
            float applied = addRaw(state, market, -delta, sourceId);
            removed += Math.max(0f, -applied);
            entry.setValue(Math.max(0f, entry.getValue() - delta));
        }
        incident.panicMitigationRatio = ratio;
        return removed;
    }

    static void updateProximityAndDecay(PTSDCrisisState state, float day) {
        if (state == null) return;
        float days = state.lastLocalPanicUpdateDay <= 0f ? 0f :
                Math.max(0f, Math.min(30f, day - state.lastLocalPanicUpdateDay));
        state.lastLocalPanicUpdateDay = day;

        for (PTSDCrisisState.SystemData systemData : state.systems.values()) {
            if (systemData != null) systemData.systemPanic = 0f;
        }

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (!isEligible(market)) continue;
            StarSystemAPI system = market.getStarSystem();
            PTSDCrisisState.ColonyPanicData data =
                    state.getColonyPanic(system.getId(), market.getId());
            if (days > 0f && day - data.lastChangedDay > 20f) {
                data.eventPanic = Math.max(0f, data.eventPanic - .035f * days);
            }
            data.proximityPanic = computeProximityPanic(state, system);
            float effective = clamp(data.eventPanic + data.proximityPanic);
            PTSDCrisisState.SystemData systemData = state.getSystemData(system.getId());
            systemData.systemPanic = Math.max(systemData.systemPanic, effective);
        }
        state.publicPanic = state.globalPanic;
    }

    private static float computeProximityPanic(PTSDCrisisState state, StarSystemAPI target) {
        float phaseMaximum;
        switch (state.phase) {
            case RECON: phaseMaximum = 4f; break;
            case EXPANSION: phaseMaximum = 24f; break;
            case FORTIFICATION: phaseMaximum = 42f; break;
            case WAR: phaseMaximum = 68f; break;
            case ENDED: phaseMaximum = 18f; break;
            default: phaseMaximum = 0f;
        }
        if (phaseMaximum <= 0f || target == null) return 0f;

        float nearest = Float.MAX_VALUE;
        for (PTSDCrisisState.SystemData data : state.systems.values()) {
            if (data == null || (data.omegaControl <= 0f && data.conversionLevel <= 0 &&
                    !data.systemId.equals(state.baseSystemId))) continue;
            StarSystemAPI crisis = state.resolveSystem(data.systemId);
            if (crisis != null) nearest = Math.min(nearest,
                    Misc.getDistance(target.getLocation(), crisis.getLocation()));
        }
        StarSystemAPI base = state.resolveSystem(state.baseSystemId);
        if (base != null) nearest = Math.min(nearest,
                Misc.getDistance(target.getLocation(), base.getLocation()));
        if (nearest == Float.MAX_VALUE) return 0f;
        float falloff = Math.max(0f, 1f - nearest / 52000f);
        return phaseMaximum * falloff * falloff;
    }

    private static float addRaw(PTSDCrisisState state, MarketAPI market,
                                float amount, String sourceId) {
        PTSDCrisisState.ColonyPanicData data =
                state.getColonyPanic(market.getStarSystem().getId(), market.getId());
        float before = data.eventPanic;
        data.eventPanic = clamp(data.eventPanic + amount);
        float applied = data.eventPanic - before;
        if (Math.abs(applied) >= .001f) {
            data.lastChangedDay = PTSDCrisisState.getDay();
            data.lastSource = sourceId;
            if (Global.getSettings().isDevMode()) {
                PTSDCrisisDevIntel.report("局部恐慌变化",
                        market.getName() + " " + signed(applied) + " → " +
                                Math.round(getMarketPanic(market)) + " / " + sourceId,
                        market.getStarSystem().getId(), market.getPrimaryEntity() == null ?
                                null : market.getPrimaryEntity().getId());
            }
        }
        return applied;
    }

    private static boolean isEligible(MarketAPI market) {
        if (market == null || market.isPlanetConditionMarketOnly() ||
                market.getStarSystem() == null || market.getId() == null) return false;
        String faction = market.getFactionId();
        return !IIRT_Omega_Invasion.WATCHER_FACTION.equals(faction) &&
                !IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(faction);
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(100f, value));
    }

    private static String signed(float value) {
        return (value >= 0f ? "+" : "") + Math.round(value * 10f) / 10f;
    }
}
