// Powered by Cat Magic

package data.missions.Omega_Uprising;

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
		api.initFleet(FleetSide.PLAYER, "INT", FleetGoal.ATTACK, false);
		api.initFleet(FleetSide.ENEMY, "ISS", FleetGoal.ATTACK, true);

		// Set a small blurb for each fleet that shows up on the mission detail and
		// mission results screens to identify each side.

		FleetSide DevSide,Devside_E;
		Boolean isDevmode = Global.getSettings().isDevMode();
		if(isDevmode){
			api.setFleetTagline(FleetSide.PLAYER, "网络冥魂");
			api.setFleetTagline(FleetSide.ENEMY, "人类卫戍部队");
			api.addBriefingItem("目标：消灭全部[人类]部队");
			DevSide = FleetSide.PLAYER;
			Devside_E = FleetSide.ENEMY;
		}else{
			api.setFleetTagline(FleetSide.PLAYER, "人类卫戍部队");
			api.setFleetTagline(FleetSide.ENEMY, "网络冥魂");
			api.addBriefingItem("目标：抵御入侵");
			DevSide = FleetSide.ENEMY;
			Devside_E = FleetSide.PLAYER;
		}

		// Set up the player's fleet.
		api.addToFleet(DevSide, "Omega_EPP_Lazer", FleetMemberType.SHIP, true);

		api.addToFleet(DevSide, "IIRT_Omega_Firewall_Only", FleetMemberType.SHIP, false);
		api.addToFleet(DevSide, "IIRT_Omega_Antitrack_Only_3", FleetMemberType.SHIP, false);
		
		//api.addToFleet(DevSide, "IIRT_Omega_Proxy_Only", FleetMemberType.SHIP, false);
		api.addToFleet(DevSide, "IIRT_Omega_Proxy_Only2", FleetMemberType.SHIP, false);
		//api.addToFleet(DevSide, "IIRT_Omega_Proxy_Only3", FleetMemberType.SHIP, false);

		api.addToFleet(DevSide, "IIRT_Omega_Gateway_Only", FleetMemberType.SHIP, false);
		//api.addToFleet(DevSide, "IIRT_Omega_Gateway_Only2", FleetMemberType.SHIP, false);
		api.addToFleet(DevSide, "IIRT_Omega_Gateway_Only5", FleetMemberType.SHIP, false);
		
		api.addToFleet(DevSide, "IIRT_Omega_Bit_Only", FleetMemberType.SHIP, false);
		api.addToFleet(DevSide, "IIRT_Omega_Kb_Only", FleetMemberType.SHIP, false);

		// Set up the enemy fleet.
		api.addToFleet(Devside_E, "station3_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "onslaught_xiv_Elite", FleetMemberType.SHIP, true);
		api.addToFleet(Devside_E, "onslaught_xiv_Elite", FleetMemberType.SHIP, true);
		api.addToFleet(Devside_E, "onslaught_xiv_Elite", FleetMemberType.SHIP, true);
		api.addToFleet(Devside_E, "dominator_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "dominator_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "dominator_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "dominator_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "enforcer_XIV_Elite", FleetMemberType.SHIP, false);
		
		api.addToFleet(Devside_E, "enforcer_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "enforcer_XIV_Elite", FleetMemberType.SHIP, false);
		
		
		api.addToFleet(Devside_E, "buffalo2_FS", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "buffalo2_FS", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "buffalo2_FS", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "buffalo2_FS", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "lasher_CS", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "lasher_CS", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "dram_Light", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "tarsus_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "tarsus_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "heron_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "condor_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "condor_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(Devside_E, "condor_Support", FleetMemberType.SHIP, false);


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