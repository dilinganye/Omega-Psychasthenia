package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.ids.Pings;
import org.lwjgl.util.vector.Vector2f;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import java.awt.Color;
import java.util.*;

public final class NewsPresentationRegressionTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        PTSDCrisisState.CrisisIncident i=new PTSDCrisisState.CrisisIncident();i.targetSystemId="system";
        i.cardId="F-TT-01";i.investigable=true;i.siteMaterialized=true;i.recordedByPlayer=true;
        check(!PTSDNewsPresentation.hasPublicLocation(i)&&!PTSDNewsPresentation.canInvestigate(i),"Saved tax report exposed map/acceptance");
        i.cardId="N-01";i.investigable=false;i.siteMaterialized=false;i.recordedByPlayer=false;
        check(!PTSDNewsPresentation.hasPublicLocation(i),"Random simulation target exposed ordinary article location");
        i.cardId="N-09";
        check(PTSDNewsPresentation.hasPublicLocation(i)&&!PTSDNewsPresentation.canInvestigate(i),"Ambient salvage lost map or gained accept button");
        check(PTSDNewsAmbientSalvage.eligible(i,5,"system"),"Unaccepted salvage cannot appear");
        check(!PTSDNewsAmbientSalvage.eligible(i,5,"elsewhere")&&!PTSDNewsAmbientSalvage.eligible(i,30,"system"),"Salvage created outside location/window");
        i.siteMaterialized=true;check(!PTSDNewsAmbientSalvage.eligible(i,5,"system"),"Salvage duplicate after re-entry");
        i.siteMaterialized=false;i.siteEntityIds.add("existing");check(!PTSDNewsAmbientSalvage.eligible(i,5,"system"),"Existing site copied");
        i.cardId="E2-01";check(!PTSDNewsPresentation.hasPublicLocation(i),"Report-only E2 exposes a fabricated location");
        i.siteEntityIds.clear();i.cardId="SITE-01";i.investigable=true;
        i.investigationOutcome=1;check(PTSDNewsPresentation.hasPublicLocation(i),"True lead lost map");
        i.investigationOutcome=2;check(PTSDNewsPresentation.hasPublicLocation(i),"Map visibility leaks truth roll");
        i.targetSystemId=null;check(!PTSDNewsPresentation.hasPublicLocation(i),"Missing location renders empty map");
        SectorAPI previous=Global.getSector();
        try{
            LocationAPI location=NewsSceneMechanicsTest.mock(LocationAPI.class,n->null);
            CampaignFleetAPI player=NewsSceneMechanicsTest.mock(CampaignFleetAPI.class,n->n.equals("getContainingLocation")?location:n.equals("getVelocity")?new Vector2f(100,0):null);
            SectorEntityToken source=NewsSceneMechanicsTest.mock(SectorEntityToken.class,n->n.equals("getContainingLocation")?location:null);
            final String[] ping={null};
            Global.setSector((SectorAPI)Proxy.newProxyInstance(SectorAPI.class.getClassLoader(),new Class[]{SectorAPI.class},(p,m,a)->{
                if(m.getName().equals("getPlayerFleet"))return player;
                if(m.getName().equals("addPing")){ping[0]=(String)a[1];return null;}
                return null;
            }));
            PTSDNewsScene.pulse(source,false);
            check(Pings.INTERDICT.equals(ping[0]),"Pulse used wrong ping ID");
            String config=new String(Files.readAllBytes(Paths.get("D:/Game/Starsector_098/starsector-core/data/campaign/pings.json")),StandardCharsets.UTF_8);
            check(config.contains("\""+ping[0]+"\":"),"Pulse spec absent from installed vanilla config");
        }finally{Global.setSector(previous);}
        newsMessageBody();
        System.out.println("NewsPresentationRegressionTest: passed (pure/ambient/mission map policy, no truth leak, salvage lifetime/dedup, actual pulse ID matches vanilla)");
    }
    private static void newsMessageBody()throws Exception {
        SectorAPI previous=Global.getSector();SettingsAPI settings=Global.getSettings();
        try {
            java.lang.reflect.Constructor<PTSDCrisisState> ctor=PTSDCrisisState.class.getDeclaredConstructor();ctor.setAccessible(true);
            PTSDCrisisState state=ctor.newInstance();state.progressInitialized=true;
            Map<String,Object> data=new HashMap<>();data.put(PTSDCrisisState.PERSISTENT_KEY,state);
            FactionAPI faction=NewsSceneMechanicsTest.mock(FactionAPI.class,n->n.endsWith("Color")?Color.WHITE:null);
            Global.setSettings(NewsSceneMechanicsTest.mock(SettingsAPI.class,n->null));
            Global.setSector(NewsSceneMechanicsTest.mock(SectorAPI.class,n->n.equals("getPersistentData")?data:n.equals("getPlayerFaction")?faction:null));
            PTSDCrisisState.CrisisIncident incident=new PTSDCrisisState.CrisisIncident();
            incident.id="message-body";incident.cardId="N-01";incident.headline="新闻标题";incident.sourceLabel="公开来源";
            incident.publicText="据悉，记录完整度为100%。\n信息来源：公开港务记录。";
            incident.trueText="SECRET_TRUTH";incident.effectSummary="SECRET_BUDGET";state.incidents.add(incident);
            java.lang.reflect.Constructor<PTSDCrisisNewsIntel> create=PTSDCrisisNewsIntel.class.getDeclaredConstructor(String.class);create.setAccessible(true);
            PTSDCrisisNewsIntel intel=create.newInstance(incident.id);
            List<String> lines=new ArrayList<>();
            TooltipMakerAPI tooltip=(TooltipMakerAPI)Proxy.newProxyInstance(TooltipMakerAPI.class.getClassLoader(),new Class[]{TooltipMakerAPI.class},(p,m,a)->{
                if(m.getName().equals("addPara"))lines.add(a.length==4&&a[3] instanceof String[]?String.format((String)a[0],(Object[])a[3]):(String)a[0]);return null;
            });
            intel.createIntelInfo(tooltip,ListInfoMode.MESSAGES);
            check(lines.contains(incident.publicText),"Message omitted/truncated public report or damaged literal percentage");
            check(lines.stream().noneMatch(t->t.contains("SECRET")),"Message exposed backend data");
            check(!incident.readByPlayer&&!incident.recordedByPlayer,"Message accepted/read the investigation");
            lines.clear();intel.createIntelInfo(tooltip,ListInfoMode.INTEL);
            check(!lines.contains(incident.publicText)&&lines.contains(incident.headline),"Intel list expanded into a full article");
            incident.publicText=null;lines.clear();intel.createIntelInfo(tooltip,ListInfoMode.MESSAGES);
            check(lines.size()==2,"Empty report generated a blank paragraph");
        }finally{Global.setSettings(settings);Global.setSector(previous);}
    }
}
