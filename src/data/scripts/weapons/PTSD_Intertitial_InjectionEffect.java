package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.EnumSet;

/** Smaller, rift-colored Churning Locus analogue implemented as a guided projectile. */
public class PTSD_Intertitial_InjectionEffect implements OnFireEffectPlugin, OnHitEffectPlugin {
    private static final Color RIFT = new Color(132, 62, 235, 225);
    private static final Color RIFT_CORE = new Color(232, 205, 255, 245);
    private static final Color RIFT_DARK = new Color(12, 0, 24, 210);
    private static final IntervalUtil LinkPulse = new IntervalUtil(0.75f, 1.78f);

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (!(projectile instanceof MissileAPI) || engine == null) return;
        MissileAPI missile = (MissileAPI) projectile;
        missile.getSpriteAPI().setAlphaMult(0f);
        missile.setGlowRadius(0f);
        missile.setShineBrightness(0f);
        missile.setRenderGlowAbove(false);
        missile.setEmpResistance(4);
        missile.setMissileAI(new IntertitialVortexAI(missile));
        engine.addLayeredRenderingPlugin(new IntertitialVortexVisual(missile,weapon));
        PTSDTarotEffects.spawnInversionField(engine, missile.getLocation(), missile.getVelocity(), 50f, RIFT);

    }

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point,
                      boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (engine == null || point == null) return;
        Vector2f velocity = projectile == null ? new Vector2f() : new Vector2f(projectile.getVelocity());
        velocity.scale(0.12f);
        PTSDTarotEffects.spawnRiftVisual(engine, point, velocity, MathUtils.getRandomNumberInRange(5f,30f), RIFT, false);
        PTSDTarotEffects.spawnInversionField(engine, point, velocity,  MathUtils.getRandomNumberInRange(10f,40f), new Color(245, 70, 111, 210));
        spawnCompactImpactMist(engine, point, velocity);
    }

    /** Dedicated impact mist so its scale can be tuned without affecting flight or inversion visuals. */
    private static void spawnCompactImpactMist(CombatEngineAPI engine, Vector2f point, Vector2f velocity) {
        for (int i = 0; i < 4; i++) {
            Vector2f loc = PTSDTarotEffects.randomPoint(point, 18f);
            Vector2f drift = new Vector2f(velocity);
            drift.translate(((float)Math.random() - 0.5f) * 24f, ((float)Math.random() - 0.5f) * 24f);
            engine.addNegativeSwirlyNebulaParticle(loc, drift, 28f + (float)Math.random() * 24f,
                    1.25f, 0.06f, 0.18f, 0.48f, new Color(14, 0, 24, 160));
        }
    }

    /** Flare-compatible tracking with the loose, circling approach of the original vortex. */
    private static final class IntertitialVortexAI implements MissileAIPlugin, GuidedMissileAI {
        private final MissileAPI missile;
        private final IntervalUtil targetUpdate = new IntervalUtil(0.14f, 0.24f);
        private CombatEntityAPI target;
        private float elapsed;
        private final float weave = (float)Math.random() * 6.28318f;

        IntertitialVortexAI(MissileAPI missile) {
            this.missile = missile;
            reacquire();
        }

        @Override public CombatEntityAPI getTarget() { return target; }
        @Override public void setTarget(CombatEntityAPI target) { this.target = valid(target) ? target : null; }

        @Override
        public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused() || missile.isFizzling()) return;
            elapsed += amount;
            targetUpdate.advance(amount);
            if (!valid(target) || targetUpdate.intervalElapsed()) reacquire();

            missile.giveCommand(ShipCommand.ACCELERATE);
            missile.getEngineController().forceShowAccelerating();
            if (target == null) {
                missile.giveCommand(Math.sin(elapsed * 1.7f + weave) > 0f
                        ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT);
                return;
            }

            Vector2f aim = new Vector2f(target.getLocation());
            float distance = Misc.getDistance(missile.getLocation(), target.getLocation());
            Vector2f lead = new Vector2f(target.getVelocity());
            lead.scale(Math.min(0.55f, distance / Math.max(300f, missile.getMaxSpeed() * 2.2f)));
            Vector2f.add(aim, lead, aim);
            float desired = Misc.getAngleInDegrees(missile.getLocation(), aim)
                    + (float)Math.sin(elapsed * 3.1f + weave) * Math.min(11f, distance / 55f);
            float turn = Misc.getClosestTurnDirection(missile.getFacing(), desired);
            if (turn > 0f) missile.giveCommand(ShipCommand.TURN_LEFT);
            else if (turn < 0f) missile.giveCommand(ShipCommand.TURN_RIGHT);
        }

        private void reacquire() {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null) return;
            ShipAPI source = missile.getSource();
            if (source != null && valid(source.getShipTarget())) {
                target = source.getShipTarget();
                return;
            }
            CombatEntityAPI best = null;
            float bestScore = Float.MAX_VALUE;
            for (ShipAPI ship : engine.getShips()) {
                if (!valid(ship)) continue;
                float distance = Misc.getDistance(missile.getLocation(), ship.getLocation());
                if (distance > 1450f) continue;
                float score = distance + (ship.isFighter() ? 260f : 0f);
                if (score < bestScore) { bestScore = score; best = ship; }
            }
            target = best;
        }

        private boolean valid(CombatEntityAPI entity) {
            if (!(entity instanceof ShipAPI) || entity.getOwner() == missile.getOwner()
                    || entity.getCollisionClass() == CollisionClass.NONE) return false;
            ShipAPI ship = (ShipAPI) entity;
            return ship.isAlive() && !ship.isHulk();
        }
    }

    /** Lower cloud body, upper moving rift core, and recurring local pixel inversion. */
    private static final class IntertitialVortexVisual extends BaseCombatLayeredRenderingPlugin {
        private final MissileAPI missile;
        private final WeaponAPI weapon;
        private final SpriteAPI cloud;
        private final SpriteAPI rift;
        private float elapsed;
        private float mistTimer;
        private float inversionTimer;

        IntertitialVortexVisual(MissileAPI missile, WeaponAPI weapon) {
            this.missile = missile;
            this.weapon = weapon;
            this.cloud = Global.getSettings().getSprite("misc", "nebula_particles2");
            this.cloud.setTexWidth(0.5f);
            this.cloud.setTexHeight(0.5f);
            this.cloud.setTexX((float)Math.floor(Math.random() * 2f) * 0.5f);
            this.cloud.setTexY((float)Math.floor(Math.random() * 2f) * 0.5f);
            this.rift = Global.getSettings().getSprite("graphics/fx/radial_EX.png");
            this.mistTimer = (float)Math.random() * 0.06f;
            this.inversionTimer = (float)Math.random() * 0.09f;
        }

        @Override public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.BELOW_SHIPS_LAYER,
                    CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        }

        @Override public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused() || isExpired()) return;
            elapsed += amount;
            mistTimer -= amount;
            inversionTimer -= amount;
            if (mistTimer <= 0f) {
                mistTimer = 0.055f + (float)Math.random() * 0.04f;
                Vector2f point = new Vector2f(missile.getLocation());
                Vector2f drift = new Vector2f(missile.getVelocity());
                drift.scale(0.09f);
                engine.addNegativeSwirlyNebulaParticle(point, drift,
                        22f + (float)Math.random() * 46f, 1.65f, 0.07f, 0.24f,
                        0.62f + (float)Math.random() * 0.22f, RIFT_DARK);
            }
            if (inversionTimer <= 0f) {
                inversionTimer = 0.11f + (float)Math.random() * 0.055f;
                Vector2f drift = new Vector2f(missile.getVelocity());
                drift.scale(0.1f);
                PTSDTarotEffects.spawnInversionField(engine, missile.getLocation(), drift,
                        32f + (float)Math.random() * 14f, new Color(180, 60, 250, 170));
            }
            missile.getSpriteAPI().setAlphaMult(0f);
            LinkPulse.advance(amount);
            if (LinkPulse.intervalElapsed()) {
                EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
                params.segmentLengthMult = 7f;
                params.zigZagReductionFactor = 0.18f;
                params.fadeOutDist = 22f;
                params.minFadeOutMult = 8f;
                params.flickerRateMult = 0.9f;
                params.glowSizeMult = 3.2f;
                params.glowAlphaMult = 0.55f;
                params.movementDurOverride = 3000f;
                params.glowColorOverride = new Color(245, 70, 111, 210);
                EmpArcEntityAPI arc = engine.spawnEmpArcVisual(weapon.getLocation(), weapon.getShip(), missile.getLocation(), missile, 12f,
                        new Color(126, 52, 225, 245), new Color(238, 210, 255, 255), params);
                arc.setRenderGlowAtStart(false);
                arc.setRenderGlowAtEnd(true);
            }
        }

        @Override public boolean isExpired() {
            CombatEngineAPI engine = Global.getCombatEngine();
            return missile == null || missile.isExpired() || missile.didDamage()
                    || engine == null || !engine.isEntityInPlay(missile);
        }

        @Override public float getRenderRadius() { return 230f; }

        @Override public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (isExpired()) return;
            Vector2f point = missile.getLocation();
            float pulse = 0.5f + 0.5f * (float)Math.sin(elapsed * 4.2f);
            float alpha = viewport.getAlphaMult();
            if (layer == CombatEngineLayers.BELOW_SHIPS_LAYER) {
                renderCloud(point, 85f + pulse * 22f, elapsed * 24f, alpha * 0.17f, new Color(255, 0, 0));
                renderCloud(point, 42f - pulse * 14f, -elapsed * 39f + 60f, alpha * 0.23f, new Color(18, 0, 31));
                renderCloud(point, 96f + pulse * 12f, elapsed * 57f + 145f, alpha * 0.28f, new Color(52, 5, 76));
                return;
            }
            rift.setNormalBlend();
            float s = 72;
            rift.setSize(s + pulse * 15f, s + pulse * 15f);
            rift.setAngle(elapsed * 115f);
            rift.setColor(RIFT);
            rift.setAlphaMult(alpha * 0.72f);
            rift.renderAtCenter(point.x, point.y);
            rift.setAdditiveBlend();
            rift.setSize(s/2 + pulse * 11f, s/2 + pulse * 11f);
            rift.setAngle(-elapsed * 168f + 90f);
            rift.setColor(RIFT_CORE);
            rift.setAlphaMult(alpha * 0.78f);
            rift.renderAtCenter(point.x, point.y);
        }

        private void renderCloud(Vector2f point, float size, float angle, float alpha, Color color) {
            cloud.setNormalBlend();
            cloud.setSize(size, size);
            cloud.setAngle(angle);
            cloud.setAlphaMult(alpha);
            cloud.setColor(color);
            cloud.renderAtCenter(point.x, point.y);
        }
    }
}