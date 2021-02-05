package org.colocation.loadgenerator;

import org.colocation.qos.EntrypointAPI;

/**
 * Created by wkj on 2019/7/23.
 */
public class NumberableRequestGenerator extends LoadGenerator {
    int requestNum;
    EntrypointAPI api;
    public NumberableRequestGenerator(int brokerID, EntrypointAPI api, double startAt, int requestNum) {
        super(startAt, -1);
        this.api = api;
        this.requestNum = requestNum;
    }
    @Override
    public void genLoad() {
        for (int i = 0; i < requestNum; i++) {
            sendARequest(api.getEntrypoint(), api.getAPIName()+"request_"+i, this.startTime+i);
        }
    }
}
