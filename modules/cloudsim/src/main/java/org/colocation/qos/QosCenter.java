package org.colocation.qos;

import org.cloudbus.cloudsim.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wkj on 2019/3/21.
 */
public class QosCenter {
    // config qos
    Map<String, QoSConfiguration> config;
    Map<String, Double> bestRT;
    private double sla;
    public QosCenter(){
        this(0);
    }
    public QosCenter(double sla){
        config = new HashMap<>();
        bestRT = new HashMap<>();
        this.sla = sla;
    }

    public void setService(String apiName, String entrypoint, double baseRT, double violationRate) {
        EntrypointAPI api = new EntrypointAPI(entrypoint, apiName);
        QoSConfiguration qoSConfiguration = new QoSConfiguration(api, baseRT, violationRate);
        config.put(apiName, qoSConfiguration);
    }

    public void updateRT(String apiName, double rt) {
        if (!this.bestRT.containsKey(apiName)) {
            this.bestRT.put(apiName, rt);
            return;
        }
        double oldRT =this.bestRT.get(apiName);
        if (rt < oldRT) {
            this.bestRT.put(apiName, rt);
            return;
        }
        if (rt > oldRT/sla){
            Log.printLine("Alert! service"+apiName+" violates SLA");
        }

    }

    public double getBestRTof(String apiName) {
        return this.bestRT.get(apiName);
    }

    public void addQosConfig(QoSConfiguration configuration) {
        this.config.put(configuration.getAPIName(), configuration);
    }

    public double getApiRTBase(String apiName) {
        if (!this.config.containsKey(apiName)) {
            return -1;
        }
        return config.get(apiName).getTargetQoS();
    }

    public double getServiceUpperRT(String apiName) {
        if (!this.config.containsKey(apiName)) {
            return -1;
        }
        return config.get(apiName).getUpperQoS();
    }
}
