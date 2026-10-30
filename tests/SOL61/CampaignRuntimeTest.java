import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import data.scripts.campaign.invasion.*;
import java.lang.reflect.*;
import java.util.*;

/** Repeated state reads, reload repair and cache invalidation without a running game. */
public final class CampaignRuntimeTest {
    interface Calls { Object call(String name, Object[] args); }
    @SuppressWarnings("unchecked")
    private static <T> T mock(Class<T> type, Calls calls) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (object, method, args) -> {
            if (method.getName().equals("equals")) return object == args[0];
            if (method.getName().equals("hashCode")) return System.identityHashCode(object);
            Object value = calls.call(method.getName(), args);
            if (value != null || !method.getReturnType().isPrimitive()) return value;
            if (method.getReturnType() == boolean.class) return false;
            if (method.getReturnType() == float.class) return 0f;
            if (method.getReturnType() == long.class) return 0L;
            if (method.getReturnType() == int.class) return 0;
            return null;
        });
    }
    static class CountedIncidents extends ArrayList<PTSDCrisisState.CrisisIncident> {
        int traversals;
        @Override public Iterator<PTSDCrisisState.CrisisIncident> iterator() { traversals++; return super.iterator(); }
    }
    private static void check(boolean condition, String why) { if (!condition) throw new AssertionError(why); }
    public static void main(String[] args) throws Exception {
        SectorAPI before = Global.getSector(); SettingsAPI beforeSettings = Global.getSettings();
        try {
            Global.setSettings(mock(SettingsAPI.class, (name, a) -> {
                if (name.equals("getFloat")) return 1f;
                if (name.equals("getColor")) return java.awt.Color.GRAY;
                return null;
            }));
            long[] time = {0L};
            CampaignClockAPI clock = mock(CampaignClockAPI.class, (name, a) -> name.equals("getTimestamp") ? time[0] : null);
            int[] localReads = {0}, remoteReads = {0};
            List<CampaignFleetAPI> localValues = new ArrayList<CampaignFleetAPI>();
            List<CampaignFleetAPI> remoteValues = new ArrayList<CampaignFleetAPI>();
            LocationAPI local = mock(LocationAPI.class, (name, a) -> {
                if (name.equals("getFleets")) { localReads[0]++; return localValues; } return null;
            });
            LocationAPI remote = mock(LocationAPI.class, (name, a) -> {
                if (name.equals("getFleets")) { remoteReads[0]++; return remoteValues; } return null;
            });
            LocationAPI[] playerPlace = {local};
            CampaignFleetAPI player = mock(CampaignFleetAPI.class, (name, a) ->
                    name.equals("getContainingLocation") ? playerPlace[0] : null);
            Map<String,Object> persistent = new HashMap<String,Object>();
            int[] audited = {0};
            List<LocationAPI> locations = new ArrayList<LocationAPI>();
            for (int i=0; i<20; i++) locations.add(mock(LocationAPI.class, (name,a) -> {
                if (name.equals("getFleets")) { audited[0]++; return Collections.emptyList(); }
                if (name.equals("getAllEntities")) return Collections.emptyList();
                return null;
            }));
            EconomyAPI economy = mock(EconomyAPI.class, (name,a) ->
                    name.equals("getMarketsCopy") ? Collections.emptyList() : null);
            Global.setSector(mock(SectorAPI.class, (name, a) -> {
                if (name.equals("getPersistentData")) return persistent;
                if (name.equals("getClock")) return clock;
                if (name.equals("getPlayerFleet")) return player;
                if (name.equals("getAllLocations")) return locations;
                if (name.equals("getStarSystems")) return Collections.emptyList();
                if (name.equals("getEconomy")) return economy;
                return null;
            }));
            Constructor<PTSDCrisisState> constructor = PTSDCrisisState.class.getDeclaredConstructor(); constructor.setAccessible(true);
            PTSDCrisisState state = constructor.newInstance(); state.progressInitialized = true; state.version = 16;
            PTSDCrisisState.CrisisIncident legacy = new PTSDCrisisState.CrisisIncident();
            check(legacy.investigationResult == PTSDTaskRules.InvestigationResult.PENDING,
                    "New incident depends on a later migration");
            legacy.investigationResult = null; // Simulate a missing old-save field.
            CountedIncidents incidents = new CountedIncidents(); incidents.add(legacy);
            state.incidents = incidents; state.incidentCooldowns.put("keep", 123f);
            persistent.put(PTSDCrisisState.PERSISTENT_KEY, state);
            PTSDCrisisState.get();
            check(incidents.get(0).investigationResult != null, "Old incident not migrated");
            int repaired = incidents.traversals;
            for (int i=0; i<1000; i++) check(PTSDCrisisState.get() == state, "State identity changed");
            check(incidents.traversals == repaired, "Repeated getters still deep-repair state");
            state.invalidateRuntime(); state.ensureRuntimeReady();
            check(incidents.traversals == repaired + 1, "Reload did not repair exactly once");
            check(state.incidentCooldowns.get("keep") == 123f, "Cooldown reset on reload");
            Method conditionalTarget = PTSDConditionalNewsDirector.class.getDeclaredMethod(
                    "pickTarget", PTSDCrisisState.class, String.class, String.class, Random.class);
            conditionalTarget.setAccessible(true);
            for (String trigger : new String[]{"FORT_MEDIUM", "FORT_MOVING", "TRANSPORT", "REPORT"})
                check(conditionalTarget.invoke(null, state, trigger, "", new Random(1)) == null,
                        "Conditional wait manufactured a target: " + trigger);
            check(state.conditionalNewsSeen.isEmpty() && state.incidents.size() == 1,
                    "Missing conditional target consumed news or altered history");
            PTSDCrisisState.PendingReconReport daily = new PTSDCrisisState.PendingReconReport();
            daily.sourceFleetId = "missing-scout"; daily.targetSystemId = "observed";
            daily.sourceGroup = "missing-scout:daily:7"; daily.kind = "SCOUT_DAILY_MAX";
            daily.uploadDay = 20f; daily.fleetStrength = 40f;
            state.pendingReconReports.add(daily);
            PTSDReconReportDirector.queueDailyMaximum(state, "observed", "missing-scout", 80f, 7);
            PTSDReconReportDirector.queueDailyMaximum(state, "observed", "missing-scout", 15f, 7);
            check(state.pendingReconReports.size() == 1 && daily.fleetStrength == 80f && daily.uploadDay == 20f,
                    "Daily maximum duplicated a report, shrank a sample or changed its deadline");
            PTSDReconReportDirector.advance(state, 19f);
            check(state.pendingReconReports.size() == 1 && !daily.uploaded, "Report uploaded before its deadline");
            PTSDReconReportDirector.advance(state, 20f);
            PTSDReconReportDirector.advance(state, 21f);
            check(state.pendingReconReports.isEmpty() && !daily.uploaded && !state.systems.containsKey("observed"),
                    "Lost scout manufactured evidence or repeatedly uploaded");
            PTSDCampaignRuntime.reset();
            for (int i=0; i<10; i++) {
                time[0] = (long)(i * .5d * 86400000d);
                PTSDCampaignRuntime.audit(state, PTSDCrisisState.getDay());
                check(audited[0] == (i+1)*2, "Compatibility audit not split into ten batches");
                PTSDCampaignRuntime.audit(state, PTSDCrisisState.getDay());
                check(audited[0] == (i+1)*2, "Audit repeated within half-day gate");
            }
            List<com.fs.starfarer.api.campaign.listeners.FleetEventListener> listeners = new ArrayList<>();
            FactionAPI faction = mock(FactionAPI.class, (name,a) ->
                    name.equals("getId") ? IIRT_Omega_Invasion.WATCHER_FACTION : null);
            CampaignFleetAPI asset = mock(CampaignFleetAPI.class, (name,a) -> {
                if (name.equals("isAlive")) return true;
                if (name.equals("getFaction")) return faction;
                if (name.equals("getEventListeners")) return listeners;
                if (name.equals("getFleetPoints")) return 20;
                return null;
            });
            PTSDCampaignRuntime.normalizeFleet(state, asset); PTSDCampaignRuntime.normalizeFleet(state, asset);
            check(listeners.size() == 1, "Strategic listener installed twice");
            listeners.clear(); listeners.add(new PTSDStrategicFleetListener("specific-event", 20f));
            PTSDCampaignRuntime.normalizeFleet(state, asset);
            check(listeners.size() == 1, "Specific event listener duplicated by generic listener");
            PTSDCampaignRuntime.reset();
            List<CampaignFleetAPI> first = PTSDCampaignRuntime.fleets(local);
            for (int i=0;i<100;i++) check(PTSDCampaignRuntime.fleets(local) == first, "Local candidates recopied");
            check(localReads[0] == 1, "Local fleet list repeatedly enumerated");
            time[0] += 10800000L; // .125 game day, exceeds the local .1-day lifetime.
            PTSDCampaignRuntime.fleets(local); check(localReads[0] == 2, "Local cache did not expire");
            CampaignFleetAPI added = mock(CampaignFleetAPI.class, (name,a) -> name.equals("getContainingLocation") ? local : null);
            localValues.add(added); PTSDCampaignRuntime.entityAdded(added);
            check(PTSDCampaignRuntime.fleets(local).contains(added) && localReads[0] == 3, "Spawn not immediately visible to scans");
            PTSDCampaignRuntime.leaving(added); localValues.remove(added);
            check(!PTSDCampaignRuntime.fleets(local).contains(added), "Removed fleet remained in candidates");
            List<CampaignFleetAPI> view = PTSDCampaignRuntime.fleets(local);
            localValues.add(added);
            check(view.contains(added), "External spawn hidden until cache timeout");
            localValues.remove(added);
            check(!view.contains(added), "External removal hidden until cache timeout");
            List<CampaignFleetAPI> distant = PTSDCampaignRuntime.fleets(remote);
            time[0] += 32400000L; // .375 day
            check(PTSDCampaignRuntime.fleets(remote) == distant, "Remote cache expired early");
            time[0] += 21600000L; // .25 day
            check(PTSDCampaignRuntime.fleets(remote) != distant, "Remote cache did not expire");
            int reads = remoteReads[0]; playerPlace[0] = remote;
            PTSDCampaignRuntime.fleets(remote); check(remoteReads[0] == reads+1, "Player entry did not invalidate remote cache");
            PTSDCampaignRuntime.reset();
            check(PTSDCampaignPerformance.begin() == 0L, "Profiling runs while disabled");
            System.out.println("CampaignRuntimeTest: passed (1000 fast reads, reload, TTL, lifecycle, ten audit batches, listener deduplication)");
        } finally { PTSDCampaignRuntime.reset(); Global.setSector(before); Global.setSettings(beforeSettings); }
    }
}
