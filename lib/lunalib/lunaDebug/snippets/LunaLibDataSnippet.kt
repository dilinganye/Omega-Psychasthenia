package lunalib.lunaDebug.snippets

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ModSpecAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import lunalib.backend.ui.settings.LunaSettingsLoader
import lunalib.lunaDebug.LunaDebug
import lunalib.lunaDebug.LunaSnippet
import lunalib.lunaDebug.SnippetBuilder

class LunaLibDataSnippet() : LunaSnippet() {
    override fun getName(): String {
        return "打印 LunaLib 的相关数据"
    }

    override fun getDescription(): String {
        return "显示LunaLib的一些细节信息。"
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

        var baseColor = Misc.getBasePlayerColor()
        var highlightColor = Misc.getHighlightColor()

        var mod = Global.getSettings().modManager.getModSpec("lunalib")

        var title = output.addPara("${mod.name}: V${mod.version}", 0f, baseColor, highlightColor)
        title.setHighlight("${mod.name}")
        output.addSpacer(20f)

        var data = LunaSettingsLoader.Settings

        output.addPara("LunaSettings", 0f, highlightColor, highlightColor)

        output.addPara("当前已被激活且携带了LunaLib设置项的mod： ${data.keys}", 0f, baseColor, highlightColor)
        output.addSpacer(5f)
        output.addPara(
            "所有已被加载的mod设置项的数量为 ${LunaSettingsLoader.SettingsData.size}",
            0f,
            baseColor,
            highlightColor
        )

        output.addSpacer(20f)

        output.addPara("LunaSnippets", 0f, highlightColor, highlightColor)

        var modsWithSnippets = ArrayList<ModSpecAPI>()
        for (snippet in LunaDebug.snippets) {
            if (!modsWithSnippets.contains(Global.getSettings().modManager.getModSpec(snippet.modId)))
                modsWithSnippets.add(Global.getSettings().modManager.getModSpec(snippet.modId))
        }

        output.addPara("已装载了函数的mod：$modsWithSnippets", 0f, baseColor, highlightColor)
        output.addSpacer(5f)
        output.addPara("所有函数的数量：${LunaDebug.snippets.size}", 0f, baseColor, highlightColor)
    }
}