package data.scripts.campaign.invasion;

import com.fs.starfarer.api.campaign.SectorEntityToken;

/** Player-facing scene copy. Keep evidence and guidance here, separate from scene mechanics. */
public final class PTSDNewsSceneText {
    public static final String RARE_HEAT="无应答的巨大舰影", RARE_TRANSPORT="拖带密封舱的未知运载舰", RARE_ESCORT="伴随运载舰核查航路";
    private PTSDNewsSceneText(){}
    public static String sampling(PTSDNewsScene.State s) {
        int remaining=0; String system=null;
        for(String id:s.nodes) if(!s.visited.contains(id)) {
            remaining++;
            SectorEntityToken n=PTSDNewsScene.entity(id);
            if(system==null && n!=null && n.getStarSystem()!=null)system=n.getStarSystem().getName();
        }
        if(remaining==0)return "现有目标的采样已齐备。你可以核对调查结果，或就此结束调查。";
        if(system!=null && s.samples%2==0)return "你的数据和打捞小组的数据中着重强调了一段大致是位置信息的东西，尽管数据已经损坏，但根据目前的数值来看，应该是在"+system+"内。你可以继续调查，也可以宣布到此为止。你或许只需要再跑 "+remaining+" 个目标。";
        return "数据似乎不太完整，你可以继续调查，也可以宣布到此为止。根据你的数据和打捞小组给出的分析来看，你或许还有 "+remaining+" 个目标需要跑。";
    }
    static String intro(PTSDNewsScene.State s){
        switch(s.kind){
            case PORT_RECORDS:return "封存副本来自当地港务部门，另一份由承运方独立保管。两者可以核对，但只有原始签名才能说明记录何时被修改。";
            case ABANDONED_FACILITY:return "设施已不再接待靠泊，断开的记录舱仍在播送旧内容。广播与现场电源状态并不一致，可以先从舱外读取。";
            case SURRENDERED_FLEET:return "船队停在港口外，武器接口已封存。乘员不愿复述此前的遭遇，只允许核对他们递交的航行记录。";
            case TRAFFIC_OBSERVATION:return "接触对应一支仍在航行的真实舰队。两处采样位置记录了经过时刻，离开后只会保留航迹，不能把旧坐标当成实时位置。";
            case FORT_GRAVITY_SURVEY:return "三处采样位置沿黑洞外围向内排列。旧曲线无法给出下一个峰值。先在外围建立基准；深入会增加环境风险，也可能让发信者测出你的位置。";
            case FORT_SALVAGE_INTERCEPTION:return s.branch==0?"打捞船正在拖带记录舱。舰长请求接应，发动机回波却表明它已无法保持正常航速。":s.branch==1?"残骸间有舰船反复靠泊。每次离开，原本发出信号的舱段便少一块。":"求救录音再次从同一个字开始。通讯人数与被动生命读数不符；对方仍坚持要求近距离接驳。";
            case FORT_SUPPLY_WINDOW:return "航迹确实指向工程区。可观察交接窗口，或追踪真实运输队。运输抵达与损失会改变现场，不会等待你的决定。";
            case FORT_SENSOR_LATTICE:return "节点的朝向与本星系交通不符。另有几处相似信号散布在邻近星系，位置关系本身也许比广播内容更有意义。";
            case FORT_PATROL_GAP:return "三个观测位置记录了不同的巡逻时刻。空白可能是换防，也可能是专门留给进入者的通道。";
            case FORT_DECOY_TRANSIT:return "两处跃迁口出现近似轮廓，但质量采样并不一致。接近前应留意它们的发射功率。";
            case ROUTE_REVERSAL:return "三段尾迹沿真实跃迁航段分布。折返点缺少正常转弯的过渡，你需要分别核验时间与推力方向。";
            case ROUTE_DIVERSION:return "民用残骸附近仍有间歇信号。传感器边缘的接触没有可识别船体，却正在改变接近方向。";
            case RELAY_PARASITE:return "设施的服务信道先复述了你的识别请求，随后索取一份不属于正常检修流程的访问凭据。异常记录来自设备内部；它的公共功能仍在运行。";
            case SIGNAL_PURSUIT:return "应答没有署名。每当接收缓冲区增长，发射功率便随之上升。保持接通可能让远处的接收者找到这里。";
            case WRECK_RECONSTRUCTION:
                if("HEATDEATH".equals(s.wreckMode))return "巨大的轮廓独自掠过残骸，没有回应呼叫。它的扫描依次照过已经冷却的舱段，像在核对一份早已写好的清单。";
                if("ENTROPY_CONVOY".equals(s.wreckMode))return "一艘运载舰拖着密封舱缓慢经过，护卫始终守在侧后方。它们沿残骸间的空隙行进，没有主动接近本舰。";
                if("TWIN_SCOUTS".equals(s.wreckMode))return "两组陌生轮廓正在不同的残骸间移动。扫描脉冲掠过裸露的舱壁；它们似乎也在寻找记录。";
                if("SALVAGERS".equals(s.wreckMode))return "民用打捞船正穿过数片残骸，灯光照向不同的破口。船员只能提供所到之处的记录，公开报告还有空白。";
                if("BELT_OBSERVER".equals(s.wreckMode))return "漂流碎片混在小行星间。一个陌生接触仍按自己的顺序检查残骸，没有回应你的到来。";
                return "外围残片已被搬空，留下的接驳口却仍有供电。打捞组准备读取残留的日志；远处尚无可确认的接触。";
            case MISSING_CONVOY:return "航段上留有失联船体与救生信号。没有完整回执能够证明人员仍在船上。";
            case FACILITY_INTERDICTION:return "实际设施与往返航迹均可观测。取证不会改变它们的工作；切断补给或干扰则会让对方作出反应。";
            default:return "几个观测点的时间戳相互重叠。改变采样顺序或重复发射，可能得到不同的回应。";
        }
    }
    static String findings(PTSDNewsScene.State s){
        String[] results;
        switch(s.kind){
            case PORT_RECORDS:results=new String[]{"独立副本保留了更早的签名，港务记录随后才被改写。差异已归档；不能仅凭文件格式认定操作者。","两份记录的载荷与受理时间相符。报道所述的登记行为得到核实，但其理由仍未公开。","回执被批量使用，同一编号关联了不同的船。能确认的是档案冲突，尚无法指认幕后来源。"};break;
            case ABANDONED_FACILITY:results=new String[]{"断电区仍保留循环广播，现场没有近期人员活动的证据。记录舱已完成复制。","载波藏在广播的停顿中，设备安装痕迹晚于废弃时间。相关读数已保存，设备归属仍不明。","广播包含接近者刚发出的识别码。下载与现场状态已记录，继续停留可能暴露位置。"};break;
            case SURRENDERED_FLEET:results=new String[]{"缴械时间与乘员递交的日志吻合。没有人改变关于拒绝陈述的决定，记录只能证明船队主动交出了武器。","封存清单齐全，弹药消耗与战斗报告不符。投降属实，但没有证据能够还原此前遭遇。","几名乘员在相同位置停止叙述。最后一段航行数据已保存，未把推测写入证词。"};break;
            case TRAFFIC_OBSERVATION:results=new String[]{"前后段航迹连续，时间差与实际航行相符。历史读数不能用于推算其目前目的地。","两点质量采样能够互相校验，经过的舰影不是单独一段无线重放。","回波中的识别码在航段内更换过。原始轨迹已经保存，尚无法确定变更原因。"};break;
            case ROUTE_REVERSAL:results=new String[]{"两端记录引用了同一段旧尾流。所谓折返来自重放；发射位置已被测定。","推力残留在折返点连续变化，时间戳也能闭合。航迹确实经过了一次折返。","第三处节点复制了本舰经过前两点的间隔。这条路线正在用后来者校正自己，接收端已经应答。"};break;
            case ROUTE_DIVERSION:results=new String[]{"残骸来自多艘民船，偏航指令的发送时间早于事故。干扰信号已不再应答。","各船导航记录使用了相同的错误偏移量。最后一个无线接触在残骸区外突然加速离开。","损伤发生在抵达错误坐标之后。失踪航段被重新拼出，但没有找到完整幸存者名单。"};break;
            case WRECK_RECONSTRUCTION:results=new String[]{"弹着面指向第三个方向；两支被报道互相交火的船队无法造成这些损伤。","连接断口不属于同一次爆炸。有人移动过船体，公开的交战顺序无法成立。","回收工具留下的切口比炮击痕迹更新。最后一次记录读取引来了仍在工作的接收端。"};break;
            case SPATIAL_ECHO:results=new String[]{"三个点保留同一段旧读数，基准时钟却持续前进。重复发生的是数据，不是本舰的时间。","三组回波的相位差能够互相校验，原始记录已保存，异常边界可供复测。","最后一次回波带有本舰的新位置。取证资料已收齐，但采样次序也已被未知接收点记录。"};break;
            case FORT_SENSOR_LATTICE:results=new String[]{"三个节点仅在接收时耗能，朝向与本地航路无关。分布记录已保留，尚不足以确定终端位置。","节点在本舰靠近后轮流发射。记录显示它们属于同一套交接程序，不能据此直接确认远端位置。","停机外壳内仍有短促载波；各点的唤醒间隔互相吻合。所谓离线状态并不可信。"};break;
            case FORT_PATROL_GAP:results=new String[]{"旧队离开与新队抵达之间存在固定间隔。窗口很短，不能假定下一次仍然安全。","缺口与补给方向的集中航迹相重合。原有覆盖被暂时抽走，记录已标注观察时刻。","两侧航迹同时转向空白区域。这个缺口正在闭合，记录已保存，请立即离开。"};break;
            case FORT_DECOY_TRANSIT:results=new String[]{"两端轮廓相似，质量记录却只有一份能够闭合。诱饵留下了民用船体，无法据此认定原船队遭到袭击。","第二处回波重复了第一处较早的姿态。两次接触属于延迟重放，不能算作两支舰队。","诱饵广播覆盖了另一方向的运输尾迹。对照记录已保存，运输是否仍在该航段需继续侦察。"};break;
            case FORT_GRAVITY_SURVEY:results=new String[]{"仪器的共用校准记录有误，无法复现报道中的周期。","内侧记录补齐了外围缺失的周期。测绘保留了下一次活动窗口及误差范围，没有证据能说明其用途。","完整周期已下载；返回包里同时包含本舰识别脉冲。远距回应正在靠近最后采样位置。"};break;
            default:return s.recordTaken?"记录装置已保存，可交由其他观测站复核。":"现场观测已保存；未取得的部分保持未知。";
        }
        return results[Math.floorMod(s.branch,results.length)];
    }
    public static final String DIALOG_01 = "Je的附注：...因为此前的多次取样后都出现了武装接触。建议你先确认好你的撤离方向再进行操作。甚至我们不能排除事件本身就是一次陷阱的可能。 ";
    public static final String DIALOG_02 = "断开连接";
    public static final String DIALOG_03 = "接收缓冲仍在变化。预计连接剩余 ";
    public static final String DIALOG_04 = " 秒。";
    public static final String DIALOG_05 = "采样并记录此节点";
    public static final String DIALOG_06 = "封存调查[结束后不能继续操作]";
    public static final String DIALOG_07 = "保持信道通畅，集中传感器深入读取数值";
    public static final String DIALOG_08 = "切断异常发信模块";
    public static final String DIALOG_09 = "干扰现场[预计消耗10补给]";
    public static final String DIALOG_10 = "记录读数";
    public static final String DIALOG_11 = "接应船队并保留拖载记录舱";
    public static final String DIALOG_12 = "让船队抛弃拖载物，尽快撤离";
    public static final String DIALOG_13 = "近距离接驳，并准备回收记录";
    public static final String DIALOG_14 = "暂时离开";
    public static final String DIALOG_15 = "两组设备的共用校准错误已被证实，未冒险深入内侧。";
    public static final String DIALOG_16 = "已取得记录装置，现场仍可能有其他舰船需要接应。";
    public static final String DIALOG_17 = "已带走 ";
    public static final String DIALOG_18 = " 处采样。资料不完整，未作超出证据的判断。";
    public static final String DIALOG_19 = "重复录音与生命读数不符，求援诱饵已经识破。未接驳、未破坏装置，保留距离撤离。";
    public static final String DIALOG_20 = "未知频段开始复述本舰识别脉冲。继续接通会暴露接近位置。";
    public static final String DIALOG_21 = "发信装置已被破坏。远处的应答没有随它一起停止。";
    public static final String DIALOG_22 = "干扰设备已部署。关闭窗口后需在现场驻留1日，离开会重置驻留进度。";
    public static final String DIALOG_23 = "前往撤离出口";
    public static final String DIALOG_24 = "船队已经启航。救援将按实际抵达情况结算。";
    public static final String DIALOG_25 = "回收舰仍控制着接驳区。需要先引开它或通过交战解除封锁，当前只能远距取证。";
    public static final String DIALOG_26 = "舱门后没有人。发信装置却刚刚完成了一次握手，位置上传正在进行。";
    public static final String DIALOG_27 = "完整记录已被带走。只能保存拆卸与接驳痕迹。";
    public static final String DIALOG_28 = "记录舱已接入本舰。远处接触正在转向。";
    public static final String DIALOG_29 = "记录已完整转入本舰。远端仍在请求确认；现在可以携带记录撤离。";
    public static final String DIALOG_30 = "连接剩余 ";
    public static final String DIALOG_31 = " 秒";
    public static final String SCENE_001 = "目标星系不存在";
    public static final String SCENE_002 = "缺少跃迁点";
    public static final String SCENE_003 = "缺少真实黑洞";
    public static final String SCENE_004 = "缺少报道所需的殖民地";
    public static final String SCENE_005 = "等待真实航行舰队";
    public static final String SCENE_006 = "缺少可布置外围阵列的相邻星系";
    public static final String SCENE_007 = "缺少现存要塞";
    public static final String SCENE_008 = "等待要塞实体";
    public static final String SCENE_009 = "缺少可用撤离出口";
    public static final String SCENE_010 = "等待真实运输";
    public static final String SCENE_011 = "等待真实精神创伤设施";
    public static final String SCENE_012 = "报道中的废弃轨道设施";
    public static final String SCENE_013 = "脱落的记录舱";
    public static final String SCENE_014 = "解除武装的船队";
    public static final String SCENE_015 = "封存的武器接口";
    public static final String SCENE_016 = "船队递交的航行记录";
    public static final String SCENE_017 = "航迹前段采样";
    public static final String SCENE_018 = "航迹后段采样";
    public static final String SCENE_019 = "出航记录";
    public static final String SCENE_020 = "折返断点";
    public static final String SCENE_021 = "重叠尾迹";
    public static final String SCENE_022 = "间歇性求救区域";
    public static final String SCENE_023 = "未登记的通讯装置";
    public static final String SCENE_024 = "散逸的残片";
    public static final String SCENE_025 = "残骸区打捞船队";
    public static final String SCENE_026 = "搜集漂流残片";
    public static final String SCENE_027 = "救生舱定位回执";
    public static final String SCENE_028 = "设施外侧监视位置";
    public static final String SCENE_029 = "回波采样点 ";
    public static final String SCENE_030 = "备用交接位置";
    public static final String SCENE_031 = "运输交接观测位置";
    public static final String SCENE_032 = "改用备用交接航段";
    public static final String SCENE_033 = "等待交接回执";
    public static final String SCENE_034 = "完成物资入站";
    public static final String SCENE_035 = "等待护航交接";
    public static final String SCENE_036 = "追赶运输航次";
    public static final String SCENE_037 = "无登记监听节点";
    public static final String SCENE_038 = "巡防航迹 ";
    public static final String SCENE_039 = "跃迁口质量回波 ";
    public static final String SCENE_040 = "外围基准采样";
    public static final String SCENE_041 = "过渡区周期采样";
    public static final String SCENE_042 = "内侧校验采样";
    public static final String SCENE_043 = "加强运输护航";
    public static final String SCENE_044 = "现场实体未创建";
    public static final String SCENE_045 = "接近现场节点并交互；多点记录需逐一核验";
    public static final String SCENE_046 = "未知调查编队";
    public static final String SCENE_047 = "受困打捞船队";
    public static final String SCENE_048 = "等待撤离接应";
    public static final String SCENE_049 = "折返航迹";
    public static final String SCENE_050 = "航道偏引";
    public static final String SCENE_051 = "中继寄生";
    public static final String SCENE_052 = "陌生应答";
    public static final String SCENE_053 = "残骸复核";
    public static final String SCENE_054 = "船队失联";
    public static final String SCENE_055 = "工程截断";
    public static final String SCENE_056 = "空间回声";
    public static final String SCENE_057 = "补给窗口";
    public static final String SCENE_058 = "监听阵列";
    public static final String SCENE_059 = "巡防缺口";
    public static final String SCENE_060 = "双口诱影";
    public static final String SCENE_061 = "引力失拍";
    public static final String SCENE_062 = "打捞截击";
    public static final String SCENE_063 = "港务核验";
    public static final String SCENE_064 = "废站留声";
    public static final String SCENE_065 = "缴械船队";
    public static final String SCENE_066 = "过境观测";
    public static final String SCENE_067 = "期限";
    public static final String SCENE_068 = " 救援与记录报酬：";
    public static final String SCENE_069 = "星币。";
    public static final String SCENE_070 = "回波复述了刚刚改变的采样顺序。远距接收点已开始应答。";
    public static final String SCENE_071 = "两组设备复用了同一份错误校准。可以携带证据结束调查，或继续取回内侧原始记录。";
    public static final String SCENE_072 = "内侧原始记录也证实了共用校准错误，异常周期无法复现。";
    public static final String SCENE_073 = "期限内未能重新找到满足报道条件的现场，调查结束。";
    public static final String SCENE_074 = "调查期限已过，未能取得完整记录。";
    public static final String SCENE_075 = "截击正在撤离的船队";
    public static final String SCENE_076 = "调查中断的信号";
    public static final String SCENE_077 = "搜索失联接触";
    public static final String SCENE_078 = "封锁异常接触方向";
    public static final String SCENE_079 = "搜索侧翼航迹";
    public static final String SCENE_080 = "追踪已发现的舰队";
    public static final String SCENE_081 = "搜索最后接触位置";
    public static final String SCENE_082 = "现场干扰完成，工作窗口被打断。远距频段已出现回应，请准备撤离。";
    public static final String SCENE_083 = "核查阵列报警";
    public static final String SCENE_084 = "阵列周边巡查";
    public static final String SCENE_085 = "停止发射的诱饵残片";
    public static final String SCENE_086 = "航段记录确认运输中断，入站窗口未被使用。后续航次出现了额外护航接触。";
    public static final String SCENE_087 = "运输已经完成入站，卸载信号与航迹吻合。取证完成，但未能截断本次交付。";
    public static final String SCENE_088 = "连接已被远端切断；本地接口拒绝再次访问。";
    public static final String SCENE_089 = "船队未能脱险，仅保住了记录。";
    public static final String SCENE_090 = "救援船队失去联系，未取得完整记录。";
    public static final String SCENE_091 = "仍存活的 ";
    public static final String SCENE_092 = " 艘船已抵达出口。";
    public static final String SCENE_093 = "记录舱随船队一同脱险。";
    public static final String SCENE_094 = "拖载记录舱未能保全。";
    public static final String SCENE_095 = "拆取残骸记录";
    public static final String SCENE_096 = "携带记录离开";
    public static final String SCENE_097 = "记录舱已被拆走；残骸仍留下接驳痕迹。";
    public static final String SCENE_098 = "高速切断接触";
}
