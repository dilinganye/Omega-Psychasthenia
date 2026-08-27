package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect.RoilingSwarmParams;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.EnumSet;
import java.util.Objects;

/**
 * Mixed Tarot projectile rendering with adaptive Shroud-limb throttling for high-rate weapons.
 */
public class PTSD_TarotProjectileEffect implements OnFireEffectPlugin {
    private static final String SWARM_MARKER = "PTSD_tarot_projectile_swarm";
    private static final int MAX_ACTIVE_SWARMS = 42;

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (projectile == null || weapon == null || engine == null) return;
        String projectileId = projectile.getProjectileSpecId();
        Style style = Style.forProjectile(projectileId);
        if (isVolatilizer(projectileId)) {
            engine.addLayeredRenderingPlugin(new VolatilizerNegativeMist(projectile));
        }
        if (shouldAddPathCloud(projectileId, weapon)) {
            engine.addLayeredRenderingPlugin(new TarotPathCloud(projectile, style));
        }
        if ("PTSD_Aftershockor_shot".equals(projectileId)) {
            engine.addPlugin(new AftershockSpaceDistortion(projectile));
            return; // the physical projectile is intentionally invisible; only space distortion is shown
        }
        boolean limbSwarm = Math.random() < getLimbChance(weapon, style);
        if (limbSwarm) {
            createShroudedLimbGroup(projectile, style);
        } else {
            engine.addLayeredRenderingPlugin(new TarotProjectileVisual(projectile, style));
        }
    }

    private static boolean isAftershock(String id) {
        return "PTSD_Aftershockor_shot".equals(id);
    }
    private static boolean isVolatilizer(String id) {
        return "PTSD_Fracture_Volatilizatior_shot".equals(id)
                || "PTSD_Facture_Volatilizatior_shot".equals(id);
    }
    private static boolean shouldAddPathCloud(String id, WeaponAPI weapon) {
        if ("PTSD_Fracture_Calculus_shot".equals(id)) return false;
        boolean supported = "PTSD_Aftershockor_shot".equals(id)
                || "PTSD_TasloLauncher_shot".equals(id)
                || isVolatilizer(id);
        if (!supported) return false;
        // The Launcher fires at 0.01s intervals; sampling still produces a continuous field
        // without allocating one layered renderer for every single micro-projectile.
        if ("PTSD_TasloLauncher_shot".equals(id)) return Math.random() < 0.625f;
        return true;
    }
    private static float getLimbChance(WeaponAPI weapon, Style style) {
        int active = 0;
        for (RoilingSwarmEffect swarm : RoilingSwarmEffect.getShipMap().values()) {
            // Do not call isExpired() while iterating: the original implementation removes itself from this map.
            if (SWARM_MARKER.equals(swarm.custom1)) active++;
        }
        if (active >= MAX_ACTIVE_SWARMS) return 0f;
        float cooldown = Math.max(0.005f, weapon.getCooldown());
        // At 4 shots/sec and below use the full style chance. Very high rates approach a small sample.
        float rateFactor = Math.max(0.035f, Math.min(1f, cooldown / 0.25f));
        float budgetFactor = Math.max(0f, (MAX_ACTIVE_SWARMS - active) / (float) MAX_ACTIVE_SWARMS);
        return style.swarmChance * rateFactor * budgetFactor;
    }

    /** Uses the original Shroud/Dweller roiling member implementation and sprite atlas. */
    private static void createShroudedLimbGroup(DamagingProjectileAPI projectile, Style style) {
        RoilingSwarmParams params = new RoilingSwarmParams();
        // The vanilla atlas is registered under the "dweller" category in 0.98a.
        // Using "misc" reaches RoilingSwarmEffect's generic default but crashes when
        // "misc", "nebula_particles2"
        // "dweller", "dweller_pieces"
        params.spriteCat = "dweller";
        params.spriteKey = "dweller_pieces";
        params.despawnSound = null;
        params.initialMembers = style.members;
        params.baseMembersToMaintain = style.members;
        params.withInitialMembers = true;
        params.withRespawn = false;
        params.removeMembersAboveMaintainLevel = true;
        params.baseDur = 100000f;
        params.durRange = 0f;
        params.baseSpriteSize = style.spriteSize;
        params.baseScale = 0.45f;
        params.scaleRange = 0.55f;
        params.minOffset = 2f;
        params.maxOffset = style.swarmRadius;
        params.maxSpeed = Math.max(650f, projectile.getVelocity().length() + 80f);
        params.outspeedAttachedEntityBy = 80f;
        params.baseFriction = 180f;
        params.frictionRange = 650f;
        params.baseSpringConstant = 75f;
        params.springConstantNegativeRange = 25f;
        params.baseSpringFreeLength = 4f;
        params.springFreeLengthRange = 10f;
        params.offsetRotationDegreesPerSecond = style.rotation;
        params.swarmLeadsByFractionOfVelocity = 0.015f;
        params.alphaMult = 0.9f;
        params.alphaMultBase = 0.9f;
        params.color = style.limbColor;
        params.flashProbability = 0.18f;
        params.flashFrequency = 2f;
        params.flashRateMult = 1.5f;
        params.flashRadius = style.spriteSize * 2.8f;
        params.flashCoreRadiusMult = 0.2f;
        params.flashFringeColor = style.glowColor;
        params.flashCoreColor = Color.WHITE;
        params.minFadeoutTime = 0.16f;
        params.maxFadeoutTime = 0.32f;
        params.minDespawnTime = 0.12f;
        params.maxDespawnTime = 0.28f;
        RoilingSwarmEffect swarm = new RoilingSwarmEffect(projectile, params);
        swarm.custom1 = SWARM_MARKER;
        swarm.custom2 = projectile.getProjectileSpecId();
    }

    private static final class Style {
        final Color bodyColor;
        final Color glowColor;
        final Color darkColor;
        final Color limbColor;
        final float swarmChance;
        final int members;
        final float spriteSize;
        final float swarmRadius;
        final float rotation;
        final float trailInterval;

        Style(Color bodyColor, Color glowColor, Color darkColor, Color limbColor,
              float swarmChance, int members, float spriteSize, float swarmRadius,
              float rotation, float trailInterval) {
            this.bodyColor = bodyColor;
            this.glowColor = glowColor;
            this.darkColor = darkColor;
            this.limbColor = limbColor;
            this.swarmChance = swarmChance;
            this.members = members;
            this.spriteSize = spriteSize;
            this.swarmRadius = swarmRadius;
            this.rotation = rotation;
            this.trailInterval = trailInterval;
        }

        static Style forProjectile(String id) {
            if ("PTSD_TasloLauncher_shot".equals(id)) {
                return new Style(new Color(208, 155, 255), new Color(111, 69, 239),
                        new Color(34, 8, 58, 185), new Color(118, 89, 192),
                        0.74f, 2, 20f, 20f, 121f, 0.057f);
            }
            if ("PTSD_Aftershockor_shot".equals(id)) {
                return new Style(new Color(145, 120, 255), new Color(154, 30, 255),
                        new Color(13, 0, 38, 170), new Color(152, 80, 220),
                        1f, 9, 58f, 28f, 125f, 0.08f);
            }
            if ("PTSD_Fracture_Calculus_shot".equals(id)) {
                return new Style(new Color(220, 180, 255), new Color(150, 70, 255),
                        new Color(18, 0, 38, 185), new Color(160, 105, 210),
                        0.34f, 7, 20f, 18f, -170f, 0.07f);
            }
            if ("PTSD_Fracture_Volatilizatior_shot".equals(id)
                    || "PTSD_Facture_Volatilizatior_shot".equals(id)) {
                return new Style(new Color(255, 125, 220), new Color(255, 45, 165),
                        new Color(42, 0, 24, 195), new Color(205, 75, 150),
                        1f, 12, 28f, 28f, 220f, 0.045f);
            }
            return new Style(new Color(195, 165, 255), new Color(115, 80, 255),
                    new Color(10, 0, 30, 175), new Color(125, 105, 205),
                    0.3f, 4, 20f, 50f, 190f, 0.09f);
        }
    }

    /** Moving, overlapping inversion pulses; the underlying Aftershock projectile remains invisible. */
    private static final class AftershockSpaceDistortion extends BaseEveryFrameCombatPlugin {
        private final DamagingProjectileAPI projectile;
        private float pulse;

        AftershockSpaceDistortion(DamagingProjectileAPI projectile) {
            this.projectile = projectile;
            this.pulse = (float)Math.random() * 0.05f;
        }

        @Override public void advance(float amount, java.util.List<com.fs.starfarer.api.input.InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null) return;
            if (projectile == null || projectile.isExpired() || projectile.didDamage()
                    || !engine.isEntityInPlay(projectile)) {
                engine.removePlugin(this);
                return;
            }
            if (engine.isPaused()) return;
            pulse -= amount;
            if (pulse > 0f) return;
            pulse = 0.085f + (float)Math.random() * 0.035f;
            Vector2f velocity = new Vector2f(projectile.getVelocity());
            velocity.scale(0.12f);
            PTSDTarotEffects.spawnInversionField(engine, projectile.getLocation(), velocity,
                    32f + (float)Math.random() * 36f, new Color(118, 48, 225, 185));
            Color color = new Color(77, 30, 173, 175);
            Color coreColor = Color.white;
            ShipAPI target = AIUtils.getNearestEnemy(projectile);
            if (target == null) return;
            float distance = MathUtils.getDistance(target,projectile.getLocation());
            if(distance<100f & Math.random()<=0.6f){
                EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
                params.segmentLengthMult = 7f;
                params.zigZagReductionFactor = 0.18f;
                params.fadeOutDist = 45f;
                params.minFadeOutMult = 2f;
                params.flickerRateMult = 0.6f;
                params.glowSizeMult = 2.2f;
                //params.movementDurOverride = Math.max(0.08f, distance/100f);
                Global.getCombatEngine().spawnEmpArc(projectile.getSource(),projectile.getLocation(),target,target,DamageType.ENERGY,55f,90f,100f, null,
                        MathUtils.getRandomNumberInRange(30f,53f),
                        color,
                        coreColor,
                        params);
            }
        }
    }
    /** Spinor-like cloud core plus a persistent fading trail, rendered for selected Tarot shots. */
    private static final class TarotPathCloud extends BaseCombatLayeredRenderingPlugin {
        private final DamagingProjectileAPI projectile;
        private final Style style;
        private final SpriteAPI cloud;
        private float emitTimer;
        private float spin;

        TarotPathCloud(DamagingProjectileAPI projectile, Style style) {
            this.projectile = projectile;
            this.style = style;
            this.cloud = Global.getSettings().getSprite("misc", "nebula_particles2");
            this.cloud.setTexWidth(0.5f);
            this.cloud.setTexHeight(0.5f);
            this.cloud.setTexX((float)Math.floor(Math.random() * 2f) * 0.5f);
            this.cloud.setTexY((float)Math.floor(Math.random() * 2f) * 0.5f);
            this.cloud.setNormalBlend();
            this.emitTimer = (float)Math.random() * 0.07f;
        }

        @Override public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.BELOW_SHIPS_LAYER);
        }

        @Override public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused() || isExpired()) return;
            spin += amount * style.rotation * 0.42f;
            emitTimer -= amount;
            if (emitTimer > 0f) return;
            emitTimer = 0.065f + (float)Math.random() * 0.045f;
            Vector2f point = new Vector2f(projectile.getLocation());
            Vector2f backwards = new Vector2f(projectile.getVelocity());
            backwards.scale(-0.018f);
            Vector2f.add(point, backwards, point);
            Vector2f drift = new Vector2f(projectile.getVelocity());
            drift.scale(0.07f);
            float size = Math.max(28f, style.spriteSize * (0.72f + (float)Math.random() * 0.42f));
            engine.addNegativeSwirlyNebulaParticle(point, drift, size, 1.55f,
                    0.08f, 0.28f, 0.58f + (float)Math.random() * 0.24f, style.darkColor);
            if (Math.random() < 0.38f) {
                engine.addSmoothParticle(point, drift, size * 0.18f, 0.45f, 0.28f, style.glowColor);
            }
        }

        @Override public boolean isExpired() {
            CombatEngineAPI engine = Global.getCombatEngine();
            return projectile == null || projectile.isExpired() || projectile.didDamage()
                    || engine == null || !engine.isEntityInPlay(projectile);
        }

        @Override public float getRenderRadius() { return Math.max(110f, style.spriteSize * 2.5f); }

        @Override public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (isExpired()) return;
            Vector2f point = projectile.getLocation();
            float base = Math.max(42f, style.spriteSize * 1.45f);
            float alpha = viewport.getAlphaMult();
            renderCloud(point, base * 1.45f, spin, alpha * 0.18f, style.darkColor);
            renderCloud(point, base, -spin * 1.4f + 95f, alpha * 0.24f,
                    new Color(Math.max(0, style.darkColor.getRed() / 2), 0,
                            Math.max(8, style.darkColor.getBlue() / 2), 205));
        }

        private void renderCloud(Vector2f point, float size, float angle, float alpha, Color color) {
            cloud.setSize(size, size);
            cloud.setAngle(angle);
            cloud.setAlphaMult(alpha);
            cloud.setColor(color);
            cloud.renderAtCenter(point.x, point.y);
        }
    }
    /** Continuous, projectile-bound negative mist for both corrected and legacy Volatilizer IDs. */
    private static final class VolatilizerNegativeMist extends BaseCombatLayeredRenderingPlugin {
        private final DamagingProjectileAPI projectile;
        private final SpriteAPI cloud;
        private float riftTimer;
        private float spin;

        VolatilizerNegativeMist(DamagingProjectileAPI projectile) {
            this.projectile = projectile;
            this.cloud = Global.getSettings().getSprite("misc", "nebula_particles2");
            this.cloud.setTexWidth(0.5f);
            this.cloud.setTexHeight(0.5f);
            this.cloud.setTexX((float)Math.floor(Math.random() * 2f) * 0.5f);
            this.cloud.setTexY((float)Math.floor(Math.random() * 2f) * 0.5f);
            this.cloud.setNormalBlend();
            this.riftTimer = (float)Math.random() * 0.12f;
        }

        @Override public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.BELOW_SHIPS_LAYER);
        }

        @Override public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused() || isExpired()) return;
            spin += amount * 95f;
            riftTimer -= amount;
            if (riftTimer <= 0f) {
                riftTimer = 0.16f + (float)Math.random() * 0.08f;
                Vector2f point = new Vector2f(projectile.getLocation());
                Vector2f backwards = new Vector2f(projectile.getVelocity());
                backwards.scale(-0.012f);
                Vector2f.add(point, backwards, point);
                Vector2f drift = new Vector2f(projectile.getVelocity());
                drift.scale(0.16f);
                PTSDTarotEffects.spawnRiftVisual(engine, point, drift,
                        13f + (float)Math.random() * 9f, new Color(20, 0, 28, 125), true);
            }
        }

        @Override public boolean isExpired() {
            CombatEngineAPI engine = Global.getCombatEngine();
            return projectile == null || projectile.isExpired() || projectile.didDamage()
                    || engine == null || !engine.isEntityInPlay(projectile);
        }

        @Override public float getRenderRadius() { return 115f; }

        @Override public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (isExpired()) return;
            Vector2f point = projectile.getLocation();
            float alpha = viewport.getAlphaMult();
            renderCloud(point, 92f, spin, alpha * 0.28f, new Color(0, 0, 0));
            renderCloud(point, 61f, -spin * 1.55f + 80f, alpha * 0.38f, new Color(18, 0, 25));
            renderCloud(point, 36f, spin * 2.1f + 170f, alpha * 0.32f, new Color(48, 2, 55));
        }

        private void renderCloud(Vector2f point, float size, float angle, float alpha, Color color) {
            cloud.setSize(size, size);
            cloud.setAngle(angle);
            cloud.setAlphaMult(alpha);
            cloud.setColor(color);
            cloud.renderAtCenter(point.x, point.y);
        }
    }
    private static final class TarotProjectileVisual extends BaseCombatLayeredRenderingPlugin {
        private final DamagingProjectileAPI projectile;
        private final Style style;
        private final SpriteAPI sprite;
        private float particleTimer;
        private float spin;

        TarotProjectileVisual(DamagingProjectileAPI projectile, Style style) {
            super(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
            this.projectile = projectile;
            this.style = style;
            this.sprite = Global.getSettings().getSprite("graphics/fx/radial_EX.png");
            // this.sprite = Global.getSettings().getSprite("graphics/missiles/PTSD_Taslo.png");
            this.particleTimer = (float) Math.random() * style.trailInterval;
        }

        @Override
        public void advance(float amount) {
            if (isExpired() || Global.getCombatEngine().isPaused()) return;
            spin += amount * style.rotation;
            particleTimer -= amount;
            if (particleTimer > 0f) return;
            particleTimer = style.trailInterval;
            CombatEngineAPI engine = Global.getCombatEngine();
            Vector2f point = new Vector2f(projectile.getLocation());
            Vector2f backwards = new Vector2f(projectile.getVelocity());
            backwards.scale(-0.025f);
            Vector2f.add(point, backwards, point);
            Vector2f velocity = new Vector2f(projectile.getVelocity());
            velocity.scale(0.12f);
            engine.addNegativeSwirlyNebulaParticle(point, velocity,
                    style.spriteSize * (0.65f + (float) Math.random() * 0.45f),
                    1.45f, 0.05f, 0.15f, 0.28f, style.darkColor);
            engine.addSmoothParticle(point, velocity, style.spriteSize * 0.3f,
                    0.75f, 0.16f, style.glowColor);
        }

        @Override public boolean isExpired() {
            CombatEngineAPI engine = Global.getCombatEngine();
            return projectile.isExpired() || projectile.didDamage() || engine == null
                    || !engine.isEntityInPlay(projectile);
        }

        @Override public float getRenderRadius() { return 140f; }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (isExpired()) return;
            float alpha = 0.92f * viewport.getAlphaMult();
            float size = style.spriteSize;
            sprite.setNormalBlend();
            sprite.setSize(size * 2.2f, size * 2.2f);
            sprite.setAngle(projectile.getFacing() + spin * 0.35f);
            sprite.setAlphaMult(alpha * 0.35f);
            sprite.setColor(style.glowColor);
            sprite.renderAtCenter(projectile.getLocation().x, projectile.getLocation().y);
            sprite.setAdditiveBlend();
            sprite.setSize(size, size);
            sprite.setAngle(projectile.getFacing() + spin);
            sprite.setAlphaMult(alpha);
            sprite.setColor(style.bodyColor);
            sprite.renderAtCenter(projectile.getLocation().x, projectile.getLocation().y);
        }
    }
}