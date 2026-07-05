package data.scripts;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import data.scripts.world.systems.IIRT_Detachment_outpost;
import data.scripts.world.systems.KRM_Aleph_Post;
import data.scripts.world.systems.Omega_Pre_Zone_01;

import static data.scripts.IIRT_Omega_ModPlugin.OMEGA_PTSD_PREV;

public class IIRT_NormalGenerate implements SectorGeneratorPlugin {

	@Override
	public void generate(SectorAPI sector) {
		if(OMEGA_PTSD_PREV) {
			new KRM_Aleph_Post().generate(sector);
			// new Omega_Pre_Zone_01().generate(sector);
			// #临时禁用
			// new IIRT_Detachment_outpost().generate(sector);

			relationAdj(sector);
		}
	}

	private void relationAdj(SectorAPI sector) {

		FactionAPI KRM = sector.getFaction("KRM");
		FactionAPI IIRT_SD = sector.getFaction("IIRT");

		// 设置KRM好感度
		KRM.setRelationship(Factions.LUDDIC_CHURCH, -0.8f);
		KRM.setRelationship(Factions.LUDDIC_PATH, -0.8f);
		KRM.setRelationship(Factions.TRITACHYON, 0.6f);
		KRM.setRelationship(Factions.PERSEAN, -0.1f);
		KRM.setRelationship(Factions.PIRATES, -1f);
		KRM.setRelationship(Factions.INDEPENDENT, 0.05f);
		KRM.setRelationship(Factions.LIONS_GUARD, -0.4f);
		KRM.setRelationship(Factions.HEGEMONY, -0.45f);
		KRM.setRelationship(Factions.REMNANTS, -0.6f);
		KRM.setRelationship(Factions.OMEGA, -0.8f);

		IIRT_SD.setRelationship(Factions.LUDDIC_CHURCH, -1.0f);
		IIRT_SD.setRelationship(Factions.LUDDIC_PATH, -0.8f);
		IIRT_SD.setRelationship(Factions.TRITACHYON, 0.2f);
		IIRT_SD.setRelationship(Factions.PERSEAN, -0.2f);
		IIRT_SD.setRelationship(Factions.PIRATES, -0.8f);
		IIRT_SD.setRelationship(Factions.INDEPENDENT, 0.1f);
		IIRT_SD.setRelationship(Factions.LIONS_GUARD, -0.4f);
		IIRT_SD.setRelationship(Factions.HEGEMONY, -0.4f);
		IIRT_SD.setRelationship(Factions.REMNANTS, -0.6f);
		IIRT_SD.setRelationship(Factions.OMEGA, -1.0f);
		IIRT_SD.setRelationship(KRM.getId(), RepLevel.FAVORABLE);

		KRM.setRelationship(KRM.getId(), RepLevel.SUSPICIOUS);

		//与审判联盟、IIRT、Escort的独特关系
		FactionAPI BLG = sector.getFaction("BLG");
		if (BLG != null) {
			BLG.setRelationship(KRM.getId(), RepLevel.HOSTILE);
			BLG.setRelationship(IIRT_SD.getId(), RepLevel.FAVORABLE);
		}
		FactionAPI Escort = sector.getFaction("Escort");
		if (Escort != null) {
			Escort.setRelationship(KRM.getId(), RepLevel.FAVORABLE);
		}
		FactionAPI IIRT = sector.getFaction("IIRT");
		if (IIRT != null) {
			IIRT.setRelationship(KRM.getId(), RepLevel.INHOSPITABLE);
		}
	}
}