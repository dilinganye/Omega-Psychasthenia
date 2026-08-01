package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Extension surface for future stories, special battles and occupation-zone interactions. */
public final class PTSDOccupationAPI {
    public enum Action {
        HARASSMENT_BOMBARDMENT,
        SATURATION_BOMBARDMENT,
        PROBE,
        NEGOTIATION,
        DEFENSE_SPAWNED,
        DEFENSE_DEFEATED
    }

    public static final class InteractionContext {
        public final MarketAPI market;
        public final CampaignFleetAPI playerFleet;
        public final PTSDCrisisState state;
        public final PTSDCrisisState.OccupationData occupation;

        private InteractionContext(MarketAPI market) {
            this.market = market;
            this.playerFleet = Global.getSector().getPlayerFleet();
            this.state = PTSDCrisisState.get();
            this.occupation = state == null ? null : state.getOccupationData(market.getId());
        }
    }

    public interface InteractionExtension {
        void addOptions(InteractionContext context, InteractionDialogAPI dialog);
        boolean optionSelected(InteractionContext context, InteractionDialogAPI dialog, Object optionData);
        void reportAction(InteractionContext context, Action action, float omegaAttention, float humanAttention);
    }

    private static final Map<String, InteractionExtension> EXTENSIONS =
            new LinkedHashMap<String, InteractionExtension>();

    private PTSDOccupationAPI() {
    }

    public static void registerExtension(String id, InteractionExtension extension) {
        if (id != null && extension != null) EXTENSIONS.put(id, extension);
    }

    public static void unregisterExtension(String id) {
        if (id != null) EXTENSIONS.remove(id);
    }

    static InteractionContext createContext(MarketAPI market) {
        return market == null ? null : new InteractionContext(market);
    }

    static List<InteractionExtension> getExtensions() {
        return new ArrayList<InteractionExtension>(EXTENSIONS.values());
    }

    public static void addAttention(MarketAPI market, Action action,
                                    float omegaAttention, float humanAttention) {
        if (market == null || market.getStarSystem() == null) return;
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return;
        PTSDCrisisState.OccupationData occupation = state.getOccupationData(market.getId());
        occupation.omegaAttention += Math.max(0f, omegaAttention);
        occupation.humanAttention += Math.max(0f, humanAttention);
        occupation.lastInteractionDay = PTSDCrisisState.getDay();
        occupation.lastInteraction = action.name();

        PTSDCrisisState.SystemData system = state.getSystemData(market.getStarSystem().getId());
        system.attackWeight = Math.min(1000f,
                system.attackWeight * (1f + Math.max(0f, omegaAttention) * 0.08f) +
                        Math.max(0f, omegaAttention) * 0.5f);
        system.humanDefenseWeight = Math.min(1000f,
                system.humanDefenseWeight * (1f + Math.max(0f, humanAttention) * 0.05f) +
                        Math.max(0f, humanAttention) * 0.35f);
        system.knownToPlayer = true;

        String systemId = market.getStarSystem().getId();
        PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.OMEGA_ESCALATION,
                Math.max(0f, omegaAttention) * 1.5f, "OCCUPATION_" + action.name(), systemId);
        PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.HUMAN_AWARENESS,
                Math.max(0f, humanAttention) * 5f + Math.max(0f, omegaAttention) * 0.4f,
                "OCCUPATION_" + action.name(), systemId);
        PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.PUBLIC_PANIC,
                Math.max(0f, omegaAttention) * 0.8f, "OCCUPATION_" + action.name(), systemId);
        if (action == Action.HARASSMENT_BOMBARDMENT || action == Action.SATURATION_BOMBARDMENT ||
                action == Action.PROBE || action == Action.DEFENSE_DEFEATED) {
            PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.WATCHER_AGGRESSION,
                    Math.max(0.5f, omegaAttention), "PLAYER_HOSTILITY_" + action.name(), systemId);
        }
        if (action == Action.DEFENSE_DEFEATED) {
            PTSDCrisisProgress.add(state, PTSDCrisisProgress.Variable.HUMAN_COHESION,
                    1.5f, "DEFENSE_DEFEATED", systemId);
        }

        InteractionContext context = new InteractionContext(market);
        for (InteractionExtension extension : getExtensions()) {
            try {
                extension.reportAction(context, action, omegaAttention, humanAttention);
            } catch (Throwable ex) {
                Global.getLogger(PTSDOccupationAPI.class).warn("Occupation extension failed", ex);
            }
        }
        PTSDCrisisDevIntel.report("占领区互动",
                action.name() + " / Omega注意 +" + omegaAttention + " / 人类关注 +" + humanAttention,
                market.getStarSystem().getId(),
                market.getPrimaryEntity() == null ? null : market.getPrimaryEntity().getId());
    }
}
