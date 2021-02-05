package org.colocation.scheduler;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.colocation.ColocationHost;
import org.colocation.bestEffort.ColocationTask;
import org.workflowsim.scheduling.BaseSchedulingAlgorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ParagonAlgorithm extends BaseSchedulingAlgorithm {
    public ParagonAlgorithm() {
        super();
        this.setCloudletList(new ArrayList<>());
        Log.printLine("create Paragon scheduler");
    }

    @Override
    public void run() throws Exception {
        List<ColocationHost> vmList = getVmList();
        List<ColocationTask> taskToRemove = new ArrayList<>();
        List<ColocationTask> tasks = this.getCloudletList();
        for(ColocationTask t : tasks) {
            ArrayList<HostAndValue> dValues = new ArrayList<>();
            for (ColocationHost vm : vmList) {
                double d1 = 0.0;
                double d2 = 0.0;
                //cal d1
                double t_server = vm.getMinSLASlack();
                double c_newapp = vm.getInterferenceBy(t.getMemBWQuota());
                d1 = t_server - c_newapp;

                //cal d2
                double currMBW = vm.getCurrentBW();
                double t_newapp = 0.1-t.getSensitiveFunction().LossByMem(currMBW,0);
                double c_server = 1/t.getSensitiveFunction().LossByMem(currMBW,0);
                d2 = t_newapp - c_server;

                double d = Math.abs(d1) + Math.abs(d2);
                HostAndValue hostAndDValue = new HostAndValue(vm, d);
                dValues.add(hostAndDValue);
            }
            dValues.sort(Comparator.reverseOrder());
            for (int i = 0; i < dValues.size(); i++) {
                HostAndValue curr = dValues.get(i);
                ColocationHost host = curr.getHost();
                Log.printLine(CloudSim.clock()+": schedule task #"+t.getTaskFullName()+" to vm:#"+host.getId());
                int scheRes = t.tryRunningOnHost(host.getId());
                if (scheRes >0){
                    taskToRemove.add(t);
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
