package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Public extension surface for the physical scene reached after a true news investigation. */
public final class PTSDNewsSiteAPI {
    public interface SiteHandler {
        /** Materializes a scene. The returned anchor becomes the player's investigation target. */
        SiteResult materialize(SiteContext context);
        /** Optional persisted-scene heartbeat. Runtime handlers are re-resolved after loading. */
        default void advance(SiteContext context) { }
        /** Called once when the player confirms the scene. */
        default void onConfirmed(SiteContext context) { }
        /** Called before hard expiry or cleanup. */
        default void onExpired(SiteContext context) { }
    }

    public static final class SiteContext {
        public final PTSDCrisisState state;
        public final PTSDCrisisState.CrisisIncident incident;
        public final StarSystemAPI system;
        public final MarketAPI market;
        public final SectorEntityToken targetLocation;
        public final Random random;

        public SiteContext(PTSDCrisisState state, PTSDCrisisState.CrisisIncident incident,
                           StarSystemAPI system, MarketAPI market,
                           SectorEntityToken targetLocation, Random random) {
            this.state = state; this.incident = incident; this.system = system;
            this.market = market; this.targetLocation = targetLocation; this.random = random;
        }
    }

    public static final class SiteResult {
        public final SectorEntityToken anchor;
        public final String title;
        public final String description;
        public final String confirmationHint;
        public final List<String> entityIds;

        public SiteResult(SectorEntityToken anchor, String title, String description,
                          String confirmationHint, List<String> entityIds) {
            this.anchor = anchor; this.title = title; this.description = description;
            this.confirmationHint = confirmationHint;
            this.entityIds = entityIds == null ? new ArrayList<String>() : entityIds;
        }
    }

    private static final Map<String, SiteHandler> HANDLERS = new LinkedHashMap<String, SiteHandler>();
    private PTSDNewsSiteAPI() { }

    public static void registerHandler(String alias, SiteHandler handler) {
        if (alias != null && !alias.trim().isEmpty() && handler != null) HANDLERS.put(alias.trim(), handler);
    }
    public static void unregisterHandler(String alias) { if (alias != null) HANDLERS.remove(alias.trim()); }

    public static SiteHandler resolveHandler(String expression) {
        if (expression == null || expression.trim().isEmpty()) return null;
        String key = unwrap(expression.trim());
        SiteHandler handler = HANDLERS.get(key);
        if (handler != null) return handler;
        try {
            Class<?> type = Global.getSettings().getScriptClassLoader().loadClass(key);
            Object value = type.getDeclaredConstructor().newInstance();
            if (!(value instanceof SiteHandler)) throw new IllegalArgumentException(key + " does not implement SiteHandler");
            handler = (SiteHandler)value; HANDLERS.put(key, handler); return handler;
        } catch (Throwable ex) {
            Global.getLogger(PTSDNewsSiteAPI.class).warn("Unable to load news site handler " + expression, ex);
            return null;
        }
    }

    private static String unwrap(String value) {
        int open = value.indexOf('(');
        if (open > 0 && value.endsWith(")")) {
            String kind = value.substring(0, open).trim();
            if ("CUSTOM".equalsIgnoreCase(kind)) value = value.substring(open + 1, value.length() - 1).trim();
        }
        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'")))) value = value.substring(1, value.length() - 1).trim();
        return value;
    }
}