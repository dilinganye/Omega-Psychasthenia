//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package data.scripts.util;

public class Iirt_Omega_Misc {

	public Iirt_Omega_Misc() {
	}

	public static String getDigitValue(float value) {
		return getDigitValue(value, 1);
	}

	public static String getDigitValue(float value, int digit) {
		return Math.abs((float)Math.round(value) - value) < 1.0E-4F ? String.format("%d", Math.round(value)) : String.format("%." + digit + "f", value);
	}
}
