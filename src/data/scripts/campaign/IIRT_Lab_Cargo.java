package data.scripts.campaign;

import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import com.fs.starfarer.api.impl.campaign.GenericFieldItemManager;

public class IIRT_Lab_Cargo extends BaseCustomEntityPlugin {

	//	private CustomCampaignEntityAPI entity;
	private transient GenericFieldItemManager manager;

	@Override
	public void init(SectorEntityToken entity, Object pluginParams) {
		super.init(entity, pluginParams);
		//this.entity = (CustomCampaignEntityAPI) entity;
		readResolve();
	}

	Object readResolve() {
		manager = new GenericFieldItemManager(entity);
		manager.category = "KRM_bp";
		manager.key = "cargoPods";
		manager.cellSize = 32;

		manager.minSize = 10;
		manager.maxSize = 10;

		//manager.initDebrisIfNeeded();
		//manager.numPieces = 15;

		return this;
	}

	@Override
	public void advance(float amount) {
		if (manager == null) {
			return;
		}

		if (entity.isInCurrentLocation()) {
			float totalCapacity = entity.getRadius();
			int minPieces = 5;
			int numPieces = (int)(totalCapacity / 4);
			if (numPieces < minPieces) numPieces = minPieces;
			if (numPieces > 40) numPieces = 40;

			manager.numPieces = numPieces;
		}
		manager.advance(amount);
	}

	@Override
	public float getRenderRange() {
		return entity.getRadius() + 100f;
	}

	@Override
	public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
		manager.render(layer, viewport);
	}

}



