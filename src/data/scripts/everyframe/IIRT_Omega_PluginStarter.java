package data.scripts.everyframe;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.IIRT_Omega_ModPlugin;
import exerelin.utilities.NexConfig;
import exerelin.utilities.NexFactionConfig;
import exerelin.utilities.NexFactionConfig.StartFleetSet;
import exerelin.utilities.NexFactionConfig.StartFleetType;

import java.util.ArrayList;
import java.util.List;

public class IIRT_Omega_PluginStarter extends BaseEveryFrameCombatPlugin {

	private static boolean addedOnce = false;
	private static boolean checkedOncePerCombat = false;

	@Override
	public void advance(float amount, List<InputEventAPI> events) {
		if (IIRT_Omega_ModPlugin.NEX()) {
			if (checkedOncePerCombat) {
				if (!addedOnce && Global.getCurrentState() == GameState.TITLE && (Global.getSettings().getMissionScore("Omega_PersonalTailor") > 75)) {
					NexFactionConfig faction = NexConfig.getFactionConfig("KRM");
					StartFleetSet fleetSet = faction.getStartFleetSet(StartFleetType.SUPER.name());
					List<String> excelsiorFleet = new ArrayList<>();
					excelsiorFleet.add("IIRT_Omega_Cipher_Beam");
					fleetSet.addFleet(excelsiorFleet);

					addedOnce = true;
				}
				checkedOncePerCombat = false;
			}
		}
	}

	@Override
	public void init(CombatEngineAPI engine) {
		checkedOncePerCombat = true;
	}
}
