package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.BaseFleetEventListener;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Three small human-only E2 chains; the other twelve E2 rows remain short reports. */
public final class PTSDE2Director {
    private static final Random RANDOM = new Random();
    private PTSDE2Director() { }

    public static void onPublished(PTSDCrisisState state, PTSDCrisisState.CrisisIncident incident) {
        if (state == null || state.late == null || incident == null || incident.cardId == null) return;
        if (!"E2-01".equals(incident.cardId) &&
                !"E2-02".equals(incident.cardId) &&
                !"E2-03".equals(incident.cardId)) {
            incident.effectSummary = incident.investigationResult == PTSDTaskRules.InvestigationResult.DISPROVED ?
                    "报道尚未核实，不能据此确认现场" : "地方报道进入公开档案";
            return;
        }
        if (state.late.e2Chains.containsKey(incident.id)) return;
        PTSDLateState.E2Chain chain = new PTSDLateState.E2Chain();
        chain.incidentId = incident.id;
        chain.cardId = incident.cardId;
        chain.systemId = incident.targetSystemId;
        chain.node = "REPORTED";
        chain.deadlineDay = incident.scenarioDeadline;
        state.late.e2Chains.put(incident.id, chain);
        incident.effectSummary = "报道提供了港口与当事船号；尚待到场核查";
        if (incident.scenarioFalse) return;
        StarSystemAPI system = state.resolveSystem(chain.systemId);
        MarketAPI market = state.resolveMarket(incident.targetMarketId);
        if (system == null || market == null || market.getPrimaryEntity() == null) {
            resolve(state, chain, "报道所指港口已经不存在；线索失效");
            return;
        }
        if ("E2-01".equals(chain.cardId)) {
            CampaignFleetAPI pirate = spawn(system, market, Factions.PIRATES,
                    FleetTypes.PATROL_SMALL, 28f, "索取打捞费的海盗船");
            CampaignFleetAPI tug = spawn(system, market, Factions.INDEPENDENT,
                    FleetTypes.TRADE_SMALL, 14f, "等待许可的打捞驳船");
            chain.actorFleetId = pirate == null ? null : pirate.getId();
            chain.supportFleetId = tug == null ? null : tug.getId();
            if (pirate != null) pirate.getEventListeners().add(new FleetResult(chain.incidentId, true));
            if (tug != null) tug.getEventListeners().add(new FleetResult(chain.incidentId, false));
        } else if ("E2-02".equals(chain.cardId)) {
            CampaignFleetAPI merchant = spawn(system, market, Factions.INDEPENDENT,
                    FleetTypes.TRADE_SMALL, 22f, "改签护航约的商船");
            CampaignFleetAPI mercenary = spawn(system, market, Factions.INDEPENDENT,
                    FleetTypes.PATROL_SMALL, 26f, "临时受雇武装");
            chain.actorFleetId = merchant == null ? null : merchant.getId();
            chain.supportFleetId = mercenary == null ? null : mercenary.getId();
            if (merchant != null) merchant.getEventListeners().add(new FleetResult(chain.incidentId, true));
            if (mercenary != null) mercenary.getEventListeners().add(new FleetResult(chain.incidentId, false));
            MarketAPI destination = destination(market);
            chain.destinationMarketId = destination == null ? null : destination.getId();
            if (merchant != null && destination != null) {
                merchant.addAssignment(FleetAssignment.GO_TO_LOCATION,
                        destination.getPrimaryEntity(), 20f, "按新护航合约前往目的港");
                if (mercenary != null)
                    mercenary.addAssignment(FleetAssignment.FOLLOW, merchant, 20f,
                            "伴随商船执行合同");
            }
        } else {
            CampaignFleetAPI trader = spawn(system, market, Factions.INDEPENDENT,
                    FleetTypes.TRADE_SMALL, 16f, "等待更正船号的商船");
            chain.actorFleetId = trader == null ? null : trader.getId();
        }
        if (chain.actorFleetId == null)
            resolve(state, chain, "报道所指船只未能在现场确认；未生成合同或奖励");
    }

    private static CampaignFleetAPI spawn(StarSystemAPI system, MarketAPI market,
                                           String faction, String type, float fp, String name) {
        Vector2f point = PTSDCrisisAPI.findSafePoint(system, market.getPrimaryEntity(),
                1200f, 2800f, RANDOM);
        if (point == null) return null;
        FleetParamsV3 params = new FleetParamsV3(point, faction, .5f, type,
                fp, 0f, 0f, 0f, 0f, 0f, 0f);
        params.maxNumShips = 5;
        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
        if (fleet == null || fleet.getFleetData().getMembersListCopy().isEmpty()) return null;
        { system.addEntity(fleet); data.scripts.campaign.invasion.PTSDCampaignRuntime.entityAdded(fleet); }
        fleet.setLocation(point.x, point.y);
        fleet.setName(name);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
        return fleet;
    }

    private static MarketAPI destination(MarketAPI origin) {
        List<MarketAPI> choices = new ArrayList<MarketAPI>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy())
            if (market != null && market != origin && !market.isPlanetConditionMarketOnly() &&
                    market.getPrimaryEntity() != null && market.getStarSystem() != null &&
                    !IIRT_Omega_Invasion.WATCHER_FACTION.equals(market.getFactionId()) &&
                    !IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(market.getFactionId()))
                choices.add(market);
        return choices.isEmpty() ? null : choices.get(RANDOM.nextInt(choices.size()));
    }

    public static String status(PTSDCrisisState.CrisisIncident incident) {
        PTSDLateState.E2Chain chain = chain(incident);
        if (chain == null) return "";
        if (chain.resolved) return chain.outcome == null ? "已结算" : chain.outcome;
        if (incident.scenarioFalse) return "到报道所指港口核实这条消息";
        if ("E2-01".equals(chain.cardId) && chain.accepted) return "正在等待实际海盗战果";
        if ("E2-02".equals(chain.cardId) && chain.accepted) return "护航合约进行中；到港复核是否随行";
        if ("E2-03".equals(chain.cardId) && "REVIEW".equals(chain.node))
            return "船号已经核对，可决定是否公开记录";
        return "需抵达报道所指港口，现场作出决定";
    }

    public static String[] choices(PTSDCrisisState.CrisisIncident incident) {
        PTSDLateState.E2Chain chain = chain(incident);
        if (chain == null || chain.resolved || chain.accepted ||
                PTSDCrisisState.getDay() >= chain.deadlineDay) return null;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null || player.getStarSystem() == null ||
                !chain.systemId.equals(player.getStarSystem().getId())) return null;
        MarketAPI market = PTSDCrisisState.get().resolveMarket(incident.targetMarketId);
        if (market == null || market.getPrimaryEntity() == null ||
                Misc.getDistance(player, market.getPrimaryEntity()) > 5000f) return null;
        if (incident.scenarioFalse) return new String[] {"核查报道", "保留疑点"};
        if ("E2-01".equals(chain.cardId)) return new String[] {"保护打捞船", "支付5000星币通行费"};
        if ("E2-02".equals(chain.cardId)) {
            CampaignFleetAPI merchant = fleet(chain.actorFleetId);
            MarketAPI destination = PTSDCrisisState.get().resolveMarket(chain.destinationMarketId);
            if (merchant == null || destination == null ||
                    destination.getPrimaryEntity() == null ||
                    merchant.getContainingLocation() ==
                            destination.getPrimaryEntity().getContainingLocation() &&
                    Misc.getDistance(merchant, destination.getPrimaryEntity()) < 1200f)
                return null;
            return new String[] {"接受护航合约", "拒绝改约"};
        }
        if ("REVIEW".equals(chain.node)) return new String[] {"向港务处公开记录", "私下归档"};
        return new String[] {"核对商船船号", "接受港务说明"};
    }

    public static boolean choose(PTSDCrisisState.CrisisIncident incident, int option) {
        String[] available = choices(incident);
        PTSDLateState.E2Chain chain = chain(incident);
        PTSDCrisisState state = PTSDCrisisState.get();
        if (available == null || chain == null || state == null || option < 0 || option >= available.length)
            return false;
        if (incident.scenarioFalse) {
            resolve(state, chain, option == 0 ?
                    "现场没有报道所称的船只；港务处撤回了错误通报" :
                    "未取得可核实对象；报道按未证实归档");
            return true;
        }
        if ("E2-01".equals(chain.cardId)) {
            if (option == 0) {
                chain.accepted = true;
                chain.node = "INTERCEPT";
                CampaignFleetAPI pirate = fleet(chain.actorFleetId);
                if (pirate != null) {
                    pirate.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, false);
                    pirate.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
                }
                incident.effectSummary = "打捞船仍在现场；只有实际击退索价舰队才会结算报酬";
            } else {
                if (Global.getSector().getPlayerFleet().getCargo().getCredits().get() < 5000f) return false;
                Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(5000f);
                resolve(state, chain, "已支付5000星币；打捞船获准离开，海盗保留了索价记录");
            }
        } else if ("E2-02".equals(chain.cardId)) {
            if (option == 0) {
                if (chain.destinationMarketId == null || fleet(chain.actorFleetId) == null) return false;
                chain.accepted = true;
                chain.node = "ESCORT";
                incident.effectSummary = "护航约已签；商船到港且玩家实际随行时支付25000星币";
            } else resolve(state, chain, "玩家拒绝改约；商船按原合同自行航行");
        } else if (!"REVIEW".equals(chain.node)) {
            if (option == 0) {
                chain.node = "REVIEW";
                incident.effectSummary = "两个船号指向同一艘真实商船；港务记录仍可公开或私下归档";
            } else resolve(state, chain, "接受港务处更正说明；争议未再扩大");
        } else {
            if (option == 0) {
                if (!chain.paid) {
                    Global.getSector().getPlayerFleet().getCargo().getCredits().add(10000f);
                    chain.paid = true;
                }
                resolve(state, chain, "重复登记暴露了一笔人类走私账；港务处支付10000星币核查酬金");
            } else resolve(state, chain, "原始登记被私人保存；港务争议以内部更正结案");
        }
        PTSDCrisisDevIntel.report("E2 现场选择", chain.cardId + " / " + available[option],
                chain.systemId, chain.actorFleetId);
        return true;
    }

    public static void advance(PTSDCrisisState state, float day) {
        if (state == null || state.late == null) return;
        for (PTSDLateState.E2Chain chain : new ArrayList<PTSDLateState.E2Chain>(state.late.e2Chains.values())) {
            if (chain == null || chain.resolved) continue;
            PTSDCrisisState.CrisisIncident incident = PTSDCrisisAPI.getIncident(chain.incidentId);
            if (incident == null) { chain.resolved = true; continue; }
            if ("E2-01".equals(chain.cardId) && chain.actorDefeated) {
                if (chain.accepted && chain.actorPlayerVictory) {
                    if (!chain.paid) {
                        Global.getSector().getPlayerFleet().getCargo().getCredits().add(15000f);
                        chain.paid = true;
                    }
                    resolve(state, chain, "玩家实际击退索价舰队；打捞船支付15000星币");
                } else resolve(state, chain, "索价舰队被其他力量击退，或玩家尚未签约；没有合同报酬");
            } else if ("E2-02".equals(chain.cardId) && chain.accepted) {
                CampaignFleetAPI merchant = fleet(chain.actorFleetId);
                MarketAPI destination = state.resolveMarket(chain.destinationMarketId);
                if (merchant == null || chain.actorDefeated) {
                    resolve(state, chain, "商船未能到港；护航合同失败，没有尾款");
                } else if (destination != null && merchant.getContainingLocation() ==
                        destination.getPrimaryEntity().getContainingLocation() &&
                        Misc.getDistance(merchant, destination.getPrimaryEntity()) < 1200f) {
                    CampaignFleetAPI player = Global.getSector().getPlayerFleet();
                    boolean present = player != null && player.getContainingLocation() ==
                            merchant.getContainingLocation() &&
                            Misc.getDistance(player, merchant) < 5000f;
                    if (present && !chain.paid) {
                        player.getCargo().getCredits().add(25000f);
                        chain.paid = true;
                    }
                    resolve(state, chain, present ?
                            "商船实际到港，玩家随行；护航尾款25000星币已付" :
                            "商船到港时玩家未随行；合同不支付护航尾款");
                }
            }
            if (!chain.resolved && day >= chain.deadlineDay)
                resolve(state, chain, "干预期限届满；地方事件按现存船只和合同状态结案");
        }
    }

    public static void onInvestigated(PTSDCrisisState state, PTSDCrisisState.CrisisIncident incident) {
        if (state == null || incident == null) return;
        incident.effectSummary = incident.investigationResult == PTSDTaskRules.InvestigationResult.DISPROVED ?
                "取得了足以证伪地方报道的记录" :
                incident.investigationResult == PTSDTaskRules.InvestigationResult.CONFIRMED ?
                        "现场记录与地方报道相符；其他关联尚无证据" : "现有记录不足以验证或证伪地方报道";
    }

    public static void onExpired(PTSDCrisisState state, PTSDCrisisState.CrisisIncident incident,
                                  String why) {
        if (state == null || incident == null) return;
        PTSDLateState.E2Chain chain = chain(incident);
        if (chain != null && !chain.resolved)
            resolve(state, chain, why + "；尚无新的当事人回执");
        else incident.effectSummary = why + "；" +
                (incident.scenarioFalse ? "报道内容未获证实" : "地方报道归档");
    }

    private static void resolve(PTSDCrisisState state, PTSDLateState.E2Chain chain,
                                String result) {
        if (chain == null || chain.resolved) return;
        chain.resolved = true;
        chain.outcome = result;
        for (String fleetId : new String[] {chain.actorFleetId, chain.supportFleetId}) {
            CampaignFleetAPI departing = fleet(fleetId);
            if (departing == null || departing.getBattle() != null ||
                    departing.getContainingLocation() == null) continue;
            Vector2f exit = Misc.getPointAtRadius(departing.getLocation(), 16000f);
            departing.clearAssignments();
            departing.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                    departing.getContainingLocation().createToken(exit), 12f,
                    "地方事件结束后离开航路");
        }
        PTSDCrisisState.CrisisIncident incident = PTSDCrisisAPI.getIncident(chain.incidentId);
        if (incident != null) {
            incident.scenarioResolved = true;
            incident.investigationResolved = true;
            incident.effectSummary = result;
            incident.newsExpiresDay = Math.max(incident.newsExpiresDay,
                    PTSDCrisisState.getDay() + 10f);
        }
        PTSDCrisisDevIntel.report("E2 结局", chain.cardId + "：" + result,
                chain.systemId, chain.actorFleetId);
    }

    private static PTSDLateState.E2Chain chain(PTSDCrisisState.CrisisIncident incident) {
        PTSDCrisisState state = PTSDCrisisState.get();
        return incident == null || state == null || state.late == null ?
                null : state.late.e2Chains.get(incident.id);
    }

    private static CampaignFleetAPI fleet(String id) {
        if (id == null || Global.getSector() == null) return null;
        Object found = Global.getSector().getEntityById(id);
        return found instanceof CampaignFleetAPI ? (CampaignFleetAPI) found : null;
    }

    private static final class FleetResult extends BaseFleetEventListener {
        private final String incidentId;
        private final boolean actor;
        FleetResult(String incidentId, boolean actor) {
            this.incidentId = incidentId; this.actor = actor;
        }
        @Override public void reportBattleOccurred(CampaignFleetAPI fleet,
                CampaignFleetAPI primaryWinner, BattleAPI battle) {
            if (fleet == null || battle == null || !battle.isDone() ||
                    !battle.wasFleetDefeated(fleet, primaryWinner)) return;
            PTSDCrisisState state = PTSDCrisisState.get();
            if (state == null || state.late == null) return;
            PTSDLateState.E2Chain chain = state.late.e2Chains.get(incidentId);
            if (chain == null || chain.resolved ||
                    !state.late.committedIds.add("e2-loss:" + fleet.getId())) return;
            if (actor) {
                chain.actorDefeated = true;
                CampaignFleetAPI player = Global.getSector().getPlayerFleet();
                chain.actorPlayerVictory = player != null && battle.isPlayerInvolved() &&
                        battle.wasFleetVictorious(player, primaryWinner);
            } else chain.supportDefeated = true;
        }
    }
}
