package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import data.utils.iirt_omega.I18nUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicLensFlare;
import org.magiclib.util.MagicRender;

import java.awt.Color;

public class IIRT_Omega_ThermosiphonOnHitEffect implements OnHitEffectPlugin {

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		ShipAPI source = projectile.getSource();
		float decrease = 0;
		if (source != null) {
			decrease = Math.min(200, source.getCurrFlux());
			source.getFluxTracker().decreaseFlux(decrease);
		}
		if (target instanceof ShipAPI) {
			if (shieldHit) {
				ShipAPI t = (ShipAPI)target;
				t.getFluxTracker().increaseFlux(decrease, false);
			} else {
				engine.applyDamage(target, point, decrease, DamageType.ENERGY, decrease, false, true, source);
			}
		} else {
			engine.applyDamage(target, point, decrease, DamageType.ENERGY, decrease, false, true, source);
		}
		if (decrease > 0) {
			engine.spawnEmpArcVisual(projectile.getWeapon().getLocation(), source, point, target, decrease / 8f, new Color(195, 229, 253, 175), Color.WHITE);
		}

		I18nUtil.easyRippleOut(point, I18nUtil.nv, 80f, 100f, 0.1f, 60);
		MagicLensFlare.createSharpFlare(engine, projectile.getSource(), projectile.getLocation(), 10, 700, 0, new Color(195, 229, 253), new Color(186, 192, 255));

		float size = MathUtils.getRandomNumberInRange(8, 16);
		float glowth = MathUtils.getRandomNumberInRange(64, 128);

		if (MagicRender.screenCheck(0.1f, point)) {
			if (decrease > 0) {
				MagicRender.battlespace(Global.getSettings().getSprite("fx", "IIRT_Omega_Shock_0" + MathUtils.getRandomNumberInRange(0, 4)), new Vector2f(target.getLocation()), new Vector2f(target.getVelocity()), new Vector2f(size, size), new Vector2f(glowth, glowth), MathUtils.getRandomNumberInRange(0, 360), MathUtils.getRandomNumberInRange(-10, 10), new Color(186, 192, 255, 255), true, 0, MathUtils.getRandomNumberInRange(0.05f, 0.15f), MathUtils.getRandomNumberInRange(0.1f, 0.2f));

				MagicRender.battlespace(Global.getSettings().getSprite("fx", "IIRT_Omega_Shock_0" + MathUtils.getRandomNumberInRange(5, 9)), new Vector2f(target.getLocation()), new Vector2f(target.getVelocity()), new Vector2f(size, size), new Vector2f(glowth, glowth), MathUtils.getRandomNumberInRange(0, 360), MathUtils.getRandomNumberInRange(-10, 10), new Color(186, 192, 255, 255), true, 0, MathUtils.getRandomNumberInRange(0.05f, 0.15f), MathUtils.getRandomNumberInRange(0.1f, 0.2f));
			}
		}
	}
}