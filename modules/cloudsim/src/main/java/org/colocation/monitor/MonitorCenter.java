package org.colocation.monitor;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.colocation.Analysis;
import org.colocation.Constants;
import org.colocation.uitl.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by wkj on 2019/6/4.
 */
public class MonitorCenter extends SimEntity{
    HashMap<Integer, HashMap<Integer, ArrayList<MonitorData>>> data;
    HashMap<Integer, Integer> containerHostMap;

    private Analysis analyzer;

    public MonitorCenter(){
        super("monitorCenter");
        this.data = new HashMap<>();
        this.containerHostMap = new HashMap<>();
    }
    @Override
    public void startEntity() {

    }

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case Constants.MONITER:
                addMonitorData((MonitorData) ev.getData());
                break;
            case Constants.MONITOR_EVENT:
                addMonitorEvent((MonitorEvent) ev.getData());
                break;
        }
    }

    @Override
    public void shutdownEntity() {

    }

    public void setAnalyzer(Analysis analyzer) {
        this.analyzer = analyzer;
    }

    void addMonitorEvent(MonitorEvent monitorEvent){
        //report to analysis
        analyzer.addMonitorEvent(monitorEvent);
    }

    public void addMonitorData(MonitorData monitorData) {
        int hostID = monitorData.getHostID();
        int containerID = monitorData.getContainerID();

        //report to analysis
        analyzer.addMonitorData(monitorData);

        if ( containerID < 0 ) {
            return;
        }

        if ( !this.containerHostMap.containsKey(containerID)) {
            this.containerHostMap.put(containerID, hostID);
        }

        if (!this.data.containsKey(hostID)) {
            HashMap<Integer, ArrayList<MonitorData>> hostMap = new HashMap();
            hostMap.put(containerID, new ArrayList<>());
            this.data.put(hostID, hostMap);
        } else {
            if ( !this.data.get(hostID).containsKey(containerID) ) {
                ArrayList<MonitorData> arr = new ArrayList<>();
                this.data.get(hostID).put(containerID, arr);
            }
        }
        ArrayList<MonitorData> ts = this.data.get(hostID).get(containerID);
        if ( ( ts.size() == 0 ) || ( ts.get( ts.size()-1 ).getTimestamp() < monitorData.getTimestamp() ) ) {
            ts.add(monitorData);
        } else {
            MonitorData lastOne = ts.get(ts.size() - 1);
            lastOne.merge(monitorData);
        }

    }

    public List getContainerHistory(int hostID, int conID, String metric, int len) {
        return getContainerHistory(hostID, conID, metric, "m", len,Math.round(CloudSim.clock()));
    }

    public List getContainerHistory(int conID, String metric, String type, int len, long endTimestamp) {
        if (containerHostMap.containsKey(conID)) {
            int hostID = this.containerHostMap.get(conID);
            return this.getContainerHistory(hostID, conID, metric, type, len, endTimestamp);
        }
        return  null;
    }

    public List getContainerHistory(int hostID, int conID, String metric, String type, int len, long endTimestamp) {
        if (!existData(hostID, conID)) {
            return new ArrayList<>();
        }
        // type default is min;
        int granularity ;
        switch (type) {
            case "s":
                granularity = 1;
                break;
            case "m":
                granularity = 60;
                break;
            case "h":
                granularity = 3600;
                break;
            default:
                granularity = 60;
        }
        long now = endTimestamp;
        long startTime = now - len * granularity +1;
        long endTime = now;
        int originLen = len*granularity;
        ArrayList<MonitorData> originData = new ArrayList<>();

        ArrayList<MonitorData> totalTs = this.data.get(hostID).get(conID);
        long startIndex = findStartIndex(totalTs, startTime);
        for (int i=0 ; i< originLen ; i++) {

            long currTimestamp = startTime + i;
            if ( (startIndex+1 < totalTs.size()) && ( totalTs.get((int)startIndex+1).getTimestamp() == currTimestamp ) ) {
                startIndex = startIndex +1;
            }
            if (startIndex < 0) {
                // cannot find start , fill with -1;
                originData.add(null);
                continue;
            }
            MonitorData tsOne = totalTs.get((int)startIndex);
            originData.add(tsOne);
        }


        ArrayList<Double> data = new ArrayList<>();
        for (int i = 0; i < originLen; i++) {
            if (null == originData.get(i)) {
                data.add(-1.0);
            } else {
                data.add(originData.get(i).getMetric(metric));
            }
        }

        data = (ArrayList) Util.averageBySeg(data, granularity);

        return data;
    }

    private long findStartIndex(ArrayList<MonitorData> list, long targetTime) {
        for (int i=0 ; i < list.size(); i++) {
            MonitorData monData = list.get(i);
            long monTime = monData.getTimestamp();
            if (monTime == targetTime) {
                return i;
            }
            if (monTime > targetTime) {
                return i-1;
            }
        }
        return list.size()-1;
    }

    private boolean existData(int hostID, int containerID) {
        if (this.data.containsKey(hostID) && this.data.get(hostID).containsKey(containerID)){
            return true;
        }
        return false;
    }
}
