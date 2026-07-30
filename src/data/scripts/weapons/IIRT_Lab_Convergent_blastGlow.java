package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import static com.fs.starfarer.api.util.Misc.ZERO;
import data.scripts.util.I18nUtil;
import static data.scripts.util.I18nUtil.nv;
import data.scripts.util.IIRT_Omega_Color;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicLensFlare;

import java.awt.Color;

public class IIRT_Lab_Convergent_blastGlow implements OnHitEffectPlugin {

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

		float angle = projectile.getWeapon().getCurrAngle();
		for (int x = 0; x < 5; ++x) {
			engine.addNebulaParticle(point, MathUtils.getPointOnCircumference(null, 50f, MathUtils.getRandomNumberInRange(angle + 360f, angle - 360f)), 50, 0.5f, 0.2f, 0.1f, 1.5F, IIRT_Omega_Color.IIRT_Omega_Lab_Word);
		}

		MagicLensFlare.createSharpFlare(engine, projectile.getSource(), projectile.getLocation(), 10, 700, 0, IIRT_Omega_Color.IIRT_Omega_Lab_Weapon, IIRT_Omega_Color.IIRT_Omega_Lab_OtherShip_Phase);
		engine.addSmoothParticle(point, ZERO, 650f, 0.5f, 0.1f, IIRT_Omega_Color.IIRT_Omega_Lab_Weapon);
		if ((float)Math.random() > 0.7f && !shieldHit && target instanceof ShipAPI) {
			float CR = ((ShipAPI)target).getCurrentCR();
			float dam = projectile.getDamage().getDamage();
			float Loss = 0f;
			float EmptyFlux = ((ShipAPI)target).getMaxFlux() * (1 - ((ShipAPI)target).getFluxLevel());
			if (EmptyFlux == 0) {
				Loss = 1;
			} else {
				Loss = dam / EmptyFlux;
			}
			if (Loss >= 1) {
				float NewCR = Math.max(CR - 0.1f, 0f);
				((ShipAPI)target).setCurrentCR(NewCR);
				engine.spawnExplosion(point, nv, new Color(186, 202, 255, 200), 240f, 1.5f);
				I18nUtil.easyRippleOut(point, I18nUtil.nv, 200f, 500f, 0.4f, 60);
				engine.addHitParticle(point, ZERO, 500f, 0.75f, 0.25f, IIRT_Omega_Color.IIRT_Omega_Partic_perple);
				engine.addHitParticle(point, ZERO, 300f, 0.5f, 0.25f, IIRT_Omega_Color.IIRT_Omega_Lab_Weapon);
			} else {
				float NewCR = Math.max(CR - (0.1f) * Loss, 0f);
				((ShipAPI)target).setCurrentCR(NewCR);
				engine.spawnExplosion(point, nv, new Color(186, 202, 255, 200), 120f, 1.5f);
				I18nUtil.easyRippleOut(point, I18nUtil.nv, 100f, 200f, 0.2f, 60);
				engine.addHitParticle(point, ZERO, 400f, 0.5f, 0.25f, IIRT_Omega_Color.IIRT_Omega_Lab_Weapon);
			}
		}
	}
}

