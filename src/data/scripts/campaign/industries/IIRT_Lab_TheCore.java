package data.scripts.campaign.industries;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase.PatrolFleetData;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory.PatrolType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.fleets.PatrolAssignmentAIV4;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.OptionalFleetData;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteFleetSpawner;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteSegment;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.util.Random;

public class IIRT_Lab_TheCore extends BaseIndustry implements RouteFleetSpawner, FleetEventListener {

	//当不在ID为”KRM“的势力控制时隐藏此建筑
	@Override
	public boolean isHidden() {
		return !market.getFactionId().contentEquals("KRM");
	}

	@Override
	public boolean isFunctional() {
		return super.isFunctional() && market.getFactionId().contentEquals("IIRT");
	}

	@Override
	public void apply() {
		super.apply(true);

		int size = market.getSize();

		supply(Commodities.FUEL, 2); //提供燃料
		supply(Commodities.SUPPLIES, size - 2); //提供补给
		supply(Commodities.DOMESTIC_GOODS, size - 2); //提供好东西
		supply(Commodities.SHIPS, size - 1); //提供飞砖
		supply(Commodities.DRUGS, size - 4); //提供飞叶
		demand(Commodities.ORE, 1);  //铁矿石
		demand(Commodities.METALS, 1);  //铁块
		demand(Commodities.RARE_METALS, 1); //内部物件
		demand(Commodities.ORGANICS, size - 4);  //有机物
		demand(Commodities.VOLATILES, size - 4);    //气体

		modifyStabilityWithBaseMod(); //修改稳定性

		MemoryAPI memory = market.getMemoryWithoutUpdate(); //获取市场内存
		Misc.setFlagWithReason(memory, MemFlags.MARKET_PATROL, getModId(), true, -1);   //设置标志
		Misc.setFlagWithReason(memory, MemFlags.MARKET_MILITARY, getModId(), true, -1); //设置标志

	}

	@Override
	public void unapply() {
		super.unapply();
		unmodifyStabilityWithBaseMod(); //修改稳定性
	}

	/**
	 * 确定在具有post-demand部分的情况下是否需要显示区域。如果没有需求或工具提示模式为普通，则返回false。如果有需求或工具提示模式为工业，则返回true
	 * (I DON'T　GET IT)
	 */
	@Override
	protected boolean hasPostDemandSection(boolean hasDemand, IndustryTooltipMode mode) {
		return mode != IndustryTooltipMode.NORMAL || isFunctional();
	}

	/**
	 * 在具有post-demand部分的情况下添加稳定性后盾部分。如果工具提示模式不是工业或行业是功能性的，则返回false。否则返回true
	 * (I DON'T　GET IT)
	 */
	@Override
	protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {
		if (mode != IndustryTooltipMode.NORMAL || isFunctional()) {
			addStabilityPostDemandSection(tooltip, hasDemand, mode);
		}
	}

	/**
	 * 下面都看不懂了 .v。
	 */

	@Override
	protected int getBaseStabilityMod() {
		return 2;
	}

	@Override
	public String getNameForModifier() {
		if (getSpec().getName().contains("HQ")) {
			return getSpec().getName();
		}
		return Misc.ucFirst(getSpec().getName());
	}

	@Override
	protected Pair<String, Integer> getStabilityAffectingDeficit() {
		return getMaxDeficit(Commodities.SUPPLIES, Commodities.FUEL, Commodities.SHIPS, Commodities.HAND_WEAPONS);
	}

	@Override
	public boolean isDemandLegal(CommodityOnMarketAPI com) {
		return true;
	}

	@Override
	public boolean isSupplyLegal(CommodityOnMarketAPI com) {
		return true;
	}

	protected final IntervalUtil tracker = new IntervalUtil(Global.getSettings().getFloat("averagePatrolSpawnInterval") * 0.7f, Global.getSettings().getFloat("averagePatrolSpawnInterval") * 1.3f);

	protected float returningPatrolValue = 0f;

	@Override
	protected void buildingFinished() {
		super.buildingFinished();

		tracker.forceIntervalElapsed();
	}

	@Override
	protected void upgradeFinished(Industry previous) {
		super.upgradeFinished(previous);

		tracker.forceIntervalElapsed();
	}

	@Override
	public void advance(float amount) {
		super.advance(amount);

		if (Global.getSector().getEconomy().isSimMode()) return;

		if (!isFunctional()) return;

		float days = Global.getSector().getClock().convertToDays(amount);

		float spawnRate = 1f;
		float rateMult = market.getStats().getDynamic().getStat(Stats.COMBAT_FLEET_SPAWN_RATE_MULT).getModifiedValue();
		spawnRate *= rateMult;

		float extraTime = 0f;
		if (returningPatrolValue > 0) {
			// apply "returned patrols" to spawn rate, at a maximum rate of 1 interval per day
			float interval = tracker.getIntervalDuration();
			extraTime = interval * days;
			returningPatrolValue -= days;
			if (returningPatrolValue < 0) returningPatrolValue = 0;
		}
		tracker.advance(days * spawnRate + extraTime);

		//tracker.advance(days * spawnRate * 100f);

		if (tracker.intervalElapsed()) {
			String sid = getRouteSourceId();

			int light = getCount(PatrolType.FAST);
			int medium = getCount(PatrolType.COMBAT);
			int heavy = getCount(PatrolType.HEAVY);

			int maxLight = 6;
			int maxMedium = 3;
			int maxHeavy = 2;

			WeightedRandomPicker<PatrolType> picker = new WeightedRandomPicker<>();
			picker.add(PatrolType.HEAVY, maxHeavy - heavy);
			picker.add(PatrolType.COMBAT, maxMedium - medium);
			picker.add(PatrolType.FAST, maxLight - light);

			if (picker.isEmpty()) return;

			PatrolType type = picker.pick();
			PatrolFleetData custom = new PatrolFleetData(type);

			OptionalFleetData extra = new OptionalFleetData(market);
			extra.fleetType = type.getFleetType();

			RouteData route = RouteManager.getInstance().addRoute(sid, market, Misc.genRandomSeed(), extra, this, custom);
			float patrolDays = 35f + (float)Math.random() * 10f;

			route.addSegment(new RouteSegment(patrolDays, market.getPrimaryEntity()));
		}
	}

	@Override
	public void reportAboutToBeDespawnedByRouteManager(RouteData route) {
	}

	@Override
	public boolean shouldRepeat(RouteData route) {
		return false;
	}

	public int getCount(PatrolType... types) {
		int count = 0;
		for (RouteData data : RouteManager.getInstance().getRoutesForSource(getRouteSourceId())) {
			if (data.getCustom() instanceof PatrolFleetData custom) {
				for (PatrolType type : types) {
					if (type == custom.type) {
						count++;
						break;
					}
				}
			}
		}
		return count;
	}

	@Override
	public boolean shouldCancelRouteAfterDelayCheck(RouteData route) {
		return false;
	}

	@Override
	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {

	}

	@Override
	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
		if (!isFunctional()) return;

		if (reason == FleetDespawnReason.REACHED_DESTINATION) {
			RouteData route = RouteManager.getInstance().getRoute(getRouteSourceId(), fleet);
			if (route.getCustom() instanceof PatrolFleetData custom) {
				if (custom.spawnFP > 0) {
					float fraction = (float)fleet.getFleetPoints() / custom.spawnFP;
					returningPatrolValue += fraction;
				}
			}
		}
	}

	@Override
	public CampaignFleetAPI spawnFleet(RouteData route) {

		PatrolFleetData custom = (PatrolFleetData)route.getCustom();
		PatrolType type = custom.type;

		Random random = route.getRandom();

		float combat = 0f;
		float freighter = 0f;
		String fleetType = type.getFleetType();
		switch (type) {
			case FAST:
				combat = Math.round(3f + random.nextFloat() * 2f) * 5f;
				break;
			case COMBAT:
				combat = Math.round(6f + random.nextFloat() * 3f) * 5f;
				break;
			case HEAVY:
				combat = Math.round(10f + random.nextFloat() * 5f) * 5f;
				freighter = Math.round(random.nextFloat()) * 10f;
				break;
		}

		FleetParamsV3 params = new FleetParamsV3(market, null, "KRM", route.getQualityOverride(), fleetType, combat, freighter, 0f, 0f, 0f, 0f, 0f);
		params.timestamp = route.getTimestamp();
		params.random = random;
		//params.modeOverride = Misc.getShipPickMode(market);
		params.modeOverride = ShipPickMode.PRIORITY_THEN_ALL;
		CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);

		if (fleet == null || fleet.isEmpty()) return null;

		fleet.setFaction(market.getFactionId(), true);
		fleet.setNoFactionInName(true);

		fleet.addEventListener(this);

		//		PatrolAssignmentAIV2 ai = new PatrolAssignmentAIV2(fleet, custom);
		//		fleet.addScript(ai);

		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true, 0.3f);

		if (type == PatrolType.FAST || type == PatrolType.COMBAT) {
			fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_CUSTOMS_INSPECTOR, true);
		}

		String postId = Ranks.POST_PATROL_COMMANDER;
		String rankId = switch (type) {
			case FAST -> Ranks.SPACE_LIEUTENANT;
			case COMBAT -> Ranks.SPACE_COMMANDER;
			case HEAVY -> Ranks.SPACE_CAPTAIN;
		};

		fleet.getCommander().setPostId(postId);
		fleet.getCommander().setRankId(rankId);

		market.getContainingLocation().addEntity(fleet);
		fleet.setFacing((float)Math.random() * 360f);
		// this will get overridden by the patrol assignment AI, depending on route-time elapsed etc
		fleet.setLocation(market.getPrimaryEntity().getLocation().x, market.getPrimaryEntity().getLocation().y);

		fleet.addScript(new PatrolAssignmentAIV4(fleet, route));

		//market.getContainingLocation().addEntity(fleet);
		//fleet.setLocation(market.getPrimaryEntity().getLocation().x, market.getPrimaryEntity().getLocation().y);

		if (custom.spawnFP <= 0) {
			custom.spawnFP = fleet.getFleetPoints();
		}

		return fleet;
	}

	public String getRouteSourceId() {
		return getMarket().getId() + "_" + "KRM";
	}

	@Override
	public boolean isAvailableToBuild() {
		return false;
	}

	@Override
	public boolean showWhenUnavailable() {
		return false;
	}

	@Override
	public boolean canImprove() {
		return false;
	}

	@Override
	public RaidDangerLevel adjustCommodityDangerLevel(String commodityId, RaidDangerLevel level) {
		return level.next();
	}

	@Override
	public RaidDangerLevel adjustItemDangerLevel(String itemId, String data, RaidDangerLevel level) {
		return level.next();
	}
}
