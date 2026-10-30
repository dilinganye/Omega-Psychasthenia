package data.scripts.campaign.invasion;

/** Public location policy. A randomly selected simulation system is not necessarily a reported place. */
public final class PTSDNewsPresentation {
    private PTSDNewsPresentation() { }
    public static boolean nonSpatial(String cardId) { return "F-TT-01".equals(cardId); }
    public static boolean ambientSite(PTSDCrisisState.CrisisIncident item) {
        return item != null && "N-09".equals(item.cardId);
    }
    public static boolean canInvestigate(PTSDCrisisState.CrisisIncident item) {
        return item != null && item.investigable && !nonSpatial(item.cardId) && !ambientSite(item);
    }
    public static boolean hasPublicLocation(PTSDCrisisState.CrisisIncident item) {
        if (item == null || nonSpatial(item.cardId) || item.targetSystemId == null || item.targetSystemId.trim().isEmpty()) return false;
        return ambientSite(item) || canInvestigate(item) || item.siteMaterialized ||
                item.linkedEventId != null ||
                (item.customTargetHandlerExpression != null && !item.customTargetHandlerExpression.isEmpty());
    }
}
