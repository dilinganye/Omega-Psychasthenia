package data.scripts.campaign.invasion;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import org.lwjgl.util.vector.Vector2f;
/** A sensor contact, not a fleet. Only its active local instance moves each frame. */
public final class PTSDSceneGhostMotion implements EveryFrameScript {
    private final String incidentId;private final SectorEntityToken ghost;private float seconds,pulse;
    public PTSDSceneGhostMotion(String id,SectorEntityToken ghost){this.incidentId=id;this.ghost=ghost;}
    public boolean isDone(){return ghost==null||ghost.getContainingLocation()==null||ghost.isExpired();}
    public boolean runWhilePaused(){return false;}
    public void advance(float amount){
        if(isDone())return;CampaignFleetAPI player=Global.getSector().getPlayerFleet();
        if(player==null||player.getContainingLocation()!=ghost.getContainingLocation()){PTSDNewsScene.remove(ghost.getId());return;}
        seconds+=amount;pulse+=amount;
        float angle=seconds*.6f;Vector2f goal=new Vector2f(player.getLocation().x+(float)Math.cos(angle)*600,player.getLocation().y+(float)Math.sin(angle)*600);
        Vector2f delta=Vector2f.sub(goal,ghost.getLocation(),null);float distance=delta.length();if(distance>1){delta.scale(Math.min(distance,amount*550)/distance);Vector2f.add(ghost.getLocation(),delta,ghost.getLocation());}
        if(pulse>=9&&distance<1300){pulse=0;PTSDNewsScene.pulse(ghost,false);}
    }
}
