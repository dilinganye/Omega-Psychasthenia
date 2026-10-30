package data.scripts.campaign.invasion;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
public final class PTSDWarRescueIntel extends BaseIntelPlugin {
    private PTSDWarRescue.Operation operation;
    public PTSDWarRescueIntel(PTSDWarRescue.Operation o){operation=o;setImportant(true);}
    protected String getName(){return "战时求救 / "+new String[]{"残骸中的呼救","孤立接触","撤离船队","交战求援"}[operation.kind]+(operation.ended?"（已结束）":"");}
    public String getIcon(){return Global.getSettings().getSpriteName("intel","distress_call");}
    public SectorEntityToken getMapLocation(SectorMapAPI map){for(String id:operation.humans){CampaignFleetAPI f=PTSDWarRescue.fleet(id);if(f!=null&&!operation.arrived.contains(id))return f;}StarSystemAPI sys=PTSDCrisisState.get().resolveSystem(operation.systemId);return sys==null?null:sys.getHyperspaceAnchor();}
    public void createSmallDescription(TooltipMakerAPI info,float width,float height){
        info.addPara("求救信号仍在重复。追兵正在接近，人类舰队请求你帮助他们打开撤离窗口。",0);
        info.addPara(operation.rewarded?"救援谢礼已结算。幸存者正在撤往联盟。":"需要实际接敌并帮助舰队脱险；仅到达星系不算完成救援。",10);
        for(String id:operation.humans){CampaignFleetAPI f=PTSDWarRescue.fleet(id);
            info.addPara(operation.arrived.contains(id)?"已抵达联盟并加入防御":f==null?"舰队失联或被摧毁":f.getName()+" / 现存兵力 "+f.getFleetPoints(),6);
            if(f!=null&&!operation.arrived.contains(id))info.addButton("追踪这支求救舰队","RESCUE_GO:"+id,Misc.getBasePlayerColor(),java.awt.Color.BLACK,width,24,4);
        }
    }
    public void buttonPressConfirmed(Object id,IntelUIAPI ui){if(id instanceof String&&((String)id).startsWith("RESCUE_GO:")){CampaignFleetAPI f=PTSDWarRescue.fleet(((String)id).substring(10));if(f!=null)Global.getSector().layInCourseFor(f);}}
}
