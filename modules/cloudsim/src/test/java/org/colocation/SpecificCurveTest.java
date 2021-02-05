package org.colocation;

import org.colocation.sensitiveFunction.SpecificCurveFunction;
import com.alibaba.fastjson.JSONArray;
import org.junit.Test;
import org.cloudbus.cloudsim.Log;

public class SpecificCurveTest {
    @Test
    public void getPerf(){

        JSONArray arr = JSONArray.parseArray("[[0.9353,1.00122],[1.9226,1.00244],[2.8783,0.98811],[3.8443,0.95519],[4.8302,0.92561],[5.7896,0.89306],[6.7689,0.84848],[7.7415,0.82635],[8.6973,0.77378],[9.6426,0.73825],[10.6194,0.71537],[11.5857,0.73012],[12.5415,0.71076],[13.5708,0.6776],[14.5266,0.65766],[15.4824,0.65094],[16.4802,0.62571],[17.373,0.60988],[18.3603,0.61463],[19.3687,0.60263],[20.3665,0.60973],[21.2908,0.60096],[22.2466,0.60571],[23.2549,0.60194],[24.2422,0.60993],[25.1665,0.60938],[26.0803,0.60384],[28.0654,0.60365],[29.0842,0.60429]]");
        SpecificCurveFunction curve = new SpecificCurveFunction(arr);
        Log.printLine(curve.LossByMem(15,0));
        Log.printLine(curve.LossByMem(7,0));
        Log.printLine(curve.LossByMem(0,0));
        Log.printLine(curve.LossByMem(32,0));
    }
}
