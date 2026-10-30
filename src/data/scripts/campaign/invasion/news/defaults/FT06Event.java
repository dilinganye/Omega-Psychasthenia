package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** FT-06「边界事发」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 选址/链流程继承既有实现，旧公开类名继续可用。 */
public final class FT06Event extends data.scripts.campaign.invasion.news.ft.FT06Handler implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.minDay = 28f; // 当前阶段至少经过的游戏日
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
        d.choice1 = "调查争议星系"; // 专属链第一选项
    }
}
