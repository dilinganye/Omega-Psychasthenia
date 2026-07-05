package data.utils.iirt_omega;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;

//战斗技能(Combat Skills)
//
//- 战术训练(Tactical Drills)
//
//- 协同机动(Coordinated Maneuvers)
//
//- 狼群战术(Wolfpack Tactics)
//
//- 船员训练(Crew Training)
//
//- 战斗机上行链接(Fighter Uplink)
//
//- 载具群组(Carrier Group)
//
//- 军官培训(Officer Training)
//
//- 军官管理(Officer Management)
//
//- 最优秀者(Best of the Best)
//
//- 支持学说(Support Doctrine)
//
//
//
//导航技能(Navigation Skills)
//
//- 传感器技术(Sensors)
//
//- 炮手植入物(Gunnery Impants)
//
//- 能量武器精通(Energy Weapon Mastery)
//
//- 电子战(Electronic Warfare)
//
//- 磁场调节(Flux Regulation)
//
//- 神经网络增强(Neural Link)
//
//- 自动化船只(Automated Ships)
//
//
//
//后勤技能(Logistics Skills)
//
//- 物资运输(Bulk Transport)
//
//- 救援行动(Salvaging)
//
//- 维修工作(Field Repairs)
//
//- 偏振装甲(Polarized Armor)
//
//- 弹药专家(Ordnance Expertise)
//
//- 隔离程序(Containment Procedures)
//
//- 便携式装备制造(Makeshift Equipment)
//
//- 工业规划(Industrial Planning)
//
//- 废弃小队处理(Derelict Contingent)
//
//- 船体修复(Hull Restoration)
//
//
//
//电子战技能(Electronic Warfare Skills)
//
//- 电磁干扰(Omega ECM)
//
//- 超认知能力(Hypercognition)
//
public class IIRT_SD_OfficerData {

	//Silver_X：独特的赏金队伍领导人之一，打算在任务中出场。
	public static PersonAPI createSilver_X() {
		PersonAPI person = Global.getFactory().createPerson();
		person.setName(new FullName("Silver", "X", FullName.Gender.MALE));
		person.setFaction("IIRT");
		person.setPortraitSprite(Global.getSettings().getSpriteName("intel", "IIRT_Silver_X"));
		person.setPersonality(Personalities.AGGRESSIVE);
		person.setRankId(Ranks.SPECIAL_AGENT);
		person.setPostId(Ranks.POST_SPECIAL_AGENT);//设置该人物的职位
		person.setId("IIRT_Silver_X");

		person.getStats().setSkipRefresh(true);
		person.getStats().setLevel(10);
		person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
		person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
		person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
		person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);

		person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
		person.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 2);

		person.getStats().setSkillLevel(Skills.TACTICAL_DRILLS, 1);
		person.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
		person.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);
		person.getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 1);

		person.getStats().setSkipRefresh(false);

		Global.getSector().getImportantPeople().addPerson(person);

		return person;
	}

	//Davis_K_Ougust：超级星际骗子，还是跟小军阀，神偷一个，打算在任务中出场。
	public static PersonAPI createDavis_K_Ougust() {
		PersonAPI person = Global.getFactory().createPerson();
		person.setName(new FullName("Davis K", "Ougust", FullName.Gender.MALE));
		person.setFaction("IIRT");
		person.setPortraitSprite(Global.getSettings().getSpriteName("intel", "IIRT_Davis_K_Ougust"));
		person.setPersonality(Personalities.AGGRESSIVE);
		person.setRankId(Ranks.SPECIAL_AGENT);
		person.setPostId(null);
		person.setId("IIRT_Davis_K_Ougust");

		person.getStats().setSkipRefresh(true);

		person.getStats().setLevel(10);
		person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
		person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
		person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
		person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);

		person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
		person.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 2);

		person.getStats().setSkillLevel(Skills.TACTICAL_DRILLS, 1);
		person.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
		person.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);
		person.getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 1);

		person.getStats().setSkipRefresh(false);

		Global.getSector().getImportantPeople().addPerson(person);

		return person;
	}

	//Ai_Core_P0A：特殊AI系列，作为卫队的特殊AI部队。
	public static PersonAPI createEscort_Ai_Core_P01() {
		PersonAPI person = Global.getFactory().createPerson();
		person.setName(new FullName("断箭", "01", FullName.Gender.MALE));
		person.isAICore();
		person.setFaction("Escort");
		person.setPortraitSprite(Global.getSettings().getSpriteName("intel", "Ai_Core_P01"));
		person.setPersonality(Personalities.AGGRESSIVE);
		person.setRankId(Ranks.SPECIAL_AGENT);
		person.setPostId(null);
		person.setId("Escort_Ai_Core_P01");

		person.getStats().setSkipRefresh(true);

		person.getStats().setLevel(10);
		person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
		person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
		person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
		person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);

		person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
		person.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 2);

		person.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
		person.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);
		person.getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 1);

		person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);

		person.getStats().setSkipRefresh(false);

		Global.getSector().getImportantPeople().addPerson(person);

		return person;
	}

	public static PersonAPI createEscort_Ai_Core_P02() {
		PersonAPI person = Global.getFactory().createPerson();
		person.setName(new FullName("断桥", "02", FullName.Gender.MALE));
		person.isAICore();
		person.setFaction("Escort");
		person.setPortraitSprite(Global.getSettings().getSpriteName("intel", "Ai_Core_P02"));
		person.setPersonality(Personalities.AGGRESSIVE);
		person.setRankId(Ranks.SPECIAL_AGENT);
		person.setPostId(null);
		person.setId("Escort_Ai_Core_P02");

		person.getStats().setSkipRefresh(true);

		person.getStats().setLevel(10);
		person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
		person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
		person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
		person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);

		person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
		person.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 2);

		person.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
		person.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);
		person.getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 1);

		person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);

		person.getStats().setSkipRefresh(false);

		Global.getSector().getImportantPeople().addPerson(person);

		return person;
	}

	public static PersonAPI createEscort_Ai_Core_P03() {
		PersonAPI person = Global.getFactory().createPerson();
		person.setName(new FullName("熄炊", "03", FullName.Gender.MALE));
		person.isAICore();
		person.setFaction("Escort");
		person.setPortraitSprite(Global.getSettings().getSpriteName("intel", "Ai_Core_P03"));
		person.setPersonality(Personalities.AGGRESSIVE);
		person.setRankId(Ranks.SPECIAL_AGENT);
		person.setPostId(null);
		person.setId("Escort_Ai_Core_P03");

		person.getStats().setSkipRefresh(true);

		person.getStats().setLevel(10);
		person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
		person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
		person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
		person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);

		person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
		person.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 2);

		person.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
		person.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);
		person.getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 1);

		person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);

		person.getStats().setSkipRefresh(false);

		Global.getSector().getImportantPeople().addPerson(person);

		return person;
	}

	public static PersonAPI createEscort_Ai_Core_P04() {
		PersonAPI person = Global.getFactory().createPerson();
		person.setName(new FullName("残躯", "04", FullName.Gender.MALE));
		person.isAICore();
		person.setFaction("Escort");
		person.setPortraitSprite(Global.getSettings().getSpriteName("intel", "Ai_Core_P04"));
		person.setPersonality(Personalities.AGGRESSIVE);
		person.setRankId(Ranks.SPECIAL_AGENT);
		person.setPostId(null);
		person.setId("Escort_Ai_Core_P04");

		person.getStats().setSkipRefresh(true);

		person.getStats().setLevel(10);
		person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
		person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
		person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
		person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);

		person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
		person.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 2);

		person.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
		person.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);
		person.getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 1);

		person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);

		person.getStats().setSkipRefresh(false);

		Global.getSector().getImportantPeople().addPerson(person);

		return person;
	}
}