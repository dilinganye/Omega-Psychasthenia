package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.combat.RiftCascadeEffect;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.shard.IIRT_TranShardSpawner;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class IIRT_Omega_WingMan_Black extends BaseShipSystemScript {
	private final IntervalUtil ShadeTimer = new IntervalUtil(0.3f, 1.0f);

	protected static final float SPAWN_RANGE = 2f;
	protected static final String DATA_KEY = "iirt_omega_wingman_black_state";
	protected static final String JITTER_KEY = "iirt_omega_wingman_jitter";

	private static class CloneState {
		java.util.List<String> activeClones = new ArrayList<>();
		float cloneTimer = 0f;
	}

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine == null || engine.isPaused()) return;

		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null || !ship.isAlive()) return;

		boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI)stats.getEntity();
			player = ship == Global.getCombatEngine().getPlayerShip();
			id = id + "_" + ship.getId();
		} else {
			return;
		}

		CloneState cloneState = getCloneState(engine, ship);
		cloneState.cloneTimer -= Global.getCombatEngine().getElapsedInLastFrame();

		String id2 = ship.getId()+"wingman_half";
		if (state == State.IN) {
			handleInPhase(ship, cloneState, engine, effectLevel);
		} else if (state == State.ACTIVE) {
			ShadeTimer.setElapsed(Global.getCombatEngine().getElapsedInLastFrame());
			handleActivePhase(ship, cloneState, engine);

			stats.getMaxSpeed().modifyMult(id, 1.25f);
			stats.getTurnAcceleration().modifyMult(id, 3);
			stats.getMaxTurnRate().modifyMult(id, 2);
		} else if (state == State.OUT) {
			handleOutPhase(ship,cloneState, engine, effectLevel);
			ship.setHoldFire(true);
			float shipTimeMult = 1f + 0.5f * effectLevel;
			stats.getTimeMult().modifyMult(id, shipTimeMult);
			if (player) {
				Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);
			} else {
				Global.getCombatEngine().getTimeMult().unmodify(id);
			}
			ship.fadeToColor(JITTER_KEY, new Color(0, 0, 0, 120), 0.05f, 0.05f, 1f);
			ship.setJitterUnder(JITTER_KEY, new Color(120, 80, 200, 180), 0.8f, 12, 0f, 18f);
		} else if (ship.getHitpoints()<=200f) {
			ship.setHoldFire(false);
			cleanupDeadClones(cloneState, engine);

			stats.getMaxSpeed().unmodifyMult(id);
			stats.getTurnAcceleration().unmodifyMult(id);
			stats.getTimeMult().unmodifyMult(id);
			stats.getMaxSpeed().unmodifyMult(id2);
			stats.getTurnAcceleration().unmodifyMult(id2);
			stats.getTimeMult().unmodifyMult(id2);
			if (player) {
				Global.getCombatEngine().getTimeMult().unmodify(id);
			}
			stats.getMaxTurnRate().unmodifyMult(id);
			ship.fadeToColor(JITTER_KEY, new Color(0, 0, 0, 0), 0.05f, 0.05f, 1f);
			ship.setJitterUnder(JITTER_KEY, new Color(120, 80, 200, 0), 0.8f, 12, 0f, 18f);

			handleOutPhase(ship,cloneState, engine, effectLevel); //因为某些原因中止系统
			ship.setHoldFire(false);
		} else { //state == State.COOLDOWN
			ship.setHoldFire(false);
			cleanupDeadClones(cloneState, engine);

			stats.getMaxSpeed().unmodifyMult(id);
			stats.getTurnAcceleration().unmodifyMult(id);
			stats.getTimeMult().unmodifyMult(id);
			stats.getMaxSpeed().unmodifyMult(id2);
			stats.getTurnAcceleration().unmodifyMult(id2);
			stats.getTimeMult().unmodifyMult(id2);
			if (player) {
				Global.getCombatEngine().getTimeMult().unmodify(id);
			}
			stats.getMaxTurnRate().unmodifyMult(id);
			ship.fadeToColor(JITTER_KEY, new Color(0, 0, 0, 0), 0.05f, 0.05f, 1f);
			ship.setJitterUnder(JITTER_KEY, new Color(120, 80, 200, 0), 0.8f, 12, 0f, 18f);

			handleOutPhase(ship,cloneState, engine, effectLevel); //因为某些原因中止系统
			ship.setHoldFire(false);
		}
	}

	private void handleInPhase(ShipAPI ship, CloneState cloneState, CombatEngineAPI engine, float effectLevel) {
		// 如果没有分身或分身死了，召唤一个新的
		if (cloneState.activeClones.isEmpty() || !hasActiveClone(cloneState, engine)) {
			if (cloneState.cloneTimer <= 0f) {
				spawnClone(ship, cloneState, engine);
				cloneState.cloneTimer = 0.5f;
			}
		}

		applyCloneVisuals(cloneState, engine);
	}

	private void handleActivePhase(ShipAPI ship, CloneState cloneState, CombatEngineAPI engine) {
		// 持续保持分身的视觉效果和AI
		for (String cloneId : new ArrayList<>(cloneState.activeClones)) {
			ShipAPI clone = findShipById(engine, cloneId);
			if (clone == null || !clone.isAlive()) {
				cloneState.activeClones.remove(cloneId);
				continue;
			}

			// 保持黑色轮廓效果
			clone.getSpriteAPI().setAlphaMult(0f);
			clone.fadeToColor(JITTER_KEY, new Color(0, 0, 0, 255), 0.05f, 0.05f, 1f);
			clone.setJitterUnder(JITTER_KEY, new Color(120, 80, 200, 180), 0.8f, 12, 0f, 18f);
			clone.isDoNotRenderWeapons();

			// 强制分身靠近主舰附近
			if (Misc.getDistance(ship.getLocation(), clone.getLocation()) > SPAWN_RANGE * ship.getCollisionRadius() * 2.5f) {
				Vector2f direction = new Vector2f();
				Vector2f.sub(ship.getLocation(), clone.getLocation(), direction);
				direction.normalise();
				direction.scale(50f);
				clone.getVelocity().x += direction.x * 0.1f;
				clone.getVelocity().y += direction.y * 0.1f;
			}
			if(ShadeTimer.intervalElapsed()){
				for (int i = 0; i < MathUtils.getRandomNumberInRange(2,4); i++) {
					engine.addNegativeNebulaParticle(MathUtils.getRandomPointInCircle(clone.getLocation(), clone.getCollisionRadius() * 0.5f),
							clone.getVelocity(),
							clone.getCollisionRadius() * 0.75f,
							2f,
							0.65f,
							0.5f,
							MathUtils.getRandomNumberInRange(3f, 5f),
							new Color(224, 7, 72, 75)
					);
					engine.addSwirlyNebulaParticle(MathUtils.getRandomPointInCircle(clone.getLocation(), clone.getCollisionRadius() * 0.5f),
							clone.getVelocity(),
							clone.getCollisionRadius() * 0.95f,
							1.53f,
							0.35f,
							0.5f,
							MathUtils.getRandomNumberInRange(4f, 7f),
							new Color(134, 80, 200, 182),
							true
					);
				}
			}
		}
	}

	private void handleOutPhase(ShipAPI ship, CloneState cloneState, CombatEngineAPI engine, float effectLevel) {
		// OUT阶段逐渐移除分身或保留但不再召唤
		for (String cloneId : new ArrayList<>(cloneState.activeClones)) {
			ShipAPI clone = findShipById(engine, cloneId);
			if (clone != null && clone.isAlive()) {
				// 淡化效果
				clone.fadeToColor(JITTER_KEY, new Color(0, 0, 0, 255), 0.1f, 0.1f, effectLevel * 0.5f);
				clone.setJitterUnder(JITTER_KEY, new Color(255, 39, 39, 180), 0.8f, 20, 7f, 80f);
				clone.getSpriteAPI().setAlphaMult(0f);
				float sizeMult = (ship.getCollisionRadius() / 55f) * 1.75f;
				spawnMine(clone, clone.getLocation(), sizeMult);

				for (int i = 0; i < 3; i++) {
					spawnFluxLinkArc(clone,ship,new Color(255, 39, 39, 180), 1.5f, 0.8f);
				}

				for (int i = 0; i < 3; i++) {
					spawnFluxLinkArc(clone,ship,new Color(208, 52, 182, 220), 4f, 0.5f);
				}
				for (int i = 0; i < 2; i++) {
					spawnFluxLinkArc(clone,ship,new Color(122, 52, 208, 255), 7f, 0.3f);
				}

				clone.setHulk(true);
				engine.getFleetManager(clone.getOriginalOwner()).removeDeployed(clone,false);
				engine.removeEntity(clone);
				cloneState.activeClones.remove(cloneId);
			}
		}
	}

	private void applyCloneVisuals(CloneState cloneState, CombatEngineAPI engine) {
		for (String cloneId : cloneState.activeClones) {
			ShipAPI clone = findShipById(engine, cloneId);
			if (clone == null || !clone.isAlive()) continue;

			// 纯黑轮廓效果
			clone.fadeToColor(JITTER_KEY, new Color(0, 0, 0, 200), 0.05f, 0.05f, 1f);
			clone.getSpriteAPI().setAlphaMult(0f);
			// 闪烁效果以显示分身存在
			clone.setJitterUnder(JITTER_KEY, new Color(140, 100, 220, 70), 0.75f, 14, 0f, 20f);
		}
	}

	private void spawnClone(ShipAPI ship, CloneState cloneState, CombatEngineAPI engine) {
		if (!cloneState.activeClones.isEmpty()) return; // 最多1个分身

		try {
			Vector2f offset = MathUtils.getRandomPointOnCircumference(ship.getLocation(),SPAWN_RANGE * ship.getCollisionRadius());

			CombatFleetManagerAPI fleetManager = engine.getFleetManager(ship.getOriginalOwner());
			boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
			fleetManager.setSuppressDeploymentMessages(true);
			// 生涯装配经常使用只存在于运行时的临时 variant id；复制当前实际装配，
			// 避免再次按 getSpecId() 查询全局 variant 表。该路径也适用于共用此系统的其他舰船。
			ShipAPI clone = spawnCloneWithCurrentVariant(ship, fleetManager, offset);
			if (clone == null) throw new IllegalStateException("Unable to create macro-virus clone");
				// 为clone设置属性
			//com.fs.starfarer.api.impl.combat.dweller.HumanShipShroudCreator
				clone.getSystem().deactivate();
				clone.setShipSystemDisabled(true);
				clone.setSpawnDebris(false);
				EnumSet<WeaponAPI.WeaponType> allowedTypes = EnumSet.of(
						WeaponAPI.WeaponType.ENERGY,
						WeaponAPI.WeaponType.BALLISTIC,
						WeaponAPI.WeaponType.MISSILE,
						WeaponAPI.WeaponType.BUILT_IN
				);
			//Global.getCombatEngine().addPlugin();

				clone.setWeaponGlow(2,new Color(177, 61, 255, 166),allowedTypes);
				clone.getMutableStats().getHullBonus().modifyMult(ship.getId()+"wingman_half", 0.5f);
				clone.getMutableStats().getArmorBonus().modifyMult(ship.getId()+"wingman_half", 0.5f);
				clone.getMutableStats().getCombatWeaponRepairTimeMult().modifyMult(ship.getId()+"wingman_half", 70f);
				clone.getMutableStats().getWeaponHealthBonus().modifyMult(ship.getId()+"wingman_half", 999999f);
				clone.getShield().setType(ShieldAPI.ShieldType.NONE);
				clone.getMutableStats().getTimeMult().modifyMult(ship.getId()+"wingman_half", 2f);
				for (int i = 0; i < 10; i++) {
					engine.addNegativeNebulaParticle(MathUtils.getRandomPointInCircle(clone.getLocation(), clone.getCollisionRadius() * 0.5f),
							clone.getVelocity(),
							clone.getCollisionRadius() * 0.75f,
							2f,
							0.65f,
							0.5f,
							MathUtils.getRandomNumberInRange(3f, 5f),
							new Color(224, 7, 7, 181)
					);
					engine.addSwirlyNebulaParticle(MathUtils.getRandomPointInCircle(clone.getLocation(), clone.getCollisionRadius() * 0.5f),
							clone.getVelocity(),
							clone.getCollisionRadius() * 0.75f,
							0.92f,
							0.65f,
							0.5f,
							MathUtils.getRandomNumberInRange(4f, 7f),
							new Color(134, 80, 200, 182),
							true
					);
				}


				Color color = new Color(93, 7, 187, 180);
				ship.getEngineController().fadeToOtherColor(this, color, null, 1f, 0.4f);
				//com.fs.starfarer.api.impl.hullmods.DoNotBackOff

				ShipwideAIFlags flags = clone.getAIFlags();
				if (flags != null) {
					flags.setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF, 0.98f);
				}

				cloneState.activeClones.add(clone.getId());
			fleetManager.setSuppressDeploymentMessages(wasSuppressed);
		} catch (Exception e) {
			// 失败时创建视觉分身
			cloneState.activeClones.add(ship.getId() + "_clone_visual");
		}
	}
	private ShipAPI spawnCloneWithCurrentVariant(ShipAPI source, CombatFleetManagerAPI fleetManager,
											Vector2f location) {
		if (source == null || fleetManager == null) return null;
		try {
			ShipVariantAPI current = source.getVariant();
			if (current != null) {
				FleetMemberAPI member = Global.getFactory().createFleetMember(
						FleetMemberType.SHIP, current.clone());
				member.setOwner(source.getOriginalOwner());
				return fleetManager.spawnFleetMember(member, location, source.getFacing(), 1f);
			}
		} catch (Throwable ex) {
			Global.getLogger(getClass()).warn("Unable to clone runtime ship variant; using registered fallback", ex);
		}
		ShipVariantAPI current = source.getVariant();
		String fallbackVariant = current == null ? null : current.getOriginalVariant();
		if (fallbackVariant == null || fallbackVariant.trim().isEmpty()) {
			fallbackVariant = current == null ? null : current.getHullVariantId();
		}
		return fallbackVariant == null ? null : fleetManager.spawnShipOrWing(
				fallbackVariant, location, source.getFacing(), 1f);
	}
	//ONDOONDOONDOONDOONDOONDOONDO
	/*
	protected EveryFrameCombatPlugin createShipFadeOutPlugin(final ShipAPI ship, final float fadeOutTime, final java.util.List<IIRT_TranShardSpawner.ShardFadeInPlugin> shards) {
		return new BaseEveryFrameCombatPlugin() {
			float elapsed = 0f;
			IntervalUtil interval = new IntervalUtil(0.075f, 0.125f);

			@Override
			public void advance(float amount, List<InputEventAPI> events) {
				if (Global.getCombatEngine().isPaused()) return;

				if (elapsed > fadeOutTime) {
					ship.setHitpoints(0f);
					Global.getCombatEngine().removeEntity(ship);
					ship.setAlphaMult(0f);
					Global.getCombatEngine().removePlugin(this);
				}
			}
		};
	}

	 */
	private Vector2f randomOffsetNear(float range) {
		float angle = (float) (Math.random() * Math.PI * 2f);
		float dist = (float) Math.random() * range;
		return new Vector2f((float) Math.cos(angle) * dist, (float) Math.sin(angle) * dist);
	}

	private boolean hasActiveClone(CloneState cloneState, CombatEngineAPI engine) {
		for (String cloneId : cloneState.activeClones) {
			ShipAPI clone = findShipById(engine, cloneId);
			if (clone != null && clone.isAlive()) return true;
		}
		return false;
	}

	private void cleanupDeadClones(CloneState cloneState, CombatEngineAPI engine) {
		for (String cloneId : new ArrayList<>(cloneState.activeClones)) {
			ShipAPI clone = findShipById(engine, cloneId);
			if (clone == null || !clone.isAlive()) {
				cloneState.activeClones.remove(cloneId);
			}
		}
	}

	private ShipAPI findShipById(CombatEngineAPI engine, String id) {
		if (id == null || engine == null) return null;
		for (ShipAPI ship : engine.getShips()) {
			if (ship != null && id.equals(ship.getId())) return ship;
		}
		return null;
	}

	private CloneState getCloneState(CombatEngineAPI engine, ShipAPI ship) {
		String key = DATA_KEY + "_" + ship.getId();
		CloneState state = (CloneState) engine.getCustomData().get(key);
		if (state == null) {
			state = new CloneState();
			engine.getCustomData().put(key, state);
		}
		return state;
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI) stats.getEntity();
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine != null && ship != null) {
			engine.getCustomData().remove(DATA_KEY + "_" + ship.getId());
			stats.getMaxSpeed().unmodifyMult(id);
			stats.getTurnAcceleration().unmodifyMult(id);
			stats.getMaxTurnRate().unmodifyMult(id);
		}
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

		for (int i = 0; i < MathUtils.getRandomNumberInRange(8,12); i++) {
			engine.addNegativeNebulaParticle(MathUtils.getRandomPointInCircle(source.getLocation(), source.getCollisionRadius()),
					source.getVelocity(),
					source.getCollisionRadius() * 0.75f,
					2f,
					0.65f,
					0.5f,
					MathUtils.getRandomNumberInRange(1f, 3f),
					new Color(224, 7, 72, 75)
			);
			engine.addSwirlyNebulaParticle(MathUtils.getRandomPointInCircle(source.getLocation(), source.getCollisionRadius()),
					source.getVelocity(),
					source.getCollisionRadius() * 1.25f,
					2.7f,
					0.35f,
					0.5f,
					MathUtils.getRandomNumberInRange(2f, 3f),
					new Color(134, 80, 200, 182),
					true
			);
		}
	}private void spawnFluxLinkArc(ShipAPI from, ShipAPI to, Color color, float thickness, float intensityMult) {
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
}







