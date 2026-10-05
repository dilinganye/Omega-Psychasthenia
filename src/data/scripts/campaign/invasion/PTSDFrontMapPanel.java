package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
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
        boolean omega;
        String description;
    }

    private PositionAPI position;
    private LabelAPI hoverLabel;
    private LabelAPI selectionLabel;
    private CustomPanelAPI parent;
    private UIPanelAPI contextMenu;
    private final List<Edge> edges = new ArrayList<Edge>();
    private float minX;
    private float maxX;
    private float minY;
    private float maxY;
    private float zoom = 1f;
    private float centerX;
    private float centerY;
    private boolean dragging;

    public PTSDFrontMapPanel() {
        computeBounds();
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null) edges.addAll(getEdges(state));
        // Enemy truth and unreported NPC forces are deliberately excluded.
    }

    public void setParent(CustomPanelAPI panel, LabelAPI selection) { parent = panel; selectionLabel = selection; }

    public void setHoverLabel(LabelAPI hoverLabel) {
        this.hoverLabel = hoverLabel;
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (position == null) return;
        for (InputEventAPI event : events) {
            if (event.isConsumed()) continue;
            if (contextMenu != null && contextMenu.getPosition().containsEvent(event)) continue;
            boolean inside = position.containsEvent(event);
            if (event.isLMBUpEvent()) dragging = false;
            if (!inside) {
                if (event.isMouseMoveEvent()) setDefaultHover();
                continue;
            }
            if (event.isRMBDownEvent()) { openContext(event.getX(), event.getY()); event.consume(); continue; }
            if (event.isRMBEvent()) { event.consume(); continue; }
            if (event.isMouseScrollEvent()) {
                zoomAt(event.getX(), event.getY(), event.getEventValue() > 0 ? 1.22f : 1f / 1.22f);
                event.consume();
                continue;
            }
            if (event.isLMBDownEvent()) { closeContext(); selectAt(event.getX(), event.getY()); dragging = true; event.consume(); continue; }
            if (event.isMouseMoveEvent()) {
                int mouseX = event.getX();
                int mouseY = event.getY();
                if (dragging) {
                    int mouseDX = event.getDX();
                    int mouseDY = event.getDY();
                    centerX -= mouseDX * viewSpanX() / Math.max(1f, mapRight() - mapLeft());
                    centerY -= mouseDY * viewSpanY() / Math.max(1f, mapTop() - mapBottom());
                    clampCenter();
                }
                updateHover(mouseX, mouseY);
                if (dragging) event.consume();
            }
        }
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (position == null || Global.getSector() == null) return;
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LINE_BIT | GL11.GL_SCISSOR_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        drawBackground(alphaMult);
        drawGrid(alphaMult);

        float scale = Global.getSettings().getScreenScaleMult();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(Math.round(mapLeft() * scale), Math.round(mapBottom() * scale),
                Math.round((mapRight()-mapLeft()) * scale), Math.round((mapTop()-mapBottom()) * scale));
        for (Edge edge : edges) drawEdge(edge, alphaMult);
        drawSystemNodes(state, alphaMult);

        GL11.glLineWidth(1f);
        GL11.glPopAttrib();
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
        setColor(new Color(105, 125, 145, 165), alphaMult);
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
        GL11.glEnable(GL11.GL_LINE_STIPPLE);
        GL11.glLineStipple(2, (short) 0x0F0F);
        // Broad side affiliation underlay, finer individual-faction route on top.
        setColor(edge.omega ? new Color(165, 120, 149, 95) : new Color(113, 153, 170, 95), alphaMult);
        GL11.glLineWidth(edge.width + 3f);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(from.x, from.y); GL11.glVertex2f(to.x, to.y); GL11.glEnd();
        setColor(edge.color, alphaMult * .65f);
        GL11.glLineWidth(edge.width);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(from.x, from.y); GL11.glVertex2f(to.x, to.y); GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_STIPPLE);
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
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (!PTSDHumanWarReports.known(state, system)) continue;
            Vector2f point = getScreenPoint(system.getId());
            if (point == null) continue;
            PTSDCrisisState.HumanReport report = PTSDHumanWarReports.latest(state, system.getId());
            Color color = report == null ? new Color(132, 146, 162, 170) :
                    getFactionColor(report.factionId, report.omega ? new Color(165, 120, 149) : new Color(113, 153, 170));
            if (state.playerMarkers.containsKey(system.getId())) color = new Color(145, 195, 194);
            float age = report == null ? 0f : PTSDCrisisState.getDay() - report.day;
            if (report != null && report.battle) drawControlZone(point.x, point.y, 14f, color, alphaMult * (age > 10f ? .4f : .8f));
            drawNode(point.x, point.y, report == null ? 2.5f : 4f, color, alphaMult * (age > 10f ? .4f : 1f));
        }
    }

    private void drawControlZone(float x, float y, float radius, Color color, float alphaMult) {
        setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 38), alphaMult);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(x, y);
        for (int i = 0; i <= 24; i++) {
            double angle = Math.PI * 2d * i / 24d;
            GL11.glVertex2f(x + (float) Math.cos(angle) * radius, y + (float) Math.sin(angle) * radius);
        }
        GL11.glEnd();
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
        for (PTSDCrisisState.HumanReport report : state.humanWarReports.values()) {
            if (report == null || !report.movement || report.sourceSystemId == null ||
                    report.sourceSystemId.equals(report.systemId)) continue;
            Edge edge = new Edge(); edge.fromId = report.sourceSystemId; edge.toId = report.systemId;
            edge.arrow = true; edge.omega = report.omega; edge.width = 2.5f;
            Color factionColor = getFactionColor(report.factionId, new Color(140, 163, 175));
            float[] routeHSV = Color.RGBtoHSB(factionColor.getRed(), factionColor.getGreen(), factionColor.getBlue(), null);
            float variation = ((report.sourceSystemId + ":" + report.systemId).hashCode() & 255) / 255f * .08f - .04f;
            Color routeColor = Color.getHSBColor((routeHSV[0] + variation + 1f) % 1f, routeHSV[1], routeHSV[2]);
            edge.color = new Color(routeColor.getRed(), routeColor.getGreen(), routeColor.getBlue(), 180);
            edge.description = report.source + "｜记录日期 " + Math.round(report.day) + "｜" + report.text;
            result.add(edge);
        }
        return result;
    }

    private StarSystemAPI nearestSystem(float x, float y) {
        PTSDCrisisState state = PTSDCrisisState.get(); StarSystemAPI best = null; float distance = 12f;
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (!PTSDHumanWarReports.known(state, system)) continue;
            Vector2f point = getScreenPoint(system.getId()); if (point == null) continue;
            float next = Vector2f.sub(point, new Vector2f(x, y), new Vector2f()).length();
            if (next < distance) { distance = next; best = system; }
        }
        return best;
    }

    private String systemDescription(StarSystemAPI system) {
        PTSDCrisisState state = PTSDCrisisState.get();
        PTSDCrisisState.HumanReport report = PTSDHumanWarReports.latest(state, system.getId());
        boolean inhabited = false, memberRegion = false;
        for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system)) {
            if (market.isPlanetConditionMarketOnly()) continue;
            inhabited = true;
            if (market.isPlayerOwned() || PTSDHumanWarReports.member(state, market.getFactionId())) memberRegion = true;
        }
        if (inhabited && !memberRegion) return system.getName() + "｜非成员区域；无联合军报权限。";
        if (report == null) return system.getName() + "｜尚无成员军报；无人区域不显示兵力数值。";
        int age = Math.max(0, Math.round(PTSDCrisisState.getDay() - report.day));
        return system.getName() + "｜" + report.source + "｜" + age + "日前" + (age > 10 ? "（过期）" : "") +
                (!inhabited ? "" : report.strength < 0f ? "｜兵力未知" : "｜报告规模约 " + Math.round(report.strength)) + "｜" + report.text;
    }

    private void selectAt(float x, float y) {
        StarSystemAPI system = nearestSystem(x, y);
        if (system != null && selectionLabel != null) selectionLabel.setText(systemDescription(system));
    }

    private void closeContext() {
        if (parent != null && contextMenu != null) parent.removeComponent(contextMenu);
        contextMenu = null;
    }

    private void openContext(float x, float y) {
        closeContext();
        if (x < mapLeft() || x > mapRight() || y < mapBottom() || y > mapTop()) return;
        StarSystemAPI system = nearestSystem(x, y);
        if (parent == null || system == null) return;
        selectAt(x, y);
        TooltipMakerAPI menu = parent.createUIElement(225f, 160f, false);
        Color color = Misc.getBasePlayerColor(), dark = new Color(16, 23, 31);
        menu.addPara("%s", 3f, color, system.getName());
        menu.addButton("设置防御信标", "PTSD_MARK_DEFEND:" + system.getId(), color, dark, 225f, 23f, 5f);
        menu.addButton("撤销此处信标", "PTSD_CLEAR_MARKER:" + system.getId(), color, dark, 225f, 23f, 2f);
        menu.addButton("规划航线", "PTSD_MAP_COURSE:" + system.getId(), color, dark, 225f, 23f, 2f);
        PTSDCrisisState state = PTSDCrisisState.get();
        for (PTSDLateState.Front front : state.late.fronts.values()) {
            if (front != null && !front.resolved && system.getId().equals(front.systemId)) {
                menu.addButton("申请盟军增援", "PTSD_MAP_REINFORCE:" + front.id, color, dark, 225f, 23f, 2f); break;
            }
        }
        for (PTSDLateState.Reserve reserve : state.late.reserves) {
            if (reserve == null || reserve.destroyed || !reserve.authorized) continue;
            menu.addButton("调动授权后备队：" + reserve.id, "PTSD_MAP_RESERVE:" + reserve.id + ":" + system.getId(),
                    color, dark, 225f, 23f, 2f);
        }
        contextMenu = parent.wrapTooltipWithBox(menu, color);
        parent.addComponent(contextMenu).inTL(Math.max(4f, Math.min(position.getWidth()-245f, x-position.getX())),
                Math.max(82f, Math.min(position.getHeight()-210f, position.getY()+position.getHeight()-y)));
        parent.bringComponentToTop(contextMenu);
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
        StarSystemAPI nearest = nearestSystem(mouseX, mouseY);
        if (nearest != null && hoverLabel != null) {
            hoverLabel.setText(systemDescription(nearest)); hoverLabel.setColor(Misc.getTextColor()); return;
        }
        setDefaultHover();
    }

    private void setDefaultHover() {
        if (hoverLabel == null) return;
        hoverLabel.setText("悬停查看来源与日期；点击固定报告；右键星系下达请求。宽虚线区分交战双方，细虚线区分势力。");
        hoverLabel.setColor(new Color(180, 190, 205));
    }

    private Vector2f getScreenPoint(String systemId) {
        if (position == null || systemId == null) return null;
        StarSystemAPI system = Global.getSector().getStarSystem(systemId);
        if (system == null) return null;
        Vector2f location = system.getLocation();
        float x = mapLeft() + (location.x - (centerX - viewSpanX() * 0.5f)) / viewSpanX() * (mapRight() - mapLeft());
        float y = mapBottom() + (location.y - (centerY - viewSpanY() * 0.5f)) / viewSpanY() * (mapTop() - mapBottom());
        return new Vector2f(x, y);
    }

    private void computeBounds() {
        minX = minY = Float.MAX_VALUE;
        maxX = maxY = -Float.MAX_VALUE;
        if (Global.getSector() == null) { minX = minY = -10000f; maxX = maxY = 10000f; resetView(); return; }
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (!PTSDHumanWarReports.known(PTSDCrisisState.get(), system)) continue;
            Vector2f p = system.getLocation();
            minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
        }
        if (minX == Float.MAX_VALUE) { minX = minY = -10000f; maxX = maxY = 10000f; }
        float padX = Math.max(2500f, (maxX - minX) * 0.05f);
        float padY = Math.max(2500f, (maxY - minY) * 0.05f);
        minX -= padX; maxX += padX; minY -= padY; maxY += padY;
        resetView();
    }

    private void resetView() { zoom = 1f; centerX = (minX + maxX) * 0.5f; centerY = (minY + maxY) * 0.5f; }

    private void zoomAt(float mouseX, float mouseY, float factor) {
        float oldSpanX = viewSpanX(), oldSpanY = viewSpanY();
        float fx = (mouseX - mapLeft()) / Math.max(1f, mapRight() - mapLeft()) - 0.5f;
        float fy = (mouseY - mapBottom()) / Math.max(1f, mapTop() - mapBottom()) - 0.5f;
        float worldX = centerX + fx * oldSpanX, worldY = centerY + fy * oldSpanY;
        zoom = Math.max(1f, Math.min(8f, zoom * factor));
        centerX = worldX - fx * viewSpanX(); centerY = worldY - fy * viewSpanY();
        clampCenter();
    }

    private void clampCenter() {
        float hx = viewSpanX() * 0.5f, hy = viewSpanY() * 0.5f;
        centerX = Math.max(minX + hx, Math.min(maxX - hx, centerX));
        centerY = Math.max(minY + hy, Math.min(maxY - hy, centerY));
    }

    private float viewSpanX() { return Math.max(1f, maxX - minX) / zoom; }
    private float viewSpanY() { return Math.max(1f, maxY - minY) / zoom; }
    private float mapLeft() { return position.getX() + 12f; }
    private float mapRight() { return position.getX() + position.getWidth() - 12f; }
    private float mapBottom() { return position.getY() + 58f; }
    private float mapTop() { return position.getY() + position.getHeight() - 82f; }
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
        Color base = faction == null ? fallback : faction.getBaseUIColor();
        float[] hsv = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        Color muted = Color.getHSBColor(hsv[0], Math.min(.35f, hsv[1]), Math.max(.55f, hsv[2]));
        return new Color(muted.getRed(), muted.getGreen(), muted.getBlue(), 180);
    }

    private static void setColor(Color color, float alphaMult) {
        GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(),
                (byte) Math.max(0, Math.min(255, Math.round(color.getAlpha() * alphaMult))));
    }

    private static float roundOne(float value) {
        return Math.round(value * 10f) / 10f;
    }
}