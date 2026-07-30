//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.WeaponOPCostModifier;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.scripts.util.IIRT_Omega_Color;

import java.util.HashSet;
import java.util.Set;

public class IIRT_Lab_PhaseHangar extends BaseHullMod {

	public static final String id = "IIRT_Lab_PhaseHangar";

	/**
	 * For Those Phase Fighters
	 */

	public static final float DAMAGE_INCREASE = 20F;
	public static final float DAMAGE_REDUCTION = 20F;
	public static final float RATE_INCREASE_MODIFIER = 25F;

	public static final float REFIT_TIME_REDUCTION_BAYNUB = 0.00F;
	public static final float REFIT_TIME_REDUCTION_CHANGE = 0.00F;
	public static final float REFIT_TIME_REDUCTION_NUB = 0.00F;

	// 冲突崩掉
	private static final Set<String> BLOCK_THOSE_HULLMODS = new HashSet<>();

	static {
		BLOCK_THOSE_HULLMODS.add("expanded_deck_crew");
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		for (String NO_PLS : BLOCK_THOSE_HULLMODS) {
			if (ship.getVariant().getHullMods().contains(NO_PLS)) {
				//MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), NO_PLS, "IIRT_Lab_PhaseHangar");
				ship.getVariant().removeMod("expanded_deck_crew");
			}
		}
		super.applyEffectsAfterShipCreation(ship, id);
	}

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		// stats .addListener(new IIRT_Omega_Lab_FighterOPCostListener());
		// 获取甲板数量
		int BayNub = stats.getNumFighterBays().getModifiedInt();
		// 获取克劳斯姆LPC数量
		int Nub = 0;
		for (String Fi : stats.getVariant().getWings()) {
			FighterWingSpecAPI f = Global.getSettings().getFighterWingSpec(Fi);
			if (f == null) {
				break;
			}
			if (f.hasTag("KRM_fighter")) {
				Nub++;
			}
		}
		// 如果有1个甲板那就25，两个50
		if (Nub > 0) {
			float BayNubF = BayNub;
			float NubF = Nub;
			float Reduction = NubF / BayNubF;
			stats.getFighterRefitTimeMult().modifyMult(id, 1.0F - (Reduction * 0.50f));
		} else {
			stats.getFighterRefitTimeMult().unmodify(id);
		}

		stats.getDynamic().getStat(Stats.REPLACEMENT_RATE_INCREASE_MULT).modifyPercent(id, RATE_INCREASE_MODIFIER);

	}

	@Override
	public boolean affectsOPCosts() {
		return true;
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) { // 输出全部至文字
		if (index == 0) return "" + (int)(DAMAGE_INCREASE) + "%";
		if (index == 1) return "" + (int)(DAMAGE_REDUCTION) + "%";
		if (index == 2) return "" + (int)(RATE_INCREASE_MODIFIER) + "%";
		return null;
	}

	@Override
	public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
		MutableShipStatsAPI stats = fighter.getMutableStats();
		if (!ship.getVariant().hasHullMod("defective_manufactory")) {
			if (fighter.getVariant().hasTag("KRM_fighter")) {
				stats.getBeamWeaponDamageMult().modifyPercent(id, DAMAGE_INCREASE);
				stats.getEnergyWeaponDamageMult().modifyPercent(id, DAMAGE_INCREASE);
				stats.getBallisticWeaponDamageMult().modifyPercent(id, DAMAGE_INCREASE);
				stats.getMissileWeaponDamageMult().modifyPercent(id, DAMAGE_INCREASE);

				stats.getShieldDamageTakenMult().modifyPercent(id, -DAMAGE_REDUCTION);
			}
		}
	}

	// 更多的描述拓展
	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		MutableShipStatsAPI stats = ship.getMutableStats();
		tooltip.addSectionHeading("备注", Alignment.TMID, 5f);
		tooltip.addPara("此插件为 克劳斯姆概念设计局 的航母内置插件。", IIRT_Omega_Color.IIRT_Omega_Lab_Word, 4f);
		tooltip.addPara("根据设计局的飞机模块以及特性，舰船内提前预置了大量维护以及校准设备，而这能保证舰载机在大部分情况下保持相当优秀的状态；", IIRT_Omega_Color.IIRT_Omega_Lab_Word, 4f);
		tooltip.addPara("非机兵的设计局LPC占比越多，那么舰船机库便越匹配 - 运转效率就越高；", IIRT_Omega_Color.IIRT_Omega_Lab_Word, 4f);
		tooltip.addPara("   整备计算公式为 (设计局非机兵甲板/总甲板数)*50%", IIRT_Omega_Color.IIRT_Omega_Lab_Interest, 3f);
		int BayNub = stats.getNumFighterBays().getModifiedInt();
		//获取克劳斯姆LPC数量
		int Nub = 0;
		for (String Fi : stats.getVariant().getWings()) {
			FighterWingSpecAPI f = Global.getSettings().getFighterWingSpec(Fi);
			if (f == null) continue;
			if (f.hasTag("KRM_fighter")) {
				Nub++;
			}
		}
		//如果有1个甲板那就25，两个50
		float BayNubF = BayNub;
		float NubF = Nub;
		float Reduction = NubF / BayNubF * 50f;
		tooltip.addPara("当前加成为 %s ", 4f, IIRT_Omega_Color.IIRT_Omega_Lab_Math, "( " + Nub + " / " + BayNub + " * 50%" + " ) " + "= " + Reduction);
		tooltip.addPara("如果舰船含有 受损的制造工厂 时，因为制造工艺过于恶劣，所有额外维护都会无法进行！从而会禁用以上所有加成！", IIRT_Omega_Color.IIRT_Omega_Lab_Warn, 4f);
		tooltip.addSectionHeading("小知识", Alignment.MID, 5f);
		tooltip.addPara("相场机库之所以对机兵没有加成，是因为其设施本质上就完全不匹配；除此以外，正常的机舱大小对于机兵来说也有些过于拥挤了。", IIRT_Omega_Color.IIRT_Omega_Lab_Name, 4f);
	}

	public static class IIRT_Omega_Lab_FighterOPCostListener implements WeaponOPCostModifier { // 武器有此tag则降低OP需求1

		public static final int COST_SUBTRACT = 1;

		@Override
		public int getWeaponOPCost(MutableShipStatsAPI stats, WeaponSpecAPI weapon, int currCost) {
			if (weapon.hasTag("KRM_fighter")) {
				return (currCost - COST_SUBTRACT);
			}
			return currCost;
		}
	}
}