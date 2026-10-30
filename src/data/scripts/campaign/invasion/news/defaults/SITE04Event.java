package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** SITE-04「无人应答」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class SITE04Event implements NewsEvent {
    // JE-02 使用同一机制；JeOnly 保存在 incident.jeExclusiveScene，增强遭遇仍走其专属分支。
    @Override public void configure(NewsEventDefinition d) {
        d.templates = "SIGNAL_PURSUIT"; // 复用现场机制；分支仍保存到每次调查的存档状态
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
    }
}
