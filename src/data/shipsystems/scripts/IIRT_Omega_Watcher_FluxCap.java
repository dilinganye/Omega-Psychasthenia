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
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.ui.W;
import data.scripts.util.I18nUtil;
import data.scripts.util.IIRT_Omega_Color;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IIRT_Omega_Watcher_FluxCap extends BaseShipSystemScript {
	// Still need a follower AI for this ship
	protected static float RANGE = 1000f;
	protected static final float ABSORB_TIME = 2.5f;
	protected static final float OUT_DISPERSE_THRESHOLD = 0.85f;
	protected static final float ABSORB_COLOR_ALPHA = 185f;
	protected static final Color ABSORB_ARC_COLOR = new Color(160, 120, 255, 220);
	protected static final Color DISPERSE_ARC_COLOR = new Color(255, 160, 245, 235);
	protected static final Color ARC_CORE_COLOR = new Color(255, 255, 255, 255);
	protected static final String DATA_KEY = "iirt_omega_watcher_fluxcap_state";
	protected static final String FORCE_DISPERSE_KEY = "iirt_omega_watcher_fluxcap_force_disperse";
	private WeaponSlotAPI ss1; private WeaponAPI LINE,RING,TAIL;
	private static final IntervalUtil timer = new IntervalUtil(0.12f, 0.3f);
	private static final IntervalUtil timer_Find = new IntervalUtil(1f, 2f);
	float Ring_R_Size = 100f, Line_Long = 80f;
	public static Object KEY_SHIP = new Object();
	private boolean hasCrack = false, runOnce = false;

	private static class FluxCapState {
		boolean disperseMode = false;
		final Map<String, Float> targetDrain = new HashMap<>();
	}
	//com.fs.starfarer.api.impl.combat.dweller.TenebrousExpulsionSystemAI
	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		//com.fs.starfarer.api.impl.combat.dweller.RiftLightningEffect
		//com.fs.starfarer.api.impl.combat.RiftCascadeEffect
		//com.fs.starfarer.api.impl.combat.threat.DisplacerGlowScript
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine == null || engine.isPaused()) return;
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null) return; if (!ship.isAlive()) return;
		if (!runOnce) {
			for (WeaponAPI w : ship.getAllWeapons()) {
				switch (w.getSlot().getId()) {
					case "WS0004":
						LINE = w;
						break;
					case "WS0005":
						RING = w;
						break;
					case "WS0007":
						TAIL = w;
						break;
				}
			}
			/*for (WeaponSlotAPI s : ship.getHullSpec().getAllWeaponSlotsCopy()) {
				switch (s.getId()) {
				}
			}*/
			runOnce = true;
		}
		if (LINE == null || RING == null || TAIL == null) return;
		timer_Find.advance(engine.getElapsedInLastFrame());
		FluxCapState fluxState = getFluxCapState(engine, ship);
		if (timer_Find.intervalElapsed()) {
			initFluxCapState(engine, ship, fluxState);
		}
		boolean forceDisperse = ship.getFluxLevel()>=0.85f;
		if (forceDisperse) {
			fluxState.disperseMode = true;
		} else if (ship.getFluxLevel() < OUT_DISPERSE_THRESHOLD) {
			fluxState.disperseMode = false;
		}
		float amount = Global.getCombatEngine().getElapsedInLastFrame();
		Color RIFT_LIGHTNING_COLOR = new Color(255, 47, 47, 145);
		Color negtive_color = new Color(200,255,200, 34);
		Color backColor = new Color(0, 0, 0, 255);
		Color MainFadeColor = new Color(180, 100, 255, 255);
		Color MainFadeColor2 = new Color(160, 123, 255, 255);
		Color DecFadeColor = new Color(248, 56, 255, 255);
		Vector2f ZERO = new Vector2f(0,0);
		if (ship.getHullSpec().hasTag("Omega_System_Type_II")){
			MainFadeColor = new Color(221, 100, 255, 255);
			MainFadeColor2 = new Color(196, 123, 255, 255);
			DecFadeColor = new Color(255, 56, 209, 255);
			hasCrack = true;
		}

		Color testing = new Color(98, 105, 110, 255);
		Color W1C = LINE.getSprite().getAverageColor();
		Color W2C = RING.getSprite().getAverageColor();
		Color W3C = TAIL.getShip().getFluxTracker().getOverloadColor();
		W1C = new Color(W1C.getRed(), W1C.getGreen(), W1C.getBlue(), 175);
		W2C = new Color(W2C.getRed(), W2C.getGreen(), W2C.getBlue(), 175);
		W3C = new Color(W3C.getRed(), W3C.getGreen(), W3C.getBlue(), 175);
		int T_R = W1C.getRed() + W2C.getRed() + W3C.getRed(),
				T_G = W1C.getGreen() + W2C.getGreen() + W3C.getGreen(),
				T_B = W1C.getBlue() + W2C.getBlue() + W3C.getBlue(),
				T_Max = Math.max(Math.max(T_R,T_G),T_B);
		if(Global.getSector().getCurrentLocation()!=null && Global.getSector().getCurrentLocation().getBackgroundColorShifter()!=null){
			Global.getSector().getCurrentLocation().getBackgroundColorShifter().getBase();
		}
		float rampUp = 0.25f + 0.25f * (float) Math.random(),dur = 1f + (float) Math.random();
		Vector2f Curr_Line_Point = MathUtils.getPoint(LINE.getLocation(),Line_Long,LINE.getCurrAngle());
		Vector2f Curr_Line_Point_B = MathUtils.getPoint(LINE.getLocation(),Line_Long,LINE.getCurrAngle()+180f);
		Vector2f Curr_Ring_Point = MathUtils.getRandomPointOnCircumference(RING.getLocation(),Ring_R_Size);

		if (state == State.IN || state == State.ACTIVE) {
			disperseStoredFlux(ship, fluxState, amount, effectLevel);
			absorbFluxFromNearbyAllies(ship, fluxState, amount,W3C);
		}
// ————————————————————————————————————————_____________________————————————————————————————————————————————

// ————————————————————————————————————————_____________________————————————————————————————————————————————
		if (state == State.IN) {// ————————————————————————————————————————
			timer.advance(amount);
			ship.fadeToColor(KEY_SHIP, backColor, 0.1f, 0.1f, effectLevel);
			ship.setJitterUnder(KEY_SHIP, MainFadeColor, effectLevel, 15, 0f, 15f);

				IN_spawnSwirlyParticle(Curr_Line_Point, ZERO,
						7, 0.1f, W1C);
				IN_spawnSwirlyParticle(Curr_Line_Point_B, ZERO,
						5, 0.2f, W2C);
				IN_spawnSwirlyParticle(Curr_Ring_Point, ship.getVelocity(),
						8, 0.35f, W3C);

			if(hasCrack) {
			}
		}
// ————————————————————————————————————————
		if (state == State.ACTIVE) {// ————————————————————————————————————————

				IN_spawnParticle(Curr_Line_Point, ZERO,
						5, 0.1f, W1C);
				IN_spawnParticle(Curr_Line_Point_B, ZERO,
						2, 0.2f, W2C);
				IN_spawnParticle(Curr_Ring_Point, ZERO,
						7, 0.35f, W3C);

				if (Math.random() < 0.25f) {
					IN_spawnEmp(ship, Curr_Line_Point, MathUtils.getRandomPointInCircle(Curr_Line_Point, Line_Long / 2),
							3f, W1C);
					IN_spawnEmp(ship, Curr_Line_Point_B, MathUtils.getRandomPointInCircle(Curr_Line_Point, Line_Long / 2),
							4f, W2C);
				}



			ship.fadeToColor(KEY_SHIP, MainFadeColor2, 0.1f, 0.1f, effectLevel);
			ship.setJitterUnder(KEY_SHIP, DecFadeColor, effectLevel, 15, 0f, 15f);
			if (hasCrack) {
			}
		}
// ————————————————————————————————————————
		if (state == State.OUT) { // ————————————————————————————————————————
			if (ship.getFluxLevel() >= OUT_DISPERSE_THRESHOLD || forceDisperse) {
				fluxState.disperseMode = true;
			}

			I18nUtil.easyRippleOut(ship.getLocation(), ship.getVelocity(),
						ship.getCollisionRadius(),
						90f,
						ship.getCollisionRadius(),
						ship.getCollisionRadius()/5f,
						17f);
			ship.setJitterUnder(ship, new Color(100,60,255,174), Math.min((effectLevel + 0.5f), 1f), 2, 0f, 10f);
			if (fluxState.disperseMode) {
				disperseStoredFlux(ship, fluxState, amount, effectLevel);
			}

			if (timer.intervalElapsed()) {//————————————————————————————
				if (Math.random() < 0.75f) {
					IN_spawnEmp(ship, Curr_Line_Point, MathUtils.getRandomPointInCircle(Curr_Line_Point, Line_Long),
							2f, W1C);
					IN_spawnEmp(ship, Curr_Line_Point_B, MathUtils.getRandomPointInCircle(Curr_Line_Point, Line_Long),
							2f, W2C);
				}
			}

			if(!hasCrack){
				ship.fadeToColor(KEY_SHIP, new Color(238, 18, 18, 255), 0.1f, 0.1f, effectLevel);
				ship.setJitterUnder(KEY_SHIP, new Color(99, 18, 238, 255), effectLevel, 15, 0f, 15f);
			}
		}

// ————————————————————————————————————————
		if (state == State.COOLDOWN) { // ————————————————————————————————————————

		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		CombatEngineAPI engine = Global.getCombatEngine();
		hasCrack = false;
		runOnce = false;
		if (engine != null && ship != null) {
			engine.getCustomData().remove(DATA_KEY + "_" + ship.getId());
			engine.getCustomData().remove(FORCE_DISPERSE_KEY + "_" + ship.getId());
		}
	}

	private FluxCapState getFluxCapState(CombatEngineAPI engine, ShipAPI ship) {
		String key = DATA_KEY + "_" + ship.getId();
		FluxCapState data = (FluxCapState) engine.getCustomData().get(key);
		if (data == null) {
			data = new FluxCapState();
			engine.getCustomData().put(key, data);
		}
		return data;
	}

	private void initFluxCapState(CombatEngineAPI engine, ShipAPI ship, FluxCapState state) {
		for (ShipAPI other : engine.getShips()) {
			if (other == null || other == ship) continue;
			if (!other.isAlive() || other.isHulk()) continue;
			if (other.getOwner() != ship.getOwner()) continue;
			float dist = Misc.getDistance(ship.getLocation(), other.getLocation());
			if (dist > RANGE) continue;

			float currentFlux = other.getFluxLevel() * other.getMaxFlux();
			float target = currentFlux * 0.5f;
			if (target > 0f) {
				state.targetDrain.put(other.getId(), target);
			}
			spawnFluxLinkArc(other, ship, new Color(160, 120, 255, 220), 3.0f + 2.5f * dist/1000f, 1f);
			spawnFluxBurst(other, new Color(255, 120, 120, 220), 0.85f + dist/1000f * 0.85f, 1f, false);

		}
	}

	private void absorbFluxFromNearbyAllies(ShipAPI ship, FluxCapState state, float amount, Color color) {
		if (state.targetDrain.isEmpty()) return;

		float drainScale = amount / Math.max(0.1f, ABSORB_TIME);
		if (drainScale <= 0f) return;

		CombatEngineAPI engine = Global.getCombatEngine();
		boolean absorbed = false;
		List<String> remove = new ArrayList<>();

		for (Map.Entry<String, Float> entry : state.targetDrain.entrySet()) {
			ShipAPI ally = findShipById(engine, entry.getKey());
			if (ally == null || !ally.isAlive() || ally.isHulk()) {
				remove.add(entry.getKey());
				continue;
			}

			float remaining = entry.getValue();
			if (remaining <= 0f) {
				remove.add(entry.getKey());
				continue;
			}

			float transfer = Math.min(remaining, remaining * drainScale);
			if (transfer <= 0f) continue;

			ally.getFluxTracker().decreaseFlux(transfer);
			ship.getFluxTracker().increaseFlux(transfer, false);
			IN_spawnParticle(MathUtils.getRandomPointInCircle(ally.getLocation(),ally.getCollisionRadius()*1.25f),
					VectorUtils.rotate(new Vector2f(0, transfer * 0.75f),
							(float) Math.random() * 360f
					),
					5,
					0.25f,
					color
			);
			float intensity = Math.min(1f, Math.max(0.2f, transfer / Math.max(1f, ally.getMaxFlux()) * 12f));
			spawnFluxLinkArc(ally, ship, new Color(160, 120, 255, 220), 3.0f + 2.5f * intensity, intensity);
			spawnFluxBurst(ally, new Color(160, 120, 255, 220), 0.85f + intensity * 0.85f, transfer, false);
			spawnFluxBurst(ship, new Color(255, 255, 255, 255), 1.15f + intensity, transfer, true);
			entry.setValue(remaining - transfer);
			absorbed = true;

			if (entry.getValue() <= 1f) {
				remove.add(entry.getKey());
			}
		}

		for (String key : remove) {
			state.targetDrain.remove(key);
		}

		if (absorbed) {
			ship.setJitterUnder(KEY_SHIP, new Color(170, 120, 255, (int) ABSORB_COLOR_ALPHA), 0.65f, 10, 0f, 14f);
		}
	}

	private void disperseStoredFlux(ShipAPI ship, FluxCapState state, float amount, float effectLevel) {
		CombatEngineAPI engine = Global.getCombatEngine();
		float currentFlux = ship.getFluxLevel() * ship.getMaxFlux();
		if (currentFlux <= 0f) return;

		float release = Math.min(currentFlux, amount * ship.getMaxFlux() * 0.9f);
		if (release <= 0f) return;

		ship.getFluxTracker().decreaseFlux(release);
		ship.setJitterUnder(KEY_SHIP, new Color(255, 145, 245, 180), Math.min(effectLevel + 0.25f, 1f), 12, 0f, 18f);

		// 散幅模式下，用电弧把“释放中的能量”视觉化到周围仍在清单中的友军。
		for (Map.Entry<String, Float> entry : state.targetDrain.entrySet()) {
			ShipAPI ally = findShipById(engine, entry.getKey());
			if (ally == null || !ally.isAlive() || ally.isHulk()) continue;
			if (Misc.getDistance(ship.getLocation(), ally.getLocation()) > RANGE) continue;
			spawnFluxLinkArc(ship, ally, new Color(255, 160, 245, 235), 4.25f, 0.95f);
			spawnFluxBurst(ship, new Color(255, 180, 250, 220), 1.25f, release, true);
			spawnFluxBurst(ally, new Color(255, 240, 255, 170), 0.65f, release, false);
		}
	}

	private void spawnFluxLinkArc(ShipAPI from, ShipAPI to, Color color, float thickness, float intensityMult) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine == null || from == null || to == null) return;

		EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
		// 每段电弧的长度倍数：越大越“长条”，越小越碎、越像高压电网。
		params.segmentLengthMult = 2.4f + 2.6f * intensityMult;
		// 折线抖动削减：越大越平稳，越小越狂野。
		params.zigZagReductionFactor = 0.22f + 0.10f * intensityMult;
		// 距离衰减开始点：超过这个距离后会逐渐淡出。
		params.fadeOutDist = 120f + 220f * intensityMult;
		// 最小淡出倍率：控制远端尾巴的透明度衰减速度。
		params.minFadeOutMult = 3.0f + 1.5f * intensityMult;
		// 闪烁频率倍率：越高越像高能电弧。
		params.flickerRateMult = 1.1f + 1.2f * intensityMult;
		// 持续时间覆盖：按两船距离生成更自然的“拉线”感。
		params.movementDurOverride = Math.max(0.05f, Misc.getDistance(from.getLocation(), to.getLocation()) / 900f);
		// 光晕强度：让电弧在中段更亮，避免“看不出来”。
		params.glowSizeMult = 1.65f + 1.55f * intensityMult;
		// 亮点占比：提高白芯覆盖，让电弧更像高能抽放。
		params.brightSpotFullFraction = 0.38f + 0.28f * intensityMult;

		EmpArcEntityAPI arc = (EmpArcEntityAPI) engine.spawnEmpArcVisual(
				from.getLocation(), from,
				to.getLocation(), to,
				thickness,
				color,
				new Color(255, 255, 255, 255),
				params
		);
		arc.setCoreWidthOverride(thickness + 2f + 4f * intensityMult);
		arc.setRenderGlowAtStart(false);
		arc.setFadedOutAtStart(false);
		arc.setSingleFlickerMode(true);
	}

	private void spawnFluxBurst(ShipAPI ship, Color color, float sizeMult, float fluxAmount, boolean bright) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine == null || ship == null) return;

		float intensity = Math.min(1f, Math.max(0.15f, fluxAmount / Math.max(1f, ship.getMaxFlux()) * 10f));
		float size = ship.getCollisionRadius() * 0.10f * sizeMult * (0.65f + intensity);
		float dur = 0.10f + 0.18f * intensity;
		Vector2f loc = new Vector2f(ship.getLocation());
		Vector2f vel = new Vector2f(ship.getVelocity());

		engine.addNebulaParticle(loc, vel, size, 1.6f + 0.8f * intensity, 0.65f, 0.45f, dur, color);
		engine.addSwirlyNebulaParticle(loc, vel, size * 0.75f, 1.3f + 0.7f * intensity, 0.75f, 0.55f, dur, color, bright);
		engine.addHitParticle(loc, vel, size * 0.35f, 1f + 1.25f * intensity, dur * 1.1f, color);
	}

	private ShipAPI findShipById(CombatEngineAPI engine, String id) {
		for (ShipAPI ship : engine.getShips()) {
			if (ship != null && id.equals(ship.getId())) return ship;
		}
		return null;
	}

	public static void IN_spawnEmp(ShipAPI source, Vector2f start, Vector2f end, float thickness, Color color) {
		CombatEngineAPI engine = Global.getCombatEngine();


		EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
		// 每段电弧长度倍数：用于控制闪电是“碎裂”还是“成段”。
		params.segmentLengthMult = 4f;
		// 折线抖动削减：越小越像野蛮的电击，越大越平滑。
		params.zigZagReductionFactor = 0.35f;
		// 淡出距离：超过这个距离后电弧开始明显变虚。
		params.fadeOutDist = 250f;
		// 最小淡出倍率：影响尾部和远端透明度变化。
		params.minFadeOutMult = 3f;
		// 闪烁频率倍率：越大越有“电弧爆鸣感”。
		params.flickerRateMult = 0.7f;

		// 持续时间：按距离缩放，避免近距离过长、远距离过短。
		params.movementDurOverride = Math.max(0.05f, MathUtils.getDistance(start, end) / 100000f);
		EmpArcEntityAPI arc = (EmpArcEntityAPI) engine.spawnEmpArcVisual(
				start, source,
				end, null,
				thickness, // thickness
				color,
				new Color(255, 255, 255, 255),
				params
		);
		arc.setCoreWidthOverride(thickness+2f);

		arc.setRenderGlowAtStart(false);
		arc.setFadedOutAtStart(true);
		arc.setSingleFlickerMode(true);
	}
	public static void IN_spawnDamageEmp(ShipAPI source,ShipAPI target, Vector2f start, DamageType damageType,float damage,float thickness, Color color) {
		CombatEngineAPI engine = Global.getCombatEngine();


		EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
		// 每段长度倍数：更高会让弧线更“拉长”，更低更碎更乱。
		params.segmentLengthMult = 10f;
		// 折线抖动削减：控制雷电的“抖腿”程度。
		params.zigZagReductionFactor = 0.35f;
		params.fadeOutDist = 150f;
		params.minFadeOutMult = 3f;
		// 闪烁频率倍率：越大越像持续放电。
		params.flickerRateMult = 0.7f;

		// 持续时间覆盖：按起点到源舰距离估算，让弧线时长更稳定。
		params.movementDurOverride = Math.max(0.05f, MathUtils.getDistance(start, source.getLocation()) / 100000f);
		// 高亮范围：让击中一侧更亮、更像能量灌注。
		params.glowSizeMult = 2f;
		// 中心亮点占比：影响中央亮白区域的覆盖程度。
		params.brightSpotFullFraction = 0.4f;
		EmpArcEntityAPI arc = (EmpArcEntityAPI) engine.spawnEmpArc(source, start, source, target,
				damageType,
				damage,
				0f,
				10000f,
				"rifttorpedo_fire",
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
	public static void IN_spawnParticle(Vector2f loc, Vector2f vel, float sizeStart, float sizeEndMult, Color color) {
		CombatEngineAPI engine = Global.getCombatEngine();
		timer.advance(Global.getCombatEngine().getElapsedInLastFrame());
		if (timer.intervalElapsed()) {//————————————————————————————
			engine.addNebulaParticle(
					loc,
					vel,
					sizeStart,sizeEndMult,
					0.7f,0.4765f,
					0.4375f,
					color);
		}
	}
	public static void IN_spawnSwirlyParticle(Vector2f loc, Vector2f vel, float sizeStart, float sizeEndMult, Color color) {
		CombatEngineAPI engine = Global.getCombatEngine();
		timer.advance(Global.getCombatEngine().getElapsedInLastFrame());
		if (timer.intervalElapsed()) {//————————————————————————————
			engine.addSwirlyNebulaParticle(
					loc, vel,
					sizeStart*3f,sizeEndMult,
					0.7f,0.4765f,
					0.4375f,
					color,true);
		}
	}

}
