package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** WR-16「视界危急」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class WR16Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.minDay = 40f; // 当前阶段至少经过的游戏日
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
        d.choice1 = "立即撤离"; // 专属链第一选项
        d.choice2 = "尝试救出最后一批船"; // 专属链第二选项
    }
}
