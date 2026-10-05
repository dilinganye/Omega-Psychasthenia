import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import data.scripts.campaign.invasion.PTSDOmegaEncounterResponse;
import org.lwjgl.util.vector.Vector2f;
import java.lang.reflect.*;
import java.util.*;

/** Exercises the actual advance() retreat branch, including an old save's missing threat ID. */
public final class EncounterResponseRegressionTest {
    interface Calls { Object call(String method, Object[] args); }
    @SuppressWarnings("unchecked")
    private static <T> T mock(Class<T> type, Calls calls) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Object value = calls.call(method.getName(), args);
            if (value != null || !method.getReturnType().isPrimitive()) return value;
            if (method.getReturnType() == boolean.class) return false;
            if (method.getReturnType() == float.class) return 0f;
            if (method.getReturnType() == double.class) return 0d;
            if (method.getReturnType() == long.class) return 0L;
            if (method.getReturnType() == int.class) return 0;
            return null;
        });
    }
    public static void main(String[] args) throws Exception {
        SectorAPI previous = Global.getSector();
        SettingsAPI previousSettings = Global.getSettings();
        try {
            Global.setSettings(mock(SettingsAPI.class, (name, values) -> {
                if (name.equals("getFloat")) return 1f;
                if (name.equals("getColor")) return java.awt.Color.GRAY;
                return null;
            }));
            LocationAPI location = mock(LocationAPI.class, (name, values) ->
                    name.equals("getFleets") ? Collections.emptyList() : null);
            CampaignFleetAPI player = mock(CampaignFleetAPI.class, (name, values) -> {
                if (name.equals("getId")) return "player";
                if (name.equals("getContainingLocation")) return location;
                if (name.equals("getLocation")) return new Vector2f(0f, 0f);
                return null;
            });
            CampaignFleetAPI fleet = mock(CampaignFleetAPI.class, (name, values) -> {
                if (name.equals("isAlive") || name.equals("isVisibleToPlayerFleet")) return true;
                if (name.equals("getContainingLocation")) return location;
                if (name.equals("getFleetPoints")) return 80;
                if (name.equals("getLocation")) return new Vector2f(0f, 0f);
                if (name.equals("despawn")) throw new AssertionError("Visible fleet must not despawn");
                return null;
            });
            SectorEntityToken waypoint = mock(SectorEntityToken.class, (name, values) ->
                    name.equals("getLocation") ? new Vector2f(5000f, 0f) : null);
            CampaignClockAPI clock = mock(CampaignClockAPI.class, (name, values) ->
                    name.equals("convertToDays") ? .2f : null);
            List<String> queried = new ArrayList<String>();
            SectorAPI sector = mock(SectorAPI.class, (name, values) -> {
                if (name.equals("getClock")) return clock;
                if (name.equals("getPlayerFleet")) return player;
                if (name.equals("getEntityById")) {
                    String id = (String) values[0];
                    if (id == null || id.trim().isEmpty()) throw new AssertionError("Invalid ID reached game API");
                    queried.add(id);
                    return id.equals("player") || id.equals("known-threat") ? player : null;
                }
                return null;
            });
            Global.setSector(sector);
            Constructor<PTSDOmegaEncounterResponse> constructor = PTSDOmegaEncounterResponse.class.getDeclaredConstructor(
                    CampaignFleetAPI.class, CampaignFleetAPI.class, boolean.class);
            constructor.setAccessible(true);
            for (String threatId : new String[]{null, "", "  ", "gone-threat", "known-threat"}) {
                queried.clear();
                PTSDOmegaEncounterResponse script = constructor.newInstance(fleet, player, false);
                for (String field : new String[]{"escaping", "escapeWaypoint", "escapeThreatId"}) {
                    Field value = PTSDOmegaEncounterResponse.class.getDeclaredField(field);
                    value.setAccessible(true);
                    value.set(script, field.equals("escaping") ? Boolean.TRUE : field.equals("escapeWaypoint") ? waypoint : threatId);
                }
                script.advance(.2f);
                if (!queried.contains("player")) throw new AssertionError("Player lookup did not run");
                if (threatId != null && !threatId.trim().isEmpty() && !queried.contains(threatId))
                    throw new AssertionError("Valid or stale saved ID must be queried");
            }
            MemoryAPI memory = mock(MemoryAPI.class, (name, values) -> null);
            for (String mode : new String[]{"escaping", "observing", "ambushing", "trap", "combat"}) {
                int[] despawns = {0};
                CampaignFleetAPI hidden = mock(CampaignFleetAPI.class, (name, values) -> {
                    if (name.equals("isAlive")) return true;
                    if (name.equals("getContainingLocation")) return location;
                    if (name.equals("getFleetPoints")) return 20;
                    if (name.equals("getLocation")) return new Vector2f(1000f, 0f);
                    if (name.equals("getMemoryWithoutUpdate")) return memory;
                    if (name.equals("hasTag")) return mode.equals("trap") && values[0].equals("PTSD_late_trap");
                    if (name.equals("despawn")) { despawns[0]++; return null; }
                    return null;
                });
                PTSDOmegaEncounterResponse script = constructor.newInstance(hidden, null, !mode.equals("combat"));
                set(script, "escaping", mode.equals("escaping") || mode.equals("trap") || mode.equals("combat"));
                set(script, "ambushing", mode.equals("ambushing"));
                set(script, "escapeWaypoint", waypoint);
                script.advance(.2f);
                if (despawns[0] != (mode.equals("escaping") ? 1 : 0))
                    throw new AssertionError("Wrong hidden fleet lifecycle: " + mode);
                if (mode.equals("escaping")) {
                    Class<?> reactiveType = Class.forName(
                            "data.scripts.campaign.invasion.PTSDReconPlayerEvents$ReactivePulseScript");
                    Constructor<?> reactiveConstructor = reactiveType.getDeclaredConstructor(
                            CampaignFleetAPI.class, CampaignFleetAPI.class);
                    reactiveConstructor.setAccessible(true);
                    Object reactive = reactiveConstructor.newInstance(hidden, player);
                    set(reactive, "stage", 2);
                    ((com.fs.starfarer.api.EveryFrameScript) reactive).advance(.2f);
                    if (despawns[0] != 2)
                        throw new AssertionError("Reactive retreat stayed alive outside sensor visibility");
                }
            }
            System.out.println("EncounterResponseRegressionTest: passed (saved IDs, hidden survey/reactive retreat, observation, ambush, trap, combat)");
        } finally { Global.setSector(previous); Global.setSettings(previousSettings); }
    }
    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
