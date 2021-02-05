package org.cloudbus.cloudsim.examples.colocation;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.colocation.*;
import org.colocation.bestEffort.ColocationJob;
import org.colocation.bestEffort.ColocationTask;
import org.colocation.loadgenerator.LoadGenerator;
import org.colocation.loadgenerator.NumberableRequestGenerator;
import org.colocation.pressureFunction.LinearPressureFunction;
import org.colocation.qos.EntrypointAPI;
import org.colocation.qos.QosCenter;
import org.colocation.sensitiveFunction.AbstractSensitiveFunction;
import org.colocation.sensitiveFunction.LinearSensitiveFunction;
import org.colocation.sensitiveFunction.SensitiveFunction1;
import org.colocation.trace.ServiceGraph;
import org.workflowsim.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by wkj on 2019/3/7.
 */
public class CloudSimColocationExample {
    public static String DCName = "alicloud";
    public static int USERID = 955;
    public static int vmNum = 6;
    public static String APP = "web1";
    public static double memBandwidth = 12800 ; //DDR2
    public static int CPUSHARE_WEIGHT = 100;

    public static void main(String[] args){
        int num_user = 1;
        Calendar calendar = Calendar.getInstance();
        CloudSim.init(num_user, calendar, false);
        Datacenter datacenter = createDatacenter(DCName);

        QosCenter qosCenter = new QosCenter();

        ServiceGraph sg = new ServiceGraph(qosCenter);
        ColocationDatacenterBroker broker = createBroker(sg);

        int brokerId = broker.getId();
        USERID = brokerId;
        List<Vm> vmlist = null;
        vmlist = new ArrayList<Vm>();

        // VM description

        int mips = 49360;//49360;  // i7
        long size = 10000; // image size (MB)
        int ram = 16384; // vm memory (MB) =16GB
        long bw = 1024;   // net bandwidth 1GBps
        int pesNumber = 1; // number of cpus
        String vmm = "Xen"; // VMM name

        for (int vmId = 0; vmId < vmNum; vmId++) {
            Vm vm = new ColocationHost(vmId, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared(), 0.5,0.9, memBandwidth, Constants.CPUSHARE);
            vmlist.add(vm);
        }

        broker.submitVmList(vmlist);

        int jobNum = 10;

        List<ColocationJob> jobs = createJobList(jobNum);
        List<ServiceEntity> services = genServices(broker);
        broker.setBeJobsForSchedule(jobs);
        broker.setLcServicesForScheduler(services);


        ServiceEntity service1 = null;
        for(ServiceEntity se: services) {
            if (se.getName() == "service1") {
                service1 = se;
                qosCenter.setService(APP, se.getName(), 0.3, 100);
            }
        }

        double lcStartAt = 100;
        EntrypointAPI api = new EntrypointAPI(service1.getName(), "api1");
        LoadGenerator lcLoader = new NumberableRequestGenerator(broker.getId(), api, lcStartAt, 3);
        broker.setLcLoadGenerator(lcLoader);

        broker.setWorkloadConfig(lcStartAt, lcStartAt + 100, 1);

        // init Scheduler
        Scheduler scheduler = new Scheduler("simple", DCName);
        broker.bindScheduler(scheduler);
        CloudSim.startSimulation();
        CloudSim.stopSimulation();
        List<ColocationJob> job_return = broker.getFinishedJobs();
        List<Request> requests = broker.getReturnedRequests();
        printJob(job_return);
        CloudSimColocationLCExample.printRequests(requests);

    }

    private static ColocationDatacenterBroker createBroker(ServiceGraph graphService) {
        ColocationDatacenterBroker broker = null;
        try {
            broker = new ColocationDatacenterBroker("ColocationBroker", DCName, Constants.DIAS);
            broker.setGraphServiceForScheduler(graphService);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    private static List<ColocationJob> createJobList(int jobNum){
        List<ColocationJob> jobs = new ArrayList<>();
        long taskLength = 1000000;
        double cpuQuota = 1.5;
        int ramQuota = 512;
        double memBWQuota = 250;
        Program program = new Program();
        program.add(new Procedure(taskLength));
        LinearSensitiveFunction beFun = new LinearSensitiveFunction();
        for (int i = 0; i< jobNum; i++) {
            String jobid = "JOB_" + i;
            ColocationTask task2 = new ColocationTask(jobid, "t2", i*10+2, cpuQuota, ramQuota, memBWQuota, DCName, USERID, CPUSHARE_WEIGHT, program, beFun);
            ColocationTask task1 = new ColocationTask(jobid, "t1", i*10+1, cpuQuota, ramQuota, memBWQuota, DCName, USERID, CPUSHARE_WEIGHT, program, beFun);
            ColocationTask task3 = new ColocationTask(jobid, "t3", i*10+3, cpuQuota, ramQuota, memBWQuota, DCName, USERID, CPUSHARE_WEIGHT, program, beFun);
            ColocationTask task4 = new ColocationTask(jobid, "t4", i*10+4, cpuQuota, ramQuota, memBWQuota, DCName, USERID, CPUSHARE_WEIGHT, program, beFun);
            ColocationTask task5 = new ColocationTask(jobid, "t5", i*10+5, cpuQuota, ramQuota, memBWQuota, DCName, USERID, CPUSHARE_WEIGHT, program, beFun);
            task2.addParent( task1);
            task3.addParent( task1);
            task1.addChild( task2);
            task1.addChild( task3);
            task3.addChild(task4);
            task4.addParent(task3);
            task4.addChild(task5);
            task2.addChild(task5);
            task5.addParent(task2);
            task5.addParent(task4);
            ColocationJob job1 = new ColocationJob(jobid, i*10, cpuQuota, ramQuota, memBWQuota, DCName, USERID);
            job1.addRootTask(task1);
            job1.addTask(task1);
            job1.addTask(task2);
            job1.addTask(task3);
            job1.addTask(task4);
            job1.addTask(task5);
            jobs.add(job1);
        }

        return jobs;
        //return new ArrayList<>();
    }

    private static List genServices(ColocationDatacenterBroker broker){
        // gen services
        List<ServiceEntity> serviceList;
        serviceList = new ArrayList<>();

        //service2
        ServiceEntity service2;
        Program program2 = new Program();
        ArrayList<String> pragram2Dep = new ArrayList<>();
        program2.add(new Procedure(1000, pragram2Dep, false, 0));
        AbstractSensitiveFunction senFun = new SensitiveFunction1();
        service2 = new ServiceEntity("service2", APP, 100, 2.0, DCName, USERID, 800, program2, senFun, CPUSHARE_WEIGHT);
        service2.setMemPressureFun(new LinearPressureFunction(200,20));
        serviceList.add(service2);

        //service3
        ServiceEntity service3;
        Program program3 = new Program();
        program3.add(new Procedure(3000, new ArrayList<>(), false, 0));
        service3 = new ServiceEntity("service3", APP, 100, 0.5, DCName, USERID, 1024, program3, senFun, CPUSHARE_WEIGHT);
        service3.setMemPressureFun(new LinearPressureFunction(200,20));
        serviceList.add(service3);

        //service4
        ServiceEntity service4;
        Program program4 = new Program();
        program4.add(new Procedure(4000, new ArrayList<>(), false, 0));
        service4 = new ServiceEntity("service4", APP, 100, 0.5, DCName, USERID, 1024, program4, senFun, CPUSHARE_WEIGHT);
        service4.setMemPressureFun(new LinearPressureFunction(200,20));
        serviceList.add(service4);

        //service5
        ServiceEntity service5;
        Program program5 = new Program();
        program5.add(new Procedure(4000, new ArrayList<>(), false, 0));
        service5 = new ServiceEntity("service5", APP, 100, 0.5, DCName, USERID, 1024, program5, senFun, CPUSHARE_WEIGHT);
        service5.setMemPressureFun(new LinearPressureFunction(200,20));
        serviceList.add(service5);

        //service1
        ServiceEntity service1;
        Program program1 = new Program();
        program1.add(new Procedure(1000, new ArrayList<>(), false, 0));
        List<String> depService = new ArrayList<>();
        depService.add(service2.getName());
        depService.add(service3.getName());
        depService.add(service4.getName());
        program1.add(new Procedure(0, depService, false, depService.size()));
        program1.add(new Procedure(2000, new ArrayList<>(), false, 0));
        List<String> dep5 = new ArrayList<>();
        dep5.add(service5.getName());
        program1.add(new Procedure(0,dep5, false, dep5.size()));
        program1.add(new Procedure(3000, new ArrayList<>(), false, 0));
        service1 = new ServiceEntity("service1", APP,100, 2.0, DCName, USERID, 800, program1, senFun, CPUSHARE_WEIGHT);
        service1.setMemPressureFun(new LinearPressureFunction(200,20));
        serviceList.add(service1);

        return serviceList;
    }

    private static Datacenter createDatacenter(String name) {

        // Here are the steps needed to create a PowerDatacenter:
        // 1. We need to create a list to store
        // our machine
        List<Host> hostList = new ArrayList<Host>();

        // 2. A Machine contains one or more PEs or CPUs/Cores.
        // In this example, it will have only one core.
        List<Pe> peList = new ArrayList<Pe>();

        int mips = 50000;

        // 3. Create PEs and add these into a list.
        peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
        //peList.add(new Pe(1, new PeProvisionerSimple(mips)));
        //peList.add(new Pe(2, new PeProvisionerSimple(mips)));
        //peList.add(new Pe(3, new PeProvisionerSimple(mips)));

        // 4. Create Host with its id and list of PEs and add them to the list
        // of machines
        int ram = 71680; // host memory (MB)
        long storage = 1000000; // host storage
        int bw = 10000;


        for (int hostId = 0; hostId < vmNum; hostId++) {
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
                double tStart = ct.getExecStartTime();
                double tEnd = ct.getFinishTime();
                double tCost = tEnd - tStart;
                Log.printLine(ct.getCloudletId()+indent+tCost+indent+tStart+indent+tEnd);
            }
            Log.printLine();
        }
        double avg = jobSum/jobs.size();
        Log.printLine("avg cost time:"+avg);
    }
}
