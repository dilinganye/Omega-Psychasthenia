//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package data.scripts.util;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.Map;

public class HullModUtil {

	public HullModUtil() {
	}

	public static String getHullSizeFlatString(Map<ShipAPI.HullSize, Float> map) {
		return "" + map.get(HullSize.FRIGATE).intValue() + "/" + map.get(HullSize.DESTROYER).intValue() + "/" + map.get(HullSize.CRUISER).intValue() + "/" + map.get(HullSize.CAPITAL_SHIP).intValue() + "";
	}

	public static String getHullSizePercentString(Map<ShipAPI.HullSize, Float> map) {
		return Iirt_Omega_Misc.getDigitValue(map.get(HullSize.FRIGATE)) + "%/" + Iirt_Omega_Misc.getDigitValue(map.get(HullSize.DESTROYER)) + "%/" + Iirt_Omega_Misc.getDigitValue(map.get(HullSize.CRUISER)) + "%/" + Iirt_Omega_Misc.getDigitValue(map.get(HullSize.CAPITAL_SHIP)) + "%";
	}
}
