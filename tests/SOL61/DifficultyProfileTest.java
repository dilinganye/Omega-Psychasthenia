package data.scripts;
import com.fs.starfarer.api.*;
import data.scripts.campaign.invasion.PTSDCrisisState;
import lunalib.backend.ui.settings.*;
import org.lazywizard.lazylib.JSONUtils;
import org.json.JSONObject;
import java.lang.reflect.*;
import java.util.*;
public final class DifficultyProfileTest {
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    public static void main(String[] args)throws Exception{
        final String exports=PTSDDifficultySettings.SNAPSHOT_ROOT;
        final Map<String,String> writes=new HashMap<String,String>();
        Global.setSettings((SettingsAPI)Proxy.newProxyInstance(SettingsAPI.class.getClassLoader(),new Class[]{SettingsAPI.class},(o,m,a)->{
            if(m.getName().equals("getModManager"))return Proxy.newProxyInstance(ModManagerAPI.class.getClassLoader(),new Class[]{ModManagerAPI.class},(x,y,z)->y.getName().equals("getEnabledModsCopy")?Collections.emptyList():y.getName().equals("getModSpec")?Proxy.newProxyInstance(ModSpecAPI.class.getClassLoader(),new Class[]{ModSpecAPI.class},(q,r,t)->r.getName().equals("getPath")?exports.toString():null):null);
            if(m.getName().equals("loadJSON"))return new JSONObject("{简单:{PTSD_test:2,PTSD_hidden:8},中等:{},困难:{},天灾:{}}");
            if(m.getName().equals("isDevMode"))return true;
            if(m.getName().equals("fileExistsInCommon"))return writes.containsKey((String)a[0]);
            if(m.getName().equals("readTextFileFromCommon"))return writes.get((String)a[0]);
            if(m.getName().equals("writeTextFileToCommon")){writes.put((String)a[0],(String)a[1]);return null;}
            if(m.getReturnType()==com.fs.starfarer.api.graphics.SpriteAPI.class)return Proxy.newProxyInstance(com.fs.starfarer.api.graphics.SpriteAPI.class.getClassLoader(),new Class[]{com.fs.starfarer.api.graphics.SpriteAPI.class},(q,r,t)->r.getReturnType()==float.class?16f:null);
            if(m.getReturnType()==float.class)return 1000f;
            if(m.getReturnType()==boolean.class)return false;
            if(m.getReturnType()==int.class)return 0;
            if(m.getReturnType()==java.awt.Color.class)return java.awt.Color.CYAN;
            return null;
        }));
        IIRT_Omega_ModPlugin.hasLunaLib=true;String mod=IIRT_Omega_ModPlugin.getModId();
        LunaSettingsLoader.getSettingsData().add(new LunaSettingsData(mod,"PTSD_test","test","Int","",1,"",0,100,""));
        LunaSettingsLoader.getSettingsData().add(new LunaSettingsData(mod,"PTSD_hidden","hidden","Int","",3,"",0,100,""));
        LunaSettingsLoader.getSettingsData().add(new LunaSettingsData(mod,"PTSD_news_true_chance","truth","Int","",90,"",0,100,""));
        PTSDDifficultySettings.registerCurves(mod);PTSDDifficultySettings.capture(mod);
        LunaSettingsLoader.getSettingsData().removeIf(d->d.getFieldID().equals("PTSD_hidden"));PTSDDifficultySettings.hideCurves(mod);
        JSONUtils.CommonDataJSONObject data=new JSONUtils.CommonDataJSONObject("test-only");
        data.put(PTSDDifficultySettings.MODE,"中等");LunaSettingsLoader.getSettings().put(mod,data);
        PTSDDifficultySettings.reconcile();
        data.put(PTSDDifficultySettings.MODE,"简单");PTSDDifficultySettings.reconcile();
        check(data.getInt("PTSD_hidden")==8,"Hidden preset ignored");
        data.put("PTSD_test",7);data.put("PTSD_news_true_chance",85);PTSDDifficultySettings.reconcile();
        check(data.getString(PTSDDifficultySettings.MODE).equals("自定义"),"Manual edit not custom");
        check(data.getInt("PTSD_hidden")==8,"Public edit erased hidden preset");
        data.put(PTSDDifficultySettings.MODE,"中等");PTSDDifficultySettings.reconcile();check(data.getInt("PTSD_test")==1,"Preset not restored");
        data.put(PTSDDifficultySettings.MODE,"自定义");PTSDDifficultySettings.reconcile();check(data.getInt("PTSD_test")==7&&data.getInt("PTSD_hidden")==8,"Custom not restored");
        check(writes.containsKey(exports+"latest.json"),"No export");
        check(data.getInt("PTSD_news_true_chance")==85&&new JSONObject(writes.get(exports+"latest.json")).getInt("PTSD_news_true_chance")==85,"Truth setting missing from custom restoration/export");
        data.put("PTSD_curve_recon","1,1.1,1.5,2");PTSDDifficultySettings.reconcile();
        check(data.getString("PTSD_curve_recon").equals("1,1.1,1.5,2"),"Lost multi-point curve");
        java.lang.reflect.Constructor<data.scripts.campaign.invasion.PTSDCrisisState> ctor=data.scripts.campaign.invasion.PTSDCrisisState.class.getDeclaredConstructor();ctor.setAccessible(true);
        data.scripts.campaign.invasion.PTSDCrisisState state=ctor.newInstance();state.phase=PTSDCrisisState.Phase.RECON;state.phaseStartedDay=0;
        data.put("PTSD_curve_recon","5,2");PTSDDifficultySettings.reconcile();
        check(Math.abs(PTSDDifficultySettings.scale(state,0)-5)<.001,"Initial spike incorrect");
        check(Math.abs(PTSDDifficultySettings.scale(state,IIRT_Omega_ModPlugin.collect_data_time*2)-1)<.001,"End ratio incorrect");
        data.put("PTSD_curve_recon","1,0,0,2");PTSDDifficultySettings.reconcile();
        check(PTSDDifficultySettings.scale(state,IIRT_Omega_ModPlugin.collect_data_time)==0,"Zero plateau not paused");
        data.put("PTSD_curve_recon","1,4");PTSDDifficultySettings.reconcile();
        check(Math.abs(PTSDDifficultySettings.scale(state,IIRT_Omega_ModPlugin.collect_data_time*2)-2)<.001,"Doubled endpoint incorrect");
        state.phase=PTSDCrisisState.Phase.EXPANSION;
        check(PTSDDifficultySettings.scale(state,0)==1,"Phase curve leaked into next phase");
        state.phase=PTSDCrisisState.Phase.RECON;
        data.put("PTSD_curve_recon","1,2");data.put("PTSD_curve_growth_recon","2,2");
        data.put("PTSD_growth_limit_recon",0);PTSDDifficultySettings.reconcile();
        float end=IIRT_Omega_ModPlugin.collect_data_time*2;
        check(PTSDDifficultySettings.growthProgress(state,end)==1,"Capped stage exceeded endpoint");
        check(Math.abs(PTSDDifficultySettings.scale(state,end)-1)<.001,"Capped endpoint changed default strength");
        data.put("PTSD_growth_limit_recon",1);PTSDDifficultySettings.reconcile();
        check(PTSDDifficultySettings.growthProgress(state,end)==2,"Uncapped growth was clipped");
        check(PTSDDifficultySettings.scale(state,end)>1,"Uncapped growth did not raise budget");
        data.put("PTSD_curve_growth_recon","0,0");PTSDDifficultySettings.reconcile();
        check(PTSDDifficultySettings.growthProgress(state,end)==0,"Zero rate did not pause growth");
        check(Math.abs(PTSDDifficultySettings.scale(state,end)-12f/36f)<.001,"Paused growth did not retain initial recon budget");
        data.put("PTSD_curve_production_recon","0,0");PTSDDifficultySettings.reconcile();
        check(PTSDDifficultySettings.productionScale(state,end)==0,"Zero production curve ignored");
        state.phase=PTSDCrisisState.Phase.EXPANSION;
        check(PTSDDifficultySettings.productionScale(state,0)==1,"Production curve leaked across phase boundary");
        check(Math.abs(PTSDStageCurve.integral(new double[]{1,3},1)-2)<.000001,"Linear growth integral wrong");
        check(Math.abs(PTSDStageCurve.integral(new double[]{1,3},2)-5)<.000001,"Extended growth did not use endpoint rate");
        data.put("PTSD_curve_recon","1,4");PTSDDifficultySettings.reconcile();
        state.phase=PTSDCrisisState.Phase.RECON;
        data.put("PTSD_curve_officer_recon","2,10,20");PTSDDifficultySettings.reconcile();
        check(PTSDDifficultySettings.officerLevel(state,0)==2,"Officer curve start ignored");
        check(PTSDDifficultySettings.officerLevel(state,end/2)==10,"Officer control point ignored");
        check(PTSDDifficultySettings.officerLevel(state,end)==20,"Officer endpoint ignored");
        data.put("PTSD_curve_officer_recon","0,30");PTSDDifficultySettings.reconcile();
        check(data.getString("PTSD_curve_officer_recon").equals("2,10,20"),"Invalid officer curve did not retain valid values");
        state.phase=PTSDCrisisState.Phase.EXPANSION;
        check(PTSDDifficultySettings.officerLevel(state,0)==9,"Officer phase boundary not independent");
        check(!PTSDDifficultySettings.managed("PTSD_omega_officer_level_bonus"),"Retired setting still managed");
        JSONObject latest=new JSONObject(writes.get(exports+"latest.json"));
        check(latest.getString("PTSD_curve_recon").equals("1,4"),"Export is not a flat preset-ready snapshot");
        long before=writes.keySet().stream().filter(k->k.startsWith(exports+"history/")).count();
        PTSDDifficultySettings.reconcile();
        check(writes.keySet().stream().filter(k->k.startsWith(exports+"history/")).count()==before,"Unchanged refresh created history spam");
        // Reproduce: visit numeric/preset tabs, edit graph, apply, then Luna Save Settings.
        data.put(PTSDDifficultySettings.MODE,"中等");PTSDDifficultySettings.reconcile();
        java.util.List<ChangedSetting> draft=LunaSettingsUISettingsPanel.Companion.getChangedSettings();
        draft.clear();
        draft.add(new ChangedSetting(mod,PTSDDifficultySettings.MODE,"中等"));
        draft.add(new ChangedSetting(mod,"PTSD_curve_recon","1,2"));
        draft.add(new ChangedSetting(mod,"PTSD_test",17));
        draft.add(new ChangedSetting("other-mod","example",42));
        Map<String,Object> graph=PTSDDifficultySettings.curveValues();
        graph.put("PTSD_curve_recon","5,3,2");graph.put("PTSD_curve_growth_recon","2,3");
        graph.put("PTSD_curve_production_recon","4,5");graph.put("PTSD_curve_officer_recon","9,16");graph.put("PTSD_growth_limit_recon",1);
        PTSDDifficultySettings.applyCurves(graph);
        check(data.getString(PTSDDifficultySettings.MODE).equals("自定义"),"Graph apply did not select custom");
        // Luna reloads the JSON, overlays its tab drafts, then calls the settings listeners.
        JSONUtils.CommonDataJSONObject reloaded=new JSONUtils.CommonDataJSONObject("test-reloaded");
        for(Iterator<?> keys=data.keys();keys.hasNext();){String k=(String)keys.next();reloaded.put(k,data.get(k));}
        for(ChangedSetting c:draft)if(mod.equals(c.getModID()))reloaded.put(c.getFieldID(),c.getData());
        LunaSettingsLoader.getSettings().put(mod,reloaded);PTSDDifficultySettings.reconcile();
        for(Map.Entry<String,Object> e:graph.entrySet())check(PTSDDifficultySettings.same(e.getValue(),PTSDDifficultySettings.curveValues().get(e.getKey())),"Luna save reverted "+e.getKey());
        check(reloaded.getInt("PTSD_test")==17,"Unrelated pending edit lost");
        check(draft.stream().anyMatch(c->c.getModID().equals("other-mod")),"Other mod draft erased");
        check(new JSONObject(writes.get(exports+"latest.json")).getString("PTSD_curve_recon").equals("5,3,2"),"Export reverted after Luna save");
        reloaded.put(PTSDDifficultySettings.MODE,"中等");PTSDDifficultySettings.reconcile();
        check(reloaded.getString("PTSD_curve_recon").equals("1,2"),"Later explicit preset switch blocked");
        reloaded.put(PTSDDifficultySettings.MODE,"自定义");PTSDDifficultySettings.reconcile();
        check(reloaded.getString("PTSD_curve_recon").equals("5,3,2"),"Returning to custom lost graph");
        draft.clear();
        check(PTSDDifficultyCurveEditor.dragValue(0,3.62,true)==4,"Alt did not round");
        check(PTSDDifficultyCurveEditor.dragValue(0,3.62,false)==3.62,"Normal drag lost decimals");
        check(PTSDDifficultyCurveEditor.dragValue(3,1.1,true)==2&&PTSDDifficultyCurveEditor.dragValue(3,22,true)==20,"Officer bounds lost");
        testModalEditor();
        System.out.println("DifficultyProfileTest passed (hidden preset, custom restore, export, curve preservation)");
    }
    static void testModalEditor() {
        com.fs.starfarer.api.campaign.FactionAPI faction=(com.fs.starfarer.api.campaign.FactionAPI)Proxy.newProxyInstance(com.fs.starfarer.api.campaign.FactionAPI.class.getClassLoader(),new Class[]{com.fs.starfarer.api.campaign.FactionAPI.class},(o,m,a)->m.getReturnType()==java.awt.Color.class?java.awt.Color.CYAN:null);
        Global.setSector((com.fs.starfarer.api.campaign.SectorAPI)Proxy.newProxyInstance(com.fs.starfarer.api.campaign.SectorAPI.class.getClassLoader(),new Class[]{com.fs.starfarer.api.campaign.SectorAPI.class},(o,m,a)->m.getName().equals("getPlayerFaction")?faction:null));
        final java.util.List<com.fs.starfarer.api.campaign.CustomUIPanelPlugin> plugins=new ArrayList<>();
        final java.util.List<String> labels=new ArrayList<>();
        com.fs.starfarer.api.ui.PositionAPI pos=(com.fs.starfarer.api.ui.PositionAPI)Proxy.newProxyInstance(
            com.fs.starfarer.api.ui.PositionAPI.class.getClassLoader(),new Class[]{com.fs.starfarer.api.ui.PositionAPI.class},(o,m,a)->{
                if(m.getName().equals("getWidth"))return 900f;
                if(m.getName().equals("getHeight"))return 900f;
                if(m.getReturnType()==com.fs.starfarer.api.ui.PositionAPI.class)return o;
                if(m.getReturnType()==float.class)return 0f;return null;
            });
        com.fs.starfarer.api.ui.LabelAPI label=(com.fs.starfarer.api.ui.LabelAPI)Proxy.newProxyInstance(
            com.fs.starfarer.api.ui.LabelAPI.class.getClassLoader(),new Class[]{com.fs.starfarer.api.ui.LabelAPI.class},(o,m,a)->m.getName().equals("getPosition")?pos:null);
        com.fs.starfarer.api.ui.TooltipMakerAPI tooltip=(com.fs.starfarer.api.ui.TooltipMakerAPI)Proxy.newProxyInstance(
            com.fs.starfarer.api.ui.TooltipMakerAPI.class.getClassLoader(),new Class[]{com.fs.starfarer.api.ui.TooltipMakerAPI.class},(o,m,a)->{
                if(m.getName().equals("addPara")){labels.add((String)a[0]);return label;}
                if(m.getName().equals("getPosition"))return pos;return null;
            });
        com.fs.starfarer.api.ui.CustomPanelAPI panel=(com.fs.starfarer.api.ui.CustomPanelAPI)Proxy.newProxyInstance(
            com.fs.starfarer.api.ui.CustomPanelAPI.class.getClassLoader(),new Class[]{com.fs.starfarer.api.ui.CustomPanelAPI.class},(o,m,a)->{
                if(m.getName().equals("hashCode"))return System.identityHashCode(o);
                if(m.getName().equals("equals"))return o==a[0];
                if(m.getName().equals("createCustomPanel")){plugins.add((com.fs.starfarer.api.campaign.CustomUIPanelPlugin)a[2]);return o;}
                if(m.getName().equals("createUIElement"))return tooltip;
                if(m.getReturnType()==com.fs.starfarer.api.ui.PositionAPI.class)return pos;return null;
            });
        LunaSettingsUIMainPanel main=new LunaSettingsUIMainPanel(false);main.setPanel(panel);
        lunalib.backend.ui.OpenCustomPanelFromDialog opener=new lunalib.backend.ui.OpenCustomPanelFromDialog(main);
        com.fs.starfarer.api.campaign.InteractionDialogAPI dialog=(com.fs.starfarer.api.campaign.InteractionDialogAPI)Proxy.newProxyInstance(
            com.fs.starfarer.api.campaign.InteractionDialogAPI.class.getClassLoader(),new Class[]{com.fs.starfarer.api.campaign.InteractionDialogAPI.class},(o,m,a)->m.getName().equals("getPlugin")?opener:null);
        PTSDDifficultyCurveEditor.attach(dialog);PTSDDifficultyCurveEditor.attach(dialog);
        check(plugins.size()==1,"Modal updater installed more than once");
        com.fs.starfarer.api.campaign.CustomUIPanelPlugin updater=plugins.get(0);
        LunaSettingsData marker=new LunaSettingsData(IIRT_Omega_ModPlugin.getModId(),"PTSD_curve_editor","editor","String","","","",0,0,"曲线编辑");
        lunalib.backend.ui.components.base.LunaUIBaseElement field=new lunalib.backend.ui.components.base.LunaUIBaseElement(200,30,marker,"test",panel,tooltip){public void render(float alpha){} public void renderBelow(float alpha){}};
        LunaSettingsUISettingsPanel.Companion.getAddedElements().add(field);
        updater.advance(.1f);updater.advance(.1f);
        check(plugins.stream().filter(p->p instanceof PTSDDifficultyCurveEditor).count()==1,"Curve editor missing or duplicated during modal advance");
        check(labels.contains("基础难度调控")&&labels.contains("舰队强度增长率")&&labels.contains("战略资产生产速度")&&labels.contains("军官等级（绝对等级，2～20）"),"Four charts not built");
        LunaSettingsUISettingsPanel.Companion.getAddedElements().clear();
    }

}

