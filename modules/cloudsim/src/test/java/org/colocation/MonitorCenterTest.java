package org.colocation;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.colocation.monitor.MonitorCenter;
import org.colocation.monitor.MonitorData;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;

/**
 * Created by wkj on 2019/6/5.
 */
public class MonitorCenterTest {
    @Before
    public void setUp(){
        int num_user = 1;
        Calendar calendar = Calendar.getInstance();
        CloudSim.init(num_user, calendar, false);

    }
    @Test
    public void testFetchData(){
        MonitorCenter center = new MonitorCenter();
        MonitorData data1 = new MonitorData(0, 0, 1.0); data1.addMetric(Constants.METRIC_CPU, 1);data1.addMetric(Constants.METRIC_MEM, 100);
        MonitorData data2 = new MonitorData(0, 0, 4.0); data2.addMetric(Constants.METRIC_CPU, 5);data2.addMetric(Constants.METRIC_MEM, 100);
        MonitorData data3 = new MonitorData(0, 0, 6.0); data3.addMetric(Constants.METRIC_CPU, 3);data3.addMetric(Constants.METRIC_MEM, 120);
        MonitorData data4 = new MonitorData(0, 0, 7.0); data4.addMetric(Constants.METRIC_CPU, 2);data4.addMetric(Constants.METRIC_MEM, 150);
        MonitorData data5 = new MonitorData(0, 0, 10.0); data5.addMetric(Constants.METRIC_CPU, 0);data5.addMetric(Constants.METRIC_MEM, 110);
        MonitorData data6 = new MonitorData(0, 0, 15.0); data6.addMetric(Constants.METRIC_CPU, 1);data6.addMetric(Constants.METRIC_MEM, 110);
        center.addMonitorData(data1);
        center.addMonitorData(data2);
        center.addMonitorData(data3);
        center.addMonitorData(data4);
        center.addMonitorData(data5);
        center.addMonitorData(data6);
        List arr = center.getContainerHistory(0, 0, Constants.METRIC_CPU, "s", 17, 16);
        Log.printLine("arr size:"+ arr.size());
        Log.printLine(arr);
    }
}
