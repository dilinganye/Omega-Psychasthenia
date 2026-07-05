package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.utils.iirt_omega.I18nUtil;
import data.utils.iirt_omega.IIRT_Omega_Color;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;
import sound.A;

import java.awt.*;
import java.util.List;

public class IIRT_Omega_ECM_hijack extends BaseShipSystemScript {
	public static Object KEY_SHIP = new Object();
	public static Object KEY_TARGET = new Object();
	public static Color TEXT_COLOR = new Color(255,55,55,255);

	protected static float RANGE = 1500f;
	public static float DAM_MULT = 3f;

	public static float INCOMING_DAMAGE_MULT = 0.5f;
	public static float REPAIR_RATE_MULT = 10f;

	private boolean isMano = false, hasCrack = false;
	private final IntervalUtil ECM_DecShock_timer1 = new IntervalUtil(0.24f, 1.77f);
	private final IntervalUtil ECM_DecShock_timer2 = new IntervalUtil(1.00f, 5.00f);
	private final IntervalUtil ECM_DecShock_timer3 = new IntervalUtil(0.70f, 2.00f);

	public static class TargetData {
		public ShipAPI ship;
		public ShipAPI target;
		public EveryFrameCombatPlugin targetEffectPlugin;
		public float currEcmDamageMult;
		public float elaspedAfterInState;
		public TargetData(ShipAPI ship, ShipAPI target) {
			this.ship = ship;
			this.target = target;
		}
	}

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		//com.fs.starfarer.api.impl.combat.dweller.RiftLightningEffect
		//com.fs.starfarer.api.impl.combat.RiftCascadeEffect
		//com.fs.starfarer.api.impl.combat.threat.DisplacerGlowScript
		//com.fs.starfarer.api.impl.combat.EntropyAmplifierStats
		ShipAPI ship = (ShipAPI)stats.getEntity();
		CombatEngineAPI engine = Global.getCombatEngine();
		final String targetDataKey = ship.getId() + "_entropy_target_data";

		if (ship.getHullSpec().hasTag("Omega_System_Type_II")){
			hasCrack = true;
		}
		Color ECM_LIGHTNING_COLOR = new Color(196, 255, 222, 145);
		Color negtive_color = new Color(200,255,200, 34);
		Color backColor = new Color(0, 0, 0, 255);
		Color MainFadeColor = new Color(255, 247, 212, 225);
		Color MainFadeColor2 = new Color(131, 127, 104, 173);
		Color DecFadeColor = new Color(146, 56, 255, 255);

		Object targetDataObj = Global.getCombatEngine().getCustomData().get(targetDataKey);

		ship.fadeToColor(KEY_SHIP, backColor, 0.1f, 0.1f, effectLevel);
		ship.setJitterUnder(KEY_SHIP, ECM_LIGHTNING_COLOR, effectLevel, 15, 0f, 15f);
		if (state == State.IN && targetDataObj == null) {
			ShipAPI target = findTarget(ship);
			Global.getCombatEngine().getCustomData().put(targetDataKey, new TargetData(ship, target));
			if (target != null) {
				if (target.getFluxTracker().showFloaty() ||
						ship == Global.getCombatEngine().getPlayerShip() ||
						target == Global.getCombatEngine().getPlayerShip()) {
					target.getFluxTracker().showOverloadFloatyIfNeeded("ECM Hijack!", TEXT_COLOR, 4f, true);
				}
			}
		} else if (state == State.IDLE && targetDataObj != null) {
			Global.getCombatEngine().getCustomData().remove(targetDataKey);
			((TargetData)targetDataObj).currEcmDamageMult = 1f;
			targetDataObj = null;
		}
		if (targetDataObj == null || ((TargetData) targetDataObj).target == null) return;

		final TargetData targetData = (TargetData) targetDataObj;
		targetData.currEcmDamageMult = 1f + (DAM_MULT - 1f) * effectLevel;


		if (targetData.target != null
				&& targetData.target.getOriginalOwner() != ship.getOriginalOwner()
				&& targetData.target.getHullSpec().getMinCrew()>0
				&& !targetData.target.getHullSpec().getTags().contains("Gamma_dbp")   // Gamma isn't Mano!
				&& !targetData.target.getHullSpec().getTags().contains("Gamma_bp")		// Gamma is Gamma!
				&& !targetData.target.getHullSpec().getTags().contains("show_Gamma_Hullmods")) isMano = true;
		if (targetData.targetEffectPlugin == null) {
			targetData.targetEffectPlugin = new BaseEveryFrameCombatPlugin() {
				@Override
				public void advance(float amount, List<InputEventAPI> events) {
					if (Global.getCombatEngine().isPaused()) return;
					if (targetData.target == Global.getCombatEngine().getPlayerShip()) {
						Global.getCombatEngine().maintainStatusForPlayerShip(KEY_TARGET,
								targetData.ship.getSystem().getSpecAPI().getIconSpriteName(),
								targetData.ship.getSystem().getDisplayName(),
								"==Got ECM Hijack==", true);
					}

					if (targetData.currEcmDamageMult <= 1f || !targetData.ship.isAlive()) {
						targetData.target.getMutableStats().getDamageToTargetEnginesMult().unmodify(id);
						targetData.target.getMutableStats().getDamageToTargetWeaponsMult().unmodify(id);
						targetData.target.getMutableStats().getCombatEngineRepairTimeMult().unmodify(id);
						targetData.target.getMutableStats().getCombatWeaponRepairTimeMult().unmodify(id);
						targetData.target.getMutableStats().getEmpDamageTakenMult().unmodify(id);
						Global.getCombatEngine().removePlugin(targetData.targetEffectPlugin);
					} else {
						targetData.target.getMutableStats().getDamageToTargetEnginesMult().modifyMult(id, targetData.currEcmDamageMult);
						targetData.target.getMutableStats().getDamageToTargetWeaponsMult().modifyMult(id, targetData.currEcmDamageMult);
						targetData.target.getMutableStats().getCombatEngineRepairTimeMult().modifyMult(id, targetData.currEcmDamageMult);
						targetData.target.getMutableStats().getCombatWeaponRepairTimeMult().modifyMult(id, targetData.currEcmDamageMult);
						targetData.target.getMutableStats().getEmpDamageTakenMult().modifyMult(id, targetData.currEcmDamageMult);
					}
				}
			};
			Global.getCombatEngine().addPlugin(targetData.targetEffectPlugin);
		}


		if (effectLevel > 0) {
			stats.getHullDamageTakenMult().modifyMult(id, 1f - (1f - INCOMING_DAMAGE_MULT) * effectLevel);
			stats.getArmorDamageTakenMult().modifyMult(id, 1f - (1f - INCOMING_DAMAGE_MULT) * effectLevel);
			stats.getEmpDamageTakenMult().modifyMult(id, 1f - (1f - INCOMING_DAMAGE_MULT) * effectLevel);

			stats.getCombatEngineRepairTimeMult().modifyMult(id, 1f / (1f + (REPAIR_RATE_MULT - 1f) * effectLevel));
			stats.getCombatWeaponRepairTimeMult().modifyMult(id, 1f / (1f + (REPAIR_RATE_MULT - 1f) * effectLevel));

			if (state != State.IN) {
				targetData.elaspedAfterInState += Global.getCombatEngine().getElapsedInLastFrame();
			}
			float shipJitterLevel = 0;
			if (state == State.IN) {
				shipJitterLevel = effectLevel;
			} else {
				float durOut = 0.5f;
				shipJitterLevel = Math.max(0, durOut - targetData.elaspedAfterInState) / durOut;
			}
			float targetJitterLevel = effectLevel;

			float maxRangeBonus = 50f;
			float jitterRangeBonus = shipJitterLevel * maxRangeBonus;

			if (shipJitterLevel > 0) {
				//ship.setJitterUnder(KEY_SHIP, JITTER_UNDER_COLOR, shipJitterLevel, 21, 0f, 3f + jitterRangeBonus);
				ship.setJitter(KEY_SHIP, MainFadeColor, shipJitterLevel, 4, 0f, 0 + jitterRangeBonus * 1f);
			}
			//com.fs.starfarer.api.impl.combat.RealityDisruptorEffect
			//com.fs.starfarer.api.impl.combat.ShockRepeaterOnFireEffect
			if (targetJitterLevel > 0) {
				float EMPdamage = 50f,ENRdamage = 100f;
				if(isMano){
					EMPdamage = 75f;
					ENRdamage = 10f;
				}
				//target.setJitterUnder(KEY_TARGET, JITTER_UNDER_COLOR, targetJitterLevel, 5, 0f, 15f);
				targetData.target.setJitter(KEY_TARGET, MainFadeColor2, targetJitterLevel, 3, 0f, 5f);
				ECM_DecShock_timer1.advance(
						Global.getCombatEngine().getElapsedInLastFrame() * targetData.currEcmDamageMult);
				ECM_DecShock_timer2.advance(
						Global.getCombatEngine().getElapsedInLastFrame() * targetData.currEcmDamageMult);
				ECM_DecShock_timer3.advance(
						Global.getCombatEngine().getElapsedInLastFrame() * targetData.currEcmDamageMult);

				Vector2f start = MathUtils.getRandomPointInCircle(targetData.target.getLocation(),targetData.target.getCollisionRadius()/1.7f);
				Vector2f end = MathUtils.getRandomPointInCircle(targetData.target.getLocation(),targetData.target.getCollisionRadius());

				EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
				params.segmentLengthMult = 8f;
				params.zigZagReductionFactor = 0.12f;
				params.fadeOutDist = 70f;
				params.minFadeOutMult = 8f;
				params.flickerRateMult = 0.25f;
				params.movementDurOverride = Math.max(0.1f, MathUtils.getDistance(start, end) / 10000f);
				params.glowSizeMult = 1.75f;
				params.brightSpotFullFraction = 0.25f;
				//————————————————————————————
				if (ECM_DecShock_timer1.intervalElapsed()) {//————————————————————————————

					EmpArcEntityAPI arc = engine.spawnEmpArcVisual(
							new Vector2f(end), null,
							new Vector2f(start), null,
							60f, // thickness
							MainFadeColor2,
							new Color(255, 255, 255, 255),
							params
					);
					arc.setCoreWidthOverride(40f);

					arc.setRenderGlowAtStart(false);
					arc.setFadedOutAtStart(true);
					arc.setSingleFlickerMode(true);

				}//————————————————————————————
				//com.fs.starfarer.api.impl.hullmods.ECMPackage
				if(isMano) {
					if (ECM_DecShock_timer2.intervalElapsed()) {//————————————————————————————
						EmpArcEntityAPI arc2 = engine.spawnEmpArcPierceShields(
								ship, start,
								targetData.target, targetData.target,
								DamageType.ENERGY,
								ENRdamage,
								EMPdamage, // emp
								100000f, // max range
								"shock_repeater_emp_impact",
								60f, // thickness
								MainFadeColor2,
								new Color(255, 255, 255, 255)
						);
						arc2.setCoreWidthOverride(40f);
						arc2.setSingleFlickerMode();
						arc2.setWarping(1f);
					}
				}else {
					if (ECM_DecShock_timer3.intervalElapsed()) {//————————————————————————————
						EmpArcEntityAPI arc3 = engine.spawnEmpArcPierceShields(
								ship, start,
								targetData.target, targetData.target,
								DamageType.ENERGY,
								ENRdamage,
								EMPdamage, // emp
								100000f, // max range
								"shock_repeater_emp_impact",
								60f, // thickness
								MainFadeColor2,
								new Color(255, 255, 255, 255)
						);
						arc3.setCoreWidthOverride(40f);
						arc3.setSingleFlickerMode();
						arc3.setWarping(1f);
					}
				}
			}
		}
// ————————————————————————————————————————
		if (state == State.COOLDOWN) { // ————————————————————————————————————————
			stats.getAcceleration().unmodify(id);
			stats.getDeceleration().unmodify(id);
			stats.getMaxTurnRate().unmodify(id);
			stats.getTurnAcceleration().unmodify(id);
			stats.getMaxSpeed().unmodify(id);

			stats.getHullDamageTakenMult().unmodify(id);
			stats.getArmorDamageTakenMult().unmodify(id);
			stats.getEmpDamageTakenMult().unmodify(id);

			stats.getCombatEngineRepairTimeMult().unmodifyMult(id);
			stats.getCombatWeaponRepairTimeMult().unmodifyMult(id);
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		CombatEngineAPI engine = Global.getCombatEngine();
		hasCrack = false;
		isMano = false;
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getMaxSpeed().unmodify(id);
	}

	public static float getMaxRange(ShipAPI ship) {
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE);
	}

	protected ShipAPI findTarget(ShipAPI ship) {
		float range = getMaxRange(ship);
		boolean player = ship == Global.getCombatEngine().getPlayerShip();
		ShipAPI target = ship.getShipTarget();

		if (ship.getShipAI() != null && ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.TARGET_FOR_SHIP_SYSTEM)){
			target = (ShipAPI) ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.TARGET_FOR_SHIP_SYSTEM);
			if (target != null
					&& target.getOriginalOwner() == ship.getOriginalOwner()) target = null;
		}

		if (target != null) {
			float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
			float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
			if (dist > range + radSum) target = null;
		} else {
			if (target == null || target.getOwner() == ship.getOwner()) {
				if (player) {
					target = Misc.findClosestShipEnemyOf(ship, ship.getMouseTarget(), ShipAPI.HullSize.FRIGATE, range, true);
				} else {
					Object test = ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET);
					if (test instanceof ShipAPI) {
						target = (ShipAPI) test;
						float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
						float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
						if (dist > range + radSum || target.isFighter()) target = null;
						if (target != null && target.getOriginalOwner() == ship.getOriginalOwner()) target = null;
					}
				}
			}
		}

		if (target != null && target.isFighter()) target = null;
		if (target == null) {
			target = Misc.findClosestShipEnemyOf(ship, ship.getLocation(), ShipAPI.HullSize.FRIGATE, range, true);
		}

		return target;
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo()) return null;
		if (system.getState() != ShipSystemAPI.SystemState.IDLE) return null;

		ShipAPI target = findTarget(ship);

		if (target != null && target != ship && target.getHullSpec()!=null && target.getHullSpec().getMinCrew() != 0) {
			return "TARGET READY: MANO";
		}
		if (target != null && target != ship && target.getHullSpec()!=null
				&& target.getHullSpec().getTags().contains("Gamma_dbp")   // Gamma isn't Auto!
				&& target.getHullSpec().getTags().contains("Gamma_bp")		// Gamma is Gamma!
				&& target.getHullSpec().getTags().contains("show_Gamma_Hullmods")) {
			return "TARGET READY: GAMMA, GaMMa, GamMA GamMA GAM...";
		}
		if (target != null && target != ship) {
			return "TARGET READY: AUTO";
		}
		if ((target == null) && ship.getShipTarget() != null) {
			return "OUT OF RANGE";
		}
		return "NO TARGET";
	}
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		//if (true) return true;
		ShipAPI target = findTarget(ship);
		return target != null && target != ship;
	}
}
