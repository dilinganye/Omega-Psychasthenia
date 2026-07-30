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

public class IIRT_Omega_displacer_shock extends BaseShipSystemScript {

	private int fireamount = 1;
	private final IntervalUtil timer = new IntervalUtil(0.5f, 1.5f);
	public static Object KEY_SHIP = new Object();
	private boolean getOut = false, hasFlip = false, hasCrack = false, runIn = false;

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

		//com.fs.starfarer.api.impl.combat.dweller.RiftLightningEffect
		//com.fs.starfarer.api.impl.combat.RiftCascadeEffect
		//com.fs.starfarer.api.impl.combat.threat.DisplacerGlowScript
		ShipAPI ship = (ShipAPI)stats.getEntity();
		CombatEngineAPI engine = Global.getCombatEngine();

		Color RIFT_LIGHTNING_COLOR = new Color(255, 47, 47, 145);
		Color negtive_color = new Color(200,255,200, 34);
		Color backColor = new Color(0, 0, 0, 0);
		Color MainFadeColor = new Color(255, 100, 100, 255);
		Color MainFadeColor2 = new Color(255, 123, 123, 255);
		Color DecFadeColor = new Color(56, 255, 239, 255);
		Vector2f start = new Vector2f(0,0);
		if (ship.getHullSpec().hasTag("Omega_System_Type_II")){
			MainFadeColor = new Color(100, 255, 227, 255);
			MainFadeColor2 = new Color(123, 255, 240, 255);
			DecFadeColor = new Color(255, 56, 56, 255);
			hasCrack = true;
		}
		if(Global.getSector().getCurrentLocation()!=null && Global.getSector().getCurrentLocation().getBackgroundColorShifter()!=null){
			Global.getSector().getCurrentLocation().getBackgroundColorShifter().getBase();
		}
		float rampUp = 0.25f + 0.25f * (float) Math.random(),dur = 1f + (float) Math.random();
// ————————————————————————————————————————
		if (state == State.IN) {// ————————————————————————————————————————
			ship.fadeToColor(KEY_SHIP, backColor, 0.1f, 0.1f, effectLevel);
			ship.setJitterUnder(KEY_SHIP, MainFadeColor, effectLevel, 15, 0f, 15f);
			start = ship.getLocation();
			if(hasCrack) {
				stats.getTurnAcceleration().modifyMult(id, 0);
				stats.getMaxTurnRate().modifyMult(id, 0);
			}
		}
// ————————————————————————————————————————
		if (state == State.ACTIVE) {// ————————————————————————————————————————
			ship.fadeToColor(KEY_SHIP, MainFadeColor2, 0.1f, 0.1f, effectLevel);
			ship.setJitterUnder(KEY_SHIP, DecFadeColor, effectLevel, 15, 0f, 15f);
			EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
			params.segmentLengthMult = 10f;
			params.zigZagReductionFactor = 0.15f;
			params.fadeOutDist = 50f;
			params.minFadeOutMult = 10f;
//		params.flickerRateMult = 0.7f;
			params.flickerRateMult = 0.23f;

			params.movementDurOverride = Math.max(0.05f, MathUtils.getDistance(start, ship.getLocation()) / 100000f);
			params.flickerRateMult = 0.07f;
			params.glowSizeMult = 3f;
			params.brightSpotFullFraction = 0.4f;
			if (hasCrack) {
				stats.getMaxTurnRate().unmodify(id);
				stats.getTurnAcceleration().unmodify(id);
				stats.getAcceleration().modifyPercent(id, 200f);
				stats.getDeceleration().modifyPercent(id, 200f);
				stats.getMaxSpeed().modifyMult(id, 5f);

				//Color color = weapon.getSpec().getGlowColor();
				EmpArcEntityAPI arc = (EmpArcEntityAPI) engine.spawnEmpArcVisual(MathUtils.getRandomPointInCircle(ship.getLocation(), 400f),
						ship,ship.getLocation(),
						null,
						80f, // thickness
						RIFT_LIGHTNING_COLOR,
						new Color(255, 255, 255, 255),
						params
				);
				arc.setCoreWidthOverride(40f);

				arc.setRenderGlowAtStart(false);
				arc.setFadedOutAtStart(true);
				arc.setSingleFlickerMode(true);

				spawnMine(ship, ship.getLocation(), 0f); // - 0.05f);
			}
			else{
				stats.getMaxTurnRate().modifyMult(id,3f);
				stats.getTurnAcceleration().modifyMult(id,4f);
				stats.getAcceleration().modifyMult(id, 2f);
				stats.getDeceleration().modifyMult(id, 2f);
				stats.getMaxSpeed().modifyMult(id, 1.25f);
			}
		}


// ————————————————————————————————————————
		if (state == State.OUT) { // ————————————————————————————————————————
			for (int i = 0; i < MathUtils.getRandomNumberInRange(1,3); i++) {
					Vector2f thisParticle_Loc = MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius() / 5);
					Vector2f thisParticle_Direct = VectorUtils.getDirectionalVector(ship.getLocation(),thisParticle_Loc);

					engine.addNebulaParticle(thisParticle_Loc,
							thisParticle_Direct,
							ship.getCollisionRadius()/5,ship.getCollisionRadius()/3f+20f, rampUp, 0.7f,dur,
							backColor
					);
					engine.addNegativeSwirlyNebulaParticle(thisParticle_Loc,
							thisParticle_Direct,
							ship.getCollisionRadius()/4,ship.getCollisionRadius()/1.5f+20f, rampUp, 0.7f,dur,
							new Color(79, 255, 94, 40)
					);
			}
			if(ship!=null && ship.getLocation()!=null && ship.getVelocity()!=null && ship.getCollisionRadius()>0) {
				I18nUtil.easyRippleOut(ship.getLocation(), ship.getVelocity(),
						ship.getCollisionRadius(),
						90f,
						ship.getCollisionRadius(),
						ship.getCollisionRadius() / 5f,
						20f);
			}


			if(hasCrack){
				stats.getMaxTurnRate().modifyMult(id,3f);
				stats.getTurnAcceleration().modifyMult(id,4f);
				stats.getAcceleration().modifyMult(id, 2f);
				stats.getDeceleration().modifyMult(id, 2f);
				stats.getMaxSpeed().modifyMult(id, 2f);
			}else{stats.getAcceleration().unmodify(id);
			stats.getDeceleration().unmodify(id);
			stats.getMaxTurnRate().unmodify(id);
			stats.getTurnAcceleration().unmodify(id);
			stats.getMaxSpeed().unmodify(id);}
			ship.setJitterUnder(ship, new Color(100,60,255,174), Math.min((effectLevel + 0.5f), 1f), 2, 0f, 10f);


			if(!hasCrack){
				EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
				params.segmentLengthMult = 8f;
				params.zigZagReductionFactor = 0.15f;
				params.fadeOutDist = 50f;
				params.minFadeOutMult = 10f;
				params.flickerRateMult = 0.3f;
				params.movementDurOverride = Math.max(0.05f, MathUtils.getDistance(start, ship.getLocation()) / 100000f);
				params.glowSizeMult = 3f;
				params.brightSpotFullFraction = 0.4f;

				MissileAPI mine = (MissileAPI) engine.spawnProjectile(ship, null,
						"rift_lightning_minelayer",
						ship.getLocation(),
						(float) Math.random() * 360f, null);
				if (ship != null) {
					Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(
							ship, WeaponAPI.WeaponType.ENERGY, false, mine.getDamage());
				}
				mine.setMineExplosionRange(mine.getMineExplosionRange()*2f);
				mine.explode();

				EmpArcEntityAPI arc = (EmpArcEntityAPI) engine.spawnEmpArcVisual(new Vector2f(ship.getLocation()),
						null, new Vector2f(MathUtils.getRandomPointInCircle(ship.getLocation(), 200f)),
						null,
						80f, // thickness
						RIFT_LIGHTNING_COLOR,
						new Color(255, 255, 255, 255),
						params
				);
				arc.setCoreWidthOverride(40f);

				arc.setRenderGlowAtStart(false);
				arc.setFadedOutAtStart(true);
				arc.setSingleFlickerMode(true);
			}
			ship.fadeToColor(KEY_SHIP, new Color(238, 18, 18, 255), 0.1f, 0.1f, effectLevel);
			ship.setJitterUnder(KEY_SHIP, new Color(99, 18, 238, 255), effectLevel, 15, 0f, 15f);
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
		timer.setElapsed(0);
		hasFlip = false;
		hasCrack = false;
		getOut = false;
		runIn = false;
		fireamount = 1;
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getMaxSpeed().unmodify(id);
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
		//mine.getProjectileSpec().setHitGlowRadius(mine.getProjectileSpec().getHitGlowRadius()*0.75f);
		if (liveTime <= 0.016f) {
			mine.explode();
		}
	}
}
