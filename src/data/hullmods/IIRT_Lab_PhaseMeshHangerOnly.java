//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.WeaponOPCostModifier;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.utils.iirt_omega.IIRT_Omega_Color;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IIRT_Lab_PhaseMeshHangerOnly extends BaseHullMod {

	public static final String id = "IIRT_Lab_PhaseMeshHangerOnly";

	/**
	 * For Those Phase Mesh
	 */

	public static final float SPEED_INCREASE = 0.20F;
	public static final float REPAIR_REDUCTION = 0.10F;
	public static final float DAMAGE_REDUCTION = 0.20F;
	public static final float REFIT_TIME_REDUCTION = 0.50F;
	public static final float RATE_DECREASE_MODIFIER = 10f;

	public static final float ALL_MESH_COST_PERCENT = 20F;

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
				//MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), NO_PLS, "IIRT_Lab_PhaseMeshHangerOnly");
				ship.getVariant().removeMod("expanded_deck_crew");
			}
		}
		super.applyEffectsAfterShipCreation(ship, id);
	}

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		//stats .addListener(new IIRT_Omega_Lab_MeshOPCostListener());

		if (!stats.getVariant().hasHullMod("defective_manufactory")) {
			stats.getDynamic().getMod(Stats.BOMBER_COST_MOD).modifyPercent(id, -ALL_MESH_COST_PERCENT);
			stats.getDynamic().getMod(Stats.FIGHTER_COST_MOD).modifyPercent(id, -ALL_MESH_COST_PERCENT);
			stats.getDynamic().getMod(Stats.INTERCEPTOR_COST_MOD).modifyPercent(id, -ALL_MESH_COST_PERCENT);
			stats.getDynamic().getMod(Stats.SUPPORT_COST_MOD).modifyPercent(id, -ALL_MESH_COST_PERCENT);
		}
		stats.getFighterRefitTimeMult().modifyMult(id, 1.0F - REFIT_TIME_REDUCTION);
		stats.getDynamic().getStat(Stats.REPLACEMENT_RATE_DECREASE_MULT).modifyMult(id, 1f - RATE_DECREASE_MODIFIER / 100f);
		//锁死甲板可行性
		List<String> wingslist = stats.getVariant().getNonBuiltInWings();

		ArrayList<String> ids = new ArrayList<>();
		ids.add("IIRT_Lab_Meniscus_Rifle_AC");
		ids.add("IIRT_Lab_FirstQuarter_Assault_AC");
		ids.add("IIRT_Lab_Crescent_Attack_AC");
		for (String idst : ids) {
			if (stats.getVariant().getWings().contains("IIRT_Lab_Meniscus_Rifle_AC") || stats.getVariant().getWings().contains("IIRT_Lab_FirstQuarter_Assault_AC") || stats.getVariant().getWings().contains("IIRT_Lab_Crescent_Attack_AC")) {
				break;
			}
			stats.getVariant().getWings().clear();
		}
		wingslist.retainAll(ids);
	}

	static class IIRT_Omega_Lab_MeshOPCostListener implements WeaponOPCostModifier {  //武器有此tag则降低OP需求2

		int costSubtract = 2;

		@Override
		public int getWeaponOPCost(MutableShipStatsAPI stats, WeaponSpecAPI weapon, int currCost) {
			if (weapon.hasTag("KRM_mesh")) {
				return (currCost - costSubtract);
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
		if (index == 1) return (int)(REPAIR_REDUCTION * 100f) + "%";
		if (index == 2) return (int)(DAMAGE_REDUCTION * 100f) + "%";

		if (index == 3) return (int)(REFIT_TIME_REDUCTION * 100f) + "%";
		if (index == 4) return (int)(ALL_MESH_COST_PERCENT) + "%";
		if (index == 5) return (int)(RATE_DECREASE_MODIFIER) + "%";
		return null;
	}

	@Override
	public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
		MutableShipStatsAPI stats = fighter.getMutableStats();
		if (!ship.getVariant().hasHullMod("defective_manufactory")) {
			if (fighter.getVariant().hasTag("KRM_mesh")) {
				stats.getMaxSpeed().modifyMult(id, 1.0F + SPEED_INCREASE);
				stats.getFighterRefitTimeMult().modifyMult(id, 1.0F - REPAIR_REDUCTION);
				stats.getHullDamageTakenMult().modifyMult(id, 1.0F - DAMAGE_REDUCTION);
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
		tooltip.addSectionHeading("备注", Alignment.TMID, 5f);
		tooltip.addPara("此插件为 克劳斯姆概念设计局 的兵船内置插件。", IIRT_Omega_Color.IIRT_Omega_Lab_Word, 4f);
		tooltip.addPara("为了进一步部署相位机兵联队的控制范围，这种特化的机库甲板便产生了；", IIRT_Omega_Color.IIRT_Omega_Lab_Word, 4f);
		tooltip.addPara("其内置的制造工厂针对机兵进行了特殊化适配，但这也导致除了机兵以外的所有LPC都不能被正常识别；", IIRT_Omega_Color.IIRT_Omega_Lab_Word, 4f);
		tooltip.addPara("经过特化的机兵中枢能进一步缩短机兵的整备维修所需时间，同时内涵的大量调校设施能让机兵具备最优的发挥效果;", IIRT_Omega_Color.IIRT_Omega_Lab_Word, 4f);
		tooltip.addPara("如果舰船含有 受损的制造工厂 时，因为制造工艺过于恶劣，所有额外维护都会无法进行！从而会禁用以上除加快整备速度外的所有加成！", IIRT_Omega_Color.IIRT_Omega_Lab_Warn, 4f);
		tooltip.addPara("此舰船不适用 扩编飞行甲板人员 的插件！", IIRT_Omega_Color.IIRT_Omega_Lab_Warn, 4f);
		tooltip.addPara("此舰船只能使用克劳斯姆机兵类的LPC！", IIRT_Omega_Color.IIRT_Omega_Lab_Warn_Big, 5f);
		tooltip.addSectionHeading("小知识", Alignment.MID, 5f);
		tooltip.addPara("机甲中枢的设计目的是为了进一步扩大相位机兵在战场所能控制的影响区域，但由于资金的种种问题，最终也仅仅成为了一种试行配置；", IIRT_Omega_Color.IIRT_Omega_Lab_Name, 4f);
		tooltip.addPara("因为设计之初就已经最大化了载员数量以及甲板大小，所以不能进行飞行甲板的人员扩编 - 你可以理解为实际上已经内置了这个效果。", IIRT_Omega_Color.IIRT_Omega_Lab_Name, 4f);
	}
}