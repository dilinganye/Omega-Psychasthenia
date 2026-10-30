package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** FT-07「航路索赔」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 选址/链流程继承既有实现，旧公开类名继续可用。 */
public final class FT07Event extends data.scripts.campaign.invasion.news.ft.FT07Handler implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.minDay = 20f; // 当前阶段至少经过的游戏日
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
    }
}
