package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

/** Non-interactive visual layer for the occupied-world intrusion sequence. */
public final class PTSDOccupationHostileVisual extends BaseCustomUIPanelPlugin {
    private PositionAPI position;
    private SpriteAPI planet;
    private SpriteAPI flagship;
    private SpriteAPI glow;
    private float elapsed;

    public PTSDOccupationHostileVisual(PlanetAPI target) {
        try {
            if (target != null && target.getSpec() != null) planet = Global.getSettings().getSprite(target.getSpec().getTexture());
        } catch (Throwable ignored) { }
        try {
            FleetMemberAPI member = Global.getSector().getPlayerFleet().getFlagship();
            if (member != null) flagship = Global.getSettings().getSprite(member.getHullSpec().getSpriteName());
        } catch (Throwable ignored) { }
        try { glow = Global.getSettings().getSprite("graphics/fx/explosion0.png"); }
        catch (Throwable ignored) { }
    }

    @Override
    public void positionChanged(PositionAPI position) { this.position = position; }

    @Override
    public void advance(float amount) { elapsed += Math.max(0f, amount); }

    public float getElapsed() { return elapsed; }

    @Override
    public void renderBelow(float alphaMult) {
        if (position == null) return;
        float x = position.getX(), y = position.getY(), w = position.getWidth(), h = position.getHeight();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4ub((byte) 2, (byte) 5, (byte) 12, (byte) (255f * alphaMult));
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y); GL11.glVertex2f(x, y + h); GL11.glVertex2f(x + w, y + h); GL11.glVertex2f(x + w, y);
        GL11.glEnd();
    }

    @Override
    public void render(float alphaMult) {
        if (position == null) return;
        float cx = position.getX() + position.getWidth() * 0.72f;
        float cy = position.getY() + position.getHeight() * 0.50f;
        float planetSize = Math.min(position.getHeight() * 1.15f, position.getWidth() * 0.62f);
        if (planet != null) {
            planet.setNormalBlend(); planet.setColor(new Color(255, 150, 220));
            planet.setAlphaMult(alphaMult); planet.setSize(planetSize, planetSize); planet.renderAtCenter(cx, cy);
        }
        if (flagship != null) {
            flagship.setNormalBlend(); flagship.setColor(Color.WHITE); flagship.setAlphaMult(alphaMult * 0.9f);
            float size = Math.max(18f, planetSize * 0.055f);
            flagship.setSize(size, size); flagship.setAngle(20f); flagship.renderAtCenter(cx - planetSize * 0.56f, cy + planetSize * 0.05f);
        }
        if (elapsed >= 10f && glow != null) {
            float t = elapsed - 10f;
            float flicker = 0.45f + 0.55f * Math.abs((float) Math.sin(t * 17f));
            float growth = t < 1.2f ? 0.12f : 0.18f + (t - 1.2f) * 0.48f;
            glow.setAdditiveBlend(); glow.setColor(Color.WHITE);
            glow.setAlphaMult(alphaMult * Math.min(1f, flicker * (0.35f + t * 0.25f)));
            glow.setSize(planetSize * growth, planetSize * growth); glow.renderAtCenter(cx, cy);
            if (t > 1.2f) {
                glow.setAlphaMult(alphaMult * Math.min(1f, (t - 1.2f) * 0.65f));
                glow.setSize(planetSize * (0.5f + t * 0.55f), planetSize * (0.5f + t * 0.55f));
                glow.renderAtCenter(cx, cy);
            }
        }
    }
}
