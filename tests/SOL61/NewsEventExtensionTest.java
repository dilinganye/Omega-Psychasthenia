import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import data.scripts.campaign.invasion.*;
import data.scripts.campaign.invasion.news.*;
import org.json.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
import java.util.function.BiFunction;

/** Exercises the actual eventLoc loader, full migrated configuration, and runtime callbacks. */
public class NewsEventExtensionTest {
    static String testDirectory(){return System.getProperty("ptsd.newsTestDir","build/sol61-news-api-20261028");}
    static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    @SuppressWarnings("unchecked") static <T>T proxy(Class<T> type,BiFunction<String,Object[],Object> calls){
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},(p,m,a)->{
            Object v=calls.apply(m.getName(),a);if(v!=null)return v;
            if(m.getReturnType()==boolean.class)return false;if(m.getReturnType()==float.class)return 0f;
            if(m.getReturnType()==int.class)return 0;if(m.getReturnType()==long.class)return 0L;
            if(List.class.isAssignableFrom(m.getReturnType()))return Collections.emptyList();return null;
        });
    }
    public static SettingsAPI settings(){return proxy(SettingsAPI.class,(n,a)->{
        if(n.equals("getScriptClassLoader"))throw new AssertionError("Production must use game script factory");
        if(n.equals("getInstanceOfScript"))try{
            ClassLoader loader=NewsEventExtensionTest.class.getClassLoader();
            if(((String)a[0]).startsWith("example.ptsd."))loader=new java.net.URLClassLoader(new java.net.URL[]{
                    Paths.get(testDirectory(),"dlc-example/jars/ptsd-news-example.jar").toUri().toURL()},loader);
            return Class.forName((String)a[0],true,loader).getDeclaredConstructor().newInstance();}
            catch(Exception ex){throw new RuntimeException(ex);}
        return null;
    });}
    public static class ExternalEvent implements NewsEvent,PTSDNewsSiteAPI.SiteHandler {
        static int accepted,finished,updates,cleaned,confirmed;
        public void configure(NewsEventDefinition d){d.templates="PORT_RECORDS";d.martialSite="FALSE";d.manualCompletion=true;}
        public void onAccepted(NewsEventContext c){accepted++;c.data().put("dlc.branch","retained");}
        public void advanceIncident(NewsEventContext c){updates++;}
        public void onFinished(NewsEventContext c){finished++;}
        public PTSDNewsSiteAPI.SiteResult materialize(PTSDNewsSiteAPI.SiteContext c){
            return new PTSDNewsSiteAPI.SiteResult(c.targetLocation,"Borrowed relay","Evidence","Inspect",Collections.emptyList());
        }
        public void onExpired(PTSDNewsSiteAPI.SiteContext c){cleaned++;}
        public void onConfirmed(PTSDNewsSiteAPI.SiteContext c){confirmed++;}
    }
    static JSONArray read(String name)throws Exception{return new JSONArray(new String(Files.readAllBytes(Paths.get(testDirectory(),name)),StandardCharsets.UTF_8));}
    static Map<String,JSONObject> indexed(JSONArray rows)throws Exception{
        Map<String,JSONObject> map=new LinkedHashMap<>();for(int n=0;n<rows.length();n++){
            JSONObject row=rows.getJSONObject(n);String id=row.optString("id");
            if(!id.isEmpty()&&!id.startsWith("#"))map.put(id,row);
        }return map;
    }
    public static void main(String[] args)throws Exception{
        SettingsAPI old=Global.getSettings();SectorAPI sector=Global.getSector();
        try{
            Global.setSettings(settings());
            JSONObject dlc=NewsEventLoader.prepare(new JSONObject().put("id","DLC_EXAMPLE_01").put("eventLoc","example.ptsd.news.PortRecordEvent"));
            check(dlc.getString("target").equals("CUSTOM(example.ptsd.news.PortRecordEvent)")&&
                    dlc.getString("siteHandler").equals("example.ptsd.news.PortRecordEvent"),"Independent DLC JAR not loaded/bridged");
            Map<String,JSONObject> before=indexed(read("migration-baseline.json")),after=indexed(read("news-after.json"));
            check(before.size()==174&&before.keySet().equals(after.keySet()),"Lost/duplicated news rows");
            Set<String> numbers=new HashSet<>(Arrays.asList("recon","awareness","aggression","panic","distortion","physicalChance","strength","minDay","priority"));
            for(String id:before.keySet()){
                JSONObject original=before.get(id),raw=after.get(id),expanded=NewsEventLoader.prepare(raw);
                check(raw.length()==16,"Expected compact 16-column schema");
                check(!raw.has("siteTemplates"),"Preparation modified original merged row");
                if(id.equals("P-10"))check(expanded.optString("target").equals("CUSTOM(data.scripts.campaign.invasion.news.defaults.P10Event)")&&
                        expanded.optDouble("physicalChance")==0&&expanded.optString("martialSite").equals("FALSE")&&
                        expanded.optBoolean("eventManualCompletion")&&Math.abs(raw.optDouble("eventWeight")-.8)<.00001,"P-10 approved redesign config missing");
                for(Iterator<?> it=original.keys();it.hasNext();){String k=(String)it.next();
                    // 2026-10-29用户批准P-10独立回收/尾随/截击，不再是旧通用现场。
                    if(id.equals("P-10")&&Arrays.asList("target","siteHandler","martialSite","physicalChance","eventWeight").contains(k))continue;
                    if(k.equals("target")&&original.optString(k).startsWith("CUSTOM(")){
                        check(NewsEventLoader.resolve(raw.getString("eventLoc")) instanceof PTSDCrisisNewsAPI.CustomNewsHandler,"Lost custom target");continue;
                    }
                    if(numbers.contains(k)){
                        double fallback=k.equals("priority")?1:0;
                        check(Math.abs(original.optDouble(k,fallback)-expanded.optDouble(k,fallback))<.00001,id+":"+k+" numeric changed");
                    }else{
                        String a=original.optString(k,""),b=expanded.optString(k,"");
                        if((k.equals("siteTemplates")||k.equals("martialSite"))){if(a.isEmpty())a="AUTO";if(b.isEmpty())b="AUTO";
                            if(k.equals("martialSite")&&original.optBoolean("filler",false)&&a.equals("FALSE")&&b.equals("AUTO"))continue;}
                        if(k.equals("visibility")){if(a.isEmpty())a="PUBLIC";if(b.isEmpty())b="PUBLIC";}
                        check(a.equals(b),id+":"+k+" text/config changed: "+a+" -> "+b);
                    }
                }
            }
            // Exercise the production merge/parse path, not just individual definitions.
            JSONArray merged=read("news-after.json");
            JSONObject extra=new JSONObject().put("id","DLC_MERGE_TEST").put("phases","RECON")
                    .put("category","暗流").put("investigable",true).put("eventLoc","example.ptsd.news.PortRecordEvent");
            merged.put(extra);
            final int[] merges={0};SettingsAPI factory=settings();
            Global.setSettings(proxy(SettingsAPI.class,(n,a)->{
                if(n.equals("getInstanceOfScript"))return factory.getInstanceOfScript((String)a[0]);
                if(n.equals("getMergedSpreadsheetDataForMod")){
                    check(a[2].equals("Omega_PTSD"),"Wrong master mod");merges[0]++;return merged;
                }
                if(n.equals("getModManager"))throw new AssertionError("Repeated per-mod merge returned");
                return null;
            }));
            Field loaded=PTSDCrisisIncidentManager.class.getDeclaredField("loaded");loaded.setAccessible(true);loaded.set(null,false);
            check(PTSDCrisisIncidentManager.getTemplate("DLC_MERGE_TEST")!=null,"DLC row absent from production loader");
            for(String id:before.keySet())check(PTSDCrisisIncidentManager.getTemplate(id)!=null,"Production loader rejected "+id);
            check(merges[0]==1,"CSV merged more than once");
            String key=ExternalEvent.class.getName();JSONObject raw=new JSONObject().put("id","dlc_unique").put("eventLoc",key);
            JSONObject configured=NewsEventLoader.prepare(raw);
            check(configured.getString("siteHandler").equals(key)&&configured.getBoolean("eventManualCompletion"),"DLC handler not bridged");
            check(PTSDNewsSiteAPI.resolveHandler(key)==NewsEventLoader.resolve(key),"Duplicate runtime instance");
            for(String bad:new String[]{"no.such.Event","java.lang.String"}){
                boolean rejected=false;try{NewsEventLoader.prepare(new JSONObject().put("eventLoc",bad));}catch(IllegalArgumentException expected){rejected=true;}
                check(rejected,"Invalid event silently became AUTO");
            }
            Constructor<PTSDCrisisState> ctor=PTSDCrisisState.class.getDeclaredConstructor();ctor.setAccessible(true);PTSDCrisisState state=ctor.newInstance();
            PTSDCrisisState.CrisisIncident i=new PTSDCrisisState.CrisisIncident();i.id="dlc-incident";i.cardId="dlc_unique";
            i.eventLoc=key;i.siteHandlerExpression=key;i.siteTemplate="PORT_RECORDS";i.eventManualCompletion=true;
            i.recordedByPlayer=true;i.investigable=true;i.investigationOutcome=1;i.targetSystemId="system";i.targetEntityId="relay";
            i.investigationExpiresDay=30;state.incidents.add(i);
            check(!PTSDNewsScene.manages(i),"Built-in template swallowed custom handler");
            NewsEventLoader.advance(state,1);NewsEventLoader.advance(state,2);
            check(ExternalEvent.accepted==1&&ExternalEvent.updates==2,"Acceptance/update counts");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();new ObjectOutputStream(bytes).writeObject(i);
            i=(PTSDCrisisState.CrisisIncident)new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();
            check(i.eventAccepted&&"retained".equals(i.eventData.get("dlc.branch")),"Persistent branch lost");
            state.incidents.clear();state.incidents.add(i);
            NewsEventLoader.advance(state,3);check(ExternalEvent.accepted==1,"Reload repeated accept");
            final int[] removals={0},reports={0};
            StarSystemAPI sys=proxy(StarSystemAPI.class,(n,a)->n.equals("getId")?"system":n.equals("removeEntity")?++removals[0]:null);
            SectorEntityToken relay=proxy(SectorEntityToken.class,(n,a)->n.equals("getId")?"relay":n.equals("getContainingLocation")||n.equals("getStarSystem")?sys:n.equals("getLocation")?new org.lwjgl.util.vector.Vector2f():null);
            CargoAPI cargo=proxy(CargoAPI.class,(n,a)->{if(n.equals("addSpecial"))reports[0]+=((Number)a[1]).intValue();return null;});
            CampaignFleetAPI player=proxy(CampaignFleetAPI.class,(n,a)->n.equals("getCargo")?cargo:null);
            Global.setSector(proxy(SectorAPI.class,(n,a)->n.equals("getStarSystems")?Collections.singletonList(sys):n.equals("getEntityById")?relay:n.equals("getPlayerFleet")?player:n.equals("getClock")?proxy(CampaignClockAPI.class,(x,y)->null):null));
            check(PTSDNewsSiteManager.materialize(state,i,new Random(1),false,false),"External scene did not materialize");
            check(i.siteEntityIds.isEmpty(),"Borrowed relay acquired deletion ownership");
            NewsEventContext context=new NewsEventContext(state,i,4);context.finish(PTSDTaskRules.InvestigationResult.INCONCLUSIVE,true);
            context.finish(PTSDTaskRules.InvestigationResult.INCONCLUSIVE,true);
            check(reports[0]==1,"Partial evidence reward duplicated/lost");
            NewsEventLoader.advance(state,4);NewsEventLoader.advance(state,5);check(ExternalEvent.finished==1,"Completion callback repeated");
            PTSDNewsSiteManager.cleanup(state,i,false);PTSDNewsSiteManager.cleanup(state,i,false);
            check(removals[0]==0&&ExternalEvent.cleaned==1,"Borrowed relay removed or cleanup repeated");
            PTSDCrisisState.CrisisIncident complete=new PTSDCrisisState.CrisisIncident();
            complete.id="full";complete.recordedByPlayer=true;complete.investigable=true;complete.siteHandlerExpression=key;
            complete.targetSystemId="system";complete.targetEntityId="relay";
            NewsEventContext completed=new NewsEventContext(state,complete,6);
            completed.finish(PTSDTaskRules.InvestigationResult.CONFIRMED,true);
            completed.finish(PTSDTaskRules.InvestigationResult.CONFIRMED,true);
            check(reports[0]==4&&ExternalEvent.confirmed==1,"Full report/confirm callback duplicated or missing");
            System.out.println("NewsEventExtensionTest passed: 174 rows preserved, runtime DLC, persistence, lifecycle, borrowed-entity safety, report settlement");
        }finally{Global.setSettings(old);Global.setSector(sector);}
    }
}
