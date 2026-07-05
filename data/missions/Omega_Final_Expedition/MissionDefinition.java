package data.missions.Omega_Final_Expedition;

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
		api.initFleet(FleetSide.ENEMY, "Human", FleetGoal.ATTACK, true);

		// Set a small blurb for each fleet that shows up on the mission detail and
		// mission results screens to identify each side.

		api.setFleetTagline(FleetSide.PLAYER, "乱码尸骸");
		api.setFleetTagline(FleetSide.ENEMY, "人类 联军");

		// These show up as items in the bulleted list under
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("目标：消灭全部[人类]部队");

		// Set up the player's fleet.

		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Firewall_Only", FleetMemberType.SHIP, "防火墙", true);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Antitrack_Only2", FleetMemberType.SHIP, "反跟踪", true);

		api.addToFleet(FleetSide.PLAYER, "Omega_Scrap_Heap_Garbage", FleetMemberType.SHIP, "要塞", false);

		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Inner_Normal_1", FleetMemberType.SHIP, "内形", false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Inner_Normal_1", FleetMemberType.SHIP, "内形", false);

		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Torsion_2_Normal", FleetMemberType.SHIP, "扭矩", false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Torsion_2_Normal", FleetMemberType.SHIP, "扭矩", false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Torsion_2_Normal_4", FleetMemberType.SHIP, "扭矩", false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Tranquil_Normal_4", FleetMemberType.SHIP, "宁静", false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Tranquil_Normal_4", FleetMemberType.SHIP, "宁静", false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Riots_Normal_1", FleetMemberType.SHIP, "缭乱", false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Riots_Normal_3", FleetMemberType.SHIP, "缭乱", false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Riots_Normal_4", FleetMemberType.SHIP, "缭乱", false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Bustle_Normal_1", FleetMemberType.SHIP, "喧嚣", false);


		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Inspect_Normal_1", FleetMemberType.SHIP, "侦察", false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Inspect_Normal_4", FleetMemberType.SHIP, "侦察", false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Deplorable_Normal_5", FleetMemberType.SHIP, "可叹", false);


		// Set up the enemy fleet.
		api.addToFleet(FleetSide.ENEMY, "hammerhead_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hammerhead_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hammerhead_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "dominator_Outdated", FleetMemberType.SHIP, "HSS Temblor", false);
		api.addToFleet(FleetSide.ENEMY, "monitor_Escort", FleetMemberType.SHIP, "HSS Aspis", false);
		api.addToFleet(FleetSide.ENEMY, "monitor_Escort", FleetMemberType.SHIP, "HSS Aegis", false);
		api.addToFleet(FleetSide.ENEMY, "buffalo2_FS", FleetMemberType.SHIP, "HSS Archer", false);
		api.addToFleet(FleetSide.ENEMY, "kite_hegemony_Interceptor", FleetMemberType.SHIP, "HSS Gadfly", false);
		api.addToFleet(FleetSide.ENEMY, "kite_hegemony_Interceptor", FleetMemberType.SHIP, "HSS Midge", false);
		api.addToFleet(FleetSide.ENEMY, "hound_Standard", FleetMemberType.SHIP, "Daisy", false);
		api.addToFleet(FleetSide.ENEMY, "hound_Standard", FleetMemberType.SHIP, "Lucy", false);
		

		api.addToFleet(FleetSide.ENEMY, "onslaught_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "dominator_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "dominator_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "dominator_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "dominator_XIV_Elite", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "heron_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "condor_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "condor_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "condor_Support", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "eagle_xiv_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "falcon_xiv_Escort", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "eagle_xiv_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "falcon_xiv_Escort", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "enforcer_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "enforcer_XIV_Elite", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "lasher_CS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "lasher_CS", FleetMemberType.SHIP, false);

		api.addToFleet(FleetSide.ENEMY, "lasher_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "lasher_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "lasher_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "kite_hegemony_Interceptor", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "kite_hegemony_Interceptor", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hound_hegemony_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hound_hegemony_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hound_hegemony_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hound_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hound_Standard", FleetMemberType.SHIP, false);


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