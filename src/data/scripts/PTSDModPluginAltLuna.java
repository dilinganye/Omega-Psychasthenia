/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.scripts;

import lunalib.lunaSettings.LunaSettings;

/**
 * @author 49747
 */
public class PTSDModPluginAltLuna {

    public static boolean getLunaBoolean(String modID, String fieldId) {

        return LunaSettings.getBoolean(modID, fieldId);
    }

    public static int getLunaInt(String modID, String fieldId) {
        return LunaSettings.getInt(modID, fieldId);
    }
    public static String getLunaString(String modID, String fieldId) {
        return LunaSettings.getString(modID, fieldId);
    }

    public static float getLunaIntToFloat(String modId, String filedId, float mult) {
        return getLunaInt(modId, filedId) * mult;
    }

}
