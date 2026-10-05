package data.scripts.campaign.invasion.news.ft;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import data.scripts.campaign.cargo.PTSDOmegaFleetSupport;
import data.scripts.campaign.invasion.*;

public final class FT10Handler extends AbstractFTHandler {
    protected String id() { return "FT-10"; }
    @Override public String unavailableReason(PTSDCrisisState state) {
        if (state.late.verifiedPlayerOmegaDefeats < 6) return "真实击毁危机舰队不足6支";
        return fragments() >= 5f ? "可触发" : "货舱协议残片不足5块";
    }
    private static float fragments() {
        if (player() == null) return 0f;
        float sum = 0f;
        for (CargoStackAPI stack : player().getCargo().getStacksCopy())
            if (stack.isSpecialStack() && stack.getSpecialDataIfSpecial() != null &&
                    PTSDOmegaFleetSupport.FRAGMENT_ITEM_ID.equals(stack.getSpecialDataIfSpecial().getId()))
                sum += stack.getSize();
        return sum;
    }
    @Override public void start(PTSDCrisisState state, PTSDLateState.FTChain chain) {
        chain.systemId = player() == null || player().getStarSystem() == null ?
                null : player().getStarSystem().getId();
        chain.node = "CREW_ARGUMENT";
        chain.outcome = "科学家说有个\"临时定义\"。工程主管问它是不是又要占用舰船电网；两人等你决定是否听下去。";
        chain.expiresDay = Float.MAX_VALUE;
    }
    @Override public String[] actions(PTSDCrisisState state, PTSDLateState.FTChain chain) {
        if (chain.resolved) return new String[0];
        if ("HARDWARE_INSTALLED".equals(chain.node)) return new String[]{"接通舰船电网"};
        if ("GRID_POWERED".equals(chain.node)) return new String[]{"启动强扫描广播"};
        return new String[]{"拒绝舰内试验", "同意安装并扫描"};
    }
    @Override public boolean choose(PTSDCrisisState state, PTSDLateState.FTChain chain, String action) {
        if (chain.resolved || state.phase == PTSDCrisisState.Phase.WAR) return false;
        if ("拒绝舰内试验".equals(action) && "CREW_ARGUMENT".equals(chain.node)) {
            chain.refused = true;
            finish(chain, "研究计划被否决；舰内设备未上舰，残片未消耗");
            return true;
        }
        if ("同意安装并扫描".equals(action) && "CREW_ARGUMENT".equals(chain.node)) {
            if (fragments() < 5f || player() == null ||
                    state.late.committedIds.contains("ft10-fragments")) return false;
            if (!player().getCargo().removeItems(CargoAPI.CargoItemType.SPECIAL,
                    new SpecialItemData(PTSDOmegaFleetSupport.FRAGMENT_ITEM_ID, null), 5f)) return false;
            state.late.committedIds.add("ft10-fragments");
            chain.node = "HARDWARE_INSTALLED";
            chain.outcome = "五块残片送进临时设备。工程主管把它固定在空舱上，要求所有人远离尚未接通的接口。";
            return true;
        }
        if ("接通舰船电网".equals(action) && "HARDWARE_INSTALLED".equals(chain.node)) {
            chain.node = "GRID_POWERED";
            chain.outcome = "电网负载上升。首席科学家在广播前又强调一次：\"这只是临时定义。\"";
            return true;
        }
        if (!"启动强扫描广播".equals(action) || !"GRID_POWERED".equals(chain.node) ||
                !state.late.committedIds.add("ft10-scan")) return false;
        state.late.warPointBelief += 45f;
        PTSDThreatDirector.recordAction(state, "DECEPTION", "FT-10 强扫描广播",
                chain.systemId, 60f, 1f, true, 90f);
        PTSDCrisisDevIntel.report("FT-10 精确账本", "真实准备点不变；敌方估计+45；玩家威胁样本+60",
                chain.systemId, null);
        finish(chain, "设备占据一段舱室，电网负载骤升。强扫描播报穿过星系；科学家只说接下来要看谁改变部署。");
        return true;
    }
    @Override public void advanceChain(PTSDCrisisState state, PTSDLateState.FTChain chain, float day) {
        if (state.phase == PTSDCrisisState.Phase.WAR)
            finish(chain, "战时窗口关闭；未广播的设备记录已封存。敌方准备估计未因此改变。");
    }
}
