package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;

/** Shared context actions for the theater map and coalition map. */
public final class PTSDMapCommands {
    private PTSDMapCommands() { }

    public static boolean handle(PTSDCrisisState state, String id) {
        if (state == null || id == null) return false;
        if(id.startsWith("PTSD_MAP_SUPPORT:")){
            String[] request=id.split(":",3);
            if(request.length==3)PTSDGeneralStaffDirector.propose(state,request[2],request[1]);
            return true;
        }
        if (id.startsWith("PTSD_MARK_DEFEND:") || id.startsWith("PTSD_CLEAR_MARKER:") || id.startsWith("PTSD_MAP_COURSE:")) {
            String systemId = id.substring(id.indexOf(':') + 1);
            StarSystemAPI system = state.resolveSystem(systemId);
            if (!PTSDHumanWarReports.known(state, system)) return true;
            if (id.startsWith("PTSD_MARK_DEFEND:")) {
                state.putMarker(systemId, "DEFEND", 1f);
                Global.getSector().getCampaignUI().addMessage("已登记防御请求：" + system.getName() + "。增援仍需实际调拨。", Misc.getHighlightColor());
            } else if (id.startsWith("PTSD_CLEAR_MARKER:")) state.playerMarkers.remove(systemId);
            else Global.getSector().layInCourseFor(system.getHyperspaceAnchor());
            return true;
        }
        if (id.startsWith("PTSD_MAP_REINFORCE:")) {
            PTSDGeneralStaffDirector.propose(state, id.substring("PTSD_MAP_REINFORCE:".length()), "EVIDENCE");
            return true;
        }
        if (id.startsWith("PTSD_MAP_RESERVE:")) {
            String[] parts = id.substring("PTSD_MAP_RESERVE:".length()).split(":", 2);
            if (parts.length == 2 && !PTSDGeneralStaffDirector.orderReserve(state, parts[0], parts[1]))
                Global.getSector().getCampaignUI().addMessage("调令未执行：后备队未授权、仍在行军或目标防区已结束。", Misc.getGrayColor());
            return true;
        }
        return false;
    }
}
