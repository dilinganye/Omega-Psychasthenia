package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.opengl.GL11;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.ui.LazyFont;
import java.awt.Color;
import java.util.*;

/** Three independent accounts: alliance allocation, strategic Path tasks, and battle-only Path waves. */
public final class PTSDCombatRelief extends BaseEveryFrameCombatPlugin {
    private static final String BATTLE="$PTSD_relief_encounter", FREE="PTSD_relief_zero_dp";
    private CombatEngineAPI engine;private PTSDReliefState.BattleRecord record;private PTSDCrisisState state;
    private final Map<String,ShipAPI> ships=new LinkedHashMap<String,ShipAPI>();
    private LazyFont.DrawableString text;private int scroll;private boolean stopped;
    static int cfg(String key){return PTSDReliefDirector.cfg(key);}
    @Override public void init(CombatEngineAPI e){
        engine=e;if(e==null||!e.isInCampaign()||e.isInCampaignSim()||e.getContext()==null||Global.getSector()==null)return;
        state=PTSDCrisisState.get();if(state==null||state.phase!=PTSDCrisisState.Phase.WAR)return;
        CampaignFleetAPI player=e.getContext().getPlayerFleet(),other=e.getContext().getOtherFleet();if(player==null||other==null)return;
        BattleAPI battle=player.getBattle();List<CampaignFleetAPI> enemies=battle==null?Collections.singletonList(other):battle.getNonPlayerSideSnapshot();
        if(enemies==null||enemies.isEmpty())enemies=Collections.singletonList(other);
        float fp=0;boolean crisis=false;for(CampaignFleetAPI f:enemies){fp+=f.getFleetPoints();if(PTSDJeOtloesManager.isOmegaFleet(f))crisis=true;}if(!crisis)return;
        PTSDReliefState r=PTSDReliefState.get(state);String id=null;
        for(CampaignFleetAPI f:enemies){String old=f.getMemoryWithoutUpdate().getString(BATTLE);if(old!=null&&r.battles.containsKey(old)){id=old;break;}}
        if(id==null){id="battle-relief-"+Misc.genUID();record=new PTSDReliefState.BattleRecord();record.id=id;
            record.system=player.getStarSystem()==null?null:player.getStarSystem().getId();record.x=player.getLocation().x;record.y=player.getLocation().y;
            float min=cfg("path_ratio_min")/100f,max=Math.max(min,cfg("path_ratio_max")/100f);
            record.pathBudget=budget(fp,MathUtils.getRandomNumberInRange(min,max),cfg("path_total_min"),cfg("path_total_max"));
            record.nextPath=cfg("path_first_seconds");r.battles.put(id,record);
        }else record=r.battles.get(id);
        for(CampaignFleetAPI f:enemies){f.getMemoryWithoutUpdate().set(BATTLE,id);record.enemyIds.add(f.getId());}
        if(record.settled)stopped=true;
    }
    static float budget(float fp,float ratio,float min,float max){return Math.max(min,Math.min(Math.max(min,max),fp*ratio));}
    public static void zeroDeployment(FleetMemberAPI m){m.getStats().getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(FREE,0);m.setAlly(true);}
    static void restoreDeployment(FleetMemberAPI m){m.getStats().getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).unmodify(FREE);m.setAlly(false);}
    private void request(){
        if(record.requested||record.settled)return;record.requested=true;record.requestTime=record.elapsed;
        CampaignFleetAPI player=Global.getSector().getPlayerFleet();Vector2f here=player.getLocationInHyperspace();Random rng=new Random(record.id.hashCode());
        for(MarketAPI market:PTSDReliefDirector.sources(state,here,Float.MAX_VALUE/4000f)){
            boolean near=Misc.getDistance(here,market.getStarSystem().getLocation())<=cfg("relief_range")*2000f;
            if(!near&&rng.nextFloat()*100>=cfg("relief_far_chance"))continue;
            float capacity=PTSDReliefDirector.available(state,market);if(capacity<3)continue;
            CampaignFleetAPI roster=PTSDReliefDirector.generate(market.getFactionId(),market,capacity);if(roster==null)continue;
            record.pending.put(market.getId(),new ArrayList<FleetMemberAPI>(roster.getFleetData().getMembersListCopy()));
            record.sourceFactions.put(market.getId(),market.getFactionId());
            record.arrivalSeconds.put(market.getId(),record.elapsed+(near?cfg("relief_near_seconds"):cfg("relief_far_seconds")));
            PTSDReliefDirector.charge(state,market.getId(),roster.getFleetPoints());
        }
    }
    @Override public void advance(float amount,List<InputEventAPI> events){
        if(record==null)return;
        if(engine.getCombatUI()!=null&&engine.getCombatUI().isShowingCommandUI())for(InputEventAPI event:events){
            if(event.isConsumed())continue;float x=panelX(),y=panelY();
            if(event.isLMBDownEvent()&&event.getX()>=x+12&&event.getX()<=x+348&&event.getY()>=y+180&&event.getY()<=y+212){request();event.consume();}
            if(event.isMouseScrollEvent()&&event.getX()>=x&&event.getX()<=x+360&&event.getY()>=y&&event.getY()<=y+260){scroll=Math.max(0,Math.min(Math.max(0,record.pending.size()-3),scroll-event.getEventValue()));event.consume();}
        }
        sample();if(stopped||record.settled||engine.isPaused())return;
        if(engine.isCombatOver()){stopped=true;return;}
        record.elapsed+=Math.max(0,amount);
        if(!record.pathExhausted&&record.pathSpent<record.pathBudget&&record.elapsed>=record.nextPath){
            record.nextPath=record.elapsed+Math.max(1,cfg("path_wave_seconds"));float limit=Math.min(cfg("path_wave_fp"),record.pathBudget-record.pathSpent);
            CampaignFleetAPI generated=PTSDReliefDirector.generate(PTSDReliefDirector.PATH,null,limit);
            if(generated==null)record.pathExhausted=true;
            else for(FleetMemberAPI member:generated.getFleetData().getMembersListCopy())if(deploy("PATH",member)){record.pathSpent+=member.getFleetPointCost();}
        }
        for(String source:new ArrayList<String>(record.pending.keySet()))if(record.elapsed>=record.arrivalSeconds.get(source)){
            List<FleetMemberAPI> pending=record.pending.get(source);float batch=0;
            for(FleetMemberAPI member:new ArrayList<FleetMemberAPI>(pending)){
                if(batch>0&&batch+member.getFleetPointCost()>60)continue;
                if(deploy(source,member)){pending.remove(member);batch+=member.getFleetPointCost();}if(batch>=60)break;
            }
            record.arrivalSeconds.put(source,record.elapsed+Math.max(1,cfg("relief_wave_seconds")));
        }
    }
    private boolean deploy(String source,FleetMemberAPI member){
        zeroDeployment(member);CombatFleetManagerAPI manager=engine.getFleetManager(0);boolean suppressed=manager.isSuppressDeploymentMessages();manager.setSuppressDeploymentMessages(true);
        ShipAPI ship;
        try{float x=(float)(Math.random()-.5)*engine.getMapWidth()*.65f;
            ship=manager.spawnFleetMember(member,new Vector2f(x,-engine.getMapHeight()/2-350),90,2);
        }finally{manager.setSuppressDeploymentMessages(suppressed);}
        if(ship==null){restoreDeployment(member);return false;}
        ship.setAlly(true);ships.put(member.getId(),ship);
        List<FleetMemberAPI> list=record.arrived.get(source);if(list==null){list=new ArrayList<FleetMemberAPI>();record.arrived.put(source,list);}list.add(member);
        engine.getCombatUI().addMessage(0,ship,PTSDCommandSurface.INK,source.equals("PATH")?"誓师频道：不要退缩。我们将同你们一同燃烧。":"联盟援军已进入战场。");
        return true;
    }
    private void sample(){for(Map.Entry<String,ShipAPI> entry:ships.entrySet()){
        ShipAPI ship=entry.getValue();if(ship.isHulk()||ship.getHitpoints()<=0){record.dead.add(entry.getKey());continue;}
        FleetMemberAPI member=ship.getFleetMember();if(member!=null){member.getStatus().setHullFraction(Math.max(0,ship.getHitpoints()/Math.max(1,ship.getMaxHitpoints())));member.getRepairTracker().setCR(ship.getCurrentCR());}
    }}
    public static void settleCampaign(PTSDCrisisState state){
        if(state==null||state.late==null||state.late.relief==null)return;
        // Multiple engagements may belong to one campaign battle. Settle only after all rounds end.
        CampaignFleetAPI player=Global.getSector().getPlayerFleet();if(player!=null&&player.getBattle()!=null)return;
        CombatEngineAPI engine=Global.getCombatEngine();if(engine!=null&&engine.isInCampaign()&&!engine.isCombatOver())return;
        for(PTSDReliefState.BattleRecord record:state.late.relief.battles.values())if(!record.settled){
            record.settled=true;
            for(String enemyId:record.enemyIds){SectorEntityToken enemy=enemyId==null?null:Global.getSector().getEntityById(enemyId);if(enemy!=null&&record.id.equals(enemy.getMemoryWithoutUpdate().getString(BATTLE)))enemy.getMemoryWithoutUpdate().unset(BATTLE);}
            for(Map.Entry<String,List<FleetMemberAPI>> pending:record.pending.entrySet()){float fp=0;for(FleetMemberAPI m:pending.getValue()){restoreDeployment(m);fp+=m.getFleetPointCost();}PTSDReliefDirector.charge(state,pending.getKey(),-fp);pending.getValue().clear();}
            for(Map.Entry<String,List<FleetMemberAPI>> batch:record.arrived.entrySet()){
                MarketAPI source=state.resolveMarket(batch.getKey());boolean path="PATH".equals(batch.getKey());
                String faction=path?PTSDReliefDirector.PATH:record.sourceFactions.get(batch.getKey());if(faction==null)faction="independent";
                CampaignFleetAPI survivors=Global.getFactory().createEmptyFleet(faction,path?"誓师舰队幸存者":"联盟返航分队",true);
                for(FleetMemberAPI m:batch.getValue()){restoreDeployment(m);if(!record.dead.contains(m.getId())&&m.getStatus().getHullFraction()>0)survivors.getFleetData().addFleetMember(m);}
                if(survivors.isEmpty())continue;
                PTSDReliefState.Unit u=new PTSDReliefState.Unit();u.id=record.id+":"+batch.getKey();u.kind=path?"PATH_SURVIVOR":"ALLIANCE_RETURN";u.faction=faction;u.fleet=survivors;
                u.sourceMarket=path?null:batch.getKey();u.sourceSystem=source==null||source.getStarSystem()==null?record.system:source.getStarSystem().getId();u.targetSystem=record.system;u.borrowed=!path;u.physical=true;
                u.stage=PTSDReliefState.Stage.ACTIVE;u.stageDay=u.lastDay=PTSDCrisisState.getDay();u.status="战斗幸存者，正在重整";
                StarSystemAPI system=state.resolveSystem(record.system);LocationAPI location=system==null?Global.getSector().getHyperspace():system;
                location.addEntity(survivors);survivors.setLocation(record.x,record.y);survivors.setTransponderOn(true);survivors.getMemoryWithoutUpdate().set(PTSDReliefDirector.UNIT,u.id);
                PTSDCampaignRuntime.entityAdded(survivors);Vector2f p=survivors.getLocationInHyperspace();u.x=p.x;u.y=p.y;state.late.relief.units.add(u);
                if(path&&system!=null)survivors.addAssignment(FleetAssignment.PATROL_SYSTEM,system.getCenter(),Math.max(1,cfg("path_survivor_days")),"护卫尚未熄灭的灯火");
                else PTSDReliefDirector.returnHome(state,u,PTSDCrisisState.getDay());
            }
            record.arrived.clear();record.dead.clear();record.enemyIds.clear();
        }
        int completed=0;List<String> ids=new ArrayList<String>(state.late.relief.battles.keySet());Collections.reverse(ids);
        for(String id:ids)if(state.late.relief.battles.get(id).settled&&++completed>20)state.late.relief.battles.remove(id);
    }
    private float panelX(){return 24;}
    private float panelY(){return Math.max(30,Global.getSettings().getScreenHeight()-360);}
    @Override public void renderInUICoords(ViewportAPI viewport){
        if(record==null||engine.getCombatUI()==null||!engine.getCombatUI().isShowingCommandUI())return;
        if(text==null)try{text=LazyFont.loadFont("graphics/fonts/insignia21LTaa.fnt").createText();}catch(Exception ex){return;}
        float x=panelX(),y=panelY();GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
        rect(x,y,360,260,new Color(8,18,24,235));rect(x,y+257,360,3,PTSDCommandSurface.INK);
        rect(x+12,y+180,336,32,new Color(28,65,72,245));
        float ratio=record.pathBudget<=0?0:Math.min(1,record.pathSpent/record.pathBudget);rect(x+12,y+132,336,4,new Color(54,49,35));rect(x+12,y+132,336*ratio,4,PTSDCommandSurface.AMBER);
        GL11.glPopAttrib();
        line("紧急联络 / 联盟与誓师频道",x+12,y+244,Color.WHITE);
        line(record.requested?"求援已发送 / 来源按实际可用兵力答复":"发送求救信号",x+20,y+204,PTSDCommandSurface.INK);
        line("誓师援军 "+Math.round(record.pathSpent)+" / "+Math.round(record.pathBudget)+" FP",x+12,y+163,PTSDCommandSurface.AMBER);
        line(record.pathExhausted?"余量不足以组成下一批舰船":record.pathSpent>=record.pathBudget?"本次誓师编队已全部投入":"下一轮约 "+Math.max(0,Math.round(record.nextPath-record.elapsed))+" 秒",x+12,y+123,PTSDCommandSurface.AMBER);
        int n=0,row=0;for(String source:record.pending.keySet()){if(n++<scroll)continue;if(row>=3)break;
            MarketAPI market=state.resolveMarket(source);List<FleetMemberAPI> pending=record.pending.get(source);
            String label=(market==null?source:market.getName())+" / "+(pending.isEmpty()?"全部进场":pending.size()+"艘，约"+Math.max(0,Math.round(record.arrivalSeconds.get(source)-record.elapsed))+"秒");
            line(label,x+12,y+91-row++*24,PTSDCommandSurface.INK);
        }
        if(record.requested&&record.pending.isEmpty())line("当前没有可派出的联盟舰队。",x+12,y+87,Color.GRAY);
        if(stopped)line("交战已结束，停止后续投入",x+12,y+27,Color.GRAY);
    }
    private void line(String value,float x,float y,Color color){text.setText(value);text.setBaseColor(color);text.setMaxWidth(330);text.draw(x,y);}
    private static void rect(float x,float y,float w,float h,Color c){GL11.glColor4f(c.getRed()/255f,c.getGreen()/255f,c.getBlue()/255f,c.getAlpha()/255f);GL11.glBegin(GL11.GL_QUADS);GL11.glVertex2f(x,y);GL11.glVertex2f(x+w,y);GL11.glVertex2f(x+w,y+h);GL11.glVertex2f(x,y+h);GL11.glEnd();}
}
