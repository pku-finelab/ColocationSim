package org.colocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wkj on 2019/3/7.
 */
public class Request {

    private String id;
    private double startTime;
    private double endTime;
    private List<RequestLog> requestLogs;
    private Request parentReq;
    private String parentService;
    private String service;

    private int src;

    public int getSrc() {
        return src;
    }

    public void setSrc(int src) {
        this.src = src;
    }

    public Request(String id, String service, String parentService,Request parentReq){
        this.id = id;
        this.service = service;
        this.parentReq = parentReq;
        this.requestLogs = new ArrayList<>();
        this.parentService = parentService;
    }

    public String getService() {
        return service;
    }

    public List<RequestLog> getRequestLogs() {
        return requestLogs;
    }

    public String getParentService() {
        return parentService;
    }

    public void addRequestLog(RequestLog log) {
        this.requestLogs.add(log);
    }

    public void mergeRequestLog(List<RequestLog> logs) {
        for (int i = 0; i <logs.size(); i++) {
            addRequestLog(logs.get(i));
        }
    }

    public String getParentRequestId(){
        if (this.parentReq == null) {
            return "";
        }
        return this.parentReq.getId();
    }

    public String getId() {
        //RequestID format:
        return id;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(double endTime) {
        this.endTime = endTime;
    }

    public double getEndTime() {

        return endTime;
    }

    public double getStartTime() {
        return startTime;
    }

}
