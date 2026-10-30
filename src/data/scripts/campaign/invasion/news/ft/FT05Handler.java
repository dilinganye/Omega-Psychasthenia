package data.scripts.campaign.invasion.news.ft;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.cargo.PTSDOmegaFleetSupport;
import data.scripts.campaign.invasion.*;

public class FT05Handler extends AbstractFTHandler {
    protected String id() { return "FT-05"; }
    @Override public void start(PTSDCrisisState state, PTSDLateState.FTChain chain) {
        super.start(state, chain);
        MarketAPI market = state.resolveMarket(chain.marketId);
        if (market == null || market.getPrimaryEntity() == null) { chain.node = null; return; }
        CampaignFleetAPI survivor = Global.getFactory().createEmptyFleet(market.getFactionId(),
                "遇袭幸存舰", true);
        survivor.getFleetData().addFleetMember(Global.getFactory().createFleetMember(
                FleetMemberType.SHIP, "buffalo2_Standard"));
        market.getStarSystem().addEntity(survivor);
        survivor.setLocation(market.getPrimaryEntity().getLocation().x + 700f,
                market.getPrimaryEntity().getLocation().y + 700f);
        survivor.addAssignment(FleetAssignment.ORBIT_PASSIVE, market.getPrimaryEntity(),
                45f, "等待港务调查");
        chain.fleetId = survivor.getId();
        chain.node = "SURVIVOR_WAITING";
        float nearest = Float.MAX_VALUE;
        for (com.fs.starfarer.api.campaign.StarSystemAPI candidate : Global.getSector().getStarSystems()) {
            if (candidate == null || candidate == market.getStarSystem() ||
                    candidate.hasTag(com.fs.starfarer.api.impl.campaign.ids.Tags.THEME_HIDDEN)) continue;
            float distance = Misc.getDistance(candidate.getLocation(), market.getStarSystem().getLocation());
            if (distance < nearest) { nearest = distance; chain.routeSystemId = candidate.getId(); }
        }
        chain.encounters = state.totalPlayerOmegaBattles;
        chain.outcome = "幸存舰停泊在殖民地附近。船员尚不愿提交完整航迹。";
        chain.expiresDay = chain.startedDay + 45f;
    }
    @Override public String[] actions(PTSDCrisisState state, PTSDLateState.FTChain chain) {
        if (chain.resolved) return new String[0];
        if ("SURVIVOR_WAITING".equals(chain.node) || "REFUSED".equals(chain.node))
            return new String[]{"联络幸存舰"};
        if ("NEGOTIATING".equals(chain.node)) return new String[]{"出示协议残片", "提出下一笔报价"};
        return new String[0];
    }
    @Override public boolean choose(PTSDCrisisState state, PTSDLateState.FTChain chain, String action) {
        CampaignFleetAPI survivor = PTSDFortressDirector.findFleet(chain.fleetId);
        CampaignFleetAPI player = player();
        if (chain.resolved || survivor == null || player == null ||
                player.getContainingLocation() != survivor.getContainingLocation() ||
                Misc.getDistance(player, survivor) > 1800f) return false;
        if ("联络幸存舰".equals(action)) {
            chain.node = "NEGOTIATING";
            if (chain.encounters >= 5) reveal(chain, "五次实际异常接触记录让船员愿意提供残缺航迹");
            else chain.outcome = "对方不愿再描述袭击者。你可以出示实际残片，或提出报价。";
            return true;
        }
        if ("出示协议残片".equals(action)) {
            if (!hasFragment()) return false;
            reveal(chain, "协议残片的结构令幸存船员确认你已见过类似舰只；残片未被取走");
            return true;
        }
        if ("提出下一笔报价".equals(action)) {
            int[] values = {10000, 20000, 50000, 100000};
            if (chain.offers >= values.length) return false;
            int amount = values[chain.offers];
            if (player.getCargo().getCredits().get() < amount) return false;
            chain.offers++;
            if (chain.offers == values.length) reveal(chain,
                    "听完第四次报价，船员让你收起钱。他说你既然一直追问，就自己去找那条残缺航迹");
            else chain.outcome = "提出" + amount + "星币报价。船员没有接钱，只要求你离开。";
            return true;
        }
        return false;
    }
    private static boolean hasFragment() {
        if (player() == null) return false;
        for (CargoStackAPI stack : player().getCargo().getStacksCopy())
            if (stack.isSpecialStack() && stack.getSpecialDataIfSpecial() != null &&
                    PTSDOmegaFleetSupport.FRAGMENT_ITEM_ID.equals(stack.getSpecialDataIfSpecial().getId()) &&
                    stack.getSize() >= 1f) return true;
        return false;
    }
    private static void reveal(PTSDLateState.FTChain chain, String reason) {
        chain.node = "INCOMPLETE_TRACE";
        chain.expiresDay = Math.max(chain.expiresDay, PTSDCrisisState.getDay() + 30f);
        com.fs.starfarer.api.campaign.StarSystemAPI route = PTSDCrisisState.get().resolveSystem(chain.routeSystemId);
        chain.outcome = reason + "。航迹只指向 " + (route == null ? "附近数个星系" :
                route.getName() + " 一带") + "，没有说明那支编队在运送什么。";
    }
    @Override public void advanceChain(PTSDCrisisState state, PTSDLateState.FTChain chain, float day) {
        if (day >= chain.startedDay + 45f && chain.fleetId != null) {
            CampaignFleetAPI survivor = PTSDFortressDirector.findFleet(chain.fleetId);
            if (survivor != null && survivor.getContainingLocation() != null)
                survivor.getContainingLocation().removeEntity(survivor);
            chain.fleetId = null;
            if (!"INCOMPLETE_TRACE".equals(chain.node)) {
                finish(chain, "幸存舰四十五日后离港；没有取得残缺航迹");
                return;
            }
        }
        CampaignFleetAPI player = player();
        com.fs.starfarer.api.campaign.StarSystemAPI route = state.resolveSystem(chain.routeSystemId);
        if ("INCOMPLETE_TRACE".equals(chain.node) && player != null && route != null &&
                player.getStarSystem() == route && !chain.triggered &&
                day >= chain.startedDay + 2f && state.late.strategicReserve >= 24f) {
            if (route.getStar() != null) {
                CampaignFleetAPI convoy = PTSDEntropyTransportFactory.create(state,
                        route,
                        route.getStar().getLocation().x + 5000f,
                        route.getStar().getLocation().y + 1000f, 24f, "FT-05 残缺航迹编队");
                if (convoy != null) {
                    convoy.getMemoryWithoutUpdate().set("$PTSD_ft05_convoy", true);
                    chain.reservedAssets += 24f;
                    chain.secondaryId = convoy.getId();
                    chain.triggered = true;
                    chain.outcome = "残缺航迹附近出现一支未知编队。它有自己的航路，是否追上取决于实际航速。";
                    convoy.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                            route.getHyperspaceAnchor(), 15f, "沿既定航迹航行");
                }
            }
        }
        if (day >= chain.expiresDay) finish(chain, "残缺航迹的搜索窗口结束；已有战果保留");
    }
}
