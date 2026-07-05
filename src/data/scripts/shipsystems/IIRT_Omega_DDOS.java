package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.awt.Color;

public class IIRT_Omega_DDOS extends BaseShipSystemScript {

	String VARIANTID = "IIRT_Omega_Arrow_Only";

	public static Color JITTER_COLOR = new Color(255, 175, 255, 255);
	public static float JITTER_FADE_TIME = 0.5f;

	public static float SHIP_ALPHA_MULT = 0.25f;
	public static float VULNERABLE_FRACTION = 0f;
	public static float INCOMING_DAMAGE_MULT = 0.25f;

	public static float MAX_TIME_MULT = 3f;

	public static boolean FLUX_LEVEL_AFFECTS_SPEED = true;
	public static float MIN_SPEED_MULT = 0.33f;
	public static float BASE_FLUX_LEVEL_FOR_MIN_SPEED = 0.5f;

	public static float PHASE_CALCULATE_C = 0.2f;

	public static String WEAPONID = "IIRT_Omega_DDOS_Weapon";
	public static String SOUND = "amsrm_fire";

	protected Object STATUSKEY1 = new Object();
	protected Object STATUSKEY2 = new Object();
	protected Object STATUSKEY3 = new Object();
	protected Object STATUSKEY4 = new Object();

	float effectLevelLast = 0;

	public static float getMaxTimeMult(MutableShipStatsAPI stats) {
		return 1f + (MAX_TIME_MULT - 1f) * stats.getDynamic().getValue(Stats.PHASE_TIME_BONUS_MULT);
	}

	protected boolean isDisruptable(ShipSystemAPI cloak) {
		return cloak.getSpecAPI().hasTag(Tags.DISRUPTABLE);
	}

	protected float getDisruptionLevel(ShipAPI ship) {
		if (FLUX_LEVEL_AFFECTS_SPEED) {
			float threshold = ship.getMutableStats().getDynamic().getMod(Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD).computeEffective(BASE_FLUX_LEVEL_FOR_MIN_SPEED);
			if (threshold <= 0) return 1f;
			float level = ship.getHardFluxLevel() / threshold;
			if (level > 1f) level = 1f;
			return level;
		}
		return 0f;
	}

	protected void maintainStatus(ShipAPI playerShip, State state, float effectLevel) {
		float level = effectLevel;
		float f = VULNERABLE_FRACTION;

		ShipSystemAPI cloak = playerShip.getPhaseCloak();
		if (cloak == null) cloak = playerShip.getSystem();
		if (cloak == null) return;

		if (level > f) {
			Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY2, cloak.getSpecAPI().getIconSpriteName(), cloak.getDisplayName(), "time flow altered", false);
		}

		if (FLUX_LEVEL_AFFECTS_SPEED) {
			if (level > f) {
				if (getDisruptionLevel(playerShip) <= 0f) {
					Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY3, cloak.getSpecAPI().getIconSpriteName(), "phase coils stable", "top speed at 100%", false);
				} else {
					String speedPercentStr = Math.round(getSpeedMult(playerShip, effectLevel) * 100f) + "%";
					Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY3, cloak.getSpecAPI().getIconSpriteName(), "phase coil stress", "top speed at " + speedPercentStr, true);
				}
			}
		}
	}

	public float getSpeedMult(ShipAPI ship, float effectLevel) {
		if (getDisruptionLevel(ship) <= 0f) return 1f;
		return MIN_SPEED_MULT + (1f - MIN_SPEED_MULT) * (1f - getDisruptionLevel(ship) * effectLevel);
	}

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI)stats.getEntity();
			player = ship == Global.getCombatEngine().getPlayerShip();
			id = id + "_" + ship.getId();
		} else {
			return;
		}

		if (player) {
			maintainStatus(ship, state, effectLevel);
		}

		if (Global.getCombatEngine().isPaused()) {
			return;
		}

		ShipSystemAPI cloak = ship.getPhaseCloak();
		if (cloak == null) cloak = ship.getSystem();
		if (cloak == null) return;

		if ((state == State.IN && effectLevelLast == 0f)) {
			MissileAPI missile = shootWeapon(ship, WEAPONID, SOUND);
			missile.setCustomData("IIRT_summon_variant", VARIANTID);
		}

		if (state == State.OUT) {
			stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
		} else {
			stats.getMaxSpeed().modifyFlat(id, 60f);
			stats.getAcceleration().modifyFlat(id, 1200f);
		}

		if (FLUX_LEVEL_AFFECTS_SPEED) {
			if (state == State.ACTIVE || state == State.OUT || state == State.IN) {
				float mult = getSpeedMult(ship, effectLevel);
				if (mult < 1f) {
					stats.getMaxSpeed().modifyMult(id + "_2", mult);
				} else {
					stats.getMaxSpeed().unmodifyMult(id + "_2");
				}
				((PhaseCloakSystemAPI)cloak).setMinCoilJitterLevel(getDisruptionLevel(ship));
			}
		}

		if (state == State.COOLDOWN || state == State.IDLE) {
			unapply(stats, id);
			effectLevelLast = 0;
			return;
		}

		float speedPercentMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD).computeEffective(0f);
		float accelPercentMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_ACCEL_MOD).computeEffective(0f);
		stats.getMaxSpeed().modifyPercent(id, speedPercentMod * effectLevel);
		stats.getAcceleration().modifyPercent(id, accelPercentMod * effectLevel);
		stats.getDeceleration().modifyPercent(id, accelPercentMod * effectLevel);

		float speedMultMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD).getMult();
		float accelMultMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_ACCEL_MOD).getMult();
		stats.getMaxSpeed().modifyMult(id, speedMultMod * effectLevel);
		stats.getAcceleration().modifyMult(id, accelMultMod * effectLevel);
		stats.getDeceleration().modifyMult(id, accelMultMod * effectLevel);

		float level = effectLevel;

		float jitterLevel = 0f;
		float jitterRangeBonus = 0f;
		float levelForAlpha = level;

		if (state == State.IN || state == State.ACTIVE) {
			ship.setPhased(true);
			levelForAlpha = level;
		} else if (state == State.OUT) {
			if (level > 0.5f) {
				ship.setPhased(true);
			} else {
				ship.setPhased(false);
			}
			levelForAlpha = level;
		}

		ship.setExtraAlphaMult(1f - (1f - SHIP_ALPHA_MULT) * levelForAlpha);
		ship.setApplyExtraAlphaToEngines(true);

		float extra = 0f;

		float shipTimeMult = 1f + (getMaxTimeMult(stats) - 1f) * levelForAlpha * (1f - extra);
		stats.getTimeMult().modifyMult(id, shipTimeMult);
		if (player) {
			Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);
		} else {
			Global.getCombatEngine().getTimeMult().unmodify(id);
		}

		effectLevelLast = effectLevel;
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {

		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI)stats.getEntity();
		} else {
			return;
		}

		Global.getCombatEngine().getTimeMult().unmodify(id);
		stats.getTimeMult().unmodify(id);

		stats.getMaxSpeed().unmodify(id);
		stats.getMaxSpeed().unmodifyMult(id + "_2");
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);

		ship.setPhased(false);
		ship.setExtraAlphaMult(1f);

		ShipSystemAPI cloak = ship.getPhaseCloak();
		if (cloak == null) cloak = ship.getSystem();
		if (cloak != null) {
			((PhaseCloakSystemAPI)cloak).setMinCoilJitterLevel(0f);
		}
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		return null;
	}

	public static MissileAPI shootWeapon(ShipAPI ship, String weaponID, String sound) {
		SoundPlayerAPI soundplayer = Global.getSoundPlayer();
		WeaponAPI weapon = getspecialWeapon(ship, weaponID);

		MissileAPI missile = (MissileAPI)Global.getCombatEngine().spawnProjectile(ship, weapon, weaponID, weapon.getLocation(), ship.getFacing(), ship.getVelocity());

		soundplayer.playUISound(sound, 1f, 1.5f);

		return missile;
	}

	public static WeaponAPI getspecialWeapon(ShipAPI ship, String weaponID) {
		WeaponAPI specialWeapon = null;
		for (WeaponAPI weapon : ship.getAllWeapons()) {
			if (weapon.getId().contentEquals(weaponID)) {
				specialWeapon = weapon;
			}
		}
		return specialWeapon;
	}
}