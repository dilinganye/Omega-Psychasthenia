package lunalib.lunaDebug.snippets

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ModSpecAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import lunalib.lunaDebug.LunaDebug
import lunalib.lunaDebug.LunaSnippet
import lunalib.lunaDebug.SnippetBuilder

class SnippetsListSnippet() : LunaSnippet() {
    override fun getName(): String {
        return "显示可用 Snippets 的列表"
    }

    override fun getDescription(): String {
        return "显示所有可以在这里使用的 Snippets 及其所属mod。"
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


        var title = output.addPara("已被加载的 Snippets", 0f, baseColor, highlightColor, "已被加载的 Snippets")
        output.addSpacer(10f)

        var modsWithSnippets = ArrayList<ModSpecAPI>()
        for (snippet in LunaDebug.snippets) {
            if (!modsWithSnippets.contains(Global.getSettings().modManager.getModSpec(snippet.modId)))
                modsWithSnippets.add(Global.getSettings().modManager.getModSpec(snippet.modId))
        }

        for (mod in modsWithSnippets) {
            var snippets = LunaDebug.snippets.filter { it.modId == mod.id }
            output.addSpacer(10f)

            output.addPara(mod.name, 0f, Misc.getBasePlayerColor(), Misc.getHighlightColor(), mod.name)
            output.addSpacer(2f)

            for (snippet in snippets) {
                output.addPara(snippet.name, 0f, Misc.getBasePlayerColor(), Misc.getHighlightColor())
            }
        }
    }
}