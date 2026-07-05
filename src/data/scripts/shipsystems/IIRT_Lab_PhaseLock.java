package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class IIRT_Lab_PhaseLock extends BaseShipSystemScript {

	public static float MULT = 0.1f;
	public static float MULT2 = 0.75f;

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

		stats.getPhaseCloakUpkeepCostBonus().modifyMult(id, MULT);
		stats.getPhaseCloakCooldownBonus().modifyMult(id, MULT2);

		//stats.getPhaseCloakActivationCostBonus().modifyMult(id, 0f);
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getPhaseCloakUpkeepCostBonus().unmodify(id);
		stats.getPhaseCloakCooldownBonus().unmodify(id);
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		float multPhaseCloakUpkeep = (1f - MULT) * 100f * effectLevel;
		float multPhaseCloakCooldown = (1f - MULT2) * 100f * effectLevel;
		if (index == 0) {
			return new StatusData("降低自身" + (int)MULT + "% 相位维持需求", false);
		} else if (index == 1) {
			return new StatusData("降低自身" + (int)MULT2 + "% 相位线圈冷却", false);
		} else if (index == 2) {
			return null;
		}
		return null;
	}
}
