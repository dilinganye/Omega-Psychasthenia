package data.scripts.campaign.invasion;
import org.lwjgl.util.vector.Vector2f;
import data.scripts.IIRT_Omega_ModPlugin;
import java.util.*;

public class WarCampaignPolicyTest {
    static void check(boolean v,String message){if(!v)throw new AssertionError(message);}
    public static void main(String[] args) {
        List<Vector2f> sites=Arrays.asList(new Vector2f(-20000,-20000),new Vector2f(-20000,20000),
                new Vector2f(20000,20000),new Vector2f(20000,-20000),new Vector2f(0,0),new Vector2f(0,18500));
        List<Vector2f> hull=PTSDWarCampaignDirector.hull(sites);
        check(hull.size()==4,"Interior points entered convex boundary");
        check(!PTSDWarCampaignDirector.onEdge(new Vector2f(0,0),hull,3000),"Core classified as outer layer");
        check(PTSDWarCampaignDirector.onEdge(new Vector2f(0,18500),hull,3000),"Near-edge colony omitted");
        List<Vector2f> shifted=new ArrayList<Vector2f>();for(Vector2f p:sites)shifted.add(new Vector2f(p.x+80000,p.y-30000));
        check(!PTSDWarCampaignDirector.onEdge(new Vector2f(80000,-30000),PTSDWarCampaignDirector.hull(shifted),3000),"Boundary depends on sector origin");
        check(PTSDWarCampaignDirector.hull(Arrays.asList(new Vector2f(0,0),new Vector2f(0,0),new Vector2f(1,0))).size()==2,"Duplicate/collinear hull broken");
        check(PTSDWarCampaignDirector.refund(100,true,false)==0,"Opening must not refund assets");
        check(PTSDWarCampaignDirector.refund(40,false,false)==20,"Paid follow-up survivor refund changed");
        check(PTSDWarCampaignDirector.refund(100,true,true)==0,"Free singularity force creates assets");
        check(PTSDWarCampaignDirector.points(160,true)==0 && PTSDWarCampaignDirector.points(160,false)==20,"Opening/free versus follow-up/full cost");
        float old=IIRT_Omega_ModPlugin.warSingularityMultiplier;IIRT_Omega_ModPlugin.warSingularityMultiplier=3;
        check(!PTSDWarCampaignDirector.singularity(300,100),"Exact multiplier threshold triggers singularity");
        check(PTSDWarCampaignDirector.singularity(100000,100),"Exceptional defense not recognized");
        IIRT_Omega_ModPlugin.warSingularityMultiplier=old;
        PTSDCrisisState.StrategicEvent e=new PTSDCrisisState.StrategicEvent();e.musterUntilDay=10;e.operationArrivalDay=20;
        e.operationOriginX=0;e.operationTargetX=10000;
        check(PTSDWarCampaignDirector.position(e,5).x==0,"Before departure drift");
        check(PTSDWarCampaignDirector.position(e,15).x==5000,"Transit interpolation wrong");
        check(PTSDWarCampaignDirector.position(e,30).x==10000,"Overshoots destination");
        e.operationReturnScheduled=true;e.resolveDay=25;e.operationReturnDay=35;
        check(PTSDWarCampaignDirector.position(e,30).x==5000,"Return interpolation wrong");
        check(PTSDWarCampaignDirector.position(e,40).x==0,"Return misses origin");
        System.out.println("WarCampaignPolicyTest: passed (outer layer, shifted sector, duplicate points, costs, survivor refunds, singularity, travel)");
    }
}
