package data.scripts.campaign.invasion;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import data.scripts.IIRT_Omega_ModPlugin;
import lunalib.lunaSettings.LunaSettings;
import org.json.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Optional, source-attributed mod contributions. Keys survive row reordering. */
public final class PTSDPriorityTargetList {
    public static final String PATH="data/config/ptsdCSV/PTSD_first_wave_targets.csv";
    public static final String MASTER="PTSD_priority_targets_enabled";
    public static final class Entry {
        public final String starId, reason, requiredMod, sourceMod, sourceName, key;
        Entry(String star,String why,String required,String mod,String name) {
            starId=star;reason=why;requiredMod=required;sourceMod=mod;sourceName=name;
            key="PTSD_target_"+UUID.nameUUIDFromBytes((mod+"\n"+star).getBytes(StandardCharsets.UTF_8)).toString().replace("-","");
        }
        public boolean requirementMet(){return requiredMod.isEmpty() || Global.getSettings().getModManager().isModEnabled(requiredMod);}
        public boolean enabled(){return requirementMet() && setting(MASTER) && setting(key);}
        public StarSystemAPI system(){
            if(Global.getSector()==null)return null;
            SectorEntityToken token=Global.getSector().getEntityById(starId);
            return token instanceof PlanetAPI && ((PlanetAPI)token).isStar()?token.getStarSystem():null;
        }
    }
    private static List<Entry> entries;
    private static final Set<String> unresolved=new HashSet<String>();
    private PTSDPriorityTargetList(){}
    static boolean setting(String key) {
        if(!IIRT_Omega_ModPlugin.hasLunaLib)return true;
        Boolean value=LunaSettings.getBoolean(IIRT_Omega_ModPlugin.getModId(),key);
        return value==null || value;
    }
    static int interval(boolean harassment){
        int fallback=harassment?15:30;
        if(!IIRT_Omega_ModPlugin.hasLunaLib)return fallback;
        Integer days=LunaSettings.getInt(IIRT_Omega_ModPlugin.getModId(),harassment?"PTSD_priority_harass_days":"PTSD_priority_recon_days");
        return days==null?fallback:Math.max(1,Math.min(365,days));
    }
    public static List<Entry> entries(){
        if(entries!=null)return entries;
        entries=new ArrayList<Entry>();
        for(ModSpecAPI mod:Global.getSettings().getModManager().getEnabledModsCopy()) {
            // Check the contributing mod itself: fallback lookup must not attribute our file to every mod.
            if(!new File(mod.getPath(),PATH).isFile())continue;
            try {
                JSONArray rows=Global.getSettings().loadCSV(PATH,mod.getId());Set<String> seen=new HashSet<String>();
                for(int i=0;i<rows.length();i++) {
                    JSONObject row=rows.getJSONObject(i);if(!row.has("starID")||!row.has("reasonID")||!row.has("requireModId"))throw new IllegalArgumentException("Expected starID,reasonID,requireModId");String star=row.optString("starID","").trim();
                    if(star.isEmpty()||star.startsWith("#"))continue;
                    if(!seen.add(star))throw new IllegalArgumentException("Duplicate starID: "+star);
                    entries.add(new Entry(star,row.optString("reasonID","").trim(),row.optString("requireModId","").trim(),mod.getId(),mod.getName()));
                }
            }catch(Exception ex){Global.getLogger(PTSDPriorityTargetList.class).error("Invalid priority target CSV from "+mod.getId()+": "+PATH,ex);}
        }
        return entries;
    }
    public static void addSettings(){
        String mod=IIRT_Omega_ModPlugin.getModId(),tab="指定打击名单";
        LunaSettings.SettingsCreator.addBoolean(mod,MASTER,"启用指定打击名单","名单星系将遭受武装侦察、骚扰和免费首波攻势。关闭仅停止新任务，已出发舰队仍可通过战斗拦截。",true,tab);
        LunaSettings.SettingsCreator.addInt(mod,"PTSD_priority_recon_days","名单武装侦察间隔（天）","RECON阶段的名单任务；不改变普通五日巡回侦察。",30,1,365,tab);
        LunaSettings.SettingsCreator.addInt(mod,"PTSD_priority_harass_days","名单骚扰间隔（天）","从要塞阶段持续至开战前。",15,1,365,tab);
        for(Entry e:entries()) {
            String details="恒星ID："+e.starId+"\n原因："+e.reason+"\n需求模组："+(e.requiredMod.isEmpty()?"无":e.requiredMod)+
                    "（"+(e.requirementMet()?"已满足":"未满足，不派兵")+"）\n来源模组："+e.sourceName+" ["+e.sourceMod+"]";
            // Luna passes tooltips through String.format.
            LunaSettings.SettingsCreator.addBoolean(mod,e.key,(e.starId+" · "+e.sourceName).replace('%','％'),details.replace('%','％'),true,tab);
            LunaSettings.SettingsCreator.addText(mod,e.key+"_info",details.replace('%','％'),tab);
        }
    }
    public static Map<String,StarSystemAPI> enabledSystems(){
        Map<String,StarSystemAPI> result=new LinkedHashMap<String,StarSystemAPI>();
        for(Entry e:entries())if(e.enabled()) {StarSystemAPI s=e.system();if(s!=null && !PTSDProtectedFaction.system(s))result.put(s.getId(),s);
            else if(s==null && unresolved.add(e.key))Global.getLogger(PTSDPriorityTargetList.class).warn("Unresolved starID "+e.starId+" from "+e.sourceMod+"; no target substituted");}
        return result;
    }
}
