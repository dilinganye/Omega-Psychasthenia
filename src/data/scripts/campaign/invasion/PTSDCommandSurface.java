package data.scripts.campaign.invasion;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import org.lwjgl.opengl.GL11;
import java.awt.Color;
import java.util.List;

/** Independently drawn command-board geometry; no internal vanilla UI classes or textures. */
public final class PTSDCommandSurface extends BaseCustomUIPanelPlugin {
    public static final Color INK = new Color(158,195,190), AMBER = new Color(199,169,112);
    private final Color accent;
    private final Runnable action;
    private final boolean selected;
    private PositionAPI position;
    private boolean hover, pending, scanlines;
    private float screenTime;
    public PTSDCommandSurface(Color accent) { this(accent, false, null); }
    private PTSDCommandSurface(Color accent, boolean selected, Runnable action) {
        this.accent=accent; this.selected=selected; this.action=action;
    }
    @Override public void positionChanged(PositionAPI value) { position=value; }
    @Override public void processInput(List<InputEventAPI> events) {
        if(action==null || position==null)return;
        for(InputEventAPI event:events) {
            if(event.isConsumed())continue;
            if(event.isMouseMoveEvent())hover=position.containsEvent(event);
            if(event.isLMBDownEvent() && position.containsEvent(event)){pending=true;event.consume();}
        }
    }
    @Override public void advance(float amount) { screenTime+=Math.max(0,amount); if(pending){pending=false;action.run();} }
    @Override public void renderBelow(float alpha) {
        if(position==null)return;
        float x=position.getX(),y=position.getY(),w=position.getWidth(),h=position.getHeight();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT|GL11.GL_COLOR_BUFFER_BIT|GL11.GL_CURRENT_BIT|GL11.GL_LINE_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
        color(new Color(9,18,23),alpha*.97f);rect(x,y,w,h);
        if(hover||selected){color(accent,alpha*.16f);rect(x+2,y+2,w-4,h-4);}
        color(accent,alpha*.65f);rect(x,y,3,h);rect(x,y+h-2,w,2);
        GL11.glLineWidth(1f);color(accent,alpha*.12f);
        GL11.glBegin(GL11.GL_LINES);
        if(action==null)for(float row=y+24;row<y+h;row+=24){GL11.glVertex2f(x+6,row);GL11.glVertex2f(x+w-6,row);}
        GL11.glEnd();
        color(accent,alpha*.55f);rect(x+w-22,y+7,15,2);rect(x+w-9,y+7,2,12);
        if(scanlines) {
            float sweep=(screenTime%7f)/7f;
            float line=y+h*(1f-sweep);
            color(accent,alpha*.08f);rect(x+4,line,w-8,2);
            color(accent,alpha*.035f);rect(x+4,Math.max(y,line-10),w-8,Math.min(10,line-y));
        }
        GL11.glPopAttrib();
    }
    private static void color(Color c,float a){GL11.glColor4f(c.getRed()/255f,c.getGreen()/255f,c.getBlue()/255f,a);}
    private static void rect(float x,float y,float w,float h){GL11.glBegin(GL11.GL_QUADS);GL11.glVertex2f(x,y);GL11.glVertex2f(x+w,y);GL11.glVertex2f(x+w,y+h);GL11.glVertex2f(x,y+h);GL11.glEnd();}
    public static CustomPanelAPI tab(CustomPanelAPI parent,float width,String label,boolean active,Runnable action){
        CustomPanelAPI cell=parent.createCustomPanel(width,34f,new PTSDCommandSurface(INK,active,action));
        TooltipMakerAPI text=cell.createUIElement(width-18f,26f,false);
        text.addPara("%s",0f,active?Color.WHITE:INK,label);cell.addUIElement(text).inTL(10f,8f);return cell;
    }
    public interface Content { void draw(TooltipMakerAPI info,float width,float height); }
    public static void document(CustomPanelAPI parent,float width,float height,String title,String channel,Color accent,Content content){
        document(parent,width,height,title,channel,accent,content,false);
    }
    public static void screenDocument(CustomPanelAPI parent,float width,float height,String title,String channel,Color accent,Content content){
        document(parent,width,height,title,channel,accent,content,true);
    }
    private static void document(CustomPanelAPI parent,float width,float height,String title,String channel,Color accent,Content content,boolean scanlines){
        PTSDCommandSurface surface=new PTSDCommandSurface(accent);surface.scanlines=scanlines;
        CustomPanelAPI page=parent.createCustomPanel(width,height,surface);
        TooltipMakerAPI mast=page.createUIElement(width-40f,65f,false);
        mast.setParaFont("graphics/fonts/insignia21LTaa.fnt");mast.addPara("%s",0f,accent,title);mast.setParaFontDefault();
        mast.addPara("%s  /  接收档案",8f,new Color(139,155,166),channel);page.addUIElement(mast).inTL(20f,14f);
        float reading=Math.min(840f,width-50f);
        TooltipMakerAPI body=page.createUIElement(reading,height-100f,true);
        content.draw(body,reading-16f,height-100f);page.addUIElement(body).inTL(24f,88f);
        parent.addComponent(page).inTL(0f,0f);
    }
}
