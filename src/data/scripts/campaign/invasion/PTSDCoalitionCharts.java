package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;
import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

/** Member-only, separately attributed coalition statistics with native text and faction colors. */
public final class PTSDCoalitionCharts {
    private PTSDCoalitionCharts() { }

    public static Map<String, Float> values(PTSDCrisisState state, int metric) {
        Map<String, Float> result = new LinkedHashMap<String, Float>();
        if (state == null || state.late == null) return result;
        for (String faction : state.late.coalitionMembers) result.put(faction, 0f);
        if (metric == 0) {
            // Fleet projections are not counted again: these are the same committed strategic units.
            for (PTSDLateState.Reserve reserve : state.late.reserves)
                if (reserve != null && !reserve.destroyed) add(result, reserve.factionId, reserve.strength);
            for (PTSDCrisisState.StrategicEvent event : state.getActiveEvents())
                if (PTSDCrisisAPI.SIDE_HUMAN.equals(event.side)) add(result, event.factionId, event.strength);
        } else if (metric == 1) {
            for (MarketAPI market : PTSDOccupationManager.getAllMarkets())
                if (market != null && !market.isPlanetConditionMarketOnly()) add(result, market.getFactionId(), 1f);
        } else {
            for (PTSDLateState.Contribution item : state.late.contributions)
                if (item != null && (metric == 2 || PTSDCrisisState.getDay() - item.day <= 90f))
                    add(result, item.factionAtTime, item.amount);
        }
        return result;
    }

    private static void add(Map<String, Float> values, String faction, float amount) {
        if (values.containsKey(faction)) values.put(faction, values.get(faction) + Math.max(0f, amount));
    }

    public static void populate(CustomPanelAPI parent, float width, float height, PTSDCrisisState state) {
        float gap = 12f, cellWidth = (width - gap) / 2f, cellHeight = Math.max(230f, (height-gap)/2f);
        String[] titles = {"已承诺兵力", "有效殖民地", "累计贡献", "近90日贡献"};
        for (int metric = 0; metric < 4; metric++) {
            Map<String, Float> values = values(state, metric);
            CustomPanelAPI cell = parent.createCustomPanel(cellWidth, cellHeight, null);
            TooltipMakerAPI title = cell.createUIElement(cellWidth, 46f, false);
            title.addSectionHeading(titles[metric], Misc.getBasePlayerColor(), new Color(16,23,31),
                    com.fs.starfarer.api.ui.Alignment.MID, 0f);
            float sum = 0f; for (float value : values.values()) sum += value;
            title.addPara("登记日期 %s｜总量 %s", 4f, Misc.getHighlightColor(),
                    String.valueOf(Math.round(PTSDCrisisState.getDay())), String.valueOf(Math.round(sum)));
            cell.addUIElement(title).inTL(0f, 0f);
            float diameter = Math.min(150f, cellWidth * .43f);
            cell.addComponent(cell.createCustomPanel(diameter, diameter, new Pie(values))).inTL(8f, 56f);
            TooltipMakerAPI legend = cell.createUIElement(Math.max(70f,cellWidth-diameter-24f), cellHeight-58f, true);
            if (sum <= 0f) legend.addPara("暂无已登记投入。", 0f, Misc.getGrayColor());
            for (Map.Entry<String, Float> entry : values.entrySet()) {
                FactionAPI faction = Global.getSector().getFaction(entry.getKey());
                String name = faction == null ? entry.getKey() : faction.getDisplayName();
                legend.addPara("%s：%s（%s%%）", 5f, color(entry.getKey()), name,
                        String.valueOf(Math.round(entry.getValue())), sum <= 0f ? "—" :
                                String.valueOf(Math.round(entry.getValue()/sum*100f)));
            }
            cell.addUIElement(legend).inTR(0f, 55f);
            parent.addComponent(cell).inTL((metric%2)*(cellWidth+gap), (metric/2)*(cellHeight+gap));
        }
    }

    private static Color color(String factionId) {
        FactionAPI faction = Global.getSector().getFaction(factionId);
        Color base = faction == null ? new Color(155,175,185) : faction.getBaseUIColor();
        float[] hsv = Color.RGBtoHSB(base.getRed(),base.getGreen(),base.getBlue(),null);
        return Color.getHSBColor(hsv[0],Math.min(.45f,hsv[1]),Math.max(.6f,hsv[2]));
    }

    private static final class Pie extends BaseCustomUIPanelPlugin {
        private PositionAPI position;
        private final Map<String, Float> values;
        Pie(Map<String, Float> values) { this.values = values; }
        @Override public void positionChanged(PositionAPI position) { this.position = position; }
        @Override public void renderBelow(float alphaMult) {
            if (position == null) return;
            float total=0f; for(float value:values.values()) total+=value;
            float x=position.getCenterX(), y=position.getCenterY(), radius=Math.min(position.getWidth(),position.getHeight())*.46f;
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT);
            GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            double start=Math.PI*.5d;
            if(total>0f) for(Map.Entry<String,Float> entry:values.entrySet()) {
                if(entry.getValue()<=0f)continue;
                double end=start+Math.PI*2d*entry.getValue()/total;
                Color color=color(entry.getKey());
                GL11.glColor4f(color.getRed()/255f,color.getGreen()/255f,color.getBlue()/255f,.75f*alphaMult);
                GL11.glBegin(GL11.GL_TRIANGLE_FAN); GL11.glVertex2f(x,y);
                int steps=Math.max(2,(int)Math.ceil((end-start)*20d));
                for(int i=0;i<=steps;i++) {
                    double angle=start+(end-start)*i/steps;
                    GL11.glVertex2f(x+(float)Math.cos(angle)*radius,y+(float)Math.sin(angle)*radius);
                }
                GL11.glEnd(); start=end;
            }
            GL11.glColor4f(.55f,.65f,.7f,.65f*alphaMult); GL11.glLineWidth(1f);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            for(int i=0;i<96;i++) {
                double angle=Math.PI*2d*i/96d;
                GL11.glVertex2f(x+(float)Math.cos(angle)*radius,y+(float)Math.sin(angle)*radius);
            }
            GL11.glEnd(); GL11.glPopAttrib();
        }
    }
}
