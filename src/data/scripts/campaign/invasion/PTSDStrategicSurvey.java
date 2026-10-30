package data.scripts.campaign.invasion;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import java.util.*;
/** Phase-bound strategic estimates and a separate human-owned public baseline. */
public final class PTSDStrategicSurvey {
    private PTSDStrategicSurvey(){}
    public static boolean populated(StarSystemAPI sys){
        for(MarketAPI m:Global.getSector().getEconomy().getMarkets(sys))if(!m.isPlanetConditionMarketOnly()&&
                !IIRT_Omega_Invasion.WATCHER_FACTION.equals(m.getFactionId())&&!IIRT_Omega_Invasion.PSYCHASTHENIA_FACTION.equals(m.getFactionId()))return true;
        return false;
    }
    public static float estimateMultiplier(long seed){return .4f+new Random(seed).nextFloat()*1.4f;}
    public static boolean postRecon(PTSDCrisisState s){return s.phase.ordinal()>=PTSDCrisisState.Phase.EXPANSION.ordinal();}
    private static boolean eligible(StarSystemAPI sys){return !sys.hasTag(Tags.THEME_HIDDEN)&&!sys.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);}
    public static void advance(PTSDCrisisState s,float day){
        if(s==null||Global.getSector()==null)return;
        if(postRecon(s)&&!s.uninhabitedSurveyCompleted){
            for(StarSystemAPI sys:Global.getSector().getStarSystems())if(eligible(sys)&&!populated(sys))survey(s,sys,day,false);
            s.uninhabitedSurveyCompleted=true;
        }
        if(s.phase==PTSDCrisisState.Phase.WAR)prepareWarOpening(s,day);
        boolean unlocked=!Global.getSector().getIntelManager().getIntel(PTSDWarIntel.class).isEmpty()||
                !Global.getSector().getIntelManager().getIntel(PTSDGeneralStaffIntel.class).isEmpty();
        if(!s.humanBaselineInitialized||(unlocked&&day>=s.nextHumanBaselineDay)){
            for(StarSystemAPI sys:Global.getSector().getStarSystems())if(eligible(sys)&&populated(sys)){
                float fp=0;for(CampaignFleetAPI f:sys.getFleets())if(!PTSDJeOtloesManager.isOmegaFleet(f))fp+=f.getFleetPoints();
                PTSDCrisisState.HumanReport report=new PTSDCrisisState.HumanReport();report.id="human-baseline:"+sys.getId();report.systemId=sys.getId();
                report.day=day;report.source="航路与驻防汇总";report.strength=fp;report.omega=false;report.text="已登记殖民地与驻防；不包含隐藏敌情";
                s.humanWarReports.put(report.id,report);
            }
            s.humanBaselineInitialized=true;s.nextHumanBaselineDay=day+15;
        }
    }
    public static void prepareWarOpening(PTSDCrisisState s,float day) {
        if(s.warOpeningSurveyCompleted)return;
        for(StarSystemAPI sys:Global.getSector().getStarSystems())
            if(eligible(sys)&&populated(sys)&&!PTSDReconReportDirector.isSurveyed(s.getSystemData(sys.getId()),day))survey(s,sys,day,true);
        s.warOpeningSurveyCompleted=true;
    }
    private static void survey(PTSDCrisisState s,StarSystemAPI sys,float day,boolean coarse){
        float fp=0,defense=0,value=0;boolean colony=populated(sys);
        for(CampaignFleetAPI f:sys.getFleets())if(!PTSDJeOtloesManager.isOmegaFleet(f))fp+=f.getFleetPoints();
        for(MarketAPI m:Global.getSector().getEconomy().getMarkets(sys))if(!m.isPlanetConditionMarketOnly()){
            defense+=m.getSize()*m.getSize()*9f+Math.max(0,m.getStabilityValue())*3f;value+=m.getSize()*10f;}
        float mult=coarse?estimateMultiplier((sys.getId()+":"+s.phaseStartedDay).hashCode()):1f;
        PTSDCrisisState.EvidenceRecord e=PTSDCrisisAPI.recordEvidence(sys.getId(),coarse?"WAR_OPENING_ESTIMATE":"UNINHABITED_BASELINE",sys.getId(),fp*mult,defense*mult,value,
                colony?1:0,fp>0?1:0,coarse?.35f:.95f,coarse?90:100000,false,0);
        if(e!=null){e.remoteEstimate=coarse;e.sourceGroup="phase-survey:"+sys.getId();PTSDCrisisAPI.refreshBelief(s.getSystemData(sys.getId()),day);}
    }
}
