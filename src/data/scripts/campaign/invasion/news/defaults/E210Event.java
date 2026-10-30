package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** E2-10「彩带入舱」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class E210Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.minDay = 20f; // 当前阶段至少经过的游戏日
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
        d.trigger = "MARKET"; // 条件投放所需的真实目标类型
        d.action = "-"; // 现场行动提示
        d.remoteOutcome = "分拣争议已提交，费用归属仍待双方答复。"; // 远程结束描述
    }
}
