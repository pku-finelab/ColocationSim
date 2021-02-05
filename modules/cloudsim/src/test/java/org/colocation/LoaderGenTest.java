package org.colocation;

import org.cloudbus.cloudsim.Log;
import org.colocation.loadgenerator.DailyLoadGenerator;
import org.colocation.loadgenerator.LoadGenerator;
import org.colocation.qos.EntrypointAPI;
import org.colocation.uitl.Util;
import org.junit.Test;

/**
 * Created by wkj on 2019/6/13.
 */
public class LoaderGenTest {
    @Test
    public void testLoaderGen(){
        double[] dailyPattern = {100.0,4.0,8.0};
        LoadGenerator lcLoader = new DailyLoadGenerator(0, new EntrypointAPI("test","test"), 0, 60, dailyPattern);
        lcLoader.genLoad();
    }
}
