package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import data.scripts.IIRT_Omega_ModPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldSource;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Physical black-hole works. The ledger owns each station; normal event unloading never does. */
public final class PTSDFortressDirector {
    private static final String[] VARIANTS = {"", "IIRT_Omega_Station_Small",
            "IIRT_Omega_Station_Stable", "IIRT_Omega_Station_Medium", "PTSD_Omega_EDEN_ZONE"};
    private PTSDFortressDirector() { }
    private static com.fs.starfarer.api.campaign.PlanetAPI blackHole(StarSystemAPI system) {
        if (system == null) return null;
        for (com.fs.starfarer.api.campaign.PlanetAPI star : system.getPlanets())
            if (star.isStar() && star.getSpec() != null && star.getSpec().isBlackHole()) return star;
        return null;
    }

    private static boolean isInhabited(StarSystemAPI system) {
        if (system == null || Global.getSector() == null) return false;
        for (com.fs.starfarer.api.campaign.econ.MarketAPI market :
                Global.getSector().getEconomy().getMarkets(system))
            if (market != null && !market.isPlanetConditionMarketOnly()) return true;
        return false;
    }

    public static int activeFortressCount(PTSDCrisisState state) {
        int count = 0;
        if (state != null && state.late != null)
            for (PTSDLateState.Fortress fortress : state.late.fortresses.values())
                if (fortress != null && !fortress.destroyed) count++;
        return count;
    }

    public static boolean devStartNext(PTSDCrisisState state, StarSystemAPI preferred) {
        if (state == null || !Global.getSettings().isDevMode() || state.late.warPoints < 10f ||
                state.late.strategicReserve < 30f ||
                activeFortressCount(state) >=
                        Math.round(IIRT_Omega_ModPlugin.max_black_hole_fortresses)) return false;
        List<StarSystemAPI> candidates = new ArrayList<StarSystemAPI>();
        if (preferred != null && !isInhabited(preferred)) candidates.add(preferred);
        for (StarSystemAPI system : Global.getSector().getStarSystems())
            if (!isInhabited(system)) candidates.add(system);
        if (preferred != null && isInhabited(preferred)) candidates.add(preferred);
        for (StarSystemAPI system : Global.getSector().getStarSystems())
            if (isInhabited(system)) candidates.add(system);
        for (StarSystemAPI system : candidates) {
            if (system == null || blackHole(system) == null ||
                    state.late.committedIds.contains("fortress-site:" + system.getId())) continue;
            construct(state, system, PTSDCrisisState.getDay());
            return state.late.fortresses.containsKey("fortress:" + system.getId());
        }
        return false;
    }

    /** Delivery teleports one paid, physical transport; each click supplies one construction increment. */
    public static boolean devDeliverOne(PTSDCrisisState state) {
        if (state == null || !Global.getSettings().isDevMode()) return false;
        float day = PTSDCrisisState.getDay();
        for (PTSDLateState.Fortress record : state.late.fortresses.values()) {
            if (record == null || record.destroyed || record.tier >= 4) continue;
            CampaignFleetAPI station = findFleet(record.fleetId);
            if (station == null || station.getStarSystem() == null) continue;
            if (record.transportIds.isEmpty()) sendTransport(state, record, station, day);
            if (record.transportIds.isEmpty()) return false;
            CampaignFleetAPI transport = findFleet(record.transportIds.get(0));
            if (transport == null) { processTransports(state, record, station, day); return false; }
            transport.setLocation(station.getLocation().x + 200f, station.getLocation().y);
            processTransports(state, record, station, day);
            return true;
        }
        return false;
    }

    public static boolean devInterceptOne(PTSDCrisisState state) {
        if (state == null || !Global.getSettings().isDevMode()) return false;
        for (PTSDLateState.Fortress record : state.late.fortresses.values()) {
            if (record == null || record.destroyed || record.transportIds.isEmpty()) continue;
            CampaignFleetAPI station = findFleet(record.fleetId);
            CampaignFleetAPI transport = findFleet(record.transportIds.get(0));
            if (station == null || transport == null || transport.getContainingLocation() == null) continue;
            PTSDFTDirector.onTransportLost(state, transport.getId(), record.currentSystemId,
                    transport.getLocation().x, transport.getLocation().y);
            PTSDCrisisDevIntel.report("DEV 模拟运输损失", "实体由 DEV 指令移除；FT-09 仅按模拟来源触发",
                    record.currentSystemId, transport.getId());
            transport.getContainingLocation().removeEntity(transport);
            processTransports(state, record, station, PTSDCrisisState.getDay());
            return true;
        }
        return false;
    }

    public static boolean devOrderMove(PTSDCrisisState state) {
        if (state == null || !Global.getSettings().isDevMode()) return false;
        for (PTSDLateState.Fortress record : state.late.fortresses.values()) {
            if (record == null || record.destroyed || record.tier < 4 || record.inTransit) continue;
            CampaignFleetAPI station = findFleet(record.fleetId);
            if (station == null || station.getStarSystem() == null) continue;
            moveIfOrdered(state, record, station, PTSDCrisisState.getDay(), true);
            return record.inTransit;
        }
        return false;
    }

    /** Explicit DEV simulation; it changes the station variant but does not invent deliveries or spend resources. */
    public static boolean devCompleteOne(PTSDCrisisState state) {
        if (state == null || state.late == null || !Global.getSettings().isDevMode()) return false;
        for (PTSDLateState.Fortress record : state.late.fortresses.values()) {
            if (record == null || record.destroyed || record.tier >= 4 || record.inTransit) continue;
            CampaignFleetAPI station = findFleet(record.fleetId);
            if (station == null || station.getFleetData().getMembersListCopy().isEmpty()) continue;
            FleetMemberAPI next;
            try {
                next = Global.getFactory().createFleetMember(FleetMemberType.SHIP, VARIANTS[4]);
            } catch (Throwable ex) {
                Global.getLogger(PTSDFortressDirector.class).warn("DEV fortress variant unavailable", ex);
                return false;
            }
            for (FleetMemberAPI old : new ArrayList<FleetMemberAPI>(
                    station.getFleetData().getMembersListCopy()))
                station.getFleetData().removeFleetMember(old);
            station.getFleetData().addFleetMember(next);
            next.getStats().getCRLossPerSecondPercent().modifyMult("PTSD_black_hole_work", 0f);
            record.tier = 4;
            record.tierProgress = 0f;
            record.collapsed = true;
            station.setName("节点");
            collapseBlackHole(state.resolveSystem(record.originSystemId));
            PTSDCrisisDevIntel.report("DEV 模拟要塞竣工",
                    record.id + "：完整体变体已安装；没有补造运输或扣费",
                    record.currentSystemId, station.getId());
            return true;
        }
        return false;
    }

    public static String devDestroyReason(PTSDCrisisState state, boolean hyperspace) {
        if (state == null || state.late == null) return "危机或要塞账本尚未初始化";
        if (!Global.getSettings().isDevMode()) return "仅限 DevMode";
        if (state.late.fortresses.isEmpty()) return "没有已登记要塞；先执行要塞开工";
        for (PTSDLateState.Fortress record : state.late.fortresses.values()) {
            if (record == null || record.destroyed) continue;
            if (record.tier < 4) return record.id + " 目前仅为等级 " + record.tier +
                    "；先等待施工或使用“模拟推进至完整体”";
            if (hyperspace != record.inTransit) return record.id +
                    (hyperspace ? " 尚未进入超空间调动" : " 正在超空间调动，尚未抵达星系");
            if (findFleet(record.fleetId) == null) return record.id + " 的持久舰队对象不存在";
        }
        return "没有处于所选地点且可击毁的完整要塞";
    }

    public static boolean devDestroyOne(PTSDCrisisState state, boolean hyperspace) {
        if (state == null || !Global.getSettings().isDevMode()) return false;
        for (PTSDLateState.Fortress record : state.late.fortresses.values()) {
            if (record == null || record.destroyed || record.tier < 4 ||
                    (hyperspace != record.inTransit)) continue;
            CampaignFleetAPI station = findFleet(record.fleetId);
            if (station == null) continue;
            onFleetDefeated(state, station, false);
            if (!hyperspace) {
                PTSDLateState.Detonation d = state.late.detonations.get(record.id);
                CampaignFleetAPI player = Global.getSector().getPlayerFleet();
                if (d != null && (player == null || player.getStarSystem() == null ||
                        !record.currentSystemId.equals(player.getStarSystem().getId())))
                    PTSDFortressEscapeScript.explode(state, d, player);
            }
            return true;
        }
        return false;
    }

    public static void advance(PTSDCrisisState state, float day) {
        if (state == null || state.late == null) return;
        if (state.late.committedIds.add("fortress-legacy-adoption-v2")) adoptLegacyFortresses(state, day);
        if ((state.phase == PTSDCrisisState.Phase.FORTIFICATION || state.phase == PTSDCrisisState.Phase.WAR) &&
                day >= state.late.nextFortressBuildDay &&
                activeFortressCount(state) <
                        Math.round(IIRT_Omega_ModPlugin.max_black_hole_fortresses)) {
            state.late.nextFortressBuildDay = day + 2f;
            outer:
            for (int pass = 0; pass < 2; pass++)
                for (StarSystemAPI system : Global.getSector().getStarSystems()) {
                    if (isInhabited(system) != (pass == 1) ||
                            blackHole(system) == null)
                        continue;
                    if (state.late.committedIds.contains("fortress-site:" + system.getId())) continue;
                    if (state.late.warPoints < 10f || state.late.strategicReserve < 30f) break outer;
                    construct(state, system, day);
                    if (state.late.fortresses.containsKey("fortress:" + system.getId()))
                        state.late.nextFortressBuildDay = day + 30f;
                    break outer;
                }
        }
        for (PTSDLateState.Fortress record : new ArrayList<PTSDLateState.Fortress>(state.late.fortresses.values())) {
            if (record == null || record.destroyed) continue;
            CampaignFleetAPI station = findFleet(record.fleetId);
            if (station == null || station.getFleetData().getMembersListCopy().isEmpty()) {
                onDestroyed(state, record, day, false);
                continue;
            }
            if (record.inTransit && record.destinationSystemId != null &&
                    station.getContainingLocation() == Global.getSector().getHyperspace()) {
                advanceTransit(state, record, station, day);
            }
            if (record.inTransit && record.destinationSystemId != null &&
                    station.getStarSystem() != null &&
                    record.destinationSystemId.equals(station.getStarSystem().getId())) {
                record.inTransit = false;
                record.currentSystemId = record.destinationSystemId;
                record.destinationSystemId = null;
                record.movedPrewar |= state.phase != PTSDCrisisState.Phase.WAR;
                station.clearAssignments();
                station.setStationMode(true);
                station.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
                station.setCircularOrbitWithSpin(station.getStarSystem().getStar(), 0f, 1400f, 40f, 1f, 2f);
                for (String escortId : record.patrolIds) {
                    CampaignFleetAPI escort = findFleet(escortId);
                    if (escort == null || escort.getContainingLocation() != Global.getSector().getHyperspace()) continue;
                    Global.getSector().getHyperspace().removeEntity(escort);
                    station.getStarSystem().addEntity(escort);
                    escort.setLocation(station.getLocation().x + 900f,
                            station.getLocation().y + 900f);
                    escort.addAssignment(FleetAssignment.PATROL_SYSTEM, station, 10000f,
                            "环绕设施");
                }
                PTSDLateWarDirector.enqueueAlert(state, record.id + ":arrived:" + Math.round(day),
                        record.currentSystemId, "视界要塞到达；当地防务与补给航线重新计算。", day);
                PTSDLateWarDirector.creditStrategicAssets(state,
                        "fortress-arrival:" + record.id + ":" + Math.round(day), 5f,
                        "要塞调动完成", record.currentSystemId);
            }
            if (record.tier < 4) {
                processTransports(state, record, station, day);
                if (day >= record.nextTransportDay && day >= record.blockedUntilDay &&
                        record.tierProgress < IIRT_Omega_ModPlugin.lateFortressWork[record.tier] &&
                        record.transportIds.size() < 2) sendTransport(state, record, station, day);
            } else {
                moveIfOrdered(state, record, station, day, false);
                if (!record.inTransit) ensurePatrols(state, record, station);
        if (!record.inTransit && state.phase == PTSDCrisisState.Phase.WAR) {
                    spawnArrivalAftermath(state, record, station);
                }
            }
        }
    }

    private static void spawnArrivalAftermath(PTSDCrisisState state, PTSDLateState.Fortress record,
                                              CampaignFleetAPI station) {
        StarSystemAPI system = station.getStarSystem();
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (system == null || player == null || player.getStarSystem() != system ||
                system.getId().equals(record.originSystemId) ||
                !state.late.committedIds.add("arrival-scene:" + record.id + ":" + system.getId())) return;
        Random rng = new Random(record.id.hashCode() ^ system.getId().hashCode());
        for (int n = 0; n < 10; n++) {
            DebrisFieldParams params = new DebrisFieldParams(160f + rng.nextInt(180),
                    -1f, 10f + rng.nextInt(21), .4f);
            params.source = DebrisFieldSource.BATTLE;
            com.fs.starfarer.api.campaign.SectorEntityToken debris = Misc.addDebrisField(system, params, rng);
            Vector2f p = Misc.getPointAtRadius(station.getLocation(), 1500f + rng.nextInt(4200));
            debris.setLocation(p.x, p.y);
            debris.setName("未知残骸");
        }
        for (com.fs.starfarer.api.campaign.econ.MarketAPI market : PTSDOccupationManager.getAllMarkets()) {
            if (market == null || market.getStarSystem() != system ||
                    market.isPlanetConditionMarketOnly()) continue;
            String faction = market.getFactionId();
            if (PTSDCrisisFactionModifierAPI.get(faction, system.getId()).immune) continue;
            for (int n = 0; n < 4; n++) {
                Vector2f p = Misc.getPointAtRadius(market.getPrimaryEntity().getLocation(),
                        700f + rng.nextInt(600));
                FleetParamsV3 params = new FleetParamsV3(p, faction, 1f,
                        FleetTypes.PATROL_SMALL, 5f, 0f, 0f, 0f, 0f, 0f, 0f);
                CampaignFleetAPI remnant = FleetFactoryV3.createFleet(params);
                if (remnant == null) continue;
                { system.addEntity(remnant); data.scripts.campaign.invasion.PTSDCampaignRuntime.entityAdded(remnant); }
                remnant.setLocation(p.x, p.y);
                remnant.setName("地方防卫");
                com.fs.starfarer.api.campaign.econ.MarketAPI refuge = nearestRefuge(system, faction);
                if (refuge != null) remnant.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                        refuge.getPrimaryEntity(), 90f, "撤往友好港口");
                else remnant.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                        market.getPrimaryEntity(), 20f, "寻找撤离航路");
            }
            break;
        }
    }

    private static com.fs.starfarer.api.campaign.econ.MarketAPI nearestRefuge(
            StarSystemAPI danger, String faction) {
        com.fs.starfarer.api.campaign.econ.MarketAPI best = null;
        float distance = Float.MAX_VALUE;
        for (com.fs.starfarer.api.campaign.econ.MarketAPI market : PTSDOccupationManager.getAllMarkets()) {
            if (market == null || market.isPlanetConditionMarketOnly() ||
                    market.getStarSystem() == null || market.getStarSystem() == danger ||
                    market.isHidden() || market.getStarSystem().hasTag(Tags.THEME_HIDDEN) ||
                    market.getPrimaryEntity() == null ||
                    market.getFaction().isHostileTo(faction)) continue;
            float d = Misc.getDistance(market.getStarSystem().getLocation(), danger.getLocation());
            if (d < distance) { distance = d; best = market; }
        }
        return best;
    }

    private static void adoptLegacyFortresses(PTSDCrisisState state, float day) {
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            for (CampaignFleetAPI fleet : system.getFleets()) {
                if (!fleet.getMemoryWithoutUpdate().getBoolean("$PTSD_black_hole_fortress")) continue;
                PTSDLateState.Fortress record = new PTSDLateState.Fortress();
                record.id = "fortress:" + system.getId();
                record.originSystemId = system.getId();
                record.currentSystemId = system.getId();
                record.blackHoleId = system.getStar() == null ? null : system.getStar().getId();
                record.fleetId = fleet.getId();
                record.tier = 2;
                record.nextTransportDay = day + 5f;
                record.nextMoveRollDay = day + 30f;
                fleet.getMemoryWithoutUpdate().set("$PTSD_fortress", true);
                boolean listenerFound = false;
                for (com.fs.starfarer.api.campaign.listeners.FleetEventListener listener :
                        fleet.getEventListeners()) {
                    if (listener instanceof PTSDStrategicFleetListener) { listenerFound = true; break; }
                }
                if (!listenerFound) fleet.getEventListeners().add(
                        new PTSDStrategicFleetListener(null, fleet.getFleetPoints()));
                if (state.late.fortresses.containsKey(record.id)) continue;
                state.late.fortresses.put(record.id, record);
                state.late.committedIds.add("fortress-site:" + system.getId());
                PTSDCrisisDevIntel.report("旧要塞接管", system.getName() + "：Stable工程接入新账本",
                        system.getId(), fleet.getId());
            }
        }
    }

    private static void construct(PTSDCrisisState state, StarSystemAPI system, float day) {
        construct(state, system, day, false);
    }

    static void startPhasePair(PTSDCrisisState state, float day) {
        int built = 0;
        for (String id : state.late.committedIds) if (id.startsWith("phase-fortress:")) built++;
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (built >= 2) break;
            if (blackHole(system) == null || isInhabited(system) ||
                    system.hasTag(Tags.THEME_HIDDEN) || system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER) ||
                    state.late.committedIds.contains("fortress-site:" + system.getId())) continue;
            construct(state, system, day, true);
            if (state.late.fortresses.containsKey("fortress:" + system.getId())) {
                state.late.committedIds.add("phase-fortress:" + system.getId());
                state.late.nextFortressBuildDay = Math.max(state.late.nextFortressBuildDay, day + 30f);
                built++;
            }
        }
    }

    private static void construct(PTSDCrisisState state, StarSystemAPI system, float day, boolean phaseGrant) {
        com.fs.starfarer.api.campaign.PlanetAPI anchor = blackHole(system);
        if (anchor == null) return;
        if (state.late.committedIds.contains("fortress-site:" + system.getId()) ||
                (!phaseGrant && (state.late.warPoints < 10f || state.late.strategicReserve < 30f))) return;
        CampaignFleetAPI station = makeFleet("未知设施", VARIANTS[1]);
        if (station == null) return;
        state.late.committedIds.add("fortress-site:" + system.getId());
        { system.addEntity(station); data.scripts.campaign.invasion.PTSDCampaignRuntime.entityAdded(station); }
        station.setCircularOrbitWithSpin(anchor, 0f,
                Math.max(850f, anchor.getRadius() + 550f), 18f, 1f, 2f);
        station.setStationMode(true);
        station.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        station.getMemoryWithoutUpdate().set("$PTSD_fortress", true);
        PTSDLateState.Fortress record = new PTSDLateState.Fortress();
        record.id = "fortress:" + system.getId();
        record.originSystemId = system.getId();
        record.currentSystemId = system.getId();
        record.blackHoleId = anchor.getId();
        record.fleetId = station.getId();
        record.nextTransportDay = day + 4f;
        record.nextMoveRollDay = day + 30f;
        state.late.fortresses.put(record.id, record);
        if (!phaseGrant) {
        state.late.warPoints -= 10f;
        state.late.warPointBelief = Math.max(0f, state.late.warPointBelief - 10f);
        state.late.strategicReserve -= 30f;
        PTSDLateWarDirector.recordSpending(state, "要塞开工 " + record.id, 10f, 30f);
        }
        PTSDCrisisState.SystemData systemData = state.getSystemData(system.getId());
        systemData.blackHoleFortress = true;
        systemData.omegaControl = 1f;
        systemData.humanControl = 0f;
        PTSDCrisisDevIntel.report("黑洞工程开工", system.getName() +
                (phaseGrant ? "：一次性阶段投入，免费开工" : "：准备-10，战略资产-30"), system.getId(), station.getId());
    }

    private static CampaignFleetAPI makeFleet(String name, String variant) {
        try {
            PTSDCrisisState current = PTSDCrisisState.get();
            CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                    PTSDCrisisProgress.getActiveFactionId(current), name, true);
            fleet.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant));
            for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
                member.getStats().getCRLossPerSecondPercent().modifyMult("PTSD_black_hole_work", 0f);
            }
            fleet.setNoFactionInName(true);
            fleet.addTag("PTSD_crisis_fleet");
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
            data.scripts.campaign.cargo.PTSD_OmegaOfficerGeneratorPlugin.repairExistingFleet(fleet);
            fleet.getEventListeners().add(new PTSDStrategicFleetListener(null, fleet.getFleetPoints()));
            return fleet;
        } catch (Throwable ex) {
            Global.getLogger(PTSDFortressDirector.class).warn("Fortress variant unavailable: " + variant, ex);
            return null;
        }
    }

    private static void sendTransport(PTSDCrisisState state, PTSDLateState.Fortress record,
                                      CampaignFleetAPI station, float day) {
        if (state.late.strategicReserve < 12f) return;
        StarSystemAPI system = station.getStarSystem();
        if (system == null) return;
        Vector2f p = Misc.getPointAtRadius(system.getLocation(), 4500f);
        CampaignFleetAPI transport = PTSDEntropyTransportFactory.create(state,
                Global.getSector().getHyperspace(), p.x, p.y, 12f,
                "要塞运输 " + record.id);
        if (transport == null) return;
        transport.getMemoryWithoutUpdate().set("$PTSD_fortress_transport", record.id);
        for (FleetMemberAPI member : transport.getFleetData().getMembersListCopy()) {
            member.getStats().getCRLossPerSecondPercent().modifyMult("PTSD_black_hole_work", 0f);
        }
        transport.addAssignment(FleetAssignment.GO_TO_LOCATION, station, 35f, "从超空间前往工程区");
        record.transportIds.add(transport.getId());
        record.nextTransportDay = day + IIRT_Omega_ModPlugin.lateTransportInterval[record.tier];
    }

    private static void processTransports(PTSDCrisisState state, PTSDLateState.Fortress record,
                                          CampaignFleetAPI station, float day) {
        for (String id : new ArrayList<String>(record.transportIds)) {
            CampaignFleetAPI transport = findFleet(id);
            if (transport == null || transport.getFleetData().getMembersListCopy().isEmpty()) {
                record.transportIds.remove(id);
                record.lostTransports++;
                record.blockedUntilDay = Math.max(record.blockedUntilDay, day + 8f);
                PTSDLateWarDirector.disruptProduction(state, "transport:" + id, 6f, 0f,
                        record.currentSystemId);
                continue;
            }
            if (transport.getContainingLocation() == station.getContainingLocation() &&
                    Misc.getDistance(transport, station) < 650f) {
                record.transportIds.remove(id);
                transport.getContainingLocation().removeEntity(transport);
                record.deliveredTransports++;
                record.tierProgress += 10f * IIRT_Omega_ModPlugin.lateConstructionSpeed;
                PTSDLateWarDirector.creditStrategicAssets(state, "transport-delivered:" + id,
                        5f, "要塞运输任务完成", record.currentSystemId);
                if (record.tierProgress >= IIRT_Omega_ModPlugin.lateFortressWork[record.tier])
                    upgrade(state, record, station, day);
            }
        }
        if (record.tier < 4 && record.tierProgress >=
                IIRT_Omega_ModPlugin.lateFortressWork[record.tier])
            upgrade(state, record, station, day);
    }

    private static void upgrade(PTSDCrisisState state, PTSDLateState.Fortress record,
                                CampaignFleetAPI station, float day) {
        if (state.late.warPoints < 5f) return;
        FleetMemberAPI next;
        try {
            next = Global.getFactory().createFleetMember(FleetMemberType.SHIP, VARIANTS[record.tier + 1]);
        } catch (Throwable ex) {
            Global.getLogger(PTSDFortressDirector.class).warn("Upgrade variant unavailable", ex);
            return;
        }
        state.late.warPoints -= 5f;
        state.late.warPointBelief = Math.max(0f, state.late.warPointBelief - 5f);
        PTSDLateWarDirector.recordSpending(state, "要塞升级 " + record.id, 5f, 0f);
        record.tierProgress = 0f;
        record.tier++;
        FleetMemberAPI old = station.getFleetData().getMembersListCopy().get(0);
        next.getRepairTracker().setCR(Math.min(next.getRepairTracker().getMaxCR(), old.getRepairTracker().getCR()));
        next.getStats().getCRLossPerSecondPercent().modifyMult("PTSD_black_hole_work", 0f);
        station.getFleetData().removeFleetMember(old);
        station.getFleetData().addFleetMember(next);
        station.setName(record.tier == 4 ? "节点" : "未知设施·" + record.tier);
        PTSDCrisisDevIntel.report("黑洞工程升级", record.id + " → " + VARIANTS[record.tier] +
                "；准备-5",
                record.currentSystemId, station.getId());
        if (record.tier == 4) {
            record.collapsed = true;
            collapseBlackHole(state.resolveSystem(record.originSystemId));
            PTSDLateWarDirector.enqueueAlert(state, record.id + ":complete", record.originSystemId,
                    "天文台记录：原黑洞坐标仍能定位；视界读数已不属于任何已知天体。", day);
        }
    }

    private static void collapseBlackHole(StarSystemAPI system) {
        com.fs.starfarer.api.campaign.PlanetAPI anchor = blackHole(system);
        if (anchor == null) return;
        // Preserve the entity and all IDs; only the local visual/corona configuration changes.
        anchor.getSpec().setBlackHole(false);
        anchor.getSpec().setCoronaSize(0f);
        anchor.getSpec().setScaleMultMapIcon(0f);
        anchor.getSpec().setScaleMultStarscapeIcon(0f);
        try {
            Global.getSettings().loadTexture("graphics/fx/empty.png");
            anchor.getSpec().setTexture("graphics/fx/empty.png");
            anchor.getSpec().setGlowTexture("graphics/fx/empty.png");
            anchor.getSpec().setCoronaTexture("graphics/fx/empty.png");
        } catch (Exception ex) {
            Global.getLogger(PTSDFortressDirector.class).warn("Unable to hide former black hole", ex);
        }
        for (com.fs.starfarer.api.campaign.CampaignTerrainAPI terrain : system.getTerrainCopy()) {
            if (terrain.getOrbitFocus() == anchor &&
                    terrain.getPlugin() instanceof com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin) {
                { data.scripts.campaign.invasion.PTSDCampaignRuntime.leaving(terrain); system.removeEntity(terrain); }
            }
        }
        anchor.getSpec().setGlowColor(new Color(115, 15, 140, 0));
        anchor.getSpec().setCoronaColor(new Color(60, 0, 80, 0));
        anchor.applySpecChanges();
        for (com.fs.starfarer.api.campaign.SectorEntityToken entity : system.getAllEntities()) {
            if (entity == anchor) continue;
            if (entity.getOrbitFocus() == anchor) entity.setOrbit(null);
        }
        system.getMemoryWithoutUpdate().set("$PTSD_collapsed_black_hole", true);
    }

    private static void ensurePatrols(PTSDCrisisState state, PTSDLateState.Fortress record,
                                      CampaignFleetAPI station) {
        if (station.getStarSystem() == null) return;
        int desired = record.tier == 4 ? 8 : Math.min(3, record.tier);
        for (String id : new ArrayList<String>(record.patrolIds)) if (findFleet(id) == null) record.patrolIds.remove(id);
        for (String id : record.patrolIds) PTSDControlledPatrolDirector.installNodePatrol(findFleet(id));
        while (record.patrolIds.size() < desired && state.late.strategicReserve >=
                (record.patrolIds.size() < 5 ? 30f : 70f)) {
            int index = record.patrolIds.size();
            CampaignFleetAPI patrol = makeFleet(index < 5 ? "集群" : "视界原罪群",
                    index < 5 ? "Omega_Scrap_Heap_Garbage" : "IIRT_Omega_Singularity_Attack");
            if (patrol == null) break;
            if (index < 5) {
                for (int n = 1; n < 5; n++) patrol.getFleetData().addFleetMember(
                        Global.getFactory().createFleetMember(FleetMemberType.SHIP, "Omega_Scrap_Heap_Garbage"));
            } else {
                patrol.getFleetData().addFleetMember(Global.getFactory().createFleetMember(
                        FleetMemberType.SHIP, "IIRT_Omega_Spectrum_Assault"));
                patrol.getFleetData().addFleetMember(Global.getFactory().createFleetMember(
                        FleetMemberType.SHIP, "IIRT_Omega_Spectrum_Assault"));
            }
            data.scripts.campaign.cargo.PTSD_OmegaOfficerGeneratorPlugin.repairExistingFleet(patrol);
            station.getContainingLocation().addEntity(patrol);
            patrol.setLocation(station.getLocation().x + 800f + index * 100f,
                    station.getLocation().y + 800f);
            patrol.addAssignment(FleetAssignment.PATROL_SYSTEM, station, 10000f, "维护设施");
            PTSDControlledPatrolDirector.installNodePatrol(patrol);
            record.patrolIds.add(patrol.getId());
            state.late.strategicReserve -= index < 5 ? 30f : 70f;
        }
    }

    private static void moveIfOrdered(PTSDCrisisState state, PTSDLateState.Fortress record,
                                      CampaignFleetAPI station, float day, boolean forced) {
        if ((!forced && day < record.nextMoveRollDay) || record.inTransit ||
                (state.phase != PTSDCrisisState.Phase.WAR && record.movedPrewar)) return;
        record.nextMoveRollDay = day + (state.phase == PTSDCrisisState.Phase.WAR ?
                30f + (float) Math.random() * 30f : 30f);
        if ((!forced && Math.random() >= .1) || state.late.strategicReserve < 50f) return;
        StarSystemAPI target = null;
        float distance = Float.MAX_VALUE;
        for (StarSystemAPI candidate : Global.getSector().getStarSystems()) {
            if (candidate.getId().equals(record.currentSystemId)) continue;
            if (candidate.hasTag(Tags.THEME_HIDDEN) ||
                    candidate.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)) continue;
            boolean protectedSystem = false;
            for (com.fs.starfarer.api.campaign.econ.MarketAPI market : PTSDOccupationManager.getAllMarkets()) {
                if (market != null && market.getStarSystem() == candidate &&
                        PTSDCrisisFactionModifierAPI.get(market.getFactionId(), candidate.getId()).immune) {
                    protectedSystem = true;
                    break;
                }
            }
            if (protectedSystem) continue;
            PTSDCrisisState.SystemData data = state.getSystemData(candidate.getId());
            boolean omega = data.omegaControl >= .5f;
            if (state.phase == PTSDCrisisState.Phase.WAR ? omega : !omega) continue;
            float d = Misc.getDistance(candidate.getLocation(), station.getStarSystem().getLocation());
            if (d < distance) { distance = d; target = candidate; }
        }
        if (target == null) return;
        record.destinationSystemId = target.getId();
        record.lastLocationId = record.currentSystemId;
        record.inTransit = true;
        record.transitProgress = 0f;
        record.transitDistance = Math.max(1f, distance);
        record.lastTravelDay = day;
        state.late.strategicReserve -= 50f;
        station.setStationMode(false);
        station.getMemoryWithoutUpdate().unset(MemFlags.MEMORY_KEY_NO_JUMP);
        station.setOrbit(null);
        station.clearAssignments();
        station.getContainingLocation().removeEntity(station);
        Global.getSector().getHyperspace().addEntity(station);
        StarSystemAPI origin = state.resolveSystem(record.lastLocationId);
        if (origin != null) station.setLocation(origin.getLocation().x, origin.getLocation().y);
        for (String escortId : record.patrolIds) {
            CampaignFleetAPI escort = findFleet(escortId);
            if (escort == null || escort.getContainingLocation() == null) continue;
            escort.clearAssignments();
            escort.getContainingLocation().removeEntity(escort);
            Global.getSector().getHyperspace().addEntity(escort);
            escort.setLocation(station.getLocation().x + 350f, station.getLocation().y + 350f);
        }
        PTSDLateWarDirector.enqueueAlert(state, record.id + ":move:" + Math.round(day), record.currentSystemId,
                "视界附近的常驻警备群正在撤离；要塞航迹指向 " + target.getName() + "。", day);
        PTSDLateEventDirector.force(state, "WR-15");
    }

    private static void advanceTransit(PTSDCrisisState state, PTSDLateState.Fortress record,
                                       CampaignFleetAPI station, float day) {
        StarSystemAPI origin = state.resolveSystem(record.lastLocationId);
        StarSystemAPI target = state.resolveSystem(record.destinationSystemId);
        if (origin == null || target == null) return;
        float elapsed = Math.max(0f, Math.min(30f, day - record.lastTravelDay));
        record.lastTravelDay = day;
        record.transitProgress = Math.min(record.transitDistance,
                record.transitProgress + elapsed * Math.max(350f, record.transitSpeed * 120f));
        float part = record.transitProgress / record.transitDistance;
        float x = origin.getLocation().x + (target.getLocation().x - origin.getLocation().x) * part;
        float y = origin.getLocation().y + (target.getLocation().y - origin.getLocation().y) * part;
        station.setLocation(x, y);
        int escortIndex = 0;
        for (String escortId : record.patrolIds) {
            CampaignFleetAPI escort = findFleet(escortId);
            if (escort != null && escort.getContainingLocation() == Global.getSector().getHyperspace()) {
                escort.setLocation(x + 280f + escortIndex * 100f, y + 280f);
            }
            escortIndex++;
        }
        record.lastHyperX = x;
        record.lastHyperY = y;
        if (part >= 1f) {
            Global.getSector().getHyperspace().removeEntity(station);
            { target.addEntity(station); data.scripts.campaign.invasion.PTSDCampaignRuntime.entityAdded(station); }
            station.setLocation(2500f, 2500f);
        }
    }

    public static void advanceDetonation(PTSDCrisisState state) {
        // Real-time countdown is advanced by the campaign/combat plugin; daily strategy only finalizes state.
        if (state == null || state.late == null) return;
        for (PTSDLateState.Detonation d : state.late.detonations.values()) {
            if (d != null && !d.exploded && d.remainingSeconds <= 0f) {
                PTSDFortressEscapeScript.explode(state, d, Global.getSector().getPlayerFleet());
                PTSDLateWarDirector.enqueueAlert(state, "detonation:" + d.fortressId, d.systemId,
                        "原要塞所在星系的市场登记和轨道资料同时失效。", PTSDCrisisState.getDay());
            }
        }
    }

    /** The combat plugin and post-battle listener converge on the same saved countdown. */
    public static PTSDLateState.Detonation beginCombatDetonation(PTSDCrisisState state, String fortressId) {
        if (state == null || state.late == null || fortressId == null) return null;
        PTSDLateState.Fortress record = state.late.fortresses.get(fortressId);
        if (record == null || record.destroyed || record.tier < 4 || record.inTransit) return null;
        PTSDLateState.Detonation d = state.late.detonations.get(fortressId);
        if (d != null) return d;
        d = new PTSDLateState.Detonation();
        d.fortressId = fortressId;
        d.systemId = record.currentSystemId;
        d.combatStarted = true;
        PTSDFortressCollapse.initialize(d);
        state.late.detonation = d;
        state.late.detonations.put(fortressId, d);
        StarSystemAPI system = state.resolveSystem(d.systemId);
        Global.getSector().getCampaignUI().addMessage(
                "视界要塞失稳：黑洞正在逆向坍缩，立即远离破裂波面！", Color.RED);
        return d;
    }

    private static void onDestroyed(PTSDCrisisState state, PTSDLateState.Fortress record,
                                    float day, boolean playerVictory) {
        if (record.detonationHandled) return;
        record.destroyed = true;
        record.detonationHandled = true;
        state.getSystemData(record.originSystemId).blackHoleFortress = false;
        if (!record.originSystemId.equals(state.baseSystemId)) {
            state.getSystemData(record.originSystemId).omegaControl = 0f;
        }
        PTSDLateWarDirector.disruptProduction(state, record.id + ":destroyed", 25f, 25f,
                record.currentSystemId);
        if (playerVictory) {
            PTSDThreatDirector.recordAction(state, "FORTRESS", record.id, record.currentSystemId,
                    100f, .95f, true, 180f);
            PTSDGeneralStaffDirector.recordContribution(state, "fortress:" + record.id, "FORTRESS",
                    "player", record.currentSystemId, 100f);
        }
        if (record.tier < 4) return;
        if (record.inTransit) {
            makeHyperspaceRuin(state, record);
            return;
        }
        PTSDLateEventDirector.force(state, "WR-16");
        PTSDLateState.Detonation d = state.late.detonations.get(record.id);
        if (d == null) {
            d = state.late.detonation;
        }
        if (d == null || !record.currentSystemId.equals(d.systemId)) {
            d = new PTSDLateState.Detonation();
            d.systemId = record.currentSystemId;
        }
        d.fortressId = record.id;
        PTSDFortressCollapse.initialize(d);
        state.late.detonations.put(record.id, d);
        StarSystemAPI detonationSystem = state.resolveSystem(d.systemId);
        boolean playerPresent = Global.getSector().getPlayerFleet() != null &&
                Global.getSector().getPlayerFleet().getStarSystem() != null &&
                record.currentSystemId.equals(Global.getSector().getPlayerFleet().getStarSystem().getId());
        if (playerPresent) {
            state.late.detonation = d;
            if (!d.combatStarted)
                Global.getSector().getCampaignUI().addMessage("视界要塞已失稳：远离正在扩张的破裂波面！", Color.RED);
        } else {
            d.collapseElapsed=PTSDFortressCollapseVisual.DURATION;d.remainingSeconds = 0f;
            state.late.detonation = d;
        }
    }

    public static void onFleetDefeated(PTSDCrisisState state, CampaignFleetAPI fleet,
                                       boolean playerVictory) {
        if (state == null || state.late == null || fleet == null) return;
        for (PTSDLateState.Fortress record : state.late.fortresses.values()) {
            if (record == null || record.destroyed || !fleet.getId().equals(record.fleetId)) continue;
            if (fleet.getContainingLocation() == Global.getSector().getHyperspace()) {
                record.inTransit = true;
                record.lastHyperX = fleet.getLocation().x;
                record.lastHyperY = fleet.getLocation().y;
            } else if (fleet.getStarSystem() != null) {
                record.currentSystemId = fleet.getStarSystem().getId();
            }
            onDestroyed(state, record, PTSDCrisisState.getDay(), playerVictory);
            if (fleet.getContainingLocation() != null) fleet.getContainingLocation().removeEntity(fleet);
            break;
        }
    }

    private static void makeHyperspaceRuin(PTSDCrisisState state, PTSDLateState.Fortress record) {
        if (!state.late.generatedCollapseSystems.add(record.id)) return;
        try {
            String name = "视界残址 " + (state.late.generatedCollapseSystems.size());
            StarSystemAPI ruin = Global.getSector().createStarSystem(name);
            ruin.getLocation().set(record.lastHyperX, record.lastHyperY);
            com.fs.starfarer.api.campaign.PlanetAPI hole = ruin.initStar(
                    "PTSD_collapse_" + state.late.generatedCollapseSystems.size(),
                    com.fs.starfarer.api.impl.campaign.ids.StarTypes.BLACK_HOLE,
                    450f, 1200f, 1f, 0f, 2f);
            ruin.addAsteroidBelt(hole, 250, 3200f, 1300f, 50f, 120f);
            ruin.autogenerateHyperspaceJumpPoints(true, true);
            CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            if (player != null && player.getContainingLocation() == Global.getSector().getHyperspace()) {
                float dx = player.getLocation().x - record.lastHyperX;
                float dy = player.getLocation().y - record.lastHyperY;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                if (distance < 3500f) {
                    float length = Math.max(1f, distance);
                    player.setLocation(player.getLocation().x + dx / length * 4500f,
                            player.getLocation().y + dy / length * 4500f);
                    for (FleetMemberAPI member : player.getFleetData().getMembersListCopy()) {
                        member.getRepairTracker().setCR(Math.max(0f,
                                member.getRepairTracker().getCR() - .45f));
                    }
                }
            }
            PTSDLateWarDirector.enqueueAlert(state, record.id + ":hyper-ruin", ruin.getId(),
                    "超空间航图新增一处黑洞残址。它的两个跳跃点均记录到要塞最后的航迹。",
                    PTSDCrisisState.getDay());
        } catch (Throwable ex) {
            Global.getLogger(PTSDFortressDirector.class).warn("Unable to create fortress hyperspace ruin", ex);
        }
    }

    public static CampaignFleetAPI findFleet(String id) {
        if (id == null || Global.getSector() == null) return null;
        for (com.fs.starfarer.api.campaign.LocationAPI location : Global.getSector().getAllLocations()) {
            for (CampaignFleetAPI fleet : location.getFleets()) if (id.equals(fleet.getId())) return fleet;
        }
        return null;
    }
}
