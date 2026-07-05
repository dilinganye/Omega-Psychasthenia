package lunalib.lunaSettings

/**
Listener for LunaSettings. [settingsChanged] gets called whenever settings are saved.

[LunaSettingsListener on the Github Wiki](https://github.com/Lukas22041/LunaLib/wiki/LunaSettingsListener)
 */
interface LunaSettingsListener {
    fun settingsChanged(modID: String)
}