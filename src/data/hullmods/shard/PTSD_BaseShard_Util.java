package data.hullmods.shard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PTSD_BaseShard_Util {

    public enum ShardType {
        GENERAL, ANTI_ARMOR, ANTI_SHIELD, POINT_DEFENSE, MISSILE,
    }

    public static class ShardTypeVariants {

        public Map<ShardType, WeightedRandomPicker<String>> variants = new HashMap<>();

        public ShardTypeVariants() {
        }

        public WeightedRandomPicker<String> get(ShardType type) {
            WeightedRandomPicker<String> result = variants.get(type);
            if (result == null) {
                result = new WeightedRandomPicker<>();
                variants.put(type, result);
            }
            return result;
        }
    }
    public static Map<ShipAPI.HullSize, ShardTypeVariants> TranvariantData = new HashMap<>();

    static {
        //——————Tran——————
        ShardTypeVariants fighters = new ShardTypeVariants();
        TranvariantData.put(ShipAPI.HullSize.FIGHTER, fighters);
        fighters.get(ShardType.GENERAL).add("IIRT_Omega_Allusion_attack_wing", 10f);
        fighters.get(ShardType.GENERAL).add("IIRT_Omega_Allusion_missile_wing", 1f);

        fighters.get(ShardType.MISSILE).add("IIRT_Omega_Allusion_missile_wing", 10f);

        fighters.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Allusion_attack_wing", 10f);

        fighters.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Allusion_shieldbreaker_wing", 10f);

        fighters.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Allusion_shock_wing", 10f);

        fighters.get(ShardType.GENERAL).add("IIRT_Omega_Record_attack_wing", 10f);
        fighters.get(ShardType.GENERAL).add("IIRT_Omega_Record_missile_wing", 1f);

        fighters.get(ShardType.MISSILE).add("IIRT_Omega_Record_missile_wing", 10f);
        fighters.get(ShardType.MISSILE).add("aspect_missile_wing", 10f);

        fighters.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Record_attack_wing", 10f);

        fighters.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Record_shieldbreaker_wing", 10f);

        fighters.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Record_shock_wing", 10f);
        fighters.get(ShardType.POINT_DEFENSE).add("aspect_shock_wing", 10f);

        ShardTypeVariants small = new ShardTypeVariants();
        TranvariantData.put(ShipAPI.HullSize.FRIGATE, small);

        small.get(ShardType.GENERAL).add("IIRT_Omega_Arrow_Only", 10f);
        small.get(ShardType.GENERAL).add("IIRT_Omega_Arrow_Only_3", 10f);
        small.get(ShardType.GENERAL).add("IIRT_Omega_Arrow_Only_7", 10f);
        small.get(ShardType.GENERAL).add("IIRT_Omega_Sinus_Only_3", 10f);
        small.get(ShardType.GENERAL).add("IIRT_Omega_Crack_Only2", 10f);
        small.get(ShardType.GENERAL).add("IIRT_Omega_Sinus_Only_2", 10f);
        small.get(ShardType.GENERAL).add("IIRT_Omega_Allusion_attack_wing", 2f);
        fighters.get(ShardType.GENERAL).add("IIRT_Omega_Record_attack_wing", 2f);
        small.get(ShardType.GENERAL).add("IIRT_Omega_Allusion_missile_wing", 1f);
        fighters.get(ShardType.GENERAL).add("IIRT_Omega_Record_missile_wing", 1f);
        small.get(ShardType.GENERAL).add("aspect_missile_wing", 1f);

        small.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Arrow_Only_2", 10f);
        small.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Sinus_Only_4", 10f);

        small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Arrow_Only_4", 10f);
        small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Arrow_Only_5", 10f);
        small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Crack_Only_2", 10f);
        small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Sinus_Only", 10f);

        small.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Crack_Only_3", 10f);
        small.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Arrow_Only_6", 10f);
        fighters.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Record_shock_wing", 4f);
        fighters.get(ShardType.POINT_DEFENSE).add("aspect_shock_wing", 4f);

        small.get(ShardType.MISSILE).add("IIRT_Omega_Crack_Only", 10f);
        small.get(ShardType.MISSILE).add("IIRT_Omega_Sinus_Only_5", 10f);

        small.get(ShardType.GENERAL).add("shard_left_Attack2", 3f);
        small.get(ShardType.GENERAL).add("shard_right_Attack", 3f);
        small.get(ShardType.ANTI_ARMOR).add("shard_left_Armorbreaker", 3f);
        small.get(ShardType.ANTI_SHIELD).add("shard_left_Shieldbreaker", 3f);
        small.get(ShardType.ANTI_SHIELD).add("shard_right_Shieldbreaker", 3f);
        small.get(ShardType.POINT_DEFENSE).add("shard_left_Defense", 3f);
        small.get(ShardType.POINT_DEFENSE).add("shard_right_Shock", 3f);
        small.get(ShardType.MISSILE).add("shard_left_Missile", 3f);
        small.get(ShardType.MISSILE).add("shard_right_Missile", 3f);

        small.get(ShardType.GENERAL).add("IIRT_Omega_Cosinus_Only_4", 2f);
        small.get(ShardType.GENERAL).add("IIRT_Omega_Cosinus_Only_3", 1f);

        small.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Cosinus_Only", 1f);

        small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Cosinus_Only_2", 1f);

        ShardTypeVariants medium = new ShardTypeVariants();
        TranvariantData.put(ShipAPI.HullSize.DESTROYER, medium);

        medium.get(ShardType.GENERAL).add("IIRT_Omega_Cosinus_Only_4", 10f);
        medium.get(ShardType.GENERAL).add("IIRT_Omega_Cosinus_Only_3", 10f);

        medium.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Cosinus_Only", 10f);

        medium.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Cosinus_Only_2", 10f);

        //medium.get(ShardType.GENERAL).add("facet_Attack", 3f);
        //medium.get(ShardType.GENERAL).add("facet_Attack2", 3f);
        //medium.get(ShardType.ANTI_ARMOR).add("facet_Armorbreaker", 3f);
        //medium.get(ShardType.ANTI_SHIELD).add("facet_Shieldbreaker", 3f);
        //medium.get(ShardType.POINT_DEFENSE).add("facet_Defense", 3f);
        //medium.get(ShardType.MISSILE).add("facet_Missile", 3f);

        ShardTypeVariants large = new ShardTypeVariants();
        TranvariantData.put(ShipAPI.HullSize.CRUISER, large);

        large.get(ShardType.GENERAL).add("IIRT_Omega_Tangento_Only_3", 5f);

        large.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Tangento_Only_2", 5f);

        large.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Cube_Lazer_Plus", 3f);
        large.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Cube_Shock", 3f);

        large.get(ShardType.MISSILE).add("IIRT_Omega_Tangento_Only", 5f);

        large.get(ShardType.GENERAL).add("IIRT_Omega_Firewall_Only_3", 3f);
        large.get(ShardType.GENERAL).add("IIRT_Omega_Antitrack_Only_2", 3f);
        large.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Firewall_Only_2", 3f);
        large.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Firewall_Only", 3f);
        large.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Antitrack_Only_4", 3f);
        large.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Antitrack_Only_3", 3f);
        large.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Antitrack_Only2", 3f);
        large.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Tangento_Only2", 5f);
        large.get(ShardType.MISSILE).add("IIRT_Omega_Firewall_Only_4", 3f);
        large.get(ShardType.MISSILE).add("IIRT_Omega_Antitrack_Only", 3f);
    }

    public static Map<ShipAPI.HullSize, ShardTypeVariants> CubevariantData = new HashMap<>();

    static {
        ShardTypeVariants fighters = new ShardTypeVariants();
        CubevariantData.put(ShipAPI.HullSize.FIGHTER, fighters);
        fighters.get(ShardType.GENERAL).add("IIRT_Omega_Allusion_attack_wing");
        fighters.get(ShardType.GENERAL).add("IIRT_Omega_Allusion_missile_wing");

        fighters.get(ShardType.MISSILE).add("IIRT_Omega_Allusion_missile_wing");

        fighters.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Allusion_attack_wing");

        fighters.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Allusion_shieldbreaker_wing");

        fighters.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Allusion_shock_wing");

        fighters.get(ShardType.GENERAL).add("IIRT_Omega_Record_attack_wing");
        fighters.get(ShardType.GENERAL).add("IIRT_Omega_Record_missile_wing");

        fighters.get(ShardType.MISSILE).add("IIRT_Omega_Record_missile_wing");
        fighters.get(ShardType.MISSILE).add("aspect_missile_wing");

        fighters.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Record_attack_wing");

        fighters.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Record_shieldbreaker_wing");

        fighters.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Record_shock_wing");
        fighters.get(ShardType.POINT_DEFENSE).add("aspect_shock_wing");

        ShardTypeVariants small = new ShardTypeVariants();
        CubevariantData.put(ShipAPI.HullSize.FRIGATE, small);

        small.get(ShardType.GENERAL).add("IIRT_Omega_Arrow_Only");
        small.get(ShardType.GENERAL).add("IIRT_Omega_Arrow_Only_3");
        small.get(ShardType.GENERAL).add("IIRT_Omega_Arrow_Only_7");
        small.get(ShardType.GENERAL).add("IIRT_Omega_Sinus_Only_3");
        small.get(ShardType.GENERAL).add("IIRT_Omega_Crack_Only2");
        small.get(ShardType.GENERAL).add("IIRT_Omega_Sinus_Only_2");
        small.get(ShardType.GENERAL).add("aspect_missile_wing");

        small.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Arrow_Only_2");
        small.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Sinus_Only_4");

        small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Arrow_Only_4");
        small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Arrow_Only_5");
        small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Crack_Only_2");
        small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Sinus_Only");

        small.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Crack_Only_3");
        small.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Arrow_Only_6");

        small.get(ShardType.MISSILE).add("IIRT_Omega_Crack_Only");
        small.get(ShardType.MISSILE).add("IIRT_Omega_Sinus_Only_5");

        small.get(ShardType.GENERAL).add("shard_left_Attack2");
        small.get(ShardType.GENERAL).add("shard_right_Attack");
        small.get(ShardType.ANTI_ARMOR).add("shard_left_Armorbreaker");
        small.get(ShardType.ANTI_SHIELD).add("shard_left_Shieldbreaker");
        small.get(ShardType.ANTI_SHIELD).add("shard_right_Shieldbreaker");
        small.get(ShardType.POINT_DEFENSE).add("shard_left_Defense");
        small.get(ShardType.POINT_DEFENSE).add("shard_right_Shock");
        small.get(ShardType.MISSILE).add("shard_left_Missile");
        small.get(ShardType.MISSILE).add("shard_right_Missile");

        small.get(ShardType.GENERAL).add("IIRT_Omega_Cosinus_Only_4");
        small.get(ShardType.GENERAL).add("IIRT_Omega_Cosinus_Only_3");

        small.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Cosinus_Only");

        small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Cosinus_Only_2");

        small.get(ShardType.GENERAL).add("IIRT_Omega_Gateway_Only2");
        small.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Gateway_Only5");
        small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Gateway_Only4");
        small.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Gateway_Only");
        small.get(ShardType.MISSILE).add("IIRT_Omega_Gateway_Only3");

        ShardTypeVariants medium = new ShardTypeVariants();
        CubevariantData.put(ShipAPI.HullSize.DESTROYER, medium);

        medium.get(ShardType.GENERAL).add("IIRT_Omega_Cosinus_Only_4");
        medium.get(ShardType.GENERAL).add("IIRT_Omega_Cosinus_Only_3");

        medium.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Cosinus_Only");

        medium.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Cosinus_Only_2");

        //medium.get(ShardType.GENERAL).add("facet_Attack");
        //medium.get(ShardType.GENERAL).add("facet_Attack2");
        //medium.get(ShardType.ANTI_ARMOR).add("facet_Armorbreaker");
        //medium.get(ShardType.ANTI_SHIELD).add("facet_Shieldbreaker");
        //medium.get(ShardType.POINT_DEFENSE).add("facet_Defense");
        //medium.get(ShardType.MISSILE).add("facet_Missile");
        small.get(ShardType.GENERAL).add("IIRT_Omega_Watcher_Support");
        small.get(ShardType.GENERAL).add("IIRT_Omega_Watcher_Support");


        small.get(ShardType.GENERAL).add("IIRT_Omega_Gateway_Only2");
        small.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Gateway_Only5");
        small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Gateway_Only4");
        small.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Gateway_Only");
        small.get(ShardType.MISSILE).add("IIRT_Omega_Gateway_Only3");

        ShardTypeVariants large = new ShardTypeVariants();
        CubevariantData.put(ShipAPI.HullSize.CRUISER, large);

        large.get(ShardType.GENERAL).add("IIRT_Omega_Tangento_Only_3");

        large.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Tangento_Only_2");

        large.get(ShardType.MISSILE).add("IIRT_Omega_Tangento_Only");

        large.get(ShardType.GENERAL).add("IIRT_Omega_Firewall_Only_3");
        large.get(ShardType.GENERAL).add("IIRT_Omega_Antitrack_Only_2");
        large.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Firewall_Only_2");
        large.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Firewall_Only");
        large.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Antitrack_Only_4");
        large.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Antitrack_Only_3");
        large.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Antitrack_Only2");
        large.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Tangento_Only2");
        large.get(ShardType.MISSILE).add("IIRT_Omega_Firewall_Only_4");
        large.get(ShardType.MISSILE).add("IIRT_Omega_Antitrack_Only");

        //large.get(ShardType.GENERAL).add("tesseract_Attack");
        //large.get(ShardType.GENERAL).add("tesseract_Attack2");
        //large.get(ShardType.GENERAL).add("tesseract_Strike");
        //large.get(ShardType.GENERAL).add("tesseract_Disruptor");

        //large.get(ShardType.ANTI_ARMOR).add("tesseract_Disruptor");
        //large.get(ShardType.ANTI_ARMOR).add("tesseract_Strike");
        //large.get(ShardType.ANTI_SHIELD).add("tesseract_Shieldbreaker");
        //large.get(ShardType.POINT_DEFENSE).add("tesseract_Defense");
        //large.get(ShardType.MISSILE).add("tesseract_Strike");
    }

    public static Map<ShipAPI.HullSize, ShardTypeVariants> WebvariantData = new HashMap<>();

    static {
        ShardTypeVariants fighters = new ShardTypeVariants();
        WebvariantData.put(ShipAPI.HullSize.FIGHTER, fighters);
        fighters.get(ShardType.GENERAL).add("IIRT_Omega_Allusion_attack_wing", 10f);
        fighters.get(ShardType.GENERAL).add("IIRT_Omega_Allusion_missile_wing", 1f);

        fighters.get(ShardType.MISSILE).add("IIRT_Omega_Allusion_missile_wing", 10f);

        fighters.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Allusion_attack_wing", 10f);

        fighters.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Allusion_shieldbreaker_wing", 10f);

        fighters.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Allusion_shock_wing", 10f);

        fighters.get(ShardType.GENERAL).add("IIRT_Omega_Record_attack_wing", 30f);
        fighters.get(ShardType.GENERAL).add("IIRT_Omega_Record_missile_wing", 3f);

        fighters.get(ShardType.MISSILE).add("IIRT_Omega_Record_missile_wing", 30f);
        fighters.get(ShardType.MISSILE).add("aspect_missile_wing", 10f);

        fighters.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Record_attack_wing", 30f);

        fighters.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Record_shieldbreaker_wing", 30f);

        fighters.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Record_shock_wing", 30f);
        fighters.get(ShardType.POINT_DEFENSE).add("aspect_shock_wing", 10f);

        ShardTypeVariants small = new ShardTypeVariants();
        WebvariantData.put(ShipAPI.HullSize.FRIGATE, small);

        small.get(ShardType.GENERAL).add("IIRT_Omega_Arrow_Only", 10f);
        small.get(ShardType.GENERAL).add("IIRT_Omega_Arrow_Only_3", 10f);
        small.get(ShardType.GENERAL).add("IIRT_Omega_Arrow_Only_7", 10f);
        small.get(ShardType.GENERAL).add("IIRT_Omega_Proxy_Only2", 50f);
        small.get(ShardType.GENERAL).add("IIRT_Omega_Crack_Only2", 10f);
        small.get(ShardType.GENERAL).add("IIRT_Omega_Allusion_attack_wing", 3f);
        small.get(ShardType.GENERAL).add("IIRT_Omega_Kb_Only", 3f);
        small.get(ShardType.GENERAL).add("IIRT_Omega_Bit_Only", 3f);

        small.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Arrow_Only_2", 10f);

        small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Arrow_Only_4", 10f);
        small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Arrow_Only_5", 10f);
        small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Crack_Only_2", 10f);
        small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Proxy_Only", 50f);

        small.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Crack_Only_3", 10f);
        small.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Arrow_Only_6", 10f);
        small.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Proxy_Only3", 50f);

        small.get(ShardType.MISSILE).add("IIRT_Omega_Crack_Only", 10f);
        small.get(ShardType.MISSILE).add("IIRT_Omega_Proxy_Only4", 50f);

        small.get(ShardType.GENERAL).add("shard_left_Attack2", 3f);
        small.get(ShardType.GENERAL).add("shard_right_Attack", 3f);
        small.get(ShardType.ANTI_ARMOR).add("shard_left_Armorbreaker", 3f);
        small.get(ShardType.ANTI_SHIELD).add("shard_left_Shieldbreaker", 3f);
        small.get(ShardType.ANTI_SHIELD).add("shard_right_Shieldbreaker", 3f);
        small.get(ShardType.POINT_DEFENSE).add("shard_left_Defense", 3f);
        small.get(ShardType.POINT_DEFENSE).add("shard_right_Shock", 3f);
        small.get(ShardType.MISSILE).add("shard_left_Missile", 3f);
        small.get(ShardType.MISSILE).add("shard_right_Missile", 3f);

        small.get(ShardType.GENERAL).add("IIRT_Omega_Gateway_Only2", 5f);

        small.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Gateway_Only5", 5f);

        small.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Gateway_Only", 5f);

        small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Gateway_Only4", 5f);

        small.get(ShardType.MISSILE).add("IIRT_Omega_Gateway_Only3", 5f);

        ShardTypeVariants medium = new ShardTypeVariants();
        WebvariantData.put(ShipAPI.HullSize.DESTROYER, medium);

        medium.get(ShardType.GENERAL).add("IIRT_Omega_Watcher_Support", 60f);
        medium.get(ShardType.GENERAL).add("IIRT_Omega_Gateway_Only2", 50f);
        medium.get(ShardType.GENERAL).add("IIRT_Omega_AttackChain_Assault", 50f);

        medium.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_AttackChain_AntiArmor", 50f);
        medium.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Gateway_Only5", 50f);

        medium.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Gateway_Only4", 50f);
        medium.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_AttackChain_AntiShield", 50f);

        medium.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Gateway_Only", 50f);
        medium.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_AttackChain_PD", 50f);

        medium.get(ShardType.MISSILE).add("IIRT_Omega_AttackChain_Missile", 50f);
        medium.get(ShardType.MISSILE).add("IIRT_Omega_Gateway_Only3", 50f);

        medium.get(ShardType.GENERAL).add("facet_Attack", 3f);
        medium.get(ShardType.GENERAL).add("facet_Attack2", 3f);
        medium.get(ShardType.ANTI_ARMOR).add("facet_Armorbreaker", 3f);
        medium.get(ShardType.ANTI_SHIELD).add("facet_Shieldbreaker", 3f);
        medium.get(ShardType.POINT_DEFENSE).add("facet_Defense", 3f);
        medium.get(ShardType.MISSILE).add("facet_Missile", 3f);

        ShardTypeVariants large = new ShardTypeVariants();
        WebvariantData.put(ShipAPI.HullSize.CRUISER, large);

        large.get(ShardType.GENERAL).add("IIRT_Omega_Firewall_Only_3", 50f);
        large.get(ShardType.GENERAL).add("IIRT_Omega_Antitrack_Only_2", 50f);

        large.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Firewall_Only_2", 50f);

        large.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Firewall_Only", 50f);
        large.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Antitrack_Only_4", 50f);

        large.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Antitrack_Only_3", 50f);
        large.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Antitrack_Only2", 50f);
        large.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Firewall_Only_5", 50f);

        large.get(ShardType.MISSILE).add("IIRT_Omega_Firewall_Only_4", 50f);
        large.get(ShardType.MISSILE).add("IIRT_Omega_Antitrack_Only", 50f);

        large.get(ShardType.GENERAL).add("tesseract_Attack", 1f);
        large.get(ShardType.GENERAL).add("tesseract_Attack2", 1f);
        large.get(ShardType.GENERAL).add("tesseract_Strike", 1f);
        large.get(ShardType.GENERAL).add("tesseract_Disruptor", 1f);
        large.get(ShardType.ANTI_ARMOR).add("tesseract_Disruptor", 1f);
        large.get(ShardType.ANTI_ARMOR).add("tesseract_Strike", 1f);
        large.get(ShardType.ANTI_SHIELD).add("tesseract_Shieldbreaker", 1f);
        large.get(ShardType.POINT_DEFENSE).add("tesseract_Defense", 1f);
        large.get(ShardType.MISSILE).add("tesseract_Strike", 1f);

        ShardTypeVariants huge = new ShardTypeVariants();
        WebvariantData.put(ShipAPI.HullSize.CAPITAL_SHIP, huge);

        huge.get(ShardType.GENERAL).add("Omega_EPP_Lazer", 10f);
        huge.get(ShardType.ANTI_ARMOR).add("Omega_EPP_Lazer", 10f);
        huge.get(ShardType.ANTI_SHIELD).add("Omega_EPP_Lazer", 10f);
        huge.get(ShardType.POINT_DEFENSE).add("Omega_EPP_Lazer", 10f);
        huge.get(ShardType.MISSILE).add("Omega_EPP_Lazer", 10f);
    }

    public static Map<ShipAPI.HullSize, ShardTypeVariants> BugvariantData = new HashMap<>();

    static {
        ShardTypeVariants fighters = new ShardTypeVariants();
        BugvariantData.put(ShipAPI.HullSize.FIGHTER, fighters);
        fighters.get(ShardType.GENERAL).add("IIRT_Omega_Pupal_attack_wing", 10f);
        fighters.get(ShardType.GENERAL).add("IIRT_Omega_Pupal_missile_wing", 1f);
        fighters.get(ShardType.GENERAL).add("IIRT_Omega_Pupal_point_wing", 1f);
        fighters.get(ShardType.MISSILE).add("IIRT_Omega_Pupal_missile_wing", 10f);
        fighters.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Pupal_shieldbreaker_wing", 10f);
        fighters.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Pupal_shock_wing", 10f);

		/*
		fighters.get(ShardType.GENERAL).add("IIRT_Omega_Record_attack_wing", 2f);
		fighters.get(ShardType.MISSILE).add("IIRT_Omega_Record_missile_wing", 2f);
		fighters.get(ShardType.MISSILE).add("aspect_missile_wing", 2f);
		fighters.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Record_shieldbreaker_wing", 2f);
		fighters.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Record_shock_wing", 2f);
		fighters.get(ShardType.POINT_DEFENSE).add("aspect_shock_wing", 2f);
		 */

        ShardTypeVariants small = new ShardTypeVariants();
        BugvariantData.put(ShipAPI.HullSize.FRIGATE, small);
		/*
		small.get(ShardType.GENERAL).add("IIRT_Omega_Tranquil_Normal_2", 10f);
		small.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Tranquil_Normal_4", 10f);
		small.get(ShardType.MISSILE).add("IIRT_Omega_Tranquil_Normal_1", 10f);
		small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Tranquil_Normal_3", 10f);

		small.get(ShardType.GENERAL).add("IIRT_Omega_Riots_Normal_1", 10f);
		small.get(ShardType.GENERAL).add("IIRT_Omega_Riots_Normal_6", 3f);
		small.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Riots_Normal_3", 10f);
		small.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Riots_Normal_5", 10f);
		small.get(ShardType.MISSILE).add("IIRT_Omega_Riots_Normal_2", 10f);
		small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Riots_Normal_4", 10f);

		small.get(ShardType.GENERAL).add("IIRT_Omega_Torsion_2_Normal_2", 10f);
		small.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Torsion_2_Normal_3", 10f);
		small.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Torsion_2_Normal", 10f);
		small.get(ShardType.MISSILE).add("IIRT_Omega_Torsion_2_Normal_5", 10f);
		small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Torsion_2_Normal_4", 10f);

		small.get(ShardType.GENERAL).add("IIRT_Omega_Bustle_Normal_5", 3f);
		small.get(ShardType.GENERAL).add("IIRT_Omega_Bustle_Normal_6", 10f);
		small.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Bustle_Normal_4", 10f);
		small.get(ShardType.MISSILE).add("IIRT_Omega_Bustle_Normal_1", 10f);
		small.get(ShardType.GENERAL).add("IIRT_Omega_Bustle_Normal_2", 10f);
		small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Bustle_Normal_3", 10f);

		small.get(ShardType.GENERAL).add("IIRT_Omega_Deplorable_Normal_1", 10f);
		small.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Deplorable_Normal_2", 10f);
		small.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Deplorable_Normal_3", 10f);
		small.get(ShardType.MISSILE).add("IIRT_Omega_Deplorable_Normal_4", 10f);
		small.get(ShardType.GENERAL).add("IIRT_Omega_Deplorable_Normal_5", 10f);
		 */

        small.get(ShardType.GENERAL).add("IIRT_Omega_Inner_Normal_1", 20f);
        small.get(ShardType.GENERAL).add("IIRT_Omega_Inspect_Normal_1", 5f);
        small.get(ShardType.GENERAL).add("IIRT_Omega_Inspect_Normal_4", 5f);
        small.get(ShardType.GENERAL).add("IIRT_Omega_Inspect_Normal_2", 5f);
        small.get(ShardType.GENERAL).add("IIRT_Omega_Inspect_Normal_3", 5f);

        small.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Inspect_Normal_4", 30f);
        small.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Inspect_Normal_2", 30f);
        small.get(ShardType.MISSILE).add("IIRT_Omega_Inspect_Normal_1", 30f);


        ShardTypeVariants medium = new ShardTypeVariants();
        BugvariantData.put(ShipAPI.HullSize.DESTROYER, medium);

        medium.get(ShardType.GENERAL).add("IIRT_Omega_Boltzmann_Assault", 20f);

        medium.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Boltzmann_Anti_Armor", 20f);

        medium.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Boltzmann_AntiShield", 20f);

        medium.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Boltzmann_Point_Defense", 20f);

        medium.get(ShardType.MISSILE).add("IIRT_Omega_Boltzmann_Missile", 20f);

        ShardTypeVariants large = new ShardTypeVariants();
        BugvariantData.put(ShipAPI.HullSize.CRUISER, large);

        large.get(ShardType.GENERAL).add("IIRT_Omega_DevilFork_Attack", 40f);
        //large.get(ShardType.GENERAL).add("IIRT_Omega_Firewall_Only_3", 10f);
        //large.get(ShardType.GENERAL).add("IIRT_Omega_Antitrack_Only_2", 10f);

        large.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_DevilFork_AntiArmor", 40f);

        large.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_DevilFork_AntiShield", 40f);

        large.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_DevilFork_Support", 40f);

        large.get(ShardType.MISSILE).add("IIRT_Omega_DevilFork_Missile", 40f);
        //large.get(ShardType.MISSILE).add("IIRT_Omega_Firewall_Only_4", 10f);
        //large.get(ShardType.MISSILE).add("IIRT_Omega_Antitrack_Only", 10f);
/*
		large.get(ShardType.GENERAL).add("tesseract_Attack", 1f);
		large.get(ShardType.GENERAL).add("tesseract_Attack2", 1f);
		large.get(ShardType.GENERAL).add("tesseract_Strike", 1f);
		large.get(ShardType.GENERAL).add("tesseract_Disruptor", 1f);
		large.get(ShardType.ANTI_ARMOR).add("tesseract_Disruptor", 1f);
		large.get(ShardType.ANTI_ARMOR).add("tesseract_Strike", 1f);
		large.get(ShardType.ANTI_SHIELD).add("tesseract_Shieldbreaker", 1f);
		large.get(ShardType.POINT_DEFENSE).add("tesseract_Defense", 1f);
		large.get(ShardType.MISSILE).add("tesseract_Strike", 1f);

 */
    }

    //———— = PTSD FORCE GENERATOR = ————

    public static void spawnRandomPTSDForce(FleetParamsV3 fleet, int spawnPoints){
        int Flt = MathUtils.getRandomNumberInRange(1,4);
        Map<ShipAPI.HullSize, PTSD_BaseShard_Util.ShardTypeVariants> fac = BugvariantData;
        if(Flt==1){fac = TranvariantData;}
        if(Flt==2){fac = CubevariantData;}
        if(Flt==3){fac = WebvariantData;}
        if(Flt==4){fac = BugvariantData;}

        assignShardBeforeSpawnFleet(fleet, fac, spawnPoints);
    }

    public static void assignShardBeforeSpawnFleet(FleetParamsV3 Para, Map<ShipAPI.HullSize, ShardTypeVariants> variantData, float spawnPoints) {
        if (Para == null) return;
        try {
            // clear any auto-generated members and fill the fleet based on variantData
            int remaining = (int) Math.max(0, spawnPoints);
            int safety = 0;
            while (remaining > 0 && safety < 200) {
                safety++;
                ShipAPI.HullSize size;
                int cost;
                if (remaining > 250 || variantData.get(ShipAPI.HullSize.CAPITAL_SHIP) == null) {
                    size = ShipAPI.HullSize.CAPITAL_SHIP; cost = 70;
                }
                if (remaining > 80) { size = ShipAPI.HullSize.CRUISER; cost = 55; }
                else if (remaining > 30) { size = ShipAPI.HullSize.DESTROYER; cost = 20; }
                else if (remaining >= 10) { size = ShipAPI.HullSize.FRIGATE; cost = 7; }
                else { size = ShipAPI.HullSize.FIGHTER; cost = 3; }

                String variant = pickVariantForSize(size, variantData);
                if (variant == null) {
                    // try fallbacks
                    if (size != ShipAPI.HullSize.FIGHTER && size != ShipAPI.HullSize.DEFAULT) variant = pickVariantForSize(size, variantData);
                    if (variant == null) variant = pickVariantForSize(ShipAPI.HullSize.FIGHTER, variantData);
                }

                remaining -= cost;
                Para.addShips.add(variant);
            }
        } catch (Throwable t) {
            Global.getLogger(PTSD_BaseShard_Util.class).info("assignShardSpawnToFleet failed: " + t.getMessage());
        }
    }
    public static void assignShardAfterSpawnFleet(CampaignFleetAPI fleet, Map<ShipAPI.HullSize, ShardTypeVariants> variantData, int spawnPoints) {
        if (fleet == null) return;
        try {
            // clear any auto-generated members and fill the fleet based on variantData
            fleet.getFleetData().clear();
            int remaining = Math.max(0, spawnPoints);
            int safety = 0;
            while (remaining > 0 && safety < 200) {
                safety++;
                ShipAPI.HullSize size;
                int cost;
                if (remaining > 250 || variantData.get(ShipAPI.HullSize.CAPITAL_SHIP) == null) {
                    size = ShipAPI.HullSize.CAPITAL_SHIP; cost = 70;
                }
                if (remaining > 80) { size = ShipAPI.HullSize.CRUISER; cost = 55; }
                else if (remaining > 30) { size = ShipAPI.HullSize.DESTROYER; cost = 20; }
                else if (remaining >= 10) { size = ShipAPI.HullSize.FRIGATE; cost = 7; }
                else { size = ShipAPI.HullSize.FIGHTER; cost = 3; }

                String variant = pickVariantForSize(size, variantData);
                if (variant == null) {
                    // try fallbacks
                    if (size != ShipAPI.HullSize.FIGHTER && size != ShipAPI.HullSize.DEFAULT) variant = pickVariantForSize(size, variantData);
                    if (variant == null) variant = pickVariantForSize(ShipAPI.HullSize.FIGHTER, variantData);
                }
                if (variant == null) break; // nothing available for this size

                fleet.getFleetData().addFleetMember(variant);
                remaining -= cost;
            }
        } catch (Throwable t) {
            Global.getLogger(PTSD_BaseShard_Util.class).info("assignShardSpawnToFleet failed: " + t.getMessage());
        }
    }

    private static String pickVariantForSize(ShipAPI.HullSize size, Map<ShipAPI.HullSize, ShardTypeVariants> variantData) {
        ShardTypeVariants variants = variantData.get(size);
        if (variants == null) return null;
        ShardType[] types = ShardType.values();
        // try random attempts first
        for (int i = 0; i < types.length; i++) {
            ShardType t = types[(int) (Math.random() * types.length)];
            String pick = variants.get(t).pick();
            if (pick != null) return pick;
        }
        // then deterministic fallback
        for (ShardType t : types) {
            String pick = variants.get(t).pick();
            if (pick != null) return pick;
        }
        return null;
    }
}
