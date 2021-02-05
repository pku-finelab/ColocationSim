package org.colocation.scheduler.mcr;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.colocation.ServiceEntity;
import org.colocation.trace.RequestGraphNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

/**
 * Created by wkj on 2019/8/3.
 */
public class ServicePathSlack {
    ArrayList<Function<Double, Double>> polynomial;
    double slack;
    private HashMap<String, ServiceInPath> services;

    public ServicePathSlack(){
        polynomial = new ArrayList<>();
        slack = 0;
        services = new HashMap<>();
    }

    public void addServiceStage(RequestGraphNode stage){
        String svcName = stage.getSvcName();
        if (!services.containsKey(svcName)){
            ServiceEntity se = (ServiceEntity) CloudSim.getEntity(svcName);
            ServiceInPath sip = new ServiceInPath(se, stage);
            services.put(svcName, sip);
        } else {
            ServiceInPath sip = services.get(svcName);
            sip.addStage(stage);
        }
    }

    public void addFunction(Function<Double, Double> function) {
        polynomial.add(function);
    }

    public void setSlack(double slack) {
        this.slack = slack;
    }

    public boolean canEstablishedBy(double x) {
        double sum =0;
        for (Function<Double, Double> f: polynomial) {
            double res =  f.apply(x);
            sum = sum + res;
        }
        if (sum < slack) {
            return true;
        }
        return false;
    }
    public double getSlackAfter(double x){
        double rasiedToatal = 0;
        if (slack<0) {
            //Log.printLine("slack < 0");
        }
        for (String key : services.keySet()){
            ServiceInPath sip = services.get(key);
            double currPress= sip.getCurrentPressure();
            double sipBest = sip.getBestTotalStageTimeCost();
            double currCost = sip.currTotalStageTimeCost();
            double perf = sip.getPerf(currPress + x);
            double afterSipLength = sipBest / perf;
            double raised = afterSipLength - currCost;
            rasiedToatal += raised;
        }
        return slack - rasiedToatal;
    }
}
