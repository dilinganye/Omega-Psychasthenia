package data.missions.IIRT_Omega_test_2;// Powered by Cat Magic

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

	static int missionType = 0; static Boolean isDevmode = false;

	@Override
	public void defineMission(MissionDefinitionAPI api) {

		// Set up the fleets so we can add ships and fighter wings to them.
		// In this scenario, the fleets are attacking each other, but
		// in other scenarios, a fleet may be defending or trying to escape
		api.initFleet(FleetSide.PLAYER, "Omega", FleetGoal.ATTACK, false);
		api.initFleet(FleetSide.ENEMY, "ISS", FleetGoal.ATTACK, true);

		if(Global.getSettings().isDevMode()){
			isDevmode = true;
		}
		// Set a small blurb for each fleet that shows up on the mission detail and
		// mission results screens to identify each side.

		api.setFleetTagline(FleetSide.PLAYER, "未知");
		api.setFleetTagline(FleetSide.ENEMY, "未知");

		Global.getSoundPlayer().pauseCustomMusic();

		// These show up as items in the bulleted list under
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("测试");

		// Set up the player's fleet.
		FleetSide DevSide = FleetSide.ENEMY;
		if(isDevmode){
			DevSide = FleetSide.PLAYER;
		}


		switch (missionType) { // 正式版会只启用克劳斯姆的展示
			case 0:
				api.addToFleet(DevSide, "IIRT_Omega_Bit_Only", FleetMemberType.SHIP, "字节", true);
				api.addToFleet(DevSide, "IIRT_Omega_Bit_Only", FleetMemberType.SHIP, "字节", true);
				api.addToFleet(DevSide, "IIRT_Omega_Kb_Only", FleetMemberType.SHIP, "字节", true);
				api.addToFleet(DevSide, "IIRT_Omega_Arrow_Only", FleetMemberType.SHIP, "箭头", true);
				api.addToFleet(DevSide, "IIRT_Omega_Arrow_Only_2", FleetMemberType.SHIP, "箭头", true);
				api.addToFleet(DevSide, "IIRT_Omega_Arrow_Only_3", FleetMemberType.SHIP, "箭头", true);
				api.addToFleet(DevSide, "IIRT_Omega_Arrow_Only_4", FleetMemberType.SHIP, "箭头", true);
				api.addToFleet(DevSide, "IIRT_Omega_Arrow_Only_5", FleetMemberType.SHIP, "箭头", true);
				api.addToFleet(DevSide, "IIRT_Omega_Arrow_Only_6", FleetMemberType.SHIP, "箭头", true);
				api.addToFleet(DevSide, "IIRT_Omega_Arrow_Only_7", FleetMemberType.SHIP, "箭头", true);
				api.addToFleet(DevSide, "IIRT_Omega_Crack_Only", FleetMemberType.SHIP, "瑕疵", true);
				api.addToFleet(DevSide, "IIRT_Omega_Crack_Only_2", FleetMemberType.SHIP, "瑕疵", true);
				api.addToFleet(DevSide, "IIRT_Omega_Crack_Only_3", FleetMemberType.SHIP, "瑕疵", true);
				api.addToFleet(DevSide, "IIRT_Omega_Crack_Only2", FleetMemberType.SHIP, "瑕疵", true);
				api.addToFleet(DevSide, "IIRT_Omega_Cube_Shock", FleetMemberType.SHIP, "超晶格", true);
				api.addToFleet(DevSide, "IIRT_Omega_Cube_Shock_Plus", FleetMemberType.SHIP, "超晶格", true);
				api.addToFleet(DevSide, "IIRT_Omega_Cube_Lazer_Plus", FleetMemberType.SHIP, "超晶格", true);
				api.addToFleet(DevSide, "IIRT_Omega_Inner_Normal_1", FleetMemberType.SHIP, "内形", true);

				api.addToFleet(DevSide, "IIRT_Omega_Proxy_Only", FleetMemberType.SHIP, "代理", true);
				api.addToFleet(DevSide, "IIRT_Omega_Proxy_Only2", FleetMemberType.SHIP, "代理", true);
				api.addToFleet(DevSide, "IIRT_Omega_Proxy_Only3", FleetMemberType.SHIP, "代理", true);
				api.addToFleet(DevSide, "IIRT_Omega_Proxy_Only4", FleetMemberType.SHIP, "代理", true);
				api.addToFleet(DevSide, "IIRT_Omega_Gateway_Only", FleetMemberType.SHIP, "网关", true);
				api.addToFleet(DevSide, "IIRT_Omega_Gateway_Only2", FleetMemberType.SHIP, "网关", true);
				api.addToFleet(DevSide, "IIRT_Omega_Gateway_Only3", FleetMemberType.SHIP, "网关", true);
				api.addToFleet(DevSide, "IIRT_Omega_Gateway_Only4", FleetMemberType.SHIP, "网关", true);
				api.addToFleet(DevSide, "IIRT_Omega_Gateway_Only5", FleetMemberType.SHIP, "网关", true);

				api.addToFleet(DevSide, "IIRT_Omega_AttackChain_Assault", FleetMemberType.SHIP, "攻击链", true);
				api.addToFleet(DevSide, "IIRT_Omega_AttackChain_AntiShield", FleetMemberType.SHIP, "攻击链", true);
				api.addToFleet(DevSide, "IIRT_Omega_AttackChain_AntiArmor", FleetMemberType.SHIP, "攻击链", true);
				api.addToFleet(DevSide, "IIRT_Omega_AttackChain_Missile", FleetMemberType.SHIP, "攻击链", true);
				api.addToFleet(DevSide, "IIRT_Omega_AttackChain_PD", FleetMemberType.SHIP, "攻击链", true);

				api.addToFleet(DevSide, "IIRT_Omega_Firewall_Only", FleetMemberType.SHIP, "防火墙", true);
				api.addToFleet(DevSide, "IIRT_Omega_Firewall_Only_2", FleetMemberType.SHIP, "防火墙", true);
				api.addToFleet(DevSide, "IIRT_Omega_Firewall_Only_3", FleetMemberType.SHIP, "防火墙", true);
				api.addToFleet(DevSide, "IIRT_Omega_Firewall_Only_4", FleetMemberType.SHIP, "防火墙", true);
				api.addToFleet(DevSide, "IIRT_Omega_Firewall_Only_5", FleetMemberType.SHIP, "防火墙", true);
				api.addToFleet(DevSide, "IIRT_Omega_Antitrack_Only", FleetMemberType.SHIP, "反跟踪", true);
				api.addToFleet(DevSide, "IIRT_Omega_Antitrack_Only_2", FleetMemberType.SHIP, "反跟踪", true);
				api.addToFleet(DevSide, "IIRT_Omega_Antitrack_Only_3", FleetMemberType.SHIP, "反跟踪", true);
				api.addToFleet(DevSide, "IIRT_Omega_Antitrack_Only_4", FleetMemberType.SHIP, "反跟踪", true);
				api.addToFleet(DevSide, "IIRT_Omega_Antitrack_Only2", FleetMemberType.SHIP, "反跟踪", true);
				api.addToFleet(DevSide, "IIRT_Omega_Watcher_Support", FleetMemberType.SHIP, "节点", true);
				api.addToFleet(DevSide, "IIRT_Omega_Watcher_Support", FleetMemberType.SHIP, "节点", true);
				api.addToFleet(DevSide, "IIRT_Omega_Watcher_Support", FleetMemberType.SHIP, "节点", true);
				api.addToFleet(DevSide, "IIRT_Omega_Watcher_Support", FleetMemberType.SHIP, "节点", true);

				api.addToFleet(DevSide, "IIRT_Omega_Sinus_Only", FleetMemberType.SHIP, "正弦", true);
				api.addToFleet(DevSide, "IIRT_Omega_Sinus_Only_2", FleetMemberType.SHIP, "正弦", true);
				api.addToFleet(DevSide, "IIRT_Omega_Sinus_Only_3", FleetMemberType.SHIP, "正弦", true);
				api.addToFleet(DevSide, "IIRT_Omega_Sinus_Only_4", FleetMemberType.SHIP, "正弦", true);
				api.addToFleet(DevSide, "IIRT_Omega_Sinus_Only_5", FleetMemberType.SHIP, "正弦", true);
				api.addToFleet(DevSide, "IIRT_Omega_Cosinus_Only", FleetMemberType.SHIP, "余弦", true);
				api.addToFleet(DevSide, "IIRT_Omega_Cosinus_Only_2", FleetMemberType.SHIP, "余弦", true);
				api.addToFleet(DevSide, "IIRT_Omega_Cosinus_Only_3", FleetMemberType.SHIP, "余弦", true);
				api.addToFleet(DevSide, "IIRT_Omega_Cosinus_Only_4", FleetMemberType.SHIP, "余弦", true);
				api.addToFleet(DevSide, "IIRT_Omega_Tangento_Only", FleetMemberType.SHIP, "正切", true);
				api.addToFleet(DevSide, "IIRT_Omega_Tangento_Only2", FleetMemberType.SHIP, "正切", true);


				api.addToFleet(DevSide, "shard_left_Attack", FleetMemberType.SHIP, "原版", true);
				api.addToFleet(DevSide, "shard_right_Attack", FleetMemberType.SHIP, "原版", true);


				api.addToFleet(DevSide, "IIRT_Omega_Torsion_2_Normal", FleetMemberType.SHIP, "扭矩", true);
				api.addToFleet(DevSide, "IIRT_Omega_Torsion_2_Normal_2", FleetMemberType.SHIP, "扭矩", true);
				api.addToFleet(DevSide, "IIRT_Omega_Torsion_2_Normal_3", FleetMemberType.SHIP, "扭矩", true);
				api.addToFleet(DevSide, "IIRT_Omega_Torsion_2_Normal_4", FleetMemberType.SHIP, "扭矩", true);
				api.addToFleet(DevSide, "IIRT_Omega_Torsion_2_Normal_5", FleetMemberType.SHIP, "扭矩", true);
				api.addToFleet(DevSide, "IIRT_Omega_Tranquil_Normal_1", FleetMemberType.SHIP, "宁静", true);
				api.addToFleet(DevSide, "IIRT_Omega_Tranquil_Normal_2", FleetMemberType.SHIP, "宁静", true);
				api.addToFleet(DevSide, "IIRT_Omega_Tranquil_Normal_3", FleetMemberType.SHIP, "宁静", true);
				api.addToFleet(DevSide, "IIRT_Omega_Tranquil_Normal_4", FleetMemberType.SHIP, "宁静", true);
				api.addToFleet(DevSide, "IIRT_Omega_Riots_Normal_1", FleetMemberType.SHIP, "缭乱", true);
				api.addToFleet(DevSide, "IIRT_Omega_Riots_Normal_2", FleetMemberType.SHIP, "缭乱", true);
				api.addToFleet(DevSide, "IIRT_Omega_Riots_Normal_3", FleetMemberType.SHIP, "缭乱", true);
				api.addToFleet(DevSide, "IIRT_Omega_Riots_Normal_4", FleetMemberType.SHIP, "缭乱", true);
				api.addToFleet(DevSide, "IIRT_Omega_Riots_Normal_5", FleetMemberType.SHIP, "缭乱", true);
				api.addToFleet(DevSide, "IIRT_Omega_Riots_Normal_6", FleetMemberType.SHIP, "缭乱", true);
				api.addToFleet(DevSide, "IIRT_Omega_Bustle_Normal_1", FleetMemberType.SHIP, "喧嚣", true);
				api.addToFleet(DevSide, "IIRT_Omega_Bustle_Normal_2", FleetMemberType.SHIP, "喧嚣", true);
				api.addToFleet(DevSide, "IIRT_Omega_Bustle_Normal_3", FleetMemberType.SHIP, "喧嚣", true);
				api.addToFleet(DevSide, "IIRT_Omega_Bustle_Normal_4", FleetMemberType.SHIP, "喧嚣", true);
				api.addToFleet(DevSide, "IIRT_Omega_Bustle_Normal_5", FleetMemberType.SHIP, "喧嚣", true);
				api.addToFleet(DevSide, "IIRT_Omega_Bustle_Normal_6", FleetMemberType.SHIP, "喧嚣", true);
				api.addToFleet(DevSide, "IIRT_Omega_Deplorable_Normal_1", FleetMemberType.SHIP, "可叹", true);
				api.addToFleet(DevSide, "IIRT_Omega_Deplorable_Normal_2", FleetMemberType.SHIP, "可叹", true);
				api.addToFleet(DevSide, "IIRT_Omega_Deplorable_Normal_3", FleetMemberType.SHIP, "可叹", true);
				api.addToFleet(DevSide, "IIRT_Omega_Deplorable_Normal_4", FleetMemberType.SHIP, "可叹", true);
				api.addToFleet(DevSide, "IIRT_Omega_Deplorable_Normal_5", FleetMemberType.SHIP, "可叹", true);

				api.addToFleet(DevSide, "IIRT_Omega_DevilFork_Attack", FleetMemberType.SHIP, "音叉", true);
				api.addToFleet(DevSide, "IIRT_Omega_DevilFork_Missile", FleetMemberType.SHIP, "音叉", true);
				api.addToFleet(DevSide, "IIRT_Omega_DevilFork_AntiShield", FleetMemberType.SHIP, "音叉", true);
				api.addToFleet(DevSide, "IIRT_Omega_DevilFork_AntiArmor", FleetMemberType.SHIP, "音叉", true);
				api.addToFleet(DevSide, "IIRT_Omega_DevilFork_Support", FleetMemberType.SHIP, "音叉", true);


				api.addToFleet(DevSide, "IIRT_Omega_Inspect_Normal_4", FleetMemberType.SHIP, "侦察", true);

				api.addToFleet(DevSide, "IIRT_Omega_RedShift_Boomer", FleetMemberType.SHIP, "红移", true);

				api.addToFleet(DevSide, "IIRT_Omega_Boltzmann_Assault", FleetMemberType.SHIP, "玻尔兹曼", true);
				api.addToFleet(DevSide, "IIRT_Omega_Boltzmann_Missile", FleetMemberType.SHIP, "玻尔兹曼", true);
				api.addToFleet(DevSide, "IIRT_Omega_Boltzmann_AntiShield", FleetMemberType.SHIP, "玻尔兹曼", true);
				api.addToFleet(DevSide, "IIRT_Omega_Boltzmann_Point_Defense", FleetMemberType.SHIP, "玻尔兹曼", true);
				api.addToFleet(DevSide, "IIRT_Omega_Boltzmann_Anti_Armor", FleetMemberType.SHIP, "玻尔兹曼", true);


				api.addToFleet(DevSide, "IIRT_Omega_Heatdeath_Normal", FleetMemberType.SHIP, "热寂", true);
				api.addToFleet(DevSide, "IIRT_Omega_Heatdeath_Normal_Plus", FleetMemberType.SHIP, "热寂", true);
				api.addToFleet(DevSide, "IIRT_Omega_Heatdeath_Missile", FleetMemberType.SHIP, "热寂", true);
				api.addToFleet(DevSide, "IIRT_Omega_Heatdeath_AntiArmor", FleetMemberType.SHIP, "热寂", true);
				api.addToFleet(DevSide, "IIRT_Omega_Heatdeath_Support", FleetMemberType.SHIP, "热寂", true);

				api.addToFleet(DevSide, "IIRT_Omega_Singularity_Attack", FleetMemberType.SHIP, "奇点", true);
				api.addToFleet(DevSide, "IIRT_Omega_Singularity_Attack_Plus", FleetMemberType.SHIP, "奇点", true);
				api.addToFleet(DevSide, "IIRT_Omega_Singularity_Missile", FleetMemberType.SHIP, "奇点", true);
				api.addToFleet(DevSide, "IIRT_Omega_Singularity_Support", FleetMemberType.SHIP, "奇点", true);
				api.addToFleet(DevSide, "IIRT_Omega_Singularity_AntiShield", FleetMemberType.SHIP, "奇点", true);
				api.addToFleet(DevSide, "IIRT_Omega_Singularity_AntiArmor", FleetMemberType.SHIP, "奇点", true);

				//api.addToFleet(DevSide, "IIRT_Omega01_Only", FleetMemberType.SHIP, "无形", true);

				api.addToFleet(DevSide, "Omega_EPP_Lazer", FleetMemberType.SHIP, "终端防护平台", true);
				api.addToFleet(DevSide, "PTSD_Omega_MacroVirus_Assault", FleetMemberType.SHIP, "宏病毒", true);

				api.addToFleet(DevSide, "IIRT_Omega_Station_Small", FleetMemberType.SHIP, "? ? ?", true);
				api.addToFleet(DevSide, "IIRT_Omega_Station_Stable", FleetMemberType.SHIP, "? ? ?", true);
				api.addToFleet(DevSide, "IIRT_Omega_Station_Pulse", FleetMemberType.SHIP, "? ? ?", true);
				api.addToFleet(DevSide, "IIRT_Omega_Station_Medium", FleetMemberType.SHIP, "? ? ?", true);
				api.addToFleet(DevSide, "IIRT_Omega_Station_Common", FleetMemberType.SHIP, "? ? ?", true);
				api.addToFleet(DevSide, "Omega_Scrap_Heap_Garbage", FleetMemberType.SHIP, "要塞", true);

				api.addToFleet(DevSide, "IIRT_Omega_Station_move_Small", FleetMemberType.SHIP, "凝聚体网络", true);
				api.addToFleet(DevSide, "IIRT_Omega_Station_move_Medium", FleetMemberType.SHIP, "凝聚体网络", true);
				api.addToFleet(DevSide, "IIRT_Omega_Station_move_Common", FleetMemberType.SHIP, "凝聚体网络", true);
				api.addToFleet(DevSide, "IIRT_Omega_Station_move_Stable", FleetMemberType.SHIP, "凝聚体网络", true);

				api.addToFleet(DevSide, "IIRT_Omega_Bustle_D_Normal_1", FleetMemberType.SHIP, "? ? ?", false);
				api.addToFleet(DevSide, "IIRT_Omega_Bustle_D_Normal_2", FleetMemberType.SHIP, "? ? ?", false);
				api.addToFleet(DevSide, "IIRT_Omega_Bustle_D_Normal_3", FleetMemberType.SHIP, "? ? ?", false);
				api.addToFleet(DevSide, "IIRT_Omega_Bustle_D_Normal_4", FleetMemberType.SHIP, "? ? ?", false);
				api.addToFleet(DevSide, "IIRT_Omega_Bustle_D_Normal_5", FleetMemberType.SHIP, "? ? ?", false);
				api.addToFleet(DevSide, "IIRT_Omega_Bustle_D_Normal_6", FleetMemberType.SHIP, "? ? ?", false);

				api.addToFleet(DevSide, "IIRT_Omega_Biohigraphisto_History", FleetMemberType.SHIP, true);
				api.addToFleet(DevSide, "IIRT_Omega_Writor_Writor", FleetMemberType.SHIP, true);
				api.addToFleet(DevSide, "IIRT_Omega_Candelabrum_Candle", FleetMemberType.SHIP, true);
				api.addToFleet(DevSide, "IIRT_Omega_Candelabrum_Candle", FleetMemberType.SHIP, true);


				api.addToFleet(DevSide, "IIRT_Omega_Record_shieldbreaker_wing", FleetMemberType.FIGHTER_WING, false);
				api.addToFleet(DevSide, "IIRT_Omega_Record_attack_wing", FleetMemberType.FIGHTER_WING, false);
				api.addToFleet(DevSide, "IIRT_Omega_Record_missile_wing", FleetMemberType.FIGHTER_WING, false);

				api.addToFleet(DevSide, "IIRT_Omega_Allusion_shock_wing", FleetMemberType.FIGHTER_WING, false);
				api.addToFleet(DevSide, "IIRT_Omega_Allusion_shieldbreaker_wing", FleetMemberType.FIGHTER_WING, false);
				api.addToFleet(DevSide, "IIRT_Omega_Allusion_attack_wing", FleetMemberType.FIGHTER_WING, false);
				api.addToFleet(DevSide, "IIRT_Omega_Allusion_missile_wing", FleetMemberType.FIGHTER_WING, false);

				api.addToFleet(DevSide, "IIRT_Omega_Pupal_shieldbreaker_wing", FleetMemberType.FIGHTER_WING, false);
				api.addToFleet(DevSide, "IIRT_Omega_Pupal_attack_wing", FleetMemberType.FIGHTER_WING, false);
				api.addToFleet(DevSide, "IIRT_Omega_Pupal_missile_wing", FleetMemberType.FIGHTER_WING, false);
				api.addToFleet(DevSide, "IIRT_Omega_Pupal_point_wing", FleetMemberType.FIGHTER_WING, false);
				api.addToFleet(DevSide, "IIRT_Omega_Pupal_shock_wing", FleetMemberType.FIGHTER_WING, false);

				api.addToFleet(DevSide, "IIRT_Omega_Nihility_shieldbreaker_wing", FleetMemberType.FIGHTER_WING, false);
				api.addToFleet(DevSide, "IIRT_Omega_Nihility_attack_wing", FleetMemberType.FIGHTER_WING, false);
				api.addToFleet(DevSide, "IIRT_Omega_Nihility_missile_wing", FleetMemberType.FIGHTER_WING, false);
				api.addToFleet(DevSide, "IIRT_Omega_Nihility_shock_wing", FleetMemberType.FIGHTER_WING, false);

				if (isDevmode) {
					api.addToFleet(DevSide, "IIRT_Omega_Shrike_Assault", FleetMemberType.SHIP, true);
					api.addToFleet(DevSide, "IIRT_Omega_Spectrum_Assault", FleetMemberType.SHIP, "光谱蝶", true);
					break;
				}
				if (!isDevmode){
					api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Cipher_Antimatter", FleetMemberType.SHIP, "Cipher of the Moon", true);
				}
			case 1:

				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Pioneer_Assault", FleetMemberType.SHIP, "先驱", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Pioneer_Assault", FleetMemberType.SHIP, "先驱", true);

				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Pathfinder_Assault", FleetMemberType.SHIP, "寻路士", true);

				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Slander_Assault", FleetMemberType.SHIP, "中伤", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Slander_Assault", FleetMemberType.SHIP, "中伤", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Slander_Support", FleetMemberType.SHIP, "中伤", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Slander_Support", FleetMemberType.SHIP, "中伤", true);

				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Foil_Assault", FleetMemberType.SHIP, "挫败", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Foil_Support", FleetMemberType.SHIP, "挫败", true);

				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Lethe_Assault", FleetMemberType.SHIP, "忘却", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Lethe_Common", FleetMemberType.SHIP, "忘却", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Lethe_Tanker_Common", FleetMemberType.SHIP, "忘却-运输型", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Lethe_Tanker_Common", FleetMemberType.SHIP, "忘却-运输型", true);

				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Norm_Standard", FleetMemberType.SHIP, "准则", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Norm_Standard", FleetMemberType.SHIP, "准则", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Perplex_Assault", FleetMemberType.SHIP, "困扰", true);


				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Thought_Hunter", FleetMemberType.SHIP, "思绪", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Thought_Assault", FleetMemberType.SHIP, "思绪", true);


				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Coveye_Assault", FleetMemberType.SHIP, "障目", true);

				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Gallows_Assault", FleetMemberType.SHIP, "绞刑", true);

				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Pessimism_Assault", FleetMemberType.SHIP, "悲绪", true);


				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Diffuse_Assault", FleetMemberType.SHIP, "弥漫", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Diffuse_Attack", FleetMemberType.SHIP, "弥漫", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Diffuse_Support", FleetMemberType.SHIP, "弥漫", true);

				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Watchman_Attack", FleetMemberType.SHIP, "守望者", true);

				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Ferryman_Support", FleetMemberType.SHIP, "摆渡人", true);

				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Revenge_Assault", FleetMemberType.SHIP, "寻仇", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Revenge_Assault", FleetMemberType.SHIP, "寻仇", true);

				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Nonsense_Assault", FleetMemberType.SHIP, "荒谬", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Nonsense_Attack", FleetMemberType.SHIP, "荒谬", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Lopes_Assault", FleetMemberType.SHIP, "疾步", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Lopes_Attack", FleetMemberType.SHIP, "疾步", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_FarBore_Mine", FleetMemberType.SHIP, "远凿", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_FarBore_Assault", FleetMemberType.SHIP, "远凿", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_FarBore_Support", FleetMemberType.SHIP, "远凿", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_FarBore_Support_2", FleetMemberType.SHIP, "远凿", true);

				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Perished_Assault", FleetMemberType.SHIP, "泯灭", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Perished_Support", FleetMemberType.SHIP, "泯灭", true);

				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Misty_Support", FleetMemberType.SHIP, "阴霾", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Misty_Assault", FleetMemberType.SHIP, "阴霾", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_LongKnives_Assault", FleetMemberType.SHIP, "长刀", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Impudent_Assault", FleetMemberType.SHIP, "不恭", true);


				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_NightMare_Assault", FleetMemberType.SHIP, "梦魇", true);

				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Assignation_Assault", FleetMemberType.SHIP, "幽会", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Assignation_Attack", FleetMemberType.SHIP, "幽会", true);

				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Slod_Attack", FleetMemberType.SHIP, "归尘", true);

				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Mourning_Assault", FleetMemberType.SHIP, "哀恸", true);


				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Obscurity_Support", FleetMemberType.SHIP, "晦涩", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Obscurity_Cargo", FleetMemberType.SHIP, "晦涩", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Vague_Common_Assault", FleetMemberType.SHIP, "未详", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Vague_Assault", FleetMemberType.SHIP, "定制-未详", true);
				api.addToFleet(FleetSide.PLAYER, "IIRT_Omega_Cipher_Antimatter", FleetMemberType.SHIP, "密钥-月相", true);

				//This is a unfinish stuff, dont use it unless you dont know waht you are doing.

				//api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_station3_hightech_KRM_SomeTime", FleetMemberType.SHIP, false);

				if(isDevmode){
					api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Pioneer_Omega", FleetMemberType.SHIP, "实验-先驱", true);
					api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Pathfinder_Omega", FleetMemberType.SHIP, "实验-寻路士", true);
					api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Slander_Omega", FleetMemberType.SHIP, "实验-中伤", true);
					api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Slander_Omega_2", FleetMemberType.SHIP, "实验-中伤", true);
					api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Norm_Omega", FleetMemberType.SHIP, "实验-准则", true);
					api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Perplex_Omega", FleetMemberType.SHIP, "实验-困扰", true);
					api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Thought_Omega", FleetMemberType.SHIP, "实验-思绪", true);
					api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Nonsense_Omega", FleetMemberType.SHIP, "荒谬", true);
					api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Perished_Omega", FleetMemberType.SHIP, "实验-泯灭", true);
					api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Perished_Omega2", FleetMemberType.SHIP, "实验-泯灭", true);
					api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Misty_Omega", FleetMemberType.SHIP, "实验-阴霾", true);
					api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_LongKnives_Omega", FleetMemberType.SHIP, "实验-长刀", true);
					api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Impudent_Omega", FleetMemberType.SHIP, "实验-不恭", true);
					api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Assignation_Omega", FleetMemberType.SHIP, "实验-幽会", true);
					api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Assignation_Omega_2", FleetMemberType.SHIP, "实验-幽会", true);
					api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Vague_Common_Omega", FleetMemberType.SHIP, "实验-未详", true);
					api.addToFleet(FleetSide.PLAYER, "IIRT_Lab_Obscurity_Omega", FleetMemberType.SHIP, "实验-晦涩", true);

					api.addToFleet(FleetSide.PLAYER, "hermes_SF_Standard", FleetMemberType.SHIP, "顺丰速运的快递员", true);
					api.addToFleet(FleetSide.PLAYER, "hermes_TY_Standard", FleetMemberType.SHIP, "纸上的画", true);
					api.addToFleet(FleetSide.PLAYER, "hermes_ELM_Standard", FleetMemberType.SHIP, "您的饿了么骑手", true);
					api.addToFleet(FleetSide.PLAYER, "hermes_MT_Standard", FleetMemberType.SHIP, "您的外卖", true);
					api.addToFleet(FleetSide.PLAYER, "hermes_YZ_Standard", FleetMemberType.SHIP, "由中国邮政送来的录取通知书", true);
					api.addToFleet(FleetSide.PLAYER, "heaxu_Attack", FleetMemberType.SHIP, "可爱的仓叙", true);
					api.addToFleet(FleetSide.PLAYER, "IIRT_TDB_qiYa_Common", FleetMemberType.SHIP, "枚气压", true);
					api.addToFleet(FleetSide.PLAYER, "IIRT_FSF_Helico_Common", FleetMemberType.SHIP, "纸升机", true);

				}
				break;
			case 2:
				if(isDevmode){
					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Skirmisher_Assault", FleetMemberType.SHIP, true);
					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Skirmisher_Assault", FleetMemberType.SHIP, false);
					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Recursion_Support", FleetMemberType.SHIP, false);
					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Recursion_Support", FleetMemberType.SHIP, false);
					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_DownLight_Assault", FleetMemberType.SHIP, false);
					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_DownLight_Assault", FleetMemberType.SHIP, false);

					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Gap_Cargo", FleetMemberType.SHIP, false);
					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Gap_Cargo", FleetMemberType.SHIP, false);
					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Gap_Tanker_Tanker", FleetMemberType.SHIP, false);
					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Gap_Tanker_Tanker", FleetMemberType.SHIP, false);
					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Dim_Tanker", FleetMemberType.SHIP, false);
					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Dim_Tanker", FleetMemberType.SHIP, false);

					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Stroll_Support", FleetMemberType.SHIP, false);
					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Stroll_Support", FleetMemberType.SHIP, false);
					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Lens_Assault", FleetMemberType.SHIP, false);
					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Lens_Assault", FleetMemberType.SHIP, false);
					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Realm_Assault", FleetMemberType.SHIP, false);
					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Realm_Assault", FleetMemberType.SHIP, false);

					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Exordium_Support", FleetMemberType.SHIP, false);
					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Exordium_Support", FleetMemberType.SHIP, false);

					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Tide_Common", FleetMemberType.SHIP, false);
					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Tide_Common", FleetMemberType.SHIP, false);

					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Gaze_Common", FleetMemberType.SHIP, false);
					api.addToFleet(FleetSide.PLAYER, "IIRT_SD_Gaze_Common", FleetMemberType.SHIP, false);



					break;
				}

			case 3:

				api.addToFleet(FleetSide.PLAYER, "PTSD_Threat_Tiny_variant", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "PTSD_Threat_Tiny_variant", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "PTSD_Threat_Tiny_variant", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "PTSD_Threat_Tiny_variant", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "PTSD_Threat_Reconnaissance_Assault", FleetMemberType.SHIP, false); //Type270
				api.addToFleet(FleetSide.PLAYER, "PTSD_Threat_Reconnaissance_Assault", FleetMemberType.SHIP, false); //Type270
				api.addToFleet(FleetSide.PLAYER, "PTSD_Threat_Reverse_Assault", FleetMemberType.SHIP, true);

				api.addToFleet(FleetSide.PLAYER, "skirmish_unit_Type100", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "skirmish_unit_Type101", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "assault_unit_Type200", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "assault_unit_Type201", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "overseer_unit_Type250", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "standoff_unit_Type300", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "standoff_unit_Type301", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "standoff_unit_Type302", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "hive_unit_Type350", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "fabricator_unit_Type450", FleetMemberType.SHIP, false);
			default:
				break;
		}


		// Set up the enemy fleet.
		//api.addToFleet(FleetSide.ENEMY, "IIRT_Lab_station3_hightech_KRM_SomeTime", FleetMemberType.SHIP, true);
		api.addToFleet(FleetSide.ENEMY, "remnant_station2_Damaged", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "brilliant_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "brilliant_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "scintilla_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "scintilla_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "brilliant_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "scintilla_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "scintilla_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "scintilla_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "glimmer_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "glimmer_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "fulgent_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "fulgent_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "fulgent_Support", FleetMemberType.SHIP, false);


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
		missionType++;
		if (missionType > 3) {
			missionType = 0;}
	}
}