package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class IIRT_Telson extends BaseHullMod {

	public static final String Weapon_Used = "WS0001";
	public static final String Weapon_Main_Used = "WS0001";

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

		if (stats.getEntity() == null) {
			return;
		} else {
			// clear slot
			stats.getVariant().clearSlot(Weapon_Used);
			// add gun
			stats.getVariant().addWeapon(Weapon_Used, Weapon_Main_Used);
		}
	}

}
