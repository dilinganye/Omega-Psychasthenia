package data.scripts.campaign.invasion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.Set;

/**
 * Independent contact intel. Deliberately not a ContactIntel, so it never consumes the vanilla
 * contact limit and remains available even when the player's ordinary contact roster is full.
 */
public final class PTSDJeOtloesIntel extends BaseIntelPlugin {
    private static final long serialVersionUID = 1L;
    private static final String CONTACT = "PTSD_JE_CONTACT";
    private static final String GO_MEETING = "PTSD_JE_GO_MEETING";

    public PTSDJeOtloesIntel() { setImportant(false); }

    public static PTSDJeOtloesIntel ensureIntel() {
        Object existing = Global.getSector().getIntelManager()
                .getFirstIntel(PTSDJeOtloesIntel.class);
        if (existing instanceof PTSDJeOtloesIntel) return (PTSDJeOtloesIntel) existing;
        getOrCreatePerson();
        PTSDJeOtloesIntel intel = new PTSDJeOtloesIntel();
        Global.getSector().getIntelManager().addIntel(intel, false);
        return intel;
    }

    public static PersonAPI getOrCreatePerson() {
        PTSDCrisisState state = PTSDCrisisState.get();
        ImportantPeopleAPI people = Global.getSector().getImportantPeople();
        PersonAPI person = people.getPerson(PTSDJeOtloesManager.PERSON_ID);
        if (person == null) {
            person = Global.getSector().getFaction(Factions.INDEPENDENT)
                    .createRandomPerson(FullName.Gender.MALE);
            person.setId(PTSDJeOtloesManager.PERSON_ID);
            person.setName(new FullName("Je", "Otloes", FullName.Gender.MALE));
            person.setFaction(Factions.INDEPENDENT);
            person.setRankId(Ranks.SPECIAL_AGENT);
            person.setPostId(Ranks.POST_SPECIAL_AGENT);
            person.setImportance(PersonImportance.HIGH);
            person.setPortraitSprite("graphics/portraits/Helper/Helper_Ja.png");
            people.addPerson(person);
        }
        if (state != null) state.jePersonId = person.getId();
        return person;
    }

    public void sendContactUpdate() {
        sendUpdateIfPlayerHasIntel("contact", false);
    }

    public void finishMeeting() {
        setImportant(false);
        sendUpdateIfPlayerHasIntel("report_complete", false);
    }
    public void reportMeetingReady(MarketAPI market) {
        setImportant(true);
        sendUpdateIfPlayerHasIntel("meeting", false);
        Global.getSector().getCampaignUI().addMessage(
                "Je Otloes 请求在 " + market.getName() + " 与你见面。",
                Misc.getHighlightColor());
    }

    @Override protected String getName() {
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null && state.jeMeetingReady) return "Je Otloes：请求会面";
        return "Je Otloes";
    }

    @Override public String getSmallDescriptionTitle() { return getName(); }

    @Override public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(getName(), getTitleColor(mode), 0f);
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null && state.jeAgentIncidentId != null && !state.jeMeetingReady) {
            info.addPara("通讯暂时中断", 3f, Misc.getGrayColor());
        } else if (state != null && state.jeMeetingReady) {
            MarketAPI market = state.resolveMarket(state.jeMeetingMarketId);
            info.addPara(market == null ? "等待会面" : market.getName(), 3f,
                    Misc.getHighlightColor());
        } else {
            info.addPara("独立调查联系人", 3f, Misc.getGrayColor());
        }
    }

    @Override public boolean hasLargeDescription() { return true; }
    @Override public void createLargeDescription(com.fs.starfarer.api.ui.CustomPanelAPI panel,float width,float height) {
        PTSDNewsLocationPanel.document(panel,width,height,"Je Otloes / 私人信道","独立网络 / 任务回执",new java.awt.Color(163,151,185),
                new PTSDCommandSurface.Content(){public void draw(TooltipMakerAPI info,float w,float h){createSmallDescription(info,w,h);}},currentTask(),true);
    }
    private PTSDCrisisState.CrisisIncident currentTask() {
        PTSDCrisisState state=PTSDCrisisState.get();
        if(state==null || state.jePlayerTaskIncidentId==null)return null;
        for(PTSDCrisisState.CrisisIncident incident:state.incidents)
            if(incident!=null && state.jePlayerTaskIncidentId.equals(incident.id))return incident;
        return null;
    }
    @Override public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        PersonAPI person = getOrCreatePerson();
        info.addImage(person.getPortraitSprite(), width, 96f, 8f);
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state == null) return;
        if(PTSDJeWarExpedition.unavailable(state)){
            PTSDJeWarExpedition.Mission mission=state.late.relief.jeExpedition;
            info.addPara("%s",8f,Misc.getHighlightColor(),"战区调查 / "+mission.status);
            info.addPara(mission.missing?"最后一段回执停在受袭战区。Je尚未被确认死亡，但目前无法取得联系；需要后续救援线索。":"Je正在调查航程中；护航队的位置和求援期限已同步至联盟行动页。",6f);
            info.addButton("查看Je战区调查与求援","JE_WAR_RELIEF",width-24,30,8);return;
        }
        info.addPara("Je Otloes 通过一组不属于任何公开当局的信道与你保持联系。这个独立联系人不占用普通联系人上限。", 8f);
        info.addPara("但他似乎是个大忙人，如果一直联系不上，试着等一两天再说。", 5f);
        info.addPara("准确说，联系上他是个很看运气的事——或者你可以尝试足够烦人来引起他的注意力。", 5f);
        info.addPara("已协助完成的调查：%s", 8f, Misc.getHighlightColor(),
                String.valueOf(state.jeCompletedInvestigations));

        if (state.jeMeetingReady) {
            MarketAPI market = state.resolveMarket(state.jeMeetingMarketId);
            if (market != null) {
                info.addPara("他要求你前往 %s。调查结果只会在见面后交付。",
                        8f, Misc.getHighlightColor(), market.getName());
                info.addButton("前往 " + market.getName(), GO_MEETING,
                        getFactionForUIColors().getBaseUIColor(),
                        getFactionForUIColors().getDarkUIColor(), width, 24f, 8f);
            }
        } else if (state.jeAgentIncidentId != null) {
            int days = Math.max(0, (int) Math.ceil(
                    state.jeAgentReturnDay - PTSDCrisisState.getDay()));
            info.addPara("Je 正在代为调查；预计至少还需 %s 日。在此期间无法联系。",
                    8f, Misc.getHighlightColor(), String.valueOf(days));
        } else {
            info.addButton("联系 Je Otloes", CONTACT,
                    getFactionForUIColors().getBaseUIColor(),
                    getFactionForUIColors().getDarkUIColor(), width, 24f, 8f);
        }
    }

    @Override public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        PTSDCrisisState state = PTSDCrisisState.get();
        if("JE_WAR_RELIEF".equals(buttonId)){PTSDWarIntel war=PTSDWarIntel.ensureIntel();war.coalitionView().openReliefPage();Global.getSector().getCampaignUI().showCoreUITab(com.fs.starfarer.api.campaign.CoreUITabId.INTEL,war);return;}
        if (CONTACT.equals(buttonId) && state != null && state.jeAgentIncidentId == null &&
                !state.jeMeetingReady && !PTSDJeWarExpedition.unavailable(state)) {
            ui.showDialog(Global.getSector().getPlayerFleet(),
                    new PTSDJeOtloesDialog(PTSDJeOtloesDialog.Mode.CONTACT, null));
            return;
        }
        if (GO_MEETING.equals(buttonId) && state != null) {
            MarketAPI market = state.resolveMarket(state.jeMeetingMarketId);
            if (market != null && market.getPrimaryEntity() != null) {
                Global.getSector().layInCourseFor(market.getPrimaryEntity());
                ui.updateUIForItem(this);
                return;
            }
        }
        super.buttonPressConfirmed(buttonId, ui);
    }

    @Override public SectorEntityToken getMapLocation(SectorMapAPI map) {
        PTSDCrisisState state = PTSDCrisisState.get();
        MarketAPI market = state == null ? null : state.resolveMarket(state.jeMeetingMarketId);
        return market == null ? null : market.getPrimaryEntity();
    }

    @Override public String getIcon() { return getOrCreatePerson().getPortraitSprite(); }

    @Override public FactionAPI getFactionForUIColors() {
        FactionAPI independent = Global.getSector() == null ? null :
                Global.getSector().getFaction(Factions.INDEPENDENT);
        return independent == null ? super.getFactionForUIColors() : independent;
    }

    @Override public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add("个人");
        tags.add("联系人");
        PTSDCrisisState state = PTSDCrisisState.get();
        if (state != null && state.jeMeetingReady) tags.add("重要");
        return tags;
    }
}
