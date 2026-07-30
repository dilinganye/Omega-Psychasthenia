package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class IIRT_SupportPhaseAI implements ShipSystemAIScript {

	private final IntervalUtil inte = new IntervalUtil(1f, 1f);
	public static float RANGE = 2000f;
	private CombatEngineAPI engine;
	private ShipAPI ship;
	private ShipSystemAPI system;

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.engine = engine;
		this.system = system;
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (engine.isPaused() || !ship.isAlive()) {
			return;
		}
		if (!system.isActive() && AIUtils.canUseSystemThisFrame(ship)) {
			for (ShipAPI tship : engine.getShips()) {
				if (tship.isAlly()) {
					if (tship.isPhased()) {
						if (MathUtils.getDistance(ship, tship) <= RANGE) {
							ship.useSystem();
						}
					}
				}
			}
		}

		//if (!system.isActive() && AIUtils.canUseSystemThisFrame(ship)) {
		//    ship.useSystem();
		//}
	}
}