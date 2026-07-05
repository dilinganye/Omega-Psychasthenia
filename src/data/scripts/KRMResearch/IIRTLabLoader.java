package data.scripts.KRMResearch;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.SettingsAPI;
import data.scripts.KRMResearch.IIRTLabProject.REWARD_TYPE;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class IIRTLabLoader {

	public static List<IIRTLabInstitute> loadInstitutes() {
		List<IIRTLabInstitute> institutes = new ArrayList<>();
		SettingsAPI settings = Global.getSettings();
		for (ModSpecAPI mod : settings.getModManager().getEnabledModsCopy()) {
			JSONArray csvData;
			try {
				csvData = settings.loadCSV("data/config/modFiles/KRM/KRMInstitutes.csv", mod.getId());
			} catch (Exception e) {
				continue;
			}
			for (int i = 0; i < csvData.length(); i++) {
				try {
					JSONObject row = csvData.getJSONObject(i);
					IIRTLabInstitute institute = new IIRTLabInstitute(row.getString("id"), row.getString("name"), row.getString("desc"), row.getString("spriteName"), row.getInt("baseSpeed"));
					institute.setProjects(loadProjects(institute.getId()));
					institutes.add(institute);
				} catch (Exception e) {
					continue;
				}
			}
		}

		return institutes;
	}

	public static List<IIRTLabProject> loadProjects(String Insititue) {
		List<IIRTLabProject> projects = new ArrayList<>();
		SettingsAPI settings = Global.getSettings();
		for (ModSpecAPI mod : settings.getModManager().getEnabledModsCopy()) {
			JSONArray csvData;
			try {
				csvData = settings.loadCSV("data/config/modFiles/KRM/KRMProjects.csv", mod.getId());
			} catch (Exception e) {
				continue;
			}
			for (int i = 0; i < csvData.length(); i++) {
				try {
					JSONObject row = csvData.getJSONObject(i);
					if (Insititue.contentEquals(row.getString("institute"))) {
						projects.add(new IIRTLabProject(row.getString("id"), row.getInt("progress"), row.getBoolean("repeatable"), row.getInt("tier"), (float)row.getDouble("rarity"), row.getString("reward"), row.getBoolean("withBP"), REWARD_TYPE.valueOf(row.getString("type")), row.getBoolean("competitive")));
					}
				} catch (Exception e) {
					continue;
				}
			}
		}

		return projects;
	}
}
