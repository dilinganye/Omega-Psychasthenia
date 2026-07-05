package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.combat.RiftTrailEffect;
import org.lwjgl.util.vector.Vector2f;

public class IIRT_Omega_DDOS_Weapon_EffectPlugin implements OnFireEffectPlugin, OnHitEffectPlugin {

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		Vector2f vel = new Vector2f();
		if (target != null) vel.set(target.getVelocity());
		Global.getSoundPlayer().playSound("rifttorpedo_explosion", 1f, 1f, point, vel);
		if (projectile.getCustomData().get("IIRT_summon_variant") != null) {
			ShipAPI ship = spawnShip(projectile.getOwner(), (String)projectile.getCustomData().get("IIRT_summon_variant"), point, projectile.getVelocity(), projectile.getFacing(), projectile.getAngularVelocity(), false);
			ship.setCollisionClass(CollisionClass.FIGHTER);
		}
	}

	@Override
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		RiftTrailEffect trail = new RiftTrailEffect((MissileAPI)projectile, "rifttorpedo_loop");
		((MissileAPI)projectile).setEmpResistance(1000);
		((MissileAPI)projectile).setEccmChanceOverride(1f);
		Global.getCombatEngine().addPlugin(trail);
	}

	private ShipAPI spawnShip(int owner, String variantID, Vector2f location, Vector2f velocity, float facing, float angular, boolean isAlly) {
		boolean suppress = Global.getCombatEngine().getFleetManager(owner).isSuppressDeploymentMessages();
		Global.getCombatEngine().getFleetManager(owner).setSuppressDeploymentMessages(true);
		ShipAPI newShip = Global.getCombatEngine().getFleetManager(owner).spawnShipOrWing(variantID, location, (float)Math.random() * 360f);
		FleetMemberAPI member = newShip.getFleetMember();

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
