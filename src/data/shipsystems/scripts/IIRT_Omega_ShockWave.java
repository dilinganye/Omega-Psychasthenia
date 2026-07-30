package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.I18nUtil;
import data.scripts.util.IIRT_Omega_Color;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.List;

public class IIRT_Omega_ShockWave extends BaseShipSystemScript {

	private int fireamount = 2;
	private final IntervalUtil timer = new IntervalUtil(0.5f, 1.5f);
	private final IntervalUtil inte = new IntervalUtil(1f, 1f);
	public static Object KEY_SHIP = new Object();
	public static float RANGE = 4000f;
	public static float PHASE_CLOAK_UPKEEP_SUBTRACTION = 50f;
	public static float FLUX_THRESHOLD_INCREASE_PERCENT = 25f;
	public static float MULT = 0.75f;
	public static float MULT2 = 0.5f;
	private boolean runOnce = false, hasfire = false;
	List<CombatEntityAPI> nearByEnemies = null;

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		//com.fs.starfarer.api.impl.combat.dweller.RiftLightningEffect
		//com.fs.starfarer.api.impl.combat.RiftCascadeEffect

		Color negtive_color = new Color(200,255,200, 34);
		ShipAPI ship = (ShipAPI)stats.getEntity();
		CombatEngineAPI engine = Global.getCombatEngine();
		float rampUp = 0.25f + 0.25f * (float) Math.random(),dur = 1f + (float) Math.random();

		if (state == ShipSystemStatsScript.State.IN) {
			ship.fadeToColor(KEY_SHIP, new Color(0, 0, 0, 255), 0.1f, 0.1f, effectLevel);
			ship.setJitterUnder(KEY_SHIP, new Color(255, 100, 100, 255), effectLevel, 15, 0f, 15f);
		}


		if (timer.intervalElapsed()) {
			if (fireamount > 0 && !hasfire) {

				for (int i = 0; i < MathUtils.getRandomNumberInRange(3,5); i++) {
					Vector2f thisParticle_Loc = MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius() / 5);
					Vector2f thisParticle_Direct = VectorUtils.getDirectionalVector(ship.getLocation(),thisParticle_Loc);

					engine.addNegativeSwirlyNebulaParticle(thisParticle_Loc,
							thisParticle_Direct,
							ship.getCollisionRadius()/4,ship.getCollisionRadius()+20f, rampUp, 0.7f,dur,
							new Color(255, 29, 85, 86)
					);
				}

				if (engine.getAsteroids() != null) {
					for (CombatEntityAPI ce : engine.getAsteroids()) {
						if (MathUtils.getDistance(ce, ship) <= 1000f) {
							nearByEnemies.add(ce);
						}
					}
					if (engine.getMissiles() != null) {
					}
					for (CombatEntityAPI cm : engine.getMissiles()) {
						if (cm.getOwner() != ship.getOwner() && MathUtils.getDistance(cm, ship) <= 1000f) {
							nearByEnemies.add(cm);
						}
					}
					if (engine.getShips() != null) {
					}
					for (CombatEntityAPI cs : engine.getShips()) {
						if (cs.getOwner() != ship.getOwner() && MathUtils.getDistance(cs, ship) <= 1000f) {
							nearByEnemies.add(cs);
						}
					}
				}
				//List<MissileAPI> nearByEnemyMissiles = AIUtils.getNearbyEnemyMissiles(ship, 1000f);


				DamagingExplosionSpec boom = new DamagingExplosionSpec(
						0.1f,
						100,
						50,
						100,
						50,
						CollisionClass.PROJECTILE_NO_FF,
						CollisionClass.PROJECTILE_FIGHTER,
						2,
						5,
						5,
						25,
						new Color(186, 218, 174),
						new Color(18, 19, 18)
				);
				boom.setDamageType(DamageType.ENERGY);
				boom.setShowGraphic(false);
				boom.setSoundSetId("riftcascade_rift");


				if (nearByEnemies != null && nearByEnemies.size() > 0) {
					I18nUtil.easyRippleOut(
							ship.getLocation(),
							ship.getVelocity(),
							1000,
							100f,
							1f,
							20f);

					for (CombatEntityAPI Em : nearByEnemies) {

						boom.setMaxDamage(Em.getHitpoints());
						boom.setMinDamage(Em.getHitpoints() / 2);
						boom.setCoreRadius(Em.getCollisionRadius() / 2);
						boom.setRadius(Em.getCollisionRadius() + 20f);
						engine.spawnDamagingExplosion(boom,
								ship,
								Em.getLocation());

						Vector2f EmVel = VectorUtils.clampLength(VectorUtils.getDirectionalVector(ship.getLocation(), Em.getLocation()), 20f);
						engine.addNegativeParticle(Em.getLocation(), EmVel, Em.getCollisionRadius() + 20f, rampUp, dur, negtive_color);
					}
				}
				fireamount--;
			} else {
				hasfire = true;
				timer.setElapsed(0);
				fireamount = 2;
			}
		}

		if (state == ShipSystemStatsScript.State.OUT) {
			stats.getAcceleration().modifyPercent(id, 200f);
			stats.getDeceleration().modifyPercent(id, 200f);
			stats.getTurnAcceleration().modifyFlat(id, 30f);
			stats.getTurnAcceleration().modifyPercent(id, 200f);
			stats.getMaxTurnRate().modifyFlat(id, 15f);
			stats.getMaxTurnRate().modifyPercent(id, 100f);
			ship.setJitterUnder(ship, new Color(100,60,255,174), Math.min((effectLevel + 0.5f), 1f), 2, 0f, 10f);
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getSightRadiusMod().unmodify(id);

		ShipAPI ship = (ShipAPI)stats.getEntity();
		CombatEngineAPI engine = Global.getCombatEngine();

		timer.setElapsed(0);
		hasfire = false;
		fireamount = 2;
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
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
