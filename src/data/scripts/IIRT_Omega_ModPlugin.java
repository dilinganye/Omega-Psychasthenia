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
import data.scripts.campaign.invasion.PTSDCrisisState;
import data.scripts.campaign.invasion.PTSDFortressEscapeScript;
import data.scripts.campaign.invasion.PTSDJeOtloesManager;
import data.scripts.campaign.invasion.PTSDOccupationManager;
import data.scripts.campaign.invasion.news.ft.FT02ObservatoryBarCreator;
import data.scripts.campaign.PTSD_CampaignPlugin;
import data.scripts.campaign.cargo.PTSD_OmegaOfficerGeneratorPlugin;
import data.scripts.util.PTSD_I18nUtil;
import data.scripts.world.IIRT_NEXGenerate;
import data.scripts.world.IIRT_NormalGenerate;
import data.scripts.world.IIRT_Omega_Person;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;

public class IIRT_Omega_ModPlugin extends BaseModPlugin {
	//public static final boolean OMEGA_PTSD_PREV = false; //决定是否开放实际内容！
	public static final boolean OMEGA_PTSD_PREV = true; //决定是否开放实际内容！

	public static boolean hasLunaLib = false;
	/**
	 * Release gate; set this directly for published builds. It is deliberately not a LunaLib
	 * setting, so player configuration can never unlock unfinished phases or developer panels.
	 */
	public static boolean OMEGA_PTSD_is_Alpha = false; // if it's true, then it will close those DEV UI.
	/** Last phase shipped by the current Alpha. Raise this as later content becomes releasable. */
	public static PTSDCrisisState.Phase OMEGA_PTSD_ALPHA_LAST_PHASE = PTSDCrisisState.Phase.WAR;
	/** Increment when an Alpha ceiling needs a new one-time old-save migration. */
	public static final int OMEGA_PTSD_ALPHA_GATE_REVISION = 1;
	public static boolean omega_invasion_enabled = true;
	public static float start_stage_time = 90f;
	public static float collect_data_time = 365f;
	public static float invade_time = 30f;
	public static float repair_time = 30f;
	public static float scout_min_interval = 10f;
	public static float scout_max_interval = 25f;
	public static float scout_spawn_radius = 300f;
	public static float max_guard_fleets = 10f;
	public static float final_invasion_max_strength = 200f;
	public static float scout_max_active = 3f;
	/** Dev-only recon controls; probabilities are mutually exclusive percentages. */
	public static float reconReportPlayerChance = 20f, reconReportThreeChance = 25f,
			reconReportTwoChance = 40f, reconReportInterval = 5f;
	public static float reconUploadMin = 1f, reconUploadMax = 3f;
	public static float reconRemoteError = .25f, reconRemoteLifetime = 45f,
			reconLowFleetThreshold = 30f;
	public static float reconEmptyRerouteMin = 2f, reconEmptyRerouteMax = 4f,
			reconPostPhaseIntervalMultiplier = 3f;
	public static float reconNewsDensity = 1f, expansionNewsDensity = 1f,
			fortressNewsDensity = 1f;
	public static float reconNewsFalseRate = .30f, expansionNewsFalseRate = .18f,
			fortressNewsFalseRate = .08f;
	public static int warning_encounter_threshold = 4;
	public static float strategic_update_interval = 7f;
	public static float hidden_materialization_range = 5000f;
	public static float expansion_interval = 14f;
	public static float max_black_hole_fortresses = 3f;
	public static float front_turn_min_interval = 6f;
	public static float front_turn_max_interval = 12f;
	public static float omega_fleet_strength_multiplier = 1f;
	/** Added to the two-level Omega officer baseline before phase and +/-2 variation. */
	public static int omegaOfficerLevelBonus = 6;
	public static float unknown_event_strength_flat = 0f;
	public static float unknown_event_frequency = 1f;
	public static float unknown_event_min_interval = 3f;
	public static float unknown_event_max_interval = 7f;
	/** Late-crisis controls. Values are refreshed from LunaLib and read at the next settlement. */
	public static float lateConstructionSpeed = 1f;
	public static float lateProductionMultiplier = 1f;
	public static float lateOffensiveTempo = 1f;
	public static float latePlayerResponse = 1f;
	public static float lateAlertDensity = 1f;
	public static float lateProductionInterval = 10f;
	public static float lateBaseIncome = 4f;
	public static float lateOutpostIncome = 2f;
	public static float[] lateFortressIncome = {0f, 1f, 2f, 3f, 5f};
	public static float lateProductionPerTurnCap = 20f;
	public static float lateProductionStorageCap = 200f;
	public static float lateMobilizationThreshold = 60f;
	public static float lateSurpriseThreshold = 120f;
	public static float lateSurpriseMinimum = 45f;
	public static float lateFortificationDeadline = 180f;
	public static float[] lateFortressWork = {0f, 20f, 30f, 40f, 50f};
	public static float[] lateTransportInterval = {0f, 8f, 7f, 5f, 5f};
	public static float lateEventMajorCooldown = 5f;
	public static float lateEventShortCooldown = 3f;
	public static float lateEventTaskDeadline = 12f;
	public static float lateFrontStrikeMin = 3f;
	public static float lateFrontStrikeMax = 5f;
	public static boolean phase_dormant_enabled = true;
	public static boolean phase_recon_enabled = true;
	public static boolean phase_expansion_enabled = true;
	public static boolean phase_fortification_enabled = true;
	public static boolean phase_mobilization_enabled = true;
	public static boolean phase_war_enabled = true;
	public static String PTSD_DefStat_onNewGame = "Sar";

	public static final boolean EXP = false;

	public static final String CLASS_MARK = "IIRT_Omega_ModPlugin";

	@Override
	public void onApplicationLoad() {
		data.scripts.campaign.invasion.PTSDCrisisNewsAPI.registerBuiltinHandlers();
		try {
			hasLunaLib = Global.getSettings().getModManager().isModEnabled("lunalib");
		} catch (Throwable ignored) {
			hasLunaLib = false;
		}
		if (hasLunaLib) {
			PTSDModPluginAltLuna.registerSettingsListener();
			PTSDLunaConfigVisibility.sync();
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
			PTSDLunaConfigVisibility.sync();
			updateLunaSettings();
		}
		ProcgenUsedNames.notifyUsed("Unknown Neutron");
		ProcgenUsedNames.notifyUsed("Aleph Core");
		ProcgenUsedNames.notifyUsed(PTSD_I18nUtil.getStarSystemsString("KRM_planet1_name"));
		ProcgenUsedNames.notifyUsed(PTSD_I18nUtil.getStarSystemsString("KRM_planet2_name"));
		ProcgenUsedNames.notifyUsed(PTSD_I18nUtil.getStarSystemsString("KRM_planet4_name"));
		ProcgenUsedNames.notifyUsed(PTSD_I18nUtil.getStarSystemsString("KRM_planet3_name"));
		ProcgenUsedNames.notifyUsed(PTSD_I18nUtil.getStarSystemsString("KRM_planet5_name"));

		ProcgenUsedNames.notifyUsed("Depravity Nodes");
		ProcgenUsedNames.notifyUsed("MandoLas");
		ProcgenUsedNames.notifyUsed(PTSD_I18nUtil.getStarSystemsString("IIRT_planet1_name"));
		ProcgenUsedNames.notifyUsed(PTSD_I18nUtil.getStarSystemsString("IIRT_planet2_name"));
		ProcgenUsedNames.notifyUsed(PTSD_I18nUtil.getStarSystemsString("IIRT_planet3_name"));
		ProcgenUsedNames.notifyUsed(PTSD_I18nUtil.getStarSystemsString("IIRT_planet5_name"));
		ProcgenUsedNames.notifyUsed(PTSD_I18nUtil.getStarSystemsString("IIRT_planet6_name"));
		ProcgenUsedNames.notifyUsed(PTSD_I18nUtil.getStarSystemsString("IIRT_planet7_name"));

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
		data.scripts.campaign.invasion.PTSDCrisisNewsAPI.registerBuiltinHandlers();
		this.registerCampaignPlugin(new PTSD_CampaignPlugin());
		if (!Global.getSector().hasScript(PTSDFortressEscapeScript.class)) {
			Global.getSector().addScript(new PTSDFortressEscapeScript());
		}
		try {
			hasLunaLib = Global.getSettings().getModManager().isModEnabled("lunalib");
		} catch (Throwable ignored) {
			hasLunaLib = false;
		}
		boolean classLoad = false;
		if (hasLunaLib) {
			PTSDModPluginAltLuna.registerSettingsListener();
			PTSDLunaConfigVisibility.sync();
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
			if (!manager.hasEventCreator(FT02ObservatoryBarCreator.class))
				manager.addEventCreator(new FT02ObservatoryBarCreator());

		}

		runIfNeed();
	}

	@Override
	public void onNewGameAfterEconomyLoad() {
		runIfNeed();
		BarEventManager manager = BarEventManager.getInstance();
		if (!manager.hasEventCreator(FT02ObservatoryBarCreator.class))
			manager.addEventCreator(new FT02ObservatoryBarCreator());

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
		omega_invasion_enabled = PTSDModPluginAltLuna.getLunaBoolean(PTSD,
				"PTSD_omega_invasion_enabled", omega_invasion_enabled);
		unknown_event_frequency = Math.max(0.1f, Math.min(5f,
				PTSDModPluginAltLuna.getLunaFloat(PTSD, "PTSD_unknown_event_frequency", unknown_event_frequency)));
		unknown_event_min_interval = Math.max(1f,
				PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_unknown_event_min_interval", (int) unknown_event_min_interval));
		unknown_event_max_interval = Math.max(unknown_event_min_interval,
				PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_unknown_event_max_interval", (int) unknown_event_max_interval));
		unknown_event_strength_flat = Math.max(0f, Math.min(200f,
				PTSDModPluginAltLuna.getLunaFloat(PTSD, "PTSD_unknown_event_strength_flat", unknown_event_strength_flat)));
		omega_fleet_strength_multiplier = Math.max(0.5f, Math.min(10f,
				PTSDModPluginAltLuna.getLunaFloat(PTSD, "PTSD_unknown_event_strength_multiplier", omega_fleet_strength_multiplier)));
		omegaOfficerLevelBonus = lateInt(PTSD, "PTSD_omega_officer_level_bonus", 6, 0, 12);
		lateConstructionSpeed = lateFloat(PTSD, "PTSD_late_construction_speed", 1f, .5f, 2f);
		lateProductionMultiplier = lateFloat(PTSD, "PTSD_late_production_multiplier", 1f, .5f, 2f);
		lateOffensiveTempo = lateFloat(PTSD, "PTSD_late_offensive_tempo", 1f, .5f, 2f);
		latePlayerResponse = lateFloat(PTSD, "PTSD_late_player_response", 1f, .5f, 2f);
		lateAlertDensity = lateFloat(PTSD, "PTSD_late_alert_density", 1f, .5f, 2f);

		// The public event bounds also govern scout appearances so all unknown activity shares one cadence control.
		scout_min_interval = unknown_event_min_interval;
		scout_max_interval = unknown_event_max_interval;
		if (Global.getSettings() == null || !Global.getSettings().isDevMode()) {
			resetReconDevDefaults();
			lateProductionInterval = 10f;
			lateBaseIncome = 4f;
			lateOutpostIncome = 2f;
			lateFortressIncome = new float[] {0f, 1f, 2f, 3f, 5f};
			lateProductionPerTurnCap = 20f;
			lateProductionStorageCap = 200f;
			lateMobilizationThreshold = 60f;
			lateSurpriseThreshold = 120f;
			lateSurpriseMinimum = 45f;
			lateFortificationDeadline = 180f;
			lateFortressWork = new float[] {0f, 20f, 30f, 40f, 50f};
			lateTransportInterval = new float[] {0f, 8f, 7f, 5f, 5f};
			lateEventMajorCooldown = 5f;
			lateEventShortCooldown = 3f;
			lateEventTaskDeadline = 12f;
			lateFrontStrikeMin = 3f;
			lateFrontStrikeMax = 5f;
			return;
		}
		int playerReport = lateInt(PTSD, "PTSD_recon_report_player", 20, 0, 100);
		int threeReport = lateInt(PTSD, "PTSD_recon_report_three", 25, 0, 100);
		int twoReport = lateInt(PTSD, "PTSD_recon_report_two", 40, 0, 100);
		reconReportPlayerChance = playerReport;
		reconReportThreeChance = Math.min(threeReport, 100 - playerReport);
		reconReportTwoChance = Math.min(twoReport, 100 - playerReport - reconReportThreeChance);
		reconReportInterval = lateInt(PTSD, "PTSD_recon_report_interval", 5, 1, 20);
		reconUploadMin = lateInt(PTSD, "PTSD_recon_upload_min", 1, 1, 10);
		reconUploadMax = Math.max(reconUploadMin, lateInt(PTSD, "PTSD_recon_upload_max", 3, 1, 15));
		reconRemoteError = lateInt(PTSD, "PTSD_recon_remote_error", 25, 0, 60) / 100f;
		reconRemoteLifetime = lateInt(PTSD, "PTSD_recon_remote_lifetime", 45, 5, 180);
		reconLowFleetThreshold = lateInt(PTSD, "PTSD_recon_low_fleet_fp", 30, 0, 150);
		reconEmptyRerouteMin = lateInt(PTSD, "PTSD_recon_empty_reroute_min", 2, 1, 15);
		reconEmptyRerouteMax = Math.max(reconEmptyRerouteMin,
				lateInt(PTSD, "PTSD_recon_empty_reroute_max", 4, 1, 20));
		reconPostPhaseIntervalMultiplier = lateInt(PTSD, "PTSD_recon_post_phase_multiplier", 3, 1, 10);
		reconNewsDensity = lateInt(PTSD, "PTSD_news_recon_density", 100, 10, 300) / 100f;
		expansionNewsDensity = lateInt(PTSD, "PTSD_news_expansion_density", 100, 10, 300) / 100f;
		fortressNewsDensity = lateInt(PTSD, "PTSD_news_fortress_density", 100, 10, 300) / 100f;
		reconNewsFalseRate = lateInt(PTSD, "PTSD_news_recon_false", 30, 0, 60) / 100f;
		expansionNewsFalseRate = Math.min(reconNewsFalseRate,
				lateInt(PTSD, "PTSD_news_expansion_false", 18, 0, 50) / 100f);
		fortressNewsFalseRate = Math.min(expansionNewsFalseRate,
				lateInt(PTSD, "PTSD_news_fortress_false", 8, 0, 35) / 100f);
		lateProductionInterval = lateInt(PTSD, "PTSD_late_production_interval", 10, 2, 30);
		lateBaseIncome = lateInt(PTSD, "PTSD_late_base_income", 4, 1, 20);
		lateOutpostIncome = lateInt(PTSD, "PTSD_late_outpost_income", 2, 0, 10);
		lateFortressIncome = new float[] {0f,
				lateInt(PTSD, "PTSD_late_small_income", 1, 0, 10),
				lateInt(PTSD, "PTSD_late_stable_income", 2, 0, 10),
				lateInt(PTSD, "PTSD_late_medium_income", 3, 0, 10),
				lateInt(PTSD, "PTSD_late_eden_income", 5, 0, 15)};
		lateProductionPerTurnCap = lateInt(PTSD, "PTSD_late_turn_cap", 20, 1, 100);
		lateProductionStorageCap = Math.max(lateProductionPerTurnCap,
				lateInt(PTSD, "PTSD_late_storage_cap", 200, 45, 500));
		lateMobilizationThreshold = lateInt(PTSD, "PTSD_late_mobilization_threshold", 60, 15, 200);
		lateSurpriseThreshold = Math.max(lateMobilizationThreshold + 5f,
				lateInt(PTSD, "PTSD_late_surprise_threshold", 120, 30, 300));
		lateSurpriseMinimum = Math.min(lateSurpriseThreshold,
				lateInt(PTSD, "PTSD_late_surprise_minimum", 45, 10, 150));
		lateFortificationDeadline = lateInt(PTSD, "PTSD_late_fortification_deadline", 180, 30, 600);
		lateFortressWork = new float[] {0f,
				lateInt(PTSD, "PTSD_late_small_work", 20, 5, 100),
				lateInt(PTSD, "PTSD_late_stable_work", 30, 5, 150),
				lateInt(PTSD, "PTSD_late_medium_work", 40, 5, 200), 50f};
		lateTransportInterval = new float[] {0f,
				lateInt(PTSD, "PTSD_late_small_transport", 8, 2, 30),
				lateInt(PTSD, "PTSD_late_stable_transport", 7, 2, 30),
				lateInt(PTSD, "PTSD_late_medium_transport", 5, 2, 30), 5f};
		lateEventMajorCooldown = lateInt(PTSD, "PTSD_late_major_card_cooldown", 5, 1, 30);
		lateEventShortCooldown = lateInt(PTSD, "PTSD_late_short_card_cooldown", 3, 1, 30);
		lateEventTaskDeadline = lateInt(PTSD, "PTSD_late_task_deadline", 12, 3, 40);
		lateFrontStrikeMin = lateInt(PTSD, "PTSD_late_strike_min", 3, 1, 15);
		lateFrontStrikeMax = Math.max(lateFrontStrikeMin,
				lateInt(PTSD, "PTSD_late_strike_max", 5, 1, 20));

		start_stage_time = Math.max(1f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_start_stage_time", (int) start_stage_time));
		collect_data_time = Math.max(1f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_collect_data_time", (int) collect_data_time));
		invade_time = Math.max(1f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_invade_time", (int) invade_time));
		repair_time = Math.max(1f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_repair_time", (int) repair_time));
		scout_spawn_radius = Math.max(100f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_scout_spawn_radius", (int) scout_spawn_radius));
		max_guard_fleets = Math.max(0f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_max_guard_fleets", (int) max_guard_fleets));
		final_invasion_max_strength = Math.max(1f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_final_invasion_max_strength", (int) final_invasion_max_strength));
		scout_max_active = Math.max(1f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_scout_max_active", (int) scout_max_active));
		warning_encounter_threshold = Math.max(1, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_warning_encounter_threshold", warning_encounter_threshold));
		strategic_update_interval = Math.max(1f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_strategic_update_interval", (int) strategic_update_interval));
		hidden_materialization_range = Math.max(1000f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_hidden_materialization_range", (int) hidden_materialization_range));
		expansion_interval = Math.max(3f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_expansion_interval", (int) expansion_interval));
		max_black_hole_fortresses = Math.max(0f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_max_black_hole_fortresses", (int) max_black_hole_fortresses));
		front_turn_min_interval = Math.max(2f, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_front_turn_min_interval", (int) front_turn_min_interval));
		front_turn_max_interval = Math.max(front_turn_min_interval, PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_front_turn_max_interval", (int) front_turn_max_interval));
		phase_dormant_enabled = PTSDModPluginAltLuna.getLunaBoolean(PTSD, "PTSD_phase_dormant_enabled", phase_dormant_enabled);
		phase_recon_enabled = PTSDModPluginAltLuna.getLunaBoolean(PTSD, "PTSD_phase_recon_enabled", phase_recon_enabled);
		phase_expansion_enabled = PTSDModPluginAltLuna.getLunaBoolean(PTSD, "PTSD_phase_expansion_enabled", phase_expansion_enabled);
		phase_fortification_enabled = PTSDModPluginAltLuna.getLunaBoolean(PTSD, "PTSD_phase_fortification_enabled", phase_fortification_enabled);
		phase_mobilization_enabled = phase_fortification_enabled;
		phase_war_enabled = PTSDModPluginAltLuna.getLunaBoolean(PTSD, "PTSD_phase_war_enabled", phase_war_enabled);

		String defaultStage = PTSDModPluginAltLuna.getLunaString(PTSD, "PTSD_DefStat_onNewGame", PTSD_DefStat_onNewGame);
		if ("Sar".equals(defaultStage) || "Cod".equals(defaultStage) || "Inv".equals(defaultStage)
				|| "Rep".equals(defaultStage) || "FuA".equals(defaultStage) || "End".equals(defaultStage)) {
			PTSD_DefStat_onNewGame = defaultStage;
		} else {
			PTSD_DefStat_onNewGame = "Sar";
		}
	}
	private static float lateFloat(String modId, String id, float fallback, float min, float max) {
		return Math.max(min, Math.min(max, PTSDModPluginAltLuna.getLunaFloat(modId, id, fallback)));
	}
	private static void resetReconDevDefaults() {
		reconReportPlayerChance = 20f; reconReportThreeChance = 25f; reconReportTwoChance = 40f;
		reconReportInterval = 5f; reconUploadMin = 1f; reconUploadMax = 3f;
		reconRemoteError = .25f; reconRemoteLifetime = 45f; reconLowFleetThreshold = 30f;
		reconEmptyRerouteMin = 2f; reconEmptyRerouteMax = 4f;
		reconPostPhaseIntervalMultiplier = 3f;
		reconNewsDensity = 1f; expansionNewsDensity = 1f; fortressNewsDensity = 1f;
		reconNewsFalseRate = .30f; expansionNewsFalseRate = .18f; fortressNewsFalseRate = .08f;
	}
	private static int lateInt(String modId, String id, int fallback, int min, int max) {
		return Math.max(min, Math.min(max, PTSDModPluginAltLuna.getLunaInt(modId, id, fallback)));
	}

	public static void reloadLunaSettingsAfterVisibilityChange() {
		updateLunaSettings();
	}
	public static void onLunaSettingsChanged(String changedModId) {
		if (!getModId().equals(changedModId)) return;
		PTSDLunaConfigVisibility.sync();
		updateLunaSettings();
		syncInvasionScriptWithSettings();
	}

	/** The Pandora switch becomes save-latched once opened outside DevMode. */
	public static boolean isInvasionEnabled() {
		if (!OMEGA_PTSD_PREV) return false;
		SectorAPI sector = Global.getSector();
		if (sector == null || Global.getSettings().isDevMode()) return omega_invasion_enabled;
		PTSDCrisisState state = PTSDCrisisState.get();
		if (state == null) return omega_invasion_enabled;
		if (!state.pandoraInitialized) {
			state.pandoraInitialized = true;
			state.pandoraOpened = omega_invasion_enabled;
		} else if (omega_invasion_enabled) {
			state.pandoraOpened = true;
		}
		return state.pandoraOpened;
	}

	public static boolean isPhaseEnabled(PTSDCrisisState.Phase phase) {
		if (!isPhaseAvailableInBuild(phase)) return false;
		if (Global.getSettings() == null || !Global.getSettings().isDevMode()) return true;
		if (phase == PTSDCrisisState.Phase.DORMANT) return phase_dormant_enabled;
		if (phase == PTSDCrisisState.Phase.RECON) return phase_recon_enabled;
		if (phase == PTSDCrisisState.Phase.EXPANSION) return phase_expansion_enabled;
		if (phase == PTSDCrisisState.Phase.FORTIFICATION) return phase_fortification_enabled;
		if (phase == PTSDCrisisState.Phase.MOBILIZATION) return phase_mobilization_enabled;
		if (phase == PTSDCrisisState.Phase.WAR) return phase_war_enabled;
		return true;
	}

	/** Build-level content gate. Unlike Dev phase toggles, this also applies in normal play. */
	public static boolean isPhaseAvailableInBuild(PTSDCrisisState.Phase phase) {
		if (phase == null) return false;
		if (!OMEGA_PTSD_is_Alpha || phase == PTSDCrisisState.Phase.ENDED) return true;
		PTSDCrisisState.Phase last = OMEGA_PTSD_ALPHA_LAST_PHASE;
		if (last == null || last == PTSDCrisisState.Phase.ENDED) return true;
		return phase.ordinal() <= last.ordinal();
	}

	public static PTSDCrisisState.Phase getLatestReleasedPhase() {
		if (!OMEGA_PTSD_is_Alpha || OMEGA_PTSD_ALPHA_LAST_PHASE == null ||
				OMEGA_PTSD_ALPHA_LAST_PHASE == PTSDCrisisState.Phase.ENDED) {
			return PTSDCrisisState.Phase.WAR;
		}
		return OMEGA_PTSD_ALPHA_LAST_PHASE;
	}

	private static void syncInvasionScriptWithSettings() {
		SectorAPI sector = Global.getSector();
		if (sector != null) {
			installCrisisDevWatcher();
			PTSDCrisisDevIntel.sync();
			PTSDOccupationManager.syncMapVisibility();
			installJeOtloesManager();
		}
		if (sector == null || !isInvasionEnabled() || sector.hasScript(IIRT_Omega_Invasion.class)) return;

		Object savedStage = sector.getMemoryWithoutUpdate().get(IIRT_Omega_Invasion.stage_id);
		if (savedStage == IIRT_Omega_Invasion.STAGE.END || "END".equals(savedStage)) return;

		sector.addScript(new IIRT_Omega_Invasion());
	}

	private static void installJeOtloesManager() {
		SectorAPI sector = Global.getSector();
		if (sector == null) return;
		if (!sector.hasScript(PTSDJeOtloesManager.class)) {
			PTSDJeOtloesManager manager = new PTSDJeOtloesManager();
			sector.addTransientScript(manager);
			sector.getListenerManager().addListener(manager, true);
		} else if (!sector.getListenerManager().hasListenerOfClass(PTSDJeOtloesManager.class)) {
			sector.getListenerManager().addListener(new PTSDJeOtloesManager(), true);
		}
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
