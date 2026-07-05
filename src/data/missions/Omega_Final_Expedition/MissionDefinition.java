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

		// Set a small blurb for each fleet that shows up on the mission detail and
		// mission results screens to identify each side.
		FleetSide DevSide,Devside_E;
		Boolean isDevmode = Global.getSettings().isDevMode();
		if(isDevmode){
			api.setFleetTagline(FleetSide.PLAYER, "乱码尸骸");
			api.setFleetTagline(FleetSide.ENEMY, "人类 联军");
			api.addBriefingItem("目标：消灭全部[人类]部队");
			DevSide = FleetSide.PLAYER;
			Devside_E = FleetSide.ENEMY;
			api.initFleet(FleetSide.PLAYER, "???", FleetGoal.ATTACK, false);
			api.initFleet(FleetSide.ENEMY, "Human", FleetGoal.ATTACK, true);
		}else{
			api.setFleetTagline(FleetSide.PLAYER, "人类 联军");
			api.setFleetTagline(FleetSide.ENEMY, "乱码尸骸");
			api.addBriefingItem("目标：抵御入侵");
			DevSide = FleetSide.ENEMY;
			Devside_E = FleetSide.PLAYER;
			api.initFleet(FleetSide.ENEMY, "???", FleetGoal.ATTACK, true);
			api.initFleet(FleetSide.PLAYER, "Human", FleetGoal.ATTACK, true);
		}

		// These show up as items in the bulleted list under
		// "Tactical Objectives" on the mission detail screen

		// Set up the player's fleet.

		api.addToFleet(DevSide, "IIRT_Omega_Firewall_Only", FleetMemberType.SHIP, true);
		api.addToFleet(DevSide, "IIRT_Omega_Antitrack_Only2", FleetMemberType.SHIP, true);

		//api.addToFleet(DevSide, "Omega_Scrap_Heap_Garbage", FleetMemberType.SHIP, "要塞", false);

		api.addToFleet(DevSide, "IIRT_Omega_Inner_Normal_1", FleetMemberType.SHIP, false);

		api.addToFleet(DevSide, "IIRT_Omega_Torsion_2_Normal_4", FleetMemberType.SHIP, false);
		api.addToFleet(DevSide, "IIRT_Omega_Tranquil_Normal_4", FleetMemberType.SHIP, false);
		api.addToFleet(DevSide, "IIRT_Omega_Riots_Normal_1", FleetMemberType.SHIP, false);
		api.addToFleet(DevSide, "IIRT_Omega_Riots_Normal_3", FleetMemberType.SHIP, false);
		api.addToFleet(DevSide, "IIRT_Omega_Bustle_Normal_1", FleetMemberType.SHIP, false);


		api.addToFleet(DevSide, "IIRT_Omega_Inspect_Normal_1", FleetMemberType.SHIP,  false);
		api.addToFleet(DevSide, "IIRT_Omega_Inspect_Normal_4", FleetMemberType.SHIP, false);


		// Set up the enemy fleet.
		api.addToFleet(Devside_E, "onslaught_xiv_Elite", FleetMemberType.SHIP, true);
		api.addToFleet(Devside_E, "onslaught_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "dominator_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "dominator_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "dominator_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "dominator_XIV_Elite", FleetMemberType.SHIP, false);
		
		api.addToFleet(Devside_E, "heron_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "condor_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "condor_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "condor_Support", FleetMemberType.SHIP, false);
		
		api.addToFleet(Devside_E, "eagle_xiv_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "falcon_xiv_Escort", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "eagle_xiv_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "falcon_xiv_Escort", FleetMemberType.SHIP, false);
		
		api.addToFleet(Devside_E, "enforcer_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "enforcer_XIV_Elite", FleetMemberType.SHIP, false);
		
		api.addToFleet(Devside_E, "lasher_CS", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "lasher_CS", FleetMemberType.SHIP, false);

		api.addToFleet(Devside_E, "lasher_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "lasher_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "lasher_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "kite_hegemony_Interceptor", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "kite_hegemony_Interceptor", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "hound_hegemony_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "hound_hegemony_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "hound_hegemony_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "hound_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "hound_Standard", FleetMemberType.SHIP, false);


		api.addToFleet(Devside_E, "hammerhead_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "hammerhead_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "hammerhead_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "dominator_Outdated", FleetMemberType.SHIP, "HSS Temblor", false);
		api.addToFleet(Devside_E, "monitor_Escort", FleetMemberType.SHIP, "HSS Aspis", false);
		api.addToFleet(Devside_E, "monitor_Escort", FleetMemberType.SHIP, "HSS Aegis", false);
		api.addToFleet(Devside_E, "buffalo2_FS", FleetMemberType.SHIP, "HSS Archer", false);
		api.addToFleet(Devside_E, "kite_hegemony_Interceptor", FleetMemberType.SHIP, "HSS Gadfly", false);
		api.addToFleet(Devside_E, "kite_hegemony_Interceptor", FleetMemberType.SHIP, "HSS Midge", false);
		api.addToFleet(Devside_E, "hound_Standard", FleetMemberType.SHIP, "Daisy", false);
		api.addToFleet(Devside_E, "hound_Standard", FleetMemberType.SHIP, "Lucy", false);


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