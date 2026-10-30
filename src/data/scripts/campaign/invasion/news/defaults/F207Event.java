package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** F2-07「双重日期」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class F207Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.panic = 1f; // 当地恐慌影响
        d.minDay = 14f; // 当前阶段至少经过的游戏日
        d.templates = "SIGNAL_PURSUIT"; // 复用现场机制；分支仍保存到每次调查的存档状态
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
        d.trigger = "FORTRESS"; // 条件投放所需的真实目标类型
        d.action = "提交冲突记录"; // 现场行动提示
        d.remoteOutcome = "两个版本均已归档，尚未收到可核验的更正。"; // 远程结束描述
        d.omegaResponse = "重审工程账本并延后非必要调动。"; // 事件响应描述，仅由既有适当展示入口使用
        d.effect = "SHARE_INTEL"; // 条件行动效果
    }
}
