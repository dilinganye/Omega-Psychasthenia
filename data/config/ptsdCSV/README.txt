id	新闻的唯一编号，也是 DEV 定向测试和程序查找它的依据。若你打算扩展，则应先使用自己的编号，或者加前缀，以避免覆盖其他新闻。
phases	允许出现的危机阶段，例如 RECON；若有多个阶段会调用则用 | 分隔，ALL 表示全部阶段。
category	新闻分类。目前 '火力侦察、普通新闻' 分别进入对应候选池，其他分类进入暗流池，因此不只是显示标签。
target	通用选址条件，例如 POPULATED 有人星系、WILDERNESS 无人星系、RELAY 有中继的星系。事件 Java 可以接管选址。
weight	同一合格候选池内的基础抽取权重，不是百分比。2 通常比 1 更容易被抽中。
cooldown	同一编号再次出现的冷却，单位为天数；但条件新闻还有它设置的初始行动期限。这并不代表所有调查的统一期限。
investigable	是否允许玩家接取调查。TRUE 可调查，FALSE 为不可调查，也就是纯新闻报道。
filler	是否属于填充新闻池。程序先选择填充池或主要事件池，再按权重抽取；不表示“假新闻”。
source	玩家看到的新闻来源或发布机构。
headline	新闻标题，以现有的文风，比较建议继续使用四字标题。
report	玩家最初看到的公开报道。
truth	调查或脚本流程使用的核查文本。这是文字。
delivery	投放入口：空白或 AMBIENT 为随机新闻；CONDITIONAL 由条件满足后投放；SCRIPTED 由对应剧情程序投放。
JeOnly	TRUE 为 Je 专属，不进入公开新闻入口；空白或 FALSE 为非专属。
eventWeight	事件启用及权重倍率：空白为默认 1，0 暂停常规投放但保留 DEV 测试；随机抽取时有效权重为 weight × eventWeight，这主要是为了方便进一步二次微调。
eventLoc	事件 Java 类的完整名称，例如 data.scripts.campaign.invasion.news.defaults.P08Event，不带 .java。负责专属配置与行为；无特殊实现的纯报道可留空。