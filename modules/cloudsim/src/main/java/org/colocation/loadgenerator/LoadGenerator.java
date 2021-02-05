package org.colocation.loadgenerator;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.colocation.Constants;
import org.colocation.Request;
import org.colocation.RequestHandleEvent;
import org.colocation.ServiceEntity;
import org.colocation.qos.EntrypointAPI;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wkj on 2019/6/12.
 */
public abstract class LoadGenerator {
    double startTime;
    int brokerID;
    int duration;

    public LoadGenerator( double startAt, int durationInSencond){
        this.startTime = startAt;
        this.brokerID = -1;
        this.duration = durationInSencond;
    }
    public void setBrokerID(int brockerID){
        this.brokerID = brockerID;
    }

    public abstract void genLoad();
    public void setStartTime(double startAt) {
        this.startTime = startAt;
    }

    public void setDuration(int duration){
        this.duration = duration;
    }

    void sendARequest( String apiEntry, String requestName, double delay){

        ServiceEntity se = (ServiceEntity) CloudSim.getEntity(apiEntry);
        //Log.printLine(apiEntry);
        Request req = new Request(requestName, se.getName(), "broker", null);
        RequestHandleEvent reqHandleEvent = new RequestHandleEvent(req, brokerID, se.getProgram());

        CloudSim.send(this.brokerID, se.getId(), delay, Constants.HANDEL_REQUEST, reqHandleEvent);
    }
}
