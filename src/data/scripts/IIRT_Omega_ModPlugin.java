package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import data.scripts.campaign.invasion.IIRT_Omega_Invasion;
import data.scripts.campaign.invasion.PTSDCrisisDevIntel;
import data.scripts.campaign.invasion.PTSDCrisisDevWatcher;
import data.scripts.campaign.invasion.PTSDOccupationManager;
import data.scripts.campaign.PTSD_CampaignPlugin;
import data.scripts.campaign.cargo.PTSD_OmegaOfficerGeneratorPlugin;
import data.scripts.world.IIRT_NEXGenerate;
import data.scripts.world.IIRT_NormalGenerate;
import data.scripts.world.IIRT_Omega_Person;
import data.scripts.util.I18nUtil;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;

public class IIRT_Omega_ModPlugin extends BaseModPlugin {
	//public static final boolean OMEGA_PTSD_PREV = false; //决定是否开放实际内容！
	public static final boolean OMEGA_PTSD_PREV = true; //决定是否开放实际内容！

	public static boolean hasLunaLib = false;
	public static boolean omega_invasion_enabled = true;
	public static float start_stage_time = 65f;
	public static float collect_data_time = 60f;
	public static float invade_time = 30f;
	public static float repair_time = 30f;
	public static float scout_min_interval = 10f;
	public static float scout_max_interval = 25f;
	public static float scout_spawn_radius = 300f;
	public static float max_guard_fleets = 10f;
public static float final_invasion_max_strength = 200f;
	public static float scout_max_active = 3f;
	public static int warning_encounter_threshold = 4;
	public static float strategic_update_interval = 7f;
	public static float hidden_materialization_range = 5000f;
	public static float expansion_interval = 14f;
	public static float max_black_hole_fortresses = 3f;
	public static float front_turn_min_interval = 6f;
	public static float front_turn_max_interval = 12f;
	public static String PTSD_DefStat_onNewGame = "Sar";

	public static final boolean EXP = false;

	public static final String CLASS_MARK = "IIRT_Omega_ModPlugin";

	@Override
	public void onApplicationLoad() {
		try {
			hasLunaLib = Global.getSettings().getModManager().isModEnabled("lunalib");
		} catch (Throwable ignored) {
			hasLunaLib = false;
		}
		if (hasLunaLib) {
			PTSDModPluginAltLuna.registerSettingsListener();
			updateLunaSettings();
		}

		ShaderLib.init();

		if (ShaderLib.areShadersAllowed() && ShaderLib.areBuffersAllowed()) {
			LightData.readLightDataCSV("data/lights/IIRT_Omega_light_data.csv");
			TextureData.readTextureDataCSV("data/lights/IIRT_Omega_texture_data.csv");
		}
	}

	@Override
	public void onNewGame() {
		if (hasLunaLib) {
			PTSDModPluginAltLuna.registerSettingsListener();
			updateLunaSettings();
		}
		ProcgenUsedNames.notifyUsed("Unknown Neutron");
		ProcgenUsedNames.notifyUsed("Aleph Core");
		ProcgenUsedNames.notifyUsed(I18nUtil.getStarSystemsString("KRM_planet1_name"));
		ProcgenUsedNames.notifyUsed(I18nUtil.getStarSystemsString("KRM_planet2_name"));
		ProcgenUsedNames.notifyUsed(I18nUtil.getStarSystemsString("KRM_planet4_name"));
		ProcgenUsedNames.notifyUsed(I18nUtil.getStarSystemsString("KRM_planet3_name"));
		ProcgenUsedNames.notifyUsed(I18nUtil.getStarSystemsString("KRM_planet5_name"));

		ProcgenUsedNames.notifyUsed("Depravity Nodes");
		ProcgenUsedNames.notifyUsed("MandoLas");
		ProcgenUsedNames.notifyUsed(I18nUtil.getStarSystemsString("IIRT_planet1_name"));
		ProcgenUsedNames.notifyUsed(I18nUtil.getStarSystemsString("IIRT_planet2_name"));
		ProcgenUsedNames.notifyUsed(I18nUtil.getStarSystemsString("IIRT_planet3_name"));
		ProcgenUsedNames.notifyUsed(I18nUtil.getStarSystemsString("IIRT_planet5_name"));
		ProcgenUsedNames.notifyUsed(I18nUtil.getStarSystemsString("IIRT_planet6_name"));
		ProcgenUsedNames.notifyUsed(I18nUtil.getStarSystemsString("IIRT_planet7_name"));

		ProcgenUsedNames.notifyUsed("The Prevote Zone");
		ProcgenUsedNames.notifyUsed("Beaylon");
		ProcgenUsedNames.notifyUsed("Gisd09ui");
		ProcgenUsedNames.notifyUsed("A3las0c7sn");
		ProcgenUsedNames.notifyUsed("C8GS0sT013J");
		ProcgenUsedNames.notifyUsed("Csiu2k1IsP");

		afterNewGameLoad = false;
		onGameLoad(true);
		afterNewGameLoad = true;
	}

	boolean afterNewGameLoad = false;

	protected void registerCampaignPlugin(CampaignPlugin plugin) {
		Global.getSector().registerPlugin(plugin);
	}
	@Override
	public void onGameLoad(boolean newGame) {
		this.registerCampaignPlugin(new PTSD_CampaignPlugin());
		try {
			hasLunaLib = Global.getSettings().getModManager().isModEnabled("lunalib");
		} catch (Throwable ignored) {
			hasLunaLib = false;
		}
		boolean classLoad = false;
		if (hasLunaLib) {
			PTSDModPluginAltLuna.registerSettingsListener();
			updateLunaSettings();
		}
		if (Global.getSector().getPersistentData().containsKey(CLASS_MARK)) {
			classLoad = (Boolean)Global.getSector().getPersistentData().get(CLASS_MARK);
		}

		if (!classLoad) {
			Global.getSector().getPersistentData().put(CLASS_MARK, true);

			if (NEX()) {
				new IIRT_NEXGenerate().generate(Global.getSector());
			} else {
				new IIRT_NormalGenerate().generate(Global.getSector());
			}

			SharedData.getData().getPersonBountyEventData().addParticipatingFaction("KRM");
			SharedData.getData().getPersonBountyEventData().addParticipatingFaction("Omega_Watcher");
			SharedData.getData().getPersonBountyEventData().addParticipatingFaction("Omega_Psychasthenia");
			SharedData.getData().getPersonBountyEventData().addParticipatingFaction("IIRT");

			IIRT_Omega_Person.create();
		}

		syncInvasionScriptWithSettings();

		onDevModeF8Reload();

		if (afterNewGameLoad) {
			BarEventManager manager = BarEventManager.getInstance();

		}

		runIfNeed();
	}

	@Override
	public void onNewGameAfterEconomyLoad() {
		runIfNeed();

	}
	protected void runIfNeed(){
		SectorAPI sector = Global.getSector();
		GenericPluginManagerAPI plugins = sector.getGenericPlugins();
		if (!plugins.hasPlugin(PTSD_OmegaOfficerGeneratorPlugin.class)) {
			plugins.addPlugin(new PTSD_OmegaOfficerGeneratorPlugin(), true);
		}
		for (LocationAPI location : sector.getAllLocations()) {
			for (CampaignFleetAPI fleet : location.getFleets()) {
				PTSD_OmegaOfficerGeneratorPlugin.repairExistingFleet(fleet);
			}
		}
	}
	@Override
	public void onDevModeF8Reload() {
		installCrisisDevWatcher();
		PTSDCrisisDevIntel.sync();
		PTSDOccupationManager.syncMapVisibility();
	}

	public static boolean NEX() {
		return Global.getSettings().getModManager().isModEnabled("nexerelin");
	}

	public static final String modId = "Omega_PTSD";
	public static final String modIdDev = "Omega_PTSD";
	public static String getModId() {
		for (ModSpecAPI spec : Global.getSettings().getModManager().getEnabledModsCopy()) {
			if (spec != null && spec.getId().contains(modId) && spec.getId().contains("Dev")) {
				return modIdDev;
			}
		}
		return modId;
	}
	private static void updateLunaSettings() {
		if (!hasLunaLib) return;

		String PTSD = getModId();
		omega_invasion_enabled = PTSDModPluginAltLuna.getLunaBoolean(PTSD, "PTSD_omega_invasion_enabled", omega_invasion_enabled);
		start_stage_time = Math.max(1f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_start_stage_time", (int)start_stage_time));
		collect_data_time = Math.max(1f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_collect_data_time", (int)collect_data_time));
		invade_time = Math.max(1f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_invade_time", (int)invade_time));
		repair_time = Math.max(1f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_repair_time", (int)repair_time));
		scout_min_interval = Math.max(1f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_scout_min_interval", (int)scout_min_interval));
		scout_max_interval = Math.max(scout_min_interval, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_scout_max_interval", (int)scout_max_interval));
		scout_spawn_radius = Math.max(100f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_scout_spawn_radius", (int)scout_spawn_radius));
		max_guard_fleets = Math.max(0f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_max_guard_fleets", (int)max_guard_fleets));
final_invasion_max_strength = Math.max(1f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_final_invasion_max_strength", (int)final_invasion_max_strength));
		scout_max_active = Math.max(1f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_scout_max_active", (int)scout_max_active));
		warning_encounter_threshold = Math.max(1, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_warning_encounter_threshold", warning_encounter_threshold));
		strategic_update_interval = Math.max(1f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_strategic_update_interval", (int)strategic_update_interval));
		hidden_materialization_range = Math.max(1000f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_hidden_materialization_range", (int)hidden_materialization_range));
		expansion_interval = Math.max(3f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_expansion_interval", (int)expansion_interval));
		max_black_hole_fortresses = Math.max(0f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_max_black_hole_fortresses", (int)max_black_hole_fortresses));
		front_turn_min_interval = Math.max(2f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_front_turn_min_interval", (int)front_turn_min_interval));
		front_turn_max_interval = Math.max(front_turn_min_interval, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_front_turn_max_interval", (int)front_turn_max_interval));

		String defaultStage = PTSDModPluginAltLuna.getLunaString(PTSD, "PTSD_DefStat_onNewGame", PTSD_DefStat_onNewGame);
		if ("Sar".equals(defaultStage) || "Cod".equals(defaultStage) || "Inv".equals(defaultStage)
				|| "Rep".equals(defaultStage) || "FuA".equals(defaultStage) || "End".equals(defaultStage)) {
			PTSD_DefStat_onNewGame = defaultStage;
		} else {
			PTSD_DefStat_onNewGame = "Sar";
		}
	}

	public static void onLunaSettingsChanged(String changedModId) {
		if (!getModId().equals(changedModId)) return;
		updateLunaSettings();
		syncInvasionScriptWithSettings();
	}

	public static boolean isInvasionEnabled() {
		return OMEGA_PTSD_PREV && omega_invasion_enabled;
	}

	private static void syncInvasionScriptWithSettings() {
		SectorAPI sector = Global.getSector();
		if (sector != null) {
			installCrisisDevWatcher();
			PTSDCrisisDevIntel.sync();
			PTSDOccupationManager.syncMapVisibility();
		}
		if (sector == null || !isInvasionEnabled() || sector.hasScript(IIRT_Omega_Invasion.class)) return;

		Object savedStage = sector.getMemoryWithoutUpdate().get(IIRT_Omega_Invasion.stage_id);
		if (savedStage == IIRT_Omega_Invasion.STAGE.END || "END".equals(savedStage)) return;

		sector.addScript(new IIRT_Omega_Invasion());
	}

	private static void installCrisisDevWatcher() {
		SectorAPI sector = Global.getSector();
		if (sector != null && !sector.hasScript(PTSDCrisisDevWatcher.class)) {
			sector.addTransientScript(new PTSDCrisisDevWatcher());
		}
	}

	public static void addCampaignMessageInDev(String output,String log,Class cls){
		if(Global.getSettings().isDevMode()){
			output = "Dev: " + output;
			Global.getSector().getCampaignUI().addMessage(output);
		}
		if(log!=null){
			Global.getLogger(cls).info(log);
		}
	}
}