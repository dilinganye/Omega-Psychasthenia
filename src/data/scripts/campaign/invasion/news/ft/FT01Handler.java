package data.scripts.campaign.invasion.news.ft;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.invasion.*;

public final class FT01Handler extends AbstractFTHandler {
    protected String id() { return "FT-01"; }
    @Override public void start(PTSDCrisisState state, PTSDLateState.FTChain chain) {
        super.start(state, chain);
        chain.node = "PERMITS_SUSPENDED";
        chain.outcome = "导航机构自行停服。护航与私改许可可分别调查。";
        chain.expiresDay = chain.startedDay + 45f;
    }
    @Override public String[] actions(PTSDCrisisState state, PTSDLateState.FTChain chain) {
        if (chain.resolved) return new String[0];
        if (!chain.escortSigned && !chain.permitObtained)
            return new String[]{"接受护航委托（定金25000）", "寻找私改许可"};
        if (!chain.escortSigned) return new String[]{"接受护航委托（定金25000）"};
        if (!chain.permitObtained) return new String[]{"寻找私改许可"};
        return new String[0];
    }
    @Override public boolean choose(PTSDCrisisState state, PTSDLateState.FTChain chain, String action) {
        if (chain.resolved) return false;
        if (action.startsWith("寻找私改许可") && !chain.permitObtained) {
            chain.permitObtained = true;
            chain.outcome = "港口外的中介递来一份改写许可。它能放行一艘船，却无法替其他人排队。";
            return true;
        }
        if (action.startsWith("接受护航委托") && !chain.escortSigned) {
            MarketAPI market = state.resolveMarket(chain.marketId);
            if (market == null || market.getPrimaryEntity() == null) return false;
            CampaignFleetAPI escort = Global.getFactory().createEmptyFleet(
                    market.getFactionId(), "待护送商船", true);
            escort.getFleetData().addFleetMember(Global.getFactory().createFleetMember(
                    FleetMemberType.SHIP, "buffalo2_Standard"));
            market.getStarSystem().addEntity(escort);
            escort.setLocation(market.getPrimaryEntity().getLocation().x + 800f,
                    market.getPrimaryEntity().getLocation().y + 800f);
            if (escort == null) return false;
            Global.getSector().getPlayerFleet().getCargo().getCredits().add(25000f);
            chain.paidDeposit = true;
            chain.escortSigned = true;
            chain.fleetId = escort.getId();
            chain.node = "ESCORTING";
            chain.nextDay = PTSDCrisisState.getDay();
            chain.outcome = "已收取25000星币定金。目标船将驶往另一座有人港口；抵达后结算尾款。";
            MarketAPI destination = null;
            for (MarketAPI candidate : Global.getSector().getEconomy().getMarketsCopy())
                if (candidate != null && candidate.getPrimaryEntity() != null &&
                        !candidate.getId().equals(market.getId()) && !candidate.isPlanetConditionMarketOnly()) {
                    destination = candidate; break;
                }
            if (destination != null) {
                chain.secondaryId = destination.getPrimaryEntity().getId();
                escort.addAssignment(FleetAssignment.DELIVER_CREW, destination.getPrimaryEntity(),
                        45f, "沿临时许可航行");
            }
            return true;
        }
        return false;
    }
    @Override public void advanceChain(PTSDCrisisState state, PTSDLateState.FTChain chain, float day) {
        if (chain.escortSigned && !chain.paidTail) {
            CampaignFleetAPI escort = PTSDFortressDirector.findFleet(chain.fleetId);
            if (escort == null || !escort.isAlive()) {
                chain.node = "ESCORT_FAILED";
                chain.outcome = "护航船失联；25000星币定金保留，无75000星币尾款";
                chain.paidTail = true;
                return;
            }
            CampaignFleetAPI player = player();
            if (player != null && player.getContainingLocation() == escort.getContainingLocation() &&
                    Misc.getDistance(player, escort) <= 5500f) chain.nextDay = day;
            if (day - chain.nextDay > 2f) {
                chain.node = "ESCORT_FAILED";
                chain.outcome = "护航舰队离开商船超过两日；委托失败，尾款取消";
                chain.paidTail = true;
                return;
            }
            com.fs.starfarer.api.campaign.SectorEntityToken destination =
                    Global.getSector().getEntityById(chain.secondaryId);
            if (destination != null && escort.getContainingLocation() == destination.getContainingLocation() &&
                    Misc.getDistance(escort, destination) < 700f) {
                chain.paidTail = true;
                Global.getSector().getPlayerFleet().getCargo().getCredits().add(75000f);
                escort.clearAssignments();
                escort.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, destination,
                        3f, "卸载乘客");
                chain.node = "ESCORT_COMPLETE";
                chain.outcome = "护送到港；另收75000星币尾款";
                return;
            }
        }
        if (day >= chain.expiresDay) finish(chain, chain.escortSigned ?
                "护航逾期；定金保留，尾款取消" : "许可窗口关闭；没有签署护航委托");
    }
}
