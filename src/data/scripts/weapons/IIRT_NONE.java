package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

import java.util.HashMap;
import java.util.Map;

public class IIRT_NONE implements EveryFrameWeaponEffectPlugin {

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		weapon.disable();
		weapon.isForceDisabled();
	}
}