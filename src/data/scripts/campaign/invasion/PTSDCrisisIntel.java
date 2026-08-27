package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.Set;

/** Player-facing crisis dossier: stage estimates, recorded investigations and recent sightings. */
public final class PTSDCrisisIntel extends BaseIntelPlugin {
    private static final long serialVersionUID = 2L;
    private static final String GO_INCIDENT = "PTSD_CRISIS_GO_INCIDENT:";
    private static final String GO_TRACE = "PTSD_CRISIS_GO_TRACE:";
    private boolean devOnlyPreview;

    public PTSDCrisisIntel() { setImportant(true); }
    public static PTSDCrisisIntel ensureIntel() {
        if (Global.getSector() == null) return null;
        Object existing=Global.getSector().getIntelManager().getFirstIntel(PTSDCrisisIntel.class);
        if (existing instanceof PTSDCrisisIntel) {
            PTSDCrisisIntel intel=(PTSDCrisisIntel)existing; intel.devOnlyPreview=false;
            PTSDCrisisState state=PTSDCrisisState.get(); if(state!=null)state.preWarIntelCreated=true; return intel;
        }
        PTSDCrisisIntel intel=new PTSDCrisisIntel(); Global.getSector().getIntelManager().addIntel(intel);
        PTSDCrisisState state=PTSDCrisisState.get(); if(state!=null)state.preWarIntelCreated=true; return intel;
    }
    public static PTSDCrisisIntel ensureDevPreview() {
        if(Global.getSector()==null||!Global.getSettings().isDevMode())return null;
        Object existing=Global.getSector().getIntelManager().getFirstIntel(PTSDCrisisIntel.class);
        if(existing instanceof PTSDCrisisIntel)return (PTSDCrisisIntel)existing;
        PTSDCrisisIntel intel=new PTSDCrisisIntel(); intel.devOnlyPreview=true;
        Global.getSector().getIntelManager().addIntel(intel,true); return intel;
    }
    public static void removeDevPreview() {
        if(Global.getSector()==null)return;
        for(com.fs.starfarer.api.campaign.comm.IntelInfoPlugin plugin:new java.util.ArrayList<com.fs.starfarer.api.campaign.comm.IntelInfoPlugin>(Global.getSector().getIntelManager().getIntel(PTSDCrisisIntel.class))){
            PTSDCrisisIntel intel=(PTSDCrisisIntel)plugin; if(intel.devOnlyPreview)Global.getSector().getIntelManager().removeIntel(intel);
        }
    }
    public void replaceWithWarIntel(){if(!isEnding()&&!isEnded())endImmediately();}
    @Override protected String getName(){return devOnlyPreview?"[DEV预览] 边缘失联信号":"边缘失联信号";}
    @Override public boolean shouldRemoveIntel(){return(devOnlyPreview&&!Global.getSettings().isDevMode())||super.shouldRemoveIntel();}
    @Override public String getSmallDescriptionTitle(){return getName();}

    @Override public void createSmallDescription(TooltipMakerAPI info,float width,float height){
        PTSDCrisisState state=PTSDCrisisState.get(); float opad=10f; Color h=Misc.getHighlightColor();
        Color omega=getFactionForUIColors().getBaseUIColor(); Color dark=getFactionForUIColors().getDarkUIColor();
        info.addPara("一些来源不明的失联记录正从边缘星系累积。新闻线索只有在主动记录后才会进入此处。",opad);
        if(state==null)return;
        if(state.visibleStage<=0)info.addPara("当前没有可靠的阶段判断。",opad,Misc.getGrayColor());
        else if(state.visibleStage==1)info.addPara("推测阶段：%s。少量无法识别的舰队正在执行远距侦察。",opad,h,"远距侦察");
        else if(state.visibleStage==2)info.addPara("推测阶段：%s。若干星体参数开始偏离历史记录。",opad,h,"据点营建");
        else info.addPara("推测阶段：%s。异常活动正围绕若干星系固化。",opad,omega,"大规模行动准备");
        info.addPara("已确认的异常接触：%s；被目击的侦察单位：%s。",opad,h,String.valueOf(state.totalOmegaEncounters),String.valueOf(state.totalScoutSightings));

        int leads=0;
        for(PTSDCrisisState.CrisisIncident incident:state.incidents){
            if(incident==null||!incident.recordedByPlayer||(incident.investigationResolved&&!incident.investigationReal))continue;
            StarSystemAPI system=state.resolveSystem(incident.targetSystemId); if(system==null)continue;
            if(leads++==0)info.addSectionHeading("已记录的新闻调查",omega,dark,com.fs.starfarer.api.ui.Alignment.MID,12f);
            String status=incident.investigationResolved?"已确认，位置保留":(incident.siteMaterialized?"现场已显现，等待抵近确认":"调查中，进入星系后投影现场");
            int left=Math.max(0,(int)Math.ceil(incident.investigationExpiresDay-PTSDCrisisState.getDay()));
            info.addPara("%s｜%s｜%s（剩余 %s 天）",6f,incident.investigationResolved?h:Misc.getTextColor(),incident.headline,system.getName(),status,String.valueOf(left));
            if(incident.siteMaterialized)info.addPara("现场：%s｜%s",3f,new Color(206,142,255),String.valueOf(incident.siteTitle),String.valueOf(incident.siteConfirmationHint));
            if(!incident.investigationResolved)info.addButton("前往调查："+system.getName(),GO_INCIDENT+incident.id,omega,dark,width,23f,2f);
        }
        int traces=0;
        for(PTSDCrisisState.SignalTrace trace:PTSDCrisisAPI.getActiveSignalTraces()){
            StarSystemAPI system=state.resolveSystem(trace.systemId);if(system==null)continue;
            if(traces++==0)info.addSectionHeading("十日内异常舰队目击",omega,dark,com.fs.starfarer.api.ui.Alignment.MID,12f);
            int left=Math.max(0,(int)Math.ceil(trace.expiresDay-PTSDCrisisState.getDay()));
            info.addPara("%s｜%s｜剩余 %s 天",6f,h,trace.label,system.getName(),String.valueOf(left));
            info.addButton("前往目击位置："+system.getName(),GO_TRACE+trace.id,omega,dark,width,23f,2f);
        }
    }

    @Override public void buttonPressConfirmed(Object buttonId,IntelUIAPI ui){
        String systemId=null; SectorEntityToken target=null; PTSDCrisisState state=PTSDCrisisState.get();
        if(buttonId instanceof String&&state!=null){String id=(String)buttonId;
            if(id.startsWith(GO_INCIDENT)){PTSDCrisisState.CrisisIncident item=PTSDCrisisAPI.getIncident(id.substring(GO_INCIDENT.length()));if(item!=null){systemId=item.targetSystemId;target=PTSDCrisisAPI.resolveIncidentTarget(item);}}
            else if(id.startsWith(GO_TRACE)){String traceId=id.substring(GO_TRACE.length());for(PTSDCrisisState.SignalTrace trace:state.signalTraces)if(trace!=null&&traceId.equals(trace.id)){systemId=trace.systemId;break;}}
        }
        if(target==null&&systemId!=null){StarSystemAPI system=state.resolveSystem(systemId);if(system!=null)target=system.getHyperspaceAnchor();}
        if(target!=null){Global.getSector().layInCourseFor(target);ui.updateUIForItem(this);return;}
        super.buttonPressConfirmed(buttonId,ui);
    }
    @Override public String getIcon(){FactionAPI faction=getFactionForUIColors();return faction==null?null:faction.getCrest();}
    @Override public FactionAPI getFactionForUIColors(){FactionAPI watcher=Global.getSector()==null?null:Global.getSector().getFaction(IIRT_Omega_Invasion.WATCHER_FACTION);return watcher!=null?watcher:super.getFactionForUIColors();}
    @Override public Set<String> getIntelTags(SectorMapAPI map){Set<String>tags=super.getIntelTags(map);tags.add(IIRT_Omega_Invasion.WATCHER_FACTION);tags.add("危机");tags.add("新闻");if(devOnlyPreview)tags.add("DEV");return tags;}
    @Override public SectorEntityToken getMapLocation(SectorMapAPI map){
        PTSDCrisisState state=PTSDCrisisState.get();if(state==null)return null;
        for(PTSDCrisisState.CrisisIncident item:state.incidents)if(item!=null&&item.recordedByPlayer&&(!item.investigationResolved||item.investigationReal)){SectorEntityToken target=PTSDCrisisAPI.resolveIncidentTarget(item);if(target!=null)return target;}
        for(PTSDCrisisState.SignalTrace trace:PTSDCrisisAPI.getActiveSignalTraces()){StarSystemAPI system=state.resolveSystem(trace.systemId);if(system!=null)return system.getHyperspaceAnchor();}
        return null;
    }
}