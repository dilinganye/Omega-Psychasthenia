package data.scripts.campaign.cargo;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;

/** Right click enables the design; the module itself remains available for installation. */
public class PTSDHullmodModulePlugin extends BaseSpecialItemPlugin {
    private String modId(){return stack==null||stack.getSpecialDataIfSpecial()==null?null:stack.getSpecialDataIfSpecial().getData();}
    private HullModSpecAPI mod(){try{return modId()==null?null:Global.getSettings().getHullModSpec(modId());}catch(RuntimeException ex){return null;}}
    @Override public String getName(){return mod()==null?"特殊改装组件":mod().getDisplayName()+" · 装配组件";}
    @Override public boolean hasRightClickAction(){return mod()!=null&&mod().hasTag(PTSDWorkshopRecipes.TAG)&&Global.getSector()!=null&&!Global.getSector().getCharacterData().knowsHullMod(modId());}
    @Override public boolean shouldRemoveOnRightClickAction(){return false;}
    @Override public void performRightClickAction(RightClickActionHelper helper){if(hasRightClickAction())Global.getSector().getCharacterData().addHullMod(modId());}
    @Override public void createTooltip(TooltipMakerAPI tooltip,boolean expanded,CargoTransferHandlerAPI transfer,Object source){
        tooltip.addTitle(getName());
        tooltip.addPara("右键登记装配设计，不消耗组件。每艘船安装时占用1件，拆下时返还；仅预览方案不会扣除。",10f);
        addCostLabel(tooltip,10f,transfer,source);
    }
}
