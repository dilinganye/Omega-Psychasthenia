package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class IIRT_SD_DownLight implements EveryFrameWeaponEffectPlugin, OnHitEffectPlugin {

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		ShipAPI ship = weapon.getShip();
        /*
        MutableShipStatsAPI stats = ship.getMutableStats();
        ShipAPI Enemy = getNearestEnemyWhateverRange(weapon);
        float shipDistance = MathUtils.getDistance(ship,Enemy);*/

		if (!ship.hasListenerOfClass(rangeModifier.class)) {
			ship.addListener(new rangeModifier());
		}
	}

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		if (!(target instanceof ShipAPI)) return;
		//ShipAPI ship = (ShipAPI) target;
		WeaponAPI weapon = projectile.getWeapon();
		ShipAPI shipMe = weapon.getShip();
		//MutableShipStatsAPI stats = shipMe.getMutableStats();
		ShipAPI Enemy = getNearestEnemyWhateverRange(weapon);
		float shipDistance = MathUtils.getDistance(shipMe, Enemy);
		if (shipDistance < 900) {
			if (!shieldHit) {
				DamagingProjectileAPI SentryExplosion = engine.spawnDamagingExplosion(DownLightExplosionSpec(), projectile.getSource(), point);
				SentryExplosion.addDamagedAlready(target);
			}
		}

	}

	public DamagingExplosionSpec DownLightExplosionSpec() {
		float damage = 100f;
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
				new Color(100, 40, 255, 255), // particleColor
				new Color(193, 187, 224, 175)  // explosionColor
		);

		spec.setDamageType(DamageType.HIGH_EXPLOSIVE);
		spec.setUseDetailedExplosion(false);
		//spec.setSoundSetId("explosion_guardian");
		return spec;
	}

	public ShipAPI getNearestEnemyWhateverRange(WeaponAPI weapon) {
		ShipAPI closest = null;
		float distance, closestDistance = Float.MAX_VALUE;

		for (ShipAPI tmp : Global.getCombatEngine().getShips()) {
			if (tmp.getOwner() == weapon.getShip().getOwner() || weapon.distanceFromArc(tmp.getLocation()) > 0f) {
				continue;
			}

			distance = MathUtils.getDistance(tmp, weapon.getLocation());

			if (distance < closestDistance) {
				closest = tmp;
				closestDistance = distance;
			}
		}

		return closest;
	}

	public class rangeModifier implements WeaponBaseRangeModifier {

		public rangeModifier() {
		}

		@Override
		public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
			return 0;
		}

		@Override
		public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
			return 1f;
		}

		@Override
		public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
			String WpId = "IIRT_Downlight";
			if (weapon.getId().contentEquals(WpId)) {
				ShipAPI Enemy = getNearestEnemyWhateverRange(weapon);
				float range = weapon.getSpec().getMaxRange();
				float shipDistance = MathUtils.getDistance(ship, Enemy);
				if (shipDistance > 900) {
					float penalty = range + 200f;
					return -penalty;
				}
			}
			return 0f;
		}
	}
}