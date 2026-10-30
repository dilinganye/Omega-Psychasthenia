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
import data.scripts.IIRT_Omega_ModPlugin;
import data.scripts.campaign.invasion.PTSDCrisisState;
import org.lazywizard.lazylib.MathUtils;

import java.util.Random;

import static com.fs.starfarer.api.characters.FullName.Gender.ANY;
import static com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent.pickPortraitPreferNonDuplicate;

public class PTSD_Omega_AIOfficerPlugin extends BaseAICoreOfficerPluginImpl implements AICoreOfficerPlugin {
    public PTSD_Omega_AIOfficerPlugin() {
    }
    @Override
    public PersonAPI createPerson(String aiCoreId, String factionId, Random random) {
        if (random == null) random = new Random();
        PTSDCrisisState state = PTSDCrisisState.get();
        return createPerson(aiCoreId, factionId, random,
                data.scripts.PTSDDifficultySettings.officerLevel(state, PTSDCrisisState.getDay()));
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
        person.setName(new FullName(designation + "-" + (1000 + random.nextInt(9000)), "", ANY));
        //person.setPortraitSprite("graphics/portraits/special/portraits_GravenAI.png");
        person.setPortraitSprite(pickPortraitPreferNonDuplicate(theFaction, ANY));
        int level = Math.max(2, Math.min(20, PTSD_Level));
        person.getStats().setLevel(level);
        // The configured level also limits combat skills; otherwise a nominally level-6
        // officer would retain the old fixed set of eleven elite skills.
        String[] skills = {
                Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, Skills.IMPACT_MITIGATION,
                Skills.FIELD_MODULATION, Skills.GUNNERY_IMPLANTS, Skills.COMBAT_ENDURANCE,
                Skills.DAMAGE_CONTROL, Skills.POINT_DEFENSE, Skills.ENERGY_WEAPON_MASTERY,
                Skills.SYSTEMS_EXPERTISE, Skills.OMEGA_ECM, Skills.HYPERCOGNITION
        };
        int available = level > 12 ? skills.length : level > 8 ? skills.length - 1 :
                level > 4 ? skills.length - 2 : skills.length - 3;
        for (int index = 0; index < Math.min(level, available); index++)
            person.getStats().setSkillLevel(skills[index], 2);
        float mult = AICoreOfficerPluginImpl.OMEGA_MULT;
        person.getMemoryWithoutUpdate().set(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT, mult);
        person.setPersonality("reckless");
        person.setRankId(Ranks.SPACE_CAPTAIN); //临时，之后也要让他随机
        person.setPostId((String)null);
        person.getStats().setSkipRefresh(false);
        return person;
    }
}
