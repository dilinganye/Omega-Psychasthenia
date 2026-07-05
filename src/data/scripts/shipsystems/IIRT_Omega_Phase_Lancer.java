package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import static com.fs.starfarer.api.util.Misc.ZERO;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class IIRT_Omega_Phase_Lancer extends BaseShipSystemScript {

	public static final float Lancer_TIME_MULT = 3f;

	public static final Color JITTER_COLOR = new Color(255, 173, 173, 55);

	boolean isFriAct = false, isSecAct = false;

	float StarFacing = 0f;

	Vector2f startLoc = new Vector2f();
	Vector2f endLoc = new Vector2f();

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		CombatEngineAPI engine = Global.getCombatEngine();

		if (state == State.IN) {
			float shipTimeMult = 1f + (Lancer_TIME_MULT - 1f) * effectLevel;
			ship.getEngineController().fadeToOtherColor(this, JITTER_COLOR, new Color(0, 0, 0, 0), effectLevel, 0.5f);
			ship.getEngineController().extendFlame(this, -0.25f, -0.25f, -0.25f);
			WaveDistortion wave = new WaveDistortion(startLoc, ZERO);
			wave.setIntensity(15f);
			wave.setSize(250f);
			wave.flip(false);
			wave.setLifetime(0f);
			wave.fadeOutIntensity(0.5f);
			wave.setLocation(startLoc);
			DistortionShader.addDistortion(wave);
		}
		if (state == State.ACTIVE) {
			if (!isFriAct) {
				startLoc = new Vector2f(ship.getLocation().x, ship.getLocation().y);
				StarFacing = ship.getFacing();
				isFriAct = true;
			}

		}

		if (state == State.OUT) {
			//once
			if (!isSecAct) {

				endLoc = new Vector2f(ship.getLocation());
				isSecAct = true;
			}
		}

	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getTimeMult().unmodify(id);
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getCombatEngineRepairTimeMult().unmodify(id);
		stats.getTimeMult().unmodify(id);

		ShipAPI ship = null;
		boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {

		}
	}
}