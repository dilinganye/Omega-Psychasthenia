package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.FaderUtil;
import static data.utils.iirt_omega.I18nUtil.easyRippleOut;
import data.utils.iirt_omega.IIRT_Omega_Color;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicLensFlare;

import java.awt.Color;

public class IIRT_PhaseInBattle extends BaseShipSystemScript {

	private final FaderUtil diveFader = new FaderUtil(1f, 1f);
	private boolean i = false;

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI)stats.getEntity();

		//ship.setExtraAlphaMult(0f * effectLevel);
		//ship.setApplyExtraAlphaToEngines(true);

		ship.setPhased(state == State.ACTIVE);

		if (state == State.IDLE) {
			return;
		}

		if (state == State.OUT) {
			// to slow down ship to its regular top speed while powering drive down
			stats.getMaxSpeed().unmodifyFlat(id);
		} else {
			stats.getMaxSpeed().modifyFlat(id, 600f * effectLevel);
			stats.getAcceleration().modifyFlat(id, 600f * effectLevel);
			stats.getHardFluxDissipationFraction().modifyFlat(id, 1f);
			stats.getFluxDissipation().modifyPercent(id, 100f);
			stats.getOverloadTimeMod().modifyMult(id, 0f);

			//I know this is not necessary. But please, live this thing at here.
			if (ship == null || !ship.isAlive()) return;

			if (ship.getPhaseCloak() != null) {
				ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
				ship.getPhaseCloak().forceState(ShipSystemAPI.SystemState.IN, effectLevel);
			} else {
				ship.setPhased(true);
			}

			if (ship.isRetreating() && ship.getFullTimeDeployed() > 5f) { // true retreating
				ship.getVelocity().scale(1f - effectLevel);
				ship.getEngineController().extendFlame(this, 1f - effectLevel, 1f - effectLevel, 1f - effectLevel);
				if (effectLevel == 1f) {
					//We will change this Sound when we can.
					if (diveFader.isIdle()) {
						Global.getSoundPlayer().playSound("system_damper_omega", 1f, 1f, ship.getLocation(), ship.getVelocity());
					}

					float amount = Global.getCombatEngine().getElapsedInLastFrame();
					diveFader.fadeOut();
					diveFader.advance(amount);

					//bright
					float b = diveFader.getBrightness();
					ship.setExtraAlphaMult2(b);

					//radius
					float r = ship.getCollisionRadius() * 5f;

					//color
					Color c = IIRT_Omega_Color.IIRT_Omega_Cipher_glow;

					//Set the Jitter
					ship.setJitter(this, c, b, 20, r * (1f - b));

					if (diveFader.isFadedOut()) {
						ship.getLocation().set(0, -1000000f);
					}
				}

			}
		}

		if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
		} else {
			stats.getMaxSpeed().modifyFlat(id, 600f * effectLevel);
			stats.getAcceleration().modifyFlat(id, 600f * effectLevel);
		}

		//WRRRRRRRRRRwuuuuuuuuuu~
		//if (stats.getEntity() != null && stats.getEntity() instanceof ShipAPI)
		//{
		//舰船设置为相位
		//    ship.setPhased(true);
		//     CombatEngineAPI engine = Global.getCombatEngine();
		//    Vector2f loc = MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius() * 0.5f);
		//    float sizeFactor = MathUtils.getRandomNumberInRange(0.2f, 0.4f) * effectLevel;
		//    float opacity = MathUtils.getRandomNumberInRange(0.6f, 1f) * effectLevel;
		//    float duration = MathUtils.getRandomNumberInRange(0.4f, 0.8f);
		//    engine.addNebulaParticle(loc, I18nUtil.nv, sizeFactor * ship.getCollisionRadius(), 1.2f, 0.25f, opacity, duration, IIRT_Omega_Color.IIRT_Omega_Lab);
		//}

	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);

		//WRRRRRRRRRRwuuuuuuuuuu~
		if (stats.getEntity() instanceof ShipAPI ship) {
			CombatEngineAPI engine = Global.getCombatEngine();
			Vector2f ship_loc = ship.getLocation();
			Vector2f vel = new Vector2f(ship.getVelocity());
			if (i) {
				easyRippleOut(ship.getLocation(), vel, ship.getCollisionRadius() * 4f, 100f, 1f, 20f);
				MagicLensFlare.createSharpFlare(engine, ship, ship_loc, 9f, ship.getCollisionRadius() * 2, ship.getFacing() + 90f, IIRT_Omega_Color.IIRT_Omega_Cipher_glow, IIRT_Omega_Color.IIRT_Omega_Cipher);
				ship.setExtraAlphaMult(1f);
				i = false;
			}
		}
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null || !ship.isAlive()) return;
		ship.setPhased(false);
		diveFader.forceOut();
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("正在介入战场", false);
		}
		return null;
	}
}
