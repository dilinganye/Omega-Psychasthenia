package lunalib.backend.ui.testpanel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.Misc
import lunalib.lunaExtensions.*
import lunalib.lunaUI.elements.LunaSpriteElement
import lunalib.lunaUI.panel.LunaBaseCustomPanelPlugin
import org.lwjgl.input.Keyboard

class TestPanel : LunaBaseCustomPanelPlugin() {

    var width = 0f
    var height = 0f

    override fun init() {

        enableCloseButton = true
        width = panel.position.width
        height = panel.position.height

        var element = panel.createUIElement(width, height, false)
        panel.addUIElement(element)


        var lunaElement = element.addLunaElement(width - 40, height - 40)
        lunaElement.enableTransparency = true
        lunaElement.position.inTL(20f, 20f)

        var newElement = lunaElement.elementPanel.createUIElement(width - 40, height - 40, true)


        newElement.addPara(
             getString(1),
            0f
        ).position.inTL(10f, 10f)

        newElement.addSpacer(10f)
        newElement.addPara(
            getString(2),
            0f
        )

        newElement.addSpacer(10f)
        newElement.addLunaElement(200f, 100f)
        newElement.addSpacer(30f)

        newElement.addPara(
            getString(3),
            0f
        )
        newElement.addSpacer(10f)
        newElement.addLunaToggleButton(true, 100f, 30f)
        newElement.addSpacer(30f)

        newElement.addPara(getString(4), 0f)
        newElement.addSpacer(10f)
        newElement.addLunaChargeButton(150f, 50f)
        newElement.addSpacer(30f)

        newElement.addPara(
            getString(5),
            0f
        )
        newElement.addSpacer(10f)
        var picker = newElement.addLunaColorPicker(0.6f, 200f, 20f)
        newElement.addSpacer(30f)
        newElement.addPara(
            getString(6),
            0f
        )
        newElement.addSpacer(10f)
        newElement.addLunaProgressBar(75f, 0f, 100f, 300f, 50f, Misc.getBasePlayerColor()).apply {

            advance {
                borderColor = picker.getColor()
                backgroundColor = picker.getColor()
            }

            showNumber(false)

            onHeld {
                if (getValue() >= 100f) {
                    changeValue(0f)
                }
                changeValue(getValue() + 1f)
            }
        }
        newElement.addSpacer(30f)

        newElement.addPara(
            getString(7),
            0f
        )
        newElement.addSpacer(10f)
        newElement.addLunaTextfield("测试文本", true, 200f, 100f)
        newElement.addSpacer(30f)

        newElement.addPara(
            getString(8),
            0f
        )
        newElement.addSpacer(10f)
        newElement.addLunaSpriteElement(
            Global.getSettings().allShipHullSpecs.random().spriteName,
            LunaSpriteElement.ScalingTypes.STRETCH_ELEMENT,
            200f,
            200f
        ).apply {
            enforceSize(50f, 100f, 50f, 100f)
        }
        newElement.addSpacer(30f)

        lunaElement.elementPanel.addUIElement(newElement)


    }

    override fun processInput(events: MutableList<InputEventAPI>) {
        super.processInput(events)

        events.forEach {
            if (it.isKeyDownEvent && it.eventValue == Keyboard.KEY_ESCAPE) {
                close()
            }
        }
    }

    private fun getString(id : Int) : String {
        val className = "TestPanel"
        return Global.getSettings().getString("LunaLib_Core_backend", "${className}_${id}")
    }
}