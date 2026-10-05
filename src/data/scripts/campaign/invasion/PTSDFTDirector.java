package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import data.scripts.campaign.invasion.news.ft.AbstractFTHandler;
import java.util.ArrayList;
import java.util.Map;

/** Dispatch only. Each CSV handler owns its chain and its physical consequences. */
public final class PTSDFTDirector {
    private PTSDFTDirector() { }

    private static AbstractFTHandler handler(String id) {
        PTSDCrisisIncidentManager.ScriptedTemplate template = PTSDCrisisIncidentManager.getTemplate(id);
        if (template == null || template.targetExpression == null) return null;
        String target = template.targetExpression;
        if (!target.startsWith("CUSTOM(") || !target.endsWith(")")) return null;
        PTSDCrisisNewsAPI.CustomNewsHandler handler = PTSDCrisisNewsAPI.resolveHandler(
                target.substring(7, target.length() - 1));
        return handler instanceof AbstractFTHandler ? (AbstractFTHandler) handler : null;
    }

    private static void migrate(PTSDCrisisState state) {
        if (state.late.ftChainVersion >= 2) return;
        for (PTSDLateState.EventCard old : state.late.eventCards) {
            if (old == null || old.id == null || !old.id.startsWith("FT-")) continue;
            boolean wasResolved = old.resolved;
            old.resolved = true;
            if (old.accepted || wasResolved) {
                if (old.outcome == null || old.outcome.isEmpty())
                    old.outcome = "旧版事件已归档；已结算结果保留";
            } else old.outcome = "旧版未结算事件已安全归档";
            state.late.completedCards.add(old.id);
        }
        state.late.ftChainVersion = 2;
    }

    public static String unavailableReason(PTSDCrisisState state, String id) {
        if (state == null || state.late == null) return "危机状态不存在";
        migrate(state);
        AbstractFTHandler handler = handler(id);
        if (handler == null) return "未找到专属事件处理器";
        if (state.late.completedCards.contains(id) || state.late.ftChains.containsKey(id))
            return "事件已经开始或结算";
        if (state.phase != PTSDCrisisState.Phase.FORTIFICATION &&
                !("FT-10".equals(id) && state.phase == PTSDCrisisState.Phase.MOBILIZATION))
            return "当前不在事件适用阶段";
        return handler.unavailableReason(state);
    }

    public static boolean force(PTSDCrisisState state, String id) {
        String reason = unavailableReason(state, id);
        if (!"可触发".equals(reason)) {
            PTSDCrisisDevIntel.report("FT 事件未投放", id + "：" + reason,
                    state == null ? null : state.baseSystemId, null);
            return false;
        }
        AbstractFTHandler handler = handler(id);
        PTSDLateState.FTChain chain = new PTSDLateState.FTChain();
        chain.id = id;
        chain.startedDay = PTSDCrisisState.getDay();
        chain.expiresDay = chain.startedDay + 45f;
        handler.start(state, chain);
        if (chain.node == null) return false;
        state.late.ftChains.put(id, chain);
        state.late.completedCards.add(id);
        PTSDCrisisIncidentManager.ScriptedTemplate template = PTSDCrisisIncidentManager.getTemplate(id);
        if (template != null && "PUBLIC".equals(template.visibility) && chain.systemId != null) {
            String publicSystem = chain.systemId;
            String publicTarget = chain.targetId;
            if ("FT-02".equals(id) && state.resolveMarket(chain.marketId) != null) {
                publicSystem = state.resolveMarket(chain.marketId).getStarSystem().getId();
                publicTarget = state.resolveMarket(chain.marketId).getPrimaryEntity().getId();
            }
            PTSDCrisisIncidentManager.publishScripted(state, id, publicSystem,
                    publicTarget, "要塞与动员", false);
        }
        if (!chain.resolved) PTSDFTIntel.ensureIntel();
        if ("FT-09".equals(id)) PTSDCrisisIntel.ensureIntel();
        if ("FT-10".equals(id)) PTSDFTDialog.open(id);
        PTSDCrisisDevIntel.report("FT 事件开始", id + "：" + chain.node,
                chain.systemId, chain.targetId);
        return true;
    }

    public static void advance(PTSDCrisisState state, float day) {
        if (state == null || state.late == null) return;
        migrate(state);
        for (PTSDLateState.FTChain chain : new ArrayList<PTSDLateState.FTChain>(state.late.ftChains.values())) {
            if (chain == null || chain.resolved) continue;
            AbstractFTHandler handler = handler(chain.id);
            if (handler != null) handler.advanceChain(state, chain, day);
        }
        if (state.phase != PTSDCrisisState.Phase.FORTIFICATION &&
                state.phase != PTSDCrisisState.Phase.MOBILIZATION) return;
        if (state.phase == PTSDCrisisState.Phase.FORTIFICATION &&
                "可触发".equals(unavailableReason(state, "FT-09"))) force(state, "FT-09");
        if (day >= state.late.nextFT10RollDay) {
            state.late.nextFT10RollDay = day + 10f;
            if ("可触发".equals(unavailableReason(state, "FT-10")) &&
                    Math.random() < .10d) force(state, "FT-10");
        }
        // FT cards are individually gated. One start per heartbeat avoids a wall of simultaneous news.
        if (day < state.late.nextCardDay) return;
        for (int i = 1; i <= 10; i++) {
            String id = String.format("FT-%02d", i);
            if ("FT-10".equals(id)) continue;
            if (!"可触发".equals(unavailableReason(state, id))) continue;
            PTSDCrisisIncidentManager.ScriptedTemplate template = PTSDCrisisIncidentManager.getTemplate(id);
            if (template == null || day - state.phaseStartedDay < template.minDay) continue;
            if (force(state, id)) { state.late.nextCardDay = day + 5f; break; }
        }
    }

    public static boolean choose(PTSDCrisisState state, String id, String action) {
        if (state == null || state.late == null) return false;
        PTSDLateState.FTChain chain = state.late.ftChains.get(id);
        AbstractFTHandler handler = handler(id);
        return chain != null && !chain.resolved && handler != null && handler.choose(state, chain, action);
    }

    public static void onTransportLost(PTSDCrisisState state, String fleetId, String systemId,
                                       float x, float y) {
        if (state == null || state.late == null || fleetId == null) return;
        if (!state.late.committedIds.add("ft-transport-loss:" + fleetId)) return;
        state.late.committedIds.add("ft09-loss-ready");
        state.late.lastFTTransportLossId = fleetId;
        state.late.lastFTTransportLossSystemId = systemId;
        state.late.lastFTTransportLossX = x;
        state.late.lastFTTransportLossY = y;
        // Battle callbacks may run while an interaction dialog owns the UI. The next campaign
        // heartbeat starts the lead after the physical loss has been committed once.
    }

    public static void onPlayerDefeat(PTSDCrisisState state, CampaignFleetAPI fleet) {
        if (state == null || state.late == null) return;
        if (fleet != null && fleet.getMemoryWithoutUpdate().getBoolean("$PTSD_ft03")) {
            PTSDLateState.FTChain trap = state.late.ftChains.get("FT-03");
            if (trap != null) trap.outcome = "伏击舰队受到实际战损；剩余舰只正自行脱离";
        }
    }

    public static void onPlayerEncounter(PTSDCrisisState state, String fleetId) {
        if (state == null || state.late == null || fleetId == null ||
                !state.late.committedIds.add("ft05-encounter:" + fleetId)) return;
        PTSDLateState.FTChain chain = state.late.ftChains.get("FT-05");
        if (chain != null && !chain.resolved) chain.encounters++;
    }

    public static void onMotherInteraction(PTSDCrisisState state, String targetId) {
        if (state == null || state.late == null || targetId == null ||
                state.phase != PTSDCrisisState.Phase.FORTIFICATION) return;
        if (!state.late.ftChains.containsKey("FT-04")) force(state, "FT-04");
        PTSDLateState.FTChain chain = state.late.ftChains.get("FT-04");
        if (chain != null && !chain.resolved && targetId.equals(chain.targetId)) {
            chain.node = "DIRECT_CONTACT";
            chain.outcome = "近轨道防务已锁定交互舰队。";
        }
    }

    public static SectorEntityToken target(PTSDLateState.FTChain chain) {
        if (chain == null || chain.targetId == null || Global.getSector() == null) return null;
        return Global.getSector().getEntityById(chain.targetId);
    }

    public static String playerText(String text) {
        if (text == null) return "";
        try {
            String replaced = com.fs.starfarer.api.util.Misc.getStringWithTokenReplacement(text,
                    Global.getSector().getPlayerFleet(),
                    java.util.Collections.<String, com.fs.starfarer.api.campaign.rules.MemoryAPI>emptyMap());
            return replaced.replace("$playerSirOrMadam", "长官");
        } catch (Throwable ignored) {
            return text.replace("$playerSirOrMadam", "长官");
        }
    }
}
