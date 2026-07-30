package data.scripts.campaign.cargo;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.BaseAICoreOfficerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;

import java.util.Random;

public class IIRT_Lab_AICoreOfficerPluginImpl extends BaseAICoreOfficerPluginImpl implements AICoreOfficerPlugin {

	public static int OMEGA_PSYCH_POINTS = 0;

	/**
	 * Multiplier to deployment recovery cost for computing the effect of the "Automated Ships" skill.
	 * Omega_Psychasthenia
	 */
	public static float OMEGA_PSYCH_MULT = 5;

	@Override
	public PersonAPI createPerson(String aiCoreId, String factionId, Random random) {
		if (random == null) random = new Random();

		PersonAPI person = Global.getFactory().createPerson();
		person.setFaction(factionId);
		person.setAICoreId(aiCoreId);

		CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(aiCoreId);
		boolean omega_phs = Commodities.OMEGA_CORE.contentEquals(aiCoreId);

		person.getStats().setSkipRefresh(true);

		person.setName(new FullName(spec.getName(), "", FullName.Gender.ANY));
		int points = 0;
		float mult = 1f;
		if (omega_phs) { // assume it's not going to be integrated, no reason to do it - same as assuming it's always integrated
			person.setPortraitSprite("graphics/portraits/characters/omega.png");
			person.getStats().setLevel(9);
			person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
			person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
			person.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
			//person.getStats().setSkillLevel(Skills.SHIELD_MODULATION, 2);
			person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
			//person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
			person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
			//person.getStats().setSkillLevel(Skills.RELIABILITY_ENGINEERING, 2);
			person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
			person.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
			person.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
			person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
			person.getStats().setSkillLevel(Skills.OMEGA_ECM, 2);
			points = OMEGA_PSYCH_POINTS;
			mult = OMEGA_PSYCH_MULT;
		}

		if (points != 0) {
			person.getMemoryWithoutUpdate().set(AUTOMATED_POINTS_VALUE, points);
		}
		person.getMemoryWithoutUpdate().set(AUTOMATED_POINTS_MULT, mult);

		person.setPersonality(Personalities.RECKLESS);
		person.setRankId(Ranks.SPACE_CAPTAIN);
		person.setPostId(null);

		person.getStats().setSkipRefresh(false);

		return person;
	}

}

