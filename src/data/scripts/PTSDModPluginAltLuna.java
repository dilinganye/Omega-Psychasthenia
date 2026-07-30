/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.scripts;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;

/**
 * @author 49747
 */
public class PTSDModPluginAltLuna {

    public static boolean getLunaBoolean(String modID, String fieldId, boolean fallback) {
        Boolean value = LunaSettings.getBoolean(modID, fieldId);
        return value != null ? value : fallback;
    }

    public static int getLunaInt(String modID, String fieldId, int fallback) {
        Integer value = LunaSettings.getInt(modID, fieldId);
        return value != null ? value : fallback;
    }

    public static String getLunaString(String modID, String fieldId, String fallback) {
        String value = LunaSettings.getString(modID, fieldId);
        return value != null ? value : fallback;
    }

    public static void registerSettingsListener() {
        if (!LunaSettings.hasSettingsListenerOfClass(PTSDLunaSettingsListener.class)) {
            LunaSettings.addSettingsListener(new PTSDLunaSettingsListener());
        }
    }

    private static final class PTSDLunaSettingsListener implements LunaSettingsListener {
        @Override
        public void settingsChanged(String modID) {
            IIRT_Omega_ModPlugin.onLunaSettingsChanged(modID);
        }
    }
}
