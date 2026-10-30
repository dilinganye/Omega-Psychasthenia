package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import data.scripts.IIRT_Omega_ModPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

/** Late story cards use the same persistent news stream, with separate, expiring player orders. */
public final class PTSDLateEventDirector {
    private static final class Definition {
        String id, title, report, intercept, first, second;
        PTSDCrisisState.Phase phase;
        float day;
        boolean shortCard;
        Definition(String id, PTSDCrisisState.Phase phase, float day, String title,
                   String report, String intercept, String first, String second) {
            this.id = id; this.phase = phase; this.day = day; this.title = title;
            this.report = report; this.intercept = intercept;
            this.first = first; this.second = second;
            this.shortCard = first == null;
        }
    }
    private static final Map<String, Definition> DEFINITIONS = new LinkedHashMap<String, Definition>();
    private PTSDLateEventDirector() { }
    private static void ensureDefinitions() {
        if (!DEFINITIONS.isEmpty()) return;
        for (PTSDCrisisState.Phase phase : PTSDCrisisState.Phase.values())
            for (PTSDCrisisIncidentManager.ScriptedTemplate row :
                    PTSDCrisisIncidentManager.getTemplates("SCRIPTED", phase)) {
                if (DEFINITIONS.containsKey(row.id)) continue;
                DEFINITIONS.put(row.id, new Definition(row.id, phase, row.minDay,
                        row.title, row.report, row.intercept,
                        row.choice1.isEmpty() ? null : row.choice1,
                        row.choice2.isEmpty() ? null : row.choice2));
            }
    }

    public static void advance(PTSDCrisisState state, float day) {
        ensureDefinitions();
        if (state == null || state.late == null) return;
        PTSDFTDirector.advance(state, day);
        for (PTSDLateState.EventCard card : state.late.eventCards) {
            if (card == null || card.resolved) continue;
            if (card.accepted) {
                advanceOrder(state, card, day);
            } else if (day >= card.expiresDay) {
                card.resolved = true;
                card.outcome = "时限届满；各方按既有部署继续行动";
                PTSDCrisisDevIntel.report("后期事件过期", card.id + "：未下达命令",
                        card.systemId, card.targetEntityId);
            }
        }
        if (day < state.late.nextCardDay) return;
        for (Definition definition : DEFINITIONS.values()) {
            if (definition.id.startsWith("FT-")) continue;
            if (definition.phase != state.phase || day - state.phaseStartedDay < definition.day ||
                    state.late.completedCards.contains(definition.id) || !eligible(state, definition.id)) continue;
            publish(state, definition, day);
            state.late.nextCardDay = day + (definition.shortCard ?
                    IIRT_Omega_ModPlugin.lateEventShortCooldown :
                    IIRT_Omega_ModPlugin.lateEventMajorCooldown);
            break;
        }
    }

    public static boolean force(PTSDCrisisState state, String id) {
        if (id != null && id.startsWith("FT-")) return PTSDFTDirector.force(state, id);
        ensureDefinitions();
        String reason = unavailableReason(state, id);
        if (!"可触发".equals(reason)) {
            PTSDCrisisDevIntel.report("后期事件未投放", id + "：" + reason,
                    state == null ? null : state.baseSystemId, null);
            return false;
        }
        Definition definition = DEFINITIONS.get(id);
        if (definition != null) {
            publish(state, definition, PTSDCrisisState.getDay());
            return true;
        }
        return false;
    }

    public static String unavailableReason(PTSDCrisisState state, String id) {
        if (id != null && id.startsWith("FT-")) return PTSDFTDirector.unavailableReason(state, id);
        ensureDefinitions();
        if (state == null || state.late == null) return "危机状态不存在";
        Definition definition = DEFINITIONS.get(id);
        if (definition == null) return "CSV 中没有此后期事件";
        if (definition.phase != state.phase) return "当前阶段为 " + state.phase + "，需要 " + definition.phase;
        if (state.late.completedCards.contains(id)) return "该事件已经投放";
        if ("MB-04".equals(id) && (!state.prewarHunterSpawned || state.prewarHunterResolved))
            return "高价值目标截获舰尚未部署或已解决";
        if (("FT-02".equals(id) || "FT-07".equals(id)) && state.late.fortresses.isEmpty())
            return "尚无现存黑洞要塞工程";
        if ("FT-09".equals(id)) {
            boolean lost = false;
            for (PTSDLateState.Fortress fortress : state.late.fortresses.values())
                if (fortress != null && fortress.lostTransports > 0) lost = true;
            if (!lost) return "尚无可核实的施工运输损失";
        }
        if (("FT-10".equals(id) || "WR-08".equals(id)) &&
                state.late.warPointBelief - state.late.warPoints < 15f)
            return "估计准备点尚未高出真实点至少15";
        if (("WR-05".equals(id) || "WR-GS-03".equals(id))) {
            boolean known = false;
            for (Integer visits : state.late.witnessedSupplyVisits.values())
                if (visits != null && visits >= 2) known = true;
            if (!known) return "尚未取得同一补给港两次目击记录";
        }
        if ("FT-03".equals(id)) {
            boolean passed = false;
            for (Integer count : state.late.witnessedPasses.values()) if (count != null && count >= 3) passed = true;
            if (!passed) return "尚无同星系三次可核对通行记录";
        }
        if (("FT-04".equals(id) || "FT-05".equals(id)) &&
                PTSDThreatDirector.localThreat(state, state.baseSystemId, PTSDCrisisState.getDay()) <= 15f)
            return "母星附近尚无足够玩家威胁记录";
        if (id.startsWith("WR-") && !definition.shortCard &&
                !"WR-08".equals(id) && !"WR-15".equals(id) && !"WR-16".equals(id) &&
                !"WR-JE-01".equals(id) && !"WR-09".equals(id)) {
            boolean active = false;
            for (PTSDLateState.Front front : state.late.fronts.values())
                if (front != null && !front.resolved) active = true;
            if (!active) return "当前没有正在交战的战区";
        }
        if ("WR-15".equals(id)) {
            boolean moving = false;
            for (PTSDLateState.Fortress fortress : state.late.fortresses.values())
                if (fortress != null && fortress.inTransit) moving = true;
            if (!moving) return "没有正在移动的完整要塞";
        }
        if ("WR-16".equals(id) && state.late.detonation == null) return "没有正在倒计时的要塞坍缩";
        if ("WR-GS-01".equals(id) && !state.late.staffEstablished) return "联合总参部尚未成立";
        if ("WR-GS-02".equals(id) && state.late.reserves.isEmpty()) return "没有现存后备队";
        if ("WR-GS-04".equals(id) && PTSDGeneralStaffDirector.playerScore(state) < 160f)
            return "玩家已核实贡献低于160";
        if ("WR-JE-01".equals(id) && (!state.jeIntroCompleted || state.jeDead || state.globalPanic < 65f))
            return "需要 Je 在场且全局恐慌至少65";
        if ("WR-09".equals(id) && state.late.playerVictoryFPBySystem.isEmpty())
            return "尚无可核对的玩家战区胜利记录";
        if (!eligible(state, id)) return "对应战区、目标或敌方证据尚未成立";
        return "可触发";
    }

    /** Stable ID/title catalog for the Dev console; production triggers remain in advance(). */
    public static Map<String, String> devCardsForPhase(PTSDCrisisState.Phase phase) {
        ensureDefinitions();
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (Definition definition : DEFINITIONS.values()) {
            if (definition.phase == phase) result.put(definition.id, definition.title);
        }
        if (phase == PTSDCrisisState.Phase.MOBILIZATION)
            result.put("FT-10", "舰内临时定义");
        return result;
    }

    private static boolean eligible(PTSDCrisisState state, String id) {
        if ("MB-04".equals(id)) return state.prewarHunterSpawned && !state.prewarHunterResolved;
        if (id.startsWith("WR-") && DEFINITIONS.get(id) != null &&
                !DEFINITIONS.get(id).shortCard &&
                !"WR-08".equals(id) && !"WR-15".equals(id) &&
                !"WR-16".equals(id) && !"WR-JE-01".equals(id)) {
            boolean active = false;
            for (PTSDLateState.Front front : state.late.fronts.values())
                if (front != null && !front.resolved) { active = true; break; }
            if (!active && !"WR-09".equals(id)) return false;
        }
        if ("WR-05".equals(id) || "WR-GS-03".equals(id)) {
            for (Integer visits : state.late.witnessedSupplyVisits.values())
                if (visits != null && visits >= 2) return true;
            return false;
        }
        if ("FT-03".equals(id)) {
            for (Integer passes : state.late.witnessedPasses.values()) if (passes != null && passes >= 3) return true;
            return false;
        }
        if ("FT-02".equals(id) || "FT-07".equals(id)) return !state.late.fortresses.isEmpty();
        if ("FT-09".equals(id)) for (PTSDLateState.Fortress f : state.late.fortresses.values()) if (f.lostTransports > 0) return true;
        if ("FT-09".equals(id)) return false;
        if ("FT-10".equals(id)) return state.late.warPointBelief - state.late.warPoints >= 15f;
        if ("FT-04".equals(id) || "FT-05".equals(id)) return
                PTSDThreatDirector.localThreat(state, state.baseSystemId, PTSDCrisisState.getDay()) > 15f;
        if ("WR-15".equals(id)) for (PTSDLateState.Fortress f : state.late.fortresses.values()) if (f.inTransit) return true;
        if ("WR-15".equals(id)) return false;
        if ("WR-16".equals(id)) return state.late.detonation != null;
        if ("WR-GS-01".equals(id)) return state.late.staffEstablished;
        if ("WR-GS-02".equals(id)) return !state.late.reserves.isEmpty();
        if ("WR-GS-04".equals(id)) return PTSDGeneralStaffDirector.playerScore(state) >= 160f;
        if ("WR-JE-01".equals(id)) return state.jeIntroCompleted && !state.jeDead &&
                state.globalPanic >= 65f;
        if ("WR-09".equals(id)) return !state.late.playerVictoryFPBySystem.isEmpty();
        if ("WR-08".equals(id)) return state.late.warPointBelief > state.late.warPoints + 15f;
        return true;
    }

    private static void publish(PTSDCrisisState state, Definition def, float day) {
        PTSDLateState.EventCard card = new PTSDLateState.EventCard();
        card.id = def.id; card.title = def.title; card.report = def.report;
        card.intercept = def.intercept; card.firstChoice = def.first;
        card.secondChoice = def.second; card.shortCard = def.shortCard;
        card.createdDay = day; card.expiresDay = day + 12f;
        card.systemId = state.baseSystemId;
        if (def.id.startsWith("FT-02") || def.id.startsWith("FT-07") ||
                def.id.startsWith("FT-09") || "WR-15".equals(def.id)) {
            for (PTSDLateState.Fortress fortress : state.late.fortresses.values()) {
                if (fortress == null || fortress.destroyed) continue;
                card.systemId = fortress.currentSystemId;
                card.targetEntityId = fortress.fleetId;
                if (("FT-02".equals(def.id) || "FT-09".equals(def.id)) &&
                        !fortress.transportIds.isEmpty())
                    card.targetEntityId = fortress.transportIds.get(0);
                break;
            }
        } else if (state.phase == PTSDCrisisState.Phase.WAR) {
            for (PTSDLateState.Front front : state.late.fronts.values()) {
                if (front == null || front.resolved) continue;
                card.systemId = front.systemId;
                card.targetEntityId = front.marketId;
                card.targetFrontId = front.id;
                break;
            }
            if (card.targetFrontId == null && !state.late.fronts.isEmpty()) {
                PTSDLateState.Front latest = null;
                for (PTSDLateState.Front front : state.late.fronts.values()) if (front != null) latest = front;
                if (latest != null) {
                    card.systemId = latest.systemId;
                    card.targetEntityId = latest.marketId;
                    card.targetFrontId = latest.id;
                }
            }
        } else if (state.phase == PTSDCrisisState.Phase.MOBILIZATION) {
            for (MarketAPI market : PTSDOccupationManager.getAllMarkets()) {
                if (market == null || market.isPlanetConditionMarketOnly() ||
                        market.getStarSystem() == null ||
                        PTSDCrisisFactionModifierAPI.get(market.getFactionId(),
                                market.getStarSystem().getId()).immune) continue;
                card.systemId = market.getStarSystem().getId();
                card.targetEntityId = market.getId();
                break;
            }
        } else if ("FT-01".equals(def.id) || "FT-06".equals(def.id)) {
            for (MarketAPI market : PTSDOccupationManager.getAllMarkets()) {
                if (market == null || market.isPlanetConditionMarketOnly() ||
                        market.getStarSystem() == null ||
                        IIRT_Omega_Invasion.WATCHER_FACTION.equals(market.getFactionId()) ||
                        IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(market.getFactionId())) continue;
                card.systemId = market.getStarSystem().getId();
                card.targetEntityId = market.getId();
                break;
            }
        }
        if ("MB-04".equals(def.id)) {
            for (PTSDCrisisState.StrategicEvent event : state.events) {
                if (event != null && event.type == PTSDCrisisState.EventType.PREWAR_HUNTER &&
                        event.status != PTSDCrisisState.EventStatus.RESOLVED) {
                    card.systemId = event.targetSystemId;
                    card.targetEntityId = event.materializedFleetId;
                    break;
                }
            }
        }
        if ("FT-03".equals(def.id)) {
            for (Map.Entry<String, Integer> entry : state.late.witnessedPasses.entrySet())
                if (entry.getValue() != null && entry.getValue() >= 3 &&
                        state.resolveSystem(entry.getKey()) != null) {
                    card.systemId = entry.getKey();
                    break;
                }
        }
        if ("WR-16".equals(def.id) && state.late.detonation != null)
            card.systemId = state.late.detonation.systemId;
        state.late.eventCards.add(card);
        state.late.completedCards.add(def.id);
        PTSDCrisisIncidentManager.publishScripted(state, def.id, card.systemId, card.targetEntityId,
                state.phase == PTSDCrisisState.Phase.WAR ? "全面战争" : "要塞与动员", !def.shortCard);
        if (!def.shortCard) PTSDLateEventIntel.ensureIntel();
    }

    public static boolean choose(PTSDCrisisState state, String id, int choice) {
        if (id != null && id.startsWith("FT-")) return false;
        if (state == null || state.late == null || (choice != 1 && choice != 2)) return false;
        PTSDLateState.EventCard card = null;
        for (PTSDLateState.EventCard candidate : state.late.eventCards) {
            if (candidate != null && id.equals(candidate.id)) { card = candidate; break; }
        }
        if (card == null || card.shortCard || card.resolved || card.accepted ||
                PTSDCrisisState.getDay() >= card.expiresDay) return false;
        String label = choice == 1 ? card.firstChoice : card.secondChoice;
        String kind = PTSDTaskRules.lateOrder(id, choice);
        if (kind == null) return reject(state, card, "该事件选项尚未注册行动规则");
        if ("FT-05".equals(id) && choice == 1) {
            for (PTSDCrisisState.SystemData data : state.systems.values()) {
                if (data != null && data.systemId != null && data.omegaControl >= .5f &&
                        !data.systemId.equals(state.baseSystemId) &&
                        state.resolveSystem(data.systemId) != null) {
                    card.systemId = data.systemId;
                    card.targetEntityId = null;
                    break;
                }
            }
        }
        if (!"REMOTE".equals(kind)) {
            if ("LEAVE".equals(kind) || "ROUTE".equals(kind)) {
                CampaignFleetAPI player = Global.getSector().getPlayerFleet();
                if (player == null || player.getStarSystem() == null ||
                        !card.systemId.equals(player.getStarSystem().getId()))
                    return reject(state, card, "撤离任务须在指定星系内下达");
            }
            if ("TRANSPORT".equals(kind)) {
                String transport = findTransport(state, card.systemId);
                if (transport == null) return reject(state, card, "尚无可截击的实际施工运输舰");
                card.targetEntityId = transport;
            } else if ("BATTLE".equals(kind) && !createBattleTarget(state, card)) {
                return reject(state, card, "未能从真实战略预算中部署可拦截目标舰队");
            } else if ("ESCORT".equals(kind) && !createEscort(state, card)) {
                return reject(state, card, "目标殖民地或可用跳跃点不存在，无法组织护航");
            } else if ("DEFEND".equals(kind) && frontFor(state, card) == null &&
                    state.phase == PTSDCrisisState.Phase.WAR) {
                return reject(state, card, "该战区已不再处于攻防中");
            }
            card.accepted = true;
            card.selectedChoice = choice;
            card.orderKind = kind;
            card.orderDeadlineDay = PTSDCrisisState.getDay() + ("WR-16".equals(id) ?
                    IIRT_Omega_ModPlugin.lateEventTaskDeadline :
                    Math.max(30f, IIRT_Omega_ModPlugin.lateEventTaskDeadline));
            card.outcome = label + "：命令已下达；前往 " + locationName(state, card.systemId) +
                    "，在约" + Math.round(card.orderDeadlineDay - PTSDCrisisState.getDay()) +
                    "日内完成现场行动。";
        } else {
            String result = settleRemote(state, card, choice, label);
            if (result == null) return false;
            card.resolved = true;
            card.selectedChoice = choice;
            card.outcome = result;
        }
        PTSDCrisisDevIntel.report("后期事件选择", id + "：" + card.outcome,
                card.systemId, card.targetEntityId);
        return true;
    }

    private static boolean reject(PTSDCrisisState state, PTSDLateState.EventCard card, String reason) {
        PTSDCrisisDevIntel.report("后期事件未执行", card.id + "：" + reason,
                card.systemId, card.targetEntityId);
        Global.getSector().getCampaignUI().addMessage("[危机命令] " + reason);
        return false;
    }

    private static String findTransport(PTSDCrisisState state, String systemId) {
        for (PTSDLateState.Fortress fortress : state.late.fortresses.values()) {
            if (fortress == null || fortress.destroyed ||
                    !systemId.equals(fortress.currentSystemId)) continue;
            for (String id : fortress.transportIds) {
                if (Global.getSector().getEntityById(id) instanceof CampaignFleetAPI) return id;
            }
        }
        return null;
    }

    private static PTSDLateState.Front frontFor(PTSDCrisisState state, PTSDLateState.EventCard card) {
        PTSDLateState.Front front = state.late.fronts.get(card.targetFrontId);
        return front != null && !front.resolved ? front : null;
    }

    private static String locationName(PTSDCrisisState state, String id) {
        StarSystemAPI system = state.resolveSystem(id);
        return system == null ? "指定星系" : system.getName();
    }

    private static boolean createEscort(PTSDCrisisState state, PTSDLateState.EventCard card) {
        StarSystemAPI system = state.resolveSystem(card.systemId);
        if (system == null || system.getJumpPoints().isEmpty()) return false;
        MarketAPI market = state.resolveMarket(card.targetEntityId);
        if (market == null || market.isPlanetConditionMarketOnly() ||
                market.getPrimaryEntity() == null || market.getStarSystem() != system) {
            for (MarketAPI candidate : PTSDOccupationManager.getAllMarkets()) {
                if (candidate != null && candidate.getStarSystem() == system &&
                        !candidate.isPlanetConditionMarketOnly()) { market = candidate; break; }
            }
        }
        boolean lastSurvivors = "WR-16".equals(card.id) &&
                (market == null || market.isPlanetConditionMarketOnly() ||
                        market.getPrimaryEntity() == null || market.getStarSystem() != system);
        if (!lastSurvivors && (market == null || market.isPlanetConditionMarketOnly() ||
                market.getPrimaryEntity() == null || market.getStarSystem() != system)) return false;
        CampaignFleetAPI convoy;
        try {
            convoy = Global.getFactory().createEmptyFleet(lastSurvivors ?
                    Global.getSector().getPlayerFaction().getId() : market.getFactionId(),
                    lastSurvivors ? "最后一批撤离船" : "联合撤离船队", true);
            convoy.getFleetData().addFleetMember(Global.getFactory().createFleetMember(
                    FleetMemberType.SHIP, "buffalo_Standard"));
        } catch (Throwable ex) {
            Global.getLogger(PTSDLateEventDirector.class).warn("Unable to create escort convoy", ex);
            return false;
        }
        { system.addEntity(convoy); data.scripts.campaign.invasion.PTSDCampaignRuntime.entityAdded(convoy); }
        SectorEntityToken departure = lastSurvivors ? system.getStar() : market.getPrimaryEntity();
        if (departure == null) departure = system.getJumpPoints().get(0);
        convoy.setLocation(departure.getLocation().x + 500f,
                departure.getLocation().y);
        convoy.getMemoryWithoutUpdate().set("$PTSD_late_escort", card.id);
        if ("WR-16".equals(card.id)) {
            convoy.addAssignment(FleetAssignment.GO_TO_LOCATION, system.getJumpPoints().get(0),
                    15f, "驶向撤离航路");
        } else {
            convoy.addAssignment(FleetAssignment.ORBIT_PASSIVE, departure,
                    Math.max(30f, IIRT_Omega_ModPlugin.lateEventTaskDeadline), "等待护航舰队接应");
        }
        card.targetMarketId = lastSurvivors ? null : market.getId();
        card.targetEntityId = convoy.getId();
        return true;
    }

    private static boolean createBattleTarget(PTSDCrisisState state, PTSDLateState.EventCard card) {
        if(data.scripts.PTSDDifficultySettings.spawningPaused())return false;
        StarSystemAPI system = state.resolveSystem(card.systemId);
        if (system == null) return false;
        PTSDLateState.Front donor = frontFor(state, card);
        boolean fromFront = donor != null && donor.attackBudget >= 30f;
        CampaignFleetAPI fleet;
        try {
            fleet = Global.getFactory().createEmptyFleet(
                    PTSDCrisisProgress.getActiveFactionId(state),
                    "精神创伤航路维护舰", true);
            fleet.getFleetData().addFleetMember(Global.getFactory().createFleetMember(
                    FleetMemberType.SHIP, "IIRT_Omega_Arrow_Only"));
            fleet.getFleetData().addFleetMember(Global.getFactory().createFleetMember(
                    FleetMemberType.SHIP, "IIRT_Omega_Arrow_Only_2"));
            data.scripts.campaign.cargo.PTSD_OmegaOfficerGeneratorPlugin.repairExistingFleet(fleet);
        } catch (Throwable ex) {
            Global.getLogger(PTSDLateEventDirector.class).warn("Unable to create card fleet", ex);
            return false;
        }
        if (!fromFront && !PTSDLateWarDirector.reserveAttack(state,
                "card-target:" + card.id, 30f)) return false;
        if (fromFront) {
            donor.attackBudget -= 30f;
            PTSDLateWarDirector.enqueueAlert(state, card.id + ":divert", donor.systemId,
                    "敌方从主攻编组抽出30点火力巡查航路；原战区压力降低。",
                    PTSDCrisisState.getDay());
        }
        { system.addEntity(fleet); data.scripts.campaign.invasion.PTSDCampaignRuntime.entityAdded(fleet); }
        SectorEntityToken anchor = system.getStar();
        MarketAPI market = state.resolveMarket(card.targetEntityId);
        if (market != null && market.getPrimaryEntity() != null) anchor = market.getPrimaryEntity();
        if (anchor != null) {
            org.lwjgl.util.vector.Vector2f point = Misc.getPointAtRadius(anchor.getLocation(), 1800f);
            fleet.setLocation(point.x, point.y);
            fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, anchor, 15f, "巡查被标记的航路");
        }
        fleet.addTag("PTSD_crisis_fleet");
        fleet.getMemoryWithoutUpdate().set("$PTSD_late_card_target", card.id);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        fleet.getEventListeners().add(new PTSDStrategicFleetListener(null, 30f));
        card.targetEntityId = fleet.getId();
        return true;
    }

    private static void advanceOrder(PTSDCrisisState state, PTSDLateState.EventCard card, float day) {
        if (day >= card.orderDeadlineDay) {
            finishOrder(state, card, false, "执行期限届满；未登记有效的现场战果");
            return;
        }
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        boolean local = player != null && player.getStarSystem() != null &&
                card.systemId != null && card.systemId.equals(player.getStarSystem().getId());
        if ("ROUTE".equals(card.orderKind)) {
            if (player != null && player.getStarSystem() != null &&
                    !card.systemId.equals(player.getStarSystem().getId()))
                finishOrder(state, card, true, "玩家实际改走另一条星际航路");
        } else if ("LEAVE".equals(card.orderKind)) {
            if (!local) finishOrder(state, card, true, "玩家舰队已离开指定星系");
        } else if ("OBSERVE".equals(card.orderKind)) {
            if (!local) return;
            SectorEntityToken target = card.targetEntityId == null ? null :
                    Global.getSector().getEntityById(card.targetEntityId);
            if (target == null) {
                MarketAPI market = state.resolveMarket(card.targetEntityId);
                if (market != null) target = market.getPrimaryEntity();
            }
            if (target == null && card.targetEntityId != null) {
                finishOrder(state, card, false, "现场目标在完成记录前消失");
                return;
            }
            if (target == null || Misc.getDistance(player, target) < 1500f)
                finishOrder(state, card, true, "已在现场取得可核实记录");
        } else if ("DEFEND".equals(card.orderKind)) {
            if (state.phase == PTSDCrisisState.Phase.WAR && frontFor(state, card) == null)
                finishOrder(state, card, false, "目标战区在抵达前结束");
        } else if ("ESCORT".equals(card.orderKind)) {
            SectorEntityToken entity = card.targetEntityId == null ? null :
                    Global.getSector().getEntityById(card.targetEntityId);
            if (!(entity instanceof CampaignFleetAPI) ||
                    ((CampaignFleetAPI) entity).getFleetData().getMembersListCopy().isEmpty()) {
                finishOrder(state, card, false, "撤离船队失联或被摧毁");
                return;
            }
            StarSystemAPI system = state.resolveSystem(card.systemId);
            if (system == null || system.getJumpPoints().isEmpty()) {
                finishOrder(state, card, false, "撤离航路失效");
                return;
            }
            if (!card.escortContact && local && Misc.getDistance(player, entity) < 2500f) {
                card.escortContact = true;
                if (!"WR-16".equals(card.id)) {
                    CampaignFleetAPI convoy = (CampaignFleetAPI) entity;
                    convoy.clearAssignments();
                    convoy.addAssignment(FleetAssignment.GO_TO_LOCATION, system.getJumpPoints().get(0),
                            Math.max(1f, card.orderDeadlineDay - day), "由玩家舰队护送撤离");
                }
            }
            if (Misc.getDistance(entity, system.getJumpPoints().get(0)) < 650f) {
                boolean escorted = local && card.escortContact && Misc.getDistance(player, entity) < 2500f;
                finishOrder(state, card, escorted,
                        escorted ? "玩家护送船队到达跃迁点" : "船队到达跃迁点时玩家未在护航范围");
                if (entity.getContainingLocation() != null)
                    entity.getContainingLocation().removeEntity(entity);
            }
        } else if ("HUNTER".equals(card.orderKind)) {
            if (state.prewarHunterResolved)
                finishOrder(state, card, false, "截获舰已离场或被其他力量解决");
        } else if (("BATTLE".equals(card.orderKind) || "TRANSPORT".equals(card.orderKind) ||
                "FORTRESS".equals(card.orderKind)) &&
                (card.targetEntityId == null ||
                        Global.getSector().getEntityById(card.targetEntityId) == null)) {
            finishOrder(state, card, false, "指定目标已消失，未记录为玩家截击战果");
        }
    }

    public static void onPlayerOmegaDefeat(PTSDCrisisState state, String fleetId, String systemId,
                                          PTSDCrisisState.StrategicEvent event) {
        if (state == null || state.late == null) return;
        for (PTSDLateState.EventCard card : state.late.eventCards) {
            if (card == null || !card.accepted || card.resolved ||
                    PTSDCrisisState.getDay() >= card.orderDeadlineDay) continue;
            if (matchesDefenseVictory(state, card, systemId, event) ||
                    ("BATTLE".equals(card.orderKind) && fleetId != null &&
                    fleetId.equals(card.targetEntityId)) ||
                    ("HUNTER".equals(card.orderKind) && event != null &&
                            event.type == PTSDCrisisState.EventType.PREWAR_HUNTER) ||
                    (("TRANSPORT".equals(card.orderKind) || "FORTRESS".equals(card.orderKind)) &&
                            fleetId != null && fleetId.equals(card.targetEntityId)))
                finishOrder(state, card, true, "玩家在指定战区击毁目标舰队");
        }
    }

    private static boolean matchesDefenseVictory(PTSDCrisisState state, PTSDLateState.EventCard card,
                                                  String systemId, PTSDCrisisState.StrategicEvent event) {
        if (!"DEFEND".equals(card.orderKind) || systemId == null || !systemId.equals(card.systemId) || event == null ||
                (event.type != PTSDCrisisState.EventType.ATTACK && event.type != PTSDCrisisState.EventType.FIRE_PROBE &&
                        event.type != PTSDCrisisState.EventType.PREWAR_HUNTER)) return false;
        if (state.phase != PTSDCrisisState.Phase.WAR) return true;
        PTSDLateState.Front front = frontFor(state, card);
        return front != null && (front.marketId != null && front.marketId.equals(event.targetMarketId) ||
                front.id != null && front.id.equals(event.referenceId));
    }

    private static void finishOrder(PTSDCrisisState state, PTSDLateState.EventCard card,
                                    boolean success, String reason) {
        if (card.resolved) return;
        card.resolved = true;
        if (success) {
            PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.HUMAN_COHESION,
                    card.id.startsWith("MB-") ? 5f : 3f, card.id, card.systemId);
            if ("OBSERVE".equals(card.orderKind))
                PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.HUMAN_AWARENESS,
                        4f, card.id + ":verified", card.systemId);
            PTSDLateState.Front front = frontFor(state, card);
            if (state.phase != PTSDCrisisState.Phase.WAR && front != null && ("DEFEND".equals(card.orderKind) || "BATTLE".equals(card.orderKind)))
                front.defense += 12f;
            if ("ESCORT".equals(card.orderKind)) {
                if (!state.playerMarkers.containsKey(card.systemId))
                    state.putMarker(card.systemId, "EVACUATE", 1.4f);
                PTSDGeneralStaffDirector.recordContribution(state, card.id + ":escort", "EVACUATION",
                        "player", card.systemId, 8f);
            }
            if ("DEFEND".equals(card.orderKind) && state.phase == PTSDCrisisState.Phase.MOBILIZATION &&
                    !state.playerMarkers.containsKey(card.systemId))
                state.putMarker(card.systemId, "DEFEND", 1.4f);
            if ("FT-04".equals(card.id) && "DEFEND".equals(card.orderKind))
                PTSDThreatDirector.recordAction(state, "FRONT", card.id, card.systemId,
                        15f, .5f, hasWitness(state, card.systemId), 20f);
            if ("FT-03".equals(card.id) && "ROUTE".equals(card.orderKind))
                state.late.nextTrapDay = Math.max(state.late.nextTrapDay,
                        PTSDCrisisState.getDay() + 5f);
            if ("FT-05".equals(card.id) && "BATTLE".equals(card.orderKind)) {
                float loss = card.selectedChoice == 2 ? 8f : 5f;
                PTSDLateWarDirector.disruptProduction(state, card.id + ":battle", loss,
                        card.selectedChoice == 2 ? 0f : loss, card.systemId);
            }
            if ("FT-10".equals(card.id) && card.selectedChoice == 2)
                PTSDLateWarDirector.disruptProduction(state, card.id + ":sabotage", 8f,
                        0f, card.systemId);
        }
        card.outcome = (success ? "已完成：" : "失败：") + reason +
                (success ? "；已按实际行动更新人类凝聚、任务及适用的当地防务" :
                        "；未发放战果或敌方损失");
        PTSDCrisisDevIntel.report("后期事件任务结算", card.id + "：" + card.outcome,
                card.systemId, card.targetEntityId);
    }

    private static String settleRemote(PTSDCrisisState state, PTSDLateState.EventCard card,
                                       int choice, String label) {
        if ("FT-10".equals(card.id)) {
            float gain = 18f;
            state.late.warPointBelief = Math.min(200f, state.late.warPointBelief + gain);
            return label + "：错误回执使敌方估计准备点+" + gain + "；真实准备点不变";
        }
        if ("FT-09".equals(card.id) && choice == 2) {
            boolean witnessed = hasWitness(state, card.systemId);
            if (witnessed) state.late.warPointBelief = Math.min(200f,
                    state.late.warPointBelief + 6f);
            return label + (witnessed ? "：敌方现场链路接收虚假回执，估计准备点+6；真实点不变" :
                    "：消息只在人类网络传播，敌方未获得回执；准备点不变");
        }
        if ("WR-JE-01".equals(card.id)) {
            if(choice!=2)return label+"：调查暂缓，Je保留现有记录。";
            String result=PTSDJeWarExpedition.start(state,card);
            if(result==null){reject(state,card,"Je当前忙碌，或没有合法调查航程与护航舰队");return null;}
            return result;
        }
        if ("WR-GS-01".equals(card.id))
            return choice == 1 ? label + "：现有个人战绩 " +
                    Math.round(PTSDGeneralStaffDirector.playerScore(state)) +
                    " 点已呈交；席位按行动当时归属核算，不重复记分" :
                    label + "：未申请新的联合额度；个人战绩与现有防区保持原状";
        if (("WR-GS-02".equals(card.id) || "WR-GS-04".equals(card.id)) && choice == 2)
            return label + "：后备队保持待命；可用额度和已登记席位不变";
        if ("WR-GS-02".equals(card.id) && choice == 1) {
            for (PTSDLateState.Reserve reserve : state.late.reserves) {
                if (reserve != null && PTSDGeneralStaffDirector.orderReserve(state,
                        reserve.id, card.systemId))
                    return label + "：后备队 " + reserve.id + " 已按实际航速向 " +
                            locationName(state, card.systemId) + " 开进";
            }
            reject(state, card, "没有可用且已授权的后备队");
            return null;
        }
        if ("WR-GS-04".equals(card.id) && choice == 1) {
            if (PTSDGeneralStaffDirector.playerScore(state) < 160f) {
                reject(state, card, "已核实玩家战绩不足160点");
                return null;
            }
            PTSDGeneralStaffDirector.advance(state, PTSDCrisisState.getDay());
            for (PTSDLateState.Reserve reserve : state.late.reserves)
                if (reserve != null && reserve.authorized && !reserve.destroyed)
                    return label + "：后备队 " + reserve.id + " 已开放调令；仍占用真实舰队额度";
            reject(state, card, "支持势力没有可拨出的后备舰队额度");
            return null;
        }
        if ("WR-04".equals(card.id) && choice == 1) {
            PTSDLateState.Front neighbor = null;
            for (PTSDLateState.Front candidate : state.late.fronts.values()) {
                if (candidate != null && !candidate.resolved &&
                        !candidate.id.equals(card.targetFrontId)) { neighbor = candidate; break; }
            }
            if (neighbor == null || state.late.humanResponseBudget < 15f) {
                reject(state, card, "邻区没有仍在交战的战线或联合额度不足15点");
                return null;
            }
            return coordinateRelief(state,card,neighbor,15f,label);
        }
        if (("WR-05".equals(card.id) || "WR-GS-03".equals(card.id)) && choice == 1) {
            PTSDLateState.Front front = frontFor(state, card);
            if (front == null || state.late.humanResponseBudget < 10f) {
                reject(state, card, "补给调度需要活跃战区与10点联合额度");
                return null;
            }
            return coordinateRelief(state,card,front,10f,label);
        }
        if ("WR-09".equals(card.id) && choice == 2) {
            PTSDLateState.Front front = frontFor(state, card);
            if (front == null || state.late.humanResponseBudget < 10f) {
                reject(state, card, "修复需活跃防区和10点联合额度");
                return null;
            }
            return coordinateRelief(state,card,front,10f,label);
        }
        if ("MB-02".equals(card.id) && choice == 2) {
            if (state.late.humanResponseBudget < 10f) {
                reject(state, card, "轻兵侦查需10点联合额度");
                return null;
            }
            state.late.humanResponseBudget -= 10f;
            state.putMarker(card.systemId, "DEFEND", 1.2f);
            if (hasWitness(state, card.systemId)) PTSDThreatDirector.recordAction(state,
                    "DECEPTION", card.id, card.systemId, 20f, .5f, true, 30f);
            return label + "：当地获得预警防务，联合额度-10；敌方仅在现场目击时记录样本";
        }
        if ("MB-01".equals(card.id) && choice == 2) {
            if (state.late.humanResponseBudget < 5f) {
                reject(state, card, "地方预警需要5点联合协调额度");
                return null;
            }
            state.late.humanResponseBudget -= 5f;
            if (!state.playerMarkers.containsKey(card.systemId))
                state.putMarker(card.systemId, "EVACUATE", 1.2f);
            return label + "：地方撤离准备已登记；联合额度-5，相关战区毁伤累积减缓";
        }
        if (card.id.startsWith("WR-") && choice == 1) {
            PTSDLateState.Front front = frontFor(state, card);
            if (front == null) { reject(state, card, "关联战区已结束"); return null; }
            if (state.late.humanResponseBudget < 15f) {
                reject(state, card, "联合防务额度不足15点"); return null;
            }
            return coordinateRelief(state,card,front,15f,label);
        }
        if (card.id.startsWith("FT-") && choice == 2 && hasWitness(state, card.systemId)) {
            PTSDThreatDirector.recordAction(state, "DECEPTION", card.id, card.systemId,
                    20f, .5f, true, 30f);
            return label + "：敌方在场舰队取得误导样本；其后续判断可能改变";
        }
        if (card.id.startsWith("MB-")) {
            if (choice == 2 && "MB-02".equals(card.id) && hasWitness(state, card.systemId))
                PTSDThreatDirector.recordAction(state, "DECEPTION", card.id,
                        card.systemId, 20f, .5f, true, 30f);
            PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.HUMAN_COHESION,
                    choice == 1 ? 5f : 2f, card.id, card.systemId);
            return label + "：联合情报协同+" + (choice == 1 ? 5 : 2) +
                    (choice == 2 && hasWitness(state, card.systemId) ? "；敌方取得现场样本" :
                            "；敌方未取得现场样本");
        }
        PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.HUMAN_COHESION,
                choice == 1 ? 3f : 2f, card.id, card.systemId);
        return label + "：可核实情报已共享；人类凝聚+" + (choice == 1 ? 3 : 2) +
                "，敌方准备点不变";
    }

    private static String coordinateRelief(PTSDCrisisState state,PTSDLateState.EventCard card,PTSDLateState.Front front,float cost,String label){
        if(!PTSDReliefDirector.requestAssistance(state,front)){reject(state,card,"该战区尚未获得可执行的援军承诺，请在联盟行动页查阅原因");return null;}
        state.late.humanResponseBudget-=cost;
        return label+"：调令已签收；协调额度-"+Math.round(cost)+"。兵力须完成整备、集结及航行，实际抵达后形成防御。";
    }

    private static boolean hasWitness(PTSDCrisisState state, String systemId) {
        StarSystemAPI system = state.resolveSystem(systemId);
        if (system == null) return false;
        for (com.fs.starfarer.api.campaign.CampaignFleetAPI fleet : system.getFleets()) {
            if (fleet.hasTag("PTSD_crisis_fleet")) return true;
        }
        return false;
    }
}
