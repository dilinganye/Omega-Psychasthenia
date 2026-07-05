package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.utils.iirt_omega.I18nUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class IIRT_Omega_PupalSwarm extends BaseShipSystemScript {

	private WeaponSlotAPI W01,W02,W03,S01,S02,S03,SSF;
	private int fireamount = 1;
	private final IntervalUtil timer = new IntervalUtil(0.5f, 2f);
	public static Object KEY_SHIP = new Object();
	private boolean hasCrack = false, runOnce = false, spawnOnce = false;

	// 记录每艘船在本次系统激活期间生成的随机位置
	private static final Map<ShipAPI, List<Vector2f>> SWARM_POINTS = new HashMap<ShipAPI, List<Vector2f>>();
	// 记录每艘船在 ACTIVE 阶段生成的 swarm 次数
	private static final Map<ShipAPI, Integer> SWARM_COUNTS = new HashMap<ShipAPI, Integer>();

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		//com.fs.starfarer.api.impl.combat.dweller.RiftLightningEffect
		//com.fs.starfarer.api.impl.combat.RiftCascadeEffect
		//com.fs.starfarer.api.impl.combat.threat.DisplacerGlowScript
		//com.fs.starfarer.api.impl.combat.RiftCascadeEffect
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine.isPaused() || engine == null) return;
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null) return; if (!ship.isAlive()) return;

		Color RIFT_LIGHTNING_COLOR = new Color(216, 158, 255, 145);
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
			if (!runOnce) {
				int active_I = 5;
				if(hasCrack){
					active_I = 8;
				}
				// 生成 5 个围绕舰船的随机位置，并记录在表中
				List<Vector2f> points = new java.util.ArrayList<Vector2f>();
				for (int i = 0; i < active_I; i++) {
					Vector2f p = MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius() * 4f);
					points.add(p);
				}
				SWARM_POINTS.put(ship, points);

				// 1~5 之间随机一个数字，决定本次要触发几次 EMP，并记录下来用于 OUT 阶段
				int swarmNumber = MathUtils.getRandomNumberInRange(1, active_I);
				SWARM_COUNTS.put(ship, swarmNumber);

				if(!hasCrack) {
					for (int i = 0; i < swarmNumber && i < points.size(); i++) {
						Vector2f pStart = MathUtils.getRandomPointOnCircumference(ship.getLocation(),
								ship.getCollisionRadius() * 1.275f);
						Vector2f ploc = points.get(i);
						spawnEmp(
								ship,
								pStart,
								ploc,
								50,
								RIFT_LIGHTNING_COLOR
						);
					}
				}

				runOnce = true;
			}

		}
// ————————————————————————————————————————
		if (state == State.OUT) { // ————————————————————————————————————————
			ship.setJitterUnder(ship, new Color(100,60,255,174), Math.min((effectLevel + 0.5f), 1f), 2, 0f, 10f);


			if(!hasCrack){
				ship.fadeToColor(KEY_SHIP, new Color(238, 18, 18, 255), 0.1f, 0.1f, effectLevel);
				ship.setJitterUnder(KEY_SHIP, new Color(99, 18, 238, 255), effectLevel, 15, 0f, 15f);
			}else{
				timer.advance(Global.getCombatEngine().getElapsedInLastFrame());
			}

			// 使用在 ACTIVE 中记录的 SWARM_POINTS 做更多处理
			List<Vector2f> points = SWARM_POINTS.get(ship);
			Integer swarmNumber = SWARM_COUNTS.get(ship);
			if (points != null && swarmNumber != null && hasCrack) {  //玻尔兹曼
				if (timer.intervalElapsed()) {//————————————————————————————
					for (int i = 0; i < swarmNumber && i < points.size(); i++) {
						Vector2f ploc = MathUtils.getRandomPointInCircle(ship.getLocation(),ship.getCollisionRadius()*3f);
						int Choose = MathUtils.getRandomNumberInRange(1, 4);
						ship.fadeToColor(KEY_SHIP, new Color(238, 18, 18, 255), 0.1f, 0.1f, 0.5f);
						ship.setJitterUnder(KEY_SHIP, new Color(99, 18, 238, 255), 0.5f, 15, 0f, 15f);

						I18nUtil.easyRippleOut(ploc, new Vector2f(0, 0),
								80f,
								90f,
								90f,
								10f,
								10f);

						String specId = "IIRT_Omega_Pupal_shock_System_wing";
						Color specColor = new Color(60, 255, 255, 219);
						if (Choose == 2) {
							specId = "IIRT_Omega_Pupal_missile_System_wing";
							specColor = new Color(255, 29, 29, 240);
						}
						if (Choose == 3) {
							specId = "IIRT_Omega_Pupal_attack_System_wing";
							specColor = new Color(99, 60, 255, 199);
						}
						if (Choose == 4) {
							specId = "IIRT_Omega_Pupal_shieldbreaker_System_wing";
							specColor = new Color(255, 253, 151, 221);
						}
						ShipAPI w = engine.getFleetManager(ship.getOriginalOwner()).spawnShipOrWing(
								specId, ploc, ship.getFacing(), 0);
						w.setJitterUnder(w, specColor, Math.min((effectLevel + 0.5f), 1f), 2, 0f, 10f);
						w.fadeToColor(w, new Color(17, 0, 0, 255), 0.1f, 0.1f, effectLevel);
						spawnMine(ship, ploc, MathUtils.getRandomNumberInRange(1f,2.2f));
					}
				}
			} else if (points != null && swarmNumber != null && !spawnOnce) {  //侦察小兵
					for (int i = 0; i < swarmNumber && i < points.size(); i++) {
						Vector2f ploc = points.get(i);
						int Choose = MathUtils.getRandomNumberInRange(1, 4);

						I18nUtil.easyRippleOut(ploc, new Vector2f(0, 0),
								80f,
								90f,
								90f,
								10f,
								10f);
						String specId = "IIRT_Omega_Pupal_shock_System_wing";
						Color specColor = new Color(60, 255, 255, 219);
						if (Choose == 2) {
							specId = "IIRT_Omega_Pupal_missile_System_wing";
							specColor = new Color(255, 29, 29, 240);
						}
						if (Choose == 3) {
							specId = "IIRT_Omega_Pupal_attack_System_wing";
							specColor = new Color(99, 60, 255, 199);
						}
						if (Choose == 4) {
							specId = "IIRT_Omega_Pupal_shieldbreaker_System_wing";
							specColor = new Color(255, 253, 151, 221);
						}
						ShipAPI w = engine.getFleetManager(ship.getOriginalOwner()).spawnShipOrWing(
								specId, ploc, ship.getFacing(), 0);
						w.setJitterUnder(w, specColor, Math.min((effectLevel + 0.5f), 1f), 2, 0f, 10f);
						w.fadeToColor(w, new Color(17, 0, 0, 255), 0.1f, 0.1f, effectLevel);
					}
				spawnOnce = true;
			}
			// 直到 OUT 阶段处理完毕之前不清空表格
			if (effectLevel <= 0f) {
				// OUT 阶段即将结束，此时再清空对应舰船的数据
				SWARM_POINTS.remove(ship);
				SWARM_COUNTS.remove(ship);
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
		spawnOnce = false;
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
	public void spawnMine(ShipAPI source, Vector2f mineLoc,float sizeMult) {
		CombatEngineAPI engine = Global.getCombatEngine();

		MissileAPI mine = (MissileAPI) engine.spawnProjectile(source, null,
				"riftcascade_minelayer",
				mineLoc,
				(float) Math.random() * 360f, null);

		// "spawned" does not include this mine
		mine.setCustomData(RiftCascadeMineExplosion.SIZE_MULT_KEY, sizeMult);

		if (source != null) {
			Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(
					source, WeaponAPI.WeaponType.ENERGY, false, mine.getDamage());
		}

		mine.getDamage().getModifier().modifyMult("mine_sizeMult", sizeMult);


		float fadeInTime = 0.05f;
		mine.getVelocity().scale(0);
		mine.fadeOutThenIn(fadeInTime);

		//Global.getCombatEngine().addPlugin(createMissileJitterPlugin(mine, fadeInTime));

		//mine.setFlightTime((float) Math.random());
		float liveTime = 0f;
		//liveTime = 0.01f;
		mine.setFlightTime(mine.getMaxFlightTime() - liveTime);
		mine.addDamagedAlready(source);
		mine.setNoMineFFConcerns(false);
		mine.explode();
	}
}
