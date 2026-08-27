package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

/** Per-projectile dispatcher; canonical and legacy Volatilizer ids are both accepted. */
public class PTSD_TarotOnHitEffect implements OnHitEffectPlugin {
    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point,
                      boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (projectile == null || engine == null) return;
        String id = projectile.getProjectileSpecId();
        if ("PTSD_Aftershockor_shot".equals(id)) {
            PTSDTarotEffects.applyAftershock(engine, projectile, target, point, shieldHit, damageResult);
        } else if ("PTSD_Fracture_Calculus_shot".equals(id)) {
            PTSDTarotEffects.applyFracture(engine, target, shieldHit);
        } else if ("PTSD_TasloLauncher_shot".equals(id)) {
            PTSDTarotEffects.applyLauncher(engine, target, point, shieldHit);
        } else if ("PTSD_Fracture_Volatilizatior_shot".equals(id)
                || "PTSD_Facture_Volatilizatior_shot".equals(id)) {
            PTSDTarotEffects.applyVolatilizer(engine, projectile, target, point, shieldHit);
        }
    }
}