package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.*;
import com.fs.starfarer.api.ui.IntelUIAPI;
import java.lang.reflect.*;
import java.util.*;

/** Models the engine's row-based selection, including an active category filter. */
public final class DevIntelNavigationTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        SectorAPI oldSector=Global.getSector();SettingsAPI oldSettings=Global.getSettings();
        try {
            Map<String,Object> persistent=new HashMap<>();
            Constructor<PTSDCrisisState> ctor=PTSDCrisisState.class.getDeclaredConstructor();ctor.setAccessible(true);
            PTSDCrisisState state=ctor.newInstance();state.progressInitialized=true;
            persistent.put(PTSDCrisisState.PERSISTENT_KEY,state);
            List<IntelInfoPlugin> registered=new ArrayList<>(),rows=new ArrayList<>();
            List<String> calls=new ArrayList<>();
            IntelInfoPlugin[] selected={null};
            IntelManagerAPI manager=(IntelManagerAPI)Proxy.newProxyInstance(IntelManagerAPI.class.getClassLoader(),new Class[]{IntelManagerAPI.class},(p,m,a)->{
                if(m.getName().equals("getIntel"))return registered;
                if(m.getName().equals("addIntel")){registered.add((IntelInfoPlugin)a[0]);calls.add("add");}
                return null;
            });
            CampaignClockAPI clock=NewsSceneMechanicsTest.mock(CampaignClockAPI.class,n->null);
            Global.setSettings(NewsSceneMechanicsTest.mock(SettingsAPI.class,n->n.equals("isDevMode")?true:null));
            Global.setSector(NewsSceneMechanicsTest.mock(SectorAPI.class,n->n.equals("getPersistentData")?persistent:n.equals("getClock")?clock:n.equals("getIntelManager")?manager:null));
            IntelUIAPI ui=(IntelUIAPI)Proxy.newProxyInstance(IntelUIAPI.class.getClassLoader(),new Class[]{IntelUIAPI.class},(p,m,a)->{
                if(m.getName().equals("updateIntelList")){
                    check(a.length==2,"Must include destination despite active source filter");
                    rows.clear();rows.addAll((List<IntelInfoPlugin>)a[1]);calls.add("refresh");
                }
                if(m.getName().equals("selectItem")){
                    // Real engine silently ignores destinations without a row.
                    if(rows.contains(a[0]))selected[0]=(IntelInfoPlugin)a[0];
                    calls.add("select");
                }
                return null;
            });
            PTSDCrisisDevControlIntel control=new PTSDCrisisDevControlIntel();
            control.buttonPressConfirmed("SCENE_PANEL",ui);
            check(registered.size()==1&&selected[0]==registered.get(0),"First click failed to show new panel");
            check(calls.equals(Arrays.asList("add","refresh","select")),"Registration/refresh/selection order wrong");
            check(selected[0].getIntelTags(null).containsAll(Arrays.asList("DEV","危机")),"Panel missing source category tags");
            calls.clear();rows.clear();selected[0]=null;
            control.buttonPressConfirmed("SCENE_PANEL",ui);
            check(registered.size()==1&&selected[0]==registered.get(0),"Reopen duplicated panel or failed under filter");
            check(calls.equals(Arrays.asList("refresh","select")),"Reopen order wrong");
            System.out.println("DevIntelNavigationTest: passed (actual button first open, filtered reopen, singleton, categories)");
        } finally {Global.setSector(oldSector);Global.setSettings(oldSettings);}
    }
}
