package data.weapons;

import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;

public class PTSD_SnowStorm implements OnFireEffectPlugin,EveryFrameWeaponEffectPlugin, OnHitEffectPlugin {
	//com.fs.starfarer.api.impl.combat.CryofluxTransducerEffect
	private boolean runOnce = false;
	private final HashMap<WeaponAPI, Float> theAng = new HashMap<>();
	private float recoil = 0;
	private final float maxRecoil = -15;
	IntervalUtil timer = new IntervalUtil(0.1f, 0.5f);

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

		if (engine.isPaused() || weapon.getShip().getOriginalOwner() == -1 || !weapon.getShip().isAlive()) {
			return;
		}

		if (!runOnce) {
			runOnce = true;
			return;
		}

		Vector2f muzzle;

		//recoil
		recoil = Math.min(1, recoil + 0.33f);
		if (weapon.isFiring() && weapon.getChargeLevel() == 1f) {
			muzzle = MathUtils.getPoint(weapon.getLocation(), 150 - (recoil * maxRecoil), weapon.getCurrAngle());

			engine.addHitParticle(muzzle, weapon.getShip().getVelocity(), 170, 0.5f, 0.5f, Color.blue);
			engine.addHitParticle(muzzle, weapon.getShip().getVelocity(), 90, 0.7f, 0.25f, Color.red);
			engine.addSmoothParticle(muzzle, weapon.getShip().getVelocity(), 220, 1.5f, 0.15f, Color.white);
		}
	}

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		float MaxExplodeDamage = 250f;
		if (target instanceof ShipAPI) {

			engine.addHitParticle(point, new Vector2f(), 10 + (float) Math.random() * 5, 1, 0.05f, new Color(109, 217, 236, 255));
			engine.addHitParticle(point, new Vector2f(), 10 + (float) Math.random() * 5, 1, 0.05f, new Color(109, 130, 236, 255));

			if (target instanceof MissileAPI || target instanceof AsteroidAPI) return;
			Vector2f Speedo = projectile.getVelocity();
			Vector2f Speedo2 = new Vector2f(Speedo);
			Speedo2.setX(Speedo.getX() / 8f);
			;
			Speedo2.setY(Speedo.getY() / 8f);
			Vector2f Speedo3 = new Vector2f(Speedo);
			Speedo3.setX(Speedo.getX() / 25f);
			;
			Speedo3.setY(Speedo.getY() / 25f);
			float Angle = projectile.getFacing();

			engine.addHitParticle(point, new Vector2f(), 50, 2f, 0.25f, Color.white);
			engine.addSmoothParticle(point, new Vector2f(), 60, 2f, 0.1f, Color.white);
			engine.spawnExplosion(point, new Vector2f(), new Color(202, 243, 255), 5 + (float) Math.random() * 5, 0.25f);

			/*MagicRender.battlespace(Global.getSettings().getSprite("fx", "IIRT_Impact_Directional_01"), point, Speedo3, new Vector2f(100, 100), new Vector2f(204, 200),
					//angle,
					Angle + 2 * (float)Math.random() + 90f, 0, Color.white, true, 0.15f, 0.05f, 0.2f);
			 */

			engine.addNebulaSmokeParticle(MathUtils.getRandomPointOnCircumference(point, 50f), new Vector2f(),
					MathUtils.getRandomNumberInRange(75, 300f), MathUtils.getRandomNumberInRange(0.75f, 1.25f), 0.7f, 0.834f,
					1f, new Color(130, 162, 156, 75));

			DamagingExplosionSpec boom = new DamagingExplosionSpec(
					0.1f,
					100,
					50,
					MaxExplodeDamage,
					MaxExplodeDamage / 5f,
					CollisionClass.PROJECTILE_NO_FF,
					CollisionClass.PROJECTILE_FIGHTER,
					7, 5,
					6, 25,
					new Color(155, 196, 152), new Color(116, 144, 255));
			boom.setDamageType(DamageType.HIGH_EXPLOSIVE);
			boom.isUseDetailedExplosion();
			boom.setDetailedExplosionFlashColorCore(new Color(218, 200, 255, 195));
			boom.setDetailedExplosionFlashColorFringe(new Color(163, 152, 196, 150));
			boom.setDetailedExplosionFlashRadius(85f);
			boom.setShowGraphic(true);
			boom.setSoundSetId("explosion_flak");

			if (!shieldHit) {
				engine.spawnDamagingExplosion(boom, projectile.getSource(), point);
				/*//稳定损伤护盾
				float HitPoints = target.getHitpoints();
				float DamageToHull = MathUtils.getRandomNumberInRange(100f, 1000f);
				if (HitPoints <= DamageToHull) {
					boom.setMaxDamage(HitPoints*2);
					engine.spawnDamagingExplosion(boom, projectile.getSource(), target.getLocation());
				}
				target.setHitpoints(HitPoints - DamageToHull);
				Vector2f EnemyPlaced = target.getLocation();
				Vector2f PrjtPlaced = point;
				Vector2f NowPlace = point;
				float HalfPlacex = (EnemyPlaced.getX() + PrjtPlaced.getX()) / 2;
				float HalfPlacey = (EnemyPlaced.getY() + PrjtPlaced.getY()) / 2;
				NowPlace.setX(HalfPlacex);
				NowPlace.setY(HalfPlacey);
				engine.addFloatingDamageText(NowPlace, DamageToHull, Color.red, target, target);
				 */
				/*
				MagicRender.battlespace(Global.getSettings().getSprite("fx", "IIRT_Spark01A"), point, Speedo2, new Vector2f(32, 64), new Vector2f(104, 186),
						//angle,
						Angle + 2f * (float)Math.random() + 90f, 0, new Color(149, 248, 134, 255), true, 0.1f, 0.25f, 0.15f);
				MagicRender.battlespace(Global.getSettings().getSprite("fx", "IIRT_Spark01A"), point, Speedo3, new Vector2f(32, 64), new Vector2f(94, 186),
						//angle,
						Angle + 2f * (float)Math.random() + 90f, 0, new Color(248, 134, 229, 255), true, 0.2f, 0.2f, 0.1f);
				MagicRender.battlespace(Global.getSettings().getSprite("fx", "IIRT_Spark01B"), point, Speedo2, new Vector2f(32, 64), new Vector2f(100, 320),
						//angle,
						Angle + 2 * (float)Math.random() + 90f, 0, new Color(101, 197, 210, 255), false, 0.1f, 0.05f, 0.35f);
				 */
				engine.addNebulaSmokeParticle(MathUtils.getRandomPointOnCircumference(point, 30f), new Vector2f(),
						MathUtils.getRandomNumberInRange(100f, 150f), MathUtils.getRandomNumberInRange(0.5f, 1.35f), 0.7f, 0.834f,
						1f, new Color(150, 189, 224, 155));
				engine.addSmoothParticle(point, new Vector2f(), 175, 1f, 0.1f, Color.BLUE);
			}

			if (shieldHit) {
				boom.setDamageType(DamageType.FRAGMENTATION);
				engine.spawnDamagingExplosion(boom, projectile.getSource(), point);
			}

			if (MagicRender.screenCheck(0.1f, point)) {
				for (float i = 0; i <= 5; i++) {
					float particleSize = MathUtils.getRandomNumberInRange(60, 100);
					Vector2f randSpawnPoint = MathUtils.getRandomPointOnCircumference(point, 70);
					Vector2f randExitVector = VectorUtils.getDirectionalVector(point, randSpawnPoint);
					randExitVector.scale(70 * 2);
					engine.addHitParticle(randSpawnPoint, randExitVector, particleSize, 0.2f, 0.2f, new Color(150, 245, 220, 160));
				}

				engine.addSmoothParticle(point, new Vector2f(), 60, 2f, 0.1f, Color.orange);
				engine.spawnExplosion(point, new Vector2f(), new Color(233, 228, 238), 10 + (float) Math.random() * 40, 0.25f);
			}
		}
	}

	@Override
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		timer.advance(engine.getElapsedInLastFrame());
		if (timer.intervalElapsed()) {

			EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
			params.segmentLengthMult = 10f;
			params.zigZagReductionFactor = 0.15f;
			params.fadeOutDist = 50f;
			params.minFadeOutMult = 10f;
//		params.flickerRateMult = 0.7f;
			params.flickerRateMult = 0.23f;

			params.movementDurOverride = Math.max(0.05f, MathUtils.getRandomNumberInRange(1000f, 5000f) / 10000f);
			params.flickerRateMult = 0.07f;
			params.glowSizeMult = 3f;
			params.brightSpotFullFraction = 0.4f;
			EmpArcEntityAPI arc = (EmpArcEntityAPI) engine.spawnEmpArcVisual(
					MathUtils.getRandomPointInCircle(projectile.getSpawnLocation(), 5f),
					weapon.getShip(), projectile.getLocation(),
					null,
					80f, // thickness
					new Color(123,112,245, 102),
					new Color(255, 255, 255, 255),
					params
			);
			arc.setCoreWidthOverride(40f);

			arc.setRenderGlowAtStart(true);
			arc.setFadedOutAtStart(true);
			arc.setSingleFlickerMode(true);
		}
	}
}