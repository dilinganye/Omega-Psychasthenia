//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.utils.iirt_omega.IIRT_Omega_Color;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class IIRT_Lab_Main extends BaseHullMod {

	public static final String id = "IIRT_KRM";

	/**
	 * This is the center hullmod for KRM.
	 */
	public final float hullDamageTakenMult = 5;

	private static Map<HullSize, Float> mag = new HashMap<>(); //不同船型
	private static Map<HullSize, Float> Damg = new HashMap<>(); //不同船型

	static {
		mag.put(HullSize.FIGHTER, 5f);
		mag.put(HullSize.FRIGATE, 4f);
		mag.put(HullSize.DESTROYER, 5f);
		mag.put(HullSize.CRUISER, 8f);
		mag.put(HullSize.CAPITAL_SHIP, 12f);

		Damg.put(HullSize.FIGHTER, 0.2f);
		Damg.put(HullSize.FRIGATE, 0.75f);
		Damg.put(HullSize.DESTROYER, 1.25f);
		Damg.put(HullSize.CRUISER, 1.75f);
		Damg.put(HullSize.CAPITAL_SHIP, 3f);
	}

	public void arcstrike(ShipAPI ship, CombatEntityAPI entity) {
		Global.getCombatEngine().spawnEmpArc(ship, //damageSource
				Misc.getPointWithinRadius(ship.getLocation(), ship.getCollisionRadius() / 2f), //Vector2f
				ship, //pointAnchor
				entity, //empTargetEntity
				DamageType.ENERGY, Math.min(ship.getCurrFlux() / 100f, 100f) * Damg.get(ship.getHullSize()), //damAmount
				Math.min(ship.getCurrFlux() / 50f, 100f) * Damg.get(ship.getHullSize()), //empDamAmount
				mag.get(ship.getHullSize()) * 100f, //maxRange
				"tachyon_lance_emp_impact", //sound
				15f + (float)(25 * Math.random() * mag.get(ship.getHullSize()) / 5), //thickness
				new Color(96, 85, 147, 255), //fringe
				IIRT_Omega_Color.IIRT_Omega_Lab_Weapon); //core
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {

		CombatEngineAPI engine = Global.getCombatEngine();
		if (!engine.isEntityInPlay(ship) || engine.isPaused()) {
			return;
		}

		if (!ship.isAlive()) {
			for (ShipAPI s : CombatUtils.getShipsWithinRange(ship.getLocation(), mag.get(ship.getHullSize()) * 100f)) {
				if (Math.random() < 0.01 * mag.get(ship.getHullSize())) {
					if (ship.getCurrFlux() >= 500f) {
						if (!ship.isPhased()) {
							arcstrike(ship, s);
						}
					}
				}
			}
			for (CombatEntityAPI a : CombatUtils.getAsteroidsWithinRange(ship.getLocation(), mag.get(ship.getHullSize()) * 100f)) {
				if (Math.random() < 0.005 * mag.get(ship.getHullSize())) {
					arcstrike(ship, a);
				}
			}
		}
	}

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		if (!stats.getVariant().hasHullMod(HullMods.PHASE_FIELD)) {
			stats.getHullDamageTakenMult().modifyPercent(id, -hullDamageTakenMult); //结构受损效果降低
		}
		//Change Weapon-By Siren
		ShipVariantAPI v = stats.getVariant();
		Set<String> MapKey = WeaponIndAdaptionMap.keySet();
		for (String slotId : v.getNonBuiltInWeaponSlots()) {
			String wpnId = v.getWeaponId(slotId);
			if (wpnId != null) {
				if (MapKey.contains(wpnId)) {
					v.clearSlot(slotId);
					v.addWeapon(slotId, WeaponIndAdaptionMap.get(wpnId));
				}
			}
		}

	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {   //输出全部至文字
		if (index == 0) return "蒸发";
		if (index == 1) return (int)hullDamageTakenMult + "%";
		return null;
	}

	//更多的描述拓展
	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		//tooltip.addSectionHeading("相位舰船", Alignment.TMID, 5f);
		//tooltip.addPara("在进入P空间时会 蒸发 部分舰船幅能。",  IIRT_Omega_Color.IIRT_Omega_Lab_Word,4f);
		//tooltip.addSectionHeading("非相位舰船", Alignment.TMID, 5f);
		//tooltip.addPara("利用反冲系统精准控制舰船和外界的P空间立场影响层，从而一定程度上令舰船对外界投射物产生阻力。", IIRT_Omega_Color.IIRT_Omega_Lab_Word, 4f);
		//tooltip.addPara("使舰船结构额外获得 %s 的伤害抗性。", IIRT_Omega_Color.IIRT_Omega_Lab_Word, 4f);
		tooltip.addSectionHeading("备注", Alignment.TMID, 5f);
		tooltip.addPara("此插件为 克劳斯姆概念设计局 的核心插件。", IIRT_Omega_Color.IIRT_Omega_Lab_Word, 4f);
		//tooltip.addPara(" ", IIRT_Omega_Color.IIRT_Omega_Lab_Name, 4f);
		tooltip.addPara("舰船被摧毁后，独特的幅能泵送系统会损坏，大量幅能被迫释放而无法依靠P空间进行消除，最终将导致幅能暴力释放；", IIRT_Omega_Color.IIRT_Omega_Lab_Word, 4f);
		tooltip.addPara("这过程会使此舰残骸因不同舰船等级和幅能等级，以随机频率对附近不同半径的范围内随机目标产生不同伤害的电击。", IIRT_Omega_Color.IIRT_Omega_Lab_Word, 4f);
		tooltip.addPara("此过程不可控！舰船等级越大，越会容易产生灾难性的后果！", IIRT_Omega_Color.IIRT_Omega_Lab_Warn, 4f);
		//tooltip.addPara(" ", IIRT_Omega_Color.IIRT_Omega_Lab_Name, 4f);
		tooltip.addSectionHeading("小知识", Alignment.MID, 5f);
		tooltip.addPara("逆相构机 - RPCM (Reverse Phase Construction Machine) 是克劳斯姆设计局的主要研究方向，但最近增多的未知舰体遭遇报告逐渐从 RPCM 处吸走了更多的注意力，导致 EPCM 的研究逐渐步入停滞。", IIRT_Omega_Color.IIRT_Omega_Lab_Name, 4f);
		tooltip.addPara("由于设计契合度的原因，部分武器会自动经过额外的改装从而在此舰船上发挥更大的效益。", IIRT_Omega_Color.IIRT_Omega_Cipher, 4f);
		//tooltip.addPara("A型号与B型号无法混用", IIRT_Omega_Color.IIRT_Omega_Lab_Name, 4f);
		//tooltip.addPara("与[%s]冲突", 4f, Misc.getHighlightColor(), IIRT_Omega_Color.IIRT_Omega_Lab_Name, "不可靠子系统");
	}

	// do some crazy weapon changing——By Siren
	public static final Map<String, String> WeaponIndAdaptionMap = new HashMap<>();
	public static final Map<String, String> BaseMap = new HashMap<>();

	private static void putPair(String base, String ind) {
		WeaponIndAdaptionMap.put(base, ind);
		BaseMap.put(ind, base);
	}

	static {
		putPair("lightneedler", "IIRT_Lab_0_lightneedler");
		putPair("heavyneedler", "IIRT_Lab_0_heavyneedler");
		putPair("ioncannon", "IIRT_Lab_0_ioncannon");
		putPair("miningblaster", "IIRT_Lab_0_miningblaster");
		putPair("ionbeam", "IIRT_Lab_0_ionbeam");
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		if (Global.getSector() != null && Global.getSector().getPlayerFleet() != null && Global.getSector().getPlayerFleet().getCargo() != null && Global.getSector().getPlayerFleet().getCargo().getStacksCopy() != null && !Global.getSector().getPlayerFleet().getCargo().getStacksCopy().isEmpty()) {
			Set<String> indKey = BaseMap.keySet();
			for (CargoStackAPI s : Global.getSector().getPlayerFleet().getCargo().getStacksCopy()) {
				if (s.isWeaponStack() && (indKey.contains(s.getWeaponSpecIfWeapon().getWeaponId()))) {
					Global.getSector().getPlayerFleet().getCargo().removeStack(s);
					String base = BaseMap.get(s.getWeaponSpecIfWeapon().getWeaponId());
					if (base != null && ship.getOriginalOwner() < 0) {
						Global.getSector().getPlayerFleet().getCargo().addWeapons(base, Math.round(s.getSize()));
						Global.getLogger(this.getClass()).info("added 1" + base);
					}
				}
			}
		}
	}
}



