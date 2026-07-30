package data.scripts.campaign.cargo;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl;
import com.fs.starfarer.api.impl.campaign.BaseAICoreOfficerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import org.lazywizard.lazylib.MathUtils;

import java.util.Random;

import static com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent.pickPortraitPreferNonDuplicate;

public class PTSD_Omega_AIOfficerPlugin extends BaseAICoreOfficerPluginImpl implements AICoreOfficerPlugin {
    public PTSD_Omega_AIOfficerPlugin() {
    }
    @Override
    public PersonAPI createPerson(String aiCoreId, String factionId, Random random) {
        if (random == null) random = new Random();
        return createPerson(aiCoreId, factionId, random, 8 + random.nextInt(7));
    }

    public PersonAPI createPerson(String aiCoreId, String factionId, Random random, int PTSD_Level) {
        FactionAPI theFaction = Global.getSector().getFaction(factionId);
        PersonAPI person = Global.getFactory().createPerson();
        person.setFaction(factionId);
        person.setAICoreId(aiCoreId);
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(aiCoreId);
        person.getStats().setSkipRefresh(true);
        String designation = PTSDOmegaFleetSupport.WATCHER_FACTION_ID.equals(factionId)
                ? "Unknown target" : "P.T.S.D. Scout";
        person.setName(new FullName(designation + "-" + (1000 + random.nextInt(9000)), "", Gender.ANY));
        //person.setPortraitSprite("graphics/portraits/special/portraits_GravenAI.png");
        person.setPortraitSprite(pickPortraitPreferNonDuplicate(theFaction, person.getGender()));
        person.getStats().setLevel(PTSD_Level);
        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
        person.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
        person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
        person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
        //person.getStats().setSkillLevel(Skills.RELIABILITY_ENGINEERING, 2);
        person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
        person.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
        person.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
        person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
        if(PTSD_Level>4){
            person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
        }
        if(PTSD_Level>8){
            person.getStats().setSkillLevel(Skills.OMEGA_ECM, 2);
        }
        if(PTSD_Level>12){
            person.getStats().setSkillLevel(Skills.HYPERCOGNITION, 2);

        }
        float mult = AICoreOfficerPluginImpl.OMEGA_MULT;
        person.getMemoryWithoutUpdate().set(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT, mult);
        person.setPersonality("reckless");
        person.setRankId(Ranks.SPACE_CAPTAIN); //临时，之后也要让他随机
        person.setPostId((String)null);
        person.getStats().setSkipRefresh(false);
        return person;
    }
}
