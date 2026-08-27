package data.scripts.campaign.invasion.news;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.invasion.PTSDCrisisNewsAPI;
import data.scripts.campaign.invasion.PTSDCrisisState;
import org.lwjgl.util.vector.Vector2f;

/**
 * CUSTOM news example and built-in event: a stranded fugitive in a damaged shuttle.
 *
 * Other mods can implement PTSDCrisisNewsAPI.CustomNewsHandler in the same way and reference
 * the fully-qualified class from a CSV cell: CUSTOM(their.mod.Handler).
 */
public class FugitiveShuttleNewsHandler implements PTSDCrisisNewsAPI.CustomNewsHandler {
    private static final String FLEET_MARKER = "$PTSD_news_fugitive";
    private static final float APPROACH_DISTANCE = 2600f;

    @Override
    public PTSDCrisisNewsAPI.TargetSelection pick(PTSDCrisisNewsAPI.PickContext context) {
        WeightedRandomPicker<MarketAPI> picker =
                new WeightedRandomPicker<MarketAPI>(context.random);
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market == null || market.isPlanetConditionMarketOnly() ||
                    market.getPrimaryEntity() == null || market.getStarSystem() == null) continue;
            StarSystemAPI system = market.getStarSystem();
            if (system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER) ||
                    system.hasTag(Tags.THEME_HIDDEN)) continue;
            String factionId = market.getFactionId();
            if ("IIRT_The_Fourth_Watcher".equals(factionId) ||
                    "Omega_Psychasthenia".equals(factionId)) continue;
            picker.add(market, Math.max(1f, market.getSize()));
        }
        MarketAPI market = picker.pick();
        if (market == null) return null;
        return new PTSDCrisisNewsAPI.TargetSelection(
                market.getStarSystem(), market, market.getPrimaryEntity());
    }

    @Override
    public SectorEntityToken onIncidentCreated(PTSDCrisisNewsAPI.IncidentContext context) {
        if (context.system == null || context.targetLocation == null) return null;

        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                Factions.PIRATES, "逃犯", true);
        FleetMemberAPI shuttle = Global.getFactory().createFleetMember(
                FleetMemberType.SHIP, "kite_original_Stock");
        shuttle.setShipName("失窃穿梭机");
        shuttle.getRepairTracker().setCR(.2f);
        fleet.getFleetData().addFleetMember(shuttle);
        fleet.getFleetData().setFlagship(shuttle);
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        fleet.setName("逃犯");
        fleet.setNoFactionInName(true);
        fleet.setNoAutoDespawn(true);

        fleet.getMemoryWithoutUpdate().set(FLEET_MARKER, context.incident.id);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_AVOID_PLAYER_SLOWLY, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);

        Vector2f point = findSafePoint(context);
        context.system.addEntity(fleet);
        fleet.setLocation(point.x, point.y);
        fleet.clearAssignments();
        fleet.addAssignment(FleetAssignment.HOLD, fleet, 1000f, "燃料耗尽，保持静默");
        fleet.addScript(new FugitiveLifecycleScript(
                fleet, context.incident.id, context.incident.createdDay + 30f));
        return fleet;
    }

    private Vector2f findSafePoint(PTSDCrisisNewsAPI.IncidentContext context) {
        SectorEntityToken anchor = context.targetLocation;
        for (int attempt = 0; attempt < 30; attempt++) {
            float radius = Math.max(1600f, anchor.getRadius() + 1100f) +
                    context.random.nextFloat() * 1800f;
            Vector2f candidate = Misc.getPointAtRadius(anchor.getLocation(), radius);
            boolean safe = true;
            for (PlanetAPI planet : context.system.getPlanets()) {
                if (planet == null) continue;
                float clearance = Math.max(1400f, planet.getRadius() + 1000f);
                if (Misc.getDistance(candidate, planet.getLocation()) < clearance) {
                    safe = false;
                    break;
                }
            }
            if (safe) return candidate;
        }
        return Misc.getPointAtRadius(anchor.getLocation(),
                Math.max(5000f, anchor.getRadius() + 3500f));
    }

    private static final class FugitiveLifecycleScript implements EveryFrameScript {
        private final CampaignFleetAPI fleet;
        private final String incidentId;
        private final float hardExpireDay;
        private boolean fleeing;
        private boolean done;
        private float cleanupDay = Float.MAX_VALUE;

        private FugitiveLifecycleScript(CampaignFleetAPI fleet, String incidentId,
                                        float hardExpireDay) {
            this.fleet = fleet;
            this.incidentId = incidentId;
            this.hardExpireDay = hardExpireDay;
        }

        @Override
        public boolean isDone() {
            return done || fleet == null || !fleet.isAlive() || fleet.isExpired() ||
                    fleet.getContainingLocation() == null;
        }

        @Override
        public boolean runWhilePaused() {
            return false;
        }

        @Override
        public void advance(float amount) {
            if (isDone()) {
                done = true;
                return;
            }
            float day = PTSDCrisisState.getDay();
            if (day >= hardExpireDay || day >= cleanupDay) {
                fleet.setNoAutoDespawn(false);
                fleet.despawn();
                done = true;
                return;
            }

            CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            if (!fleeing && player != null &&
                    player.getContainingLocation() == fleet.getContainingLocation() &&
                    Misc.getDistance(player, fleet) <= APPROACH_DISTANCE) {
                fleeing = true;
                cleanupDay = day + 2f + (float) Math.random() * 2f;
                resolveIncident();
                orderEscape(player);
            }
        }

        private void resolveIncident() {
            PTSDCrisisState state = PTSDCrisisState.get();
            if (state == null) return;
            for (PTSDCrisisState.CrisisIncident incident : state.incidents) {
                if (incident != null && incidentId.equals(incident.id)) {
                    incident.investigationResolved = true;
                    incident.investigationReal = true;
                    break;
                }
            }
        }

        private void orderEscape(CampaignFleetAPI player) {
            LocationAPI location = fleet.getContainingLocation();
            Vector2f away = Vector2f.sub(fleet.getLocation(), player.getLocation(), null);
            if (away.lengthSquared() < 1f) away.set(1f, 0f);
            away.normalise();
            away.scale(18000f);
            Vector2f destination = Vector2f.add(fleet.getLocation(), away, null);
            SectorEntityToken escape = location.createToken(destination.x, destination.y);
            fleet.clearAssignments();
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                    escape, 1000f, "仓皇逃离");
        }
    }
}