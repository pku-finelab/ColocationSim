package org.colocation.qos;

import com.alibaba.fastjson.JSONObject;

/**
 * Created by wkj on 2019/6/4.
 */
public class QoSConfiguration {
    private EntrypointAPI entrypointAPI;
    private double  targetRT;
    private double  maxRT;

    public QoSConfiguration(JSONObject jsonObject) {
        String entryService = jsonObject.getString("service");
        this.entrypointAPI = new EntrypointAPI(entryService, entryService);
        this.targetRT = jsonObject.getDoubleValue("targetRT");
        this.maxRT = jsonObject.getDoubleValue("MaxRT");
    }

    public QoSConfiguration(EntrypointAPI entrypoint, double targetQoS, double maxRT) {
        this.entrypointAPI = entrypoint;
        this.targetRT = targetQoS;
        this.maxRT = maxRT;
    }

    public String getEntrypoint() {
        return entrypointAPI.getEntrypoint();
    }

    public String getAPIName() {
        return entrypointAPI.getAPIName();
    }

    public double getTargetQoS() {
        return targetRT;
    }

    public double getUpperQoS(){
        return this.maxRT;
    }
}
