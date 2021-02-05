package org.colocation.loadgenerator;

import org.cloudbus.cloudsim.core.CloudSim;
import org.colocation.Constants;
import org.colocation.Request;
import org.colocation.RequestHandleEvent;
import org.colocation.ServiceEntity;
import org.colocation.bestEffort.ColocationJob;

public abstract class BeLoadGenerator {
    double startTime;
    int brokerID;

    public BeLoadGenerator(double startTime, int brokerID) {
        this.startTime = startTime;
        this.brokerID = brokerID;
    }
    public void setBrokerID(int brockerID){
        this.brokerID = brockerID;
    }

    public abstract void genLoad();
    public void setStartTime(double startAt) {
        this.startTime = startAt;
    }

    void sendJob(ColocationJob job, double atTick){
        CloudSim.send(this.brokerID, this.brokerID, atTick, Constants.SCHEDULE_BE_JOB, job);
    }
}
