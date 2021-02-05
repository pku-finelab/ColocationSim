package org.colocation.sensitiveFunction;

import org.colocation.pressureFunction.LinearPressureFunction;

/**
 * Created by wkj on 2019/3/22.
 */
public class LinearSensitiveFunction extends AbstractSensitiveFunction {
    public LinearSensitiveFunction() {

    }
    @Override
    public double LossByMem(double peerBW, double totalBW) {
        double memPressure = (peerBW/totalBW) *10;
        if (memPressure <0 )
            return 1;
        if (memPressure >=10)
            return 0.00000001;
        return (-memPressure/10 +1);
    }

    @Override
    public double rtToCapMemBW(double start, double delta) {
        return 0;
    }

    @Override
    public double getCurrRealPerf(double peerBW) {
        return -1;
    }
}
