//
// 使用了MODDER群内的模板以及相当一部分MOD的星系生成代码作为了参考
// qwq
//

package data.scripts.world.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.world.TTBlackSite;
import com.fs.starfarer.api.util.Misc;
import static data.scripts.world.IIRT_Omega_ModGen.addMarketplace;
import data.utils.iirt_omega.I18nUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

public class IIRT_Detachment_outpost {

	public void generate(SectorAPI sector) {
		StarSystemAPI system = sector.createStarSystem("Depravity Nodes");

		//星系位置
		system.getLocation().set(-32000.0F, 15000.0F);
		//背景图片
		system.setBackgroundTextureFilename("graphics/backgrounds/UnKnowSky.jpg");

		//恒星（大小，半径，日冕大小
		PlanetAPI star = system.initStar("MandoLas", "star_blue_giant", 700f, 300f, 2.0F, 0.5F, 1.0F);
		//背景光颜色
		system.setLightColor(new Color(182, 175, 255));

		TTBlackSite.addDerelict(system, star, "IIRT_Omega_Firewall_Only", "???", "IIRT_Omega_Gateway", ShipRecoverySpecial.ShipCondition.BATTERED, star.getRadius() * 10F, Math.random() < 0.7D);
		TTBlackSite.addDerelict(system, star, "IIRT_Omega_Gateway_Only2", "???", "IIRT_Omega_Gateway", ShipRecoverySpecial.ShipCondition.AVERAGE, star.getRadius() * 10F, Math.random() < 0.7D);

		//让小行星带环绕它
		system.addAsteroidBelt(star, 120, 5000f, 400f, 180, 360, Terrain.ASTEROID_BELT, "");
		system.addAsteroidBelt(star, 180, 1500f, 400f, 180, 360, Terrain.ASTEROID_BELT, "");

		SectorEntityToken IIRT_nebula = Misc.addNebulaFromPNG("graphics/backgrounds/IIRT_nebula.png", // png
				0, 0, // center of nebula
				system, // location to add to
				"terrain", "nebula_blue", // "nebula_blue", // texture to use, uses xxx_map for map
				4, 4, StarAge.AVERAGE); // number of cells in texture
        /*
          一号行星————————————————————————————————————————————————————————————————————————
          @return Trunking Post
         */
		//行星（势力、圆心、引用、类型；设置星球简介以及归属
		PlanetAPI planet1 = system.addPlanet("IIRT_planet1", //行星ID
				star, //恒星ID
				I18nUtil.getStarSystemsString("IIRT_planet1_name"), //星球名字
				"frozen2", //类型
				215, 180f, 4000f, 365f);
		//行星环
		system.addAsteroidBelt(star, 150, 4000f, 180f, 180, 360, Terrain.RING, "");
		planet1.setFaction("IIRT"); //行星所属势力
		planet1.getSpec().setGlowColor(new Color(237, 233, 255));   //行星散射光照(可以理解为大气层发光)
		planet1.getSpec().setUseReverseLightForGlow(true);
		planet1.getSpec().setCloudColor(new Color(61, 64, 72, 150));    //行星de云
		planet1.applySpecChanges(); //行星应用特殊设置
		Misc.initConditionMarket(planet1);

		// 设置环境，市场
		MarketAPI planet1Market = addMarketplace("IIRT", planet1, null, planet1.getName(), 5, new ArrayList<>(Arrays.asList(Conditions.POPULATION_7, // 设置殖民地规模
						//这几块都在设置环境
						Conditions.VERY_COLD, //极寒
						Conditions.EXTREME_WEATHER, //恶劣天气
						Conditions.RUINS_WIDESPREAD,    //广泛的遗迹
						Conditions.ORE_ULTRARICH, //广袤的铁矿
						Conditions.NO_ATMOSPHERE, //煤有大汽
						Conditions.EXTREME_TECTONIC_ACTIVITY)), //极端地质活动
				new ArrayList<>(Arrays.asList(
						//这几块都在设置市场类型
						Submarkets.SUBMARKET_OPEN, "IIRT_SD_Market_Army", "IIRT_SD_Market", Submarkets.SUBMARKET_STORAGE)), new ArrayList<>(Arrays.asList(Industries.POPULATION, //这几块都在设置工业区划建设
						Industries.MEGAPORT, Industries.STARFORTRESS_HIGH, Industries.HEAVYBATTERIES, Industries.REFINING, Industries.ORBITALWORKS, Industries.WAYSTATION, Industries.HIGHCOMMAND, Industries.FUELPROD, Industries.PLANETARYSHIELD, "IIRT_SD_Yisic_Lab", "IIRT_SD_Army_Tran")), 0.3f, false, true);

		//descriptions.csv引用星球介绍位置
		planet1.setCustomDescriptionId("IIRT_planet1_description");

		//give every industry an AiCoreId    给每个地方丢个AI核心

		Industry IIRTPopulation = planet1Market.getIndustry("population");
		IIRTPopulation.setAICoreId("gamma_core");

		Industry IIRTHighCommand = planet1Market.getIndustry("highcommand");
		IIRTHighCommand.setAICoreId("alpha_core");

		Industry IIRTStarFortress = planet1Market.getIndustry("starfortress_high");
		IIRTStarFortress.setAICoreId("gamma_core");

		Industry IIRTOrbitalWorks = planet1Market.getIndustry("orbitalworks");
		IIRTOrbitalWorks.setAICoreId("alpha_core");
		//一个纳米锻造炉
		planet1Market.getIndustry(Industries.ORBITALWORKS).setSpecialItem(new SpecialItemData(Items.PRISTINE_NANOFORGE, null));

		//生成遗弃舰
		TTBlackSite.addDerelict(system, planet1, "IIRT_SD_DownLight_Assault", "DL 筒灯", "IIRT_SD_DownLight", ShipRecoverySpecial.ShipCondition.GOOD, planet1.getRadius() * 2.5F, Math.random() < 0.5D);

        /*
          二号行星————————————————————————————————————————————————————————————————————————
          @return Description Post
         */
		//行星（势力、圆心、引用、类型；设置星球简介以及归属
		PlanetAPI planet2 = system.addPlanet("IIRT_planet2", //行星ID
				star, //恒星ID
				I18nUtil.getStarSystemsString("IIRT_planet2_name"), //星球名字
				"cryovolcanic", //类型
				35, 190f, 4800f, 365f);
		//行星环
		system.addAsteroidBelt(star, 150, 4800f, 190f, 180, 360, Terrain.RING, "");
		planet2.getSpec().setGlowColor(new Color(255, 255, 255));
		planet2.getSpec().setUseReverseLightForGlow(true);
		planet2.getSpec().setCloudColor(new Color(230, 244, 248, 150));
		planet2.setFaction("IIRT");
		Misc.initConditionMarket(planet2);

		// 设置环境，市场
		MarketAPI planet2Market = addMarketplace("IIRT", planet2, null, planet2.getName(), 4, new ArrayList<>(Arrays.asList(Conditions.POPULATION_5, // 设置殖民地规模
						//这几块都在设置环境
						Conditions.VERY_COLD, //极寒
						Conditions.EXTREME_WEATHER, //恶劣天气
						Conditions.RUINS_SCATTERED, Conditions.RARE_ORE_ABUNDANT, Conditions.ORE_ABUNDANT, Conditions.VOLATILES_ABUNDANT, Conditions.NO_ATMOSPHERE, //煤有大汽
						Conditions.EXTREME_TECTONIC_ACTIVITY)), //极端地质活动
				new ArrayList<>(Arrays.asList( //这几块都在设置市场类型
						Submarkets.SUBMARKET_OPEN, "IIRT_SD_Market_Nor", Submarkets.SUBMARKET_STORAGE)), new ArrayList<>(Arrays.asList(Industries.POPULATION, //这几块都在设置工业区划建设
						Industries.MEGAPORT, Industries.STARFORTRESS_HIGH, Industries.HEAVYBATTERIES, Industries.REFINING, Industries.ORBITALWORKS, Industries.WAYSTATION, Industries.MILITARYBASE, "IIRT_SD_Army_Tran")), 0.3f, false, true);

		TTBlackSite.addDerelict(system, planet2, "IIRT_SD_Dim_Tanker", "DM 无光", "IIRT_SD_Dim", ShipRecoverySpecial.ShipCondition.AVERAGE, planet2.getRadius() * 4.5F, Math.random() < 0.7D);
		//TTBlackSite.addDerelict(system, planet2,  "IIRT_Omega_Gateway_Only3", "???", "IIRT_Omega_Gateway", ShipRecoverySpecial.ShipCondition.AVERAGE, planet2.getRadius() * 4.8F, Math.random() < 0.7D);

		//descriptions.csv引用星球介绍位置
		planet2.setCustomDescriptionId("IIRT_planet2_description");

		//give every industry an AiCoreId    给每个地方丢个AI核心
		Industry IIRTMegaport2 = planet2Market.getIndustry("megaport");
		IIRTMegaport2.setAICoreId("gamma_core");

		Industry IIRTPopulation2 = planet2Market.getIndustry("population");
		IIRTPopulation2.setAICoreId("gamma_core");

		Industry IIRTMilitarybase2 = planet2Market.getIndustry("militarybase");
		IIRTMilitarybase2.setAICoreId("gamma_core");

		Industry IIRTOrbitalWorks2 = planet2Market.getIndustry("orbitalworks");
		IIRTOrbitalWorks2.setAICoreId("gamma_core");

        /*
          三号行星————————————————————————————————————————————————————————————————————————
          @return Description Post
         */
		//行星（势力、圆心、引用、类型；设置星球简介以及归属
		PlanetAPI planet3 = system.addPlanet("IIRT_planet3", //行星ID
				star, //恒星ID
				I18nUtil.getStarSystemsString("IIRT_planet3_name"), //星球名字
				"barren-bombarded", //类型
				70, 120f, 2400f, 370f);
		//行星环
		system.addAsteroidBelt(star, 200, 2400f, 120f, 180, 360, Terrain.RING, "");
		planet3.getSpec().setGlowColor(new Color(236, 255, 252));
		planet3.getSpec().setUseReverseLightForGlow(true);
		planet3.getSpec().setCloudColor(new Color(158, 184, 196, 150));
		planet3.setFaction("IIRT");
		Misc.initConditionMarket(planet3);
		//让小行星带环绕它
		system.addAsteroidBelt(planet3, 30, 300f, 100f, 180, 360, Terrain.ASTEROID_BELT, "");
		system.addAsteroidBelt(planet3, 60, 600f, 100f, 180, 360, Terrain.ASTEROID_BELT, "");
		system.addAsteroidBelt(planet3, 100, 1000f, 200f, 180, 360, Terrain.ASTEROID_BELT, "");

		// 设置环境，市场
		MarketAPI planet3Market = addMarketplace("IIRT", planet3, null, planet3.getName(), 3, new ArrayList<>(Arrays.asList(Conditions.POPULATION_4, // 设置殖民地规模
						//这几块都在设置环境
						Conditions.VERY_COLD, //极寒
						Conditions.EXTREME_WEATHER, //恶劣天气
						Conditions.RUINS_SCATTERED, Conditions.METEOR_IMPACTS, Conditions.RARE_ORE_ABUNDANT, Conditions.LOW_GRAVITY, //煤有大汽
						Conditions.NO_ATMOSPHERE, Conditions.ORE_ULTRARICH)), //
				new ArrayList<>(Arrays.asList( //这几块都在设置市场类型
						Submarkets.SUBMARKET_BLACK, Submarkets.SUBMARKET_OPEN, "IIRT_SD_Market_Nor", Submarkets.SUBMARKET_STORAGE)), new ArrayList<>(Arrays.asList(Industries.POPULATION, //这几块都在设置工业区划建设
						Industries.SPACEPORT, Industries.GROUNDDEFENSES, Industries.PATROLHQ, Industries.MINING, Industries.ORBITALWORKS, Industries.WAYSTATION, Industries.ORBITALSTATION_HIGH)), 0.3f, false, true);

		TTBlackSite.addDerelict(system, planet3, "IIRT_SD_Lens_Attack", "LS 透镜", "IIRT_SD_Lens", ShipRecoverySpecial.ShipCondition.AVERAGE, planet3.getRadius() * 4.5F, Math.random() < 0.7D);

		//descriptions.csv引用星球介绍位置
		planet3.setCustomDescriptionId("IIRT_planet3_description");

		//give every industry an AiCoreId    给每个地方丢个AI核心
		Industry IIRT3Megaport = planet3Market.getIndustry("orbitalstation_high");
		IIRT3Megaport.setAICoreId("gamma_core");

		Industry IIRT3Population = planet3Market.getIndustry("population");
		IIRT3Population.setAICoreId("gamma_core");

		Industry IIRT3HighCommand = planet3Market.getIndustry("patrolhq");
		IIRT3HighCommand.setAICoreId("alpha_core");


        /*
          五号行星————————————————————————————————————————————————————————————————————————
          @return Description Post
         */
		//行星（势力、圆心、引用、类型；设置星球简介以及归属
		PlanetAPI planet5 = system.addPlanet("IIRT_planet5", //行星ID
				planet3, //恒星ID
				I18nUtil.getStarSystemsString("IIRT_planet5_name"), //星球名字
				"rocky_ice", 40, 85f, 800f, 320f);
		//行星环
		system.addAsteroidBelt(planet3, 150, 800f, 85f, 180, 360, Terrain.RING, "");
		planet5.setFaction("IIRT");
		//生成太空破烂
		//descriptions.csv引用星球介绍位置
		planet5.setCustomDescriptionId("IIRT_resourcesPlanet_description");
		planet5.getSpec().setGlowColor(new Color(155, 155, 155));
		planet5.getSpec().setUseReverseLightForGlow(true);
		planet5.getSpec().setCloudColor(new Color(200, 200, 200, 150));

        /*
          六号行星————————————————————————————————————————————————————————————————————————
          @return Description Post
         */
		PlanetAPI planet6 = system.addPlanet("IIRT_planet6", //行星ID
				planet3, //恒星ID
				I18nUtil.getStarSystemsString("IIRT_planet6_name"), //星球名字
				"cryovolcanic", 120, 90f, 1100f, 320f);
		//行星环
		system.addAsteroidBelt(planet3, 150, 1100f, 90f, 180, 360, Terrain.RING, "");
		planet6.setFaction("IIRT");//descriptions.csv引用星球介绍位置
		planet6.setCustomDescriptionId("IIRT_resourcesPlanet_description");
		planet6.getSpec().setGlowColor(new Color(180, 255, 246));
		planet6.getSpec().setUseReverseLightForGlow(true);
		planet6.getSpec().setCloudColor(new Color(245, 255, 254, 195));
         /*
          七号行星————————————————————————————————————————————————————————————————————————
          @return Description Post
         */
		PlanetAPI planet7 = system.addPlanet("planet7", star, I18nUtil.getStarSystemsString("IIRT_planet7_name"), "ice_giant", 230, 320, 7000, 300);
		//行星环
		system.addAsteroidBelt(star, 150, 7000f, 320f, 180, 360, Terrain.RING, "");
		system.addAsteroidBelt(planet7, 150, 800f, 100f, 180, 360, Terrain.RING, "");
		planet7.setCustomDescriptionId("IIRT_resourcesPlanet_description");
		planet7.getSpec().setGlowColor(new Color(155, 155, 155));
		planet7.getSpec().setUseReverseLightForGlow(true);
		planet7.getSpec().setCloudColor(new Color(200, 200, 200, 150));

		//为星系生成指定跳跃点
		JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint("inside_point", "IIRT Jump-point");
		OrbitAPI orbit = Global.getFactory().createCircularOrbit(planet1, 0, 1000, 30);
		jumpPoint.setOrbit(orbit);
		jumpPoint.setRelatedPlanet(planet1);
		jumpPoint.setStandardWormholeToHyperspaceVisual();
		system.addEntity(jumpPoint);

		//为星系生成指定跳跃点
		JumpPointAPI jumpPoint2 = Global.getFactory().createJumpPoint("inside_point2", "IIRT Jump-point");
		OrbitAPI orbit2 = Global.getFactory().createCircularOrbit(star, 40, 3500, 360);
		jumpPoint2.setOrbit(orbit2);
		jumpPoint2.setRelatedPlanet(planet1);
		jumpPoint2.setStandardWormholeToHyperspaceVisual();
		system.addEntity(jumpPoint);

		//扫描本星系所有跳跃点并为之配置数据
		system.autogenerateHyperspaceJumpPoints(true, false);

		HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin)Misc.getHyperspaceTerrain().getPlugin();
		NebulaEditor editor = new NebulaEditor(plugin);
		float minRadius = plugin.getTileSize() * 2f;
		float radius = system.getMaxRadiusInHyperspace();
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius * 0.5f, 0, 360f);
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);

		//生成星门
		SectorEntityToken gate = system.addCustomEntity("IIRT_gate", // unique id 设置星门id
				"IIRT前哨 星门", // name - if null, defaultName from custom_entities.json will be used 设置你星门的名字
				"inactive_gate", // type of object, defined in custom_entities.json 设置标签（让系统识别这是个星门）根据custom_entities.json设置
				null); // faction

		gate.setCircularOrbit(star, 5, 3080f, 350f);

		//设置你星系的永久稳定点建筑
		SectorEntityToken A = system.addCustomEntity("IIRT_A", "中继通讯基座", "comm_relay", "IIRT");
		A.setCircularOrbit(star, 180f, 2900f, 365f);
		SectorEntityToken B = system.addCustomEntity("IIRT_B", "中继导航浮标", "nav_buoy", "IIRT");
		B.setCircularOrbit(star, 220f, 2500f, 365f);
		SectorEntityToken C = system.addCustomEntity("IIRT_C", "中继传感器阵列", "sensor_array", "IIRT");
		C.setCircularOrbit(star, 240f, 2900f, 365f);

	}

}
