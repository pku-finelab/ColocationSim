package org.colocation.sensitiveFunction;


/**
 * Created by wkj on 2019/3/9.
 */
public class SensitiveFunction1 extends AbstractSensitiveFunction{
    public SensitiveFunction1() {
    }
    @Override
    public double LossByMem(double currBW, double totalBW) {
        double memPressure = (currBW/totalBW) *10;
        //(1-1/(1+e^-(x-7)))/2+0.5
        if (memPressure <0 )
            return 1;
        return 1-1/(1+Math.exp(-(2*memPressure-3)));
    }

    @Override
    public double rtToCapMemBW(double start, double delta) {
        return 0;
    }

    @Override
    public double getCurrRealPerf(double peerBW) {
        return 0;
    }
}
