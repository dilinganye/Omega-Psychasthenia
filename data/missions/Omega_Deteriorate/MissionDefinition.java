// Powered by Cat Magic

package data.missions.Omega_Deteriorate;

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
		Global.getSoundPlayer().playCustomMusic(1,1,"music_KRM_encounter_neutral",true);
		// Set up the fleets so we can add ships and fighter wings to them.
		api.initFleet(FleetSide.PLAYER, "ISS", FleetGoal.ESCAPE, false, 5);
		api.initFleet(FleetSide.ENEMY, "???", FleetGoal.ATTACK, true);
		api.setFleetTagline(FleetSide.PLAYER, "Clotilde Volf 的相位走私队");
		api.setFleetTagline(FleetSide.ENEMY, "未知舰体群");

		api.addBriefingItem("提示：敌人为=热寂死界=系列，其非常善于近身搏斗");
		api.addBriefingItem("速战速决或是引走敌人，至少需要 25% 的走私舰船撤离战场");

		// Set up the player's fleet.
		api.addToFleet(FleetSide.PLAYER, "doom_Strike", FleetMemberType.SHIP, "Edward", true);
		api.addToFleet(FleetSide.PLAYER, "afflictor_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "shade_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "shade_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "shade_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "shade_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "shade_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "revenant_Elite", FleetMemberType.SHIP, false).getCaptain().setPersonality(Personalities.TIMID);
		api.addToFleet(FleetSide.PLAYER, "revenant_Elite", FleetMemberType.SHIP, false).getCaptain().setPersonality(Personalities.TIMID);
		api.addToFleet(FleetSide.PLAYER, "phantom_Elite", FleetMemberType.SHIP, false).getCaptain().setPersonality(Personalities.TIMID);

		// Mark player flagship as essential
		//api.defeatOnShipLoss("Edward");

		// Set up the enemy fleet.
		api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_Singularity_AntiShield", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_Heatdeath_Normal", FleetMemberType.SHIP, false);
		//api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_Heatdeath_Support", FleetMemberType.SHIP, false);
		//api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_Nihility_attack_wing", FleetMemberType.FIGHTER_WING, false);
		api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_Nihility_attack_wing", FleetMemberType.FIGHTER_WING, false);


		// Set up the map.
		float width = 9000f;
		float height = 15000f;
		api.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f);
		api.setBackgroundSpriteName("data/missions/Omega_Deteriorate/DarkSky.jpg");

		float minX = -width / 2;
		float minY = -height / 2;

		for (int i = 0; i < 300; i++) {
			float x = (float) Math.random() * width - width/2;
			float y = (float) Math.random() * height - height/4;

			if (x > -1000 && x < 1500 && y < -1000) continue;
			float radius = 200f + (float) Math.random() * 900f;
			api.addNebula(x, y, radius);
		}
		// Add an asteroid field
		api.addAsteroidField(minX, minY + height / 2f, 0f, 4000f, 5f, 50f, 50);
		//api.addPlanet(-500f, 500f, 5f, StarTypes.YELLOW, 50f, true);

		api.addNebula(-400, 2100, 200f);
		api.addObjective(minX + width * 0.25f, minY + 5500, "nav_buoy");
		api.addObjective(minX + width * 0.3f, minY + height * 0.75f, "comm_relay");
		api.addObjective(minX + width * 0.7f, minY + height * 0.7f, "nav_buoy");
	}
}