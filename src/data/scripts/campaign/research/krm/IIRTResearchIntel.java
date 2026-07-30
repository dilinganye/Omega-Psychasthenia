package data.scripts.campaign.research.krm;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.EventFactor;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.Color;
import java.util.List;

public class IIRTResearchIntel extends BaseEventIntel {

	public static String KEY = "$IIRT_Lab_Research_ref";
	public static int TAB_BUTTON_HEIGHT = 20;
	public static int TAB_BUTTON_WIDTH = 180;
	public static int ENTRY_HEIGHT = 80;
	public static int ENTRY_WIDTH = 300;
	public static int IMAGE_WIDTH = 80;
	public static int BUTTON_WIDTH = 120;
	public static int IMAGE_DESC_GAP = 12;
	public static float PLAYER_REWARD_FRAC = 0.1f;// 玩家至少需要提供的贡献度百分比
	public static float SAMPLE_FRAC = 0.8f; // 提供蓝图的研究，终止时提供样品需要的最低百分比
	public static final String INFO = "info";
	public static final String RESEARCH_ICON = "";
	public static final String WEAPON_ICON = "";

	public enum REWARD_STAGE {
		BP, REWARD
	}

	private List<IIRTLabInstitute> institutes;
	private IIRTLabInstitute activeInstitute = null;

	private String currentTab = INFO;

	public IIRTResearchIntel() {
		super();
		// setup();
		institutes = IIRTLabLoader.loadInstitutes();
		Global.getSector().getMemoryWithoutUpdate().set(KEY, this);
		Global.getSector().getIntelManager().addIntel(this);
	}

	@Override
	public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
		TooltipMakerAPI main = panel.createUIElement(width, height, true);
		if (currentTab == null) currentTab = INFO;
		TooltipMakerAPI buttonHolder = addTabButtons(main, panel, width);
		if (currentTab.contentEquals(INFO)) {
			//drawInfoPanel(panel, width, height); //todo
			return;
		}
		for (IIRTLabInstitute institute : institutes) {
			if (institute.getId().contentEquals(currentTab)) {
				activeInstitute = institute;
				activeInstitute.createLargeDescription(panel, width, height);
			}
		}

		panel.addUIElement(main).belowLeft(buttonHolder, 3);
	}

	protected TooltipMakerAPI addTabButtons(TooltipMakerAPI tm, CustomPanelAPI panel, float width) {
		FactionAPI fc = getFactionForUIColors();
		Color base = fc.getBaseUIColor(), bg = fc.getDarkUIColor(), bright = fc.getBrightUIColor();

		TooltipMakerAPI btnHolder1 = generateTabButton(panel, "简介", INFO, base, bg, bright, null);
		for (IIRTLabInstitute institute : institutes) {
			generateTabButton(panel, institute.getName(), institute.getId(), base, bg, bright, btnHolder1);
		}
		return btnHolder1;
	}

	public TooltipMakerAPI generateTabButton(CustomPanelAPI buttonRow, String nameId, String id, Color base, Color bg, Color bright, TooltipMakerAPI rightOf) {
		TooltipMakerAPI holder = buttonRow.createUIElement(TAB_BUTTON_WIDTH, TAB_BUTTON_HEIGHT, false);

		ButtonAPI button = holder.addAreaCheckbox(nameId, id, base, bg, bright, TAB_BUTTON_WIDTH, TAB_BUTTON_HEIGHT, 0);
		button.setChecked(id.contentEquals(this.currentTab));

		if (rightOf != null) {
			buttonRow.addUIElement(holder).rightOfTop(rightOf, 4);
		} else {
			buttonRow.addUIElement(holder).inTL(0, 3);
		}
		return holder;
	}

	@Override
	public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {

		ui.updateUIForItem(this);
		return;
	}

	public void addFactorToInstitute(EventFactor factor, IIRTLabInstitute institute) {
		institute.addFactor(factor);
	}

	@Override
	public void reportEconomyTick(int iterIndex) {
		for (IIRTLabInstitute institute : institutes) {
			institute.reportEconomyTick(iterIndex);
			if (institute.getActiveProject() != null && institute.getActiveProject().isFinished() && !institute.getActiveProject().isRewarded()) {
				IIRTLabProject active = institute.getActiveProject();
				active.finish(active.getPlayerRetribution() > active.getProgress() * PLAYER_REWARD_FRAC, true, active.isWithBP());
				if (institute.getActiveProject().isCompetitive()) {
					for (IIRTLabInstitute institute2 : institutes) {
						if (institute2 == institute) continue;
						for (IIRTLabProject project : institute2.getProjects()) {
							if (project.isCompetitive() && project.getTier() == active.getTier() && !project.isFinished() && !project.isRewarded) {
								project.finish(project.getPlayerRetribution() > project.getProgress() * PLAYER_REWARD_FRAC, project.getCurr() >= SAMPLE_FRAC * project.getProgress(), false);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void reportEconomyMonthEnd() {
		for (IIRTLabInstitute institute : institutes) {
			institute.reportEconomyMonthEnd();
		}
	}

	public int getMonthlyProgress(IIRTLabInstitute institute) {
		return institute.getMonthlyProgress();
	}

}
