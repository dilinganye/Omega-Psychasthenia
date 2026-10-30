package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** P-13「频道空白」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class P13Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.recon = 1.8f; // 原侦察影响；核实结算仍遵守情报隔离规则
        d.awareness = 3.5f; // 人类认知影响
        d.aggression = 2f; // 危机进攻倾向影响
        d.panic = 8f; // 当地恐慌影响
        d.distortion = 1f; // 现实扭曲影响
        d.templates = "RELAY_PARASITE"; // 复用现场机制；分支仍保存到每次调查的存档状态
        d.martialSite = "TRUE"; // 是否叠加旧武力现场
    }
}
