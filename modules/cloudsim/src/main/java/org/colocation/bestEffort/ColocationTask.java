package org.colocation.bestEffort;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.colocation.ColocationHost;
import org.colocation.Program;
import org.colocation.sensitiveFunction.AbstractSensitiveFunction;
import org.colocation.uitl.Util;
import org.workflowsim.Task;



/**
 * Created by wkj on 2019/3/12.
 */
public class ColocationTask extends Task {
    private double memBWUsage;
    private double memBWQuota;
    private long ramQuota;
    private double cpuQuota;
    private String dcName;
    private int userID;
    private String jobName;
    private AbstractSensitiveFunction perf;
    private ColocationHost vm;
    private int ContianerID;
    private int cpushare;
    private Program program;
    private int progressReporter;
    private String taskName;

    private double firstScheduleTime;
    private double lastStartTime;
    private double finishTime;
    private double estimateFinishTime;

    public ColocationTask(String jobName, String taskName, final int taskId, double cpuQuota, long ramQuota, double memBWQuota, String
            DatacenterName, int userID, int cpushare, Program program, AbstractSensitiveFunction fun){
        super(taskId, program.getAllInstructionNum());
        this.jobName = jobName;
        this.taskName = taskName;
        this.cpuQuota = cpuQuota;
        this.ramQuota = ramQuota;
        this.memBWQuota = memBWQuota;
        this.memBWUsage = memBWQuota;
        this.dcName = DatacenterName;
        this.userID = userID;
        this.cpushare = cpushare;
        this.program = program;
        this.perf = fun;

        this.firstScheduleTime = -1;
        this.lastStartTime = -1;
        this.finishTime = -1;
    }


    public String getTaskName() {
        return this.taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public void setJobName(String newJobName) {
        this.jobName = newJobName;
    }

    public String getTaskFullName(){
        return this.jobName +":"+this.taskName;
    }

    public double getEstimateFinishTime() {
        return estimateFinishTime;
    }

    public void setEstimateFinishTime(double estimateFinishTime) {
        this.estimateFinishTime = estimateFinishTime;
    }

    public String getTaskUniName() {
        return  this.taskName+"_"+this.lastStartTime;
    }

    public double getFirstScheduleTime() {
        return firstScheduleTime;
    }

    public void setScheduleTime(double firstScheduleTime) {
        if (firstScheduleTime < 0) {
            this.firstScheduleTime = firstScheduleTime;
        }
    }

    public double getStartTime() {
        return lastStartTime;
    }

    public void setStartTime(double lastStartTime) {
        if (lastStartTime > this.lastStartTime) {
            this.lastStartTime = lastStartTime;
        }
    }

    public double getCpuUsage(){
        return this.cpuQuota;
    }

    public double getCpuQuota(){
        return this.cpuQuota;
    }

    @Override
    public double getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(double finishTime) {
        this.finishTime = finishTime;
    }

    public int getProgressReporter() {
        return progressReporter;
    }

    public void setProgressReporter(int progressReporter) {
        this.progressReporter = progressReporter;
    }

    public Program getProgram() {
        return program;
    }

    public int getContianerID() {
        return ContianerID;
    }

    public void setContianerID(int contianerID) {
        ContianerID = contianerID;
    }

    public int getCpushare() {
        return  this.cpushare;
    }

    public int getId() {
        return this.getCloudletId();
    }

    public void setSensitiveFunc(AbstractSensitiveFunction func) {
        perf = func;
    }
    public AbstractSensitiveFunction getSensitiveFunction(){
        return perf;
    }
    public double getPerfScore() {
        double pressure = this.vm.getPressure(this.ContianerID);
        double total = this.vm.getTotalMemBW();
        return this.perf.LossByMem(pressure, total);
    }
    public double getCurrMemBW() {
        return this.getMemBWUsage();
    }
    public double getMemBWUsage() {
        return memBWUsage;
    }

    public void setMemBWUsage(double memBWUsage) {
        this.memBWUsage = memBWUsage;
    }

    public int tryScheduleToHost(final int hostID){
        ColocationHost vm = Util.getVM(this.dcName, this.userID, hostID);
        int res = vm.assignBETask(this);
        if (res>0) {
            Log.printLine(CloudSim.clock()+"set be task:#"+this.getTaskName()+" to vm:#"+hostID+" Success");
            this.vmId = hostID;
            this.vm = vm;
        } else {
            Log.printLine(CloudSim.clock()+"set be task:#"+this.getTaskName()+" to vm:#"+hostID+" Failed");
        }
        return res;
    }
    @Override
    public void setVmId(final int vmId) {
        ColocationHost vm = Util.getVM(this.dcName, this.userID, vmId);
        int res = vm.assignBETask(this);
        if (res>0) {
            Log.printLine(CloudSim.clock()+"set be task:#"+this.getTaskName()+" to vm:#"+vmId+" Success");
            this.vmId = vmId;
            this.vm = vm;
        } else {
            Log.printLine(CloudSim.clock()+"set be task:#"+this.getTaskName()+" to vm:#"+vmId+" Failed");
        }
    }

    public int tryRunningOnHost(final int hostID){
        ColocationHost vm = Util.getVM(this.dcName, this.userID, hostID);
        int res = vm.assignBETask(this);
        if (res>0) {
            Log.printLine(CloudSim.clock()+"set be task:#"+this.getTaskFullName()+" to vm:#"+hostID+" Success");
            this.vmId = hostID;
            this.vm = vm;
            return 1;
        } else {
            Log.printLine(CloudSim.clock()+"set be task:#"+this.getTaskFullName()+" to vm:#"+hostID+" Failed");
            return -1;
        }
    }

    public long getRamQuota() {
        return ramQuota;
    }

    public double getMemBWQuota() {
        return memBWQuota;
    }

    public String getJobName() {
        return jobName;
    }

    public boolean isRunable(){
        if (this.getParentList().size() == 0 ){
            return true;
        }
        for (Task task : this.getParentList()) {
            if( task.getStatus() != SUCCESS) {
                return  false;
            }
        }
        return true;
    }

    public double getRunningTime(){
        double mips = this.vm.getMips();
        double costTime =  this.getCloudletTotalLength()/mips;
        return costTime;
    }
}
