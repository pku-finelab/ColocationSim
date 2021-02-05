package org.colocation.scheduler;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.colocation.ColocationHost;
import org.colocation.ServiceEntity;
import org.colocation.scheduler.mcr.ServicePathSlack;
import org.colocation.trace.ServiceGraph;
import org.colocation.bestEffort.ColocationTask;
import org.colocation.bestEffort.TaskResourceComparator;
import org.colocation.uitl.Util;
import org.workflowsim.scheduling.BaseSchedulingAlgorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by wkj on 2019/3/14.
 */
public class DIASAlgorithm extends BaseSchedulingAlgorithm {

    private ServiceGraph graphInfo;
    private double sla;
    public DIASAlgorithm(double sla){
        super();
        this.sla = sla;
        this.setCloudletList(new ArrayList<>());
        Log.printLine("create cpas scheduler");
    }

    public void setServiceGraph(ServiceGraph graphInfo) {
        this.graphInfo = graphInfo;
    }
    /**
     * The main function
     *
     * @throws Exception
     */
    @Override
    public void run() throws Exception {
        // remove success scheduled tasks
        List<ColocationHost> vmList = getVmList();
        List<ColocationTask> taskToRemove = new ArrayList<>();
        List<ColocationTask> tasks = this.getCloudletList();
        // Step0. sort task by resource requirements
        sortByBwQuota(tasks);


        for(ColocationTask t : tasks) {
            // for every task in task list
            // Step1. filter vm by quota, bandwidth internal quota and sort

            List<MaxContributedResource> allowedVms = filter3(t, vmList);
            // Step2. schedule task to 1st available vm

            boolean success = false;
            for (MaxContributedResource mcr: allowedVms){
                ColocationHost vm = mcr.getVm();
                int res = t.tryRunningOnHost(vm.getId());
                if ( res > 0 ) {
                    taskToRemove.add(t);
                    success = true;
                    Log.printLine(CloudSim.clock()+": schedule task #"+t.getTaskFullName()+" to vm:#"+vm.getId());
                    break;
                }
            }
            if (!success) {
                Log.printLine("no host for task " + t.getTaskFullName());
            }

        }
        tasks.removeAll(taskToRemove);
        this.setCloudletList(tasks);
    }

    void sortByBwQuota(List<ColocationTask> tasks ) {
        Comparator cmp = new TaskResourceComparator();
        tasks.sort(cmp);
    }

    List<MaxContributedResource> filter(ColocationTask t, List<ColocationHost> vms) {
        List<MaxContributedResource> allowed = new ArrayList<>();
        for (ColocationHost vm: vms) {


            double lcBwInterval = Double.POSITIVE_INFINITY;
            for (ServiceEntity se : vm.getLcList()) {
                // get the min bw interval of all online services in this vm
                double intevalTime = this.graphInfo.getWorstQosIntervalTime(se);
                double currTime = this.graphInfo.getCurrTimeOfService(se);
                double memBwInterval = se.convertTimeToBW(currTime, intevalTime);
                if ( memBwInterval < lcBwInterval) {
                    lcBwInterval = memBwInterval;
                }
            }
            if (t.getMemBWQuota() < lcBwInterval) {
                // add vm
                allowed.add(new MaxContributedResource(vm, lcBwInterval));
            }
        }
        allowed.sort(new VmIntervalCmp());
        return allowed;
    }

    List<MaxContributedResource> filter2(ColocationTask t, List<ColocationHost> vms) {
        List<MaxContributedResource> allowed = new ArrayList<>();
        for (ColocationHost vm: vms) {

            double mcr = getMCR(vm);

            if (t.getMemBWQuota() < mcr) {
                // add vm
                allowed.add(new MaxContributedResource(vm, mcr));
            }
        }
        allowed.sort(new VmIntervalCmp());
        return allowed;
    }

    private double getMCR(ColocationHost host) {
        // step1. get paths of this host ; get RT slack list of all paths, generate equation set Replace node to sensitive function
        ArrayList<ServicePathSlack> polynomial = graphInfo.getPathAndSlack(host);

        // find the result by binary search
        double res = solve(polynomial, 0, (host.getTotalMemBW() - host.getCurrentBW()) );
        return res;
    }

    public double solve(ArrayList<ServicePathSlack> equationSet, double min, double max) {
        // TODO: binary search

        double precision = 0.001;
        int maxIter = 50;
        int iterationTimes = 0;
        double res=0;

        while ( (max - min)>precision && (iterationTimes < maxIter) ) {
            double mid = (max-min)/2 + min;
            res = mid;
            boolean allEstabilish = true;
            for (int i = 0; i < equationSet.size(); i++) {
                ServicePathSlack sps = equationSet.get(i);
                allEstabilish = allEstabilish && sps.canEstablishedBy(mid);
            }
            if (allEstabilish) {
                // the result maybe bigger
                min = mid;
            } else {
                // the result maybe smaller
                max = mid;
            }
            iterationTimes = iterationTimes + 1;
            //Log.printLine("iter Num:"+iterationTimes+" Min:"+min+" Max:"+max);
        }

        return res;
    }

    List<MaxContributedResource> filter3(ColocationTask t, List<ColocationHost> hosts) {
        hosts = Util.filterHostByQuota(hosts, t);
        List<MaxContributedResource> allowed = new ArrayList<>();
        for (ColocationHost h: hosts) {
            double rtSlack = getRTSlack(t, h);

            if (rtSlack > 0) {
                // add vm
                allowed.add(new MaxContributedResource(h, rtSlack));
            }
        }
        allowed.sort(new VmIntervalCmp());
        return allowed;
    }

    private double getRTSlack(ColocationTask t, ColocationHost host) {
        // step1. get paths of this host ; get RT slack list of all paths, generate equation set Replace node to sensitive function
        ArrayList<ServicePathSlack> polynomial = graphInfo.getPathAndSlack2(host, this.sla);
        double minSlack = Double.MAX_VALUE;
        // find the min RT slack
        for (ServicePathSlack sps : polynomial) {
            double pathSlack = sps.getSlackAfter(t.getMemBWQuota());
            if (pathSlack < minSlack) {
                minSlack = pathSlack;
            }
        }
        return minSlack;
    }


    class MaxContributedResource {
        ColocationHost vm;
        double interval;

        MaxContributedResource(ColocationHost vm, double interval) {
            this.vm = vm;
            this.interval = interval;
        }

        public double getInterval() {
            return interval;
        }

        public ColocationHost getVm() {
            return vm;
        }
    }

    class VmIntervalCmp implements Comparator{
        @Override
        public int compare(Object o1, Object o2) {
            MaxContributedResource vm1 = (MaxContributedResource) o1;
            MaxContributedResource vm2 = (MaxContributedResource) o2;
            double diff = vm1.getInterval() - vm2.getInterval();
            if ( diff>0 ){
                return -1;
            } else if ( diff <0){
                return 1;
            }
            return 0;
        }
    }

}
