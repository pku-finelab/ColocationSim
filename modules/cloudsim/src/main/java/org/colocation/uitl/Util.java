package org.colocation.uitl;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.colocation.ColocationHost;
import org.colocation.bestEffort.ColocationJob;
import org.colocation.bestEffort.ColocationTask;
import org.workflowsim.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wkj on 2019/3/13.
 */
public class Util {
    public static ColocationHost getVM(String dcName, int userID, int vmID){
        Datacenter dc = (Datacenter) CloudSim.getEntity(dcName);
        Host host = dc.getVmAllocationPolicy().getHost(vmID, userID);
        //Log.printLine("VmMIPS Clock"+CloudSim.clock()+", vmid:"+this.VMId);
        if (host==null) {
            Log.printLine("host is null");
        }
        ColocationHost vm = (ColocationHost) host.getVm(vmID, userID);
        return vm;
    }
    public static void printJob(List<ColocationJob> jobs) {
        int size = jobs.size();
        ColocationJob job;
        double jobSum = 0;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("job ID" + indent + "Cost" + indent +
                "Start Time" + indent + "Finish Time");
        for (int i=0; i < size; i++) {
            job = jobs.get(i);
            double start = job.getJobStartTime();
            double end = job.getJobEndTime();
            double cost = end - start;
            jobSum = cost+jobSum;
            Log.printLine(job.getJobName()+indent+cost+indent+start+indent+end);
            List<Task> tasks = job.getAllTasks();
            for(Task t: tasks) {
                ColocationTask ct = (ColocationTask) t;
                double tStart = ct.getStartTime();
                double tEnd = ct.getFinishTime();
                double tCost = tEnd - tStart;
                Log.printLine(ct.getTaskName()+indent+tCost+indent+tStart+indent+tEnd);
            }
            Log.printLine();
        }
        double avg = jobSum/jobs.size();
        Log.printLine("avg cost time:"+avg);
    }

    public static void linearInterpolation(double[] arrSrc, double[] arrDest) {
        if (arrSrc.length<=0 || arrDest.length <= 0) {
            Log.printLine("the size of interpolation array need bigger than 0");
            return;
        }
        float step = (float) arrDest.length/arrSrc.length;
        for (int i = 0; i < arrDest.length; i++) {
            //step#1 find SRC index and value
            int srcX1 = (int) Math.floor(i / step);
            double y1 = arrSrc[srcX1];

            //step#2 find src index2 and value2

            int srcX2 = srcX1+1;
            double y2;
            if (srcX2 >= arrSrc.length) {
                y2= arrSrc[0];
            } else {
                y2 = arrSrc[srcX2];
            }

            //step#3 find dest x1 and x2
            int x1 = (int) Math.floor(srcX1 * step);
            int x2 = (int) Math.floor(srcX2 * step);

            double value = ((y2 - y1)/(x2 - x1))*(i - x1) + y1;
            arrDest[i] = value;
        }
    }

    public static List<Double> averageBySeg(List<Double> arr, int segLen) {
        int segNum = (int)(arr.size()/segLen) ;
        ArrayList<Double> res = new ArrayList(segNum);
        for (int i = 0; i < segNum; i++) {
            int start = i*segLen;
            int end = start + segLen;
            if (end > arr.size()) {
                end = arr.size();
            }
            //average from start to end
            double sum = 0.0;
            int dount = end-start ;
            for ( ; start < end ; start++) {
                sum += arr.get(start);
            }
            res.add(sum/dount);
        }
        return  res;
    }

    public static List<ColocationHost> filterHostByQuota(List<ColocationHost> hosts, ColocationTask task){
        ArrayList<ColocationHost> res = new ArrayList<>();
        for( ColocationHost host: hosts) {
            double availCPU = host.getAvailableCPU();
            double avialMem = host.getAvailableRam();
            if ( (task.getMemBWQuota()< avialMem) && (task.getCpuQuota() < availCPU)){
                res.add(host);
            }
        }
        return res;
    }
}
