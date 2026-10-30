package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

/** Local fleet decisions. Ambushes retain the original fleet; escaping survey scouts vanish out of sight. */
public final class PTSDOmegaEncounterResponse implements EveryFrameScript {
    private static final String INSTALLED = "$PTSD_encounter_response_installed";
    private static final String AMBUSH_FP = "$PTSD_ambush_original_fp";
    private static final float LARGE_FLEET_FP = 75f;
    private static final float SENSE_RANGE = 10000f;
    private static final Random RANDOM = new Random();

    private final CampaignFleetAPI fleet;
    private final String playerId;
    private final boolean surveyScout;
    private final float originalFp;
    private float checkDays;
    private float hiddenDays;
    private boolean ambushing;
    private boolean escaping;
    private SectorEntityToken ambushPoint;
    private SectorEntityToken escapeWaypoint;
    private String escapeThreatId;

    private PTSDOmegaEncounterResponse(CampaignFleetAPI fleet, CampaignFleetAPI player, boolean surveyScout) {
        this.fleet = fleet;
        this.playerId = player == null ? null : player.getId();
        this.surveyScout = surveyScout;
        this.originalFp = Math.max(0f, fleet.getFleetPoints());
    }

    public static void attachPursuer(CampaignFleetAPI fleet, CampaignFleetAPI player) {
        attach(fleet, player, false);
    }

    public static void attachSurveyScout(CampaignFleetAPI fleet) {
        attach(fleet, null, true);
    }

    /** Old saves may already contain pursuing fleets when this behavior is installed. */
    public static void attachExistingLocalPursuers() {
        if (Global.getSector() == null) return;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null || player.getContainingLocation() == null) return;
        for (CampaignFleetAPI candidate : PTSDCampaignRuntime.fleets(player.getContainingLocation())) {
            if (candidate == null || candidate.getFaction() == null || !PTSDOmegaFaction(candidate) ||
                    candidate.getAI() == null) continue;
            FleetAssignmentDataAPI order = candidate.getAI().getCurrentAssignment();
            if (order != null && order.getTarget() == player &&
                    (order.getAssignment() == FleetAssignment.INTERCEPT ||
                     order.getAssignment() == FleetAssignment.ATTACK_LOCATION))
                attachPursuer(candidate, player);
        }
    }

    private static void attach(CampaignFleetAPI fleet, CampaignFleetAPI player, boolean scout) {
        if (fleet == null || fleet.getMemoryWithoutUpdate().getBoolean(INSTALLED)) return;
        PTSDCampaignRuntime.normalizeFleet(PTSDCrisisState.get(), fleet);
        fleet.getMemoryWithoutUpdate().set(INSTALLED, true);
        fleet.addScript(new PTSDOmegaEncounterResponse(fleet, player, scout));
    }

    @Override public boolean isDone() {
        return fleet == null || !fleet.isAlive() || fleet.isExpired() || fleet.isDespawning();
    }

    @Override public boolean runWhilePaused() { return false; }

    @Override public void advance(float amount) {
        if (isDone() || Global.getSector() == null || fleet.getBattle() != null) return;
        float days = Global.getSector().getClock().convertToDays(amount);
        checkDays += days;
        if (ambushing) hiddenDays += days;
        if (checkDays < .12f) return;
        checkDays = 0f;
        CampaignFleetAPI player = playerId == null ? Global.getSector().getPlayerFleet() :
                asFleet(Global.getSector().getEntityById(playerId));
        LocationAPI location = fleet.getContainingLocation();
        if (location == null) return;
        if (escaping) {
            // Ambush timeouts in existing saves may have entered retreat without an ID.
            CampaignFleetAPI threat = escapeThreatId == null || escapeThreatId.trim().isEmpty() ? null :
                    asFleet(Global.getSector().getEntityById(escapeThreatId));
            if (threat == null || threat.getContainingLocation() != location) threat = player;
            // Only survey scouts lose the distance gate; combat pursuers keep their prior retreat rules.
            if (!fleet.hasTag("PTSD_late_trap") && !fleet.isVisibleToPlayerFleet() &&
                    (surveyScout || player == null || player.getContainingLocation() != location ||
                            Misc.getDistance(fleet, player) > 6500f)) {
                { PTSDEventFleetReturn.credit(fleet); data.scripts.campaign.invasion.PTSDCampaignRuntime.leaving(fleet); fleet.despawn(); }
                return;
            }
            if (escapeWaypoint == null || Misc.getDistance(fleet, escapeWaypoint) < 1600f) {
                escapeWaypoint = escapePoint(location, threat == null ? fleet : threat, 13000f);
                fleet.clearAssignments();
                fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, escapeWaypoint, 8f,
                        "持续撤出传感器视野");
            }
            return;
        }
        if (fleet.getMemoryWithoutUpdate().getBoolean(PTSDReconPlayerEvents.RETREAT_TRIGGERED)) return;

        if (surveyScout) {
            CampaignFleetAPI threat = nearestObservedThreat(location, player, true);
            if (threat != null && !escaping) withdraw(threat, false);
            return;
        }
        if (player == null || player.getContainingLocation() != location) return;
        CampaignFleetAPI third = nearestObservedThreat(location, player, false);
        boolean outmatched = third != null &&
                estimatedStrength(fleet) < (estimatedStrength(player) + estimatedStrength(third)) * 1.1f;
        if (outmatched && !ambushing) {
            withdraw(third, true);
        } else if (ambushing) {
            if (!outmatched && Misc.getDistance(fleet, player) <= 5500f) {
                ambushing = false;
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, false);
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
                fleet.clearAssignments();
                fleet.addAssignment(FleetAssignment.INTERCEPT, player, 5f, "从侧翼伏击目标");
                fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                        escapePoint(location, player, 14000f), 15f, "伏击结束后脱离战区");
                PTSDCrisisDevIntel.report("伏击点启用", "原舰队回到交战，未生成额外自动分",
                        fleet.getStarSystem() == null ? null : fleet.getStarSystem().getId(), fleet.getId());
            } else if (hiddenDays >= 12f) {
                // A failed ambush remains a retreat; it must not turn into an infinite local fleet.
                ambushing = false;
                escaping = true;
                escapeThreatId = (third == null ? player : third).getId();
                fleet.clearAssignments();
                fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                        escapePoint(location, third == null ? player : third, 16000f), 20f,
                        "伏击窗口结束，撤出战区");
            }
        }
    }

    private CampaignFleetAPI nearestObservedThreat(LocationAPI location, CampaignFleetAPI player,
                                                    boolean includePlayer) {
        CampaignFleetAPI nearest = null;
        float best = Float.MAX_VALUE;
        for (CampaignFleetAPI other : PTSDCampaignRuntime.fleets(location)) {
            if (other == null || !other.isAlive() || other.isExpired() || other.getContainingLocation() != location) continue;
            if (other == null || other == fleet || !other.isAlive() || other.isExpired() ||
                    other.getFaction() == null || PTSDOmegaFaction(other)) continue;
            if (other == player && !includePlayer) continue;
            if (other != player && other.getFleetPoints() < LARGE_FLEET_FP) continue;
            float distance = Misc.getDistance(fleet, other);
            if (distance > SENSE_RANGE || other != player && distance > 5000f &&
                    Vector2f.dot(other.getVelocity(),
                            Vector2f.sub(fleet.getLocation(), other.getLocation(), null)) <= 0f) continue;
            if (distance < best) { nearest = other; best = distance; }
        }
        return nearest;
    }

    private static boolean PTSDOmegaFaction(CampaignFleetAPI fleet) {
        String id = fleet.getFaction().getId();
        return IIRT_Omega_Invasion.WATCHER_FACTION.equals(id) ||
                IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(id);
    }

    private static float estimatedStrength(CampaignFleetAPI fleet) {
        if (fleet == null) return 0f;
        float value = fleet.getEffectiveStrength();
        return Float.isNaN(value) || Float.isInfinite(value) || value <= 0f ?
                Math.max(1f, fleet.getFleetPoints()) : value;
    }

    private void withdraw(CampaignFleetAPI threat, boolean reserveAmbush) {
        LocationAPI location = fleet.getContainingLocation();
        if (location == null) return;
        fleet.clearAssignments();
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, false);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
        if (reserveAmbush) {
            ambushing = true;
            hiddenDays = 0f;
            fleet.getMemoryWithoutUpdate().set(AMBUSH_FP, originalFp);
            ambushPoint = escapePoint(location, threat, 7000f);
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, ambushPoint, 8f,
                    "避开联合舰队，转入附近伏击区");
            fleet.addAssignment(FleetAssignment.HOLD, ambushPoint, 8f, "等待战线分离");
            PTSDCrisisDevIntel.report("事件舰队暂撤", "敌方实际战力不足；原舰队 " +
                    Math.round(originalFp) + " 自动分转入近处伏击点；第三方 " +
                    Math.round(threat.getFleetPoints()) + " FP",
                    fleet.getStarSystem() == null ? null : fleet.getStarSystem().getId(), fleet.getId());
        } else {
            escaping = true;
            escapeThreatId = threat.getId();
            escapeWaypoint = escapePoint(location, threat, 13000f);
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, escapeWaypoint, 8f,
                    "中止残骸测绘并撤离");
            if (fleet.getAbility(Abilities.EMERGENCY_BURN) == null)
                fleet.addAbility(Abilities.EMERGENCY_BURN);
            if (fleet.getAbility(Abilities.EMERGENCY_BURN) != null &&
                    fleet.getAbility(Abilities.EMERGENCY_BURN).isUsable())
                fleet.getAbility(Abilities.EMERGENCY_BURN).activate();
            PTSDCrisisDevIntel.report("残骸测绘队撤离", "目击逼近舰队后放弃现场",
                    fleet.getStarSystem() == null ? null : fleet.getStarSystem().getId(), fleet.getId());
        }
    }

    private SectorEntityToken escapePoint(LocationAPI location, CampaignFleetAPI threat, float range) {
        Vector2f away = Vector2f.sub(fleet.getLocation(), threat.getLocation(), null);
        if (away.lengthSquared() < 1f) away = Misc.getUnitVectorAtDegreeAngle(RANDOM.nextFloat() * 360f);
        else away.normalise();
        away.scale(range);
        Vector2f point = Vector2f.add(fleet.getLocation(), away, null);
        Vector2f safe = PTSDCrisisAPI.findSafePoint(location, point, 0f, 2400f, RANDOM);
        return location.createToken(safe == null ? point : safe);
    }

    private static CampaignFleetAPI asFleet(Object entity) {
        return entity instanceof CampaignFleetAPI ? (CampaignFleetAPI) entity : null;
    }
}
