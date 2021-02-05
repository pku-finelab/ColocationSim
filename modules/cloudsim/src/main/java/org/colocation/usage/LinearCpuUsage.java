package org.colocation.usage;

import com.alibaba.fastjson.JSONObject;

import java.util.Random;

/**
 * Created by wkj on 2019/6/5.
 */
public class LinearCpuUsage implements CpuUsageModel {
    double base;
    double cpuPerReq;
    double randomRange;
    Random random;

    public LinearCpuUsage(JSONObject json) {
        this(json.getDoubleValue("base"), json.getDoubleValue("cpuPerReq"), json.getDoubleValue("randomRange"));
    }

    public LinearCpuUsage(double base, double cpuPerReq, double randomRange) {
        this(base, cpuPerReq, randomRange, 0);
    }

    public LinearCpuUsage(double base, double cpuPerReq, double randomRange, long randomSeed) {
        this.base = base;
        Random random = new Random(randomSeed);
        this.cpuPerReq = cpuPerReq;
        this.randomRange = randomRange;
        this.random = random;
    }
    @Override
    /**
     * @return w * requestNum + random(-randomRange~randomRange)
     * **/
    public double getCpuUsageByRequestNum(int requestNum) {
        // b range: -randomRange randomRange
        double b = (random.nextDouble()*2-1) * randomRange;
        if ( b<0 ){
            b = 0;
        }
        double res = base + cpuPerReq*requestNum + b;
        return res;
    }
}
