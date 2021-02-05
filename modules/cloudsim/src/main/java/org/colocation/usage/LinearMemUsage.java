package org.colocation.usage;

import com.alibaba.fastjson.JSONObject;
import org.colocation.Constants;

/**
 * Created by wkj on 2019/6/5.
 */
public class LinearMemUsage implements MemUsageModel {
    long leakPerReq;
    long memPerReq;
    long base;

    public LinearMemUsage(JSONObject json) {
        this.base = Math.round(json.getDouble("baseMB") * Constants.MB);
        this.memPerReq = Math.round(json.getDoubleValue("memPerReqMB") * Constants.MB);
        this.leakPerReq = Math.round(json.getDoubleValue("bytesLeakPerReq"));
    }

    public LinearMemUsage(long base, long leakPerReq, long memPerReq) {
        this.leakPerReq = leakPerReq;
        this.memPerReq = memPerReq;
        this.base = base;
    }
    @Override
    public long getMemUsageByRequestNum(long proccessedReqNum, int currReqNum) {
        long res = base + leakPerReq * proccessedReqNum + currReqNum*memPerReq;
        return res;
    }
}
