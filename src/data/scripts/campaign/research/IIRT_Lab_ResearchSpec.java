package data.scripts.campaign.research;

public class IIRT_Lab_ResearchSpec {

	public enum REWARD_TYPE {
		WEAPON, SHIP, WEAPON_ONLY, SHIP_ONLY
	}

	//weapon,ship类型会在完成时同时提供1样品和蓝图；完成度高于指定值的项目即使未完成也会提供样品
	//weapon_only,ship_only类型只会提供1样品，未完成项目不会提供样品
	private String id;
	private int tier;
	private int difficulty;
	private REWARD_TYPE type;
	private int stage;

	public IIRT_Lab_ResearchSpec(String id, int tier, int difficulty, int stage, String type) {
		this.id = id;
		this.tier = tier;
		this.difficulty = difficulty;
		this.type = REWARD_TYPE.valueOf(type);
		this.stage = stage;
	}

	public String getId() {
		return id;
	}

	public int getDifficulty() {
		return difficulty;
	}

	public int getTier() {
		return tier;
	}

	public REWARD_TYPE getType() {
		return type;
	}

	public int getStage() {
		return stage;
	}
}
