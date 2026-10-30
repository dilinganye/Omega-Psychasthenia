package data.scripts;

/** Equal-time control points; shape-preserving cubic interpolation, no overshoot. */
public final class PTSDStageCurve {
    private PTSDStageCurve(){}
    public static double[] parse(String text){
        if(text==null)throw new IllegalArgumentException("Missing curve");
        String[] parts=text.trim().replace('，',',').split(",",-1);
        if(parts.length<2||parts.length>64)throw new IllegalArgumentException("Curve requires 2 to 64 points");
        double[] result=new double[parts.length];
        for(int n=0;n<parts.length;n++){
            result[n]=Double.parseDouble(parts[n].trim());
            if(Double.isNaN(result[n])||Double.isInfinite(result[n])||result[n]<0||result[n]>100)
                throw new IllegalArgumentException("Curve values must be finite, between 0 and 100");
        }
        return result;
    }
    public static double sample(double[] points,double progress){
        if(points.length<2)throw new IllegalArgumentException("Missing endpoints");
        double x=Math.max(0,Math.min(1,progress))*(points.length-1);
        int i=Math.min(points.length-2,(int)x);double t=x-i;
        if(points.length==2)return points[0]+(points[1]-points[0])*t;
        double a=points[i],b=points[i+1],d=b-a;
        if(d==0)return a;
        double left=i==0?d:slope(points[i]-points[i-1],d);
        double right=i+2==points.length?d:slope(d,points[i+2]-points[i+1]);
        double value=(2*t*t*t-3*t*t+1)*a+(t*t*t-2*t*t+t)*left
                +(-2*t*t*t+3*t*t)*b+(t*t*t-t*t)*right;
        return Math.max(Math.min(a,b),Math.min(Math.max(a,b),value));
    }
    /** Integrate the same cubic used by the chart; two-point Gauss quadrature is exact per segment. */
    public static double integral(double[] points,double progress){
        if(progress<=0)return 0;
        double end=Math.min(1,progress),sum=0,step=1d/(points.length-1);
        for(int i=0;i<points.length-1;i++){
            double a=i*step,b=Math.min(end,(i+1)*step);if(b<=a)break;
            double mid=(a+b)/2,half=(b-a)/2,offset=half/Math.sqrt(3);
            sum+=half*(sample(points,mid-offset)+sample(points,mid+offset));
        }
        return sum+Math.max(0,progress-1)*points[points.length-1];
    }
    private static double slope(double a,double b){return a*b<=0?0:2*a*b/(a+b);}
}
