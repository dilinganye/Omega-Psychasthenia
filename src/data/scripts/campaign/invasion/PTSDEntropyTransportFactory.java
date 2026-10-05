package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import data.hullmods.shard.PTSD_BaseShard_Util;
import org.lwjgl.util.vector.Vector2f;
import java.util.Random;

/** One paid, genuine Entropy transport branch for fortress work and FT physical leads. */
public final class PTSDEntropyTransportFactory {
    private static final Random RANDOM = new Random();
    private PTSDEntropyTransportFactory() { }
    public static CampaignFleetAPI create(PTSDCrisisState state, LocationAPI location,
                                          float x, float y, float assetCost, String source) {
        if (state == null || state.late == null || location == null ||
                state.late.strategicReserve < assetCost) return null;
        FleetParamsV3 params = new FleetParamsV3(new Vector2f(x, y),
                PTSDCrisisProgress.getActiveFactionId(state), .5f, FleetTypes.PATROL_LARGE,
                100f, 0f, 0f, 0f, 0f, 0f, 0f);
        params.maxNumShips = Global.getSettings().getMaxShipsInFleet();
        PTSD_BaseShard_Util.BranchDefinition branch = PTSD_BaseShard_Util.prepareFleetParams(
                params, 100f, PTSD_BaseShard_Util.BRANCH_CUBE, RANDOM);
        if (branch == null || params.addShips == null || params.addShips.isEmpty()) return null;
        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
        if (fleet == null || fleet.getFleetData().getMembersListCopy().isEmpty()) return null;
        PTSD_BaseShard_Util.tagFleet(fleet, branch);
        fleet.setName("未知编队");
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(false);
        fleet.addTag("PTSD_crisis_fleet");
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        fleet.getEventListeners().add(new PTSDStrategicFleetListener(null, fleet.getFleetPoints()));
        location.addEntity(fleet);
        fleet.setLocation(x, y);
        state.late.strategicReserve -= assetCost;
        PTSDLateWarDirector.recordSpending(state, source, 0f, assetCost);
        return fleet;
    }
}
