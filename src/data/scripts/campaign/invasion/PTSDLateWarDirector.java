package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.IIRT_Omega_ModPlugin;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** One authoritative ledger for late production, phase gates and the opening bombardment. */
public final class PTSDLateWarDirector {
    private static final Random RANDOM = new Random();
    private static final Color RED = new Color(255, 90, 98);
    private static final float STRATEGIC_ASSET_CAP = 5000f;
    private static final String DEVELOPED_TAG = "PTSD_fully_developed_planet";

    private PTSDLateWarDirector() { }

    public static void advance(PTSDCrisisState state, float day) {
        if (state == null || state.late == null) return;
        PTSDLateState late = state.late;
        if (!late.initialized) initialize(state, day);
        if (state.phase == PTSDCrisisState.Phase.FORTIFICATION || state.phase == PTSDCrisisState.Phase.MOBILIZATION)
            PTSDWarCampaignDirector.prepare(state, day, false);
        if (state.phase == PTSDCrisisState.Phase.FORTIFICATION && day >= late.nextPhaseExpansionCheck) {
            late.nextPhaseExpansionCheck = day + 2f;
            expandFortificationFrontier(state);
            PTSDFortressDirector.startPhasePair(state, day);
        }
        if (state.phase == PTSDCrisisState.Phase.EXPANSION ||
                state.phase == PTSDCrisisState.Phase.FORTIFICATION)
            ensureDevelopedPlanets(state);
        if (state.phase == PTSDCrisisState.Phase.EXPANSION ||
                state.phase == PTSDCrisisState.Phase.FORTIFICATION ||
                state.phase == PTSDCrisisState.Phase.MOBILIZATION ||
                state.phase == PTSDCrisisState.Phase.WAR) reconcileEconomy(state, day);
        PTSDThreatDirector.advance(state, day);
        if (state.phase == PTSDCrisisState.Phase.EXPANSION ||
                state.phase == PTSDCrisisState.Phase.FORTIFICATION ||
                state.phase == PTSDCrisisState.Phase.MOBILIZATION ||
                state.phase == PTSDCrisisState.Phase.WAR) {
            PTSDFortressDirector.advance(state, day);
        }
        if (state.phase == PTSDCrisisState.Phase.EXPANSION ||
                state.phase == PTSDCrisisState.Phase.FORTIFICATION ||
                state.phase == PTSDCrisisState.Phase.MOBILIZATION ||
                state.phase == PTSDCrisisState.Phase.WAR) {
            if (late.warEconomyVersion > 0 && day >= late.nextProductionDay) {
                produce(state, day, false);
                late.nextProductionDay = day + IIRT_Omega_ModPlugin.lateProductionInterval;
            }
        }
        if (state.phase == PTSDCrisisState.Phase.MOBILIZATION) {
            PTSDMobilizationIntel.ensureIntel();
        }
        if (state.phase == PTSDCrisisState.Phase.WAR) {
            if (!late.firstWaveSelected) onWarStarted(state);
            advanceFronts(state, day);
            PTSDGeneralStaffDirector.advance(state, day);
        }
        PTSDFortressDirector.advanceDetonation(state);
        PTSDReliefDirector.advance(state,day);
        PTSDLateEventDirector.advance(state, day);
        deliverAlerts(state, day);
    }

    private static void initialize(PTSDCrisisState state, float day) {
        PTSDLateState late = state.late;
        late.initialized = true;
        late.warPoints = Math.max(0f, Math.min(25f, late.warPoints));
        late.warPointBelief = late.warPoints;
        late.nextProductionDay = day + 10f;
        late.nextTrapDay = day + 20f;
        late.nextPlayerVictoryRollDay = day + 10f;
        late.lastAdvanceDay = day;
        if (state.phase == PTSDCrisisState.Phase.WAR) {
            // A pre-v12 WAR save already has its own war history; do not replay the first wave.
            late.firstWaveSelected = true;
            late.warStartedDay = day;
        }
    }

    private static void expandFortificationFrontier(final PTSDCrisisState state) {
        if (state.late.phaseExpansionSelected) return;
        final List<StarSystemAPI> origins = new ArrayList<StarSystemAPI>();
        final List<PlanetAPI> candidates = new ArrayList<PlanetAPI>();
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (state.getSystemData(system.getId()).omegaControl >= .5f) {
                origins.add(system); continue;
            }
            if (system.hasTag(com.fs.starfarer.api.impl.campaign.ids.Tags.THEME_HIDDEN) ||
                    system.hasTag(com.fs.starfarer.api.impl.campaign.ids.Tags.SYSTEM_CUT_OFF_FROM_HYPER)) continue;
            boolean inhabited = false;
            for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system))
                if (!market.isPlanetConditionMarketOnly()) { inhabited = true; break; }
            if (inhabited) continue;
            for (PlanetAPI planet : system.getPlanets()) if (!planet.isStar()) {
                candidates.add(planet); break; // One occupation site per system.
            }
        }
        if (origins.isEmpty()) return;
        Collections.sort(candidates, new Comparator<PlanetAPI>() {
            public int compare(PlanetAPI a, PlanetAPI b) {
                return Float.compare(distance(a), distance(b));
            }
            private float distance(PlanetAPI planet) {
                float distance = Float.MAX_VALUE;
                for (StarSystemAPI origin : origins)
                    distance = Math.min(distance, Misc.getDistance(origin.getLocation(),
                            planet.getStarSystem().getLocation()));
                return distance;
            }
        });
        if (state.late.phaseExpansionPlanetIds == null)
            state.late.phaseExpansionPlanetIds = new ArrayList<String>();
        int desired = IIRT_Omega_ModPlugin.fortificationExpansionSystems;
        for (int i = 0; i < Math.min(desired, candidates.size()); i++) {
            PlanetAPI planet = candidates.get(i);
            planet.setFaction(PTSDCrisisProgress.getActiveFactionId(state));
            if (planet.getMarket() != null)
                planet.getMarket().setFactionId(PTSDCrisisProgress.getActiveFactionId(state));
            IIRT_Omega_Invasion.applyPlanetMutation(planet, 5);
            PTSDCrisisState.SystemData data = state.getSystemData(planet.getStarSystem().getId());
            data.omegaControl = 1f; data.humanControl = 0f; data.conversionLevel = 5;
            state.late.phaseExpansionPlanetIds.add(planet.getId());
        }
        state.late.phaseExpansionSelected = true;
        PTSDCrisisDevIntel.report("要塞阶段扩张", "免费新增 " +
                state.late.phaseExpansionPlanetIds.size() + "/" + desired + " 处占领区；不足表示没有更多合法无人星系",
                state.baseSystemId, null);
    }

    private static void produce(PTSDCrisisState state, float day, boolean dev) {
        PTSDLateState late = state.late;
        StringBuilder sources = new StringBuilder();
        float income = sourceIncome(state, day, sources);
        float capped = Math.min(IIRT_Omega_ModPlugin.lateProductionPerTurnCap,
                income * IIRT_Omega_ModPlugin.lateProductionMultiplier);
        // Scale active industry to sector population instead of budgeting a sector-wide war
        // with the former four-target cap. No functioning sources means no extra production.
        float buildCycles = state.phase == PTSDCrisisState.Phase.WAR ? 3f : 6f;
        float requiredPoints = Math.max(late.openingPointsRequired,late.regionalPointsRequired);
        float requiredAssets = Math.max(late.openingAssetsRequired,late.regionalAssetsRequired);
        if (income > 0f && requiredPoints > 0f)
            capped = Math.max(capped, requiredPoints / buildCycles * IIRT_Omega_ModPlugin.lateProductionMultiplier);
        float before = late.warPoints;
        float preparationCap = Math.max(IIRT_Omega_ModPlugin.lateProductionStorageCap, requiredPoints * 1.25f);
        late.warPoints = Math.min(preparationCap, before + capped);
        float credited = late.warPoints - before;
        late.warPointBelief = Math.min(preparationCap,
                late.warPointBelief + credited);
        float assetIncome = income <= 0f ? 0f : 8f +
                Math.min(IIRT_Omega_ModPlugin.lateProductionPerTurnCap, income) * 2f;
        StringBuilder planets = new StringBuilder();
        float planetIncome = planetAssetIncome(state, late.productionSettlementCount, planets);
        if (income > 0f && requiredAssets > 0f)
            assetIncome = Math.max(assetIncome,
                    requiredAssets / buildCycles * IIRT_Omega_ModPlugin.lateProductionMultiplier - planetIncome);
        float productionCurve = data.scripts.PTSDDifficultySettings.productionScale(state,day);
        assetIncome *= productionCurve;planetIncome *= productionCurve;
        float beforeAssets = late.strategicReserve;
        late.strategicReserve = Math.max(beforeAssets, Math.min(Math.max(STRATEGIC_ASSET_CAP, requiredAssets * 1.25f),
                beforeAssets + assetIncome + planetIncome));
        float assetCredited = late.strategicReserve - beforeAssets;
        late.productionSettlementCount++;
        late.lastProductionIncome = credited;
        late.lastProductionBreakdown = sources.toString() + "；原始 " + income +
                " ×倍率 " + IIRT_Omega_ModPlugin.lateProductionMultiplier +
                "，本期入账 " + credited + (credited < capped ? "（存量上限）" : "") +
                "；战略资产基础恢复+" + assetIncome + "；星球产出+" + planetIncome +
                planets + "；资产实际入账+" + assetCredited +
                (assetCredited < assetIncome + planetIncome ? "（存量上限5000）" : "") +
                "；上期后支出 准备-" + late.spentWarPointsSinceProduction +
                " / 战略资产-" + late.spentStrategicReserveSinceProduction +
                "（最近：" + (late.lastSpendingCause == null ? "无" : late.lastSpendingCause) + "）" +
                "；真实 " + late.warPoints + " / 估计 " + late.warPointBelief;
        late.spentWarPointsSinceProduction = 0f;
        late.spentStrategicReserveSinceProduction = 0f;
        late.lastSpendingCause = null;
        PTSDCrisisDevIntel.report(dev ? "DEV 手动生产结算" : "战争生产结算",
                late.lastProductionBreakdown,
                state.baseSystemId, null);
    }

    /** Every currently owned planet contributes five assets; completed industry adds ten to twenty. */
    private static float planetAssetIncome(PTSDCrisisState state, int settlement,
                                           StringBuilder breakdown) {
        float total = 0f;
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system == null) continue;
            for (PlanetAPI planet : system.getPlanets()) {
                if (!isOwnedPlanet(state, planet)) continue;
                int bonus = 0;
                if (planet.hasTag(DEVELOPED_TAG)) {
                    long seed = ((long) planet.getId().hashCode() * 73856093L) ^
                            ((long) settlement * 19349663L);
                    bonus = 10 + new Random(seed).nextInt(11);
                }
                total += 5f + bonus;
                breakdown.append("；").append(planet.getName()).append(" 控制+5");
                if (bonus > 0) breakdown.append("/完整产线+").append(bonus);
            }
        }
        return total;
    }

    private static boolean isOwnedPlanet(PTSDCrisisState state, PlanetAPI planet) {
        if (planet == null || planet.isStar() || planet.getStarSystem() == null ||
                planet.getFaction() == null) return false;
        String faction = planet.getFaction().getId();
        if (!IIRT_Omega_Invasion.WATCHER_FACTION.equals(faction) &&
                !IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(faction)) return false;
        PTSDCrisisState.SystemData data = state.systems.get(planet.getStarSystem().getId());
        return data != null && data.omegaControl >= .5f;
    }

    private static void ensureDevelopedPlanets(PTSDCrisisState state) {
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system == null) continue;
            for (PlanetAPI planet : system.getPlanets()) {
                if (!isOwnedPlanet(state, planet) || planet.hasTag(DEVELOPED_TAG)) continue;
                IIRT_Omega_Invasion.applyPlanetMutation(planet, 5);
                planet.addTag(DEVELOPED_TAG);
                planet.getSpec().setGlowTexture(
                        Global.getSettings().getSpriteName("hab_glows", "sindria"));
                planet.getSpec().setGlowColor(new Color(190, 72, 238, 230));
                planet.getSpec().setUseReverseLightForGlow(true);
                planet.applySpecChanges();
                state.getSystemData(system.getId()).conversionLevel = 5;
                PTSDCrisisDevIntel.report("地表产线完成", planet.getName() +
                        "：完整开发；此后每期控制+5，产线额外+10～20战略资产",
                        system.getId(), planet.getId());
            }
        }
    }

    /** Reward only a verified one-off mission; duplicate callbacks cannot create assets. */
    public static float creditStrategicAssets(PTSDCrisisState state, String key,
                                              float amount, String reason, String systemId) {
        if (state == null || state.late == null || key == null || amount <= 0f) return 0f;
        PTSDLateState late = state.late;
        if (!late.committedIds.add("asset-credit:" + key)) return 0f;
        float before = late.strategicReserve;
        float cap = key.startsWith("virtual-return:") || key.startsWith("war-wave-return:") || key.startsWith("mobilization-convoy:") ?
                before + amount : Math.max(STRATEGIC_ASSET_CAP, late.openingAssetsRequired * 1.25f);
        late.strategicReserve = Math.max(before,
                Math.min(cap, before + amount));
        float credited = late.strategicReserve - before;
        late.lastAssetCreditCause = reason;
        late.lastAssetCreditAmount = credited;
        late.lastAssetCreditDay = PTSDCrisisState.getDay();
        PTSDCrisisDevIntel.report("战略资产任务回收", reason + "；应收+" + amount +
                "，实收+" + credited + "，当前 " + late.strategicReserve,
                systemId, null);
        return credited;
    }

    public static void creditSuccessfulEvent(PTSDCrisisState.StrategicEvent event) {
        if (event == null || event.simulatedWarOperation || !event.successful ||
                !PTSDCrisisAPI.SIDE_OMEGA.equals(event.side) || event.id == null ||
                event.type == PTSDCrisisState.EventType.EXTERNAL) return;
        creditStrategicAssets(PTSDCrisisState.get(), "event:" + event.id, 5f,
                "完成 " + event.type + " 任务", event.targetSystemId);
    }

    private static float sourceIncome(PTSDCrisisState state, float day, StringBuilder sources) {
        float income = 0f;
        PTSDLateState late = state.late;
        MarketAPI base = state.resolveMarket(state.baseMarketId);
        if (base != null && (IIRT_Omega_Invasion.WATCHER_FACTION.equals(base.getFactionId()) ||
                IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(base.getFactionId()))) {
            income += IIRT_Omega_ModPlugin.lateBaseIncome;
            sources.append("母星+").append(IIRT_Omega_ModPlugin.lateBaseIncome);
        } else sources.append("母星0（未建成、失联或控制权不符）");
        for (PTSDCrisisState.SystemData data : state.systems.values()) {
            if (data == null || data.systemId == null || data.omegaControl < .5f) continue;
            if (data.systemId.equals(state.baseSystemId)) continue;
            StarSystemAPI outpost = state.resolveSystem(data.systemId);
            boolean hasPhysicalFacility = false;
            if (outpost != null) for (PlanetAPI planet : outpost.getPlanets()) {
                if (planet != null && planet.getFaction() != null &&
                        planet.hasTag("PTSD_mutated_planet") &&
                        (IIRT_Omega_Invasion.WATCHER_FACTION.equals(planet.getFaction().getId()) ||
                                IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(planet.getFaction().getId()))) {
                    hasPhysicalFacility = true;
                    break;
                }
            }
            if (data.conversionLevel >= 2 && hasPhysicalFacility) {
                income += IIRT_Omega_ModPlugin.lateOutpostIncome;
                sources.append("；据点 ").append(data.systemId).append("+")
                        .append(IIRT_Omega_ModPlugin.lateOutpostIncome);
            }
        }
        for (PTSDLateState.Fortress fortress : late.fortresses.values()) {
            if (fortress.destroyed) continue;
            float amount = IIRT_Omega_ModPlugin.lateFortressIncome[Math.max(1, Math.min(4, fortress.tier))];
            if (day < fortress.blockedUntilDay) amount *= .5f;
            income += amount;
            sources.append("；要塞 ").append(fortress.id).append("+").append(amount);
        }
        return income;
    }

    private static void reconcileEconomy(PTSDCrisisState state, float day) {
        PTSDLateState late = state.late;
        if (late.warEconomyVersion >= 1) return;
        MarketAPI base = state.resolveMarket(state.baseMarketId);
        if (base == null || (!IIRT_Omega_Invasion.WATCHER_FACTION.equals(base.getFactionId()) &&
                !IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(base.getFactionId()))) return;
        StringBuilder sources = new StringBuilder();
        float seed = Math.min(25f, sourceIncome(state, day, sources) * 2f);
        float previous = late.warPoints;
        late.warPoints = Math.max(previous, seed);
        late.warPointBelief = Math.min(IIRT_Omega_ModPlugin.lateProductionStorageCap,
                Math.max(0f, late.warPointBelief + late.warPoints - previous));
        late.warEconomyVersion = 1;
        late.nextProductionDay = day + IIRT_Omega_ModPlugin.lateProductionInterval;
        PTSDCrisisDevIntel.report("战争生产初始校准", sources + "；实际补足 " +
                (late.warPoints - previous) + "，下次生产于 " + late.nextProductionDay,
                state.baseSystemId, state.baseMarketId);
    }

    /** Dev panel: execute one ordinary ten-day production settlement immediately. */
    public static boolean devProduce(PTSDCrisisState state) {
        if (state == null || !Global.getSettings().isDevMode()) return false;
        float day = PTSDCrisisState.getDay();
        if (!state.late.initialized) initialize(state, day);
        reconcileEconomy(state, day);
        if (state.late.warEconomyVersion == 0) {
            PTSDCrisisDevIntel.report("战争生产结算", "未执行：母星尚无有效生产设施", state.baseSystemId, null);
            return false;
        }
        produce(state, day, true);
        return true;
    }

    /** Atomically fund a local conversion and its actual construction fleet. */
    public static boolean reserveConstruction(PTSDCrisisState state, String id, float fp) {
        if (state == null || state.late == null || id == null) return false;
        PTSDLateState late = state.late;
        if (late.committedIds.contains("construction:" + id)) return false;
        if ((state.phase == PTSDCrisisState.Phase.FORTIFICATION || state.phase == PTSDCrisisState.Phase.MOBILIZATION) &&
                (late.warPoints - 5f < late.openingPointsRequired ||
                        late.strategicReserve - fp < late.openingAssetsRequired)) return false;
        if (late.warPoints < 5f || late.strategicReserve < fp) return false;
        late.warPoints -= 5f;
        late.warPointBelief = Math.max(0f, late.warPointBelief - 5f);
        late.strategicReserve -= fp;
        recordSpending(state, "行星改造 " + id, 5f, fp);
        late.committedIds.add("construction:" + id);
        PTSDCrisisDevIntel.report("行星扩张支出", id + "：准备-5，战略资产-" + fp,
                state.baseSystemId, id);
        return true;
    }

    public static void recordSpending(PTSDCrisisState state, String cause, float points, float reserve) {
        if (state == null || state.late == null) return;
        state.late.spentWarPointsSinceProduction += Math.max(0f, points);
        state.late.spentStrategicReserveSinceProduction += Math.max(0f, reserve);
        state.late.lastSpendingCause = cause;
    }

    public static void disruptProduction(PTSDCrisisState state, String sourceId, float actualLoss,
                                         float beliefLoss, String systemId) {
        if (state == null || state.late == null) return;
        PTSDLateState late = state.late;
        if (!late.committedIds.add("disrupt:" + sourceId)) return;
        late.warPoints = Math.max(0f, late.warPoints - Math.max(0f, actualLoss));
        late.warPointBelief = Math.max(0f, Math.min(200f, late.warPointBelief - beliefLoss));
        PTSDCrisisDevIntel.report("战争生产受扰", sourceId + " / 真实-" + actualLoss +
                " / 估计-" + beliefLoss, systemId, null);
    }

    public static void checkFortificationExit(PTSDCrisisState state, float elapsed, float day) {
        if (state == null || state.phase != PTSDCrisisState.Phase.FORTIFICATION) return;
        PTSDLateState late = state.late;
        MarketAPI base = state.resolveMarket(state.baseMarketId);
        int sources = base != null &&
                (IIRT_Omega_Invasion.WATCHER_FACTION.equals(base.getFactionId()) ||
                        IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(base.getFactionId())) ? 1 : 0;
        for (PTSDLateState.Fortress fortress : late.fortresses.values()) {
            if (!fortress.destroyed && fortress.tier >= 2) sources++;
        }
        for (PTSDCrisisState.SystemData data : state.systems.values()) {
            if (data.omegaControl >= .5f && data.conversionLevel >= 2 &&
                    !data.systemId.equals(state.baseSystemId)) sources++;
        }
        boolean routeKnown = false;
        for (PTSDCrisisState.SystemData data : state.systems.values()) {
            if (data.hasNonCrisisColony && hasValidEvidence(data, day)) { routeKnown = true; break; }
        }
        if (elapsed >= 20f && sources >= 2 && routeKnown &&
                (late.warPoints >= IIRT_Omega_ModPlugin.lateSurpriseThreshold ||
                        (late.warPointBelief >= IIRT_Omega_ModPlugin.lateSurpriseThreshold &&
                                late.warPoints >= IIRT_Omega_ModPlugin.lateSurpriseMinimum))) {
            IIRT_Omega_Invasion.beginWar(state);
            if(state.phase==PTSDCrisisState.Phase.WAR)
                enqueueAlert(state, "surprise:" + Math.round(day), state.baseSystemId,
                        "联合警戒网紧急通报：多地侦测到未知武装编队接近。各港正核对来袭方向，民用航班暂停离港。", day);
            return;
        }
        if (elapsed >= 60f && ((late.warPoints >= IIRT_Omega_ModPlugin.lateMobilizationThreshold &&
                late.warPointBelief >= IIRT_Omega_ModPlugin.lateMobilizationThreshold && routeKnown) ||
                elapsed >= IIRT_Omega_ModPlugin.lateFortificationDeadline * 2f)) {
            IIRT_Omega_Invasion.transition(state, PTSDCrisisState.Phase.MOBILIZATION);
            Global.getSector().getCampaignUI().addMessage(
                    "多当局联合确认：边缘未知舰体事件具有统一来源。殖民地防务计划进入公开修订。", RED);
            PTSDCrisisIntel.ensureIntel();
            PTSDMobilizationIntel.ensureIntel();
        }
    }

    public static float affordableAttackStrength(PTSDCrisisState state) {
        if (state == null || state.late == null) return 0f;
        return Math.max(0f, Math.min(state.late.strategicReserve,
                (float)Math.floor(state.late.warPoints / 10f) * 80f));
    }

    public static boolean reserveAttack(PTSDCrisisState state, String eventId, float fp) {
        if (state == null || state.late == null || eventId == null) return false;
        PTSDLateState late = state.late;
        if (late.committedIds.contains("attack:" + eventId)) return false;
        float pointCost = Math.max(5f, (float) Math.ceil(fp / 80f) * 10f);
        if (late.warPoints < pointCost || late.strategicReserve < fp) return false;
        late.warPoints -= pointCost;
        late.warPointBelief = Math.max(0f, late.warPointBelief - pointCost);
        late.strategicReserve -= fp;
        recordSpending(state, "进攻部署 " + eventId, pointCost, fp);
        late.committedIds.add("attack:" + eventId);
        return true;
    }

    public static void onWarStarted(PTSDCrisisState state) {
        if (state == null || state.late == null) return;
        PTSDWarCampaignDirector.start(state);
    }

    private static String sourceFor(PTSDCrisisState state, StarSystemAPI target) {
        String bestId = state.baseSystemId;
        float bestDistance = Float.MAX_VALUE;
        for (PTSDCrisisState.SystemData data : state.systems.values()) {
            if (data == null || data.omegaControl < .5f) continue;
            if (data.systemId.equals(state.baseSystemId)) {
                MarketAPI base = state.resolveMarket(state.baseMarketId);
                if (base == null || (!IIRT_Omega_Invasion.WATCHER_FACTION.equals(base.getFactionId()) &&
                        !IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(base.getFactionId()))) continue;
            }
            StarSystemAPI source = state.resolveSystem(data.systemId);
            if (source == null) continue;
            float distance = Misc.getDistance(source.getLocation(), target.getLocation());
            if (distance < bestDistance) { bestDistance = distance; bestId = data.systemId; }
        }
        return bestId;
    }

    public static boolean hasValidEvidence(PTSDCrisisState.SystemData data, float day) {
        if (data == null || data.evidence == null || data.beliefConfidence < .08f) return false;
        for (PTSDCrisisState.EvidenceRecord evidence : data.evidence) {
            if (evidence != null && evidence.expiresDay > day && evidence.confidence >= .08f &&
                    evidence.colonySignal == 1) return true;
        }
        return false;
    }

    private static float score(PTSDCrisisState state, MarketAPI market) {
        StarSystemAPI system = market.getStarSystem();
        PTSDCrisisState.SystemData data = state.getSystemData(system.getId());
        PTSDCrisisFactionModifierAPI.Effects effect =
                PTSDCrisisFactionModifierAPI.get(market.getFactionId(), system.getId());
        float outer = Math.min(3f, system.getLocation().length() / 15000f);
        float threat = PTSDThreatDirector.localThreat(state, system.getId(), PTSDCrisisState.getDay());
        PTSDCrisisState.PlayerMarker marker = state.playerMarkers.get(system.getId());
        float prepared = 160f / (80f + Math.max(0f, data.observedMarketDefense) + Math.max(0f, data.observedFleetStrength)); // Only uploaded enemy estimates influence selection.
        return effect.targetWeight * prepared * (1f + outer + Math.min(1f, market.getSize() / 8f)) *
                Math.max(.25f, data.beliefConfidence) * (1f - Math.min(.65f, threat / 140f));
    }

    public static boolean devAdvanceOneFront(PTSDCrisisState state) {
        if (state == null || state.phase != PTSDCrisisState.Phase.WAR ||
                !Global.getSettings().isDevMode()) return false;
        float day = PTSDCrisisState.getDay();
        for (PTSDLateState.Front front : state.late.fronts.values()) {
            if (front == null || front.resolved) continue;
            if (front.operationId != null) {
                PTSDCrisisState.StrategicEvent event=state.getEvent(front.operationId);
                if(event!=null)event.resolveDay=Math.max(day,event.operationRescueDeadline);
                return event!=null;
            }
            front.nextStrikeDay = day;
            advanceFronts(state, day);
            return true;
        }
        return false;
    }

    private static void advanceFronts(PTSDCrisisState state, float day) {
        PTSDLateState late = state.late;
        for (PTSDLateState.Front front : late.fronts.values()) {
            if (front != null && front.operationId != null) continue;
            if (front == null || front.resolved || day < front.nextStrikeDay) continue;
            MarketAPI market = state.resolveMarket(front.marketId);
            if (market == null || market.isPlanetConditionMarketOnly()) {
                front.resolved = true;
                continue;
            }
            PTSDCrisisFactionModifierAPI.Effects effect =
                    PTSDCrisisFactionModifierAPI.get(market.getFactionId(), front.systemId);
            if (effect.immune) { front.resolved = true; continue; }
            PTSDCrisisState.SystemData data = state.getSystemData(front.systemId);
            float defense = market.getSize() * 8f;
            defense += front.defense;
            if (data != null) defense += Math.max(0f, data.groundTruthMarketDefense * .3f);
            PTSDCrisisState.PlayerMarker marker = state.playerMarkers.get(front.systemId);
            // Defense requests have no strength multiplier; only committed forces count.
            boolean evacuating = marker != null && "EVACUATE".equals(marker.type);
            if (evacuating && front.strikes == 0) {
                enqueueAlert(state, front.id + ":evac", front.systemId,
                        market.getName() + "：撤离令先于第一次轨道打击生效；人口转运降低后续毁伤累积。", day);
            }
            defense *= effect.humanStrength;
            float attack = front.attackBudget * effect.omegaStrength;
            front.strikes++;
            front.barrage += (attack > defense ? 26f : 12f) * (evacuating ? .75f : 1f);
            market.getStability().modifyFlat("PTSD_first_wave_" + front.id,
                    -Math.min(12f, front.barrage / 8f), "精神创伤轨道打击");
            front.nextStrikeDay = day + (IIRT_Omega_ModPlugin.lateFrontStrikeMin +
                    RANDOM.nextFloat() * (IIRT_Omega_ModPlugin.lateFrontStrikeMax -
                            IIRT_Omega_ModPlugin.lateFrontStrikeMin)) /
                    IIRT_Omega_ModPlugin.lateOffensiveTempo;
            enqueueAlert(state, front.id + ":" + front.strikes, front.systemId,
                    market.getName() + "：第 " + front.strikes + " 次轨道火力报告；稳定度持续下降。", day);
            boolean rescueWindowOpen = market.isPlayerOwned() &&
                    (front.playerRescueDeadline <= 0f || day < front.playerRescueDeadline);
            if (!rescueWindowOpen && front.strikes >= 3 && attack > defense &&
                    front.barrage >= (evacuating ? 95f : 75f)) {
                String owner = market.getFactionId();
                PTSDFortressEscapeScript.abandonMarket(market);
                front.lost = true;
                front.resolved = true;
                PTSDGeneralStaffDirector.recordContribution(state, front.id + ":loss", "LOSS",
                        owner, front.systemId, 0f);
                enqueueAlert(state, front.id + ":lost", front.systemId,
                        market.getName() + "：市场登记已转为永久废弃；此前的撤离人数仍在复核。", day);
            } else if (!rescueWindowOpen && (front.strikes >= 6 || day >= late.warStartedDay + 30f)) {
                front.resolved = true;
                market.getStability().unmodify("PTSD_first_wave_" + front.id);
                PTSDGeneralStaffDirector.recordContribution(state, front.id + ":saved", "SAVED",
                        market.isPlayerOwned() ? "player" : market.getFactionId(), front.systemId, 30f);
                enqueueAlert(state, front.id + ":saved", front.systemId,
                        market.getName() + "：港口仍能接收船只；敌方主攻已转移。", day);
            }
        }
    }

    public static void enqueueAlert(PTSDCrisisState state, String id, String systemId,
                                    String text, float day) {
        if (state == null || id == null || !state.late.committedIds.add("alert:" + id)) return;
        PTSDLateState.Alert alert = new PTSDLateState.Alert();
        alert.id = id;
        alert.systemId = systemId;
        alert.text = text;
        alert.source = "联合警戒网";
        alert.day = day;
        state.late.alerts.add(alert);
    }

    private static void deliverAlerts(PTSDCrisisState state, float day) {
        if (day < state.late.nextWarningDay) return;
        int delivered = 0;
        for (PTSDLateState.Alert alert : new java.util.ArrayList<PTSDLateState.Alert>(state.late.alerts)) {
            if (alert == null || alert.delivered || alert.day > day ||
                    delivered >= Math.max(1, Math.round(8f * IIRT_Omega_ModPlugin.lateAlertDensity))) continue;
            alert.delivered = true;
            for (PTSDLateState.Front front : state.late.fronts.values()) {
                // The first confirmed bombardment report is reliable; earlier civilian silence isn't.
                if (front == null || front.resolved || front.playerRescueDeadline > 0f ||
                        !alert.id.matches(java.util.regex.Pattern.quote(front.id) + ":[1-9][0-9]*")) continue;
                MarketAPI market = state.resolveMarket(front.marketId);
                if (market == null || !market.isPlayerOwned()) continue;
                CampaignFleetAPI player = Global.getSector().getPlayerFleet();
                float travel = 0f;
                if (player != null) {
                    float burn = Math.max(1f, player.getFleetData().getMinBurnLevel());
                    float perDay = Math.max(.01f, Misc.getLYPerDayAtBurn(player, burn));
                    travel = Misc.getDistanceLY(player.getLocationInHyperspace(), market.getLocationInHyperspace()) / perDay;
                }
                front.playerRescueWarningDay = day;
                front.lockedTravelDays = travel;
                front.playerRescueDeadline = day + PTSDTaskRules.rescueWindow(travel);
                enqueueAlert(state, front.id + ":rescue-window", front.systemId,
                        market.getName() + "：首次可靠炮击预警已确认，救援窗口约" +
                                Math.round(front.playerRescueDeadline - day) + "日；航程估计已锁定。", day);
            }
            Global.getSector().getCampaignUI().addMessage(alert.text, RED);
            delivered++;
        }
        state.late.nextWarningDay = day + (delivered > 0 ?
                .15f / IIRT_Omega_ModPlugin.lateAlertDensity : 1f);
    }
}
