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

		api.setFleetTagline(FleetSide.PLAYER, "网络冥魂");
		api.setFleetTagline(FleetSide.ENEMY, "人类卫戍部队");

		// These show up as items in the bulleted list under
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("提示：赶在[人类]部队建立防线之前封锁空间站");

		// Set up the player's fleet.
		
		api.addToFleet(FleetSide.PLAYER, "Omega_EPP_Lazer", FleetMemberType.SHIP, "EPP 终端防护平台", true);

		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Firewall_Only", FleetMemberType.SHIP, "防火墙", false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Antitrack_Only_3", FleetMemberType.SHIP, "反跟踪", false);
		
		
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Proxy_Only", FleetMemberType.SHIP, "代理", false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Proxy_Only2", FleetMemberType.SHIP, "代理", false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Proxy_Only3", FleetMemberType.SHIP, "代理", false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Proxy_Only4", FleetMemberType.SHIP, "代理", false);

		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Gateway_Only", FleetMemberType.SHIP, "网关", false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Gateway_Only2", FleetMemberType.SHIP, "网关", false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Gateway_Only3", FleetMemberType.SHIP, "网关", false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Gateway_Only4", FleetMemberType.SHIP, "网关", false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Gateway_Only5", FleetMemberType.SHIP, "网关", false);
		
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Bit_Only", FleetMemberType.SHIP, "字节", false);
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Kb_Only", FleetMemberType.SHIP, "字节", false);

		// Set up the enemy fleet.
		api.addToFleet(FleetSide.ENEMY, "station3_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "dominator_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "dominator_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "dominator_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "dominator_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "enforcer_XIV_Elite", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "enforcer_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "enforcer_XIV_Elite", FleetMemberType.SHIP, false);
		
		
		api.addToFleet(FleetSide.ENEMY, "buffalo2_FS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "buffalo2_FS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "buffalo2_FS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "buffalo2_FS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "lasher_CS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "lasher_CS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "dram_Light", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "tarsus_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "tarsus_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "heron_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "condor_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "condor_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "condor_Support", FleetMemberType.SHIP, false);


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