package org.colocation;

import org.cloudbus.cloudsim.Log;
import org.colocation.uitl.Util;
import org.junit.Test;

import java.util.ArrayList;

/**
 * Created by wkj on 2019/6/13.
 */
public class InterpolationTest {
    @Test
    public void testLinearInterpolation(){
        double[] arr = {100.0, 3.0, 2.0};
        double[] dest = new double[50];
        Util.linearInterpolation(arr, dest);
        for (int i = 0; i < dest.length; i++) {
            Log.print(dest[i]+",");
        }
    }
}
