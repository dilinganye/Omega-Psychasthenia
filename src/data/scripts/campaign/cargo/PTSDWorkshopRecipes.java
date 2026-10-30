package data.scripts.campaign.cargo;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.SpecialItemPlugin.RightClickActionHelper;
import data.scripts.campaign.invasion.PTSDInvestigationRewards;
import org.json.*;
import java.util.*;

/** Eight-column recipes. All material checks use the cargo dialog's actual source. */
public final class PTSDWorkshopRecipes {
    public static final String PATH = "data/config/ptsdCSV/PTSD_workshop_recipes.csv";
    public static final String MODULE = "PTSD_hullmod_module", TAG = "ptsd_item_bound";
    public static final String[] COLUMNS = {"itemId","itemQuantity","hullmodId","protocolFragments","investigationReports","supplies","heavyMachinery","heavyArmaments"};
    public static final String[] LABELS = {"协议残片","调查报告","补给","重型装备","重型武器"};
    public static final CargoItemType[] TYPES = {CargoItemType.SPECIAL,CargoItemType.SPECIAL,CargoItemType.RESOURCES,CargoItemType.RESOURCES,CargoItemType.RESOURCES};
    public static final Object[] MATERIALS = {new SpecialItemData("PTSD_core_fragment",null),new SpecialItemData(PTSDInvestigationRewards.REPORT,null),"supplies","heavy_machinery","hand_weapons"};
    public static final class Recipe {
        public final String itemId, hullmodId;
        public final int quantity;
        public final int[] costs;
        public Recipe(JSONObject row) throws JSONException {
            for(String column:COLUMNS) if(!row.has(column)) throw new IllegalArgumentException("Missing column: "+column);
            itemId=row.getString("itemId").trim(); hullmodId=row.getString("hullmodId").trim();
            if(itemId.isEmpty()==hullmodId.isEmpty())throw new IllegalArgumentException("Exactly one output is required");
            quantity=integer(row,"itemQuantity");
            if(quantity<1||quantity>10000)throw new IllegalArgumentException("Invalid output quantity");
            costs=new int[5]; long total=0;
            for(int n=0;n<5;n++){costs[n]=integer(row,COLUMNS[n+3]);total+=costs[n];}
            if(total==0)throw new IllegalArgumentException("Free recipe rejected");
        }
        public String key(){return itemId.isEmpty()?"h:"+hullmodId:"w:"+itemId;}
        public String name(){return itemId.isEmpty()?Global.getSettings().getHullModSpec(hullmodId).getDisplayName():Global.getSettings().getWeaponSpec(itemId).getWeaponName();}
        public boolean module(){return itemId.isEmpty()&&Global.getSettings().getHullModSpec(hullmodId).hasTag(TAG);}
        public CargoItemType outputType(){return itemId.isEmpty()?CargoItemType.SPECIAL:CargoItemType.WEAPONS;}
        public Object outputData(){return itemId.isEmpty()?new SpecialItemData(module()?MODULE:"modspec",hullmodId):itemId;}
    }
    private static int integer(JSONObject row,String key)throws JSONException {
        String value=row.getString(key).trim();
        if(!value.matches("[0-9]+"))throw new IllegalArgumentException("Non-negative integer required: "+key);
        int n=Integer.parseInt(value);if(n>1000000)throw new IllegalArgumentException("Cost too large: "+key);return n;
    }
    public static List<Recipe> load(){
        List<Recipe> result=new ArrayList<Recipe>(); Set<String> seen=new HashSet<String>();
        try {
            JSONArray rows=Global.getSettings().loadCSV(PATH,"Omega_PTSD");
            for(int n=0;n<rows.length();n++)try{
                JSONObject row=rows.getJSONObject(n);
                if(row.optString("itemId","").trim().startsWith("#"))continue;
                Recipe r=new Recipe(row);
                if(r.itemId.isEmpty()) {
                    if(Global.getSettings().getHullModSpec(r.hullmodId)==null)throw new IllegalArgumentException("Unknown hullmod");
                    if(r.module()&&!(Global.getSettings().getHullModSpec(r.hullmodId).getEffect() instanceof PTSDItemBoundHullMod))
                        throw new IllegalArgumentException("Tagged hullmod must extend PTSDItemBoundHullMod");
                } else if(Global.getSettings().getWeaponSpec(r.itemId)==null)throw new IllegalArgumentException("Unknown weapon");
                if(!seen.add(r.key()))throw new IllegalArgumentException("Duplicate output: "+r.key());
                result.add(r);
            }catch(Exception ex){Global.getLogger(PTSDWorkshopRecipes.class).error(PATH+" row "+(n+2)+" disabled",ex);}
        }catch(Exception ex){Global.getLogger(PTSDWorkshopRecipes.class).error("Cannot read "+PATH,ex);}
        return result;
    }
    public static final class Order {
        public final Map<Recipe,Integer> batches=new LinkedHashMap<Recipe,Integer>();
        private boolean settled;
        public long[] costs(){
            long[] sum=new long[5]; int batchesTotal=0;
            for(Map.Entry<Recipe,Integer> e:batches.entrySet()) {
                if(e.getValue()==null||e.getValue()<1||e.getValue()>100)throw new IllegalArgumentException("Invalid batch count");
                batchesTotal+=e.getValue();if(batchesTotal>100)throw new IllegalArgumentException("Order exceeds 100 batches");
                for(int n=0;n<5;n++)sum[n]+=(long)e.getKey().costs[n]*e.getValue();
            }
            return sum;
        }
        public boolean affordable(RightClickActionHelper helper){
            if(settled||batches.isEmpty()||helper==null)return false;
            long[] required=costs();for(int n=0;n<5;n++)if(helper.getNumItems(TYPES[n],MATERIALS[n])<required[n])return false;
            return true;
        }
        public boolean fulfill(RightClickActionHelper helper){
            if(!affordable(helper))return false;
            long[] required=costs();
            // The helper's synchronous cargo operations match the vanilla substrate transaction.
            settled=true;
            for(int n=0;n<5;n++)if(required[n]>0)helper.removeFromAnyStack(TYPES[n],MATERIALS[n],(int)required[n]);
            for(Map.Entry<Recipe,Integer> e:batches.entrySet())helper.addItems(e.getKey().outputType(),e.getKey().outputData(),e.getKey().quantity*e.getValue());
            return true;
        }
    }
}
