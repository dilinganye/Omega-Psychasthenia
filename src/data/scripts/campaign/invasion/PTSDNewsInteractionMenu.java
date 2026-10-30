package data.scripts.campaign.invasion;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import java.util.*;

/** 共享实体上的调查入口；选择任务前不初始化子对话，不触发其传输/伏击/奖励。 */
public final class PTSDNewsInteractionMenu implements InteractionDialogPlugin {
    public static final class Entry {
        public final PTSDCrisisState.CrisisIncident incident;
        public final String title;
        public final InteractionDialogPlugin plugin;
        public Entry(PTSDCrisisState.CrisisIncident incident, String title, InteractionDialogPlugin plugin) {
            this.incident=incident; this.title=title; this.plugin=plugin;
        }
        boolean active() { return incident==null || !incident.investigationResolved&&!incident.investigationAbandoned; }
    }
    private final SectorEntityToken target;
    private final List<Entry> entries;
    private InteractionDialogAPI dialog;
    private PTSDNewsInteractionMenu(SectorEntityToken target,List<Entry> entries) {
        this.target=target;this.entries=new ArrayList<Entry>(entries);
    }
    public static InteractionDialogPlugin choose(SectorEntityToken target,List<Entry> entries) {
        if(entries.isEmpty())return null;
        return entries.size()==1?entries.get(0).plugin:new PTSDNewsInteractionMenu(target,entries);
    }
    public void init(InteractionDialogAPI dialog) { this.dialog=dialog;menu(); }
    private void menu() {
        dialog.getOptionPanel().clearOptions();
        dialog.getTextPanel().addPara("此处涉及多项调查。请选择要处理的线索；其他调查仍会保留。");
        for(int n=0;n<entries.size();n++)if(entries.get(n).active())
            dialog.getOptionPanel().addOption("调查："+entries.get(n).title,"news:"+n);
        dialog.getOptionPanel().addOption("办理此处的正常事务","normal");
        dialog.getOptionPanel().addOption("暂时离开","leave");
    }
    public void optionSelected(String text,Object option) {
        String id=String.valueOf(option);
        if("leave".equals(id)){dialog.dismiss();return;}
        if("normal".equals(id)){PTSDPortInvestigation.openNormal(dialog,target);return;}
        if(!id.startsWith("news:"))return;
        int index;try{index=Integer.parseInt(id.substring(5));}catch(NumberFormatException bad){return;}
        if(index<0||index>=entries.size())return;
        Entry entry=entries.get(index);if(!entry.active()){menu();return;}
        dialog.setPlugin(entry.plugin);entry.plugin.init(dialog);
    }
    public void advance(float amount) { }
    public void optionMousedOver(String text,Object data) { }
    public void backFromEngagement(EngagementResultAPI result) { }
    public Object getContext() { return null; }
    public Map<String,MemoryAPI> getMemoryMap() { return null; }
}
