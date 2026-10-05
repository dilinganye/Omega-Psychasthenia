package data.scripts.campaign.invasion.news.ft;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.invasion.*;

/** A physical four-ship pursuit trap. The ambush is armed only after a real chase. */
public final class FT03Handler extends AbstractFTHandler {
    protected String id() { return "FT-03"; }
    @Override public String unavailableReason(PTSDCrisisState state) {
        CampaignFleetAPI player = player();
        if (player == null || player.getStarSystem() == null) return "玩家当前不在可调查星系";
        if (site(player.getStarSystem()) == null) return "当前星系没有可定位的小行星带或星云";
        return state.late.strategicReserve < 16f ? "实际舰队资产不足最低诱追编成" : "可触发";
    }
    private static SectorEntityToken site(StarSystemAPI system) {
        if (system == null) return null;
        for (SectorEntityToken candidate : system.getAllEntities()) {
            if (candidate == null || candidate.getName() == null) continue;
            String name = candidate.getName().toLowerCase();
            if (name.contains("asteroid") || name.contains("nebula") ||
                    name.contains("小行星") || name.contains("星云")) return candidate;
        }
        return null;
    }
    @Override public void start(PTSDCrisisState state, PTSDLateState.FTChain chain) {
        StarSystemAPI system = player().getStarSystem();
        if (system == null) return;
        chain.systemId = system.getId();
        SectorEntityToken site = site(system);
        if (site == null) return;
        chain.targetId = site.getId();
        chain.originX = site.getLocation().x;
        chain.originY = site.getLocation().y;
        chain.node = "SITE_MARKED";
        chain.outcome = "商队黑匣定位到小行星带或星云附近。现场没有可辨认的攻击者。";
        chain.expiresDay = chain.startedDay + 30f;
    }
    @Override public void advanceChain(PTSDCrisisState state, PTSDLateState.FTChain chain, float day) {
        if ("SITE_MARKED".equals(chain.node) && inSystem(chain) && playerNear(chain, 3500f) &&
                state.late.strategicReserve >= 16f) {
            CampaignFleetAPI lure = spawn(state, chain, "未知编队", "Omega_Scrap_Heap_Garbage",
                    chain.originX + 900f, chain.originY, 8f, "$PTSD_ft03");
            if (lure != null) {
                chain.fleetId = lure.getId();
                chain.node = "LURE_RETREAT";
                chain.outcome = "一艘未知舰向外侧撤退。黑匣所载的烧蚀频谱与其尾迹相近。";
                SectorEntityToken exit = state.resolveSystem(chain.systemId).getHyperspaceAnchor();
                if (exit != null) lure.addAssignment(FleetAssignment.GO_TO_LOCATION, exit,
                        20f, "脱离事故区");
            }
        }
        if ("LURE_RETREAT".equals(chain.node)) {
            CampaignFleetAPI lure = PTSDFortressDirector.findFleet(chain.fleetId);
            CampaignFleetAPI player = player();
            if (lure == null || !lure.isAlive()) {
                finish(chain, "诱追舰已被实际击毁；伏击未能展开");
                return;
            }
            if (player != null && player.getStarSystem() == lure.getStarSystem() &&
                    Misc.getDistance(player, lure) < 2800f &&
                    Misc.getDistance(player.getLocation(), new org.lwjgl.util.vector.Vector2f(
                            chain.originX, chain.originY)) > 3500f) {
                int count = state.late.strategicReserve >= 24f ? 3 :
                        state.late.strategicReserve >= 16f ? 2 : 1;
                for (int i = 0; i < count; i++) {
                    double angle = (i * 360d / count + 35d) * Math.PI / 180d;
                    float x = player.getLocation().x + (float) Math.cos(angle) * 4200f;
                    float y = player.getLocation().y + (float) Math.sin(angle) * 4200f;
                    CampaignFleetAPI hunter = spawn(state, chain, "未知编队", "Omega_Scrap_Heap_Garbage",
                            x, y, 8f, "$PTSD_ft03");
                    if (hunter != null) {
                        hunter.addAssignment(FleetAssignment.INTERCEPT,
                                player, 20f, "收拢包围");
                        PTSDOmegaEncounterResponse.attachPursuer(hunter, player);
                    }
                }
                lure.clearAssignments();
                lure.addAssignment(FleetAssignment.INTERCEPT, player, 20f, "返航接敌");
                PTSDOmegaEncounterResponse.attachPursuer(lure, player);
                chain.node = "AMBUSH";
                chain.outcome = "远处出现" + count + "支编队并向内收拢。舰队仍可凭实际航速突围。";
            }
        }
        if (day >= chain.expiresDay) finish(chain, "事故区已失去可追踪舰影；未使用的资产仍留在原防区");
    }
}
