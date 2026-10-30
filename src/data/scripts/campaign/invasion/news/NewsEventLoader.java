package data.scripts.campaign.invasion.news;

import com.fs.starfarer.api.Global;
import data.scripts.campaign.invasion.*;
import org.json.JSONObject;
import java.util.*;

/**
 * eventLoc -> 游戏脚本类 -> 现有新闻/现场管线。运行时缓存不写入存档。
 * 不使用反射构造器：游戏沙箱禁止的反射路径不能作为 DLC 的唯一入口。
 * 加载失败明确拒绝该行，绝不回退成 AUTO 探针现场。
 */
public final class NewsEventLoader {
    private static final Map<String,NewsEvent> EVENTS=new LinkedHashMap<String,NewsEvent>();
    private static final Set<String> FAILED=new HashSet<String>();
    private NewsEventLoader() { }

    public static NewsEvent resolve(String className) {
        if(className==null||className.trim().isEmpty())return null;
        String key=className.trim();
        if(EVENTS.containsKey(key))return EVENTS.get(key);
        if(FAILED.contains(key))return null;
        try {
            Object value=Global.getSettings().getInstanceOfScript(key);
            if(!(value instanceof NewsEvent))throw new IllegalArgumentException(key+" 必须实现 NewsEvent");
            NewsEvent event=(NewsEvent)value;
            EVENTS.put(key,event);
            // 一份实例可同时提供专属选址、现场、或完整 FT 事件链。
            if(value instanceof PTSDCrisisNewsAPI.CustomNewsHandler)
                PTSDCrisisNewsAPI.registerHandler(key,(PTSDCrisisNewsAPI.CustomNewsHandler)value);
            if(value instanceof PTSDNewsSiteAPI.SiteHandler)
                PTSDNewsSiteAPI.registerHandler(key,(PTSDNewsSiteAPI.SiteHandler)value);
            return event;
        }catch(Throwable error){
            FAILED.add(key);
            Global.getLogger(NewsEventLoader.class).error("无法加载新闻 eventLoc: "+key,error);
            return null;
        }
    }

    /**
     * 创建独立行副本，避免修改游戏合并表缓存。eventLoc 的代码配置优先于旧列；
     * eventLoc 留空的旧 DLC 暂时仍可使用旧列，方便作者逐步迁移。
     */
    public static JSONObject prepare(JSONObject source) throws org.json.JSONException {
        JSONObject row=new JSONObject();
        for(Iterator<?> keys=source.keys();keys.hasNext();){String key=(String)keys.next();row.put(key,source.get(key));}
        String key=row.optString("eventLoc","").trim();
        if(key.isEmpty())return row;
        NewsEvent event=resolve(key);
        if(event==null)throw new IllegalArgumentException("eventLoc 不可用: "+key);
        NewsEventDefinition definition=new NewsEventDefinition();
        event.configure(definition);
        definition.applyTo(row);
        if(event instanceof PTSDCrisisNewsAPI.CustomNewsHandler)row.put("target","CUSTOM("+key+")");
        if(event instanceof PTSDNewsSiteAPI.SiteHandler)row.put("siteHandler",key);
        return row;
    }

    public static void published(PTSDCrisisState state, PTSDCrisisState.CrisisIncident incident, float day) {
        NewsEvent event=resolve(incident.eventLoc);
        if(event==null)return;
        try{event.onPublished(new NewsEventContext(state,incident,day));}
        catch(Throwable error){fail(incident,"发布",error);}
    }

    public static String unavailable(String key, PTSDCrisisState state) {
        if(key==null||key.trim().isEmpty())return null;
        NewsEvent event=resolve(key);
        if(event==null)return "eventLoc 加载失败";
        try{return event.unavailableForNews(state);}
        catch(Throwable error){
            Global.getLogger(NewsEventLoader.class).error("新闻前置条件检查失败: "+key,error);
            return "前置条件处理器异常";
        }
    }

    /** 在点击实体时按任务列表查找；不在每帧扫描星系。 */
    public static com.fs.starfarer.api.campaign.InteractionDialogPlugin dialogFor(
            com.fs.starfarer.api.campaign.SectorEntityToken target) {
        if(target==null||target.getId()==null||PTSDPortInvestigation.openingNormal())return null;
        PTSDCrisisState state=PTSDCrisisState.get();if(state==null)return null;
        List<PTSDNewsInteractionMenu.Entry> entries=new ArrayList<PTSDNewsInteractionMenu.Entry>();
        String primary=null;
        // 殖民地的星球入口和轨道站入口是同一个港口；普通贸易舰不能借市场引用抢到入口。
        if(target.getMarket()!=null&&target.getMarket().getPrimaryEntity()!=null&&
                (target==target.getMarket().getPrimaryEntity()||target instanceof com.fs.starfarer.api.campaign.CampaignFleetAPI&&
                ((com.fs.starfarer.api.campaign.CampaignFleetAPI)target).isStationMode()))
            primary=target.getMarket().getPrimaryEntity().getId();
        for(PTSDCrisisState.CrisisIncident incident:state.incidents){
            if(incident==null||!incident.recordedByPlayer||incident.investigationResolved||incident.investigationAbandoned||incident.eventError!=null)continue;
            boolean direct=target.getId().equals(incident.targetEntityId)||
                    incident.eventInteractionIds!=null&&incident.eventInteractionIds.contains(target.getId());
            boolean port=primary!=null&&(primary.equals(incident.targetEntityId)||
                    incident.eventInteractionIds!=null&&incident.eventInteractionIds.contains(primary));
            if(!direct&&!port)continue;
            NewsEvent event=resolve(incident.eventLoc);if(event==null)continue;
            try{
                accepted(state,incident,PTSDCrisisState.getDay());
                if(incident.eventError!=null||incident.investigationResolved)continue;
                com.fs.starfarer.api.campaign.InteractionDialogPlugin dialog=event.createDialog(
                        new NewsEventContext(state,incident,PTSDCrisisState.getDay()),target);
                if(dialog!=null)entries.add(new PTSDNewsInteractionMenu.Entry(incident,PTSDPlayerNewsText.headline(incident),dialog));
            }catch(Throwable error){fail(incident,"交互",error);}
        }
        if(entries.isEmpty())return null; // 本体入口由 CampaignPlugin 接续，避免重复创建现场。
        Set<String> overridden=new HashSet<String>();
        for(PTSDNewsInteractionMenu.Entry entry:entries)overridden.add(entry.incident.id);
        for(PTSDNewsInteractionMenu.Entry entry:PTSDNewsScene.dialogEntries(target))
            if(entry.incident==null||!overridden.contains(entry.incident.id))entries.add(entry);
        return PTSDNewsInteractionMenu.choose(target,entries);
    }

    /** 接取回调在真假结果确定后立即触发，现场创建和交互也提供幂等保障。 */
    public static void accepted(PTSDCrisisState state,PTSDCrisisState.CrisisIncident incident,float day) {
        if(incident==null||!incident.recordedByPlayer||incident.investigationResolved||incident.investigationAbandoned||
                incident.eventAccepted||incident.eventError!=null)return;
        NewsEvent event=resolve(incident.eventLoc);if(event==null)return;
        incident.eventAccepted=true;
        try{event.onAccepted(new NewsEventContext(state,incident,day));}
        catch(Throwable error){fail(incident,"接取",error);}
    }

    private static void unbind(PTSDCrisisState.CrisisIncident incident){
        if(incident.eventInteractionIds==null||Global.getSector()==null)return;
        for(String id:incident.eventInteractionIds){
            if(id==null||id.isEmpty())continue;
            com.fs.starfarer.api.campaign.SectorEntityToken entity=Global.getSector().getEntityById(id);
            if(entity!=null)com.fs.starfarer.api.util.Misc.makeUnimportant(entity,"PTSD_event_"+incident.id);
        }
        incident.eventInteractionIds.clear();
    }

    /** 调用频率沿用新闻后台心跳。每个事件终态回调至多一次，不新增全星区扫描。 */
    public static void advance(PTSDCrisisState state,float day) {
        for(PTSDCrisisState.CrisisIncident incident:new ArrayList<PTSDCrisisState.CrisisIncident>(state.incidents)){
            if(incident==null||incident.eventFinished||incident.eventLoc==null||incident.eventLoc.isEmpty())continue;
            NewsEvent event=resolve(incident.eventLoc);if(event==null)continue;
            NewsEventContext context=new NewsEventContext(state,incident,day);
            try {
                accepted(state,incident,day);
                boolean ended=incident.investigationResolved&&
                        (incident.scenarioTrigger==null||incident.scenarioResolved||incident.investigationAbandoned||
                                incident.investigationResult==PTSDTaskRules.InvestigationResult.EXPIRED||
                                incident.investigationResult==PTSDTaskRules.InvestigationResult.TARGET_LOST);
                if(!incident.recordedByPlayer&&day>=incident.newsExpiresDay)ended=true;
                if(ended){incident.eventFinished=true;unbind(incident);event.onFinished(context);}
                else if(incident.eventError==null)event.advanceIncident(context);
            }catch(Throwable error){fail(incident,"推进",error);}
        }
    }

    private static void fail(PTSDCrisisState.CrisisIncident incident,String stage,Throwable error){
        incident.eventError=stage+": "+error.getClass().getSimpleName()+": "+error.getMessage();
        Global.getLogger(NewsEventLoader.class).error("新闻扩展回调失败 "+incident.cardId+" / "+incident.eventLoc+" / "+stage,error);
    }
}
