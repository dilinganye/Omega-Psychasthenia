package data.scripts;

import com.fs.starfarer.api.campaign.SectorAPI;
import exerelin.campaign.SectorManager;

import static data.scripts.IIRT_Omega_ModPlugin.OMEGA_PTSD_PREV;

public class IIRT_NEXGenerate extends IIRT_NormalGenerate {
	
	@Override
	public void generate(SectorAPI sector) {
		if (SectorManager.getManager().isCorvusMode() && OMEGA_PTSD_PREV) {
			super.generate(sector);
		}
	}
}