package data.scripts.campaign.invasion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Save-persistent late crisis ledger. Physical fleets are projections of these records. */
public final class PTSDLateState implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final class ThreatEvidence implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id, domain, sourceFleetId, sourceSystemId, uploadState;
        public float strength, confidence, day, expiresDay;
        public boolean playerInvolved;
    }

    public static final class Fortress implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id, originSystemId, currentSystemId, blackHoleId, fleetId;
        public String destinationSystemId, lastLocationId;
        public int tier = 1;
        public float tierProgress, nextTransportDay, blockedUntilDay, nextMoveRollDay;
        public float transitProgress, transitDistance, transitSpeed = 8f;
        public float lastTravelDay;
        public float lastHyperX, lastHyperY;
        public int deliveredTransports, lostTransports;
        public boolean collapsed, destroyed, movedPrewar, inTransit, detonationHandled;
        public boolean warnedPlayer, autoSavePending, autoSaveAttempted;
        public List<String> patrolIds = new ArrayList<String>();
        public List<String> transportIds = new ArrayList<String>();
        public List<String> escortIds = new ArrayList<String>();
    }

    public static final class Front implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id, systemId, marketId, sourceSystemId;
        public float attackBudget, barrage, defense, warningDay, nextStrikeDay;
        public float playerRescueWarningDay, playerRescueDeadline, lockedTravelDays;
        public int strikes;
        public boolean burst, lost, resolved, warned;
        public List<String> evidenceIds = new ArrayList<String>();
    }

    public static final class Contribution implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id, type, actorId, factionAtTime, systemId;
        public float amount, day;
    }

    public static final class Reserve implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id, factionId, sourceSystemId, targetSystemId, fleetId;
        public float strength, distance, travelled, speed = 8f, lastDay;
        public boolean destroyed, arrived;
        public boolean authorized;
        public List<String> variantIds = new ArrayList<String>();
    }

    public static final class Alert implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id, text, systemId, source;
        public float day;
        public boolean delivered;
    }

    public static final class Resolution implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id, frontId, sponsorFactionId, method, outcome, exportMarketId;
        public float day, voteChance, reservedStrength, executedDay;
        public boolean resolved;
    }

    public static final class EventCard implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id, title, systemId, report, intercept, firstChoice, secondChoice, outcome;
        public float createdDay, expiresDay;
        public boolean shortCard, resolved;
        public boolean accepted;
        public int selectedChoice;
        public String targetEntityId, targetMarketId, targetFrontId, orderKind;
        public float orderDeadlineDay;
        public boolean escortContact;
    }

    /** FT v2 owns the entire chain; generic late orders must never settle these nodes. */
    public static final class FTChain implements Serializable {
        private static final long serialVersionUID = 1L;
        public int version = 2;
        public String id, node, systemId, routeSystemId, marketId, targetId, secondaryId, fleetId, outcome;
        public String factionA, factionB;
        public float startedDay, expiresDay, nextDay, progress, reservedAssets;
        public float originX, originY;
        public int steps, encounters, offers;
        public boolean resolved, paidDeposit, paidTail, fragmentAwarded, refused, triggered;
        public boolean permitObtained, identityExposed, escortSigned;
    }

    public static final class E2Chain implements Serializable {
        private static final long serialVersionUID = 1L;
        public int version = 1;
        public String incidentId, cardId, systemId, actorFleetId, supportFleetId;
        public String destinationMarketId, node, outcome;
        public float deadlineDay;
        public boolean accepted, resolved, paid, actorDefeated, actorPlayerVictory, supportDefeated;
    }

    public static final class ControlledPatrol implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id, systemId, fleetId;
        public int index, waypointIndex;
        public float nominalFp, survivingFp, replenishDay, nextWaypointDay;
    }

    public static final class Detonation implements Serializable {
        private static final long serialVersionUID = 1L;
        public String fortressId, systemId;
        public float remainingSeconds = 180f;
        public boolean combatStarted, battleEscaped, exploded, autoSaveAttempted;
    }

    public float warPoints, warPointBelief, strategicReserve = 240f;
    /** v1 reconciles production from real facilities once, including saves from before expansion production. */
    public int warEconomyVersion;
    public String lastProductionBreakdown;
    public float lastProductionIncome;
    public float spentWarPointsSinceProduction, spentStrategicReserveSinceProduction;
    public String lastSpendingCause;
    public int productionSettlementCount;
    public String lastAssetCreditCause;
    public float lastAssetCreditAmount, lastAssetCreditDay;
    /** Finite civilian/military coordination capacity spent by event-card orders. */
    public float humanResponseBudget = 90f;
    public float nextProductionDay, nextFrontDecisionDay, nextTrapDay;
    public float nextFortressBuildDay;
    public float lastAdvanceDay, recentPlayerVictoryFP, previousPlayerVictoryFP;
    public float nextPlayerVictoryRollDay, nextWarningDay, warStartedDay;
    public int selectedBurstCount, nextAlertIndex;
    public int mobilizationOrdersUsed;
    public boolean initialized, firstWaveSelected, staffEstablished, staffChecked;
    public boolean legacyWarSave;
    public Map<String, Fortress> fortresses = new LinkedHashMap<String, Fortress>();
    public Map<String, Front> fronts = new LinkedHashMap<String, Front>();
    public List<ThreatEvidence> threatEvidence = new ArrayList<ThreatEvidence>();
    public List<Contribution> contributions = new ArrayList<Contribution>();
    public List<Reserve> reserves = new ArrayList<Reserve>();
    public List<Resolution> resolutions = new ArrayList<Resolution>();
    public List<EventCard> eventCards = new ArrayList<EventCard>();
    public Map<String, FTChain> ftChains = new LinkedHashMap<String, FTChain>();
    public int ftChainVersion;
    public int verifiedPlayerOmegaDefeats;
    public Set<String> ftCountedDefeats = new LinkedHashSet<String>();
    public String lastFTTransportLossId, lastFTTransportLossSystemId;
    public float lastFTTransportLossX, lastFTTransportLossY;
    public float nextFT10RollDay;
    public float nextCardDay;
    public Set<String> coalitionMembers = new LinkedHashSet<String>();
    public boolean coalitionFoundersRegistered;
    public Map<String, Float> coalitionJoinRequests = new LinkedHashMap<String, Float>();
    public Map<String, String> coalitionDiplomaticReplies = new LinkedHashMap<String, String>();
    public Map<String, Float> coalitionPreviousRelations = new LinkedHashMap<String, Float>();
    public Map<String, Float> staffAvailableByFaction = new LinkedHashMap<String, Float>();
    public Map<String, Float> staffExportContracts = new LinkedHashMap<String, Float>();
    public List<Alert> alerts = new ArrayList<Alert>();
    public Set<String> committedIds = new LinkedHashSet<String>();
    public Set<String> completedCards = new LinkedHashSet<String>();
    public Set<String> transformedSystems = new LinkedHashSet<String>();
    public Set<String> generatedCollapseSystems = new LinkedHashSet<String>();
    public Map<String, Float> playerLastVictoryDayBySystem = new LinkedHashMap<String, Float>();
    public Map<String, Float> playerVictoryFPBySystem = new LinkedHashMap<String, Float>();
    public Map<String, Integer> witnessedPasses = new LinkedHashMap<String, Integer>();
    public Map<String, Integer> witnessedSupplyVisits = new LinkedHashMap<String, Integer>();
    public Map<String, Float> lastSupplyVisitDay = new LinkedHashMap<String, Float>();
    public String lastPlayerSystemId;
    public Detonation detonation;
    public Map<String, Detonation> detonations = new LinkedHashMap<String, Detonation>();
    public Map<String, E2Chain> e2Chains = new LinkedHashMap<String, E2Chain>();
    public Map<String, ControlledPatrol> controlledPatrols =
            new LinkedHashMap<String, ControlledPatrol>();
    public Map<String, Float> controlledPatrolAwayDay = new LinkedHashMap<String, Float>();

    public void repair() {
        if (fortresses == null) fortresses = new LinkedHashMap<String, Fortress>();
        if (fronts == null) fronts = new LinkedHashMap<String, Front>();
        if (threatEvidence == null) threatEvidence = new ArrayList<ThreatEvidence>();
        if (contributions == null) contributions = new ArrayList<Contribution>();
        if (reserves == null) reserves = new ArrayList<Reserve>();
        if (resolutions == null) resolutions = new ArrayList<Resolution>();
        if (eventCards == null) eventCards = new ArrayList<EventCard>();
        if (ftChains == null) ftChains = new LinkedHashMap<String, FTChain>();
        if (ftCountedDefeats == null) ftCountedDefeats = new LinkedHashSet<String>();
        if (staffAvailableByFaction == null) staffAvailableByFaction = new LinkedHashMap<String, Float>();
        if (coalitionMembers == null) coalitionMembers = new LinkedHashSet<String>();
        if (coalitionJoinRequests == null) coalitionJoinRequests = new LinkedHashMap<String, Float>();
        if (coalitionDiplomaticReplies == null) coalitionDiplomaticReplies = new LinkedHashMap<String, String>();
        if (coalitionPreviousRelations == null) coalitionPreviousRelations = new LinkedHashMap<String, Float>();
        if (staffExportContracts == null) staffExportContracts = new LinkedHashMap<String, Float>();
        if (detonations == null) detonations = new LinkedHashMap<String, Detonation>();
        if (e2Chains == null) e2Chains = new LinkedHashMap<String, E2Chain>();
        if (controlledPatrols == null)
            controlledPatrols = new LinkedHashMap<String, ControlledPatrol>();
        if (controlledPatrolAwayDay == null)
            controlledPatrolAwayDay = new LinkedHashMap<String, Float>();
        if (detonation != null && detonation.fortressId != null &&
                !detonations.containsKey(detonation.fortressId)) detonations.put(detonation.fortressId, detonation);
        if (alerts == null) alerts = new ArrayList<Alert>();
        if (committedIds == null) committedIds = new LinkedHashSet<String>();
        if (completedCards == null) completedCards = new LinkedHashSet<String>();
        if (transformedSystems == null) transformedSystems = new LinkedHashSet<String>();
        if (generatedCollapseSystems == null) generatedCollapseSystems = new LinkedHashSet<String>();
        if (playerLastVictoryDayBySystem == null) playerLastVictoryDayBySystem = new LinkedHashMap<String, Float>();
        if (playerVictoryFPBySystem == null) playerVictoryFPBySystem = new LinkedHashMap<String, Float>();
        if (witnessedPasses == null) witnessedPasses = new LinkedHashMap<String, Integer>();
        if (witnessedSupplyVisits == null) witnessedSupplyVisits = new LinkedHashMap<String, Integer>();
        if (lastSupplyVisitDay == null) lastSupplyVisitDay = new LinkedHashMap<String, Float>();
        for (Fortress fortress : fortresses.values()) {
            if (fortress.patrolIds == null) fortress.patrolIds = new ArrayList<String>();
            if (fortress.transportIds == null) fortress.transportIds = new ArrayList<String>();
            if (fortress.escortIds == null) fortress.escortIds = new ArrayList<String>();
        }
        for (Front front : fronts.values()) if (front.evidenceIds == null) front.evidenceIds = new ArrayList<String>();
        for (Reserve reserve : reserves) if (reserve.variantIds == null) reserve.variantIds = new ArrayList<String>();
    }
}
