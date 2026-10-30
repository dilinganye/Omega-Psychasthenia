package data.scripts.campaign.invasion;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.*;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector2f;
import java.nio.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.lang.reflect.Proxy;
import java.util.*;
/** Optional real-GL offscreen material check. Does not launch or modify a campaign. */
public final class CollapseMaterialRenderTest {
 public static void main(String[] args)throws Exception{
  final boolean fallback=args.length>3 && "fallback".equals(args[3]);
  final Path mod=Paths.get(args[0]),core=Paths.get(args[1]),out=Paths.get(args[2]);Files.createDirectories(out);
  Pbuffer buffer=new Pbuffer(1000,700,new PixelFormat(),null,null);buffer.makeCurrent();
  final Map<String,SpriteAPI> images=new HashMap<>();
  Global.setSettings((SettingsAPI)Proxy.newProxyInstance(SettingsAPI.class.getClassLoader(),new Class[]{SettingsAPI.class},(o,m,a)->{
   if(m.getName().equals("loadText")){if(fallback)throw new java.io.IOException("Intentional shader failure for fallback test");return new String(Files.readAllBytes(mod.resolve((String)a[0])),"UTF-8");}
   if(m.getName().equals("loadTexture")){
    String name=(String)a[0];if(images.containsKey(name))return null;
    Path path=mod.resolve(name);if(!Files.exists(path))path=core.resolve(name);
    BufferedImage image=ImageIO.read(path.toFile());int w=image.getWidth(),h=image.getHeight();
    ByteBuffer pixels=BufferUtils.createByteBuffer(w*h*4);
    for(int y=h-1;y>=0;y--)for(int x=0;x<w;x++){int c=image.getRGB(x,y);pixels.put((byte)(c>>16)).put((byte)(c>>8)).put((byte)c).put((byte)(c>>24));}pixels.flip();
    int id=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,id);
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
    GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,w,h,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
    images.put(name,(SpriteAPI)Proxy.newProxyInstance(SpriteAPI.class.getClassLoader(),new Class[]{SpriteAPI.class},(q,r,b)->{
     if(r.getName().equals("bindTexture")){GL11.glBindTexture(GL11.GL_TEXTURE_2D,id);return null;}
     if(r.getName().equals("getWidth"))return (float)w;if(r.getName().equals("getHeight"))return (float)h;
     if(r.getName().equals("getTextureWidth")||r.getName().equals("getTextureHeight"))return 1f;
     return null;
    }));return null;
   }
   if(m.getName().equals("getSprite"))return images.get((String)a[0]);return null;
  }));
  GL11.glViewport(0,0,1000,700);GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GL11.glOrtho(-6000,6000,-4200,4200,-1,1);GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();
  for(float t:new float[]{12,24,46,74,77}){
   GL11.glClearColor(.025f,.035f,.06f,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);GL11.glDisable(GL11.GL_TEXTURE_2D);
   GL11.glPointSize(2);GL11.glBegin(GL11.GL_POINTS);Random rng=new Random(17);
   for(int i=0;i<1000;i++){float brightness=.15f+rng.nextFloat()*.5f;GL11.glColor3f(brightness,brightness,brightness);GL11.glVertex2f(rng.nextFloat()*12000-6000,rng.nextFloat()*8400-4200);}GL11.glEnd();
   PTSDFortressCollapseVisual.draw(new Vector2f(),t,1);
   if(PTSDCollapseMaterials.prepare()==fallback)throw new AssertionError("Unexpected shader/fallback mode");
   if(GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM)!=0)throw new AssertionError("Shader state leaked");
   int error=GL11.glGetError();if(error!=GL11.GL_NO_ERROR)throw new AssertionError("GL error "+error);
   ByteBuffer result=BufferUtils.createByteBuffer(1000*700*4);GL11.glReadPixels(0,0,1000,700,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,result);
   BufferedImage frame=new BufferedImage(1000,700,BufferedImage.TYPE_INT_RGB);
   for(int y=0;y<700;y++)for(int x=0;x<1000;x++){int k=(y*1000+x)*4;frame.setRGB(x,699-y,((result.get(k)&255)<<16)|((result.get(k+1)&255)<<8)|(result.get(k+2)&255));}
   ImageIO.write(frame,"png",out.resolve("collapse-"+(int)t+"s.png").toFile());
  }
  buffer.destroy();System.out.println("CollapseMaterialRenderTest passed: real GL shader, textures, state restoration, five frames; fallback="+fallback);
 }
}

