package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseAssignmentAI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

/** A persistent recon mission: travel, observe/roam, react to discovery, then withdraw. */
public final class IIRT_Omega_ScoutAI extends BaseAssignmentAI {
    public enum MissionType { RELAY, HYPERSPACE_WATCH, COLONY_INFILTRATION, WILDERNESS_ROAM }
    private enum MissionStage { TRAVEL, OBSERVE, ROAM, ESCAPE }

    // Preserve the original names/superclass for old-save deserialization.
    protected SectorEntityToken target;
    protected IntervalUtil checkInterval = new IntervalUtil(0.12f, 0.28f);
    protected IntervalUtil checksHankInterval = new IntervalUtil(4f, 6f);

    private String targetSystemId;
    private float observeForDays;
    private float ageDays;
    private float fleeDays;
    private boolean sightingReported;
    private boolean fleeing;

    private MissionType missionType;
    private MissionStage missionStage;
    private float stageDays;
    private float missionDays;
    private float nextRoamDay;
    private float nextReconSampleDay; // legacy throttle field; retained for save compatibility
    private int reconSampleDayBucket = -1;
    private List<Float> dailyReconSamples = new ArrayList<Float>();
    private float lastPlayerDistance = Float.MAX_VALUE;
    private boolean arrived;
    private boolean escapeReported;

    public IIRT_Omega_ScoutAI(CampaignFleetAPI fleet, SectorEntityToken target) {
        this(fleet, target, MissionType.RELAY,
                target == null || target.getStarSystem() == null ? null : target.getStarSystem().getId(),
                4f + (float) Math.random() * 7f);
    }

    public IIRT_Omega_ScoutAI(CampaignFleetAPI fleet, SectorEntityToken target, MissionType missionType,
                              String targetSystemId, float observeForDays) {
        super();
        this.fleet = fleet;
        this.target = target;
        this.targetSystemId = targetSystemId;
        this.missionType = missionType == null ? MissionType.RELAY : missionType;
        this.observeForDays = Math.max(2f, observeForDays);
        this.missionStage = MissionStage.TRAVEL;
        configureFleet();
        giveInitialAssignments();
    }

    private void configureFleet() {
        if (fleet == null) return;
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_AVOID_PLAYER_SLOWLY, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED, true);
        fleet.getMemoryWithoutUpdate().set("$PTSD_scout_mission", missionType == null ? "LEGACY" : missionType.name());
        fleet.getMemoryWithoutUpdate().set("$PTSD_scout_stage", missionStage == null ? "LEGACY" : missionStage.name());
        fleet.addTag(Tags.SALVAGE_ENTITY_NO_DEBRIS);
        fleet.setTransponderOn(false);
    }

    @Override
    protected void giveInitialAssignments() {
        beginTravel();
    }

    @Override
    protected void pickNext() {
        if (fleet == null || isDone()) return;
        if (missionStage == MissionStage.TRAVEL) {
            if (isAtTarget()) beginOnStation();
            else beginTravel();
        } else if (missionStage == MissionStage.OBSERVE || missionStage == MissionStage.ROAM) {
            if (stageDays >= observeForDays) beginEscape(null, false);
            else assignOnStation();
        } else if (missionStage == MissionStage.ESCAPE) {
            finishEscape();
        }
    }

    private void beginTravel() {
        if (fleet == null || target == null) { safeDespawn("任务目标失效"); return; }
        missionStage = MissionStage.TRAVEL;
        fleet.getMemoryWithoutUpdate().set("$PTSD_scout_stage", missionStage.name());
        fleet.clearAssignments();
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, target, 120f, travelText());
    }

    private String travelText() {
        if (missionType == MissionType.RELAY) return "沿静默航线接近通讯设施";
        if (missionType == MissionType.HYPERSPACE_WATCH) return "前往超空间监听点";
        if (missionType == MissionType.COLONY_INFILTRATION) return "潜行接近核心星域目标";
        return "前往无人星域";
    }

    private void beginOnStation() {
        if (arrived) return;
        arrived = true;
        stageDays = 0f;
        missionStage = missionType == MissionType.WILDERNESS_ROAM ? MissionStage.ROAM : MissionStage.OBSERVE;
        fleet.getMemoryWithoutUpdate().set("$PTSD_scout_stage", missionStage.name());
        IIRT_Omega_Invasion.reportScoutMissionStage(targetSystemId, fleet.getId(), missionType.name(), "抵达任务区");
        assignOnStation();
    }

    private void assignOnStation() {
        if (fleet == null || target == null) { beginEscape(null, false); return; }
        fleet.clearAssignments();
        if (missionStage == MissionStage.ROAM) {
            SectorEntityToken roam = pickRoamTarget();
            if (roam != null) target = roam;
            nextRoamDay = stageDays + 4f + (float) Math.random() * 7f;
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, target, 12f, "在无人星系内变换观测位置");
        } else if (missionType == MissionType.HYPERSPACE_WATCH) {
            fleet.addAssignment(FleetAssignment.HOLD, target, Math.max(2f, observeForDays - stageDays), "维持超空间静默监听");
        } else {
            fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, target, Math.max(2f, observeForDays - stageDays),
                    missionType == MissionType.COLONY_INFILTRATION ? "截取殖民地外层通讯" : "扫描通讯设施");
        }
    }

    @Override
    public void advance(float amount) {
        ensureRuntimeFields();
        if (isDone() || fleet == null || !fleet.isAlive() || fleet.isExpired() || fleet.getContainingLocation() == null) {
            setDone();
            return;
        }
        super.advance(amount);
        float days = Global.getSector().getClock().convertToDays(amount);
        ageDays += days;
        missionDays += days;
        if (missionStage != MissionStage.TRAVEL && missionStage != MissionStage.ESCAPE) stageDays += days;
        if (missionStage == MissionStage.ESCAPE) fleeDays += days;

        if (missionStage == MissionStage.TRAVEL && isAtTarget()) beginOnStation();
        if (missionStage == MissionStage.ROAM && stageDays >= nextRoamDay) assignOnStation();

        checkInterval.advance(days);
        if (checkInterval.intervalElapsed()) {
            boolean visible = fleet.isVisibleToPlayerFleet();
            if (visible) recordPlayerSighting();
            CampaignFleetAPI threat = findImmediateThreat(visible);
            if (missionStage != MissionStage.ESCAPE && threat != null) beginEscape(threat, threat.isPlayerFleet());
            if (missionStage == MissionStage.OBSERVE || missionStage == MissionStage.ROAM) {
                recordLocalStrength();
                if (missionType == MissionType.HYPERSPACE_WATCH && hasForeignFleetNearby(5200f)) {
                    beginEscape(null, false);
                }
            }
            if (missionStage == MissionStage.ESCAPE) checkEscapeComplete();
        }

        if ((missionStage == MissionStage.OBSERVE || missionStage == MissionStage.ROAM) && stageDays >= observeForDays) {
            beginEscape(null, false);
        }
        // Only a broken/pathless mission may hit this failsafe; ordinary travel gets ample time.
        if (missionStage == MissionStage.TRAVEL && missionDays >= 120f) beginEscape(null, false);
        if (missionStage == MissionStage.ESCAPE && fleeDays >= 18f && !fleet.isVisibleToPlayerFleet()) finishEscape();
    }

    private void ensureRuntimeFields() {
        if (checkInterval == null) checkInterval = new IntervalUtil(0.12f, 0.28f);
        if (dailyReconSamples == null) dailyReconSamples = new ArrayList<Float>();
        if (checksHankInterval == null) checksHankInterval = new IntervalUtil(4f, 6f);
        if (observeForDays <= 0f) observeForDays = 5f;
        if (missionType == null) missionType = MissionType.RELAY;
        if (missionStage == null) {
            // Old saves started their observation timer before arrival; migrate them back to travel when needed.
            missionStage = isAtTarget() ? MissionStage.OBSERVE : MissionStage.TRAVEL;
            stageDays = 0f;
            fleeing = false;
            configureFleet();
            if (missionStage == MissionStage.TRAVEL) beginTravel(); else beginOnStation();
        }
        if (targetSystemId == null && target != null && target.getStarSystem() != null) {
            targetSystemId = target.getStarSystem().getId();
        }
    }

    private boolean isAtTarget() {
        if (fleet == null || target == null || fleet.getContainingLocation() != target.getContainingLocation()) return false;
        float threshold = Math.max(700f, target.getRadius() + fleet.getRadius() + 450f);
        return Misc.getDistance(fleet.getLocation(), target.getLocation()) <= threshold;
    }

    private void recordPlayerSighting() {
        if (sightingReported) return;
        sightingReported = true;
        IIRT_Omega_Invasion.reportScoutSighting(targetSystemId);
    }

    private CampaignFleetAPI findImmediateThreat(boolean playerCanSeeScout) {
        LocationAPI location = fleet.getContainingLocation();
        if (location == null) return null;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        CampaignFleetAPI closest = null;
        float closestDistance = Float.MAX_VALUE;
        for (CampaignFleetAPI other : new ArrayList<CampaignFleetAPI>(location.getFleets())) {
            if (other == null || other == fleet || !other.isAlive() || other.isExpired()) continue;
            String factionId = other.getFaction() == null ? null : other.getFaction().getId();
            if (IIRT_Omega_Invasion.WATCHER_FACTION.equals(factionId) ||
                    IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(factionId)) continue;
            float distance = Misc.getDistance(fleet.getLocation(), other.getLocation());
            boolean targeted = assignmentTargetsFleet(other);
            if (other == player) {
                boolean approaching = lastPlayerDistance < Float.MAX_VALUE && distance + 250f < lastPlayerDistance;
                lastPlayerDistance = distance;
                if (!playerCanSeeScout) continue;
                if (!(targeted || distance < 6500f || (approaching && distance < 12000f))) continue;
            } else {
                // Patrol interception or a very close accidental encounter causes an immediate abort.
                if (!(targeted && distance < 12000f) && distance >= 2600f) continue;
            }
            if (distance < closestDistance) { closest = other; closestDistance = distance; }
        }
        return closest;
    }

    private boolean assignmentTargetsFleet(CampaignFleetAPI other) {
        if (other == null || other.getAI() == null || other.getAI().getCurrentAssignment() == null) return false;
        FleetAssignmentDataAPI assignment = other.getAI().getCurrentAssignment();
        return assignment.getTarget() == fleet && (assignment.getAssignment() == FleetAssignment.INTERCEPT ||
                assignment.getAssignment() == FleetAssignment.ATTACK_LOCATION || other.isPlayerFleet());
    }

    private boolean hasForeignFleetNearby(float range) {
        LocationAPI location = fleet.getContainingLocation();
        if (location == null) return false;
        for (CampaignFleetAPI other : location.getFleets()) {
            if (other == null || other == fleet || !other.isAlive()) continue;
            String factionId = other.getFaction() == null ? null : other.getFaction().getId();
            if (IIRT_Omega_Invasion.WATCHER_FACTION.equals(factionId) ||
                    IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(factionId)) continue;
            if (Misc.getDistance(fleet.getLocation(), other.getLocation()) <= range) return true;
        }
        return false;
    }

    private void recordLocalStrength() {
        if (targetSystemId == null || fleet.getContainingLocation() == null) return;
        int bucket = (int) Math.floor(PTSDCrisisState.getDay());
        if (reconSampleDayBucket < 0) reconSampleDayBucket = bucket;
        if (bucket != reconSampleDayBucket) {
            submitDailyReconMaximum();
            reconSampleDayBucket = bucket;
        }
        float strength = 0f;
        for (CampaignFleetAPI other : fleet.getContainingLocation().getFleets()) {
            if (other == null || other == fleet || other.getFaction() == null) continue;
            String factionId = other.getFaction().getId();
            if (IIRT_Omega_Invasion.WATCHER_FACTION.equals(factionId) ||
                    IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(factionId)) continue;
            if (Misc.getDistance(fleet.getLocation(), other.getLocation()) <= 12000f) {
                strength += Math.max(0f, other.getFleetPoints());
            }
        }
        dailyReconSamples.add(strength);
    }

    private void submitDailyReconMaximum() {
        if (dailyReconSamples == null || dailyReconSamples.isEmpty() || targetSystemId == null) return;
        float maximum = 0f;
        for (Float sample : dailyReconSamples) {
            if (sample != null && !Float.isNaN(sample) && !Float.isInfinite(sample)) {
                maximum = Math.max(maximum, Math.max(0f, sample));
            }
        }
        dailyReconSamples.clear();
        IIRT_Omega_Invasion.reportReconDailyMaximum(targetSystemId, maximum,
                fleet == null ? null : fleet.getId(), reconSampleDayBucket);
    }
    private SectorEntityToken pickRoamTarget() {
        StarSystemAPI system = targetSystemId == null ? null : Global.getSector().getStarSystem(targetSystemId);
        if (system == null) return target;
        List<SectorEntityToken> candidates = new ArrayList<SectorEntityToken>();
        candidates.addAll(system.getPlanets());
        candidates.addAll(system.getJumpPoints());
        if (system.getCenter() != null) candidates.add(system.getCenter());
        if (candidates.isEmpty()) return target;
        return candidates.get((int) (Math.random() * candidates.size()));
    }

    private void beginEscape(CampaignFleetAPI threat, boolean escapedPlayer) {
        if (missionStage == MissionStage.ESCAPE || fleet.getContainingLocation() == null) return;
        submitDailyReconMaximum();
        missionStage = MissionStage.ESCAPE;
        fleeing = true;
        fleeDays = 0f;
        fleet.getMemoryWithoutUpdate().set("$PTSD_scout_stage", missionStage.name());
        if (!escapeReported) {
            escapeReported = true;
            IIRT_Omega_Invasion.reportScoutEscape(targetSystemId, escapedPlayer);
        }
        try { if (fleet.getAbility(Abilities.EMERGENCY_BURN) != null) fleet.getAbility(Abilities.EMERGENCY_BURN).activate(); }
        catch (Throwable ignored) { }
        SectorEntityToken exit = createDistantExit(threat);
        fleet.clearAssignments();
        if (exit == null) { safeDespawn("无法建立撤离航线"); return; }
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, exit, 60f,
                escapedPlayer ? "规避追踪并切断信号" : "沿静默航线撤离");
    }

    private SectorEntityToken createDistantExit(CampaignFleetAPI threat) {
        LocationAPI hyperspace = Global.getSector().getHyperspace();
        if (hyperspace == null) return null;
        Vector2f origin = fleet.getLocationInHyperspace();
        Vector2f away = new Vector2f((float) Math.random() - 0.5f, (float) Math.random() - 0.5f);
        if (threat != null) Vector2f.sub(origin, threat.getLocationInHyperspace(), away);
        else if (target != null) Vector2f.sub(origin, target.getLocationInHyperspace(), away);
        if (away.lengthSquared() < 0.01f) away.set(1f, 0f);
        away.normalise();
        away.scale(18000f + (float) Math.random() * 10000f);
        Vector2f.add(origin, away, away);
        return hyperspace.createToken(away);
    }

    private void checkEscapeComplete() {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null || player.getContainingLocation() != fleet.getContainingLocation()) return;
        float distance = Misc.getDistance(fleet.getLocation(), player.getLocation());
        if (sightingReported && distance >= 18000f && !fleet.isVisibleToPlayerFleet()) finishEscape();
    }

    private void finishEscape() {
        safeDespawn("完成撤离");
    }

    private void safeDespawn(String reason) {
        submitDailyReconMaximum();
        if (fleet != null && fleet.getBattle() == null && fleet.isAlive()) {
            IIRT_Omega_Invasion.reportScoutMissionStage(targetSystemId, fleet.getId(),
                    missionType == null ? "LEGACY" : missionType.name(), reason);
            fleet.despawn(FleetDespawnReason.OTHER, null);
        }
        setDone();
    }
}