package vn.vietan.deepflow.study;

/** Cùng mô hình exponential với MEMORY61 trên web; nghe hết chỉ là exposure. */
public final class StudyMemory {
    public static final long DAY=86400000L;
    public static final double TARGET=.92,BASE=.9;
    public static double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,Double.isFinite(x)?x:lo));}
    public static double retention(long last,double interval90,long now){return Math.pow(BASE,Math.max(0,now-last)/(DAY*interval90));}
    public static long due(long last,double interval90){return last+Math.round(interval90*Math.log(TARGET)/Math.log(BASE)*DAY);}
    public static double nextInterval(long last,double interval90,double difficulty,Double score,long now){
        boolean rated=score!=null&&Double.isFinite(score);double q=rated?clamp(score,0,100)/100:0;
        double d=rated?.85*difficulty+.15*(1-q):difficulty;
        double growth=!rated?1.2:q<.6?.3+.7*q:1.2+1.6*q+.5*(1-retention(last,interval90,now));
        return clamp(interval90*growth*(rated?1-.25*d:1),.125,365);
    }
}
