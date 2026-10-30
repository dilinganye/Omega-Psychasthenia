package data.scripts.campaign.invasion;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import java.util.*;
/** Explicit one-case-at-a-time queue; preview never invokes a fleet factory. */
public final class PTSDSceneDevIntel extends BaseIntelPlugin {
    private final List<String> queue=new ArrayList<String>();private int cursor;
    private boolean showDisabled;
    private PTSDCrisisState.Phase reviewPhase=PTSDCrisisState.Phase.RECON;
    private String directRequest="D-01:0",templateFilter="";
    private transient TextFieldAPI requestField,filterField;
    private String selectedId,status="尚未建立测试队列";private transient TooltipMakerAPI body;private float scroll;
    public static PTSDSceneDevIntel ensure(){for(com.fs.starfarer.api.campaign.comm.IntelInfoPlugin p:Global.getSector().getIntelManager().getIntel(PTSDSceneDevIntel.class))return (PTSDSceneDevIntel)p;
        PTSDSceneDevIntel panel=new PTSDSceneDevIntel();panel.setImportant(true);Global.getSector().getIntelManager().addIntel(panel,true);return panel;}
    protected String getName(){return "[DEV] 事件现场与舰队测试";}
    @Override public Set<String> getIntelTags(SectorMapAPI map){
        Set<String> tags=super.getIntelTags(map);
        tags.add(com.fs.starfarer.api.impl.campaign.ids.Tags.INTEL_IMPORTANT);
        tags.add("危机");tags.add("DEV");return tags;
    }
    public boolean hasLargeDescription(){return true;}
    public void createSmallDescription(TooltipMakerAPI info,float w,float h){info.addPara("逐事件/差分测试；不会同时生成全部舰队。",5);}
    private PTSDCrisisState.CrisisIncident selected(){PTSDCrisisState s=PTSDCrisisState.get();if(s!=null)for(PTSDCrisisState.CrisisIncident i:s.incidents)if(i.id.equals(selectedId))return i;return null;}
    public void createLargeDescription(CustomPanelAPI panel,float w,float h){
        body=panel.createUIElement(w,h,true);if(!Global.getSettings().isDevMode()){body.addPara("仅开发模式可用。",0);panel.addUIElement(body);return;}
        PTSDCrisisState s=PTSDCrisisState.get();body.addPara("%s",0,Misc.getHighlightColor(),status);
        body.addSectionHeading("战前事件分阶段验收",Alignment.MID,10);
        for(PTSDCrisisState.Phase phase:new PTSDCrisisState.Phase[]{PTSDCrisisState.Phase.DORMANT,PTSDCrisisState.Phase.RECON,PTSDCrisisState.Phase.EXPANSION,PTSDCrisisState.Phase.FORTIFICATION,PTSDCrisisState.Phase.MOBILIZATION})
            body.addButton((phase==reviewPhase?"[当前查看] ":"")+phase.name(),"REVIEW:"+phase.name(),w-24,24,3);
        body.addPara("查看 "+reviewPhase+" / 存档实际阶段 "+s.phase+"。页签只筛选清单，不跳过危机流程；生成仍检查实际阶段和前置。0权重不投放，但可定向DEV测试。",6);
        body.addButton(showDisabled?"隐藏暂禁条目":"同时显示暂禁条目","SHOW_DISABLED",w-24,24,3);
        body.addButton("建立所选阶段的启用调查队列","BUILD_REVIEW",w-24,26,4);
        for(PTSDCrisisIncidentManager.DevCard card:PTSDCrisisIncidentManager.getDevCardsForPhase(reviewPhase))if(card.investigable&&(showDisabled||card.eventWeight>0)){
            body.addPara("%s",4,card.eventWeight>0?Misc.getHighlightColor():Misc.getGrayColor(),card.id+" / "+card.headline+" / 权重倍率 "+card.eventWeight+" / "+card.siteTemplate);
            if(card.eventWeight>0)body.addButton("生成 "+card.id+" 的属实现场","DIRECT:"+card.id,w-24,24,2);
        }
        body.addPara("指定编号与差分（例 D-01:0；-1为不属实，0/1/2为属实分支）",6);
        requestField=body.addTextField(w-24,4);requestField.setText(directRequest);
        body.addButton("检查指定编号前置（不生成）","CHECK_DIRECT",w-24,24,3);
        body.addButton("生成指定编号与差分","RUN_DIRECT",w-24,24,3);
        body.addPara("队列模板过滤（空白为全部，可填模板名片段）",6);
        filterField=body.addTextField(w-24,4);filterField.setText(templateFilter);
        body.addButton("跳过下一项（不生成、不结算）","SKIP",w-24,24,3);
        body.addButton("建立当前阶段测试队列", "BUILD", w-24,26,8);
        body.addButton("触发下一项（含互斥差分）", "NEXT", w-24,26,5);
        body.addButton("重试当前项", "RETRY", w-24,26,5);
        body.addButton("显式建立要塞前置", "FORT", w-24,26,5);
        for(String card:new String[]{"D-01","D-02","D-03","D-07","D-12","JE-01","R2-03"})
            body.addButton("定向生成属实现场："+card,"DIRECT:"+card,w-24,24,3);
        body.addButton("D-02：热寂独行彩蛋", "RARE:100",w-24,24,3);
        body.addButton("D-02：熵级运输与护卫彩蛋", "RARE:101",w-24,24,3);
        body.addButton("风暴侦察测试（RECON超空间，跳过随机与风暴条件）","STORM",w-24,24,3);
        body.addButton("黑洞毁灭演出预览（无伤害，关闭窗口后播放）","COLLAPSE_PREVIEW",w-24,24,3);
        body.addButton("停止黑洞演出预览","COLLAPSE_STOP",w-24,24,3);
        body.addButton("生成一支战前运输（仅动员期）","CONVOY",w-24,24,3);
        body.addPara("队列位置 "+cursor+" / "+queue.size()+"；缺前置会列出原因，不自动造目标。",6);
        PTSDCrisisFleetLimits limits=PTSDCrisisFleetLimits.forPhase(s,PTSDCrisisState.getDay());
        body.addPara("阶段生成上限：单队 "+Math.round(limits.perFleetFp)+" FP / 单现场 "+Math.round(limits.totalFp)+" FP / "+limits.maxShips+" 艘。实际舰型由对应工厂生成，预览不造舰。",6);
        preview(limits,w);
        PTSDCrisisState.CrisisIncident i=selected();
        if(i!=null){body.addPara("%s",12,Misc.getHighlightColor(),i.cardId+" / "+i.headline);
            body.addButton("清理本次DEV现场（仅自建实体，无奖励）","CLEAN_SELECTED",w-24,24,4);
            body.addPara("%s",5,Misc.getTextColor(),"模板 "+i.siteTemplate+" / 分支 "+i.randomBranch+" / 目标 "+i.targetSystemId);
            if(i.eventLoc!=null&&!i.eventLoc.isEmpty())body.addPara("%s",5,Misc.getGrayColor(),"eventLoc: "+i.eventLoc);
            if(i.eventError!=null)body.addPara("%s",5,Misc.getNegativeHighlightColor(),"扩展事件错误："+i.eventError);
            data.scripts.campaign.invasion.news.NewsEvent extension=data.scripts.campaign.invasion.news.NewsEventLoader.resolve(i.eventLoc);
            if(extension!=null)try{
                data.scripts.campaign.invasion.news.NewsEventContext context=new data.scripts.campaign.invasion.news.NewsEventContext(s,i,PTSDCrisisState.getDay());
                String details=extension.devStatus(context);if(details!=null)body.addPara("%s",5,Misc.getHighlightColor(),details);
                for(String fleetId:extension.devFleetIds(context)){CampaignFleetAPI f=PTSDNewsScene.fleet(fleetId);if(f==null)continue;
                    body.addPara("%s",5,Misc.getTextColor(),f.getName()+" / "+f.getFleetPoints()+" FP");
                    body.addShipList(6,Math.max(1,(f.getFleetData().getMembersListCopy().size()+5)/6),64,Misc.getBasePlayerColor(),f.getFleetData().getMembersListCopy(),4);}
                for(Map.Entry<String,String> action:extension.devActions(context).entrySet())body.addButton(action.getValue(),"EXT:"+action.getKey(),w-24,24,3);
            }catch(Throwable error){body.addPara("%s",5,Misc.getNegativeHighlightColor(),"扩展DEV预览失败："+error.getMessage());}
            SectorEntityToken target=PTSDNewsScene.entity(i.targetEntityId);if(target!=null){body.addPara("现场坐标 "+Math.round(target.getLocation().x)+", "+Math.round(target.getLocation().y),4);body.addButton("导航至实际现场","NAV",w-24,26,5);}
            if(i.scene!=null){body.addPara("%s",5,Misc.getTextColor(),"已结束 "+i.scene.resolved+" / 等待响应 "+i.scene.responseQueued+" / "+i.scene.lastError);
                if(i.scene.blockade!=null)body.addPara("%s",3,Misc.getHighlightColor(),"封锁截止日 "+PTSDJumpBlockade.deadline(i.scene.blockade.endDay,i.scene.blockade.cancelledDay)+" / 实体化 "+i.scene.blockade.spawned+" / 航路已恢复 "+i.scene.blockade.restored);
                else if(i.scene.recon!=null)body.addPara("%s",3,Misc.getHighlightColor(),"照射次数/跟踪日数 "+i.scene.recon.observed+" / 压制秒数 "+Math.round(i.scene.recon.seconds)+" / 正在护送 "+i.scene.escort+" / 撤离 "+i.scene.recon.escaping);
                else if(i.scene.kind==PTSDNewsScene.Kind.FORT_PATROL_GAP)body.addPara("%s",3,Misc.getHighlightColor(),"窗口取证 "+i.scene.downloadProgress+" / 0.25 日；实际巡逻队 "+i.scene.operationNodeId);
                else body.addPara("已采样 "+i.scene.samples,3);
                for(String id:i.scene.fleets.keySet()){CampaignFleetAPI f=PTSDNewsScene.fleet(id);if(f==null)continue;body.addPara("%s",8,Misc.getHighlightColor(),f.getName()+" / "+f.getFleetPoints()+" FP / 退场日 "+i.scene.fleets.get(id));body.addShipList(6,Math.max(1,(f.getFleetData().getMembersListCopy().size()+5)/6),64,Misc.getBasePlayerColor(),f.getFleetData().getMembersListCopy(),4);}
                for(String id:i.scene.nodes){SectorEntityToken n=PTSDNewsScene.entity(id);if(n!=null&&n.getStarSystem()!=null)body.addPara("%s",3,Misc.getGrayColor(),n.getStarSystem().getName()+" / "+n.getName()+" / "+Math.round(n.getLocation().x)+", "+Math.round(n.getLocation().y));}}
            StarSystemAPI sys=s.resolveSystem(i.targetSystemId);if(sys!=null)body.addSectorMap(w-32,220,sys,10);
        }
        panel.addUIElement(body).inTL(0,0);if(body.getExternalScroller()!=null)body.getExternalScroller().setYOffset(scroll);
    }
    private void preview(PTSDCrisisFleetLimits limits,float width){
        java.util.List<com.fs.starfarer.api.fleet.FleetMemberAPI> ships=new ArrayList<>();
        java.util.Set<String> seen=new LinkedHashSet<>();
        for(data.hullmods.shard.PTSD_BaseShard_Util.BranchDefinition branch:data.hullmods.shard.PTSD_BaseShard_Util.getRegisteredBranches()){
            for(java.util.Map.Entry<com.fs.starfarer.api.combat.ShipAPI.HullSize,data.hullmods.shard.PTSD_BaseShard_Util.ShardTypeVariants> entry:branch.variants.entrySet()){
                if(entry.getKey()==com.fs.starfarer.api.combat.ShipAPI.HullSize.FIGHTER||entry.getKey().ordinal()>limits.maxHullSize.ordinal())continue;
                com.fs.starfarer.api.util.WeightedRandomPicker<String> candidates=entry.getValue().variants.get(data.hullmods.shard.PTSD_BaseShard_Util.ShardType.GENERAL);
                if(candidates==null)continue;
                for(String variant:candidates.getItems())if(seen.add(variant)&&Global.getSettings().doesVariantExist(variant)){
                    if(Global.getSettings().getVariant(variant).getHullSpec().getFleetPoints()>limits.perFleetFp)continue;
                    ships.add(Global.getFactory().createFleetMember(com.fs.starfarer.api.fleet.FleetMemberType.SHIP,variant));break;
                }
            }
        }
        body.addPara("阶段可用舰型预览（各分支候选，并非承诺的随机编成；仅创建UI舰船资料）",6);
        int columns=Math.max(1,(int)((width-24)/68));
        if(!ships.isEmpty())body.addShipList(columns,(ships.size()+columns-1)/columns,64,Misc.getBasePlayerColor(),ships,5);
    }
    public void buttonPressConfirmed(Object buttonId,IntelUIAPI ui){
        if(!Global.getSettings().isDevMode())return;if(body!=null&&body.getExternalScroller()!=null)scroll=body.getExternalScroller().getYOffset();String id=String.valueOf(buttonId);
        if(requestField!=null)directRequest=requestField.getText();if(filterField!=null)templateFilter=filterField.getText();
        if(id.startsWith("EXT:")){
            PTSDCrisisState.CrisisIncident i=selected();
            if(i!=null)try{
                data.scripts.campaign.invasion.news.NewsEvent extension=data.scripts.campaign.invasion.news.NewsEventLoader.resolve(i.eventLoc);
                if(extension!=null)status=extension.runDevAction(new data.scripts.campaign.invasion.news.NewsEventContext(PTSDCrisisState.get(),i,PTSDCrisisState.getDay()),id.substring(4));
            }catch(Throwable error){status="扩展DEV操作失败："+error.getMessage();}
        }
        else if("SHOW_DISABLED".equals(id)){showDisabled=!showDisabled;}
        else if(id.startsWith("REVIEW:")){reviewPhase=PTSDCrisisState.Phase.valueOf(id.substring(7));scroll=0;}
        else if("BUILD_REVIEW".equals(id)){queue.clear();cursor=0;for(PTSDCrisisIncidentManager.DevCard card:PTSDCrisisIncidentManager.getDevCardsForPhase(reviewPhase))if(card.investigable&&card.eventWeight>0&&matchesTemplate(PTSDCrisisIncidentManager.getTemplate(card.id),templateFilter))for(int branch:branches(card.id))queue.add(card.id+":"+branch);status="已建立 "+reviewPhase+" 启用调查队列；逐项触发，未改变实际阶段。";}
        else if("CHECK_DIRECT".equals(id)||"RUN_DIRECT".equals(id)){
            String entry=normalizeRequest(directRequest);
            if(entry==null)status="格式无效：请填新闻编号:差分；差分只允许-1、0、1、2、100、101。";
            else if("CHECK_DIRECT".equals(id))status=entry+"："+PTSDCrisisIncidentManager.unavailableReason(entry.split(":")[0])+"（仅检查，未生成）";
            else{queue.add(cursor,entry);runNext();}
        }
        else if("SKIP".equals(id)){status=cursor<queue.size()?"已跳过 "+queue.get(cursor++)+"；未生成或结算。":"队列已结束";}
        else if("BUILD".equals(id)){queue.clear();cursor=0;for(PTSDCrisisIncidentManager.DevCard card:PTSDCrisisIncidentManager.getDevCardsForCurrentPhase())if(matchesTemplate(PTSDCrisisIncidentManager.getTemplate(card.id),templateFilter))for(int branch:branches(card.id))queue.add(card.id+":"+branch);
            for(PTSDCrisisIncidentManager.ScriptedTemplate t:PTSDCrisisIncidentManager.getTemplates("SCRIPTED",PTSDCrisisState.get().phase))if(matchesTemplate(t,templateFilter))queue.add(t.id+":0");status="队列已建立；每次仅执行一项。";}
        else if(id.startsWith("RARE:")){queue.add(cursor,"D-02:"+id.substring(5));runNext();}
        else if(id.startsWith("DIRECT:")){queue.add(cursor,id.substring(7)+":0");runNext();}
        else if("CLEAN_SELECTED".equals(id)){status=cleanSelected(PTSDCrisisState.get(),selected());}
        else if("COLLAPSE_PREVIEW".equals(id)){status=PTSDFortressCollapseVisual.preview()?"演出已准备：关闭Intel并解除暂停查看。仅预览，不破坏星系或舰队。":"无法预览：需要战役玩家舰队。";}
        else if("COLLAPSE_STOP".equals(id)){PTSDFortressCollapseVisual.stopPreview();status="已停止演出预览。";}
        else if("STORM".equals(id)){PTSDStormScoutEncounter.devSpawn();status="已请求风暴接触；需要RECON阶段且位于超空间。自然每月抽签仍需另外验证。";}
        else if("CONVOY".equals(id)){PTSDCrisisState s=PTSDCrisisState.get();status=s.phase==PTSDCrisisState.Phase.MOBILIZATION&&PTSDMobilizationConvoys.spawn(s,PTSDCrisisState.getDay())?"已生成一支实际运输及押运；影响真实测试存档。":"未生成：请检查动员阶段及己方据点。";}
        else if("NEXT".equals(id)||"RETRY".equals(id)){if("RETRY".equals(id)&&cursor>0)cursor--;runNext();}
        else if("FORT".equals(id)){PTSDFortressDirector.devStartNext(PTSDCrisisState.get(),Global.getSector().getPlayerFleet().getStarSystem());status="已请求创建要塞测试前置；下一步重试事件。";}
        else if("NAV".equals(id)){PTSDCrisisState.CrisisIncident i=selected();SectorEntityToken target=i==null?null:PTSDNewsScene.entity(i.targetEntityId);if(target!=null)Global.getSector().getCampaignUI().showCoreUITab(com.fs.starfarer.api.campaign.CoreUITabId.MAP,target);}
        ui.updateUIForItem(this);
    }
    static String cleanSelected(PTSDCrisisState state,PTSDCrisisState.CrisisIncident incident){
        if(incident!=null&&incident.devForced&&incident.siteHandlerExpression!=null&&!incident.siteHandlerExpression.isEmpty()){
            Set<String> owned=new LinkedHashSet<String>();if(incident.siteEntityIds!=null)owned.addAll(incident.siteEntityIds);
            data.scripts.campaign.invasion.news.NewsEvent extension=data.scripts.campaign.invasion.news.NewsEventLoader.resolve(incident.eventLoc);
            if(extension!=null)owned.addAll(extension.devFleetIds(new data.scripts.campaign.invasion.news.NewsEventContext(state,incident,PTSDCrisisState.getDay())));
            for(String id:owned){
                CampaignFleetAPI fleet=PTSDNewsScene.fleet(id);
                if(fleet!=null&&fleet.getBattle()!=null)return "扩展现场舰队仍在交战，脱战后再清理。";
            }
            // DEV丢弃没有证据报酬；沿用扩展处理器自己的清理回调，并保留借用设施。
            incident.investigationAbandoned=true;incident.investigationResolved=true;
            incident.investigationResult=PTSDTaskRules.InvestigationResult.INCONCLUSIVE;
            new data.scripts.campaign.invasion.news.NewsEventContext(state,incident,PTSDCrisisState.getDay()).data().put("framework.devDiscard","true");
            PTSDNewsSiteManager.cleanup(state,incident,false);
            data.scripts.campaign.invasion.news.NewsEventLoader.advance(state,PTSDCrisisState.getDay());
            return "扩展事件已结束并执行清理；借用设施保留，不发放标准调查报告。";
        }
        if(incident==null||!incident.devForced||incident.scene==null)return "仅清理本面板生成且具有现场账本的DEV调查；原专属链及自然事件不清理。";
        Set<String> ids=new LinkedHashSet<String>(incident.scene.owned);ids.addAll(incident.scene.fleets.keySet());
        for(String entityId:ids){CampaignFleetAPI f=PTSDNewsScene.fleet(entityId);if(f!=null&&f.getBattle()!=null)return "现场舰队仍在交战，未清理任何对象；脱战后重试。";}
        PTSDJumpBlockade.restore(incident);
        PTSDReconScenes.close(incident);
        PTSDCrisisIntel.abandon(incident);
        int removed=0,shared=0;
        for(String entityId:ids){
            boolean referenced=false;
            for(PTSDCrisisState.CrisisIncident other:state.incidents)if(other!=incident&&other.scene!=null&&!other.scene.cleaned&&
                    (other.scene.nodes.contains(entityId)||other.scene.owned.contains(entityId)||other.scene.fleets.containsKey(entityId))){referenced=true;break;}
            if(referenced){shared++;continue;}
            SectorEntityToken entity=PTSDNewsScene.entity(entityId);if(entity==null)continue;
            entity.getMemoryWithoutUpdate().set("$PTSD_dev_discarded",true);
            PTSDNewsScene.remove(entityId);removed++;
        }
        incident.scene.owned.clear();incident.scene.fleets.clear();incident.scene.responseQueued=false;
        incident.scene.responseSpawned=true;incident.scene.cleaned=true;
        return "已清理本次自建实体 "+removed+" 个；保留被其他事件引用对象 "+shared+" 个。既有设施/真实运输不删除，已发生的战略后果不回滚。";
    }
    static int[] branches(String card){
        if("P-08".equals(card)||"P-09".equals(card)||"P-10".equals(card))return new int[]{-1,0};
        if("P-04".equals(card))return new int[]{-1,0,2};
        return new int[]{-1,0,1,2};
    }
    static String normalizeRequest(String request){
        if(request==null)return null;
        String[] parts=request.trim().toUpperCase(Locale.ROOT).replace('：',':').split(":",-1);
        if(parts.length<1||parts.length>2||!parts[0].matches("[A-Z0-9_-]+"))return null;
        int branch=0;try{if(parts.length==2)branch=Integer.parseInt(parts[1].trim());}catch(NumberFormatException ex){return null;}
        if(branch < -1 || branch>2&&branch!=100&&branch!=101)return null;
        if(branch>=100&&!"D-02".equals(parts[0]))return null;
        return parts[0]+":"+branch;
    }
    static boolean matchesTemplate(PTSDCrisisIncidentManager.ScriptedTemplate t,String filter){
        return filter==null||filter.trim().isEmpty()||t!=null&&t.siteTemplate!=null&&t.siteTemplate.toUpperCase(Locale.ROOT).contains(filter.trim().toUpperCase(Locale.ROOT));
    }
    private void runNext(){
        if(cursor>=queue.size()){status="队列已结束";return;}String[] item=queue.get(cursor++).split(":");PTSDCrisisState s=PTSDCrisisState.get();
        PTSDCrisisIncidentManager.ScriptedTemplate t=PTSDCrisisIncidentManager.getTemplate(item[0]);
        if(t!=null&&"SCRIPTED".equals(t.delivery)){PTSDCrisisIncidentManager.force(item[0]);status=item[0]+"：已请求原专属链入口";return;}
        PTSDCrisisState.CrisisIncident i=t!=null&&"CONDITIONAL".equals(t.delivery)?PTSDConditionalNewsDirector.forSceneTest(s,item[0]):PTSDCrisisIncidentManager.forceAndGetPrivate(item[0]);if(i==null){status=item[0]+"："+PTSDCrisisIncidentManager.unavailableReason(item[0])+"；未生成合法现场";return;}
        selectedId=i.id;if(!i.investigable){status=item[0]+"：纯报道已生成，无调查现场";return;}
        int branch=Integer.parseInt(item[1]);i.devForced=true;i.randomBranch=Math.max(0,branch);PTSDCrisisAPI.recordNewsIncident(i.id,branch<0?2:1);
        if(branch<0){i.scenarioFalse=true;status=item[0]+"：不属实差分，按原调查入口核实，不生成敌方现场";return;}
        StarSystemAPI system=s.resolveSystem(i.targetSystemId);boolean ok=system!=null&&PTSDNewsSiteManager.materialize(s,i,new Random(i.id.hashCode()),false,true);
        status=item[0]+" / 分支 "+item[1]+(ok?"：已生成":"：等待有效现场");
    }
}
