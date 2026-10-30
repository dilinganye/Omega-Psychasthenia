package data.scripts;

import com.fs.starfarer.api.Global;
import data.scripts.campaign.invasion.PTSDCrisisState;
import lunalib.lunaSettings.LunaSettings;
import lunalib.backend.ui.settings.*;
import org.lazywizard.lazylib.JSONUtils;
import org.json.JSONObject;
import java.util.*;

/** Saved preset/custom separation. Only explicit difficulty fields participate in change detection. */
public final class PTSDDifficultySettings {
    public static final String MODE="PTSD_difficulty_mode", CUSTOM="自定义";
    public static final String PRESETS="data/config/ptsdCSV/PTSD_difficulty_presets.json";
    public static final String[] PHASES={"DORMANT","RECON","EXPANSION","FORTIFICATION","MOBILIZATION","WAR"};
    private static boolean changing;
    private static String profileError;
    private static final Map<String,LunaSettingsData> schema=new LinkedHashMap<String,LunaSettingsData>();
    public static void capture(String mod){
        for(LunaSettingsData d:LunaSettingsLoader.getSettingsData())
            if(mod.equals(d.getModID())&&managed(d.getFieldID())&&numeric(d))schema.put(d.getFieldID(),d);
    }
    private static final Map<String,double[]> growth=new HashMap<String,double[]>(), production=new HashMap<String,double[]>(), officers=new HashMap<String,double[]>();
    private static final Set<String> uncapped=new HashSet<String>();
    private static final Map<String,double[]> curves=new HashMap<String,double[]>();
    private static final float[] starts={0,1,2,3,4,5}, ends={1,2,3,4,5,6};
    private PTSDDifficultySettings(){}
    public static boolean managed(String id){
        if(id==null || !id.startsWith("PTSD_") || MODE.equals(id) || id.equals("PTSD_curve_editor") || id.equals("PTSD_omega_officer_level_bonus"))return false;
        if(id.startsWith("PTSD_phase_") || id.equals("PTSD_DefStat_onNewGame") || id.equals("PTSD_omega_invasion_enabled") ||
                id.startsWith("PTSD_target_") || id.equals("PTSD_priority_targets_enabled") ||
                id.equals("PTSD_late_alert_density") || id.equals("PTSD_strategic_update_interval") || id.equals("PTSD_hidden_materialization_range"))return false;
        return true;
    }
    public static String curveId(String phase){return "PTSD_curve_"+phase.toLowerCase(java.util.Locale.ROOT);}
    public static String growthId(String phase){return "PTSD_curve_growth_"+phase.toLowerCase(Locale.ROOT);}
    public static String officerId(String phase){return "PTSD_curve_officer_"+phase.toLowerCase(Locale.ROOT);}
    private static final int[] OFFICER_DEFAULTS={8,8,9,10,11,12};
    public static String productionId(String phase){return "PTSD_curve_production_"+phase.toLowerCase(Locale.ROOT);}
    public static String limitId(String phase){return "PTSD_growth_limit_"+phase.toLowerCase(Locale.ROOT);}
    public static void registerCurves(String mod){
        for(int i=0;i<PHASES.length;i++){
            LunaSettings.SettingsCreator.addString(mod,curveId(PHASES[i]),PHASES[i]+"基础难度曲线","等距控制点，多点平滑，阶段独立。",((int)starts[i])+","+((int)ends[i]),"自定义难度");
            LunaSettings.SettingsCreator.addString(mod,growthId(PHASES[i]),PHASES[i]+"舰队强度增长率","1原速，0暂停；按本阶段已过时间累计。","1,1","自定义难度");
            LunaSettings.SettingsCreator.addString(mod,productionId(PHASES[i]),PHASES[i]+"战略资产产速","只影响自然生产，0停产；不改变运输与回收。","1,1","自定义难度");
            LunaSettings.SettingsCreator.addString(mod,officerId(PHASES[i]),PHASES[i]+"军官等级曲线","绝对等级2至20，平滑采样后四舍五入；取消旧加成与随机浮动，只影响新生军官。",OFFICER_DEFAULTS[i]+","+OFFICER_DEFAULTS[i],"自定义难度");
            LunaSettings.SettingsCreator.addInt(mod,limitId(PHASES[i]),PHASES[i]+"增长封顶模式","0封顶，1继续增长。",0,0,1,"自定义难度");
        }
        LunaSettings.SettingsCreator.addString(mod,"PTSD_curve_editor","四曲线可视化编辑","四张可拖动曲线在此显示；向下滚动可见军官等级与保存按钮。\n"+String.join("\n",Collections.nCopies(72,".")),"曲线编辑器","曲线编辑");
    }
    public static void hideCurves(String mod){
        Iterator<LunaSettingsData> it=LunaSettingsLoader.getSettingsData().iterator();
        while(it.hasNext()){LunaSettingsData d=it.next();if(mod.equals(d.getModID())&&(d.getFieldID().startsWith("PTSD_curve_")||d.getFieldID().startsWith("PTSD_growth_limit_")))it.remove();}
    }
    public static void register(String mod){
        LunaSettings.SettingsCreator.addRadio(mod,MODE,"危机难度","四个预设目前采用相同默认参数。保存手动改动后自动转为自定义；切回自定义恢复上次保存的自定义参数。","中等","简单,中等,困难,天灾,自定义","难度");
        LunaSettings.SettingsCreator.addText(mod,"PTSDDifficultyHelp","选择预设并保存即可应用；手动参数统一列于自定义难度；DEV模式点击曲线编辑页签，可直接拖动四张曲线。已有舰队不重建，修改影响后续生成与结算。","难度");
    }
    public static Map<String,Object> defaults(String preset){
        Map<String,Object> result=new LinkedHashMap<String,Object>();String mod=IIRT_Omega_ModPlugin.getModId();
        for(LunaSettingsData d:schema.values())result.put(d.getFieldID(),d.getDefaultValue());
        try{
            JSONObject all=Global.getSettings().loadJSON(PRESETS,mod), overrides=all.optJSONObject(preset);
            if(overrides!=null)for(String key:result.keySet())if(overrides.has(key))result.put(key,normalize(key,overrides.get(key),result.get(key)));
        }catch(Exception ex){Global.getLogger(PTSDDifficultySettings.class).warn("Unable to load difficulty preset overrides",ex);}
        return result;
    }
    private static Object normalize(String key,Object value,Object fallback){
        for(LunaSettingsData d:schema.values())if(key.equals(d.getFieldID())){
            if(key.startsWith("PTSD_curve_")){
                try{validateCurve(key,String.valueOf(value));return String.valueOf(value);}
                catch(RuntimeException ex){return fallback;}
            }
            if(!(value instanceof Number))return fallback;
            double number=((Number)value).doubleValue();if(Double.isNaN(number)||Double.isInfinite(number))return fallback;
            number=Math.max(d.getMinValue(),Math.min(d.getMaxValue(),number));
            if("Int".equals(d.getFieldType()))return Integer.valueOf((int)Math.round(number));
            return Double.valueOf(number);
        }
        return fallback;
    }
    private static boolean numeric(LunaSettingsData d){return d.getFieldID().startsWith("PTSD_curve_")|| "Int".equals(d.getFieldType())||"Double".equals(d.getFieldType());}
    public static void regroup(String mod){
        List<LunaSettingsData> fields=LunaSettingsLoader.getSettingsData();
        for(int n=0;n<fields.size();n++){
            LunaSettingsData d=fields.get(n);if(!mod.equals(d.getModID())||!managed(d.getFieldID())||!numeric(d))continue;
            fields.set(n,new LunaSettingsData(d.getModID(),d.getFieldID(),d.getFieldName(),d.getFieldType(),d.getFieldDescription(),d.getDefaultValue(),d.getSecondaryValue(),d.getMinValue(),d.getMaxValue(),"自定义难度"));
        }
    }
    public static void reconcile(){
        if(changing || schema.isEmpty() || !IIRT_Omega_ModPlugin.hasLunaLib)return;
        String mod=IIRT_Omega_ModPlugin.getModId();JSONUtils.CommonDataJSONObject data=LunaSettingsLoader.getSettings().get(mod);
        if(data==null)return;changing=true;profileError=null;
        try{
            Map<String,Object> base=defaults("中等"), current=new LinkedHashMap<String,Object>();
            for(Map.Entry<String,Object> e:base.entrySet())current.put(e.getKey(),data.has(e.getKey())?data.get(e.getKey()):e.getValue());
            String selected=data.optString(MODE,"中等"), previous=data.optString("_PTSD_previousDifficulty",selected);
            JSONObject applied=data.optJSONObject("_PTSD_appliedDifficulty"), custom=data.optJSONObject("_PTSD_customDifficulty");
            if(!selected.equals(previous)||(applied==null&&!CUSTOM.equals(selected)&&!"中等".equals(selected))){
                Map<String,Object> desired=defaults(selected);
                if(CUSTOM.equals(selected))for(String key:desired.keySet())desired.put(key,custom!=null&&custom.has(key)?normalize(key,custom.get(key),desired.get(key)):current.get(key));
                for(Map.Entry<String,Object> e:desired.entrySet())data.put(e.getKey(),e.getValue());
                current=desired;
            }else{
                boolean edited=false;
                for(String key:current.keySet()){
                    Object prior=applied!=null&&applied.has(key)?applied.get(key):base.get(key);
                    if(!same(current.get(key),prior)){edited=true;break;}
                }
                if(edited){selected=CUSTOM;data.put(MODE,CUSTOM);data.put("_PTSD_customDifficulty",new JSONObject(current));}
            }
            if(CUSTOM.equals(selected)||custom==null)data.put("_PTSD_customDifficulty",new JSONObject(current));
            for(String key:current.keySet()){
                Object fallback=applied!=null&&applied.has(key)?applied.get(key):base.get(key);
                Object valid=normalize(key,current.get(key),fallback);
                if(!same(valid,current.get(key)))Global.getLogger(PTSDDifficultySettings.class).warn("Invalid difficulty value retained prior valid setting: "+key);
                current.put(key,valid);data.put(key,valid);
            }
            if(CUSTOM.equals(selected))data.put("_PTSD_customDifficulty",new JSONObject(current));
            for(String phase:PHASES){
                curves.put(phase,PTSDStageCurve.parse(String.valueOf(current.get(curveId(phase)))));
                growth.put(phase,PTSDStageCurve.parse(String.valueOf(current.get(growthId(phase)))));
                officers.put(phase,PTSDStageCurve.parse(String.valueOf(current.get(officerId(phase)))));
                production.put(phase,PTSDStageCurve.parse(String.valueOf(current.get(productionId(phase)))));
                if(((Number)current.get(limitId(phase))).intValue()==1)uncapped.add(phase);else uncapped.remove(phase);
            }
            data.put("_PTSD_appliedDifficulty",new JSONObject(current));data.put("_PTSD_previousDifficulty",selected);data.save();
            if(CUSTOM.equals(selected))exportCustomSnapshot(mod,current);

        }catch(Exception ex){profileError=ex.getClass().getSimpleName();Global.getLogger(PTSDDifficultySettings.class).error("Difficulty profile update failed",ex);}
        finally{changing=false;}
    }

    public static final String SNAPSHOT_ROOT="PTSD-custom-difficulty/";
    private static String snapshotError;
    public static String snapshotStatus(){return snapshotError==null
            ? "已保存；快照位于 saves/common/PTSD-custom-difficulty/latest.json.data"
            : "参数已保存，但快照导出失败，请检查日志："+snapshotError;}
    /** Only the official common-file API is permitted inside Starsector's script sandbox. */
    private static void exportCustomSnapshot(String mod,Map<String,Object> current) {
        try { saveCustomSnapshot(current); snapshotError=null; }
        catch(Exception ex){snapshotError=ex.getClass().getSimpleName();Global.getLogger(PTSDDifficultySettings.class).error("Custom difficulty saved, but common-file snapshot export failed",ex);}
    }
    static void saveCustomSnapshot(Map<String,Object> current)throws Exception {
        String latest=SNAPSHOT_ROOT+"latest.json";
        if(Global.getSettings().fileExistsInCommon(latest)) {
            JSONObject prior=new JSONObject(Global.getSettings().readTextFileFromCommon(latest));
            boolean unchanged=prior.length()==current.size();
            for(String key:current.keySet())if(!prior.has(key)||!same(prior.opt(key),current.get(key))){unchanged=false;break;}
            if(unchanged)return;
        }
        String text=new JSONObject(current).toString(2);
        String stamp=new java.text.SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new java.util.Date());
        String history=SNAPSHOT_ROOT+"history/"+stamp+"-"+java.util.UUID.randomUUID().toString()+".json";
        Global.getSettings().writeTextFileToCommon(history,text);
        Global.getSettings().writeTextFileToCommon(latest,text);
    }

    public static float scale(PTSDCrisisState state,float day){
        if(state==null)return 1f;
        String phase=state.phase.name();double[] custom=curves.get(phase);if(custom==null)return 1f;
        int i=Arrays.asList(PHASES).indexOf(phase);if(i<0)return 1f;
        double p=Math.min(1,progress(state,day));
        double base=starts[i]+(ends[i]-starts[i])*p;
        // Default zero origin is a coordinate, not a no-fleet toggle. Bound the singular ratio.
        double value=PTSDStageCurve.sample(custom,p);
        if(base==0&&value==0&&custom.length==2&&custom[0]==starts[i]&&custom[1]==ends[i])return 1f;
        return (float)Math.max(0,Math.min(100,value/Math.max(.01,base)*growthFactor(state,day)));
    }
    public static double progress(PTSDCrisisState state,float day){
        float duration;
        switch(state.phase){
            case DORMANT:duration=IIRT_Omega_ModPlugin.start_stage_time;break;
            case RECON:duration=IIRT_Omega_ModPlugin.collect_data_time*2;break;
            case EXPANSION:duration=IIRT_Omega_ModPlugin.invade_time;break;
            case FORTIFICATION:duration=IIRT_Omega_ModPlugin.lateFortificationDeadline*2;break;
            case MOBILIZATION:duration=30;break;
            default:duration=240;
        }
        return Math.max(0,(day-state.phaseStartedDay)/Math.max(1,duration));
    }
    public static double growthProgress(PTSDCrisisState state,float day){
        if(state==null)return 0;
        String phase=state.phase.name();double p=progress(state,day);double[] rate=growth.get(phase);
        double elapsed=rate==null?p:PTSDStageCurve.integral(rate,p);
        return uncapped.contains(phase)?elapsed:Math.min(1,elapsed);
    }
    static double strengthCoordinate(PTSDCrisisState.Phase phase,double p){
        if(phase==PTSDCrisisState.Phase.RECON){
            if(p>1)return 36+24*(p-1);
            return p<.25?12:p<.5?18:p<.75?24:p<.9?30:36;
        }
        if(phase==PTSDCrisisState.Phase.WAR)return 120+280*p;
        return 1+p;
    }
    private static double growthFactor(PTSDCrisisState state,float day){
        String phase=state.phase.name();double[] rate=growth.get(phase);
        if(rate==null)return 1;
        return strengthCoordinate(state.phase,growthProgress(state,day))/strengthCoordinate(state.phase,Math.min(1,progress(state,day)));
    }
    static void validateCurve(String key,String value){
        double[] points=PTSDStageCurve.parse(value);
        if(key.startsWith("PTSD_curve_officer_"))for(double p:points)
            if(p<2||p>20)throw new IllegalArgumentException("Officer level must be between 2 and 20");
    }
    public static int officerLevel(PTSDCrisisState state,float day){
        String phase=state==null||state.phase==null?"DORMANT":state.phase.name();
        if("ENDED".equals(phase))phase="WAR";
        int index=Arrays.asList(PHASES).indexOf(phase);if(index<0)index=0;
        double[] points=officers.get(phase);
        if(points==null)return OFFICER_DEFAULTS[index];
        return Math.max(2,Math.min(20,(int)Math.round(PTSDStageCurve.sample(points,state==null?0:progress(state,day)))));
    }
    public static float productionScale(PTSDCrisisState state,float day){
        if(state==null)return 1;
        double[] values=production.get(state.phase.name());
        return values==null?1:(float)PTSDStageCurve.sample(values,progress(state,day));
    }
    public static Map<String,Object> curveValues(){
        Map<String,Object> result=new LinkedHashMap<String,Object>();
        JSONUtils.CommonDataJSONObject data=LunaSettingsLoader.getSettings().get(IIRT_Omega_ModPlugin.getModId());
        Map<String,Object> fallback=defaults("中等");
        for(String phase:PHASES)for(String key:new String[]{curveId(phase),growthId(phase),productionId(phase),officerId(phase),limitId(phase)})
            result.put(key,data!=null&&data.has(key)?data.opt(key):fallback.get(key));
        return result;
    }
    public static void applyCurves(Map<String,Object> values)throws Exception{
        if(!Global.getSettings().isDevMode())return;
        String mod=IIRT_Omega_ModPlugin.getModId();JSONUtils.CommonDataJSONObject data=LunaSettingsLoader.getSettings().get(mod);
        if(data==null||schema.isEmpty())throw new IllegalStateException("Difficulty settings are not initialized");
        for(Map.Entry<String,Object> e:values.entrySet()){
            if(e.getKey().startsWith("PTSD_curve_"))validateCurve(e.getKey(),String.valueOf(e.getValue()));
        }
        for(Map.Entry<String,Object> e:values.entrySet())data.put(e.getKey(),e.getValue());
        // Applying a graph is an explicit custom edit, not a request to load an older preset.
        data.put(MODE,CUSTOM);data.put("_PTSD_previousDifficulty",CUSTOM);
        reconcile();if(profileError!=null)throw new IllegalStateException(profileError);
        Map<String,Object> saved=curveValues();saved.put(MODE,CUSTOM);
        syncLunaDraft(mod,saved);
        LunaSettings.reportSettingsChanged(mod);
    }
    private static void syncLunaDraft(String mod,Map<String,Object> saved){
        // Luna retains values from previously visited tabs and writes them again on Save Settings.
        // Replace only the keys owned by this apply; unrelated edits and other mods remain pending.
        List<ChangedSetting> pending=LunaSettingsUISettingsPanel.Companion.getChangedSettings();
        pending.removeIf(c->mod.equals(c.getModID())&&saved.containsKey(c.getFieldID()));
        for(Map.Entry<String,Object> e:saved.entrySet())pending.add(new ChangedSetting(mod,e.getKey(),e.getValue()));
        for(lunalib.backend.ui.components.base.LunaUIBaseElement element:LunaSettingsUISettingsPanel.Companion.getAddedElements()){
            if(!(element.getKey() instanceof LunaSettingsData))continue;
            LunaSettingsData field=(LunaSettingsData)element.getKey();
            if(!mod.equals(field.getModID())||!saved.containsKey(field.getFieldID()))continue;
            Object value=saved.get(field.getFieldID());
            if(element instanceof lunalib.backend.ui.components.base.LunaUITextField)
                ((lunalib.backend.ui.components.base.LunaUITextField)element).updateValue(value);
            else if(element instanceof lunalib.backend.ui.components.LunaUITextFieldWithSlider)
                ((lunalib.backend.ui.components.LunaUITextFieldWithSlider)element).updateValue(value);
            else if(element instanceof lunalib.backend.ui.components.LunaUIRadioButton)
                ((lunalib.backend.ui.components.LunaUIRadioButton)element).setValue(String.valueOf(value));
        }
    }
    public static boolean spawningPaused(){
        if(curves.isEmpty()||Global.getSector()==null)return false;
        Object saved=Global.getSector().getPersistentData().get(PTSDCrisisState.PERSISTENT_KEY);
        return saved instanceof PTSDCrisisState && scale((PTSDCrisisState)saved,PTSDCrisisState.getDay())<=0;
    }
    static boolean same(Object a,Object b){
        if(a instanceof Number&&b instanceof Number)return Double.compare(((Number)a).doubleValue(),((Number)b).doubleValue())==0;
        return Objects.equals(a,b);
    }
}
