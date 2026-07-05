// 非常感谢k的硬辐能beam代码授权，
// 我完全不会写代码qwq，太感谢了
// IIRT_Theaten等硬辐能武器使用

package data.scripts.weapons;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;

public class IIRT_HeavyBeam implements BeamEffectPlugin {

	@Override
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		beam.getDamage().setForceHardFlux(true);
		beam.getDamage().isForceHardFlux();
	}
}