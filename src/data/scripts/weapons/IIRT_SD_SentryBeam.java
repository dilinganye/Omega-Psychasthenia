package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import static com.fs.starfarer.api.util.Misc.ZERO;
import data.utils.iirt_omega.IIRT_Omega_Color;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;

public class IIRT_SD_SentryBeam implements BeamEffectPlugin {

	private final Color PARTICLE_COLOR = new Color(221, 255, 251);
	private final Color FLASH_COLOR = new Color(255, 151, 151);
	private final Color EXPLOSION_COLOR = new Color(191, 179, 211);
	private final float PARTICLE_SIZE = 4.5f;
	private final float PARTICLE_BRIGHTNESS = 1;
	private final float PARTICLE_DURATION = 0.5f;

	private final float EXPLOSION_SIZE = 25f;

	private final float FLASH_SIZE = 30f;

	private final float GLOW_SIZE = 70;
	private final IntervalUtil fireInterval = new IntervalUtil(0.2f, 0.3f);

	public static boolean shieldHit(BeamAPI beam, ShipAPI target) {
		return target.getShield().isWithinArc(beam.getTo());
	}

	@Override
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		CombatEntityAPI target = beam.getDamageTarget();
		if (target instanceof ShipAPI theTarget) {
			float dur = beam.getDamage().getDpsDuration();
			fireInterval.advance(dur);
			if (fireInterval.intervalElapsed()) {
				if (beam.didDamageThisFrame() && beam.getBrightness() >= 1f) {
					Vector2f point = beam.getRayEndPrevFrame();
					engine.addSmoothParticle(point, ZERO, 650f, 0.5f, 0.1f, IIRT_Omega_Color.IIRTcorePurple);

					DamagingProjectileAPI SentryExplosion = engine.spawnDamagingExplosion(SentryExplosionSpec(), beam.getSource(), point);
					SentryExplosion.addDamagedAlready(target);
					//engine.spawnDamagingExplosion(SentryExplosionSpec, theTarget, point);
                    /*
                    StandardLight light = new StandardLight(weapon.getShip().getLocation(), ZERO, ZERO, (CombatEntityAPI)null);
                    light.setIntensity(3.0F);
                    light.setSize(3500.0F);
                    light.setColor(EXPLOSION_COLOR);
                    light.fadeOut(2.0F);
                    LightShader.addLight(light);*/

					float emp = beam.getDamage().getFluxComponent() * 0.5f;
					float dam = beam.getDamage().getDamage() * 0.25f;
					engine.spawnEmpArc(beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(), DamageType.FRAGMENTATION, dam, // damage
							emp, // emp
							100000f, // max range
							"tachyon_lance_emp_impact", beam.getWidth() + 5f, beam.getFringeColor(), beam.getCoreColor());

					if (MagicRender.screenCheck(0.1f, point)) {
						for (float i = 0; i <= 5; i++) {
							float particleSize = MathUtils.getRandomNumberInRange(PARTICLE_SIZE - 2, PARTICLE_SIZE + 2);
							Vector2f randSpawnPoint = MathUtils.getRandomPointOnCircumference(point, EXPLOSION_SIZE);
							Vector2f randExitVector = VectorUtils.getDirectionalVector(point, randSpawnPoint);
							randExitVector.scale(EXPLOSION_SIZE * 2);
							engine.addHitParticle(randSpawnPoint, randExitVector, particleSize, PARTICLE_BRIGHTNESS, PARTICLE_DURATION, PARTICLE_COLOR);
						}

						//void spawnExplosion(Vector2f loc, Vector2f vel, Color color, float size, float maxDuration);
						engine.spawnExplosion(point, new Vector2f(), EXPLOSION_COLOR, EXPLOSION_SIZE + (float)Math.random() * 5, 0.5f);
						engine.spawnExplosion(point, new Vector2f(), FLASH_COLOR, FLASH_SIZE + (float)Math.random() * 5, 0.25f);
						engine.addHitParticle(point, new Vector2f(), GLOW_SIZE + (float)Math.random() * 5, 1, 0.05f, IIRT_Omega_Color.IIRT_SD_word);
					}
				}
			}
		}
	}

	public DamagingExplosionSpec SentryExplosionSpec() {
		float damage = 10f;
		DamagingExplosionSpec spec = new DamagingExplosionSpec(0.1f, // duration
				45f, // radius
				50f, // coreRadius
				damage, // maxDamage
				damage / 2f, // minDamage
				CollisionClass.PROJECTILE_NO_FF, // collisionClass
				CollisionClass.PROJECTILE_FIGHTER, // collisionClassByFighter
				3f, // particleSizeMin
				3f, // particleSizeRange
				0.5f, // particleDuration
				150, // particleCount
				new Color(243, 169, 169, 255), // particleColor
				new Color(218, 190, 253, 175)  // explosionColor
		);

		spec.setDamageType(DamageType.ENERGY);
		spec.setUseDetailedExplosion(false);
		//spec.setSoundSetId("explosion_guardian");
		return spec;
	}
}