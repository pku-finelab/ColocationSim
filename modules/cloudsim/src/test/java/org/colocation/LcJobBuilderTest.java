package org.colocation;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.colocation.bestEffort.ColocationJob;
import org.colocation.bestEffort.ColocationTask;
import org.colocation.bestEffort.JobBuilder;
import org.colocation.lcjob.LcJobBuilder;
import org.colocation.qos.QoSConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.workflowsim.Task;

import java.util.Calendar;
import java.util.List;

/**
 * Created by wkj on 2019/6/20.
 */
public class LcJobBuilderTest {
    @Before
    public void setUp(){
        int num_user = 1;
        Calendar calendar = Calendar.getInstance();
        CloudSim.init(num_user, calendar, false);

    }

    @Test
    public void TestBuilder() {
        String path = "E:\\PKU_DOC\\colocaton_research\\cloudsim-cloudsim-4" +
                ".0\\modules\\cloudsim-examples\\src\\main\\java\\org\\cloudbus\\cloudsim\\examples\\colocation\\socialnetwork.json";
        LcJobBuilder builder = new LcJobBuilder("aliDC", 0, path);
        List<ServiceEntity> ses = builder.getServices();
        for (int i = 0; i < ses.size(); i++) {
            //print job
            ServiceEntity se = ses.get(i);
            Log.printLine(se.getName());
        }
        List<QoSConfiguration> qos = builder.getQosConfigurations();
        for (int i = 0; i < qos.size(); i++) {
            //print job
            QoSConfiguration q = qos.get(i);
            Log.printLine(q.getEntrypoint()+" RT:"+q.getTargetQoS()+" MAXRT:"+q.getUpperQoS());
        }
    }
}
