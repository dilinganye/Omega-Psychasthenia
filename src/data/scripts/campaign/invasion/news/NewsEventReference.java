package data.scripts.campaign.invasion.news;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import data.scripts.campaign.invasion.*;
import java.util.*;

/**
 * 可复制到 DLC 自己包中的完整参考类，不在本体 CSV 中投放。
 * <p>演示事件：到真实殖民地核对地方记录。不会生成替代殖民地或额外探针。
 * CSV eventLoc 填写复制后类的完整包名；DLC mod_info.json 依赖 Omega_PTSD。
 * <p>以下列出了当前对外支持的全部新闻生命周期、精确选址、现场和交互接口。
 * 通用模板事件只需 implements NewsEvent + configure；其余方法均可按需删除。
 * 完全独立的现场实现本例的两个可选接口，不能只改新闻编号要求本体猜测玩法。
 * <p>每个处理器实例可被多条新闻共享；所有任务状态使用 context.data()/incident。
 * 构造器和 configure 可能在没有战役存档时调用，禁止在构造阶段访问玩家或生成实体。
 */
public class NewsEventReference implements NewsEvent,
        PTSDCrisisNewsAPI.CustomNewsHandler, PTSDNewsSiteAPI.SiteHandler {

    /**
     * 填写专属规则。调用一次/每次主表加载，不随玩家打开 Intel 重复。
     * @param d 本行独立的配置对象；不会改变 CSV 的阶段、权重、来源或正文。
     */
    @Override public void configure(NewsEventDefinition d) {
        d.templates="PORT_RECORDS"; // 有独立 SiteHandler 时仅作为现场类别说明。
        d.martialSite="FALSE"; // 不额外混入通用武力场景。
        d.manualCompletion=true; // 玩家必须完成交互，靠近殖民地不会自动结算。
        d.awareness=.2f; // 证实后按既有核实规则结算，不是发布新闻就揭露敌情。
        // 其余全部配置字段、默认值和单位见 NewsEventDefinition。
    }

    /**
     * 无副作用前置检查。正常等待返回具体原因，允许投放返回 null。
     * @param state 当前危机状态；检查现有部署，不读取玩家未知内容用于公开文本。
     * @return 等待原因或 null。选址本身仍由 pick 处理。
     */
    @Override public String unavailableForNews(PTSDCrisisState state) { return null; }

    /**
     * 精确选址，替代 CSV target。此时还没生成任务，禁止在这里制造舰队/标记。
     * @param context 含危机状态、当前游戏日、抽签 Random 和新闻编号。
     * @return 合法系统、可选市场、实际设施；找不到就返回 null 等待下次，不用无关探针代替。
     */
    @Override public PTSDCrisisNewsAPI.TargetSelection pick(PTSDCrisisNewsAPI.PickContext context) {
        List<MarketAPI> markets=new ArrayList<MarketAPI>();
        for(MarketAPI market:Global.getSector().getEconomy().getMarketsCopy()){
            StarSystemAPI system=market.getStarSystem();
            if(!market.isPlanetConditionMarketOnly()&&market.getPrimaryEntity()!=null&&system!=null&&
                    !system.hasTag(Tags.THEME_HIDDEN)&&!system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)&&
                    !IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(market.getFactionId())&&
                    !IIRT_Omega_Invasion.WATCHER_FACTION.equals(market.getFactionId()))markets.add(market);
        }
        if(markets.isEmpty())return null;
        MarketAPI market=markets.get(context.random.nextInt(markets.size()));
        return new PTSDCrisisNewsAPI.TargetSelection(market.getStarSystem(),market,market.getPrimaryEntity());
    }

    /**
     * 新闻发布后一次。不要在这里把尚未接取的任务插入失联列表，也不要提前创建伏击。
     * 抽签结果、版本和计时可在 data 中保存；本例保存数据结构版本。
     */
    @Override public void onPublished(NewsEventContext context) { context.data().put("example.version","1"); }

    /**
     * 接取时立即调用，早于现场创建；真假值已保存到 incident.investigationOutcome，禁止重抽。
     * 仅作轻量初始化；真正现场在 materialize 中创建。
     */
    @Override public void onAccepted(NewsEventContext context) { context.data().put("example.acceptedDay",Float.toString(context.day)); }

    /**
     * 旧 CustomNewsHandler 的生成接口：调查型新闻仅在属实、接取后调用；纯报道则发布时调用。
     * 本例复用选中的设施，不在此重复创建现场。
     * @return null 保持既有目标；返回实体会替换调查目标，请保存其 ID。
     */
    @Override public SectorEntityToken onIncidentCreated(PTSDCrisisNewsAPI.IncidentContext context) { return null; }

    /**
     * 属实任务第一次进入目标星系时创建现场。失败返回 null 会等待重试，必须无半成品泄漏。
     * @param context 包含目标星系、市场、精确锚点及任务记录。
     * @return 锚点、文本和事件自己创建的实体 ID。借用设施绝对不能放进 entityIds！
     */
    @Override public PTSDNewsSiteAPI.SiteResult materialize(PTSDNewsSiteAPI.SiteContext context) {
        SectorEntityToken anchor=context.market==null?null:context.market.getPrimaryEntity();
        if(anchor==null)return null;
        new NewsEventContext(context.state,context.incident,PTSDCrisisState.getDay()).bind(anchor);
        return new PTSDNewsSiteAPI.SiteResult(anchor,"地方记录核查","前往港口核对原始记录。",
                "进入目标殖民地，选择调查入口。",Collections.<String>emptyList());
    }

    /**
     * 已创建现场的后台更新，可检查实际舰队是否抵达/战斗是否结束。
     * 使用 incident.eventData 保存下一次检查日，避免每个周期全星区扫描。
     */
    @Override public void advance(PTSDNewsSiteAPI.SiteContext context) { }

    /**
     * 旧目标处理器的全局后台接口，每个已加载实例每周期一次。不是每个任务一次！
     * 本例无全局任务，所以留空。新代码通常应使用 advanceIncident。
     */
    @Override public void advance(PTSDCrisisNewsAPI.AdvanceContext context) { }

    /**
     * 每条未结束新闻的后台更新。任务未接取时也可能调用，读取 recordedByPlayer 再做现场逻辑。
     * 真实秒数特效应另用脚本；本接口 day 是游戏日，不应拿它实现秒级对话倒计时。
     */
    @Override public void advanceIncident(NewsEventContext context) { }

    /**
     * 自动确认或 Context.finish(CONFIRMED, ...) 时调用；标准报告由框架结算。
     * 舰队资源/额外报酬仍须自己用持久布尔标记去重，不要在这里复制标准调查报告。
     */
    @Override public void onConfirmed(PTSDNewsSiteAPI.SiteContext context) { }

    /**
     * 现场清理前调用一次，包含正常完成、放弃、超时。历史名字 onExpired 不只代表超时。
     * 归还临时能力修改、恢复路线、安排自有舰队退场；不得删除借用设施。
     * 已销毁目标、玩家离开星系和重复读档都必须能安全处理。
     */
    @Override public void onExpired(PTSDNewsSiteAPI.SiteContext context) { }

    /**
     * 调查终态或未接取新闻过期后调用一次；黄色 bind 标记由框架解除。
     * 此时 result 区分确认、证伪、无结论、过期或放弃；禁止重新投骰/重新发奖。
     */
    @Override public void onFinished(NewsEventContext context) { }

    /** 返回DEV面板可读摘要；只读，不能为预览启动事件或修改状态。 */
    @Override public String devStatus(NewsEventContext context) { return "示例港口核查 / 已取得证据："+context.incident.eventEvidenceCollected; }
    /** 返回事件中已存在的舰队ID；本例没有舰队，不能为填充这个列表生成假舰队。 */
    @Override public java.util.List<String> devFleetIds(NewsEventContext context) { return Collections.emptyList(); }
    /** 声明按钮键和名称；空集合表示无额外操作。仅在DEV模式显示。 */
    @Override public java.util.Map<String,String> devActions(NewsEventContext context) { return Collections.emptyMap(); }
    /** 仅显式点击DEV按钮执行；编写实现时验证任务、实体与战斗状态，再返回结果说明。 */
    @Override public String runDevAction(NewsEventContext context,String action) { return "本例无需额外调试操作。"; }

    /**
     * 点击调查目标或 bind 的节点时提供专属对话，任务结束后框架不再拦截。
     * 不改变殖民地原版插件；“正常港口功能”通过一次性绕过调查入口重新打开原交互，保留其他模组功能。
     * @return 本次对话实例，或 null 使用该设施正常交互。
     */
    @Override public InteractionDialogPlugin createDialog(NewsEventContext context,SectorEntityToken target) {
        if(target.getMarket()==null)return null;
        return new EvidenceDialog(context);
    }

    /** 对话实例仅存活于本次窗口；证据、进度、奖励不能只存在这些 UI 字段中。 */
    private static final class EvidenceDialog implements InteractionDialogPlugin {
        private final NewsEventContext context;
        private InteractionDialogAPI dialog;
        EvidenceDialog(NewsEventContext context){this.context=context;}
        public void init(InteractionDialogAPI dialog){
            this.dialog=dialog;
            dialog.getTextPanel().addPara("值班人员把两份记录放在一起。现在可以核对报道中的航次。");
            dialog.getOptionPanel().clearOptions();
            dialog.getOptionPanel().addOption("核对原始记录","records");
            dialog.getOptionPanel().addOption("办理正常港口事务","normal");
            dialog.getOptionPanel().addOption("暂时离开","leave");
        }
        public void optionSelected(String text,Object option){
            if("records".equals(option)){
                boolean real=context.incident.investigationOutcome==1;
                if(!real)context.incident.investigationDisproofEvidence="原始时戳与报道指称的航次不符。";
                context.finish(real?PTSDTaskRules.InvestigationResult.CONFIRMED:PTSDTaskRules.InvestigationResult.DISPROVED,true);
                dialog.getTextPanel().addPara(real?"原始记录确认了报道中的异常。":"原始记录足以排除报道中的这项说法。");
                dialog.getOptionPanel().clearOptions();dialog.getOptionPanel().addOption("离开","leave");
            }else if("normal".equals(option)){
                PTSDPortInvestigation.openNormal(dialog,dialog.getInteractionTarget());
            }else dialog.dismiss();
        }
        public void optionMousedOver(String text,Object data){ }
        public void advance(float amount){ }
        public void backFromEngagement(EngagementResultAPI result){ }
        public Object getContext(){return null;}
        public Map<String,MemoryAPI> getMemoryMap(){return null;}
    }
}
