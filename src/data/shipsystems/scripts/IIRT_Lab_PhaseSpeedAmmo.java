package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class IIRT_Lab_PhaseSpeedAmmo extends BaseShipSystemScript {

	public static final float SPEED_BONUS_PERCENT = 50f;
	public static final float FLUX_BONUS_PERCENT = 0.5f;
	public static final float EXTRA_DAMAGE_TAKEN_PERCENT = 100f;

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

		float speedBonusPercent = SPEED_BONUS_PERCENT * effectLevel;

		stats.getProjectileSpeedMult().modifyPercent(id, speedBonusPercent);

		stats.getMissileMaxSpeedBonus().modifyPercent(id, speedBonusPercent);
		stats.getMissileAccelerationBonus().modifyPercent(id, speedBonusPercent);
		stats.getMissileMaxTurnRateBonus().modifyPercent(id, speedBonusPercent);
		stats.getMissileTurnAccelerationBonus().modifyPercent(id, speedBonusPercent);
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getProjectileSpeedMult().unmodify(id);

		stats.getMissileMaxSpeedBonus().unmodify(id);
		stats.getMissileAccelerationBonus().unmodify(id);
		stats.getMissileMaxTurnRateBonus().unmodify(id);
		stats.getMissileTurnAccelerationBonus().unmodify(id);
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		float speedBonusPercent = SPEED_BONUS_PERCENT * effectLevel;
		if (index == 0) {
			return new StatusData("+" + (int)speedBonusPercent + "% 抛射体速度", false);
		} else if (index == 1) {
			return null;
		} else if (index == 2) {
			return null;
		}
		return null;
	}
}
