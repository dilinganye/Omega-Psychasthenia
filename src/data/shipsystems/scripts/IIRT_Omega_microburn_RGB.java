package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.ui.W;
import data.scripts.util.I18nUtil;
import data.scripts.util.IIRT_Omega_Color;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.List;

public class IIRT_Omega_microburn_RGB extends BaseShipSystemScript {

	protected static float RANGE = 1000f;
	private WeaponSlotAPI ss1; private WeaponAPI S01,S02,S03,SSF,W01,W02,W03;
	private int fireamount = 1;
	private static final IntervalUtil timer = new IntervalUtil(0.07f, 0.2f);
	public static Object KEY_SHIP = new Object();
	private boolean hasCrack = false, runOnce = false;

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		//com.fs.starfarer.api.impl.combat.dweller.RiftLightningEffect
		//com.fs.starfarer.api.impl.combat.RiftCascadeEffect
		//com.fs.starfarer.api.impl.combat.threat.DisplacerGlowScript
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine.isPaused() || engine == null) return;
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null) return; if (!ship.isAlive()) return;
		if (!runOnce) {
			for (WeaponAPI w : ship.getAllWeapons()) {
				switch (w.getSlot().getId()) {
					case "WS0001":
						W01 = w;
						break;
					case "WS0002":
						W02 = w;
						break;
					case "WS0003":
						W03 = w;
						break;
					case "SS_01":
						S01 = w;
						break;
					case "SS_02":
						S02 = w;
						break;
					case "SS_03":
						S03 = w;
						break;
					case "SSF":
						SSF = w;
						break;
				}
			}
			/*for (WeaponSlotAPI s : ship.getHullSpec().getAllWeaponSlotsCopy()) {
				switch (s.getId()) {
				}
			}*/
			runOnce = true;
		}

		Color RIFT_LIGHTNING_COLOR = new Color(255, 47, 47, 145);
		Color negtive_color = new Color(200,255,200, 34);
		Color backColor = new Color(0, 0, 0, 255);
		Color MainFadeColor = new Color(180, 100, 255, 255);
		Color MainFadeColor2 = new Color(160, 123, 255, 255);
		Color DecFadeColor = new Color(248, 56, 255, 255);
		Vector2f ZERO = new Vector2f(0,0);
		if (ship.getHullSpec().hasTag("Omega_System_Type_II")){
			MainFadeColor = new Color(221, 100, 255, 255);
			MainFadeColor2 = new Color(196, 123, 255, 255);
			DecFadeColor = new Color(255, 56, 209, 255);
			hasCrack = true;
		}

		Color testing = new Color(98, 105, 110, 255);
		Color W1C = W01.getSprite().getAverageColor();
		Color W2C = W02.getSprite().getAverageColor();
		Color W3C = W03.getSprite().getAverageColor();
		W1C = new Color(W1C.getRed(), W1C.getGreen(), W1C.getBlue(), 175);
		W2C = new Color(W2C.getRed(), W2C.getGreen(), W2C.getBlue(), 175);
		W3C = new Color(W3C.getRed(), W3C.getGreen(), W3C.getBlue(), 175);
		int T_R = W1C.getRed() + W2C.getRed() + W3C.getRed(),
				T_G = W1C.getGreen() + W2C.getGreen() + W3C.getGreen(),
				T_B = W1C.getBlue() + W2C.getBlue() + W3C.getBlue(),
				T_Max = Math.max(Math.max(T_R,T_G),T_B);
		if(Global.getSector().getCurrentLocation()!=null && Global.getSector().getCurrentLocation().getBackgroundColorShifter()!=null){
			Global.getSector().getCurrentLocation().getBackgroundColorShifter().getBase();
		}
		float rampUp = 0.25f + 0.25f * (float) Math.random(),dur = 1f + (float) Math.random();
// ————————————————————————————————————————_____________________————————————————————————————————————————————
		stats.getMaxSpeed().modifyFlat(id, 750f * effectLevel);
//			stats.getMaxTurnRate().modifyFlat(id, 500f * effectLevel);
//			stats.getTurnAcceleration().modifyFlat(id, 1000f * effectLevel);
		stats.getAcceleration().modifyFlat(id, 1200f * effectLevel);
// ————————————————————————————————————————_____________________————————————————————————————————————————————
		if (state == State.IN) {// ————————————————————————————————————————
			ship.fadeToColor(KEY_SHIP, backColor, 0.1f, 0.1f, effectLevel);
			ship.setJitterUnder(KEY_SHIP, MainFadeColor, effectLevel, 15, 0f, 15f);

			IN_spawnSwirlyParticle(S01.getLocation(),ZERO,
					7,0.1f,W1C);
			IN_spawnSwirlyParticle(S02.getLocation(),ZERO,
					5,0.2f,W2C);
			IN_spawnSwirlyParticle(S03.getLocation(),ZERO,
					8,0.35f,W3C);

			if(hasCrack) {
				stats.getTurnAcceleration().modifyMult(id, 3);
				stats.getMaxTurnRate().modifyMult(id, 2);
			}
		}
// ————————————————————————————————————————
		if (state == State.ACTIVE) {// ————————————————————————————————————————
			IN_spawnParticle(S01.getLocation(),ZERO,
					5,0.1f,W1C);
			IN_spawnParticle(S02.getLocation(),ZERO,
					2,0.2f,W2C);
			IN_spawnParticle(S03.getLocation(),ZERO,
					7,0.35f,W3C);
			/*
			IN_spawnEmp(ship,S01.getLocation(),SSF.getLocation(),
					3f,W1C);
			IN_spawnEmp(ship,S02.getLocation(),SSF.getLocation(),
					3f,W2C);
			IN_spawnEmp(ship,S03.getLocation(),SSF.getLocation(),
					3f,W3C);

			 */


			ship.fadeToColor(KEY_SHIP, MainFadeColor2, 0.1f, 0.1f, effectLevel);
			ship.setJitterUnder(KEY_SHIP, DecFadeColor, effectLevel, 15, 0f, 15f);
			if (hasCrack) {
			}
		}
// ————————————————————————————————————————
		if (state == State.OUT) { // ————————————————————————————————————————
			timer.advance(Global.getCombatEngine().getElapsedInLastFrame());
			ShipAPI target = findTarget(ship);
			Vector2f point = SSF.getLocation(); //SSF是个武器
			engine.spawnDebrisLarge(point, new Vector2f(200,0),
					1, 1, 360, 10f, 10f, 180f);

			if(target == null){
				ship.fadeToColor(KEY_SHIP, new Color(238, 18, 18, 255), 0.1f, 0.1f, effectLevel);
				ship.setJitterUnder(KEY_SHIP, new Color(99, 18, 238, 255), effectLevel, 15, 0f, 15f);
			}
			if(target != null) {
				if (timer.intervalElapsed()) {//————————————————————————————
					if (T_Max == T_R) {
						target.setJitterUnder(target, new Color(200, 0, 0,100),
								effectLevel,
								7,
								0f,
								15f);
						spawnMine(ship, point, 0.1f);
						engine.addNegativeSwirlyNebulaParticle(point, ZERO, 10f, 1.5f, 0f, 0.2f, 0.5f, Color.RED);
						engine.addNegativeNebulaParticle(point, ZERO, 16f, 2f, 0f, 0f, 0.5f, Color.white);

						for (int i = 0; i < MathUtils.getRandomNumberInRange(1, 5); i++) {
							IN_spawnDamageEmp(ship, target, point, DamageType.HIGH_EXPLOSIVE, 100f, 5f, RIFT_LIGHTNING_COLOR);
						}
					}
					if (T_Max == T_G) {
						target.setJitterUnder(target, new Color(0,200,25,100),
								effectLevel,
								7,
								0f,
								15f);
						//com.fs.starfarer.api.impl.combat.threat.VoidblasterEffect
						/*
						engine.addNegativeParticle(point, ZERO, 25f, 0f, 0.5f, Color.white);
						engine.addNegativeParticle(point, ZERO, 20f, 0f, 0.5f, Color.white);
						engine.addNegativeSwirlyNebulaParticle(point, ZERO, 10f, 1.5f, 0f, 0.2f, 0.5f, Color.white);
						engine.addNegativeNebulaParticle(point, ZERO, 30f, 2f, 0f, 0f, 0.5f, Color.white);
						 */


						float dir = Misc.getAngleInDegrees(point, ship.getLocation());
						Vector2f velToTarget = new Vector2f(target.getLocation().x - ship.getLocation().x,
								target.getLocation().y - ship.getLocation().y);
						engine.spawnDebrisSmall(point, VectorUtils.resize(velToTarget, 700f),
								12, dir, 60, 20f, 20f, 720f);
						engine.spawnDebrisMedium(point, VectorUtils.resize(velToTarget, 320f),
								4, dir, 40, 10f, 20f, 500f);
						engine.spawnDebrisLarge(point, VectorUtils.resize(velToTarget, 80f),
								1, dir, 30, 10f, 10f, 300f);

						for (int i = 0; i < MathUtils.getRandomNumberInRange(1, 5); i++) {
							EmpArcEntityAPI arc = engine.spawnEmpArc(
									ship, point, ship, target,
									DamageType.KINETIC,
									100f,
									0f, // emp
									100000f, // max range
									"voidblaster_fire",
									60f, // thickness
									new Color(0, 0, 0, 0),
									new Color(255, 255, 255, 0)
							);
						}
					}
					if (T_Max == T_B) {
						target.setJitterUnder(target, new Color(50, 0, 200,100),
								effectLevel,
								7,
								0f,
								15f);
						for (int i = 0; i < MathUtils.getRandomNumberInRange(1, 3); i++) {
							engine.spawnEmpArc(
									ship, point, target, target,
									DamageType.ENERGY,
									MathUtils.getRandomNumberInRange(100f, 200f),
									1000, // emp
									100000f, // max range
									"mote_attractor_targeted_ship",
									MathUtils.getRandomNumberInRange(15f, 28f), // thickness
									//new Color(100,165,255,255),
									new Color(193, 136, 255, 255),
									new Color(255, 255, 255, 255)
							);
						}
					}
				}
			}



			I18nUtil.easyRippleOut(ship.getLocation(), ship.getVelocity(),
						ship.getCollisionRadius(),
						90f,
						ship.getCollisionRadius(),
						ship.getCollisionRadius()/5f,
						20f);
			ship.setJitterUnder(ship, new Color(100,60,255,174), Math.min((effectLevel + 0.5f), 1f), 2, 0f, 10f);


			if(!hasCrack){
				ship.fadeToColor(KEY_SHIP, new Color(238, 18, 18, 255), 0.1f, 0.1f, effectLevel);
				ship.setJitterUnder(KEY_SHIP, new Color(99, 18, 238, 255), effectLevel, 15, 0f, 15f);
			}
		}

// ————————————————————————————————————————
		if (state == State.COOLDOWN) { // ————————————————————————————————————————
			stats.getAcceleration().unmodify(id);
			stats.getDeceleration().unmodify(id);
			stats.getMaxTurnRate().unmodify(id);
			stats.getTurnAcceleration().unmodify(id);
			stats.getMaxSpeed().unmodify(id);
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		CombatEngineAPI engine = Global.getCombatEngine();
		hasCrack = false;
		runOnce = false;
		fireamount = 1;
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getMaxSpeed().unmodify(id);
	}
	public static float getMaxRange(ShipAPI ship) {
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE);
	}

	public static void spawnMine(ShipAPI source, Vector2f mineLoc, float delay) {
		CombatEngineAPI engine = Global.getCombatEngine();


		//Vector2f currLoc = mineLoc;
		MissileAPI mine = (MissileAPI) engine.spawnProjectile(source, null,
				"rift_lightning_minelayer",
				mineLoc,
				(float) Math.random() * 360f, null);
		if (source != null) {
			Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(
					source, WeaponAPI.WeaponType.ENERGY, false, mine.getDamage());
		}


		float fadeInTime = 0.05f;
		mine.getVelocity().scale(0);
		mine.fadeOutThenIn(fadeInTime);

		float liveTime = Math.max(delay, 0f);
		mine.setFlightTime(mine.getMaxFlightTime() - liveTime);
		mine.addDamagedAlready(source);
		mine.setNoMineFFConcerns(true);
		mine.getProjectileSpec().setHitGlowRadius(mine.getProjectileSpec().getHitGlowRadius()*0.75f);
		if (liveTime <= 0.016f) {
			mine.explode();
		}
	}
	protected ShipAPI findTarget(ShipAPI ship) {
		float range = getMaxRange(ship);
		boolean player = ship == Global.getCombatEngine().getPlayerShip();
		ShipAPI target = ship.getShipTarget();

		if (ship.getShipAI() != null && ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.TARGET_FOR_SHIP_SYSTEM)){
			target = (ShipAPI) ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.TARGET_FOR_SHIP_SYSTEM);
			if (target != null
					&& target.getOriginalOwner() == ship.getOriginalOwner()) target = null;
		}

		if (target != null) {
			float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
			float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
			if (dist > range + radSum) target = null;
		} else {
			if (target == null || target.getOwner() == ship.getOwner()) {
				if (player) {
					target = Misc.findClosestShipEnemyOf(ship, ship.getMouseTarget(), ShipAPI.HullSize.FRIGATE, range, true);
				} else {
					Object test = ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET);
					if (test instanceof ShipAPI) {
						target = (ShipAPI) test;
						float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
						float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
						if (dist > range + radSum || target.isFighter()) target = null;
						if (target != null && target.getOriginalOwner() == ship.getOriginalOwner()) target = null;
					}
				}
			}
		}

		if (target != null && target.isFighter()) target = null;
		if (target == null) {
			target = Misc.findClosestShipEnemyOf(ship, ship.getLocation(), ShipAPI.HullSize.FRIGATE, range, true);
		}

		return target;
	}
	public static void IN_spawnEmp(ShipAPI source, Vector2f start, Vector2f end, float thickness, Color color) {
		CombatEngineAPI engine = Global.getCombatEngine();


		EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
		params.segmentLengthMult = 4f;
		params.zigZagReductionFactor = 0.35f;
		params.fadeOutDist = 50f;
		params.minFadeOutMult = 3f;
		params.flickerRateMult = 0.7f;

		params.movementDurOverride = Math.max(0.05f, MathUtils.getDistance(start, end) / 100000f);
		EmpArcEntityAPI arc = (EmpArcEntityAPI) engine.spawnEmpArcVisual(
				start, source,
				end, null,
				thickness, // thickness
				color,
				new Color(255, 255, 255, 255),
				params
		);
		arc.setCoreWidthOverride(thickness+2f);

		arc.setRenderGlowAtStart(false);
		arc.setFadedOutAtStart(true);
		arc.setSingleFlickerMode(true);
	}
	public static void IN_spawnDamageEmp(ShipAPI source,ShipAPI target, Vector2f start, DamageType damageType,float damage,float thickness, Color color) {
		CombatEngineAPI engine = Global.getCombatEngine();


		EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
		params.segmentLengthMult = 10f;
		params.zigZagReductionFactor = 0.35f;
		params.fadeOutDist = 50f;
		params.minFadeOutMult = 3f;
		params.flickerRateMult = 0.7f;

		params.movementDurOverride = Math.max(0.05f, MathUtils.getDistance(start, source.getLocation()) / 100000f);
		params.glowSizeMult = 2f;
		params.brightSpotFullFraction = 0.4f;
		EmpArcEntityAPI arc = (EmpArcEntityAPI) engine.spawnEmpArc(source, start, source, target,
				damageType,
				damage,
				0f,
				10000f,
				"rifttorpedo_fire",
				thickness, // thickness
				color,
				new Color(255, 255, 255, 255),
				params
		);
		arc.setCoreWidthOverride(40f);

		arc.setRenderGlowAtStart(false);
		arc.setFadedOutAtStart(true);
		arc.setSingleFlickerMode(true);
	}
	public static void IN_spawnParticle(Vector2f loc, Vector2f vel, float sizeStart, float sizeEndMult, Color color) {
		CombatEngineAPI engine = Global.getCombatEngine();
		timer.advance(Global.getCombatEngine().getElapsedInLastFrame());
		if (timer.intervalElapsed()) {//————————————————————————————
			engine.addNebulaParticle(
					loc,
					vel,
					sizeStart,sizeEndMult,
					0.7f,0.4765f,
					0.4375f,
					color);
		}
	}
	public static void IN_spawnSwirlyParticle(Vector2f loc, Vector2f vel, float sizeStart, float sizeEndMult, Color color) {
		CombatEngineAPI engine = Global.getCombatEngine();
		timer.advance(Global.getCombatEngine().getElapsedInLastFrame());
		if (timer.intervalElapsed()) {//————————————————————————————
			engine.addSwirlyNebulaParticle(
					loc, vel,
					sizeStart,sizeEndMult,
					0.7f,0.4765f,
					0.4375f,
					color,true);
		}
	}

}
