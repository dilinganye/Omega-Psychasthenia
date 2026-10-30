package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** 调查席的显示策略；隐藏条目不删除任务状态、证据或仍在执行的追兵。 */
final class PTSDInvestigationList {
    enum Order {
        NEWEST("时间最近"), OLDEST("时间最远"), NEAREST("距离最近"), FARTHEST("距离最远");
        final String label;
        Order(String label) { this.label = label; }
    }

    private PTSDInvestigationList() { }

    static boolean visible(PTSDCrisisState.CrisisIncident i, float day) {
        if (i == null || !i.recordedByPlayer || i.investigationAbandoned || i.investigationReportsAwarded > 0)
            return false;
        // 奖励结算是立即退列表的依据。已记录的终态在后台下一次更新前也不能重新出现。
        if (i.investigationResult == PTSDTaskRules.InvestigationResult.EXPIRED ||
                i.investigationResult == PTSDTaskRules.InvestigationResult.TARGET_LOST) return false;
        boolean followUp = i.scenarioTrigger != null && !i.scenarioResolved;
        float deadline = followUp && i.scenarioDeadline > 0f ? i.scenarioDeadline :
                (i.investigationExpiresDay > 0f ? i.investigationExpiresDay :
                        i.createdDay + PTSDTaskRules.NEWS_INVESTIGATION_DAYS);
        if (day >= deadline) return false;
        if (!i.investigationResolved || followUp) return true;
        // “抵近确认”并不总是任务完成：仍在进行的现场树及尚未结算的已确认位置继续显示。
        return (i.scene != null && !i.scene.resolved) ||
                (i.scene == null && i.investigationReal);
    }

    /** 在打开/刷新面板时建立距离快照，比较器不扫描星区、不生成导航实体。 */
    static Map<String, Double> distances(PTSDCrisisState state, Collection<String> systems) {
        Map<String, Double> result = new HashMap<String, Double>();
        CampaignFleetAPI player = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        Vector2f origin = player == null ? null : player.getLocationInHyperspace();
        for (String id : systems) {
            if (result.containsKey(id)) continue;
            StarSystemAPI system = id == null ? null : state.resolveSystem(id);
            Vector2f location = system == null ? null : system.getLocation();
            double distance = Double.NaN;
            if (origin != null && location != null) {
                double x = origin.x - location.x, y = origin.y - location.y;
                distance = x * x + y * y;
            }
            result.put(id, distance);
        }
        return result;
    }

    /** 未知位置始终置底；相同距离按新消息优先，再以持久ID消除刷新时的跳动。 */
    static int compare(Order order, float aDay, String aId, double aDistance,
                       float bDay, String bId, double bDistance) {
        int value;
        if (order == Order.NEAREST || order == Order.FARTHEST) {
            boolean aKnown = !Double.isNaN(aDistance) && !Double.isInfinite(aDistance);
            boolean bKnown = !Double.isNaN(bDistance) && !Double.isInfinite(bDistance);
            if (aKnown != bKnown) return aKnown ? -1 : 1;
            value = aKnown ? Double.compare(aDistance, bDistance) : 0;
            if (order == Order.FARTHEST) value = -value;
            if (value != 0) return value;
            value = Float.compare(bDay, aDay);
        } else value = order == Order.OLDEST ? Float.compare(aDay, bDay) : Float.compare(bDay, aDay);
        return value != 0 ? value : String.valueOf(aId).compareTo(String.valueOf(bId));
    }

    static List<PTSDCrisisState.CrisisIncident> incidents(PTSDCrisisState state, final Order order, float day) {
        List<PTSDCrisisState.CrisisIncident> items = new ArrayList<PTSDCrisisState.CrisisIncident>();
        Set<String> systems = new HashSet<String>();
        for (PTSDCrisisState.CrisisIncident i : state.incidents) if (visible(i, day)) {
            items.add(i); systems.add(i.targetSystemId);
        }
        final Map<String, Double> distances = distances(state, systems);
        Collections.sort(items, new Comparator<PTSDCrisisState.CrisisIncident>() {
            public int compare(PTSDCrisisState.CrisisIncident a, PTSDCrisisState.CrisisIncident b) {
                return PTSDInvestigationList.compare(order, a.createdDay, a.id, distances.get(a.targetSystemId),
                        b.createdDay, b.id, distances.get(b.targetSystemId));
            }
        });
        return items;
    }

    static List<PTSDCrisisState.SignalTrace> traces(PTSDCrisisState state, final Order order, float day) {
        List<PTSDCrisisState.SignalTrace> items = new ArrayList<PTSDCrisisState.SignalTrace>();
        Set<String> systems = new HashSet<String>();
        for (PTSDCrisisState.SignalTrace trace : state.signalTraces) if (trace != null && trace.expiresDay > day) {
            items.add(trace); systems.add(trace.systemId);
        }
        final Map<String, Double> distances = distances(state, systems);
        Collections.sort(items, new Comparator<PTSDCrisisState.SignalTrace>() {
            public int compare(PTSDCrisisState.SignalTrace a, PTSDCrisisState.SignalTrace b) {
                return PTSDInvestigationList.compare(order, a.createdDay, a.id, distances.get(a.systemId),
                        b.createdDay, b.id, distances.get(b.systemId));
            }
        });
        return items;
    }
}
