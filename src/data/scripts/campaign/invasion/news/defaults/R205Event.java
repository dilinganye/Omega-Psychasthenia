package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** R2-05「假求救」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class R205Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.panic = 2f; // 当地恐慌影响
        d.minDay = 10f; // 当前阶段至少经过的游戏日
        d.templates = "SIGNAL_PURSUIT"; // 复用现场机制；分支仍保存到每次调查的存档状态
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
        d.trigger = "WILDERNESS"; // 条件投放所需的真实目标类型
        d.action = "核对求救发射源"; // 现场行动提示
        d.remoteOutcome = "信号自行停止，救援船只找到空置发射器。"; // 远程结束描述
        d.omegaResponse = "放弃这一诱饵，回收设备。"; // 事件响应描述，仅由既有适当展示入口使用
        d.effect = "VERIFY"; // 条件行动效果
    }
}
