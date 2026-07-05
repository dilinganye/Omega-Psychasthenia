// Powered by Cat Magic

package data.missions.Omega_Harassing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.combat.EscapeRevealPlugin;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;


public class MissionDefinition implements MissionDefinitionPlugin {


	@Override
	public void defineMission(MissionDefinitionAPI api) {
		Global.getSoundPlayer().playCustomMusic(1,1,"music_KRM_market_hostile",true);
		// Set up the fleets so we can add ships and fighter wings to them.
		api.initFleet(FleetSide.PLAYER, "ISS", FleetGoal.ATTACK, false, 3);
		api.initFleet(FleetSide.ENEMY, "???", FleetGoal.ATTACK, true, 5);
		api.setFleetTagline(FleetSide.PLAYER, "速子科技 特遣佣兵舰队");
		api.setFleetTagline(FleetSide.ENEMY, "未知舰体群");

		api.addBriefingItem("提示：敌人为=网络冥魂=系列，其具备优秀的火力配置和更机警的AI");
		api.addBriefingItem("不要拖延战斗，时间永远不在你这一边");

		// Set up the player's fleet.
		api.addToFleet(FleetSide.PLAYER, "doom_Strike", FleetMemberType.SHIP, "Edward", true);
		api.addToFleet(FleetSide.PLAYER, "odyssey_Balanced", FleetMemberType.SHIP, "Never The Les", true);
		//api.addToFleet(FleetSide.PLAYER, "legion_Assault", FleetMemberType.SHIP, "No-fear Of Nightmares", false);
		api.addToFleet(FleetSide.PLAYER, "doom_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "doom_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "hyperion_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "hyperion_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "eagle_Balanced", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "eagle_Balanced", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "mora_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "mora_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "mora_Assault", FleetMemberType.SHIP, false);
		//api.addToFleet(FleetSide.PLAYER, "scarab_Experimental", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "scarab_Experimental", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "monitor_Escort", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "monitor_Escort", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "monitor_Escort", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "wolf_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "wolf_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "omen_PD", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "tempest_Attack", FleetMemberType.SHIP, false);


		// Mark player flagship as essential
		//api.defeatOnShipLoss("No-fear Of Nightmares");

		// Set up the enemy fleet.
		api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_DevilFork_Missile", FleetMemberType.SHIP, "？？？",false);
		api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_DevilFork_Missile", FleetMemberType.SHIP, "？？？",false);

		//api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_Allusion_shieldbreaker_wing", FleetMemberType.FIGHTER_WING, false);


		// Set up the map.
		float width = 8000f;
		float height = 15000f;
		api.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f);
		api.setBackgroundSpriteName("data/missions/Omega_Deteriorate/DarkSky.jpg");

		float minX = -width / 2;
		float minY = -height / 2;

		for (int i = 0; i < 100; i++) {
			float x = (float) Math.random() * width - width/2;
			float y = (float) Math.random() * height - height/4;

			if (x > -1000 && x < 1500 ) continue;
			//&& y < -1000
			float radius = 200f + (float) Math.random() * 900f;
			api.addNebula(x, y, radius);
		}
		// Add an asteroid field
		api.addAsteroidField(minX, minY + height / 2f, 0f, 4000f, 5f, 50f, 50);
		api.addAsteroidField(minX + width * 0.3f, minY, 90, 3000f, 20f, 70f, 50);

		api.addObjective(minX + width * 0.8f - 1000, minY + height * 0.4f, "nav_buoy");
		api.addObjective(minX + width * 0.8f - 1000, minY + height * 0.6f, "nav_buoy");
		api.addObjective(minX + width * 0.3f + 1000, minY + height * 0.3f, "comm_relay");
		api.addObjective(minX + width * 0.3f + 1000, minY + height * 0.7f, "comm_relay");
		api.addObjective(minX + width * 0.5f, minY + height * 0.5f, "sensor_array");
		api.addObjective(minX + width * 0.2f + 1000, minY + height * 0.5f, "sensor_array");
	}
}