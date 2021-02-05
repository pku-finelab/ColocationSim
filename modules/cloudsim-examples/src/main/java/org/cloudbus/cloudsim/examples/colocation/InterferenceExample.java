package org.cloudbus.cloudsim.examples.colocation;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.colocation.*;
import org.colocation.bestEffort.ColocationJob;
import org.colocation.bestEffort.ColocationTask;
import org.colocation.pressureFunction.LinearPressureFunction;
import org.colocation.sensitiveFunction.AbstractSensitiveFunction;
import org.colocation.sensitiveFunction.LinearSensitiveFunction;
import org.colocation.uitl.Util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by wkj on 2019/3/7.
 */
public class InterferenceExample {
    public static String DCName = "alicloud";
    public static int USERID = 955;
    public static String APP = "web";
    public static int LcCpuShare = 100;


    public static void main(String[] args){
        int num_user = 1;
        Calendar calendar = Calendar.getInstance();
        CloudSim.init(num_user, calendar, false);
        Datacenter datacenter = createDatacenter(DCName);
        ColocationDatacenterBroker broker = createBroker();

        int brokerId = broker.getId();
        List<Vm> vmlist = null;
        vmlist = new ArrayList<Vm>();

        // VM description
        int vmNum = 1;
        int mips = 1000;
        long size = 10000; // image size (MB)
        int ram = 2000; // vm memory (MB)
        long bw = 900;
        int pesNumber = 1; // number of cpus
        String vmm = "Xen"; // VMM name
        double memBandwidth = 1000.0;


        for (int vmId = 0; vmId < vmNum; vmId++) {
            Vm vm = new ColocationHost(vmId, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared(), 0.5,0.9, memBandwidth, Constants.CPUSHARE);
            vmlist.add(vm);
        }

        broker.submitVmList(vmlist);
        USERID = brokerId;
        // gen services
        List<ServiceEntity> serviceList;
        serviceList = new ArrayList<>();

        //service2
        ServiceEntity service2;
        Program program2 = new Program();
        ArrayList<String> pragram2Dep = new ArrayList<>();
        program2.add(new Procedure(10000, pragram2Dep, false, 0));
        AbstractSensitiveFunction senFun = new LinearSensitiveFunction();
        service2 = new ServiceEntity("service2", APP,100, 2.0, DCName, USERID, 800, program2, senFun, LcCpuShare);
        service2.setMemPressureFun(new LinearPressureFunction(200,20));
        serviceList.add(service2);


        broker.setLcServicesForScheduler(serviceList);


        List<ColocationJob> jobs = createJobList(1);
        broker.setBeJobsForSchedule(jobs);
        broker.setWorkloadConfig(100, 95, 1);


        // init Scheduler
        Scheduler scheduler = new Scheduler("simple", DCName);
        broker.bindScheduler(scheduler);
        CloudSim.startSimulation();
        CloudSim.stopSimulation();
        List<Request> requests = broker.getReturnedRequests();
        printRequests(requests);
        Util.printJob(jobs);
    }

    private static ColocationDatacenterBroker createBroker() {
        ColocationDatacenterBroker broker = null;
        try {
            broker = new ColocationDatacenterBroker("ColocationBroker",DCName);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }



    private static Datacenter createDatacenter(String name) {

        // Here are the steps needed to create a PowerDatacenter:
        // 1. We need to create a list to store
        // our machine
        List<Host> hostList = new ArrayList<Host>();

        // 2. A Machine contains one or more PEs or CPUs/Cores.
        // In this example, it will have only one core.
        List<Pe> peList = new ArrayList<Pe>();

        int mips = 1200;

        // 3. Create PEs and add these into a list.
        peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating

        // 4. Create Host with its id and list of PEs and add them to the list
        // of machines
        int ram = 2048; // host memory (MB)
        long storage = 1000000; // host storage
        int bw = 10000;

        int hostNum = 1;

        for (int hostId = 0; hostId < hostNum; hostId++) {
            hostList.add(
                    new Host(
                            hostId,
                            new RamProvisionerSimple(ram),
                            new BwProvisionerSimple(bw),
                            storage,
                            peList,
                            new VmSchedulerTimeShared(peList)
                    )
            ); // This is our machine
        }



        // 5. Create a DatacenterCharacteristics object that stores the
        // properties of a data center: architecture, OS, list of
        // Machines, allocation policy: time- or space-shared, time zone
        // and its price (G$/Pe time unit).
        String arch = "x86"; // system architecture
        String os = "Linux"; // operating system
        String vmm = "Xen";
        double time_zone = 10.0; // time zone this resource located
        double cost = 3.0; // the cost of using processing in this resource
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.001; // the cost of using storage in this
        // resource
        double costPerBw = 0.0; // the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
        // devices by now

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        // 6. Finally, we need to create a PowerDatacenter object.
        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    private static List<ColocationJob> createJobList(int jobNum){
        List<ColocationJob> jobs = new ArrayList<>();
        long taskLength = 50000;
        double cpuQuota = 1.5;
        int ramQuota = 512;
        double memBWQuota = 500;
        Program program = new Program();
        Procedure p = new Procedure(taskLength, null, false, 0);
        program.add(p);
        int cpushare = 100;
        LinearSensitiveFunction beFun = new LinearSensitiveFunction();
        for (int i = 0; i< jobNum; i++) {
            String jobid = "JOB_" + i;
            ColocationTask task1 = new ColocationTask(jobid, "t1", i*10+1, cpuQuota, ramQuota, memBWQuota, DCName, USERID,cpushare,
                    program,beFun);
            ColocationJob job1 = new ColocationJob(jobid, i*10, cpuQuota, ramQuota, memBWQuota, DCName, USERID);
            job1.addRootTask(task1);
            jobs.add(job1);
        }

        return jobs;
    }

    public static void printRequests(List<Request> requests) {
        int size = requests.size();
        Request req;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Request ID" + indent + "Cost" + indent +
                "Start Time" + indent + "Finish Time");
        for (int i=0; i < size; i++) {
            req = requests.get(i);
            double cost = req.getEndTime() - req.getStartTime();
            Log.printLine(req.getId()+indent+cost+indent+req.getStartTime()+indent+req.getEndTime());
            List<RequestLog> logs = req.getRequestLogs();
            for (int j = 0; j < logs.size(); j++) {
                RequestLog log = logs.get(j);
                Log.printLine(log.toString());
            }
        }
    }
}
