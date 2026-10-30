package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.BaseFleetEventListener;

import java.util.List;

/** Bridges any crisis-fleet defeat back into the save-persistent strategic model. */
public final class PTSDStrategicFleetListener extends BaseFleetEventListener {
    private final String eventId;
    private final float initialStrength;

    public PTSDStrategicFleetListener(String eventId) {
        this(eventId, 0f);
    }

    public PTSDStrategicFleetListener(String eventId, float initialStrength) {
        this.eventId = eventId;
        this.initialStrength = Math.max(0f, initialStrength);
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (fleet == null || battle == null) return;
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return;
        if (battle.isPlayerInvolved()) PTSDJeOtloesManager.recordPlayerBattle(fleet);
        if (!battle.isDone()) return;
        // A factory may install its event-specific listener after the common creation hook.
        // Keep the generic first-contact fallback, but leave strategic consequences to that listener.
        if (eventId == null) for (com.fs.starfarer.api.campaign.listeners.FleetEventListener listener : fleet.getEventListeners()) {
            if (listener != this && listener instanceof PTSDStrategicFleetListener &&
                    ((PTSDStrategicFleetListener) listener).eventId != null) return;
        }
        PTSDCrisisState.StrategicEvent event = eventId == null ? null : state.getEvent(eventId);
        boolean defeated = battle.wasFleetDefeated(fleet, primaryWinner);
        if (event != null && event.type == PTSDCrisisState.EventType.FIRE_PROBE) {
            event.probeEngaged = true;
            event.probePlayerIntervention |= battle.isPlayerInvolved();
            if (battle.isPlayerInvolved() && Global.getSector().getPlayerFleet() != null) {
                event.probeObservedPlayerFP = Math.max(event.probeObservedPlayerFP,
                        Global.getSector().getPlayerFleet().getFleetPoints());
            }
            if (defeated) event.probeDefeatedGroups++;
        }
        if (!defeated) return;
        if (fleet.getMemoryWithoutUpdate().contains("$PTSD_fortress_transport"))
            PTSDFTDirector.onTransportLost(state, fleet.getId(),
                    fleet.getStarSystem() == null ? null : fleet.getStarSystem().getId(),
                    fleet.getLocation().x, fleet.getLocation().y);
        if (event != null) {
            if (!PTSDCrisisAPI.SIDE_OMEGA.equals(event.side) || event.defeatLearningRecorded) return;
            event.defeatLearningRecorded = true;
        } else {
            if (fleet.getFaction() == null) return;
            String id = fleet.getFaction().getId();
            if (!IIRT_Omega_Invasion.WATCHER_FACTION.equals(id) &&
                    !IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(id)) return;
        }
        boolean player = battle.isPlayerInvolved() &&
                battle.wasFleetVictorious(Global.getSector().getPlayerFleet(), primaryWinner);
        String opponent = player ? Global.getSector().getPlayerFaction().getId() : findOpponentFaction(fleet, battle);
        String systemId = event == null ? (fleet.getStarSystem() == null ? null : fleet.getStarSystem().getId()) : event.targetSystemId;
        float strength = event == null ? Math.max(initialStrength, fleet.getFleetPoints()) : Math.max(event.strength, fleet.getFleetPoints());
        if (event != null) {
            event.opponentFactionId = opponent;
            event.aftermathKind = "OMEGA_DEFEAT";
        }
        PTSDCrisisAPI.recordOmegaDefeat(opponent, player, systemId, strength);
        if (player) PTSDLateEventDirector.onPlayerOmegaDefeat(state, fleet.getId(), systemId, event);
        if (player && state.late != null && state.late.ftCountedDefeats.add(fleet.getId())) {
            state.late.verifiedPlayerOmegaDefeats++;
            PTSDFTDirector.onPlayerDefeat(state, fleet);
        }
        if (player) {
            boolean witnessed = false;
            if (systemId != null) {
                PTSDCrisisState.SystemData local = state.getSystemData(systemId);
                witnessed = local != null && local.omegaControl >= .5f;
            }
            if (!witnessed && fleet.getContainingLocation() != null) {
                for (CampaignFleetAPI other : fleet.getContainingLocation().getFleets()) {
                    if (other == fleet || other.getFaction() == null) continue;
                    String id = other.getFaction().getId();
                    if (IIRT_Omega_Invasion.WATCHER_FACTION.equals(id) ||
                            IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(id)) {
                        witnessed = true;
                        break;
                    }
                }
            }
            PTSDThreatDirector.recordVictory(state, fleet.getId(), systemId, strength, witnessed);
        }
        if (fleet.getMemoryWithoutUpdate().getBoolean("$PTSD_fortress")) {
            PTSDFortressDirector.onFleetDefeated(state, fleet, player);
        }
        if (player && event != null && event.type == PTSDCrisisState.EventType.PREWAR_HUNTER) {
            event.successful = false;
            event.status = PTSDCrisisState.EventStatus.RESOLVED;
            state.prewarHunterResolved = true;
            state.prewarHunterResolvedDay = PTSDCrisisState.getDay();
            state.warCommitDay = state.prewarHunterResolvedDay + 1f + (float) Math.random() * 7f;
        }
    }

    private String findOpponentFaction(CampaignFleetAPI fleet, BattleAPI battle) {
        List<CampaignFleetAPI> others = battle.getOtherSideSnapshotFor(fleet);
        CampaignFleetAPI best = null;
        if (others != null) for (CampaignFleetAPI other : others) {
            if (other == null || other.getFaction() == null) continue;
            if (best == null || other.getFleetPoints() > best.getFleetPoints()) best = other;
        }
        return best == null || best.getFaction() == null ? "unknown" : best.getFaction().getId();
    }
}
