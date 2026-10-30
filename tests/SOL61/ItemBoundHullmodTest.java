package data.scripts.campaign.cargo;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.impl.campaign.HullModItemManager;
import java.util.*;

/** Executes the vanilla manager, including proposed variants, commit, and removal. */
public final class ItemBoundHullmodTest {
    public static void main(String[] args){
        SectorAPI old=Global.getSector();SettingsAPI settings=Global.getSettings();FactoryAPI factory=Global.getFactory();
        try{
            EngineeringWorkshopTest.Inventory inv=new EngineeringWorkshopTest.Inventory();
            SpecialItemData item=new SpecialItemData(PTSDWorkshopRecipes.MODULE,"test_module");inv.addItems(CargoItemType.SPECIAL,item,1);
            CargoAPI playerCargo=EngineeringWorkshopTest.mock(CargoAPI.class,(n,a)->{
                if(n.equals("getQuantity"))return inv.getNumItems((CargoItemType)a[0],a[1]);
                if(n.equals("removeItems"))inv.removeFromAnyStack((CargoItemType)a[0],a[1],((Number)a[2]).intValue());
                if(n.equals("addItems"))inv.addItems((CargoItemType)a[0],a[1],((Number)a[2]).floatValue());return null;
            });
            CargoStackAPI requirement=EngineeringWorkshopTest.mock(CargoStackAPI.class,(n,a)->n.equals("getType")?CargoItemType.SPECIAL:n.equals("getData")?item:null);
            Global.setFactory(EngineeringWorkshopTest.mock(FactoryAPI.class,(n,a)->n.equals("createCargo")?
                    EngineeringWorkshopTest.mock(CargoAPI.class,(x,y)->x.equals("getStacksCopy")?Collections.singletonList(requirement):null):null));
            PTSDItemBoundHullMod effect=new PTSDItemBoundHullMod();
            HullModSpecAPI spec=EngineeringWorkshopTest.mock(HullModSpecAPI.class,(n,a)->n.equals("getId")?"test_module":n.equals("hasTag")?PTSDWorkshopRecipes.TAG.equals(a[0]):n.equals("getEffect")?effect:null);
            effect.init(spec);
            Global.setSettings(EngineeringWorkshopTest.mock(SettingsAPI.class,(n,a)->n.equals("getHullModSpec")?spec:n.equals("getCurrentState")?GameState.CAMPAIGN:null));
            CampaignFleetAPI fleet=EngineeringWorkshopTest.mock(CampaignFleetAPI.class,(n,a)->n.equals("getCargo")?playerCargo:null);
            ListenerManagerAPI listeners=EngineeringWorkshopTest.mock(ListenerManagerAPI.class,(n,a)->null);
            Global.setSector(EngineeringWorkshopTest.mock(SectorAPI.class,(n,a)->n.equals("getPlayerFleet")?fleet:n.equals("getListenerManager")?listeners:null));
            Set<String> mods=new LinkedHashSet<String>();
            ShipVariantAPI variant=EngineeringWorkshopTest.mock(ShipVariantAPI.class,(n,a)->n.equals("getHullMods")?mods:null);
            ShipHullSpecAPI hull=EngineeringWorkshopTest.mock(ShipHullSpecAPI.class,(n,a)->null);
            FleetMemberAPI member=EngineeringWorkshopTest.mock(FleetMemberAPI.class,(n,a)->n.equals("getId")?"ship-a":n.equals("getVariant")?variant:n.equals("getHullSpec")?hull:null);
            HullModItemManager manager=new HullModItemManager();
            EngineeringWorkshopTest.check(effect.getRequiredItem().getData().equals(item),"Required item identity");
            EngineeringWorkshopTest.check(manager.isRequiredItemAvailable("test_module",member,variant,null),"Available component rejected");
            mods.add("test_module");
            EngineeringWorkshopTest.check(inv.getNumItems(CargoItemType.SPECIAL,item)==1,"Preview consumed component");
            manager.reportFleetMemberVariantSaved(member,null);
            EngineeringWorkshopTest.check(inv.getNumItems(CargoItemType.SPECIAL,item)==0,"Commit did not reserve component");
            manager.reportFleetMemberVariantSaved(member,null);
            EngineeringWorkshopTest.check(inv.getNumItems(CargoItemType.SPECIAL,item)==0,"Repeated commit changed inventory");
            FleetMemberAPI other=EngineeringWorkshopTest.mock(FleetMemberAPI.class,(n,a)->n.equals("getId")?"ship-b":n.equals("getHullSpec")?hull:null);
            ShipVariantAPI empty=EngineeringWorkshopTest.mock(ShipVariantAPI.class,(n,a)->n.equals("getHullMods")?Collections.emptySet():null);
            EngineeringWorkshopTest.check(!manager.isRequiredItemAvailable("test_module",other,empty,null),"Second ship installed without component");
            mods.clear();manager.reportFleetMemberVariantSaved(member,null);
            EngineeringWorkshopTest.check(inv.getNumItems(CargoItemType.SPECIAL,item)==1,"Removal did not return component");
            manager.reportFleetMemberVariantSaved(member,null);
            EngineeringWorkshopTest.check(inv.getNumItems(CargoItemType.SPECIAL,item)==1,"Repeated removal duplicated component");
            System.out.println("ItemBoundHullmodTest passed");
        }finally{Global.setFactory(factory);Global.setSettings(settings);Global.setSector(old);}
    }
}
