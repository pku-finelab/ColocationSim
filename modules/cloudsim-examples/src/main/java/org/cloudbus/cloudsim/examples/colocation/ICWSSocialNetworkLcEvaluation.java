package org.cloudbus.cloudsim.examples.colocation;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.colocation.*;
import org.colocation.bestEffort.ColocationJob;
import org.colocation.bestEffort.ColocationTask;
import org.colocation.bestEffort.JobBuilder;
import org.colocation.lcjob.LcJobBuilder;
import org.colocation.loadgenerator.BeLoadGenerator;
import org.colocation.loadgenerator.LoadGenerator;
import org.colocation.loadgenerator.SimpleBeLoadGenerator;
import org.colocation.qos.QoSConfiguration;
import org.colocation.qos.QosCenter;
import org.colocation.trace.ServiceGraph;
import org.workflowsim.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by wkj on 2019/3/7.
 */
public class ICWSSocialNetworkLcEvaluation {
    public static String DCName = "alicloud";
    public static int USERID = 955;
    public static double beStartAt = 110;
    public static int beRepeat = 20;
    public static int beRepeatInterval = 5;
    public static int jobNumOnce = 5;
    public static int vmNum = 33;
    static String lcPath = "modules/cloudsim-examples/src/main/java/org/cloudbus/cloudsim/examples/colocation/socialnetwork.json";
    static String bePath = "modules/cloudsim-examples/src/main/java/org/cloudbus/cloudsim/examples/colocation/beJobs-sf-mbw.json";
    public static double sla = 0.85;

    static String ALG = Constants.PARAGON;
    static String outputPath = "/Users/kangjin/sim-out";

    // VM description
    public static int mips = 1000;
    public static long size = 10000; // image size (MB)
    public static long ram = 64 * Constants.GB; // vm memory
    public static long bw = 900;
    public static int pesNumber = 1; // number of cpus
    public static String vmm = "Xen"; // VMM name
    public static double memBandwidth = 35.0;

    public static void main(String[] args){
        long start = System.currentTimeMillis();
        int num_user = 1;
        Calendar calendar = Calendar.getInstance();
        CloudSim.init(num_user, calendar, false);
        Datacenter datacenter = createDatacenter(DCName);
        ColocationDatacenterBroker broker = createBroker();
        int totalBENumbers= beRepeat*jobNumOnce;
        broker.setBeSchedulerExitNumber(totalBENumbers);

        broker.setOutput(false);
        QosCenter qosCenter = new QosCenter();

        int brokerId = broker.getId();
        List<Vm> vmlist = null;
        vmlist = new ArrayList<Vm>();

        for (int vmId = 0; vmId < vmNum; vmId++) {
            Vm vm = new ColocationHost(vmId, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared(), 0.5,0.9, memBandwidth, Constants.CPUSHARE);
            vmlist.add(vm);
        }

        broker.submitVmList(vmlist);
        USERID = brokerId;

        // gen services
        List<ServiceEntity> serviceList;
        LcJobBuilder lcBuilder = new LcJobBuilder(DCName, USERID, lcPath);
        serviceList = lcBuilder.getServices();
        for (int i = 0; i < serviceList.size(); i++) {
            serviceList.get(i).setBrokerID(brokerId);
        }
        broker.setLcServicesForScheduler(serviceList);

        List<QoSConfiguration> qoSConfigurations = lcBuilder.getQosConfigurations();
        for (int i = 0; i < qoSConfigurations.size(); i++) {
            qosCenter.addQosConfig(qoSConfigurations.get(i));
        }
        broker.setQosCenter(qosCenter);
        ServiceGraph sg = new ServiceGraph(qosCenter);
        broker.setGraphServiceForScheduler(sg);


        LoadGenerator lcLoader = lcBuilder.getLoadGenerator();
        broker.setLcLoadGenerator(lcLoader);

        JobBuilder builder = new JobBuilder(DCName, USERID, bePath);
        BeLoadGenerator beLoader = new SimpleBeLoadGenerator(beStartAt, brokerId, beRepeat, beRepeatInterval, jobNumOnce, builder );
        broker.setBeLoadGenerator(beLoader);

        // init Scheduler
        Scheduler scheduler = new Scheduler("simple", DCName);
        broker.bindScheduler(scheduler);
        double lastClock = CloudSim.startSimulation();
        CloudSim.stopSimulation();

        long end = System.currentTimeMillis();
        double cost = end - start;
        //printRequests(broker.getReturnedRequests());
        //printJob(broker.getFinishedJobs());
        broker.printReport();
        Log.printLine("Simulation cost: " + cost);
    }

    private static ColocationDatacenterBroker createBroker() {
        ColocationDatacenterBroker broker = null;
        try {
            broker = new ColocationDatacenterBroker("ColocationBroker", DCName, ALG, outputPath, sla);
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


        // 3. Create PEs and add these into a list.
        peList.add(new Pe(0, new PeProvisionerSimple(mips+100))); // need to store Pe id and MIPS Rating

        // 4. Create Host with its id and list of PEs and add them to the list
        // of machines
        long storage = 1000000; // host storage


        for (int hostId = 0; hostId < vmNum; hostId++) {
            hostList.add(
                    new Host(
                            hostId,
                            new RamProvisionerSimple((int)((ram*1.1)/Constants.MB)),
                            new BwProvisionerSimple(bw+10),
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
                //Log.printLine(log.toString());
            }
        }
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
                Log.printLine(ct.getTaskFullName()+indent+tCost+indent+tStart+indent+tEnd);
            }
            Log.printLine();
        }
        double avg = jobSum/jobs.size();
        Log.printLine("avg cost time:"+avg);
    }
}
