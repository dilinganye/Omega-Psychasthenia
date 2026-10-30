package data.scripts.campaign.invasion;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.*;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.lwjgl.util.vector.Vector2f;
import java.lang.reflect.*;
import java.util.*;
import static data.scripts.campaign.invasion.ControlledPatrolRouteTest.proxy;
import static data.scripts.campaign.invasion.PTSDReliefState.Stage.*;

/** Calls production schedulers and ledger methods against controlled fleets/markets. */
public final class ReliefLifecycleTest {
    static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
    static PTSDCrisisState state()throws Exception{Constructor<PTSDCrisisState> c=PTSDCrisisState.class.getDeclaredConstructor();c.setAccessible(true);PTSDCrisisState s=c.newInstance();s.progressInitialized=true;s.phase=PTSDCrisisState.Phase.WAR;return s;}
    public static void main(String[] args)throws Exception{
        SectorAPI previous=Global.getSector();SettingsAPI old=Global.getSettings();CombatEngineAPI oldEngine=Global.getCombatEngine();
        final float[] day={100},fp={80};final int[] removed={0};final boolean[] alive={true},inBattle={false};
        try{
            ModManagerAPI mods=proxy(ModManagerAPI.class,(n,a)->n.equals("getEnabledModsCopy")?Collections.emptyList():null);
            Global.setSettings(proxy(SettingsAPI.class,(n,a)->n.equals("getModManager")?mods:null));Global.setCombatEngine(null);
            org.lazywizard.lazylib.JSONUtils.CommonDataJSONObject settings=new org.lazywizard.lazylib.JSONUtils.CommonDataJSONObject("relief-test");for(Map.Entry<String,Integer> e:PTSDReliefSettings.DEFAULTS.entrySet())settings.put(e.getKey(),e.getValue());lunalib.backend.ui.settings.LunaSettingsLoader.getSettings().put(data.scripts.IIRT_Omega_ModPlugin.getModId(),settings);
            CampaignClockAPI clock=proxy(CampaignClockAPI.class,(n,a)->n.equals("getTimestamp")?(long)((day[0]-PTSDCrisisState.CAMPAIGN_DAY_EPOCH_OFFSET)*86400000d):null);
            SectorEntityToken center=proxy(SectorEntityToken.class,(n,a)->n.equals("getLocation")?new Vector2f():null);
            StarSystemAPI system=proxy(StarSystemAPI.class,(n,a)->n.equals("getId")?"target":n.equals("getLocation")?new Vector2f(100,100):n.equals("getCenter")?center:n.equals("getAllEntities")?Collections.emptyList():null);
            Map<String,MarketAPI> markets=new LinkedHashMap<>();Map<String,FactionAPI> factions=new LinkedHashMap<>();Map<String,Float> relations=new HashMap<>();
            for(String id:new String[]{"hegemony","tritachyon","persean","luddic_church","independent","sindrian_diktat","player","luddic_path",IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION}){
                factions.put(id,proxy(FactionAPI.class,(n,a)->n.equals("getId")?id:n.equals("getDisplayName")?id:n.equals("getRelationship")?relations.getOrDefault(id+":"+a[0],-.7f):n.equals("setRelationship")?relations.put(id+":"+a[0],(Float)a[1]):null));
            }
            for(String id:new String[]{"hegemony","tritachyon","persean","luddic_church","independent","sindrian_diktat"})markets.put(id,proxy(MarketAPI.class,(n,a)->n.equals("getId")?id:n.equals("getFactionId")?id:n.equals("getSize")?5:n.equals("getStarSystem")?system:n.equals("getPrimaryEntity")?center:null));
            EconomyAPI economy=proxy(EconomyAPI.class,(n,a)->n.equals("getMarketsCopy")?new ArrayList<>(markets.values()):n.equals("getMarket")?markets.get(a[0]):n.equals("getMarkets")?new ArrayList<>(markets.values()):null);
            Map<String,Object> persistent=new HashMap<>();BattleAPI battle=proxy(BattleAPI.class,(n,a)->null);
            CampaignFleetAPI fleet=proxy(CampaignFleetAPI.class,(n,a)->{if(n.equals("getFleetPoints"))return (int)fp[0];if(n.equals("isAlive"))return alive[0];if(n.equals("getBattle"))return inBattle[0]?battle:null;if(n.equals("getLocationInHyperspace")||n.equals("getLocation"))return new Vector2f();if(n.equals("getStarSystem")||n.equals("getContainingLocation"))return system;if(n.equals("despawn")){removed[0]++;return null;}return null;});
            CharacterDataAPI character=proxy(CharacterDataAPI.class,(n,a)->n.equals("getMemoryWithoutUpdate")?SceneRevisionTest.memory(new HashMap<>()):null);
            Global.setSector(proxy(SectorAPI.class,(n,a)->n.equals("getCharacterData")?character:n.equals("getEconomy")?economy:n.equals("getClock")?clock:n.equals("getAllLocations")?Collections.singletonList(system):n.equals("getStarSystems")?Collections.singletonList(system):n.equals("getStarSystem")?system:n.equals("getPersistentData")?persistent:n.equals("getPlayerFaction")?factions.get("player"):n.equals("getFaction")?factions.get(a[0]):n.equals("getAllFactions")?new ArrayList<>(factions.values()):n.equals("getPlayerFleet")?fleet:null));
            PTSDCrisisState s=state();persistent.put(PTSDCrisisState.PERSISTENT_KEY,s);s.late.staffEstablished=true;
            PTSDGeneralStaffDirector.registerFounders(s);
            check(s.late.coalitionMembers.equals(new LinkedHashSet<>(Arrays.asList("hegemony","tritachyon"))),"Incorrect first founders");
            check(s.late.coalitionJoinRequests.size()==4&&!s.late.coalitionMembers.contains("player"),"Invites bypassed or player autojoined");
            check(!PTSDGeneralStaffDirector.requestMembership(s,"luddic_path"),"Path joined coalition");
            Method membership=PTSDGeneralStaffDirector.class.getDeclaredMethod("advanceMembership",PTSDCrisisState.class,float.class);membership.setAccessible(true);
            membership.invoke(null,s,day[0]+2);check(s.late.coalitionMembers.containsAll(Arrays.asList("independent","persean","luddic_church","sindrian_diktat")),"Fixed factions rejected on old hostility");
            PTSDGeneralStaffIntel staff=new PTSDGeneralStaffIntel();com.fs.starfarer.api.ui.IntelUIAPI ui=proxy(com.fs.starfarer.api.ui.IntelUIAPI.class,(n,a)->null);
            staff.buttonPressConfirmed("PLAYER:ACCEPT",ui);check(s.late.coalitionJoinRequests.containsKey("player"),"Player acceptance missing");staff.buttonPressConfirmed("PLAYER:DECLINE",ui);check(!s.late.coalitionJoinRequests.containsKey("player"),"Decline failed to cancel pending acceptance");
            markets.remove("hegemony");markets.remove("tritachyon");PTSDCrisisState fallback=state();fallback.late.staffEstablished=true;PTSDGeneralStaffDirector.registerFounders(fallback);
            check(fallback.late.coalitionMembers.contains("persean")&&fallback.late.coalitionMembers.contains("luddic_church"),"Founder fallback failed");
            PTSDReliefDirector.relations(s);check(relations.get("luddic_path:persean")==.5f&&relations.get("luddic_path:player")==0f&&relations.get("luddic_path:"+IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION)==-1f,"Path war diplomacy mismatch");
            s.phase=PTSDCrisisState.Phase.ENDED;PTSDReliefDirector.relations(s);check(relations.get("luddic_path:persean")==-.7f,"Pre-war relationship not restored");s.phase=PTSDCrisisState.Phase.WAR;
            MarketAPI source=markets.get("persean");check(PTSDReliefDirector.available(s,source)==100,"Half source reserve not retained");
            PTSDReliefDirector.charge(s,"persean",80);check(PTSDReliefDirector.available(s,source)==20,"Commitment not deducted");
            PTSDReliefState.Unit u=new PTSDReliefState.Unit();u.id="unit";u.kind="ALLIANCE";u.faction="persean";u.sourceMarket="persean";u.targetSystem="target";u.fleet=fleet;u.stage=ACTIVE;u.stageDay=day[0];u.physical=true;u.borrowed=true;
            PTSDReliefState.get(s).units.add(u);PTSDLateState.Front front=new PTSDLateState.Front();front.id="f";front.systemId="target";front.defense=20;s.late.fronts.put("f",front);u.frontId="f";
            PTSDReliefDirector.advanceUnit(s,u,day[0]);PTSDReliefDirector.advanceUnit(s,u,day[0]);check(front.defense==100,"Repeated support duplicated defense");
            fp[0]=50;PTSDReliefDirector.advanceUnit(s,u,day[0]+.25f);check(front.defense==70,"Loss ignored by front defense");
            PTSDReliefDirector.replenish(s,day[0]);PTSDReliefDirector.replenish(s,day[0]+29);check(PTSDReliefState.get(s).sourceSpent.get("persean")==80,"Early replenishment");
            PTSDReliefDirector.replenish(s,day[0]+30);check(PTSDReliefState.get(s).sourceSpent.get("persean")==65,"Replenishment is not 15% of mobilization cap");
            PTSDReliefDirector.replenish(s,day[0]+60);PTSDReliefDirector.replenish(s,day[0]+90);check(PTSDReliefState.get(s).sourceSpent.get("persean")==50,"Live deployed soldiers replenished twice");
            PTSDReliefDirector.finish(s,u,true);PTSDReliefDirector.finish(s,u,true);check(front.defense==20&&PTSDReliefState.get(s).sourceSpent.get("persean")==0&&removed[0]==1,"Return credit or withdrawal repeated");
            // Exercise the actual preparation/muster scheduler. No defense during travel or waiting.
            PTSDReliefState.Unit journey=new PTSDReliefState.Unit();journey.id="travel";journey.kind="ALLIANCE";journey.faction="persean";journey.fleet=fleet;journey.physical=true;journey.sourceMarket="persean";journey.musterMarket="persean";journey.targetMarket="persean";journey.targetSystem="target";journey.frontId="f";journey.group="assembly";journey.stageDay=day[0];PTSDReliefState.get(s).units.add(journey);
            PTSDReliefState.Unit delayed=new PTSDReliefState.Unit();delayed.group="assembly";PTSDReliefState.get(s).units.add(delayed);
            PTSDReliefDirector.advanceUnit(s,journey,day[0]+4);check(journey.stage==PREPARING&&front.defense==20,"Preparation skipped");
            PTSDReliefDirector.advanceUnit(s,journey,day[0]+5);check(journey.stage==MUSTERING,"Five-day prep failed");
            PTSDReliefDirector.advanceUnit(s,journey,day[0]+6);check(journey.stage==ASSEMBLED,"Physical muster arrival missed");
            PTSDReliefDirector.advanceUnit(s,journey,day[0]+15);check(journey.stage==ASSEMBLED&&front.defense==20,"Muster departed early or added remote defense");
            PTSDReliefDirector.advanceUnit(s,journey,day[0]+16);check(journey.stage==TRAVELLING,"Ten-day assembly timeout failed");
            PTSDReliefDirector.advanceUnit(s,journey,day[0]+17);check(journey.stage==ACTIVE&&front.defense==70,"Actual arrival failed to add defense");
            PTSDReliefDirector.finish(s,journey,false);check(front.defense==20,"Ended action leaked support");
            front.warningDay=day[0];PTSDReliefState.get(s).alerts.add(front.id);check(PTSDReliefDirector.requestAssistance(s,front)&&!PTSDReliefState.get(s).alerts.contains(front.id),"Request before three-day window bypassed scheduler");
            check(PTSDCombatRelief.budget(100,.4f,60,300)==60&&Math.abs(PTSDCombatRelief.budget(400,.6f,60,300)-240)<.001f&&PTSDCombatRelief.budget(1000,.6f,60,300)==300,"Path quota bounds");
            StatBonus deployment=new StatBonus();deployment.modifyFlat("other_mod",3);DynamicStatsAPI dynamic=proxy(DynamicStatsAPI.class,(n,a)->n.equals("getMod")?deployment:null);MutableShipStatsAPI stats=proxy(MutableShipStatsAPI.class,(n,a)->n.equals("getDynamic")?dynamic:null);boolean[] ally={false};
            FleetMemberAPI member=proxy(FleetMemberAPI.class,(n,a)->{if(n.equals("getStats"))return stats;if(n.equals("setAlly")){ally[0]=(Boolean)a[0];return null;}return null;});
            PTSDCombatRelief.zeroDeployment(member);check(deployment.computeEffective(20)==0&&ally[0],"Reinforcements consume DP");PTSDCombatRelief.restoreDeployment(member);check(deployment.computeEffective(20)==23&&!ally[0],"Deployment restore clobbered unrelated modifiers");
            PTSDReliefState.BattleRecord record=new PTSDReliefState.BattleRecord();record.id="battle";PTSDReliefState.get(s).battles.put(record.id,record);inBattle[0]=true;PTSDCombatRelief.settleCampaign(s);check(!record.settled,"Settled between engagements");inBattle[0]=false;PTSDCombatRelief.settleCampaign(s);PTSDCombatRelief.settleCampaign(s);check(record.settled,"Battle not settled");
            PTSDJeWarExpedition.Mission je=new PTSDJeWarExpedition.Mission();je.escort=fleet;je.system="target";je.distress=true;je.deadline=day[0]+10;je.started=day[0];PTSDReliefState.get(s).jeExpedition=je;
            CampaignFleetAPI enemy=proxy(CampaignFleetAPI.class,(n,a)->n.equals("isAlive")?true:n.equals("getId")?"pursuer":null);je.pursuer=enemy;
            PTSDJeWarExpedition.advance(s,day[0]+9);check(!je.done&&!je.rolled&&!s.jeDead,"Je fate rolled before rescue deadline");PTSDJeWarExpedition.recordBattle(s,enemy);check(je.playerEngaged,"Je pursuit combat not recognized");
            check(PTSDJeWarExpedition.missing(.29f,30)&&!PTSDJeWarExpedition.missing(.30f,30),"Failure chance boundary");
            je.done=true;je.missing=true;s.jeMissedContactsSinceSuccess=9;check(PTSDJeOtloesManager.rollContactAttempt()==PTSDJeOtloesManager.ContactResult.MISSED,"Repeated calls bypass permanent Je missing state");
            for(String key:PTSDReliefSettings.DEFAULTS.keySet())check(data.scripts.PTSDDifficultySettings.managed(key),"Setting missing from difficulty "+key);
            System.out.println("ReliefLifecycleTest: passed (founders/invites, diplomacy restore, finite pool, 30-day loss-only refill, actual support, dedup return, quota/zero DP, battle rounds, Je rescue deadline)");
        }finally{Global.setSector(previous);Global.setSettings(old);Global.setCombatEngine(oldEngine);}
    }
}
