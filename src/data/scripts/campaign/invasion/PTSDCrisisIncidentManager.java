package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldSource;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lwjgl.util.vector.Vector2f;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static data.scripts.IIRT_Omega_ModPlugin.*;

/** Random, save-persistent narrative cards for the dark-current and fire-probe eras. */
public final class PTSDCrisisIncidentManager {
    private enum TargetKind { ANY, EDGE_MARKET, PIRATE, RELAY, POPULATED, MILITARY, WILDERNESS, CUSTOM, FACTION }

    private static final class Card {
        final String id;
        final String category;
        final String phases;
        final boolean investigable;
        final boolean filler;
        final TargetKind target;
        final String targetExpression;
        final String targetArgument;
        final float weight;
        final float cooldown;
        final float recon;
        final float awareness;
        final float aggression;
        final float panic;
        final float distortion;
        final float physicalChance;
        final float strength;
        final String source;
        final String[] headlines;
        final String[] reports;
        final String[] truths;
        final String siteTemplates;
        final String siteHandler;
        final String martialSite;

        Card(String id, String category, String phases, boolean investigable, boolean filler,
             TargetKind target, String targetExpression, String targetArgument, float weight, float cooldown,
             float recon, float awareness, float aggression, float panic, float distortion,
             float physicalChance, float strength, String source,
             String[] headlines, String[] reports, String[] truths,
             String siteTemplates, String siteHandler, String martialSite) {
            this.id = id;
            this.category = category;
            this.phases = phases;
            this.investigable = investigable;
            this.filler = filler;
            this.target = target;
            this.targetExpression = targetExpression;
            this.targetArgument = targetArgument;
            this.weight = weight;
            this.cooldown = cooldown;
            this.recon = recon;
            this.awareness = awareness;
            this.aggression = aggression;
            this.panic = panic;
            this.distortion = distortion;
            this.physicalChance = physicalChance;
            this.strength = strength;
            this.source = source;
            this.headlines = headlines;
            this.reports = reports;
            this.truths = truths;
            this.siteTemplates = siteTemplates;
            this.siteHandler = siteHandler;
            this.martialSite = martialSite;
        }
    }

    private static final class Target {
        final StarSystemAPI system;
        final MarketAPI market;
        final SectorEntityToken targetLocation;
        final PTSDCrisisNewsAPI.CustomNewsHandler handler;

        Target(StarSystemAPI system, MarketAPI market) {
            this(system, market, market == null ? null : market.getPrimaryEntity(), null);
        }

        Target(StarSystemAPI system, MarketAPI market, SectorEntityToken targetLocation,
               PTSDCrisisNewsAPI.CustomNewsHandler handler) {
            this.system = system;
            this.market = market;
            this.targetLocation = targetLocation;
            this.handler = handler;
        }
    }

    private static final List<Card> DARK = new ArrayList<Card>();
    private static final List<Card> PROBE = new ArrayList<Card>();
    private static final List<Card> NEWS = new ArrayList<Card>();
    private static final Color WHISPER_COLOR = new Color(182, 164, 198);
    private static boolean loaded;

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        DARK.clear();
        PROBE.clear();
        NEWS.clear();

        int loadedCards = 0;
        for (com.fs.starfarer.api.ModSpecAPI mod :
                Global.getSettings().getModManager().getEnabledModsCopy()) {
            JSONArray rows;
            try {
                rows = Global.getSettings().getMergedSpreadsheetDataForMod(
                        "id", "data/config/ptsdCSV/PTSD_crisis_news.csv", mod.getId());
            } catch (Throwable missingOrInvalidForMod) {
                continue;
            }
            for (int i = 0; i < rows.length(); i++) {
                try {
                    JSONObject row = rows.getJSONObject(i);
                    String id = row.optString("id", "").trim();
                    if (id.length() == 0 || id.startsWith("#")) continue;
                    String category = row.optString("category", "普通新闻").trim();
                    String targetExpression = row.optString("target", "ANY").trim();
                    ParsedTarget parsedTarget = parseTarget(targetExpression);
                    TargetKind target = parsedTarget.kind;
                    String targetArgument = parsedTarget.argument;
                    if (target == TargetKind.CUSTOM && targetArgument != null) {
                        PTSDCrisisNewsAPI.resolveHandler(targetArgument);
                    }
                    Card card = new Card(id, category, row.optString("phases", "ALL"),
                            row.optBoolean("investigable", false), row.optBoolean("filler", false), target,
                            targetExpression, targetArgument,
                            (float) row.optDouble("weight", 1d), (float) row.optDouble("cooldown", 12d),
                            (float) row.optDouble("recon", 0d), (float) row.optDouble("awareness", 0d),
                            (float) row.optDouble("aggression", 0d), (float) row.optDouble("panic", 0d),
                            (float) row.optDouble("distortion", 0d),
                            (float) row.optDouble("physicalChance", 0d),
                            (float) row.optDouble("strength", 0d),
                            row.optString("source", "匿名航路简报"),
                            variants(row.optString("headline", id)),
                            variants(row.optString("report", "")),
                            variants(row.optString("truth", "")),
                            row.optString("siteTemplates", "AUTO").trim(),
                            row.optString("siteHandler", "").trim(),
                            row.optString("martialSite", "AUTO").trim());
                    addOrReplace(card);
                    loadedCards++;
                } catch (Throwable rowError) {
                    Global.getLogger(PTSDCrisisIncidentManager.class).warn(
                            "Ignoring invalid crisis news row from " + mod.getId() + " at index " + i,
                            rowError);
                }
            }
        }

        if (loadedCards == 0) {
            loaded = false;
            Global.getLogger(PTSDCrisisIncidentManager.class).error(
                    "Unable to load any PTSD crisis news CSV rows");
        }
    }

    private static void addOrReplace(Card card) {
        removeCard(DARK, card.id);
        removeCard(PROBE, card.id);
        removeCard(NEWS, card.id);
        if ("火力侦察".equals(card.category)) PROBE.add(card);
        else if ("普通新闻".equals(card.category)) NEWS.add(card);
        else DARK.add(card);
    }

    private static void removeCard(List<Card> cards, String id) {
        for (int i = cards.size() - 1; i >= 0; i--) {
            if (cards.get(i).id.equalsIgnoreCase(id)) cards.remove(i);
        }
    }
    private static boolean phaseMatches(Card card, PTSDCrisisState.Phase phase) {
        if (card == null || phase == null) return false;
        String[] tokens = card.phases.split("\\|");
        for (String token : tokens) if ("ALL".equalsIgnoreCase(token.trim()) || phase.name().equalsIgnoreCase(token.trim())) return true;
        return false;
    }

    private static List<Card> selectPool(PTSDCrisisState state, boolean fireProbe, Random random) {
        ensureLoaded();
        List<Card> filler = new ArrayList<Card>();
        List<Card> substantive = new ArrayList<Card>();
        List<Card> all = new ArrayList<Card>(); all.addAll(DARK); all.addAll(PROBE); all.addAll(NEWS);
        for (Card card : all) {
            if (!phaseMatches(card, state.phase)) continue;
            // Contact-only cards are created through forceAndGet(), never the ambient news pool.
            if ("专项调查".equals(card.category)) continue;
            if (card.filler) filler.add(card);
            else if (state.phase == PTSDCrisisState.Phase.DORMANT && "暗流".equals(card.category)) substantive.add(card);
            else if (state.phase == PTSDCrisisState.Phase.RECON &&
                    ((fireProbe && "火力侦察".equals(card.category)) || (!fireProbe && "暗流".equals(card.category)))) substantive.add(card);
            else if (state.phase != PTSDCrisisState.Phase.DORMANT && state.phase != PTSDCrisisState.Phase.RECON) substantive.add(card);
        }
        float fillerChance;
        switch (state.phase) {
            case DORMANT: fillerChance = .32f; break;
            case RECON: fillerChance = .25f; break;
            case EXPANSION: fillerChance = .15f; break;
            case FORTIFICATION: fillerChance = .07f; break;
            case WAR: fillerChance = .02f; break;
            default: fillerChance = .1f;
        }
        return !filler.isEmpty() && random.nextFloat() < fillerChance ? filler : substantive;
    }
    /** Adds small, deterministic wording differences without changing the card's factual core. */
    private static String[] variants(String base) {
        return new String[] { base, base, base };
    }

    public static void advance(PTSDCrisisState state, float day, Random random) {
        if (state == null || random == null) return;
        ensureLoaded();
        PTSDNewsSiteManager.advance(state, day, random);
        advanceInvestigations(state, day, random);
        float detectorFrequency = PTSDCrisisDetectorAbility.getEventFrequencyMultiplier();
        float configuredMax = Math.max(unknown_event_min_interval, unknown_event_max_interval) /
                Math.max(.1f, unknown_event_frequency * detectorFrequency);
        if (state.nextIncidentDay > day + configuredMax) state.nextIncidentDay = day + configuredMax;
        if (day < state.nextIncidentDay) return;

        float frequency = Math.max(.1f, unknown_event_frequency * detectorFrequency);
        state.nextIncidentDay = day + between(random, unknown_event_min_interval,
                Math.max(unknown_event_min_interval, unknown_event_max_interval)) / frequency;

        boolean fireProbe = state.phase == PTSDCrisisState.Phase.RECON &&
                (state.watcherAggression >= 22f || state.reconConfidence >= 36f ||
                        day - state.phaseStartedDay >= Math.max(8f, collect_data_time * .42f));
        List<Card> pool = selectPool(state, fireProbe, random);
        if (pool == null) return;

        for (int attempt = 0; attempt < 12; attempt++) {
            Card card = pick(pool, state, day, random);
            if (card == null) return;
            Target target = pickTarget(card, state, random);
            if (target == null || target.system == null) continue;
            createIncident(state, card, target, day, random, false);
            return;
        }
        state.nextIncidentDay = Math.min(state.nextIncidentDay, day + 2f);
        PTSDCrisisDevIntel.report("未知事件抽取失败", "候选目标均失效；两日后重试", null, null);
    }

    public static boolean forceRandomCategory(String category) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return false;
        ensureLoaded();
        List<Card> pool = new ArrayList<Card>();
        for (Card card : DARK) if (category.equals(card.category)) pool.add(card);
        for (Card card : PROBE) if (category.equals(card.category)) pool.add(card);
        for (Card card : NEWS) if (category.equals(card.category)) pool.add(card);
        Random random = new Random(Misc.genUID().hashCode());
        WeightedRandomPicker<Card> picker = new WeightedRandomPicker<Card>(random);
        for (Card card : pool) picker.add(card, card.weight);
        for (int attempt = 0; attempt < 12; attempt++) {
            Card card = picker.pick();
            if (card == null) return false;
            Target target = pickTarget(card, state, random);
            if (target == null) continue;
            createIncident(state, card, target, PTSDCrisisState.getDay(), random, true);
            return true;
        }
        return false;
    }
    public static boolean force(String cardId) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || cardId == null) return false;
        ensureLoaded();
        Card card = find(cardId);
        if (card == null) return false;
        Random random = new Random(Misc.genUID().hashCode());
        Target target = pickTarget(card, state, random);
        if (target == null) return false;
        createIncident(state, card, target, PTSDCrisisState.getDay(), random, true);
        return true;
    }

    public static PTSDCrisisState.CrisisIncident forceAndGet(String cardId) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null || cardId == null) return null;
        ensureLoaded();
        Card card = find(cardId);
        if (card == null) return null;
        Random random = new Random(Misc.genUID().hashCode());
        Target target = pickTarget(card, state, random);
        if (target == null) return null;
        return createIncident(state, card, target, PTSDCrisisState.getDay(), random, true);
    }
    /** Read-only description used by the Dev event console. */
    public static final class DevCard {
        public final String id;
        public final String category;
        public final String headline;
        public final String phases;
        private DevCard(Card card) {
            id = card.id;
            category = card.category;
            headline = card.headlines == null || card.headlines.length == 0 ? card.id : card.headlines[0];
            phases = card.phases;
        }
    }

    /** Returns only cards whose normal phase condition is currently satisfied. */
    public static List<DevCard> getDevCardsForCurrentPhase() {
        ensureLoaded();
        List<DevCard> result = new ArrayList<DevCard>();
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return result;
        for (Card card : DARK) if (phaseMatches(card, state.phase)) result.add(new DevCard(card));
        for (Card card : PROBE) if (phaseMatches(card, state.phase)) result.add(new DevCard(card));
        for (Card card : NEWS) if (phaseMatches(card, state.phase)) result.add(new DevCard(card));
        return result;
    }
    private static Card find(String id) {
        for (Card card : DARK) if (card.id.equalsIgnoreCase(id)) return card;
        for (Card card : PROBE) if (card.id.equalsIgnoreCase(id)) return card;
        for (Card card : NEWS) if (card.id.equalsIgnoreCase(id)) return card;
        return null;
    }

    private static Card pick(List<Card> pool, PTSDCrisisState state, float day, Random random) {
        WeightedRandomPicker<Card> picker = new WeightedRandomPicker<Card>(random);
        for (Card card : pool) {
            Float until = state.incidentCooldowns.get(card.id);
            if (until != null && day < until) continue;
            float weight = card.weight;
            if ("P-11".equals(card.id)) weight *= 1f + state.totalScoutEscapes * .18f;
            if ("P-12".equals(card.id)) weight *= Math.max(.15f, state.watcherAggression / 55f);
            picker.add(card, weight);
        }
        return picker.pick();
    }

    private static final class ParsedTarget {
        final TargetKind kind;
        final String argument;

        ParsedTarget(TargetKind kind, String argument) {
            this.kind = kind;
            this.argument = argument;
        }
    }

    private static ParsedTarget parseTarget(String expression) {
        String value = expression == null ? "ANY" : expression.trim();
        int open = value.indexOf('(');
        if (open > 0 && value.endsWith(")")) {
            String type = value.substring(0, open).trim();
            String argument = value.substring(open + 1, value.length() - 1).trim();
            if (argument.length() >= 2 &&
                    ((argument.startsWith("\"") && argument.endsWith("\"")) ||
                     (argument.startsWith("'") && argument.endsWith("'")))) {
                argument = argument.substring(1, argument.length() - 1).trim();
            }
            if ("CUSTOM".equalsIgnoreCase(type)) return new ParsedTarget(TargetKind.CUSTOM, argument);
            if ("FACTION".equalsIgnoreCase(type)) return new ParsedTarget(TargetKind.FACTION, argument);
        }
        try {
            return new ParsedTarget(TargetKind.valueOf(value.toUpperCase()), null);
        } catch (Throwable ignored) {
            Global.getLogger(PTSDCrisisIncidentManager.class).warn(
                    "Unknown crisis news target expression, using ANY: " + value);
            return new ParsedTarget(TargetKind.ANY, null);
        }
    }

    private static Target pickTarget(Card card, PTSDCrisisState state, Random random) {
        if (card == null) return null;
        if (card.target == TargetKind.CUSTOM) {
            PTSDCrisisNewsAPI.CustomNewsHandler handler =
                    PTSDCrisisNewsAPI.resolveHandler(card.targetArgument);
            if (handler == null) return null;
            try {
                PTSDCrisisNewsAPI.TargetSelection selection = handler.pick(
                        new PTSDCrisisNewsAPI.PickContext(state, PTSDCrisisState.getDay(), random,
                                card.id, card.targetExpression));
                if (selection == null || selection.system == null) return null;
                SectorEntityToken location = selection.targetLocation;
                if (location == null && selection.market != null) {
                    location = selection.market.getPrimaryEntity();
                }
                return new Target(selection.system, selection.market, location, handler);
            } catch (Throwable ex) {
                Global.getLogger(PTSDCrisisIncidentManager.class).warn(
                        "Custom crisis news pick failed: " + card.targetExpression, ex);
                return null;
            }
        }

        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player != null && player.getStarSystem() != null && random.nextFloat() < .18f) {
            MarketAPI playerMarket = bestMarket(player.getStarSystem());
            if (matches(card, player.getStarSystem(), playerMarket)) {
                return new Target(player.getStarSystem(), playerMarket);
            }
        }

        WeightedRandomPicker<Target> picker = new WeightedRandomPicker<Target>(random);
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system == null || system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER) ||
                    system.hasTag(Tags.THEME_HIDDEN)) continue;
            List<MarketAPI> markets = Global.getSector().getEconomy().getMarkets(system);
            boolean populated = false;
            for (MarketAPI market : markets) {
                if (market != null && !market.isPlanetConditionMarketOnly() && !isCrisisMarket(market)) {
                    populated = true;
                    break;
                }
            }
            if (!populated && (card.target == TargetKind.WILDERNESS || card.target == TargetKind.ANY)) {
                picker.add(new Target(system, null), 2f);
            }
            for (MarketAPI market : markets) {
                if (market == null || market.isPlanetConditionMarketOnly() || isCrisisMarket(market)) continue;
                if (!matches(card, system, market)) continue;
                float edge = Math.max(1f, system.getLocation().length() / 12000f);
                float weight = card.target == TargetKind.EDGE_MARKET ?
                        edge * (2f + market.getSize()) : 2f + market.getSize();
                picker.add(new Target(system, market), weight);
            }
            if (card.target == TargetKind.RELAY && !system.getEntitiesWithTag(Tags.COMM_RELAY).isEmpty()) {
                picker.add(new Target(system, bestMarket(system)), 7f);
            }
        }
        return picker.pick();
    }

    private static boolean matches(Card card, StarSystemAPI system, MarketAPI market) {
        TargetKind kind = card.target;
        if (kind == TargetKind.ANY) return true;
        if (kind == TargetKind.WILDERNESS) return market == null;
        if (kind == TargetKind.RELAY) {
            return system != null && !system.getEntitiesWithTag(Tags.COMM_RELAY).isEmpty();
        }
        if (market == null) return false;
        if (kind == TargetKind.FACTION) {
            return card.targetArgument != null &&
                    card.targetArgument.equalsIgnoreCase(market.getFactionId());
        }
        if (kind == TargetKind.PIRATE) return Factions.PIRATES.equals(market.getFactionId());
        if (kind == TargetKind.POPULATED || kind == TargetKind.EDGE_MARKET) return market.getSize() >= 3;
        if (kind == TargetKind.MILITARY) {
            return market.hasIndustry("militarybase") || market.hasIndustry("highcommand") ||
                    market.hasIndustry("patrolhq");
        }
        return true;
    }
    private static boolean isCrisisMarket(MarketAPI market) {
        String id = market.getFactionId();
        return IIRT_Omega_Invasion.WATCHER_FACTION.equals(id) ||
                IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(id);
    }

    private static MarketAPI bestMarket(StarSystemAPI system) {
        MarketAPI best = null;
        for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system)) {
            if (market == null || market.isPlanetConditionMarketOnly() || isCrisisMarket(market)) continue;
            if (best == null || market.getSize() > best.getSize()) best = market;
        }
        return best;
    }

    private static PTSDCrisisState.CrisisIncident createIncident(PTSDCrisisState state, Card card, Target target,
                                       float day, Random random, boolean forced) {
        int branch = random.nextInt(3);
        float branchMult = branch == 0 ? .85f : (branch == 2 ? 1.18f : 1f);
        PTSDCrisisState.CrisisIncident incident = new PTSDCrisisState.CrisisIncident();
        incident.id = "PTSD_incident_" + Misc.genUID();
        incident.cardId = card.id;
        incident.category = card.category;
        incident.randomBranch = branch;
        incident.phase = state.phase;
        incident.targetSystemId = target.system.getId();
        incident.targetMarketId = target.market == null ? null : target.market.getId();
        SectorEntityToken investigationTarget = target.targetLocation != null ?
                target.targetLocation : pickInvestigationTarget(card, target, random);
        incident.targetEntityId = investigationTarget == null ? null : investigationTarget.getId();
        incident.createdDay = day;
        incident.expiresDay = day + card.cooldown;
        incident.newsExpiresDay = day + 10f;
        incident.sourceLabel = sourceVariant(card.source, branch);
        incident.headline = headlineVariant(card.headlines[branch], branch);
        incident.publicText = reportVariant(card.reports[branch], branch);
        incident.trueText = truthVariant(card.truths[branch], branch);
        incident.disclosed = true;
        incident.investigable = card.investigable;
        incident.siteTemplate = inferSiteTemplates(card);
        incident.siteHandlerExpression = card.siteHandler == null ? "" : card.siteHandler;
        incident.martialSiteEligible = martialSiteEnabled(card);
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        incident.playerRelevant = player != null && player.getStarSystem() == target.system;
        incident.devForced = forced;
        incident.effectSummary = applyEffects(state, card, target.system.getId(), branchMult);
        incident.panicByMarket.putAll(PTSDLocalPanicAPI.spreadFromSystem(
                target.system.getId(), card.panic * branchMult,
                PTSDLocalPanicAPI.NEWS_RADIUS, card.id));
        if (Math.abs(card.panic * branchMult) >= .001f) {
            incident.effectSummary += "，局部恐慌" + signed(card.panic * branchMult) +
                    "（影响 " + incident.panicByMarket.size() + " 个殖民地）";
        }

        if (target.handler != null) {
            try {
                SectorEntityToken created = target.handler.onIncidentCreated(
                        new PTSDCrisisNewsAPI.IncidentContext(state, incident, target.system,
                                target.market, target.targetLocation, random));
                if (created != null) incident.targetEntityId = created.getId();
            } catch (Throwable ex) {
                Global.getLogger(PTSDCrisisIncidentManager.class).warn(
                        "Custom crisis news creation failed: " + card.targetExpression, ex);
                PTSDCrisisDevIntel.report("CUSTOM 新闻创建失败", card.targetExpression,
                        incident.targetSystemId, incident.targetEntityId);
            }
        }

        if (card.physicalChance > 0f && (forced || random.nextFloat() < card.physicalChance)) {
            if (card.category.equals("火力侦察") && card.strength > 0f) {
                PTSDCrisisState.StrategicEvent event = state.addEvent(
                        PTSDCrisisState.EventType.FIRE_PROBE, PTSDCrisisAPI.SIDE_OMEGA,
                        IIRT_Omega_Invasion.WATCHER_FACTION, null, target.system.getId(),
                        target.market == null ? null : target.market.getId(),
                        Math.max(18f, card.strength * branchMult), between(random, 4f, 9f));
                event.description = card.id + "：" + incident.trueText;
                event.playerRelevant = incident.playerRelevant;
                event.referenceId = incident.id;
                incident.linkedEventId = event.id;
            } else if (incident.playerRelevant && ("D-01".equals(card.id) || "D-07".equals(card.id))) {
                projectDebris(target.system, target.market, card.id, random);
            } else if ("D-12".equals(card.id)) {
                state.nextScoutDay = Math.min(state.nextScoutDay, day + .25f);
            }
        }

        state.incidents.add(incident);
        while (state.incidents.size() > 100) state.incidents.remove(0);
        state.incidentCooldowns.put(card.id, day + card.cooldown);
        PTSDCrisisNewsIntel.report(incident);
        // PTSDNewsTicker.report(incident); 简介新闻条可动态但是需要字体所以烂完了
        PTSDCrisisDevIntel.report("随机事件 " + card.id,
                "分支 " + branch + "|公开：" + incident.publicText + "|报告：" + incident.trueText +
                        "|影响：" + incident.effectSummary,
                incident.targetSystemId, null);

        if (incident.playerRelevant) {
            Global.getSector().getCampaignUI().addMessage(incident.headline + "：" + incident.publicText,
                    WHISPER_COLOR);
        }
        return incident;
    }

    private static SectorEntityToken pickInvestigationTarget(Card card, Target target, Random random) {
        if (target == null || target.system == null) return null;
        if ("P-08".equals(card.id) && !target.system.getJumpPoints().isEmpty()) {
            return target.system.getJumpPoints().get(random.nextInt(target.system.getJumpPoints().size()));
        }
        if (card.target == TargetKind.RELAY) {
            List<SectorEntityToken> relays = target.system.getEntitiesWithTag(Tags.COMM_RELAY);
            if (!relays.isEmpty()) return relays.get(random.nextInt(relays.size()));
        }
        if (target.market != null && target.market.getPrimaryEntity() != null) return target.market.getPrimaryEntity();
        List<SectorEntityToken> candidates = new ArrayList<SectorEntityToken>();
        for (com.fs.starfarer.api.campaign.PlanetAPI planet : target.system.getPlanets()) {
            if (planet != null && !planet.isStar()) candidates.add(planet);
        }
        candidates.addAll(target.system.getJumpPoints());
        if (!candidates.isEmpty()) return candidates.get(random.nextInt(candidates.size()));
        return target.system.getHyperspaceAnchor();
    }

    /** Recorded investigations continue independently of the ten-day news item, up to thirty days. */
    private static void advanceInvestigations(PTSDCrisisState state, float day, Random random) {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        for (PTSDCrisisState.CrisisIncident incident : state.incidents) {
            if (incident == null || !incident.recordedByPlayer || incident.investigationResolved) continue;
            if (incident.investigationExpiresDay <= 0f) incident.investigationExpiresDay = incident.createdDay + 30f;
            if (day >= incident.investigationExpiresDay) {
                incident.investigationResolved = true;
                incident.investigationReal = false;
                PTSDCrisisDevIntel.report("新闻调查过期", incident.headline + " / 达到30日上限", incident.targetSystemId, incident.targetEntityId);
                continue;
            }
            SectorEntityToken target = PTSDCrisisAPI.resolveIncidentTarget(incident);
            if (player == null || target == null || player.getContainingLocation() != target.getContainingLocation()) continue;
            float radius = Math.max(1800f, target.getRadius() + 1200f);
            if (Misc.getDistance(player.getLocation(), target.getLocation()) > radius) continue;
            incident.investigationResolved = true;
            if (incident.investigationOutcome == 1) {
                incident.investigationReal = true;
                PTSDNewsSiteManager.confirm(state, incident);
                Global.getSector().getCampaignUI().addMessage("现场发现了与报道相符的异常：" +
                        (incident.siteTitle == null || incident.siteTitle.isEmpty() ? "未分类痕迹" : incident.siteTitle), WHISPER_COLOR);
            } else if (incident.investigationOutcome == 3) {
                incident.investigationReal = true;
                IIRT_Omega_Invasion.spawnNewsTracker(incident.targetSystemId, target);
                Global.getSector().getCampaignUI().addMessage("传感器边缘出现了一个正在跟踪你的微弱信号。", WHISPER_COLOR);
            } else {
                incident.investigationReal = false;
                Global.getSector().getCampaignUI().addMessage("现场没有任何异常；这条报道已被证伪。", Misc.getGrayColor());
            }
            PTSDCrisisDevIntel.report("新闻调查结算", "结果 " + incident.investigationOutcome,
                    incident.targetSystemId, incident.targetEntityId);
        }
        for (int i = state.signalTraces.size() - 1; i >= 0; i--) {
            PTSDCrisisState.SignalTrace trace = state.signalTraces.get(i);
            if (trace == null || trace.expiresDay <= day) state.signalTraces.remove(i);
        }
    }
    private static String inferSiteTemplates(Card card) {
        if (card.siteTemplates != null && !card.siteTemplates.isEmpty() && !"AUTO".equalsIgnoreCase(card.siteTemplates)) return card.siteTemplates;
        if (card.target == TargetKind.RELAY) return PTSDNewsSiteManager.COMMUNICATION + "|" + PTSDNewsSiteManager.ROUTE;
        if ("火力侦察".equals(card.category) || card.id.startsWith("P-")) return PTSDNewsSiteManager.BATTLE + "|" + PTSDNewsSiteManager.ROUTE;
        if (card.id.equals("D-01") || card.id.equals("D-04") || card.id.equals("D-09")) return PTSDNewsSiteManager.CREW;
        if (card.id.equals("D-07") || card.id.equals("D-12")) return PTSDNewsSiteManager.BATTLE;
        if (card.target == TargetKind.WILDERNESS) return PTSDNewsSiteManager.FACILITY + "|" + PTSDNewsSiteManager.DISTORTION;
        if (card.target == TargetKind.POPULATED || card.target == TargetKind.EDGE_MARKET) return PTSDNewsSiteManager.COMMUNICATION + "|" + PTSDNewsSiteManager.CREW;
        if (card.target == TargetKind.MILITARY || card.target == TargetKind.PIRATE) return PTSDNewsSiteManager.BATTLE;
        return PTSDNewsSiteManager.DISTORTION + "|" + PTSDNewsSiteManager.ROUTE;
    }

    private static boolean martialSiteEnabled(Card card) {
        if ("TRUE".equalsIgnoreCase(card.martialSite)) return true;
        if ("FALSE".equalsIgnoreCase(card.martialSite)) return false;
        return "火力侦察".equals(card.category) || card.strength >= 20f && card.aggression >= 1f;
    }
    private static String applyEffects(PTSDCrisisState state, Card card, String systemId, float mult) {
        add(state, PTSDCrisisProgress.Variable.RECON_CONFIDENCE, card.recon * mult, card.id, systemId);
        add(state, PTSDCrisisProgress.Variable.HUMAN_AWARENESS, card.awareness * mult, card.id, systemId);
        add(state, PTSDCrisisProgress.Variable.WATCHER_AGGRESSION, card.aggression * mult, card.id, systemId);
        add(state, PTSDCrisisProgress.Variable.REALITY_DISTORTION, card.distortion * mult, card.id, systemId);
        PTSDCrisisState.SystemData data = state.getSystemData(systemId);
        data.lastObservedDay = PTSDCrisisState.getDay();
        data.attackWeight *= 1f + Math.min(.22f, (card.recon + card.aggression) * .018f * mult);
        if (card.category.equals("火力侦察")) data.hostileContacts++;
        return "侦察+" + round(card.recon * mult) + "，认知+" + round(card.awareness * mult) +
                "，攻击性+" + round(card.aggression * mult);
    }

    private static void add(PTSDCrisisState state, PTSDCrisisProgress.Variable variable,
                            float amount, String source, String systemId) {
        if (amount > 0f) PTSDCrisisProgress.add(state, variable, amount, source, systemId);
    }

    private static void projectDebris(StarSystemAPI system, MarketAPI market, String cardId, Random random) {
        SectorEntityToken focus = market != null && market.getPrimaryEntity() != null ?
                market.getPrimaryEntity() : system.getCenter();
        if (focus == null) return;
        DebrisFieldParams params = new DebrisFieldParams(180f, -1f, 5f, .25f);
        params.source = DebrisFieldSource.BATTLE;
        params.baseSalvageXP = 20;
        SectorEntityToken debris = Misc.addDebrisField(system, params, random);
        Vector2f point = null;
        for (int attempt = 0; attempt < 24; attempt++) {
            Vector2f candidate = Misc.getPointAtRadius(focus.getLocation(),
                    Math.max(1600f, focus.getRadius() + 900f) + random.nextFloat() * 1800f);
            boolean safe = true;
            for (com.fs.starfarer.api.campaign.PlanetAPI planet : system.getPlanets()) {
                if (Misc.getDistance(candidate, planet.getLocation()) < Math.max(1300f, planet.getRadius() + 900f)) {
                    safe = false; break;
                }
            }
            if (safe) { point = candidate; break; }
        }
        if (point == null && !system.getJumpPoints().isEmpty()) {
            SectorEntityToken jump = system.getJumpPoints().get(0);
            point = Misc.getPointAtRadius(jump.getLocation(), Math.max(900f, jump.getRadius() + 600f));
        }
        if (point == null) point = Misc.getPointAtRadius(focus.getLocation(), 7000f);
        debris.setLocation(point.x, point.y);
        debris.setName("无法归类的微小碎片");
        PTSDCrisisDevIntel.report("未知实体投影 " + cardId, "仅在玩家已位于目标星系时生成", system.getId(), debris.getId());
    }

    private static String sourceVariant(String source, int branch) {
        if (branch == 0) return source;
        if (branch == 1) return source + "（转述）";
        return "未经核实 / " + source;
    }

    private static String headlineVariant(String text, int branch) {
        if (branch == 0) return text;
        if (branch == 1) return text + "：后续记录";
        return "未证实：" + text;
    }

    private static String reportVariant(String text, int branch) {
        if (branch == 0) return text;
        if (branch == 1) return text + " 当地机构称没有理由发布进一步警告。";
        return text + " 不同来源对时间和方位的描述并不一致。";
    }

    private static String truthVariant(String text, int branch) {
        if (branch == 0) return text + " 它们的行动似乎早于预定时间结束了。";
        if (branch == 1) return text;
        return text + " 这或许是它们的误差，但事件本身也一定被写入了下一轮参数。";
    }

    private static float between(Random random, float min, float max) {
        float low = Math.min(min, max);
        return low + random.nextFloat() * Math.max(0f, Math.max(min, max) - low);
    }

    private static String signed(float value) {
        return (value >= 0f ? "+" : "") + round(value);
    }
    private static String round(float value) {
        return String.valueOf(Math.round(value * 10f) / 10f);
    }
}
