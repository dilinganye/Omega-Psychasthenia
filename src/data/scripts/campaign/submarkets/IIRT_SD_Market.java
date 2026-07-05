package data.scripts.campaign.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;

import java.util.List;

public class IIRT_SD_Market extends BaseSubmarketPlugin {

	/**
	 * 初始化方法，在插件被加载时调用。
	 *
	 * @param submarket API实例
	 */
	@Override
	public void init(SubmarketAPI submarket) {
		this.submarket = submarket;
		this.market = submarket.getMarket();
	}

	/**
	 * 获取关税比例。
	 *
	 * @return 返回浮点数类型的关税比例
	 */
	@Override
	public float getTariff() {
		return 1f;
	}

	/**
	 * 在更新玩家进程前触发器，用于检查是否可以更新船只和武器
	 *
	 */
	@Override
	public void updateCargoPrePlayerInteraction() {
		sinceLastCargoUpdate = 0f;  //重置最后一次货物更新时间戳。如果货物已准备好，则不需要再次检查
		//all
		if (okToUpdateShipsAndWeapons()) {  //若可以更新，则继续进行。否则返回false
			sinceSWUpdate = 0f;
			pruneWeapons(0f);

			// 计算需要生成的武器数量
			int weapons = 4 + Math.max(0, market.getSize()) * 5;
			int fighterNum = Math.max(1, market.getSize() - 5);
			int hullmods = Math.max(1, market.getSize() - 5);

			// 根据FactionId“IIRT_714”获取”IIRT_Market“对象
			FactionAPI IIRT_Market = null;
			List<FactionAPI> Factions = Global.getSector().getAllFactions();
			for (FactionAPI F : Factions) {
				if (F.getId().contentEquals("IIRT_714")) {
					IIRT_Market = F;
				}
			}

			// 添加战斗机和武器
			addFighters(fighterNum - 1, fighterNum, 3, "IIRT_714"); //最小数值, 最大数值, 最大堆叠(武器同时出现最大数), 阵营ID
			addWeapons(weapons, weapons + 2, 3, "IIRT_714");
			addHullMods(hullmods, hullmods + 1);

			// 从货舱中移除过期的商品舰船
			getCargo().getMothballedShips().clear();

			// 根据FactionDoctrineAPI(阵营学说API)设置商品舰船的概率、尺寸等参数从而生成商品舰船
			FactionDoctrineAPI doctrineOverrided = submarket.getFaction().getDoctrine().clone();
			doctrineOverrided.setCombatFreighterProbability(0.25f);
			doctrineOverrided.setShipSize(3);

			// 在所有可用的舰船中按优先级选择商品舰船并生成
			addShips("IIRT_714", 100f, // 战斗舰船
					10f, // 货轮
					10f, // 油轮
					10f, // 运兵
					0f, // liner(?)
					0f, // utilityPts(?)
					null, // 质量覆盖(?)
					0f, // 内置插槽
					ShipPickMode.PRIORITY_THEN_ALL,//FactionAPI.ShipPickMode modeOverride, at what priority to pick ship in all availables
					doctrineOverrided);// FactionDoctrineAPI doctrineOverride, at what fraction to pick ship among all availables

		}

		getCargo().sort();
	}

	//判断是否违法
	@Override
	public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action) {

		FactionAPI player = Global.getSector().getPlayerFaction();
		RepLevel IIRT_Level = Global.getSector().getFaction("IIRT").getRelationshipLevel(player);

		//如果货物被卖出，则返回true
		if (action == TransferAction.PLAYER_SELL) return true;

		// 如果货物被购买，且玩家与"IIRT"派系的关系等级低于最低友好水准，则返回true
		if (action == TransferAction.PLAYER_BUY && !IIRT_Level.isAtWorst(RepLevel.COOPERATIVE)) return true;
		return action == TransferAction.PLAYER_BUY && !IIRT_Level.isAtWorst(RepLevel.COOPERATIVE);
	}

	@Override
	public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action) {

		FactionAPI player = Global.getSector().getPlayerFaction();
		RepLevel IIRT_Level = Global.getSector().getFaction("IIRT").getRelationshipLevel(player);

		if (action == TransferAction.PLAYER_SELL) return true;
		if (action == TransferAction.PLAYER_BUY && !IIRT_Level.isAtWorst(RepLevel.COOPERATIVE)) return true;
		return action == TransferAction.PLAYER_BUY && !IIRT_Level.isAtWorst(RepLevel.COOPERATIVE);
	}

	//不能使用的原因
	@Override
	public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {

		FactionAPI player = Global.getSector().getPlayerFaction();
		RepLevel IIRT_Level = Global.getSector().getFaction("IIRT").getRelationshipLevel(player);

		if (action == TransferAction.PLAYER_SELL) return "此市场不支持出售";  //如果玩家试图贩卖(不支持退货)
		if (!IIRT_Level.isAtWorst(RepLevel.COOPERATIVE)) {
			return "在你被足够信任之前,你似乎无法在此市场内购买物品";   //好感需为：欢迎
		}
		return "在你被足够信任前,你无法使用此市场";

	}

	@Override
	public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
		FactionAPI player = Global.getSector().getPlayerFaction();
		RepLevel IIRT_Level = Global.getSector().getFaction("IIRT").getRelationshipLevel(player);

		if (action == TransferAction.PLAYER_SELL) return "此市场不支持出售";
		if (!IIRT_Level.isAtWorst(RepLevel.COOPERATIVE)) return "在[好感：合作]之前,你无法在此交易";
		return "你无法在此交易";

	}

	//当不在ID为”IIRT“的势力控制时隐藏此市场
	@Override
	public boolean isHidden() {
		return !submarket.getFaction().getId().contentEquals("IIRT");
	}

}
