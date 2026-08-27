package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

/** Exact first-meeting narrative plus independent contact and investigation-report conversations. */
public final class PTSDJeOtloesDialog implements InteractionDialogPlugin {
    public enum Mode { INTRO, CONTACT, REPORT }

    private static final String GO_OFFICE = "go_office";
    private static final String RESIST = "resist";
    private static final String SMUGGLING = "smuggling";
    private static final String ASK_REASON = "ask_reason";
    private static final String CONTINUE_1 = "continue_1";
    private static final String CONTINUE_2 = "continue_2";
    private static final String CONTINUE_3 = "continue_3";
    private static final String CONTINUE_4 = "continue_4";
    private static final String ACCEPT = "accept";
    private static final String ACCEPT_2 = "accept_2";
    private static final String ACCEPT_3 = "accept_3";
    private static final String DECLINE = "decline";
    private static final String CONTACT_PLAYER = "contact_player";
    private static final String CONTACT_AGENT = "contact_agent";
    private static final String RECEIVE_DETECTOR = "receive_detector";
    private static final String REPORT_REPLY_PREFIX = "report_reply_";
    private static final String REPORT_REVEAL = "report_reveal";
    private static final String LEAVE = "leave";

    private static final String[] JE_SMALL_TALK = {
            "\"你来得比港务局预计得早。不错，他们至少又错了一次。\"",
            "\"航程还顺利？不用回答，我已经看过你的燃料记录了。\"",
            "\"这里的咖啡糟透了。好消息是，港口指挥官以为我喜欢。\"",
            "\"别在意门外的人，他们只知道这里正在进行一场税务审查。\"",
            "\"你的船员看起来比上次更紧张。看来传闻确实跑得比舰船快。\"",
            "\"我原本订了一个更体面的房间，但那里有三套监听器，只好作罢。\"",
            "\"你还活着。每次看到这一点，我都会重新检查一次概率表。\"",
            "\"附近有个记者等了我两天。我告诉他自己是冷藏设备推销员。\"",
            "\"这里的灯太亮了，不过比完全没有灯好。最近我开始讨厌黑暗。\"",
            "\"港务局问我们是什么关系。我说你欠我一份事故报告。\"",
            "\"我猜你不会喜欢这里的安检。放心，他们更不喜欢我。\"",
            "\"先坐。椅子没有问题——至少我检查过能检查的部分。\""
    };

    private static final String[] PLAYER_SMALL_TALK = {
            "\"你总是这样欢迎合作对象？\"",
            "\"我更关心你查到了什么。\"",
            "\"如果这是寒暄，你做得很糟。\"",
            "\"我的船员开始一直怀疑你根本不存在，就像你希望的那样。\"",
            "\"港口指挥官对我们的会面似乎很紧张-他看起来快要昏过去了。\"",
            "\"下一次至少告诉我会面地点有没有像样的类酒物。\"",
            "\"你欠我一个不被安检盘问的解释。\"",
            "\"那些监听器现在怎么样了？\"",
            "\"你看过我的燃料记录？\"",
            "\"我希望这次不是另一条假消息。\"",
            "\"说正事吧，Je。\""
    };

    private static final String[] MISSED_APOLOGIES = {
            "\"抱歉！来晚了，之前...的确不太方便。你问我在哪里——你肯定不想知道。\"",
            "\"重新接通！之前刚遇到了一些很麻烦的事情，你知道这个就够了。\"",
            "\"听到你的声音真亲切，我看见你之前有试图联系我，但那时候我还在应付那些政治戏码，虽然我不知道那些会导致动乱的消息传播有什么好的...\"\n\"但让他们合作也是费了我好大力气。\"",
            "\"可算完事了，舰长。上次你发来的通讯，信道旁边坐着几个不该听见你声音的人，所以我没有接通。\"\n\"不用谢。\"",
            "\"我之前看到了你的呼叫，只是当时回答会让一份名单里多出不必要的名字，我可不想把你牵扯进来。\"",
            "\"刚刚在和那些趾高气扬的达官显贵们进行一些...利益交换。\"他脸上的皱纹抽了抽，随后他侧过头去，几秒后意识到了自己还在和你进行谈话，便又和你四目相对继续说道，\"那是一场没有人愿意承认参加过的会议。\"\n\"比交火更难脱身。\"",
            "\"信道不是坏了，是我主动掐掉的。\"他后面的暗淡景象不时闪烁颤动...\n他应该是在一艘舰船上，而且大抵航速很快。\n\"现在可以说话，但咱们得尽量简短。\""
    };
    private static final String[] SPAM_COMPLAINTS = {
            "\"有一件事，舰长。当我在拿预算、豁免权和几条航路做政治交换来防止我们的明天被那些尸位素餐的家伙送进棺材时，真的很难立刻回复你的每一次呼叫。\"",
            "在你试图说什么之前，对方先对你提出了一份小小的指控：\n\"今天，我亲爱的舰长，你今天呼叫了三次。那时我正对着六个互相敌视的人解释为什么他们必须共享情报——但通讯器每亮一次，他们就多怀疑我一次。\"",
            "\"...下次我没接，请，先假设我在应酬，而不是死了。\"他的表情或许是在笑，但他那毁容的半张脸让你也不太能确认，\"为了解决那些麻烦事，我需要和一些更麻烦的人商讨。而交易进行到一半时，我不能因为舰长想聊天就离席。\"",
            "\"三次呼叫都被会议桌上的人看见了。现在他们一致认为我有一位耐心很差、而且非常昂贵的线人。\""
    };
    private static final String[] LATE_URGENCY = {
            "Je 的影像比以往更模糊，背景中不断有人递来新的信息板。他没有一次把视线完整地停在你身上。",
            "信道另一端至少有三场对话同时进行。Je 抬手让某个人闭嘴，随后才转向你：\"说吧，舰长，时间现在比燃料还贵。\"",
            "你听见远处有人争执撤离顺序。Je 关掉拾音器几秒，再打开时眼中的血丝比上一次更多。",
            "\"尽量快些。\"Je 说。他身后的态势图每隔几秒就有一处从黄色转为红色。"
    };
    private static final String[] PLAYER_NOT_FINISHING_TASK = {
            "\"先把我交给你的那组坐标查清楚，舰长。新的问题只会让旧问题更难收拾。\"\n这句话不假。\n\n更何况这件事是你自己请缨的。",
            "\"啊，舰长。\"\n\"你回来的很快，所以事情我们完成了吗？\"",
            "\"闲言少叙。\"\n他透过影像看着你，随后向后瘫在椅子上，\"所以你还没完成之前的那件事，我这儿的人手可还在等着。\"",
    };

    private final Mode mode;
    private final MarketAPI market;
    private final Random random = new Random();
    private InteractionDialogAPI dialog;
    private int reportRound;
    private PTSDJeOtloesManager.ContactResult contactResult;
    private float missedContactElapsed;
    private boolean missedContactFinished;

    public PTSDJeOtloesDialog(Mode mode, MarketAPI market) {
        this.mode = mode;
        this.market = market;
    }

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        if (mode == Mode.CONTACT) {
            contactResult = PTSDJeOtloesManager.rollContactAttempt();
            if (contactResult != PTSDJeOtloesManager.ContactResult.CONNECTED) {
                dialog.hideVisualPanel();
                dialog.getOptionPanel().clearOptions();
                dialog.getTextPanel().addPara("你尝试联系Je...");
                return;
            }
        }
        PersonAPI je = PTSDJeOtloesIntel.getOrCreatePerson();
        dialog.getVisualPanel().showPersonInfo(je, true);
        if (mode == Mode.INTRO) showApproach();
        else if (mode == Mode.CONTACT) showContact();
        else showReportGreeting();
    }

    private void showApproach() {
        PTSDCrisisState state = PTSDCrisisState.get();
        TextPanelAPI text = dialog.getTextPanel();
        OptionPanelAPI options = dialog.getOptionPanel();

        dialog.hideVisualPanel();
        /*
        dialog.getVisualPanel().showImagePortion(
                "illustrations",
                "Je_meeting",
                640f, 400f,   // 原始取样区域尺寸
                0f, 0f,       // 原图取样偏移
                480f, 300f    // 对话界面显示尺寸
        ); */

        options.clearOptions();

        if (state.jeResistedOnce) {
            text.addPara("你和你的护卫队在遭受强光弹和各种控制弹药的招呼后不得不放弃抵抗，你也不知道他们是如何这么快追到这里来的。");
            text.addPara("随后，你被送进了港口指挥官的办公室。");
            options.addOption("继续", GO_OFFICE);
            return;
        }

        text.addPara("你刚完成靠港手续，一队装备过分齐全的港务安保人员便封锁了通向泊位的廊桥。他们没有宣布逮捕，只是要求你立刻前往港口指挥官的办公室。");
        options.addOption("前往港口指挥官的办公室", GO_OFFICE);
        options.addOption("强硬反抗（消耗 1 故事点）", RESIST,Color.RED,
                "强行突破安保封锁。对方会暂时失去你的行踪，但不会放弃。");
        if (Global.getSector().getPlayerStats().getStoryPoints() < 1) {
            options.setEnabled(RESIST, false);
            options.setTooltip(RESIST, "需要 1 故事点");
        }
    }

    private void showOffice() {
        TextPanelAPI text = dialog.getTextPanel();
        OptionPanelAPI options = dialog.getOptionPanel();
        dialog.showVisualPanel();
        options.clearOptions();
        text.addPara("\n");
        text.addPara("\"舰长，我亲爱的舰长。\"");
        text.addPara("你一进门，就发现戴着港口指挥官帽子的家伙站在一旁，而他原本的位置上的人则开口道：\"我希望你知道我找你是为了什么。\"");
        options.addOption("承认走私", SMUGGLING);
        options.addOption("询问缘由", ASK_REASON);
    }

    private void showReason() {
        TextPanelAPI text = dialog.getTextPanel();
        OptionPanelAPI options = dialog.getOptionPanel();
        options.clearOptions();
        text.addPara("\n");
        text.addPara("\"啊，别来这套，舰长，别装傻了，那些船，船，我是指那些舰船！我知道你和他们不止见过面那么简单。\"说到这里时，他把板子从桌子上甩给你——但板子并不是亮着的。");
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null && state.totalPlayerOmegaBattles > 0) {
            text.addPara("\"看看那些伤痕痕迹，由于你在交战后大摇大摆的四处乱逛，现在很多人——无论是闹事还是好事者——都已经对这件事产生了不必要的兴趣。\"");
        }
        options.addOption("继续", CONTINUE_1);
    }

    private void showContact() {
        TextPanelAPI text = dialog.getTextPanel();
        OptionPanelAPI options = dialog.getOptionPanel();
        options.clearOptions();
        PTSDCrisisState state = PTSDCrisisState.get();
        text.addPara("通讯经过数次转接后，Je Otloes 出现在画面中。他身后的环境没有提供任何可以定位的细节。");
        if (state.jeSpamComplaintPending) {
            text.addPara(SPAM_COMPLAINTS[random.nextInt(SPAM_COMPLAINTS.length)]);
        } else if (state.jeMissedContactApologyPending) {
            text.addPara(MISSED_APOLOGIES[random.nextInt(MISSED_APOLOGIES.length)]);
        }
        if (state.phase == PTSDCrisisState.Phase.FORTIFICATION || state.phase == PTSDCrisisState.Phase.WAR) {
            text.addPara(LATE_URGENCY[random.nextInt(LATE_URGENCY.length)]);
        }
        PTSDJeOtloesManager.consumeSuccessfulContactContext();
        if (PTSDJeOtloesManager.isDetectorUnlockReady(state)) {
            showDetectorOffer(state);
            return;
        }
        addContactOptions(state);
    }

    private void addContactOptions(PTSDCrisisState state) {
        TextPanelAPI text = dialog.getTextPanel();
        OptionPanelAPI options = dialog.getOptionPanel();
        options.clearOptions();
        if (PTSDJeOtloesManager.isPostTaskBusy(state)) {
            text.addPara("Je 似乎正在处理后续情况。数条状态不断从他身后的屏幕上划过；至少短期内，他无法再替你整理新的目标。");
        } else if (state.jePlayerTaskIncidentId != null) {
            text.addPara(PLAYER_NOT_FINISHING_TASK[MathUtils.getRandomNumberInRange(0,PLAYER_NOT_FINISHING_TASK.length-1)]);
        } else if (state.jeAgentIncidentId != null || state.jeMeetingReady) {
            text.addPara("当前信道只在循环播放一段无法追踪来源的静态噪声。");
        } else {
            options.addOption("请求一项实地调查任务", CONTACT_PLAYER);
            options.addOption("请求他帮助调查", CONTACT_AGENT);
        }
        addLeave(options, "结束通讯");
    }

    private void showDetectorOffer(PTSDCrisisState state) {
        TextPanelAPI text = dialog.getTextPanel();
        OptionPanelAPI options = dialog.getOptionPanel();
        options.clearOptions();
        if (state.jeCompletedInvestigations >= 30) {
            text.addPara("Je 沉默地翻过一串任务记录。\"三十次。你替我确认了三十次别人连坐标都不愿意看的东西。至少这说明你不会把下面这件东西当成玩具。\"");
        } else if (state.phase == PTSDCrisisState.Phase.WAR) {
            text.addPara("\"我们原本打算再验证几个月。\"Je 看向画面外一片正在迅速变红的态势图，\"但时钟已经走完了。现在，未经验证也比什么都看不见强。\"");
        } else {
            text.addPara("\"你的航行日志、交战遥测和那些被证实的传闻终于叠出了一个图样。\"Je 把一块没有任何制造标识的数据板推入传输槽，\"它很糟，但比盲目要好。\"");
        }
        text.addPara("他解释说，这是一套从废弃监听阵列、民用航标和数个不愿署名的军方项目中拼接出的相关器。它无法告诉你目标是什么，却能让危机信号在背景噪声中留下方向。开启它，也会反过来放大某些未知活动对你的响应。");
        options.addOption("接收\"危机信号相关器\"", RECEIVE_DETECTOR, Misc.getHighlightColor(), "将新的开关能力加入生涯能力栏。开启后未知事件会更频繁，并指示附近已生成的精神创伤相关目标。");
        addLeave(options, "暂不接收");
    }

    private void showTaskCooldown() {
        TextPanelAPI text = dialog.getTextPanel();
        OptionPanelAPI options = dialog.getOptionPanel();
        options.clearOptions();
        int days = PTSDJeOtloesManager.daysUntilNextTask(PTSDCrisisState.get());
        text.addPara("\"目前不用麻烦你，也许你可以试试看%s天后再来找我，我大概能给你带来一些新的...传闻。\"",
                Color.YELLOW, String.valueOf(days));
        addLeave(options, "结束通讯");
    }
    private void showReportGreeting() {
        TextPanelAPI text = dialog.getTextPanel();
        OptionPanelAPI options = dialog.getOptionPanel();
        options.clearOptions();
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null && (state.phase == PTSDCrisisState.Phase.FORTIFICATION || state.phase == PTSDCrisisState.Phase.WAR)) {
            text.addPara(LATE_URGENCY[random.nextInt(LATE_URGENCY.length)]);
        } else {
            text.addPara(JE_SMALL_TALK[random.nextInt(JE_SMALL_TALK.length)]);
        }
        addGreetingReplies();
    }
    private void addGreetingReplies() {
        OptionPanelAPI options = dialog.getOptionPanel();
        try {
            int start = random.nextInt(PLAYER_SMALL_TALK.length);
            int index = (start) % PLAYER_SMALL_TALK.length;
            options.addOption(PLAYER_SMALL_TALK[index],
                    REPORT_REPLY_PREFIX + index);
        }
        catch (IndexOutOfBoundsException e) {
            options.addOption(PLAYER_SMALL_TALK[MathUtils.getRandomNumberInRange(0, PLAYER_SMALL_TALK.length-1)],
                    REPORT_REPLY_PREFIX + MathUtils.getRandomNumberInRange(0, PLAYER_SMALL_TALK.length-1));
        }
    }

    private void revealReport() {
        PTSDCrisisState.CrisisIncident incident = PTSDJeOtloesManager.getAgentIncident();
        TextPanelAPI text = dialog.getTextPanel();
        OptionPanelAPI options = dialog.getOptionPanel();
        options.clearOptions();
        if (incident == null) {
            text.addPara("\n\nJe 沉默了几秒，随后关闭了一块空白的信息板。\"目标记录不见了。这本身可能就是答案，但不是我想交给你的答案。\"");
            addLeave(options, "结束会面");
            return;
        }
        text.addPara("\n\"你要的事情我办完了，我希望你还记得。\"他顿了顿，\"" +
                incident.sourceLabel + "，关于" + incident.headline + "的那件事。\"");
        text.addPara("\n\"官方报告的说法是，我想你也还记得：" + incident.publicText + "\"");
        text.addPara("\"" + incident.trueText + "\"");
        PTSDNewsSiteManager.resolveRemotely(PTSDCrisisState.get(), incident);
        if (incident.siteTitle != null && !incident.siteTitle.isEmpty()) {
            text.addPara("\n\"现场是" + incident.siteTitle + "。" + incident.siteDescription + "\"");
        }
        options.addOption("继续", REPORT_REVEAL);
    }

    private void finishReport() {
        PTSDCrisisState.CrisisIncident incident = PTSDJeOtloesManager.getAgentIncident();
        float impact = panicImpact(incident);
        String label = panicLabel(impact);
        int percent = PTSDJeOtloesManager.completeAgentReport();
        TextPanelAPI text = dialog.getTextPanel();
        OptionPanelAPI options = dialog.getOptionPanel();
        options.clearOptions();
        text.addPara("\n\n你了解到这一事件对周边殖民地的影响：" + label,
                impact >= 40f ? Color.RED : Misc.getHighlightColor());
        text.addPara("\n\"但，我尽可能降低了影响，现在理论上，这件事造成的影响为之前的" +
                percent + "%\"");
        addLeave(options, "结束会面");
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        PTSDCrisisState state = PTSDCrisisState.get();
        TextPanelAPI text = dialog.getTextPanel();
        OptionPanelAPI options = dialog.getOptionPanel();

        if (RESIST.equals(optionData)) {
            Global.getSector().getPlayerStats().spendStoryPoints(
                    1, true, text, false, "强行摆脱港务安保");
            state.jeResistedOnce = true;
            PTSDCrisisDevIntel.report("Je Otloes 首次拦截被拒绝",
                    "玩家消耗故事点；下次合格港口将强制触发",
                    market == null || market.getStarSystem() == null ? null :
                            market.getStarSystem().getId(), null);
            dialog.dismiss();
            return;
        }
        if (GO_OFFICE.equals(optionData)) { showOffice(); return; }
        if (SMUGGLING.equals(optionData)) {
            options.clearOptions();
            text.addPara("\n");
            text.addPara("那人好像是听到了什么笑话一样，饶有兴趣的看着港口指挥官，而后看了看你，\"…你在走私？黑市相关的事情我根本不关注。不过…\"他侧身朝向发抖的港口指挥官，而眼睛仍旧盯着你，他脸部一侧的疮疤显得十分狰狞。");
            text.addPara("\n\"如果你是在他的眼皮子下自由自在的干这件事，那我后面也许会对这件事产生一些兴趣。\"");
            options.addOption("询问缘由", ASK_REASON);
            return;
        }
        if (ASK_REASON.equals(optionData)) { showReason(); return; }
        if (CONTINUE_1.equals(optionData)) {
            options.clearOptions();
            text.addPara("\n");
            text.addPara("\"实话实说，我不知道那是什么，也没人知道，但…\"他示意港口指挥官离开，后者看了看你。");
            text.addPara("\"我相信这位舰长不会对我的安全产生威胁，因为我们有着共同的棘手问题，不要让我说第二次。\"");
            text.addPara("在门被带上后，他打开了隔音立场，向你摆开几个信息板：\"现在各处都在传来疑点重重的报告，无论是官方内部信息还是民间新闻——后者我相信你也看过了不少，真假参半。\"");
            options.addOption("继续", CONTINUE_2);
            return;
        }
        if (CONTINUE_2.equals(optionData)) {
            options.clearOptions();
            text.addPara("\n");
            text.addPara("\"各方势力的侦查，\"他咬着牙从椅子上前倾，桌子反射的灯光和平板上的数据映射在他痛苦的表情上，但其中涉及到那近乎毁容的脸部的肌肉却没有抽动。\"大多数其实都以失败告终，如果舰队过大，那些家伙会老远就跑掉，而轻型侦查队往往连黑匣子都找不回来。\"");
            options.addOption("继续", CONTINUE_3);
            return;
        }
        if (CONTINUE_3.equals(optionData)) {
            options.clearOptions();
            text.addPara("\n");
            text.addPara("\"所以，舰长，如果可以的话，我希望能获得你的帮助。\"");
            text.addPara("他的眼睛早就布满血丝。");
            options.addOption("接受", ACCEPT);
            options.addOption("找借口推辞", DECLINE);
            return;
        }
        if (ACCEPT.equals(optionData)) {
            options.clearOptions();
            text.addPara("\n");
            text.addPara("他明显愉悦了很多，你现在才发觉他脸部的伤口大抵不是烧伤，而是某种更恐怖的东西造成的。");
            text.addPara("\"是的，是的，这样最好。\"");
            options.addOption("继续", ACCEPT_2);
            return;
        }
        if (ACCEPT_2.equals(optionData)) {
            options.clearOptions();
            text.addPara("\n");
            text.addPara("随后，他和你同步了各种信息，大多数就像你知道的那样，然而更多的汇报则显示，整个英仙座的四周都在逐渐出现这种报告，也就是，最糟糕的情况下：");
            text.addPara("\"我们被包围了，无论是哪种意义上。\"");
            text.addPara("\"目前我们对他们的有效信息还是太少了，我们需要像你这样的实地考察团去确认各种可疑信息。我们会减少后期名义上的官方行动，如果大众知道了这些信息，英仙座绝对会崩成一团散沙，记住我的话。\"");
            options.addOption("继续", ACCEPT_3);
            return;
        }
        if (ACCEPT_3.equals(optionData)) {
            options.clearOptions();
            text.addPara("\n");
            text.addPara("*一些事件会造成大众的恐慌，每当你确认具体情况后，官方会在此基础上保持舆论控制。");
            text.addPara("\n*如果 恐慌 增长过快，或者超过阈值，大众可能会自发行动，局势将会逐渐失控。");
            completeIntro();
            addLeave(options, "离开办公室");
            return;
        }
        if (DECLINE.equals(optionData)) {
            options.clearOptions();
            text.addPara("\n");
            text.addPara("\n");
            text.addPara("他叹了口气，示意你可以走了。");
            text.addPara("\"如果你改变主意了，我随时都在。\"");
            completeIntro();
            addLeave(options, "离开办公室");
            return;
        }
        if (CONTACT_PLAYER.equals(optionData)) {
            if (!PTSDJeOtloesManager.canStartAnotherTask(state)) { showTaskCooldown(); return; }
            options.clearOptions();
            PTSDCrisisState.CrisisIncident incident =
                    PTSDJeOtloesManager.startPlayerInvestigation(text);
            if (incident == null) text.addPara("\"现在没有合适的实地目标。等下一批报告。\"");
            addLeave(options, "结束通讯");
            return;
        }
        if (CONTACT_AGENT.equals(optionData)) {
            if (!PTSDJeOtloesManager.canStartAnotherTask(state)) { showTaskCooldown(); return; }
            options.clearOptions();
            PTSDCrisisState.CrisisIncident incident =
                    PTSDJeOtloesManager.startAgentInvestigation(text);
            if (incident == null) text.addPara("\"没有值得我亲自离线的目标。至少现在没有。\"");
            addLeave(options, "结束通讯");
            return;
        }
        if (RECEIVE_DETECTOR.equals(optionData)) {
            PTSDJeOtloesManager.grantDetector();
            options.clearOptions();
            text.addPara("数据包完成校验后，舰队能力栏中多出一个陌生的波形图标。");
            text.addPara("\"别把它当雷达。\"Je 的语气忽然严厉起来，\"它给你的只是方向，而且当你凝视那些信号时，它们也会更容易沿着同一条链路看回来。\"");
            addContactOptions(state);
            return;
        }
        if (optionData instanceof String && ((String) optionData).startsWith(REPORT_REPLY_PREFIX)) {
            int index = Integer.parseInt(((String) optionData).substring(REPORT_REPLY_PREFIX.length()));
            text.addPara(PLAYER_SMALL_TALK[index]);
            options.clearOptions();
            reportRound++;
            if (reportRound < 2) {
                text.addPara(JE_SMALL_TALK[random.nextInt(JE_SMALL_TALK.length)]);
                addGreetingReplies();
            } else {
                text.addPara("Je 的神情收敛下来。他把房间里的公共终端逐一切断，只留下自己带来的信息板。");
                options.addOption("谈正事", CONTINUE_4);
            }
            return;
        }
        if (CONTINUE_4.equals(optionData)) { revealReport(); return; }
        if (REPORT_REVEAL.equals(optionData)) { finishReport(); return; }
        if (LEAVE.equals(optionData)) {
            if (mode == Mode.REPORT) PTSDJeOtloesIntel.ensureIntel().finishMeeting();
            dialog.dismiss();
        }
    }

    private void completeIntro() {
        PTSDCrisisState state = PTSDCrisisState.get();
        state.jeIntroCompleted = true;
        state.jePendingIntroMarketId = null;
        PTSDJeOtloesIntel.ensureIntel();
        PTSDCrisisDevIntel.report("Je Otloes 成为独立联系人",
                "不占用普通联系人上限", market == null || market.getStarSystem() == null ?
                        null : market.getStarSystem().getId(), null);
    }

    private void addLeave(OptionPanelAPI options, String label) {
        options.addOption(label, LEAVE);
        options.setShortcut(LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
        dialog.setOptionOnEscape(label, LEAVE);
    }

    private static float panicImpact(PTSDCrisisState.CrisisIncident incident) {
        float total = 0f;
        if (incident != null && incident.panicByMarket != null) {
            for (Float value : incident.panicByMarket.values()) {
                if (value != null && value > 0f) total += value;
            }
        }
        return total;
    }

    private static String panicLabel(float value) {
        if (value < 5f) return "恐慌+";
        if (value < 10f) return "恐慌++";
        if (value < 20f) return "恐慌+++";
        if (value < 45f) return "【事件几近失控】恐慌++++";
        return "【事件失控】";
    }

    @Override public void optionMousedOver(String optionText, Object optionData) { }
    @Override public void advance(float amount) {
        if (mode != Mode.CONTACT || contactResult == null || contactResult == PTSDJeOtloesManager.ContactResult.CONNECTED || missedContactFinished) return;
        missedContactElapsed += Math.max(0f, amount);
        if (missedContactElapsed < 2f) return;
        missedContactFinished = true;
        dialog.getTextPanel().addPara("他似乎不在。");
        addLeave(dialog.getOptionPanel(), "关闭通讯");
    }
    @Override public void backFromEngagement(EngagementResultAPI battleResult) { }
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return Collections.emptyMap(); }
}
