package org.colocation;

import org.cloudbus.cloudsim.Log;
import org.colocation.bestEffort.ColocationJob;
import org.colocation.bestEffort.ColocationTask;
import org.colocation.bestEffort.JobBuilder;
import org.junit.Test;
import org.workflowsim.Task;

import java.util.List;

/**
 * Created by wkj on 2019/6/20.
 */
public class JobBuilderTest {
    @Test
    public void TestBuilder() {
        String path = "modules\\cloudsim-examples\\src\\main\\java\\org\\cloudbus\\cloudsim\\examples\\colocation\\beJobs.json";
        JobBuilder builder = new JobBuilder("aliDC", 0, path);
        List<ColocationJob> jobs = builder.getJobs();
        for (int i = 0; i < jobs.size(); i++) {
            //print job
            ColocationJob job = jobs.get(i);
            List<Task> tasks = job.getAllTasks();
            for (int j = 0; j < tasks.size(); j++) {
                ColocationTask ctask = (ColocationTask) tasks.get(j);
                Log.printLine(ctask.getTaskName()+"parent");
                List<Task> parentList = ctask.getParentList();
                for (int k = 0; k < parentList.size(); k++) {
                    ColocationTask p = (ColocationTask) parentList.get(k);
                    Log.print(p.getTaskName()+",");
                }
                Log.printLine();
            }
        }
    }
}
