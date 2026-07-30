package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

public class IIRT_MappingLayerAI implements ShipSystemAIScript {

	private ShipAPI ship;
	private ShipwideAIFlags flags;
	private ShipSystemAPI system;

	ShipAPI target;

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.flags = flags;
		this.system = system;
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		ship.useSystem();
	}
}