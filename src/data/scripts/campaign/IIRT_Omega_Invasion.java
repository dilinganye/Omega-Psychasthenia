package data.scripts.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.hullmods.shard.IIRT_ColdShardSpawner;
import data.hullmods.shard.PTSD_BaseShard_Util;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import data.scripts.IIRT_Omega_ModPlugin.*;
import data.hullmods.shard.IIRT_ColdShardSpawner;

import static com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3.addCommanderAndOfficers;
import static data.hullmods.shard.PTSD_BaseShard_Util.*;
import static data.scripts.IIRT_Omega_ModPlugin.*;


public class IIRT_Omega_Invasion implements EveryFrameScript {

	private static final Color NOTICE_COLOR = new Color(238, 165, 143, 255);

	public enum STAGE {
		START, COLLECT_DATA, INVADE, REPAIR, FULL_ATTACK, END
	}

	protected STAGE currStage;

	public static final String stage_id = "$IIRT_Omega_Invasion_Stage";
	public static final String baseSystem_id = "$IIRT_Omega_Base_System";
	public static final String baseMarket_id = "$IIRT_Omega_Base_Market";
	public static final String IIRT_Omega_Faction = "Omega_Psychasthenia";
	// these defaults (in days) can be overridden by LunaSettings using LunaWrapper
	/*
	public static int start_stage_time = 65; // days before starting data collection
	public static int collect_data_time = 60; // days of scouting
	public static int invade_time = 30; // days for initial invade phase
	public static int repair_time = 30; // days of core construction
	public static int scout_min_interval = 10; // minimum days between scouts
	public static int scout_max_interval = 25; // maximum days between scouts
	public static int scout_spawn_radius = 300; // radius for scout spawn around target (in hyperspace coords)
	public static int max_guard_fleets = 10; // maximum number of guard fleets to spawn during repair
	public static int final_invasion_max_strength = 200; // placeholder cap for attack FP

	 */

	protected float stageElapsed = 0f;
	protected float elapsed = 0f;
	protected float temp_time = 0f;
	protected StarSystemAPI baseSystem = null;
	protected MarketAPI baseMarket = null;
	protected int inv_interval = (int)(10 + Math.random() * 10);
	// helper to read LunaLib settings via reflection to avoid a hard dependency
	private Integer lunaGetInt(String modID, String fieldID) {
		try {
			Class<?> c = Class.forName("org.magiclib.LunaWrapper");
			Method m = c.getMethod("getInt", String.class, String.class);
			Object res = m.invoke(null, modID, fieldID);
			return (Integer) res;
		} catch (Throwable t) {
			return null;
		}
	}

	public void setStage(STAGE stage) {
		currStage = stage;
		// Global.getSector().getMemoryWithoutUpdate().unset(stage_id);
		Global.getSector().getMemoryWithoutUpdate().set(stage_id, stage);
	}

	@Override
	public boolean isDone() {
		if (Global.getSector() == null) return false;
		ensureStageInitialized();
		return currStage == STAGE.END;
	}

	/**
	 * @return whether advance() should be called while the campaign engine is
	 * paused.
	 */
	@Override
	public boolean runWhilePaused() {
		return false;
	}

	/**
	 * Use SectorAPI.getClock() to convert to campaign days.
	 *
	 * @param amount seconds elapsed during the last frame.
	 */
	@Override
	public void advance(float amount) {
		if (Global.getSector() == null && !OMEGA_PTSD_PREV) return;
		if (Global.getSector() == null || Global.getSector().getClock() == null) return;
		ensureStageInitialized();
		stageElapsed += Global.getSector().getClock().convertToDays(amount);
		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		if (playerFleet == null) return;

		switch (currStage) {
			case START:
				// do nothing at all
				if (stageElapsed >= start_stage_time) {
					stageElapsed = 0;
					setStage(STAGE.COLLECT_DATA);
					// temp_time = (float) (0.2 + 0.5f * Math.random());
					temp_time = 0.1f;
				}
				break;
			case COLLECT_DATA:
				elapsed += Global.getSector().getClock().convertToDays(amount);
				// Global.getLogger(this.getClass()).info("omega scout fleet test spawn
				// elapsed"+elapsed);
				if (elapsed >= temp_time) {
					elapsed = 0f;
					// scout interval scales between configured min/max and slowly increases during repair stage
					//temp_time = (float)(10f + 15f * Math.random());
					float rand = (float)Math.random(); // 生成侦查舰队的间隔时间
					temp_time = (float)(scout_min_interval + (scout_max_interval - scout_min_interval) * rand);
					// temp_time = 0.1f;
					// Global.getLogger(this.getClass()).info("omega scout fleet test spawn");
					WeightedRandomPicker<SectorEntityToken> scoutTargets = new WeightedRandomPicker<>();
					for (StarSystemAPI scoutSystem : Global.getSector().getStarSystems()) {
						if (!scoutSystem.isProcgen()) {
							Global.getLogger(this.getClass()).info("===___===__=Omega_Invasion Scout=__===___===");
							for (SectorEntityToken relay : scoutSystem.getEntitiesWithTag(Tags.COMM_RELAY)) {
								scoutTargets.add(relay);

								if (Global.getSettings().isDevMode()) {
									addCampaignMessageInDev("omega scoutTargets.add  The ID is" + relay.getId() + " & the Name is" + relay.getName(),
											"omega scoutTargets.add  The ID is" + relay.getId() + " & the Name is" + relay.getName());
									Global.getLogger(this.getClass()).info("omega scoutTargets.add  The ID is" + relay.getId() + " & the Name is" + relay.getName());
									playerFleet.addFloatingText("omega scoutTargets.add  The ID is" + relay.getId() + " & the Name is" + relay.getName(), NOTICE_COLOR, 5.0F);
								}
							}
						}
					}
					SectorEntityToken scoutTarget = scoutTargets.pick();
					if (scoutTarget == null) {
						Global.getLogger(this.getClass()).info("===___===__=Omega_Invasion Scout=__===___===");
						Global.getLogger(this.getClass()).info("omega scout fleet test spawn fail to find target");
						if (Global.getSettings().isDevMode()) {
							addCampaignMessageInDev("omega scout fleet test spawn fail to find target",
									"omega scout fleet test spawn fail to find target");
							playerFleet.addFloatingText("omega scout fleet test spawn fail to find target", NOTICE_COLOR, 5.0F);
						}
						break;
					}
					float min_range = 1000000;
					SectorEntityToken scoutStart = null;
					// WeightedRandomPicker<SectorEntityToken> scoutBegins = new
					// WeightedRandomPicker<>();
					for (SectorEntityToken gate : Global.getSector().getEntitiesWithTag(Tags.GATE)) {
						if (gate.getStarSystem().equals(scoutTarget.getStarSystem())) {
							scoutStart = gate;
							break;
						} else {
							if (Misc.getDistance(gate.getLocationInHyperspace(), scoutTarget.getLocationInHyperspace()) < min_range) {
								min_range = Misc.getDistance(gate.getLocationInHyperspace(), scoutTarget.getLocationInHyperspace());
								scoutStart = gate;
							}
						}
					}
					Vector2f scoutStartLoc = Global.getSector().getPlayerFleet().getLocationInHyperspace();
					boolean isInHyperspace = true;
					if (scoutStart != null) {
						if (scoutStart.getStarSystem() != null) {
							scoutStartLoc = scoutStart.getLocation();
							isInHyperspace = false;
						} else {
							scoutStartLoc = scoutStart.getLocationInHyperspace();
							isInHyperspace = true;
						}
					} else {
						// spawn on an arc at some distance from the target (configurable)
						scoutStartLoc = Misc.getPointWithinRadius(scoutTarget.getLocationInHyperspace(), scout_spawn_radius);
					}
					FleetParamsV3 scoutFleetParam = new FleetParamsV3(scoutStartLoc, IIRT_Omega_Faction, 0.25f, FleetTypes.MERC_SCOUT, 16, 0, 0, 0, 0, 0, 0);
					//scoutFleetParam.aiCores = OfficerQuality.AI_GAMMA;
					assignShardBeforeSpawnFleet(scoutFleetParam, BugvariantData, scoutFleetParam.combatPts);
					CampaignFleetAPI scoutFleet = FleetFactoryV3.createFleet(scoutFleetParam);
					addCommanderAndOfficers(scoutFleet, scoutFleetParam, null);
					// scoutFleet.getFleetData().addFleetMember("IIRT_Omega_Kb_Only");

					scoutFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
					scoutFleet.getMemoryWithoutUpdate().set(MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);
					// assignShardBeforeSpawnFleet(scoutFleet, Math.max(10, Math.round(scoutFleet.getFleetPoints())));
					if (isInHyperspace) {
						Global.getSector().getHyperspace().addEntity(scoutFleet);
						scoutFleet.setLocation(scoutStartLoc.x, scoutStartLoc.y);
					} else {
						scoutStart.getStarSystem().addEntity(scoutFleet);
						scoutFleet.setLocation(scoutStartLoc.x, scoutStartLoc.y);
					}
					//____________________这里的AI需要更换：更换为前往到目标地点后消极环绕，侦测到任何舰队靠近就离开此星系_________________________
					//scoutFleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, scoutTarget, 1000, "前往" + scoutTarget.getName());
					// assign a smarter scout AI which will pick behaviors (orbit, roam, stealth) and flee if chased
					scoutFleet.addScript(new IIRT_Omega_ScoutAI(scoutFleet, scoutTarget));
					scoutFleet.addTag("IIRT_Omega_Scout");
					Global.getLogger(this.getClass()).info("omega scout fleet spawn at (" + scoutStartLoc.x + " , " + scoutStartLoc.y + ") and target to " + scoutTarget.getStarSystem().getId() + " with " + scoutFleet.getFleetSizeCount() + " ships, Fleet Id is " + scoutFleet.getId() + " . And For Hyperspace, they are " + isInHyperspace + "in.");

					scoutFleet.getMemoryWithoutUpdate().set(MemFlags.ENTITY_MISSION_IMPORTANT, true);
					scoutFleet.setTransponderOn(false);
					scoutFleet.setSensorProfile(200f);
					if (Global.getSettings().isDevMode()) {
						Global.getLogger(this.getClass()).info("===___===__=Omega_Invasion Scout=__===___===");
						addCampaignWayInDev(scoutFleet,"omegaScout spawn at (" + scoutStartLoc.x + " , " + scoutStartLoc.y + ") and target to " + scoutTarget.getStarSystem().getId() + " with " + scoutFleet.getFleetSizeCount() + " ships, Fleet Id is " + scoutFleet.getId() + " . And For Hyperspace, they are " + isInHyperspace + "in.",
								"omegaScout spawn at (" + scoutStartLoc.x + " , " + scoutStartLoc.y + ") and target to " + scoutTarget.getStarSystem().getId() + " with " + scoutFleet.getFleetSizeCount() + " ships, Fleet Id is " + scoutFleet.getId() + " . And For Hyperspace, they are " + isInHyperspace + "in.");
						playerFleet.addFloatingText("omegaScout spawn at (" + scoutStartLoc.x + " , " + scoutStartLoc.y + ") and target to " + scoutTarget.getStarSystem().getId() + " with " + scoutFleet.getFleetSizeCount() + " ships, Fleet Id is " + scoutFleet.getId() + " . And For Hyperspace, they are " + isInHyperspace + "in.", NOTICE_COLOR, 60.0F);
						scoutFleet.setAlwaysUseSensorFaderBrightness(true);
					}else{
						scoutFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF, true);
					}
				}
				if (stageElapsed > collect_data_time) {
					setStage(STAGE.INVADE);
					stageElapsed = 0f;
					elapsed = 0;
				}
				break;
			case INVADE:
				stageElapsed += Global.getSector().getClock().convertToDays(amount);
				if (baseSystem == null && Global.getSector().getMemoryWithoutUpdate().contains(baseSystem_id)) {
					String baseSystemId = (String)Global.getSector().getMemoryWithoutUpdate().get(baseSystem_id);
					baseSystem = Global.getSector().getStarSystem(baseSystemId);
				}
				if (baseMarket == null && baseSystem != null && Global.getSector().getMemoryWithoutUpdate().contains(baseMarket_id)) {
					String marketId = (String)Global.getSector().getMemoryWithoutUpdate().get(baseMarket_id);
					for (PlanetAPI p : baseSystem.getPlanets()) {
						if (p.getMarket() != null && p.getMarket().getId().contentEquals(marketId)) baseMarket = p.getMarket();
					}
				}
				if (baseSystem == null || baseMarket == null) {
					// 选择一个星系
					WeightedRandomPicker<StarSystemAPI> SystemPicker = new WeightedRandomPicker<>();
					for (StarSystemAPI system : Global.getSector().getStarSystems()) {
						if (system.isProcgen()) {
							if (system.getStar().getTypeId().contentEquals(StarTypes.BLUE_GIANT) || system.getStar().getTypeId().contentEquals(StarTypes.BLUE_SUPERGIANT)) {
								if (system.getPlanets().size() <= 1) continue;
								float weight = 1f;
								if (system.isEnteredByPlayer()) weight *= 0.9f;
								if (system.getAllEntities().contains(Global.getSector().getPlayerFleet())) weight = 0f;
								for (PlanetAPI planet : system.getPlanets()) {
									if (planet.getMarket() != null) weight *= 0.2f;
								}
								SystemPicker.add(system, weight);
							}
						}
					}
					Global.getLogger(this.getClass()).info(SystemPicker.getItems().size() + " systems with blue giants");
					if (SystemPicker.isEmpty()) {
						for (StarSystemAPI system : Global.getSector().getStarSystems()) {
							if (system.isProcgen()) {
								if (system.getPlanets().size() <= 1) continue;
								float weight = 1f;
								if (system.isEnteredByPlayer()) weight *= 0.9f;
								if (system.getAllEntities().contains(Global.getSector().getPlayerFleet())) weight = 0f;
								for (PlanetAPI planet : system.getPlanets()) {
									if (planet.getMarket() != null) weight *= 0.2f;
								}
								SystemPicker.add(system, weight);
							}
						}
					}
					if (!SystemPicker.isEmpty()) {
						baseSystem = SystemPicker.pick();
						if (Global.getSettings().isDevMode()) {
							Global.getLogger(this.getClass()).info("===___===__=Omega_Invasion Invade=__===___===");
							addCampaignMessageInDev(SystemPicker.getItems().size() + " systems with blue giants, And" + baseSystem.getId() + " had been picked",
									SystemPicker.getItems().size() + " systems with blue giants, And" + baseSystem.getId() + " had been picked");
							playerFleet.addFloatingText(SystemPicker.getItems().size() + " systems with blue giants, And" + baseSystem.getId() + " had been picked", NOTICE_COLOR, 60.0F);
							Global.getLogger(this.getClass()).info(SystemPicker.getItems().size() + " systems with blue giants, And" + baseSystem.getId() + " had been picked");
						}
					}
					if (baseSystem != null) {
						Global.getSector().getMemoryWithoutUpdate().set(baseSystem_id, baseSystem.getId());
					}
					if (!baseSystem.getMemoryWithoutUpdate().contains("$IIRT_Omega_Invaded")) {
						baseSystem.getMemoryWithoutUpdate().set("$IIRT_Omega_Invaded", true);
						WeightedRandomPicker<PlanetAPI> basePicker = new WeightedRandomPicker<>();
						for (PlanetAPI planet : baseSystem.getPlanets()) {
							float weight = 1f;
							if (planet.isStar()) continue;
							if (!planet.isGasGiant()) weight *= 1.25f;
							if (planet.getMarket() != null && planet.getMarket().getFaction() != null && !planet.getMarket().getFaction().getId().contentEquals(Factions.OMEGA)) {
								Global.getSector().getEconomy().removeMarket(planet.getMarket());
							}
							basePicker.add(planet, weight);
						}
						PlanetAPI base = basePicker.pick();
						Global.getLogger(this.getClass()).info("picked " + base.getId());
						base.addTag("IIRT_Omega_Invaded");
						base.getSpec().setGlowColor(new Color(173, 181, 182));
						base.getSpec().setIconColor(new Color(154, 154, 154));
						base.getSpec().setCloudColor(new Color(127, 175, 185));
						base.getSpec().setPlanetColor(new Color(152, 141, 164));
						base.getSpec().setUseReverseLightForGlow(true);
						baseMarket = Global.getFactory().createMarket("market_" + base.getId(), base.getName(), 7);
						Global.getSector().getMemoryWithoutUpdate().set(baseMarket_id, baseMarket.getId());
						baseMarket.setFactionId(IIRT_Omega_Faction);
						baseMarket.addCondition("IIRT_Omega_Repair_Facility");
						baseMarket.addIndustry("orbitalworks");
						baseMarket.addIndustry("planetaryshield");
						baseMarket.addIndustry("highcommand");
						baseMarket.setPrimaryEntity(base);
						baseMarket.getMemoryWithoutUpdate().set("$IIRT_Omega_Invaded", true);
						Global.getSector().getEconomy().addMarket(baseMarket, false);
						base.setMarket(baseMarket);
					}
				}
				elapsed += Global.getSector().getClock().convertToDays(amount);
				if (elapsed >= 2f) {
					elapsed = 0;
					Vector2f invStartLoc = Misc.getPointWithinRadius(baseSystem.getLocation(), 300f);
					FleetParamsV3 invFleetParam = new FleetParamsV3(invStartLoc, IIRT_Omega_Faction, 0.25f, FleetTypes.PATROL_LARGE, 80, 0, 0, 0, 0, 0, 0);
					invFleetParam.maxNumShips = (int)(Global.getSettings().getMaxShipsInFleet() * 1.5f);
					// invFleetParam.aiCores = OfficerQuality.AI_OMEGA;
					spawnRandomPTSDForce(invFleetParam, Global.getSettings().getBattleSize());//————————————————————————
					CampaignFleetAPI invFleet = FleetFactoryV3.createFleet(invFleetParam);
					// scoutFleet.getFleetData().addFleetMember("IIRT_Omega_Kb_Only");
					invFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
					// IIRT_ColdShardSpawner.assignShardSpawnToFleet(invFleet, Math.max(30, Math.round(invFleet.getFleetPoints())));

					Global.getSector().getHyperspace().addEntity(invFleet);
					invFleet.setLocation(invStartLoc.x, invStartLoc.y);

					if (Global.getSettings().isDevMode()) {
						Global.getLogger(this.getClass()).info("===___===__=Omega_Invasion Invade=__===___===");
						addCampaignWayInDev(invFleet,"Small Invade fleet been set, And at x=" + invStartLoc.x + " & y=" + invStartLoc.y + ". In the Hyperspace, target to "+baseMarket.getName()+" in "+baseMarket.getStarSystem().getName(),
								"Small Invade fleet been set, And at x=" + invStartLoc.x + " & y=" + invStartLoc.y + ". In the Hyperspace, target to "+baseMarket.getName()+" in "+baseMarket.getStarSystem().getName());
						playerFleet.addFloatingText("Small Invade fleet been set, And at x=" + invStartLoc.x + " & y=" + invStartLoc.y + ". In the Hyperspace, target to "+baseMarket.getName()+" in "+baseMarket.getStarSystem().getName(), NOTICE_COLOR, 40.0F);
						Global.getLogger(this.getClass()).info("Small Invade fleet been set, And at x=" + invStartLoc.x + " & y=" + invStartLoc.y + ". In the Hyperspace, target to "+baseMarket.getName()+" in "+baseMarket.getStarSystem().getName());

						DebugGOTO(playerFleet,invFleet);
					}

					invFleet.addAssignment(FleetAssignment.DEFEND_LOCATION, baseMarket.getPrimaryEntity(), 100000f, "前往" + baseMarket.getName());
					invFleet.addTag("IIRT_Omega_Scout");
					invFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, true);
				}
				if (stageElapsed > invade_time) {
					stageElapsed = 0;
					setStage(STAGE.REPAIR);
					elapsed = 0;
				}
				break;
			case REPAIR:
				// sleeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeep
				// until they can wipe out anything in this sector
				stageElapsed += Global.getSector().getClock().convertToDays(amount);
				FactionAPI iirt_omega = Global.getSector().getFaction(IIRT_Omega_Faction);
				// during repair stage, gradually spawn guard fleets up to the configured cap
				int spawnedGuards = 0;
				if (baseSystem != null) spawnedGuards = baseSystem.getMemoryWithoutUpdate().contains("$IIRT_Omega_spawnedGuards") ? baseSystem.getMemoryWithoutUpdate().getInt("$IIRT_Omega_spawnedGuards") : 0;
				float guardSpawnInterval = 5f; // days between guard spawns
				if (baseSystem != null && spawnedGuards < max_guard_fleets) {
					baseSystem.getMemoryWithoutUpdate().set("$IIRT_Omega_lastGuardSpawnCheck", baseSystem.getMemoryWithoutUpdate().getFloat("$IIRT_Omega_lastGuardSpawnCheck") + Global.getSector().getClock().convertToDays(amount));
					float last = baseSystem.getMemoryWithoutUpdate().getFloat("$IIRT_Omega_lastGuardSpawnCheck");
					if (last >= guardSpawnInterval) {
						baseSystem.getMemoryWithoutUpdate().set("$IIRT_Omega_lastGuardSpawnCheck", 0f);
						Vector2f loc = Misc.getPointWithinRadius(baseMarket.getPrimaryEntity().getLocation(), 300f + 1000f * spawnedGuards);
						FleetParamsV3 params = new FleetParamsV3(loc, IIRT_Omega_Faction, 1f, FleetTypes.PATROL_LARGE, Global.getSettings().getBattleSize(), 0, 0, 0, 0, 0, 0);
						params.maxNumShips = (int)(Global.getSettings().getMaxShipsInFleet() * (1f + spawnedGuards * 0.1f));
						params.aiCores = OfficerQuality.AI_OMEGA;
						CampaignFleetAPI guard = FleetFactoryV3.createFleet(params);
						guard.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
						// IIRT_ColdShardSpawner.assignShardSpawnToFleet(guard, Math.max(30, Math.round(guard.getFleetPoints())));
						baseSystem.addEntity(guard);
						guard.setLocation(loc.x, loc.y);
						guard.addAssignment(FleetAssignment.DEFEND_LOCATION, baseMarket.getPrimaryEntity(), 100000f);
						guard.setName("Omega Guard");
						spawnedGuards++;
						baseSystem.getMemoryWithoutUpdate().set("$IIRT_Omega_spawnedGuards", spawnedGuards);
					}
				}

				if (stageElapsed > repair_time) {
					if (Global.getSettings().isDevMode()) {
						Global.getLogger(this.getClass()).info("===___===__=Omega_Invasion Repair=__===___===");
						addCampaignMessageInDev("Omega Repair Complete, Stage change to Full Attack",
								"Omega Repair Complete, Stage change to Full Attack");
						playerFleet.addFloatingText("Omega repair Complete", NOTICE_COLOR, 15.0F);
					}
                    setStage(STAGE.FULL_ATTACK);
                    stageElapsed = 0;
                    iirt_omega.getDoctrine().setNumShips(5);
                    iirt_omega.getDoctrine().setOfficerQuality(5);
                    iirt_omega.getDoctrine().setShipQuality(5);
                    iirt_omega.getDoctrine().setAggression(5);
                    // final invasion notification and choose a reserve system unvisited by player
                    try {
                        WeightedRandomPicker<StarSystemAPI> picker = new WeightedRandomPicker<>();
                        for (StarSystemAPI sys : Global.getSector().getStarSystems()) {
                            if (!sys.isProcgen()) continue;
                            if (sys.isEnteredByPlayer()) continue;
                            picker.add(sys);
                        }
                        if (!picker.isEmpty()) {
                            StarSystemAPI reserve = picker.pick();
                            Global.getSector().getMemoryWithoutUpdate().set("$IIRT_Omega_ReserveSystem", reserve.getId());
                            Global.getSector().getCampaignUI().getMessageDisplay().addMessage("警报：检测到Omega在远方建立了后备据点（" + reserve.getName() + "）。", NOTICE_COLOR);
                        }
                        // Launch reserve attacker script
                        Global.getSector().addScript(new IIRT_Omega_ReserveAttacker(Global.getSector()));
                    } catch (Throwable t) {
                        // ignore UI failures
                    }
				}

				break;
			case FULL_ATTACK:
				// better use nexerlin here
				elapsed += Global.getSector().getClock().convertToDays(amount);
				stageElapsed += Global.getSector().getClock().convertToDays(amount);
				if (elapsed >= inv_interval) {
					for (FactionAPI faction : Global.getSector().getAllFactions()) {
						Global.getSector().getFaction(IIRT_Omega_Faction).setRelationship(faction.getId(), RepLevel.HOSTILE);
					}
					elapsed = 0;
					inv_interval = (int)(10f + Math.random() * 10);
					WeightedRandomPicker<FactionAPI> toInv = new WeightedRandomPicker<>();
					for (FactionAPI faction : Global.getSector().getAllFactions()) {
						if (!faction.isShowInIntelTab()) continue;
						toInv.add(faction);
					}
					if (toInv != null || !toInv.isEmpty()) {
						FactionAPI toInvFac = toInv.pick();
						List<MarketAPI> markets = Global.getSector().getEconomy().getMarketsCopy();
						WeightedRandomPicker<MarketAPI> toInvMarket = new WeightedRandomPicker<>();
						for (MarketAPI market : markets) {
							if (market.getFaction().getId().contentEquals(toInvFac.getId())) {
								toInvMarket.add(market);
							}
						}
						MarketAPI toInvMkt = toInvMarket.pick();

						FleetParamsV3 invFleetParam = new FleetParamsV3(baseMarket.getLocationInHyperspace(), IIRT_Omega_Faction, 1f, FleetTypes.TASK_FORCE, Global.getSettings().getBattleSize() * 2f, 0, 0, 0, 0, 0, 4);
						invFleetParam.maxNumShips = (int)(Global.getSettings().getMaxShipsInFleet() * 1.5f);
						invFleetParam.aiCores = OfficerQuality.AI_OMEGA;
						CampaignFleetAPI invFleet = FleetFactoryV3.createFleet(invFleetParam);
						// scoutFleet.getFleetData().addFleetMember("IIRT_Omega_Kb_Only");
						invFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
						Global.getSector().getHyperspace().addEntity(invFleet);
						invFleet.setLocation(baseMarket.getLocation().x, baseMarket.getLocation().y);
						invFleet.setName("Omega Invaders");
						if (Global.getSettings().isDevMode()) {
							Global.getLogger(this.getClass()).info("===___===__=Omega_Invasion Full Attack=__===___===");
							addCampaignMessageInDev("Omega invading " + toInvMkt.getName() + " at System: " + toInvMkt.getStarSystem().getId()+", through ("+baseMarket.getLocation().x+", "+baseMarket.getLocation().y+") in Hyperspace.",
									"Omega invading " + toInvMkt.getName() + " at System: " + toInvMkt.getStarSystem().getId()+", through ("+baseMarket.getLocation().x+", "+baseMarket.getLocation().y+") in Hyperspace.");
							playerFleet.addFloatingText("Omega invading " + toInvMkt.getName() + " at System: " + toInvMkt.getStarSystem().getId()+", through ("+baseMarket.getLocation().x+", "+baseMarket.getLocation().y+") in Hyperspace.", NOTICE_COLOR, 50.0F);
							Global.getLogger(this.getClass()).info("Omega invading " + toInvMkt.getName() + " at System: " + toInvMkt.getStarSystem().getId()+", through ("+baseMarket.getLocation().x+", "+baseMarket.getLocation().y+") in Hyperspace.");

							DebugGOTO(playerFleet,invFleet);
						}
						FleetParamsV3 defFleetParam = new FleetParamsV3(baseMarket.getLocationInHyperspace(), IIRT_Omega_Faction, 1f, FleetTypes.PATROL_LARGE, Global.getSettings().getBattleSize(), 0, 0, 0, 0, 0, 0);
						invFleetParam.maxNumShips = (int)(Global.getSettings().getMaxShipsInFleet() * 1.5f);
						invFleetParam.aiCores = OfficerQuality.AI_OMEGA;
						CampaignFleetAPI defFleet = FleetFactoryV3.createFleet(defFleetParam);
						// scoutFleet.getFleetData().addFleetMember("IIRT_Omega_Kb_Only");
						defFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
					//IIRT_ColdShardSpawner.assignShardSpawnToFleet(defFleet, Math.max(30, Math.round(defFleet.getFleetPoints())));

						baseMarket.getContainingLocation().addEntity(defFleet);
						defFleet.setLocation(baseMarket.getPrimaryEntity().getLocation().x, baseMarket.getPrimaryEntity().getLocation().y);
						defFleet.setContainingLocation(baseSystem);
						defFleet.clearAssignments();
						defFleet.addAssignment(FleetAssignment.DEFEND_LOCATION, baseMarket.getPrimaryEntity(), 1000f);
						defFleet.setName("Omega Defenders");
						invFleet.clearAssignments();
						invFleet.addAssignment(FleetAssignment.ATTACK_LOCATION, toInvMkt.getPrimaryEntity(), 1000f);

					}
				}
				if (Global.getSector().getMemoryWithoutUpdate().getBoolean("$IIRT_omega_Invasion_End")) {
					Global.getSector().getMemoryWithoutUpdate().set(stage_id, STAGE.END);
					setStage(STAGE.END);
					baseMarket.removeCondition("IIRT_Omega_Repair_Facility");
				}
				break;
			case END:
				break;
			default:
				break;

		}
	}

	private void ensureStageInitialized() {
		if (currStage != null) return;
		if (Global.getSector() == null) {
			currStage = STAGE.START;
			return;
		}
		if (Global.getSector() != null && Global.getSector().getMemoryWithoutUpdate().contains(stage_id)) {
			Object stageObj = Global.getSector().getMemoryWithoutUpdate().get(stage_id);
			if (stageObj instanceof STAGE) {
				currStage = (STAGE) stageObj;
				return;
			}
			if (stageObj instanceof String) {
				try {
					currStage = STAGE.valueOf((String) stageObj);
					return;
				} catch (Throwable ignored) {
					// fall through to default
				}
			}
		}
		if(Global.getSettings().isDevMode()) {
			switch (PTSD_DefStat_onNewGame){
				case "Sar":
					setStage(STAGE.START);
					return;
				case "Cod":
					setStage(STAGE.COLLECT_DATA);
					return;
				case "Inv":
					setStage(STAGE.INVADE);
					return;
				case "Rep":
					setStage(STAGE.REPAIR);
					return;
				case "FuA":
					setStage(STAGE.FULL_ATTACK);
					return;
				case "End":
					setStage(STAGE.END);
					return;
			};
		}
		setStage(STAGE.START);
	}
	private static final String[] SHARD_HULLMODS = new String[] {"IIRT_Omega_BugShardSpawner","IIRT_Omega_ColdShardSpawner","IIRT_Omega_WebShardSpawner","IIRT_Omega_TranShardSpawner","IIRT_Omega_CubeShardSpawner"};

/*
    private void assignShardSystemToFleet(CampaignFleetAPI fleet) {
        if (fleet == null) return;
        String chosen = SHARD_HULLMODS[(int) (Math.random() * SHARD_HULLMODS.length)];
        try {
            for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                ShipVariantAPI var = member.getVariant();
                for (String m : SHARD_HULLMODS) {
                    if (var.getHullMods().contains(m)) var.removeMod(m);
                }
                var.addMod(chosen);
            }
            fleet.getMemoryWithoutUpdate().set("$IIRT_Omega_ShardSystem", chosen);
        } catch (Throwable t) {
            Global.getLogger(this.getClass()).info("Failed to assign shard system to fleet: " + t.getMessage());
        }
    }

 */

	public static class ShardTypeVariants {

		public Map<ShardType, WeightedRandomPicker<String>> variants = new HashMap<>();
		public ShardTypeVariants() {
		}
		public WeightedRandomPicker<String> get(ShardType type) {
			WeightedRandomPicker<String> result = variants.get(type);
			if (result == null) {
				result = new WeightedRandomPicker<>();
				variants.put(type, result);
			}
			return result;
		}
	}
	public enum ShardType {
		GENERAL, ANTI_ARMOR, ANTI_SHIELD, POINT_DEFENSE, MISSILE,
	}


    public void DebugGOTO (CampaignFleetAPI playerFleet, CampaignFleetAPI invFleet){ //因为一般生成在超空间，所以请在超空间待机
		if(invFleet != null) {
			SectorEntityToken thisFleet = (SectorEntityToken) invFleet;
			if(thisFleet != null) {  //I DON'T Know WHY, It just should like this.
				final Vector2f loc = thisFleet.getLocation();
				playerFleet.setLocation(loc.x, loc.y);
				playerFleet.clearAssignments();
				playerFleet.addAssignment(FleetAssignment.GO_TO_LOCATION, thisFleet, 1f);
			}else{
				Global.getLogger(this.getClass()).info("===___===__=Omega_Invasion SectorEntityToken ERROR=__===___===");
				addCampaignMessageInDev("Omega Invation Fleet SectorEntityToken ERROR",
						"Omega Invation Fleet SectorEntityToken ERROR");
				playerFleet.addFloatingText("Omega Invation Fleet SectorEntityToken ERROR",new Color(255,0,0,255),60f);
				Global.getLogger(this.getClass()).info("Omega Invation Fleet SectorEntityToken ERROR");
			}
			/*
			if(invFleet.getLightSource()!=null) {
				SectorEntityToken thisFleet = invFleet.getLightSource();

				final Vector2f loc = thisFleet.getLocation();
				playerFleet.setLocation(loc.x, loc.y);
				playerFleet.clearAssignments();
				playerFleet.addAssignment(FleetAssignment.GO_TO_LOCATION, thisFleet, 1f);
			} else if (invFleet.getOrbitFocus()!=null) {
				SectorEntityToken thisFleet = invFleet.getOrbitFocus();

				final Vector2f loc = thisFleet.getLocation();
				playerFleet.setLocation(loc.x, loc.y);
				playerFleet.clearAssignments();
				playerFleet.addAssignment(FleetAssignment.GO_TO_LOCATION, thisFleet, 1f);

			} else if (()invFleet){

			}

			 */
		}else{
			Global.getLogger(this.getClass()).info("===___===__=Omega_Invasion CREATE ERROR=__===___===");
			playerFleet.addFloatingText("Omega Invation Fleet Create ERROR",new Color(255,0,0,255),60f);
			Global.getLogger(this.getClass()).info("Omega Invation Fleet Create ERROR");

		}
	}
	public static void addCampaignMessageInDev(String output,String log){
		if(Global.getSettings().isDevMode()){
			output = "Dev: " + output;
			Global.getSector().getCampaignUI().addMessage(output);
		}
		if(log!=null){
			Global.getLogger(IIRT_Omega_Invasion.class).info(log);
		}
	}
	public static void addCampaignWayInDev(CampaignFleetAPI fleet,String output,String log){
		if(Global.getSettings().isDevMode()){
			output = "Dev: " + output;
			Global.getSector().getCampaignUI().addMessage(output);

			CampaignFleetAPI player = Global.getSector().getPlayerFleet();
			SectorEntityToken destination = null;
			destination = fleet;

			player.getContainingLocation().removeEntity(player);
			destination.getContainingLocation().addEntity(player);
			Global.getSector().setCurrentLocation(destination.getContainingLocation());
			player.setLocation(destination.getLocation().x,
					destination.getLocation().y);

			player.setNoEngaging(5.0f);
			player.clearAssignments();
			player.addAssignment(FleetAssignment.GO_TO_LOCATION, destination, 1f);
		}
		if(log!=null){
			Global.getLogger(IIRT_Omega_Invasion.class).info(log);
		}
	}
}