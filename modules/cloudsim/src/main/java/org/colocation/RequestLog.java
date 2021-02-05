package org.colocation;

import org.apache.commons.math3.analysis.function.Constant;
import org.cloudbus.cloudsim.Log;

/**
 * Created by wkj on 2019/3/8.
 */
public class RequestLog {
    double timestamp;
    String serviceName;
    int localSeID;
    int remoteSeID;
    String requestId;
    long dataId;
    long netcomId;
    int hostID;
    int    optType;

    public RequestLog(String serviceName, double timestamp,long dataId, long netcomId, int local, int remote, String requestId, int optType, int hostID){
        this.timestamp = timestamp;
        this.serviceName = serviceName;
        this.localSeID = local;
        this.remoteSeID = remote;
        this.requestId = requestId;
        this.optType = optType;
        this.dataId = dataId;
        this.netcomId = netcomId;
        this.hostID = hostID;
    }

    public String toString(){
        return String.format("serviceName=%s&timestamp=%f&localSeID=%d&remoteSeID=%s&requestId=%s&optType=%d&dataID=%d&netcomID=%d", this.serviceName,this.timestamp, this.localSeID, this.remoteSeID,this.requestId, this.optType, this.dataId, this.netcomId);
    }

    public double getTimestamp() {
        return timestamp;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getLocalSeID() {
        return localSeID;
    }

    public int getRemoteSeID() {
        return remoteSeID;
    }

    public String getRequestId() {
        return requestId;
    }

    public long getDataId() {
        return dataId;
    }

    public int getOptType() {
        return optType;
    }

    public long getNetcomId() {
        return netcomId;
    }

    public String getEventID() {
        return String.format("sn=%s&t=%f&optType=%d&dataID=%d&netcomID=%d", this.serviceName,this.timestamp, this.optType, this.dataId, this.netcomId);
    }

    public boolean hasChild(RequestLog log) {
        switch (this.optType) {
            case Constants.FTYPE_SEND:
                if (log.optType == Constants.FTYPE_RECV && this.dataId == log.dataId){
                    return true;
                } else {
                    return false;
                }
            default:
                if ( (log.netcomId == this.netcomId) && (this.timestamp<=log.timestamp) ){
                    return true;
                }
        }
        return false;
    }

    public int getHostID() {
        return hostID;
    }

    public boolean hasParent(RequestLog log) {
        return log.hasChild(this);
    }
}
