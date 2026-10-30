package data.scripts.campaign.invasion;

import com.fs.starfarer.api.combat.ShipAPI;
import data.hullmods.shard.PTSD_BaseShard_Util;

import static data.scripts.IIRT_Omega_ModPlugin.*;

/** Physical fleet ceilings applied after all normal strength modifiers. */
public final class PTSDCrisisFleetLimits {
    public final float totalFp;
    public final float perFleetFp;
    public final int maxShips;
    public final ShipAPI.HullSize maxHullSize;

    private PTSDCrisisFleetLimits(float totalFp, float perFleetFp, int maxShips,
                                  ShipAPI.HullSize maxHullSize) {
        this.totalFp = totalFp;
        this.perFleetFp = perFleetFp;
        this.maxShips = maxShips;
        this.maxHullSize = maxHullSize;
    }

    public PTSD_BaseShard_Util.FleetLimits forFleet(float allocatedFp) {
        return new PTSD_BaseShard_Util.FleetLimits(
                Math.min(perFleetFp, Math.max(3f, allocatedFp)), maxShips, maxHullSize);
    }

    public static float reconProgress(PTSDCrisisState state, float day) {
        if(state==null || state.phase!=PTSDCrisisState.Phase.RECON)return 0f;
        return Math.max(0f,Math.min(1f,(day-state.phaseStartedDay)/Math.max(1f,collect_data_time*2f)));
    }

    /** Recon grows with the same elapsed-time denominator used for its phase transition. */
    public static PTSDCrisisFleetLimits forPhase(PTSDCrisisState state, float day) {
        PTSDCrisisState.Phase phase = state == null ? PTSDCrisisState.Phase.RECON : state.phase;
        float total, single;
        int ships;
        ShipAPI.HullSize hull;
        switch (phase) {
            case EXPANSION:
                total = 210f; single = 90f; ships = 6; hull = ShipAPI.HullSize.CRUISER; break;
            case FORTIFICATION:
                total = 270f; single = 105f; ships = 8; hull = ShipAPI.HullSize.CRUISER; break;
            case MOBILIZATION:
                total = 325f; single = 120f; ships = 10; hull = ShipAPI.HullSize.CAPITAL_SHIP; break;
            case WAR:
            case ENDED:
                float progress = state == null ? 0f :
                        Math.max(0f, Math.min(1f, (day - state.phaseStartedDay) / 240f));
                total = 325f + 275f * progress;
                single = 120f + 280f * progress;
                ships = 12;
                hull = ShipAPI.HullSize.CAPITAL_SHIP;
                break;
            default:
                total = 150f; single = 65f; ships = 3; hull = ShipAPI.HullSize.DESTROYER;
        }
        float difficulty = difficultyIncrease();
        float curve = data.scripts.PTSDDifficultySettings.scale(state,day);
        ships=Math.max(1,(int)Math.ceil(ships*curve));
        // Difficulty does not skip early hull/count gates. These constrain new fleets only.
        if (phase == PTSDCrisisState.Phase.RECON || phase == PTSDCrisisState.Phase.DORMANT) {
            float p=reconProgress(state,day);
            single=p<.25f?12f:p<.5f?18f:p<.75f?24f:p<.9f?30f:36f;
            ships=Math.max(1,(int)Math.ceil((p<.25f?1:p<.75f?2:3)*curve));
            hull=data.scripts.PTSDDifficultySettings.growthProgress(state,day)<.25f?ShipAPI.HullSize.FRIGATE:ShipAPI.HullSize.DESTROYER;
            return new PTSDCrisisFleetLimits(total * difficulty * curve,single * curve,ships,hull);
        }
        return new PTSDCrisisFleetLimits(total * difficulty * curve, single * difficulty * curve, ships, hull);
    }

    /** An increase to any gameplay difficulty control raises the strategic ceiling.
     *  Display-only alert density, DevMode diagnostics and enable/disable are excluded. */
    public static float difficultyIncrease() {
        float result = 1f;
        result = Math.max(result, omega_fleet_strength_multiplier);
        result = Math.max(result, 1f + unknown_event_strength_flat / 65f);
        result = Math.max(result, unknown_event_frequency);
        result = Math.max(result, 3f / Math.max(1f, unknown_event_min_interval));
        result = Math.max(result, 7f / Math.max(1f, unknown_event_max_interval));
        result = Math.max(result, lateConstructionSpeed);
        result = Math.max(result, lateProductionMultiplier);
        result = Math.max(result, lateOffensiveTempo);
        result = Math.max(result, latePlayerResponse);
        return Math.max(1f, Math.min(10f, result));
    }

}
