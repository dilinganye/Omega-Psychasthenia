package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import org.lwjgl.util.vector.Vector2f;

/** Navigation hints for observation/search scenes, never their interaction or simulation targets. */
public final class PTSDSearchAreaNavigation {
    private PTSDSearchAreaNavigation() { }
    static boolean searches(PTSDNewsScene.Kind kind) {
        return kind == PTSDNewsScene.Kind.STATION_BLACKOUT || kind == PTSDNewsScene.Kind.RECON_FIRE_CONTROL ||
                kind == PTSDNewsScene.Kind.DARK_TRANSIT || kind == PTSDNewsScene.Kind.FORT_PATROL_GAP;
    }
    public static SectorEntityToken resolve(PTSDCrisisState.CrisisIncident incident) {
        SectorEntityToken target = PTSDCrisisAPI.resolveIncidentTarget(incident);
        if (incident == null || target == null || Global.getSector() == null) return target;
        PTSDNewsScene.Kind kind = incident.scene == null ? PTSDNewsScene.parse(incident.siteTemplate) : incident.scene.kind;
        if (!searches(kind)) return target;
        if (kind == PTSDNewsScene.Kind.FORT_PATROL_GAP && incident.scene != null) {
            SectorEntityToken facility=PTSDNewsScene.entity(incident.scene.reference);
            if(facility!=null)target=facility;
        }
        StarSystemAPI system = PTSDCrisisState.get().resolveSystem(incident.targetSystemId);
        if (system == null) return target;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null || player.getContainingLocation() != system) return system.getHyperspaceAnchor();
        Vector2f point;
        PTSDReconScenes.Progress progress = incident.scene == null ? null : incident.scene.recon;
        if (kind == PTSDNewsScene.Kind.STATION_BLACKOUT && progress != null && progress.searchReady) {
            point = new Vector2f(progress.searchX, progress.searchY);
        } else {
            if (target.getContainingLocation() != system) return system.getHyperspaceAnchor();
            point = PTSDNewsScene.offset(target.getLocation(), Math.floorMod(incident.id == null ? 0 : incident.id.hashCode(), 360),
                    Math.max(1800f, target.getRadius() + 1400f));
            if (!PTSDCrisisAPI.isSafePoint(system, point)) {
                point = PTSDCrisisAPI.findSafePoint(system, target, Math.max(1800f,target.getRadius()+1400f),
                        Math.max(3000f,target.getRadius()+2600f),new java.util.Random(incident.id == null ? 0 : incident.id.hashCode()));
            }
        }
        if (point == null) return system.getHyperspaceAnchor();
        SectorEntityToken area = system.createToken(point);
        area.setName(kind == PTSDNewsScene.Kind.STATION_BLACKOUT ? "空间站外搜索空域" : "现场搜索空域");
        return area;
    }
}
