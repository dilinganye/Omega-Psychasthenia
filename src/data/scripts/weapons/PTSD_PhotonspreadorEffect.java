package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** Automatic short-range component restoration pulse. */
public class PTSD_PhotonspreadorEffect implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {
    private static final float RIPPLE_RADIUS = 100f;
    private static final float RIPPLE_DURATION = 1f;
    private static final float WEAPON_REPAIR_FRACTION = 0.25f;
    private static final float ENGINE_REPAIR_FRACTION = 0.25f;
    private static final float DISABLED_RECOVERY_FRACTION = 0.50f;
    private static final float MODULE_REPAIR_FRACTION = 0.12f;
    private static final float JITTER_DURATION = 0.20f;
    private static final float EPSILON = 0.5f;
    private static final Color JITTER_COLOR = new Color(174, 120, 255, 150);

    private final IntervalUtil targetScan = new IntervalUtil(0.07f, 0.12f);
    private boolean repairAvailable;
    private boolean selfRecoveryLatched;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || engine.isPaused() || weapon == null || weapon.getShip() == null) return;
        if (weapon.isDisabled() && !weapon.isPermanentlyDisabled()
                && weapon.usesAmmo() && weapon.getAmmo() > 0 && !selfRecoveryLatched) {
            int ammoBeforeRecovery = weapon.getAmmo();
            weapon.repair();
            weapon.setCurrHealth(Math.max(weapon.getCurrHealth(),
                    weapon.getMaxHealth() * DISABLED_RECOVERY_FRACTION));
            weapon.setAmmo(Math.max(0, ammoBeforeRecovery - 1));
            selfRecoveryLatched = true;
            releasePulse(engine, weapon, new Vector2f(weapon.getLocation()));
            return;
        }
        if (!weapon.isDisabled()) selfRecoveryLatched = false;
        targetScan.advance(amount);
        if (targetScan.intervalElapsed()) repairAvailable = hasRepairTarget(engine, weapon);

        boolean canCycle = !weapon.isDisabled() && !weapon.isPermanentlyDisabled()
                && (!weapon.usesAmmo() || weapon.getAmmo() > 0);
        if (repairAvailable && canCycle) {
            // The script, rather than player input or autofire AI, is the only firing authority.
            weapon.setForceFireOneFrame(true);
        } else {
            weapon.setForceNoFireOneFrame(true);
            weapon.stopFiring();
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (engine == null || weapon == null || weapon.getShip() == null) return;
        Vector2f origin = projectile == null ? new Vector2f(weapon.getLocation())
                : new Vector2f(projectile.getLocation());
        releasePulse(engine, weapon, origin);
        if (projectile != null && engine.isEntityInPlay(projectile)) engine.removeEntity(projectile);
    }

    private void releasePulse(CombatEngineAPI engine, WeaponAPI weapon, Vector2f origin) {
        spawnRipple(origin, weapon.getShip().getVelocity());
        RepairResult result = repairTargets(engine, weapon);
        if (!result.weapons.isEmpty() || !result.modules.isEmpty()) {
            engine.addLayeredRenderingPlugin(new RepairJitterVisual(result.weapons, result.modules));
        }
        repairAvailable = false;
    }

    private static void spawnRipple(Vector2f location, Vector2f sourceVelocity) {
        RippleDistortion ripple = new RippleDistortion(location,
                sourceVelocity == null ? new Vector2f() : new Vector2f(sourceVelocity));
        ripple.setSize(RIPPLE_RADIUS);
        ripple.setIntensity(18f);
        ripple.setFrameRate(60f / RIPPLE_DURATION);
        ripple.setLifetime(RIPPLE_DURATION);
        DistortionShader.addDistortion(ripple);
    }

    private static boolean hasRepairTarget(CombatEngineAPI engine, WeaponAPI sourceWeapon) {
        ShipAPI source = sourceWeapon.getShip();
        Vector2f origin = sourceWeapon.getLocation();
        float range = sourceWeapon.getRange();
        for (ShipAPI ship : engine.getShips()) {
            if (!validAlly(ship, source)) continue;
            if (Misc.getDistance(origin, ship.getLocation()) > range + ship.getCollisionRadius()) continue;
            if (isRepairableModule(ship) && inRange(origin, ship.getLocation(), range)
                    && ship.getHitpoints() < ship.getMaxHitpoints() - EPSILON) return true;
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (repairable(weapon) && inRange(origin, weapon.getLocation(), range)) return true;
            }
            for (ShipEngineControllerAPI.ShipEngineAPI enginePart
                    : ship.getEngineController().getShipEngines()) {
                if (repairable(enginePart) && inRange(origin, enginePart.getLocation(), range)) return true;
            }
        }
        return false;
    }

    private static RepairResult repairTargets(CombatEngineAPI engine, WeaponAPI sourceWeapon) {
        RepairResult result = new RepairResult();
        ShipAPI source = sourceWeapon.getShip();
        Vector2f origin = sourceWeapon.getLocation();
        float range = sourceWeapon.getRange();
        for (ShipAPI ship : engine.getShips()) {
            if (!validAlly(ship, source)) continue;
            if (Misc.getDistance(origin, ship.getLocation()) > range + ship.getCollisionRadius()) continue;

            if (isRepairableModule(ship) && inRange(origin, ship.getLocation(), range)
                    && ship.getHitpoints() < ship.getMaxHitpoints() - EPSILON) {
                ship.setHitpoints(Math.min(ship.getMaxHitpoints(), ship.getHitpoints()
                        + ship.getMaxHitpoints() * MODULE_REPAIR_FRACTION));
                result.modules.add(ship);
            }
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (!repairable(weapon) || !inRange(origin, weapon.getLocation(), range)) continue;
                boolean disabled = weapon.isDisabled();
                float restored = disabled
                        ? Math.max(weapon.getCurrHealth(), weapon.getMaxHealth() * DISABLED_RECOVERY_FRACTION)
                        : Math.min(weapon.getMaxHealth(), weapon.getCurrHealth()
                                + weapon.getMaxHealth() * WEAPON_REPAIR_FRACTION);
                if (disabled) weapon.repair();
                weapon.setCurrHealth(Math.min(weapon.getMaxHealth(), restored));
                result.weapons.add(weapon);
            }
            for (ShipEngineControllerAPI.ShipEngineAPI enginePart
                    : ship.getEngineController().getShipEngines()) {
                if (!repairable(enginePart) || !inRange(origin, enginePart.getLocation(), range)) continue;
                boolean disabled = enginePart.isDisabled();
                float restored = disabled
                        ? Math.max(enginePart.getHitpoints(), enginePart.getMaxHitpoints()
                                * DISABLED_RECOVERY_FRACTION)
                        : Math.min(enginePart.getMaxHitpoints(), enginePart.getHitpoints()
                                + enginePart.getMaxHitpoints() * ENGINE_REPAIR_FRACTION);
                if (disabled) enginePart.repair();
                enginePart.setHitpoints(Math.min(enginePart.getMaxHitpoints(), restored));
            }
        }
        return result;
    }

    private static boolean validAlly(ShipAPI ship, ShipAPI source) {
        return ship != null && source != null && ship.isAlive() && !ship.isHulk()
                && ship.getOwner() == source.getOwner();
    }

    private static boolean isRepairableModule(ShipAPI ship) {
        return ship.isStationModule() || ship.getParentStation() != null;
    }

    private static boolean repairable(WeaponAPI weapon) {
        return weapon != null && !weapon.isDecorative() && !weapon.isPermanentlyDisabled()
                && (weapon.isDisabled() || weapon.getCurrHealth() < weapon.getMaxHealth() - EPSILON);
    }

    private static boolean repairable(ShipEngineControllerAPI.ShipEngineAPI engine) {
        return engine != null && !engine.isSystemActivated() && !engine.isPermanentlyDisabled()
                && (engine.isDisabled() || engine.getHitpoints() < engine.getMaxHitpoints() - EPSILON);
    }

    private static boolean inRange(Vector2f from, Vector2f to, float range) {
        return from != null && to != null && Misc.getDistance(from, to) <= range;
    }

    private static final class RepairResult {
        final List<WeaponAPI> weapons = new ArrayList<WeaponAPI>();
        final List<ShipAPI> modules = new ArrayList<ShipAPI>();
    }

    /** Renders only short-lived displaced copies of repaired weapon sprites. */
    private static final class RepairJitterVisual extends BaseCombatLayeredRenderingPlugin {
        private final List<WeaponGhost> ghosts = new ArrayList<WeaponGhost>();
        private final List<ShipAPI> modules;
        private float elapsed;

        RepairJitterVisual(List<WeaponAPI> weapons, List<ShipAPI> modules) {
            for (WeaponAPI weapon : weapons) {
                WeaponGhost ghost = WeaponGhost.create(weapon);
                if (ghost != null) ghosts.add(ghost);
            }
            this.modules = new ArrayList<ShipAPI>(modules);
        }

        @Override public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        }

        @Override public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused()) return;
            elapsed += amount;
            float level = Math.max(0f, 1f - elapsed / JITTER_DURATION);
            for (ShipAPI module : modules) {
                if (engine.isEntityInPlay(module) && module.isAlive()) {
                    module.setJitter(this, JITTER_COLOR, level, 2, 0f, 3f * level);
                }
            }
        }

        @Override public boolean isExpired() { return elapsed >= JITTER_DURATION; }
        @Override public float getRenderRadius() { return 100000f; }

        @Override public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || isExpired()) return;
            float level = Math.max(0f, 1f - elapsed / JITTER_DURATION) * viewport.getAlphaMult();
            for (WeaponGhost ghost : ghosts) {
                WeaponAPI weapon = ghost.weapon;
                if (weapon.getShip() == null || !engine.isEntityInPlay(weapon.getShip())) continue;
                ghost.sprite.setAngle(weapon.getCurrAngle() - 90f);
                ghost.sprite.setColor(JITTER_COLOR);
                ghost.sprite.setAlphaMult(level * 0.32f);
                ghost.sprite.setNormalBlend();
                for (int copy = 0; copy < 2; copy++) {
                    float phase = elapsed * (47f + copy * 13f) + ghost.phase;
                    float dx = (float)Math.sin(phase) * (2.2f + copy);
                    float dy = (float)Math.cos(phase * 1.37f) * (1.6f + copy * 0.7f);
                    ghost.sprite.renderAtCenter(weapon.getLocation().x + dx, weapon.getLocation().y + dy);
                }
            }
        }
    }

    private static final class WeaponGhost {
        final WeaponAPI weapon;
        final SpriteAPI sprite;
        final float phase;

        private WeaponGhost(WeaponAPI weapon, SpriteAPI sprite) {
            this.weapon = weapon;
            this.sprite = sprite;
            phase = (float)Math.random() * 6.2831855f;
        }

        static WeaponGhost create(WeaponAPI weapon) {
            if (weapon == null || weapon.getSpec() == null || weapon.getSprite() == null) return null;
            String path = weapon.getSlot().isHardpoint()
                    ? weapon.getSpec().getHardpointSpriteName() : weapon.getSpec().getTurretSpriteName();
            if (path == null || path.isEmpty()) return null;
            SpriteAPI sprite = Global.getSettings().getSprite(path);
            sprite.setSize(weapon.getSprite().getWidth(), weapon.getSprite().getHeight());
            return new WeaponGhost(weapon, sprite);
        }
    }
}