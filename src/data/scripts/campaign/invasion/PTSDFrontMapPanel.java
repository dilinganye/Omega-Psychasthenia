package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Fixed whole-sector overview used for per-arrow and per-front hover details. */
public final class PTSDFrontMapPanel extends BaseCustomUIPanelPlugin {
    private static final class Edge {
        String fromId;
        String toId;
        Color color;
        float width;
        boolean arrow;
        String description;
    }

    private PositionAPI position;
    private LabelAPI hoverLabel;
    private final List<Edge> edges = new ArrayList<Edge>();
    private final List<PTSDCrisisAPI.ForceContribution> contributions =
            new ArrayList<PTSDCrisisAPI.ForceContribution>();
    private float minX;
    private float maxX;
    private float minY;
    private float maxY;

    public PTSDFrontMapPanel() {
        computeBounds();
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null) edges.addAll(getEdges(state));
        contributions.addAll(PTSDCrisisAPI.getForceContributions());
    }

    public void setHoverLabel(LabelAPI hoverLabel) {
        this.hoverLabel = hoverLabel;
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (position == null || hoverLabel == null) return;
        for (InputEventAPI event : events) {
            if (event.isConsumed() || !event.isMouseMoveEvent()) continue;
            if (!position.containsEvent(event)) {
                setDefaultHover();
                continue;
            }
            updateHover(event.getX(), event.getY());
        }
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (position == null || Global.getSector() == null) return;
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        drawBackground(alphaMult);
        drawGrid(alphaMult);

        for (Edge edge : edges) drawEdge(edge, alphaMult);
        drawSystemNodes(state, alphaMult);

        GL11.glLineWidth(1f);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private void drawBackground(float alphaMult) {
        float x = position.getX();
        float y = position.getY();
        float w = position.getWidth();
        float h = position.getHeight();
        setColor(new Color(4, 8, 18, 235), alphaMult);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + w, y);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x, y + h);
        GL11.glEnd();
        setColor(new Color(105, 45, 140, 180), alphaMult);
        GL11.glLineWidth(1.5f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x + 1f, y + 1f);
        GL11.glVertex2f(x + w - 1f, y + 1f);
        GL11.glVertex2f(x + w - 1f, y + h - 1f);
        GL11.glVertex2f(x + 1f, y + h - 1f);
        GL11.glEnd();
    }

    private void drawGrid(float alphaMult) {
        float left = mapLeft();
        float right = mapRight();
        float bottom = mapBottom();
        float top = mapTop();
        setColor(new Color(90, 105, 130, 45), alphaMult);
        GL11.glLineWidth(1f);
        GL11.glBegin(GL11.GL_LINES);
        for (int i = 0; i <= 8; i++) {
            float x = left + (right - left) * i / 8f;
            GL11.glVertex2f(x, bottom);
            GL11.glVertex2f(x, top);
        }
        for (int i = 0; i <= 5; i++) {
            float y = bottom + (top - bottom) * i / 5f;
            GL11.glVertex2f(left, y);
            GL11.glVertex2f(right, y);
        }
        GL11.glEnd();
    }

    private void drawEdge(Edge edge, float alphaMult) {
        Vector2f from = getScreenPoint(edge.fromId);
        Vector2f to = getScreenPoint(edge.toId);
        if (from == null || to == null) return;
        setColor(edge.color, alphaMult * (edge.arrow ? 0.82f : 0.34f));
        GL11.glLineWidth(edge.width);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(from.x, from.y);
        GL11.glVertex2f(to.x, to.y);
        GL11.glEnd();
        if (!edge.arrow) return;

        Vector2f direction = Vector2f.sub(to, from, new Vector2f());
        if (direction.lengthSquared() < 1f) return;
        direction.normalise();
        Vector2f normal = new Vector2f(-direction.y, direction.x);
        float size = 7f + edge.width * 0.45f;
        Vector2f tip = new Vector2f(to.x - direction.x * 5f, to.y - direction.y * 5f);
        Vector2f base = new Vector2f(tip.x - direction.x * size, tip.y - direction.y * size);
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex2f(tip.x, tip.y);
        GL11.glVertex2f(base.x + normal.x * size * 0.55f, base.y + normal.y * size * 0.55f);
        GL11.glVertex2f(base.x - normal.x * size * 0.55f, base.y - normal.y * size * 0.55f);
        GL11.glEnd();
    }

    private void drawSystemNodes(PTSDCrisisState state, float alphaMult) {
        Set<String> active = new HashSet<String>();
        for (PTSDCrisisState.StrategicEvent event : state.getActiveEvents()) {
            if (event.sourceSystemId != null) active.add(event.sourceSystemId);
            if (event.targetSystemId != null) active.add(event.targetSystemId);
        }
        for (PTSDCrisisState.SystemData data : state.systems.values()) {
            if (data.omegaControl <= 0f && !data.knownToPlayer && !active.contains(data.systemId) &&
                    !state.playerMarkers.containsKey(data.systemId)) continue;
            Vector2f point = getScreenPoint(data.systemId);
            if (point == null) continue;
            Color color = data.omegaControl >= 0.5f
                    ? getFactionColor(IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION, new Color(210, 45, 230))
                    : Global.getSector().getPlayerFaction().getBaseUIColor();
            if (state.playerMarkers.containsKey(data.systemId)) color = Color.CYAN;
            float radius = data.blackHoleFortress ? 6f : 3.5f + Math.min(2.5f, data.strategicValue / 180f);
            drawNode(point.x, point.y, radius, color, alphaMult);
        }
        for (PTSDCrisisAPI.ForceContribution force : contributions) {
            Vector2f point = getScreenPoint(force.systemId);
            if (point == null) continue;
            drawNode(point.x, point.y, 3f + Math.min(4f, force.strength / 80f),
                    getFactionColor(force.factionId, Color.LIGHT_GRAY), alphaMult);
        }
    }

    private void drawNode(float x, float y, float radius, Color color, float alphaMult) {
        setColor(color, alphaMult * 0.9f);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(x, y);
        for (int i = 0; i <= 16; i++) {
            double angle = Math.PI * 2d * i / 16d;
            GL11.glVertex2f(x + (float) Math.cos(angle) * radius, y + (float) Math.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    private List<Edge> getEdges(PTSDCrisisState state) {
        List<Edge> result = new ArrayList<Edge>();
        for (PTSDCrisisState.StrategicEvent event : state.getActiveEvents()) {
            if (event.targetSystemId == null) continue;
            String from = event.sourceSystemId == null ? state.baseSystemId : event.sourceSystemId;
            if (from == null || from.equals(event.targetSystemId)) continue;
            Edge edge = new Edge();
            edge.fromId = from;
            edge.toId = event.targetSystemId;
            edge.arrow = true;
            edge.width = Math.max(2f, Math.min(8f, 1.5f + event.strength / 45f));
            edge.color = getFactionColor(event.factionId,
                    PTSDCrisisAPI.SIDE_OMEGA.equals(event.side) ? new Color(210, 45, 230) : Color.CYAN);
            StarSystemAPI target = state.resolveSystem(event.targetSystemId);
            String targetName = target == null ? event.targetSystemId : target.getName();
            edge.description = (PTSDCrisisAPI.SIDE_OMEGA.equals(event.side) ? "精神创伤推进" : "人类侧阻滞") +
                    " → " + targetName + "；强度 " + Math.round(event.strength) + "；" +
                    (event.status == PTSDCrisisState.EventStatus.MATERIALIZED ? "已实体化" : "远程推演") +
                    (event.description == null ? "" : "；" + event.description);
            result.add(edge);
        }

        Set<String> pairs = new HashSet<String>();
        for (PTSDCrisisState.SystemData omega : state.systems.values()) {
            if (omega.omegaControl < 0.5f) continue;
            StarSystemAPI omegaSystem = state.resolveSystem(omega.systemId);
            if (omegaSystem == null) continue;
            PTSDCrisisState.SystemData nearest = null;
            float nearestDistance = Float.MAX_VALUE;
            for (PTSDCrisisState.SystemData human : state.systems.values()) {
                if (human.omegaControl >= 0.5f || !hasHumanMarket(human.systemId)) continue;
                StarSystemAPI humanSystem = state.resolveSystem(human.systemId);
                if (humanSystem == null) continue;
                float distance = Vector2f.sub(omegaSystem.getLocation(), humanSystem.getLocation(), new Vector2f()).lengthSquared();
                if (distance < nearestDistance) { nearestDistance = distance; nearest = human; }
            }
            if (nearest == null) continue;
            String key = omega.systemId + ":" + nearest.systemId;
            if (!pairs.add(key)) continue;
            Edge edge = new Edge();
            edge.fromId = omega.systemId;
            edge.toId = nearest.systemId;
            edge.arrow = false;
            edge.width = 7f;
            edge.color = new Color(185, 55, 205);
            StarSystemAPI humanSystem = state.resolveSystem(nearest.systemId);
            edge.description = "战线边缘：" + omegaSystem.getName() + " 实控区 / " +
                    (humanSystem == null ? nearest.systemId : humanSystem.getName()) + " 防区；精神创伤控制 " +
                    Math.round(omega.omegaControl * 100f) + "%";
            result.add(edge);
        }
        return result;
    }

    private boolean hasHumanMarket(String systemId) {
        StarSystemAPI system = Global.getSector().getStarSystem(systemId);
        if (system == null) return false;
        for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system)) {
            if (!market.isPlanetConditionMarketOnly() && !IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(market.getFactionId())) return true;
        }
        return false;
    }

    private void updateHover(float mouseX, float mouseY) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return;
        Edge best = null;
        float bestDistance = 12f;
        for (Edge edge : edges) {
            Vector2f from = getScreenPoint(edge.fromId);
            Vector2f to = getScreenPoint(edge.toId);
            if (from == null || to == null) continue;
            float distance = distanceToSegment(mouseX, mouseY, from, to);
            if (distance < bestDistance) { bestDistance = distance; best = edge; }
        }
        if (best != null) {
            hoverLabel.setText(best.description);
            hoverLabel.setColor(best.color.brighter());
            return;
        }
        PTSDCrisisState.SystemData nearest = null;
        float nearestDistance = 10f;
        for (PTSDCrisisState.SystemData data : state.systems.values()) {
            Vector2f point = getScreenPoint(data.systemId);
            if (point == null) continue;
            float dx = mouseX - point.x;
            float dy = mouseY - point.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance < nearestDistance) { nearestDistance = distance; nearest = data; }
        }
        if (nearest != null) {
            StarSystemAPI system = state.resolveSystem(nearest.systemId);
            hoverLabel.setText((system == null ? nearest.systemId : system.getName()) + "：攻击权重 " +
                    roundOne(nearest.attackWeight) + "；人类防卫权重 " + roundOne(nearest.humanDefenseWeight) +
                    "；精神创伤控制 " + Math.round(nearest.omegaControl * 100f) + "%" +
                    (nearest.blackHoleFortress ? "；黑洞要塞活动" : ""));
            hoverLabel.setColor(nearest.omegaControl >= 0.5f ? new Color(235, 105, 255) : Color.CYAN);
            return;
        }
        setDefaultHover();
    }

    private void setDefaultHover() {
        if (hoverLabel == null) return;
        hoverLabel.setText("将鼠标移到推进箭头、阻滞箭头、战线边缘或星系节点上查看局部态势。粗线表示更强的部署。 ");
        hoverLabel.setColor(new Color(180, 190, 205));
    }

    private Vector2f getScreenPoint(String systemId) {
        if (position == null || systemId == null) return null;
        StarSystemAPI system = Global.getSector().getStarSystem(systemId);
        if (system == null) return null;
        Vector2f location = system.getLocation();
        float x = mapLeft() + (location.x - minX) / Math.max(1f, maxX - minX) * (mapRight() - mapLeft());
        float y = mapBottom() + (location.y - minY) / Math.max(1f, maxY - minY) * (mapTop() - mapBottom());
        return new Vector2f(x, y);
    }

    private void computeBounds() {
        minX = minY = Float.MAX_VALUE;
        maxX = maxY = -Float.MAX_VALUE;
        if (Global.getSector() == null) { minX = minY = -10000f; maxX = maxY = 10000f; return; }
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            Vector2f p = system.getLocation();
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }
        float padX = Math.max(2500f, (maxX - minX) * 0.05f);
        float padY = Math.max(2500f, (maxY - minY) * 0.05f);
        minX -= padX; maxX += padX; minY -= padY; maxY += padY;
    }

    private float mapLeft() { return position.getX() + 12f; }
    private float mapRight() { return position.getX() + position.getWidth() - 12f; }
    private float mapBottom() { return position.getY() + 48f; }
    private float mapTop() { return position.getY() + position.getHeight() - 12f; }

    private static float distanceToSegment(float x, float y, Vector2f a, Vector2f b) {
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        float lengthSq = dx * dx + dy * dy;
        if (lengthSq <= 0.001f) return (float) Math.sqrt((x - a.x) * (x - a.x) + (y - a.y) * (y - a.y));
        float t = ((x - a.x) * dx + (y - a.y) * dy) / lengthSq;
        t = Math.max(0f, Math.min(1f, t));
        float px = a.x + t * dx;
        float py = a.y + t * dy;
        return (float) Math.sqrt((x - px) * (x - px) + (y - py) * (y - py));
    }

    private static Color getFactionColor(String factionId, Color fallback) {
        FactionAPI faction = factionId == null || Global.getSector() == null ? null : Global.getSector().getFaction(factionId);
        return faction == null ? fallback : faction.getBaseUIColor();
    }

    private static void setColor(Color color, float alphaMult) {
        GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(),
                (byte) Math.max(0, Math.min(255, Math.round(color.getAlpha() * alphaMult))));
    }

    private static float roundOne(float value) {
        return Math.round(value * 10f) / 10f;
    }
}