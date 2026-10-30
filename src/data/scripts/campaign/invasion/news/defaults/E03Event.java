package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** E-03「光谱漂移」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class E03Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.recon = 0.5f; // 原侦察影响；核实结算仍遵守情报隔离规则
        d.awareness = 3f; // 人类认知影响
        d.aggression = 2f; // 危机进攻倾向影响
        d.panic = 2.5f; // 当地恐慌影响
        d.distortion = 1.5f; // 现实扭曲影响
        d.physicalChance = 0.3f; // 原附属物理行动概率（不是新闻真实性）
        d.strength = 45f; // 附属行动名义强度
        d.templates = "SPATIAL_ECHO"; // 复用现场机制；分支仍保存到每次调查的存档状态
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
    }
}
