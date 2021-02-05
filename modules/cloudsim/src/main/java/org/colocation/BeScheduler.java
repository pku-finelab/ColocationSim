package org.colocation;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.colocation.bestEffort.ColocationJob;
import org.colocation.bestEffort.ColocationTask;
import org.colocation.monitor.MonitorEvent;
import org.colocation.scheduler.*;
import org.colocation.uitl.Util;
import org.workflowsim.Task;
import org.workflowsim.scheduling.*;

import java.util.*;

/**
 * Created by wkj on 2019/3/14.
 */
public class BeScheduler extends SimEntity {
    String dcName;
    int userID;
    BaseSchedulingAlgorithm scheduler;
    List<Cloudlet> finishedTasks;
    Map<String, ColocationJob> finishedJobs;
    Map<String, ColocationJob> runningJobs;
    Map<String, ColocationTask> runningTasks;
    List<ColocationTask> schedulableQueue;
    List<Cloudlet> taskQueue;
    String algName;
    private int brokerID;
    private int countScheduleFail;
    private Analysis analysis;
    private double lastScheduleTime;
    private double totalJobNumbers;

    public BeScheduler(String name, String dcName, int userID, int brokerID){
        this(name, Constants.DIAS, dcName, userID, brokerID,-1);
    }
    public BeScheduler(String name, String AlgName, String dcName, int userID, int brokerID, double sla){
        super(name);
        this.brokerID = brokerID;
        this.schedulableQueue = new ArrayList<>();
        this.runningJobs = new HashMap<>();
        this.finishedTasks = new ArrayList<>();
        this.finishedJobs = new HashMap<>();
        this.runningTasks = new HashMap<>();
        this.dcName = dcName;
        this.userID = userID;
        this.taskQueue = new ArrayList<>();
        this.algName = AlgName;
        this.analysis = null;
        this.countScheduleFail = 0;
        Log.printLine("set be schedule alg:" + AlgName);
        switch (AlgName) {
            case Constants.FCFS:
                scheduler = new FCFSAlgorithm();
                break;
            case Constants.MINMIN:
                scheduler = new MinMinSchedulingAlgorithm();
                break;
            case Constants.MAXMIN:
                scheduler = new MaxMinSchedulingAlgorithm();
                break;
            case Constants.ROUNDROBIN:
                scheduler = new RoundRobinAlgorithm();
                break;
            case Constants.PARAGON:
                scheduler = new ParagonAlgorithm();
                break;
            case Constants.LL:
                scheduler = new LLAlgorithm();
                break;
            case Constants.ParagonNI:
                scheduler = new ParagonNIAlgorithm();
                break;
            case Constants.DIAS:
                scheduler = new DIASAlgorithm(sla);
                break;
            case Constants.BUBBLEUP:
                scheduler = new BUAlgorithm(sla);
                break;
            case Constants.COOPER:
                scheduler = new CooperAlgorithm(sla);
                break;
            default:
                scheduler = new StaticSchedulingAlgorithm();
                break;
        }
    }


    public BaseSchedulingAlgorithm getScheduler() {
        return scheduler;
    }

    public void setScheduler(BaseSchedulingAlgorithm scheduler) {
        this.scheduler = scheduler;
    }

    public void setAnalysis(Analysis analysis){
        this.analysis = analysis;
    }

    /**
     * This method is invoked by the class when the simulation is started.
     * It should be responsible for starting the entity up.
     */
    @Override
    public void startEntity() {
        send(this.getId(), 0.5, Constants.RUN_SCHEDULE);
    }

    /**
     * Processes events or services that are available for the entity.
     * deferred queue, which needs to be processed by the entity.
     *
     * @param ev information about the event just happened
     * @pre ev != null
     * @post $none
     */
    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case Constants.SCHEDULE_BE_JOB:
                scheduleBeJob( (ColocationJob) ev.getData());
                break;
            case Constants.COLOCATION_BE_RETURN:
                processTaskReturn(ev);
                break;
            case Constants.VM_UPDATE:
                updateVmList(ev);
                break;
            case Constants.EVICTION:
                reschedule((ColocationTask) ev.getData());
                break;
            case Constants.RUN_SCHEDULE:
                runScheduler();
                break;
            default:
                return;
        }
    }

    public void setTotalJobNumbers(int number) {
        this.totalJobNumbers = number;
    }

    void scheduleBeJob(ColocationJob job) {
        Log.printLine("schedule be job:"+job.getJobName());
        //convert job DAG to queue
        this.runningJobs.put(job.getJobName(), job);
        job.setProgressReporter(this.getId());
        List<Task> tasks = job.getAllTasks();
        this.taskQueue.addAll(tasks);
        // remove tasks in schedulable queue who has scheduled
        // TODO
    }

    void reschedule(ColocationTask task) {
        Log.printLine("reschedule task:"+ task.getTaskFullName());
        this.runningTasks.remove(task.getTaskFullName());
        this.taskQueue.add(task);
        updateScheduableQueue();
        //runScheduler();
    }

    void runScheduler(){
        //Log.printLine(CloudSim.clock()+" RunScheduler");
        try {
            if (finishedJobs.size() != totalJobNumbers){
                send(this.getId(), 0.1, Constants.RUN_SCHEDULE);
            }
            lastScheduleTime = CloudSim.clock();
            this.updateScheduableQueue();
            int taskNum =  this.schedulableQueue.size();
            if (taskNum == 0) {
                return;
            }
            long startTime = System.nanoTime();
            scheduler.run();
            long endTime = System.nanoTime();
            double avgSchTime = (double) (endTime-startTime)/taskNum;
            analysis.addBeResult("ScheduleDur", CloudSim.clock(), avgSchTime);
            // start tasks that successful schedule
            List<ColocationTask> remainTasks = scheduler.getCloudletList();
            List<ColocationTask> toRemoveTask = new ArrayList<>();
            for (ColocationTask t : this.schedulableQueue) {
                if (remainTasks.contains(t)){
                    continue;
                }
                toRemoveTask.add(t);
                this.runningTasks.put(t.getTaskFullName(), t);

                t.setProgressReporter(this.getId());
                // update vm performance
                int vmID = t.getVmId();
                ColocationHost vm = Util.getVM(this.dcName, this.userID, vmID);
                vm.updateUtilization();
                //exec
                double now = CloudSim.clock();
                ColocationJob job = this.runningJobs.get(t.getJobName());
                double jobStart = job.getJobStartTime();
                if ( jobStart <=0 ) {
                    job.setJobStartTime( now );
                } else if (jobStart > now) {
                    job.setJobStartTime(now);
                }
                send(t.getContianerID(), 0, Constants.CONTAINER_START);
            }
            //analysis.addBeResult("Capacity", 0, toRemoveTask.size());
            //CloudSim.abruptallyTerminate();
            if (toRemoveTask.size()==0){
                Log.printLine("no task schedule successfully");
                this.countScheduleFail += 1;
                if (this.countScheduleFail > 200) {
                    Log.printLine("can not schedule tasks, exit");
                    analysis.addMonitorEvent(new MonitorEvent(-1,-1,0L, "CANT_SCHEDULE","CANT_SCHEDULE"));
                    CloudSim.abruptallyTerminate();
                }
            } else {
                this.countScheduleFail = 0;
            }
            // remove tasks that are successful scheduled
            this.schedulableQueue.removeAll(toRemoveTask);
        } catch (Exception e) {
            Log.printLine("Error during scheule Job "+e.toString());
            e.printStackTrace();
        }
    }

    void  updateScheduableQueue(){
        List<ColocationTask> runableTasks = new ArrayList<>();
        for (Cloudlet cloudlet : this.taskQueue) {
            ColocationTask coTask = (ColocationTask) cloudlet;
            if ( coTask.isRunable() ) {
                // add this task to schedulable queue
                coTask.setScheduleTime(CloudSim.clock());
                this.schedulableQueue.add(coTask);
                runableTasks.add(coTask);
            }
        }
        this.taskQueue.removeAll(runableTasks);
        List<ColocationTask> schedulableTasks = new ArrayList<>();
        schedulableTasks.addAll(this.schedulableQueue);
        scheduler.setCloudletList(schedulableTasks);
    }

    void processTaskReturn( SimEvent ev){
        try {
            ColocationTask task = (ColocationTask) ev.getData();
            if (! this.runningTasks.containsKey(task.getTaskFullName())) {
                Log.printLine("Task "+task.getTaskFullName()+" is not running");
                return;
            }
            double now = CloudSim.clock();
            if (now != task.getEstimateFinishTime()) {
                return;
            }
            Log.printLine("task "+task.getTaskFullName()+" finishd");
            task.setFinishTime(CloudSim.clock());
            task.setCloudletStatus(Cloudlet.SUCCESS);
            // remove be task from vm
            int vmID = task.getVmId();
            ColocationHost vm = Util.getVM(dcName, userID, vmID);
            vm.deleteBETask(task);

            this.finishedTasks.add(task);
            String jobID = task.getJobName();
            ColocationJob job = this.runningJobs.get(jobID);
            job.updateStatus();
            if (job.getStatus() == Cloudlet.SUCCESS) {
                this.finishedJobs.put(job.getJobName(),job);
                if (analysis != null ){
                    double start = job.getJobStartTime();
                    double end = job.getJobEndTime();
                    double cost = end - start;
                    analysis.addBeResult(job.getJobName(), job.getJobStartTime(), cost);
                }
            }

            this.runningTasks.remove(task.getTaskFullName());
            updateScheduableQueue();
            //runScheduler();
        } catch (Exception e) {
            Log.printLine("processTaskReturn error:"+e.toString());
            e.printStackTrace();
        }
    }

    public List<ColocationJob> getFinishedJobs(){
        List<ColocationJob> jobs = new ArrayList<>();
        Iterator<String> iter = this.finishedJobs.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            jobs.add(this.finishedJobs.get(key));
        }
        return jobs;
    }

    /**
     * Shuts down the entity.
     * This method is invoked by the {@link } before the simulation finishes. If you want
     * to save data in log files this is the method in which the corresponding code would be placed.
     */
    @Override
    public void shutdownEntity() {
    }

    void updateVmList(SimEvent event){

        List<? extends Vm> vmList = (List<? extends Vm>) event.getData();
        this.scheduler.setVmList(vmList);
        Log.printLine("update vm list for beScheduler, vm num:"+ vmList.size());
    }
}
