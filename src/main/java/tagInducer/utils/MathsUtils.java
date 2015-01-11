package tagInducer.utils;

/**
 * A collection of statistical functions used during sampling.
 */
public class MathsUtils {

    public double randNormal(double mean, double stdDev){
        double r1 = Math.random();
        double r2 = Math.random();
        return stdDev*Math.sqrt(-2*Math.log(r1))*Math.cos(2*Math.PI*r2)+mean;
    }

    public double densityNorm(double val, double mean, double stdDev){
        return 1.0/(stdDev*Math.sqrt(2*Math.PI))*Math.exp(-1*Math.pow(val-mean, 2)/(2*stdDev*stdDev));
    }

    public int multSample(double[] p){
        int totObs = p.length;
        double[] dist = new double[totObs];
        System.arraycopy(p, 0, dist, 0, totObs);
        int sample;
        //Cumulate multinomial parameters
        for (sample = 1; sample < totObs; sample++) {
            dist[sample] += dist[sample-1];
        }
        //Sample from the cumulative distribution
        //Scaled sample because of unnormalised p[]
        double u = Math.random()*dist[totObs-1];
        for (sample = 0; sample < totObs; sample++) {
            if (dist[sample] > u) break;
        }
        return sample;
    }

    /**
     * Multinomial sample in log-space
     */
    public int multSampleLog(double[] p){
        int totObs = p.length;
        double[] dist = new double[totObs];
        System.arraycopy(p, 0, dist, 0, totObs);
        double min = 0; double max = Double.NEGATIVE_INFINITY;
        for (int sample = 0; sample < totObs; sample++) {
            if (dist[sample] < min) min = dist[sample];
            if (dist[sample] > max) max = dist[sample];
        }
        boolean mult = false;
        if (max < -100) mult = true;
        for (int sample = 0; sample < totObs; sample++) {
            if (mult) {
                double div = min/(-100);
                dist[sample] = Math.exp(dist[sample]/div);
            }
            else dist[sample] = Math.exp(dist[sample]);
        }
        return multSample(dist);
    }
}
