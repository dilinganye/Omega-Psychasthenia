package data.scripts.campaign.invasion;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.util.IntervalUtil;

/** Transient campaign watcher that adds/removes the crisis monitor when DevMode changes. */
public final class PTSDCrisisDevWatcher implements EveryFrameScript {
    private final IntervalUtil interval = new IntervalUtil(0.15f, 0.25f);

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
        interval.advance(amount);
        if (interval.intervalElapsed()) {
            PTSDCrisisDevIntel.sync();
            PTSDOccupationManager.syncMapVisibility();
        }
    }
}
