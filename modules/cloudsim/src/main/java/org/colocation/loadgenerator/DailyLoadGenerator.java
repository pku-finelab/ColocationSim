package org.colocation.loadgenerator;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.colocation.Constants;
import org.colocation.Request;
import org.colocation.RequestHandleEvent;
import org.colocation.ServiceEntity;
import org.colocation.qos.EntrypointAPI;
import org.colocation.uitl.Util;

/**
 * Created by wkj on 2019/6/12.
 */
public class DailyLoadGenerator extends LoadGenerator {
    EntrypointAPI api;
    double[] dailyPattern;
    double[] dailyPartternInSecond;
    int dayLen = 60*60*24;

    public DailyLoadGenerator(int brokerID, EntrypointAPI api, double startAt, int durationInSencond, double[] dailyPattern) {
        super(startAt, durationInSencond);
        this.api = api;
        this.dailyPattern = dailyPattern;
        this.dailyPartternInSecond = new double[dayLen];

        //interpolation
        Util.linearInterpolation(dailyPattern, dailyPartternInSecond);
    }
    @Override
    public void genLoad() {
        int counter = 0;
        for (int i = 0; i < this.duration; i++) {
            int xInDay = i % dayLen;
            int requestsNum = (int) Math.round(dailyPartternInSecond[xInDay] );

            for (int j = 0; j < requestsNum; j++) {
                counter += 1;
                sendARequest(api.getEntrypoint(), api.getAPIName()+"request_"+i+"_"+j, this.startTime+i);
            }
        }
        Log.printLine("Generated "+counter+" requests");
    }

}
