package data.scripts.campaign.invasion;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.Random;

/** Save-backed trigger and task coordinator for the independent Je Otloes contact. */
public final class PTSDJeOtloesManager implements EveryFrameScript, ColonyInteractionListener {
    public static final String PERSON_ID = "PTSD_je_otloes";
    public static final float POST_TASK_BUSY_DAYS = 3f;
    public static final float TASK_COOLDOWN_DAYS = 12f;
    public enum ContactResult { CONNECTED, MISSED, BLOCKED_FOR_DAY }
    private final Random random = new Random();

    @Override
    public boolean isDone() { return false; }

    @Override
    public boolean runWhilePaused() { return true; }

    @Override
    public void advance(float amount) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return;
        advanceTasks(state);
        if (state.jeDetectorGranted) {
            if (!Global.getSector().getCharacterData().getAbilities().contains(PTSDCrisisDetectorAbility.ID)) {
                Global.getSector().getCharacterData().addAbility(PTSDCrisisDetectorAbility.ID);
            }
            if (Global.getSector().getPlayerFleet() != null && !Global.getSector().getPlayerFleet().hasAbility(PTSDCrisisDetectorAbility.ID)) {
                Global.getSector().getPlayerFleet().addAbility(PTSDCrisisDetectorAbility.ID);
            }
        }
        // Port introductions and return meetings are selected by PTSD_CampaignPlugin.
        // This avoids opening a nested interaction dialog from a market listener.
        state.jePendingIntroMarketId = null;
        state.jePendingMeetingDialog = false;
    }

    @Override
    public void reportPlayerOpenedMarket(MarketAPI market) { }

    @Override public void reportPlayerClosedMarket(MarketAPI market) { }
    @Override public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) { }
    @Override public void reportPlayerMarketTransaction(
            com.fs.starfarer.api.campaign.PlayerMarketTransaction transaction) { }

    public static ContactResult rollContactAttempt() {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return ContactResult.MISSED;
        float day = PTSDCrisisState.getDay();
        int bucket = (int) Math.floor(day);
        if (state.jeContactDayBucket != bucket) {
            state.jeContactDayBucket = bucket;
            state.jeContactAttemptsToday = 0;
            if (state.jeContactBlockedUntilDay <= day) state.jeContactBlockedUntilDay = 0f;
        }
        state.jeContactAttemptsToday++;
        if (Global.getSettings().isDevMode() && state.jeDevNextContactOutcome != 0) {
            int forced = state.jeDevNextContactOutcome;
            state.jeDevNextContactOutcome = 0;
            if (forced == 1) return ContactResult.CONNECTED;
            state.jeMissedContactsSinceSuccess++;
            state.jeMissedContactApologyPending = true;
            if (forced == 3) {
                state.jeContactBlockedUntilDay = bucket + 1f;
                return ContactResult.BLOCKED_FOR_DAY;
            }
            return ContactResult.MISSED;
        }
        if (state.jeContactBlockedUntilDay > day) {
            state.jeMissedContactsSinceSuccess++;
            state.jeMissedContactApologyPending = true;
            if (state.jeContactAttemptsToday >= 3) state.jeSpamComplaintPending = true;
            return ContactResult.BLOCKED_FOR_DAY;
        }
        if (Math.random() < .75d) {
            state.jeMissedContactsSinceSuccess++;
            state.jeMissedContactApologyPending = true;
            if (Math.random() < .20d) {
                state.jeContactBlockedUntilDay = bucket + 1f;
                if (state.jeContactAttemptsToday >= 3) state.jeSpamComplaintPending = true;
                return ContactResult.BLOCKED_FOR_DAY;
            }
            return ContactResult.MISSED;
        }
        return ContactResult.CONNECTED;
    }

    public static void consumeSuccessfulContactContext() {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return;
        state.jeMissedContactsSinceSuccess = 0;
        state.jeMissedContactApologyPending = false;
        state.jeSpamComplaintPending = false;
    }

    public static boolean isPostTaskBusy(PTSDCrisisState state) {
        return state != null && PTSDCrisisState.getDay() < state.jePostTaskBusyUntilDay;
    }

    public static int daysUntilNextTask(PTSDCrisisState state) {
        if (state == null) return 0;
        return Math.max(0, (int) Math.ceil(state.jeNextTaskAvailableDay - PTSDCrisisState.getDay()));
    }

    public static boolean canStartAnotherTask(PTSDCrisisState state) {
        return state != null && daysUntilNextTask(state) <= 0;
    }

    private static void recordTaskAccepted(PTSDCrisisState state) {
        float day = PTSDCrisisState.getDay();
        state.jeLastTaskAcceptedDay = day;
        state.jePostTaskBusyUntilDay = day + POST_TASK_BUSY_DAYS;
        state.jeNextTaskAvailableDay = day + TASK_COOLDOWN_DAYS;
    }

    public static void devForceNextContact(int outcome) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null && Global.getSettings().isDevMode()) state.jeDevNextContactOutcome = Math.max(0, Math.min(3, outcome));
    }

    public static void resetTaskCooldownForDev() {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || !Global.getSettings().isDevMode()) return;
        float day = PTSDCrisisState.getDay();
        state.jePostTaskBusyUntilDay = day;
        state.jeNextTaskAvailableDay = day;
        state.jeContactBlockedUntilDay = 0f;
        state.jeContactAttemptsToday = 0;
    }

    public static boolean isDetectorUnlockReady(PTSDCrisisState state) {
        if (state == null || state.jeDetectorGranted) return false;
        int combined = state.totalOmegaEncounters + state.totalPlayerOmegaBattles + state.jeCompletedInvestigations;
        return state.jeCompletedInvestigations >= 30 || state.phase == PTSDCrisisState.Phase.WAR || combined > 55;
    }

    public static void grantDetector() {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return;
        state.jeDetectorGranted = true;
        Global.getSector().getCharacterData().addAbility(PTSDCrisisDetectorAbility.ID);
        if (Global.getSector().getPlayerFleet() != null && !Global.getSector().getPlayerFleet().hasAbility(PTSDCrisisDetectorAbility.ID)) {
            Global.getSector().getPlayerFleet().addAbility(PTSDCrisisDetectorAbility.ID);
        }
        PTSDCrisisDevIntel.report("Je 授予危机探测能力", "危机信号相关器已加入生涯能力栏", null, null);
    }

    public static float getEarlyPanicMitigationBonus(PTSDCrisisState state) {
        if (state == null || state.phase == PTSDCrisisState.Phase.WAR || state.phase == PTSDCrisisState.Phase.ENDED) return 1f;
        float readiness = PTSDCrisisProgress.getInvasionReadiness(state);
        return Math.max(1f, 1.65f - Math.min(1f, readiness / 70f) * .65f);
    }
    public static boolean triggerUnlocked(PTSDCrisisState state) {
        return state != null &&
                (state.totalScoutSightings >= 4 || state.totalPlayerOmegaBattles >= 1);
    }

    public static boolean eligibleMarket(MarketAPI market) {
        return market != null && !market.isPlanetConditionMarketOnly() &&
                market.getPrimaryEntity() != null && market.getFaction() != null &&
                !market.getFaction().isHostileTo(Factions.PLAYER) &&
                !IIRT_Omega_Invasion.WATCHER_FACTION.equals(market.getFactionId()) &&
                !IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(market.getFactionId());
    }

    public static PTSDCrisisState.CrisisIncident startPlayerInvestigation(TextPanelAPI text) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || state.jePlayerTaskIncidentId != null || !canStartAnotherTask(state)) return null;
        PTSDCrisisState.CrisisIncident incident =
                PTSDCrisisIncidentManager.forceAndGet("JE-01");
        if (incident == null) return null;
        PTSDCrisisAPI.recordNewsIncident(incident.id);
        // Je's dedicated field work always has a material truth to find; ordinary news retains 25/70/5 outcomes.
        incident.investigationOutcome = 1;
        incident.investigationExpiresDay = PTSDCrisisState.getDay() + 30f;
        state.jePlayerTaskIncidentId = incident.id;
        recordTaskAccepted(state);
        PTSDCrisisIntel.ensureIntel();
        if (text != null) text.addPara("Je 将一组强度明显更高的异常报告转入了你的调查列表。");
        PTSDCrisisDevIntel.report("Je 实地调查委托", incident.headline,
                incident.targetSystemId, incident.targetEntityId);
        return incident;
    }

    public static PTSDCrisisState.CrisisIncident startAgentInvestigation(TextPanelAPI text) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || state.jeAgentIncidentId != null || state.jeMeetingReady || !canStartAnotherTask(state)) return null;
        PTSDCrisisState.CrisisIncident picked = null;
        for (int i = state.incidents.size() - 1; i >= 0; i--) {
            PTSDCrisisState.CrisisIncident item = state.incidents.get(i);
            if (item == null || !item.investigable || item.investigationResolved ||
                    item.panicByMarket == null || item.panicByMarket.isEmpty()) continue;
            picked = item;
            break;
        }
        if (picked == null) picked = PTSDCrisisIncidentManager.forceAndGet("JE-01");
        if (picked == null) return null;
        state.jeAgentIncidentId = picked.id;
        recordTaskAccepted(state);
        state.jeAgentReturnDay = PTSDCrisisState.getDay() + 7f + (float) Math.random() * 11f;
        if (text != null) text.addPara("Je 切断了公开信道。他只留下了一句：在他重新联系你之前，不要主动寻找他。");
        PTSDCrisisDevIntel.report("Je 代为调查开始",
                picked.headline + " / 预计 " + Math.round(state.jeAgentReturnDay - PTSDCrisisState.getDay()) + " 日",
                picked.targetSystemId, picked.targetEntityId);
        PTSDJeOtloesIntel.ensureIntel().sendContactUpdate();
        return picked;
    }

    private void advanceTasks(PTSDCrisisState state) {
        if (state.jePlayerTaskIncidentId != null) {
            PTSDCrisisState.CrisisIncident incident =
                    PTSDCrisisAPI.getIncident(state.jePlayerTaskIncidentId);
            if (incident == null) {
                state.jePlayerTaskIncidentId = null;
            } else if (incident.investigationResolved) {
                float mitigation = Math.min(.9f, .5f * getEarlyPanicMitigationBonus(state));
                float removed = PTSDLocalPanicAPI.mitigateIncident(
                        incident, mitigation, "JE_PLAYER_CONFIRMED");
                state.jeCompletedInvestigations++;
                state.jePlayerTaskIncidentId = null;
                Global.getSector().getCampaignUI().addMessage(
                        "已确认的情报帮助附近殖民地压低了恐慌传播（-" +
                                Math.round(removed) + "）。", Misc.getPositiveHighlightColor());
                PTSDJeOtloesIntel.ensureIntel().sendContactUpdate();
            }
        }

        if (state.jeAgentIncidentId != null && !state.jeMeetingReady &&
                PTSDCrisisState.getDay() >= state.jeAgentReturnDay) {
            MarketAPI meeting = nearestEligibleMarket();
            if (meeting != null) {
                state.jeMeetingMarketId = meeting.getId();
                state.jeMeetingReady = true;
                PTSDJeOtloesIntel.ensureIntel().reportMeetingReady(meeting);
                PTSDCrisisDevIntel.report("Je 调查完成，等待会面",
                        meeting.getName(), meeting.getStarSystem().getId(),
                        meeting.getPrimaryEntity().getId());
            }
        }
    }

    public static PTSDCrisisState.CrisisIncident getAgentIncident() {
        PTSDCrisisState state = PTSDCrisisState.get();
        return state == null ? null : PTSDCrisisAPI.getIncident(state.jeAgentIncidentId);
    }

    public static int completeAgentReport() {
        PTSDCrisisState state = PTSDCrisisState.get();
        PTSDCrisisState.CrisisIncident incident = getAgentIncident();
        if (state == null || incident == null) return 100;
        int percent = 5 + new Random((long) incident.id.hashCode() * 31L +
                (long) PTSDCrisisState.getDay()).nextInt(71);
        percent = Math.min(95, Math.round(percent * getEarlyPanicMitigationBonus(state)));
        PTSDLocalPanicAPI.mitigateIncident(incident, percent / 100f, "JE_AGENT_CONTROL");
        PTSDNewsSiteManager.resolveRemotely(state, incident);
        incident.investigationResolved = true;
        incident.investigationReal = true;
        state.jeCompletedInvestigations++;
        state.jeAgentIncidentId = null;
        state.jeAgentReturnDay = 0f;
        state.jeMeetingMarketId = null;
        state.jeMeetingReady = false;
        PTSDJeOtloesIntel.ensureIntel().sendContactUpdate();
        return percent;
    }

    private static MarketAPI nearestEligibleMarket() {
        MarketAPI best = null;
        float bestDistance = Float.MAX_VALUE;
        com.fs.starfarer.api.campaign.CampaignFleetAPI player =
                Global.getSector().getPlayerFleet();
        if (player == null) return null;
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (!eligibleMarket(market) || market.getStarSystem() == null) continue;
            float distance = Misc.getDistance(player.getLocationInHyperspace(),
                    market.getStarSystem().getLocation());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = market;
            }
        }
        return best;
    }
}
