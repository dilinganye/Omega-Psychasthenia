package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.combat.ViewportAPI;
import lunalib.lunaUtil.campaign.LunaCampaignRenderer;
import lunalib.lunaUtil.campaign.LunaCampaignRenderingPlugin;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.EnumSet;

/** Screen-space campaign news ribbon. Polls mouse state and never consumes InputEventAPI objects. */
public final class PTSDNewsTicker implements LunaCampaignRenderingPlugin {
    private static final float WIDTH=520f, HEIGHT=34f, BOTTOM=282f;
    private static final long DISPLAY_MS=13000L;
    private static LazyFont.DrawableString text;
    private static String laidOutText;
    private String headline="";
    private String incidentId;
    private long shownAt;
    private boolean rightWasDown;

    public static void install() {
        if (Global.getSector()==null || LunaCampaignRenderer.hasRendererOfClass(PTSDNewsTicker.class)) return;
        LunaCampaignRenderer.addTransientRenderer(new PTSDNewsTicker());
    }
    public static void report(PTSDCrisisState.CrisisIncident incident) {
        if (incident==null || Global.getSector()==null) return;
        install();
        LunaCampaignRenderingPlugin plugin=LunaCampaignRenderer.getRendererOfClass(PTSDNewsTicker.class);
        if (plugin instanceof PTSDNewsTicker) ((PTSDNewsTicker)plugin).show(incident);
    }
    private void show(PTSDCrisisState.CrisisIncident incident){headline=incident.headline==null?"未命名边缘新闻":incident.headline;incidentId=incident.id;shownAt=System.currentTimeMillis();}
    @Override public boolean isExpired(){return false;}
    @Override public EnumSet<CampaignEngineLayers> getActiveLayers(){return EnumSet.of(CampaignEngineLayers.ABOVE);}
    @Override public void advance(float amount){
        boolean down=Mouse.isButtonDown(1);boolean clicked=down&&!rightWasDown;rightWasDown=down;
        if(!clicked||!visible())return;float x=barX(),y=BOTTOM;int mx=Mouse.getX(),my=Mouse.getY();
        if(mx>=x&&mx<=x+WIDTH&&my>=y&&my<=y+HEIGHT){CampaignUIAPI ui=Global.getSector().getCampaignUI();Object target=PTSDCrisisNewsIntel.find(incidentId);if(target==null)target=PTSDCrisisIntel.ensureIntel();ui.clearLaidInCourse();ui.showCoreUITab(CoreUITabId.INTEL,target);}
    }
    private boolean visible(){if(shownAt<=0||System.currentTimeMillis()-shownAt>DISPLAY_MS||Global.getSector()==null)return false;CampaignUIAPI ui=Global.getSector().getCampaignUI();return !ui.isShowingDialog()&&!ui.isShowingMenu()&&ui.getCurrentCoreTab()==null&&!ui.isHideUI();}
    private float progress(){long age=System.currentTimeMillis()-shownAt;if(age<650)return Math.max(0f,age/650f);if(age>11500)return Math.max(0f,(DISPLAY_MS-age)/1500f);return 1f;}
    private float barX(){float p=progress();return Display.getWidth()-WIDTH-20f+(1f-p)*(WIDTH+30f);}
    @Override public void render(CampaignEngineLayers layer,ViewportAPI viewport){
        if(layer!=CampaignEngineLayers.ABOVE||!visible())return;float p=progress(),x=barX(),y=BOTTOM;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glLoadIdentity();GL11.glOrtho(0,Display.getWidth(),0,Display.getHeight(),-1,1);GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();GL11.glLoadIdentity();GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
        Color t = Global.getSettings().getBasePlayerColor();
        Color b = Global.getSettings().getBrightPlayerColor();
        quad(x,y,x+WIDTH,y+HEIGHT,new Color(7,17,24,Math.round(225*p)));quad(x,y+HEIGHT-3f,x+WIDTH,y+HEIGHT,new Color(t.getRed(),t.getGreen(),t.getBlue(),Math.round(245*p)));quad(x,y,x+8f,y+HEIGHT,new Color(b.getRed(),b.getGreen(),b.getBlue(),Math.round(245*p)));
        try{
            if(text==null) {
                // The small uni16 atlas has no CJK glyphs and renders Chinese as '?'.
                // This localized campaign UI atlas includes the active Chinese glyph set.
                text = LazyFont.loadFont("graphics/fonts/orbitron20aabold.fnt").createText();
                text.setFontSize(15f);
                text.setMaxWidth(WIDTH - 30f);
            }
            String current = "边缘新闻  //  " + headline + "";
            if (!current.equals(laidOutText)) {
                text.setText(current);
                laidOutText = current;
            }
            text.setBaseColor(new Color(226, 217, 194, Math.round(255 * p)));
            text.draw(x + 17f, y + 9f);
        }
        catch(Throwable ignored){}
        GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glPopAttrib();
    }
    private static void quad(float x1,float y1,float x2,float y2,Color c){GL11.glColor4f(c.getRed()/255f,c.getGreen()/255f,c.getBlue()/255f,c.getAlpha()/255f);GL11.glBegin(GL11.GL_QUADS);GL11.glVertex2f(x1,y1);GL11.glVertex2f(x2,y1);GL11.glVertex2f(x2,y2);GL11.glVertex2f(x1,y2);GL11.glEnd();}
}