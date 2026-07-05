// Powered by Cat Magic

package data.missions.Omega_Warn;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;


public class MissionDefinition implements MissionDefinitionPlugin {


	@Override
	public void defineMission(MissionDefinitionAPI api) {

		// Set up the fleets so we can add ships and fighter wings to them.
		// In this scenario, the fleets are attacking each other, but
		// in other scenarios, a fleet may be defending or trying to escape
		api.initFleet(FleetSide.PLAYER, "???", FleetGoal.ATTACK, false);
		api.initFleet(FleetSide.ENEMY, "Derelict", FleetGoal.ATTACK, true);

		// Set a small blurb for each fleet that shows up on the mission detail and
		// mission results screens to identify each side.

		api.setFleetTagline(FleetSide.PLAYER, "未知");
		api.setFleetTagline(FleetSide.ENEMY, "未知");

		// These show up as items in the bulleted list under
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("提示：活下去");

		// Set up the player's fleet.

		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Bit_Only", FleetMemberType.SHIP, true);
		//api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Heart_Only", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Kb_Only", FleetMemberType.SHIP, false);

		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Cube_Shock", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Cube_Shock", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Cube_Shock_Plus", FleetMemberType.SHIP, false);

		// Set up the enemy fleet.

		api.addToFleet(FleetSide.ENEMY, "guardian_Standard", FleetMemberType.SHIP, true);
		api.addToFleet(FleetSide.ENEMY, "guardian_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "rampart_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "rampart_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "rampart_Standard", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "guardian_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "guardian_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "rampart_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "rampart_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "rampart_Standard", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "guardian_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "guardian_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "rampart_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "rampart_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "rampart_Standard", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "defender_PD", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "picket_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sentry_FS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "warden_Defense", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "defender_PD", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "picket_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sentry_FS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "warden_Defense", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "defender_PD", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "picket_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sentry_FS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "warden_Defense", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "defender_PD", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "picket_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sentry_FS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "warden_Defense", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sentry_FS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "warden_Defense", FleetMemberType.SHIP, false);

		api.addToFleet(FleetSide.ENEMY, "bastillon_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "bastillon_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "bastillon_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "berserker_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "berserker_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "berserker_Assault", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "bastillon_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "bastillon_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "bastillon_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "berserker_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "berserker_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "berserker_Assault", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "bastillon_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "bastillon_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "bastillon_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "berserker_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "berserker_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "berserker_Assault", FleetMemberType.SHIP, false);


		// Set up the map.
		float width = 10000f;
		float height = 10000f;
		api.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f);
		api.setBackgroundSpriteName("graphics/backgrounds/background1.jpg");

		float minX = -width / 2;
		float minY = -height / 2;

		// Add an asteroid field
		api.addAsteroidField(minX, minY + height / 2f, 0f, 4000f, 5f, 50f, 50);
		api.addPlanet(-500f, 500f, 5f, StarTypes.YELLOW, 50f, true);

		api.addNebula(-400, 2100, 200f);
	}
}