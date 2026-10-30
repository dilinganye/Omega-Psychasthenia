package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** D-01「被动翘班」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class D01Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.recon = 0.8f; // 原侦察影响；核实结算仍遵守情报隔离规则
        d.panic = 0.2f; // 当地恐慌影响
        d.physicalChance = 0.35f; // 原附属物理行动概率（不是新闻真实性）
        d.templates = "WRECK_RECONSTRUCTION"; // 复用现场机制；分支仍保存到每次调查的存档状态
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
    }
}
