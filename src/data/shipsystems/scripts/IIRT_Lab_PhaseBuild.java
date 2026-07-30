package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.I18nUtil;
import data.scripts.util.IIRT_Omega_Color;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

public class IIRT_Lab_PhaseBuild extends BaseShipSystemScript {

	private final IntervalUtil inte = new IntervalUtil(1f, 1f);
	public static float RANGE = 4000f;
	public static float PHASE_CLOAK_UPKEEP_SUBTRACTION = 50f;
	public static float FLUX_THRESHOLD_INCREASE_PERCENT = 25f;
	public static float MULT = 0.75f;
	public static float MULT2 = 0.5f;

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

		stats.getPhaseCloakUpkeepCostBonus().modifyMult(id, MULT);
		stats.getPhaseCloakCooldownBonus().modifyMult(id, MULT2);

		ShipAPI ship = (ShipAPI)stats.getEntity();
		CombatEngineAPI engine = Global.getCombatEngine();

		if (stats.getEntity() != null && stats.getEntity() instanceof ShipAPI) {
			for (ShipAPI tship : engine.getShips()) {
				if (tship.isPhased()) {

					if (MathUtils.getDistance(ship, tship) <= RANGE) {
						if (ship.getOwner() == tship.getOwner()) {
							tship.getMutableStats().getPhaseCloakUpkeepCostBonus().modifyPercent(id, -PHASE_CLOAK_UPKEEP_SUBTRACTION);
							tship.getMutableStats().getDynamic().getMod(Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD).modifyPercent(id, FLUX_THRESHOLD_INCREASE_PERCENT);
							tship.setJitterUnder(ship, IIRT_Omega_Color.IIRT_Omega_Lab_Phase, effectLevel, 2, 5f);

						}
					} else {
						tship.getMutableStats().getDynamic().getMod(Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD).unmodify(id);
					}
				}
			}

			inte.advance(0.015F);
			if (inte.intervalElapsed()) {
				SpriteAPI sp2 = Global.getSettings().getSprite("fx", "IIRT_Lab_Wave");
				float dynamicSize = MathUtils.getRandomNumberInRange(30f, 90f);
				MagicRender.battlespace(sp2, ship.getLocation(), I18nUtil.nv, new Vector2f(dynamicSize, dynamicSize), new Vector2f(1000f, 1000f), 360f, MathUtils.getRandomNumberInRange(-60f, 60f), IIRT_Omega_Color.IIRT_Omega_Lab, true, 0.25f, 0.01f, 2f);
				Global.getSoundPlayer().playSound("IIRT_Lab_Scan", 1f, 1f, ship.getLocation(), ship.getVelocity());
			}
		}

		//stats.getSightRadiusMod().modifyPercent(id, 80f);
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getSightRadiusMod().unmodify(id);

		ShipAPI ship = (ShipAPI)stats.getEntity();
		CombatEngineAPI engine = Global.getCombatEngine();

		if (!(stats.getEntity() instanceof ShipAPI)) {
			return;
		}

		for (ShipAPI tship : engine.getShips()) {
			if (tship.getOwner() != ship.getOwner()) {
				tship.getMutableStats().getDynamic().getMod(Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD).unmodify(id);
			}
		}
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("提升" + (int)RANGE + "范围内我方舰船的相位波动硬幅能阈值" + (int)FLUX_THRESHOLD_INCREASE_PERCENT + "%", false);
		}
		if (index == 1) {
			return new StatusData("降低" + (int)RANGE + "范围内我方舰船的相位维持需求减少" + (int)PHASE_CLOAK_UPKEEP_SUBTRACTION + "%", false);
		}
		if (index == 2) {
			return new StatusData("降低自身" + (int)MULT + "% 相位维持需求", false);
		}
		if (index == 3) {
			return new StatusData("降低自身" + (int)MULT2 + "% 相位线圈冷却", false);
		}
		return null;
	}

}
