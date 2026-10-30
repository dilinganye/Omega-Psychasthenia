package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** WR-08「空域失守」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class WR08Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.minDay = 31f; // 当前阶段至少经过的游戏日
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
        d.choice1 = "测量异常坐标"; // 专属链第一选项
        d.choice2 = "趁机反攻主线"; // 专属链第二选项
    }
}
