package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.utils.iirt_omega.IIRT_Omega_Color;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicAnim;
import org.magiclib.util.MagicUI;

import java.awt.Color;
import java.util.List;

public class IIRT_Lab_PhasePlasmajets extends BaseShipSystemScript {

	public static float SPEED_BONUS = 275f;
	public static float TURN_BONUS = 25f;

	private Color color = new Color(170, 100, 255, 255);
	private Color UIcolor = new Color(111, 49, 187, 255);
	private Color UILcolor = new Color(49, 125, 187, 255);

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		//引入ShipAPI & CombatEngineAPI
		ShipAPI ship = (ShipAPI)stats.getEntity();
		CombatEngineAPI engine = Global.getCombatEngine();

		float effect = Math.min(1, Math.max(0, MagicAnim.smoothReturnNormalizeRange(effectLevel, 0, 1) / 2 + MagicAnim.smoothReturnNormalizeRange(effectLevel * 1.5f, 0, 1) / 2 + MagicAnim.smoothReturnNormalizeRange(effectLevel * 2, 0, 1) / 2));

		if (ship != null && engine != null) {
			if (Math.random() > 0.9f) {
				//拖影效果
				ship.addAfterimage(IIRT_Omega_Color.IIRT_Omega_Partic_perple, 0f, 0f, -ship.getVelocity().x, -ship.getVelocity().y, 0f, 0f, 0f, 1f, true, false, false);
				ship.setJitter(ship, IIRT_Omega_Color.IIRT_Omega_Lab_Weapon, 1f, 25, 0, 0);
				//引擎电弧
				List<ShipEngineControllerAPI.ShipEngineAPI> engines = ship.getEngineController().getShipEngines();
				for (ShipEngineControllerAPI.ShipEngineAPI enginea : engines) {
					float EmpThickness = MathUtils.getRandomNumberInRange(10f, 30f);
					Vector2f to = MathUtils.getRandomPointOnCircumference(enginea.getLocation(), 200f);
					if (Math.random() > 0.99f) {
						engine.spawnEmpArcVisual(enginea.getLocation(), ship, to, ship, EmpThickness, IIRT_Omega_Color.IIRT_Omega_Lab_Weapon, IIRT_Omega_Color.IIRT_Omega_Partic_perple);
						//engine.addHitParticle(ship.getLocation(), ship.getLocation(), 100f, 0.75f, 0.25f, IIRT_Omega_Color.IIRT_Omega_Partic_perple);
					}
				}
			}
		}

		stats.getTurnAcceleration().modifyPercent(id, 500 * effect);
		stats.getMaxTurnRate().modifyPercent(id, 300 * effect);

		stats.getMaxSpeed().modifyPercent(id, 1000 * effect);
		stats.getAcceleration().modifyPercent(id, 250);
		stats.getDeceleration().modifyPercent(id, 300);

		stats.getTimeMult().modifyPercent(id, 1000 * effect);

		if (stats.getEntity() instanceof ShipAPI) {
			if (ship != null && engine != null) {
				ship.getEngineController().fadeToOtherColor(this, color, new Color(0, 0, 0, 0), effectLevel, 0.67f);
				ship.getEngineController().extendFlame(this, 2f * effectLevel, 0f * effectLevel, 0f * effectLevel);
			}
		}

		effectLevel *= effectLevel;
		float TIME_MULT = 1000f;
		float shipTimeMult = (1f + (TIME_MULT - 1f) * effectLevel) * 1.5F;
		float shipTimeMultNF = shipTimeMult;
		stats.getTimeMult().modifyPercent(id, shipTimeMult);
		boolean player = false;
		// Are you the player?
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI)stats.getEntity();
			player = ship == Global.getCombatEngine().getPlayerShip();
			id = id + "_" + ship.getId();
		} else {
			return;
		}
		// So I might give ya more time
		if (player) {
			Global.getCombatEngine().getTimeMult().modifyMult(id, Math.max(0.1f, 1f / shipTimeMult));
			if (ship != null) {
				MagicUI.drawHUDStatusBar(ship, Math.min(1f, Math.max(0, 1f - shipTimeMult / 1000f)), UIcolor, UILcolor, Math.min(1f, Math.max(0, shipTimeMultNF / 1000f)), "扭曲程度", "相场压制", false);
			}
		} else {
			Global.getCombatEngine().getTimeMult().unmodify(id);
		}

	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getTimeMult().unmodify(id);
		stats.getPeakCRDuration().unmodify(id);

		ShipAPI ship = null;
		boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI)stats.getEntity();
			player = ship == Global.getCombatEngine().getPlayerShip();
			id = id + "_" + ship.getId();
		} else {
			return;
		}

		Global.getCombatEngine().getTimeMult().unmodify(id);
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("提高机动性", false);
		} else if (index == 1) {
			return new StatusData("+" + (int)SPEED_BONUS + " 最高航速", false);
		}
		return null;
	}
}
