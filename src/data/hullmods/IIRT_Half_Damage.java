package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class IIRT_Half_Damage extends BaseHullMod {

	public static final float BEAM_WEAPON_DAMAGE_MULT = 0.7f;    //光束 伤害
	public static final float ENERGY_WEAPON_DAMAGE_MULT = 0.75f;    //能量 伤害
	public static final float BALLISTIC_WEAPON_DAMAGE_MULT = 0.75f;    //实弹 伤害
	public static final float MISSILE_WEAPON_DAMAGE_MULT = 0.75f;    //导弹 伤害
	public static final float ENERGY_WEAPON_RANGE = 0.7f;    //能量武器 距离

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getBeamWeaponDamageMult().modifyMult(id, BEAM_WEAPON_DAMAGE_MULT);
		stats.getEnergyWeaponDamageMult().modifyMult(id, ENERGY_WEAPON_DAMAGE_MULT);
		stats.getBallisticWeaponDamageMult().modifyMult(id, BALLISTIC_WEAPON_DAMAGE_MULT);
		stats.getMissileWeaponDamageMult().modifyMult(id, MISSILE_WEAPON_DAMAGE_MULT);

		stats.getEnergyWeaponRangeBonus().modifyMult(id, ENERGY_WEAPON_RANGE);
	}
}