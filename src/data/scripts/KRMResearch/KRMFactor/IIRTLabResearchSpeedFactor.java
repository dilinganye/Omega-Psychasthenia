package data.scripts.KRMResearch.KRMFactor;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel;

import java.awt.Color;

public class IIRTLabResearchSpeedFactor extends BaseEventFactor {

	private int progress;
	private String desc;
	private float mult;
	private float days;
	private Long timestamp = 0L;

	public IIRTLabResearchSpeedFactor(int progress, String desc) {
		super();
		this.progress = progress;
		this.desc = desc;
		this.mult = 1f;
		this.days = -1;
	}

	public IIRTLabResearchSpeedFactor(int progress, float mult, String desc) {
		super();
		this.progress = progress;
		this.desc = desc;
		this.mult = mult;
		this.days = -1;
	}

	//用于有限时需求的场合
	public IIRTLabResearchSpeedFactor(int progress, float mult, String desc, float days) {
		super();
		this.progress = progress;
		this.desc = desc;
		this.mult = mult;
		this.days = days;
		timestamp = Global.getSector().getClock().getTimestamp();
	}

	@Override
	public String getDesc(BaseEventIntel intel) {
		return desc;
	}

	@Override
	public int getProgress(BaseEventIntel intel) {
		return progress;
	}

	@Override
	public float getAllProgressMult(BaseEventIntel intel) {
		return mult;
	}

	@Override
	public boolean isExpired() {
		return timestamp != 0 && days >= 0 && Global.getSector().getClock().getElapsedDaysSince(timestamp) > days;
	}

	@Override
	public Color getDescColor(BaseEventIntel intel) {
		return super.getDescColor(intel);
	}

	public float getDays() {
		return days;
	}

	public float getMult() {
		return mult;
	}

	public void setDays(float days) {
		this.days = days;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public void setMult(float mult) {
		this.mult = mult;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}
}
