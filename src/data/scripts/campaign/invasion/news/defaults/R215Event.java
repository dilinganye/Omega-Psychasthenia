package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** R2-15「伪造气象记录」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class R215Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.panic = 1f; // 当地恐慌影响
        d.minDay = 30f; // 当前阶段至少经过的游戏日
        d.templates = "SPATIAL_ECHO"; // 复用现场机制；分支仍保存到每次调查的存档状态
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
        d.trigger = "WILDERNESS"; // 条件投放所需的真实目标类型
        d.action = "重测背景辐射"; // 现场行动提示
        d.remoteOutcome = "异常被归档为校准误差。"; // 远程结束描述
        d.omegaResponse = "降低之后在该区域的驻留时间。"; // 事件响应描述，仅由既有适当展示入口使用
        d.effect = "VERIFY"; // 条件行动效果
    }
}
