package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.ViewportAPI;
import lunalib.lunaUtil.campaign.LunaCampaignRenderer;
import lunalib.lunaUtil.campaign.LunaCampaignRenderingPlugin;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import java.util.EnumSet;

/** Authored visual sequence. Rendering never mutates fleets, CR, markets or crisis progress. */
public final class PTSDFortressCollapseVisual implements LunaCampaignRenderingPlugin {
    public static final float DURATION=90f;
    private static final float[] FRONTS={20f,43f,49f,73f};
    private final String locationId;
    private final Vector2f center;
    private float elapsed;
    private PTSDLateState.Detonation live;
    private long anchor;
    private boolean expired;
    public PTSDFortressCollapseVisual(String locationId,Vector2f center){this.locationId=locationId;this.center=new Vector2f(center);}
    public static void install(PTSDLateState.Detonation d,Vector2f origin){
        LunaCampaignRenderingPlugin existing=LunaCampaignRenderer.getRendererOfClass(PTSDFortressCollapseVisual.class);
        if(existing instanceof PTSDFortressCollapseVisual){
            PTSDFortressCollapseVisual prior=(PTSDFortressCollapseVisual)existing;
            if(prior.live==d&&!prior.expired)return;
            prior.expired=true;
        }
        PTSDFortressCollapseVisual effect=new PTSDFortressCollapseVisual(d.systemId,origin);effect.live=d;
        LunaCampaignRenderer.addTransientRenderer(effect);
    }
    public static boolean preview(){
        if(Global.getSector()==null||!Global.getSettings().isDevMode())return false;
        CampaignFleetAPI player=Global.getSector().getPlayerFleet();if(player==null||player.getContainingLocation()==null)return false;
        LunaCampaignRenderingPlugin running=LunaCampaignRenderer.getRendererOfClass(PTSDFortressCollapseVisual.class);
        if(running instanceof PTSDFortressCollapseVisual&&((PTSDFortressCollapseVisual)running).live!=null&&!running.isExpired())return false;
        stopPreview();
        LunaCampaignRenderer.addTransientRenderer(new PTSDFortressCollapseVisual(player.getContainingLocation().getId(),player.getLocation()));
        return true;
    }
    public static void stopPreview(){
        LunaCampaignRenderingPlugin prior=LunaCampaignRenderer.getRendererOfClass(PTSDFortressCollapseVisual.class);
        if(prior instanceof PTSDFortressCollapseVisual&&((PTSDFortressCollapseVisual)prior).live==null)((PTSDFortressCollapseVisual)prior).expired=true;
    }
    @Override public boolean isExpired(){return expired||(live==null?elapsed>=DURATION:live.exploded);}
    @Override public EnumSet<CampaignEngineLayers> getActiveLayers(){return EnumSet.of(CampaignEngineLayers.ABOVE);}
    @Override public void advance(float amount){
        long now=System.nanoTime();float dt=anchor==0?0:Math.min(.25f,(now-anchor)/1000000000f);anchor=now;
        if(Global.getSector()==null){expired=true;return;}
        if(Global.getCurrentState()!=GameState.CAMPAIGN||Global.getSector().isPaused()||Global.getSector().getCampaignUI().getCurrentCoreTab()!=null||Global.getSector().getCampaignUI().isShowingDialog())return;
        if(live==null)elapsed+=Math.max(0,dt);else elapsed=live.collapseElapsed;
    }
    @Override public void render(CampaignEngineLayers layer,ViewportAPI viewport){
        if(isExpired()||Global.getSector()==null)return;
        CampaignFleetAPI player=Global.getSector().getPlayerFleet();
        if(player==null||player.getContainingLocation()==null||!locationId.equals(player.getContainingLocation().getId()))return;
        CampaignUIAPI ui=Global.getSector().getCampaignUI();if(ui.getCurrentCoreTab()!=null||ui.isShowingDialog()||ui.isShowingMenu())return;
        draw(center,live==null?elapsed:live.collapseElapsed,viewport.getAlphaMult());
    }
    public static float frontRadius(int wave,float seconds){
        if(wave<0||wave>=FRONTS.length)return -1;
        float age=seconds-FRONTS[wave];if(age<0)return -1;
        return age*(wave==3?2200f:1250f)+age*age*18f;
    }
    public static float frontDistance(int wave,float x,float y){
        x-=wave==1?400:wave==2?-330:0;y-=wave==2?220:0;
        float angle=wave*.6f,c=(float)Math.cos(angle),s=(float)Math.sin(angle);
        float u=x*c+y*s,v=-x*s+y*c;
        v/=wave==0?.38f:wave==1?.72f:wave==2?1.15f:1f;
        return (float)Math.sqrt(u*u+v*v);
    }
    public static boolean crosses(int wave,float x,float y,float before,float after){
        float distance=frontDistance(wave,x,y);
        return frontRadius(wave,before)<distance&&frontRadius(wave,after)>=distance;
    }
    public static float frontStart(int wave){return FRONTS[wave];}
    public static int frontCount(){return FRONTS.length;}
    private static float clamp(float v){return Math.max(0,Math.min(1,v));}
    private static float smooth(float v){v=clamp(v);return v*v*(3-2*v);}
    public static void draw(Vector2f origin,float t,float alpha){
        float fade=1-smooth((t-80)/10);if(fade<=0||alpha<=0)return;
        alpha*=fade;
        boolean shaders=org.lwjgl.opengl.GLContext.getCapabilities().OpenGL20;
        int previousProgram=shaders?GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM):0;
        int previousUnit=GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try{
            org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
            GL11.glEnable(GL11.GL_BLEND);GL11.glDisable(GL11.GL_DEPTH_TEST);
            boolean relief=PTSDCollapseMaterials.prepare();
            float opening=smooth(t/7),rupture=smooth((t-12)/14);
            float silence=1-.88f*smooth((t-32)/4)*(1-smooth((t-39)/4));
            float inward=opening*(1-smooth((t-62)/10));
            // Inversion has a mottled, feathered mask, instead of a pale solid disc.
            float inverse=.32f*opening*(1-smooth((t-35)/9))*alpha;
            GL11.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR,GL11.GL_ONE_MINUS_SRC_COLOR);
            if(relief)stamp(PTSDCollapseMaterials.SMOKE,origin.x,origin.y,3700,3400,-t*.035f,1,1,1,inverse,1,t);
            else softMask(origin.x,origin.y,1900,inverse,0);
            float multiply=smooth((t-59)/7)*(1-smooth((t-76)/4))*alpha;
            GL11.glBlendFunc(GL11.GL_DST_COLOR,GL11.GL_ZERO);
            if(relief)stamp(PTSDCollapseMaterials.SMOKE,origin.x,origin.y,7500,6800,t*.02f,1,1,1,.75f*multiply,2,t);
            else softMask(origin.x,origin.y,4000,1-.5f*multiply,1);

            // Dense low-contrast cloud body and hot, counter-rotating knots form a volumetric vortex.
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);
            for(int i=0;i<72;i++){
                float phase=(t*.055f+i*.618034f)%1f;
                float life=PTSDCollapseMaterials.feather(phase); // fades before each deterministic recycle
                float angle=i*2.399963f-t*(.12f+(i%5)*.012f);
                float radius=380+3200*(1-phase)*(1-phase)+rupture*(i%4)*120;
                float x=origin.x+(float)Math.cos(angle)*radius,y=origin.y+(float)Math.sin(angle)*radius*(.68f+.04f*(i%5));
                float size=450+(i%7)*100+phase*450;
                stamp(i%3==0?PTSDCollapseMaterials.CLOUD:PTSDCollapseMaterials.SMOKE,x,y,size,size*(.6f+phase*.3f),angle+phase*2,
                        .30f,.18f,.56f,.18f*life*inward*silence*alpha,0,t);
                if(i%3==0)stamp(PTSDCollapseMaterials.GLOW,x,y,size*.24f,size*.1f,angle,.48f,.66f,1f,.28f*life*inward*silence*alpha,0,t);
            }
            for(int i=0;i<5;i++){
                float r=(720+i*320)*(1-.5f*smooth(t/20))+rupture*i*200;
                float x=origin.x+(float)Math.sin(i*2.4+t*.17)*rupture*180;
                float y=origin.y+(float)Math.cos(i*1.7+t*.13)*rupture*160;
                stamp(PTSDCollapseMaterials.WAVE,x,y,r*2.5f,r*(1.0f+i*.12f),t*(i%2==0?-.09f:.07f)+i,
                        .46f,.22f,.85f,.26f*inward*silence*alpha,0,t);
            }
            // Dark cavity remains soft; no hard-edged black disc.
            if(relief){GL11.glBlendFunc(GL11.GL_DST_COLOR,GL11.GL_ZERO);
                stamp(PTSDCollapseMaterials.GLOW,origin.x,origin.y,1000,1000,0,1,1,1,.9f*inward*alpha,2,t);}
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);
            for(int n=0;n<FRONTS.length;n++){
                float radius=frontRadius(n,t);if(radius<=0)continue;
                float age=t-FRONTS[n],visibility=smooth(age/.65f)*(1-smooth((age-17)/10))*alpha;if(visibility<=0)continue;
                float x=origin.x+(n==1?400:n==2?-330:0),y=origin.y+(n==2?220:0);
                float aspect=n==0?.38f:n==1?.72f:n==2?1.15f:1f,rot=n*.6f;
                float width=Math.min(radius*.8f,700+age*65);
                PTSDCollapseMaterials.annulus(x,y,radius,width,aspect,rot,.5f*visibility,t,n);
                // Rolling vapour rides the same elliptical front. Broad wakes, no wire outlines.
                for(int i=0;i<24;i++){
                    float a=(i+.25f*(float)Math.sin(i*19.7f))*(float)Math.PI/12,cs=(float)Math.cos(rot),sn=(float)Math.sin(rot);
                    float u=(float)Math.cos(a)*radius,v=(float)Math.sin(a)*radius*aspect;
                    float size=width*(1.1f+(i%3)*.3f);
                    stamp(i%3==0?PTSDCollapseMaterials.SMOKE:PTSDCollapseMaterials.CLOUD,x+u*cs-v*sn,y+u*sn+v*cs,size*1.6f,size,a+rot+t*.08f+i,
                            .43f,.32f,.68f,.32f*visibility,0,t);
                }
            }
            float age=t-73;
            if(age>=0){
                float core=smooth(age/.35f)*(1-smooth(age/6))*alpha;
                stamp(PTSDCollapseMaterials.GLOW,origin.x,origin.y,1800+age*600,1800+age*600,0,1f,.75f,.57f,.8f*core,0,t);
                stamp(PTSDCollapseMaterials.FIRE,origin.x,origin.y,1000+age*700,1000+age*700,age*.12f,1f,.65f,.85f,.75f*core,0,t);
                for(int i=0;i<20;i++){
                    float a=i*2.399963f,rad=age*(140+i%5*35),size=700+age*150;
                    float life=smooth(age/.8f)*(1-smooth((age-8)/9))*alpha;
                    stamp(PTSDCollapseMaterials.CLOUD,origin.x+(float)Math.cos(a)*rad,origin.y+(float)Math.sin(a)*rad,
                            size,size*.8f,a-age*.07f,.42f,.24f,.48f,.24f*life,0,t);
                }
                // Actual textured fragments tumble within plumes; no geometric arrowheads or line tails.
                for(int i=0;i<42;i++){
                    float angle=i*2.399963f,velocity=420+(i*193%950),r=age*velocity;
                    float x=origin.x+(float)Math.cos(angle)*r,y=origin.y+(float)Math.sin(angle)*r;
                    float life=smooth(age/.3f)*(1-smooth(age/17))*alpha;
                    float size=70+i%7*25;
                    stamp(PTSDCollapseMaterials.SMOKE,x-(float)Math.cos(angle)*size*2,y-(float)Math.sin(angle)*size*2,
                            size*(5+age*.25f),size*2.4f,angle,.48f,.23f,.62f,.35f*life,0,t);
                    stamp(PTSDCollapseMaterials.FIRE,x,y,size*2,size*1.4f,angle+age*.4f,.95f,.53f,.29f,.55f*life,0,t);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
                    stamp(PTSDCollapseMaterials.DEBRIS,x,y,size,size*.7f,angle+age*(i%2==0?.8f:-.6f),.7f,.58f,.65f,life,3,t);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);
                }
            }
        }finally{
            if(shaders)org.lwjgl.opengl.GL20.glUseProgram(previousProgram);
            GL11.glPopAttrib();org.lwjgl.opengl.GL13.glActiveTexture(previousUnit);
        }
    }
    private static void stamp(String texture,float x,float y,float w,float h,float angle,float r,float g,float b,float alpha,int mode,float t){
        PTSDCollapseMaterials.billboard(texture,x,y,w,h,angle,r,g,b,alpha,mode,t);
    }
    /** Shader-free inverse/multiply fallback with a smooth zero-slope boundary. */
    private static void softMask(float x,float y,float radius,float inner,float outer){
        if(org.lwjgl.opengl.GLContext.getCapabilities().OpenGL20)org.lwjgl.opengl.GL20.glUseProgram(0);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        for(int band=0;band<24;band++){
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for(int i=0;i<=96;i++)for(int side=0;side<2;side++){
                float p=(band+side)/24f,c=inner+(outer-inner)*smooth(p),r=radius*p;
                double a=i*Math.PI*2/96;GL11.glColor4f(c,c,c,1);GL11.glVertex2f(x+(float)Math.cos(a)*r,y+(float)Math.sin(a)*r);
            }GL11.glEnd();
        }
    }
}
