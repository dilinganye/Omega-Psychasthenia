package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.ui.*;
import java.lang.reflect.*;
import java.awt.Color;
import java.util.*;

public final class InvestigationNavigationTest {
    static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
    public static void main(String[] args)throws Exception{
        SectorAPI old=Global.getSector();SettingsAPI settings=Global.getSettings();
        try{
            Map<String,Object> data=new HashMap<>();
            Constructor<PTSDCrisisState> c=PTSDCrisisState.class.getDeclaredConstructor();c.setAccessible(true);
            PTSDCrisisState state=c.newInstance();state.progressInitialized=true;data.put(PTSDCrisisState.PERSISTENT_KEY,state);
            CampaignClockAPI clock=NewsSceneMechanicsTest.mock(CampaignClockAPI.class,n->null);
            SectorEntityToken site=NewsSceneMechanicsTest.mock(SectorEntityToken.class,n->n.equals("getId")?"site":null);
            StarSystemAPI system=NewsSceneMechanicsTest.mock(StarSystemAPI.class,n->n.equals("getName")?"Test":n.equals("getId")?"system":null);
            FactionAPI faction=NewsSceneMechanicsTest.mock(FactionAPI.class,n->n.endsWith("Color")?Color.WHITE:null);
            SectorEntityToken[] course={null};
            Global.setSettings(NewsSceneMechanicsTest.mock(SettingsAPI.class,n->null));
            Global.setSector((SectorAPI)Proxy.newProxyInstance(SectorAPI.class.getClassLoader(),new Class[]{SectorAPI.class},(p,m,a)->{
                switch(m.getName()){
                    case "getPersistentData":return data;case "getClock":return clock;
                    case "getFaction":return faction;case "getStarSystem":return system;
                    case "getEntityById":return "site".equals(a[0])?site:null;
                    case "layInCourseFor":course[0]=(SectorEntityToken)a[0];return null;
                }return null;
            }));
            PTSDCrisisState.CrisisIncident i=new PTSDCrisisState.CrisisIncident();
            i.id="lead";i.cardId="SITE-01";i.headline="Test";i.targetEntityId="site";i.targetSystemId="system";
            i.createdDay=PTSDCrisisState.getDay();i.investigationExpiresDay=i.createdDay+30f;
            i.recordedByPlayer=true;state.incidents.add(i);
            PTSDCrisisIntel intel=new PTSDCrisisIntel();intel.focusIncident(i.id);
            List<String> buttons=new ArrayList<>();
            List<String> headings=new ArrayList<>();
            final int[] rowAnchors={0};
            TooltipMakerAPI tooltip=(TooltipMakerAPI)Proxy.newProxyInstance(TooltipMakerAPI.class.getClassLoader(),new Class[]{TooltipMakerAPI.class},(p,m,a)->{
                if(m.getName().equals("addSpacer"))return NewsSceneMechanicsTest.mock(UIComponentAPI.class,n->n.equals("getPosition")?ControlledPatrolRouteTest.proxy(PositionAPI.class,(k,args2)->{if(k.equals("belowLeft"))rowAnchors[0]++;return null;}):null);
                if(m.getName().equals("addSectionHeading"))headings.add(String.valueOf(a[0]));
                if(m.getName().equals("addButton")){buttons.add(String.valueOf(a[1]));return NewsSceneMechanicsTest.mock(ButtonAPI.class,n->n.equals("getPosition")?NewsSceneMechanicsTest.mock(PositionAPI.class,k->null):null);}return null;
            });
            for(int stage=0;stage<3;stage++){
                i.siteMaterialized=stage>0;i.siteConfirmed=stage==2;i.investigationResolved=stage==2;i.investigationReal=stage==2;
                buttons.clear();rowAnchors[0]=0;intel.createSmallDescription(tooltip,600,800);
                check(rowAnchors[0]==2,"Sort/investigation row anchors not restored");
                check(Collections.frequency(buttons,"PTSD_CRISIS_GO_INCIDENT:lead")==2,"Focused/list tracking lost at stage "+stage);
                check(intel.getMapLocation(null)==site,"Focus lost actual target");
                intel.buttonPressConfirmed("PTSD_CRISIS_GO_INCIDENT:lead",NewsSceneMechanicsTest.mock(IntelUIAPI.class,n->null));
                check(course[0]==site,"Navigation did not point at scene");
            }
            // A shared news lead assigned by Je belongs only to the high-energy section.
            state.jePlayerTaskIncidentId=i.id;headings.clear();buttons.clear();
            intel.createSmallDescription(tooltip,600,800);
            check(headings.contains("高能信号")&&!headings.contains("已记录的新闻调查"),"Active Je shared lead misclassified/duplicated");
            check(Collections.frequency(buttons,"PTSD_CRISIS_GO_INCIDENT:lead")==2,"Je navigation lost");
            i.jePlayerAssignment=true;state.jePlayerTaskIncidentId=null;headings.clear();
            intel.createSmallDescription(tooltip,600,800);
            check(headings.contains("高能信号")&&!headings.contains("已记录的新闻调查"),"Completed Je assignment moved to ordinary news");
            i.jePlayerAssignment=false;i.jeExclusiveScene=true;headings.clear();
            intel.createSmallDescription(tooltip,600,800);
            check(!headings.contains("高能信号")&&headings.contains("已记录的新闻调查"),"DEV-exclusive site mistaken for accepted Je assignment");
            i.investigationReal=false;
            check(!PTSDCrisisIntel.canTrackIncident(i),"Ended false/expired lead still tracked");
            i.investigationResolved=false;i.recordedByPlayer=false;
            check(!PTSDCrisisIntel.canTrackIncident(i),"Unaccepted lead exposed");
            i.recordedByPlayer=true;i.cardId="F-TT-01";
            check(!PTSDCrisisIntel.canTrackIncident(i),"Non-spatial report exposed");
            // 奖励后两个分支、顶部焦点和地图标记都不能继续显示已结算调查。
            i.cardId="SITE-01";i.investigationReportsAwarded=3;i.jePlayerAssignment=true;
            headings.clear();buttons.clear();intel.createSmallDescription(tooltip,600,800);
            check(!headings.contains("高能信号")&&!buttons.contains("PTSD_CRISIS_GO_INCIDENT:lead"),"Rewarded Je lead retained");
            check(intel.getMapLocation(null)==null,"Rewarded focus retained map marker");
            for(String mode:Arrays.asList("NEWEST","OLDEST","NEAREST","FARTHEST")) {
                check(buttons.contains("PTSD_CRISIS_SORT:"+mode),"Missing sort button "+mode);
                intel.buttonPressConfirmed("PTSD_CRISIS_SORT:"+mode,NewsSceneMechanicsTest.mock(IntelUIAPI.class,n->null));
            }
            i.investigationReportsAwarded=0;i.investigationExpiresDay=.1f;
            check(!PTSDInvestigationList.visible(i,1f),"Expired Je lead retained");
            System.out.println("InvestigationNavigationTest: passed (rendered buttons, materialization, confirmation, focus and real navigation target)");
        }finally{Global.setSector(old);Global.setSettings(settings);}
    }
}
