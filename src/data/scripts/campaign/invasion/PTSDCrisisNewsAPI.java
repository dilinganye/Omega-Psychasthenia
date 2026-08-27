package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Public, runtime extension surface for data/config/PTSD_crisis_news.csv.
 *
 * Target expressions:
 * - CUSTOM(fully.qualified.HandlerClass)
 * - CUSTOM(registeredAlias)
 * - FACTION(factionId)
 *
 * Handlers are runtime objects and are intentionally not serialized. Other mods may either put
 * their implementation class directly in the CSV or register an alias on every game load.
 */
public final class PTSDCrisisNewsAPI {
    public interface CustomNewsHandler {
        /** Chooses the system/market and exposes the exact anchor as targetLocation. */
        TargetSelection pick(PickContext context);

        /** Optional global hook, called once per crisis heartbeat after the handler has been loaded. */
        default void advance(AdvanceContext context) { }

        /** Optional creation hook. Returning an entity replaces the incident investigation target. */
        default SectorEntityToken onIncidentCreated(IncidentContext context) { return null; }
    }

    public static final class TargetSelection {
        public final StarSystemAPI system;
        public final MarketAPI market;
        /** Public anchor requested by custom news implementations. */
        public final SectorEntityToken targetLocation;

        public TargetSelection(StarSystemAPI system, MarketAPI market, SectorEntityToken targetLocation) {
            this.system = system;
            this.market = market;
            this.targetLocation = targetLocation;
        }
    }

    public static final class PickContext {
        public final PTSDCrisisState state;
        public final float day;
        public final Random random;
        public final String cardId;
        public final String targetExpression;

        public PickContext(PTSDCrisisState state, float day, Random random,
                           String cardId, String targetExpression) {
            this.state = state;
            this.day = day;
            this.random = random;
            this.cardId = cardId;
            this.targetExpression = targetExpression;
        }
    }

    public static final class AdvanceContext {
        public final PTSDCrisisState state;
        public final float day;
        public final Random random;

        public AdvanceContext(PTSDCrisisState state, float day, Random random) {
            this.state = state;
            this.day = day;
            this.random = random;
        }
    }

    public static final class IncidentContext {
        public final PTSDCrisisState state;
        public final PTSDCrisisState.CrisisIncident incident;
        public final StarSystemAPI system;
        public final MarketAPI market;
        /** Exact anchor selected by pick(); custom creation should occur near this token. */
        public final SectorEntityToken targetLocation;
        public final Random random;

        public IncidentContext(PTSDCrisisState state, PTSDCrisisState.CrisisIncident incident,
                               StarSystemAPI system, MarketAPI market,
                               SectorEntityToken targetLocation, Random random) {
            this.state = state;
            this.incident = incident;
            this.system = system;
            this.market = market;
            this.targetLocation = targetLocation;
            this.random = random;
        }
    }

    private static final Map<String, CustomNewsHandler> HANDLERS =
            new LinkedHashMap<String, CustomNewsHandler>();

    private PTSDCrisisNewsAPI() { }

    public static void registerHandler(String alias, CustomNewsHandler handler) {
        if (alias == null || alias.trim().isEmpty() || handler == null) return;
        HANDLERS.put(alias.trim(), handler);
    }

    public static void unregisterHandler(String alias) {
        if (alias != null) HANDLERS.remove(alias.trim());
    }

    public static CustomNewsHandler resolveHandler(String aliasOrClassName) {
        if (aliasOrClassName == null || aliasOrClassName.trim().isEmpty()) return null;
        String key = aliasOrClassName.trim();
        CustomNewsHandler registered = HANDLERS.get(key);
        if (registered != null) return registered;
        try {
            Class<?> type = Global.getSettings().getScriptClassLoader().loadClass(key);
            Object instance = type.getDeclaredConstructor().newInstance();
            if (!(instance instanceof CustomNewsHandler)) {
                throw new IllegalArgumentException(key + " does not implement PTSDCrisisNewsAPI.CustomNewsHandler");
            }
            registered = (CustomNewsHandler) instance;
            HANDLERS.put(key, registered);
            return registered;
        } catch (Throwable ex) {
            Global.getLogger(PTSDCrisisNewsAPI.class).error("Unable to load custom crisis news handler: " + key, ex);
            return null;
        }
    }

    static void advanceLoadedHandlers(PTSDCrisisState state, float day, Random random) {
        AdvanceContext context = new AdvanceContext(state, day, random);
        for (Map.Entry<String, CustomNewsHandler> entry :
                new LinkedHashMap<String, CustomNewsHandler>(HANDLERS).entrySet()) {
            try {
                entry.getValue().advance(context);
            } catch (Throwable ex) {
                Global.getLogger(PTSDCrisisNewsAPI.class).warn(
                        "Custom crisis news advance failed: " + entry.getKey(), ex);
            }
        }
    }
}