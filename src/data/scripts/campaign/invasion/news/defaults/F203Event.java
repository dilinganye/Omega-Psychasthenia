package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** F2-03「护航错拍」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class F203Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.panic = 2f; // 当地恐慌影响
        d.minDay = 6f; // 当前阶段至少经过的游戏日
        d.priority = 2f; // 新闻推送优先级
        d.templates = "FORT_SUPPLY_WINDOW"; // 复用现场机制；分支仍保存到每次调查的存档状态
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
        d.trigger = "TRANSPORT"; // 条件投放所需的真实目标类型
        d.action = "比对航迹时刻"; // 现场行动提示
        d.remoteOutcome = "后续识别脉冲未完整接收，不能补全运输队的行程。"; // 远程结束描述
        d.omegaResponse = "紧急调拨护卫，原防区减防。"; // 事件响应描述，仅由既有适当展示入口使用
        d.effect = "DELAY_TRANSPORT"; // 条件行动效果
    }
}
