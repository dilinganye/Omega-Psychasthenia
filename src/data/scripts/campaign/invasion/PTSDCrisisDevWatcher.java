package data.scripts.campaign.invasion;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PTSDLunaConfigVisibility;

/** Transient campaign watcher that adds/removes the crisis monitor when DevMode changes. */
public final class PTSDCrisisDevWatcher implements EveryFrameScript {
    private final IntervalUtil interval = new IntervalUtil(0.15f, 0.25f);
    private Boolean lastDevMode;

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        data.scripts.PTSDDifficultyCurveEditor.attach(com.fs.starfarer.api.Global.getSector().getCampaignUI().getCurrentInteractionDialog());
        data.scripts.PTSDDifficultyCurveEditor.sync();
        interval.advance(amount);
        if (interval.intervalElapsed()) {
            PTSDLunaConfigVisibility.sync();
            PTSDCrisisDevIntel.sync();
            boolean dev = com.fs.starfarer.api.Global.getSettings().isDevMode();
            if (lastDevMode == null || lastDevMode != dev) {
                PTSDOccupationManager.syncMapVisibility();
                lastDevMode = dev;
            }
        }
    }
}
