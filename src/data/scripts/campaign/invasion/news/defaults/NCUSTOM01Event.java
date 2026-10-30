package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** N-CUSTOM-01「没油逃犯」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 选址/链流程继承既有实现，旧公开类名继续可用。 */
public final class NCUSTOM01Event extends data.scripts.campaign.invasion.news.FugitiveShuttleNewsHandler implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.awareness = 0.2f; // 人类认知影响
        d.panic = 0.1f; // 当地恐慌影响
    }
}
