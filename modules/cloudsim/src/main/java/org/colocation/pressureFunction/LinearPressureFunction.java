package org.colocation.pressureFunction;

import com.alibaba.fastjson.JSONObject;
import org.cloudbus.cloudsim.Log;

/**
 * Created by wkj on 2019/3/12.
 */
public class LinearPressureFunction implements IMemPressureFunction {
    double base;
    double BWPerReq;

    public LinearPressureFunction(JSONObject json){
        this.base = json.getDoubleValue("base");
        this.BWPerReq = json.getDoubleValue("BWPerReq");
    }
    public LinearPressureFunction(float base, float BWPerReq){
        this.base = base;
        this.BWPerReq = BWPerReq;
    }
    @Override
    public double getMemBWbyRequestNum(int requestNum) {
        return (double)(BWPerReq*requestNum + base );
    }
}
