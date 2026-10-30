package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.ui.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** Public report locations only: never resolves the incident's hidden physical target. */
public final class PTSDNewsLocationPanel extends com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin {
    private final List<Vector2f> stars = new ArrayList<Vector2f>();
    private final Vector2f target;
    private final Vector2f player;
    private final boolean je;
    private PositionAPI position;
    private float minX, minY, spanX, spanY;
    private PTSDNewsLocationPanel(StarSystemAPI system, boolean je) {
        this.je = je;
        target = system == null ? null : new Vector2f(system.getLocation());
        player = Global.getSector().getPlayerFleet() == null ? null :
                new Vector2f(Global.getSector().getPlayerFleet().getLocationInHyperspace());
        for (StarSystemAPI known : Global.getSector().getStarSystems()) {
            if (known == system || known.isEnteredByPlayer() || populated(known)) stars.add(new Vector2f(known.getLocation()));
        }
        minX = Float.MAX_VALUE; minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        List<Vector2f> points = new ArrayList<Vector2f>(stars);
        if (player != null) points.add(player);
        if (target != null) points.add(target);
        for (Vector2f p : points) { minX=Math.min(minX,p.x);minY=Math.min(minY,p.y);maxX=Math.max(maxX,p.x);maxY=Math.max(maxY,p.y); }
        if (points.isEmpty()) { minX=minY=-1;maxX=maxY=1; }
        spanX=Math.max(2000f,maxX-minX);spanY=Math.max(2000f,maxY-minY);
    }
    private static boolean populated(StarSystemAPI system) {
        for (com.fs.starfarer.api.campaign.econ.MarketAPI market : Global.getSector().getEconomy().getMarkets(system))
            if (!market.isPlanetConditionMarketOnly() && !IIRT_Omega_Invasion.WATCHER_FACTION.equals(market.getFactionId()) &&
                    !IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(market.getFactionId())) return true;
        return false;
    }
    @Override public void positionChanged(PositionAPI p) { position=p; }
    private float x(Vector2f p) { return position.getX()+24+(p.x-minX)/spanX*(position.getWidth()-48); }
    private float y(Vector2f p) { return position.getY()+24+(p.y-minY)/spanY*(position.getHeight()-48); }
    private static void line(float x,float y,float xx,float yy) { GL11.glVertex2f(x,y);GL11.glVertex2f(xx,yy); }
    @Override public void renderBelow(float alpha) {
        if (position == null) return;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT|GL11.GL_COLOR_BUFFER_BIT|GL11.GL_CURRENT_BIT|GL11.GL_LINE_BIT|GL11.GL_POINT_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(.035f,.065f,.09f,.95f*alpha);GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(position.getX(),position.getY());GL11.glVertex2f(position.getX()+position.getWidth(),position.getY());
        GL11.glVertex2f(position.getX()+position.getWidth(),position.getY()+position.getHeight());GL11.glVertex2f(position.getX(),position.getY()+position.getHeight());GL11.glEnd();
        GL11.glColor4f(.48f,.61f,.65f,.65f*alpha);GL11.glPointSize(3f);GL11.glBegin(GL11.GL_POINTS);
        for(Vector2f p:stars)GL11.glVertex2f(x(p),y(p));GL11.glEnd();
        if(target!=null) {
            GL11.glColor4f(.77f,.67f,.47f,.8f*alpha);GL11.glLineWidth(1.5f);
            if(player!=null){GL11.glEnable(GL11.GL_LINE_STIPPLE);GL11.glLineStipple(2,(short)0x00ff);GL11.glBegin(GL11.GL_LINES);line(x(player),y(player),x(target),y(target));GL11.glEnd();GL11.glDisable(GL11.GL_LINE_STIPPLE);}
            GL11.glBegin(GL11.GL_LINE_LOOP);for(int n=0;n<32;n++){double a=n*Math.PI/16;GL11.glVertex2f(x(target)+(float)Math.cos(a)*10,y(target)+(float)Math.sin(a)*10);}GL11.glEnd();
            if(je){GL11.glLineWidth(3f);GL11.glBegin(GL11.GL_LINES);line(x(target),y(target)+6,x(target),y(target)-1);line(x(target),y(target)-5,x(target),y(target)-7);GL11.glEnd();}
        }
        if(player!=null){GL11.glColor4f(.5f,.9f,.8f,alpha);GL11.glPointSize(6);GL11.glBegin(GL11.GL_POINTS);GL11.glVertex2f(x(player),y(player));GL11.glEnd();}
        GL11.glPopAttrib();
    }
    public static void document(CustomPanelAPI parent,float width,float height,String title,String channel,
            Color accent,PTSDCommandSurface.Content content,PTSDCrisisState.CrisisIncident incident,boolean je) {
        PTSDCrisisState state=PTSDCrisisState.get();
        StarSystemAPI system=state==null||!PTSDNewsPresentation.hasPublicLocation(incident)?null:state.resolveSystem(incident.targetSystemId);
        if(system==null){
            PTSDCommandSurface.document(parent,width,height,title,channel,accent,content);
            return;
        }
        float mapHeight=Math.min(260f,Math.max(130f,height*.34f));
        float cardHeight=Math.max(140f,height-mapHeight-44f);
        CustomPanelAPI card=parent.createCustomPanel(width,cardHeight,null);
        PTSDCommandSurface.document(card,width,cardHeight,title,channel,accent,content);
        parent.addComponent(card).inTL(0,0);
        TooltipMakerAPI caption=parent.createUIElement(width-24,28,false);
        caption.addPara("%s",0f,accent,system==null?"现场位置尚未提供":(je?"! Je任务 / ":"区域测算...位置定位... / ")+system.getName()+"，现场需自行核验");
        parent.addUIElement(caption).inTL(12,cardHeight+9);
        CustomPanelAPI map=parent.createCustomPanel(width,mapHeight,new PTSDNewsLocationPanel(system,je));
        parent.addComponent(map).inTL(0,cardHeight+38);
    }
}
