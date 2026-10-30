package data.scripts.campaign.invasion;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import data.hullmods.shard.PTSD_BaseShard_Util;
import java.lang.reflect.*;
import java.util.*;

public class ReconFleetCompositionTest {
    static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
    public static void main(String[] args)throws Exception {
        SectorAPI old=Global.getSector();SettingsAPI settings=Global.getSettings();
        try{
            Constructor<PTSDCrisisState> ctor=PTSDCrisisState.class.getDeclaredConstructor();ctor.setAccessible(true);
            PTSDCrisisState state=ctor.newInstance();state.phase=PTSDCrisisState.Phase.RECON;
            Map<String,Object> saved=new HashMap<>();saved.put(PTSDCrisisState.PERSISTENT_KEY,state);
            Global.setSector(NewsSceneMechanicsTest.mock(SectorAPI.class,n->n.equals("getPersistentData")?saved:null));
            Map<String,ShipVariantAPI> variants=new HashMap<>();
            for(String id:Arrays.asList("frigate","cosinus","cruiser")){
                ShipAPI.HullSize size=id.equals("frigate")?ShipAPI.HullSize.FRIGATE:id.equals("cosinus")?ShipAPI.HullSize.DESTROYER:ShipAPI.HullSize.CRUISER;
                int fp=id.equals("frigate")?5:id.equals("cosinus")?12:20;
                ShipHullSpecAPI hull=NewsSceneMechanicsTest.mock(ShipHullSpecAPI.class,n->n.equals("getHullSize")?size:n.equals("getFleetPoints")?fp:null);
                variants.put(id,NewsSceneMechanicsTest.mock(ShipVariantAPI.class,n->n.equals("getHullSpec")?hull:null));
            }
            Global.setSettings((SettingsAPI)Proxy.newProxyInstance(SettingsAPI.class.getClassLoader(),new Class[]{SettingsAPI.class},(p,m,a)->{
                if(m.getName().equals("doesVariantExist"))return variants.containsKey(a[0]);
                if(m.getName().equals("getVariant"))return variants.get(a[0]);
                if(m.getName().equals("getMaxShipsInFleet"))return 30;return null;
            }));
            for(PTSD_BaseShard_Util.BranchDefinition b:PTSD_BaseShard_Util.getRegisteredBranches())check(b.getWeight(PTSD_BaseShard_Util.FleetRole.RECON)==3,"Recon branch bias remains");
            Map<ShipAPI.HullSize,PTSD_BaseShard_Util.ShardTypeVariants> pool=new EnumMap<>(ShipAPI.HullSize.class);
            PTSD_BaseShard_Util.ShardTypeVariants small=new PTSD_BaseShard_Util.ShardTypeVariants();
            small.get(PTSD_BaseShard_Util.ShardType.GENERAL).add("frigate",10);
            small.get(PTSD_BaseShard_Util.ShardType.GENERAL).add("cosinus",1); // Deliberately misplaced historical entry.
            pool.put(ShipAPI.HullSize.FRIGATE,small);
            PTSD_BaseShard_Util.ShardTypeVariants medium=new PTSD_BaseShard_Util.ShardTypeVariants();
            medium.get(PTSD_BaseShard_Util.ShardType.GENERAL).add("cosinus",10);pool.put(ShipAPI.HullSize.DESTROYER,medium);
            PTSD_BaseShard_Util.ShardTypeVariants large=new PTSD_BaseShard_Util.ShardTypeVariants();
            large.get(PTSD_BaseShard_Util.ShardType.GENERAL).add("cruiser",10);pool.put(ShipAPI.HullSize.CRUISER,large);
            PTSD_BaseShard_Util.registerBranch("test-composition","test",pool);
            int frigateFirst=0,destroyerFirst=0;
            Random random=new Random(9817);
            float[] progress={0,.249f,.25f,.499f,.5f,.749f,.75f,.899f,.9f,1f};
            int[] caps={12,12,18,18,24,24,30,30,36,36};
            int[] counts={1,1,2,2,2,2,3,3,3,3};
            int threeCosinus=0;
            for(int band=0;band<progress.length;band++){
            state.phaseStartedDay=-progress[band]*data.scripts.IIRT_Omega_ModPlugin.collect_data_time*2;
            check(PTSDCrisisFleetLimits.forPhase(state,0).perFleetFp==caps[band],"Wrong progress cap");
            for(int k=0;k<500;k++){
                FleetParamsV3 params=new FleetParamsV3();params.maxNumShips=30;
                PTSD_BaseShard_Util.prepareFleetParams(params,200,"test-composition",random,
                    new PTSD_BaseShard_Util.FleetLimits(200,30,ShipAPI.HullSize.CAPITAL_SHIP));
                check(!params.addShips.isEmpty()&&params.addShips.size()<=counts[band],"Wrong count");
                float cost=0;int cos=0;
                for(String id:params.addShips){check(!id.equals("cruiser"),"Recon cruiser escaped cap");cost+=variants.get(id).getHullSpec().getFleetPoints();if(id.equals("cosinus"))cos++;}
                check(cost<=caps[band],"Recon cap exceeded");
                if(progress[band]<.25f)check(params.addShips.size()==1&&cos==0,"Initial fleet is not one frigate");
                if(progress[band]<.9f)check(cos<3,"Three Cosinus unlocked before final tenth");
                else if(cos==3)threeCosinus++;
                if(params.addShips.get(0).equals("frigate"))frigateFirst++;else destroyerFirst++;
            }
            }
            check(frigateFirst>150&&destroyerFirst>150,"Largest-first or duplicate-bucket bias persists");
            check(threeCosinus>0,"Late RECON never permits three Cosinus");
            float oldDifficulty=data.scripts.IIRT_Omega_ModPlugin.omega_fleet_strength_multiplier;
            try{
                data.scripts.IIRT_Omega_ModPlugin.omega_fleet_strength_multiplier=10;
                state.phaseStartedDay=0;
                PTSDCrisisFleetLimits early=PTSDCrisisFleetLimits.forPhase(state,0);
                check(early.perFleetFp==12&&early.maxShips==1&&early.maxHullSize==ShipAPI.HullSize.FRIGATE,"Difficulty bypasses opening gate");
            }finally{data.scripts.IIRT_Omega_ModPlugin.omega_fleet_strength_multiplier=oldDifficulty;}
            state.phase=PTSDCrisisState.Phase.FORTIFICATION;
            FleetParamsV3 later=new FleetParamsV3();later.maxNumShips=3;
            PTSD_BaseShard_Util.prepareFleetParams(later,60,"test-composition",random,new PTSD_BaseShard_Util.FleetLimits(60,3,ShipAPI.HullSize.CRUISER));
            check(later.addShips.size()==3&&later.addShips.get(0).equals("cruiser"),"Later phase unexpectedly nerfed");
            System.out.println("ReconFleetCompositionTest: passed (5000 fleets, progress boundaries, initial single frigate, three Cosinus only at 90%, equal branch weights, later phase unchanged)");
        }finally{PTSD_BaseShard_Util.unregisterBranch("test-composition");Global.setSector(old);Global.setSettings(settings);}
    }
}
