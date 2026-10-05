package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

/** Player observations, own orders and delivered reports; separate from Omega's evidence pool. */
public final class PTSDHumanWarReports {
    private PTSDHumanWarReports() { }

    public static boolean member(PTSDCrisisState state, String faction) {
        return state != null && state.late != null && state.late.coalitionMembers.contains(faction);
    }

    public static boolean known(PTSDCrisisState state, StarSystemAPI system) {
        if (system == null) return false;
        if (system.isEnteredByPlayer()) return true;
        for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system))
            if (!market.isPlanetConditionMarketOnly() && (market.isPlayerOwned() || member(state, market.getFactionId())))
                return true;
        for (PTSDCrisisState.HumanReport report : state.humanWarReports.values())
            if (report != null && system.getId().equals(report.systemId)) return true;
        return false;
    }

    public static void advance(PTSDCrisisState state, float day) {
        if (state == null || day < state.nextHumanReportDay) return;
        state.nextHumanReportDay = day + .25f;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player != null && player.getStarSystem() != null) {
            for (CampaignFleetAPI fleet : player.getStarSystem().getFleets()) {
                if (fleet == player || !fleet.isVisibleToPlayerFleet()) continue;
                String faction = fleet.getFaction().getId();
                boolean omega = IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(faction) ||
                        IIRT_Omega_Invasion.WATCHER_FACTION.equals(faction);
                if (!omega && !member(state, faction) && !fleet.getFaction().isPlayerFaction()) continue;
                PTSDCrisisState.HumanReport report = new PTSDCrisisState.HumanReport();
                report.id = "sighting:" + fleet.getId(); report.systemId = player.getStarSystem().getId();
                PTSDCrisisState.HumanReport previous = state.humanWarReports.get(report.id);
                if (previous != null) {
                    report.sourceSystemId = !previous.systemId.equals(report.systemId) ? previous.systemId : previous.sourceSystemId;
                    report.movement = report.sourceSystemId != null;
                }
                report.factionId = faction; report.source = "本舰传感器"; report.day = day;
                // Deliberately coarse; an observed contact is not the enemy's strategic budget.
                report.strength = Math.round(fleet.getFleetPoints() / 10f) * 10f;
                report.omega = omega; report.battle = fleet.getBattle() != null;
                report.text = fleet.getName() + (report.battle ? "：观测到交战" : "：局部舰队接触");
                state.humanWarReports.put(report.id, report);
            }
        }
        for (PTSDCrisisState.StrategicEvent event : state.getActiveEvents()) {
            if (!PTSDCrisisAPI.SIDE_HUMAN.equals(event.side) || event.targetSystemId == null ||
                    !(member(state, event.factionId) || Global.getSector().getPlayerFaction().getId().equals(event.factionId)))
                continue;
            PTSDCrisisState.HumanReport report = new PTSDCrisisState.HumanReport();
            report.id = "order:" + event.id; report.systemId = event.targetSystemId;
            report.sourceSystemId = event.sourceSystemId; report.factionId = event.factionId;
            report.source = "已签收调令"; report.day = day; report.strength = event.strength;
            report.text = "已承诺舰队行动"; report.movement = true;
            state.humanWarReports.put(report.id, report);
        }
        if (state.late != null) for (PTSDLateState.Alert alert : state.late.alerts) {
            if (alert == null || !alert.delivered || alert.systemId == null) continue;
            String id = "alert:" + alert.id;
            if (state.humanWarReports.containsKey(id)) continue;
            StarSystemAPI system = state.resolveSystem(alert.systemId);
            if (system == null || !known(state, system)) continue;
            PTSDCrisisState.HumanReport report = new PTSDCrisisState.HumanReport();
            report.id = id; report.systemId = alert.systemId; report.source = alert.source;
            report.day = alert.day; report.text = alert.text; report.battle = true;
            state.humanWarReports.put(id, report);
        }
    }

    public static PTSDCrisisState.HumanReport latest(PTSDCrisisState state, String systemId) {
        PTSDCrisisState.HumanReport best = null;
        for (PTSDCrisisState.HumanReport report : state.humanWarReports.values())
            if (report != null && systemId.equals(report.systemId) && (best == null || report.day > best.day)) best = report;
        return best;
    }
}
