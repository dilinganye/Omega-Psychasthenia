package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** D-05「未知回波」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class D05Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.recon = 0.2f; // 原侦察影响；核实结算仍遵守情报隔离规则
        d.panic = 0.2f; // 当地恐慌影响
        d.distortion = 0.4f; // 现实扭曲影响
        d.templates = "DISTORTION"; // 复用现场机制；分支仍保存到每次调查的存档状态
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
    }
}
