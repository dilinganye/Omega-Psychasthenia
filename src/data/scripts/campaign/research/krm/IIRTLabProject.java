package data.scripts.campaign.research.krm;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import data.scripts.campaign.research.krm.factor.IIRTLabResearchSpeedFactor;

public class IIRTLabProject {

	public enum REWARD_TYPE {
		SHIP, WEAPON, FIGHTER, RESEARCH_SPEED
	}

	protected String id;
	protected int progress;
	protected boolean repeatable;
	protected int tier;
	protected float rarity;
	protected String reward;
	protected boolean withBP;
	protected REWARD_TYPE type;
	protected int curr = 0;
	protected int playerRetribution = 0;
	protected boolean isFinished = false;
	protected IIRTLabInstitute governInstitute;
	protected boolean isRewarded = false;
	protected boolean competitive = false;

	public IIRTLabProject(String id, int progress, boolean repeatable, int tier, float rarity, String reward, boolean withBP, REWARD_TYPE type, boolean competitive) {
		this.id = id;
		this.progress = progress;
		this.repeatable = repeatable;
		this.tier = tier;
		this.rarity = rarity;
		this.reward = reward;
		this.withBP = withBP;
		this.type = type;
		this.competitive = competitive;
	}

	public String getId() {
		return id;
	}

	public int getProgress() {
		return progress;
	}

	public float getRarity() {
		return rarity;
	}

	public String getReward() {
		return reward;
	}

	public int getTier() {
		return tier;
	}

	public REWARD_TYPE getType() {
		return type;
	}

	public IIRTLabInstitute getGovernInstitute() {
		return governInstitute;
	}

	public void setGovernInstitute(IIRTLabInstitute governInstitute) {
		this.governInstitute = governInstitute;
	}

	public void setFinished(boolean isFinished) {
		this.isFinished = isFinished;
	}

	public boolean isFinished() {
		return isReachedMax() || isFinished;
	}

	public boolean isCompetitive() {
		return competitive;
	}

	public void setCompetitive(boolean competitive) {
		this.competitive = competitive;
	}

	public boolean isReachedMax() {
		return curr >= progress;
	}

	public boolean isRepeatable() {
		return repeatable;
	}

	public boolean isWithBP() {
		return withBP;
	}

	public boolean isRewarded() {
		return isRewarded;
	}

	public void setRewarded(boolean isRewarded) {
		this.isRewarded = isRewarded;
	}

	public int getCurr() {
		return curr;
	}

	public boolean setCurr(int curr) {
		this.curr = curr;
		return isFinished;
	}

	public int getPlayerRetribution() {
		return playerRetribution;
	}

	public void setPlayerRetribution(int playerRetribution) {
		this.playerRetribution = playerRetribution;
	}

	public void addPlayerRetribution(int amount) {
		this.playerRetribution += amount;
	}

	public void finish(boolean givePlayerReward, boolean giveRewardItem, boolean giveRewardBP) {
		if (givePlayerReward) {
			givePlayerReward(giveRewardBP, giveRewardItem);
		}
		if (isFinished()) {
			giveReward();
		}
		setRewarded(true);
	}

	public void giveReward() {
		FactionAPI KRM = Global.getSector().getFaction("KRM");
		if (KRM == null) return;
		if (withBP) {
			switch (type) {
				case FIGHTER:
					KRM.addKnownFighter(reward, true);
					break;
				case SHIP:
					KRM.addKnownShip(reward, true);
					break;
				case WEAPON:
					KRM.addKnownWeapon(reward, true);
					break;
				default:
					break;
			}
		}
	}

	// 给玩家分发奖励:完成项目+有蓝图->提供蓝图|未完成(其他项目结束)+足够进度->在intel提供->提供样品
	public void givePlayerReward(boolean giveRewardBP, boolean giveRewardItem) {
		if (isFinished() && giveRewardBP) {
			FactionAPI pl = Global.getSector().getPlayerFaction();
			if (withBP) {
				switch (type) {
					case FIGHTER:
						pl.addKnownFighter(reward, true);
						break;
					case SHIP:
						pl.addKnownShip(reward, true);
						break;
					case WEAPON:
						pl.addKnownWeapon(reward, true);
						break;
					default:
						break;
				}
			}
		}
		if (giveRewardItem) {
			CampaignFleetAPI plFleet = Global.getSector().getPlayerFleet();
			if (plFleet == null) return;
			switch (type) {
				case FIGHTER:
					plFleet.getCargo().addFighters(reward, 1);
					break;
				case SHIP:
					FleetMemberAPI rewardShip = Global.getFactory().createFleetMember(FleetMemberType.SHIP, Global.getSettings().createEmptyVariant(reward + "_hull", Global.getSettings().getHullSpec(reward)));
					plFleet.getFleetData().addFleetMember(rewardShip);
					break;
				case WEAPON:
					plFleet.getCargo().addWeapons(reward, 2);
					break;
				case RESEARCH_SPEED:
					IIRTLabInstitute activeInstitute = getGovernInstitute();
					if (activeInstitute != null) {
						if (reward.startsWith("+")) {
							int toadd = 3;
							try {
								toadd = Integer.parseInt(reward.split("\\+")[0]);
							} catch (Exception e) {
								toadd = 3 * getTier();
							}
							// activeInstitute.getResearchSpeed().modifyFlat(id, toadd);
							getGovernInstitute().addFactor(new IIRTLabResearchSpeedFactor(toadd, "提高科研速度Lv." + tier));
						}
						if (reward.startsWith("%")) {
							float topercent = 10;
							try {
								topercent = Float.parseFloat(reward.split("\\+")[0]);
							} catch (Exception e) {
								topercent = 10 * getTier();
							}
							// activeInstitute.getResearchSpeed().modifyPercent(id, topercent);
							getGovernInstitute().addFactor(new IIRTLabResearchSpeedFactor(0, (1f + topercent / 100f), "提高科研速度Lv." + tier));
						}
					}
				default:
					break;
			}
		}
	}

}
