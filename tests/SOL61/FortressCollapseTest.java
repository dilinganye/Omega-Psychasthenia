package data.scripts.campaign.invasion;
public final class FortressCollapseTest {
    static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    public static void main(String[] args){
        check(PTSDFortressCollapse.reducedCR(.07f,.1f,.1f)==.07f,"Below-floor ship healed or harmed");
        check(Math.abs(PTSDFortressCollapse.reducedCR(.15f,.1f,.1f)-.1f)<.00001f,"Floor lost");
        check(Math.abs(PTSDFortressCollapse.reducedCR(.7f,.07f,.1f)-.63f)<.00001f,"Loss incorrect");
        check(PTSDCollapseMaterials.feather(0)==0 && PTSDCollapseMaterials.feather(1)<.000001f,"Material edge not transparent");
        check(Math.abs(PTSDCollapseMaterials.feather(.5f)-1)<.000001f,"Material centre lost");
        PTSDLateState.Detonation d=new PTSDLateState.Detonation();d.collapseInitialized=true;
        check(PTSDFortressCollapse.claim(d,"ship",0),"First hit rejected");
        check(!PTSDFortressCollapse.claim(d,"ship",0),"Repeated wave applied");
        check(PTSDFortressCollapse.claim(d,"ship",1),"Different wave rejected");
        check(PTSDFortressCollapse.claim(d,"other",0),"Different ship rejected");
        float previous=PTSDFortressCollapse.advance(d,20);check(previous==0&&d.collapseElapsed==20,"Clock mismatch");
        PTSDFortressCollapse.advance(d,-10);check(d.collapseElapsed==20,"Clock reversed");
        PTSDFortressCollapse.advance(d,1000);check(d.collapseElapsed==90&&d.remainingSeconds==0,"Duration overflow");
        for(int wave=0;wave<PTSDFortressCollapseVisual.frontCount();wave++){
            float start=PTSDFortressCollapseVisual.frontStart(wave);
            check(PTSDFortressCollapseVisual.frontRadius(wave,start-1)<0,"Early wave");
            check(!PTSDFortressCollapseVisual.crosses(wave,1000,0,start-2,start-1),"Pre-wave damage");
            check(PTSDFortressCollapseVisual.crosses(wave,1000,0,start,start+5),"Front missed target");
            check(!PTSDFortressCollapseVisual.crosses(wave,1000,0,start+5,start+6),"Front damages wake repeatedly");
        }
        check(PTSDFortressCollapseVisual.frontDistance(0,0,1000)>PTSDFortressCollapseVisual.frontDistance(0,1000,0),"Elliptical front ignored");
        System.out.println("FortressCollapseTest passed (CR floor, no healing, hit dedup, clock, authored wave crossings)");
    }
}
