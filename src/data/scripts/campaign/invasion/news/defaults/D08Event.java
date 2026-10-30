package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** D-08「未知突袭」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class D08Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.recon = 1.2f; // 原侦察影响；核实结算仍遵守情报隔离规则
        d.awareness = 0.1f; // 人类认知影响
        d.aggression = 0.5f; // 危机进攻倾向影响
        d.panic = 0.3f; // 当地恐慌影响
        d.templates = "CREW_MISSING"; // 复用现场机制；分支仍保存到每次调查的存档状态
        d.martialSite = "TRUE"; // 是否叠加旧武力现场
    }
}
