import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import data.scripts.campaign.invasion.*;
import java.lang.reflect.*;
import java.util.*;

/** Exercise the actual contact entry point, including daily blocks and day rollover. */
public final class JeContactGuaranteeTest {
    interface Calls { Object call(String name); }
    @SuppressWarnings("unchecked")
    static <T> T mock(Class<T> type, Calls calls) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (self, method, args) -> {
            Object value = calls.call(method.getName());
            if (value != null || !method.getReturnType().isPrimitive()) return value;
            if (method.getReturnType() == boolean.class) return false;
            if (method.getReturnType() == float.class) return 0f;
            if (method.getReturnType() == int.class) return 0;
            if (method.getReturnType() == long.class) return 0L;
            return null;
        });
    }
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        SectorAPI previousSector = Global.getSector();
        SettingsAPI previousSettings = Global.getSettings();
        try {
            long[] timestamp = {0L};
            CampaignClockAPI clock = mock(CampaignClockAPI.class,
                    name -> name.equals("getTimestamp") ? timestamp[0] : null);
            Map<String, Object> data = new HashMap<>();
            Global.setSector(mock(SectorAPI.class, name -> name.equals("getPersistentData") ? data :
                    name.equals("getClock") ? clock : null));
            Global.setSettings(mock(SettingsAPI.class, name -> null));
            Constructor<PTSDCrisisState> constructor = PTSDCrisisState.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            PTSDCrisisState state = constructor.newInstance();
            state.progressInitialized = true;
            data.put(PTSDCrisisState.PERSISTENT_KEY, state);
            check(!PTSDJeOtloesManager.triggerUnlocked(state), "Fresh state unexpectedly unlocked");
            for (int n = 0; n < 3; n++) PTSDJeOtloesManager.recordParticipation(state, "incident:" + n);
            PTSDJeOtloesManager.recordParticipation(state, "incident:0");
            check(!PTSDJeOtloesManager.triggerUnlocked(state), "Duplicate participation unlocked intro");
            PTSDJeOtloesManager.recordParticipation(state, "incident:3");
            check(PTSDJeOtloesManager.triggerUnlocked(state), "Fourth participation did not unlock");
            state.jeParticipationIds.clear(); state.totalPlayerOmegaBattles = 1;
            check(PTSDJeOtloesManager.triggerUnlocked(state), "Single battle no longer unlocks");
            state.totalPlayerOmegaBattles = 0;
            check(PTSDEventFleetReturn.assetsFor(75f) == 37.5f && PTSDEventFleetReturn.assetsFor(0f) == 0f &&
                    PTSDEventFleetReturn.assetsFor(Float.NaN) == 0f, "Invalid event fleet recovery");
            float today = PTSDCrisisState.getDay();
            state.jeContactBlockedUntilDay = today + 1f;
            check(PTSDJeOtloesManager.rollContactAttempt() == PTSDJeOtloesManager.ContactResult.BLOCKED_FOR_DAY,
                    "First blocked attempt changed");
            check(PTSDJeOtloesManager.rollContactAttempt() == PTSDJeOtloesManager.ContactResult.BLOCKED_FOR_DAY,
                    "Second blocked attempt changed");
            check(PTSDJeOtloesManager.rollContactAttempt() == PTSDJeOtloesManager.ContactResult.CONNECTED,
                    "Third attempt did not override the daily block");
            check(state.jeSpamComplaintPending && state.jeContactBlockedUntilDay == 0f,
                    "Guaranteed reply lost its complaint or left the daily block active");
            PTSDJeOtloesManager.consumeSuccessfulContactContext();
            check(state.jeMissedContactsSinceSuccess == 0 && !state.jeSpamComplaintPending &&
                    !state.jeMissedContactApologyPending, "Success did not reset the streak/context");
            state.jeMissedContactsSinceSuccess = 2;
            state.jeMissedContactApologyPending = true;
            timestamp[0] += 86400000L;
            check(PTSDJeOtloesManager.rollContactAttempt() == PTSDJeOtloesManager.ContactResult.CONNECTED,
                    "Day rollover erased the unanswered-call guarantee");
            check(state.jeContactAttemptsToday == 1 && !state.jeSpamComplaintPending,
                    "Cross-day reply incorrectly claimed three calls today");
            PTSDJeOtloesManager.consumeSuccessfulContactContext();
            // All ordinary random outcomes must connect within three calls, not just blocked outcomes.
            for (int round = 0; round < 200; round++) {
                boolean connected = false;
                for (int attempt = 0; attempt < 3; attempt++) {
                    if (PTSDJeOtloesManager.rollContactAttempt() == PTSDJeOtloesManager.ContactResult.CONNECTED) {
                        connected = true; PTSDJeOtloesManager.consumeSuccessfulContactContext(); break;
                    }
                }
                check(connected, "Random contact exceeded three attempts");
            }
            state.jeDead = true; state.jeMissedContactsSinceSuccess = 2;
            check(PTSDJeOtloesManager.rollContactAttempt() == PTSDJeOtloesManager.ContactResult.MISSED,
                    "Contact guarantee revived Je");
            System.out.println("JeContactGuaranteeTest: passed (third-call guarantee, daily block, rollover, reset, random runs, death)");
        } finally {
            Global.setSector(previousSector); Global.setSettings(previousSettings);
        }
    }
}
