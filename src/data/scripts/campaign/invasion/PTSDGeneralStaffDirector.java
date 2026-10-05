package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;

import java.util.LinkedHashMap;
import java.util.Map;

/** Contributions are immutable at the moment of action; commission ownership never backfills. */
public final class PTSDGeneralStaffDirector {
    private PTSDGeneralStaffDirector() { }

    public static boolean devEstablish(PTSDCrisisState state) {
        if (state == null || state.phase != PTSDCrisisState.Phase.WAR ||
                !Global.getSettings().isDevMode()) return false;
        state.late.staffEstablished = true;
        if (state.late.warStartedDay <= 0f) state.late.warStartedDay = PTSDCrisisState.getDay();
        PTSDGeneralStaffIntel.ensureIntel();
        return true;
    }

    public static boolean devResolveNextVote(PTSDCrisisState state) {
        if (state == null || !Global.getSettings().isDevMode() ||
                state.phase != PTSDCrisisState.Phase.WAR) return false;
        float day = PTSDCrisisState.getDay();
        for (PTSDLateState.Resolution resolution : state.late.resolutions) {
            if (resolution == null || resolution.resolved) continue;
            resolution.day = day - 2f;
            advance(state, day);
            return resolution.resolved;
        }
        return false;
    }

    public static void recordContribution(PTSDCrisisState state, String id, String type,
                                          String actor, String systemId, float amount) {
        if (state == null || state.late == null || id == null ||
                !state.late.committedIds.add("contribution:" + id)) return;
        PTSDLateState.Contribution item = new PTSDLateState.Contribution();
        item.id = id;
        item.type = type;
        item.actorId = actor;
        item.systemId = systemId;
        item.amount = amount;
        item.day = PTSDCrisisState.getDay();
        item.factionAtTime = "player".equals(actor) ? playerAffiliation() : actor;
        state.late.contributions.add(item);
    }

    private static String playerAffiliation() {
        String commission = Misc.getCommissionFactionId();
        if (commission != null) return commission;
        if (Global.getSector() == null || Global.getSector().getPlayerFaction() == null) return "player";
        if (Global.getSector().getPlayerFaction().getId() == null) return "player";
        return Global.getSector().getPlayerFaction().getId();
    }

    public static void registerFounders(PTSDCrisisState state) {
        if (state == null || state.late == null || !state.late.staffEstablished || state.late.coalitionFoundersRegistered) return;
        for (String faction : new String[] {"hegemony", "tritachyon"}) {
            for (MarketAPI market : PTSDOccupationManager.getAllMarkets()) {
                if (market != null && !market.isPlanetConditionMarketOnly() && faction.equals(market.getFactionId())) {
                    state.late.coalitionMembers.add(faction); break;
                }
            }
        }
        // No substitute founder is invented when both are absent.
        state.late.coalitionFoundersRegistered = true;
        reconcileMemberRelations(state);
    }

    public static boolean requestMembership(PTSDCrisisState state, String factionId) {
        if (state == null || state.late == null || !state.late.staffEstablished || factionId == null ||
                Global.getSector().getFaction(factionId) == null || state.late.coalitionMembers.contains(factionId) ||
                state.late.coalitionJoinRequests.containsKey(factionId) ||
                IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(factionId) ||
                IIRT_Omega_Invasion.WATCHER_FACTION.equals(factionId)) return false;
        state.late.coalitionJoinRequests.put(factionId, PTSDCrisisState.getDay() + 2f);
        state.late.coalitionDiplomaticReplies.put(factionId, "入盟邀请已送达；代表团正在答复。");
        return true;
    }

    private static void advanceMembership(PTSDCrisisState state, float day) {
        for (Map.Entry<String, Float> request : new java.util.ArrayList<Map.Entry<String, Float>>(
                state.late.coalitionJoinRequests.entrySet())) {
            if (day < request.getValue()) continue;
            String faction = request.getKey();
            boolean threatened = false, hasColony = false;
            for (MarketAPI market : PTSDOccupationManager.getAllMarkets()) {
                if (market == null || market.isPlanetConditionMarketOnly() || !faction.equals(market.getFactionId())) continue;
                hasColony = true;
                for (PTSDLateState.Front front : state.late.fronts.values())
                    if (front != null && !front.resolved && market.getId().equals(front.marketId)) threatened = true;
            }
            boolean player = Global.getSector().getPlayerFaction().getId().equals(faction);
            float relation = Global.getSector().getFaction(faction).getRelationship(playerAffiliation());
            boolean willing = player || hasColony && (threatened || relation >= 0f);
            if (willing) {
                state.late.coalitionMembers.add(faction);
                reconcileMemberRelations(state);
            }
            state.late.coalitionDiplomaticReplies.put(faction, willing ?
                    "代表团同意入盟；可承诺的兵力与补给仍需分别核验。" : "代表团拒绝入盟；当前威胁与外交关系不足以达成合作。");
            state.late.coalitionJoinRequests.remove(faction);
            PTSDLateWarDirector.enqueueAlert(state, "membership:" + faction + ":" + Math.round(day), null,
                    Global.getSector().getFaction(faction).getDisplayName() + "：" + state.late.coalitionDiplomaticReplies.get(faction), day);
        }
    }

    private static void reconcileMemberRelations(PTSDCrisisState state) {
        for (String a : state.late.coalitionMembers) for (String b : state.late.coalitionMembers) {
            if (a.compareTo(b) >= 0 || Global.getSector().getFaction(a) == null || Global.getSector().getFaction(b) == null) continue;
            String key = a + "|" + b;
            float old = Global.getSector().getFaction(a).getRelationship(b);
            if (!state.late.coalitionPreviousRelations.containsKey(key)) state.late.coalitionPreviousRelations.put(key, old);
            if (old < -.4f) Global.getSector().getFaction(a).setRelationship(b, -.4f);
        }
    }

    public static Map<String, Float> seats(PTSDCrisisState state) {
        Map<String, Float> result = new LinkedHashMap<String, Float>();
        if (state == null || state.late == null) return result;
        registerFounders(state);
        for (MarketAPI market : PTSDOccupationManager.getAllMarkets()) {
            if (market == null || market.isPlanetConditionMarketOnly()) continue;
            String faction = market.getFactionId();
            if (!PTSDHumanWarReports.member(state, faction)) continue;
            if (IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(faction) ||
                    IIRT_Omega_Invasion.WATCHER_FACTION.equals(faction)) continue;
            Float old = result.get(faction);
            result.put(faction, (old == null ? 0f : old) + Math.max(1f, market.getSize() * 2f));
        }
        for (PTSDLateState.Contribution item : state.late.contributions) {
            if (item == null || item.factionAtTime == null ||
                    !PTSDHumanWarReports.member(state, item.factionAtTime)) continue;
            Float old = result.get(item.factionAtTime);
            result.put(item.factionAtTime, (old == null ? 0f : old) + Math.max(0f, item.amount));
        }
        return result;
    }

    public static float playerScore(PTSDCrisisState state) {
        float sum = 0f;
        if (state == null || state.late == null) return sum;
        for (PTSDLateState.Contribution item : state.late.contributions) {
            if (item != null && "player".equals(item.actorId)) sum += Math.max(0f, item.amount);
        }
        return sum;
    }

    public static boolean propose(PTSDCrisisState state, String frontId, String method) {
        if (state == null || state.late == null || !state.late.staffEstablished ||
                frontId == null || method == null) return proposalFailed("总参部尚未成立或提案无效");
        PTSDLateState.Front front = state.late.fronts.get(frontId);
        if (front == null || front.resolved) return proposalFailed("目标战区已结束");
        float day = PTSDCrisisState.getDay();
        String key = "staff:" + frontId + ":" + Math.round(day / 5f);
        if (state.late.committedIds.contains(key)) return proposalFailed("本轮已经提交过该战区议案");
        String sponsor = null;
        float highest = 0f;
        MarketAPI threatened = state.resolveMarket(front.marketId);
        for (Map.Entry<String, Float> entry : seats(state).entrySet()) {
            if (entry.getKey().equals(IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION)) continue;
            float preference = threatened != null &&
                    entry.getKey().equals(threatened.getFactionId()) ? 1.5f : 1f;
            if (entry.getValue() * preference > highest) {
                highest = entry.getValue() * preference;
                sponsor = entry.getKey();
            }
        }
        if (sponsor == null) return proposalFailed("没有具备投票权的支持势力");
        Float available = state.late.staffAvailableByFaction.get(sponsor);
        if (available == null) available = Math.min(180f, highest * 2f);
        if (available < 60f) return proposalFailed(sponsor + " 可调拨舰队不足60点");
        String exportMarketId = null;
        if ("EVIDENCE".equals(method)) {
            if (playerScore(state) < 20f && (front.evidenceIds == null || front.evidenceIds.isEmpty()))
                return proposalFailed("已核实战绩不足20点，且缺少敌军证据");
        } else if ("PRESSURE".equals(method)) {
            if (Global.getSector().getPlayerStats().getStoryPoints() < 1)
                return proposalFailed("故事点不足1点");
            Global.getSector().getPlayerStats().spendStoryPoints(1, true, null, false,
                    "联合总参部外交施压");
            Global.getSector().getPlayerFaction().adjustRelationship(sponsor, -.03f);
        } else if ("SUPPLIES".equals(method)) {
            if (Global.getSector().getPlayerFleet() == null ||
                    Global.getSector().getPlayerFleet().getCargo().getSupplies() < 60f)
                return proposalFailed("舰队补给不足60单位");
            Global.getSector().getPlayerFleet().getCargo().removeSupplies(60f);
        } else if ("EXPORT".equals(method)) {
            MarketAPI exporter = null;
            for (MarketAPI market : PTSDOccupationManager.getAllMarkets()) {
                if (market == null || !market.isPlayerOwned() || market.getCommodityData(Commodities.SUPPLIES) == null ||
                        market.getCommodityData(Commodities.SUPPLIES).getMaxSupply() < 3 ||
                        state.late.staffExportContracts.containsKey(market.getId())) continue;
                exporter = market;
                break;
            }
            if (exporter == null) return proposalFailed("没有符合条件且未签约的玩家殖民地出口");
            exporter.getStability().modifyFlat("PTSD_staff_export_" + exporter.getId(), -1f,
                    "总参部优先出口契约");
            state.late.staffExportContracts.put(exporter.getId(), day + 30f);
            exportMarketId = exporter.getId();
        } else return proposalFailed("未知劝说方式");
        state.late.committedIds.add(key);
        PTSDLateState.Resolution resolution = new PTSDLateState.Resolution();
        resolution.id = key;
        resolution.frontId = frontId;
        resolution.method = method;
        resolution.exportMarketId = exportMarketId;
        resolution.sponsorFactionId = sponsor;
        resolution.day = day;
        resolution.reservedStrength = 60f;
        float total = 0f;
        for (float votes : seats(state).values()) total += votes;
        float stance = Global.getSector().getFaction(sponsor) == null ? 0f :
                Global.getSector().getFaction(sponsor).getRelationship(playerAffiliation());
        float evidence = front.evidenceIds == null ? 0f : Math.min(.12f, front.evidenceIds.size() * .03f);
        resolution.voteChance = Math.min(.95f, Math.max(.05f,
                .2f + highest / Math.max(1f, total) * .4f + stance * .15f + evidence +
                ("PRESSURE".equals(method) ? .2f : "SUPPLIES".equals(method) ||
                        "EXPORT".equals(method) ? .15f : .1f)));
        state.late.staffAvailableByFaction.put(sponsor, available - 60f);
        state.late.resolutions.add(resolution);
        PTSDLateWarDirector.enqueueAlert(state, key, front.systemId,
                "总参部登记：" + sponsor + " 已冻结60点舰队额度，正复核 " + front.systemId + " 的增援议案。", day);
        return true;
    }

    private static boolean proposalFailed(String reason) {
        if (Global.getSector() != null && Global.getSector().getCampaignUI() != null)
            Global.getSector().getCampaignUI().addMessage("[总参部] 提案未受理：" + reason);
        PTSDCrisisDevIntel.report("总参部提案未受理", reason, null, null);
        return false;
    }

    public static void advance(PTSDCrisisState state, float day) {
        if (state == null || state.late == null || state.phase != PTSDCrisisState.Phase.WAR) return;
        if (!state.late.staffEstablished && day >= state.late.warStartedDay + 10f) {
            state.late.staffEstablished = true;
            PTSDGeneralStaffIntel.ensureIntel();
            PTSDLateWarDirector.enqueueAlert(state, "general-staff", state.baseSystemId,
                    "联合总参部成立。席位依既有实力及可核实战绩计算；无代表团可承诺未到位的舰队。", day);
        }
        registerFounders(state);
        advanceMembership(state, day);
        int fortressKills = 0;
        for (PTSDLateState.Contribution item : state.late.contributions) {
            if (item != null && "player".equals(item.actorId) &&
                    "FORTRESS".equals(item.type)) fortressKills++;
        }
        float score = playerScore(state);
        boolean commandQualified = fortressKills >= 2 || score >= 180f ||
                (day <= state.late.warStartedDay + 30f && score >= 120f) ||
                (day >= state.late.warStartedDay + 90f && score >= 90f);
        if (state.late.staffEstablished && commandQualified &&
                state.late.reserves.isEmpty()) {
            String faction = playerAffiliation();
            Float available = state.late.staffAvailableByFaction.get(faction);
            if (available == null) {
                Float votes = seats(state).get(faction);
                available = Math.min(180f, (votes == null ? 0f : votes) * 2f);
            }
            StarSystemAPI source = null;
            for (MarketAPI market : PTSDOccupationManager.getAllMarkets()) {
                if (market != null && faction.equals(market.getFactionId()) &&
                        market.getStarSystem() != null) { source = market.getStarSystem(); break; }
            }
            if (available >= 70f && source != null) {
                PTSDLateState.Reserve reserve = new PTSDLateState.Reserve();
                reserve.id = "player-reserve-1";
                reserve.factionId = faction;
                reserve.sourceSystemId = source.getId();
                reserve.strength = 70f;
                reserve.lastDay = day;
                reserve.authorized = true;
                state.late.reserves.add(reserve);
                state.late.staffAvailableByFaction.put(faction, available - 70f);
                PTSDLateWarDirector.enqueueAlert(state, "reserve-authorized", source.getId(),
                        "总参部已从可用舰队额度中拨出一支后备队，等待玩家指定战区。", day);
            }
        }
        for (PTSDLateState.Resolution resolution : state.late.resolutions) {
            if (resolution == null || resolution.resolved || day < resolution.day + 2f) continue;
            resolution.resolved = true;
            resolution.executedDay = day;
            PTSDLateState.Front front = state.late.fronts.get(resolution.frontId);
            Float available = state.late.staffAvailableByFaction.get(resolution.sponsorFactionId);
            boolean sponsorCanSend = false;
            for (MarketAPI market : PTSDOccupationManager.getAllMarkets()) {
                if (market != null && !market.isPlanetConditionMarketOnly() &&
                        resolution.sponsorFactionId.equals(market.getFactionId()) &&
                        market.getSize() >= 3) { sponsorCanSend = true; break; }
            }
            MarketAPI exporter = resolution.exportMarketId == null ? null :
                    state.resolveMarket(resolution.exportMarketId);
            boolean contractValid = resolution.exportMarketId == null ||
                    (exporter != null && exporter.isPlayerOwned() &&
                            exporter.getCommodityData(Commodities.SUPPLIES) != null &&
                            exporter.getCommodityData(Commodities.SUPPLIES).getMaxSupply() >= 3);
            if (front == null || front.resolved || available == null ||
                    !sponsorCanSend || !contractValid) {
                resolution.outcome = "议案过期；预留资源返还";
                state.late.staffAvailableByFaction.put(resolution.sponsorFactionId,
                        (available == null ? 0f : available) + resolution.reservedStrength);
            } else if (Math.random() < resolution.voteChance) {
                resolution.outcome = "通过并执行";
                front.defense += resolution.reservedStrength;
            } else {
                resolution.outcome = "未获足够席位";
                state.late.staffAvailableByFaction.put(resolution.sponsorFactionId,
                        available + resolution.reservedStrength);
            }
            PTSDLateWarDirector.enqueueAlert(state, resolution.id + ":result", front == null ? null : front.systemId,
                    "总参部表决：" + resolution.outcome + "。", day);
        }
        for (Map.Entry<String, Float> entry : new java.util.ArrayList<Map.Entry<String, Float>>(
                state.late.staffExportContracts.entrySet())) {
            if (day < entry.getValue()) continue;
            MarketAPI market = state.resolveMarket(entry.getKey());
            if (market != null) market.getStability().unmodify("PTSD_staff_export_" + entry.getKey());
            state.late.staffExportContracts.remove(entry.getKey());
        }
        PTSDReserveDirector.advance(state, day);
    }

    public static boolean orderReserve(PTSDCrisisState state, String reserveId, String systemId) {
        if (state == null || state.late == null || systemId == null ||
                state.resolveSystem(systemId) == null) return false;
        for (PTSDLateState.Reserve reserve : state.late.reserves) {
            if (reserve != null && reserve.id.equals(reserveId) && reserve.authorized &&
                    !reserve.destroyed) {
                if (reserve.targetSystemId != null && !reserve.arrived) return false;
                if (reserve.arrived && reserve.targetSystemId != null) {
                    reserve.sourceSystemId = reserve.targetSystemId;
                }
                reserve.targetSystemId = systemId;
                reserve.travelled = 0f;
                reserve.distance = 0f;
                reserve.arrived = false;
                reserve.lastDay = PTSDCrisisState.getDay();
                return true;
            }
        }
        return false;
    }
}
