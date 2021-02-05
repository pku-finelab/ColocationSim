package org.colocation.loadgenerator;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.cloudbus.cloudsim.core.CloudSim;
import org.colocation.qos.EntrypointAPI;

/**
 * Created by wkj on 2019/6/16.
 */
public class BoostLoadGenerator extends LoadGenerator {
    EntrypointAPI api;
    JSONArray config;
    int copyNum;

    public BoostLoadGenerator(int brokerID, EntrypointAPI api, double startAt, int durationInSencond, double steadyLoad, double boostLoad, double boostStartTime, double boostDuration) {
        super(startAt, durationInSencond);
        this.api = api;

    }

    public BoostLoadGenerator(JSONArray jsonArr, int copysNum){
        super(0, 0);
        this.config = jsonArr;
        this.copyNum = copysNum;
    }

    @Override
    public void genLoad() {
        for (int i = 0; i < this.config.size(); i++) {
            JSONObject entrypoint = this.config.getJSONObject(i);
            int startAt = entrypoint.getInteger("startAt");
            int duration = entrypoint.getInteger("duration");
            int steadyLoad = entrypoint.getInteger("steadyQPS");
            int boostLoad = entrypoint.getInteger("boostLoad");
            int boostStart = entrypoint.getInteger("boostStart");
            int boostDuration = entrypoint.getInteger("boostDuration");

            for (int j = 0; j < duration; j++) {
                for (int k = 0; k < this.copyNum; k++) {
                    String svcName = "Copy." + k + "_" + entrypoint.getString("service");
                    int qps;
                    double delay = startAt + j;

                    if ( (delay >= (boostStart+startAt) ) && (delay <= (boostStart+startAt+boostDuration)) ) {
                        // send boost requests; high rps
                        qps = boostLoad;
                    } else {
                        // send steady request; low rps
                        qps = steadyLoad;
                    }

                    for (int l = 0; l < qps; l++) {
                        sendARequest(svcName ,svcName+"request_"+j+"_"+l, delay);
                    }
                }

            }

        }
    }

}
