package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.shard.PTSD_BaseShard_Util;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Evidence-limited player response, beginning with the first actual fire-probe contact. */
public final class PTSDThreatDirector {
    private static final Random RANDOM = new Random();
    private static final String TRAP_TAG = "PTSD_late_trap";

    private PTSDThreatDirector() { }

    public static void recordProbe(PTSDCrisisState state, PTSDCrisisState.StrategicEvent event,
                                   String outcome, float observedFP, boolean uploadSurvived) {
        if (state == null || event == null || !event.probePlayerIntervention ||
                !event.probeEngaged) return;
        boolean uploaded = uploadSurvived && ("COMPLETED_SAMPLE".equals(outcome) ||
                "ESCAPED_AFTER_CONTACT".equals(outcome));
        add(state, "FRONT", event.id, event.targetSystemId,
                Math.max(1f, observedFP / 5f), uploaded ? .68f : .25f,
                uploaded ? "CONFIRMED_UPLOADED" : "LOST", 60f);
    }

    public static void recordVictory(PTSDCrisisState state, String fleetId, String systemId,
                                     float defeatedFP, boolean witnessed) {
        if (state == null || fleetId == null || defeatedFP <= 0f) return;
        PTSDLateState late = state.late;
        String key = "victory:" + fleetId;
        if (!late.committedIds.add(key)) return;
        float day = PTSDCrisisState.getDay();
        if (day >= late.nextPlayerVictoryRollDay) {
            late.previousPlayerVictoryFP = late.recentPlayerVictoryFP;
            late.recentPlayerVictoryFP = 0f;
            late.nextPlayerVictoryRollDay = day + 10f;
        }
        // An unwitnessed victory can shape the human war record, but cannot supply
        // the Omega with a fresh battle sample for an adaptive ambush.
        if (witnessed) late.recentPlayerVictoryFP += defeatedFP;
        if (systemId != null) {
            Float previous = late.playerVictoryFPBySystem.get(systemId);
            late.playerVictoryFPBySystem.put(systemId, Math.min(2000f,
                    (previous == null ? 0f : previous) + defeatedFP));
            late.playerLastVictoryDayBySystem.put(systemId, day);
        }
        add(state, "FRONT", fleetId, systemId, Math.min(100f, defeatedFP / 5f), .85f,
                witnessed ? "CONFIRMED_UPLOADED" : "LOST", 90f);
        if (witnessed) add(state, "MOBILITY", fleetId, systemId,
                Math.min(100f, defeatedFP / 8f), .45f, "CONFIRMED_UPLOADED", 30f);
        for (PTSDLateState.Front front : late.fronts.values()) {
            if (front != null && !front.resolved && systemId != null &&
                    systemId.equals(front.systemId)) {
                front.attackBudget = Math.max(0f, front.attackBudget - defeatedFP * .5f);
                PTSDLateWarDirector.enqueueAlert(state, key + ":front", systemId,
                        "战区核算：敌方主攻舰体遭受可核实战损；本区下一轮轨道压力下降。", day);
            }
        }
        PTSDGeneralStaffDirector.recordContribution(state, key, "BATTLE", "player",
                systemId, Math.max(5f, defeatedFP / 10f));
    }

    public static void recordAction(PTSDCrisisState state, String domain, String sourceId,
                                    String systemId, float strength, float confidence,
                                    boolean uploaded, float lifetime) {
        add(state, domain, sourceId, systemId, strength, confidence,
                uploaded ? "CONFIRMED_UPLOADED" : "LOCAL", lifetime);
    }

    private static void add(PTSDCrisisState state, String domain, String sourceId,
                            String systemId, float strength, float confidence,
                            String uploadState, float lifetime) {
        PTSDLateState.ThreatEvidence evidence = new PTSDLateState.ThreatEvidence();
        evidence.id = "threat:" + domain + ":" + sourceId;
        for (PTSDLateState.ThreatEvidence old : state.late.threatEvidence) {
            if (evidence.id.equals(old.id)) return;
        }
        evidence.domain = domain;
        evidence.sourceFleetId = sourceId;
        evidence.sourceSystemId = systemId;
        evidence.strength = Math.max(0f, Math.min(100f, strength));
        evidence.confidence = Math.max(0f, Math.min(1f, confidence));
        evidence.uploadState = uploadState;
        evidence.day = PTSDCrisisState.getDay();
        evidence.expiresDay = evidence.day + Math.max(1f, lifetime);
        evidence.playerInvolved = true;
        state.late.threatEvidence.add(evidence);
    }

    public static float getThreat(PTSDCrisisState state, float day) {
        if (state == null || state.late == null) return 0f;
        float[] values = new float[6];
        String[] domains = { "FRONT", "FORTRESS", "RESCUE", "SUPPLY", "MOBILITY", "DECEPTION" };
        for (PTSDLateState.ThreatEvidence item : state.late.threatEvidence) {
            if (item == null || item.expiresDay <= day ||
                    !"CONFIRMED_UPLOADED".equals(item.uploadState)) continue;
            float freshness = Math.max(0f, (item.expiresDay - day) /
                    Math.max(1f, item.expiresDay - item.day));
            for (int i = 0; i < domains.length; i++) {
                if (domains[i].equals(item.domain)) {
                    values[i] = Math.max(values[i], item.strength * item.confidence * freshness);
                    break;
                }
            }
        }
        values[0] *= 1.2f;
        values[1] *= 1.2f;
        java.util.Arrays.sort(values);
        return Math.min(100f, values[5] + values[4] * .5f + values[3] * .25f);
    }

    public static float localThreat(PTSDCrisisState state, String systemId, float day) {
        if (state == null || systemId == null) return 0f;
        float result = 0f;
        for (PTSDLateState.ThreatEvidence item : state.late.threatEvidence) {
            if (item == null || !systemId.equals(item.sourceSystemId) ||
                    !"CONFIRMED_UPLOADED".equals(item.uploadState) || item.expiresDay <= day) continue;
            result = Math.max(result, item.strength * item.confidence *
                    Math.max(0f, (item.expiresDay - day) / Math.max(1f, item.expiresDay - item.day)));
        }
        return Math.min(100f, result);
    }

    public static void advance(PTSDCrisisState state, float day) {
        if (state == null || state.late == null) return;
        List<PTSDLateState.ThreatEvidence> expired = new ArrayList<PTSDLateState.ThreatEvidence>();
        for (PTSDLateState.ThreatEvidence item : state.late.threatEvidence) {
            if (item == null || item.expiresDay <= day) expired.add(item);
        }
        state.late.threatEvidence.removeAll(expired);
        if (state.phase != PTSDCrisisState.Phase.FORTIFICATION &&
                state.phase != PTSDCrisisState.Phase.MOBILIZATION &&
                state.phase != PTSDCrisisState.Phase.WAR) return;
        if (day < state.late.nextTrapDay) return;
        float threat = getThreat(state, day);
        float spike = state.late.recentPlayerVictoryFP;
        if (threat < 35f || spike < 250f ||
                spike <= state.late.previousPlayerVictoryFP * 1.7f) return;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null || player.getContainingLocation() == null) return;
        String systemId = player.getStarSystem() == null ? null : player.getStarSystem().getId();
        if (systemId == null || localThreat(state, systemId, day) < 25f) return;
        PTSDLateState.Front donor = null;
        for (PTSDLateState.Front front : state.late.fronts.values()) {
            if (front != null && !front.resolved && front.attackBudget >= 40f &&
                    (donor == null || front.attackBudget > donor.attackBudget)) donor = front;
        }
        if (donor == null && state.late.strategicReserve < 40f) return;
        if (spawnTrap(state, player, threat)) {
            if (donor != null) {
                donor.attackBudget -= 40f;
                PTSDLateWarDirector.enqueueAlert(state, "trap-donor:" + Math.round(day), donor.systemId,
                        "战区监听：主攻舰群抽离约40点火力；原进攻区的轨道压力已经下降。", day);
            } else state.late.strategicReserve -= 40f;
            state.late.nextTrapDay = day + Math.max(10f, 50f - threat * .4f) /
                    data.scripts.IIRT_Omega_ModPlugin.latePlayerResponse;
        }
    }

    private static boolean spawnTrap(PTSDCrisisState state, CampaignFleetAPI player, float threat) {
        LocationAPI location = player.getContainingLocation();
        float cr = 0f;
        int count = 0;
        for (com.fs.starfarer.api.fleet.FleetMemberAPI member : player.getFleetData().getMembersListCopy()) {
            cr += member.getRepairTracker().getCR();
            count++;
        }
        cr = count == 0 ? 1f : cr / count;
        float min = cr < .35f ? 3800f : 1700f;
        float max = cr < .35f ? 5200f : 3000f;
        Vector2f point = PTSDCrisisAPI.findSafePoint(location, player, min, max, RANDOM);
        if (point == null) return false;
        FleetParamsV3 params = new FleetParamsV3(point,
                IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION, 1f,
                FleetTypes.TASK_FORCE, 40f,
                0f, 0f, 0f, 0f, 0f, 0f);
        CampaignFleetAPI fleet = PTSD_BaseShard_Util.createFleet(params,
                40f, PTSD_BaseShard_Util.FleetRole.GUARD_ASSAULT, RANDOM);
        if (fleet == null) return false;
        location.addEntity(fleet);
        fleet.setLocation(point.x, point.y);
        fleet.setName("精神创伤战损截击群");
        fleet.addTag(TRAP_TAG);
        fleet.addTag("PTSD_crisis_fleet");
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        fleet.addAssignment(FleetAssignment.INTERCEPT, player, 2f, "截击已暴露的高威胁目标");
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, player, 2f, "脱离战区");
        PTSDOmegaEncounterResponse.attachPursuer(fleet, player);
        PTSDCrisisDevIntel.report("高威胁伏击", "已由近期上传战损与当地目击触发；抽调40战略预算",
                player.getStarSystem() == null ? null : player.getStarSystem().getId(), fleet.getId());
        return true;
    }
}
