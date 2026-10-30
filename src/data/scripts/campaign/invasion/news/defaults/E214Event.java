package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** E2-14「空间断拍」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class E214Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.minDay = 28f; // 当前阶段至少经过的游戏日
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
        d.trigger = "BLACK_HOLE"; // 条件投放所需的真实目标类型
        d.action = "-"; // 现场行动提示
        d.remoteOutcome = "原始采样已归档，复核结果尚未公布。"; // 远程结束描述
    }
}
