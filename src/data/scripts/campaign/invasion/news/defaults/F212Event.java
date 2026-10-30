package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** F2-12「未知单元」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class F212Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.panic = 2f; // 当地恐慌影响
        d.minDay = 24f; // 当前阶段至少经过的游戏日
        d.priority = 2f; // 新闻推送优先级
        d.templates = "TRAFFIC_OBSERVATION"; // 复用现场机制；分支仍保存到每次调查的存档状态
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
        d.trigger = "OUTPOST"; // 条件投放所需的真实目标类型
        d.action = "追踪外运航迹"; // 现场行动提示
        d.remoteOutcome = "外运航迹中断，尚无到站记录可供复核。"; // 远程结束描述
        d.omegaResponse = "为转运舰增加护卫，减少外线巡逻。"; // 事件响应描述，仅由既有适当展示入口使用
        d.effect = "SHARE_INTEL"; // 条件行动效果
    }
}
