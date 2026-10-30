package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import lunalib.backend.ui.components.base.LunaUIBaseElement;
import lunalib.backend.ui.settings.*;
import org.lwjgl.opengl.GL11;
import java.awt.Color;
import java.util.*;

/** Optional LunaLib adapter: attaches only to this mod's dedicated editor card. */
public final class PTSDDifficultyCurveEditor extends BaseCustomUIPanelPlugin {
    private static final Set<LunaUIBaseElement> installed=Collections.newSetFromMap(new WeakHashMap<LunaUIBaseElement,Boolean>());
    private static boolean failed;
    private static final Set<CustomPanelAPI> roots=Collections.newSetFromMap(new WeakHashMap<CustomPanelAPI,Boolean>());
    /** The modal UI advances even when campaign scripts are suspended. */
    public static final class DialogListener extends com.fs.starfarer.api.campaign.BaseCampaignEventListener {
        public DialogListener(){super(false);}
        public void reportShownInteractionDialog(com.fs.starfarer.api.campaign.InteractionDialogAPI dialog){attach(dialog);}
    }
    public static void attach(com.fs.starfarer.api.campaign.InteractionDialogAPI dialog){
        if(dialog==null||!IIRT_Omega_ModPlugin.hasLunaLib)return;
        if(!(dialog.getPlugin() instanceof lunalib.backend.ui.OpenCustomPanelFromDialog))return;
        // LunaLib splits this return type into jars/Lunalib-Kotlin.jar; both Luna JARs are required.
        lunalib.lunaUI.panel.LunaBaseCustomPanelPlugin main=((lunalib.backend.ui.OpenCustomPanelFromDialog)dialog.getPlugin()).getPlugin();
        if(!(main instanceof LunaSettingsUIMainPanel))return;
        // Public Luna API; no reflection or access to unrelated dialogs.
        CustomPanelAPI root=main.panel;
        if(root==null||roots.contains(root))return;
        root.addComponent(root.createCustomPanel(1,1,new BaseCustomUIPanelPlugin(){
            public void advance(float amount){sync();}
        })).inTL(0,0);
        roots.add(root);
    }
    public static void sync(){
        if(failed || !IIRT_Omega_ModPlugin.hasLunaLib || !Global.getSettings().isDevMode())return;
        try{
            for(LunaUIBaseElement e:new ArrayList<LunaUIBaseElement>(LunaSettingsUISettingsPanel.Companion.getAddedElements())){
                if(!(e.getKey() instanceof LunaSettingsData)||installed.contains(e))continue;
                LunaSettingsData d=(LunaSettingsData)e.getKey();
                if(!IIRT_Omega_ModPlugin.getModId().equals(d.getModID())||!"PTSD_curve_editor".equals(d.getFieldID()))continue;
                CustomPanelAPI host=e.getPanel();
                PTSDDifficultyCurveEditor plugin=new PTSDDifficultyCurveEditor();
                CustomPanelAPI panel=host.createCustomPanel(host.getPosition().getWidth(),824,plugin);
                host.addComponent(panel).inTL(0,0);host.bringComponentToTop(panel);plugin.build(panel);installed.add(e);
                Global.getLogger(PTSDDifficultyCurveEditor.class).info("Mounted four-curve editor in LunaLib");
            }
        }catch(Throwable ex){failed=true;Global.getLogger(PTSDDifficultyCurveEditor.class).error("Luna curve editor unavailable; numeric curve fields remain available",ex);}
    }
    private Map<String,Object> values=PTSDDifficultySettings.curveValues();
    private PositionAPI position;
    private LabelAPI status;
    private final double[] ymax={8,3,3,20};
    private int dragRow=-1,dragPhase,dragPoint;
    private static final String[] LABELS={"沉寂","侦察","营建","要塞","动员","战争"};
    private String key(int row,int phase){String p=PTSDDifficultySettings.PHASES[phase];return row==0?PTSDDifficultySettings.curveId(p):row==1?PTSDDifficultySettings.growthId(p):row==2?PTSDDifficultySettings.productionId(p):PTSDDifficultySettings.officerId(p);}
    private double[] points(int row,int phase){return PTSDStageCurve.parse(String.valueOf(values.get(key(row,phase))));}
    private void setPoints(int row,int phase,double[] p){StringBuilder s=new StringBuilder();for(double v:p){if(s.length()>0)s.append(',');s.append(String.format(Locale.ROOT,"%.3f",v));}values.put(key(row,phase),s.toString());}
    private void build(CustomPanelAPI panel){
        float width=panel.getPosition().getWidth();
        TooltipMakerAPI head=panel.createUIElement(width-24,48,false);
        head.addPara("四曲线编辑 · 拖动点调整数值，右键加点，Shift＋右键删点",0,Color.CYAN,new String[0]);
        head.addPara("Alt＋左键拖动取整数；控制点等距，阶段独立。增长图下方：封顶 / 继续增长。",4);
        panel.addUIElement(head).inTL(12,8);
        for(int row=0;row<4;row++){
            TooltipMakerAPI title=panel.createUIElement(width-24,20,false);
            title.addPara(new String[]{"基础难度调控","舰队强度增长率","战略资产生产速度","军官等级（绝对等级，2～20）"}[row],0,Color.WHITE,new String[0]);
            panel.addUIElement(title).inTL(12,58+row*174);
            for(int phase=0;phase<6;phase++){
                float cell=(width-24)/6;
                TooltipMakerAPI label=panel.createUIElement(cell,40,false);
                label.addPara(LABELS[phase],0,Color.CYAN,new String[0]);
                if(row==1)label.addPara("[>| ]  [>>>]",3);
                panel.addUIElement(label).inTL(12+phase*cell,200+row*174);
                for(double v:points(row,phase))ymax[row]=Math.max(ymax[row],Math.ceil(v*1.2));
            }
        }
        TooltipMakerAPI foot=panel.createUIElement(width-24,64,false);
        foot.addPara("应用并保存           恢复已保存           纵轴放大＋ / 缩小－",0,Color.CYAN,new String[0]);
        status=foot.addPara("尚未修改。关闭前请应用；未应用的修改不影响游戏。",6);
        panel.addUIElement(foot).inTL(12,772);
    }
    public void positionChanged(PositionAPI p){position=p;}
    public void processInput(java.util.List<InputEventAPI> events){
        if(position==null||!Global.getSettings().isDevMode())return;
        for(InputEventAPI e:events){
            if(e.isConsumed())continue;
            if(e.isLMBUpEvent()){dragRow=-1;continue;}
            float x=e.getX()-position.getX(),top=position.getY()+position.getHeight()-e.getY();
            if(!position.containsEvent(e))continue;
            float cell=(position.getWidth()-24)/6;
            if(e.isLMBDownEvent()&&top>=768&&top<799){
                if(x<130){try{PTSDDifficultySettings.applyCurves(values);status.setText(PTSDDifficultySettings.snapshotStatus());}catch(Exception ex){status.setText("保存失败，请检查日志。");Global.getLogger(getClass()).error("Curve save failed",ex);}}
                else if(x<270){values=PTSDDifficultySettings.curveValues();status.setText("已恢复保存值。");}
                else {for(int r=0;r<4;r++)ymax[r]=Math.max(.5,Math.min(100,ymax[r]*(x<position.getWidth()-65?2:.5)));status.setText("纵轴上限："+Arrays.toString(ymax));}
                e.consume();continue;
            }
            if(e.isLMBDownEvent()&&top>=390&&top<417&&x>=12&&x<position.getWidth()-12){
                int phase=Math.min(5,(int)((x-12)/cell));float local=(x-12)-phase*cell;
                values.put(PTSDDifficultySettings.limitId(PTSDDifficultySettings.PHASES[phase]),local<cell/2?0:1);
                status.setText(LABELS[phase]+(local<cell/2?"：增长封顶":"：继续增长"));e.consume();continue;
            }
            for(int row=0;row<4;row++){
                float start=82+row*174;
                if(top<start||top>start+112||x<12||x>position.getWidth()-12)continue;
                int phase=Math.min(5,(int)((x-12)/cell));double[] p=points(row,phase);
                int point=(int)Math.round(((x-12)/cell-phase)*(p.length-1));point=Math.max(0,Math.min(p.length-1,point));
                if(e.isRMBDownEvent()){
                    int count=p.length+(e.isShiftDown()?-1:1);
                    if(count>=2&&count<=64){double[] q=new double[count];for(int j=0;j<count;j++)q[j]=PTSDStageCurve.sample(p,j/(double)(count-1));setPoints(row,phase,q);status.setText(LABELS[phase]+"："+count+"个等距控制点，尚未保存。");}
                    e.consume();
                }else if(e.isLMBDownEvent()){dragRow=row;dragPhase=phase;dragPoint=point;e.consume();}
                if(dragRow==row&&(e.isMouseMoveEvent()||e.isLMBDownEvent())){
                    double[] q=points(row,dragPhase);q[dragPoint]=dragValue(row,(1-(top-start)/112)*ymax[row],e.isAltDown());setPoints(row,dragPhase,q);
                    status.setText(LABELS[dragPhase]+"："+values.get(key(row,dragPhase))+"（未保存）");e.consume();
                }
            }
        }
    }
    static double dragValue(int row,double value,boolean integer){
        double clamped=Math.max(row==3?2:0,Math.min(row==3?20:100,value));
        return integer?Math.round(clamped):clamped;
    }
    public void renderBelow(float alpha){
        if(position==null)return;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
        float x=position.getX(),y=position.getY(),w=position.getWidth(),h=position.getHeight(),cell=(w-24)/6;
        GL11.glColor4f(.035f,.05f,.06f,1);GL11.glRectf(x,y,x+w,y+h);
        for(int r=0;r<4;r++){
            float bottom=y+h-(82+r*174)-112;
            for(int phase=0;phase<6;phase++){
                float left=x+12+cell*phase;
                GL11.glColor4f(.1f,.7f,.85f,alpha*.65f);GL11.glBegin(GL11.GL_LINE_STRIP);GL11.glVertex2f(left,bottom+112);GL11.glVertex2f(left,bottom);GL11.glVertex2f(left+cell,bottom);GL11.glEnd();
                double[] p=points(r,phase);GL11.glColor4f(1,.32f,.36f,alpha);GL11.glBegin(GL11.GL_LINE_STRIP);
                for(int j=0;j<=64;j++)GL11.glVertex2f(left+cell*j/64f,bottom+(float)(112*PTSDStageCurve.sample(p,j/64d)/ymax[r]));GL11.glEnd();
                GL11.glPointSize(6);GL11.glBegin(GL11.GL_POINTS);for(int j=0;j<p.length;j++)GL11.glVertex2f(left+cell*j/(p.length-1),bottom+(float)(112*p[j]/ymax[r]));GL11.glEnd();
                if(r==1){boolean open=((Number)values.get(PTSDDifficultySettings.limitId(PTSDDifficultySettings.PHASES[phase]))).intValue()==1;
                    GL11.glColor4f(.1f,.8f,.8f,.25f);float bx=left+(open?cell/2:0);GL11.glRectf(bx,bottom-42,bx+cell/2,bottom-20);}
            }
        }
        GL11.glPopAttrib();
    }
}
