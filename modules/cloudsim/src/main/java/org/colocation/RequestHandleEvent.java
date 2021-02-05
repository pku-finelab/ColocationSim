package org.colocation;

import org.cloudbus.cloudsim.core.CloudSim;
import org.colocation.trace.DataIDGenerator;
import org.cloudbus.cloudsim.Log;

import java.util.List;

/**
 * Created by wkj on 2019/6/12.
 */
public class RequestHandleEvent{
    String requestID;
    Request req;
    private int currStep;
    final int from;
    long currStepInstrFinishedNum;
    double lastExecActualMips;
    double lastEstimateFinishClock;
    double lastStartTime;
    Program program;
    private boolean isFinished;
    long dataID;
    long netcomID;
    boolean currStepIsRunning = false;
    boolean isBlocked;

    public RequestHandleEvent( Request req, int from, Program p) {
        this.requestID = req.getId();
        this.currStep = 0;
        this.req = req;
        this.from = from;
        this.program = p;
        this.currStepInstrFinishedNum = 0;
        this.lastEstimateFinishClock = -1;
        this.lastExecActualMips = -1;
        this.dataID = -1;
        this.netcomID = DataIDGenerator.getNextNetComID();
        this.isBlocked = false;
    }

    private void setIsFinished(boolean finished) {
        this.req.setEndTime(CloudSim.clock());
        this.isFinished = finished;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }

    public boolean isFinished() {
        return this.isFinished;
    }

    public long getDataID() {
        return dataID;
    }

    public void setDataID(long dataID) {
        this.dataID = dataID;
    }

    public long getNetcomID() {
        return netcomID;
    }


    public double getLastStartTime() {
        return lastStartTime;
    }

    public void setLastStartTime(double lastStartTime) {
        this.lastStartTime = lastStartTime;
    }

    public void addLogEvent(RequestLog log){
        this.req.addRequestLog(log);
    }

    public void mergeLogEvent(List<RequestLog> logs) {
        this.req.mergeRequestLog(logs);
    }

    public int getFrom() {
        return from;
    }

    public void moveToNextStep() {
        //Log.printLine(this.requestID + "finished step: " + currStep);
        currStep = currStep + 1;
        currStepIsRunning = false;
        if (currStep >= this.program.size()) {
            setIsFinished(true);
        }

    }

    public void setCurrStepIsRunning() {
        currStepIsRunning = true;
    }

    public boolean isCurrStepIsRunning() {
        return currStepIsRunning;
    }

    public int getCurrStep() {
        return currStep;
    }



    public long getCurrStepInstrNum() {
        return this.program.get(this.currStep).getInstructionNum();
    }

    public long getCurrStepInstrFinishedNum() {
        return currStepInstrFinishedNum;
    }

    public void setCurrStepInstrFinishedNum(long currStepInstrFinishedNum) {
        this.currStepInstrFinishedNum = currStepInstrFinishedNum;
    }

    public double getLastExecActualMips() {
        return lastExecActualMips;
    }

    public void setLastExecActualMips(double lastExecActualMips) {
        this.lastExecActualMips = lastExecActualMips;
    }

    public double getLastEstimateFinishClock() {
        return lastEstimateFinishClock;
    }

    public void setLastEstimateFinishClock(double lastEstimateFinishClock) {
        this.lastEstimateFinishClock = lastEstimateFinishClock;
    }

    public Request getRequest() {
        return req;
    }
}