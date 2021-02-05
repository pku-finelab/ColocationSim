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
public class RoundRobinAlgorithm extends BaseSchedulingAlgorithm {
    int currScheduleHostIndex = 0;
    public RoundRobinAlgorithm(){
        super();
        this.setCloudletList(new ArrayList<>());
        Log.printLine("create RoundRobin scheduler");
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
            for (int i = 0; i < vmList.size(); i++) {
                int curr = (i + currScheduleHostIndex)%vmList.size();
                ColocationHost vm = vmList.get(curr);
                long avail = vm.getBeAvailableRam();
                if (avail >= t.getRamQuota()) {
                    Log.printLine(CloudSim.clock()+": schedule task #"+t.getTaskFullName()+" to vm:#"+vm.getId());
                    t.setVmId(vm.getId());
                    taskToRemove.add(t);
                    currScheduleHostIndex = (currScheduleHostIndex+1) % vmList.size();
                    break;
                }
            }
        }
        for (ColocationTask t: taskToRemove) {
            tasks.remove(t);
        }
        this.setCloudletList(tasks);
    }
}
