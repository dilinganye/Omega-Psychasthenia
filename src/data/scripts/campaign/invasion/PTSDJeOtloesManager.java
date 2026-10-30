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
    public static final int MAX_CONSECUTIVE_CONTACT_ATTEMPTS = 3;
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
        if (state.jeDead) return;
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
        if (state == null || state.jeDead || PTSDJeWarExpedition.unavailable(state)) return ContactResult.MISSED;
        float day = PTSDCrisisState.getDay();
        int bucket = (int) Math.floor(day);
        if (state.jeContactDayBucket != bucket) {
            state.jeContactDayBucket = bucket;
            state.jeContactAttemptsToday = 0;
            if (state.jeContactBlockedUntilDay <= day) state.jeContactBlockedUntilDay = 0f;
        }
        state.jeContactAttemptsToday++;
        // Reopening contact after two unanswered calls must connect, even during a daily block.
        // The streak spans day boundaries and is cleared after the successful greeting.
        if (state.jeMissedContactsSinceSuccess >= MAX_CONSECUTIVE_CONTACT_ATTEMPTS - 1) {
            state.jeContactBlockedUntilDay = 0f;
            state.jeDevNextContactOutcome = 0;
            if (state.jeContactAttemptsToday >= MAX_CONSECUTIVE_CONTACT_ATTEMPTS)
                state.jeSpamComplaintPending = true;
            return ContactResult.CONNECTED;
        }
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
        return state != null && (PTSDJeWarExpedition.unavailable(state) || PTSDCrisisState.getDay() < state.jePostTaskBusyUntilDay);
    }

    public static int daysUntilNextTask(PTSDCrisisState state) {
        if (state == null) return 0;
        return Math.max(0, (int) Math.ceil(state.jeNextTaskAvailableDay - PTSDCrisisState.getDay()));
    }

    public static boolean canStartAnotherTask(PTSDCrisisState state) {
        return state != null && !state.jeDead && !PTSDJeWarExpedition.unavailable(state) && daysUntilNextTask(state) <= 0;
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
    public static boolean isOmegaFleet(com.fs.starfarer.api.campaign.CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getFaction() == null) return false;
        String id = fleet.getFaction().getId();
        return IIRT_Omega_Invasion.WATCHER_FACTION.equals(id) ||
                IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(id);
    }

    public static void recordFleetEncounter(com.fs.starfarer.api.campaign.CampaignFleetAPI fleet) {
        if (!isOmegaFleet(fleet)) return;
        com.fs.starfarer.api.campaign.CampaignFleetAPI player=Global.getSector().getPlayerFleet();
        // Dialog-plugin selection may be queried without an actual player encounter.
        if(player==null || player.getContainingLocation()!=fleet.getContainingLocation() ||
                !fleet.isVisibleToPlayerFleet()) return;
        String key = fleet.getMemoryWithoutUpdate().getString("$PTSD_participation_key");
        recordParticipation(PTSDCrisisState.get(), key == null ? "fleet:" + fleet.getId() : key);
    }

    public static void recordPlayerBattle(com.fs.starfarer.api.campaign.CampaignFleetAPI fleet) {
        if (!isOmegaFleet(fleet)) return;
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || fleet.getMemoryWithoutUpdate().getBoolean("$PTSD_player_battle_counted")) return;
        fleet.getMemoryWithoutUpdate().set("$PTSD_player_battle_counted", true);
        state.totalPlayerOmegaBattles++;
        String participation = fleet.getMemoryWithoutUpdate().getString("$PTSD_participation_key");
        recordParticipation(state, participation == null ? "fleet:" + fleet.getId() : participation);
        PTSDFTDirector.onPlayerEncounter(state, fleet.getId());
    }

    public static void recordParticipation(PTSDCrisisState state, String key) {
        if (state == null || key == null || key.trim().isEmpty()) return;
        if (state.jeParticipationIds == null) state.jeParticipationIds = new java.util.LinkedHashSet<String>();
        state.jeParticipationIds.add(key);
    }

    public static boolean triggerUnlocked(PTSDCrisisState state) {
        return state != null && !state.jeDead &&
                (state.totalScoutSightings >= 4 || state.totalPlayerOmegaBattles >= 1 ||
                (state.jeParticipationIds != null && state.jeParticipationIds.size() >= 4));
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
        com.fs.starfarer.api.util.WeightedRandomPicker<String> picker =
                new com.fs.starfarer.api.util.WeightedRandomPicker<String>(new Random());
        for (PTSDCrisisIncidentManager.ScriptedTemplate template :
                PTSDCrisisIncidentManager.getTemplates("CONDITIONAL", state.phase)) {
            if (template.id.startsWith("R2-") || template.id.startsWith("F2-")) {
                if ("可触发".equals(PTSDConditionalNewsDirector.unavailableReason(state, template.id)))
                    picker.add(template.id, Math.max(.1f, template.priority) * template.eventWeight);
            }
        }
        for (PTSDCrisisIncidentManager.ScriptedTemplate template : PTSDCrisisIncidentManager.getJeTemplates(state.phase)) {
            if (template.weight > 0f && "可触发".equals(PTSDCrisisIncidentManager.unavailableReason(template.id)))
                picker.add(template.id, template.weight);
        }
        if (PTSDCrisisIncidentManager.enabled("JE-01") && PTSDCrisisIncidentManager.getTemplate("JE-01") != null && !PTSDCrisisIncidentManager.getTemplate("JE-01").jeOnly &&
                "可触发".equals(PTSDCrisisIncidentManager.unavailableReason("JE-01")))
            picker.add("JE-01", .1f);
        String selected = picker.pick();
        PTSDCrisisState.CrisisIncident incident = selected == null ? null :
                ("JE-01".equals(selected) || PTSDCrisisIncidentManager.getTemplate(selected).jeOnly) ?
                        PTSDCrisisIncidentManager.forceAndGetPrivate(selected) :
                        PTSDConditionalNewsDirector.forJe(state, selected);
        if (incident == null) return null;
        if (!PTSDCrisisAPI.recordNewsIncident(incident.id)) return null;
        // Retain the assigned lead's truth value; supported disproof is also a valid result.
        state.jePlayerTaskIncidentId = incident.id;
        incident.jePlayerAssignment = true;
        recordTaskAccepted(state);
        PTSDCrisisIntel intel = PTSDCrisisIntel.ensureIntel();
        if (intel != null) intel.focusIncident(incident.id);
        if (text != null) text.addPara("Je 已将这份委托录入“边缘失联信号”的“高能信号”分支。打开该情报，查看“" +
                PTSDPlayerNewsText.headline(incident) + "”并使用任务下方的导航按钮前往调查。");
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
                    (state.jePlayerTaskIncidentId != null && state.jePlayerTaskIncidentId.equals(item.id)) ||
                    item.panicByMarket == null || item.panicByMarket.isEmpty()) continue;
            picked = item;
            break;
        }
        if (picked == null && PTSDCrisisIncidentManager.enabled("JE-01")) picked = PTSDCrisisIncidentManager.forceAndGetPrivate("JE-01");
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
                incident.jePlayerAssignment = true;
                state.jePlayerTaskIncidentId = null; // Claim once before any UI/callback.
                if (!PTSDTaskRules.successfulInvestigation(incident.investigationResult)) {
                    Global.getSector().getCampaignUI().addMessage("Je 委托已结束，未取得可核验的调查结论。", Misc.getGrayColor());
                    PTSDJeOtloesIntel.ensureIntel().sendContactUpdate();
                    return;
                }
                // mitigateIncident() accepts the fraction left behind. A stronger early-game
                // bonus must therefore divide that fraction instead of multiplying it.
                float remaining = Math.max(.05f, .5f / getEarlyPanicMitigationBonus(state));
                float removed = PTSDLocalPanicAPI.mitigateIncident(
                        incident, remaining, "JE_PLAYER_CONFIRMED");
                state.jeCompletedInvestigations++;
                state.jePersonalCompletedInvestigations++;
                state.jePrivateNetworkQualified = true;
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
        if (incident.investigationResolved && !PTSDTaskRules.successfulInvestigation(incident.investigationResult)) {
            state.jeAgentIncidentId = null;
            state.jeAgentReturnDay = 0f;
            state.jeMeetingMarketId = null;
            state.jeMeetingReady = false;
            PTSDJeOtloesIntel.ensureIntel().sendContactUpdate();
            return 100; // No panic mitigation or completion credit for a failed lead.
        }
        int percent = 5 + new Random((long) incident.id.hashCode() * 31L +
                (long) PTSDCrisisState.getDay()).nextInt(71);
        percent = Math.max(5, Math.round(percent / getEarlyPanicMitigationBonus(state)));
        PTSDLocalPanicAPI.mitigateIncident(incident, percent / 100f, "JE_AGENT_CONTROL");
        PTSDNewsSiteManager.resolveRemotely(state, incident);
        incident.investigationResolved = true;
        incident.investigationReal = true;
        incident.investigationResult = PTSDTaskRules.InvestigationResult.CONFIRMED;
        state.jeCompletedInvestigations++;
        PTSDInvestigationRewards.agent(incident);
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
