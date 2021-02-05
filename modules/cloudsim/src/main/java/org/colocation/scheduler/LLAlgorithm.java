package org.colocation.scheduler;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.colocation.ColocationHost;
import org.colocation.bestEffort.ColocationTask;
import org.workflowsim.scheduling.BaseSchedulingAlgorithm;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wkj on 2019/3/14.
 */
public class LLAlgorithm extends BaseSchedulingAlgorithm {
    public LLAlgorithm(){
        super();
        this.setCloudletList(new ArrayList<>());
        Log.printLine("create Least Load scheduler");
    }
    /**
     * The main function
     *
     * @throws Exception
     */
    @Override
    public void run() throws Exception {
        // score
        // weight
        // remove success scheduled tasks
        List<ColocationHost> vmList = getVmList();
        List<ColocationTask> taskToRemove = new ArrayList<>();
        List<ColocationTask> tasks = this.getCloudletList();
        for(ColocationTask t : tasks) {
            double minResource = Double.MAX_VALUE;
            ColocationHost target = null;
            for (ColocationHost vm : vmList) {
                double cpu = vm.getCpuUsagePercent();
                double mem = vm.getMemUsagePercent();
                double agg = cpu;
                if (agg < minResource){
                    minResource = agg;
                    target = vm;
                }
            }
            Log.printLine(CloudSim.clock()+": schedule task #"+t.getTaskFullName()+" to vm:#"+target.getId());
            t.setVmId(target.getId());
            taskToRemove.add(t);
        }
        for (ColocationTask t: taskToRemove) {
            tasks.remove(t);
        }
        this.setCloudletList(tasks);
    }
}
