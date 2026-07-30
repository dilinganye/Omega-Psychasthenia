//
// 使用了MODDER群内的模板以及相当一部分MOD的星系生成代码作为了参考
// qwq
//

package data.scripts.world.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.WarningBeaconEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.world.TTBlackSite;
import com.fs.starfarer.api.util.Misc;
import static data.scripts.world.IIRT_Omega_ModGen.addMarketplace;
import data.scripts.util.I18nUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

public class KRM_Aleph_Post {

	public void generate(SectorAPI sector) {
		StarSystemAPI system = sector.createStarSystem("Unknown Neutron");
		//system.addTag(Tags.THEME_HIDDEN);
		//system.addTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);
		//system.addTag(Tags.THEME_UNSAFE);

		system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "music_KRM_Aleph_site");

		LocationAPI hyper = Global.getSector().getHyperspace();
		//星系位置
		system.getLocation().set(-26570.0F, -6880.0F);
		//背景图片
		system.setBackgroundTextureFilename("graphics/backgrounds/KRM_Aleph_Post.png");

		//SectorEntityToken star = system.initNonStarCenter();
		//恒星（大小，半径，日冕大小
		PlanetAPI star = system.initStar("Aleph Core", "star_neutron", 100f, 1000f, 0.5F, 1.5F, 0.3F);
		//背景光颜色
		system.setLightColor(new Color(22, 22, 45, 192));

		TTBlackSite.addDerelict(system, star, "IIRT_Lab_Lethe_Tanker_Common", "被遗弃的油船", "IIRT_Lab_Lethe_Tanker", ShipRecoverySpecial.ShipCondition.PRISTINE, star.getRadius() * 2.5F, Math.random() < 0.5D);
		star.setCustomDescriptionId("KRM_star_description");

        /*//行星（势力、圆心、引用、类型；设置星球简介以及归属
        PlanetAPI planet1 = system.addPlanet(
                "star2", //行星ID
                star, //恒星ID
                "Aleph Core", //星球名字
                "star_neutron", //类型
                215,
                100f,
                3200f,
                365f
        );*/

		//让小行星带环绕它
		system.addAsteroidBelt(star, 120, 5000f, 400f, 180, 360, Terrain.ASTEROID_BELT, "");
		system.addAsteroidBelt(star, 150, 5000f, 400f, 180, 360, Terrain.RING, "");

		CustomCampaignEntityAPI warningBB = system.addCustomEntity(null, null, Entities.WARNING_BEACON, Factions.NEUTRAL);
		warningBB.setCircularOrbitPointingDown(star, 0, 1250, 60);

		warningBB.getMemoryWithoutUpdate().set(WarningBeaconEntityPlugin.PING_ID_KEY, Pings.WARNING_BEACON3);
		warningBB.getMemoryWithoutUpdate().set(WarningBeaconEntityPlugin.PING_FREQ_KEY, 1.5f);
		warningBB.getMemoryWithoutUpdate().set(WarningBeaconEntityPlugin.PING_COLOR_KEY, new Color(73, 12, 255, 255));
		warningBB.getMemoryWithoutUpdate().set(WarningBeaconEntityPlugin.GLOW_COLOR_KEY, new Color(12, 255, 227, 255));

		SectorEntityToken KRM_nebula = Misc.addNebulaFromPNG("graphics/backgrounds/KRM_nebula.png", // png
				0, 0, // center of nebula
				system, // location to add to
				"terrain", "nebula_blue", // "nebula_blue", // texture to use, uses xxx_map for map
				4, 4, StarAge.AVERAGE); // number of cells in texture
        /*
          一号行星————————————————————————————————————————————————————————————————————————
          @return Trunking Post
         */
		//行星（势力、圆心、引用、类型；设置星球简介以及归属
		PlanetAPI planet1 = system.addPlanet("KRM_planet1", //行星ID
				star, //恒星ID
				I18nUtil.getStarSystemsString("KRM_planet1_name"), //星球名字
				"cryovolcanic", //类型
				215, 680f, 3200f, 365f);
		//行星环
		system.addAsteroidBelt(star, 150, 3200f, 180f, 180, 360, Terrain.RING, "");

		planet1.getSpec().setGlowColor(new Color(255, 255, 255));
		planet1.getSpec().setUseReverseLightForGlow(true);
		planet1.getSpec().setCloudColor(new Color(211, 209, 255, 150));
		planet1.setFaction("KRM");
		Misc.initConditionMarket(planet1);

		//descriptions.csv引用星球介绍位置
		planet1.setCustomDescriptionId("KRM_planet1_description");
		// 设置环境，市场
		MarketAPI planet1Market = addMarketplace("KRM", planet1, null, planet1.getName(), 3, new ArrayList<>(Arrays.asList(Conditions.POPULATION_3, // 设置殖民地规模
				//这几块都在设置环境
				Conditions.VERY_COLD, //极寒
				Conditions.EXTREME_WEATHER, //恶劣天气
				Conditions.ORE_ULTRARICH, Conditions.RARE_ORE_RICH, Conditions.VOLATILES_ABUNDANT, Conditions.DARK, Conditions.NO_ATMOSPHERE, //煤有大汽
				Conditions.TECTONIC_ACTIVITY, Conditions.HIGH_GRAVITY)), new ArrayList<>(Arrays.asList( //这几块都在设置市场类型
				Submarkets.SUBMARKET_OPEN, Submarkets.SUBMARKET_STORAGE, Submarkets.SUBMARKET_BLACK)), new ArrayList<>(Arrays.asList("IIRT_Lab_DarkCity", Industries.POPULATION, //这几块都在设置工业区划建设
				Industries.SPACEPORT, Industries.GROUNDDEFENSES, Industries.BATTLESTATION_HIGH, Industries.REFINING, Industries.MINING, Industries.ORBITALWORKS, Industries.WAYSTATION, "IIRT_Lab_UnderMine")), 0.2f, false, false);
        /*
          二号行星————————————————————————————————————————————————————————————————————————
          @return Description Post
         */
		//行星（势力、圆心、引用、类型；设置星球简介以及归属
		PlanetAPI planet2 = system.addPlanet("KRM_planet2", //行星ID
				star, //恒星ID
				I18nUtil.getStarSystemsString("KRM_planet2_name"), //星球名字
				"rocky_ice", //类型
				35, 250f, 4800f, 365f);
		//行星环
		system.addAsteroidBelt(star, 150, 4800f, 190f, 180, 360, Terrain.RING, "");
		planet2.getSpec().setGlowColor(new Color(255, 255, 255));
		planet2.getSpec().setAtmosphereColor(new Color(167, 177, 255, 100));
		planet2.getSpec().setUseReverseLightForGlow(true);
		planet2.getSpec().setCloudColor(new Color(230, 244, 248, 150));
		planet2.setFaction("KRM");
		Misc.initConditionMarket(planet2);

		// 设置环境，市场
		MarketAPI planet2Market = addMarketplace("KRM", planet2, null, planet2.getName(), 4, new ArrayList<>(Arrays.asList(Conditions.POPULATION_4, // 设置殖民地规模
				//这几块都在设置环境
				Conditions.VERY_COLD, //极寒
				Conditions.EXTREME_WEATHER, //恶劣天气
				Conditions.ORE_SPARSE, Conditions.DARK, Conditions.THIN_ATMOSPHERE, Conditions.TECTONIC_ACTIVITY, Conditions.RUINS_SCATTERED)), new ArrayList<>(Arrays.asList( //这几块都在设置市场类型
				Submarkets.SUBMARKET_OPEN, Submarkets.SUBMARKET_STORAGE)), new ArrayList<>(Arrays.asList("IIRT_Lab_DarkCity", Industries.POPULATION, //这几块都在设置工业区划建设
				Industries.SPACEPORT, Industries.STARFORTRESS_HIGH, Industries.GROUNDDEFENSES, Industries.ORBITALWORKS, Industries.WAYSTATION, Industries.MILITARYBASE)), 0.3f, false, true);

		TTBlackSite.addDerelict(system, planet2, "IIRT_Omega_Heatdeath_Missile", "???", "IIRT_Omega_Heatdeath", ShipRecoverySpecial.ShipCondition.BATTERED, planet2.getRadius() * 4.5F, Math.random() < 0.7D);
		TTBlackSite.addDerelict(system, planet2, "IIRT_Omega_Singularity_Attack_Plus", "???", "IIRT_Omega_Singularity", ShipRecoverySpecial.ShipCondition.BATTERED, planet2.getRadius() * 4.8F, Math.random() < 0.7D);
		TTBlackSite.addDerelict(system, planet2, "IIRT_Lab_LongKnives_Assault", "破损的舰船", "IIRT_Lab_LongKnives", ShipRecoverySpecial.ShipCondition.AVERAGE, planet2.getRadius() * 4.8F, Math.random() < 0.7D);
		TTBlackSite.addDerelict(system, planet2, "IIRT_Lab_LongKnives_Assault", "遗弃的舰船", "IIRT_Lab_LongKnives", ShipRecoverySpecial.ShipCondition.GOOD, planet2.getRadius() * 3.8F, Math.random() < 0.7D);
		TTBlackSite.addDerelict(system, planet2, "IIRT_Lab_Thought_Hunter", "战毁的舰船", "IIRT_Lab_Thought", ShipRecoverySpecial.ShipCondition.BATTERED, planet2.getRadius() * 2.8F, Math.random() < 0.7D);

		//descriptions.csv引用星球介绍位置
		planet2.setCustomDescriptionId("KRM_planet2_description");

		//give every industry an AiCoreId    给每个地方丢个AI核心

		Industry IIRTPopulation2 = planet2Market.getIndustry("population");
		IIRTPopulation2.setAICoreId("gamma_core");

		Industry IIRTMilitarybase2 = planet2Market.getIndustry("militarybase");
		IIRTMilitarybase2.setAICoreId("gamma_core");
/*
          四号行星————————————————————————————————————————————————————————————————————————
          @return Description Post
         */
		//行星（势力、圆心、引用、类型；设置星球简介以及归属
		PlanetAPI planet4 = system.addPlanet("KRM_planet4", //行星ID
				star, //恒星ID
				I18nUtil.getStarSystemsString("KRM_planet4_name"), //星球名字
				"toxic_cold", 5, 140f, 2730f, 370f);
		//行星环
		system.addAsteroidBelt(star, 150, 2730f, 140f, 180, 360, Terrain.RING, "");
		planet4.setFaction("KRM");
		Misc.initConditionMarket(planet4);

		planet4.getMarket().addCondition(Conditions.TOXIC_ATMOSPHERE);
		planet4.getMarket().addCondition(Conditions.HOT);
		planet4.getMarket().addCondition(Conditions.DARK);
		planet4.getMarket().addCondition(Conditions.IRRADIATED);
		planet4.getMarket().addCondition(Conditions.RUINS_SCATTERED);
		planet4.getMarket().addCondition(Conditions.ORGANICS_COMMON);

		planet4.getSpec().setGlowColor(new Color(141, 141, 141));
		planet2.getSpec().setAtmosphereColor(new Color(192, 167, 255, 100));
		planet4.getSpec().setUseReverseLightForGlow(true);
		planet4.getSpec().setCloudColor(new Color(45, 45, 45, 150));

		//descriptions.csv引用星球介绍位置
		planet4.setCustomDescriptionId("KRM_planet4_description");

        /*
          三号行星————————————————————————————————————————————————————————————————————————
          @return Description Post
         */
		//行星（势力、圆心、引用、类型；设置星球简介以及归属
		PlanetAPI planet3 = system.addPlanet("KRM_planet3", //行星ID
				planet4, //恒星ID
				I18nUtil.getStarSystemsString("KRM_planet3_name"), //星球名字
				"frozen2", //类型
				70, 70f, 800f, 370f);
		//行星环
		system.addAsteroidBelt(planet4, 200, 800f, 120f, 180, 360, Terrain.RING, "");
		system.addAsteroidBelt(planet4, 200, 800f, 120f, 180, 360, Terrain.ASTEROID_BELT, "");
		planet3.getSpec().setGlowColor(new Color(172, 214, 255));
		planet3.getSpec().setAtmosphereColor(new Color(40, 248, 255, 100));
		planet3.getSpec().setGlowTexture(Global.getSettings().getSpriteName("hab_glows", "aurorae"));
		//planet2.getSpec().setCloudTexture(Global.getSettings().getSpriteName("planets", "KRM_Labyorm_clouds"));
		//planet2.getSpec().setTexture(Global.getSettings().getSpriteName("planets", "KRM_Labyorm"));
		//planet2.getSpec().setGlowTexture(Global.getSettings().getSpriteName("planets", "KRM_Labyorm_light"));
		planet3.getSpec().setUseReverseLightForGlow(true);
		planet3.getSpec().setCloudColor(new Color(89, 189, 171, 150));
		planet3.setFaction("KRM");
		Misc.initConditionMarket(planet3);
		//让小行星带环绕它
		system.addAsteroidBelt(planet3, 30, 300f, 100f, 180, 360, Terrain.ASTEROID_BELT, "");
		// 设置环境，市场
		MarketAPI planet3Market = addMarketplace("KRM", planet3, null, planet3.getName(), 4, new ArrayList<>(Arrays.asList(Conditions.POPULATION_4, // 设置殖民地规模
						//这几块都在设置环境
						Conditions.VERY_COLD, //极寒
						Conditions.EXTREME_WEATHER, //恶劣天气
						Conditions.METEOR_IMPACTS, Conditions.RUINS_SCATTERED, Conditions.LOW_GRAVITY, Conditions.THIN_ATMOSPHERE)), //
				new ArrayList<>(Arrays.asList( //这几块都在设置市场类型
						Submarkets.SUBMARKET_OPEN, Submarkets.SUBMARKET_STORAGE)), new ArrayList<>(Arrays.asList("IIRT_Lab_DarkCity", Industries.POPULATION, //这几块都在设置工业区划建设
						Industries.SPACEPORT, Industries.HEAVYBATTERIES, Industries.MILITARYBASE, "IIRT_Lab_TheCore", Industries.WAYSTATION, Industries.ORBITALWORKS, Industries.ORBITALSTATION_HIGH)), 0.3f, false, false);

		//descriptions.csv引用星球介绍位置
		planet3.setCustomDescriptionId("KRM_planet3_description");

		//give every industry an AiCoreId    给每个地方丢个AI核心
		Industry IIRT3Megaport = planet3Market.getIndustry("orbitalstation_high");
		IIRT3Megaport.setAICoreId("gamma_core");

		Industry IIRT3HighCommand = planet3Market.getIndustry("militarybase");
		IIRT3HighCommand.setAICoreId("alpha_core");


        /*
          五号行星————————————————————————————————————————————————————————————————————————
          @return Description Post
         */
		//行星（势力、圆心、引用、类型；设置星球简介以及归属
		PlanetAPI planet5 = system.addPlanet("KRM_planet5", //行星ID
				planet4, //恒星ID
				I18nUtil.getStarSystemsString("KRM_planet5_name"), //星球名字
				"ice_giant", 40, 1250f, 5000f, 320f);
		//行星环
		system.addAsteroidBelt(planet4, 150, 5000f, 8600f, 180, 360, Terrain.RING, "");

		planet5.setFaction("KRM");
		//TTBlackSite.addDerelict(system, planet5,  "IIRT_Skirmisher_variant", "SK 尸骸", "IIRT_Skirmisher", ShipRecoverySpecial.ShipCondition.AVERAGE, planet5.getRadius() * 4.5F, Math.random() < 0.7D);
		//descriptions.csv引用星球介绍位置
		planet5.setCustomDescriptionId("KRM_planet5_description");
		//planet5.getSpec().setPlanetColor(new Color(56, 105, 133,255));
		planet5.getSpec().setAtmosphereColor(new Color(100, 113, 130, 150));
		planet5.getSpec().setIconColor(new Color(91, 124, 131, 255));
		planet5.getSpec().setGlowTexture(Global.getSettings().getSpriteName("hab_glows", "aurorae"));
		planet5.getSpec().setAtmosphereThickness(0.5f);
		planet5.getSpec().setGlowColor(new Color(129, 204, 184));
		planet5.getSpec().setUseReverseLightForGlow(true);
		planet5.getSpec().setCloudColor(new Color(168, 158, 231, 150));
		SectorEntityToken planet5_field = system.addTerrain(Terrain.MAGNETIC_FIELD, new MagneticFieldTerrainPlugin.MagneticFieldParams(400f, // terrain effect band width
				620, // terrain effect middle radius
				planet5, // entity that it's around
				1250f, // visual band start
				1500f, // visual band end
				new Color(100, 84, 30, 30), // base color
				1f, // probability to spawn aurora sequence, checked once/day when no aurora in progress
				new Color(20, 51, 110, 130), new Color(90, 118, 150, 150), new Color(100, 135, 200, 190), new Color(110, 175, 250, 240), new Color(106, 80, 200, 255), new Color(35, 0, 160), new Color(72, 0, 255)));
		planet5_field.setCircularOrbit(planet5, 0, 0, 100);

		planet5.applySpecChanges();

		//为星系生成指定跳跃点
		JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint("inside_point", "在解决蠢笨的势力争霸乱开战导致游戏爆炸之前只能非常不愉悦的开出这么一个非常讨厌的门真的很不开心所以这个门正对着黑洞不要进去的 Aleph 跳跃点");
		OrbitAPI orbit = Global.getFactory().createCircularOrbit(planet1, 0, 4000, 30);
		jumpPoint.setOrbit(orbit);
		jumpPoint.setRelatedPlanet(planet1);
		jumpPoint.setStandardWormholeToHyperspaceVisual();
		system.addEntity(jumpPoint);
		//扫描本星系所有跳跃点并为之配置数据
		system.autogenerateHyperspaceJumpPoints(true, false);
		/*
		 */

		system.generateAnchorIfNeeded();

		//生成不稳定点
		NascentGravityWellAPI well = Global.getSector().createNascentGravityWell(warningBB, 200f);
		well.addTag(Tags.NO_ENTITY_TOOLTIP);
		well.setColorOverride(new Color(183, 204, 229));
		hyper.addEntity(well);
		well.autoUpdateHyperLocationBasedOnInSystemEntityAtRadius(warningBB, 0);

		//在超空间清出一些区域
		HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin)Misc.getHyperspaceTerrain().getPlugin();
		NebulaEditor editor = new NebulaEditor(plugin);
		float minRadius = plugin.getTileSize() * 2f;
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, minRadius * 5f, 8, 360f);
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, minRadius, 0, 360f, 0.25f);
		//在超空间再放点垃圾
		editor.regenNoise();

		//生成星门
		SectorEntityToken gate = system.addCustomEntity("KRM_gate", // unique id 设置星门id
				"不稳定星门", // name - if null, defaultName from custom_entities.json will be used 设置你星门的名字
				"inactive_gate", // type of object, defined in custom_entities.json 设置标签（让系统识别这是个星门）根据custom_entities.json设置
				null); // faction
		gate.setCircularOrbit(star, 5, 3080f, 350f);

		//设置各种罐头
		SectorEntityToken SPL = BaseThemeGenerator.addSalvageEntity(system, "supply_cache", Factions.NEUTRAL);
		SPL.setCircularOrbit(planet4, 180f, 700f, 365f);

		//SectorEntityToken SPC1 = system.addCustomEntity("KRM_SPC1", "一处补给藏匿点", "supply_cache_KRM", Factions.NEUTRAL);
		//SPC1.setCircularOrbit(star, 180f, 3000f, 400f);

		//SectorEntityToken SPC2 = system.addCustomEntity("KRM_SPC2", "一处补给藏匿点", "supply_cache_KRM", Factions.NEUTRAL);
		//SPC2.setCircularOrbit(planet2, 180f, 2800f, 400f);

		//SectorEntityToken SPC3 = system.addCustomEntity("KRM_SPC3", "一处补给藏匿点", "weapons_cache_KRM", Factions.NEUTRAL);
		//SPC3.setCircularOrbit(star, 180f, 5000f, 400f);

		//设置星系仓库
		SectorEntityToken STR = system.addCustomEntity("KRM_STR", "废弃的研究设施", "IIRT_Lab_BoomingStation", "neutral");
		STR.setCircularOrbitPointingDown(planet5, 75, 350, 50);
		STR.setCustomDescriptionId("KRM_Sci_platform");
		STR.setInteractionImage("illustrations", "KRM_Platform");
		Misc.setAbandonedStationMarket("corvus_abandoned_station_market", STR);
		Misc.setAbandonedStationMarket("black_market", STR);

		STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addFuel(20f);
		STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addSupplies(10f);
		//设置战役奖励
		if (Global.getSettings().getMissionScore("Omega_Deteriorate") == 100) {
			STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addWeapons("thermosiphonsrm", 1);
			STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addFuel(20f);
			STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addSupplies(50f);
		}
		if (Global.getSettings().getMissionScore("Omega_Harassing") >= 75) {
			STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addWeapons("Omega_Vpdiver_S", 1);
			STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addFuel(20f);
			STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addSupplies(50f);
		}
		if (Global.getSettings().getMissionScore("Omega_PersonalTailor") == 100) {
			STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addMothballedShip(FleetMemberType.SHIP, "IIRT_Omega_Cipher_Beam", "密钥-月相");
		}
		if (Global.getSettings().getMissionScore("Omega_NoEnterPlease") == 100) {
			STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addFighters("IIRT_Lab_Crescent_Attack_AC", 1);
			STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addFighters("IIRT_Lab_Twillight_Assault_wing", 2);
			STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addWeapons("IIRT_Lab_Volley_Blade", 2);
			STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addWeapons("IIRT_Lab_Tracicles", 2);
			STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addWeapons("IIRT_Missile_Mine", 2);
			STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addSupplies(80f);
		}
		if (Global.getSettings().getMissionScore("IIRT_Omega_test_2") >= 50) {
			STR.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addMothballedShip(FleetMemberType.SHIP, "hermes_TY_Standard", "感谢你选择此MOD，这是一幅送给你的画");
		}

		//设置你星系的永久稳定点建筑
		SectorEntityToken A = system.addCustomEntity("KRM_A", "通讯稳定信道设施", "comm_relay", "KRM");
		A.setCircularOrbit(star, 180f, 2900f, 365f);
		SectorEntityToken B = system.addCustomEntity("KRM_B", "导航中继系统浮标", "nav_buoy", "KRM");
		B.setCircularOrbit(star, 220f, 2500f, 365f);
		SectorEntityToken C = system.addCustomEntity("KRM_C", "隐秘波段探测阵列", "sensor_array", "KRM");
		C.setCircularOrbit(star, 240f, 2900f, 365f);

	}
	//SalvageSpecialAssigner.SpecialCreator
	//public static interface SpecialCreator {
	//    Object createSpecial(SectorEntityToken entity, SalvageSpecialAssigner.SpecialCreationContext context);
	//}
}
