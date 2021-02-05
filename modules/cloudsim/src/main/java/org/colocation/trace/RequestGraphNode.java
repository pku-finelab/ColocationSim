package org.colocation.trace;

import java.util.ArrayList;

/**
 * Created by wkj on 2019/7/16.
 */
public class RequestGraphNode {
    private String id;
    private ArrayList history;
    private int historyPointer;
    private int historyCap;
    private double timestamp;
    private double start;
    private double end;
    private long netcomID;
    private String svcName;
    private int hostID;
    private double minCost;
    private double currCost;


    public RequestGraphNode(String id, double timestamp, long netcomID, long dataID, String scvName, int hostID, int historyCap) {
        this.id = id;
        this.historyCap = historyCap;
        if (historyCap >0 ){
            this.history = new ArrayList(historyCap);
        }
        this.historyPointer = 0;
        this.start = -1;
        this.end = -1;
        this.minCost = Double.MAX_VALUE;
        this.timestamp = timestamp;
        this.netcomID = netcomID;
        this.svcName = scvName;
        this.hostID = hostID;
    }

    public void addValue(double value){
        this.history.set(historyPointer, value);
        historyPointer = (historyPointer+1) % historyCap;
    }

    public double getStart() {
        return start;
    }

    public double getEnd() {
        return end;
    }

    public double getMinCost() {
        return minCost;
    }

    public String toString(){
        return id;
    }

    public void mergeEvent(RequestGraphNode node) {
        if ((start <0) || (end<0)) {
            start = timestamp;
            end = timestamp;
        }
        if ( (node != null) && (node.timestamp > this.end) ) {
            this.end = node.timestamp;
        }
        if ( (node != null) && (node.timestamp < this.start) ) {
            this.start = node.timestamp;
        }
        this.currCost = end - start;
        this.minCost = currCost;
    }

    public void mergeNode(RequestGraphNode node) {
        double value = node.getMinCost();
        if (this.minCost > value) {
            this.minCost = value;
        }
    }

    public long getNetcomID() {
        return netcomID;
    }

    public double value(){
        return end - start;
    }

    public String getSvcName() {
        return svcName;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void mergeNewNode(RequestGraphNode node){
        this.addHistory(node.value());
    }

    private void addHistory(double value) {
        if (value < this.minCost) {
            minCost = value;
        }
        this.currCost = value;
        this.history.add(historyPointer, value);
        historyPointer = (historyPointer+1) % historyCap;
    }

    public void genFirstHistory(){
        if (historyCap>0){
            this.history.add(0, this.value());
            historyPointer = (historyPointer+1)% historyCap;
        }

    }

    public int getHostID() {
        return hostID;
    }
}
