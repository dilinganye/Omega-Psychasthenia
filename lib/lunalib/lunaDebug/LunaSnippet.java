package lunalib.lunaDebug;

import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.util.List;
import java.util.Map;

public abstract class LunaSnippet {
    public enum SnippetTags {
        Cheat{
            @Override
            public String toString() {
                return "作弊";
            }
        }, Debug{
            @Override
            public String toString() {
                return "调试";
            }
        }, Cargo{
            @Override
            public String toString() {
                return "货物";
            }
        }, Entity{
            @Override
            public String toString() {
                return "天体";
            }
        }, Player{
            @Override
            public String toString() {
                return "玩家";
            }
        }, Faction{
            @Override
            public String toString() {
                return "势力";
            }
        }
    }

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getModId();

    public abstract List<String> getTags();


    public void addParameters(SnippetBuilder builder) {

    }

    public void execute(Map<String, Object> parameters, TooltipMakerAPI output) {

    }
}
