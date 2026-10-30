package data.scripts.campaign.cargo;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.campaign.*;

/** Extend this base and add ptsd_item_bound to opt into vanilla per-ship item accounting. */
public class PTSDItemBoundHullMod extends BaseHullMod {
    @Override public CargoStackAPI getRequiredItem() {
        if(spec==null||!spec.hasTag(PTSDWorkshopRecipes.TAG))return null;
        CargoAPI cargo=Global.getFactory().createCargo(true);
        cargo.addSpecial(new SpecialItemData(PTSDWorkshopRecipes.MODULE,spec.getId()),1);
        return cargo.getStacksCopy().get(0);
    }
}
