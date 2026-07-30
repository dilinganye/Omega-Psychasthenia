package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseAssignmentAI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

/** Recon units sample nearby strength, flee every foreign fleet, and despawn at a distant point. */
public final class IIRT_Omega_ScoutAI extends BaseAssignmentAI {
    // Keep these original field names and superclass for old-save deserialization.
    protected SectorEntityToken target;
    protected IntervalUtil checkInterval = new IntervalUtil(0.12f, 0.28f);
    protected IntervalUtil checksHankInterval = new IntervalUtil(4f, 6f);

    private String targetSystemId;
    private float observeForDays;
    private float ageDays;
    private float fleeDays;
    private boolean sightingReported;
    private boolean fleeing;

    public IIRT_Omega_ScoutAI(CampaignFleetAPI fleet, SectorEntityToken target) {
        super();
        this.fleet = fleet;
        this.target = target;
        this.targetSystemId = target == null || target.getStarSystem() == null ? null : target.getStarSystem().getId();
        this.observeForDays = 4f + (float) Math.random() * 7f;
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
        fleet.setTransponderOn(false);
    }

    @Override
    protected void giveInitialAssignments() {
        beginObservation();
    }

    @Override
    protected void pickNext() {
        if (fleeing) {
            if (fleet != null) fleet.despawn(FleetDespawnReason.OTHER, null);
            setDone();
        } else {
            beginObservation();
        }
    }

    private void beginObservation() {
        if (fleet == null || target == null) {
            setDone();
            return;
        }
        fleet.clearAssignments();
        fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, target, Math.max(3f, observeForDays),
                "保持无法解析的观测轨道");
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
        if (fleeing) fleeDays += days;
        checkInterval.advance(days);
        if (checkInterval.intervalElapsed()) {
            recordPlayerSighting();
            CampaignFleetAPI threat = findThreat();
            if (!fleeing && threat != null) beginEscape(threat);
            recordLocalStrength();
        }
        if (!fleeing && ageDays >= observeForDays) beginEscape(null);
        if (fleeing && fleeDays >= 4.5f) {
            fleet.despawn(FleetDespawnReason.OTHER, null);
            setDone();
        }
    }

    private void ensureRuntimeFields() {
        if (checkInterval == null) checkInterval = new IntervalUtil(0.12f, 0.28f);
        if (checksHankInterval == null) checksHankInterval = new IntervalUtil(4f, 6f);
        if (observeForDays <= 0f) observeForDays = 4f + (float) Math.random() * 7f;
        if (targetSystemId == null && target != null && target.getStarSystem() != null) {
            targetSystemId = target.getStarSystem().getId();
            configureFleet();
        }
    }

    private void recordPlayerSighting() {
        if (sightingReported || !fleet.isVisibleToPlayerFleet()) return;
        sightingReported = true;
        IIRT_Omega_Invasion.reportScoutSighting(targetSystemId);
    }

    private CampaignFleetAPI findThreat() {
        LocationAPI location = fleet.getContainingLocation();
        if (location == null) return null;
        CampaignFleetAPI closest = null;
        float closestDistance = Float.MAX_VALUE;
        for (CampaignFleetAPI other : new ArrayList<CampaignFleetAPI>(location.getFleets())) {
            if (other == null || other == fleet || !other.isAlive() || other.isExpired()) continue;
            String factionId = other.getFaction() == null ? null : other.getFaction().getId();
            if (IIRT_Omega_Invasion.WATCHER_FACTION.equals(factionId) ||
                    IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(factionId)) continue;
            float distance = Misc.getDistance(fleet.getLocation(), other.getLocation());
            float trigger = other == Global.getSector().getPlayerFleet() ? 9500f : 6200f;
            if (distance < trigger && distance < closestDistance) {
                closest = other;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private void recordLocalStrength() {
        if (targetSystemId == null || fleet.getContainingLocation() == null) return;
        float strength = 0f;
        List<CampaignFleetAPI> fleets = fleet.getContainingLocation().getFleets();
        for (CampaignFleetAPI other : fleets) {
            if (other == null || other == fleet || other.getFaction() == null) continue;
            String factionId = other.getFaction().getId();
            if (IIRT_Omega_Invasion.WATCHER_FACTION.equals(factionId) ||
                    IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(factionId)) continue;
            if (Misc.getDistance(fleet.getLocation(), other.getLocation()) <= 12000f) strength += Math.max(0f, other.getFleetPoints());
        }
        IIRT_Omega_Invasion.reportReconSample(targetSystemId, strength);
    }

    private void beginEscape(CampaignFleetAPI threat) {
        if (fleeing || fleet.getContainingLocation() == null) return;
        fleeing = true;
        fleeDays = 0f;
        IIRT_Omega_Invasion.reportScoutEscape(targetSystemId, threat == Global.getSector().getPlayerFleet());
        try {
            if (fleet.getAbility("emergency_burn") != null) fleet.getAbility("emergency_burn").activate();
        } catch (Throwable ignored) { }
        Vector2f away = new Vector2f(1f, 0f);
        if (threat != null) Vector2f.sub(fleet.getLocation(), threat.getLocation(), away);
        else if (target != null) Vector2f.sub(fleet.getLocation(), target.getLocation(), away);
        if (away.lengthSquared() < 0.01f) away.set((float) Math.random() - 0.5f, (float) Math.random() - 0.5f);
        away.normalise();
        away.scale(11000f + (float) Math.random() * 5000f);
        Vector2f.add(fleet.getLocation(), away, away);
        SectorEntityToken escape = fleet.getContainingLocation().createToken(away);
        fleet.clearAssignments();
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, escape, 5f, "脱离观测区域");
    }
}