package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** F2-02「你不许说」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class F202Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.panic = 2f; // 当地恐慌影响
        d.minDay = 4f; // 当前阶段至少经过的游戏日
        d.priority = 2f; // 新闻推送优先级
        d.templates = "FORT_PATROL_GAP"; // 复用现场机制；分支仍保存到每次调查的存档状态
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
        d.trigger = "FORT_SMALL"; // 条件投放所需的真实目标类型
        d.action = "记录巡逻空窗"; // 现场行动提示
        d.remoteOutcome = "这份空窗记录已经过时，下次通行仍须重新观察。"; // 远程结束描述
        d.omegaResponse = "换班并占用其他任务舰队。"; // 事件响应描述，仅由既有适当展示入口使用
        d.effect = "DELAY_FORTRESS"; // 条件行动效果
    }
}
