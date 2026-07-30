package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.IIRT_Omega_Color;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class IIRT_Omega_PhaseBeam implements BeamEffectPlugin {
	//Some Part are from Siren: HWIExampleBeam.

	private final Color FLASH_COLOR = new Color(100, 44, 255);
	private final Color EXPLOSION_COLOR = new Color(164, 127, 255);
	private final float EXPLOSION_SIZE = 60f;

	private final float FLASH_SIZE = 60f;
	private final IntervalUtil fireInterval = new IntervalUtil(0.2f, 0.3f);
	protected IntervalUtil timer = new IntervalUtil(0.3f, 0.3f);

	@Override
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		if (engine.isPaused()) return;

		//Cool down PhaseCloak
		ShipAPI ship = beam.getSource();
		if (beam.getBrightness() >= 1) {
			timer.advance(amount);
			if (ship != null && ship.getPhaseCloak() != null) {
				float currCooldown = ship.getPhaseCloak().getCooldownRemaining();
				if (currCooldown > 0) {
					ship.getPhaseCloak().setCooldownRemaining(Math.max(currCooldown - 0.01f, 0f));
				}
			}
		}

		//Some Shock [Just Deco]
		CombatEntityAPI target = beam.getDamageTarget();
		if (target instanceof ShipAPI theTarget) {
			if (theTarget.getShield() == null || !theTarget.getShield().isWithinArc(beam.getTo())) {
				fireInterval.advance(amount * beam.getBrightness());
				if (fireInterval.intervalElapsed()) {
					Vector2f end = MathUtils.getRandomPointOnCircumference(beam.getRayEndPrevFrame(), 10f + (float)Math.random() * 10f);
					engine.spawnEmpArcVisual(beam.getRayEndPrevFrame(), beam.getSource(), end, null, 15f, IIRT_Omega_Color.IIRT_Omega_Lab, IIRT_Omega_Color.IIRTcorePurple);
				}
			}
		}
		Vector2f targetHit = MathUtils.getRandomPointOnCircumference(beam.getRayEndPrevFrame(), 10f + (float)Math.random() * 10f);
		if (beam.getLengthPrevFrame() < 600) {
			engine.spawnExplosion(targetHit, new Vector2f(), EXPLOSION_COLOR, EXPLOSION_SIZE + (float)Math.random() * 5, 0.5f);
			engine.spawnExplosion(targetHit, new Vector2f(), FLASH_COLOR, FLASH_SIZE + (float)Math.random() * 5, 0.25f);
		}
	}
}
