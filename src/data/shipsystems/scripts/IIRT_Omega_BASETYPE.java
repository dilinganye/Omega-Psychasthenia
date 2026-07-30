package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.I18nUtil;
import data.scripts.util.IIRT_Omega_Color;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.List;

public class IIRT_Omega_BASETYPE extends BaseShipSystemScript {

	private WeaponSlotAPI W01,W02,W03,S01,S02,S03,SSF;
	private int fireamount = 1;
	private final IntervalUtil timer = new IntervalUtil(0.5f, 1.5f);
	public static Object KEY_SHIP = new Object();
	private boolean hasCrack = false, runOnce = false;

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		//com.fs.starfarer.api.impl.combat.dweller.RiftLightningEffect
		//com.fs.starfarer.api.impl.combat.RiftCascadeEffect
		//com.fs.starfarer.api.impl.combat.threat.DisplacerGlowScript
		//com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion
		//com.fs.starfarer.api.impl.combat.RiftCascadeEffect
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine.isPaused() || engine == null) return;
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null) return; if (!ship.isAlive()) return;
		if (!runOnce) {
			for (WeaponSlotAPI s : ship.getHullSpec().getAllWeaponSlotsCopy()) {
				switch (s.getId()) {
					case "WS0001":
						W01 = s;
						break;
					case "WS0002":
						W02 = s;
						break;
					case "WS0003":
						W03 = s;
						break;
					case "SS_01":
						S01 = s;
						break;
					case "SS_02":
						S02 = s;
						break;
					case "SS_03":
						S03 = s;
						break;
					case "SSF":
						SSF = s;
						break;
				}
			}
			runOnce = true;
		}

		Color RIFT_LIGHTNING_COLOR = new Color(255, 47, 47, 145);
		Color negtive_color = new Color(200,255,200, 34);
		Color backColor = new Color(0, 0, 0, 255);
		Color MainFadeColor = new Color(180, 100, 255, 255);
		Color MainFadeColor2 = new Color(160, 123, 255, 255);
		Color DecFadeColor = new Color(248, 56, 255, 255);
		Vector2f start = new Vector2f(0,0);
		if (ship.getHullSpec().hasTag("Omega_System_Type_II")){
			MainFadeColor = new Color(221, 100, 255, 255);
			MainFadeColor2 = new Color(196, 123, 255, 255);
			DecFadeColor = new Color(255, 56, 209, 255);
			hasCrack = true;
		}
		if(Global.getSector().getCurrentLocation()!=null && Global.getSector().getCurrentLocation().getBackgroundColorShifter()!=null){
			Global.getSector().getCurrentLocation().getBackgroundColorShifter().getBase();
		}
		float rampUp = 0.25f + 0.25f * (float) Math.random(),dur = 1f + (float) Math.random();
// ————————————————————————————————————————_____________________————————————————————————————————————————————
		if (state == State.IN) {// ————————————————————————————————————————
			ship.fadeToColor(KEY_SHIP, backColor, 0.1f, 0.1f, effectLevel);
			ship.setJitterUnder(KEY_SHIP, MainFadeColor, effectLevel, 15, 0f, 15f);
			if(hasCrack) {
				stats.getTurnAcceleration().modifyMult(id, 3);
				stats.getMaxTurnRate().modifyMult(id, 2);
			}
		}
// ————————————————————————————————————————
		if (state == State.ACTIVE) {// ————————————————————————————————————————
			ship.fadeToColor(KEY_SHIP, MainFadeColor2, 0.1f, 0.1f, effectLevel);
			ship.setJitterUnder(KEY_SHIP, DecFadeColor, effectLevel, 15, 0f, 15f);
			if (hasCrack) {
			}
		}
// ————————————————————————————————————————
		if (state == State.OUT) { // ————————————————————————————————————————
			I18nUtil.easyRippleOut(ship.getLocation(), ship.getVelocity(),
						ship.getCollisionRadius(),
						90f,
						ship.getCollisionRadius(),
						ship.getCollisionRadius()/5f,
						20f);
			ship.setJitterUnder(ship, new Color(100,60,255,174), Math.min((effectLevel + 0.5f), 1f), 2, 0f, 10f);


			if(!hasCrack){
				ship.fadeToColor(KEY_SHIP, new Color(238, 18, 18, 255), 0.1f, 0.1f, effectLevel);
				ship.setJitterUnder(KEY_SHIP, new Color(99, 18, 238, 255), effectLevel, 15, 0f, 15f);
			}
		}

// ————————————————————————————————————————
		if (state == State.COOLDOWN) { // ————————————————————————————————————————
			stats.getAcceleration().unmodify(id);
			stats.getDeceleration().unmodify(id);
			stats.getMaxTurnRate().unmodify(id);
			stats.getTurnAcceleration().unmodify(id);
			stats.getMaxSpeed().unmodify(id);
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		CombatEngineAPI engine = Global.getCombatEngine();
		timer.setElapsed(0);
		hasCrack = false;
		runOnce = false;
		fireamount = 1;
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getMaxSpeed().unmodify(id);
	}

	public static void spawnEmp(ShipAPI source, Vector2f start, Vector2f end, float thickness, Color color) {
		CombatEngineAPI engine = Global.getCombatEngine();


		EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
		params.segmentLengthMult = 10f;
		params.zigZagReductionFactor = 0.35f;
		params.fadeOutDist = 50f;
		params.minFadeOutMult = 3f;
		params.flickerRateMult = 0.7f;

		params.movementDurOverride = Math.max(0.05f, MathUtils.getDistance(start, end) / 100000f);
		params.glowSizeMult = 2f;
		params.brightSpotFullFraction = 0.4f;
		EmpArcEntityAPI arc = (EmpArcEntityAPI) engine.spawnEmpArcVisual(
				start, source,
				end, null,
				thickness, // thickness
				color,
				new Color(255, 255, 255, 255),
				params
		);
		arc.setCoreWidthOverride(40f);

		arc.setRenderGlowAtStart(false);
		arc.setFadedOutAtStart(true);
		arc.setSingleFlickerMode(true);
	}
}
