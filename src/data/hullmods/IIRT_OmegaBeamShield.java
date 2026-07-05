package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

// Thanks to Siren who made this spirit.
public class IIRT_OmegaBeamShield extends BaseHullMod {

	public static final String id = "IIRT_OmegaBeamShield";

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		if (!ship.hasListenerOfClass(IIRTBeamShieldListener.class)) {
			ship.addListener(new IIRTBeamShieldListener(ship, id));
		}
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (ship.getFluxTracker().isOverloaded()) {
			ship.getMutableStats().getEmpDamageTakenMult().unmodify(id);
			for (ShipAPI s : Global.getCombatEngine().getShips()) {
				if (s.getOwner() != ship.getOwner() && Misc.getDistance(ship.getLocation(), s.getLocation()) < 3500f) {
					if (Math.random() < 0.02) {
						arcstrike(ship, s);
					}
				}
			}
			for (CombatEntityAPI e : Global.getCombatEngine().getAsteroids()) {
				if (Misc.getDistance(ship.getLocation(), e.getLocation()) < 3500f) {
					if (Math.random() < 0.02) {
						arcstrike(ship, e);
					}
				}
			}
		} else {
			ship.getMutableStats().getEmpDamageTakenMult().modifyMult(id, 0f);
		}
	}

	public void arcstrike(ShipAPI ship, CombatEntityAPI entity) {
		Global.getCombatEngine().spawnEmpArc(ship, Misc.getPointWithinRadius(ship.getLocation(), ship.getCollisionRadius() / 2f), ship, entity, DamageType.ENERGY, Math.min(ship.getCurrFlux() / 10f, 200f), 0f, 3500f, "tachyon_lance_emp_impact", 15f + (float)(25 * Math.random()), new Color(108, 181, 245, 255), new Color(255, 255, 255, 255));
	}

	public static class IIRTBeamShieldListener implements DamageTakenModifier {

		private final ShipAPI ship;
		private final String id;

		public IIRTBeamShieldListener(ShipAPI ship, String id) {
			this.ship = ship;
			this.id = id;
		}

		@Override
		public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
			if (ship == null || !Global.getCombatEngine().isEntityInPlay(ship) || !ship.isAlive()) {
				ship.removeListener(this);
				return null;
			}
			damage.getModifier().unmodify(id);
			if (ship.getFluxTracker().isOverloaded()) {
				return null;
			}
			if (param instanceof BeamAPI) {
				if (!shieldHit) {
					float dmg = damage.getBaseDamage() * 0.1f;
					if (ship.getMaxFlux() - ship.getCurrFlux() < dmg) {
						ship.getFluxTracker().increaseFlux(dmg, true);
						damage.getModifier().modifyMult(id, 0f);
						return id;
					} else {
						ship.getFluxTracker().increaseFlux(dmg, false);
						damage.getModifier().modifyMult(id, 0f);
						return id;
					}
				}
			}
			return null;
		}
	}
}
