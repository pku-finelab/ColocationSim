package org.colocation;

import org.cloudbus.cloudsim.Log;
import org.colocation.monitor.MonitorData;
import org.colocation.monitor.MonitorEvent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wkj on 2019/9/10.
 */
public class Analysis {
    private String storageDict;
    private boolean disableOutput;
    private BufferedWriter lcRtFileWriter;
    private BufferedWriter beEtFileWriter;
    private BufferedWriter machineUsageFileWriter;
    private BufferedWriter containerUsageFileWriter;
    private BufferedWriter eventsFileWriter;

    private HashMap<String, StatisticalData> serviceRtMap;
    private HashMap<String, StatisticalData> beEtMap;
    private HashMap<String, MonitorData> usageMap;


    public Analysis(String storageDict){
        this.storageDict = storageDict;
        try {
            String sep = System.getProperty("file.separator");

            //create 3 files: 1. LC_RT.csv, 2. BE_ET.csv,  3. machine_usage.csv
            String lc_rt_filename = "lc_rt.csv";
            String be_et_filename = "be_et.csv";
            String machine_filename = "machine_usage.csv";
            String contaierFileName = "container_usage.csv";
            String eventFileName = "events.csv";

            FileWriter fw = new FileWriter(storageDict+sep + lc_rt_filename);
            this.lcRtFileWriter = new BufferedWriter(fw);

            fw = new FileWriter(storageDict+sep + be_et_filename);
            this.beEtFileWriter = new BufferedWriter(fw);

            fw = new FileWriter(storageDict+sep + machine_filename);
            this.machineUsageFileWriter = new BufferedWriter(fw);

            fw = new FileWriter(storageDict + sep + contaierFileName);
            this.containerUsageFileWriter = new BufferedWriter(fw);

            fw = new FileWriter(storageDict + sep + eventFileName);
            this.eventsFileWriter = new BufferedWriter(fw);

            this.serviceRtMap = new HashMap<>();
            this.beEtMap      = new HashMap<>();
            this.usageMap     = new HashMap<>();

        } catch (IOException e) {
            Log.printLine("ERROR: create file failed!");
            e.printStackTrace();
        }
    }

    public void setDisableOutput(boolean value){
        this.disableOutput = value;
    }

    public void addRequest(String serviceName, double timestamp, double rt) {
        if (!this.serviceRtMap.containsKey(serviceName)) {
            StatisticalData data = new StatisticalData(serviceName);
            this.serviceRtMap.put(serviceName, data);
        }
        this.serviceRtMap.get(serviceName).addData(rt);

        if (disableOutput) {
            return;
        }

        try {
            this.lcRtFileWriter.write(String.format("%s,%.4f,%.4f\n", serviceName, timestamp, rt));
        } catch (IOException e) {
            e.printStackTrace();
            Log.printLine("ERROR: write lc_rt.csv");
        }
    }

    public void addBeResult(String jobName, double timestamp, double execTime) {
        String jobOriginName = jobName.split("_")[0];
        if (!this.beEtMap.containsKey(jobOriginName)) {
            StatisticalData data = new StatisticalData(jobOriginName);
            this.beEtMap.put(jobOriginName, data);
        }
        this.beEtMap.get(jobOriginName).addData(execTime);
        if (disableOutput) {
            return;
        }
        try {
            this.beEtFileWriter.write(String.format("%s,%.4f,%.4f\n", jobOriginName, timestamp, execTime));
        } catch (IOException e) {
            e.printStackTrace();
            Log.printLine("ERROR: be_et.csv");
        }
    }

    public void addMonitorEvent(MonitorEvent monitorEvent) {
        if (disableOutput) {
            return;
        }
        try {
            this.eventsFileWriter.write(monitorEvent.toString());
        }catch (IOException e) {
            e.printStackTrace();
            Log.printLine("ERROR: write events.csv");
        }
    }

    public void addMonitorData(MonitorData data){
        try {
            String location = data.getLocation();
            if ( !this.usageMap.containsKey(location) ) {
                this.usageMap.put(location, data);
                return;
            }
            MonitorData oldData = this.usageMap.get(location);
            if (oldData.getTimestamp() == data.getTimestamp()) {
                oldData.merge(data);
                return;
            }
            if (disableOutput) {
                return;
            }
            // next timestamp, print old data then replace it with new data
            BufferedWriter fw = null;
            if (data.getContainerID() < 0){
                //host metrics
                fw = machineUsageFileWriter;
            } else {
                //container metrics
                fw = containerUsageFileWriter;
            }
            fw.write(oldData.toString());
            this.usageMap.put(location, data);
        } catch (IOException e) {
            e.printStackTrace();
            Log.printLine("ERROR: be_et.csv");
        }
    }

    public void printReport(){
        String indent = "\t";
        Log.printLine("============= LC Response Time =============");
        Log.printLine("serviceName" + indent + "min" + indent + "max" + indent + "avg"+ indent + "count" + indent+"pd");
        for (Map.Entry<String, StatisticalData> entry : this.serviceRtMap.entrySet()) {
            StatisticalData item = entry.getValue();
            Log.formatLine(item.toString());
        }
        Log.printLine("============= BE Execute  Time =============");
        Log.printLine("jobName" + indent + "min" + indent + "max" + indent + "avg"+ indent + "count");
        for (Map.Entry<String, StatisticalData> entry : this.beEtMap.entrySet()) {
            StatisticalData be = entry.getValue();
            Log.formatLine(be.toString());
        }
        Log.printLine("============= Host Utilization =============");
    }

    public void close () {
        try {
        this.lcRtFileWriter.close();
        this.beEtFileWriter.close();
        for ( Map.Entry<String, MonitorData> entry : this.usageMap.entrySet()) {
            MonitorData data = entry.getValue();
            if (data.getContainerID() < 0) {
                //host
                this.machineUsageFileWriter.write(data.toString());
            } else {
                this.containerUsageFileWriter.write(data.toString());
            }
        }
        this.machineUsageFileWriter.close();
        this.containerUsageFileWriter.close();
        this.eventsFileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.printLine("ERROR: write lc_rt.csv");
        }
    }

    class StatisticalData{
        String name;
        double min;
        double max;
        double total;
        int count;

        public StatisticalData(String name) {
            this.name = name;
            this.count = 0;
            this.min = Double.MAX_VALUE;
            this.max = 0;
            this.total = 0;
        }
        void addData(double d) {
            this.total = this.total + d;
            this.count = this.count + 1;
            if (d < min) {
                min = d;
            }
            if (d > max) {
                max = d;
            }
        }

        @Override
        public String toString(){
            return String.format("%-12s\t%.4f\t%.4f\t%.4f\t%d\t%.4f", name, min, max, total/count, count, min/max);
        }
    }
}
