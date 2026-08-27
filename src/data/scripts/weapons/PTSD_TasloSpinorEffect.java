package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI.EmpArcParams;
import com.fs.starfarer.api.impl.combat.threat.EnergyLashSystemScript.DelayedCombatActionPlugin;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual.NEParams;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicAnim;

import java.awt.Color;
import java.util.EnumSet;
import java.util.WeakHashMap;

/** Passive 1000-range rift storm. The weapon is intentionally unable to fire a projectile. */
public class PTSD_TasloSpinorEffect implements EveryFrameWeaponEffectPlugin {
    private final WeakHashMap<WeaponAPI, SpinorState> states = new WeakHashMap<WeaponAPI, SpinorState>();
    private float enemyNum;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI source = weapon.getShip();
        weapon.setForceDisabled(true);
        if (engine.isPaused() || source == null || !source.isAlive() || source.isHulk()) return;

        SpinorState state = states.get(weapon);
        if (state == null) {
            state = new SpinorState();
            states.put(weapon, state);
            engine.addLayeredRenderingPlugin(new SpinorFieldVisual(weapon));
        }

        state.fieldPulse.advance(amount);
        if (state.fieldPulse.intervalElapsed()) {
            Vector2f point = PTSDTarotEffects.randomPoint(weapon.getLocation(), 500f);
            PTSDTarotEffects.spawnRiftVisual(engine, point, source.getVelocity(),
                    20f + (float) Math.random() * 22f, new Color(58, 4, 20, 82), true);
        }

        state.weaponPulse.advance(amount);
        if (state.weaponPulse.intervalElapsed()) {
            Vector2f point = PTSDTarotEffects.randomPoint(weapon.getLocation(), 5f);
            PTSDTarotEffects.spawnRiftVisual(engine, point, source.getVelocity(),
                    12f + (float) Math.random() * 10f, new Color(68, 24, 145, 72), false);
        }

        state.upperPulse.advance(amount);
        if (state.upperPulse.intervalElapsed()) {
            Vector2f point = PTSDTarotEffects.randomPoint(weapon.getLocation(), weapon.getShip().getCollisionRadius()*1.15f);
            PTSDTarotEffects.spawnRiftVisual(engine, point, source.getVelocity(),
                    9f + (float) Math.random() * 75f, new Color(85, 18, 112, 72), false);
        }

        enemyNum = Math.min((float) AIUtils.getNearbyEnemies(weapon.getShip(),1000f).size()+2,5);
        float theAmout = amount * (enemyNum/2);
        state.stormPulse.advance(theAmout);
        if (!state.stormPulse.intervalElapsed()) return;
        CombatEntityAPI target = PTSDTarotEffects.pickStormTarget(engine, source, weapon.getLocation());
        if (target == null) return;
        final Vector2f stormPoint = PTSDTarotEffects.randomPoint(target.getLocation(),
                PTSDTarotEffects.SPINOR_SPAWN_RADIUS);
        Vector2f muzzle = getMuzzlePoint(weapon);
        spawnMuzzleBurst(engine, source, muzzle);
        float delay = spawnShroudArc(engine, source, muzzle, stormPoint);
        engine.addPlugin(new DelayedCombatActionPlugin(delay, new Runnable() {
            @Override public void run() {
                CombatEngineAPI current = Global.getCombatEngine();
                if (current != null) PTSDTarotEffects.spawnStorm(current, source, stormPoint);
            }
        }));

        if (Global.getSettings().isDevMode()) {
            float now = engine.getTotalElapsedTime(false);
            if (now - state.lastDevText >= 1f) {
                state.lastDevText = now;
                engine.addFloatingText(stormPoint, "[DEV] 裂隙风暴 / 距离 " +
                        Math.round(com.fs.starfarer.api.util.Misc.getDistance(weapon.getLocation(),
                                target.getLocation())), 12f, Color.YELLOW, source, 0.2f, 0.1f);
            }
        }
    }

    private static Vector2f getMuzzlePoint(WeaponAPI weapon) {
        try { return new Vector2f(weapon.getFirePoint(0)); }
        catch (Throwable ignored) { return new Vector2f(weapon.getLocation()); }
    }

    private static void spawnMuzzleBurst(CombatEngineAPI engine, ShipAPI source, Vector2f muzzle) {
        Vector2f velocity = new Vector2f(source.getVelocity());
        engine.addHitParticle(muzzle, velocity, 118f, 0.95f, 0.26f, new Color(154, 74, 255, 235));
        engine.addHitParticle(muzzle, velocity, 42f, 1f, 0.12f, new Color(245, 225, 255, 255));
        for (int i = 0; i < 7; i++) {
            Vector2f spark = com.fs.starfarer.api.util.Misc.getUnitVectorAtDegreeAngle((float)Math.random() * 360f);
            spark.scale(80f + (float)Math.random() * 190f);
            engine.addSmoothParticle(muzzle, spark, 5f + (float)Math.random() * 8f,
                    0.9f, 0.18f + (float)Math.random() * 0.18f, new Color(175, 105, 255, 220));
        }
        NEParams params = new NEParams();
        params.radius = 84f;
        params.thickness = 72f;
        params.fadeIn = 0.035f;
        params.fadeOut = 0.38f;
        params.noiseMag = 1.25f;
        params.noisePeriod = 0.06f;
        params.color = new Color(128, 58, 215, 155);
        params.blackColor = new Color(2, 0, 6, 230);
        params.invertForDarkening = new Color(146, 78, 225, 170);
        params.underglow = new Color(96, 28, 165, 95);
        NegativeExplosionVisual visual = new NegativeExplosionVisual(params);
        CombatEntityAPI entity = engine.addLayeredRenderingPlugin(visual);
        entity.getLocation().set(muzzle);
        entity.getVelocity().set(source.getVelocity());
    }

    private static float spawnShroudArc(CombatEngineAPI engine, ShipAPI source,
                                        Vector2f from, Vector2f to) {
        float distance = com.fs.starfarer.api.util.Misc.getDistance(from, to);
        EmpArcParams params = new EmpArcParams();
        params.segmentLengthMult = 7f;
        params.zigZagReductionFactor = 0.18f;
        params.fadeOutDist = 45f;
        params.minFadeOutMult = 8f;
        params.flickerRateMult = 0.35f;
        params.glowSizeMult = 2.2f;
        params.movementDurOverride = Math.max(0.08f, distance / 2400f);
        EmpArcEntityAPI arc = engine.spawnEmpArcVisual(from, source, to, null, 38f,
                new Color(126, 52, 225, 245), new Color(238, 210, 255, 255), params);
        arc.setCoreWidthOverride(16f);
        arc.setRenderGlowAtStart(false);
        arc.setFadedOutAtStart(true);
        arc.setSingleFlickerMode(true);
        Global.getSoundPlayer().playSound("rift_lightning_fire", 1.15f, 0.65f, from, source.getVelocity());
        return params.movementDurOverride * 0.82f;
    }

    private static final class SpinorState {
        final IntervalUtil fieldPulse = new IntervalUtil(0.75f, 1.25f);
        final IntervalUtil upperPulse = new IntervalUtil(0.30f, 1.85f);
        final IntervalUtil weaponPulse = new IntervalUtil(0.50f, 1.5f);
        final IntervalUtil stormPulse = new IntervalUtil(1.45f, 2.25f);
        float lastDevText = -100f;
    }

    /** Persistent black, twisting cloud rendered below ships; one instance exists per mounted Spinor. */
    private static final class SpinorFieldVisual extends BaseCombatLayeredRenderingPlugin {
        private final WeaponAPI weapon;
        private final SpriteAPI cloud;
        private float elapsed;

        SpinorFieldVisual(WeaponAPI weapon) {
            this.weapon = weapon;
            this.cloud = Global.getSettings().getSprite("misc", "nebula_particles2");
            this.cloud.setTexWidth(0.5f);
            this.cloud.setTexHeight(0.5f);
            this.cloud.setTexX((float) Math.floor(Math.random() * 2f) * 0.5f);
            this.cloud.setTexY((float) Math.floor(Math.random() * 2f) * 0.5f);
            this.cloud.setNormalBlend();
        }

        @Override public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.BELOW_SHIPS_LAYER);
        }

        @Override public void advance(float amount) {
            if (Global.getCombatEngine().isPaused()) return;
            elapsed += amount;
            if (entity != null && weapon != null) entity.getLocation().set(weapon.getLocation());
        }

        @Override public boolean isExpired() {
            CombatEngineAPI engine = Global.getCombatEngine();
            ShipAPI ship = weapon == null ? null : weapon.getShip();
            return engine == null || ship == null || !ship.isAlive() || ship.isHulk()
                    || !engine.isEntityInPlay(ship);
        }

        @Override public float getRenderRadius() { return 700f; }

        @Override public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (isExpired()) return;
            Vector2f point = weapon.getLocation();
            float pulse = 0.5f + 0.5f * (float) Math.sin(elapsed * 2.1f);
            float alpha = viewport.getAlphaMult();
            renderCloud(point, 1180f + pulse * 70f, elapsed * 4.5f, alpha * 0.105f,
                    new Color(0, 0, 0));
            renderCloud(point, 860f - pulse * 55f, -elapsed * 7f + 70f, alpha * 0.14f,
                    new Color(5, 0, 10));
            renderCloud(point, 560f + pulse * 42f, elapsed * 11f + 145f, alpha * 0.18f,
                    new Color(15, 1, 27));
            renderCloud(point, 320f - pulse * 24f, -elapsed * 16f + 215f, alpha * 0.20f,
                    new Color(25, 5, 42));
        }

        private void renderCloud(Vector2f point, float size, float angle, float alpha, Color color) {
            cloud.setSize(size, size);
            cloud.setAngle(angle);
            cloud.setAlphaMult(alpha);
            cloud.setColor(color);
            cloud.renderAtCenter(point.x, point.y);
        }
    }
}