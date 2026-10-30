package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;

/** Completion-time settlement only; never scans historical incidents. */
public final class PTSDInvestigationRewards {
    public static final String REPORT = "PTSD_investigation_report";
    private PTSDInvestigationRewards() { }
    public static boolean hasEvidence(PTSDCrisisState.CrisisIncident i) {
        return i != null && (i.eventEvidenceCollected || i.siteConfirmed || (i.scene != null && (i.scene.samples > 0 || i.scene.recordTaken ||
                (i.scene.portEvidence != null && !i.scene.portEvidence.isEmpty()) || i.scene.rescuedShips > 0 ||
                (i.scene.recon!=null && i.scene.recon.observedOnce) ||
                (i.scene.blockade!=null && i.scene.blockade.seen))));
    }
    public static void complete(PTSDCrisisState.CrisisIncident i) {
        if (i == null || !i.investigationResolved || i.investigationAbandoned) return;
        if (PTSDTaskRules.successfulInvestigation(i.investigationResult)) {
            if (i.scenarioTrigger == null || i.scenarioResolved) award(i, 3);
        } else if (i.investigationResult == PTSDTaskRules.InvestigationResult.INCONCLUSIVE && hasEvidence(i)) award(i, 1);
    }
    public static void award(PTSDCrisisState.CrisisIncident i, int amount) {
        if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null) return;
        settle(i, amount, Global.getSector().getPlayerFleet().getCargo());
    }
    public static void agent(PTSDCrisisState.CrisisIncident i) {
        if(Global.getSector()==null || Global.getSector().getPlayerFleet()==null)return;
        settle(i,1,Global.getSector().getPlayerFleet().getCargo(),true);
    }
    public static boolean settle(PTSDCrisisState.CrisisIncident i, int amount, CargoAPI cargo) {
        return settle(i,amount,cargo,false);
    }
    public static boolean settle(PTSDCrisisState.CrisisIncident i, int amount, CargoAPI cargo, boolean agent) {
        if (i == null || !i.investigable || (!i.recordedByPlayer && !agent) || i.investigationReportsAwarded > 0 ||
                cargo == null || (amount != 1 && amount != 3)) return false;
        cargo.addSpecial(new SpecialItemData(REPORT, null), amount);
        i.investigationReportsAwarded = amount;
        if(Global.getSector()!=null && Global.getSector().getCampaignUI()!=null)
            Global.getSector().getCampaignUI().addMessage("获得调查报告 ×"+amount);
        return true;
    }
}
