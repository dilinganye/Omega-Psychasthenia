package data.scripts.campaign.invasion;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** A saved real-second clock; paused time is excluded and reloading resets the wall anchor. */
public final class PTSDFortressEscapeScript implements EveryFrameScript {
    private long wallAnchorNanos;
    private Map<String, Float> hiddenTrapSeconds = new HashMap<String, Float>();
    private Map<String, Float> hiddenReserveSeconds = new HashMap<String, Float>();
    private transient InteractionDialogAPI observedDialog;
    private transient Map<String, Integer> countdownBuckets;

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return true; }

    @Override public void advance(float amount) {
        long now = System.nanoTime();
        if (wallAnchorNanos == 0L) { wallAnchorNanos = now; return; }
        float seconds = Math.min(1f, Math.max(0f, (now - wallAnchorNanos) / 1000000000f));
        wallAnchorNanos = now;
        if (Global.getSector() == null) return;
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null && state.late != null) observeSupplyVisit(state);
        if (Global.getSector().isPaused()) return;
        advanceTraps(seconds);
        if (state == null || state.late == null) return;
        recordWitnessedPass(state);
        advanceReserves(state, seconds);
        offerWarning(state);
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null) return;
        if (countdownBuckets == null) countdownBuckets = new HashMap<String, Integer>();
        for (PTSDLateState.Detonation d : state.late.detonations.values()) {
            if (d == null || d.exploded) continue;
            if (player.getStarSystem() != null && d.systemId != null &&
                    d.systemId.equals(player.getStarSystem().getId())) {
                int bucket = Math.max(0, (int) Math.ceil(d.remainingSeconds / 30f));
                Integer prior = countdownBuckets.get(d.fortressId);
                if (prior == null || prior.intValue() != bucket) {
                    countdownBuckets.put(d.fortressId, bucket);
                    Global.getSector().getCampaignUI().addMessage(
                            "视界坍缩：撤离剩余约 " + Math.round(d.remainingSeconds) + " 秒",
                            Color.RED);
                    if (player.getStarSystem().getStar() != null)
                        Global.getSector().addPing(player.getStarSystem().getStar(),
                                "sensor_burst", new Color(190, 65, 225));
                }
            }
            d.battleEscaped = player.getStarSystem() == null ||
                    !d.systemId.equals(player.getStarSystem().getId());
            d.remainingSeconds = Math.max(0f, d.remainingSeconds - seconds);
            if (d.remainingSeconds <= 0f) explode(state, d, player);
        }
    }

    /** A nearby Omega witness can upload a port visit, never the player's private cargo. */
    private void observeSupplyVisit(PTSDCrisisState state) {
        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        if (dialog == null) { observedDialog = null; return; }
        if (dialog == observedDialog) return;
        observedDialog = dialog;
        if (state.phase == PTSDCrisisState.Phase.DORMANT ||
                state.phase == PTSDCrisisState.Phase.RECON || dialog.getInteractionTarget() == null) return;
        MarketAPI market = dialog.getInteractionTarget().getMarket();
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (market == null || player == null || player.getStarSystem() == null ||
                market.getStarSystem() != player.getStarSystem()) return;
        float day = PTSDCrisisState.getDay();
        Float last = state.late.lastSupplyVisitDay.get(market.getId());
        if (last != null && day - last < 1f) return;
        for (CampaignFleetAPI witness : player.getStarSystem().getFleets()) {
            if (!witness.hasTag("PTSD_crisis_fleet") ||
                    Misc.getDistance(player, witness) > 3500f) continue;
            state.late.lastSupplyVisitDay.put(market.getId(), day);
            Integer old = state.late.witnessedSupplyVisits.get(market.getId());
            int visits = (old == null ? 0 : old) + 1;
            state.late.witnessedSupplyVisits.put(market.getId(), visits);
            PTSDThreatDirector.recordAction(state, "SUPPLY", "witnessed-port:" + market.getId() + ":" + visits,
                    player.getStarSystem().getId(), Math.min(65f, 15f + visits * 12f),
                    .7f, true, 50f);
            break;
        }
    }

    private void recordWitnessedPass(PTSDCrisisState state) {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null) return;
        String systemId = player.getStarSystem() == null ? null : player.getStarSystem().getId();
        if (systemId == null) { state.late.lastPlayerSystemId = null; return; }
        if (systemId.equals(state.late.lastPlayerSystemId)) return;
        state.late.lastPlayerSystemId = systemId;
        if (state.phase == PTSDCrisisState.Phase.DORMANT ||
                state.phase == PTSDCrisisState.Phase.RECON) return;
        for (CampaignFleetAPI witness : player.getStarSystem().getFleets()) {
            if (!witness.hasTag("PTSD_crisis_fleet") ||
                    Misc.getDistance(player, witness) > 3500f) continue;
            Integer old = state.late.witnessedPasses.get(systemId);
            int passes = (old == null ? 0 : old) + 1;
            state.late.witnessedPasses.put(systemId, passes);
            PTSDThreatDirector.recordAction(state, "MOBILITY", "pass:" + systemId + ":" + passes,
                    systemId, Math.min(50f, passes * 12f), .65f, true, 55f);
            break;
        }
    }

    private void advanceReserves(PTSDCrisisState state, float seconds) {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null) return;
        for (PTSDLateState.Reserve reserve : state.late.reserves) {
            if (reserve == null || reserve.fleetId == null || reserve.destroyed) continue;
            CampaignFleetAPI fleet = PTSDFortressDirector.findFleet(reserve.fleetId);
            if (fleet == null) continue;
            boolean visible = fleet.getContainingLocation() == player.getContainingLocation() &&
                    Misc.getDistance(fleet, player) < 7000f;
            if (visible) { hiddenReserveSeconds.remove(reserve.id); continue; }
            Float previous = hiddenReserveSeconds.get(reserve.id);
            float elapsed = (previous == null ? 0f : previous) + seconds;
            if (elapsed >= 5f) {
                PTSDReserveDirector.dematerialize(reserve, fleet);
                hiddenReserveSeconds.remove(reserve.id);
            } else hiddenReserveSeconds.put(reserve.id, elapsed);
        }
    }

    private void advanceTraps(float seconds) {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null || player.getContainingLocation() == null) return;
        for (CampaignFleetAPI fleet : new ArrayList<CampaignFleetAPI>(
                player.getContainingLocation().getFleets())) {
            if (!fleet.hasTag("PTSD_late_trap")) continue;
            if (fleet.isVisibleToPlayerFleet()) {
                hiddenTrapSeconds.remove(fleet.getId());
                if (!fleet.getMemoryWithoutUpdate().getBoolean("$PTSD_emergency_chase") &&
                        Misc.getDistance(fleet, player) < 2800f) {
                    float averageCR = 0f;
                    int members = 0;
                    for (FleetMemberAPI member : player.getFleetData().getMembersListCopy()) {
                        averageCR += member.getRepairTracker().getCR();
                        members++;
                    }
                    if (members > 0 && averageCR / members >= .45f &&
                            fleet.getAbility("emergency_burn") != null) {
                        fleet.getAbility("emergency_burn").activate();
                    }
                    fleet.getMemoryWithoutUpdate().set("$PTSD_emergency_chase", true);
                }
            } else {
                // A lure stays in the world so it can draw the player into a later battle.
                hiddenTrapSeconds.remove(fleet.getId());
            }
        }
    }

    private void offerWarning(PTSDCrisisState state) {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null) return;
        for (PTSDLateState.Fortress fortress : state.late.fortresses.values()) {
            if (fortress == null || fortress.destroyed || fortress.tier < 4) continue;
            if (fortress.autoSavePending && !fortress.autoSaveAttempted &&
                    !Global.getSector().getCampaignUI().isShowingDialog()) {
                fortress.autoSavePending = false;
                fortress.autoSaveAttempted = true;
                Global.getSector().getCampaignUI().autosave();
            }
            if (fortress.warnedPlayer || Global.getSector().getCampaignUI().isShowingDialog()) continue;
            CampaignFleetAPI station = PTSDFortressDirector.findFleet(fortress.fleetId);
            if (station == null || station.getContainingLocation() != player.getContainingLocation() ||
                    Misc.getDistance(station, player) > 3500f) continue;
            fortress.warnedPlayer = true;
            Global.getSector().getCampaignUI().showInteractionDialog(
                    new PTSDFortressWarningDialog(fortress.id), station);
            break;
        }
    }

    public static void explode(PTSDCrisisState state, PTSDLateState.Detonation d,
                               CampaignFleetAPI player) {
        if (d == null || d.exploded) return;
        d.exploded = true;
        StarSystemAPI system = state.resolveSystem(d.systemId);
        if (system == null) return;
        if (state.late.transformedSystems.add(system.getId())) {
            for (MarketAPI market : new ArrayList<MarketAPI>(Global.getSector().getEconomy().getMarketsCopy())) {
                if (market == null || market.getStarSystem() != system ||
                        market.isPlanetConditionMarketOnly()) continue;
                abandonMarket(market);
            }
            if (system.getStar() != null) {
                system.addAsteroidBelt(system.getStar(), 180, 2600f, 900f, 100f, 250f);
                system.getStar().getSpec().setGlowColor(new Color(130, 40, 180, 100));
                system.getStar().applySpecChanges();
            }
            for (PlanetAPI planet : system.getPlanets()) {
                if (planet == null || planet == system.getStar()) continue;
                planet.getSpec().setPlanetColor(new Color(86, 63, 105));
                planet.getSpec().setCloudColor(new Color(95, 70, 115, 180));
                planet.getSpec().setAtmosphereColor(new Color(75, 45, 100, 130));
                planet.applySpecChanges();
            }
        }
        if (player != null && player.getStarSystem() == system) {
            for (FleetMemberAPI member : new ArrayList<FleetMemberAPI>(player.getFleetData().getMembersListCopy())) {
                player.getFleetData().removeFleetMember(member);
            }
            Global.getSector().getCampaignUI().addMessage("视界坍缩：仍处于星系内的舰队全灭。", Color.RED);
        } else {
            Global.getSector().getCampaignUI().addMessage("已脱离视界坍缩区。", Color.ORANGE);
        }
    }

    public static void abandonMarket(MarketAPI market) {
        if (market == null || market.isPlanetConditionMarketOnly() ||
                market.getMemoryWithoutUpdate().getBoolean("$PTSD_permanently_abandoned")) return;
        market.getMemoryWithoutUpdate().set("$PTSD_permanently_abandoned", true);
        if (!market.hasCondition(Conditions.DECIVILIZED)) market.addCondition(Conditions.DECIVILIZED);
        market.setPlayerOwned(false);
        market.setFactionId(Factions.NEUTRAL);
        if (market.getPrimaryEntity() != null) market.getPrimaryEntity().setFaction(Factions.NEUTRAL);
        market.setPlanetConditionMarketOnly(true);
        market.setSize(1);
        market.clearCommodities();
        for (Industry industry : new ArrayList<Industry>(market.getIndustries())) {
            market.removeIndustry(industry.getId(), null, false);
        }
        for (SubmarketAPI submarket : new ArrayList<SubmarketAPI>(market.getSubmarketsCopy())) {
            market.removeSubmarket(submarket.getSpecId());
        }
        if (market.isInEconomy()) Global.getSector().getEconomy().removeMarket(market);
    }
}
