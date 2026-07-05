package data.weapons;

import com.fs.starfarer.api.combat.*;

import java.awt.Color;

public class PhaseDecorationWeapon implements EveryFrameWeaponEffectPlugin {

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		ShipAPI ship = weapon.getShip();
		ShipSystemAPI cloak = ship.getPhaseCloak();
		if (cloak == null) cloak = ship.getSystem();
		if (cloak == null) return;
		if (cloak.isActive()) {
			Color c = weapon.getSprite().getColor();
			Color n = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(255 * cloak.getEffectLevel()));
			weapon.getSprite().setColor(n);
		} else {
			Color c = weapon.getSprite().getColor();
			Color n = new Color(c.getRed(), c.getGreen(), c.getBlue(), 0);
			weapon.getSprite().setColor(n);
		}
	}
}
