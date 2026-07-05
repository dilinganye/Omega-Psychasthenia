package data.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.WeaponOPCostModifier;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.utils.iirt_omega.IIRT_Omega_Color;
import org.magiclib.util.MagicAnim;
import org.magiclib.util.MagicUI;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class IIRT_SD extends BaseHullMod {

	/**
	 * This is the center hullmod for IIRT.
	 */

	public static final String IIRT_SD = "IIRT_SD";

	private static Map<HullSize, Float> mag = new HashMap<>(); //不同船型
	static {
		mag.put(HullSize.FIGHTER, 10f);
		mag.put(HullSize.FRIGATE, 10f);
		mag.put(HullSize.DESTROYER, 15f);
		mag.put(HullSize.CRUISER, 20f);
		mag.put(HullSize.CAPITAL_SHIP, 25f);
	}

	private static Map<HullSize, Float> neededDamage = new HashMap<>(); //不同船型
	static {
		neededDamage.put(HullSize.FIGHTER, 10000f);
		neededDamage.put(HullSize.FRIGATE, 20000f);
		neededDamage.put(HullSize.DESTROYER, 30000f);
		neededDamage.put(HullSize.CRUISER, 40000f);
		neededDamage.put(HullSize.CAPITAL_SHIP, 50000f);
	}

	private static Map<HullSize, Float> ArgneededDamage = new HashMap<>(); //不同船型
	static {
		ArgneededDamage.put(HullSize.FIGHTER, 1000f);
		ArgneededDamage.put(HullSize.FRIGATE, 50f);
		ArgneededDamage.put(HullSize.DESTROYER, 100f);
		ArgneededDamage.put(HullSize.CRUISER, 100f);
		ArgneededDamage.put(HullSize.CAPITAL_SHIP, 150f);
	}

	public final float hullDamageTakenMult = 10;

	public static float CREW_PERCENT = 95f;
	public static float SD_One = 1f;
	public static float SD_ShieldEffect = 0.25f;

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		if (!ship.hasListenerOfClass(IIRT_SD_DamageToDealListener.class)) {
			ship.addListener(new IIRT_SD_DamageToDealListener(ship));
		}

		super.applyEffectsAfterShipCreation(ship, id);
	}

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getBeamDamageTakenMult().modifyPercent(id, mag.get(hullSize));
		stats.getCrewLossMult().modifyPercent(id, -CREW_PERCENT);

		if (!stats.hasListenerOfClass(IIRT_SD_WeaponOPCostListener.class)) {
			stats.addListener(new IIRT_SD_WeaponOPCostListener());
		}
	}

	@Override
	public boolean affectsOPCosts() {
		return true;
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		//CombatEngineAPI engine = Global.getCombatEngine();
		if (ship.getShield() != null) {

			//float shieldArcFir = ship.getVariant().getHullSpec().getShieldSpec().getArc();
			//float shieldArc = ship.getShield().getArc();
			float shieldArc = ship.getVariant().getHullSpec().getShieldSpec().getArc() + ship.getMutableStats().getShieldArcBonus().getFlatBonus();

			float shieldEfcFir = ship.getVariant().getHullSpec().getShieldSpec().getFluxPerDamageAbsorbed();
			float shieldEfc = ship.getVariant().getHullSpec().getShieldSpec().getFluxPerDamageAbsorbed() * ship.getMutableStats().getShieldDamageTakenMult().getModifiedValue();
			//float shieldEfc = ship.getShield().getFluxPerPointOfDamage();

			MagicUI.drawInterfaceStatusBar(ship, (Math.min(1f, Math.max(0, shieldEfcFir))), new Color(36, 229, 229, 150), null, (Math.min(1f, Math.max(0, shieldEfc))), "Ef&Ar", (int)shieldArc);
            /*
            Global.getLogger(this.getClass()).info("--=MagicUI=--");
            Global.getLogger(this.getClass()).info("shieldArc=" + shieldArc);
            //Global.getLogger(this.getClass()).info("shieldArcFir=" + shieldArcFir);
            Global.getLogger(this.getClass()).info("shieldEfc=" + shieldEfc);
            Global.getLogger(this.getClass()).info("shieldEfcFir=" + shieldEfcFir);
            Global.getLogger(this.getClass()).info("--=MagicUI=--");

             */
		}
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {   //输出全部至文字
		if (index == 0) return "" + mag.get(HullSize.FRIGATE).intValue() + "%";
		if (index == 1) return "" + mag.get(HullSize.DESTROYER).intValue() + "%";
		if (index == 2) return "" + mag.get(HullSize.CRUISER).intValue() + "%";
		if (index == 3) return "" + mag.get(HullSize.CAPITAL_SHIP).intValue() + "%";
		if (index == 4) return (int)CREW_PERCENT + "%";
		if (index == 5) return (int)SD_One * 2f + "点";
		return null;
	}

	//更多的描述拓展
	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		MutableShipStatsAPI stats = ship.getMutableStats();
		tooltip.addSectionHeading("变量", Alignment.TMID, 5f);
		tooltip.addPara("在新增状态栏中，状态条表示 - 当前盾效以及初始盾效", IIRT_Omega_Color.IIRT_SD_word_Interesting, 4f);
		tooltip.addPara("右侧数字为 - 实际盾角", IIRT_Omega_Color.IIRT_SD_word_Interesting, 4f);
		if (ship.getShield() != null) {
			float neededDamagenumb = (float)neededDamage.get(ship.getHullSize()).intValue();
			float ArgneededDamagenumb = (float)ArgneededDamage.get(ship.getHullSize()).intValue();
			tooltip.addPara("当舰船具备护盾时，获得以下加成 -", IIRT_Omega_Color.IIRT_SD_word_Interesting, 4f);
			tooltip.addPara("根据护盾所受伤害加强盾效，护盾受到的伤害达到总量 %s 时，将获得等效于 %s 的抗性[受到其他减伤因素影响后仍旧保持不变]", 3f, IIRT_Omega_Color.IIRT_Omega_Lab_Math, "" + neededDamagenumb + "点", "" + SD_ShieldEffect + "盾效");
			tooltip.addPara("当舰船受到伤害时，根据船体结构和护甲所受到的伤害加大自身护盾最大盾角，约为每 %s 伤害便增加 %s 的最大盾角。", 3f, IIRT_Omega_Color.IIRT_Omega_Lab_Math, "" + ArgneededDamagenumb + "点", "" + SD_One + "度");
		}
		tooltip.addSectionHeading("备注", Alignment.TMID, 5f);
		tooltip.addPara("此插件为 以西卡研究所 的核心插件。", IIRT_Omega_Color.IIRT_SD_word, 4f);
		tooltip.addPara("舰船内置的伤害自特化适应器能够在大多数情况下逐渐优化针对战场上武器的即时抗性；", IIRT_Omega_Color.IIRT_SD_word, 4f);
		tooltip.addPara("具体表现为随着护盾受到伤害的增多，其盾效会逐渐变得更加优异。", IIRT_Omega_Color.IIRT_SD_word, 4f);
		tooltip.addPara("但很可惜的是 - 盾效的优异并不能解决其大多数舰船的结构异常脆弱所带来的其他严重影响。", IIRT_Omega_Color.IIRT_SD_word_Warn, 4f);
		tooltip.addPara("在装配同样来自以西卡的武器时，会进行一定的装配点需求减免。", IIRT_Omega_Color.IIRT_SD_word_Interesting, 4f);
		tooltip.addSectionHeading("小知识", Alignment.MID, 5f);
		tooltip.addPara("归墟计划是以西卡研究所的主要目的，其可简单概括为对某种特殊遗迹的控制行动，并歼灭可能窥探亦或是扩散其秘密的个人以及势力；", IIRT_Omega_Color.IIRT_SD_word_Interesting, 4f);
		tooltip.addPara("而遗迹具体指什么 - 则不为人知。", IIRT_Omega_Color.IIRT_SD_word_Interesting, 4f);
	}

	public static class IIRT_SD_WeaponOPCostListener implements WeaponOPCostModifier {  //武器有此tag则降低OP需求10

		int costSubtract = 2;

		@Override
		public int getWeaponOPCost(MutableShipStatsAPI stats, WeaponSpecAPI weapon, int currCost) {
			//武器有此tag则降低OP需求2
			if (weapon.hasTag("iirt_sd_10")) {
				return (currCost - costSubtract);
			}
			return currCost;
		}
	}

	public static class IIRT_SD_DamageToDealListener implements DamageListener {

		private ShipAPI ship;
		private ShieldAPI shield;
		private float hullDamage = 0;
		private float hullHits = 0;
		private float shieldDamage = 0;
		private float shieldHits = 0;
		private float recentShieldHits = 0;
		private float recentHullHits = 0;

		public IIRT_SD_DamageToDealListener(ShipAPI ship) {
			this.ship = ship;
			if (ship != null) {
				if (ship.getShield() != null) {
					this.shield = ship.getShield();
				}
			}
		}

		@Override
		public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
			hullHits = (result.getTotalDamageToArmor() + result.getDamageToHull());
			shieldHits = result.getDamageToShields();
			shieldDamage += shieldHits;
			hullDamage += hullHits;
			processShieldArc();
			processShieldDamageTaken();
			float max = 1f;
			if (result.isDps()) max = 0.1f;
			//recentShieldHits += Math.min(max, shieldHits / 10f);
			recentHullHits += Math.min(max, hullHits / 10f);
			processShieldDamageShown();
			processHullDamageShown();
		}

		private void processShieldArc() {
			if (shield != null) {
				float arc = shield.getArc();
				float expectShield = Math.min(360, arc + (hullDamage / ArgneededDamage.get(ship.getHullSize())));
				float bonus = expectShield - arc;
				ship.getMutableStats().getShieldArcBonus().modifyFlat("IIRT_SD_Plugin", bonus);
                /*
                Global.getLogger(this.getClass()).info("--=processShieldArc=--");
                Global.getLogger(this.getClass()).info("arc=" + arc);
                Global.getLogger(this.getClass()).info("expectShield=" + expectShield);
                Global.getLogger(this.getClass()).info("bonus=" + bonus);
                Global.getLogger(this.getClass()).info("--=processShieldArc=--");

                 */

			}
		}

		private void processShieldDamageTaken() {
			if (shield != null) {
				float effectiency = shield.getFluxPerPointOfDamage();
				//装配盾效*effTo025[即额外护盾减伤] = 目标盾效
				float effTo025 = Math.max(SD_ShieldEffect / effectiency, 0);
				//伤害总额进度
				float SD_Shield_Progress = MagicAnim.normalizeRange(shieldDamage, 0, neededDamage.get(ship.getHullSize()));
				float SD_Shield_Progress_Ret = Math.min(Math.max(1 - SD_Shield_Progress, 0), 1);
				//float bonus = (1-SD_Shield_Progress)*effTo025;
				float bonus = MagicAnim.offsetToRange(SD_Shield_Progress_Ret, effTo025, 1);
				if (bonus < effTo025) {
					bonus = effTo025;
				}
				ship.getMutableStats().getShieldDamageTakenMult().modifyMult("IIRT_SD_Plugin", bonus);
                /*
                Global.getLogger(this.getClass()).info("--=processShieldDamageTaken=--");
                Global.getLogger(this.getClass()).info("allShieldDamaged=" + shieldDamage);
                Global.getLogger(this.getClass()).info("effectiency=" + effectiency);
                Global.getLogger(this.getClass()).info("effTo025=" + effTo025);
                Global.getLogger(this.getClass()).info("SD_Shield_Progress=" + SD_Shield_Progress);
                Global.getLogger(this.getClass()).info("bonus=" + bonus);
                Global.getLogger(this.getClass()).info("--=processShieldDamageTaken=--");
                 */
			}
		}

		private void processShieldDamageShown() {
			if (shield != null) {
				float SD_Shieldrender_Progress = Math.min(Math.max(MagicAnim.normalizeRange(shieldDamage, 0, neededDamage.get(ship.getHullSize())), 0), 1);

				ship.getShield().setRingColor(new Color(Math.round(194 + (SD_Shieldrender_Progress * 55)), 100, 231, Math.round(90 + (SD_Shieldrender_Progress * 120))));
				ship.getShield().setInnerColor(new Color(Math.round(120 + (SD_Shieldrender_Progress * 100)), 160, 255, Math.round(90 + (SD_Shieldrender_Progress * 100))));
			}
		}

		private void processHullDamageShown() {
			if (shield != null) {
				recentHullHits -= 0.5f;
				if (recentHullHits < 0) recentHullHits = 0;
				if (recentHullHits > 5f) recentHullHits = 5f;

				float jitterLevel = 1f;
				float jitterRange = 0.5f;
				float maxRangeBonus = 50f;
				maxRangeBonus = 20f;
				maxRangeBonus += recentHullHits * 40f;

				float jitterRangeBonus = jitterRange * maxRangeBonus;

				float numCopiesH = 3f;
				numCopiesH += recentHullHits;

				ship.setJitter(this, new Color(168, 128, 255, 116), jitterLevel, Math.round(numCopiesH), 0f, jitterRangeBonus);
				ship.setJitterShields(false);
			}
		}
	}
    /*class IIRT_SD_WeaponOPCostListener implements WeaponOPCostModifier{  //武器有此tag则降低OP需求10
        int costSubtract = 2;
        @Override
        public int getWeaponOPCost(MutableShipStatsAPI stats,WeaponSpecAPI weapon,int currCost) {
            //武器有此tag则降低OP需求2
            if(weapon.hasTag("iirt_sd_10")){
                return (currCost - costSubtract);
            }
            return currCost;
        }
    }
    class IIRT_SD_DamageToDealListener implements DamageListener {
        public IIRT_SD_Plugin plugin;
        public IIRT_SD_DamageToDealListener() {
            this.plugin = plugin;
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            //根据护盾收到的伤害逐渐优化盾效，根据船体受到的总伤害增大盾角
            float totalDamage = result.getDamageToHull() + result.getTotalDamageToArmor();
            float damageOnShield = result.getDamageToShields();

            float Zero = 0.0F;
            IIRT_SD_Plugin varSD_Damage = this.plugin;
            varSD_Damage.allHits += Math.min(Zero, totalDamage / 100.0F);
            varSD_Damage.shieldAdds += Math.min(Zero, damageOnShield / 10000.0F);
        }
    }

    public class IIRT_SD_Plugin extends BaseEveryFrameCombatPlugin {
        float allHits = 0.0F;
        float shieldAdds = 0.0F;
        float shieldArcNow = 0.0F;
        float shieldArcFirst = 0.0F;

        public void advance(float amount, ShipAPI ship, MutableShipStatsAPI stats, String id) {
            shieldArcFirst = stats.getShieldArcBonus().getFlatBonus();
            shieldArcNow = shieldArcFirst * stats.getShieldArcBonus().getMult();
            if(shieldArcNow <= 360){
                stats.getShieldArcBonus().modifyFlat(id, shieldAdds);
            }
            if(stats.getShieldDamageTakenMult().getModifiedValue() *
                    stats.getVariant().getHullSpec().getShieldSpec().getFluxPerDamageAbsorbed() >=0.25f){
                //stats.getShieldDamageTakenMult().getModifiedValue() * stats.getShieldDamageTakenMult().getBaseValue()
                stats.getShieldDamageTakenMult().modifyMult(id, 1f - shieldAdds);
            }
        }



        private ShipAPI ship;
        private ShieldAPI shield;
        public IIRT_SD_Plugin(ShipAPI ship){
            this.ship = ship;
            if(ship.getShield() != null) {
                this.shield = ship.getShield();
            }
        }
    }*/
}



