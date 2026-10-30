package data.scripts.campaign.invasion.news.ft;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.cargo.PTSDOmegaFleetSupport;
import data.scripts.campaign.invasion.*;

/** Internal name: 欢喜冤家-事件. This lead is never published to the ordinary news feed. */
public class FT09Handler extends AbstractFTHandler {
    protected String id() { return "FT-09"; }
    @Override public String unavailableReason(PTSDCrisisState state) {
        return state.late.lastFTTransportLossId == null ?
                "没有可核实的真实运输舰损失" : "可触发";
    }
    @Override public void start(PTSDCrisisState state, PTSDLateState.FTChain chain) {
        StarSystemAPI system = state.resolveSystem(state.late.lastFTTransportLossSystemId);
        if (system == null) return;
        chain.systemId = system.getId();
        chain.secondaryId = state.late.lastFTTransportLossId;
        SectorEntityToken site = system.createToken(state.late.lastFTTransportLossX,
                state.late.lastFTTransportLossY);
        site.setName("匿名残片定位区");
        { system.addEntity(site); data.scripts.campaign.invasion.PTSDCampaignRuntime.entityAdded(site); }
        chain.targetId = site.getId();
        chain.node = "ANONYMOUS_LEAD";
        chain.outcome = "一份匿名数据指向失事舰附近的漂浮物。坐标将在四十五日后失效。";
        chain.expiresDay = chain.startedDay + 45f;
    }
    @Override public String[] actions(PTSDCrisisState state, PTSDLateState.FTChain chain) {
        if (chain.resolved) return new String[0];
        if ("ANONYMOUS_LEAD".equals(chain.node)) return new String[]{"回收匿名残片"};
        if ("CREW_SALUTE".equals(chain.node)) return new String[]{"请继续讨论"};
        if ("CREW_DEBATE".equals(chain.node)) return new String[]{"暂记为待核实材料"};
        return new String[0];
    }
    @Override public boolean choose(PTSDCrisisState state, PTSDLateState.FTChain chain, String action) {
        if (chain.resolved) return false;
        if ("回收匿名残片".equals(action) && "ANONYMOUS_LEAD".equals(chain.node)) {
            if (!playerNear(chain, 1000f) || chain.fragmentAwarded) return false;
            Global.getSector().getPlayerFleet().getCargo().addSpecial(
                    new SpecialItemData(PTSDOmegaFleetSupport.FRAGMENT_ITEM_ID, null), 1f);
            chain.fragmentAwarded = true;
            chain.node = "CREW_SALUTE";
            chain.outcome = "工程主管：\"你又把没标号的东西搬进实验舱？\" 首席科学家：\"我只是请你看一眼。\" 两人看见你后同时立正：\"$playerSirOrMadam!\"";
            return true;
        }
        if ("请继续讨论".equals(action) && "CREW_SALUTE".equals(chain.node)) {
            chain.node = "CREW_DEBATE";
            chain.outcome = "科学家强调只是\"讨论\"：外壳密度异常，内部像是被抽空后转为能量。工程主管翻了个白眼：\"也可能是我们的仪器又在撒谎。\"";
            return true;
        }
        if ("暂记为待核实材料".equals(action) && "CREW_DEBATE".equals(chain.node)) {
            finish(chain, "残片已入库一次。舰内没有给出可靠的物资用途判断。");
            return true;
        }
        return false;
    }
    @Override public void advanceChain(PTSDCrisisState state, PTSDLateState.FTChain chain, float day) {
        if (day < chain.expiresDay) return;
        SectorEntityToken site = PTSDFTDirector.target(chain);
        if (site != null && site.getContainingLocation() != null) site.getContainingLocation().removeEntity(site);
        finish(chain, chain.fragmentAwarded ? "残片已取走；现场漂浮物散尽" :
                "匿名坐标失效；现场漂浮物散尽，未取得残片");
    }
}
