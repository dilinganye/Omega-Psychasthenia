import data.scripts.campaign.invasion.PTSDCrisisIncidentManager;
import data.scripts.campaign.invasion.PTSDCrisisState;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Loads real first-stage CSV rows into the actual Card constructor and pool selector. */
public final class DormantNewsPoolTest {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void main(String[] args) throws Exception {
        Class<?> cardType = Class.forName(PTSDCrisisIncidentManager.class.getName() + "$Card");
        Constructor<?> cardConstructor = cardType.getDeclaredConstructors()[0];
        cardConstructor.setAccessible(true);
        Class<?>[] types = cardConstructor.getParameterTypes();
        List<Object> cards = new ArrayList<Object>();
        String[] header = null;
        int blankRows = 0;
        for (String line : Files.readAllLines(Paths.get("data/config/ptsdCSV/PTSD_crisis_news.csv"), StandardCharsets.UTF_8)) {
            String[] cells = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
            if (header == null) { header = cells; continue; }
            Map<String, String> row = new HashMap<String, String>();
            for (int i = 0; i < Math.min(cells.length, header.length); i++) row.put(header[i], cells[i]);
            String phases = row.get("phases");
            if (phases == null || (!"ALL".equals(phases) && !Arrays.asList(phases.split("\\|")).contains("DORMANT"))) continue;
            Object[] values = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                if (types[i] == String.class) values[i] = "";
                else if (types[i] == String[].class) values[i] = new String[]{"", "", ""};
                else if (types[i] == boolean.class) values[i] = false;
                else if (types[i] == float.class) values[i] = 0f;
                else if (types[i].isEnum()) values[i] = Enum.valueOf((Class)types[i], "ANY");
            }
            values[0] = row.get("id"); values[1] = row.get("category"); values[2] = row.get("phases");
            values[4] = Boolean.parseBoolean(row.get("filler")); values[8] = Float.parseFloat(row.get("weight"));
            values[25] = row.get("delivery");
            values[27] = "MARKET";
            Object card = cardConstructor.newInstance(values);
            Field delivery = cardType.getDeclaredField("delivery"); delivery.setAccessible(true);
            if (!"AMBIENT".equals(delivery.get(card))) throw new AssertionError("Blank CSV delivery excluded: " + values[0]);
            for (String raw : new String[]{null, "", "  ", "AMBIENT", " conditional ", "SCRIPTED"}) {
                values[25] = raw;
                String expected = raw == null || raw.trim().isEmpty() ? "AMBIENT" : raw.trim().toUpperCase(Locale.ROOT);
                if (!expected.equals(delivery.get(cardConstructor.newInstance(values))))
                    throw new AssertionError("Legacy/explicit delivery mishandled: " + raw);
            }
            blankRows++;
            cards.add(card);
        }
        Field loaded = PTSDCrisisIncidentManager.class.getDeclaredField("loaded"); loaded.setAccessible(true); loaded.set(null, true);
        for (String name : new String[]{"DARK", "PROBE", "NEWS"}) {
            Field pool = PTSDCrisisIncidentManager.class.getDeclaredField(name); pool.setAccessible(true);
            ((List)pool.get(null)).clear();
            if (name.equals("DARK")) ((List)pool.get(null)).addAll(cards);
        }
        Constructor<PTSDCrisisState> stateConstructor = PTSDCrisisState.class.getDeclaredConstructor(); stateConstructor.setAccessible(true);
        PTSDCrisisState state = stateConstructor.newInstance(); state.phase = PTSDCrisisState.Phase.DORMANT;
        Method select = PTSDCrisisIncidentManager.class.getDeclaredMethod("selectPool", PTSDCrisisState.class, boolean.class, Random.class);
        select.setAccessible(true);
        int selected = 0;
        for (final float roll : new float[]{0f, .99f}) {
            List<?> result = (List<?>) select.invoke(null, state, false, new Random() {
                @Override public float nextFloat() { return roll; }
            });
            if (result.isEmpty()) throw new AssertionError("First-stage news pool is empty");
            selected += result.size();
        }
        // Contact-only N-CUSTOM rows must stay outside random news, as before.
        if (blankRows != 39 || selected != 38) throw new AssertionError("Missing first-stage CSV rows: " + blankRows + "/" + selected);
        System.out.println("DormantNewsPoolTest: passed (37 legacy + 2 new rows; 38 ambient candidates; explicit delivery preserved)");
    }
}
