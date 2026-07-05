package data.scripts.campaign;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseAssignmentAI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

import java.util.List;

public class IIRT_Omega_ScoutAI extends BaseAssignmentAI {

    protected SectorEntityToken target;
    protected IntervalUtil checkInterval = new IntervalUtil(0.5f, 1f);
    protected IntervalUtil checksHankInterval = new IntervalUtil(5f, 10f);

    public IIRT_Omega_ScoutAI(CampaignFleetAPI fleet, SectorEntityToken target) {
        super();
        this.fleet = fleet;
        this.target = target;
        giveInitialAssignments();
    }

    @Override
    protected void giveInitialAssignments() {
        pickNext();
    }

    @Override
    protected void pickNext() {
        // pick a random behavior: orbit beacon, roam system, or go dark (stealth)
        float r = (float)Math.random();
        if (r < 0.4f) {
            // orbit/passive around the target
            fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, target, 10000f);
        } else if (r < 0.8f) {
            // roam in the system: use orbit aggressive on a random entity in the same system if available
            fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, target, 10000f);
        } else {
            // stealth: try to stay near target but force transponder off
            fleet.getMemoryWithoutUpdate().set(com.fs.starfarer.api.impl.campaign.ids.MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF, true);
            fleet.getMemoryWithoutUpdate().set(com.fs.starfarer.api.impl.campaign.ids.MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);
            fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, target, 10000f);
        }
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        float days = Misc.getDays(amount);
        checkInterval.advance(days);
        if (checkInterval.intervalElapsed()) {
            // scan for nearby hostile fleets; if any present, attempt to flee
            if (!fleet.isInCurrentLocation()) return;
            List<CampaignFleetAPI> fleets = fleet.getContainingLocation().getFleets();
            for (CampaignFleetAPI other : fleets) {
                if (other == fleet) continue;
                if (other.getFaction() == null) continue;
                if (other.getFaction().getId().contentEquals(fleet.getFaction().getId())) continue;
                float dist = Misc.getDistance(other.getLocation(), fleet.getLocation());
                if (dist < 10000f) {
                    // hostile nearby: flee
                    try {
                        if (fleet.getAbility("emergency_burn") != null) fleet.getAbility("emergency_burn").activate();
                        if (fleet.getAbility("sensor_burst") != null) fleet.getAbility("sensor_burst").activate();
                    } catch (Throwable t) {
                        // Ignore errors activating abilities
                    }
                    fleet.clearAssignments();
                    // attempt to escape: head to target point and despawn (simplified escape behavior)
                    checksHankInterval.advance(days);
                    fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, target, 3000f, "???");
                    if (checksHankInterval.intervalElapsed()) {
                        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, target, 3000f, "???");
                    }
                    return;
                }
            }
        }
    }
}


