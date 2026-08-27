package data.hullmods.shard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PTSD_BaseShard_Util {

    /** Mission family biases branch selection; ship pools are still strictly isolated per fleet. */
    public enum FleetRole {
        GENERAL,
        RECON,
        LOGISTICS_ENGINEERING,
        GUARD_ASSAULT
    }

    public static final String BRANCH_TRAN = "ectoplasm_intervention";
    public static final String BRANCH_CUBE = "entropy_transport";
    public static final String BRANCH_WEB = "network_wraith";
    public static final String BRANCH_BUG = "garbled_carcass";
    public static final String FLEET_BRANCH_MEMORY = "$PTSD_shard_branch";
    public static final String FLEET_BRANCH_NAME_MEMORY = "$PTSD_shard_branch_name";

    /** Public descriptor used by this mod and by optional external branch providers. */
    public static final class BranchDefinition {
        public final String id;
        public final String displayName;
        public final Map<ShipAPI.HullSize, ShardTypeVariants> variants;
        private final EnumMap<FleetRole, Float> roleWeights = new EnumMap<>(FleetRole.class);

        public BranchDefinition(String id, String displayName,
                                Map<ShipAPI.HullSize, ShardTypeVariants> variants) {
            if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("branch id is empty");
            if (variants == null) throw new IllegalArgumentException("branch variants are null");
            this.id = id;
            this.displayName = displayName == null ? id : displayName;
            this.variants = variants;
            for (FleetRole role : FleetRole.values()) roleWeights.put(role, 1f);
        }

        public BranchDefinition setWeight(FleetRole role, float weight) {
            if (role != null) roleWeights.put(role, Math.max(0f, weight));
            return this;
        }

        public float getWeight(FleetRole role) {
            Float value = roleWeights.get(role == null ? FleetRole.GENERAL : role);
            return value == null ? 1f : Math.max(0f, value);
        }
    }

    private static final Map<String, BranchDefinition> BRANCHES = new LinkedHashMap<>();

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
    public static Map<ShipAPI.HullSize, ShardTypeVariants> TranvariantData = new HashMap<>();//介入灵质

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

    public static Map<ShipAPI.HullSize, ShardTypeVariants> CubevariantData = new HashMap<>();//熵级运载

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

    public static Map<ShipAPI.HullSize, ShardTypeVariants> WebvariantData = new HashMap<>();//网络冥魂

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
        /*
        small.get(ShardType.GENERAL).add("shard_left_Attack2", 3f);
        small.get(ShardType.GENERAL).add("shard_right_Attack", 3f);
        small.get(ShardType.ANTI_ARMOR).add("shard_left_Armorbreaker", 3f);
        small.get(ShardType.ANTI_SHIELD).add("shard_left_Shieldbreaker", 3f);
        small.get(ShardType.ANTI_SHIELD).add("shard_right_Shieldbreaker", 3f);
        small.get(ShardType.POINT_DEFENSE).add("shard_left_Defense", 3f);
        small.get(ShardType.POINT_DEFENSE).add("shard_right_Shock", 3f);
        small.get(ShardType.MISSILE).add("shard_left_Missile", 3f);
        small.get(ShardType.MISSILE).add("shard_right_Missile", 3f);*/

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
        medium.get(ShardType.GENERAL).add("IIRT_Omega_Gateway_TALO", 50f);

        medium.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_AttackChain_AntiArmor", 50f);
        medium.get(ShardType.ANTI_ARMOR).add("IIRT_Omega_Gateway_Only5", 50f);

        medium.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_Gateway_Only4", 50f);
        medium.get(ShardType.ANTI_SHIELD).add("IIRT_Omega_AttackChain_AntiShield", 50f);

        medium.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_Gateway_Only", 50f);

        medium.get(ShardType.POINT_DEFENSE).add("IIRT_Omega_AttackChain_PD", 50f);

        medium.get(ShardType.MISSILE).add("IIRT_Omega_AttackChain_Missile", 50f);
        medium.get(ShardType.MISSILE).add("IIRT_Omega_Gateway_Only3", 50f);
        /*
        medium.get(ShardType.GENERAL).add("facet_Attack", 3f);
        medium.get(ShardType.GENERAL).add("facet_Attack2", 3f);
        medium.get(ShardType.ANTI_ARMOR).add("facet_Armorbreaker", 3f);
        medium.get(ShardType.ANTI_SHIELD).add("facet_Shieldbreaker", 3f);
        medium.get(ShardType.POINT_DEFENSE).add("facet_Defense", 3f);
        medium.get(ShardType.MISSILE).add("facet_Missile", 3f);

         */

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

        /* large.get(ShardType.GENERAL).add("tesseract_Attack", 1f);
        large.get(ShardType.GENERAL).add("tesseract_Attack2", 1f);
        large.get(ShardType.GENERAL).add("tesseract_Strike", 1f);
        large.get(ShardType.GENERAL).add("tesseract_Disruptor", 1f);
        large.get(ShardType.ANTI_ARMOR).add("tesseract_Disruptor", 1f);
        large.get(ShardType.ANTI_ARMOR).add("tesseract_Strike", 1f);
        large.get(ShardType.ANTI_SHIELD).add("tesseract_Shieldbreaker", 1f);
        large.get(ShardType.POINT_DEFENSE).add("tesseract_Defense", 1f);
        large.get(ShardType.MISSILE).add("tesseract_Strike", 1f); */

        ShardTypeVariants huge = new ShardTypeVariants();
        WebvariantData.put(ShipAPI.HullSize.CAPITAL_SHIP, huge);

        huge.get(ShardType.GENERAL).add("Omega_EPP_Lazer", 10f);
        huge.get(ShardType.ANTI_ARMOR).add("Omega_EPP_Lazer", 10f);
        huge.get(ShardType.ANTI_SHIELD).add("Omega_EPP_Lazer", 10f);
        huge.get(ShardType.POINT_DEFENSE).add("Omega_EPP_Lazer", 10f);
        huge.get(ShardType.MISSILE).add("Omega_EPP_Lazer", 10f);
    }

    public static Map<ShipAPI.HullSize, ShardTypeVariants> BugvariantData = new HashMap<>();//乱码尸骸

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

    //———— = PTSD BRANCH REGISTRY / FORCE GENERATOR = ————

    static {
        registerBranch(new BranchDefinition(BRANCH_TRAN, "介入灵质", TranvariantData)
                .setWeight(FleetRole.RECON, 5f));
        registerBranch(new BranchDefinition(BRANCH_CUBE, "熵级运载", CubevariantData)
                .setWeight(FleetRole.LOGISTICS_ENGINEERING, 5f));
        registerBranch(new BranchDefinition(BRANCH_WEB, "网络冥魂", WebvariantData)
                .setWeight(FleetRole.GUARD_ASSAULT, 5f));
        // 乱码尸骸始终保持基础权重，不因任务类型获得额外加成。
        registerBranch(new BranchDefinition(BRANCH_BUG, "乱码尸骸", BugvariantData));
    }

    /**
     * Registers or replaces a branch. This is the primary extension API for future content and other mods.
     * The supplied map uses the same hull-size -> role -> variant structure as the four built-in branches.
     */
    public static synchronized BranchDefinition registerBranch(String id, String displayName,
            Map<ShipAPI.HullSize, ShardTypeVariants> variants) {
        return registerBranch(new BranchDefinition(id, displayName, variants));
    }

    public static synchronized BranchDefinition registerBranch(BranchDefinition branch) {
        if (branch == null) throw new IllegalArgumentException("branch is null");
        BRANCHES.put(branch.id, branch);
        return branch;
    }

    public static synchronized BranchDefinition unregisterBranch(String id) {
        return id == null ? null : BRANCHES.remove(id);
    }

    public static synchronized BranchDefinition getBranch(String id) {
        return id == null ? null : BRANCHES.get(id);
    }

    public static synchronized List<BranchDefinition> getRegisteredBranches() {
        return Collections.unmodifiableList(new ArrayList<>(BRANCHES.values()));
    }

    public static synchronized BranchDefinition pickBranch(FleetRole role, Random random) {
        if (BRANCHES.isEmpty()) return null;
        Random rng = random == null ? new Random() : random;
        float total = 0f;
        for (BranchDefinition branch : BRANCHES.values()) total += branch.getWeight(role);
        if (total <= 0f) return BRANCHES.values().iterator().next();
        float roll = rng.nextFloat() * total;
        BranchDefinition fallback = null;
        for (BranchDefinition branch : BRANCHES.values()) {
            fallback = branch;
            roll -= branch.getWeight(role);
            if (roll <= 0f) return branch;
        }
        return fallback;
    }

    /** Creates a fleet containing ships from exactly one branch and records that branch on fleet memory. */
    public static CampaignFleetAPI createFleet(FleetParamsV3 params, float spawnPoints, FleetRole role) {
        return createFleet(params, spawnPoints, role, params == null ? null : params.random);
    }

    public static CampaignFleetAPI createFleet(FleetParamsV3 params, float spawnPoints,
                                                FleetRole role, Random random) {
        BranchDefinition branch = prepareFleetParams(params, spawnPoints, role, random);
        if (branch == null || params.addShips == null || params.addShips.isEmpty()) return null;
        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
        tagFleet(fleet, branch);
        return fleet;
    }

    /**
     * Locks params to one selected branch. Call this before a custom FleetFactoryV3.createFleet() flow.
     * Returns the selected branch so callers can retain it, label it, or pass it to tagFleet().
     */
    public static BranchDefinition prepareFleetParams(FleetParamsV3 params, float spawnPoints,
                                                       FleetRole role, Random random) {
        if (params == null) return null;
        BranchDefinition branch = pickBranch(role, random);
        if (branch == null) return null;
        populateParamsFromBranch(params, branch.variants, spawnPoints, random);
        return branch;
    }

    /** Explicit-branch overload useful for scripted fleets and external integrations. */
    public static BranchDefinition prepareFleetParams(FleetParamsV3 params, float spawnPoints,
                                                       String branchId, Random random) {
        if (params == null) return null;
        BranchDefinition branch = getBranch(branchId);
        if (branch == null) return null;
        populateParamsFromBranch(params, branch.variants, spawnPoints, random);
        return branch;
    }

    public static void tagFleet(CampaignFleetAPI fleet, BranchDefinition branch) {
        if (fleet == null || branch == null) return;
        fleet.getMemoryWithoutUpdate().set(FLEET_BRANCH_MEMORY, branch.id);
        fleet.getMemoryWithoutUpdate().set(FLEET_BRANCH_NAME_MEMORY, branch.displayName);
    }
    public static String getFleetBranchId(CampaignFleetAPI fleet) {
        return fleet == null ? null : fleet.getMemoryWithoutUpdate().getString(FLEET_BRANCH_MEMORY);
    }

    public static String getFleetBranchName(CampaignFleetAPI fleet) {
        if (fleet == null) return "未记录";
        String name = fleet.getMemoryWithoutUpdate().getString(FLEET_BRANCH_NAME_MEMORY);
        return name == null ? "未记录" : name;
    }

    /** Backwards-compatible general-purpose entry point. */
    public static void spawnRandomPTSDForce(FleetParamsV3 params, int spawnPoints) {
        prepareFleetParams(params, spawnPoints, FleetRole.GENERAL,
                params == null ? null : params.random);
    }

    /** Backwards-compatible explicit-map entry point; this never pulls from another branch. */
    public static void assignShardBeforeSpawnFleet(FleetParamsV3 params,
            Map<ShipAPI.HullSize, ShardTypeVariants> variantData, float spawnPoints) {
        populateParamsFromBranch(params, variantData, spawnPoints,
                params == null ? null : params.random);
    }

    private static void populateParamsFromBranch(FleetParamsV3 params,
            Map<ShipAPI.HullSize, ShardTypeVariants> variantData,
            float spawnPoints, Random random) {
        if (params == null || variantData == null) return;
        Random rng = random == null ? new Random() : random;
        params.addShips = new ArrayList<>();
        int remaining = Math.max(7, Math.round(spawnPoints));
        int maxShips = params.maxNumShips == null ? Global.getSettings().getMaxShipsInFleet() : params.maxNumShips;
        int safety = 0;
        while (remaining >= 3 && params.addShips.size() < maxShips && safety++ < 300) {
            ShipAPI.HullSize size = pickHullSize(remaining, variantData, rng);
            String variant = pickVariantForSize(size, variantData);
            if (variant == null) variant = pickAnyShipVariant(variantData, rng);
            if (variant == null) break;
            params.addShips.add(variant);
            remaining -= getVariantCost(variant, size);
        }

        // addShips are applied even with zero automatic fleet points. Zeroing every category is what
        // guarantees that FleetFactoryV3 cannot append ships from the faction's generic role pool.
        params.combatPts = 0f;
        params.freighterPts = 0f;
        params.tankerPts = 0f;
        params.transportPts = 0f;
        params.linerPts = 0f;
        params.utilityPts = 0f;
        params.doNotPrune = true;
    }

    public static void assignShardAfterSpawnFleet(CampaignFleetAPI fleet,
            Map<ShipAPI.HullSize, ShardTypeVariants> variantData, int spawnPoints) {
        if (fleet == null || variantData == null) return;
        try {
            fleet.getFleetData().clear();
            FleetParamsV3 params = new FleetParamsV3();
            params.maxNumShips = Global.getSettings().getMaxShipsInFleet();
            populateParamsFromBranch(params, variantData, spawnPoints, null);
            for (String variant : params.addShips) fleet.getFleetData().addFleetMember(variant);
        } catch (Throwable t) {
            Global.getLogger(PTSD_BaseShard_Util.class).warn("Unable to rebuild branch fleet", t);
        }
    }

    private static ShipAPI.HullSize pickHullSize(int remaining,
            Map<ShipAPI.HullSize, ShardTypeVariants> data, Random random) {
        ShipAPI.HullSize[] order = {
                ShipAPI.HullSize.CAPITAL_SHIP, ShipAPI.HullSize.CRUISER,
                ShipAPI.HullSize.DESTROYER, ShipAPI.HullSize.FRIGATE
        };
        int[] thresholds = {70, 45, 18, 3};
        List<ShipAPI.HullSize> eligible = new ArrayList<>();
        for (int i = 0; i < order.length; i++) {
            if (remaining >= thresholds[i] && hasUsableVariant(order[i], data)) eligible.add(order[i]);
        }
        if (eligible.isEmpty()) {
            for (int i = order.length - 1; i >= 0; i--) {
                if (hasUsableVariant(order[i], data)) return order[i];
            }
            return ShipAPI.HullSize.FRIGATE;
        }
        // Usually take the largest affordable level, sometimes a lower one for organic compositions.
        if (eligible.size() == 1 || random.nextFloat() < 0.72f) return eligible.get(0);
        return eligible.get(1 + random.nextInt(eligible.size() - 1));
    }

    private static boolean hasUsableVariant(ShipAPI.HullSize size,
            Map<ShipAPI.HullSize, ShardTypeVariants> data) {
        ShardTypeVariants variants = data.get(size);
        if (variants == null) return false;
        for (ShardType type : ShardType.values()) {
            String candidate = variants.get(type).pick();
            if (isShipVariant(candidate)) return true;
        }
        return false;
    }

    private static String pickAnyShipVariant(Map<ShipAPI.HullSize, ShardTypeVariants> data, Random random) {
        ShipAPI.HullSize[] sizes = {
                ShipAPI.HullSize.FRIGATE, ShipAPI.HullSize.DESTROYER,
                ShipAPI.HullSize.CRUISER, ShipAPI.HullSize.CAPITAL_SHIP
        };
        int start = random.nextInt(sizes.length);
        for (int i = 0; i < sizes.length; i++) {
            String result = pickVariantForSize(sizes[(start + i) % sizes.length], data);
            if (result != null) return result;
        }
        return null;
    }

    private static String pickVariantForSize(ShipAPI.HullSize size,
            Map<ShipAPI.HullSize, ShardTypeVariants> variantData) {
        ShardTypeVariants variants = variantData.get(size);
        if (variants == null) return null;
        ShardType[] types = ShardType.values();
        for (int attempt = 0; attempt < types.length * 2; attempt++) {
            String pick = variants.get(types[(int) (Math.random() * types.length)]).pick();
            if (isShipVariant(pick)) return pick;
        }
        for (ShardType type : types) {
            String pick = variants.get(type).pick();
            if (isShipVariant(pick)) return pick;
        }
        return null;
    }

    private static boolean isShipVariant(String variantId) {
        return variantId != null && Global.getSettings().doesVariantExist(variantId);
    }

    private static int getVariantCost(String variantId, ShipAPI.HullSize fallbackSize) {
        try {
            return Math.max(3, Math.round(Global.getSettings().getVariant(variantId).getHullSpec().getFleetPoints()));
        } catch (Throwable ignored) {
            if (fallbackSize == ShipAPI.HullSize.CAPITAL_SHIP) return 70;
            if (fallbackSize == ShipAPI.HullSize.CRUISER) return 45;
            if (fallbackSize == ShipAPI.HullSize.DESTROYER) return 18;
            return 7;
        }
    }
}