import data.scripts.campaign.invasion.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Every shipped row validates through Card, not through a duplicate delivery parser. */
public final class NewsDeliveryAuditTest {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void main(String[] args) throws Exception {
        com.fs.starfarer.api.Global.setSettings(NewsEventExtensionTest.settings());
        Class<?> cardType = Class.forName(PTSDCrisisIncidentManager.class.getName()+"$Card");
        Constructor<?> constructor = cardType.getDeclaredConstructors()[0]; constructor.setAccessible(true);
        List<Object> cards = new ArrayList<>(); String[] header = null; Object[] last = null;
        Method parseTarget=PTSDCrisisIncidentManager.class.getDeclaredMethod("parseTarget",String.class);parseTarget.setAccessible(true);
        for (String line : Files.readAllLines(Paths.get("data/config/ptsdCSV/PTSD_crisis_news.csv"), StandardCharsets.UTF_8)) {
            String[] cells = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
            if (header == null) { header = cells; continue; }
            if (cells[0].trim().isEmpty() || cells[0].startsWith("#")) continue;
            Map<String,String> row = new HashMap<>();
            for(int i=0;i<Math.min(header.length,cells.length);i++) row.put(header[i], cells[i]);
            org.json.JSONObject expanded=data.scripts.campaign.invasion.news.NewsEventLoader.prepare(new org.json.JSONObject(row));
            for(java.util.Iterator<?> keys=expanded.keys();keys.hasNext();){String key=(String)keys.next();row.put(key,expanded.optString(key));}
            String target=row.getOrDefault("target", "");
            if(target.startsWith("\"") && target.endsWith("\"")) target=target.substring(1,target.length()-1).replace("\"\"","\"");
            parseTarget.invoke(null,target);
            Class<?>[] types = constructor.getParameterTypes(); Object[] values = new Object[types.length];
            for(int i=0;i<types.length;i++) {
                if(types[i]==String.class) values[i]="";
                else if(types[i]==String[].class) values[i]=new String[]{"","",""};
                else if(types[i]==boolean.class) values[i]=false;
                else if(types[i]==float.class) values[i]=0f;
                else if(types[i].isEnum()) values[i]=Enum.valueOf((Class)types[i],"ANY");
            }
            values[0]=row.get("id"); values[1]=row.get("category"); values[2]=row.get("phases");
            values[4]=Boolean.parseBoolean(row.get("filler")); values[8]=Float.parseFloat(row.get("weight"));
            values[25]=row.get("delivery"); values[27]=row.getOrDefault("trigger","");
            Object card = constructor.newInstance(values);
            Field jeOnly = constructor.getDeclaringClass().getDeclaredField("jeOnly"); jeOnly.setAccessible(true);
            jeOnly.setBoolean(card, Boolean.parseBoolean(row.getOrDefault("JeOnly", "")));
            Field multiplier=constructor.getDeclaringClass().getDeclaredField("eventWeight");multiplier.setAccessible(true);
            String ew=row.getOrDefault("eventWeight","");multiplier.setFloat(card,ew.isEmpty()?1:Float.parseFloat(ew));
            cards.add(card); last=values;
        }
        if(cards.size()!=174) throw new AssertionError("Expected all 174 valid CSV rows");
        last[25]="BOGUS";
        try { constructor.newInstance(last); throw new AssertionError("Unknown delivery became ambient"); }
        catch(InvocationTargetException expected) { if(!(expected.getCause() instanceof IllegalArgumentException)) throw expected; }
        parseTarget.invoke(null, "");
        try { parseTarget.invoke(null, "INVALID_TARGET"); throw new AssertionError("Unknown target became ANY"); }
        catch(InvocationTargetException expected) { if(!(expected.getCause() instanceof IllegalArgumentException)) throw expected; }
        last[25]="CONDITIONAL";last[27]="BOGUS";
        try { constructor.newInstance(last); throw new AssertionError("Unknown trigger accepted"); }
        catch(InvocationTargetException expected) { if(!(expected.getCause() instanceof IllegalArgumentException)) throw expected; }
        Field loaded=PTSDCrisisIncidentManager.class.getDeclaredField("loaded");loaded.setAccessible(true);loaded.set(null,true);
        for(String name:new String[]{"DARK","PROBE","NEWS"}) {
            Field f=PTSDCrisisIncidentManager.class.getDeclaredField(name);f.setAccessible(true);
            ((List)f.get(null)).clear();if(name.equals("DARK"))((List)f.get(null)).addAll(cards);
        }
        Constructor<PTSDCrisisState> sc=PTSDCrisisState.class.getDeclaredConstructor();sc.setAccessible(true);
        PTSDCrisisState state=sc.newInstance();state.phase=PTSDCrisisState.Phase.DORMANT;
        Method select=PTSDCrisisIncidentManager.class.getDeclaredMethod("selectPool",PTSDCrisisState.class,boolean.class,Random.class,Boolean.class);select.setAccessible(true);
        Method pick=PTSDCrisisIncidentManager.class.getDeclaredMethod("pick",List.class,PTSDCrisisState.class,float.class,Random.class);pick.setAccessible(true);
        Field id=cardType.getDeclaredField("id"),delivery=cardType.getDeclaredField("delivery");id.setAccessible(true);delivery.setAccessible(true);
        Field category=cardType.getDeclaredField("category");category.setAccessible(true);
        List<?> filler=(List<?>)select.invoke(null,state,false,new Random(1),true);
        for(Object card:filler)state.incidentCooldowns.put((String)id.get(card),10f);
        if(pick.invoke(null,filler,state,0f,new Random(1))!=null)throw new AssertionError("Cooling news selected");
        List<?> alternate=(List<?>)select.invoke(null,state,false,new Random(1),false);
        if(pick.invoke(null,alternate,state,0f,new Random(1))==null)throw new AssertionError("Alternate category lost");
        state.newsIntelSubscribed=false;
        if(!alternate.equals(select.invoke(null,state,false,new Random(1),false)))
            throw new AssertionError("Subscription disabled news generation");
        for(PTSDCrisisState.Phase phase:PTSDCrisisState.Phase.values()) {
            state.phase=phase;
            for(boolean ordinary:new boolean[]{true,false})for(Object card:(List<?>)select.invoke(null,state,false,new Random(1),ordinary))
                if(!"AMBIENT".equals(delivery.get(card)) || "专项调查".equals(category.get(card)))
                    throw new AssertionError("Special delivery leaked into ambient pool");
        }
        for (PTSDCrisisState.Phase phase : PTSDCrisisState.Phase.values()) {
            for (String deliveryKind : new String[]{"AMBIENT", "CONDITIONAL", "SCRIPTED"})
                for (PTSDCrisisIncidentManager.ScriptedTemplate template : PTSDCrisisIncidentManager.getTemplates(deliveryKind, phase))
                    if (template.jeOnly || template.eventWeight<=0) throw new AssertionError("Je-only/disabled card leaked into public templates");
        }
        if (!PTSDCrisisIncidentManager.getTemplate("JE-01").jeOnly ||
                PTSDCrisisIncidentManager.getJeTemplates(PTSDCrisisState.Phase.RECON).isEmpty())
            throw new AssertionError("Je-only pool missing JE-01");
        if(PTSDCrisisIncidentManager.enabled("P-01")||PTSDCrisisIncidentManager.enabled("R2-01"))throw new AssertionError("Disabled duplicates still enabled");
        if(PTSDCrisisIncidentManager.getTemplate("P-01")==null)throw new AssertionError("Disabled event cannot be inspected by DEV");
        for(PTSDCrisisIncidentManager.DevCard card:PTSDCrisisIncidentManager.getDevCardsForPhase(PTSDCrisisState.Phase.RECON))if(!card.phases.contains("RECON")&&!card.phases.equals("ALL"))throw new AssertionError("DEV phase filter mismatch");
        System.out.println("NewsDeliveryAuditTest: passed (174 CSV rows, invalid types, cooldown fallback, phase boundaries)");
    }
}
