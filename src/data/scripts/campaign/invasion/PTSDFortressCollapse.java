package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import data.scripts.IIRT_Omega_ModPlugin;
import data.scripts.PTSDModPluginAltLuna;
import org.lwjgl.util.vector.Vector2f;
import java.util.*;

/** Damage follows authored wave geometry. No hull damage, ship deletion or automatic retreat. */
public final class PTSDFortressCollapse {
    private PTSDFortressCollapse(){}
    public static void initialize(PTSDLateState.Detonation d){
        if(d.collapseInitialized)return;d.collapseInitialized=true;d.collapseElapsed=0;
        d.remainingSeconds=PTSDFortressCollapseVisual.DURATION;
        d.collapseChance=value("hit_chance",33)/100f;d.collapseLossMin=value("cr_min",5)/100f;
        d.collapseLossMax=Math.max(d.collapseLossMin,value("cr_max",10)/100f);
        d.collapseFloor=value("cr_floor",10)/100f;d.collapseLowCR=value("dmod_threshold",30)/100f;
        d.collapseDModChance=value("dmod_chance",15)/100f;
        if(d.collapseHits==null)d.collapseHits=new HashMap<String,Integer>();
        if(d.collapseDMods==null)d.collapseDMods=new HashSet<String>();
        if(d.pendingCollapseDMods==null)d.pendingCollapseDMods=new HashSet<String>();
    }
    private static int value(String suffix,int fallback){
        if(!IIRT_Omega_ModPlugin.hasLunaLib)return fallback;
        return Math.max(0,Math.min(100,PTSDModPluginAltLuna.getLunaInt(IIRT_Omega_ModPlugin.getModId(),"PTSD_collapse_"+suffix,fallback)));
    }
    public static float advance(PTSDLateState.Detonation d,float seconds){
        initialize(d);float previous=d.collapseElapsed;
        d.collapseElapsed=Math.min(PTSDFortressCollapseVisual.DURATION,previous+Math.max(0,seconds));
        d.remainingSeconds=Math.max(0,PTSDFortressCollapseVisual.DURATION-d.collapseElapsed);return previous;
    }
    public static float reducedCR(float before,float loss,float floor){return Math.min(before,Math.max(floor,before-Math.max(0,loss)));}
    static boolean claim(PTSDLateState.Detonation d,String member,int wave){
        int old=d.collapseHits.containsKey(member)?d.collapseHits.get(member):0,bit=1<<wave;
        if((old&bit)!=0)return false;d.collapseHits.put(member,old|bit);return true;
    }
    private static Random random(PTSDLateState.Detonation d,String member,int wave){return new Random((d.fortressId+":"+member+":"+wave).hashCode());}
    private static void markDmod(PTSDLateState.Detonation d,String member,float before,Random r){
        if(before<d.collapseLowCR&&!d.collapseDMods.contains(member)&&r.nextFloat()<d.collapseDModChance){
            d.collapseDMods.add(member);d.pendingCollapseDMods.add(member);
        }
    }
    public static void campaign(PTSDLateState.Detonation d,CampaignFleetAPI player,Vector2f center,float previous){
        if(player==null||player.getStarSystem()==null||!d.systemId.equals(player.getStarSystem().getId()))return;
        int affected=0;
        for(int wave=0;wave<PTSDFortressCollapseVisual.frontCount();wave++){
            if(!PTSDFortressCollapseVisual.crosses(wave,player.getLocation().x-center.x,player.getLocation().y-center.y,previous,d.collapseElapsed))continue;
            for(FleetMemberAPI member:player.getFleetData().getMembersListCopy()){
                if(member.isFighterWing()||!claim(d,member.getId(),wave))continue;
                Random r=random(d,member.getId(),wave);if(r.nextFloat()>=d.collapseChance)continue;
                float before=member.getRepairTracker().getCR();
                member.getRepairTracker().setCR(reducedCR(before,d.collapseLossMin+r.nextFloat()*(d.collapseLossMax-d.collapseLossMin),d.collapseFloor));
                markDmod(d,member.getId(),before,r);affected++;
            }
        }
        applyPending(d,player);
        if(affected>0)Global.getSector().getCampaignUI().addMessage("破裂波面扫过舰队："+affected+"艘舰船报告战备受损。",new java.awt.Color(220,130,220));
    }
    public static void combat(PTSDLateState.Detonation d,CombatEngineAPI engine,Vector2f center,float previous){
        CampaignFleetAPI player=Global.getSector().getPlayerFleet();if(player==null)return;
        Set<String> own=new HashSet<String>();for(FleetMemberAPI m:player.getFleetData().getMembersListCopy())own.add(m.getId());
        for(ShipAPI ship:engine.getShips()){
            if(ship==null||ship.isHulk()||ship.isFighter()||ship.getFleetMember()==null||!own.contains(ship.getFleetMember().getId()))continue;
            String id=ship.getFleetMember().getId();
            for(int wave=0;wave<PTSDFortressCollapseVisual.frontCount();wave++){
                if(!PTSDFortressCollapseVisual.crosses(wave,ship.getLocation().x-center.x,ship.getLocation().y-center.y,previous,d.collapseElapsed)||!claim(d,id,wave))continue;
                Random r=random(d,id,wave);if(r.nextFloat()>=d.collapseChance)continue;
                float before=ship.getCurrentCR();ship.setCurrentCR(reducedCR(before,d.collapseLossMin+r.nextFloat()*(d.collapseLossMax-d.collapseLossMin),d.collapseFloor));
                markDmod(d,id,before,r);
            }
        }
    }
    public static void applyPending(PTSDLateState.Detonation d,CampaignFleetAPI player){
        if(player==null||d.pendingCollapseDMods==null||d.pendingCollapseDMods.isEmpty())return;
        for(FleetMemberAPI member:player.getFleetData().getMembersListCopy())if(d.pendingCollapseDMods.contains(member.getId())){
            ShipVariantAPI copy=member.getVariant().clone();
            DModManager.addDMods(copy,false,1,random(d,member.getId(),99));member.setVariant(copy,false,true);
        }
        d.pendingCollapseDMods.clear();
    }
}
