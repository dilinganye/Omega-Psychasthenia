package data.scripts.campaign.invasion.news.ft;

import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator;

public final class FT02ObservatoryBarCreator extends BaseBarEventCreator {
    @Override public PortsideBarEvent createBarEvent() { return new FT02ObservatoryBarEvent(); }
    @Override public boolean isPriority() { return true; }
    @Override public float getBarEventFrequencyWeight() { return 100f; }
    @Override public float getBarEventAcceptedTimeoutDuration() { return 60f; }
}
