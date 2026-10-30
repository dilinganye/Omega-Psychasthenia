package data.scripts.campaign.invasion.news.ft;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.invasion.*;

public class FT02Handler extends AbstractFTHandler {
    protected String id() { return "FT-02"; }
    @Override public String unavailableReason(PTSDCrisisState state) {
        return state.late.fortresses.isEmpty() ? "尚无可核实的黑洞观测异常" : "可触发";
    }
    @Override public void start(PTSDCrisisState state, PTSDLateState.FTChain chain) {
        for (PTSDLateState.Fortress fortress : state.late.fortresses.values()) {
            if (fortress == null || fortress.destroyed) continue;
            chain.systemId = fortress.currentSystemId;
            chain.targetId = fortress.blackHoleId;
            break;
        }
        if (chain.systemId == null) return;
        java.util.List<MarketAPI> coreBars = new java.util.ArrayList<MarketAPI>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy())
            if (market != null && !market.isPlanetConditionMarketOnly() &&
                    market.getStarSystem() != null && market.getPrimaryEntity() != null &&
                    market.getStarSystem().getLocation().length() < 22000f)
                coreBars.add(market);
        MarketAPI bar = coreBars.isEmpty() ? chooseMarket(chooseSystem(state)) :
                coreBars.get(RANDOM.nextInt(coreBars.size()));
        chain.marketId = bar == null ? null : bar.getId();
        chain.node = "WITHDRAWN_CURVE";
        chain.outcome = "原始曲线只留下若干相邻星系的方位。观测台当班人员可能在核心世界酒馆。";
        chain.expiresDay = chain.startedDay + 60f;
    }
    @Override public String[] actions(PTSDCrisisState state, PTSDLateState.FTChain chain) {
        if ("WITHDRAWN_CURVE".equals(chain.node)) return new String[]{"查阅旧数据"};
        if ("BAR_EVADING".equals(chain.node)) return new String[]{"支付10000星币", "花费1故事点施压"};
        return new String[0];
    }
    @Override public boolean choose(PTSDCrisisState state, PTSDLateState.FTChain chain, String action) {
        if (chain.resolved) return false;
        if ("查阅旧数据".equals(action)) {
            chain.node = "ARCHIVE_REGION";
            StarSystemAPI target = state.resolveSystem(chain.systemId);
            java.util.List<StarSystemAPI> nearby = new java.util.ArrayList<StarSystemAPI>();
            for (StarSystemAPI candidate : Global.getSector().getStarSystems())
                if (candidate != null && target != null && !candidate.hasTag(
                        com.fs.starfarer.api.impl.campaign.ids.Tags.THEME_HIDDEN)) nearby.add(candidate);
            java.util.Collections.sort(nearby, new java.util.Comparator<StarSystemAPI>() {
                public int compare(StarSystemAPI a, StarSystemAPI b) {
                    return Float.compare(Misc.getDistance(a.getLocation(), target.getLocation()),
                            Misc.getDistance(b.getLocation(), target.getLocation()));
                }
            });
            java.util.List<String> names = new java.util.ArrayList<String>();
            for (StarSystemAPI candidate : nearby) {
                names.add(candidate.getName());
                if (names.size() >= 4) break;
            }
            java.util.Collections.shuffle(names, RANDOM);
            chain.outcome = "旧索引涉及：" + names.toString() + "。必须逐个前往比对；记录没有说明异常来源。";
            return true;
        }
        if ("与观测员交谈".equals(action) &&
                ("WITHDRAWN_CURVE".equals(chain.node) || "ARCHIVE_REGION".equals(chain.node))) {
            CampaignFleetAPI player = player();
            MarketAPI bar = state.resolveMarket(chain.marketId);
            com.fs.starfarer.api.campaign.InteractionDialogAPI dialog =
                    Global.getSector().getCampaignUI().getCurrentInteractionDialog();
            if (player == null || bar == null || dialog == null ||
                    dialog.getInteractionTarget() == null ||
                    dialog.getInteractionTarget().getMarket() != bar) return false;
            chain.node = "BAR_EVADING";
            chain.outcome = "穿着维修服的观测员说曲线是旧仪器误差。他反复看向酒馆出口。";
            return true;
        }
        if ("BAR_EVADING".equals(chain.node) && "支付10000星币".equals(action)) {
            if (player().getCargo().getCredits().get() < 10000f) return false;
            player().getCargo().getCredits().subtract(10000f);
            chain.node = "EXACT_COORDINATES";
            chain.outcome = "观测员收下10000星币，写出一组精确坐标。他说自己没有见到那里有什么。";
            return true;
        }
        if ("BAR_EVADING".equals(chain.node) && "花费1故事点施压".equals(action)) {
            if (Global.getSector().getPlayerStats().getStoryPoints() < 1) return false;
            Global.getSector().getPlayerStats().spendStoryPoints(1, true, null, false, "FT-02 追问观测员");
            chain.node = "EXACT_COORDINATES";
            chain.outcome = "他终于交出观测坐标，请求不要再联系他。";
            return true;
        }
        return false;
    }
    @Override public void advanceChain(PTSDCrisisState state, PTSDLateState.FTChain chain, float day) {
        if ("ARCHIVE_REGION".equals(chain.node) || "EXACT_COORDINATES".equals(chain.node)) {
            CampaignFleetAPI player = player();
            StarSystemAPI system = state.resolveSystem(chain.systemId);
            if (player != null && system != null && player.getStarSystem() == system &&
                    state.late.strategicReserve >= 40f) {
                float x = system.getStar() == null ? 0f : system.getStar().getLocation().x + 3800f;
                float y = system.getStar() == null ? 0f : system.getStar().getLocation().y + 1200f;
                CampaignFleetAPI transport = PTSDEntropyTransportFactory.create(state, system,
                        x, y, 24f, "FT-02 实体运输");
                if (transport != null) {
                    transport.getMemoryWithoutUpdate().set("$PTSD_ft02_transport", true);
                    chain.reservedAssets += 24f;
                    for (PTSDLateState.Fortress fortress : state.late.fortresses.values())
                        if (fortress != null && !fortress.destroyed &&
                                chain.systemId.equals(fortress.currentSystemId)) {
                            fortress.transportIds.add(transport.getId());
                            transport.getMemoryWithoutUpdate().set("$PTSD_fortress_transport", fortress.id);
                            break;
                        }
                    chain.fleetId = transport.getId();
                    if (system.getStar() != null) routeTo(transport, system.getStar(), "驶向内侧轨道");
                    for (int i = 0; i < 2; i++) {
                        CampaignFleetAPI escort = spawn(state, chain, "未知护卫编队", "Omega_Scrap_Heap_Garbage",
                                x + (i + 1) * 500f, y - (i + 1) * 300f, 8f, "$PTSD_ft02_escort");
                        if (escort != null) escort.addAssignment(FleetAssignment.DEFEND_LOCATION,
                                transport, 20f, "保持编队间隔");
                    }
                    chain.node = "CONVOY_SIGHTED";
                    chain.outcome = "精确坐标附近发现一支驶向内侧轨道的未知编队。两支护卫阻拦接近者。";
                }
            }
        }
        if (day >= chain.expiresDay) finish(chain, "旧观测坐标已过期；如曾击毁真实舰队则其战损仍有效");
    }
}
