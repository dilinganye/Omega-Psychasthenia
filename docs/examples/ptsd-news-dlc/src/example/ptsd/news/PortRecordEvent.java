package example.ptsd.news;

import data.scripts.campaign.invasion.news.NewsEventDefinition;
import data.scripts.campaign.invasion.news.NewsEventReference;

/**
 * 独立 DLC 的可运行最小示例：自己的包、自己的 JAR、自己的新闻行。
 * 无 ModPlugin、无注册调用、无本体文件覆盖。
 * 正式制作时可复制 NewsEventReference 的完整代码，替换为自己的事件操作与对白。
 */
public final class PortRecordEvent extends NewsEventReference {
    @Override public void configure(NewsEventDefinition definition) {
        super.configure(definition);
        definition.awareness=.3f;
    }
}
