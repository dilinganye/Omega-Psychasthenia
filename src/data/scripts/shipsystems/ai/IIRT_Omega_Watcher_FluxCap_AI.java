package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

public class IIRT_Omega_Watcher_FluxCap_AI implements ShipSystemAIScript {

	private static final float CHECK_RANGE = 1000f;
	private static final float TOTAL_ALLY_FLUX_THRESHOLD = 10000f;
	private static final float DESTROYER_UP_FLUX_THRESHOLD = 0.85f;
	private static final float SELF_FORCE_DISPERSE_THRESHOLD = 0.90f;
	private static final String FORCE_DISPERSE_KEY = "iirt_omega_watcher_fluxcap_force_disperse";

	private ShipAPI ship;
	private ShipSystemAPI system;
	private CombatEngineAPI engine;

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.system = system;
		this.engine = engine;
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (ship == null || system == null || engine == null) return;
		if (!ship.isAlive() || ship.isHulk()) return;

		String disperseKey = FORCE_DISPERSE_KEY + "_" + ship.getId();
		if (ship.getFluxLevel() >= SELF_FORCE_DISPERSE_THRESHOLD) {
			engine.getCustomData().put(disperseKey, Boolean.TRUE);
			return;
		}

		engine.getCustomData().remove(disperseKey);

		float totalAllyFlux = 0f;
		boolean shouldActivate = false;

		for (ShipAPI other : engine.getShips()) {
			if (other == null || other == ship) continue;
			if (!other.isAlive() || other.isHulk()) continue;
			if (other.getOwner() != ship.getOwner()) continue;

			float dist = Misc.getDistance(ship.getLocation(), other.getLocation());
			if (dist > CHECK_RANGE) continue;

			float otherFlux = other.getFluxLevel() * other.getMaxFlux();
			totalAllyFlux += otherFlux;

			if (other.getHullSize().ordinal() >= ShipAPI.HullSize.DESTROYER.ordinal()
					&& other.getFluxLevel() >= DESTROYER_UP_FLUX_THRESHOLD) {
				shouldActivate = true;
			}
		}

		if (totalAllyFlux >= TOTAL_ALLY_FLUX_THRESHOLD) {
			shouldActivate = true;
		}

		if (shouldActivate && !ship.getSystem().isActive()) {
			ship.useSystem();
		}
	}
}
