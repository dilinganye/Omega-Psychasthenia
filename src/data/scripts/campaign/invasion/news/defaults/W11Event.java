package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** W-11「战时状态」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class W11Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.awareness = 5f; // 人类认知影响
        d.aggression = 6f; // 危机进攻倾向影响
        d.panic = 50f; // 当地恐慌影响
        d.distortion = 4f; // 现实扭曲影响
        d.physicalChance = 0.9f; // 原附属物理行动概率（不是新闻真实性）
        d.strength = 185f; // 附属行动名义强度
        d.templates = "BATTLE_AFTERMATH|DISTORTION"; // 复用现场机制；分支仍保存到每次调查的存档状态
        d.martialSite = "TRUE"; // 是否叠加旧武力现场
    }
}
