package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import data.scripts.campaign.IIRT_Omega_Invasion;
import data.scripts.campaign.PTSD_CampaignPlugin;
import data.scripts.campaign.cargo.PTSD_OmegaOfficerGeneratorPlugin;
import data.scripts.world.IIRT_Omega_Person;
import data.utils.iirt_omega.I18nUtil;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;

public class IIRT_Omega_ModPlugin extends BaseModPlugin {
	//public static final boolean OMEGA_PTSD_PREV = false; //决定是否开放实际内容！
	public static final boolean OMEGA_PTSD_PREV = true; //决定是否开放实际内容！

	public static boolean hasLunaLib = false;
	public static boolean doNotUpdateLunaSettingsOnNewGame = false;
	public static boolean omega_invasion_enabled = true;
	public static float start_stage_time = 15f;
	public static float collect_data_time = 60f;
	public static float invade_time = 30f;
	public static float repair_time = 30f;
	public static float scout_min_interval = 10f;
	public static float scout_max_interval = 25f;
	public static float scout_spawn_radius = 300f;
	public static float max_guard_fleets = 10f;
	public static float final_invasion_max_strength = 200f;
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
		if (hasLunaLib && !doNotUpdateLunaSettingsOnNewGame) {
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
		if (hasLunaLib && !doNotUpdateLunaSettingsOnNewGame) {
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

			// Check if Omega invasion is enabled (via LunaLib or DevMode)
			boolean omegaInvasionEnabled = false;
			if(omega_invasion_enabled){
				omegaInvasionEnabled = true;
			}

			// Allow DevMode to always enable invasion for testing anyway
			if (Global.getSettings().isDevMode() && OMEGA_PTSD_PREV) {
				omegaInvasionEnabled = true;
			}

			// Only load invasion system if enabled
			if (omegaInvasionEnabled) {
				Global.getSector().addScript(new IIRT_Omega_Invasion());
			}

			IIRT_Omega_Person.create();
		}

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
	}
	// Helper method to read LunaLib boolean settings
	private Boolean getLunaBoolean(String modID, String fieldID) {
		try {
			Class<?> c = Class.forName("org.magiclib.LunaWrapper");
			java.lang.reflect.Method m = c.getMethod("getBoolean", String.class, String.class);
			Object res = m.invoke(null, modID, fieldID);
			return (Boolean) res;
		} catch (Throwable t) {
			return null;
		}
	}

	@Override
	public void onDevModeF8Reload() {

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
		String PTSD = getModId();
		omega_invasion_enabled = PTSDModPluginAltLuna.getLunaBoolean(PTSD, "PTSD_omega_invasion_enabled");
		start_stage_time = PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_start_stage_time");
		collect_data_time = PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_collect_data_time");
		invade_time = PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_invade_time");
		repair_time = PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_repair_time");
		scout_min_interval = PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_scout_min_interval");
		scout_max_interval = PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_scout_max_interval");
		scout_spawn_radius = PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_scout_spawn_radius");
		max_guard_fleets = PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_max_guard_fleets");
		final_invasion_max_strength = PTSDModPluginAltLuna.getLunaInt(PTSD, "PTSD_final_invasion_max_strength");
		doNotUpdateLunaSettingsOnNewGame = PTSDModPluginAltLuna.getLunaBoolean(PTSD, "PTSD_Future");

		PTSD_DefStat_onNewGame = PTSDModPluginAltLuna.getLunaString(PTSD, "PTSD_DefStat_onNewGame");

		if (scout_min_interval > scout_max_interval) {
			scout_max_interval = scout_min_interval + 20;
		}
		/*if (BarShopSpecialItemSaleRelNeed < -1) {
			BarShopSpecialItemSaleRelNeed = -1;
		}

		 */

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