package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

/** Hard exclusion, including modder-supplied targets. Gamma is never an offensive objective. */
public final class PTSDProtectedFaction {
    private PTSDProtectedFaction(){}
    public static boolean isGamma(String faction){return IIRT_Omega_Invasion.GAMMA_LEGION_FACTION.equals(faction);}
    public static boolean system(StarSystemAPI system){
        if(system==null)return false;
        if(Global.getSector()!=null && Global.getSector().getEconomy()!=null) {
            java.util.List<MarketAPI> markets=Global.getSector().getEconomy().getMarkets(system);
            if(markets!=null)for(MarketAPI m:markets)
                if(m!=null && !m.isPlanetConditionMarketOnly() && isGamma(m.getFactionId()))return true;
        }
        // Include hidden/non-economy colonies, bounded to the selected system.
        if(system.getAllEntities()!=null)for(SectorEntityToken e:system.getAllEntities())
            if(e.getMarket()!=null && !e.getMarket().isPlanetConditionMarketOnly() && isGamma(e.getMarket().getFactionId()))return true;
        return false;
    }
}
