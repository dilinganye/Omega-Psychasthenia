package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** SITE-13「引力错拍」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class SITE13Event implements NewsEvent {
    // JE-03 是本事件的高能调查变种，共用处理器，不共用每次任务的状态。
    @Override public void configure(NewsEventDefinition d) {
        d.templates = "FORT_GRAVITY_SURVEY"; // 复用现场机制；分支仍保存到每次调查的存档状态
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
    }
}
