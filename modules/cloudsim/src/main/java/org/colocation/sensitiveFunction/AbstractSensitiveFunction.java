package org.colocation.sensitiveFunction;

/**
 * Created by wkj on 2019/3/8.
 */
public abstract class AbstractSensitiveFunction {

    public AbstractSensitiveFunction(){

    }
    //given mem pressure ,get performance loss [1,+oo]
    public abstract double LossByMem(double peerBW, double totalBW);
    public abstract double getCurrRealPerf(double peerBW);
    public abstract double rtToCapMemBW(double start, double delta);
}
