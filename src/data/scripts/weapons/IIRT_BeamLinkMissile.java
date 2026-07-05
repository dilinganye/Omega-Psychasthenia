//
// Thanks Siren for this code
//
package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.util.MagicFakeBeam;

import java.awt.Color;
import java.util.List;

public class IIRT_BeamLinkMissile implements OnFireEffectPlugin {

	private static final String id = "BLM0";

	public static class IIRT_BLMS extends BaseEveryFrameCombatPlugin {

		private DamagingProjectileAPI m0 = null; //定义攻击组件1
		private ShipAPI source = null; //定义舰船
		private DamagingProjectileAPI m1 = null; //定义攻击组件1
		private IntervalUtil beam = new IntervalUtil(0.2f, 0.3f);//定义时间间隔工具

		/**
		 * 构造函数，包含攻击组件1、攻击组件2和舰船对象
		 */
		public IIRT_BLMS(DamagingProjectileAPI m0, DamagingProjectileAPI m1, ShipAPI source) {
			this.source = source;
			this.m0 = m0;
			this.m1 = m1;
			// Global.getLogger(this.getClass()).info("pluginAdd");
		}

		/**
		 * 判断攻击组件是否存活，防止产生不可控的效果
		 */
		private boolean isAlive(DamagingProjectileAPI proj) {
			return !proj.isExpired() && !proj.isFading() && Global.getCombatEngine().isEntityInPlay(proj);
		}

		/**
		 * 在每一帧中执行的函数，主要实现假光束的渲染
		 */
		@Override
		public void advance(float amount, List<InputEventAPI> events) {
			if (Global.getCombatEngine().isPaused()) return;
			beam.advance(amount);
			// Global.getLogger(this.getClass()).info("beamAdvance");
			if (isAlive(m0) && isAlive(m1)) {
				//计算攻击组件间的角度和距离
				float angle = Misc.getAngleInDegreesStrict(m0.getLocation(), m1.getLocation());
				float length = Misc.getDistance(m0.getLocation(), m1.getLocation());
				if (length > 1500f) return;

				//生成假光束m0
				MagicFakeBeam.spawnAdvancedFakeBeam(Global.getCombatEngine(), m0.getLocation(), length, angle, 8f, 8f, 4f, "IIRT_trail_Normal", "IIRT_trail_Sub", 512, 2048, 0, 0, amount, 0f, 5f, Color.WHITE, new Color(140, 138, 204, 55), m0.getDamageAmount() * 5f * amount, DamageType.ENERGY, m0.getEmpAmount() * 1.0f, source);

				//生成假光束m1
				MagicFakeBeam.spawnAdvancedFakeBeam(Global.getCombatEngine(), m1.getLocation(), length, angle + 180f, 8f, 8f, 4f, "IIRT_trail_Normal", "IIRT_trail_Sub", 512, 2048, 0, 0, amount, 0f, 5f, Color.WHITE, new Color(228, 172, 240, 55), m1.getDamageAmount() * 5f * amount, DamageType.ENERGY, m1.getEmpAmount() * 1.0f, source);
				// Global.getLogger(this.getClass()).info("beamAdd");
			} else {
				// Global.getLogger(this.getClass()).info("pluginRemove");
				Global.getCombatEngine().removePlugin(this);
			}
		}
	}

	@Override
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		// Global.getLogger(this.getClass()).info("missileFired");
		ShipAPI ship = weapon.getShip();
		if (ship != null) {
			if (ship.getCustomData().containsKey(id + weapon.getSlot().getId())) {
				if (ship.getCustomData().get(id + weapon.getSlot().getId()) instanceof DamagingProjectileAPI m) {
					engine.addPlugin(new IIRT_BLMS(m, projectile, weapon.getShip()));
					// Global.getLogger(this.getClass()).info("pluginAdd");
					ship.removeCustomData(id + weapon.getSlot().getId());
				}
			} else {
				ship.setCustomData(id + weapon.getSlot().getId(), projectile);
			}
		}
	}
}
