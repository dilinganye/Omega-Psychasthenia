package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;

public class IIRT_Omega_Person {

	public IIRT_Omega_Person() {
	}

	public static PersonAPI getPerson(String id) {
		return Global.getSector().getImportantPeople().getPerson(id);
	}

	public static void create() {
		createFactionLeaders();
		createMiscCharacters();
	}

	private static void createFactionLeaders() {
		ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
		MarketAPI market = null;
		market = Global.getSector().getEconomy().getMarket("KRM_planet3_market");
		PersonAPI person;
		if (market != null) {
			person = Global.getFactory().createPerson();
			person.setId("KRM_Doc");
			person.setFaction("KRM");
			person.setGender(FullName.Gender.MALE);
			person.setRankId(Ranks.FACTION_LEADER);
			person.setPostId(Ranks.POST_FACTION_LEADER);
			person.setImportance(PersonImportance.VERY_HIGH);
			person.getName().setFirst("Krausm");
			person.getName().setLast("X");
			person.setPortraitSprite("graphics/portraits/Lab/IIRT_Lab_Krausm_Class.png");
			person.setPortraitSprite(Global.getSettings().getSpriteName("intel", "IIRT_Lab_Krausm_Class"));
			person.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
			person.getStats().setSkillLevel(Skills.BULK_TRANSPORT, 1);
			person.getStats().setSkillLevel(Skills.PHASE_CORPS, 1);
			market.setAdmin(person);
			market.getCommDirectory().addPerson(person);
			market.addPerson(person);
			ip.addPerson(person);
		}
	}

	private static void createMiscCharacters() {
		ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
		MarketAPI market = null;
		market = Global.getSector().getEconomy().getMarket("KRM_planet2_market");
		PersonAPI person;
		if (market != null) {
			person = Global.getFactory().createPerson();
			person.setId("KRM_Yisic");
			person.setFaction("KRM");
			person.setGender(FullName.Gender.MALE);
			person.setPostId(Ranks.POST_SPECIAL_AGENT);
			person.setImportance(PersonImportance.MEDIUM);
			person.getName().setFirst("Yisic");
			person.getName().setLast("Deln");
			person.setPortraitSprite(Global.getSettings().getSpriteName("intel", "IIRT_Lab_Yisic"));
			market.setAdmin(person);
			market.getCommDirectory().addPerson(person);
			market.addPerson(person);
			ip.addPerson(person);
		}
	}
}