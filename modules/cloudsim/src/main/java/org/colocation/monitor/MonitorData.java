package org.colocation.monitor;

import org.colocation.Constants;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wkj on 2019/6/4.
 */
public class MonitorData {
    int containerID;
    int hostID;
    long timestamp;
    HashMap<String, Double> data;
    public MonitorData(int containerID, int hostID, double timestamp) {
        this.containerID = containerID;
        this.hostID = hostID;
        this.timestamp = Math.round(timestamp);
        this.data = new HashMap<>();
    }

    public void merge(MonitorData another) {
        if (another.getTimestamp() != this.timestamp){
            return;
        }
        for( Map.Entry<String, Double> entry:  another.getData().entrySet()) {
            String metric = entry.getKey();
            if (this.data.containsKey(metric)) {
                double oldValue = this.getMetric(metric);
                double curr = entry.getValue();
                if (curr > oldValue) {
                    double newValue = curr;
                    this.data.put(metric, newValue);
                }
                //double newValue = (oldValue + entry.getValue()) / 2;
                //this.data.put(metric, newValue);
            } else {
                this.data.put(metric, another.getMetric(metric));
            }
        }
    }

    public String toString(){
        double cpuUsage = this.data.get(Constants.METRIC_CPU);
        double memUsage = this.data.get(Constants.METRIC_MEM);
        return String.format("%d,%d,%d,%.4f,%.4f\n", this.hostID, this.containerID, this.timestamp, cpuUsage, memUsage);
    }

    public String getLocation() {
        return this.hostID+":"+this.containerID;
    }

    public int getContainerID() {
        return containerID;
    }

    public void setContainerID(int containerID) {
        this.containerID = containerID;
    }

    public int getHostID() {
        return hostID;
    }

    public void setHostID(int hostID) {
        this.hostID = hostID;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public HashMap<String, Double> getData() {
        return  this.data;
    }

    public void addMetric(String metric, double value) {
        this.data.put(metric, value);
    }
    public double getMetric(String key) {
        return this.data.get(key);
    }
}
