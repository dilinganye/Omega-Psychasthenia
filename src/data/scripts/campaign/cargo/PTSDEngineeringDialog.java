package data.scripts.campaign.cargo;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.SpecialItemPlugin.RightClickActionHelper;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import java.util.*;

/** Cargo-origin workshop: the native weapon picker prepares an order; only confirmation pays. */
public final class PTSDEngineeringDialog implements InteractionDialogPlugin {
    public static final String READ="$PTSD_engineering_workshop_read";
    private final RightClickActionHelper helper;
    private final List<PTSDWorkshopRecipes.Recipe> recipes;
    private InteractionDialogAPI dialog;
    private PTSDWorkshopRecipes.Order order;
    private int page;
    private boolean reverseEngineering;
    public PTSDEngineeringDialog(RightClickActionHelper helper){this.helper=helper;recipes=PTSDWorkshopRecipes.load();}
    public static void open(RightClickActionHelper helper){
        if(helper==null||Global.getSector()==null||Global.getSector().getPlayerFleet()==null)return;
        Global.getSector().getCampaignUI().showInteractionDialogFromCargo(new PTSDEngineeringDialog(helper),
                Global.getSector().getPlayerFleet(),new CampaignUIAPI.DismissDialogDelegate(){public void dialogDismissed(){}});
    }
    private boolean read(){return Boolean.TRUE.equals(Global.getSector().getPersistentData().get(READ));}
    @Override public void init(InteractionDialogAPI dialog){this.dialog=dialog;if(read())hub();else intro();}
    private void text(String text){dialog.getTextPanel().addPara(text);}
    private void option(String text,Object key){dialog.getOptionPanel().addOption(text,key);}
    private void clear(){dialog.getOptionPanel().clearOptions();}
    private void introText(String[] paragraphs){
        for(String paragraph:paragraphs)text(paragraph.replace("$playerSirOrMadam","长官"));
    }
    private void intro(){
        clear();
        if(page==0){
            introText(PTSDEngineeringDialogueText.OPENING);
            option(PTSDEngineeringDialogueText.QUESTION,"next");
        }else if(page==1){
            introText(PTSDEngineeringDialogueText.APPLICATIONS);
            option(PTSDEngineeringDialogueText.WEAPON_OPTION,"weaponize");
            option(PTSDEngineeringDialogueText.REVERSE_OPTION,"reverse");
        }else if(page==2){
            introText(reverseEngineering?PTSDEngineeringDialogueText.REVERSE_REPLY:PTSDEngineeringDialogueText.WEAPON_REPLY);
            option(PTSDEngineeringDialogueText.FINAL_OPTION,"unlock");
        }
        option("暂时离开","close");
    }
    private void inventory(){
        StringBuilder b=new StringBuilder("可用材料：");
        for(int n=0;n<5;n++){if(n>0)b.append(" / ");b.append(PTSDWorkshopRecipes.LABELS[n]).append(' ').append((int)helper.getNumItems(PTSDWorkshopRecipes.TYPES[n],PTSDWorkshopRecipes.MATERIALS[n]));}
        text(b.toString());
    }
    private void hub(){
        clear();order=null;text("工程与分析台");inventory();
        for(PTSDWorkshopRecipes.Recipe r:recipes)text(r.name()+" ×"+r.quantity+"："+costText(r.costs));
        if(recipes.isEmpty())text("目前没有可用装配项目。");
        boolean weapons=false;for(PTSDWorkshopRecipes.Recipe r:recipes)if(!r.itemId.isEmpty())weapons=true;
        if(weapons)option("选择武器装配项目","weapons");
        if(hasHullmods())option("选择改装插件","hullmods");
        option("结束交谈","close");
    }
    private static String costText(int[] costs){long[] c=new long[costs.length];for(int n=0;n<c.length;n++)c[n]=costs[n];return costText(c);}
    private static String costText(long[] costs){StringBuilder b=new StringBuilder();for(int n=0;n<costs.length;n++)if(costs[n]>0){if(b.length()>0)b.append("、");b.append(PTSDWorkshopRecipes.LABELS[n]).append(' ').append(costs[n]);}return b.toString();}
    private boolean hasHullmods(){for(PTSDWorkshopRecipes.Recipe r:recipes)if(r.itemId.isEmpty())return true;return false;}
    private void hullmods(){
        clear();text("每次兑换交付配方所列数量的实物插件。带装配组件标记的项目，每艘船安装占用一件，拆下返还。");
        List<PTSDWorkshopRecipes.Recipe> mods=new ArrayList<PTSDWorkshopRecipes.Recipe>();
        for(PTSDWorkshopRecipes.Recipe r:recipes)if(r.itemId.isEmpty())mods.add(r);
        int start=page*6,end=Math.min(start+6,mods.size());
        for(int n=start;n<end;n++){PTSDWorkshopRecipes.Recipe r=mods.get(n);option(r.name()+" ×"+r.quantity+" — "+costText(r.costs),r);}
        if(start>0)option("上一页","previous");if(end<mods.size())option("下一页","more");option("返回装配台","back");
    }
    private void confirm(){
        clear();text("待确认的装配清单");
        for(Map.Entry<PTSDWorkshopRecipes.Recipe,Integer> e:order.batches.entrySet())text(e.getKey().name()+" ×"+(e.getKey().quantity*e.getValue()));
        text("本次材料合计："+costText(order.costs()));inventory();
        option("确认扣除材料并领取成品","confirm");
        dialog.getOptionPanel().setEnabled("confirm",order.affordable(helper));
        if(!order.affordable(helper))text("材料不足。没有扣除物资；可取消后重新选择。");
        option("取消清单，返回装配台","back");
    }
    private void weapons(){
        final Map<String,PTSDWorkshopRecipes.Recipe> available=new LinkedHashMap<String,PTSDWorkshopRecipes.Recipe>();
        for(PTSDWorkshopRecipes.Recipe r:recipes)if(!r.itemId.isEmpty())available.put(r.itemId,r);
        text("每个选择单位代表一批；单批产量和材料见装配台清单。选择完成后核对总材料，确认前不会扣货。");
        dialog.showCustomProductionPicker(new BaseCustomProductionPickerDelegateImpl(){
            @Override public Set<String> getAvailableWeapons(){return available.keySet();}
            @Override public Set<String> getAvailableFighters(){return Collections.emptySet();}
            @Override public Set<String> getAvailableShipHulls(){return Collections.emptySet();}
            @Override public float getCostMult(){return 1f;}
            @Override public float getMaximumValue(){return 100;}
            @Override public int getCostOverride(Object item){return item instanceof WeaponSpecAPI?1:-1;}
            @Override public boolean isUseCreditSign(){return false;}
            @Override public boolean withQuantityLimits(){return false;}
            @Override public String getWeaponColumnNameOverride(){return "武器装配项目";}
            @Override public String getMaximumOrderValueLabelOverride(){return "单次批次上限";}
            @Override public String getCurrentOrderValueLabelOverride(){return "已选批次";}
            @Override public String getCustomOrderLabelOverride(){return "分析与装配";}
            @Override public String getNoProductionOrdersLabelOverride(){return "尚未选择项目";}
            @Override public String getNoMatchingBlueprintsLabelOverride(){return "没有匹配项目";}
            @Override public String getItemGoesOverMaxValueStringOverride(){return "超过单次批次上限";}
            @Override public void notifyProductionSelected(FactionProductionAPI production){
                order=new PTSDWorkshopRecipes.Order();
                for(FactionProductionAPI.ItemInProductionAPI item:production.getCurrent())if(item.getType()==FactionProductionAPI.ProductionItemType.WEAPON){
                    PTSDWorkshopRecipes.Recipe r=available.get(item.getSpecId());if(r!=null&&item.getQuantity()>0)order.batches.put(r,item.getQuantity());
                }
                if(order.batches.isEmpty())hub();else confirm();
            }
        });
    }
    @Override public void optionSelected(String text,Object data){
        if("close".equals(data)){dialog.dismiss();return;}
        if(!read()){
            if("next".equals(data)&&page==0){page=1;intro();}
            else if(("weaponize".equals(data)||"reverse".equals(data))&&page==1){
                reverseEngineering="reverse".equals(data);page=2;intro();
            }else if("unlock".equals(data)&&page==2){
                Global.getSector().getPersistentData().put(READ,true);
                clear();introText(PTSDEngineeringDialogueText.FINAL_REPLY);
                option("查看装配项目","back");option("结束交谈","close");
            }
            return;
        }
        if("back".equals(data))hub();
        else if("weapons".equals(data))weapons();
        else if("hullmods".equals(data)){page=0;hullmods();}
        else if("previous".equals(data)){page--;hullmods();}
        else if("more".equals(data)){page++;hullmods();}
        else if(data instanceof PTSDWorkshopRecipes.Recipe&&recipes.contains(data)){order=new PTSDWorkshopRecipes.Order();order.batches.put((PTSDWorkshopRecipes.Recipe)data,1);confirm();}
        else if("confirm".equals(data)&&order!=null){
            boolean done=order.fulfill(helper);hub();text(done?"材料已交付，成品已入库。":"材料发生变化，装配未执行。请重新核对清单。");
        }
    }
    @Override public void optionMousedOver(String text,Object data){}
    @Override public void advance(float amount){}
    @Override public void backFromEngagement(EngagementResultAPI result){}
    @Override public Object getContext(){return null;}
    @Override public Map<String,MemoryAPI> getMemoryMap(){return Collections.emptyMap();}
}
