package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.util.Random;

/** Applies local panic to economy/fleet generation and creates tightly capped panic piracy. */
public final class PTSDLocalPanicManager {
    private static final String ACCESS_ID = "PTSD_local_panic_access";
    private static final String FLEET_ID = "PTSD_local_panic_fleet";
    private static final String PIRATE_MARKER = "$PTSD_panic_pirates";

    private PTSDLocalPanicManager() { }

    public static void advance(PTSDCrisisState state, float day, Random random) {
        if (state == null || random == null) return;
        // Market-wide recomputation is intentionally coarse; running it every frame would scale
        // poorly with large modded sectors and provides no useful simulation fidelity.
        if (state.lastLocalPanicUpdateDay <= 0f || day - state.lastLocalPanicUpdateDay >= .25f) {
            PTSDLocalPanicAPI.updateProximityAndDecay(state, day);
            applyMarketEffects(state);
        }
        if (day >= state.nextPanicPirateDay) {
            state.nextPanicPirateDay = day + 6f + random.nextFloat() * 7f;
            maybeSpawnPirates(state, random);
        }
    }

    private static void applyMarketEffects(PTSDCrisisState state) {
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market == null || market.isPlanetConditionMarketOnly() ||
                    market.getStarSystem() == null) continue;
            float local = PTSDLocalPanicAPI.getMarketPanic(market);
            float system = Math.max(local,
                    PTSDLocalPanicAPI.getSystemPanic(market.getStarSystem().getId()));
            if (local <= .1f) market.getAccessibilityMod().unmodifyFlat(ACCESS_ID);
            else market.getAccessibilityMod().modifyFlat(
                    ACCESS_ID, -Math.min(.65f, local * .0065f), "局部恐慌");

            if (system <= .1f) {
                market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT)
                        .unmodifyMult(FLEET_ID);
            } else {
                float mult = Math.max(.25f, 1f - system * .0075f);
                market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT)
                        .modifyMult(FLEET_ID, mult, "星系恐慌与人员流失");
            }
        }
    }

    private static void maybeSpawnPirates(PTSDCrisisState state, Random random) {
        if (countPanicPirates() >= 4) return;
        WeightedRandomPicker<MarketAPI> picker = new WeightedRandomPicker<MarketAPI>(random);
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market == null || market.isPlanetConditionMarketOnly() ||
                    market.getStarSystem() == null || market.getPrimaryEntity() == null) continue;
            float panic = PTSDLocalPanicAPI.getSystemPanic(market.getStarSystem().getId());
            if (panic < 30f) continue;
            picker.add(market, (panic - 25f) * Math.max(1f, market.getSize() - 1f));
        }
        MarketAPI target = picker.pick();
        if (target == null) return;
        float panic = PTSDLocalPanicAPI.getSystemPanic(target.getStarSystem().getId());
        if (random.nextFloat() > Math.min(.7f, (panic - 20f) / 100f)) return;

        FleetParamsV3 params = new FleetParamsV3(
                target, null, Factions.PIRATES, 1f, FleetTypes.RAIDER,
                12f + panic * .45f, 0f, 0f, 0f, 0f, 0f, 0f);
        params.maxShipSize = 3;
        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
        if (fleet == null || fleet.isEmpty()) return;
        fleet.setName("趁乱活动的海盗");
        fleet.getMemoryWithoutUpdate().set(PIRATE_MARKER, true);
        fleet.getMemoryWithoutUpdate().set("$PTSD_panic_origin", target.getId());

        StarSystemAPI system = target.getStarSystem();
        SectorEntityToken spawn = !system.getJumpPoints().isEmpty() ?
                system.getJumpPoints().get(random.nextInt(system.getJumpPoints().size())) :
                target.getPrimaryEntity();
        system.addEntity(fleet);
        fleet.setLocation(spawn.getLocation().x, spawn.getLocation().y);
        fleet.addAssignment(FleetAssignment.RAID_SYSTEM, target.getPrimaryEntity(),
                8f + random.nextFloat() * 8f, "利用当地混乱");
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                spawn, 1000f, "撤离");
        PTSDCrisisDevIntel.report("恐慌海盗活动",
                target.getName() + " / 星系恐慌 " + Math.round(panic),
                system.getId(), fleet.getId());
    }

    private static int countPanicPirates() {
        int count = 0;
        for (LocationAPI location : Global.getSector().getAllLocations()) {
            for (CampaignFleetAPI fleet : location.getFleets()) {
                if (fleet != null && fleet.getMemoryWithoutUpdate().getBoolean(PIRATE_MARKER)) count++;
            }
        }
        return count;
    }
}
