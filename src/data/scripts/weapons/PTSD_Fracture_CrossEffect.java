package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

/** Polar rift projectile and local weapon-hijack cloud for PTSD_Fracture_Cross. */
public class PTSD_Fracture_CrossEffect implements OnFireEffectPlugin, OnHitEffectPlugin {
    private static final Color RIFT_FILL = new Color(5, 0, 12, 220);
    private static final Color RIFT_EDGE = new Color(168, 70, 255, 235);
    private static final Color HACK_COLOR = new Color(131, 127, 104, 190);
    private static final String CLOUD_MANAGER_KEY = "PTSD_Fracture_Cross_cloud_manager";

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (!(projectile instanceof MissileAPI) || engine == null) return;
        MissileAPI missile = (MissileAPI) projectile;
        missile.getSpriteAPI().setAlphaMult(0f);
        missile.setGlowRadius(0f);
        missile.setShineBrightness(0f);
        CrossState state = new CrossState(missile);
        missile.setMissileAI(new CrossAI(state));
        engine.addLayeredRenderingPlugin(new CrossRiftVisual(state));
    }

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point,
                      boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (engine == null || point == null) return;
        for (int i = 0; i < 3; i++) {
            Vector2f burst = PTSDTarotEffects.randomPoint(point, 20f);
            Vector2f velocity = Misc.getUnitVectorAtDegreeAngle((float)Math.random() * 360f);
            velocity.scale(12f + (float)Math.random() * 28f);
            PTSDTarotEffects.spawnRiftVisual(engine, burst, velocity,
                    7f + (float)Math.random() * 7f,
                    new Color(188, 75, 255, 220), false);
        }
        getManager(engine).spawnOrMerge(new Vector2f(point), projectile == null ? 100 : projectile.getOwner(),
                projectile == null ? null : projectile.getSource());
    }

    private static CloudManager getManager(CombatEngineAPI engine) {
        Object existing = engine.getCustomData().get(CLOUD_MANAGER_KEY);
        if (existing instanceof CloudManager) return (CloudManager) existing;
        CloudManager manager = new CloudManager();
        engine.getCustomData().put(CLOUD_MANAGER_KEY, manager);
        engine.addPlugin(manager);
        return manager;
    }

    private static final class CrossState {
        final MissileAPI missile;
        final PTSDPolarRiftRenderer.Shape shape;
        final PTSDPolarRiftRenderer.Shape indentedShape;
        CombatEntityAPI target;
        float stretch;

        CrossState(MissileAPI missile) {
            this.missile = missile;
            long seed = Double.doubleToLongBits(Math.random()) ^ missile.hashCode();
            shape = new PTSDPolarRiftRenderer.Shape(seed, 28, 4, 0.31f, 0.18f);
            indentedShape = new PTSDPolarRiftRenderer.Shape(seed ^ 0x5DEECE66DL,
                    24, 10, 0.11f, 0.48f);
        }
    }

    private static final class CrossAI implements MissileAIPlugin, GuidedMissileAI {
        private final CrossState state;
        private final IntervalUtil search = new IntervalUtil(0.08f, 0.14f);

        CrossAI(CrossState state) { this.state = state; }
        @Override public CombatEntityAPI getTarget() { return state.target; }
        @Override public void setTarget(CombatEntityAPI target) { state.target = valid(target) ? target : null; }

        @Override
        public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            MissileAPI missile = state.missile;
            if (engine == null || engine.isPaused() || missile.isFizzling()) return;
            search.advance(amount);
            if (!valid(state.target) || search.intervalElapsed()) acquire(engine);

            if (state.target == null) {
                state.stretch = Math.max(0f, state.stretch - amount * 2.4f);
                return;
            }
            float distance = Misc.getDistance(missile.getLocation(), state.target.getLocation());
            if (distance > 560f) {
                state.target = null;
                state.stretch = Math.max(0f, state.stretch - amount * 2.4f);
                return;
            }

            float desired = Misc.getAngleInDegrees(missile.getLocation(), state.target.getLocation());
            float turn = Misc.getClosestTurnDirection(missile.getFacing(), desired);
            if (turn > 0f) missile.giveCommand(ShipCommand.TURN_LEFT);
            else if (turn < 0f) missile.giveCommand(ShipCommand.TURN_RIGHT);
            missile.giveCommand(ShipCommand.ACCELERATE);
            missile.getEngineController().forceShowAccelerating();
            state.stretch = Math.min(1f, state.stretch + amount * (2.8f + (560f - distance) / 180f));
        }

        private void acquire(CombatEngineAPI engine) {
            CombatEntityAPI best = null;
            float bestDistance = 560f;
            for (ShipAPI ship : engine.getShips()) {
                if (!valid(ship)) continue;
                float distance = Misc.getDistance(state.missile.getLocation(), ship.getLocation());
                if (distance < bestDistance) { bestDistance = distance; best = ship; }
            }
            state.target = best;
        }

        private boolean valid(CombatEntityAPI entity) {
            if (!(entity instanceof ShipAPI) || entity.getOwner() == state.missile.getOwner()
                    || entity.getCollisionClass() == CollisionClass.NONE) return false;
            ShipAPI ship = (ShipAPI) entity;
            return ship.isAlive() && !ship.isHulk();
        }
    }

    /** One mesh follows each projectile; no recurring rift entities are spawned. */
    private static final class CrossRiftVisual extends BaseCombatLayeredRenderingPlugin {
        private final CrossState state;
        private float elapsed;
        private float cloudTimer;

        CrossRiftVisual(CrossState state) {
            this.state = state;
            cloudTimer = 0.2f + (float)Math.random() * 0.8f;
        }
        @Override public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        }
        @Override public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused()) return;
            elapsed += amount;
            cloudTimer -= amount;
            if (cloudTimer <= 0f) {
                cloudTimer = 0.2f + (float)Math.random() * 0.8f;
                Vector2f drift = new Vector2f(state.missile.getVelocity());
                drift.scale(0.10f);
                engine.addNegativeSwirlyNebulaParticle(new Vector2f(state.missile.getLocation()), drift,
                        18f + (float)Math.random() * 24f, 1.45f, 0.06f, 0.18f,
                        0.38f + (float)Math.random() * 0.24f, new Color(13, 0, 25, 165));
            }
            state.missile.getSpriteAPI().setAlphaMult(0f);
        }
        @Override public boolean isExpired() {
            CombatEngineAPI engine = Global.getCombatEngine();
            return state.missile == null || state.missile.isExpired() || state.missile.didDamage()
                    || engine == null || !engine.isEntityInPlay(state.missile);
        }
        @Override public float getRenderRadius() { return 155f; }
        @Override public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (isExpired()) return;
            float speedStretch = Math.min(1f, state.missile.getVelocity().length() / 1150f);
            float rawMorph = Math.min(1f, elapsed / 0.42f);
            float morph = rawMorph * rawMorph * (3f - 2f * rawMorph);
            float stretch = Math.max(state.stretch, speedStretch * 0.48f) * morph;
            float pulse = 1f + 0.08f * (float)Math.sin(elapsed * 8.5f + state.shape.phase);
            float radius = 2.5f + (20f * pulse - 2.5f) * morph;
            float facing = state.missile.getVelocity().lengthSquared() > 25f
                    ? Misc.getAngleInDegrees(new Vector2f(), state.missile.getVelocity())
                    : state.missile.getFacing();
            PTSDPolarRiftRenderer.render(state.shape, state.missile.getLocation(), facing,
                    radius, stretch, elapsed, viewport.getAlphaMult(), RIFT_FILL, RIFT_EDGE, morph);
            PTSDPolarRiftRenderer.render(state.indentedShape, state.missile.getLocation(), facing + 8f,
                    radius * 0.86f, stretch * 0.88f, elapsed * 1.23f,
                    viewport.getAlphaMult() * 0.72f * morph, new Color(1, 0, 5, 205),
                    new Color(228, 115, 255, 190), morph);
        }
    }

    /** Merges rapid-fire impacts and caps simultaneous clouds. */
    private static final class CloudManager extends BaseEveryFrameCombatPlugin {
        private static final int MAX_CLOUDS = 8;
        private final List<HackCloud> clouds = new ArrayList<HackCloud>();

        void spawnOrMerge(Vector2f point, int owner, ShipAPI source) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null) return;
            HackCloud nearest = null;
            float nearestDistance = 115f;
            for (HackCloud cloud : clouds) {
                if (cloud.owner != owner) continue;
                float distance = Misc.getDistance(point, cloud.location);
                if (distance < nearestDistance) { nearestDistance = distance; nearest = cloud; }
            }
            if (nearest != null) {
                nearest.refresh(point, source);
                return;
            }
            if (clouds.size() >= MAX_CLOUDS) {
                // Never recycle a hostile cloud for another owner: that would invert its allegiance.
                for (HackCloud cloud : clouds) {
                    if (cloud.owner == owner) {
                        cloud.refresh(point, source);
                        return;
                    }
                }
                return;
            }
            HackCloud cloud = new HackCloud(point, owner, source);
            clouds.add(cloud);
            engine.addLayeredRenderingPlugin(cloud);
        }

        @Override public void advance(float amount, List<InputEventAPI> events) {
            Iterator<HackCloud> iterator = clouds.iterator();
            while (iterator.hasNext()) if (iterator.next().isExpired()) iterator.remove();
        }
    }

    private static final class HackCloud extends BaseCombatLayeredRenderingPlugin {
        private final Vector2f location;
        private final int owner;
        private final SpriteAPI cloudSprite;
        private final IntervalUtil scan = new IntervalUtil(0.07f, 0.11f);
        private final IntervalUtil arc = new IntervalUtil(0.20f, 0.34f);
        private ShipAPI source;
        private float elapsed;
        private float lifetime = 4.2f;
        private float radius = 185f;

        HackCloud(Vector2f point, int owner, ShipAPI source) {
            location = new Vector2f(point);
            this.owner = owner;
            this.source = source;
            cloudSprite = Global.getSettings().getSprite("misc", "nebula_particles2");
            cloudSprite.setTexWidth(0.5f);
            cloudSprite.setTexHeight(0.5f);
            cloudSprite.setTexX((float)Math.floor(Math.random() * 2f) * 0.5f);
            cloudSprite.setTexY((float)Math.floor(Math.random() * 2f) * 0.5f);
        }

        void refresh(Vector2f point, ShipAPI source) {
            location.x = location.x * 0.72f + point.x * 0.28f;
            location.y = location.y * 0.72f + point.y * 0.28f;
            if (source != null) this.source = source;
            elapsed = Math.min(elapsed, 0.7f);
            lifetime = Math.min(5.8f, lifetime + 0.45f);
            radius = Math.min(225f, radius + 7f);
        }

        @Override public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.BELOW_SHIPS_LAYER,
                    CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        }

        @Override public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused()) return;
            elapsed += amount;
            scan.advance(amount);
            arc.advance(amount);
            if (!scan.intervalElapsed()) return;

            WeaponAPI arcTarget = null;
            for (ShipAPI ship : engine.getShips()) {
                if (ship.getOwner() == owner || !ship.isAlive() || ship.isHulk()) continue;
                if (Misc.getDistance(location, ship.getLocation()) > radius + ship.getCollisionRadius()) continue;
                for (WeaponAPI weapon : ship.getAllWeapons()) {
                    if (weapon.isDecorative() || weapon.isPermanentlyDisabled()
                            || Misc.getDistance(location, weapon.getLocation()) > radius) continue;
                    weapon.setForceNoFireOneFrame(true);
                    weapon.stopFiring();
                    weapon.setRemainingCooldownTo(Math.max(weapon.getCooldownRemaining(), 0.18f));
                    ship.setJitter(HACK_COLOR, HACK_COLOR, 0.18f, 2, 0f, 3f);
                    if (arcTarget == null || Math.random() < 0.25) arcTarget = weapon;
                }
            }
            if (arcTarget != null && arc.intervalElapsed()) {
                EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
                params.segmentLengthMult = 7f;
                params.zigZagReductionFactor = 0.2f;
                params.fadeOutDist = 30f;
                params.glowSizeMult = 1.8f;
                params.flickerRateMult = 0.7f;
                engine.spawnEmpArcVisual(location, source, arcTarget.getLocation(), arcTarget.getShip(),
                        9f, new Color(126, 52, 225, 210), new Color(238, 210, 255, 230), params);
            }
        }

        @Override public boolean isExpired() { return elapsed >= lifetime; }
        @Override public float getRenderRadius() { return radius + 70f; }

        @Override public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            float fadeIn = Math.min(1f, elapsed / 0.22f);
            float fadeOut = Math.min(1f, (lifetime - elapsed) / 0.7f);
            float alpha = viewport.getAlphaMult() * fadeIn * Math.max(0f, fadeOut);
            float pulse = 0.5f + 0.5f * (float)Math.sin(elapsed * 3.8f);
            if (layer == CombatEngineLayers.BELOW_SHIPS_LAYER) {
                renderCloud(radius * (1.25f + pulse * 0.08f), elapsed * 19f, alpha * 0.23f,
                        new Color(8, 0, 17));
                renderCloud(radius * (0.82f - pulse * 0.07f), -elapsed * 31f + 70f,
                        alpha * 0.29f, new Color(54, 8, 83));
            } else {
                renderCloud(radius * 0.38f, elapsed * 43f, alpha * 0.13f,
                        new Color(151, 68, 220));
            }
        }

        private void renderCloud(float size, float angle, float alpha, Color color) {
            cloudSprite.setNormalBlend();
            cloudSprite.setSize(size, size);
            cloudSprite.setAngle(angle);
            cloudSprite.setAlphaMult(alpha);
            cloudSprite.setColor(color);
            cloudSprite.renderAtCenter(location.x, location.y);
        }
    }
}