package org.colocation;

/**
 * Created by wkj on 2019/5/30.
 */
public class ProgramExecContext {
    String id;
    int currStep;
    long currStepInstrNum;
    long currStepInstrFinishedNum;
    double lastExecActualMips;
    double lastEstimateFinishClock;
    double lastStartTime;
    Program program;
    boolean isFinished;

    public ProgramExecContext(String id, Program program) {
        this.id = id;
        this.program = program;
        this.isFinished = false;
    }

    public String getId() {
        return id;
    }

    public int getCurrStep() {
        return currStep;
    }

    public void setCurrStep(int currStep) {
        this.currStep = currStep;
        if (currStep >= program.size()) {
            this.isFinished = true;
        }
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public long getCurrStepInstrNum() {
        Procedure p = this.program.get(currStep);

        return p.getInstructionNum();
    }

    public void setCurrStepInstrNum(long currStepInstrNum) {
        this.currStepInstrNum = currStepInstrNum;
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

    public double getLastStartTime() {
        return lastStartTime;
    }

    public void setLastStartTime(double lastStartTime) {
        this.lastStartTime = lastStartTime;
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }
}
