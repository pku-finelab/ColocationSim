package org.colocation.bestEffort;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.colocation.Program;
import org.workflowsim.Task;

import java.util.*;

/**
 * Created by wkj on 2019/3/12.
 */
public class ColocationJob extends  ColocationTask{
    private double jobStartTime;
    private double jobEndTime;
    private List<ColocationTask> rootTask;
    private Map<String, ColocationTask> taskMap;
    public ColocationJob(String jobName, final int jobId, double cpuQuota, int ramQuota, double memBWQuota, String
            DatacenterName, int userID) {
        super(jobName, jobName, jobId, cpuQuota, ramQuota, memBWQuota, DatacenterName, userID, -1, new Program(), null);
        this.taskMap = new HashMap<>();
        this.rootTask = new ArrayList<>();
    }

    public List<ColocationTask> getRootTaskList() {
        return rootTask;
    }

    public void addRootTask(ColocationTask t) {
        this.rootTask.add(t);
    }

    public void setJobName(String jobName) {
        super.setTaskName(jobName);
        super.setJobName(jobName);

        //reset all tasks' name
        for (Map.Entry<String, ColocationTask> entry : taskMap.entrySet()) {
            ColocationTask task = entry.getValue();
            task.setJobName(jobName);
        }
    }
    public String getJobName(){
        return super.getJobName();
    }
    public void addTask(ColocationTask t) {
        //Log.printLine("add task:"+t.getTaskName());
        this.taskMap.put(t.getTaskName(), t);
    }

    public ColocationTask getTask(String taskName) {
        return this.taskMap.get(taskName);
    }

    @Override
    public void setProgressReporter(int progressReporter) {
        super.setProgressReporter(progressReporter);
        taskMap.entrySet();
        for (Map.Entry<String, ColocationTask> entry : taskMap.entrySet()) {
            ColocationTask task = entry.getValue();
            task.setProgressReporter(progressReporter);
        }
    }

    public double getJobStartTime() {
        return jobStartTime;
    }

    public void setJobStartTime(double jobStartTime) {
        this.jobStartTime = jobStartTime;
    }

    public double getJobEndTime() {
        return jobEndTime;
    }

    public void setJobEndTime(double jobEndTime) {
        this.jobEndTime = jobEndTime;
    }

    @Override
    public List getParentList() {
        return super.getParentList();
    }

    public List<Task> getAllTasks(){
        List<Task> res = new ArrayList<>();
        Queue<Task> queue = new LinkedList<>();
        for (int i = 0; i < this.rootTask.size(); i++) {
            queue.add(this.rootTask.get(i));
        }

        while (queue.size()>0) {
            Task i = queue.remove();
            if (!res.contains(i)) {
                res.add(i);
            }
            for (Task t : i.getChildList()){
                if ( !queue.contains(t) ){
                    queue.add(t);
                }
            }
        }
        return res;
    }

    public void updateStatus(){
        List<Task> tasks = this.getAllTasks() ;
        boolean allSuccess = true;
        boolean hasTaskFailed = false;
        boolean hasExec = false;
        try {
            for( Task t : tasks) {
                switch (t.getStatus()) {
                    case FAILED:
                        allSuccess = false;
                        hasTaskFailed = true;
                        break;
                    case INEXEC:
                        allSuccess = false;
                        hasExec = true;
                        break;
                    case SUCCESS:
                        break;
                    default:
                        allSuccess = false;
                }
            }
            if (hasTaskFailed) {
                this.setCloudletStatus(FAILED);
            }
            if (allSuccess) {
                this.setCloudletStatus(SUCCESS);
                double now = CloudSim.clock();
                this.setJobEndTime(now);
            }
            if (hasExec){
                this.setCloudletStatus(INEXEC);
            }
        } catch (Exception e) {
            Log.printLine("update job status error:"+ e.toString());
            e.printStackTrace();
        }
    }
}
