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
    private static final String EXECUTE_INCIDENT = "PTSD_CRISIS_EXECUTE_INCIDENT:";
    private static final String ABANDON_INCIDENT = "PTSD_ABANDON_INVESTIGATION:";
    private static final String GO_INCIDENT = "PTSD_CRISIS_GO_INCIDENT:";
    private static final String GO_TRACE = "PTSD_CRISIS_GO_TRACE:";
    private static final String GO_FT09 = "PTSD_CRISIS_GO_FT09";
    private static final String TOGGLE_NEWS = "PTSD_CRISIS_TOGGLE_NEWS";
    private static final String SORT = "PTSD_CRISIS_SORT:";
    /** 存入Intel以保留玩家选择；旧存档缺失此字段时默认最新消息优先。 */
    private PTSDInvestigationList.Order sortOrder = PTSDInvestigationList.Order.NEWEST;
    private PTSDInvestigationList.Order order() {
        return sortOrder == null ? PTSDInvestigationList.Order.NEWEST : sortOrder;
    }
    private boolean devOnlyPreview;
    private String focusedIncidentId;
    public void focusIncident(String id) { focusedIncidentId = id; }

    // Confirmation can occur at sensor range before the player reaches the object.
    // Keep confirmed coordinates actionable, just as the dossier keeps their entry.
    static boolean canTrackIncident(PTSDCrisisState.CrisisIncident incident) {
        return PTSDInvestigationList.visible(incident, PTSDCrisisState.getDay())
                && !PTSDNewsPresentation.nonSpatial(incident.cardId)
                && (!incident.investigationResolved || incident.investigationReal);
    }
    static boolean isHighEnergyAssignment(PTSDCrisisState state, PTSDCrisisState.CrisisIncident incident) {
        return incident != null && (incident.jePlayerAssignment ||
                (state != null && incident.id != null && incident.id.equals(state.jePlayerTaskIncidentId)));
    }

    public PTSDCrisisIntel() { setImportant(true); }
    public static PTSDCrisisIntel ensureIntel() {
        if (Global.getSector() == null) return null;
        Object existing=Global.getSector().getIntelManager().getFirstIntel(PTSDCrisisIntel.class);
        if(existing instanceof PTSDCrisisIntel && (((PTSDCrisisIntel)existing).isEnded() || ((PTSDCrisisIntel)existing).isEnding())) {
            Global.getSector().getIntelManager().removeIntel((PTSDCrisisIntel)existing); existing=null;
        }
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
    @Override public boolean hasLargeDescription(){return true;}
    @Override public void createLargeDescription(com.fs.starfarer.api.ui.CustomPanelAPI panel,float width,float height){
        PTSDCommandSurface.screenDocument(panel,width,height,getName(),"失联信号 / 调查席",PTSDCommandSurface.INK,
                new PTSDCommandSurface.Content(){public void draw(TooltipMakerAPI info,float w,float h){createSmallDescription(info,w,h);}});
    }

    @Override public void createSmallDescription(TooltipMakerAPI info,float width,float height){
        info.setParaFont("graphics/fonts/insignia21LTaa.fnt");
        PTSDCrisisState state=PTSDCrisisState.get(); float opad=10f; Color h=Misc.getHighlightColor();
        Color omega=getFactionForUIColors().getBaseUIColor(); Color dark=getFactionForUIColors().getDarkUIColor();
        info.addPara("一些来源不明的失联记录正从边缘星系累积。已接取的 Je 委托归入“高能信号”；主动记录的新闻归入“已记录的新闻调查”。",opad);
        if(state==null)return;
        PTSDCrisisState.CrisisIncident focused = PTSDCrisisAPI.getIncident(focusedIncidentId);
        if (canTrackIncident(focused)) {
            info.addPara(isHighEnergyAssignment(state,focused)?"当前高能信号：%s":"当前任务：%s", 8f, h, PTSDPlayerNewsText.headline(focused));
            info.addButton("导航至当前调查地点", GO_INCIDENT + focused.id, omega, dark, width, 28f, 4f);
        }
        boolean subscribed=PTSDCrisisAPI.isNewsIntelSubscribed();
        info.addPara("新闻推送：%s。",opad,subscribed?Misc.getPositiveHighlightColor():Misc.getGrayColor(),subscribed?"已订阅":"未订阅");
        info.addButton(subscribed?"停止订阅新闻推送":"订阅新闻推送",TOGGLE_NEWS,omega,dark,width,30f,2f);
        if(state.visibleStage<=0)info.addPara("当前没有可靠的阶段判断。",opad,Misc.getGrayColor());
        else if(state.visibleStage==1)info.addPara("推测阶段：%s。少量无法识别的舰队正在执行远距侦察。",opad,h,"远距侦察");
        else if(state.visibleStage==2)info.addPara("推测阶段：%s。若干星体参数开始偏离历史记录。",opad,h,"据点营建");
        else info.addPara("推测阶段：%s。异常活动正围绕若干星系固化。",opad,omega,"大规模行动准备");
        info.addPara("已确认的异常接触：%s；被目击的侦察单位：%s。",opad,h,String.valueOf(state.totalOmegaEncounters),String.valueOf(state.totalScoutSightings));
        info.addPara("排列：%s。时间按报道发布时间，距离按当前舰队至报道星系；各分支分别排列。",8f,h,order().label);
        com.fs.starfarer.api.ui.ButtonAPI previousSort = null;
        com.fs.starfarer.api.ui.ButtonAPI firstSort = null;
        for (PTSDInvestigationList.Order option : PTSDInvestigationList.Order.values()) {
            com.fs.starfarer.api.ui.ButtonAPI button = info.addButton(option.label, SORT + option.name(),
                    option == order() ? h : omega, dark, (width - 18f) / 4f, 30f, 4f);
            if (previousSort != null) button.getPosition().rightOfTop(previousSort,6f);
            else firstSort = button;
            previousSort = button;
        }
        info.addSpacer(0f).getPosition().belowLeft(firstSort,0f);
        if (state.late != null) {
            PTSDLateState.FTChain wreck = state.late.ftChains.get("FT-09");
            if (wreck != null && !wreck.resolved && state.resolveSystem(wreck.systemId) != null) {
                info.addSectionHeading("匿名残片线索", omega, dark,
                        com.fs.starfarer.api.ui.Alignment.MID, 12f);
                info.addPara("一份没有署名的定位数据指向 " +
                        state.resolveSystem(wreck.systemId).getName() +
                        " 的失事区域。坐标约剩 " +
                        Math.max(0, Math.round(wreck.expiresDay - PTSDCrisisState.getDay())) +
                        " 日有效。", 5f);
                info.addButton("前往匿名坐标", GO_FT09, omega, dark, width, 30f, 2f);
            }
        }

        java.util.List<PTSDCrisisState.CrisisIncident> listed = PTSDInvestigationList.incidents(state,order(),PTSDCrisisState.getDay());
        for(int group=0;group<2;group++) {
        boolean highEnergy=group==0;
        int leads=0;
        for(PTSDCrisisState.CrisisIncident incident:listed){
            if(incident.investigationAbandoned || isHighEnergyAssignment(state,incident)!=highEnergy)continue;
            if(!canTrackIncident(incident) && !(highEnergy && incident.recordedByPlayer))continue;
            StarSystemAPI system=state.resolveSystem(incident.targetSystemId); if(system==null)continue;
            if(leads++==0){
                info.addSectionHeading(highEnergy?"高能信号":"已记录的新闻调查",omega,dark,com.fs.starfarer.api.ui.Alignment.MID,12f);
                if(highEnergy)info.addPara("Je 委托 / 由你亲自调查。任务地点与导航列在下方；信号强度不代表已核实报道内容。",5f,Misc.getGrayColor());
            }
            String status=incident.investigationResolved?(incident.investigationReal?"已确认，位置保留":"调查已结束"):(incident.siteMaterialized?"现场已显现，等待抵近确认":"调查中，进入星系后投影现场");
            int left=Math.max(0,(int)Math.ceil(incident.investigationExpiresDay-PTSDCrisisState.getDay()));
            info.addPara("%s / %s / %s（剩余 %s 天）",6f,incident.investigationResolved?h:Misc.getTextColor(),PTSDPlayerNewsText.headline(incident),system.getName(),status,String.valueOf(left));
            if(Global.getSettings().isDevMode() && incident.siteMaterialized) {
                info.addPara("现场：%s / %s",3f,new Color(206,142,255),String.valueOf(incident.siteTitle),String.valueOf(incident.siteConfirmationHint));
            } else {
                String description=PTSDPlayerNewsText.report(incident);
                if(description==null || description.trim().isEmpty())description="暂无可用的新闻描述。";
                info.addPara("%s",4f,Misc.getTextColor(),description);
            }
            if (incident.scenarioTrigger != null && incident.investigationResolved)
                info.addPara("%s", 3f, Misc.getTextColor(), PTSDPlayerNewsText.summary(incident));
            if (PTSDConditionalNewsDirector.canChooseOperation(incident)) {
                boolean communication = "SHARE_INTEL".equals(incident.scenarioEffect) || "COORDINATE".equals(incident.scenarioEffect);
                info.addButton((communication ? "提交核验记录：" : "执行现场行动（10补给，驻留1日）：") + PTSDPlayerNewsText.action(incident),
                        EXECUTE_INCIDENT + incident.id, omega, dark, width, 30f, 2f);
            }
            if(canTrackIncident(incident)) {
                com.fs.starfarer.api.ui.ButtonAPI go=info.addButton((incident.investigationResolved?"追踪已确认位置：":"前往调查：")+system.getName(),GO_INCIDENT+incident.id,omega,dark,width*.75f-3f,30f,2f);
                com.fs.starfarer.api.ui.ButtonAPI cancel=info.addButton("放弃调查",ABANDON_INCIDENT+incident.id,new Color(205,95,95),new Color(55,22,27),width*.25f-3f,30f,0f);
                cancel.getPosition().rightOfTop(go,6f);
                // Restore the next row anchor after the right-hand button.
                info.addSpacer(0f).getPosition().belowLeft(go,0f);
            }
        }
        }
        int traces=0;
        for(PTSDCrisisState.SignalTrace trace:PTSDInvestigationList.traces(state,order(),PTSDCrisisState.getDay())){
            StarSystemAPI system=state.resolveSystem(trace.systemId);if(system==null)continue;
            if(traces++==0)info.addSectionHeading("十日内异常舰队目击",omega,dark,com.fs.starfarer.api.ui.Alignment.MID,12f);
            int left=Math.max(0,(int)Math.ceil(trace.expiresDay-PTSDCrisisState.getDay()));
            info.addPara("%s / %s / 剩余 %s 天",6f,h,trace.label,system.getName(),String.valueOf(left));
            info.addButton("前往目击位置："+system.getName(),GO_TRACE+trace.id,omega,dark,width,30f,2f);
        }
    }

    @Override public void buttonPressConfirmed(Object buttonId,IntelUIAPI ui){
        String systemId=null; SectorEntityToken target=null; PTSDCrisisState state=PTSDCrisisState.get();
        if(buttonId instanceof String&&state!=null){String id=(String)buttonId;
            if(id.startsWith(SORT)) {
                try { sortOrder=PTSDInvestigationList.Order.valueOf(id.substring(SORT.length())); }
                catch(IllegalArgumentException ignored) { sortOrder=PTSDInvestigationList.Order.NEWEST; }
                ui.updateUIForItem(this);return;
            }
            else if(TOGGLE_NEWS.equals(id)){PTSDCrisisAPI.setNewsIntelSubscribed(!PTSDCrisisAPI.isNewsIntelSubscribed());ui.updateUIForItem(this);return;}
            else if(id.startsWith(ABANDON_INCIDENT)) {
                abandon(PTSDCrisisAPI.getIncident(id.substring(ABANDON_INCIDENT.length())));
                ui.updateUIForItem(this); return;
            }
            else if(id.startsWith(EXECUTE_INCIDENT)) {
                if (!PTSDConditionalNewsDirector.startPlayerOperation(state, id.substring(EXECUTE_INCIDENT.length())))
                    Global.getSector().getCampaignUI().addMessage("行动未下达：请检查有效期、现场距离及10单位补给。", Misc.getGrayColor());
                ui.updateUIForItem(this); return;
            }
            else if(id.startsWith(GO_INCIDENT)){PTSDCrisisState.CrisisIncident item=PTSDCrisisAPI.getIncident(id.substring(GO_INCIDENT.length()));if(item!=null){systemId=item.targetSystemId;target=PTSDSearchAreaNavigation.resolve(item);}}
            else if(id.startsWith(GO_TRACE)){String traceId=id.substring(GO_TRACE.length());for(PTSDCrisisState.SignalTrace trace:state.signalTraces)if(trace!=null&&traceId.equals(trace.id)){systemId=trace.systemId;break;}}
            else if (GO_FT09.equals(id) && state.late != null) {
                PTSDLateState.FTChain wreck = state.late.ftChains.get("FT-09");
                if (wreck != null) { systemId = wreck.systemId; target = PTSDFTDirector.target(wreck); }
            }
        }
        if(target==null&&systemId!=null){StarSystemAPI system=state.resolveSystem(systemId);if(system!=null)target=system.getHyperspaceAnchor();}
        if(target!=null){Global.getSector().layInCourseFor(target);ui.updateUIForItem(this);return;}
        super.buttonPressConfirmed(buttonId,ui);
    }
    static void abandon(PTSDCrisisState.CrisisIncident incident) {
        if(incident==null || incident.investigationAbandoned)return;
        if((!incident.investigationResolved || (incident.scenarioTrigger!=null&&!incident.scenarioResolved)) && PTSDInvestigationRewards.hasEvidence(incident))
            PTSDInvestigationRewards.award(incident,1);
        incident.investigationAbandoned=true;
        PTSDNewsScene.unmark(incident);
        // Preserve evidence; an incomplete report does not count as a successful investigation.
        if(!incident.investigationResolved) {
            incident.investigationResolved=true;incident.investigationReal=false;
            incident.investigationResult=PTSDTaskRules.InvestigationResult.INCONCLUSIVE;
            incident.effectSummary="调查已由你主动终止，未取得的证据不作推断。";
        }
        if(incident.scene!=null) {
            incident.scene.resolved=true;incident.scene.downloading=false;incident.scene.timerArmed=false;
            incident.scene.result=incident.effectSummary;
        }
        // Already deployed pursuers finish their actual lifecycle; cancelling the UI cannot erase them.
        incident.investigationExpiresDay=PTSDCrisisState.getDay();
        if(incident.scene==null && incident.siteMaterialized)PTSDNewsSiteManager.cleanup(PTSDCrisisState.get(),incident,false);
    }
    @Override public String getIcon(){FactionAPI faction=getFactionForUIColors();return faction==null?null:faction.getCrest();}
    @Override public FactionAPI getFactionForUIColors(){FactionAPI watcher=Global.getSector()==null?null:Global.getSector().getFaction(IIRT_Omega_Invasion.WATCHER_FACTION);return watcher!=null?watcher:super.getFactionForUIColors();}
    @Override public Set<String> getIntelTags(SectorMapAPI map){Set<String>tags=super.getIntelTags(map);tags.add(IIRT_Omega_Invasion.WATCHER_FACTION);tags.add("危机");tags.add("新闻");if(devOnlyPreview)tags.add("DEV");return tags;}
    @Override public SectorEntityToken getMapLocation(SectorMapAPI map){
        PTSDCrisisState state=PTSDCrisisState.get();if(state==null)return null;
        PTSDCrisisState.CrisisIncident focused = PTSDCrisisAPI.getIncident(focusedIncidentId);
        if (canTrackIncident(focused)) {
            SectorEntityToken target = PTSDSearchAreaNavigation.resolve(focused);
            if (target != null) return target;
        }
        for(PTSDCrisisState.CrisisIncident item:PTSDInvestigationList.incidents(state,order(),PTSDCrisisState.getDay()))if(canTrackIncident(item)){SectorEntityToken target=PTSDSearchAreaNavigation.resolve(item);if(target!=null)return target;}
        for(PTSDCrisisState.SignalTrace trace:PTSDInvestigationList.traces(state,order(),PTSDCrisisState.getDay())){StarSystemAPI system=state.resolveSystem(trace.systemId);if(system!=null)return system.getHyperspaceAnchor();}
        return null;
    }
}
