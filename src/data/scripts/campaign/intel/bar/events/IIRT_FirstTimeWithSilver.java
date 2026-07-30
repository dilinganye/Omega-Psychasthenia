package data.scripts.campaign.intel.bar.events;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson;
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub;
import static data.scripts.util.IIRT_Omega_Color.IIRT_SD_Story_Words;
import data.scripts.util.IIRT_SD_OfficerData;

import java.awt.Color;
import java.util.Map;

public class IIRT_FirstTimeWithSilver extends BaseBarEventWithPerson {

	public enum StoryLine {
		BEGIN,
		K_TP,
		K_WHO_R_U,
		K_I_HAVE_JOBS_TO_DO,
		X_OUT_BATTLE, X_OUT_BATTLE_2,
		X_THIS_IS_JOB,
		X_OFFLINE,
		X_WRONG_PERSON, X_WRONG_PERSON_ASK,
		X_WHO_ARE_U, X_WHO_ARE_U_2,
		X_WHAT_R_U_DO, X_WHAT_R_U_DO_2, X_WHAT_R_U_DO_3,
		X_YOU_NEED_SAY_SRY, X_YOU_NEED_SAY_SRY_2,
		X_YOU_NEED_SAY_SRY_ASK, X_YOU_NEED_SAY_SRY_3, X_YOU_NEED_SAY_SRY_3_2, X_YOU_NEED_SAY_SRY_4,
		X_ABOUT_END,
		X_HERE_IS_END,
		REST,
		END
	}

	@Override
	protected void regen(MarketAPI market) {
		if (this.market == market) return;
		super.regen(market);
		//设置剧情主人翁
		person.setPortraitSprite(Global.getSettings().getSpriteName("intel", "IIRT_Army"));
		person.setName(new FullName("=1=", "=初遇=", FullName.Gender.MALE));
	}

	//酒馆简介/酒馆选项
	@Override
	public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		super.addPromptAndOption(dialog, memoryMap);

		regen(dialog.getInteractionTarget().getMarket());

		TextPanelAPI text = dialog.getTextPanel();

		text.addPara("酒馆外面突然有些嘈杂，这在智械里可不常见。", IIRT_SD_Story_Words);

		dialog.getOptionPanel().addOption("出去看一看", this, IIRT_SD_Story_Words, null);
	}

	//中介段，把选项中转到剧情里去
	@Override
	public void init(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		super.init(dialog, memoryMap);

		done = false;

		dialog.getVisualPanel().showPersonInfo(person, true);

		optionSelected(null, StoryLine.BEGIN);
	}

	@Override
	public void optionSelected(String optionText, Object optionData) {
		if (!(optionData instanceof StoryLine option)) {
			return;
		}

		OptionPanelAPI options = dialog.getOptionPanel();
		TextPanelAPI text = dialog.getTextPanel();
		options.clearOptions();
		//剧情部分
		switch (option) {
			case BEGIN://一切的开端
				if (Global.getSector().getFaction("IIRT").getRelToPlayer().getRel() < 0.2f) {
					//BeginConversation("IIRT_Davis_K_Ougust")
					//相识Davis K Ougust   IIRT_Davis_K_Ougust
					//person.setPortraitSprite(Global.getSettings().getSpriteName("intel", "IIRT_Davis_K_Ougust"));
					//person.setName(new FullName("???", " ", FullName.Gender.MALE));
					//person.setRankId(Ranks.POST_UNKNOWN);
					//person.setPostId(null);
					text.addPara("你刚走出门，就被一个人拉到一旁的走廊内。\"舰长，我强烈建议您不要去凑热闹，现在，您先不要关注太多事情。\"\n" + "他示意你不要出声，之后拿出一些稀奇古怪的玩意折腾了半天，你感觉一股能量在你的旁边流动。之后周围的景色被扭曲、打乱、重组。\n" + "那种能量消失时，你已经不在酒馆了，好消息与坏消息都是你没结账。\n" + "你愤怒的看向罪魁祸首，对方站在阴影里，你看不清他的容貌。他制止了你继续提问的请求，而是使用问题来回答你。\n" + "\"那么舰长，你来这里做什么？\"他笑着对你说，你能感觉到那种笑意更多是一种江湖滑头的嘴脸。\n");
					options.addOption("\"为什么把我传送到别的地方？(不回答问题)\"", StoryLine.K_TP);
					options.addOption("\"你是谁？(不回答问题)\"", StoryLine.K_WHO_R_U);
					options.addOption("\"我自有我自己要做的事情\"", StoryLine.K_I_HAVE_JOBS_TO_DO);
					break;
				} else {
					//相识Silver X
					text.addPara("一出门，你就看见一个曾经是人类的生物被击倒在地上。一旁的爬虫状智械则直接将对方的整个左小腿直接活生生拽了下来。");
					text.addPara("远处几个类似有着身着军用大衣的家伙将几个血肉模糊的人形生物按在地上拳打脚踢，你刚刚听到的惨叫声就是从这里发出的。");
					text.addPara("\"别打了，我..求...我说！我说！\"其中一个人这么叫喊着，看着他整个被撕下来的脸皮，你很怀疑他还能活多久。");
					text.addPara("\"把这个家伙带回去，送到运输机上。\"你这才看清那几个身着大衣——准确的说，那是它们身体结构的一部分——的家伙，是几个以西卡的智械，但很明显它们的地位，或职责和其他智械有着很大不同。而刚刚的爬虫智械则注意到了你，正想对你采取行动之时，被其中一个以西卡智械踢了一脚拦了下来。");
					text.addPara("几个智械都扭头看向了你，其中一个向你说到\"请不要妨碍我们执行公务。\"");
					text.addPara("附近除了你和那几个被揍翻在地动弹不得的家伙外，就只剩下那些智械冷漠的看着你。");
					options.addOption("试图阻止暴行", StoryLine.X_OUT_BATTLE_2);
					options.addOption("询问发生了什么事", StoryLine.X_OUT_BATTLE);
					options.addOption("返回酒馆", StoryLine.REST);
					break;
				}
			case X_OUT_BATTLE_2://K的剧情
				text.addPara("你让你的手下去试图阻止这些暴行的发生，但很明显这起不到任何作用，在几声惨叫后这些陆战队员就被按在地上，但并没有什么明显的伤痕。");
				text.addPara("对方很明显没有下狠手，除了那个爬虫——它真的很想咬你，现在它攻击你的欲望似乎更强烈了一点。");
				text.addPara("附近除了你和那几个被揍翻在地动弹不得的家伙外，就只剩下那些智械冷漠的看着你。");
				options.addOption("继续", StoryLine.X_OUT_BATTLE);
				break;
			case X_OUT_BATTLE://K的剧情
				//person.setPortraitSprite(Global.getSettings().getSpriteName("intel", "IIRT_Silver_X"));
				//person.setName(new FullName("???", " ", FullName.Gender.MALE));
				text.addPara("那些智械冷漠的盯着你，似乎是在等着什么一样。");
				text.addPara("你意识到身后有着什么东西靠近，你下意识躲闪。你和其他陆战队的链接不知道什么时候被切断了，几个智械那幽紫色的光线让你的虹膜愈发感到不适。");
				text.addPara("那是一把幽紫色的棍子，你不太清楚那具体是什么东西，但挨一下绝对不会好受。");
				text.addPara("你向刚刚攻击你的方向看了过去，那是一个和其他智械很类似的家伙，但它的武器拿在手上——就是刚刚从你头顶挥过去的那个——。智械将你逼退到那些嗷嗷乱叫的人旁边，随后只是单纯堵住了你的退路。这时候你收到了来自你船上通讯官的讯息。\"");
				text.addPara("\"舰长，我们在船上检测到一些其他...啊嗷！.什么——啊别.(电流声)啊呃——\"", new Color(255, 250, 120, 255));
				text.addPara("[通讯断开]", new Color(255, 100, 100, 255));
				options.addOption("继续", StoryLine.X_OFFLINE);
				break;
			case X_OFFLINE://K的剧情
				text.addPara("那些智械仍然只是盯着你看，这令你头皮发麻，也许它们上来揍你一顿都比现在这个状况要好。");
				text.addPara("你试图联系上其他舰船或是什么其他人，但没有任何通讯录上的名单是你能打通的。");
				text.addPara("在这个过程中，你意识到你和其他陆战队的链接不知道什么时候被切断了，几个智械那幽紫色的光线愈发让你的虹膜感到不适。");
				text.addPara(" ");
				text.addPara("你感觉自己刚刚不应该多管闲事。");
				options.addOption("继续", StoryLine.X_THIS_IS_JOB);
				break;
			case X_THIS_IS_JOB://K的剧情
				text.addPara("\"呃？不是？\"刚刚向你挥棍的家伙突然不知道在和谁说话，\"船上没有那些东西？\"");
				text.addPara("\"新目标不是对象？不可能，这绝不可能，我的仪器清清楚楚的显示那个杂种就是进了酒馆！这是我们的工作，而你说那里没有？\"");
				text.addPara("\"你个小杂种给我听好了，如果你不能找到那个家伙的线索，我向塔发誓我绝对会在统计会议上把你的摄像头拿下来！然后把它混在弹药里安在字节炮上打出去！\"");
				text.addPara("你估计喋喋不休的家伙是在打电话，而且你似乎很快就要脱险了。");
				options.addOption("也许是个好消息", StoryLine.X_WRONG_PERSON);
				break;
			case X_WRONG_PERSON://K的剧情
				//person.setRankId(Ranks.SPECIAL_AGENT);
				text.addPara("其他几个智械面面相觑，之前踢爬虫的那位则示意你先不要说话\"BOSS现在似乎不太开心...\"，它靠近你小声说，\"看来你不是我们要抓的对象，我对此深表抱歉。你的人估计在被确认并非目标后就被放掉了。\"他看了看那个发飙的家伙，确认对方还没有消停，于是随后继续对你说：\"我们是惩戒部——也就是你们所了解的警察，具体任务我不能对你多说。\"");
				text.addPara("\"我建议你待会问一问BOSS，它性格还不错...哦，它似乎缓过来了。\"");
				text.addPara("之前差点对你的头部进行敲击，随后还骂骂咧咧的那个智械已经停止了刚刚那种短路一样的状态。现在它看向你的目光和姿势都变得更没有敌意，而且更随意了起来，像是个自己的风筝(S)被炼狱炮轰了一发的舰长一样颓废。虽然它们只有摄像头一样的脑袋和一个光球一样的 眼睛 ，但你仍然能感到对方的那种疲惫感。");
				text.addPara("\"很抱歉，这位舰长...我们似乎搞错了目标对象。虽然对象一开始也并非是你，但是我们误以为你是它的同伙。\"");
				text.addPara("\"这些情报部给的抓捕消息总是这么不确切，不过至少地上那几个并不冤枉，Ped，把他们带走。但那几个陆战队留下，那些人是这位舰长的队伍。\"刚刚对你说悄悄话的智械闻言后，指挥其他几个智械把那些被打的半死的肉块放到爬虫上，随后走到路口拐角后就不知道去了哪里。");
				options.addOption("\"所以...\"", StoryLine.X_WRONG_PERSON_ASK);
				break;
			case X_WRONG_PERSON_ASK://K的剧情
				text.addPara("\"...我相信你有很多要问的。\"");
				options.addOption("\"你们是谁？\"", StoryLine.X_WHO_ARE_U);
				options.addOption("\"你们刚刚在做什么？\"", StoryLine.X_WHAT_R_U_DO);
				options.addOption("\"你刚刚可是差点打死我和我的队伍。\"", StoryLine.X_YOU_NEED_SAY_SRY);
				options.addOption("\"没什么了。\"", StoryLine.X_ABOUT_END);
				break;
			case X_WHO_ARE_U://K的剧情
				//person.setName(new FullName("Silver", "X", FullName.Gender.MALE));
				text.addPara("\"你可以叫我 Silver X ，刚刚被我叫走的是 Iron Ped 。\"");
				options.addOption("继续", StoryLine.X_WHO_ARE_U_2);
				break;
			case X_WHO_ARE_U_2://K的剧情
				text.addPara("它随意的转着刚刚差点打中你脑袋的棍子，然后继续说到：\"我们是以西卡的惩戒部探员，负责一些特殊的案件。当然，我们偶尔也会做一些针对性猎杀的操作，毕竟警察大多都会这么做。\"");
				text.addPara("你很确定普通警察不会那么做。");
				options.addOption("返回", StoryLine.X_WRONG_PERSON_ASK);
				break;
			case X_WHAT_R_U_DO://K的剧情
				text.addPara("\"哦...日常工作的一环，\"它随意的扬了下它的摄像头脑袋，\"当然你看到的已经是收尾阶段，我们为这个抓捕可是付出了相当长的准备。\"");
				options.addOption("\"那你们带走的那些人是...罪犯？\"", StoryLine.X_WHAT_R_U_DO_2);
				break;
			case X_WHAT_R_U_DO_2://K的剧情
				text.addPara("\"罪犯？差不多吧，这些人犯下的罪行在其他势力也少不了一次颇为严重的审判，具体内容我不好和你细说，但出于刚刚的误操作，或许适当对你说一些也不是什么问题...\"");
				text.addPara("\"那些人参与了一次针对实验性科技的盗窃行动，实际上这种事情本来不应该归我们管，但是由于这种任务包含需要很多不止在以西卡区域内智械的事情。所以才需要我们出场。\"");
				options.addOption("\"这么说你们是干黑活的？\"", StoryLine.X_WHAT_R_U_DO_3);
				break;
			case X_WHAT_R_U_DO_3://K的剧情
				text.addPara("\"啊，当然。\"");
				text.addPara("\"所以我相信我们以后还会见面的，总有可能。\"");
				options.addOption("返回", StoryLine.X_WRONG_PERSON_ASK);
				break;
			case X_YOU_NEED_SAY_SRY://K的剧情
				text.addPara("\"我对刚刚发生的一切深表抱歉，我们没能抓到那个混账，而且还对你的人员造成了一定的...皮肉伤害。所以...\"");
				text.addPara("它凭空叫出了一个数据板，浮空的窗口在你们之间若隐若现。");
				options.addOption("\"这是什么。\"", StoryLine.X_YOU_NEED_SAY_SRY_2);
				break;
			case X_YOU_NEED_SAY_SRY_2://K的剧情
				text.addPara("\"这是我的道歉，舰长。\"");
				text.addPara("\"在我们附近的一处星区内，有一个独特的黄矮星，也就是这个。\"他给你指出了那个数据，你仔细看了看。");
				text.addPara("Causton", new Color(255, 100, 100, 255));
				text.addPara("\"我之前在调查一些敌对势力舰队的时候，曾经路过这里。那里有着一颗宜居星球，对于你们这种生物来说应该很合适？反正我们用不着，哈！\"你不确定机器人笑起来是什么表情，尤其是这种没有嘴的。");
				text.addPara("\"我在那里放置了一个探测器，但现在已经在当局的控制下了。所以我不建议你之间夺取它的权限。\"");
				text.addPara("\"但这一切都不是最主要的，最主要的是，那里有一些独特的无人舰船...他们的敌意很高，具体原因我不清楚，也不在乎。\"");
				text.addPara("\"但是我听说你们这种舰长很喜欢挑战，也许你会喜欢这个消息。\"");
				text.addPara("\"即使你不是，你也可以在之后利用一下那个星系。\"");
				text.addPara("提示：此事件不会标识此星系位置，请自行记录恒星名并自行寻找。", IIRT_SD_Story_Words);
				options.addOption("听起来不错", StoryLine.X_YOU_NEED_SAY_SRY_ASK);
				break;
			case X_YOU_NEED_SAY_SRY_ASK://K的剧情
				text.addPara("\"还有什么疑问吗？\"");
				options.addOption("\"你为什么不给我确切的位置？\"", StoryLine.X_YOU_NEED_SAY_SRY_3);
				options.addOption("查看简报", StoryLine.X_YOU_NEED_SAY_SRY_4);
				options.addOption("没有问题", StoryLine.X_WRONG_PERSON_ASK);
				break;
			case X_YOU_NEED_SAY_SRY_3://K的剧情
				text.addPara("\"我其实甚至都没有义务向你道歉。\"对方似乎对你的要求不怎么开心。");
				text.addPara("\"我本以为你会更有一些探索的精神，而不是一个懒蛋。\"");
				text.addPara("\"自己去找，我不那么喜欢懒散的人。\"");
				options.addOption("\"只是问一问。\"", StoryLine.X_YOU_NEED_SAY_SRY_3_2);
				break;
			case X_YOU_NEED_SAY_SRY_3_2://K的剧情
				text.addPara("\"你最好是。\"");
				options.addOption("继续", StoryLine.X_YOU_NEED_SAY_SRY_ASK);
				break;
			case X_YOU_NEED_SAY_SRY_4://K的剧情
				text.addPara("\"这是详细数据。\"");
				text.addPara("你看了看这些表格和记录，这些东西指向的位置说不清道不明，但是整体似乎在克劳斯姆所在的星域附近一段距离，你正打算问，对方已经给出了更详细的数据。", IIRT_SD_Story_Words);
				text.addPara("坐标：-3.14w, -1.18w，我估计这样你会更感兴趣。", IIRT_SD_Story_Words);
				text.addPara("我必须提醒你，那里存在极多的威胁，我不建议你在没做好准备的情况下就进去找死。", new Color(255, 159, 159, 255));
				text.addPara("提示：此事件不会标识此星系位置，请自行记录恒星名并自行寻找。", IIRT_SD_Story_Words);
				options.addOption("返回", StoryLine.X_YOU_NEED_SAY_SRY_ASK);
				break;
			case X_ABOUT_END://K的剧情
				text.addPara("\"再次对将你卷进这种事情表示道歉，虽然...我记得是你先介入的。\"");
				text.addPara("\"我们或许还会再见面的，舰长。\"它转身走去，\"祝你航灯不熄，驱动稳定。\"");
				options.addOption("真是一次奇怪的遭遇", StoryLine.X_HERE_IS_END);
				break;
			case X_HERE_IS_END://K的剧情
				text.addPara("你感觉你们有可能还会再次相遇。(并不会，因为我没写完)");
				options.addOption("结束", StoryLine.END);

				ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
				//此处以X入侵的League of Watchmen为例
				MarketAPI market_low = Global.getSector().getEconomy().getMarket("IIRT_planet2_market");
				//事先删除整个market里的所有人物，只留一个我们新建的marx
				//if (market_low != null) {
				//	for (PersonAPI p : market_low.getPeopleCopy()) {
				//		market_low.removePerson(p);
				//		ip.removePerson(p);
				//		market_low.getCommDirectory().removePerson(p);
				//	}
				PersonAPI Silver_X = IIRT_SD_OfficerData.createSilver_X();
				Silver_X.setImportanceAndVoice(PersonImportance.HIGH, null);//设置人物的重要性，至于Voice是角色打招呼的语气，例如voice = faithful就会说“卢德保佑你”之类，可在rules中自定义

				//marx.setFaction("watchmen");//设置阵营，但是由于之前我们创建时候就设置了，所以这里注释掉
				//marx.addTag(Tags.CONTACT_MILITARY);//为人物增加tag，例如贸易，军方，影响人物能够派发的联络人任务

				ip.addPerson(Silver_X);//只有加入ImportantPeople，该人物才能被rules和missionHub识别
				ip.getData(Silver_X).getLocation().setMarket(market_low);//将人物传送到指定market里
				ip.checkOutPerson(Silver_X, "permanent_staff");//"这个的意思是把人物以'永久成员(permanent_staff)'的理由签发出去，如此一来就不会成为某些随机任务的目标。“————感谢议长订正
				//	Silver_X.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);//设置人物的技能，这里给他加了1级的工业规划

				//market_low.setAdmin(Silver_X);//市场管理员设置为他
				market_low.getCommDirectory().addPerson(Silver_X, 0);//将其加入通讯录中
				market_low.addPerson(Silver_X);//将该person加入市场的人物列表，使某些按市场寻人的方法可以找到
				//ContactIntel.addPotentialContact(Silver_X, market_low, dialog.getTextPanel());	//Alex的联系人版本
				//这里是设置该人物拥有多少个额外任务上限，若不填，则每次只能刷出一个任务，若填1，则每次最多能刷出2个人物，填2则最多刷出3个。
				Silver_X.getMemoryWithoutUpdate().set(BaseMissionHub.NUM_BONUS_MISSIONS, 1);
				//为人物添加MissionHub
				//BaseMissionHub.set(Silver_X, new BaseMissionHub(Silver_X));
				ContactIntel intel = new ContactIntel(Silver_X, market_low);
				Global.getSector().getIntelManager().addIntel(intel, false);    //群友的联系人版本
				break;

			case K_TP://K的剧情
				text.addPara("\"外面是以西卡的特种条子，它们最近盯得很紧...\"\n" + "\"我很清楚您是谁，这帮智械并不喜欢您。\"他看了看你，又笑了笑，随后你感到他的脸似乎和你最开始见到的人不一样了。\"如果撞个对面，这些条子可不会就这么轻易放过您的...\"\n" + "\"它们刚刚在抓捕一些其他势力的间谍，或许是 霸主 ，或许是 卢德左径 ，但其他的罪犯它们也会顺手收拾掉。\"他继续说，\"这些条子可不会就这么轻易放过您的...\"");
				options.addOption("继续", StoryLine.K_WHO_R_U);
				break;
			case K_WHO_R_U://K的剧情
				//person.setName(new FullName("K", " ", FullName.Gender.MALE));
				text.addPara("\"而我...\"\n" + "\"您可以叫我K，\"K 往前走了两步，你终于看清了他的脸。\"一个普普通通值得相信的星际散户。\"\n" + "你非常相信星际散户不会拥有这种单人传送设备。\n" + "\"您还是小有名气的，我很期待与您的合作...\"");
				options.addOption("\"我还有事要做(未完成其他选项)\"", StoryLine.K_I_HAVE_JOBS_TO_DO);
				break;
			case K_I_HAVE_JOBS_TO_DO://K的剧情_end
				text.addPara("\"我们以后还会见面的，但不是现在。\"他给你指了一条回去的路，\"别和那些条子撞上，祝您好运。\"\n" + "他转身混进了交易区，没一会你就找不到这个讨人厌的家伙了。\n");
				options.addOption("真是个怪事(后续未完成)", StoryLine.END);
				break;

			case REST://中途离开(保留)
				noContinue = true;
				done = true;
				break;

			case END://结束故事(END)
				noContinue = true;
				done = true;
				BarEventManager.getInstance().notifyWasInteractedWith(this);
				break;

		}
	}

	@Override    // 重写了shouldShowAtMarket方法
	public boolean shouldShowAtMarket(MarketAPI market) {
		if (!super.shouldShowAtMarket(market)) return false;

		// 如果派系不是"IIRT",则不生成此事件
		if (!market.getFactionId().contentEquals("IIRT")) {
			return false;
		}
		// 如果是Debug模式默认开启
		return Global.getSector().getPlayerStats().getLevel() >= 0 || DebugFlags.BAR_DEBUG;
	}

	@Override
	public boolean isAlwaysShow() {
		return true;
	}
}