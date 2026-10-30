package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** F-TT-01「死亡与赋税」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class FTT01Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.awareness = 0.3f; // 人类认知影响
        d.aggression = -2f; // 危机进攻倾向影响
        d.templates = "PORT_RECORDS"; // 复用现场机制；分支仍保存到每次调查的存档状态
    }
}
