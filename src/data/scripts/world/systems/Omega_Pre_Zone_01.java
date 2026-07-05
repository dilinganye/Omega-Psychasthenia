package data.scripts.world.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.CoreLifecyclePluginImpl;
import com.fs.starfarer.api.impl.campaign.WarningBeaconEntityPlugin;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.themes.IIRT_OmegaStationFleetManager;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Random;

public class Omega_Pre_Zone_01 {

	public void generate(SectorAPI sector) {
		StarSystemAPI system = sector.createStarSystem("The Prevote Zone");
		system.addTag(Tags.THEME_HIDDEN);
		system.addTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);
		system.addTag(Tags.THEME_UNSAFE);

		system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "music_Omega_Pre01");

		LocationAPI hyper = Global.getSector().getHyperspace();
		//星系位置
		system.getLocation().set(-31400.0F, -11880.0F);
		//背景图片
		system.setBackgroundTextureFilename("graphics/backgrounds/Omega_Pre.png");

		//SectorEntityToken star = system.initNonStarCenter();
		//恒星（大小，半径，日冕大小

		SectorEntityToken star = system.initNonStarCenter();

		//让小行星带环绕它
		system.addAsteroidBelt(star, 120, 5000f, 400f, 180, 360, Terrain.ASTEROID_BELT, "");
		system.addAsteroidBelt(star, 150, 5000f, 400f, 180, 360, Terrain.RING, "");

		String type = "cryovolcanic";
		type = "irradiated";
		PlanetAPI rock = system.addPlanet("Omega_zone_pre01", star, "Beaylon", type, 0, 150, 5000, 120f);
		//rock.setCustomDescriptionId("???");
		rock.getMemoryWithoutUpdate().set("$ttBlackSite", true);

		rock.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
		rock.getMarket().addCondition(Conditions.VERY_COLD);
		rock.getMarket().addCondition(Conditions.DARK);
		rock.getMarket().addCondition(Conditions.IRRADIATED);
		rock.getMarket().addCondition(Conditions.ORE_ULTRARICH);
		rock.getMarket().addCondition(Conditions.RARE_ORE_ULTRARICH);
		rock.getMarket().addCondition(Conditions.RUINS_VAST);

		rock.getMarket().getMemoryWithoutUpdate().set("$ruinsExplored", true);

		CoreLifecyclePluginImpl.addRuinsJunk(rock);

		rock.setOrbit(null);
		rock.setLocation(1200, 300);

		CustomCampaignEntityAPI warningBB = system.addCustomEntity(null, null, Entities.WARNING_BEACON, Factions.NEUTRAL);
		warningBB.setLocation(1400, 700);
		warningBB.getMemoryWithoutUpdate().set(WarningBeaconEntityPlugin.PING_ID_KEY, Pings.WARNING_BEACON3);
		warningBB.getMemoryWithoutUpdate().set(WarningBeaconEntityPlugin.PING_FREQ_KEY, 1.5f);
		warningBB.getMemoryWithoutUpdate().set(WarningBeaconEntityPlugin.PING_COLOR_KEY, new Color(77, 170, 210, 255));
		warningBB.getMemoryWithoutUpdate().set(WarningBeaconEntityPlugin.GLOW_COLOR_KEY, new Color(255, 205, 205, 255));

		SectorEntityToken STR = system.addCustomEntity("KRM_STR", "善意之礼", "station_side04", "neutral");
		STR.setCircularOrbitPointingDown(warningBB, 75, 350, 50);
		STR.setCustomDescriptionId("KRM_Sci_platform");
		STR.setInteractionImage("illustrations", "KRM_Platform");
		Misc.setAbandonedStationMarket("corvus_abandoned_station_market", STR);

		STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addFuel(5000f);
		STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addSupplies(5000f);
		STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addWeapons("thermosiphonsrm", 4);
		STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addWeapons("Omega_Vpdiver_S", 2);
		STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addMothballedShip(FleetMemberType.SHIP, "IIRT_Omega_Bit_Only", "恭喜你找到这里，这是送给你的吉祥物");
		STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addMothballedShip(FleetMemberType.SHIP, "IIRT_Omega_Kb_Only", "另一个吉祥物");
		STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addMothballedShip(FleetMemberType.SHIP, "IIRT_TDB_qiYa_Common", "和一个势力的联动，他只是在这里躲一会儿");

		SectorEntityToken Omega_pre_nebula = Misc.addNebulaFromPNG("graphics/backgrounds/Omega_Pre_nebula.png", // png
				0, 0, // center of nebula
				system, // location to add to
				"terrain", "nebula_blue", // "nebula_blue", // texture to use, uses xxx_map for map
				4, 4, StarAge.OLD); // number of cells in texture
        /*
          一号行星————————————————————————————————————————————————————————————————————————
         */
		PlanetAPI planet1 = system.addPlanet("Omega_pre_planet1", //行星ID
				star, //恒星ID
				"Gisd09ui", //星球名字
				type, //类型
				0, 400f, 3200f, 122f);
		//行星环
		system.addAsteroidBelt(star, 150, 3200f, 180f, 180, 360, Terrain.RING, "");

		planet1.getSpec().setGlowColor(new Color(126, 126, 126));
		planet1.getSpec().setUseReverseLightForGlow(true);
		planet1.getSpec().setCloudColor(new Color(147, 202, 255, 150));
		planet1.getMemoryWithoutUpdate().set("$ttBlackSite", true);

		planet1.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
		planet1.getMarket().addCondition(Conditions.VERY_COLD);
		planet1.getMarket().addCondition(Conditions.DARK);
		planet1.getMarket().addCondition(Conditions.IRRADIATED);
		planet1.getMarket().addCondition(Conditions.ORE_ULTRARICH);
		planet1.getMarket().addCondition(Conditions.RARE_ORE_ABUNDANT);
		planet1.getMarket().addCondition(Conditions.RUINS_WIDESPREAD);

		planet1.getMarket().getMemoryWithoutUpdate().set("$ruinsExplored", true);

		planet1.setOrbit(null);
		planet1.setLocation(-2100, 800);
		CoreLifecyclePluginImpl.addRuinsJunk(planet1);

        /*
        //为星系生成指定跳跃点
        JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint("inside_point", "在解决蠢笨的势力争霸乱开战导致游戏爆炸之前只能非常不愉悦的开出这么一个非常讨厌的门真的很不开心所以这个门正对着黑洞不要进去的 Aleph 跳跃点");
        OrbitAPI orbit = Global.getFactory().createCircularOrbit(planet1, 0, 4000, 30);
        jumpPoint.setOrbit(orbit);
        jumpPoint.setRelatedPlanet(planet1);
        jumpPoint.setStandardWormholeToHyperspaceVisual();
        system.addEntity(jumpPoint);
        //扫描本星系所有跳跃点并为之配置数据
        system.autogenerateHyperspaceJumpPoints(true, false);
        */
		PlanetAPI planet2 = system.addPlanet("Omega_pre_planet2", //行星ID
				star, //恒星ID
				"A3las0c7sn", //星球名字
				type, //类型
				0, 650f, 5200f, 10f);
		planet2.getSpec().setGlowColor(new Color(35, 36, 121));
		planet2.getSpec().setUseReverseLightForGlow(true);
		planet2.getSpec().setCloudColor(new Color(156, 176, 95, 150));
		planet2.setOrbit(null);
		planet2.setLocation(5800, 2800);
		planet2.getMemoryWithoutUpdate().set("$ttBlackSite", true);

		planet2.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
		planet2.getMarket().addCondition(Conditions.VERY_COLD);
		planet2.getMarket().addCondition(Conditions.DARK);
		planet2.getMarket().addCondition(Conditions.IRRADIATED);
		planet2.getMarket().addCondition(Conditions.ORE_ULTRARICH);
		planet2.getMarket().addCondition(Conditions.RARE_ORE_ABUNDANT);
		planet2.getMarket().addCondition(Conditions.RUINS_WIDESPREAD);

		planet2.getMarket().getMemoryWithoutUpdate().set("$ruinsExplored", true);

		PlanetAPI planet3 = system.addPlanet("Omega_pre_planet3", //行星ID
				star, //恒星ID
				"C8GS0sT013J", //星球名字
				type, //类型
				0, 1080f, 8200f, 250f);
		planet3.getSpec().setGlowColor(new Color(136, 132, 248));
		planet3.getSpec().setUseReverseLightForGlow(true);
		planet3.getSpec().setCloudColor(new Color(147, 255, 172, 150));
		planet3.setOrbit(null);
		planet3.setLocation(-6500, 4000);
		planet3.getMemoryWithoutUpdate().set("$ttBlackSite", true);

		planet3.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
		planet3.getMarket().addCondition(Conditions.VERY_COLD);
		planet3.getMarket().addCondition(Conditions.DARK);
		planet3.getMarket().addCondition(Conditions.IRRADIATED);
		planet3.getMarket().addCondition(Conditions.ORE_ULTRARICH);
		planet3.getMarket().addCondition(Conditions.RARE_ORE_ABUNDANT);
		planet3.getMarket().addCondition(Conditions.RUINS_WIDESPREAD);

		planet3.getMarket().getMemoryWithoutUpdate().set("$ruinsExplored", true);

		PlanetAPI planet4 = system.addPlanet("Omega_pre_planet4", //行星ID
				star, //恒星ID
				"Csiu2k1IsP", //星球名字
				type, //类型
				0, 1580f, 4200f, 192f);
		planet4.getSpec().setGlowColor(new Color(136, 132, 248));
		planet4.getSpec().setUseReverseLightForGlow(true);
		planet4.getSpec().setCloudColor(new Color(147, 255, 172, 150));
		planet4.setOrbit(null);
		planet4.setLocation(9500, -6000);
		planet4.getMemoryWithoutUpdate().set("$ttBlackSite", true);
		planet4.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
		planet4.getMarket().addCondition(Conditions.VERY_COLD);
		planet4.getMarket().addCondition(Conditions.DARK);
		planet4.getMarket().addCondition(Conditions.IRRADIATED);
		planet4.getMarket().addCondition(Conditions.ORE_ULTRARICH);
		planet4.getMarket().addCondition(Conditions.RARE_ORE_ABUNDANT);
		planet4.getMarket().addCondition(Conditions.RUINS_WIDESPREAD);

		planet4.getMarket().getMemoryWithoutUpdate().set("$ruinsExplored", true);

		system.generateAnchorIfNeeded();

		//生成不稳定点
		NascentGravityWellAPI well = Global.getSector().createNascentGravityWell(warningBB, 200f);
		well.addTag(Tags.NO_ENTITY_TOOLTIP);
		well.setColorOverride(new Color(183, 204, 229));
		hyper.addEntity(well);
		well.autoUpdateHyperLocationBasedOnInSystemEntityAtRadius(warningBB, 0);

		//在超空间清出一些区域
		HyperspaceTerrainPlugin hyperTerrain = (HyperspaceTerrainPlugin)Misc.getHyperspaceTerrain().getPlugin();
		NebulaEditor editor = new NebulaEditor(hyperTerrain);
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, 200, 0, 360f);

		//SectorEntityToken SPC1 = system.addCustomEntity("KRM_SPC1", "一处补给藏匿点", "supply_cache_KRM", Factions.NEUTRAL);
		//SPC1.setCircularOrbit(star, 180f, 3000f, 400f);

		//设置你星系的永久稳定点建筑
		SectorEntityToken A = system.addCustomEntity("Omega_A", "通讯稳定信道设施", "comm_relay", "KRM");
		A.setCircularOrbit(star, 180f, 2900f, 365f);
		SectorEntityToken C = system.addCustomEntity("Omega_A", "隐秘波段探测阵列", "sensor_array", "KRM");
		C.setCircularOrbit(star, 240f, 0f, 365f);

		this.addFleetOmega_Psychasthenia50(rock);
		this.addFleetOmega_Psychasthenia150(planet1);
		this.addFleetOmega_Psychasthenia240(planet2);
		this.addFleetOmega_PsychastheniaZONE(planet4);
		this.addFleetOmega_PsychastheniaZONE(planet4);

		this.addFleetOmega_Watcher45(rock);
		this.addFleetOmega_Watcher45(rock);
		this.addFleetOmega_Watcher60(planet3);
		this.addFleetOmega_Watcher60(planet3);
		this.addFleetOmega_Watcher60(planet3);
		this.addFleetOmega_Watcher85(planet1);
		this.addFleetOmega_Watcher85(planet1);
		this.addFleetOmega_Watcher120(star);
		this.addFleetOmega_HeatDeath_boundary(planet3);
		this.addFleetOmega_HeatDeath_boundary(planet3);
		this.addFleetOmega_HeatDeath_boundary(planet3);
	}

	public void addFleetOmega_Watcher45(SectorEntityToken rock) {
		// 向舰队中添加成员—————虚影—————RANDOM
		FleetParamsV3 params = new FleetParamsV3(null, "Omega_Watcher", 1f, "TASK_FORCE", 45f, 0f, 0f, 0f, 0f, 0f, 0f);
		params.ignoreMarketFleetSizeMult = true;

		CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
		fleet.setName("虚影-45");
		fleet.setNoFactionInName(false);
		// 设置舰队属性，使其具有敌对性和攻击性
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
		fleet.removeAbility("emergency_burn");
		fleet.removeAbility("sensor_burst");
		fleet.removeAbility("go_dark");
		fleet.getMemoryWithoutUpdate().set("$sawPlayerTransponderOn", true);
		fleet.getMemoryWithoutUpdate().set("$isPatrol", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_longPursuit", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_holdVsStronger", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_noJump", true);
		// 设置舰队属性，使其不产生重复影响和低影响(?)
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, true);
		// 设置舰队属性，使其不进行船只回收和始终追击目标
		//fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true); // so it keeps transponder on
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, false);
		fleet.setId("Omega_Unsure_Watcher45");
		// 激活舰队的传讯器能力
		fleet.getAbility(Abilities.TRANSPONDER).activate();

		// 随机设置舰队的位置并将其添加到包含该位置的实体中
		Vector2f loc = new Vector2f(rock.getLocation().x + 8000 * ((float)Math.random() - 0.5f), rock.getLocation().y + 8000 * ((float)Math.random() - 0.5f));
		fleet.setLocation(loc.x, loc.y);
		rock.getContainingLocation().addEntity(fleet);
	}

	public void addFleetOmega_Watcher60(SectorEntityToken planet3) {
		// 向舰队中添加成员—————虚影—————RANDOM
		FleetParamsV3 params = new FleetParamsV3(null, "Omega_Watcher", 1f, "TASK_FORCE", 60f, 0f, 0f, 0f, 0f, 0f, 0f);
		params.ignoreMarketFleetSizeMult = true;

		CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
		fleet.setName("虚影-60");
		fleet.setNoFactionInName(false);
		// 设置舰队属性，使其具有敌对性和攻击性
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
		fleet.removeAbility("emergency_burn");
		fleet.removeAbility("sensor_burst");
		fleet.removeAbility("go_dark");
		fleet.getMemoryWithoutUpdate().set("$sawPlayerTransponderOn", true);
		fleet.getMemoryWithoutUpdate().set("$isPatrol", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_longPursuit", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_holdVsStronger", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_noJump", true);
		// 设置舰队属性，使其不产生重复影响和低影响(?)
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, true);
		// 设置舰队属性，使其不进行船只回收和始终追击目标
		//fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true); // so it keeps transponder on
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, false);
		fleet.setId("Omega_Unsure_Watcher60");
		// 激活舰队的传讯器能力
		fleet.getAbility(Abilities.TRANSPONDER).activate();

		// 随机设置舰队的位置并将其添加到包含该位置的实体中
		Vector2f loc = new Vector2f(planet3.getLocation().x + 8000 * ((float)Math.random() - 0.5f), planet3.getLocation().y + 8000 * ((float)Math.random() - 0.5f));
		fleet.setLocation(loc.x, loc.y);
		planet3.getContainingLocation().addEntity(fleet);
	}

	public void addFleetOmega_Watcher85(SectorEntityToken planet1) {
		// 向舰队中添加成员—————幻象—————RANDOM
		FleetParamsV3 params85 = new FleetParamsV3(null, "Omega_Watcher", 1.25f, "TASK_FORCE", 80f, 0f, 0f, 0f, 0f, 0f, 0f);
		params85.ignoreMarketFleetSizeMult = true;

		CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params85);
		fleet.setName("幻象-80");
		// 设置舰队属性，使其具有敌对性和攻击性
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
		fleet.removeAbility("emergency_burn");
		fleet.removeAbility("sensor_burst");
		fleet.removeAbility("go_dark");
		fleet.getMemoryWithoutUpdate().set("$sawPlayerTransponderOn", true);
		fleet.getMemoryWithoutUpdate().set("$isPatrol", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_longPursuit", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_holdVsStronger", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_noJump", true);
		// 设置舰队属性，使其不产生重复影响和低影响(?)
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, true);
		// 设置舰队属性，使其不进行船只回收和始终追击目标
		//fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true); // so it keeps transponder on
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, false);
		fleet.setId("Omega_Unsure_Watcher85");
		// 激活舰队的传讯器能力
		fleet.getAbility(Abilities.TRANSPONDER).activate();

		// 随机设置舰队的位置并将其添加到包含该位置的实体中
		Vector2f loc = new Vector2f(planet1.getLocation().x + 8000 * ((float)Math.random() - 0.5f), planet1.getLocation().y + 8000 * ((float)Math.random() - 0.5f));
		fleet.setLocation(loc.x, loc.y);
		planet1.getContainingLocation().addEntity(fleet);

		// 为舰队添加脚本以处理牵引分配AI任务
		//fleet.addScript(new IIRT_AssignmentAI(fleet, planet1));
	}

	public void addFleetOmega_Watcher120(SectorEntityToken star) {
		// 向舰队中添加成员—————幻象—————RANDOM
		Random random = new Random();
		FleetParamsV3 params = new FleetParamsV3(null, "Omega_Watcher", 0.25f, "TASK_FORCE", 100f, 0f, 0f, 0f, 0f, 0f, 0f);
		params.ignoreMarketFleetSizeMult = true;
		CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
		fleet.setName("显现-100");
		// 设置舰队属性，使其具有敌对性和攻击性
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
		fleet.removeAbility("emergency_burn");
		fleet.removeAbility("sensor_burst");
		fleet.removeAbility("go_dark");
		fleet.getMemoryWithoutUpdate().set("$sawPlayerTransponderOn", true);
		fleet.getMemoryWithoutUpdate().set("$isPatrol", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_longPursuit", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_holdVsStronger", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_noJump", true);
		// 设置舰队属性，使其不产生重复影响和低影响(?)
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, true);
		// 设置舰队属性，使其不进行船只回收和始终追击目标
		//fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true); // so it keeps transponder on
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, false);
		fleet.setId("Omega_Unsure");
		// 激活舰队的传讯器能力
		fleet.getAbility(Abilities.TRANSPONDER).activate();

		// 随机设置舰队的位置并将其添加到包含该位置的实体中
		Vector2f loc = new Vector2f(star.getLocation().x + 8000 * ((float)Math.random() - 0.5f), star.getLocation().y + 8000 * ((float)Math.random() - 0.5f));
		fleet.setLocation(loc.x, loc.y);
		star.getContainingLocation().addEntity(fleet);

		// 为舰队添加脚本以处理牵引分配AI任务
		fleet.addScript(new IIRT_AssignmentAI(fleet, star));
	}

	public void addFleetOmega_Psychasthenia50(SectorEntityToken rock) {
		// 向舰队中添加成员——————————RANDOM
		FleetParamsV3 params = new FleetParamsV3(null, "Omega_Psychasthenia", 2f, "TASK_FORCE", 60f, 0f, 0f, 0f, 0f, 0f, 0f);
		params.ignoreMarketFleetSizeMult = true;
		CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
		fleet.setName("偏执-60");
		// 设置舰队属性，使其具有敌对性和攻击性
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
		fleet.removeAbility("emergency_burn");
		fleet.removeAbility("sensor_burst");
		fleet.removeAbility("go_dark");
		fleet.getMemoryWithoutUpdate().set("$sawPlayerTransponderOn", true);
		fleet.getMemoryWithoutUpdate().set("$isPatrol", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_longPursuit", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_holdVsStronger", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_noJump", true);
		// 设置舰队属性，使其不产生重复影响和低影响(?)
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, true);
		// 设置舰队属性，使其不进行船只回收和始终追击目标
		//fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true); // so it keeps transponder on
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, false);
		fleet.setId("Omega_Psychasthenia80");
		// 激活舰队的传讯器能力
		fleet.getAbility(Abilities.TRANSPONDER).activate();

		// 随机设置舰队的位置并将其添加到包含该位置的实体中
		Vector2f loc = new Vector2f(rock.getLocation().x + 8000 * ((float)Math.random() - 0.5f), rock.getLocation().y + 8000 * ((float)Math.random() - 0.5f));
		fleet.setLocation(loc.x, loc.y);
		rock.getContainingLocation().addEntity(fleet);

		// 为舰队添加脚本以处理牵引分配AI任务
		fleet.addScript(new IIRT_AssignmentAI(fleet, rock));
	}

	public void addFleetOmega_Psychasthenia150(SectorEntityToken planet1) {
		// 向舰队中添加成员——————————RANDOM
		FleetParamsV3 params = new FleetParamsV3(null, "Omega_Psychasthenia", 2f, "TASK_FORCE", 80f, 0f, 0f, 0f, 0f, 0f, 0f);
		params.ignoreMarketFleetSizeMult = true;
		CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
		fleet.setName("躁虑-80");
		// 设置舰队属性，使其具有敌对性和攻击性
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
		fleet.removeAbility("emergency_burn");
		fleet.removeAbility("sensor_burst");
		fleet.removeAbility("go_dark");
		fleet.getMemoryWithoutUpdate().set("$sawPlayerTransponderOn", true);
		fleet.getMemoryWithoutUpdate().set("$isPatrol", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_longPursuit", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_holdVsStronger", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_noJump", true);
		// 设置舰队属性，使其不产生重复影响和低影响(?)
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, true);
		// 设置舰队属性，使其不进行船只回收和始终追击目标
		//fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true); // so it keeps transponder on
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, false);
		fleet.setId("Omega_Psychasthenia150");
		// 激活舰队的传讯器能力
		fleet.getAbility(Abilities.TRANSPONDER).activate();

		// 随机设置舰队的位置并将其添加到包含该位置的实体中
		Vector2f loc = new Vector2f(planet1.getLocation().x + 8000 * ((float)Math.random() - 0.5f), planet1.getLocation().y + 8000 * ((float)Math.random() - 0.5f));
		fleet.setLocation(loc.x, loc.y);
		planet1.getContainingLocation().addEntity(fleet);

		// 为舰队添加脚本以处理牵引分配AI任务
		fleet.addScript(new IIRT_AssignmentAI(fleet, planet1));
	}

	public void addFleetOmega_Psychasthenia240(SectorEntityToken planet2) {
		// 向舰队中添加成员——————————RANDOM
		FleetParamsV3 params = new FleetParamsV3(null, "Omega_Psychasthenia", 2f, "TASK_FORCE", 150f, 0f, 0f, 0f, 0f, 0f, 0f);
		params.ignoreMarketFleetSizeMult = true;
		CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
		fleet.setName("创伤-150");
		// 设置舰队属性，使其具有敌对性和攻击性
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
		fleet.removeAbility("emergency_burn");
		fleet.removeAbility("sensor_burst");
		fleet.removeAbility("go_dark");
		fleet.getMemoryWithoutUpdate().set("$sawPlayerTransponderOn", true);
		fleet.getMemoryWithoutUpdate().set("$isPatrol", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_longPursuit", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_holdVsStronger", true);
		fleet.getMemoryWithoutUpdate().set("$cfai_noJump", true);
		// 设置舰队属性，使其不产生重复影响和低影响(?)
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, true);
		// 设置舰队属性，使其不进行船只回收和始终追击目标
		//fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true); // so it keeps transponder on
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, false);
		fleet.setId("Omega_Psychasthenia240");
		// 激活舰队的传讯器能力
		fleet.getAbility(Abilities.TRANSPONDER).activate();

		// 随机设置舰队的位置并将其添加到包含该位置的实体中
		Vector2f loc = new Vector2f(planet2.getLocation().x + 8000 * ((float)Math.random() - 0.5f), planet2.getLocation().y + 8000 * ((float)Math.random() - 0.5f));
		fleet.setLocation(loc.x, loc.y);
		planet2.getContainingLocation().addEntity(fleet);

		// 为舰队添加脚本以处理牵引分配AI任务
		fleet.addScript(new IIRT_AssignmentAI(fleet, planet2));
	}

	public void addFleetOmega_PsychastheniaZONE(SectorEntityToken planet4) {
		// 向舰队中添加成员——————————RANDOM
		CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet("Omega_Psychasthenia", FleetTypes.BATTLESTATION, null);
		fleet.setName("稳固侵袭-生成最多8队50~130部队");
		fleet.isStationMode();
		fleet.getFleetData().addFleetMember("IIRT_Omega_Station_Common");
		// 设置舰队属性，使其具有敌对性和攻击性
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
		// 设置舰队属性，使其不进行船只回收和始终追击目标
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, false);
		fleet.setId("Omega_PsychastheniaZONE");
		// 激活舰队的传讯器能力
		fleet.getAbility(Abilities.TRANSPONDER).activate();

		// 随机设置舰队的位置并将其添加到包含该位置的实体中
		Vector2f loc = new Vector2f(planet4.getLocation().x + 8000 * ((float)Math.random() - 0.5f), planet4.getLocation().y + 8000 * ((float)Math.random() - 0.5f));
		fleet.setLocation(loc.x, loc.y);
		planet4.getContainingLocation().addEntity(fleet);

		// 为舰队添加脚本以处理牵引分配AI任务
		fleet.addScript(new IIRT_AssignmentAI(fleet, planet4));
		fleet.addScript(new IIRT_OmegaStationFleetManager(fleet, 50f, 1, 5, 30f, 50, 130));
	}

	public void addFleetOmega_HeatDeath_boundary(SectorEntityToken planet3) {
		// 向舰队中添加成员——————————RANDOM
		CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet("Omega_Psychasthenia", FleetTypes.BATTLESTATION, null);
		fleet.setName("热寂死界");
		fleet.getFleetData().addFleetMember("IIRT_Omega_Singularity_Attack");
		fleet.getFleetData().addFleetMember("IIRT_Omega_Singularity_Attack");
		fleet.getFleetData().addFleetMember("IIRT_Omega_Singularity_Support");
		fleet.getFleetData().addFleetMember("IIRT_Omega_Singularity_Support");
		fleet.getFleetData().addFleetMember("IIRT_Omega_Heatdeath_Normal");
		fleet.getFleetData().addFleetMember("IIRT_Omega_Heatdeath_Normal");
		// 设置舰队属性，使其具有敌对性和攻击性
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
		// 设置舰队属性，使其不进行船只回收和始终追击目标
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, false);
		fleet.setId("Omega_HeatDeath_boundary");
		// 激活舰队的传讯器能力
		fleet.getAbility(Abilities.TRANSPONDER).activate();

		// 随机设置舰队的位置并将其添加到包含该位置的实体中
		Vector2f loc = new Vector2f(planet3.getLocation().x + 8000 * ((float)Math.random() - 0.5f), planet3.getLocation().y + 8000 * ((float)Math.random() - 0.5f));
		fleet.setLocation(loc.x, loc.y);
		planet3.getContainingLocation().addEntity(fleet);
		fleet.addScript(new IIRT_AssignmentAI(fleet, planet3));
	}
}
