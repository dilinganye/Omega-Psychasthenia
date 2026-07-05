package data.scripts.campaign.research;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.util.TimeoutTracker;

public class IIRT_Lab_Institute {

	protected float power = 0.5f;
	protected String id;
	protected String name;
	protected String desc;
	protected String spriteId;
	protected PersonAPI leader;
	protected TimeoutTracker<IIRT_Lab_Institute_Buff> temporayBuff = new TimeoutTracker<>();

	public static class IIRT_Lab_Institute_Buff {

		public float buff;
		public String srt;

		public IIRT_Lab_Institute_Buff(float buff, String srt) {
			this.buff = buff;
			this.srt = srt;
		}
	}

	public IIRT_Lab_Institute(float power, String id, String name, String desc, String spriteId, PersonAPI leader) {
		this.power = power;
		this.id = id;
		this.name = name;
		this.desc = desc;
		this.spriteId = spriteId;
		this.leader = leader;
	}

	public void addTempBuff(IIRT_Lab_Institute_Buff buff, float time) {
		temporayBuff.add(buff, time);
	}

	public float calTempPower() {
		float tempPower = 0;
		for (IIRT_Lab_Institute_Buff b : temporayBuff.getItems()) {
			tempPower += b.buff;
		}
		return tempPower;
	}

	public void addPermaPower(float amount) {
		power += amount;
	}

	public String getDesc() {
		return desc;
	}

	public String getId() {
		return id;
	}

	public PersonAPI getLeader() {
		return leader;
	}

	public String getName() {
		return name;
	}

	public String getSpriteId() {
		return spriteId;
	}

	public float getPower() {
		return power + calTempPower();
	}

	public float calCheckPower() {
		return (int)getPower();
	}

}
