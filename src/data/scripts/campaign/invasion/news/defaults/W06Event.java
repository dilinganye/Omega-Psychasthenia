package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** W-06「海盗联盟」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class W06Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.awareness = 2f; // 人类认知影响
        d.aggression = 5f; // 危机进攻倾向影响
        d.panic = 30f; // 当地恐慌影响
        d.distortion = 1f; // 现实扭曲影响
        d.physicalChance = 0.75f; // 原附属物理行动概率（不是新闻真实性）
        d.strength = 120f; // 附属行动名义强度
        d.templates = "BATTLE_AFTERMATH|ROUTE"; // 复用现场机制；分支仍保存到每次调查的存档状态
        d.martialSite = "TRUE"; // 是否叠加旧武力现场
    }
}
