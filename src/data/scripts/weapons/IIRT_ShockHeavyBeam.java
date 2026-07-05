// 非常感谢k的硬辐能beam代码授权，
// 我完全不会写代码qwq，太感谢了
// IIRT_Theaten等硬辐能武器使用

package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.util.IntervalUtil;
import data.utils.iirt_omega.IIRT_Omega_Color;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class IIRT_ShockHeavyBeam implements BeamEffectPlugin {

	private final IntervalUtil fireInterval = new IntervalUtil(0.2f, 0.3f);

	public static boolean shieldHit(BeamAPI beam, ShipAPI target) {
		return target.getShield() != null && target.getShield().isOn() && target.getShield().isWithinArc(beam.getTo());
	}

	@Override
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		CombatEntityAPI target = beam.getDamageTarget();
		if (target instanceof ShipAPI theTarget) {
			if (theTarget.getShield() == null || !theTarget.getShield().isWithinArc(beam.getTo())) {
				fireInterval.advance(amount * beam.getBrightness());
				if (fireInterval.intervalElapsed()) {
					Vector2f end = MathUtils.getRandomPointOnCircumference(beam.getRayEndPrevFrame(), 50f + (float)Math.random() * 10f);
					engine.spawnEmpArcVisual(beam.getRayEndPrevFrame(), beam.getSource(), end, null, 15f, IIRT_Omega_Color.IIRTcorePurple_Light, IIRT_Omega_Color.IIRTcorePurple);
				}

				if (beam.didDamageThisFrame()) {
					float damage = beam.getDamage().getDamage() * beam.getDamage().getDpsDuration();

					if (beam.getWeapon().getSize() == WeaponSize.SMALL) {
						damage *= 1f;
					} else if (beam.getWeapon().getSize() == WeaponSize.MEDIUM) {
						damage *= 0.90f;
					} else if (beam.getWeapon().getSize() == WeaponSize.LARGE) {
						damage *= 0.85f;
					}

					if (!shieldHit(beam, theTarget)) {
						theTarget.getFluxTracker().increaseFlux(damage, true);
					}
				}
			}
		}
	}
}