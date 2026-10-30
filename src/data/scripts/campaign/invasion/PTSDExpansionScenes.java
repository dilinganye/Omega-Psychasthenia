package data.scripts.campaign.invasion;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import java.util.*;

/** EXPANSION reports use world objects and their own evidence/response rules. */
final class PTSDExpansionScenes {
    static boolean handles(String card){return Arrays.asList("E-01","E-02","E-03","E-04","E-05","E-06").contains(card);}
    static boolean contested(PTSDCrisisState.CrisisIncident i){return "E-05".equals(i.cardId)||"E-06".equals(i.cardId);}
    static boolean accepts(String card,StarSystemAPI sys){
        if("E-06".equals(card))return !sys.getJumpPoints().isEmpty();
        if(!"E-02".equals(card)&&!"E-03".equals(card))return true;
        for(PlanetAPI p:sys.getPlanets())if(!p.isStar())return true;return false;
    }
    static boolean signalOptions(PTSDCrisisState.CrisisIncident i){
        if(!handles(i.cardId))return true;
        return ("E-04".equals(i.cardId)&&i.scene.branch>0)||(("E-02".equals(i.cardId)||"E-03".equals(i.cardId))&&i.scene.branch==2);
    }
    static void conclude(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i){
        PTSDNewsScene.State s=i.scene;
        boolean complete=s.recordTaken||s.samples>=s.nodes.size();
        String text="已保存"+s.samples+"处现场证据；未取得的部分保持未知。";
        if("E-01".equals(i.cardId)&&s.recordTaken)text="测绘船的上传与航程记录已经回收。只能确认它曾向非雇主地址传输，无法确定收件人。";
        if("E-02".equals(i.cardId)&&complete)text="实际设施与施工活动已记录。没有凭观测推断尚未发现的工程等级或远端部署。";
        if("E-03".equals(i.cardId)&&s.branch==2){complete=false;text="阵列与行星数据冲突，返回包包含本舰识别脉冲。已保留两份原始记录，无法确认反照率漂移的原因。";}
        if(contested(i)){
            complete=s.recordTaken;
            text=s.recordTaken?("E-05".equals(i.cardId)?"账簿副本已经封存。供货记录在同一批次中断，收款方的空白签章无法对应到任何已知身份。":"巡逻队的战斗记录已封存。损伤与交战时刻能够互证，攻击者的识别字段仍为空白。"):
                    s.recordLost?"记录装置已被对方带走。你只保留远距轮廓与残留传输，无法还原全部经过。":"你带走了远距观测与残骸轮廓，没有取得原始记录；报道中的原因仍无法确认。";
            if("E-05".equals(i.cardId)&&s.branch==2&&s.recordTaken){complete=false;text="所谓总账只有逐页重复的收讫印记。停供痕迹仍在，但这份装置无法证明补给链失联的原因；你保留了原件与异常发信记录。";}
        }
        PTSDNewsScene.finishAs(state,i,text,complete?PTSDTaskRules.InvestigationResult.CONFIRMED:PTSDTaskRules.InvestigationResult.INCONCLUSIVE);
    }
    static boolean build(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,StarSystemAPI sys,Random rng){
        PTSDNewsScene.State s=i.scene;
        // Preflight before adding owned objects, so waiting cannot leave half-built scenes.
        PlanetAPI planet=null;for(PlanetAPI p:sys.getPlanets())if(!p.isStar()){planet=p;break;}
        if(("E-02".equals(i.cardId)||"E-03".equals(i.cardId))&&planet==null){s.lastError="缺少可核查的实际行星，等待匹配地点。";return false;}
        if("E-06".equals(i.cardId)&&sys.getJumpPoints().isEmpty()){s.lastError="缺少可供幸存巡逻队撤出的实际跃迁点。";return false;}
        List<SectorEntityToken> relays=new ArrayList<SectorEntityToken>();
        if("E-04".equals(i.cardId)){
            relays.addAll(sys.getEntitiesWithTag(Tags.COMM_RELAY));
            List<StarSystemAPI> nearby=new ArrayList<StarSystemAPI>(Global.getSector().getStarSystems());
            nearby.sort((a,b)->Float.compare(Misc.getDistance(a.getLocation(),sys.getLocation()),Misc.getDistance(b.getLocation(),sys.getLocation())));
            for(StarSystemAPI other:nearby){if(relays.size()>=3)break;if(other==sys||other.hasTag(Tags.THEME_HIDDEN)||other.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER))continue;
                if(!other.getEntitiesWithTag(Tags.COMM_RELAY).isEmpty())relays.add(other.getEntitiesWithTag(Tags.COMM_RELAY).get(0));}
            if(relays.size()<2){s.lastError="缺少两处可比对的真实通讯中继；没有生成替代探针。";return false;}
        }
        Vector2f base=sys.getCenter().getLocation();SectorEntityToken old=PTSDNewsScene.entity(i.targetEntityId);if(old!=null&&old.getContainingLocation()==sys)base=old.getLocation();
        if("E-01".equals(i.cardId)){
            base=sys.getJumpPoints().get(0).getLocation();
            for(int n=0;n<(s.branch==1?3:2);n++)PTSDNewsScene.wreck(i,sys,PTSDNewsScene.offset(base,rng.nextFloat()*360,1200+rng.nextFloat()*1000),rng);
            if(s.branch==0){PTSDNewsScene.spawnCivilian(state,i,sys,PTSDNewsScene.offset(base,60,2100));CampaignFleetAPI f=PTSDNewsScene.fleet(s.civilian);if(f!=null){f.setName("失联测绘队幸存船");PTSDNewsScene.bind(i,f);}}
        }else if("E-02".equals(i.cardId)){
            if(s.reference==null)for(CampaignFleetAPI f:sys.getFleets())if(f.isStationMode()&&PTSDJeOtloesManager.isOmegaFleet(f)){s.reference=f.getId();break;}
            if(PTSDNewsScene.entity(s.reference)==null){s.lastError="没有实际施工设施可供核查。";return false;}
            PTSDNewsScene.bind(i,planet);
            SectorEntityToken installation=PTSDNewsScene.existingInstallation(sys);if(installation!=null)PTSDNewsScene.bind(i,installation);
        }else if("E-03".equals(i.cardId)){
            PTSDNewsScene.bind(i,planet);
            SectorEntityToken sensor=sys.getEntitiesWithTag(Tags.SENSOR_ARRAY).isEmpty()?null:sys.getEntitiesWithTag(Tags.SENSOR_ARRAY).get(0);
            if(sensor!=null)PTSDNewsScene.bind(i,sensor);
            else PTSDNewsScene.node(i,sys,"临时光谱复核阵列",PTSDNewsScene.offset(planet.getLocation(),115,1800),Entities.SENSOR_ARRAY_MAKESHIFT);
        }else if("E-04".equals(i.cardId)){
            for(SectorEntityToken relay:relays)PTSDNewsScene.bind(i,relay);
        }else if(contested(i)){
            s.reference=null; // Only this encounter's collector may block record recovery.
            // Scene-only wrecks are offset from the real port; never replace/delete its fleet or market.
            Vector2f field=PTSDNewsScene.offset(base,rng.nextFloat()*360,2200+rng.nextFloat()*800);
            for(int n=0;n<(s.branch==1?4:3);n++)
                PTSDNewsScene.wreck(i,sys,PTSDNewsScene.offset(field,rng.nextFloat()*360,180+rng.nextFloat()*1000),rng);
            SectorEntityToken record=PTSDNewsScene.entity(s.nodes.get(0));
            if(record!=null)record.setName("E-05".equals(i.cardId)?"补给船账簿舱残骸":"巡逻舰记录舱残骸");
            s.recordCollectionDay=PTSDCrisisState.getDay()+.5f;
            if("E-06".equals(i.cardId)&&s.branch!=1){
                String faction=old!=null&&old.getMarket()!=null?old.getMarket().getFactionId():PTSDNewsSiteManager.nearbyFaction(sys);
                CampaignFleetAPI survivors=FleetFactoryV3.createFleet(new FleetParamsV3(field,faction,.4f,FleetTypes.PATROL_SMALL,s.branch==0?22:12,0,0,0,0,0,0));
                if(survivors!=null&&!survivors.isEmpty()){
                    sys.addEntity(survivors);PTSDCampaignRuntime.entityAdded(survivors);survivors.setLocation(field.x+650,field.y-350);
                    survivors.setName("边境巡逻队幸存者");survivors.setTransponderOn(true);
                    survivors.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,true);
                    s.civilian=survivors.getId();s.owned.add(s.civilian);PTSDNewsScene.bind(i,survivors);
                    survivors.addAssignment(FleetAssignment.ORBIT_PASSIVE,record,30,"等待接应");
                }else s.lastError="未能组成幸存巡逻队；现场保留战斗记录供回收。";
            }
            if(s.branch==1||("E-06".equals(i.cardId)&&s.branch==0)){
                CampaignFleetAPI guard=PTSDNewsScene.spawnEnemy(state,i,sys,PTSDNewsScene.offset(field,210,5000),PTSDCrisisFleetLimits.forPhase(state,PTSDCrisisState.getDay()).perFleetFp,20,false);
                if(guard!=null){
                    s.reference=guard.getId();
                    guard.addAssignment(FleetAssignment.GO_TO_LOCATION,record,10,"接近记录残骸");
                    guard.addAssignment(FleetAssignment.PATROL_SYSTEM,record,10,"搜索残骸区");
                }
            }
        }
        return !s.nodes.isEmpty();
    }
    static void sampled(PTSDCrisisState state,PTSDCrisisState.CrisisIncident i,SectorEntityToken node){
        PTSDNewsScene.State s=i.scene;PTSDJeOtloesManager.recordParticipation(state,"incident:"+i.id);
        if(i.jeExclusiveScene)PTSDNewsScene.queueResponse(i,PTSDCrisisState.getDay());
        if(contested(i)){
            s.result="E-05".equals(i.cardId)?"远距读数保留了几段货单编号，无法读出账簿正文。需要在记录舱处回收原件，或就此封存观测。":"残骸上保留着交战方向与短促的识别广播。远距扫描只能还原片段；记录舱和幸存船可能保留更多。";
            return; // Observation never awards the key record or starts a pursuit.
        }
        if("E-01".equals(i.cardId)){
            if(s.branch==2){s.timerArmed=true;s.result="求救录音开始复述本舰刚发出的识别请求。保留距离可以封存现有证据；继续接驳将暴露位置。";}
            else if(s.samples>=s.nodes.size())s.recordTaken=true;
            return; // rescue, empty-ship record and bait are resolved by distinct dialogue actions
        }
        if("E-02".equals(i.cardId)){
            s.result=s.branch==0?"设施与观察时刻已关联。远距取证不会阻止施工。":s.branch==1?"出入方向上出现周期性运输回波；旧读数不能确认眼下仍有运输队。":"观察设备收到一次针对本舰扫描的应答，继续停留可能暴露位置。";
            if(s.branch==2){s.timerArmed=true;} // observe, disrupt and withdraw remain player choices
            return;
        }
        if("E-03".equals(i.cardId)){
            if(s.samples>=s.nodes.size()){
                if(s.branch==0)PTSDNewsScene.finishAs(state,i,"阵列使用了过期的基准光谱。行星反照率经校准后落回误差范围，未确认持续漂移。",PTSDTaskRules.InvestigationResult.DISPROVED);
                else if(s.branch==1)PTSDNewsScene.finish(state,i,"独立阵列与行星观测一致，反照率变化可复核。没有证据说明变化原因。",true);
                else {s.timerArmed=true;s.recordTaken=true;s.result="行星观测与阵列记录不一致；阵列返回包混入本舰识别脉冲。已保存原始数据，未知应答尚未结束。";}
            }return;
        }
        if("E-04".equals(i.cardId)){
            if(s.branch>0)s.timerArmed=true;
            if(s.samples>=s.nodes.size()){
                if(s.branch==2)PTSDNewsScene.queueResponse(i,PTSDCrisisState.getDay());
                PTSDNewsScene.finish(state,i,s.branch==0?"各站共用同一份失效路由表，盲区随路由切换复现。没有证据支持额外舰船活动。":s.branch==1?"多个独立中继在相同窗口失去回执；各站本地时钟正常，同步盲区得到核实。":"最后一站保留了本舰在前两站的访问间隔。记录已经复制，发信模块在取证过程中完成了一次上行。",true);
            }
        }
    }
    static boolean canRecover(PTSDCrisisState.CrisisIncident i,SectorEntityToken node){
        PTSDNewsScene.State s=i.scene;
        if(!contested(i)||s.resolved||s.recordLost||s.recordTaken||s.nodes.isEmpty()||node==null||!s.nodes.get(0).equals(node.getId()))return false;
        CampaignFleetAPI guard=PTSDNewsScene.fleet(s.reference);
        return guard==null||guard.getFleetPoints()<=0||guard.getContainingLocation()!=node.getContainingLocation()||Misc.getDistance(guard,node)>=1200;
    }
    static boolean recover(PTSDCrisisState.CrisisIncident i,SectorEntityToken node,float day){
        if(!canRecover(i,node))return false;
        PTSDNewsScene.State s=i.scene;s.recordTaken=true;s.responseNodeId=node.getId();
        // Branch is already persisted: reopening cannot reroll a safe retrieval into an ambush.
        if(s.branch!=0)PTSDNewsScene.queueResponse(i,day);
        s.result="E-05".equals(i.cardId)?s.branch==2?"记录舱突然发出一串收讫信号，屏幕上的账簿每一页都相同。原件已回收，但信号已经离开天线。":"打捞组取回了封存的供货账簿。你可以带走证据，接近的舰影仍可能继续搜索。":"打捞组将巡逻舰的记录舱固定在货舱里。交战记录可读，附近的未知回波仍未消失。";
        return true;
    }
    static void advance(PTSDCrisisState.CrisisIncident i,float day){
        if(!contested(i)||i.scene.resolved)return;
        PTSDNewsScene.State s=i.scene;
        if(s.branch!=1||s.recordTaken||s.recordLost||day<s.recordCollectionDay||s.nodes.isEmpty())return;
        CampaignFleetAPI collector=PTSDNewsScene.fleet(s.reference);SectorEntityToken record=PTSDNewsScene.entity(s.nodes.get(0));
        if(collector==null||record==null||collector.getBattle()!=null||collector.getFleetPoints()<=0||collector.getContainingLocation()!=record.getContainingLocation()||Misc.getDistance(collector,record)>=450)return;
        s.recordLost=true;s.result="对方已经拆走记录装置。残骸仍留在原地，远距传输的片段可供封存，但原始记录无法回收。";
        collector.clearAssignments();
        if(!collector.getStarSystem().getJumpPoints().isEmpty())collector.addAssignment(FleetAssignment.GO_TO_LOCATION,collector.getStarSystem().getJumpPoints().get(0),10,"携记录撤离");
        else collector.addAssignment(FleetAssignment.PATROL_SYSTEM,collector.getStarSystem().getCenter(),10,"撤出残骸区");
    }
    static String intro(PTSDCrisisState.CrisisIncident i){
        switch(i.cardId){
            case "E-01":return i.scene.branch==0?"测绘队幸存船停在残骸附近，上传记录仍留在受损设备内。可以优先接应人员，也可以回收记录。":i.scene.branch==1?"测绘设备被留在空船上，找不到可靠生命迹象。三处船体损伤与上传时间需要交叉核验。":"求救声来自测绘船的应急频道，却没有回应你的问题。短句始终保持相同的间隔。";
            case "E-02":return "光点又一次从行星边缘划过。观测员将它与设施方向重叠，几条记录仍有误差。你可以继续测定周期，也可以冒险接近，尝试干扰那里的活动。";
            case "E-03":return "行星在屏幕上缓慢转动。舰载仪器与当地阵列给出的亮度并不一致，观测员要求取得一份独立基准，再讨论那片阴影意味着什么。";
            case "E-05":return i.scene.branch==0?"补给船的货柜还在缓慢散开。账簿舱没有回应，残留电源却仍在逐笔核对一份早已中断的订单。":i.scene.branch==1?"陌生舰影正向补给船的记录舱靠近。要取得原件，你需要赶在它之前，或者设法将它引开。":"一只账簿舱反复广播收讫确认。没有货单正文，只有越来越整齐的确认音。";
            case "E-06":return i.scene.branch==0?"巡逻队的幸存船压低航速，守着几片还在发热的残骸。远处回波正在接近；你可以接应它们撤往跃迁口，或单独抢救记录。":i.scene.branch==1?"巡逻队已经不再回应。未知舰影正在靠近记录舱，残骸的损伤方向仍可远距读取。":"几艘幸存巡逻舰靠在残骸边，通讯员只肯重复请求离开。你可以护送它们带记录撤出，也可以让它们放下拖载先走。";
            default:return "此处是通讯盲区比对链的一站。必须取得不同中继的本地回执，才能区分共同路由故障、同步中断和对访问者的回传。";
        }
    }
}
