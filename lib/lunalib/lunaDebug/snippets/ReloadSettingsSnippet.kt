package lunalib.lunaDebug.snippets

import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import lunalib.backend.ui.settings.LunaSettingsConfigLoader
import lunalib.backend.ui.settings.LunaSettingsLoader
import lunalib.lunaDebug.LunaSnippet
import lunalib.lunaDebug.SnippetBuilder
import kotlin.system.measureTimeMillis

class ReloadSettingsSnippet() : LunaSnippet() {
    override fun getName(): String {
        return "重新载入所有 LunaSettings.csv"
    }

    override fun getDescription(): String {
        return "重新加载所有 LunaSettings.csv 文件，这可以刷新已被载入的设置项的ID、描述和参数方式。但是并不会丢失所有已保存的修改。"
    }

    override fun getModId(): String {
        return "lunalib"
    }

    override fun getTags(): MutableList<String> {
        return mutableListOf(SnippetTags.Debug.toString())
    }

    override fun addParameters(builder: SnippetBuilder?) {

    }

    override fun execute(parameters: MutableMap<String, Any>?, output: TooltipMakerAPI) {

        var time = measureTimeMillis {
            LunaSettingsConfigLoader.reload()
            LunaSettingsLoader.loadDefault()
            LunaSettingsLoader.saveDefaultsToFile()
            LunaSettingsLoader.loadSettings()
        }

        output.addPara(
            "在 ${time} ms 内重新载入了所有数据。",
            0f,
            Misc.getBasePlayerColor(),
            Misc.getBasePlayerColor()
        )

    }
}