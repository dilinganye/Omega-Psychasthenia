package data.scripts.campaign.intel.bar.events;

import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator;

public class IIRT_FirstTimeWithSilverCreator extends BaseBarEventCreator {

	@Override
	public PortsideBarEvent createBarEvent() {
		return new IIRT_FirstTimeWithSilver();
	}

	@Override
	public boolean isPriority() {
		return true;
	}

	@Override
	public float getBarEventFrequencyWeight() {
		return 100f;
	}    //任务产生概率

	@Override
	public float getBarEventAcceptedTimeoutDuration() {
		return 10000000000f;    //足够长的时间来避免再次生成
	}

	//@Override
	//public float getBarEventFrequencyWeight() {
	//	return super.getBarEventFrequencyWeight();
	//}
}
