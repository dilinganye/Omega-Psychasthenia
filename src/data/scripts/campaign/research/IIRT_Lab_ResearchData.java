package data.scripts.campaign.research;

import com.fs.starfarer.api.Global;

public class IIRT_Lab_ResearchData {

	private int curr_stage = 0;
	private int retry = 0;
	private IIRT_Lab_ResearchSpec spec;
	private IIRT_Lab_Institute institute;
	private float curr = 0.0f;
	private int difficulty_bonus = 0;
	private int lastRollPt = 0;
	public static final float BASE = 8.0f;
	public static final float FACTOR = 0.5f;

	public IIRT_Lab_ResearchData(IIRT_Lab_ResearchSpec spec, int stage_bonus, int difficulty_bonus, IIRT_Lab_Institute institute) {
		this.spec = spec;
		this.institute = institute;
		this.curr_stage = stage_bonus;
		this.difficulty_bonus = difficulty_bonus;
	}

	public void advance(float amount) {
		curr += calPowerGrow(amount);
		if (curr >= getStageLength()) {
			roll();
			curr = 0;
		}
	}

	public float calPowerGrow(float amount) {
		float day = Global.getSector().getClock().convertToDays(amount);
		return day * institute.getPower();
	}

	public void roll() {
		int pt = (int)(20 * Math.random()) + 1;
		if (pt == 20) {
			curr_stage += 2;//大成功,额外+1阶段
		} else if (pt > 1) {
			if (pt >= retry + difficulty_bonus + institute.calCheckPower() - spec.getDifficulty()) {
				curr_stage++;//普通成功
			} else {
				retry++;//普通失败,降低下次检定1需求
			}
		}
		//1=大失败 什么都不会获得
		lastRollPt = pt;
	}

	public int getLastRollPt() {
		return lastRollPt;
	}

	public int getCurrStage() {
		return curr_stage;
	}

	public boolean isFinished() {
		return curr_stage >= spec.getStage();
	}

	public IIRT_Lab_ResearchSpec getSpec() {
		return spec;
	}

	public float getStageLength() {
		return BASE + spec.getTier() * FACTOR;
	}

	public IIRT_Lab_Institute getInstitute() {
		return institute;
	}

	public float getCurr() {
		return curr;
	}

	public int getCurr_stage() {
		return curr_stage;
	}

	public int getDifficulty_bonus() {
		return difficulty_bonus;
	}

	public int getRetry() {
		return retry;
	}
}
