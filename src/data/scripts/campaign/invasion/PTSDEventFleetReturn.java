package data.scripts.campaign.invasion;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;

/** Only call at permanent event retirement; never at virtualisation or battle loss. */
public final class PTSDEventFleetReturn {
    private PTSDEventFleetReturn() { }
    public static float assetsFor(float survivingFp) {
        return Float.isNaN(survivingFp) || Float.isInfinite(survivingFp) ? 0f : Math.max(0f, survivingFp) * .5f;
    }
    public static float credit(CampaignFleetAPI fleet) {
        if (fleet==null || fleet.getMemoryWithoutUpdate()!=null&&fleet.getMemoryWithoutUpdate().getBoolean("$PTSD_dev_discarded"))return 0f;
        if (!PTSDJeOtloesManager.isOmegaFleet(fleet) || fleet.getBattle() != null ||
                !fleet.isAlive() || fleet.getFleetData().getMembersListCopy().isEmpty()) return 0f;
        return PTSDLateWarDirector.creditStrategicAssets(PTSDCrisisState.get(),
                "event-fleet-return:" + fleet.getId(), assetsFor(fleet.getFleetPoints()),
                "事件舰队永久撤离", fleet.getStarSystem() == null ? null : fleet.getStarSystem().getId());
    }
}
