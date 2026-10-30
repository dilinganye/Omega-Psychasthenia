package data.scripts.campaign.invasion.news.defaults;

import data.scripts.campaign.invasion.news.NewsEvent;
import data.scripts.campaign.invasion.news.NewsEventDefinition;

/** F2-15「许可未至」。专属配置从 CSV 迁入；原文、效果及现场分支保持。
 * 现场行为复用 templates 指定的机制；新增独立逻辑可实现 NewsEvent 的生命周期。 */
public final class F215Event implements NewsEvent {
    @Override public void configure(NewsEventDefinition d) {
        d.panic = 3f; // 当地恐慌影响
        d.minDay = 30f; // 当前阶段至少经过的游戏日
        d.priority = 2f; // 新闻推送优先级
        d.templates = "PORT_RECORDS"; // 复用现场机制；分支仍保存到每次调查的存档状态
        d.martialSite = "FALSE"; // 是否叠加旧武力现场
        d.trigger = "MARKET"; // 条件投放所需的真实目标类型
        d.action = "提交观测坐标"; // 现场行动提示
        d.remoteOutcome = "尚未取得新的审核回执，候船人员处境不明。"; // 远程结束描述
        d.omegaResponse = "转移观察对象到未撤离港口。"; // 事件响应描述，仅由既有适当展示入口使用
        d.effect = "EVACUATE"; // 条件行动效果
    }
}
