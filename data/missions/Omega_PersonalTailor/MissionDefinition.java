// Powered by Cat Magic

package data.missions.Omega_PersonalTailor;

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
		Global.getSoundPlayer().playCustomMusic(1,1,"music_KRM_theme",true);
		// Set up the fleets so we can add ships and fighter wings to them.
		api.initFleet(FleetSide.PLAYER, "ISS", FleetGoal.ATTACK, false, 3);
		api.initFleet(FleetSide.ENEMY, "TTDS", FleetGoal.ATTACK, true, 5);
		api.setFleetTagline(FleetSide.PLAYER, "博士的研究结晶");
		api.setFleetTagline(FleetSide.ENEMY, "刚被你打醒的余辉舰船");

		api.addBriefingItem("提示：你的舰船可以将光束伤害以及EMP伤害转换为幅能，过载后你会失去加成");
		api.addBriefingItem("你拥有约8秒的P空间稳定效果");

		// Set up the player's fleet.
		api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Cipher_Antimatter", FleetMemberType.SHIP, "Cipher of the Moon", true);
		//api.addToFleet(FleetSide.PLAYER, "wolf_Assault", FleetMemberType.SHIP, false);

		// Mark player flagship as essential
		api.defeatOnShipLoss("Cipher of the Moon");

		// Set up the enemy fleet.
		api.addToFleet(FleetSide.ENEMY, "fulgent_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "fulgent_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "scintilla_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "scintilla_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "scintilla_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "scintilla_Support", FleetMemberType.SHIP, false);

		api.addToFleet(FleetSide.ENEMY, "glimmer_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "glimmer_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "glimmer_Support", FleetMemberType.SHIP,false);
		api.addToFleet(FleetSide.ENEMY, "glimmer_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "glimmer_Support", FleetMemberType.SHIP,false);
		api.addToFleet(FleetSide.ENEMY, "lumen_Standard", FleetMemberType.SHIP,false);
		api.addToFleet(FleetSide.ENEMY, "lumen_Standard", FleetMemberType.SHIP,false);
		api.addToFleet(FleetSide.ENEMY, "lumen_Standard", FleetMemberType.SHIP,false);
		api.addToFleet(FleetSide.ENEMY, "lumen_Standard", FleetMemberType.SHIP,false);



		// Set up the map.
		float width = 8000f;
		float height = 10000f;
		api.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f);
		api.setBackgroundSpriteName("graphics/backgrounds/background5.jpg");

		float minX = -width / 2;
		float minY = -height / 2;

		for (int i = 0; i < 100; i++) {
			float x = (float) Math.random() * width - width/2;
			float y = (float) Math.random() * height - height/4;

			if (x > -1000 && x < 1500 ) continue;
			//&& y < -1000
			float radius = 200f + (float) Math.random() * 200f;
			api.addNebula(x, y, radius);
		}
		// Add an asteroid field
		api.addAsteroidField(minX + width * 0.3f, minY, 90, 3000f, 20f, 70f, 50);

	}
}