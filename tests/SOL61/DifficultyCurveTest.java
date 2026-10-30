package data.scripts;

public final class DifficultyCurveTest {
    static void near(double a,double b){if(Math.abs(a-b)>1e-6)throw new AssertionError(a+" != "+b);}
    public static void main(String[] args){
        near(PTSDStageCurve.sample(PTSDStageCurve.parse("1,3"),.25),1.5);
        double[] p=PTSDStageCurve.parse("1,1.1,1.5,1.6,1.7,2");
        for(int n=0;n<p.length;n++)near(PTSDStageCurve.sample(p,n/5d),p[n]);
        for(String text:new String[]{"1,5,2,8,0","5,4,3,2","1,1,2,2","0,0,0"}){
            double[] a=PTSDStageCurve.parse(text);
            for(int n=0;n<1000;n++){
                double t=n/1000d,v=PTSDStageCurve.sample(a,t);int i=Math.min(a.length-2,(int)(t*(a.length-1)));
                if(v<Math.min(a[i],a[i+1])-1e-9||v>Math.max(a[i],a[i+1])+1e-9)throw new AssertionError("Overshoot");
            }
        }
        for(String bad:new String[]{"1","1,","1,NaN","1,Infinity","-1,2","1,101"}){
            boolean rejected=false;try{PTSDStageCurve.parse(bad);}catch(IllegalArgumentException ex){rejected=true;}
            if(!rejected)throw new AssertionError("Accepted "+bad);
        }
        near(PTSDStageCurve.sample(p,-1),1);near(PTSDStageCurve.sample(p,2),2);
        System.out.println("DifficultyCurveTest passed");
    }
}
