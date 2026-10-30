package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** W-02「准备撤离」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class W02Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.awareness = 4f; // 人类认知影响
        d.aggression = 5f; // 危机进攻倾向影响
        d.panic = 60f; // 当地恐慌影响
        d.distortion = 2f; // 现实扭曲影响
        d.physicalChance = 0.8f; // 原附属物理行动概率（不是新闻真实性）
        d.strength = 140f; // 附属行动名义强度
        d.templates = "CREW_MISSING|ROUTE"; // 复用现场机制；分支仍保存到每次调查的存档状态
        d.martialSite = "TRUE"; // 是否叠加旧武力现场
    }
}
