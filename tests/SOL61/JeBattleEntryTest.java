import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import data.scripts.campaign.invasion.*;
import java.lang.reflect.*;
import java.util.*;

/** Exercise early fleet callbacks and the independent player-engagement fallback. */
public final class JeBattleEntryTest {
    public static void main(String[] args) throws Exception {
        SectorAPI old=Global.getSector();SettingsAPI settings=Global.getSettings();
        try {
            Map<String,Object> persistent=new HashMap<>();
            CampaignClockAPI clock=JeContactGuaranteeTest.mock(CampaignClockAPI.class,n->null);
            Global.setSector(JeContactGuaranteeTest.mock(SectorAPI.class,n->n.equals("getPersistentData")?persistent:n.equals("getClock")?clock:null));
            Global.setSettings(JeContactGuaranteeTest.mock(SettingsAPI.class,n->null));
            Constructor<PTSDCrisisState> c=PTSDCrisisState.class.getDeclaredConstructor();c.setAccessible(true);
            PTSDCrisisState state=c.newInstance();state.progressInitialized=true;
            persistent.put(PTSDCrisisState.PERSISTENT_KEY,state);
            Map<String,Object> memory=new HashMap<>();
            memory.put("$PTSD_participation_key","incident:shared");
            MemoryAPI mem=(MemoryAPI)Proxy.newProxyInstance(MemoryAPI.class.getClassLoader(),new Class<?>[]{MemoryAPI.class},(p,m,a)->{
                if(m.getName().equals("set")){memory.put((String)a[0],a[1]);return null;}
                if(m.getName().equals("getBoolean"))return Boolean.TRUE.equals(memory.get(a[0]));
                if(m.getName().equals("getString"))return memory.get(a[0]);
                if(m.getReturnType()==boolean.class)return false;
                return null;
            });
            FactionAPI omega=JeContactGuaranteeTest.mock(FactionAPI.class,n->n.equals("getId")?IIRT_Omega_Invasion.WATCHER_FACTION:null);
            CampaignFleetAPI enemy=JeContactGuaranteeTest.mock(CampaignFleetAPI.class,n->n.equals("getId")?"enemy":n.equals("getFaction")?omega:n.equals("getMemoryWithoutUpdate")?mem:null);
            BattleAPI battle=JeContactGuaranteeTest.mock(BattleAPI.class,n->n.equals("isPlayerInvolved")?true:n.equals("getNonPlayerSideSnapshot")?Collections.singletonList(enemy):null);
            PTSDJeOtloesManager.recordFleetEncounter(enemy);
            new PTSDStrategicFleetListener(null).reportBattleOccurred(enemy,null,battle);
            JeContactGuaranteeTest.check(state.totalPlayerOmegaBattles==1,"Early battle callback lost first combat");
            EngagementResultAPI result=JeContactGuaranteeTest.mock(EngagementResultAPI.class,n->n.equals("getBattle")?battle:null);
            new PTSDPlayerBattleObserver().reportPlayerEngagement(result);
            JeContactGuaranteeTest.check(state.totalPlayerOmegaBattles==1,"Observer duplicated fleet callback");
            JeContactGuaranteeTest.check(state.jeParticipationIds.size()==1,"One incident counted separately for sighting/battle");
            memory.remove("$PTSD_player_battle_counted");state.totalPlayerOmegaBattles=0;
            new PTSDPlayerBattleObserver().reportPlayerEngagement(result);
            JeContactGuaranteeTest.check(PTSDJeOtloesManager.triggerUnlocked(state),"Player callback failed without fleet listener");
            state.jeDead=true;
            JeContactGuaranteeTest.check(!PTSDJeOtloesManager.triggerUnlocked(state),"Dead contact unlocked");
            System.out.println("JeBattleEntryTest: passed (early callback, fallback, shared incident deduplication)");
        } finally {Global.setSector(old);Global.setSettings(settings);}
    }
}
