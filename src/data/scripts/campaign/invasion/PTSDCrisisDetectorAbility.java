package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.campaign.abilities.BaseToggleAbility;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Je's crisis-only derivative of the neutrino detector. */
public final class PTSDCrisisDetectorAbility extends BaseToggleAbility {
    public static final String ID = "PTSD_crisis_signal_correlator";
    public static final float EVENT_FREQUENCY_MULT = 1.45f;
    private float phase;

    public static boolean isPlayerDetectorActive() {
        if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null) return false;
        AbilityPlugin ability = Global.getSector().getPlayerFleet().getAbility(ID);
        return ability != null && ability.isActive();
    }

    public static float getEventFrequencyMultiplier() {
        return isPlayerDetectorActive() ? EVENT_FREQUENCY_MULT : 1f;
    }

    @Override protected String getActivationText() { return "危机信号相关器已启动"; }
    @Override protected String getDeactivationText() { return "危机信号相关器已关闭"; }
    @Override protected void activateImpl() { }
    @Override protected void deactivateImpl() { cleanupImpl(); }
    @Override protected void cleanupImpl() { }
    @Override protected void applyEffect(float amount, float level) {
        if (Global.getSector() != null) phase += Global.getSector().getClock().convertToDays(amount) * 420f;
    }
    @Override public boolean showProgressIndicator() { return false; }
    @Override public boolean showActiveIndicator() { return isActive(); }
    @Override public boolean hasTooltip() { return true; }
    @Override public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        Color highlight = Misc.getHighlightColor();
        String status = isActive() ? "(开启)" : "(关闭)";
        tooltip.addTitle("危机信号相关器 " + status);
        tooltip.addPara("将第未知来源活动留下的遥测噪声进行相关分析，并以方向标记提示当前区域内的调查目标和...有同样反应的东西。", 10f);
        tooltip.addSectionHeading("代价", Alignment.MID, 10f);
        tooltip.addPara("预测将导致受到高约 %s 的额外关注。此系统只显示目标大致方向 - 但不保证目标仍然安全或可交互。",
                10f, highlight, Math.round((EVENT_FREQUENCY_MULT - 1f) * 100f) + "%");
    }
    @Override public EnumSet<CampaignEngineLayers> getActiveLayers() { return EnumSet.of(CampaignEngineLayers.ABOVE); }

    @Override public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (layer != CampaignEngineLayers.ABOVE || !isActive() || getProgressFraction() <= 0f) return;
        CampaignFleetAPI player = getFleet();
        if (player == null || !player.isPlayerFleet() || player.getContainingLocation() == null) return;
        List<SectorEntityToken> targets = collectTargets(player);
        float view = Math.max(.2f, viewport.getViewMult());
        float radius = player.getRadius() + 95f / view;
        float size = 13f / view;
        float pulse = .72f + .28f * (float)Math.sin(Math.toRadians(phase));
        int shown = 0;
        for (SectorEntityToken target : targets) {
            if (target == null || target == player || target.getContainingLocation() != player.getContainingLocation()) continue;
            Vector2f direction = Vector2f.sub(target.getLocation(), player.getLocation(), null);
            if (direction.lengthSquared() < 1f) continue;
            direction.normalise();
            float x = player.getLocation().x + direction.x * radius;
            float y = player.getLocation().y + direction.y * radius;
            float angle = Misc.getAngleInDegrees(new Vector2f(), direction);
            drawArrow(x, y, angle, size, pulse * viewport.getAlphaMult());
            if (++shown >= 12) break;
        }
    }

    private List<SectorEntityToken> collectTargets(final CampaignFleetAPI player) {
        LocationAPI location = player.getContainingLocation();
        Set<SectorEntityToken> unique = new LinkedHashSet<SectorEntityToken>();
        for (SectorEntityToken entity : location.getAllEntities()) {
            if (isCrisisEntity(entity)) unique.add(entity);
        }
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null) {
            for (PTSDCrisisState.StrategicEvent event : state.getActiveEvents()) {
                if (event.materializedFleetIds != null) for (String id : event.materializedFleetIds) {
                    SectorEntityToken entity = Global.getSector().getEntityById(id);
                    if (entity != null && entity.getContainingLocation() == location) unique.add(entity);
                }
                SectorEntityToken target = event.targetEntityId == null ? null : Global.getSector().getEntityById(event.targetEntityId);
                if (target != null && target.getContainingLocation() == location) unique.add(target);
                if (player.isInHyperspace() && event.materializedFleetIds != null && !event.materializedFleetIds.isEmpty()) {
                    StarSystemAPI system = state.resolveSystem(event.targetSystemId);
                    if (system != null && system.getHyperspaceAnchor() != null) unique.add(system.getHyperspaceAnchor());
                }
            }
            for (PTSDCrisisState.CrisisIncident incident : state.incidents) {
                if (incident == null || incident.investigationResolved) continue;
                SectorEntityToken target = incident.targetEntityId == null ? null : Global.getSector().getEntityById(incident.targetEntityId);
                if (target != null && target.getContainingLocation() == location) unique.add(target);
                if (player.isInHyperspace() && target != null) {
                    StarSystemAPI system = state.resolveSystem(incident.targetSystemId);
                    if (system != null && system.getHyperspaceAnchor() != null) unique.add(system.getHyperspaceAnchor());
                }
            }
        }
        List<SectorEntityToken> result = new ArrayList<SectorEntityToken>(unique);
        Collections.sort(result, new Comparator<SectorEntityToken>() {
            @Override public int compare(SectorEntityToken a, SectorEntityToken b) {
                return Float.compare(Misc.getDistance(player.getLocation(), a.getLocation()),
                        Misc.getDistance(player.getLocation(), b.getLocation()));
            }
        });
        return result;
    }

    private boolean isCrisisEntity(SectorEntityToken entity) {
        if (entity == null || entity == getFleet()) return false;
        String faction = entity.getFaction() == null ? null : entity.getFaction().getId();
        if (IIRT_Omega_Invasion.WATCHER_FACTION.equals(faction) || IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(faction)) return true;
        return entity.hasTag("PTSD_crisis_fleet") || entity.hasTag("PTSD_omega_scout") ||
                entity.hasTag("PTSD_news_target") || entity.hasTag("PTSD_occupation_asset");
    }

    private void drawArrow(float x, float y, float angle, float size, float alpha) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0f);
        GL11.glRotatef(angle, 0f, 0f, 1f);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glColor4f(.52f, .18f, 1f, Math.max(0f, Math.min(1f, alpha)));
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex2f(size, 0f);
        GL11.glVertex2f(-size * .65f, size * .55f);
        GL11.glVertex2f(-size * .65f, -size * .55f);
        GL11.glEnd();
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }
}