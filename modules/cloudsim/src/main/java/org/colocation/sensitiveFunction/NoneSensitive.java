package org.colocation.sensitiveFunction;

/**
 * Created by wkj on 2019/3/11.
 */

public class NoneSensitive extends AbstractSensitiveFunction {
    public NoneSensitive() {

    }
    @Override
    public double LossByMem(double currBW, double MemPressure) {
        return 1;
    }

    @Override
    public double rtToCapMemBW(double start, double delta) {
        return 0;
    }

    @Override
    public double getCurrRealPerf(double peerBW) {
        return 1;
    }
}