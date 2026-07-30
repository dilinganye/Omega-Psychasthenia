package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import static com.fs.starfarer.api.util.Misc.ZERO;
import data.scripts.util.IIRT_Omega_Color;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicLensFlare;
import org.magiclib.util.MagicRender;

import java.awt.Color;

public class IIRT_Lab_Volley_Blade_blastGlow implements OnHitEffectPlugin {

	private final Color PARTICLE_COLOR = new Color(255, 244, 221);
	private final Color FLASH_COLOR = new Color(179, 151, 255);
	private final Color EXPLOSION_COLOR = new Color(208, 211, 179);
	private final float PARTICLE_SIZE = 2.5f;
	private final float PARTICLE_BRIGHTNESS = 1;
	private final float PARTICLE_DURATION = 0.5f;

	private final float EXPLOSION_SIZE = 25f;

	private final float FLASH_SIZE = 30f;

	private final float GLOW_SIZE = 60;

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

		MagicLensFlare.createSharpFlare(engine, projectile.getSource(), projectile.getLocation(), 10, 600, 0, new Color(168, 153, 227), new Color(234, 199, 179));
		engine.addSmoothParticle(point, ZERO, 650f, 0.5f, 0.1f, IIRT_Omega_Color.IIRT_Omega_Lab_Word);

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
			engine.addHitParticle(point, new Vector2f(), GLOW_SIZE + (float)Math.random() * 5, 1, 0.05f, Color.WHITE);
		}
	}
}