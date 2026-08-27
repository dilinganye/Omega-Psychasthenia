package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual.NEParams;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.opengl.GL14;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Shared combat state and exact hit resolution for the Tarot particle weapon family. */
public final class PTSDTarotEffects {
    public static final String STACK_KEY = "PTSD_tarot_influence";
    private static final String DEBUFF_KEY = "PTSD_tarot_temporal_debuff";
    private static final String MASK_KEY = "PTSD_tarot_influence_mask";
    private static final String DEV_TEXT_KEY = "PTSD_tarot_dev_text_time";
    private static final Random RANDOM = new Random();

    public static final int VOLATILIZER_STACKS = 500;
    public static final int VOLATILIZER_THRESHOLD = 550;
    public static final float FRACTURE_DURATION = 5f;
    public static final float FRACTURE_MAX_SLOW = 95f;
    public static final float SPINOR_RANGE = 1000f;
    public static final float SPINOR_SPAWN_RADIUS = 200f;

    private static final Color TAROT_DARK = new Color(8, 1, 18, 205);
    private static final Color TAROT_CORE = new Color(174, 78, 255, 225);
    private static final Color AFTERSHOCK = new Color(215, 35, 255, 230);
    private static final Color FRACTURE = new Color(150, 75, 255, 230);
    private static final Color COLLAPSE = new Color(255, 55, 185, 235);

    private PTSDTarotEffects() { }

    /** Shields, non-ships and Shrouded/Dweller unknown hulls do not accept Tarot hull/armor effects. */
    public static boolean isEligible(CombatEntityAPI target, boolean shieldHit) {
        if (shieldHit || !(target instanceof ShipAPI)) return false;
        ShipAPI ship = (ShipAPI) target;
        return ship.isAlive() && !ship.isHulk() && ship.getHullSpec() != null
                && !ship.getHullSpec().hasTag(Tags.DWELLER)
                && !ship.getHullSpec().hasTag(Tags.SHROUDED);
    }

    public static int getStacks(ShipAPI ship) {
        if (ship == null) return 0;
        Object value = ship.getCustomData().get(STACK_KEY);
        return value instanceof Number ? Math.max(0, ((Number) value).intValue()) : 0;
    }

    public static int addStacks(ShipAPI ship, int amount) {
        if (ship == null || amount <= 0) return getStacks(ship);
        long total = (long) getStacks(ship) + amount;
        int result = (int) Math.min(Integer.MAX_VALUE, total);
        ship.setCustomData(STACK_KEY, result);
        ensureInfluenceMask(ship);
        return result;
    }

    public static int consumeStacks(ShipAPI ship) {
        int value = getStacks(ship);
        if (ship != null) ship.removeCustomData(STACK_KEY);
        return value;
    }

    /**
     * Uses the ship renderer's own jitter/tint pass as a hull-following black mask.
     * This also follows modules, damage and animations without copying the ship sprite.
     */
    private static void ensureInfluenceMask(ShipAPI ship) {
        if (ship == null || ship.getCustomData().get(MASK_KEY) instanceof TarotInfluenceMask) return;
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;
        TarotInfluenceMask mask = new TarotInfluenceMask(ship);
        ship.setCustomData(MASK_KEY, mask);
        engine.addPlugin(mask);
    }

    /** Removes exactly the same amount from surviving local armor cells; it never spills into hull. */
    public static void applyAftershock(CombatEngineAPI engine, DamagingProjectileAPI projectile,
                                       CombatEntityAPI target, Vector2f point, boolean shieldHit,
                                       ApplyDamageResultAPI result) {
        if (!isEligible(target, shieldHit) || result == null) return;
        ShipAPI ship = (ShipAPI) target;
        spawnAftershockScorch(engine, ship, point);
        float armorLost = Math.max(0f, result.getTotalDamageToArmor());
        if (armorLost <= 0f) return;
        float removed = removeArmorExactly(ship, point, armorLost);
        if (removed <= 0f) return;
        result.setTotalDamageToArmor(result.getTotalDamageToArmor() + removed);
        Vector2f velocity = projectile == null ? new Vector2f() : new Vector2f(projectile.getVelocity());
        velocity.scale(-0.08f);
        spawnRiftVisual(engine, point, velocity, 22f + Math.min(28f, removed * 0.06f), AFTERSHOCK, false);
        for (int i = 0; i < 5; i++) {
            Vector2f spark = Misc.getUnitVectorAtDegreeAngle(RANDOM.nextFloat() * 360f);
            spark.scale(45f + RANDOM.nextFloat() * 120f);
            engine.addSmoothParticle(point, spark, 4f + RANDOM.nextFloat() * 7f,
                    1f, 0.18f + RANDOM.nextFloat() * 0.18f, AFTERSHOCK);
        }
        debugText(engine, ship, "[DEV] 余波额外装甲 -" + Math.round(removed));
    }

    private static float removeArmorExactly(ShipAPI ship, Vector2f point, float requested) {
        ArmorGridAPI grid = ship.getArmorGrid();
        if (grid == null || grid.getGrid() == null || grid.getGrid().length == 0) return 0f;
        int[] hit = grid.getCellAtLocation(point);
        if (hit == null) return 0f;
        final int cx = hit[0], cy = hit[1];
        List<int[]> cells = new ArrayList<int[]>();
        float[][] values = grid.getGrid();
        for (int x = 0; x < values.length; x++) {
            if (values[x] == null) continue;
            for (int y = 0; y < values[x].length; y++) {
                if (Math.abs(x - cx) <= 2 && Math.abs(y - cy) <= 2 && grid.getArmorValue(x, y) > 0f) {
                    cells.add(new int[] {x, y});
                }
            }
        }
        Collections.sort(cells, new Comparator<int[]>() {
            @Override public int compare(int[] a, int[] b) {
                int da = (a[0] - cx) * (a[0] - cx) + (a[1] - cy) * (a[1] - cy);
                int db = (b[0] - cx) * (b[0] - cx) + (b[1] - cy) * (b[1] - cy);
                return Integer.compare(da, db);
            }
        });
        float remaining = requested;
        for (int[] cell : cells) {
            if (remaining <= 0.0001f) break;
            float current = grid.getArmorValue(cell[0], cell[1]);
            float take = Math.min(current, remaining);
            grid.setArmorValue(cell[0], cell[1], current - take);
            remaining -= take;
        }
        float removed = requested - remaining;
        if (removed > 0f) {
            ship.syncWithArmorGridState();
            ship.syncWeaponDecalsWithArmorDamage();
        }
        return removed;
    }

    public static void applyLauncher(CombatEngineAPI engine, CombatEntityAPI target,
                                     Vector2f point, boolean shieldHit) {
        if (!isEligible(target, shieldHit)) return;
        ShipAPI ship = (ShipAPI) target;
        int stacks = addStacks(ship, 1);
        engine.addSmoothParticle(point, new Vector2f(), 7f, 0.75f, 0.16f, TAROT_CORE);
        if (stacks == 1 || stacks % 25 == 0) {
            // engine.addFloatingText(point, "塔罗影响 " + stacks, 13f, TAROT_CORE, ship, 0.25f, 0.1f);
            Color c;
            c = FRACTURE;
            ship.setJitterUnder(target,c , Math.min(1,stacks/50), Math.min(20,stacks/20), 0f, Math.min(50,stacks/50));
        }
        debugText(engine, ship, "[DEV] 塔罗影响 " + stacks);
    }

    public static void applyFracture(CombatEngineAPI engine, CombatEntityAPI target, boolean shieldHit) {
        if (!isEligible(target, shieldHit) || RANDOM.nextFloat() >= 0.05f) return;
        ShipAPI ship = (ShipAPI) target;
        int consumed = consumeHalfStacks(ship);
        if (consumed <= 0) return;
        spawnFractureTriggerVisual(engine, ship);
        Object existing = ship.getCustomData().get(DEBUFF_KEY);
        TarotTemporalDebuff debuff;
        if (existing instanceof TarotTemporalDebuff) {
            debuff = (TarotTemporalDebuff) existing;
            debuff.absorb(consumed);
        } else {
            debuff = new TarotTemporalDebuff(ship, consumed);
            ship.setCustomData(DEBUFF_KEY, debuff);
            engine.addPlugin(debuff);
        }
        float slow = Math.min(FRACTURE_MAX_SLOW, consumed);
        engine.addFloatingText(ship.getLocation(), "消耗 " + consumed + " 层 / 时间流速 -" +
                Math.round(slow) + "%", 20f, FRACTURE, ship, 0.8f, 0.2f);
        debugText(engine, ship, "[DEV] 断裂消耗 " + consumed + "，减速 " + Math.round(slow) + "%");
    }

    private static int consumeHalfStacks(ShipAPI ship) {
        int current = getStacks(ship);
        if (ship == null || current <= 0) return 0;
        int consumed = Math.max(1, current / 2);
        int remaining = current - consumed;
        if (remaining > 0) {
            ship.setCustomData(STACK_KEY, remaining);
            ensureInfluenceMask(ship);
        } else {
            ship.removeCustomData(STACK_KEY);
        }
        return consumed;
    }

    private static void spawnAftershockScorch(CombatEngineAPI engine, ShipAPI target, Vector2f point) {
        if (engine == null || target == null || point == null) return;
        Vector2f offset = Vector2f.sub(point, target.getLocation(), new Vector2f());
        offset = Misc.rotateAroundOrigin(offset, -target.getFacing());
        engine.addLayeredRenderingPlugin(new AftershockScorchVisual(target, offset));
    }

    private static void spawnFractureTriggerVisual(CombatEngineAPI engine, ShipAPI target) {
        if (engine == null || target == null) return;
        List<ShipEngineControllerAPI.ShipEngineAPI> engines =
                new ArrayList<ShipEngineControllerAPI.ShipEngineAPI>();
        for (ShipEngineControllerAPI.ShipEngineAPI part : target.getEngineController().getShipEngines()) {
            if (part != null && part.getLocation() != null) engines.add(part);
            if (engines.size() >= 6) break;
        }
        if (engines.isEmpty()) {
            for (int i = 0; i < 6; i++) {
                Vector2f loc = randomPoint(target.getLocation(), target.getCollisionRadius() * 0.72f + 18f);
                Vector2f drift = new Vector2f(target.getVelocity());
                drift.scale(0.25f);
                engine.addNegativeSwirlyNebulaParticle(loc, drift, 24f + RANDOM.nextFloat() * 42f,
                        1.5f, 0.06f, 0.18f, 0.55f, new Color(68, 0, 52, 185));
            }
        }
        engine.addLayeredRenderingPlugin(new FractureEngineBurst(target, engines));
    }

    public static void applyVolatilizer(CombatEngineAPI engine, DamagingProjectileAPI projectile,
                                        CombatEntityAPI target, Vector2f point, boolean shieldHit) {
        if (!isEligible(target, shieldHit)) return;
        ShipAPI ship = (ShipAPI) target;
        int stacks = addStacks(ship, VOLATILIZER_STACKS);
        engine.addHitParticle(point, ship.getVelocity(), 45f, 0.8f, 0.3f, COLLAPSE);
        debugText(engine, ship, "[DEV] 挥发场 +500；当前 " + stacks);
        if (stacks <= VOLATILIZER_THRESHOLD) return;
        int consumed = consumeStacks(ship);
        spawnCollapse(engine, projectile == null ? null : projectile.getSource(), point, consumed);
    }

    public static void spawnCollapse(CombatEngineAPI engine, ShipAPI source, Vector2f point, int stacks) {
        float radius = Math.min(310f, 135f + stacks * 0.1f);
        float damage = Math.min(1500f, 180f + stacks * 0.75f);
        DamagingExplosionSpec boom = new DamagingExplosionSpec(0.38f, radius, radius * 0.42f,
                damage, damage * 0.58f, CollisionClass.PROJECTILE_NO_FF,
                CollisionClass.PROJECTILE_FIGHTER, 10f, 8f, 1.2f, 42, TAROT_DARK, COLLAPSE);
        boom.setDamageType(DamageType.ENERGY);
        boom.setShowGraphic(false);
        boom.setSoundSetId("rift_hit");
        engine.spawnDamagingExplosion(boom, source, point);
        spawnRiftVisual(engine, point, new Vector2f(), radius * 0.58f, COLLAPSE, false);
        for (int i = 0; i < 10; i++) {
            Vector2f p = randomPoint(point, radius * 0.75f);
            engine.addNegativeSwirlyNebulaParticle(p, new Vector2f(), 55f + RANDOM.nextFloat() * 110f,
                    1.8f, 0.08f, 0.22f, 1.1f, TAROT_DARK);
        }
        engine.addFloatingText(point, "塔罗塌缩 / " + stacks, 24f, COLLAPSE, null, 1f, 0.25f);
    }

    public static void spawnStorm(CombatEngineAPI engine, ShipAPI source, Vector2f point) {
        DamagingExplosionSpec storm = new DamagingExplosionSpec(0.24f, 118f, 45f,
                145f, 82f, CollisionClass.PROJECTILE_NO_FF, CollisionClass.PROJECTILE_FIGHTER,
                6f, 5f, 0.7f, 18, TAROT_DARK, TAROT_CORE);
        storm.setDamageType(DamageType.ENERGY);
        storm.setShowGraphic(false);
        engine.spawnDamagingExplosion(storm, source, point);
        spawnRiftVisual(engine, point, new Vector2f(), 38f + RANDOM.nextFloat() * 14f,
                TAROT_CORE, false);
        for (int i = 0; i < 3; i++) {
            engine.addNegativeSwirlyNebulaParticle(randomPoint(point, 85f), new Vector2f(),
                    40f + RANDOM.nextFloat() * 65f, 1.5f, 0.1f, 0.25f, 0.7f, TAROT_DARK);
        }
    }

    /** Uses the combat spatial grid and exactly one (1000-distance) weight, including fighters/projectiles. */
    public static CombatEntityAPI pickStormTarget(CombatEngineAPI engine, ShipAPI source, Vector2f origin) {
        if (engine == null || source == null || origin == null) return null;
        WeightedRandomPicker<CombatEntityAPI> picker = new WeightedRandomPicker<CombatEntityAPI>(RANDOM);
        Iterator<Object> iterator = engine.getAllObjectGrid().getCheckIterator(origin,
                SPINOR_RANGE * 2f, SPINOR_RANGE * 2f);
        while (iterator.hasNext()) {
            Object next = iterator.next();
            if (!(next instanceof CombatEntityAPI)) continue;
            CombatEntityAPI entity = (CombatEntityAPI) next;
            if (entity == source || entity.isExpired() || entity.getOwner() == source.getOwner()) continue;
            if (entity.getCollisionClass() == CollisionClass.NONE) continue;
            if (entity instanceof ShipAPI) {
                ShipAPI ship = (ShipAPI) entity;
                if (!ship.isAlive() || ship.isHulk()) continue;
            } else if (!(entity instanceof DamagingProjectileAPI)) {
                // Keep neutral/enemy asteroids and other physical non-friendly combat objects.
                if (!engine.getAsteroids().contains(entity)) continue;
            }
            float distance = Misc.getDistance(origin, entity.getLocation());
            float weight = SPINOR_RANGE - distance;
            if (weight > 0f) picker.add(entity, weight);
        }
        return picker.pick();
    }

    public static void spawnRiftVisual(CombatEngineAPI engine, Vector2f point, Vector2f velocity,
                                       float radius, Color color, final boolean belowShips) {
        if (engine == null || point == null) return;
        NEParams params = new NEParams();
        params.radius = Math.max(8f, radius);
        params.thickness = Math.max(10f, radius * 0.72f);
        params.fadeIn = belowShips ? 0.35f : 0.08f;
        params.fadeOut = belowShips ? 0.65f : 0.55f;
        params.noiseMag = belowShips ? 1.8f : 1.2f;
        params.noisePeriod = 0.08f;
        params.hitGlowSizeMult = belowShips ? 0f : 0.7f;
        params.withHitGlow = !belowShips;
        params.withNegativeParticles = !belowShips;
        params.color = color;
        params.blackColor = Color.BLACK;
        params.underglow = belowShips ? null : new Color(color.getRed(), color.getGreen(), color.getBlue(), 80);
        NegativeExplosionVisual visual = new NegativeExplosionVisual(params) {
            @Override public EnumSet<CombatEngineLayers> getActiveLayers() {
                return belowShips ? EnumSet.of(CombatEngineLayers.BELOW_SHIPS_LAYER) : super.getActiveLayers();
            }
        };
        CombatEntityAPI entity = engine.addLayeredRenderingPlugin(visual);
        entity.getLocation().set(point);
        if (velocity != null) entity.getVelocity().set(velocity);
    }

    /** Short-lived local pixel inversion and negative-space distortion, reusable by Tarot projectiles. */
    public static void spawnInversionField(CombatEngineAPI engine, Vector2f point, Vector2f velocity,
                                           float radius, Color color) {
        if (engine == null || point == null) return;
        NEParams params = new NEParams();
        params.radius = Math.max(18f, radius);
        params.thickness = Math.max(20f, radius * 0.78f);
        params.fadeIn = 0.035f;
        params.fadeOut = 0.26f;
        params.noiseMag = 1.45f;
        params.noisePeriod = 0.055f;
        params.hitGlowSizeMult = 0f;
        params.withHitGlow = false;
        params.withNegativeParticles = false;
        params.color = color == null ? new Color(105, 50, 210, 175) : color;
        params.blackColor = new Color(0, 0, 0, 235);
        params.invertForDarkening = params.color;
        params.underglow = new Color(params.color.getRed(), params.color.getGreen(),
                params.color.getBlue(), 75);
        NegativeExplosionVisual visual = new NegativeExplosionVisual(params);
        CombatEntityAPI entity = engine.addLayeredRenderingPlugin(visual);
        entity.getLocation().set(point);
        if (velocity != null) entity.getVelocity().set(velocity);
    }
    public static Vector2f randomPoint(Vector2f center, float radius) {
        float angle = RANDOM.nextFloat() * 360f;
        float distance = (float) Math.sqrt(RANDOM.nextFloat()) * radius;
        Vector2f result = Misc.getUnitVectorAtDegreeAngle(angle);
        result.scale(distance);
        result.translate(center.x, center.y);
        return result;
    }

    /** Disintegrator-like persistent reverse-subtract burn mark without its damage ticks. */
    private static final class AftershockScorchVisual extends BaseCombatLayeredRenderingPlugin {
        private final ShipAPI target;
        private final Vector2f localOffset;
        private final List<ScorchParticle> particles = new ArrayList<ScorchParticle>();
        private final IntervalUtil emission = new IntervalUtil(0.075f, 0.13f);
        private float elapsed;

        AftershockScorchVisual(ShipAPI target, Vector2f localOffset) {
            this.target = target;
            this.localOffset = localOffset;
            emission.forceIntervalElapsed();
        }

        @Override public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.BELOW_INDICATORS_LAYER);
        }
        @Override public float getRenderRadius() { return target.getCollisionRadius() + 120f; }
        @Override public boolean isExpired() {
            CombatEngineAPI engine = Global.getCombatEngine();
            return engine == null || !target.isAlive() || !engine.isEntityInPlay(target)
                    || (elapsed >= 2.6f && particles.isEmpty());
        }
        @Override public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused() || isExpired()) return;
            elapsed += amount;
            emission.advance(amount);
            if (elapsed <= 1.55f && emission.intervalElapsed()) {
                particles.add(new ScorchParticle());
                if (RANDOM.nextFloat() < 0.45f) particles.add(new ScorchParticle());
            }
            Iterator<ScorchParticle> iterator = particles.iterator();
            while (iterator.hasNext()) {
                ScorchParticle particle = iterator.next();
                particle.elapsed += amount;
                particle.angle += particle.turnRate * amount;
                if (particle.elapsed >= particle.duration) iterator.remove();
            }
        }
        @Override public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (isExpired()) return;
            Vector2f center = Misc.rotateAroundOrigin(new Vector2f(localOffset), target.getFacing());
            Vector2f.add(target.getLocation(), center, center);
            GL14.glBlendEquation(GL14.GL_FUNC_REVERSE_SUBTRACT);
            for (ScorchParticle particle : particles) {
                float life = particle.elapsed / particle.duration;
                float alpha = (1f - life) * Math.min(1f, particle.elapsed / 0.12f)
                        * viewport.getAlphaMult();
                particle.sprite.setAngle(particle.angle + target.getFacing());
                particle.sprite.setSize(particle.size * (1f + life * 0.55f),
                        particle.size * (0.75f + life * 0.32f));
                particle.sprite.setColor(new Color(255, 24, 178, 88));
                particle.sprite.setAlphaMult(alpha);
                particle.sprite.renderAtCenter(center.x + particle.offset.x, center.y + particle.offset.y);
            }
            GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        }
    }

    private static final class ScorchParticle {
        final SpriteAPI sprite;
        final Vector2f offset;
        final float duration = 0.8f + RANDOM.nextFloat() * 0.8f;
        final float size = 18f + RANDOM.nextFloat() * 22f;
        final float turnRate = -18f + RANDOM.nextFloat() * 36f;
        float elapsed;
        float angle = RANDOM.nextFloat() * 360f;

        ScorchParticle() {
            sprite = Global.getSettings().getSprite("misc", "nebula_particles");
            int x = RANDOM.nextInt(4), y = RANDOM.nextInt(4);
            sprite.setTexWidth(0.25f);
            sprite.setTexHeight(0.25f);
            sprite.setTexX(x * 0.25f);
            sprite.setTexY(y * 0.25f);
            sprite.setAdditiveBlend();
            offset = randomPoint(new Vector2f(), 11f);
        }
    }

    /** Brief non-circular polar ruptures on engines; engine-less hulls receive magenta jitter. */
    private static final class FractureEngineBurst extends BaseCombatLayeredRenderingPlugin {
        private final ShipAPI target;
        private final List<ShipEngineControllerAPI.ShipEngineAPI> engines;
        private final List<PTSDPolarRiftRenderer.Shape> shapes =
                new ArrayList<PTSDPolarRiftRenderer.Shape>();
        private float elapsed;

        FractureEngineBurst(ShipAPI target, List<ShipEngineControllerAPI.ShipEngineAPI> engines) {
            this.target = target;
            this.engines = engines;
            int count = Math.max(1, engines.size());
            for (int i = 0; i < count; i++) {
                shapes.add(new PTSDPolarRiftRenderer.Shape(RANDOM.nextLong(), 20, 3 + i % 5,
                        0.30f + RANDOM.nextFloat() * 0.22f, 0.30f));
            }
        }
        @Override public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        }
        @Override public float getRenderRadius() { return target.getCollisionRadius() + 90f; }
        @Override public boolean isExpired() {
            CombatEngineAPI engine = Global.getCombatEngine();
            return elapsed >= 0.48f || engine == null || !target.isAlive()
                    || !engine.isEntityInPlay(target);
        }
        @Override public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused()) return;
            elapsed += amount;
            if (engines.isEmpty() && elapsed <= 0.24f) {
                float level = 1f - elapsed / 0.24f;
                target.setJitter(this, new Color(255, 30, 190, 190), level, 5, 0f, 7f * level);
            }
        }
        @Override public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (isExpired() || engines.isEmpty()) return;
            float progress = Math.min(1f, elapsed / 0.30f);
            float alpha = Math.max(0f, 1f - elapsed / 0.48f) * viewport.getAlphaMult();
            for (int i = 0; i < engines.size(); i++) {
                Vector2f location = engines.get(i).getLocation();
                if (location == null) continue;
                PTSDPolarRiftRenderer.render(shapes.get(i), location, target.getFacing() + 180f,
                        3f + progress * 20f, 0.58f * progress, elapsed, alpha,
                        new Color(18, 0, 20, 220), new Color(255, 42, 190, 235), 1f);
            }
        }
    }

    private static void debugText(CombatEngineAPI engine, ShipAPI ship, String text) {
        if (!Global.getSettings().isDevMode() || engine == null || ship == null) return;
        float now = engine.getTotalElapsedTime(false);
        Object previous = ship.getCustomData().get(DEV_TEXT_KEY);
        if (previous instanceof Number && now - ((Number) previous).floatValue() < 0.45f) return;
        ship.setCustomData(DEV_TEXT_KEY, now);
        engine.addFloatingText(ship.getLocation(), text, 13f, Color.YELLOW, ship, 0.2f, 0.1f);
    }

    /** Five-second local-time fracture. Repeated applications retain the largest consumption and refresh. */
    public static final class TarotTemporalDebuff extends BaseEveryFrameCombatPlugin {
        private final ShipAPI target;
        private final IntervalUtil visual = new IntervalUtil(0.12f, 0.19f);
        private float remaining = FRACTURE_DURATION;
        private int maxConsumed;

        TarotTemporalDebuff(ShipAPI target, int consumed) {
            this.target = target;
            this.maxConsumed = consumed;
        }

        void absorb(int consumed) {
            maxConsumed = Math.max(maxConsumed, consumed);
            remaining = FRACTURE_DURATION;
        }

        @Override
        public void advance(float amount, List<com.fs.starfarer.api.input.InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused()) return;
            if (!target.isAlive() || target.isHulk() || !engine.isEntityInPlay(target)) {
                clear(engine);
                return;
            }
            remaining -= amount;
            float slowPercent = Math.min(FRACTURE_MAX_SLOW, maxConsumed);
            target.getMutableStats().getTimeMult().modifyMult(DEBUFF_KEY, 1f - slowPercent / 100f);
            visual.advance(amount);
            if (visual.intervalElapsed()) {
                Vector2f point = randomPoint(target.getLocation(), target.getCollisionRadius() * 0.8f + 20f);
                Vector2f velocity = new Vector2f(target.getVelocity());
                velocity.scale(0.35f);
                engine.addNegativeSwirlyNebulaParticle(point, velocity,
                        18f + RANDOM.nextFloat() * 32f, 1.5f, 0.08f, 0.2f, 0.45f, TAROT_DARK);
                engine.addSmoothParticle(point, velocity, 5f, 0.8f, 0.22f, FRACTURE);
            }
            if (remaining <= 0f) clear(engine);
        }

        private void clear(CombatEngineAPI engine) {
            target.getMutableStats().getTimeMult().unmodify(DEBUFF_KEY);
            target.removeCustomData(DEBUFF_KEY);
            if (engine != null) engine.removePlugin(this);
        }
    }
    /** Persistent stack visualization; one lightweight plugin is created per affected ship. */
    public static final class TarotInfluenceMask extends BaseEveryFrameCombatPlugin {
        private final ShipAPI target;

        TarotInfluenceMask(ShipAPI target) {
            this.target = target;
        }

        @Override
        public void advance(float amount, List<com.fs.starfarer.api.input.InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null) return;
            int stacks = getStacks(target);
            if (stacks <= 0 || !target.isAlive() || target.isHulk() || !engine.isEntityInPlay(target)) {
                target.removeCustomData(MASK_KEY);
                engine.removePlugin(this);
                return;
            }

            // A soft exponential curve keeps an unlimited stack count visually bounded.
            float level = 1f - (float) Math.exp(-stacks / 180f);
            int alpha = Math.min(215, Math.max(2, Math.round(215f * level)));
            int copies = 1 + Math.min(4, stacks / 140);
            float range = Math.min(3.5f, level * 3.5f);
            target.setJitter(this, new Color(0, 0, 0, alpha), 1f, copies, 0f, range);
        }
    }
}