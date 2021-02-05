package org.colocation.scheduler.mcr;

import org.colocation.ServiceEntity;
import org.colocation.sensitiveFunction.AbstractSensitiveFunction;
import org.colocation.trace.RequestGraphNode;

public class ServiceInPath {
    ServiceEntity se;
    private double bestTotalStageTimeCost;
    private double currTotalTimeCost;

    public ServiceInPath(ServiceEntity se, RequestGraphNode node) {
        this.se = se;
        this.bestTotalStageTimeCost = node.getMinCost();
        this.currTotalTimeCost = node.value();
    }

    public void addStage(RequestGraphNode node) {
        this.bestTotalStageTimeCost += node.getMinCost();
        this.currTotalTimeCost += node.value();
    }


    public double getCurrentPressure(){
        return se.getPeerPressure();
    }

    public double getBestTotalStageTimeCost() {
        return bestTotalStageTimeCost;
    }

    public double getPerf(double pressure) {
        AbstractSensitiveFunction fun =se.getSensitiveFun();
        return fun.LossByMem(pressure, 0);
    }

    public double currTotalStageTimeCost() {
        return currTotalTimeCost;
    }
}
