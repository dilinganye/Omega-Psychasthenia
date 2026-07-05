package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class IIRT_SD_Unstable implements OnHitEffectPlugin {

	private static final Color PARTICLE_COLOR = new Color(191, 232, 225, 255);
	private static final Color EXPLOSION_COLOR = new Color(178, 97, 248, 255);

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		if ((float)Math.random() > 0.75f && target instanceof ShipAPI theTarget) {
			float emp = Math.min((float)Math.random() * projectile.getEmpAmount() + 100.0F, projectile.getEmpAmount());
			float dam = (float)Math.random() * projectile.getDamageAmount() / 2;
			float Zero = 0;

			float pierceChance = theTarget.getHardFluxLevel() - 0.1f;
			pierceChance *= theTarget.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);

			boolean piercedShield = shieldHit && (float)Math.random() < pierceChance;

			if (!shieldHit || piercedShield) {
				engine.spawnEmpArcPierceShields(projectile.getSource(), point, target, target, DamageType.FRAGMENTATION, dam, emp, 100000.0F, null, 20.0F, PARTICLE_COLOR, EXPLOSION_COLOR);

				for (int x = 0; x < 4; ++x) {
					Vector2f point1 = MathUtils.getPointOnCircumference(point, dam, emp);
					Vector2f point2 = new Vector2f(point);
					engine.spawnEmpArc(theTarget, point1, new SimpleEntity(point1), new SimpleEntity(point2), DamageType.ENERGY, Zero, Zero, Math.max((float)Math.random() * 400.0F, 75f), null, (float)Math.random() * 20.0F + 10F, EXPLOSION_COLOR, PARTICLE_COLOR);
				}
			}
		}
	}
}