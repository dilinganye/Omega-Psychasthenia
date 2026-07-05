package data.scripts.world.systems;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseAssignmentAI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class IIRT_AssignmentAI extends BaseAssignmentAI {

	protected SectorEntityToken toGuard;
	protected IntervalUtil moteSpawn = new IntervalUtil(0.01f, 0.1f);

	public IIRT_AssignmentAI(CampaignFleetAPI fleet, SectorEntityToken toGuard) {
		super();
		this.fleet = fleet;
		this.toGuard = toGuard;

		giveInitialAssignments();
	}

	@Override
	protected void giveInitialAssignments() {
		pickNext();
	}

	@Override
	protected void pickNext() {
		fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, toGuard, 100f);
	}

	@Override
	public void advance(float amount) {
		super.advance(amount);
		float days = Misc.getDays(amount);
		moteSpawn.advance(days * 5f);
		if (moteSpawn.intervalElapsed()) {
			spawnMote(fleet);
		}
	}

	public static void spawnMote(SectorEntityToken from) {
		if (!from.isInCurrentLocation()) return;

		float dur = 1f + 2f * (float)Math.random();
		dur *= 1f;
		float size = 3f + (float)Math.random() * 5f;
		size *= 2f;
		Color color = new Color(132, 0, 255, 255);

		Vector2f loc = Misc.getPointWithinRadius(from.getLocation(), from.getRadius());
		Vector2f vel = Misc.getUnitVectorAtDegreeAngle((float)Math.random() * 360f);
		vel.scale(5f + (float)Math.random() * 10f);
		Vector2f.add(vel, from.getVelocity(), vel);
		Misc.addHitGlow(from.getContainingLocation(), loc, vel, size, dur, color);
	}

}
