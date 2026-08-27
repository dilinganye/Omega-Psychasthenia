package data.scripts.everyframe;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.IIRT_Omega_ModPlugin;
import data.scripts.campaign.invasion.PTSDReconPlayerEvents;
import exerelin.utilities.NexConfig;
import exerelin.utilities.NexFactionConfig;
import exerelin.utilities.NexFactionConfig.StartFleetSet;
import exerelin.utilities.NexFactionConfig.StartFleetType;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class IIRT_Omega_PluginStarter extends BaseEveryFrameCombatPlugin {

    private static boolean addedOnce = false;
    private static boolean checkedOncePerCombat = false;
    private CampaignFleetAPI reactiveRetreatFleet;
    private boolean retreatListenersInstalled;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        installReactiveRetreatListeners();
        if (IIRT_Omega_ModPlugin.NEX()) {
            if (checkedOncePerCombat) {
                if (!addedOnce && Global.getCurrentState() == GameState.TITLE &&
                        Global.getSettings().getMissionScore("Omega_PersonalTailor") > 75) {
                    NexFactionConfig faction = NexConfig.getFactionConfig("KRM");
                    StartFleetSet fleetSet = faction.getStartFleetSet(StartFleetType.SUPER.name());
                    List<String> excelsiorFleet = new ArrayList<String>();
                    excelsiorFleet.add("IIRT_Omega_Cipher_Beam");
                    fleetSet.addFleet(excelsiorFleet);
                    addedOnce = true;
                }
                checkedOncePerCombat = false;
            }
        }
    }

    @Override
    public void init(CombatEngineAPI engine) {
        checkedOncePerCombat = true;
        retreatListenersInstalled = false;
        reactiveRetreatFleet = null;
        if (engine != null && engine.isInCampaign() && engine.getContext() != null) {
            CampaignFleetAPI other = engine.getContext().getOtherFleet();
            if (other != null && other.getMemoryWithoutUpdate().getBoolean(PTSDReconPlayerEvents.REACTIVE_RETREAT)) {
                reactiveRetreatFleet = other;
                engine.getContext().aiRetreatAllowed = true;
                engine.getContext().fightToTheLast = false;
            }
        }
    }

    private void installReactiveRetreatListeners() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (retreatListenersInstalled || reactiveRetreatFleet == null || engine == null) return;
        boolean found = false;
        for (ShipAPI ship : engine.getShips()) {
            if (ship == null || ship.isFighter() || ship.getOwner() == 0 ||
                    ship.hasListenerOfClass(ReactiveRetreatOnDamage.class)) continue;
            ship.addListener(new ReactiveRetreatOnDamage(engine, reactiveRetreatFleet, ship.getOwner()));
            found = true;
        }
        if (found) retreatListenersInstalled = true;
    }

    private static final class ReactiveRetreatOnDamage implements DamageTakenModifier {
        private final CombatEngineAPI engine;
        private final CampaignFleetAPI campaignFleet;
        private final int owner;
        private boolean triggered;

        private ReactiveRetreatOnDamage(CombatEngineAPI engine, CampaignFleetAPI campaignFleet, int owner) {
            this.engine = engine;
            this.campaignFleet = campaignFleet;
            this.owner = owner;
        }

        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage,
                                        Vector2f point, boolean shieldHit) {
            if (!triggered && damage != null && damage.getDamage() > 0f) {
                triggered = true;
                campaignFleet.getMemoryWithoutUpdate().set(PTSDReconPlayerEvents.RETREAT_TRIGGERED, true);
                engine.getFleetManager(owner).getTaskManager(false).orderFullRetreat();
            }
            return null;
        }
    }
}