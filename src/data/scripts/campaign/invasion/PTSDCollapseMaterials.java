package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.opengl.*;
import java.util.HashMap;
import java.util.Map;

/** Bounded, lazy material cache shared by campaign and combat. No combat-engine dependency. */
final class PTSDCollapseMaterials {
    static final String CLOUD="graphics/fx/nebula_colorless.png";
    static final String SMOKE="graphics/fx/Impact/Particle_Smoke_Dissolve_02.png";
    static final String WAVE="graphics/fx/AE/Particle_AE_Shockwave_12.png";
    static final String TRAIL="graphics/fx/PTSD_trail_noise.png";
    static final String FIRE="graphics/fx/explosion4.png";
    static final String GLOW="graphics/fx/particlealpha64sq.png";
    static final String DEBRIS="graphics/debris/debris_med1.png";
    private static final Map<String,SpriteAPI> textures=new HashMap<String,SpriteAPI>();
    private static int program,modeUniform,lightUniform,texelUniform;
    private static boolean shaderAttempted;
    private PTSDCollapseMaterials(){}
    static SpriteAPI texture(String path){
        if(textures.containsKey(path))return textures.get(path);
        SpriteAPI sprite=null;
        try{Global.getSettings().loadTexture(path);sprite=Global.getSettings().getSprite(path);}
        catch(Exception ex){Global.getLogger(PTSDCollapseMaterials.class).warn("Collapse material missing: "+path,ex);}
        textures.put(path,sprite);return sprite;
    }
    static boolean prepare(){
        if(shaderAttempted)return program!=0;shaderAttempted=true;
        if(!GLContext.getCapabilities().OpenGL20)return false;
        int vertex=0,fragment=0,candidate=0;
        try{
            vertex=compile(GL20.GL_VERTEX_SHADER,"data/shaders/ptsd_collapse.vert");
            fragment=compile(GL20.GL_FRAGMENT_SHADER,"data/shaders/ptsd_collapse.frag");
            candidate=GL20.glCreateProgram();GL20.glAttachShader(candidate,vertex);GL20.glAttachShader(candidate,fragment);GL20.glLinkProgram(candidate);
            if(GL20.glGetProgrami(candidate,GL20.GL_LINK_STATUS)==GL11.GL_FALSE)throw new IllegalStateException(GL20.glGetProgramInfoLog(candidate,4096));
            program=candidate;candidate=0;
            modeUniform=GL20.glGetUniformLocation(program,"mode");lightUniform=GL20.glGetUniformLocation(program,"lightDirection");texelUniform=GL20.glGetUniformLocation(program,"texel");
            GL20.glUseProgram(program);GL20.glUniform1i(GL20.glGetUniformLocation(program,"image"),0);
            Global.getLogger(PTSDCollapseMaterials.class).info("Collapse textured relief shader ready");
        }catch(Exception ex){Global.getLogger(PTSDCollapseMaterials.class).warn("Collapse relief unavailable; textured additive fallback retained",ex);}
        finally{if(vertex!=0)GL20.glDeleteShader(vertex);if(fragment!=0)GL20.glDeleteShader(fragment);if(candidate!=0)GL20.glDeleteProgram(candidate);}
        return program!=0;
    }
    private static int compile(int type,String file)throws Exception{
        int shader=GL20.glCreateShader(type);
        try{GL20.glShaderSource(shader,Global.getSettings().loadText(file));GL20.glCompileShader(shader);
            if(GL20.glGetShaderi(shader,GL20.GL_COMPILE_STATUS)==GL11.GL_FALSE)throw new IllegalStateException(GL20.glGetShaderInfoLog(shader,4096));return shader;
        }catch(Exception ex){GL20.glDeleteShader(shader);throw ex;}
    }
    /** mode 0: lit vapour; 1: inverse mask; 2: multiply mask; 3: solid textured debris. */
    static boolean bind(String path,int mode,float t){
        SpriteAPI sprite=texture(path);if(sprite==null)return false;
        GL11.glEnable(GL11.GL_TEXTURE_2D);sprite.bindTexture();
        if(program!=0){GL20.glUseProgram(program);GL20.glUniform1i(modeUniform,mode);
            GL20.glUniform3f(lightUniform,(float)Math.cos(t*.13f),.6f,(float)Math.sin(t*.13f)*.3f+.8f);
            GL20.glUniform2f(texelUniform,sprite.getTextureWidth()/Math.max(1,sprite.getWidth()),sprite.getTextureHeight()/Math.max(1,sprite.getHeight()));}
        else if(GLContext.getCapabilities().OpenGL20)GL20.glUseProgram(0);
        return true;
    }
    static void billboard(String path,float x,float y,float w,float h,float angle,float r,float g,float b,float alpha,int mode,float t){
        if(alpha<=.001f||w<=0||h<=0)return;
        // A raw RGB texture cannot be a destination-colour mask without the material shader.
        if(program==0&&(mode==1||mode==2))return;
        if(!bind(path,mode,t))return;
        SpriteAPI sprite=textures.get(path);
        float c=(float)Math.cos(angle),s=(float)Math.sin(angle),tw=sprite.getTextureWidth(),th=sprite.getTextureHeight();
        GL11.glColor4f(r,g,b,Math.min(1,alpha));GL11.glBegin(GL11.GL_QUADS);
        for(int k=0;k<4;k++){
            float u=(k==1||k==2)?1:0,v=k>=2?1:0,px=(u-.5f)*w,py=(v-.5f)*h;
            GL11.glTexCoord2f(u*tw,v*th);GL11.glVertex2f(x+px*c-py*s,y+px*s+py*c);
        }GL11.glEnd();
    }
    static float feather(float v){return (float)Math.pow(Math.sin(Math.PI*Math.max(0,Math.min(1,v))),2);}
    /** Feathered textured strip: radius is the same authored front used for damage, never jittered. */
    static void annulus(float x,float y,float radius,float width,float aspect,float angle,float alpha,float t,int seed){
        if(radius<=0||alpha<=.001f||!bind(TRAIL,4,t))return;
        SpriteAPI sprite=textures.get(TRAIL);float c=(float)Math.cos(angle),s=(float)Math.sin(angle);
        final int segments=192,bands=6;
        GL11.glBegin(GL11.GL_QUADS);
        for(int i=0;i<segments;i++)for(int band=0;band<bands;band++)for(int k=0;k<4;k++){
            int end=(k==1||k==2)?1:0;float v=(band+(k>=2?1:0))/(float)bands;
            float a=(i+end)*(float)(Math.PI*2/segments);
            float rad=radius+(v-.5f)*width;
            float modulation=.65f+.35f*(float)Math.sin(a*7+t*.7f+seed);
            GL11.glColor4f(.65f,.40f,1f,alpha*feather(v)*modulation);
            // Eight repeats, aligned at segment boundaries; no sampling into padded texture space.
            GL11.glTexCoord2f(((i%24)+end)/24f*sprite.getTextureWidth(),v*sprite.getTextureHeight());
            float u=(float)Math.cos(a)*rad,z=(float)Math.sin(a)*rad*aspect;
            GL11.glVertex2f(x+u*c-z*s,y+u*s+z*c);
        }GL11.glEnd();
    }
}
