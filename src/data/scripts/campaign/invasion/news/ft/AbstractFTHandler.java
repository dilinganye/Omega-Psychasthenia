package data.scripts.campaign.invasion.news.ft;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.invasion.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Shared physical and accounting helpers; narrative branching lives in each FT handler. */
public abstract class AbstractFTHandler implements PTSDCrisisNewsAPI.ChainNewsHandler {
    protected static final Random RANDOM = new Random();
    protected abstract String id();

    public PTSDCrisisNewsAPI.TargetSelection pick(PTSDCrisisNewsAPI.PickContext context) {
        StarSystemAPI system = chooseSystem(context.state);
        return system == null ? null : new PTSDCrisisNewsAPI.TargetSelection(system,
                chooseMarket(system), system.getStar());
    }

    public String unavailableReason(PTSDCrisisState state) { return "可触发"; }

    public void start(PTSDCrisisState state, PTSDLateState.FTChain chain) {
        PTSDCrisisNewsAPI.TargetSelection target = pick(new PTSDCrisisNewsAPI.PickContext(
                state, PTSDCrisisState.getDay(), RANDOM, id(), ""));
        if (target == null || target.system == null) return;
        chain.systemId = target.system.getId();
        chain.marketId = target.market == null ? null : target.market.getId();
        chain.targetId = target.targetLocation == null ? null : target.targetLocation.getId();
        chain.node = "OPEN";
    }

    public void advanceChain(PTSDCrisisState state, PTSDLateState.FTChain chain, float day) {
        if (day >= chain.expiresDay) finish(chain, "线索时限届满；现场记录已归档");
    }

    public boolean choose(PTSDCrisisState state, PTSDLateState.FTChain chain, String action) { return false; }

    public String status(PTSDLateState.FTChain chain) {
        return chain.outcome == null ? "调查尚在进行" : chain.outcome;
    }

    public String[] actions(PTSDCrisisState state, PTSDLateState.FTChain chain) {
        return new String[0];
    }

    protected static void finish(PTSDLateState.FTChain chain, String outcome) {
        chain.resolved = true;
        chain.node = "CLOSED";
        chain.outcome = outcome;
        PTSDCrisisDevIntel.report("FT 事件结算", chain.id + "：" + outcome,
                chain.systemId, chain.targetId);
    }

    protected static StarSystemAPI chooseSystem(PTSDCrisisState state) {
        if (Global.getSector() == null) return null;
        List<StarSystemAPI> systems = new ArrayList<StarSystemAPI>();
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system == null || system.hasTag(com.fs.starfarer.api.impl.campaign.ids.Tags.THEME_HIDDEN) ||
                    chooseMarket(system) == null) continue;
            systems.add(system);
        }
        if (systems.isEmpty()) return state.resolveSystem(state.baseSystemId);
        return systems.get(RANDOM.nextInt(systems.size()));
    }

    protected static MarketAPI chooseMarket(StarSystemAPI system) {
        if (system == null) return null;
        for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system))
            if (market != null && !market.isPlanetConditionMarketOnly() &&
                    !market.isHidden() && market.getPrimaryEntity() != null &&
                    !"omega".equalsIgnoreCase(market.getFactionId())) return market;
        return null;
    }

    protected static CampaignFleetAPI player() { return Global.getSector().getPlayerFleet(); }

    protected static boolean playerNear(PTSDLateState.FTChain chain, float radius) {
        CampaignFleetAPI player = player();
        SectorEntityToken target = chain.targetId == null ? null : Global.getSector().getEntityById(chain.targetId);
        return player != null && target != null && player.getContainingLocation() == target.getContainingLocation() &&
                Misc.getDistance(player, target) <= radius;
    }

    protected static CampaignFleetAPI spawn(PTSDCrisisState state, PTSDLateState.FTChain chain,
                                            String name, String variant, float x, float y,
                                            float assetCost, String memoryKey) {
        StarSystemAPI system = state.resolveSystem(chain.systemId);
        if (system == null || state.late.strategicReserve < assetCost) return null;
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                PTSDCrisisProgress.getActiveFactionId(state), name, true);
        fleet.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant));
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(false);
        fleet.addTag("PTSD_crisis_fleet");
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF, true);
        fleet.getMemoryWithoutUpdate().set("$PTSD_ft_id", chain.id);
        if (memoryKey != null) fleet.getMemoryWithoutUpdate().set(memoryKey, true);
        fleet.getEventListeners().add(new PTSDStrategicFleetListener(null, fleet.getFleetPoints()));
        system.addEntity(fleet);
        fleet.setLocation(x, y);
        state.late.strategicReserve -= assetCost;
        PTSDLateWarDirector.recordSpending(state, chain.id + " 实体舰队", 0f, assetCost);
        chain.reservedAssets += assetCost;
        return fleet;
    }

    protected static void routeTo(CampaignFleetAPI fleet, SectorEntityToken target, String text) {
        if (fleet == null || target == null) return;
        fleet.clearAssignments();
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, target, 30f, text);
    }

    protected static boolean inSystem(PTSDLateState.FTChain chain) {
        CampaignFleetAPI player = player();
        return player != null && player.getStarSystem() != null &&
                player.getStarSystem().getId().equals(chain.systemId);
    }
}
