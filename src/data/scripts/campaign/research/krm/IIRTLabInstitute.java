package data.scripts.campaign.research.krm;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.EventFactor;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipLocation;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.research.krm.IIRTResearchIntel.REWARD_STAGE;
import data.scripts.campaign.research.krm.factor.IIRTLabResearchSpeedFactor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IIRTLabInstitute extends BaseEventIntel {

	protected String id;
	protected String name;
	protected String desc;
	protected String spriteName;
	protected int tier = 1;

	protected List<EventFactor> factors = new ArrayList<>();
	protected List<IIRTLabProject> projects = new ArrayList<>();
	protected IIRTLabProject activeProject;

	public IIRTLabInstitute(String id, String name, String desc, String spriteName, int baseSpeed) {
		this.id = id;
		this.name = name;
		this.desc = desc;
		this.spriteName = spriteName;
		factors.add(new IIRTLabResearchSpeedFactor(baseSpeed, "基础研究速度"));
	}

	@Override
	public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
		TooltipMakerAPI main = panel.createUIElement(width, height, true);
		drawInstituteProgressTab(main, panel, width, height);
		panel.addUIElement(main).inTL(0, 0);
	}

	// 项目
	protected void drawInstituteProgressTab(TooltipMakerAPI main, CustomPanelAPI panel, float width, float height) {
		float opad = 10f;
		uiWidth = width;
		if (activeProject != null) {
			drawProjectPanel(main, panel, width, height);
		}
		main.addSpacer(opad);
		drawInstitutePanel(main, panel, width, height);

		float barW = getBarWidth();
		float factorWidth = (barW - opad) / 2f;

		if (withMonthlyFactors() != withOneTimeFactors()) {
			// factorWidth = barW;
			factorWidth = (int)(barW * 0.6f);
		}

		TooltipMakerAPI mFac = main.beginSubTooltip(factorWidth);

		Color c = getFactionForUIColors().getBaseUIColor();
		Color bg = getFactionForUIColors().getDarkUIColor();
		mFac.addSectionHeading("Monthly factors", c, bg, Alignment.MID, opad).getPosition().setXAlignOffset(0);

		float strW = 40f;
		float rh = 20f;
		// rh = 15f;
		mFac.beginTable2(getFactionForUIColors(), rh, false, false, "Monthly factors", factorWidth - strW - 3, "Progress", strW);

		for (EventFactor factor : factors) {
			if (factor.isOneTime()) continue;
			if (!factor.shouldShow(this)) continue;

			String desc = factor.getDesc(this);
			if (desc != null) {
				mFac.addRowWithGlow(Alignment.LMID, factor.getDescColor(this), desc, Alignment.RMID, factor.getProgressColor(this), factor.getProgressStr(this));
				TooltipCreator t = factor.getMainRowTooltip(this);
				if (t != null) {
					mFac.addTooltipToAddedRow(t, TooltipLocation.RIGHT, false);
				}
			}
			factor.addExtraRows(mFac, this);
		}

		// mFac.addButton("TEST", new String(), factorWidth, 20f, opad);
		mFac.addTable("None", -1, opad);
		mFac.getPrev().getPosition().setXAlignOffset(-5);

		main.endSubTooltip();

		TooltipMakerAPI oFac = main.beginSubTooltip(factorWidth);

		oFac.addSectionHeading("Recent one-time factors", c, bg, Alignment.MID, opad).getPosition().setXAlignOffset(0);

		oFac.beginTable2(getFactionForUIColors(), 20f, false, false, "One-time factors", factorWidth - strW - 3, "Progress", strW);

		List<EventFactor> reversed = new ArrayList<>(factors);
		Collections.reverse(reversed);
		for (EventFactor factor : reversed) {
			if (!factor.isOneTime()) continue;
			if (!factor.shouldShow(this)) continue;

			String desc = factor.getDesc(this);
			if (desc != null) {
				oFac.addRowWithGlow(Alignment.LMID, factor.getDescColor(this), desc, Alignment.RMID, factor.getProgressColor(this), factor.getProgressStr(this));
				TooltipCreator t = factor.getMainRowTooltip(this);
				if (t != null) {
					oFac.addTooltipToAddedRow(t, TooltipLocation.LEFT);
				}
			}
			factor.addExtraRows(oFac, this);
		}

		oFac.addTable("None", -1, opad);
		oFac.getPrev().getPosition().setXAlignOffset(-5);
		main.endSubTooltip();

		float factorHeight = Math.max(mFac.getHeightSoFar(), oFac.getHeightSoFar());
		mFac.setHeightSoFar(factorHeight);
		oFac.setHeightSoFar(factorHeight);

		if (withMonthlyFactors() && withOneTimeFactors()) {
			main.addCustom(mFac, opad * 2f);
			main.addCustomDoNotSetPosition(oFac).getPosition().rightOfTop(mFac, opad);
		} else if (withMonthlyFactors()) {
			main.addCustom(mFac, opad * 2f);
		} else if (withOneTimeFactors()) {
			main.addCustom(oFac, opad * 2f);
		}

	}

	// 独立项目的panel出来
	protected void drawProjectPanel(TooltipMakerAPI main, CustomPanelAPI panel, float width, float height) {
		float opad = 10f;
		uiWidth = width;
		IIRTLabProject active = activeProject;
		EventProgressBarAPI bar = main.addEventProgressBar(this, 100f);
		TooltipCreator barTC = getBarTooltip();
		if (barTC != null) {
			main.addTooltipToPrevious(barTC, TooltipLocation.BELOW, false);
		}

		for (EventStageData curr : stages) {
			if (curr.progress <= 0) continue;
			if (RANDOM_EVENT_NONE.equals(curr.rollData)) continue;
			if (curr.wasEverReached && curr.isOneOffEvent && !curr.isRepeatable) continue;
			if (curr.hideIconWhenPastStageUnlessLastActive && curr.progress <= progress && getLastActiveStage(true) != curr) {
				continue;
			}
			EventStageDisplayData data = createDisplayData(curr.id);
			UIComponentAPI marker = main.addEventStageMarker(data);
			float xOff = bar.getXCoordinateForProgress(curr.progress) - bar.getPosition().getX();
			marker.getPosition().aboveLeft(bar, data.downLineLength).setXAlignOffset(xOff - data.size / 2f - 1);

			TooltipCreator tc = getStageTooltip(curr.id);
			if (tc != null) {
				main.addTooltipTo(tc, marker, TooltipLocation.LEFT, false);
			}
		}

		// progress indicator
		{
			UIComponentAPI marker = main.addEventProgressMarker(this);
			float xOff = bar.getXCoordinateForProgress(progress) - bar.getPosition().getX();
			marker.getPosition().belowLeft(bar, -getBarProgressIndicatorHeight() * 0.5f - 2).setXAlignOffset(xOff - getBarProgressIndicatorWidth() / 2 - 1);
		}

		main.addSpacer(opad);
		main.addSpacer(opad);
		{
			String icon = getSpriteName();
			String desc = "";
			String prefix = "开发 ";
			if (active.repeatable) {
				prefix = "生产 ";
			} else {
				if (active.withBP) {
					prefix = "研发 ";
				}
			}
			switch (active.getType()) {
				case FIGHTER:
					FighterWingSpecAPI fspec = Global.getSettings().getFighterWingSpec(active.getReward());
					if (fspec != null) {
						icon = fspec.getVariant().getHullSpec().getSpriteName();
						desc = fspec.getWingName();
					}
					break;
				case SHIP:
					ShipHullSpecAPI hspec = Global.getSettings().getHullSpec(active.getReward());
					if (hspec != null) {
						icon = hspec.getSpriteName();
						desc = hspec.getNameWithDesignationWithDashClass();
					}
					break;
				case WEAPON:
					WeaponSpecAPI wspec = Global.getSettings().getWeaponSpec(active.getReward());
					if (wspec != null) {
						icon = IIRTResearchIntel.WEAPON_ICON;
						desc = wspec.getWeaponName();
					}
					break;
				case RESEARCH_SPEED:
					desc = active.getReward();
					if (desc.startsWith("%")) {
						float topercent = 10;
						try {
							String Adoa = "\\+";
							topercent = Float.parseFloat(desc.split(Adoa)[0]);
						} catch (Exception e) {
							topercent = 10 * active.getTier();
						}
						desc = "+" + topercent + "%";
					}
					prefix = "提高研究速度 ";
					break;
				default:
					break;

			}

			float imageSize = getImageSizeForStageDesc(REWARD_STAGE.REWARD);
			// float opad = 10f;
			float indent = 0;
			indent = 10f;
			indent += getImageIndentForStageDesc(REWARD_STAGE.REWARD);
			// float Dwidth = getBarWidth() - indent * 2f;

			TooltipMakerAPI info = main.beginImageWithText(icon, imageSize, width, true);
			// TooltipMakerAPI info =
			// main.beginImageWithText("graphics/icons/missions/ga_intro.png", 64);
			Color h = Misc.getHighlightColor();
			info.addPara(prefix + desc, 0f, h, desc);
			if (info.getHeightSoFar() > 0) {
				main.addImageWithText(opad).getPosition().setXAlignOffset(indent);
				main.addSpacer(0).getPosition().setXAlignOffset(-indent);
			}
		}
	}

	// 设计所部分
	protected void drawInstitutePanel(TooltipMakerAPI main, CustomPanelAPI panel, float width, float height) {
		float opad = 10f;
		uiWidth = width;

		main.addSpacer(opad);
		main.addSpacer(opad);
		TooltipMakerAPI institute = main.beginSubTooltip(width - 12f);
		Color c = getFactionForUIColors().getBaseUIColor();
		Color bg = getFactionForUIColors().getDarkUIColor();
		institute.addSectionHeading(getName(), c, bg, Alignment.MID, opad).getPosition().setXAlignOffset(0);
		float imageSize = 64f;
		TooltipMakerAPI para = institute.beginImageWithText(getSpriteName(), imageSize, width - 12f, true);
		// para.setParaFontVictor14();
		para.addPara(getDesc(), 0f);
	}

	@Override
	public FactionAPI getFactionForUIColors() {
		if (Global.getSector().getFaction("KRM") != null) {
			return Global.getSector().getFaction("KRM");
		}
		return Global.getSector().getPlayerFaction();
	}

	public void addFactorToInstitute(EventFactor factor, IIRTLabInstitute institute) {
		institute.addFactor(factor);
	}

	@Override
	public void reportEconomyTick(int iterIndex) {
		if (getActiveProject() != null) {
			int delta = getMonthlyProgress();
			float numIter = Global.getSettings().getFloat("economyIterPerMonth");
			float f = 1f / numIter;

			delta *= f;
			delta += progressDeltaRemainder;

			if (activeProject != null && !activeProject.isFinished()) {
				activeProject.setCurr(activeProject.getCurr() + delta);
			}
		}
	}

	@Override
	public int getMonthlyProgress() {
		int total = 0;
		float mult = 1f;
		for (EventFactor factor : getFactors()) {
			if (factor.isOneTime()) continue;
			total += factor.getProgress(this);
			mult *= factor.getAllProgressMult(this);
		}

		if (total != 0) {
			float sign = Math.signum(total);
			total = Math.round(sign * Math.abs(total) * mult);
			if (total == 0) total = Math.round(1f * sign);
		}

		total = Math.min(total, getMaxMonthlyProgress());

		return total;
	}

	@Override
	public void reportEconomyMonthEnd() {
		if (activeProject.isRewarded()) {
			if (activeProject.isRepeatable()) {
				activeProject.setCurr(0);
				activeProject.setFinished(false);
				activeProject.setRewarded(false);
				activeProject = null;
			} else {
				projects.remove(activeProject);
				activeProject = null;
			}
		}
		if (activeProject == null) {
			activeProject = pickNewProject();
		}
	}

	protected IIRTLabProject pickNewProject() {
		WeightedRandomPicker<IIRTLabProject> picker = new WeightedRandomPicker<>();
		for (IIRTLabProject project : projects) {
			if (project.getTier() > tier) continue;
			float factor = 1;
			if (project.isCompetitive() && project.getTier() == tier) factor = 10;
			picker.add(project, project.getRarity() * factor);
		}
		if (picker.isEmpty()) {
			return null;
		} else {
			return picker.pick();
		}
	}

	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	public String getDesc() {
		return desc;
	}

	public String getSpriteName() {
		return spriteName;
	}

	public IIRTLabProject getActiveProject() {
		return activeProject;
	}

	@Override
	public List<EventFactor> getFactors() {
		return factors;
	}

	public List<IIRTLabProject> getProjects() {
		return projects;
	}

	public void setTier(int tier) {
		this.tier = tier;
	}

	public int getTier() {
		return tier;
	}

	public void increaseTier() {
		tier += 1;
	}

	public void setProjects(List<IIRTLabProject> projects) {
		this.projects = projects;
	}

	@Override
	public void setProgress(int progress) {
		if (activeProject == null) return;
		if (activeProject.getCurr() == progress) return;

		if (progress < 0) progress = 0;
		if (progress > maxProgress) progress = maxProgress;

		activeProject.setCurr(progress);
	}
}
