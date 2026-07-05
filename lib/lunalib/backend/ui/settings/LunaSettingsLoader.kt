package lunalib.backend.ui.settings

import com.fs.starfarer.api.Global
import org.apache.log4j.Level
import org.json.JSONArray
import org.lazywizard.lazylib.JSONUtils
import org.lazywizard.lazylib.MathUtils

data class LunaSettingsData(
    val modID: String,
    val fieldID: String,
    val fieldName: String,
    val fieldType: String,
    val fieldDescription: String,
    val defaultValue: Any,
    val secondaryValue: Any,
    val minValue: Double,
    val maxValue: Double, val tab: String
)

/**
Class that both loads and holds data for LunaSettings. Can not be used outside LunaLib.

[LunaSettings on the Github Wiki](https://github.com/Lukas22041/LunaLib/wiki/Integrating-LunaSettings)
 */
object LunaSettingsLoader {

    var hasLoaded = false

    @JvmStatic
    var Settings: MutableMap<String, JSONUtils.CommonDataJSONObject> = HashMap()

    @JvmStatic
    var SettingsData: MutableList<LunaSettingsData> = ArrayList()

    private val log = Global.getLogger(LunaSettingsLoader::class.java)

    init {
        log.level = Level.ALL
    }

    fun load() {
        loadDefault()
        saveDefaultsToFile()
        loadSettings()
    }

    /**
     * 加载默认配置。
     */
    fun loadDefault() {

        val mods = Global.getSettings().modManager.enabledModsCopy

        for (mod in mods) {
            var CSV: JSONArray? = null

            try {
                CSV = Global.getSettings().loadCSV("data/config/LunaSettings.csv", mod.id)
            } catch (_: Throwable) {
            }
            if (CSV == null) continue

            for (index in 0 until CSV.length()) {
                val row = CSV.getJSONObject(index)

                // val modID = row.getString("modID");
                val id = row.getString("fieldID");
                if (id == "") continue
                val name = row.getString("fieldName")
                val type = row.getString("fieldType")

                var description = ""
                try {
                    description = row.getString("fieldTooltip")
                } catch (_: Throwable) {
                }
                try {
                    description = row.getString("fieldDescription")
                } catch (_: Throwable) {
                }

                var tab = ""
                try {
                    tab = row.getString("tab")
                } catch (_: Throwable) {
                }

                var default: Any = row.getString("defaultValue")

                when (type) {
                    "Int" -> default = default.toString().toInt()
                    "Boolean" -> default = default.toString().toBoolean()
                    "Double" -> default = default.toString().toDouble()
                    "String" -> default = default.toString()
                    // 此枚举使用,进行分割
                    "Enum" -> default = default.toString().split(",")
                    "Keycode" -> default = default.toString().toInt()
                }

                var secondary = ""
                try {
                    secondary = row.getString("secondaryValue")
                } catch (_: Throwable) {
                }


                var minValue = 0.0
                var maxValue = 0.0

                if (type == "Int" || type == "Double") {
                    try {
                        minValue = MathUtils.clamp(
                            row.getString("minValue").toFloat(),
                            Int.MIN_VALUE.toFloat(),
                            Int.MAX_VALUE.toFloat()
                        ).toDouble()
                    } catch (e: Throwable) {
                        maxValue = 0.0
                    }

                    try {
                        maxValue = MathUtils.clamp(
                            row.getString("maxValue").toFloat(),
                            Int.MIN_VALUE.toFloat(),
                            Int.MAX_VALUE.toFloat()
                        ).toDouble()
                    } catch (e: Throwable) {
                        maxValue = 100.0
                    }
                }

                var oldData = SettingsData.find { it.modID == mod.id && it.fieldID == id }

                if (oldData != null) {
                    SettingsData.remove(oldData)
                }

                SettingsData.add(
                    LunaSettingsData(
                        mod.id,
                        id,
                        name,
                        type,
                        description,
                        default,
                        secondary,
                        minValue,
                        maxValue,
                        tab
                    )
                )
                log.debug("LunaSettings: Loaded default settings data: $id, from ${mod.id}")
            }
        }
        hasLoaded = true
    }

    /**
     * 调用此函数生成mod的默认配置。
     * @param modID 要生成的mod配置的ID。
     */
    fun saveDefaultsToFile(modID: String = "") {
        var defaults = SettingsData
        var mods = when (modID) {
            "" -> Global.getSettings().modManager.enabledModsCopy
            else -> listOf(Global.getSettings().modManager.getModSpec(modID))
        }

        for (mod in mods) {
            val data = JSONUtils.loadCommonJSON("LunaSettings/${mod.id}.json",
                "data/config/LunaSettingsDefault.default");

            for (default in defaults) {
                if (default.fieldType == "Text" || default.fieldType == "Header") continue
                if (default.modID == mod.id) {
                    try {
                        data!!.get(default.fieldID)
                    } catch (e: Throwable) {
                        data!!.put(default.fieldID, default.defaultValue)
                    }
                }
            }

            if (data.length() > 0) {
                data.save()
            }
        }
    }

    fun loadSettings(modID: String = "", reload: Boolean = false) {
        //Settings.clear()
        val mods = when (modID) {
            "" -> Global.getSettings().modManager.enabledModsCopy
            else -> listOf(Global.getSettings().modManager.getModSpec(modID))
        }

        for (mod in mods) {
            val data = JSONUtils.loadCommonJSON("LunaSettings/${mod.id}.json",
                "data/config/LunaSettingsDefault.default");
            if (data.length() == 0 || data == null) {
                if (!reload) {
                    log.debug("LunaSettings: Could not find any mod settings for ${mod.id}, skipping.")
                }
                continue
            }
            Settings.put(mod.id, data)
            if (!reload) {
                log.debug("LunaSettings: Loaded Mod Settings for ${mod.id}")
            } else {
                log.debug("LunaSettings: Reloaded Mod Settings for ${mod.id}")
            }
        }
    }
}