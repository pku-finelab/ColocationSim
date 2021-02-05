package org.colocation;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.colocation.bestEffort.ColocationTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by wkj on 2019/5/27.
 */
public class ColocationContainer extends SimEntity {
    boolean isLC = false;
    int cpuShare = 0;
    ServiceEntity lcService ;
    ColocationTask beTask;
    ColocationHost vm ;
    private boolean isRunning;

    Program program;

    private Map<String, ProgramExecContext> execContext;
    private Set<String> execContextFinished;


    ColocationContainer(String name,
                        boolean isLC, int cpuShare, ServiceEntity se, ColocationTask beTask, ColocationHost vm){
        super(name);
        this.isLC = isLC;
        this.cpuShare = cpuShare;
        this.lcService = se;
        this.beTask = beTask;
        this.execContext = new HashMap<>();
        this.execContextFinished = new HashSet<>();
        this.vm = vm;
        if (isLC) {
            program = se.getProgram();
        } else {
            program = beTask.getProgram();
        }
    }

    public boolean isRunning(){
        return isRunning;
    }

    public int getContainerId() {
        return this.getId();
    }

    @Override
    public void startEntity() {
        this.isRunning = true;
    }

    @Override
    public void processEvent(SimEvent ev) {
        ProgramExecContext pec = (ProgramExecContext) ev.getData();
        switch (ev.getTag()) {
            case Constants.CONTAINER_START:
                if (!isLC) {
                    startBE();
                }
                break;
            case Constants.UPDATE_PERFORMANCE:
                updateExecContext();
                break;
            case Constants.PROGRAM_RUNNING:
                running(pec);
                break;
            case Constants.PROGRAM_FINISHED:
                programFinishProcess(pec);
                break;
            case Constants.CONTAINER_SHUTDOWN:
                stopContainer();
                break;
            default:
        }
    }

    private void startBE(){
        //gen be exe id
        String exeCtxID = "BE_pec_"+ beTask.getTaskFullName();
        if (exeCtxID.equals("BE_pec_job1_1:t4")) {
            Log.print("");
        }

        ProgramExecContext pec = new ProgramExecContext(exeCtxID, program);
        pec.setCurrStep(0);
        this.beTask.setStartTime(CloudSim.clock());
        this.beTask.setExecStartTime(CloudSim.clock());
        pec.setCurrStepInstrFinishedNum(0);
        pec.setLastStartTime(CloudSim.clock());
        double currMips = this.vm.getActualMips(this.getContainerId());
        pec.setLastExecActualMips(currMips);
        double estimateExeTime = this.vm.getExecTime(program.get(0).getInstructionNum(), this.getContainerId());
        double finishClock = CloudSim.clock()+estimateExeTime;
        this.beTask.setEstimateFinishTime(finishClock);
        pec.setLastEstimateFinishClock(finishClock);
        execContext.put(exeCtxID,pec);
        Log.printLine(exeCtxID+" estimate finishTime:"+ finishClock );
        send(this.getId(), estimateExeTime, Constants.PROGRAM_RUNNING, pec);
    }

    private void running(ProgramExecContext pec){
        if (this.execContextFinished.contains(pec.getId())){
            //context finished, ignore
            return;
        }
        if (!this.execContext.containsKey(pec.getId()) ) {
            //record request context
            Log.printLine("container #"+this.getContainerId()+" dose not has execontext:"+pec.getId());

        } else {
            double currTime = CloudSim.clock();
            ProgramExecContext newerPEC = this.execContext.get(pec.getId());
            double lastEstiTime = pec.getLastEstimateFinishClock();
            if (currTime < lastEstiTime) {
                return;
            }
            // performance not degrade, finished on time. move to next step
            int currStep = newerPEC.getCurrStep();
            currStep = currStep +1;
            Log.printLine(this.beTask.getTaskName()+" move to step #"+currStep);
            newerPEC.setCurrStep(currStep);
            this.execContext.put(pec.getId(), newerPEC);

        }
        int currStep = this.execContext.get(pec.getId()).getCurrStep();
        if (currStep < this.program.size()) {
            // exec next step
        } else {
            //program finished
            Log.printLine("program in container"+this.getContainerId()+" finished");
            programFinishProcess( pec);
        }
    }

    private void programFinishProcess(ProgramExecContext pec) {
        if (isLC) {
            //return to upper service
        } else {
            int reporter = beTask.getProgressReporter();
            send(reporter, 0, Constants.COLOCATION_BE_RETURN, this.beTask);
            this.execContextFinished.add(pec.getId());
            pec.setFinished(true);
            stopContainer();
        }

    }

    public void stopContainer(){
        if (!isRunning) {
            return;
        }
        this.isRunning = false;

        Log.printLine("Container "+this.getName()+" shutdown");
    }

    public void evictContainer(){
        // notify cluster manager
        if (isLC) {
            //
        } else {
            // this container is evicted. notify upper BE manager. Reschedule this container
            send(this.beTask.getProgressReporter(), 0, Constants.EVICTION, this.beTask);
        }
        stopContainer();
    }

    public void updateExecContext(){
        if (!isRunning) {
            return;
        }
        if (isLC) {
            this.lcService.updateInterference();
            return;
        }
        for( Map.Entry<String, ProgramExecContext> entry:  this.execContext.entrySet()) {
            String key = entry.getKey();
            double now = CloudSim.clock();
            ProgramExecContext pec = entry.getValue();
            if (pec.isFinished) {
                continue;
            }
            long finishdIns = pec.getCurrStepInstrFinishedNum();
            double lastStartTime = pec.getLastStartTime();
            double lastFinishTime = pec.getLastEstimateFinishClock();
            long execInsNum = Math.round( (now-lastStartTime) * pec.getLastExecActualMips() * Constants.MILLION);
            finishdIns = finishdIns + execInsNum;
            long remaningInsNum = pec.getCurrStepInstrNum() - finishdIns;
            double aMips = this.vm.getActualMips(this.getContainerId());
            if (aMips <0 ) {
                Log.printLine(this.getContainerId()+" allocated mips:" +aMips+" running status:"+this.isRunning());
                continue;
            }
            double newEstimateCost = remaningInsNum/(aMips*Constants.MILLION);

            if (newEstimateCost > 2000){
                Log.printLine(key+" cost too long. remainingIns:"+remaningInsNum+" mips:"+aMips+" . on vm "+ this.vm.getId());
            }

            double newEndTime = now + newEstimateCost;
            if (newEndTime == pec.getLastEstimateFinishClock()){
                continue;
            }

            //Log.printLine(pec.getId()+": lastEstFinishTime:"+lastFinishTime+" new EstFinishTime:"+ newEndTime);

            pec.setLastEstimateFinishClock(newEndTime);
            this.beTask.setEstimateFinishTime(newEndTime);
            pec.setCurrStepInstrFinishedNum(finishdIns);
            pec.setLastStartTime(now);
            pec.setLastExecActualMips(aMips);
            this.execContext.put(key, pec);
            CloudSim.send(this.getId(), this.getId(), newEstimateCost, Constants.PROGRAM_RUNNING, pec);
        }
    }

    @Override
    public void shutdownEntity() {
        stopContainer();
    }



    public int getCpuShare() {
        return cpuShare;
    }

    public void setCpuShare(int cpuShare) {
        this.cpuShare = cpuShare;
    }

    public ServiceEntity getLcService() {
        return lcService;
    }

    public void setLcService(ServiceEntity lcService) {
        this.lcService = lcService;
    }

    public ColocationTask getBeTask() {
        return beTask;
    }

    public void setBeTask(ColocationTask beTask) {
        this.beTask = beTask;
    }

    public double getPerfScore(){
        if (isLC) {
            return lcService.getPerfLoss();
        } else {
            return beTask.getPerfScore();
        }
    }

    public double getCpuUsage(){
        //if (!isRunning) {
        //   return 0.0;
        //}
        if (isLC) {
            return lcService.getCpuUsage();
        } else {
            // assume be cpu is 100%
            //double mips = this.vm.getActualMips(this.getContainerId());
            //double be_cpu = mips/vm.getMips();
            double be_cpu = this.beTask.getCpuUsage();
            return be_cpu;
        }
    }

    public long getMemUsage(){
        if (!isRunning) {
            return 0L;
        }
        if (isLC) {
            return lcService.getMemUsage();
        } else {
            return beTask.getRamQuota();
        }
    }
}
