package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.*;

/** Operations are cards with a phase strip and a real navigation target, not repeated report text. */
public final class PTSDReliefPanel {
    public static String stage(PTSDReliefState.Unit u){switch(u.stage){case PREPARING:return "原港整备";case MUSTERING:return "前往集结";case ASSEMBLED:return "等待编队";case TRAVELLING:return "航行中";case ACTIVE:return "现场行动";case GARRISON:return "临时驻防";case RETURNING:return "返港";default:return "已结束";}}
    public static boolean known(PTSDReliefState.Unit u){return !u.kind.startsWith("PATH")||u.physical||u.stage==PTSDReliefState.Stage.ACTIVE||u.stage==PTSDReliefState.Stage.GARRISON;}
    public static void populate(CustomPanelAPI parent,float width,float height,PTSDCrisisState s){
        float mapWidth=width*.48f, listWidth=width-mapWidth-14;
        PTSDFrontMapPanel mapPlugin=new PTSDFrontMapPanel();CustomPanelAPI map=parent.createCustomPanel(mapWidth,height,mapPlugin);
        TooltipMakerAPI mapText=map.createUIElement(mapWidth-20,62,false);mapText.addSectionHeading("联合行动航图",Alignment.MID,0);
        mapPlugin.setHoverLabel(mapText.addPara("悬停航线查看来源和任务；右键星系操作。",5));map.addUIElement(mapText).inTL(10,10);
        TooltipMakerAPI selection=map.createUIElement(mapWidth-20,80,false);mapPlugin.setParent(map,selection.addPara("点击星系查看战况。",0));map.addUIElement(selection).inBL(10,8);parent.addComponent(map).inTL(0,0);
        TooltipMakerAPI list=parent.createUIElement(listWidth,height,true);list.setParaFont("graphics/fonts/insignia21LTaa.fnt");
        list.addSectionHeading("行动调度 / 战地回执",Alignment.MID,0);
        int active=0;for(PTSDReliefState.Unit u:PTSDReliefState.get(s).units)if(u.stage!=PTSDReliefState.Stage.DONE&&known(u))active++;
        list.addPara("可查看的进行中行动：%s",6,Misc.getHighlightColor(),String.valueOf(active));
        list.addPara("援军抵达才形成防御。联盟调拨、誓师远征和战斗幸存者分别记账。",6);
        PTSDJeWarExpedition.Mission je=PTSDReliefState.get(s).jeExpedition;
        if(je!=null){list.addSectionHeading("Je / "+je.status,PTSDCommandSurface.AMBER,Color.BLACK,Alignment.MID,10);
            if(je.outcome!=null)list.addPara("%s",4,Misc.getTextColor(),je.outcome);
            if(je.distress&&!je.returning&&!je.done)list.addPara("接应期限剩余约 %s 日。击退实际追兵以打开撤离窗口。",5,Misc.getHighlightColor(),String.valueOf(Math.max(0,Math.ceil(je.deadline-PTSDCrisisState.getDay()))));
            if(!je.done)list.addButton("导航至Je调查护航队","RELIEF:JE",listWidth-12,30,5);
        }
        for(Map.Entry<String,String> note:PTSDReliefState.get(s).requestNotes.entrySet()){PTSDLateState.Front front=s.late.fronts.get(note.getKey());if(front==null||front.resolved)continue;StarSystemAPI sys=s.resolveSystem(front.systemId);list.addPara("%s",5,PTSDCommandSurface.INK,(sys==null?"战区":sys.getName())+" / "+note.getValue());}
        if(Global.getSettings().isDevMode()){
            list.addButton("DEV：组织联盟侦察队","RELIEF:SCOUT",listWidth-12,26,5);
            list.addButton("DEV：处理现有战区求援（保留3日响应）","RELIEF:REQUEST",listWidth-12,26,3);
            list.addButton("DEV：直接生成一队战区左径救援","RELIEF:PATH",listWidth-12,26,3);
            list.addButton("DEV：跳过现有整备与集结等待","RELIEF:READY",listWidth-12,26,3);
            list.addButton("DEV：发起Je战区调查航程","RELIEF:JE_TEST",listWidth-12,26,3);
            list.addButton("DEV：将Je调查队送抵目标战区","RELIEF:JE_ARRIVE",listWidth-12,26,3);
            if(je!=null&&je.done&&je.missing)list.addButton("DEV：解除本次Je失联状态","RELIEF:JE_RESET",listWidth-12,26,3);
        }
        List<PTSDReliefState.Unit> units=new ArrayList<PTSDReliefState.Unit>(PTSDReliefState.get(s).units);Collections.reverse(units);
        int closed=0;for(final PTSDReliefState.Unit u:units){if(!known(u))continue;if(u.stage==PTSDReliefState.Stage.DONE&&closed++>=5)continue;
            StarSystemAPI target=s.resolveSystem(u.targetSystem);MarketAPI source=s.resolveMarket(u.sourceMarket);FactionAPI faction=Global.getSector().getFaction(u.faction);
            Color color=u.kind.startsWith("PATH")?PTSDCommandSurface.AMBER:faction==null?PTSDCommandSurface.INK:faction.getBaseUIColor();
            list.addSectionHeading((u.kind.equals("SCOUT")?"巡回侦察":u.kind.startsWith("PATH")?"誓师支援":"联合行动")+" / "+stage(u),color,Color.BLACK,Alignment.MID,14);
            CustomPanelAPI strip=parent.createCustomPanel(listWidth-12,22,new Strip(u,color));list.addCustom(strip,5);
            list.addPara("%s",4,color,(source==null?"战地集结":source.getName())+" / "+(target==null?"等待定位":target.getName()));
            list.addPara("%s",3,Misc.getTextColor(),(u.fleet==null?0:Math.round(u.fleet.getFleetPoints()))+" FP / "+u.status);
            if(u.stage==PTSDReliefState.Stage.PREPARING)list.addPara("整备剩余约 "+Math.max(0,Math.round(u.stageDay+PTSDReliefDirector.cfg("relief_prepare")-PTSDCrisisState.getDay()))+"日",3);
            if(u.stage==PTSDReliefState.Stage.GARRISON)list.addPara("驻防剩余约 "+Math.max(0,Math.round(u.stageDay+PTSDReliefDirector.cfg("relief_garrison")-PTSDCrisisState.getDay()))+"日",3);
            if(u.stage!=PTSDReliefState.Stage.DONE)list.addButton("定位这支行动队","RELIEF:TRACK:"+u.id,listWidth-12,28,5);
        }
        list.addSectionHeading("来源兵力账本",Alignment.MID,16);
        list.addPara("可调兵力扣除了在外承诺与未补战损；返港归建只计幸存舰船。",5);
        for(MarketAPI market:PTSDReliefDirector.markets())if(PTSDReliefDirector.port(market)&&s.late.coalitionMembers.contains(market.getFactionId())){
            Float spent=PTSDReliefState.get(s).sourceSpent.get(market.getId());
            list.addPara("%s",4,Misc.getTextColor(),market.getName()+" / 可调 "+Math.round(PTSDReliefDirector.available(s,market))+" FP / 外派与待补战损 "+Math.round(spent==null?0:spent)+" FP");
        }
        list.addPara("战损补员：每 %s 日补充动员上限的 %s%%；不补回仍在外的兵力。",6,Misc.getHighlightColor(),String.valueOf(PTSDReliefDirector.cfg("relief_replenish_days")),String.valueOf(PTSDReliefDirector.cfg("relief_replenish_percent")));
        parent.addUIElement(list).inTR(0,0);
    }
    private static final class Strip extends BaseCustomUIPanelPlugin{
        final PTSDReliefState.Unit unit;final Color color;PositionAPI pos;Strip(PTSDReliefState.Unit u,Color c){unit=u;color=c;}
        public void positionChanged(PositionAPI p){pos=p;}
        public void renderBelow(float alpha){if(pos==null)return;org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
            for(int n=0;n<8;n++){float a=n<=unit.stage.ordinal()?.7f:.15f;org.lwjgl.opengl.GL11.glColor4f(color.getRed()/255f,color.getGreen()/255f,color.getBlue()/255f,a*alpha);float x=pos.getX()+n*pos.getWidth()/8,w=pos.getWidth()/8-4,y=pos.getY()+5;
                org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_QUADS);org.lwjgl.opengl.GL11.glVertex2f(x,y);org.lwjgl.opengl.GL11.glVertex2f(x+w,y);org.lwjgl.opengl.GL11.glVertex2f(x+w,y+10);org.lwjgl.opengl.GL11.glVertex2f(x,y+10);org.lwjgl.opengl.GL11.glEnd();}
            org.lwjgl.opengl.GL11.glPopAttrib();}
    }
    public static boolean action(String id,PTSDCrisisState s){
        if(!id.startsWith("RELIEF:"))return false;
        if("RELIEF:JE".equals(id)){PTSDJeWarExpedition.Mission je=PTSDReliefState.get(s).jeExpedition;if(je!=null&&!je.done&&PTSDJeWarExpedition.alive(je.escort))Global.getSector().layInCourseFor(je.escort);return true;}
        if(id.startsWith("RELIEF:TRACK:")){String key=id.substring(13);for(PTSDReliefState.Unit u:PTSDReliefState.get(s).units)if(key.equals(u.id)&&known(u)){
            SectorEntityToken target=u.physical&&u.fleet.isAlive()?u.fleet:s.resolveSystem(u.targetSystem)==null?null:s.resolveSystem(u.targetSystem).getHyperspaceAnchor();if(target!=null)Global.getSector().layInCourseFor(target);break;}return true;}
        if(!Global.getSettings().isDevMode())return true;
        if("RELIEF:JE_TEST".equals(id)){
            PTSDLateState.EventCard card=new PTSDLateState.EventCard();card.id="WR-JE-01";CampaignFleetAPI player=Global.getSector().getPlayerFleet();
            if(player!=null&&player.getStarSystem()!=null)card.systemId=player.getStarSystem().getId();
            String result=s.phase==PTSDCrisisState.Phase.WAR?PTSDJeWarExpedition.start(s,card):null;
            Global.getSector().getCampaignUI().addMessage(result==null?"未创建：需要WAR、合法战区与来源港口、可用护航编成，且Je当前可出任务。":result);return true;
        }
        if("RELIEF:JE_ARRIVE".equals(id)){PTSDJeWarExpedition.Mission m=PTSDReliefState.get(s).jeExpedition;
            if(m!=null&&!m.done&&!m.distress&&PTSDJeWarExpedition.alive(m.escort)&&m.escort.getBattle()==null){StarSystemAPI target=s.resolveSystem(m.system);if(target!=null){PTSDCampaignRuntime.leaving(m.escort);m.escort.getContainingLocation().removeEntity(m.escort);target.addEntity(m.escort);m.escort.setLocation(target.getCenter().getLocation().x,target.getCenter().getLocation().y);PTSDCampaignRuntime.entityAdded(m.escort);PTSDJeWarExpedition.advance(s,PTSDCrisisState.getDay());}}return true;}
        if("RELIEF:JE_RESET".equals(id)){PTSDJeWarExpedition.Mission m=PTSDReliefState.get(s).jeExpedition;if(m!=null&&m.done&&m.missing){PTSDReliefState.get(s).jeExpedition=null;s.jePostTaskBusyUntilDay=s.jeNextTaskAvailableDay=PTSDCrisisState.getDay();}return true;}
        if("RELIEF:SCOUT".equals(id)||"RELIEF:PATH".equals(id)){int before=PTSDReliefState.get(s).units.size();
            if("RELIEF:SCOUT".equals(id))PTSDReliefDirector.scout(s);else PTSDReliefDirector.devPath(s);
            Global.getSector().getCampaignUI().addMessage(PTSDReliefState.get(s).units.size()>before?"测试行动已创建，见行动列表与地图。":"未创建：请检查WAR与活动战线、合法来源星系、可用兵力及侦察队上限。");
        }
        else if("RELIEF:REQUEST".equals(id)){int accepted=0;for(PTSDLateState.Front front:s.late.fronts.values())if(!front.resolved&&PTSDReliefDirector.requestAssistance(s,front))accepted++;
            Global.getSector().getCampaignUI().addMessage("已受理或已有援军的战区："+accepted+"。警报不足3日的仍会等待；未承诺原因见战地回执。");}
        else if("RELIEF:READY".equals(id)){float day=PTSDCrisisState.getDay();for(PTSDReliefState.Unit u:PTSDReliefState.get(s).units)if(u.stage==PTSDReliefState.Stage.PREPARING)u.stageDay=day-PTSDReliefDirector.cfg("relief_prepare");for(String group:PTSDReliefState.get(s).groups.keySet())PTSDReliefState.get(s).groups.put(group,day-PTSDReliefDirector.cfg("relief_muster"));}
        return true;
    }
}
