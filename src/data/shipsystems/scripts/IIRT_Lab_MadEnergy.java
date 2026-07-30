package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class IIRT_Lab_MadEnergy extends BaseShipSystemScript {

	public static final float DAMAGE_BONUS_PERCENT = 50f;
	public static final float FLUX_BONUS_PERCENT = 0.5f;
	public static final float EXTRA_DAMAGE_TAKEN_PERCENT = 100f;

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

		float damageBonusPercent = DAMAGE_BONUS_PERCENT * effectLevel;
		float fluxBonusPercent = FLUX_BONUS_PERCENT * effectLevel;

		stats.getEnergyWeaponDamageMult().modifyPercent(id, damageBonusPercent);
		stats.getBallisticWeaponDamageMult().modifyPercent(id, damageBonusPercent);

		stats.getEnergyWeaponFluxCostMod().modifyMult(id, fluxBonusPercent);

	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getEnergyWeaponFluxCostMod().unmodify(id);
		stats.getBallisticWeaponDamageMult().unmodify(id);
		stats.getEnergyWeaponFluxCostMod().unmodify(id);
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		float damageBonusPercent = DAMAGE_BONUS_PERCENT * effectLevel;
		float fluxBonusPercent = FLUX_BONUS_PERCENT * 100f * effectLevel;

		float damageTakenPercent = EXTRA_DAMAGE_TAKEN_PERCENT * effectLevel;
		if (index == 0) {
			return new StatusData("+" + (int)damageBonusPercent + "% 能量/实弹武器伤害", false);
		} else if (index == 1) {
			return new StatusData("-" + (int)fluxBonusPercent + "% 能量武器幅能需求", false);
		} else if (index == 2) {
			return null;
		}
		return null;
	}
}
