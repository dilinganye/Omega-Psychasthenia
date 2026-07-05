package data.scripts.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;

/**
 * Manages Omega's reserve base attacks during the FULL_ATTACK stage.
 * This script spreads Omega raids from a reserve system to nearby player colonies.
 */
public class IIRT_Omega_ReserveAttacker implements EveryFrameScript {

    private static final Color NOTICE_COLOR = new Color(238, 165, 143, 255);

    private final SectorAPI sector;
    private float stageElapsed = 0f;
    private int attackInterval = (int)(15 + Math.random() * 15); // 15-30 days between reserve attacks

    public IIRT_Omega_ReserveAttacker(SectorAPI sector) {
        this.sector = sector;
    }

    @Override
    public boolean isDone() {
        // Run until explicitly removed
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null) return;
        if (sector == null) return;

        stageElapsed += Global.getSector().getClock().convertToDays(amount);

        // Check if reserve system exists and get it
        String reserveSystemId = (String) Global.getSector().getMemoryWithoutUpdate().get("$IIRT_Omega_ReserveSystem");
        if (reserveSystemId == null) return; // No reserve system assigned yet

        StarSystemAPI reserveSystem = Global.getSector().getStarSystem(reserveSystemId);
        if (reserveSystem == null) return; // Invalid system

        // Check attack interval
        if (stageElapsed >= attackInterval) {
            stageElapsed = 0f;
            attackInterval = (int)(15 + Math.random() * 15);

            // Launch an attack from the reserve system
            launchReserveAttack(reserveSystem);
        }
    }

    private void launchReserveAttack(StarSystemAPI reserveSystem) {
        // Find player colonies to attack
        List<MarketAPI> markets = Global.getSector().getEconomy().getMarketsCopy();
        WeightedRandomPicker<MarketAPI> targetMarkets = new WeightedRandomPicker<>();

        for (MarketAPI market : markets) {
            if (market == null || market.getPrimaryEntity() == null) continue;
            if (market.getFaction() == null) continue;

            // Skip Omega's own markets
            if (market.getFaction().getId().contentEquals("Omega_Psychasthenia")) continue;

            // Prefer player-aligned markets
            float weight = 1f;
            if (market.getFaction().getId().contentEquals("player")) weight = 2f;

            targetMarkets.add(market, weight);
        }

        if (targetMarkets.isEmpty()) return;

        MarketAPI targetMarket = targetMarkets.pick();
        if (targetMarket == null) return;

        // Create attack fleet from reserve system
        Vector2f spawnLoc = reserveSystem.getLocation();
        spawnLoc = Misc.getPointWithinRadius(spawnLoc, 300f);

        // Calculate attack strength based on reserve location and player threat level
        float battleSize = Global.getSettings().getBattleSize();
        float strength = battleSize * (1.5f + (float)Math.random() * 0.5f);

        FleetParamsV3 params = new FleetParamsV3(
                spawnLoc,
                "Omega_Psychasthenia",
                1f,
                FleetTypes.TASK_FORCE,
                strength,
                0, 0, 0, 0, 0, 4
        );
        params.maxNumShips = (int)(Global.getSettings().getMaxShipsInFleet() * 1.5f);
        params.aiCores = OfficerQuality.AI_OMEGA;

        CampaignFleetAPI attackFleet = FleetFactoryV3.createFleet(params);
        attackFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        attackFleet.setName("Omega Reserve Strike");

        // Add to hyperspace and assign target
        Global.getSector().getHyperspace().addEntity(attackFleet);
        attackFleet.setLocation(spawnLoc.x, spawnLoc.y);
        attackFleet.addAssignment(FleetAssignment.ATTACK_LOCATION, targetMarket.getPrimaryEntity(), 1000f);

        // Notification
        try {
            String msg = String.format("警告：从远方据点检测到Omega舰队向%s的%s发起进攻！",
                    targetMarket.getStarSystem().getName(),
                    targetMarket.getName());
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(msg, NOTICE_COLOR);
        } catch (Throwable t) {
            // Ignore UI failures
        }

        if (Global.getSettings().isDevMode()) {
            Global.getLogger(this.getClass()).info("Reserve attack launched from " + reserveSystem.getName() +
                    " targeting " + targetMarket.getName() + " with strength " + strength);
        }
    }
}



