package data.scripts.campaign.cargo;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.SpecialItemPlugin.RightClickActionHelper;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.*;
import data.scripts.campaign.invasion.*;
import org.json.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;

/** Economic settlement and the actual cargo dialog callbacks; no game/GL required. */
public final class EngineeringWorkshopTest {
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    @SuppressWarnings("unchecked") static <T>T mock(Class<T> cls,BiFunction<String,Object[],Object> f){
        return (T)Proxy.newProxyInstance(cls.getClassLoader(),new Class[]{cls},(p,m,a)->{
            Object value=f.apply(m.getName(),a);if(value!=null)return value;
            Class<?> r=m.getReturnType();if(r==boolean.class)return false;if(r==int.class)return 0;
            if(r==float.class)return 0f;if(r==long.class)return 0L;if(r==double.class)return 0d;
            if(Set.class.isAssignableFrom(r))return Collections.emptySet();if(List.class.isAssignableFrom(r))return Collections.emptyList();return null;
        });
    }
    static JSONObject row(String item,String qty,String hull,String... costs)throws Exception{
        JSONObject r=new JSONObject();r.put("itemId",item);r.put("itemQuantity",qty);r.put("hullmodId",hull);
        for(int n=0;n<5;n++)r.put(PTSDWorkshopRecipes.COLUMNS[n+3],costs[n]);return r;
    }
    static final class Inventory implements RightClickActionHelper {
        final Map<String,Float> quantities=new HashMap<String,Float>();int mutations;
        String key(CargoItemType t,Object o){return t+":"+(o instanceof SpecialItemData?((SpecialItemData)o).getId()+":"+((SpecialItemData)o).getData():o);}
        public void removeFromClickedStackFirst(int n){throw new AssertionError("Clicked stack must not be treated as the only currency");}
        public void removeFromAnyStack(CargoItemType t,Object o,int n){check(getNumItems(t,o)>=n,"Overdraw");addItems(t,o,-n);}
        public void addItems(CargoItemType t,Object o,float n){quantities.put(key(t,o),getNumItems(t,o)+n);mutations++;}
        public float getNumItems(CargoItemType t,Object o){return quantities.containsKey(key(t,o))?quantities.get(key(t,o)):0f;}
        void stock(){for(int n=0;n<5;n++)addItems(PTSDWorkshopRecipes.TYPES[n],PTSDWorkshopRecipes.MATERIALS[n],1000);mutations=0;}
    }
    public static void main(String[] args)throws Exception{
        SectorAPI old=Global.getSector();SettingsAPI settings=Global.getSettings();
        try{
            Global.setSettings(mock(SettingsAPI.class,(n,a)->n.equals("getHullModSpec")?mock(HullModSpecAPI.class,(x,y)->null):null));
            JSONObject json=row("PTSD_Photonspreador","1","","1","2","50","10","0");
            PTSDWorkshopRecipes.Recipe r=new PTSDWorkshopRecipes.Recipe(json);
            for(JSONObject bad:Arrays.asList(row("w","1","h","1","0","0","0","0"),row("w","1","","0","0","0","0","0"),row("w","1","","-1","0","0","0","0"),row("w","1.5","","1","0","0","0","0"))){
                boolean rejected=false;try{new PTSDWorkshopRecipes.Recipe(bad);}catch(Exception expected){rejected=true;}check(rejected,"Invalid recipe accepted");
            }
            Inventory inventory=new Inventory();inventory.stock();
            PTSDWorkshopRecipes.Order order=new PTSDWorkshopRecipes.Order();order.batches.put(r,3);
            check(Arrays.equals(order.costs(),new long[]{3,6,150,30,0}),"Aggregation");
            inventory.addItems(CargoItemType.RESOURCES,"supplies",-900);inventory.mutations=0;
            check(!order.fulfill(inventory)&&inventory.mutations==0,"Insufficient resources consumed materials");
            inventory.addItems(CargoItemType.RESOURCES,"supplies",100);inventory.mutations=0;
            check(order.fulfill(inventory),"Valid order refused");
            check(inventory.getNumItems(CargoItemType.WEAPONS,"PTSD_Photonspreador")==3,"Output quantity");
            int mutations=inventory.mutations;check(!order.fulfill(inventory)&&mutations==inventory.mutations,"Duplicate confirmation charged twice");
            PTSDWorkshopRecipes.Recipe hull=new PTSDWorkshopRecipes.Recipe(row("","2","test_hullmod","1","0","0","0","0"));
            check(hull.outputType()==CargoItemType.SPECIAL,"Hullmod not an item");
            check(((SpecialItemData)hull.outputData()).getId().equals("modspec"),"Ordinary hullmod not a learnable plugin");

            final int[] reports={0};CargoAPI cargo=mock(CargoAPI.class,(n,a)->{if(n.equals("addSpecial"))reports[0]+=((Number)a[1]).intValue();return null;});
            Map<String,Object> persistent=new HashMap<String,Object>();
            CampaignFleetAPI fleet=mock(CampaignFleetAPI.class,(n,a)->n.equals("getCargo")?cargo:null);
            Global.setSector(mock(SectorAPI.class,(n,a)->n.equals("getPlayerFleet")?fleet:n.equals("getPersistentData")?persistent:null));
            WeaponSpecAPI weapon=mock(WeaponSpecAPI.class,(n,a)->n.equals("getWeaponName")?"辉光体":null);
            Global.setSettings(mock(SettingsAPI.class,(n,a)->n.equals("loadCSV")?new JSONArray().put(json):n.equals("getWeaponSpec")?weapon:null));
            PTSDCrisisState.CrisisIncident incident=new PTSDCrisisState.CrisisIncident();incident.investigable=true;incident.recordedByPlayer=true;
            incident.investigationResolved=true;incident.investigationResult=PTSDTaskRules.InvestigationResult.CONFIRMED;incident.scenarioTrigger="TEST";
            PTSDInvestigationRewards.complete(incident);check(reports[0]==0,"Unfinished chain rewarded");
            incident.scenarioResolved=true;PTSDInvestigationRewards.complete(incident);PTSDInvestigationRewards.complete(incident);
            check(reports[0]==3&&incident.investigationReportsAwarded==3,"Full chain reward/duplicate");
            incident=new PTSDCrisisState.CrisisIncident();incident.investigable=true;incident.recordedByPlayer=true;incident.scene=new PTSDNewsScene.State();
            check(!PTSDInvestigationRewards.hasEvidence(incident),"Empty scene considered evidence");incident.scene.samples=1;
            check(PTSDInvestigationRewards.hasEvidence(incident),"Sample ignored");
            incident.investigationResolved=true;incident.investigationResult=PTSDTaskRules.InvestigationResult.INCONCLUSIVE;
            PTSDInvestigationRewards.complete(incident);check(reports[0]==4,"Partial report not awarded");
            incident=new PTSDCrisisState.CrisisIncident();incident.investigable=true;
            PTSDInvestigationRewards.agent(incident);PTSDInvestigationRewards.agent(incident);check(reports[0]==5,"Private Je report must give exactly one");
            incident=new PTSDCrisisState.CrisisIncident();incident.investigable=true;incident.recordedByPlayer=true;incident.investigationResolved=true;
            incident.investigationResult=PTSDTaskRules.InvestigationResult.EXPIRED;PTSDInvestigationRewards.complete(incident);check(reports[0]==5,"Expiry rewarded");
            check(!PTSDInvestigationRewards.settle(incident,3,null)&&incident.investigationReportsAwarded==0,"Null cargo consumed reward");

            List<String> text=new ArrayList<String>();Map<Object,Boolean> buttons=new HashMap<Object,Boolean>();
            final CustomProductionPickerDelegate[] picker={null};
            TextPanelAPI panel=mock(TextPanelAPI.class,(n,a)->{if(n.equals("addPara"))text.add((String)a[0]);return null;});
            OptionPanelAPI options=mock(OptionPanelAPI.class,(n,a)->{if(n.equals("clearOptions"))buttons.clear();if(n.equals("addOption"))buttons.put(a[1],true);return null;});
            InteractionDialogAPI dialog=mock(InteractionDialogAPI.class,(n,a)->{if(n.equals("showCustomProductionPicker"))picker[0]=(CustomProductionPickerDelegate)a[0];return n.equals("getTextPanel")?panel:n.equals("getOptionPanel")?options:null;});
            inventory=new Inventory();inventory.stock();PTSDEngineeringDialog ui=new PTSDEngineeringDialog(inventory);ui.init(dialog);
            check(!persistent.containsKey(PTSDEngineeringDialog.READ)&&inventory.mutations==0,"Opening consumed/unlocked");
            ui.optionSelected(null,"close");check(!persistent.containsKey(PTSDEngineeringDialog.READ),"Closing unlocked unread story");
            ui=new PTSDEngineeringDialog(inventory);ui.init(dialog);ui.optionSelected(null,"next");ui.optionSelected(null,"weaponize");ui.optionSelected(null,"unlock");ui.optionSelected(null,"back");
            check(Boolean.TRUE.equals(persistent.get(PTSDEngineeringDialog.READ))&&buttons.containsKey("weapons"),"Intro did not unlock workshop");
            check(inventory.mutations==0,"Intro charged materials");
            ui.optionSelected(null,"weapons");check(picker[0]!=null&&picker[0].getAvailableWeapons().contains("PTSD_Photonspreador"),"Native picker omitted recipe");
            FactionProductionAPI.ItemInProductionAPI selection=mock(FactionProductionAPI.ItemInProductionAPI.class,(n,a)->n.equals("getType")?FactionProductionAPI.ProductionItemType.WEAPON:n.equals("getSpecId")?"PTSD_Photonspreador":n.equals("getQuantity")?2:null);
            FactionProductionAPI production=mock(FactionProductionAPI.class,(n,a)->n.equals("getCurrent")?Collections.singletonList(selection):null);
            picker[0].notifyProductionSelected(production);check(buttons.containsKey("confirm")&&inventory.mutations==0,"Selection paid before confirmation");
            ui.optionSelected(null,"back");check(inventory.mutations==0,"Cancelling paid");
            ui.optionSelected(null,"weapons");picker[0].notifyProductionSelected(production);
            inventory.addItems(CargoItemType.RESOURCES,"supplies",-1000);inventory.mutations=0;
            ui.optionSelected(null,"confirm");check(inventory.mutations==0,"Confirmation did not recheck inventory");
            inventory.addItems(CargoItemType.RESOURCES,"supplies",1000);inventory.mutations=0;
            ui.optionSelected(null,"weapons");picker[0].notifyProductionSelected(production);ui.optionSelected(null,"confirm");
            check(inventory.getNumItems(CargoItemType.WEAPONS,"PTSD_Photonspreador")==2&&inventory.getNumItems(CargoItemType.RESOURCES,"supplies")==900,"Native selection settlement");
            int after=inventory.mutations;ui.optionSelected(null,"confirm");check(after==inventory.mutations,"Stale confirmation paid again");
            text.clear();new PTSDEngineeringDialog(inventory).init(dialog);check(text.stream().noneMatch(s->s.contains("气闸声")),"Intro repeated");
            check(new PTSDCoreFragmentItemPlugin().hasRightClickAction()&&!new PTSDCoreFragmentItemPlugin().shouldRemoveOnRightClickAction(),"Cargo entry consumes clicked item");
            check(!new PTSDHullmodModulePlugin().hasRightClickAction(),"Data-less codex module is actionable");
            System.out.println("EngineeringWorkshopTest passed");
        }finally{Global.setSector(old);Global.setSettings(settings);}
    }
}
