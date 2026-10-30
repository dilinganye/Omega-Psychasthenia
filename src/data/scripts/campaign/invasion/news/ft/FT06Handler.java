package data.scripts.campaign.invasion.news.ft;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.BaseFleetEventListener;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.invasion.*;
import java.util.ArrayList;
import java.util.List;

public class FT06Handler extends AbstractFTHandler {
    protected String id() { return "FT-06"; }
    @Override public String unavailableReason(PTSDCrisisState state) {
        return pair(state) == null ? "没有两个可核对的非玩家相邻势力" : "可触发";
    }
    private static MarketAPI[] pair(PTSDCrisisState state) {
        List<MarketAPI> markets = Global.getSector().getEconomy().getMarketsCopy();
        MarketAPI a = null, b = null;
        float shortest = Float.MAX_VALUE;
        for (MarketAPI x : markets) for (MarketAPI y : markets) {
            if (x == y || x.getStarSystem() == null || y.getStarSystem() == null ||
                    x.isPlanetConditionMarketOnly() || y.isPlanetConditionMarketOnly() ||
                    x.getFactionId().equals(y.getFactionId()) ||
                    x.getFactionId().equals(Global.getSector().getPlayerFaction().getId()) ||
                    y.getFactionId().equals(Global.getSector().getPlayerFaction().getId()) ||
                    x.getFactionId().equals(IIRT_Omega_Invasion.WATCHER_FACTION) ||
                    x.getFactionId().equals(IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION) ||
                    y.getFactionId().equals(IIRT_Omega_Invasion.WATCHER_FACTION) ||
                    y.getFactionId().equals(IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION)) continue;
            float distance = Misc.getDistance(x.getStarSystem().getLocation(), y.getStarSystem().getLocation());
            if (distance < shortest) { a = x; b = y; shortest = distance; }
        }
        return a == null ? null : new MarketAPI[]{a,b};
    }
    @Override public void start(PTSDCrisisState state, PTSDLateState.FTChain chain) {
        MarketAPI[] pair = pair(state);
        if (pair == null) return;
        chain.factionA = pair[0].getFactionId();
        chain.factionB = pair[1].getFactionId();
        chain.systemId = pair[0].getStarSystem().getId();
        chain.marketId = pair[0].getId();
        chain.targetId = pair[0].getPrimaryEntity().getId();
        chain.node = "DISPUTE";
        chain.outcome = "两方使用不同的采样时间标注同一航道；各自派出核验舰。";
        chain.expiresDay = chain.startedDay + 35f;
    }
    @Override public String[] actions(PTSDCrisisState state, PTSDLateState.FTChain chain) {
        return chain.resolved ? new String[0] : new String[]{"调查争议星系"};
    }
    @Override public boolean choose(PTSDCrisisState state, PTSDLateState.FTChain chain, String action) {
        if (!"调查争议星系".equals(action) || !inSystem(chain) || chain.resolved) return false;
        chain.node = "INVESTIGATING";
        chain.outcome = "恒星周围的采样记录有缺口。继续靠近可能遇见真实采集舰。";
        return true;
    }
    @Override public void advanceChain(PTSDCrisisState state, PTSDLateState.FTChain chain, float day) {
        StarSystemAPI system = state.resolveSystem(chain.systemId);
        if (system == null) return;
        if (chain.fleetId == null && chain.factionA != null && system.getStar() != null) {
            CampaignFleetAPI patrol = Global.getFactory().createEmptyFleet(chain.factionA,
                    "边界核验舰", true);
            patrol.getFleetData().addFleetMember(Global.getFactory().createFleetMember(
                    FleetMemberType.SHIP, "hammerhead_Balanced"));
            { system.addEntity(patrol); data.scripts.campaign.invasion.PTSDCampaignRuntime.entityAdded(patrol); }
            patrol.setLocation(system.getStar().getLocation().x + 2600f,
                    system.getStar().getLocation().y);
            patrol.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, system.getStar(), 35f, "核验边界标记");
            patrol.getEventListeners().add(new DisputeBattleListener(patrol.getId()));
            chain.fleetId = patrol.getId();
        }
        if ("INVESTIGATING".equals(chain.node) && chain.steps == 0 && inSystem(chain) &&
                system.getStar() != null && Misc.getDistance(player(), system.getStar()) < 4000f &&
                state.late.strategicReserve >= 24f) {
            chain.steps = 1;
            if (RANDOM.nextFloat() >= .15f) {
                chain.outcome = "本次恒星采样窗口没有发现可追踪舰影；机会已经过去。";
                return;
            }
            CampaignFleetAPI sampler = PTSDEntropyTransportFactory.create(state, system,
                    system.getStar().getLocation().x + 1800f,
                    system.getStar().getLocation().y + 900f, 24f, "FT-06 恒星采样编队");
            if (sampler != null) {
                sampler.getMemoryWithoutUpdate().set("$PTSD_ft06_sampler", true);
                chain.reservedAssets += 24f;
                sampler.addAssignment(FleetAssignment.ORBIT_PASSIVE, system.getStar(),
                        8f, "绕恒星采样");
                chain.secondaryId = sampler.getId();
                chain.triggered = true;
                chain.outcome = "恒星附近出现一支未知采样编队；两方核验舰均未公开解释。";
            }
        }
        if (day >= chain.expiresDay) finish(chain, "双方各自维持航道声明；没有新的身份归因");
    }
    public static final class DisputeBattleListener extends BaseFleetEventListener {
        private final String fleetId;
        public DisputeBattleListener(String fleetId) { this.fleetId = fleetId; }
        @Override public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI winner, BattleAPI battle) {
            if (fleet == null || battle == null || !battle.isDone() || !battle.isPlayerInvolved() ||
                    !battle.wasFleetDefeated(fleet, winner)) return;
            PTSDCrisisState state = PTSDCrisisState.get();
            if (state == null || state.late == null) return;
            PTSDLateState.FTChain chain = state.late.ftChains.get("FT-06");
            if (chain == null || chain.resolved || !fleetId.equals(chain.fleetId) ||
                    !state.late.committedIds.add("ft06-raid:" + fleetId)) return;
            CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            chain.identityExposed = player != null && player.isTransponderOn();
            if (chain.identityExposed) {
                Global.getSector().getFaction(chain.factionA).adjustRelationship(
                        Global.getSector().getPlayerFaction().getId(), -.08f);
                finish(chain, "袭击身份暴露；受害势力向玩家追责");
            } else {
                Global.getSector().getFaction(chain.factionA).adjustRelationship(chain.factionB, -.06f);
                Global.getSector().getFaction(chain.factionB).adjustRelationship(chain.factionA, -.06f);
                PTSDCrisisState.SystemData local = state.getSystemData(chain.systemId);
                if (local != null) local.humanDefenseWeight += .2f;
                finish(chain, "袭击者未被识别；两方互相指责并增强争议星系防务");
            }
        }
    }
}
