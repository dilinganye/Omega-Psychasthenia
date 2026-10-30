package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** R2-01「中继重放」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class R201Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.panic = 1f; // 当地恐慌影响
        d.minDay = 2f; // 当前阶段至少经过的游戏日
        d.priority = 2f; // 新闻推送优先级
        d.templates = "RELAY_PARASITE"; // 复用现场机制；分支仍保存到每次调查的存档状态
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
        d.trigger = "RELAY"; // 条件投放所需的真实目标类型
        d.action = "核对重放时标"; // 现场行动提示
        d.remoteOutcome = "重放器继续覆盖原始时标；运输记录仍无法互证。"; // 远程结束描述
        d.omegaResponse = "改用备用频段，并抽走一组监听资源。"; // 事件响应描述，仅由既有适当展示入口使用
        d.effect = "SHARE_INTEL"; // 条件行动效果
    }
}
