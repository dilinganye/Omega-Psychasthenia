package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import static data.utils.iirt_omega.I18nUtil.easyRippleOut;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;

public class IIRT_Lab_Overflow_Gun_Fire implements EveryFrameWeaponEffectPlugin {

	private boolean firing = false;
	private boolean moreThan5 = false;
	private float recoil = 0;
	private float AmmoHad = 0;
	private double Vectorx = 0;
	private double Vectory = 0;
	private Vector2f Speedo2;
	private final float maxRecoil = -10;

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

		if (engine.isPaused() || weapon.getShip().getOriginalOwner() == -1) {return;}
		if (weapon.getChargeLevel() == 1) {

			firing = true;
			//Vector2f muzzle;

			//recoil
			recoil = Math.min(1, recoil + 0.33f);

			//muzzle non hidden weapon
			if (MagicRender.screenCheck(0.1f, weapon.getLocation())) {

				for (DamagingProjectileAPI p : CombatUtils.getProjectilesWithinRange(weapon.getLocation(), 150)) {
					if (p.getWeapon() != weapon) continue;
					engine.addHitParticle(p.getLocation(), weapon.getShip().getVelocity(), 75f, 0.25f, 0.33f, new Color(200, 96, 0, 32));
				}
				//muzzle = MathUtils.getPoint(weapon.getLocation(), 40 - (recoil), weapon.getCurrAngle());
                /*
                engine.addHitParticle(
                        muzzle,
                        weapon.getShip().getVelocity(),
                        30,
                        0.5f,
                        1,
                        Color.green
                );
                engine.addHitParticle(
                        muzzle,
                        weapon.getShip().getVelocity(),
                        30,
                        0.5f,
                        0.3f,
                        Color.red
                );*/
			}
		}
		Vector2f point = weapon.getLocation();
		AmmoHad = weapon.getAmmo();
		if (firing && weapon.getAmmo() >= 5f) {
			moreThan5 = true;
		}
		if (firing && weapon.getChargeLevel() < 1) {
			firing = false;
		}
		if (firing) {
			weapon.setAmmo(0);
		}
		if (firing && !moreThan5) {
			float Kode = AmmoHad / 5;
			Vector2f muzzle;
			muzzle = MathUtils.getPoint(weapon.getLocation(), 40 - (recoil), weapon.getCurrAngle());
			engine.addHitParticle(muzzle, weapon.getShip().getVelocity(), 5, 0.05f, 0.35f, new Color(172, 139, 239));

			Vectorx = Math.sin(weapon.getCurrAngle()) * Kode;
			Vectory = Math.cos(weapon.getCurrAngle()) * Kode;
			Speedo2 = new Vector2f((float)Vectorx, (float)Vectory);
			//muzzle non hidden weapon
			if (MagicRender.screenCheck(0.1f, weapon.getLocation())) {

				muzzle = MathUtils.getPoint(weapon.getLocation(), 40 - (recoil * maxRecoil), weapon.getCurrAngle());

				engine.addHitParticle(muzzle, Speedo2, 20 * Kode, 0.5f, 1, Color.blue);
				engine.addHitParticle(muzzle, Speedo2, 20 * Kode, 1f, 0.3f, Color.red);
				engine.addSmoothParticle(muzzle, Speedo2, 30, 2f, 0.15f, Color.white);
				engine.addSmoothParticle(muzzle, Speedo2, 50 * Kode, 2f, 0.1f, Color.white);

				easyRippleOut(muzzle, Speedo2, 30 * Kode, 1, 0, 0.25f);
			}
		}
		if (firing && moreThan5) {
			Vector2f muzzle;
			muzzle = MathUtils.getPoint(weapon.getLocation(), 40 - (recoil), weapon.getCurrAngle());
			Global.getSoundPlayer().playSound("IIRT_Lab_Heavy_Rifle_more", 1, 1, weapon.getLocation(), weapon.getShip().getVelocity());
			engine.addHitParticle(muzzle, weapon.getShip().getVelocity(), 5, 0.05f, 1, new Color(172, 139, 239));

			Vectorx = Math.sin(weapon.getCurrAngle());
			Vectory = Math.cos(weapon.getCurrAngle());
			Speedo2 = new Vector2f((float)Vectorx, (float)Vectory);
			//muzzle non hidden weapon
			if (MagicRender.screenCheck(0.1f, weapon.getLocation())) {

				muzzle = MathUtils.getPoint(weapon.getLocation(), 40 - (recoil * maxRecoil), weapon.getCurrAngle());

				engine.addHitParticle(muzzle, weapon.getShip().getVelocity(), 20, 0.5f, 1, Color.blue);
				engine.addHitParticle(muzzle, weapon.getShip().getVelocity(), 20, 1f, 0.3f, Color.red);
				engine.addSmoothParticle(muzzle, weapon.getShip().getVelocity(), 30, 2f, 0.15f, Color.white);
				engine.addSmoothParticle(muzzle, weapon.getShip().getVelocity(), 50, 2f, 0.1f, Color.white);

				easyRippleOut(muzzle, weapon.getShip().getVelocity(), 50, 1, 0, 0.25f);

			}

			MagicRender.battlespace(Global.getSettings().getSprite("fx", "IIRT_Impact_Directional_01"), point, new Vector2f(((float)Vectorx) * 10, ((float)Vectory) * 10), new Vector2f(64, 64), new Vector2f(96, 96),
					//angle,
					weapon.getCurrAngle() + 90f,
					//+2f*(float)Math.random()+90f
					0, new Color(232, 238, 255, 150), true, 0.5f, 2f, 0.25f);

			moreThan5 = false;
		}
	}
}