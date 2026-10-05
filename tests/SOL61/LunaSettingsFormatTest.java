import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Checks authored settings strings against the Formatter used by vanilla addPara. */
public final class LunaSettingsFormatTest {
    public static void main(String[] args) throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/data/scripts/PTSDLunaConfigVisibility.java")), StandardCharsets.UTF_8);
        Matcher literals = Pattern.compile("\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"").matcher(source);
        int checked = 0;
        while (literals.find()) {
            String text = literals.group(1);
            if (!String.format(text).equals(text))
                throw new AssertionError("Settings text changed during formatting: " + text);
            checked++;
        }
        String csv = new String(Files.readAllBytes(Paths.get("data/config/LunaSettings.csv")),
                StandardCharsets.UTF_8);
        for (String row : csv.split("\\r?\\n")) String.format(row);
        if (!source.contains("附报概率％") || checked < 100)
            throw new AssertionError("Settings input was not fully checked");
        System.out.println("LunaSettingsFormatTest: passed (" + checked + " source strings and public CSV)");
    }
}
