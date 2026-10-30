package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** SITE-14「打捞求援」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class SITE14Event implements NewsEvent {
    // JE-04 是本事件的高能调查变种，JeOnly 控制任务归属与遭遇增强。
    @Override public void configure(NewsEventDefinition d) {
        d.templates = "FORT_SALVAGE_INTERCEPTION"; // 复用现场机制；分支仍保存到每次调查的存档状态
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
    }
}
