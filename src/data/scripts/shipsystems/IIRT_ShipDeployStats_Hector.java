package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.combat.MineStrikeStatsAIInfoProvider;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.utils.iirt_omega.I18nUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class IIRT_ShipDeployStats_Hector extends BaseShipSystemScript implements MineStrikeStatsAIInfoProvider {

	private static final float RANGE_FACTOR = 900f;
	private static final float MIN_SPAWN_DIST = 75f;

	private static boolean IsHector = false;
	private static boolean IsNum2 = false;
	private static boolean ally = false;

	public static float getRange(ShipAPI ship) {
		if (ship == null) {
			return RANGE_FACTOR;
		}
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE_FACTOR);
	}

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) {
			return;
		}
		if (ship.isAlly()) {
			ally = true;
		}
		if (state == State.OUT) {
			effectLevel *= effectLevel;
		}

		if (effectLevel == 1) {
			Vector2f target = ship.getMouseTarget();
			if (ship.getShipAI() != null && ship.getAIFlags().hasFlag(AIFlags.SYSTEM_TARGET_COORDS)) {
				target = (Vector2f)ship.getAIFlags().getCustom(AIFlags.SYSTEM_TARGET_COORDS);
			}

			if (target != null) {
				float dist = MathUtils.getDistance(ship, target);
				float max = getMineRange(ship) + ship.getCollisionRadius();
				if (dist > max) {
					target = VectorUtils.getDirectionalVector(ship.getLocation(), target);
					target.scale(max);
					Vector2f.add(target, ship.getLocation(), target);
				}

				target = findClearLocation(target);
				/*——————————————————————————————————————————————————————————————————————————————————————————————
				 *随机生成任意“暴徒”
				 **/
				float Random = (float)Math.random();
				if (Random >= 0.5f) {
					IsHector = true;
				} else {
					IsHector = true;
					IsNum2 = true;
				}
				spawnMine(ship, target);

			}

		}
	}

	private static void spawnMine(ShipAPI source, Vector2f mineLoc) {
		CombatEngineAPI engine = Global.getCombatEngine();
		Vector2f currLoc = MathUtils.getRandomPointOnCircumference(mineLoc, 20f + (float)Math.random() * 20f);

		float start = (float)Math.random() * 360f;
		for (float angle = start; angle < start + 390; angle += 30f) {
			if (angle != start) {
				Vector2f loc = MathUtils.getPointOnCircumference(null, 50f + (float)Math.random() * 30f, angle);
				currLoc = Vector2f.add(mineLoc, loc, I18nUtil.nv);
			}

			for (MissileAPI other : engine.getMissiles()) {
				if (!other.isMine()) {
					continue;
				}
				float dist = MathUtils.getDistance(currLoc, other.getLocation());
				if (dist < other.getCollisionRadius() + 40f) {
					currLoc = null;
					break;
				}
			}

			if (currLoc != null) {
				break;
			}
		}

		if (currLoc == null) {
			currLoc = MathUtils.getRandomPointOnCircumference(mineLoc, 20f + (float)Math.random() * 20f);
		}

		CombatFleetManagerAPI manager = engine.getFleetManager(source.getOwner());
		boolean orig = manager.isSuppressDeploymentMessages();

		manager.setSuppressDeploymentMessages(true);
		/*——————————————————————————————————————————————————————————————————————————————————————————————
		 *检测目标条件对应生成船只
		 *
		 **/
		if (IsHector) {   //暴徒
			if (IsNum2) {
				ShipAPI newShip = manager.spawnShipOrWing("IIRT_Hector_Boomer_Ship", currLoc, (float)Math.random() * 360f, 1);
				manager.setSuppressDeploymentMessages(orig);
				newShip.setAlly(ally);
				ShipAPI newShip2 = manager.spawnShipOrWing("IIRT_Hector_Boomer_Ship", currLoc, (float)Math.random() * 360f, 1);
				manager.setSuppressDeploymentMessages(orig);
				newShip2.setAlly(ally);
				Global.getSoundPlayer().playSound("mine_teleport", 1f, 1f, newShip.getLocation(), newShip.getVelocity());
				//生成扭曲
				I18nUtil.easyRippleOut(newShip.getLocation(), newShip.getVelocity(), newShip.getCollisionRadius() * 4f, 100f, 1f, 20f);
			} else {
				ShipAPI newShip = manager.spawnShipOrWing("IIRT_Hector_Fighter_Ship", currLoc, (float)Math.random() * 360f, 1);
				manager.setSuppressDeploymentMessages(orig);
				newShip.setAlly(ally);
				ShipAPI newShip2 = manager.spawnShipOrWing("IIRT_Hector_Fighter_Ship", currLoc, (float)Math.random() * 360f, 1);
				manager.setSuppressDeploymentMessages(orig);
				newShip2.setAlly(ally);
				ShipAPI newShip3 = manager.spawnShipOrWing("IIRT_Hector_Fighter_Ship", currLoc, (float)Math.random() * 360f, 1);
				manager.setSuppressDeploymentMessages(orig);
				newShip3.setAlly(ally);
				Global.getSoundPlayer().playSound("mine_teleport", 1f, 1f, newShip.getLocation(), newShip.getVelocity());
				//生成扭曲
				I18nUtil.easyRippleOut(newShip.getLocation(), newShip.getVelocity(), newShip.getCollisionRadius() * 4f, 100f, 1f, 20f);
			}
			IsHector = false;

		} else {   //BD
			ShipAPI newShip = manager.spawnShipOrWing("IIRT_Hector_Boomer_Ship", currLoc, (float)Math.random() * 360f, 3);
			manager.setSuppressDeploymentMessages(orig);
			newShip.setAlly(ally);
			Global.getSoundPlayer().playSound("mine_teleport", 1f, 1f, newShip.getLocation(), newShip.getVelocity());
			//生成扭曲
			I18nUtil.easyRippleOut(newShip.getLocation(), newShip.getVelocity(), newShip.getCollisionRadius() * 4f, 100f, 1f, 20f);
		}
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo()) {
			return null;
		}
		if (system.getState() != SystemState.IDLE) {
			return null;
		}

		Vector2f target = ship.getMouseTarget();
		if (target != null) {
			float dist = MathUtils.getDistance(ship, target);
			float max = getMineRange(ship) + ship.getCollisionRadius();
			if (dist > max) {
				return "超出范围";
			} else {
				return "正在待命";
			}
		}

		return null;
	}

	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		return ship.getMouseTarget() != null;
	}

	private static Vector2f findClearLocation(Vector2f dest) {
		if (isLocationClear(dest)) {
			return dest;
		}
		float incr = 50f;

		WeightedRandomPicker<Vector2f> tested = new WeightedRandomPicker<>();
		for (float distIndex = 1; distIndex <= 32f; distIndex *= 2f) {
			float start = (float)Math.random() * 360f;
			for (float angle = start; angle < start + 360; angle += 60f) {
				Vector2f loc = MathUtils.getPointOnCircumference(null, incr * distIndex, angle);
				Vector2f.add(dest, loc, loc);
				tested.add(loc);
				if (isLocationClear(loc)) {
					return loc;
				}
			}
		}

		if (tested.isEmpty()) {
			return dest; // shouldn't happen
		}
		return tested.pick();
	}

	private static boolean isLocationClear(Vector2f loc) {
		for (ShipAPI other : Global.getCombatEngine().getShips()) {
			if (other.isShuttlePod()) {
				continue;
			}
			if (other.isFighter()) {
				continue;
			}
			Vector2f otherLoc = other.getShieldCenterEvenIfNoShield();
			float otherR = other.getShieldRadiusEvenIfNoShield();
			if (MathUtils.getDistance(loc, otherLoc) < otherR + MIN_SPAWN_DIST) {
				return false;
			}
		}

		for (CombatEntityAPI other : Global.getCombatEngine().getAsteroids()) {
			float dist = MathUtils.getDistance(other, loc);
			if (dist < other.getCollisionRadius() + MIN_SPAWN_DIST) {
				return false;
			}
		}

		return true;
	}

	@Override
	public float getFuseTime() {
		return 3f;
	}

	@Override
	public float getMineRange(ShipAPI ship) {
		return getRange(ship);
	}
}