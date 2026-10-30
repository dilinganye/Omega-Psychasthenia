package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;
import data.scripts.IIRT_Omega_ModPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** World-bound CSV stories. Every row names a real target or a persistent survey area. */
public final class PTSDConditionalNewsDirector {
    private PTSDConditionalNewsDirector() { }

    private static final class Target {
        final StarSystemAPI system;
        final MarketAPI market;
        final SectorEntityToken anchor;
        Target(StarSystemAPI system, MarketAPI market, SectorEntityToken anchor) {
            this.system = system; this.market = market; this.anchor = anchor;
        }
    }

    public static void advance(PTSDCrisisState state, float day) {
        if (state == null || state.phase == PTSDCrisisState.Phase.DORMANT ||
                state.phase == PTSDCrisisState.Phase.ENDED) return;
        for (PTSDCrisisState.CrisisIncident incident : state.incidents) {
            if (incident == null || incident.scenarioTrigger == null || incident.scenarioResolved) continue;
            // Specialized scenes own their entities, deadlines and investigation outcome.
            if (PTSDNewsScene.manages(incident) && incident.investigationOutcome == 1 &&
                    !incident.investigationResolved) continue;
            if (incident.targetEntityId != null &&
                    Global.getSector().getEntityById(incident.targetEntityId) == null) {
                onExpired(state, incident, "现场目标已消失");
                continue;
            }
            if (day >= incident.scenarioDeadline)
                onExpired(state, incident, "现场干预窗口结束");
            else if (incident.playerOperationAccepted) advancePlayerOperation(state, incident, day);
        }
        if (state.phase != PTSDCrisisState.Phase.RECON &&
                state.phase != PTSDCrisisState.Phase.EXPANSION &&
                state.phase != PTSDCrisisState.Phase.FORTIFICATION) return;
        if (state.nextConditionalNewsDay <= 0f) state.nextConditionalNewsDay = day + 2f;
        if (day < state.nextConditionalNewsDay) return;
        float density = state.phase == PTSDCrisisState.Phase.RECON ? IIRT_Omega_ModPlugin.reconNewsDensity :
                state.phase == PTSDCrisisState.Phase.EXPANSION ? IIRT_Omega_ModPlugin.expansionNewsDensity :
                        IIRT_Omega_ModPlugin.fortressNewsDensity;
        state.nextConditionalNewsDay = day + Math.max(2f, 8f / Math.max(.1f, density));
        List<PTSDCrisisIncidentManager.ScriptedTemplate> templates =
                PTSDCrisisIncidentManager.getTemplates("CONDITIONAL", state.phase);
        if (templates.isEmpty()) return;
        Random random = new Random((long) day * 431L + state.phase.ordinal() * 19L);
        float total = 0f;
        List<Target> targets = new ArrayList<Target>();
        List<PTSDCrisisIncidentManager.ScriptedTemplate> choices =
                new ArrayList<PTSDCrisisIncidentManager.ScriptedTemplate>();
        for (PTSDCrisisIncidentManager.ScriptedTemplate template : templates) {
            if (state.conditionalNewsSeen.contains(template.id) ||
                    day - state.phaseStartedDay < template.minDay) continue;
            Target target = pickTarget(state, template.trigger, template.siteTemplate, random);
            if (target == null) continue;
            choices.add(template);
            targets.add(target);
            total += Math.max(.1f, template.priority) * template.eventWeight;
        }
        if (choices.isEmpty()) return;
        // This director is the R2/E2/F2 pool; scaling only relative row weights would
        // change nothing. A failed ten-percent draw leaves every card eligible.
        if (random.nextFloat() >= .10f) return;
        float roll = random.nextFloat() * total;
        for (int i = 0; i < choices.size(); i++) {
            roll -= Math.max(.1f, choices.get(i).priority) * choices.get(i).eventWeight;
            if (roll > 0f && i + 1 < choices.size()) continue;
            publish(state, choices.get(i), targets.get(i), false);
            return;
        }
    }

    public static String unavailableReason(PTSDCrisisState state, String id) {
        if (state == null) return "危机状态不存在";
        PTSDCrisisIncidentManager.ScriptedTemplate template = PTSDCrisisIncidentManager.getTemplate(id);
        if (template == null || !"CONDITIONAL".equals(template.delivery)) return "CSV 中没有该条件事件";
        if (!templatePhase(template, state.phase)) return "当前阶段与 CSV 阶段不符";
        if (state.conditionalNewsSeen.contains(id)) return "该独立事件已投放";
        float remaining = template.minDay - (PTSDCrisisState.getDay() - state.phaseStartedDay);
        if (remaining > 0f) return "距离最早投放还需 " + Math.round(remaining) + " 日";
        if (!knownTrigger(template.trigger)) return "数据错误：未知触发类型 " + template.trigger;
        if (pickTarget(state, template.trigger, template.siteTemplate, new Random(id.hashCode())) == null)
            return "缺少触发类型 " + template.trigger + " 所需的现存目标";
        return "可触发";
    }

    static boolean knownTrigger(String trigger) {
        return trigger != null && java.util.Arrays.asList("SCOUT", "REPORT", "PLAYER_TRAIL", "JUMP",
                "RELAY", "MARKET", "WILDERNESS", "BLACK_HOLE", "BASE", "OUTPOST", "TRANSPORT",
                "FORTRESS", "FORT_SMALL", "FORT_STABLE", "FORT_MEDIUM", "FORT_EDEN", "FORT_BLOCKED",
                "FORT_MOVING").contains(trigger);
    }

    public static boolean force(PTSDCrisisState state, String id) {
        String reason = unavailableReason(state, id);
        if (!"可触发".equals(reason)) {
            PTSDCrisisDevIntel.report("条件新闻未投放", id + "：" + reason, null, null);
            return false;
        }
        PTSDCrisisIncidentManager.ScriptedTemplate template = PTSDCrisisIncidentManager.getTemplate(id);
        Target target = pickTarget(state, template.trigger, template.siteTemplate, new Random(Misc.genUID().hashCode()));
        return target != null && publish(state, template, target, true);
    }

    /** A true, private Je lead from the same target picker and one-shot card registry. */
    public static PTSDCrisisState.CrisisIncident forJe(PTSDCrisisState state, String id) {
        if (!PTSDCrisisIncidentManager.enabled(id) || !"可触发".equals(unavailableReason(state, id))) return null;
        PTSDCrisisIncidentManager.ScriptedTemplate template =
                PTSDCrisisIncidentManager.getTemplate(id);
        if (template == null || !(id.startsWith("R2-") || id.startsWith("F2-"))) return null;
        Target target = pickTarget(state, template.trigger, template.siteTemplate,
                new Random(Misc.genUID().hashCode()));
        if (target == null) return null;
        PTSDCrisisState.CrisisIncident incident = PTSDCrisisIncidentManager.publishConditional(
                state, id, target.system, target.market, target.anchor, false, true);
        if (incident != null) {
            state.conditionalNewsSeen.add(id);
            PTSDCrisisDevIntel.report("Je 私下委托", id + "：属实；未刊入公开新闻",
                    incident.targetSystemId, incident.targetEntityId);
        }
        return incident;
    }

    /** Explicit DEV entry bypasses seen/cooldown only, retaining the real target prerequisites. */
    static PTSDCrisisState.CrisisIncident forSceneTest(PTSDCrisisState state, String id) {
        if (!Global.getSettings().isDevMode()) return null;
        PTSDCrisisIncidentManager.ScriptedTemplate template=PTSDCrisisIncidentManager.getTemplate(id);
        if(template==null||!PTSDCrisisIncidentManager.matchesPhase(id,state.phase)||!"CONDITIONAL".equals(template.delivery)||!knownTrigger(template.trigger))return null;
        Target target=pickTarget(state,template.trigger,template.siteTemplate,new Random(Misc.genUID().hashCode()));
        return target==null?null:PTSDCrisisIncidentManager.publishConditional(state,id,target.system,target.market,target.anchor,false,true);
    }

    private static boolean templatePhase(PTSDCrisisIncidentManager.ScriptedTemplate template,
                                         PTSDCrisisState.Phase phase) {
        return PTSDCrisisIncidentManager.matchesPhase(template.id,phase);
    }

    private static boolean publish(PTSDCrisisState state,
                                   PTSDCrisisIncidentManager.ScriptedTemplate template,
                                   Target target, boolean forced) {
        if (state == null || target == null || target.system == null) return false;
        float falseRate = state.phase == PTSDCrisisState.Phase.RECON ? IIRT_Omega_ModPlugin.reconNewsFalseRate :
                state.phase == PTSDCrisisState.Phase.EXPANSION ? IIRT_Omega_ModPlugin.expansionNewsFalseRate :
                        IIRT_Omega_ModPlugin.fortressNewsFalseRate;
        boolean falseReport = !forced && new Random(
                (template.id + ":" + Math.round(PTSDCrisisState.getDay())).hashCode())
                .nextFloat() < falseRate;
        PTSDCrisisState.CrisisIncident incident = PTSDCrisisIncidentManager.publishConditional(
                state, template.id, target.system, target.market, target.anchor, falseReport);
        if (incident == null) return false;
        incident.devForced = forced;
        state.conditionalNewsSeen.add(template.id);
        // E2 remains reportage: publication must not spawn the retired local event chains.
        if (forced && incident.investigable) {
            PTSDCrisisAPI.recordNewsIncident(incident.id);
            PTSDNewsSiteManager.materialize(state, incident,
                    new Random(incident.id.hashCode()), false, true);
        }
        PTSDCrisisDevIntel.report("条件新闻投放", template.id + " / " + template.trigger +
                (falseReport ? " / 待查伪报" : " / 实况") + " / 目标 " +
                (target.anchor == null ? target.system.getName() : target.anchor.getFullName()),
                target.system.getId(), target.anchor == null ? null : target.anchor.getId());
        return true;
    }

    private static Target pickTarget(PTSDCrisisState state, String trigger, String templates, Random random) {
        if (trigger == null || Global.getSector() == null) return null;
        List<Target> choices = new ArrayList<Target>();
        if ("SCOUT".equals(trigger) || "REPORT".equals(trigger)) {
            for (StarSystemAPI system : Global.getSector().getStarSystems())
                for (CampaignFleetAPI fleet : system.getFleets())
                    if (fleet != null && fleet.isAlive() && fleet.hasTag(IIRT_Omega_Invasion.SCOUT_TAG) &&
                            (!"REPORT".equals(trigger) || hasPending(state, fleet.getId())))
                        choices.add(new Target(system, null, fleet));
        } else if ("PLAYER_TRAIL".equals(trigger)) {
            StarSystemAPI system = state.resolveSystem(PTSDReconReportDirector.knownPlayerTrail(state,
                    PTSDCrisisState.getDay()));
            if (system != null) choices.add(new Target(system, null, system.getCenter()));
        } else if (trigger.startsWith("FORT") || "TRANSPORT".equals(trigger)) {
            if (state.late != null) for (PTSDLateState.Fortress fortress : state.late.fortresses.values()) {
                if (fortress == null || fortress.destroyed) continue;
                boolean matches = !fortress.inTransit && ("FORTRESS".equals(trigger) ||
                        "FORT_SMALL".equals(trigger) && fortress.tier == 1 ||
                        "FORT_STABLE".equals(trigger) && fortress.tier == 2 ||
                        "FORT_MEDIUM".equals(trigger) && fortress.tier == 3 ||
                        "FORT_EDEN".equals(trigger) && fortress.tier == 4 ||
                        "FORT_BLOCKED".equals(trigger) && fortress.blockedUntilDay > PTSDCrisisState.getDay()) ||
                        "FORT_MOVING".equals(trigger) && fortress.inTransit;
                if ("TRANSPORT".equals(trigger)) {
                    for (String id : fortress.transportIds) {
                        SectorEntityToken transport = Global.getSector().getEntityById(id);
                        StarSystemAPI system = transport == null ? null : transport.getStarSystem();
                        if (system != null) choices.add(new Target(system, null, transport));
                    }
                } else if (matches) {
                    SectorEntityToken anchor = fortress.fleetId == null ? null :
                            Global.getSector().getEntityById(fortress.fleetId);
                    StarSystemAPI system = state.resolveSystem(fortress.currentSystemId);
                    if (system != null) choices.add(new Target(system, null,
                            anchor == null ? system.getCenter() : anchor));
                }
            }
        } else if ("BLACK_HOLE".equals(trigger)) {
            for (StarSystemAPI system : Global.getSector().getStarSystems())
                if (system != null && system.getStar() != null &&
                        system.getStar().getSpec().isBlackHole() &&
                        !system.hasTag(Tags.THEME_HIDDEN))
                    choices.add(new Target(system, null, system.getStar()));
        } else if ("BASE".equals(trigger)) {
            StarSystemAPI system = state.resolveSystem(state.baseSystemId);
            if (system != null) choices.add(new Target(system, state.resolveMarket(state.baseMarketId),
                    state.resolveMarket(state.baseMarketId) == null ? system.getCenter() :
                            state.resolveMarket(state.baseMarketId).getPrimaryEntity()));
        } else if ("OUTPOST".equals(trigger)) {
            for (PTSDCrisisState.SystemData data : state.systems.values()) {
                if (data == null || data.systemId == null || data.omegaControl < .5f ||
                        data.systemId.equals(state.baseSystemId)) continue;
                StarSystemAPI system = state.resolveSystem(data.systemId);
                if (system != null) choices.add(new Target(system, null, system.getCenter()));
            }
        } else {
            for (StarSystemAPI system : Global.getSector().getStarSystems()) {
                if (system == null || system.hasTag(Tags.THEME_HIDDEN) ||
                        system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)) continue;
                if ("RELAY".equals(trigger)) {
                    for (SectorEntityToken relay : system.getEntitiesWithTag(Tags.COMM_RELAY))
                        choices.add(new Target(system, null, relay));
                } else if ("JUMP".equals(trigger)) {
                    for (SectorEntityToken jump : system.getJumpPoints())
                        choices.add(new Target(system, null, jump));
                } else if ("WILDERNESS".equals(trigger)) {
                    if (eligibleMarkets(system).isEmpty()) choices.add(new Target(system, null, system.getCenter()));
                } else if ("MARKET".equals(trigger)) {
                    for (MarketAPI market : eligibleMarkets(system))
                        choices.add(new Target(system, market, market.getPrimaryEntity()));
                }
            }
        }
        for (java.util.Iterator<Target> it=choices.iterator();it.hasNext();)
            if (!PTSDNewsScene.accepts(templates,state,it.next().system)) it.remove();
        return choices.isEmpty() ? null : choices.get(random.nextInt(choices.size()));
    }

    private static boolean hasPending(PTSDCrisisState state, String fleetId) {
        for (PTSDCrisisState.PendingReconReport report : state.pendingReconReports)
            if (report != null && fleetId.equals(report.sourceFleetId)) return true;
        return false;
    }

    private static List<MarketAPI> eligibleMarkets(StarSystemAPI system) {
        List<MarketAPI> result = new ArrayList<MarketAPI>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system))
            if (market != null && !market.isPlanetConditionMarketOnly() && market.getPrimaryEntity() != null &&
                    !IIRT_Omega_Invasion.WATCHER_FACTION.equals(market.getFactionId()) &&
                    !IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(market.getFactionId())) result.add(market);
        return result;
    }

    public static void onInvestigated(PTSDCrisisState state, PTSDCrisisState.CrisisIncident incident) {
        if (state == null || incident == null || incident.scenarioTrigger == null || incident.scenarioResolved) return;
        if (incident.cardId != null && incident.cardId.startsWith("E2-")) {
            incident.scenarioResolved = true;
            PTSDE2Director.onInvestigated(state, incident);
            return;
        }
        if (!incident.investigationReal || incident.scenarioFalse) {
            incident.scenarioResolved = true;
            incident.effectSummary = incident.investigationResult == PTSDTaskRules.InvestigationResult.DISPROVED ?
                    "现场记录证伪了公开报道；相关恐慌已撤销。" :
                    "未取得足以验证或证伪报道的记录；基础调查结束。";
            return;
        }
        if ("VERIFY".equals(incident.scenarioEffect) || incident.scenarioEffect == null) {
            incident.scenarioResolved = true;
            incident.effectSummary = "现场记录已核实并自动归档。";
        } else {
            incident.effectSummary = "基础取证完成；后续行动尚未下达：" + incident.scenarioAction;
        }
    }

    public static boolean canChooseOperation(PTSDCrisisState.CrisisIncident incident) {
        return incident != null && incident.scenarioTrigger != null && !incident.scenarioResolved &&
                !incident.playerOperationAccepted && incident.investigationResult ==
                PTSDTaskRules.InvestigationResult.CONFIRMED && !incident.scenarioFalse &&
                (incident.cardId == null || !incident.cardId.startsWith("E2-"));
    }

    private static boolean isCommunication(PTSDCrisisState.CrisisIncident incident) {
        return "SHARE_INTEL".equals(incident.scenarioEffect) || "COORDINATE".equals(incident.scenarioEffect);
    }

    public static boolean startPlayerOperation(PTSDCrisisState state, String incidentId) {
        PTSDCrisisState.CrisisIncident incident = PTSDCrisisAPI.getIncident(incidentId);
        if (state == null || !canChooseOperation(incident) || PTSDCrisisState.getDay() >= incident.scenarioDeadline)
            return false;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (!isCommunication(incident)) {
            SectorEntityToken target = PTSDCrisisAPI.resolveIncidentTarget(incident);
            if (player == null || target == null || player.getContainingLocation() != target.getContainingLocation() ||
                    Misc.getDistance(player, target) > Math.max(1800f, target.getRadius() + 1200f) ||
                    player.getCargo().getSupplies() < 10f) return false;
            player.getCargo().removeSupplies(10f);
        }
        incident.playerOperationAccepted = true;
        incident.playerOperationLastDay = PTSDCrisisState.getDay();
        incident.effectSummary = isCommunication(incident) ? "正在提交经核验的记录。" :
                "行动已下达；补给支出10，需持续驻留1日。离开现场将重新计时。";
        if (isCommunication(incident)) completePlayerOperation(state, incident);
        return true;
    }

    private static void advancePlayerOperation(PTSDCrisisState state, PTSDCrisisState.CrisisIncident incident, float day) {
        if (day >= incident.scenarioDeadline) return;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        SectorEntityToken target = PTSDCrisisAPI.resolveIncidentTarget(incident);
        boolean nearby = player != null && target != null && player.getContainingLocation() == target.getContainingLocation() &&
                Misc.getDistance(player, target) <= Math.max(1800f, target.getRadius() + 1200f);
        float elapsed = Math.max(0f, day - incident.playerOperationLastDay);
        incident.playerOperationLastDay = day;
        incident.playerOperationProgressDays = nearby ? incident.playerOperationProgressDays + elapsed : 0f;
        if (nearby && incident.playerOperationProgressDays >= 1f) completePlayerOperation(state, incident);
    }

    private static void completePlayerOperation(PTSDCrisisState state, PTSDCrisisState.CrisisIncident incident) {
        if (incident.playerOperationCompleted || incident.scenarioResolved) return;
        incident.playerOperationCompleted = true;
        incident.scenarioResolved = true;
        PTSDInvestigationRewards.complete(incident);
        String result = applyPlayerEffect(state, incident);
        String response = applyResponse(state, incident);
        incident.effectSummary = PTSDPlayerNewsText.summary(incident);
        PTSDCrisisDevIntel.report("条件事件现场结算", incident.cardId + "：" + result + "；敌方应对：" + response,
                incident.targetSystemId, incident.targetEntityId);
    }

    public static void onExpired(PTSDCrisisState state, PTSDCrisisState.CrisisIncident incident, String why) {
        if (incident == null || incident.scenarioTrigger == null || incident.scenarioResolved) return;
        incident.scenarioResolved = true;
        if (!incident.investigationResolved) {
            incident.investigationResult = "现场目标已消失".equals(why) ?
                    PTSDTaskRules.InvestigationResult.TARGET_LOST : PTSDTaskRules.InvestigationResult.EXPIRED;
            incident.investigationReal = false;
        }
        incident.investigationResolved = true;
        if (incident.cardId != null && incident.cardId.startsWith("E2-")) {
            PTSDE2Director.onExpired(state, incident, why);
            return;
        }
        if (state != null && incident.siteMaterialized)
            PTSDNewsSiteManager.cleanup(state, incident, true);
        incident.effectSummary = PTSDPlayerNewsText.summary(incident);
        String internalOutcome = why + "；" + incident.scenarioRemoteOutcome;
        if (state != null && !incident.scenarioFalse && !"现场目标已消失".equals(why) &&
                state.late != null && state.late.strategicReserve >= 2f &&
                ("RELAY".equals(incident.scenarioTrigger) ||
                        "JUMP".equals(incident.scenarioTrigger) ||
                        "MARKET".equals(incident.scenarioTrigger) ||
                        "WILDERNESS".equals(incident.scenarioTrigger))) {
            state.late.strategicReserve -= 2f;
            state.nextScoutDay = Math.min(state.nextScoutDay, PTSDCrisisState.getDay() + 3f);
            internalOutcome += "；敌方调拨侦察预算2点，其余任务可用额度下降";
        }
        PTSDCrisisDevIntel.report("条件事件远程结局", incident.cardId + "：" + internalOutcome,
                incident.targetSystemId, incident.targetEntityId);
    }

    private static String applyPlayerEffect(PTSDCrisisState state, PTSDCrisisState.CrisisIncident incident) {
        String effect = incident.scenarioEffect == null ? "VERIFY" : incident.scenarioEffect;
        String systemId = incident.sceneSourceSystemId==null?incident.targetSystemId:incident.sceneSourceSystemId;
        if ("JAM_UPLOAD".equals(effect)) {
            int removed = 0;
            for (java.util.Iterator<PTSDCrisisState.PendingReconReport> it = state.pendingReconReports.iterator(); it.hasNext();) {
                PTSDCrisisState.PendingReconReport report = it.next();
                if (report != null && incident.targetEntityId != null &&
                        incident.targetEntityId.equals(report.sourceFleetId)) { it.remove(); removed++; }
            }
            return removed > 0 ? "截断 " + removed + " 份尚未上传的舰载报告" : "上传窗口已过去，取得了回执副本";
        }
        if ("DELAY_SCOUT".equals(effect)) {
            state.nextScoutDay += 3f;
            return "下一支侦察队推迟约三日";
        }
        if ("SHARE_INTEL".equals(effect)) {
            PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.HUMAN_AWARENESS, 3f,
                    incident.cardId + ":verified", systemId);
            return "可核实坐标进入人类情报网；人类认知提高";
        }
        if ("COORDINATE".equals(effect)) {
            PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.HUMAN_COHESION, 3f,
                    incident.cardId + ":verified", systemId);
            return "地方协同提高，未凭空增加舰船";
        }
        if ("DEFEND".equals(effect)) {
            state.putMarker(systemId, "DEFEND", 1.2f);
            return "指定星系的预设防御标记已更新";
        }
        if ("MISDIRECT".equals(effect)) {
            PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.HUMAN_AWARENESS, 3f,
                    incident.cardId + ":false-axis", systemId);
            if ("PLAYER_TRAIL".equals(incident.scenarioTrigger) &&
                    systemId.equals(state.reconPlayerTrailSystemId))
                state.reconPlayerTrailExpiresDay = PTSDCrisisState.getDay();
            return "已辨认诱饵并共享真实坐标；敌方既有档案仍按自身证据有效期衰减";
        }
        if ("DISRUPT_OUTPOST".equals(effect)) {
            PTSDLateWarDirector.disruptProduction(state, incident.id, 3f, 0f, systemId);
            return "现存生产设施的下一次结算受阻，已记入准备点账本";
        }
        if ("DELAY_FORTRESS".equals(effect) || "DELAY_TRANSPORT".equals(effect)) {
            for (PTSDLateState.Fortress fortress : state.late.fortresses.values())
                if (fortress != null && !fortress.destroyed && systemId.equals(fortress.currentSystemId)) {
                    fortress.blockedUntilDay = Math.max(fortress.blockedUntilDay,
                            PTSDCrisisState.getDay() + ("DELAY_TRANSPORT".equals(effect) ? 2f : 4f));
                    return "要塞工程阻滞已写入实际工程账本";
                }
            return "原施工目标已离场，仅保留现场记录";
        }
        if ("EVACUATE".equals(effect)) {
            state.putMarker(systemId, "EVACUATE", 1.2f);
            return "当地撤离标记已登记，首波结算会复核";
        }
        PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.HUMAN_AWARENESS, 1f,
                incident.cardId + ":verified", systemId);
        return "现场资料已核实并归档";
    }

    private static String applyResponse(PTSDCrisisState state, PTSDCrisisState.CrisisIncident incident) {
        if (incident.scenarioFalse || state.late == null ||
                incident.scenarioOmegaResponse == null || incident.scenarioOmegaResponse.isEmpty())
            return "未取得足够证据或资源，暂无额外调兵";
        String trigger = incident.scenarioTrigger;
        if ("BASE".equals(trigger) || "OUTPOST".equals(trigger) ||
                "TRANSPORT".equals(trigger) || trigger.startsWith("FORT")) {
            String eventId = "news-response:" + incident.id;
            if (!PTSDLateWarDirector.reserveAttack(state, eventId, 30f))
                return "准备点或可用舰队额度不足，未能增派护卫";
            PTSDCrisisState.StrategicEvent event = state.addEvent(
                    PTSDCrisisState.EventType.GARRISON, PTSDCrisisAPI.SIDE_OMEGA,
                    PTSDCrisisProgress.getActiveFactionId(state), state.baseSystemId,
                    incident.targetSystemId, incident.targetEntityId, 30f, 3f);
            event.description = incident.cardId + "：现场干预后的有成本回防";
            return incident.scenarioOmegaResponse + "（准备点-10、战略资产-30；其他部署额度同步下降）";
        }
        if (state.late.strategicReserve < 3f) return "可用侦察额度不足，未执行调整";
        state.late.strategicReserve -= 3f;
        state.nextScoutDay = Math.min(state.nextScoutDay, PTSDCrisisState.getDay() + 4f);
        PTSDLateWarDirector.recordSpending(state, "条件事件侦察调整 " + incident.cardId, 0f, 3f);
        return incident.scenarioOmegaResponse + "（侦察调令提前，战略资产-3）";
    }
}
