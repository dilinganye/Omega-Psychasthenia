package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** R2-10「星图批注」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class R210Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.panic = 1f; // 当地恐慌影响
        d.minDay = 20f; // 当前阶段至少经过的游戏日
        d.templates = "RELAY_PARASITE"; // 复用现场机制；分支仍保存到每次调查的存档状态
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
        d.trigger = "RELAY"; // 条件投放所需的真实目标类型
        d.action = "比对两版星图"; // 现场行动提示
        d.remoteOutcome = "批注进入下一版公开星图。"; // 远程结束描述
        d.omegaResponse = "减少对该中继的依赖。"; // 事件响应描述，仅由既有适当展示入口使用
        d.effect = "SHARE_INTEL"; // 条件行动效果
    }
}
