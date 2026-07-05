// Powered by Cat Magic

package data.missions.Omega_NoEnterPlease;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.combat.EscapeRevealPlugin;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

import java.awt.*;


public class MissionDefinition implements MissionDefinitionPlugin {


	static int missionType = 0;
	@Override
	public void defineMission(MissionDefinitionAPI api) {
		Global.getSoundPlayer().playCustomMusic(1,1,"music_KRM_encounter_hostile",true);
		switch (missionType) {
			case 0:
			// Set up the fleets so we can add ships and fighter wings to them.
				api.initFleet(FleetSide.PLAYER, "KRM", FleetGoal.ATTACK, false, 10);
				api.initFleet(FleetSide.ENEMY, "???", FleetGoal.ATTACK, true, 5);
				api.setFleetTagline(FleetSide.PLAYER, "Clotilde{Volf} & 克劳斯姆概念 巡逻队");
				api.setFleetTagline(FleetSide.ENEMY, "主动进攻的余辉舰船 & 未知目标");
				api.addBriefingItem("提示：点击切换任务 —目前[Andels 近地轨道]");
				api.addBriefingItem("相位船只无论如何都是相位船只，在正面冲突上你永远不占优势");
				api.addBriefingItem("不要拖延战斗，时间永远不在你这一边，别害死你自己");
				break;
			case 1:
				// Set up the fleets so we can add ships and fighter wings to them.
				api.initFleet(FleetSide.PLAYER, "KRM", FleetGoal.ATTACK, false, 6);
				api.initFleet(FleetSide.ENEMY, "???", FleetGoal.ATTACK, true, 6);
				api.setFleetTagline(FleetSide.PLAYER, "克劳斯姆概念 舰船测试小组");
				api.setFleetTagline(FleetSide.ENEMY, "主动进攻的余辉舰船 & 未知目标");
				api.addBriefingItem("提示：点击切换任务 —目前[脉冲星附近]");
				api.addBriefingItem("克劳斯姆概念的相位船只在下潜时会消除部分幅能，并且拥有一小段的相位稳定时间");
				api.addBriefingItem("你的舰船拥有一些独特的武器配置，试着好好利用它们");
				break;
			case 2:
				// Set up the fleets so we can add ships and fighter wings to them.
				api.initFleet(FleetSide.PLAYER, "KRM", FleetGoal.ATTACK, false, 15);
				api.initFleet(FleetSide.ENEMY, "???", FleetGoal.ATTACK, true, 10);
				api.setFleetTagline(FleetSide.PLAYER, "克劳斯姆概念 近地护卫舰队");
				api.setFleetTagline(FleetSide.ENEMY, "主动进攻的余辉舰船 & 大量能量反应的未知目标");
				api.addBriefingItem("提示：点击切换任务 —目前[Feds 近地轨道]");
				api.addBriefingItem("你的部队已经抵挡了数次进攻，而对面似乎铁了心要对你后面的星球做点啥");
				api.addBriefingItem("保护好太空港，消耗掉敌人的主力，并依靠相位舰船点杀敌人的脆弱目标");
				break;
			default:
				break;
		}

		// Set up the player's fleet.
		switch (missionType) {
			case 0: {
				//api.addToFleet(FleetSide.PLAYER, "doom_Strike", FleetMemberType.SHIP, "Edward", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Assignation_Attack", FleetMemberType.SHIP, "Test List - Core", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_LongKnives_Assault", FleetMemberType.SHIP, "The Night", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_LongKnives_Assault", FleetMemberType.SHIP, "With - NonCloseten", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Misty_Assault", FleetMemberType.SHIP, "Re-mote For Dream", false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Revenge_Assault", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Revenge_Assault", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Lethe_Assault", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Lethe_Common", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Foil_Support", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Slander_Assault", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Slander_Support", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "omen_PD", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Pioneer_Assault", FleetMemberType.SHIP, false);

				// Mark player flagship as essential
				api.defeatOnShipLoss("Test List - Core");
				//api.defeatOnShipLoss("The Night","Edward","Blade With Fake");
				break;
			}
			case 1: {
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Pathfinder_Omega", FleetMemberType.SHIP, "Gone with Non-seen", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_FarBore_Support_2", FleetMemberType.SHIP,true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Lopes_Assault", FleetMemberType.SHIP, "The List", false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Nonsense_Assault", FleetMemberType.SHIP, "Close for - pid", false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Nonsense_Attack", FleetMemberType.SHIP, "Con Sences", false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Slander_Support", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Slander_Omega_2", FleetMemberType.SHIP, "Blade With Fake", false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Foil_Support", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Foil_Support", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Lethe_Assault", FleetMemberType.SHIP, false);
				//api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Lethe_Tanker_Common", FleetMemberType.SHIP,false);
				//api.defeatOnShipLoss("Blade With Fake");
				break;
			}
			case 2: {
				api.addToFleet(FleetSide.PLAYER, "station3_hightech_Standard", FleetMemberType.SHIP, "近地要塞", false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Slod_Attack", FleetMemberType.SHIP,"The - Slod", true);
				api.addToFleet(FleetSide.PLAYER, "doom_Strike", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Assignation_Attack", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Misty_Assault", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Misty_Assault", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_LongKnives_Assault", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_LongKnives_Assault", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_LongKnives_Assault", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Vague_Common_Assault", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Lethe_Assault", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Lethe_Assault", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Revenge_Assault", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Foil_Support", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Foil_Support", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Foil_Support", FleetMemberType.SHIP, false);
				//api.defeatOnShipLoss("The - Slod");
				break;
			}
			default:
				break;
		}

		// Set up the enemy fleet.
		switch (missionType) {
			case 0: {
				api.addToFleet(FleetSide.ENEMY, "radiant_Standard", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.ENEMY, "brilliant_Standard", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.ENEMY, "brilliant_Standard", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.ENEMY, "fulgent_Support", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.ENEMY, "scintilla_Support", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.ENEMY, "glimmer_Assault", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.ENEMY, "glimmer_Assault", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.ENEMY, "fulgent_Support", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.ENEMY, "lumen_Standard", FleetMemberType.SHIP,false);

				api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_Inspect_Normal_4", FleetMemberType.SHIP,"未知目标",false);
				break;
			}
			case 1: {
				api.addToFleet(FleetSide.ENEMY, "brilliant_Standard", FleetMemberType.SHIP, false);
				//api.addToFleet(FleetSide.ENEMY, "fulgent_Support", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.ENEMY, "scintilla_Strike", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.ENEMY, "fulgent_Support", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.ENEMY, "glimmer_Support", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.ENEMY, "glimmer_Support", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.ENEMY, "lumen_Standard", FleetMemberType.SHIP,false);
				api.addToFleet(FleetSide.ENEMY, "lumen_Standard", FleetMemberType.SHIP,false);

				api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_Cosinus_Only_2", FleetMemberType.SHIP,"未知目标",false);
				break;
			}
			case 2: {
				//api.addToFleet(FleetSide.ENEMY, "radiant_Standard", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.ENEMY, "radiant_Standard", FleetMemberType.SHIP, false);
				//api.addToFleet(FleetSide.ENEMY, "brilliant_Standard", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.ENEMY, "scintilla_Strike", FleetMemberType.SHIP, false);
				//api.addToFleet(FleetSide.ENEMY, "glimmer_Assault", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.ENEMY, "fulgent_Support", FleetMemberType.SHIP, false);

				api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_Cube_Shock_Plus", FleetMemberType.SHIP,"未知目标",false);
				api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_Antitrack_Only_2", FleetMemberType.SHIP,"未知目标",false);
				//api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_Firewall_Only_2", FleetMemberType.SHIP,"未知目标",false);
				//api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_Torsion_2_Normal_3", FleetMemberType.SHIP,"未知目标",false);
				//api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_Riots_Normal_6", FleetMemberType.SHIP,"未知目标",false);
				api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_Deplorable_Normal_3", FleetMemberType.SHIP,"未知目标",false);
				//api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_Proxy_Only2", FleetMemberType.SHIP,"未知目标",false);
				api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_Proxy_Only3", FleetMemberType.SHIP,"未知目标",false);
				//api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_Gateway_Only3", FleetMemberType.SHIP,"未知目标",false);
				//api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_Gateway_Only2", FleetMemberType.SHIP,"未知目标",false);
				api.addToFleet(FleetSide.ENEMY, "IIRT_Omega_Gateway_Only4", FleetMemberType.SHIP,"未知目标",false);
				break;
			}
			default:
				break;
		}


		// Set up the map.
		float width = 8000f;
		float height = 10000f;
		int nebulas = 40;
		switch (missionType) {
			case 0:
				width = 8000f;
				height = 10000f;
				nebulas = 40;
				break;
			case 1:
				width = 8000f;
				height = 10000f;
				nebulas = 100;
				break;
			case 2:
				width = 15000f;
				height = 20000f;
				nebulas = 30;
				break;
			default:
				break;
		}
		api.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f);
		api.setBackgroundSpriteName("graphics/backgrounds/KRM_Aleph_Post.png");

		for (int i = 0; i < nebulas; i++) {
			float x = (float) Math.random() * width - width / 2;
			float y = (float) Math.random() * height - height / 2;
			float radius = 200f + (float) Math.random() * 900f;
			api.addNebula(x, y, radius);
		}

		float minX = -width / 2;
		float minY = -height / 2;


		switch (missionType) {
			case 0:
				// Add an asteroid field
				api.addAsteroidField(minX, minY + height / 2f, 0f, 4000f, 5f, 50f, 80);
				api.addAsteroidField(minX + width * 0.3f, minY, 90f, 3000f, 20f, 70f, 80);
				api.addPlanet(800f,-1000f,2750f,"cryovolcanic",-10f);
				api.setBackgroundGlowColor(Color.black);

				api.addObjective(minX + width * 0.8f - 1000, minY + height * 0.5f, "nav_buoy");
				api.addObjective(minX + width * 0.3f + 1000, minY + height * 0.3f, "comm_relay");
				api.addObjective(minX + width * 0.5f, minY + height * 0.7f, "sensor_array");
				break;
			case 1:
				api.addAsteroidField(minX, minY + height / 2f, 20f, 4000f, 5f, 50f, 100);
				api.addAsteroidField(minX + width * 0.3f, minY, 90f, 2000f, 20f, 70f, 30);
				api.addPlanet(1250f,2000f,1750f,"star_neutron",-10f);
				api.setHyperspaceMode(true);
				api.setBackgroundGlowColor(new Color(28, 29, 30, 187));

				api.addObjective(minX + width * 0.25f, minY + height * 0.25f, "nav_buoy");
				api.addObjective(minX + width * 0.75f, minY + height * 0.75f, "nav_buoy");
				api.addObjective(minX + width * 0.5f, minY + height * 0.5f, "sensor_array");
				break;
			case 2:
				api.addAsteroidField(minX, minY + height / 2f, 20f, 4000f, 5f, 50f, 20);
				api.addAsteroidField(minX + width * 0.3f, minY, 90f, 2000f, 20f, 70f, 70);
				api.addPlanet(0f,-10000f,3750f,"rocky_ice",-10f);
				api.setBackgroundGlowColor(Color.black);
				api.addObjective(minX + width * 0.25f, minY + height * 0.3f, "nav_buoy");
				//api.addObjective(minX + width * 0.5f, minY + height * 0.25f, "comm_relay");
				api.addObjective(minX + width * 0.75f, minY + height * 0.3f, "sensor_array");
				break;
			default:
				break;
		}
		missionType++;
		if (missionType > 2) {
			missionType = 0;
		}
	}
}