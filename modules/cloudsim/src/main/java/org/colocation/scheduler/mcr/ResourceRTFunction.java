package org.colocation.scheduler.mcr;

import org.colocation.sensitiveFunction.AbstractSensitiveFunction;

import java.util.function.Function;

/**
 * Created by wkj on 2019/8/5.
 */
public class ResourceRTFunction implements Function {
    AbstractSensitiveFunction fun;
    double currentBW;
    double hostTotalBW;
    double currentRT;

    public ResourceRTFunction(AbstractSensitiveFunction fun, double currentBW, double hostTotalBW, double currentRT){
        this.fun = fun;
        this.currentBW = currentBW;
        this.hostTotalBW = hostTotalBW;
        this.currentRT = currentRT;
    }


    @Override
    public Object apply(Object o) {
        double addition = (double) o;
        double currLoss = fun.LossByMem(currentBW, hostTotalBW);
        double additionLoss = fun.LossByMem(currentBW + addition, hostTotalBW);
        double res = (currLoss/additionLoss -1)* currentRT;
        return res;
    }

    @Override
    public Function compose(Function before) {
        return null;
    }

    @Override
    public Function andThen(Function after) {
        return null;
    }
}
