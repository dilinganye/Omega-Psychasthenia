package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;
import data.scripts.IIRT_Omega_ModPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Scout-only, delayed observations. A public news item never enters this queue. */
public final class PTSDReconReportDirector {
    private PTSDReconReportDirector() { }

    public static void advance(PTSDCrisisState state, float day) {
        if (state == null) return;
        if (state.phase == PTSDCrisisState.Phase.DORMANT && state.pendingReconReports.isEmpty() &&
                state.reconLowActivityUntil.isEmpty()) return;
        for (Iterator<PTSDCrisisState.PendingReconReport> it = state.pendingReconReports.iterator(); it.hasNext();) {
            PTSDCrisisState.PendingReconReport report = it.next();
            if (report == null) { it.remove(); continue; }
            if (day < report.uploadDay) continue;
            Object entity = report.sourceFleetId == null ? null : Global.getSector().getEntityById(report.sourceFleetId);
            if (!(entity instanceof CampaignFleetAPI) || !((CampaignFleetAPI) entity).isAlive()) {
                PTSDCrisisDevIntel.report("侦察资料遗失", report.kind + "：上传前舰队失效", report.targetSystemId, report.sourceFleetId);
                it.remove();
                continue;
            }
            publish(state, report);
            it.remove();
        }
        for (Iterator<java.util.Map.Entry<String, Float>> it = state.reconLowActivityUntil.entrySet().iterator(); it.hasNext();) {
            if (it.next().getValue() <= day) it.remove();
        }
    }

    public static void flushReturningScout(PTSDCrisisState state, String fleetId) {
        if (state == null || fleetId == null) return;
        for (Iterator<PTSDCrisisState.PendingReconReport> it = state.pendingReconReports.iterator(); it.hasNext();) {
            PTSDCrisisState.PendingReconReport report = it.next();
            if (report != null && fleetId.equals(report.sourceFleetId)) {
                publish(state, report);
                it.remove();
            }
        }
    }

    public static void queueBatch(PTSDCrisisState state, CampaignFleetAPI fleet, String fallbackSystemId) {
        if (state == null || fleet == null || !fleet.isAlive()) return;
        StarSystemAPI current = fleet.getStarSystem();
        if (current == null) current = state.resolveSystem(fallbackSystemId);
        if (current == null) return;
        float day = PTSDCrisisState.getDay();
        String group = fleet.getId() + ":" + Math.round(day * 10f);
        queueSurvey(state, fleet, current, group, fleet.getStarSystem() == null, "SCOUT_SURVEY");
        int roll = new Random(group.hashCode()).nextInt(100);
        int three = Math.round(IIRT_Omega_ModPlugin.reconReportThreeChance);
        int two = Math.round(IIRT_Omega_ModPlugin.reconReportTwoChance);
        int player = Math.round(IIRT_Omega_ModPlugin.reconReportPlayerChance);
        if (roll < player) {
            String trail = knownPlayerTrail(state, day);
            StarSystemAPI playerRegion = state.resolveSystem(trail);
            if (playerRegion != null && playerRegion != current)
                queueSurvey(state, fleet, playerRegion, group, true, "SCOUT_PLAYER_TRAIL");
            return;
        }
        int count = roll < player + three ? 3 : (roll < player + three + two ? 2 : 0);
        if (count == 0) return;
        List<StarSystemAPI> nearby = nearby(current);
        for (int i = 0; i < Math.min(count, nearby.size()); i++)
            queueSurvey(state, fleet, nearby.get(i), group, true, "SCOUT_REMOTE_SWEEP");
    }

    public static void queueDailyMaximum(PTSDCrisisState state, String systemId, String fleetId,
                                         float strength, int bucket) {
        if (state == null || systemId == null || fleetId == null) return;
        String group = fleetId + ":daily:" + bucket;
        for (PTSDCrisisState.PendingReconReport pending : state.pendingReconReports)
            if (pending != null && group.equals(pending.sourceGroup)) {
                pending.fleetStrength = Math.max(pending.fleetStrength, strength);
                pending.fleetSignal = pending.fleetStrength > .01f ? 1 : 0;
                return;
            }
        PTSDCrisisState.PendingReconReport report = create(state, fleetId, systemId, systemId,
                group, "SCOUT_DAILY_MAX", false);
        report.fleetStrength = Math.max(0f, strength);
        report.fleetSignal = strength > .01f ? 1 : 0;
        report.confidence = .72f;
        report.expiresDay = report.createdDay + 45f;
    }

    public static void queuePlayerContact(PTSDCrisisState state, CampaignFleetAPI fleet, String systemId) {
        if (state == null || fleet == null || systemId == null) return;
        PTSDCrisisState.PendingReconReport report = create(state, fleet.getId(), systemId, systemId,
                fleet.getId() + ":player", "SCOUT_PLAYER_CONTACT", false);
        report.confidence = .82f;
        report.expiresDay = report.createdDay + 25f;
    }

    /** Mission-local payload: cannot enter the timed upload queue before the carrier returns. */
    static PTSDCrisisState.PendingReconReport captureCarrierSurvey(PTSDCrisisState state, CampaignFleetAPI fleet,
                                                                  StarSystemAPI system, boolean armed) {
        String group=fleet.getId()+":carrier:"+system.getId();
        int before=state.pendingReconReports.size();
        queueSurvey(state,fleet,system,group,false,armed?"ARMED_CARRIER_SURVEY":"SCOUT_SURVEY");
        return state.pendingReconReports.remove(before);
    }
    static void deliverCarrierSurvey(PTSDCrisisState state, PTSDCrisisState.PendingReconReport report) {
        publish(state,report);
    }

    private static void queueSurvey(PTSDCrisisState state, CampaignFleetAPI fleet,
                                    StarSystemAPI system, String group, boolean remote, String kind) {
        PTSDCrisisState.PendingReconReport report = create(state, fleet.getId(),
                fleet.getStarSystem() == null ? system.getId() : fleet.getStarSystem().getId(),
                system.getId(), group, kind, remote);
        Random noise = new Random((group + ":" + system.getId()).hashCode());
        boolean populated = false;
        boolean marketObserved = false;
        float defense = 0f, value = 1f;
        for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system)) {
            if (market != null && !market.isPlanetConditionMarketOnly() &&
                    !IIRT_Omega_Invasion.WATCHER_FACTION.equals(market.getFactionId()) &&
                    !IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(market.getFactionId())) {
                populated = true;
                if (remote || "ARMED_CARRIER_SURVEY".equals(kind) || market.getPrimaryEntity() != null &&
                        Misc.getDistance(fleet.getLocation(), market.getPrimaryEntity().getLocation()) <= 12000f) {
                    marketObserved = true;
                    defense += market.getSize() * market.getSize() * 9f + market.getStabilityValue() * 3f;
                    value += market.getSize() * market.getSize() * 12f;
                }
            }
        }
        float strength = 0f;
        for (CampaignFleetAPI other : PTSDCampaignRuntime.fleets(system)) {
            if (other == null || !other.isAlive() || other.isExpired() || other.getContainingLocation() != system) continue;
            if (other == null || !other.isAlive() || other.getFaction() == null || other == fleet) continue;
            String faction = other.getFaction().getId();
            if (IIRT_Omega_Invasion.WATCHER_FACTION.equals(faction) ||
                    IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(faction)) continue;
            if (!remote && !"ARMED_CARRIER_SURVEY".equals(kind) && fleet.getContainingLocation() == system &&
                    Misc.getDistance(fleet.getLocation(), other.getLocation()) > 12000f) continue;
            strength += Math.max(0f, other.getFleetPoints());
        }
        if (remote) {
            float error = IIRT_Omega_ModPlugin.reconRemoteError;
            report.colonySignal = noise.nextFloat() < error ? (populated ? 0 : 1) : (populated ? 1 : 0);
            float noisy = strength * (1f + (noise.nextFloat() * 2f - 1f) * error);
            report.fleetStrength = noisy < 15f ? 0f : noisy < 50f ? 30f : noisy < 110f ? 75f : 140f;
            report.fleetSignal = noisy > 15f ? 1 : 0;
            report.confidence = Math.max(.15f, .55f - error * .5f);
            report.expiresDay = report.createdDay + IIRT_Omega_ModPlugin.reconRemoteLifetime;
        } else {
            report.colonySignal = populated ? 1 : 0;
            report.fleetStrength = strength;
            report.marketDefense = marketObserved ? defense : -1f;
            report.strategicValue = marketObserved ? value : -1f;
            report.fleetSignal = strength > 0f ? 1 : 0;
            report.confidence = .8f;
            report.expiresDay = report.createdDay + 120f;
        }
    }

    private static PTSDCrisisState.PendingReconReport create(PTSDCrisisState state, String fleetId,
                                                              String originId, String targetId,
                                                              String group, String kind, boolean remote) {
        PTSDCrisisState.PendingReconReport report = new PTSDCrisisState.PendingReconReport();
        report.id = "PTSD_report_" + Misc.genUID();
        report.sourceFleetId = fleetId;
        report.sourceSystemId = originId;
        report.targetSystemId = targetId;
        report.sourceGroup = group;
        report.kind = kind;
        report.remote = remote;
        report.createdDay = PTSDCrisisState.getDay();
        Random random = new Random(report.id.hashCode());
        report.uploadDay = report.createdDay + IIRT_Omega_ModPlugin.reconUploadMin +
                random.nextFloat() * Math.max(0f, IIRT_Omega_ModPlugin.reconUploadMax - IIRT_Omega_ModPlugin.reconUploadMin);
        state.pendingReconReports.add(report);
        if(!group.contains(":carrier:")) PTSDCrisisDevIntel.report("侦察报告待上传", kind + "；源 " + originId + "；约 " +
                Math.round(report.uploadDay - report.createdDay) + " 日后传出", targetId, fleetId);
        return report;
    }

    private static void publish(PTSDCrisisState state, PTSDCrisisState.PendingReconReport report) {
        if (report.uploaded || report.targetSystemId == null) return;
        report.uploaded = true;
        if ("SCOUT_PLAYER_CONTACT".equals(report.kind)) {
            if (report.createdDay >= state.reconPlayerTrailObservedDay) {
                state.reconPlayerTrailSystemId = report.targetSystemId;
                state.reconPlayerTrailExpiresDay = report.expiresDay;
                state.reconPlayerTrailObservedDay = report.createdDay;
            }
            state.reconLowActivityUntil.remove(report.targetSystemId);
        } else {
            PTSDCrisisState.SystemData data = state.getSystemData(report.targetSystemId);
            if (!report.remote && ("SCOUT_SURVEY".equals(report.kind) || "ARMED_CARRIER_SURVEY".equals(report.kind)))
                for (java.util.Iterator<PTSDCrisisState.EvidenceRecord> it=data.evidence.iterator();it.hasNext();)
                    if ("WAR_OPENING_ESTIMATE".equals(it.next().sourceType)) it.remove();
            String stableGroup = report.sourceFleetId + ":" + report.targetSystemId + ":" + report.kind;
            for (Iterator<PTSDCrisisState.EvidenceRecord> it = data.evidence.iterator(); it.hasNext();) {
                PTSDCrisisState.EvidenceRecord old = it.next();
                if (old != null && stableGroup.equals(old.sourceGroup)) it.remove();
            }
            PTSDCrisisState.EvidenceRecord evidence = PTSDCrisisAPI.recordEvidence(report.targetSystemId,
                    report.kind, report.sourceFleetId, report.fleetStrength,
                    report.marketDefense, report.strategicValue,
                    report.colonySignal, report.fleetSignal, report.confidence,
                    Math.max(1f, report.expiresDay - report.createdDay), false, 0f);
            if (evidence != null) {
                evidence.sourceGroup = stableGroup;
                evidence.remoteEstimate = report.remote;
                evidence.uploadState = "CONFIRMED_UPLOADED";
                PTSDCrisisAPI.refreshBelief(data, PTSDCrisisState.getDay());
                if (!report.remote && ("SCOUT_SURVEY".equals(report.kind) || "ARMED_CARRIER_SURVEY".equals(report.kind))) {
                    PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.RECON_CONFIDENCE,
                            .6f, "SCOUT_UPLOAD", report.targetSystemId);
                    PTSDLateWarDirector.creditStrategicAssets(state,
                            "scout-survey:" + report.sourceFleetId + ":" + report.targetSystemId,
                            5f, "实地侦察报告已上传", report.targetSystemId);
                }
            }
            if (report.remote && report.colonySignal == 0 &&
                    report.fleetStrength <= IIRT_Omega_ModPlugin.reconLowFleetThreshold)
                state.reconLowActivityUntil.put(report.targetSystemId, report.expiresDay);
        }
        PTSDCrisisDevIntel.report("侦察报告上传", report.kind + (report.remote ? "；远距估测" : "；实地观测") +
                "；源 " + report.sourceFleetId + "；置信 " + Math.round(report.confidence * 100f) + "%",
                report.targetSystemId, report.sourceFleetId);
    }

    public static String knownPlayerTrail(PTSDCrisisState state, float day) {
        String latest = state.reconPlayerTrailExpiresDay > day ? state.reconPlayerTrailSystemId : null;
        float newest = latest == null ? -1f : state.reconPlayerTrailObservedDay;
        if (state.late != null) for (PTSDLateState.ThreatEvidence evidence : state.late.threatEvidence)
            if (evidence != null && evidence.playerInvolved && "CONFIRMED_UPLOADED".equals(evidence.uploadState) &&
                    evidence.expiresDay > day && evidence.sourceSystemId != null && evidence.day > newest) {
                newest = evidence.day;
                latest = evidence.sourceSystemId;
            }
        return latest;
    }

    public static StarSystemAPI nextUnscouted(PTSDCrisisState state, StarSystemAPI origin,
                                              String sourceFleetId) {
        if (state == null || origin == null) return null;
        float day = PTSDCrisisState.getDay();
        for (StarSystemAPI candidate : nearby(origin)) {
            if (PTSDStrategicSurvey.postRecon(state) && !PTSDStrategicSurvey.populated(candidate)) continue;
            PTSDCrisisState.SystemData data = state.getSystemData(candidate.getId());
            if (data.omegaControl >= .5f || data.conversionLevel > 0) continue;
            boolean hasTarget = !candidate.getJumpPoints().isEmpty();
            if (!hasTarget) for (com.fs.starfarer.api.campaign.PlanetAPI planet : candidate.getPlanets())
                if (planet != null && !planet.isStar()) { hasTarget = true; break; }
            if (!hasTarget) continue;
            Float low = state.reconLowActivityUntil.get(candidate.getId());
            if (low != null && low > day) continue;
            if (!isSurveyed(data, day) && !surveyPendingAboard(state, sourceFleetId, candidate.getId()))
                return candidate;
        }
        return null;
    }

    private static boolean surveyPendingAboard(PTSDCrisisState state, String fleetId, String systemId) {
        if (fleetId == null) return false;
        for (PTSDCrisisState.PendingReconReport report : state.pendingReconReports)
            if (report != null && fleetId.equals(report.sourceFleetId) &&
                    systemId.equals(report.targetSystemId) && ("SCOUT_SURVEY".equals(report.kind) || "ARMED_CARRIER_SURVEY".equals(report.kind)) &&
                    !report.remote) return true;
        return false;
    }

    public static boolean isSurveyed(PTSDCrisisState.SystemData data, float day) {
        if (data == null || data.evidence == null) return false;
        for (PTSDCrisisState.EvidenceRecord evidence : data.evidence)
            if (evidence != null && (("SCOUT_SURVEY".equals(evidence.sourceType) || "ARMED_CARRIER_SURVEY".equals(evidence.sourceType)) || "UNINHABITED_BASELINE".equals(evidence.sourceType)) &&
                    !evidence.remoteEstimate && evidence.expiresDay > day &&
                    (evidence.uploadState == null || "CONFIRMED_UPLOADED".equals(evidence.uploadState)))
                return true;
        return false;
    }

    public static boolean hasBlindSpot(PTSDCrisisState state) {
        if (state == null || Global.getSector() == null) return false;
        float day = PTSDCrisisState.getDay();
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system == null || system.hasTag(Tags.THEME_HIDDEN) ||
                    system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)) continue;
            boolean reachable = !system.getJumpPoints().isEmpty();
            if (!reachable) for (com.fs.starfarer.api.campaign.PlanetAPI planet : system.getPlanets())
                if (planet != null && !planet.isStar()) { reachable = true; break; }
            if (!reachable) continue;
            PTSDCrisisState.SystemData data = state.getSystemData(system.getId());
            Float low = state.reconLowActivityUntil.get(system.getId());
            if ((low == null || low <= day) && data.omegaControl < .5f &&
                    data.conversionLevel == 0 && !isSurveyed(data, day)) return true;
        }
        return false;
    }

    public static String coverageSummary(PTSDCrisisState state) {
        if (state == null || Global.getSector() == null) return "尚无侦察状态";
        int total = 0, local = 0, remote = 0, low = 0;
        float day = PTSDCrisisState.getDay();
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system == null || system.hasTag(Tags.THEME_HIDDEN) ||
                    system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)) continue;
            total++;
            PTSDCrisisState.SystemData data = state.getSystemData(system.getId());
            boolean surveyed = false, estimated = false;
            for (PTSDCrisisState.EvidenceRecord evidence : data.evidence) {
                if (evidence == null || evidence.expiresDay <= day ||
                        evidence.uploadState != null &&
                                !"CONFIRMED_UPLOADED".equals(evidence.uploadState)) continue;
                if (("SCOUT_SURVEY".equals(evidence.sourceType) || "ARMED_CARRIER_SURVEY".equals(evidence.sourceType)) && !evidence.remoteEstimate) surveyed = true;
                if (evidence.remoteEstimate) estimated = true;
            }
            if (surveyed) local++;
            else if (estimated) remote++;
            Float until = state.reconLowActivityUntil.get(system.getId());
            if (until != null && until > day) low++;
        }
        return "实地 " + local + "/" + total + "；仅远距 " + remote +
                "；暂缓低活动 " + low + "；待上传 " + state.pendingReconReports.size();
    }

    public static List<StarSystemAPI> nearby(final StarSystemAPI origin) {
        List<StarSystemAPI> systems = new ArrayList<StarSystemAPI>();
        if (origin == null || Global.getSector() == null) return systems;
        for (StarSystemAPI candidate : Global.getSector().getStarSystems()) {
            if (candidate == null || candidate == origin || candidate.hasTag(Tags.THEME_HIDDEN) ||
                    candidate.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)) continue;
            systems.add(candidate);
        }
        Collections.sort(systems, new Comparator<StarSystemAPI>() {
            @Override public int compare(StarSystemAPI a, StarSystemAPI b) {
                int value = Float.compare(Misc.getDistance(origin.getLocation(), a.getLocation()),
                        Misc.getDistance(origin.getLocation(), b.getLocation()));
                return value != 0 ? value : a.getId().compareTo(b.getId());
            }
        });
        return systems;
    }
}
