package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** FT-04「母星外的等待」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 选址/链流程继承既有实现，旧公开类名继续可用。 */
public final class FT04Event extends data.scripts.campaign.invasion.news.ft.FT04Handler implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.minDay = 16f; // 当前阶段至少经过的游戏日
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
        d.visibility = "NONE"; // 公开/内部可见性
    }
}
