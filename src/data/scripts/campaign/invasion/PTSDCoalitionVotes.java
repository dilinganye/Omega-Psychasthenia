package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import java.util.Map;
import java.util.LinkedHashMap;

/** Submission-time electorate; later membership changes cannot rewrite an open vote. */
public final class PTSDCoalitionVotes {
    private PTSDCoalitionVotes() { }
    public static void snapshot(PTSDCrisisState state, PTSDLateState.Resolution vote) {
        boolean legacy = vote.seatSnapshot == null;
        if (vote.seatSnapshot == null) vote.seatSnapshot = new LinkedHashMap<String, Float>();
        if (vote.ballots == null) vote.ballots = new LinkedHashMap<String, String>();
        if (!vote.seatSnapshot.isEmpty()) return;
        if (legacy) vote.day = PTSDCrisisState.getDay(); // Give old pending proposals a real response window.
        vote.seatSnapshot.putAll(PTSDGeneralStaffDirector.seats(state));
        PTSDLateState.Front front = state.late.fronts.get(vote.frontId);
        MarketAPI target = front == null ? null : state.resolveMarket(front.marketId);
        for (String member : vote.seatSnapshot.keySet()) {
            if (member.equals(Global.getSector().getPlayerFaction().getId())) {
                vote.ballots.put(member, "PENDING"); continue;
            }
            FactionAPI faction = Global.getSector().getFaction(member);
            float relation = faction == null ? -1f : faction.getRelationship(vote.sponsorFactionId);
            boolean local = target != null && member.equals(target.getFactionId());
            boolean evidence = front != null && PTSDGeneralStaffDirector.hasRecentHumanReport(state, front.systemId);
            boolean supported = "SUPPLIES".equals(vote.method) || "EXPORT".equals(vote.method) ||
                    "PRESSURE".equals(vote.method) || "EVIDENCE".equals(vote.method) && PTSDGeneralStaffDirector.playerScore(state) >= 20f;
            String ballot = local || member.equals(vote.sponsorFactionId) || relation >= 0f ||
                    (evidence || supported) && relation >= -.4f ? "YES" : relation < -.4f ? "NO" : "ABSTAIN";
            vote.ballots.put(member, ballot);
        }
    }
    public static float weight(PTSDLateState.Resolution vote, String ballot) {
        float sum = 0f;
        if (vote.seatSnapshot != null && vote.ballots != null)
            for (Map.Entry<String, Float> entry : vote.seatSnapshot.entrySet())
                if (ballot.equals(vote.ballots.get(entry.getKey()))) sum += entry.getValue();
        return sum;
    }
    public static boolean passed(PTSDLateState.Resolution vote) {
        float total = 0f;
        if (vote.seatSnapshot != null) for (float value : vote.seatSnapshot.values()) total += value;
        return total > 0f && weight(vote, "YES") > total * .5f;
    }
    public static boolean cast(PTSDCrisisState state, String id, String choice) {
        if (!"YES".equals(choice) && !"NO".equals(choice) && !"ABSTAIN".equals(choice)) return false;
        for (PTSDLateState.Resolution vote : state.late.resolutions) {
            if (!id.equals(vote.id) || vote.resolved || PTSDCrisisState.getDay() >= vote.day + 2f) continue;
            String player = Global.getSector().getPlayerFaction().getId();
            if (vote.seatSnapshot == null || !vote.seatSnapshot.containsKey(player)) return false;
            vote.ballots.put(player, choice); return true;
        }
        return false;
    }
}
