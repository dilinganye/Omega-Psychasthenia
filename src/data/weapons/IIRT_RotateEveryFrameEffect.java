package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

import java.util.HashMap;
import java.util.Map;

public class IIRT_RotateEveryFrameEffect implements EveryFrameWeaponEffectPlugin {

	public static boolean isInRefit() {
		return Global.getCombatEngine().isInCampaign() || Global.getCombatEngine().getCombatUI() == null;
	}

	private float angle = 0f;

	private static final Map<String, Float> ROTATE_SPEED = new HashMap<>();

	static {
		ROTATE_SPEED.put("IIRT_Omega_Cube_Wave", 100f);
		ROTATE_SPEED.put("IIRT_Omega_Cube_Wave_B", 60f);
		ROTATE_SPEED.put("IIRT_Omega_Cube_Wave_R", -60f);
		ROTATE_SPEED.put("IIRT_FSF_Helico_ten", 240f);
		ROTATE_SPEED.put("IIRT_Omega_Watcher_Line", 70f);
		ROTATE_SPEED.put("IIRT_Omega_Watcher_Ring", 100f);
	}

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		ShipAPI ship = weapon.getShip();
		if (ship == null || !ship.isAlive()) {
			return;
		}

		if (!engine.isPaused() && !weapon.isDisabled()) {
			float BaseAngle = ship.getFacing();
			angle += ROTATE_SPEED.get(weapon.getSpec().getWeaponId()) * amount;
			angle += ship.getAngularVelocity() * amount;
			if(weapon.getSpec().getWeaponId().equals("IIRT_Omega_Watcher_Line")
					|| weapon.getSpec().getWeaponId().equals("IIRT_Omega_Watcher_Ring")){
				if (weapon.getShip().getFluxTracker().getFluxLevel() > 0.5f) {
					angle += ship.getAngularVelocity() * amount/2f*3f;
				} else if (weapon.getShip().getFluxTracker().isOverloadedOrVenting()) {
					angle += ship.getAngularVelocity() * amount/2f*7f;
				} else if (weapon.getShip().getSystem().isActive()){
					angle += ship.getAngularVelocity() * amount/2f*10f;
				}
			}
			if (angle > 360f) angle -= 360f;
			weapon.setCurrAngle(BaseAngle + angle);
		}
	}
}