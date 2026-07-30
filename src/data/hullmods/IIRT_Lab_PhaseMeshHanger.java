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

public class IIRT_Lab_PhaseMeshHanger extends BaseHullMod {

	public static final String id = "IIRT_Lab_PhaseMeshHanger";

	/**
	 * For Those Phase Mesh
	 */

	public static final float SPEED_INCREASE = 0.20F;
	public static final float PHASE_KEEP_REDUCTION = 0.25F;
	public static final float REPAIR_REDUCTION = 0.10F;
	public static final float DAMAGE_REDUCTION = 0.20F;
	public static final float REFIT_TIME_REDUCTION = 0.50F;

	public static final float FT_SPEED_INCREASE = 0.10F;
	public static final float FT_DAMAGE_REDUCTION = 0.10F;
	public static final float REFIT_TIME_REDUCTION_BAYNUB = 0.00F;
	public static final float REFIT_TIME_REDUCTION_CHANGE = 0.00F;
	public static final float REFIT_TIME_REDUCTION_NUB = 0.00F;
	public static final float RATE_INCREASE_MODIFIER = 15F;

	public static final float COST_SUBTRACT = 2F;

	// 冲突崩掉
	private static final Set<String> BLOCK_THOSE_HULLMODS = new HashSet<>();

	static {
		BLOCK_THOSE_HULLMODS.add("expanded_deck_crew");
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		for (String NO_PLS : BLOCK_THOSE_HULLMODS) {
			if (ship.getVariant().getHullMods().contains(NO_PLS)) {
				//MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), NO_PLS, "IIRT_Lab_PhaseMeshHanger");
				ship.getVariant().removeMod("expanded_deck_crew");
			}
		}
		super.applyEffectsAfterShipCreation(ship, id);
	}

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		//stats .addListener(new IIRT_Omega_Lab_MeshOPCostListener());
		//获取甲板数量
		//REFIT_TIME_REDUCTION_BAYNUB = stats.getVariant().getWings().size();
		int BayNub = stats.getNumFighterBays().getModifiedInt();
		//获取机兵LPC数量
		int mNub = 0;
		for (String Fi : stats.getVariant().getWings()) {
			FighterWingSpecAPI f = Global.getSettings().getFighterWingSpec(Fi);
			if (f == null) {
				break;
			}
			if (f.hasTag("KRM_mesh")) {
				mNub++;
			}
		}
		// 如果有1个甲板那就25，两个50
		if (mNub > 0) {
			float BayNubF = BayNub;
			float mNubF = mNub;
			float Reduction = mNubF / BayNubF;
			stats.getFighterRefitTimeMult().modifyMult(id, 1.0F - (Reduction * 0.75f));
		} else {
			stats.getFighterRefitTimeMult().unmodify(id);
		}
		stats.getDynamic().getStat(Stats.REPLACEMENT_RATE_INCREASE_MULT).modifyPercent(id, RATE_INCREASE_MODIFIER);
	}

	static class IIRT_Omega_Lab_MeshOPCostListener implements WeaponOPCostModifier {  //武器有此tag则降低OP需求2

		public static final int COST_SUBTRACT = 2;

		@Override
		public int getWeaponOPCost(MutableShipStatsAPI stats, WeaponSpecAPI weapon, int currCost) {
			if (weapon.hasTag("KRM_mesh")) {
				return (currCost - COST_SUBTRACT);
			}
			return currCost;
		}
	}

	@Override
	public boolean affectsOPCosts() {
		return true;
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {   //输出全部至文字
		if (index == 0) return (int)(SPEED_INCREASE * 100f) + "%";
		if (index == 1) return (int)(PHASE_KEEP_REDUCTION * 100f) + "%";
		if (index == 2) return (int)(REPAIR_REDUCTION * 100f) + "%";
		if (index == 3) return (int)(DAMAGE_REDUCTION * 100f) + "%";

		if (index == 4) return (int)(FT_SPEED_INCREASE * 100f) + "%";
		if (index == 5) return (int)(FT_DAMAGE_REDUCTION * 100f) + "%";

		//if (index == 6) return (int) (REFIT_TIME_REDUCTION_CHANGE * 75f) + "%";
		if (index == 6) return (int)(RATE_INCREASE_MODIFIER) + "%";
		return null;
	}

	@Override
	public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
		MutableShipStatsAPI stats = fighter.getMutableStats();
		if (!ship.getVariant().hasHullMod("defective_manufactory")) {
			if (fighter.getVariant().hasTag("KRM_mesh")) {
				stats.getMaxSpeed().modifyMult(id, 1.0F + SPEED_INCREASE);
				stats.getPhaseCloakUpkeepCostBonus().modifyMult(id, 1.0F - PHASE_KEEP_REDUCTION);
				stats.getFighterRefitTimeMult().modifyMult(id, 1.0F - REPAIR_REDUCTION);
				stats.getHullDamageTakenMult().modifyMult(id, 1.0F - DAMAGE_REDUCTION);
			}
			if (fighter.getVariant().hasTag("KRM_fighter")) {
				stats.getMaxSpeed().modifyMult(id, 1.0F + FT_SPEED_INCREASE);
				stats.getHullDamageTakenMult().modifyMult(id, 1.0F - FT_DAMAGE_REDUCTION);
			}
		}
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if (ship.getVariant().getHullMods().contains("expanded_deck_crew")) {
			return "不兼容于 扩编飞行甲板人员";
		}

		return null;
	}

	//更多的描述拓展
	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		MutableShipStatsAPI stats = ship.getMutableStats();
		tooltip.addSectionHeading("备注", Alignment.TMID, 5f);
		tooltip.addPara("此插件为 克劳斯姆概念设计局 的航母内置插件。", IIRT_Omega_Color.IIRT_Omega_Lab_Word, 4f);
		tooltip.addPara("根据设计局的机兵组件以及调研报告，舰船内提前预置了宽阔的机兵维护区的同时，仍旧具备大量维护以及校准机兵的设备，这能保证机兵在大多数情况下保持相当优秀的状态；", IIRT_Omega_Color.IIRT_Omega_Lab_Word, 4f);
		//tooltip.addPara("机兵库的设计能令设计局的机兵更易于装载 - 也就是降低装配需求；", IIRT_Omega_Color.IIRT_Omega_Lab_Word, 4f);
		tooltip.addPara("机兵本身也是一种军用工程设施 - 所以随着机兵的装载变多，便可以提升整个舰船的甲板效率;", IIRT_Omega_Color.IIRT_Omega_Lab_Word, 4f);
		tooltip.addPara("机兵维护库的调校系统也具有给其他设计局LPC调校的能力，但是因为设施并不匹配，所以效果并不明显;", IIRT_Omega_Color.IIRT_Omega_Lab_Word, 4f);
		tooltip.addPara("   整备计算公式为 (设计局机兵甲板/总甲板数)*75%", IIRT_Omega_Color.IIRT_Omega_Lab_Interest, 3f);
		int BayNub = stats.getNumFighterBays().getModifiedInt();
		//获取克劳斯姆LPC数量
		int mNub = 0;
		for (String Fi : stats.getVariant().getWings()) {
			FighterWingSpecAPI f = Global.getSettings().getFighterWingSpec(Fi);
			if (f == null) continue;
			if (f.hasTag("KRM_mesh")) {
				mNub++;
			}
		}
		//如果有1个甲板那就25，两个50
		float BayNubF = BayNub;
		float mNubF = mNub;
		float Reduction = mNubF / BayNubF * 75f;
		tooltip.addPara("当前加成为 %s ", 4f, IIRT_Omega_Color.IIRT_Omega_Lab_Math, "( " + mNub + " / " + BayNub + " * 75%" + " ) " + "= " + Reduction);
		tooltip.addPara("如果舰船含有 受损的制造工厂 时，因为制造工艺过于恶劣，所有额外维护都会无法进行！从而会禁用以上所有加成！", IIRT_Omega_Color.IIRT_Omega_Lab_Warn, 4f);
		tooltip.addPara("此舰船不适用 扩编飞行甲板人员 的插件！", IIRT_Omega_Color.IIRT_Omega_Lab_Warn, 4f);
		tooltip.addSectionHeading("小知识", Alignment.MID, 5f);
		tooltip.addPara("机兵维护库在一开始其实打算同时具备相场机库的能力，但是最终还是因为资金不足的原因下马；", IIRT_Omega_Color.IIRT_Omega_Lab_Name, 4f);
		tooltip.addPara("因为设计之初就已经最大化了载员数量以及甲板大小，所以不能进行飞行甲板的人员扩编 - 你可以理解为实际上已经内置了这个效果。", IIRT_Omega_Color.IIRT_Omega_Lab_Name, 4f);
	}
}