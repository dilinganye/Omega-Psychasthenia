package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import java.util.List;

/** Player combat fallback, independent of which factory installed fleet listeners. */
public final class PTSDPlayerBattleObserver extends BaseCampaignEventListener {
    public PTSDPlayerBattleObserver() { super(false); }
    public static void install() {
        if (Global.getSector() == null) return;
        for (CampaignEventListener listener : Global.getSector().getAllListeners())
            if (listener instanceof PTSDPlayerBattleObserver) return;
        Global.getSector().addTransientListener(new PTSDPlayerBattleObserver());
    }
    @Override public void reportPlayerEngagement(EngagementResultAPI result) {
        if (result == null || result.getBattle() == null) return;
        List<CampaignFleetAPI> enemies = result.getBattle().getNonPlayerSideSnapshot();
        if (enemies != null) for (CampaignFleetAPI fleet : enemies) {
            PTSDJeOtloesManager.recordPlayerBattle(fleet);
            PTSDJeWarExpedition.recordBattle(PTSDCrisisState.get(),fleet);
        }
    }
}
