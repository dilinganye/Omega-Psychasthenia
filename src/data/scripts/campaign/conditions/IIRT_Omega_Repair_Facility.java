package data.scripts.campaign.conditions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.IIRT_Omega_ModPlugin;

public class IIRT_Omega_Repair_Facility extends BaseMarketConditionPlugin {

	protected float elapsed = 0;
	protected static final float qualityBonus = 300f;

	@Override
	public void apply(String id) {
		market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).modifyFlat(id, qualityBonus * (elapsed / IIRT_Omega_ModPlugin.repair_time), "Secret Omega Repair Facility");
	}

	@Override
	public void unapply(String id) {
		market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).unmodify(id);
	}

	@Override
	public void advance(float amount) {
		if (!IIRT_Omega_ModPlugin.isInvasionEnabled()) return;
		elapsed += Global.getSector().getClock().convertToDays(amount);
		if (elapsed >= IIRT_Omega_ModPlugin.repair_time) elapsed = IIRT_Omega_ModPlugin.repair_time;
	}
}
