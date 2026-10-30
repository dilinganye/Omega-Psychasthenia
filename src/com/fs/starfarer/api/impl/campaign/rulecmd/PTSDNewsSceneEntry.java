package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.invasion.*;
import java.util.*;

/** Shared-world objects expose all eligible incidents, without replacing their vanilla dialog. */
public final class PTSDNewsSceneEntry extends BaseCommandPlugin {
    private static final String PREFIX="PTSD_scene_entry:";
    public boolean execute(String ruleId,InteractionDialogAPI dialog,List<Misc.Token> params,Map<String,MemoryAPI> memoryMap){
        if(dialog==null || params.isEmpty())return false;
        PTSDCrisisState state=PTSDCrisisState.get();SectorEntityToken target=dialog.getInteractionTarget();
        if(state==null || target==null)return false;
        String mode=params.get(0).getString(memoryMap);
        if("populate".equals(mode)) {
            String menu=memoryMap.get("local").getString("$menuState");
            if(target.getMarket()!=null && menu!=null && !"main".equals(menu))return false;
            // Accepting while docked must not require an unpaused campaign tick to create the entry.
            for(PTSDCrisisState.CrisisIncident i:state.incidents){
                if(!i.recordedByPlayer || i.investigationAbandoned || i.investigationResolved || i.investigationOutcome!=1)continue;
                if(!i.siteMaterialized && target.getStarSystem()!=null && target.getStarSystem().getId().equals(i.targetSystemId))
                    PTSDNewsSiteManager.materialize(state,i,new Random(i.id.hashCode()),false,false);
                if(PTSDNewsScene.dialogForIncident(i,target)!=null)
                    dialog.getOptionPanel().addOption("调查："+PTSDPlayerNewsText.headline(i),PREFIX+i.id);
            }
            return true;
        }
        String selected=memoryMap.get("local").getString("$option");
        if(selected==null || !selected.startsWith(PREFIX))return false;
        if("matches".equals(mode))return true;
        PTSDCrisisState.CrisisIncident incident=PTSDCrisisAPI.getIncident(selected.substring(PREFIX.length()));
        if(incident==null || incident.scene==null || incident.investigationAbandoned || incident.investigationResolved || incident.scene.resolved || PTSDNewsScene.boundNode(incident,target)==null)return false;
        InteractionDialogPlugin scene=PTSDNewsScene.dialogForIncident(incident,target);
        if(scene==null)return false;
        dialog.setPlugin(scene);scene.init(dialog);return true;
    }
}
