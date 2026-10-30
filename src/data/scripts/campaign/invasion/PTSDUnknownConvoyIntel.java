package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import java.util.Set;

/** Public, continuously updated last-month transport sightings. */
public final class PTSDUnknownConvoyIntel extends BaseIntelPlugin {
    public static void ensure(){
        if(Global.getSector().getIntelManager().getFirstIntel(PTSDUnknownConvoyIntel.class)==null)
            Global.getSector().getIntelManager().addIntel(new PTSDUnknownConvoyIntel(),!PTSDCrisisAPI.isNewsIntelSubscribed());
    }
    protected String getName(){return "未知舰船遭遇";}
    public String getSmallDescriptionTitle(){return getName();}
    public void createSmallDescription(TooltipMakerAPI info,float width,float height){
        info.addPara("据沿途航船报告，未知运输编队经过后出现大面积漂流残片。以下接触位置随最新回执更新，可追踪并尝试拦截。",8);
        PTSDCrisisState state=PTSDCrisisState.get();if(state==null || state.late.mobilizationConvoys==null)return;
        for(PTSDMobilizationConvoys.Record r:state.late.mobilizationConvoys){
            CampaignFleetAPI f=PTSDNewsScene.fleet(r.fleetId);
            if(r.settled || f==null){info.addPara(r.delivered?"接触已在目的地附近消失。":"接触中断，运输未获确认。",5,Misc.getGrayColor());continue;}
            info.addPara("%s / %s",8,Misc.getHighlightColor(),f.getName(),f.getContainingLocation().getName());
            info.addButton("追踪当前接触",r.fleetId,width,24,3);
        }
    }
    public void buttonPressConfirmed(Object id,IntelUIAPI ui){
        CampaignFleetAPI fleet=PTSDNewsScene.fleet(String.valueOf(id));if(fleet!=null)Global.getSector().layInCourseFor(fleet);
    }
    public SectorEntityToken getMapLocation(SectorMapAPI map){
        PTSDCrisisState state=PTSDCrisisState.get();if(state==null || state.late.mobilizationConvoys==null)return null;
        for(PTSDMobilizationConvoys.Record r:state.late.mobilizationConvoys)if(!r.settled){CampaignFleetAPI fleet=PTSDNewsScene.fleet(r.fleetId);if(fleet!=null)return fleet;}
        return null;
    }
    public Set<String> getIntelTags(SectorMapAPI map){Set<String> tags=super.getIntelTags(map);tags.add("新闻");tags.add("危机");return tags;}
}
