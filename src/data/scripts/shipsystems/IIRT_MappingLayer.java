package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class IIRT_MappingLayer extends BaseShipSystemScript {

	Color COLOR1 = new Color(220, 210, 255, 255);
	Color COLOR2 = new Color(105, 123, 135, 55);

	float effectLevelLast = 0;

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI)stats.getEntity();
			player = ship == Global.getCombatEngine().getPlayerShip();
			id = id + "_" + ship.getId();
		} else {
			return;
		}

		if (!ship.isAlive()) return;

		if (ship.getCustomData().get("can_use_system") == null) {
			ship.setCustomData("can_use_system", true);
		}

		if (effectLevel != 0 && effectLevelLast == 0) {
			ShipAPI phantom = spawnShip(ship.getOwner(), ship.getVariant(), ship.getLocation(), ship.getVelocity(), ship.getFacing(), ship.getAngularVelocity(), false);
			phantom.setCollisionClass(CollisionClass.FIGHTER);
			phantom.setCustomData("can_use_system", false);
		}

		if ((Boolean)ship.getCustomData().get("can_use_system")) {
			ship.addAfterimage(COLOR1, 0f, 0f, -ship.getVelocity().x, -ship.getVelocity().y, 0f, 0f, 0f, 1f, true, false, false);
		} else {
			ship.setJitter(ship, COLOR2, 1f, 25, 0, 0);
		}

		effectLevelLast = effectLevel;

	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = null;
		boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI)stats.getEntity();
			player = ship == Global.getCombatEngine().getPlayerShip();
			id = id + "_" + ship.getId();
		} else {
			return;
		}

		if (!ship.isAlive()) return;

		if ((Boolean)ship.getCustomData().get("can_use_system")) {
			ship.addAfterimage(COLOR1, 0f, 0f, -ship.getVelocity().x, -ship.getVelocity().y, 0f, 0f, 0f, 1f, true, false, false);
		} else {
			ship.setJitter(ship, COLOR2, 1f, 25, 0, 0);
		}
	}

	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		if (ship.getCustomData().get("can_use_system") == null) {
			ship.setCustomData("can_use_system", true);
		}
		return (Boolean)ship.getCustomData().get("can_use_system");
	}

	private ShipAPI spawnShip(int owner, ShipVariantAPI variant, Vector2f location, Vector2f velocity, float facing, float angular, boolean isAlly) {
		boolean suppress = Global.getCombatEngine().getFleetManager(owner).isSuppressDeploymentMessages();
		Global.getCombatEngine().getFleetManager(owner).setSuppressDeploymentMessages(true);
		//ShipAPI newShip = Global.getCombatEngine().getFleetManager(owner).spawnShipOrWing(variant, location, (float) Math.random() * 360f);
		ShipVariantAPI variantclone = variant.clone();
		variantclone.addMod("IIRT_0DP");
		FleetMemberAPI member = createMember(variantclone);
		member.setOwner(owner);
		member.setAlly(isAlly);
		ShipAPI newShip = Global.getCombatEngine().getFleetManager(owner).spawnFleetMember(member, location, facing, 0f);

		newShip.setAlly(isAlly);
		newShip.setOwner(owner);
		newShip.setCurrentCR(0.7f);
		newShip.getVelocity().set(velocity);
		newShip.setAngularVelocity(angular);

		Global.getCombatEngine().getFleetManager(owner).setSuppressDeploymentMessages(suppress);

		return newShip;
	}

	private FleetMemberAPI createMember(ShipVariantAPI variant) {
		FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
		member.getRepairTracker().setCR(70f);
		member.getCrewComposition().addCrew(member.getHullSpec().getMaxCrew());
		member.getRepairTracker().setCrashMothballed(false);
		member.getRepairTracker().setMothballed(false);
		return member;
	}
}








