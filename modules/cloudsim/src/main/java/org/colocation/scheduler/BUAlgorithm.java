package org.colocation.scheduler;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.colocation.ColocationHost;
import org.colocation.ServiceEntity;
import org.colocation.bestEffort.ColocationTask;
import org.workflowsim.scheduling.BaseSchedulingAlgorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by wkj on 2019/3/14.
 * Bubble-up scheduler
 */
public class BUAlgorithm extends BaseSchedulingAlgorithm {
    public BUAlgorithm(double sla){
        super();
        this.sla = sla;
        this.setCloudletList(new ArrayList<>());
        Log.printLine("create BubbleUp scheduler");
    }
    double sla;
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
            List<HostAndValue> avaliableHosts = scoring(t, vmList);
            if (avaliableHosts.size() == 0) {
                //Log.printLine("no host for:"+t.getTaskFullName() );
            }
            for (HostAndValue hav : avaliableHosts) {
                ColocationHost h = hav.getHost();
                double res = t.tryRunningOnHost(h.getId());
                if (res>0) {
                    Log.printLine(CloudSim.clock()+": schedule task #"+t.getTaskFullName()+" to vm:#"+h.getId());
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

    private List scoring(ColocationTask t, List<ColocationHost> vmList){
        List<HostAndValue> res = new ArrayList<>();
        for (ColocationHost h: vmList){
            double minPerf = Double.MAX_VALUE;
            List<ServiceEntity> lcList = h.getLcList();
            for (ServiceEntity se : lcList) {
                double currentPressure = se.getPeerPressure();
                double perf = se.getSensitiveFun().LossByMem(currentPressure+t.getMemBWQuota(), 0);
                if (perf<minPerf){
                    minPerf = perf;
                }
            }
            if (minPerf > sla) {
                res.add(new HostAndValue(h, minPerf));
            }
        }
        res.sort(Comparator.reverseOrder());
        return res;
    }
}
