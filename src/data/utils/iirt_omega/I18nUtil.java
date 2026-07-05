//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package data.utils.iirt_omega;

import com.fs.starfarer.api.Global;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lwjgl.util.vector.Vector2f;

public class I18nUtil {

	private static final String CATE_SHIP_SYSTEM = "shipSystem";
	private static final String CATE_STAR_SYSTEMS = "starSystems";
	private static final String CATE_HULL_MOD = "hullMod";

	public I18nUtil() {
	}

	public static String getString(String category, String id) {
		return Global.getSettings().getString(category, id);
	}

	public static String getShipSystemString(String id) {
		return getString("shipSystem", id);
	}

	public static String getStarSystemsString(String id) {
		return getString("starSystems", id);
	}

	public static String getHullModString(String id) {
		return getString("hullMod", id);
	}

	public static void easyRippleOut(Vector2f location, Vector2f velocity, float size, float intensity, float fadesize, float frameRate) {
		if (intensity == -1f) {
			intensity = size / 3f;
		}
		if (velocity == null) {
			velocity = nv;
		}
		RippleDistortion ripple = new RippleDistortion(location, velocity);
		ripple.setSize(size);
		ripple.setIntensity(intensity);
		ripple.setFrameRate(frameRate);
		ripple.fadeInSize(fadesize);
		if(fadesize>=100){
			fadesize = 100f;
		}
		ripple.fadeOutIntensity(fadesize);

		DistortionShader.addDistortion(ripple);
	}
	public static void easyRippleOut(Vector2f location, Vector2f velocity, float size, float intensity, float fadeInSize, float fadeOutSize, float frameRate) {
		if(location == null || velocity == null ) {
			return;
		}
		if (intensity == -1f) {
			intensity = size / 3f;
		}
		if (velocity == null) {
			velocity = nv;
		}
		RippleDistortion ripple = new RippleDistortion(location, velocity);
		ripple.setSize(size);
		ripple.setIntensity(intensity);
		ripple.setFrameRate(frameRate);
		ripple.fadeInSize(fadeInSize);
		ripple.fadeOutSize(fadeOutSize);
		if(fadeInSize>=100){
			fadeInSize = 100f;
		}
		ripple.fadeOutIntensity(fadeInSize);

		DistortionShader.addDistortion(ripple);
	}
	public static final Vector2f nv = new Vector2f();
}
