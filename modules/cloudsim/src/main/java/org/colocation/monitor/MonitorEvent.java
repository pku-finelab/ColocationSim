package org.colocation.monitor;

public class MonitorEvent {
    int containerID;
    int hostID;
    long timestamp;
    String name;
    String event;

    public MonitorEvent(int containerID, int hostID, long timestamp, String name, String event) {
        this.containerID = containerID;
        this.hostID = hostID;
        this.timestamp = timestamp;
        this.name = name;
        this.event = event;
    }

    @Override
    public String toString() {
        return String.format("%d,%d,%d,%s,%s\n", this.hostID, this.containerID, this.timestamp, name, event);
    }
}
