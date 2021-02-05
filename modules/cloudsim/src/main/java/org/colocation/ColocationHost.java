package org.colocation;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.colocation.bestEffort.ColocationTask;
import org.colocation.engine.CPUShareExecEngine;
import org.colocation.engine.ExecEngine;
import org.colocation.monitor.MonitorData;
import org.colocation.monitor.MonitorEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wkj on 2019/3/8.
 */
public class ColocationHost extends Vm {

    double currentMemPressure;
    double currentCPUPressure;
    double currentBWPressure;
    double currentBW;
    double currentDiskPressure;

    private double currCpuUsage;
    private long currMemUsage;

    // only for Resource Ram
    double lcRatio;
    double limitMemRatio;
    long lcAllocatedRam;
    long beAllocatedRam;

    // cpu limitation
    double cpuUtilUpper;
    double memUtilUpper;


    List <ServiceEntity> lcList;
    List <ColocationTask> beList;
    List <ColocationContainer> containerList;

    private double lcMemBW;
    private double beMemBW;
    private long ramBytes;

    private double totalMemBW;

    private ExecEngine exeEngine;
    private int monitorCenter;

    public ColocationHost(
            int id,
            int userId,
            double mips,
            int numberOfPes,
            long ramBytes,
            long bw,
            long size,
            String vmm,
            CloudletScheduler cloudletScheduler, double lcratio, double limitRaito, double memBandwidth, String execEngineType){
        super(id, userId, mips, numberOfPes, (int)(ramBytes/Constants.MB), bw, size, vmm, cloudletScheduler);
        this.ramBytes = ramBytes;
        this.lcRatio = lcratio;
        this.limitMemRatio = limitRaito;
        this.cpuUtilUpper = limitRaito;
        this.memUtilUpper = limitRaito;
        this.lcList = new ArrayList<>();
        this.beList = new ArrayList<>();
        this.containerList = new ArrayList<>();
        this.totalMemBW = memBandwidth;
        this.exeEngine = null;
        switch (execEngineType) {
            case Constants.CPUSHARE:
                this.exeEngine = new CPUShareExecEngine(mips*numberOfPes, this);
                break;
            default:
                this.exeEngine = new CPUShareExecEngine(mips*numberOfPes, this);
        }
    }

    public long getRamBytes(){
        return this.ramBytes;
    }

    public List<ServiceEntity> getLcList() {
        return lcList;
    }

    public List<ColocationContainer> getContainerList() {
        return containerList;
    }

    public double getPressure(int containerId) {
        double peerMemBW = 0.0;
        for (ServiceEntity se: lcList) {
            if ( se.getContainerID() == containerId)
                continue;
            peerMemBW += se.getCurrMemBW();
        }
        for (ColocationTask task: beList) {
            if ( task.getContianerID() == containerId)
                continue;
            peerMemBW += task.getCurrMemBW();
        }
        return peerMemBW;
    }

    public double getTotalMemBW() {
        return totalMemBW;
    }

    private void updateAllProgramPerformance(){
        for (ColocationContainer c : this.containerList) {
            if (c.isRunning()) {
                c.updateExecContext();
                //CloudSim.send(0, c.getId(), 0, Constants.UPDATE_PERFORMANCE, null);
            }
        }

    }

    public int assignServiceEntity(ServiceEntity se){
        if (se.isLc()){
            long ifAllocatedRam = se.getMemQuota() + this.getLcAllocatedRam();
            long lcAvail = this.getLcAvailableRam();
            long totalAvail = this.getTotalLimitRam();
            if ( (se.getMemQuota() <= lcAvail) && (ifAllocatedRam <= totalAvail) ) {
                // accept this entity
                ColocationContainer con = new ColocationContainer("LC_con_"+se.getName(), true, se.getCpushare(), se, null, this);
                se.setContainerID(con.getId());
                this.containerList.add(con);
                this.lcList.add(se);
                this.lcAllocatedRam = this.lcAllocatedRam + se.getMemQuota();
                updateAllProgramPerformance();

                MonitorEvent event = new MonitorEvent(con.getContainerId(), this.getId(), Math.round(CloudSim.clock()), con.getName(), Constants.EVENT_TYPE_ADD_CON );
                CloudSim.send(this.getId(), this.monitorCenter, 0, Constants.MONITOR_EVENT, event);

                return 1;
            } else {
                return -1;
            }
        }
        return -1;
    }

    public int assignBETask(ColocationTask task) {
        long ifAllocatedRam = task.getRamQuota();
        long beAvail = this.getBeAvailableRam();
        long totalAvail = this.getTotalLimitRam();
        double availCPU = this.getAvailableCPU();
        double avialMem = this.getAvailableRam();
        //if ( (ifAllocatedRam <= beAvail) && (ifAllocatedRam <= totalAvail) ) {
        Log.printLine("Host #"+this.getId()+" available CPU MEM: "+avialMem +" "+ availCPU +"task needs: "+ task.getCpuQuota()+" "+task.getMemBWQuota());
        if ( (task.getMemBWQuota()< avialMem) && (task.getCpuQuota() < availCPU)){
            // accept this entity
            ColocationContainer con = new ColocationContainer("BE_con_"+task.getJobName()+"_"+task.getTaskName(), false, task.getCpushare(),
                    null,task, this);
            task.setContianerID(con.getId());
            this.containerList.add(con);
            this.beList.add(task);
            this.beAllocatedRam = this.beAllocatedRam + task.getRamQuota();
            //updateAllProgramPerformance();
            updateUtilization();
            MonitorEvent event = new MonitorEvent(con.getContainerId(), this.getId(), Math.round(CloudSim.clock()), con.getName(), Constants.EVENT_TYPE_ADD_CON );
            CloudSim.send(this.getId(), this.monitorCenter, 0, Constants.MONITOR_EVENT, event);

            return 1;
        } else {
            return -1;
        }
    }

    public double getExecTime(long instructionNum, int containerID) {
        return this.exeEngine.getExecTime(instructionNum, containerID);
    }
    public double getActualMips(int containerID) {
        return  this.exeEngine.getActualMips(containerID);
    }

    public void deleteBETask(ColocationTask task) {
        // remove container
        ColocationContainer targetCon = null;
        for( ColocationContainer c: this.containerList) {
            if (c.getContainerId() == task.getContianerID()) {
                targetCon = c;
            }
        }
        this.containerList.remove(targetCon);
        this.beList.remove(task);
        updateUtilization();
    }

    public long getBeAvailableRam(){
        double tmp = this.getRamBytes() * this.getBeRatio() - this.getBecAllocatedRam();
        return (long) tmp;
    }

    public double getAvailableRam(){
        return this.getRamBytes() * this.memUtilUpper - this.currMemUsage;
    }

    public long getTotalLimitRam(){
        return (long) (this.getLimitMemRatio()* this.getRamBytes());
    }

    public void setLcRaito(double lcRatio) {
        this.lcRatio = lcRatio;
    }

    public long getLcAvailableRam(){
        double tmp = this.getRamBytes() * this.getLcRatio() - this.getLcAllocatedRam();
        return (long) tmp;
    }

    public int getLcRamQuota(){
        return (int)(this.getRamBytes()*lcRatio);
    }

    public double getLcRatio() {
        return lcRatio;
    }

    public double getBeRatio(){
        return 0.9;
    }

    public void setLcRatio(double lcRatio) {
        this.lcRatio = lcRatio;
    }

    public double getLimitMemRatio() {
        return limitMemRatio;
    }

    public void setLimitMemRatio(double limitMemRatio) {
        this.limitMemRatio = limitMemRatio;
    }

    public long getLcAllocatedRam() {
        return lcAllocatedRam;
    }

    public int getLCNumber(){
        return this.lcList.size();
    }

    public void setLcAllocatedRam(int lcAllocatedRam) {
        this.lcAllocatedRam = lcAllocatedRam;
    }

    public long getBecAllocatedRam() {
        return beAllocatedRam;
    }

    public void setBecAllocatedRam(int becAllocatedRam) {
        this.beAllocatedRam = becAllocatedRam;
    }


    public void setCurrentMemPressure(double currentMemPressure) {
        this.currentMemPressure = currentMemPressure;
    }

    public double getCurrentCPUPressure() {
        return currentCPUPressure;
    }

    public void setCurrentCPUPressure(double currentCPUPressure) {
        this.currentCPUPressure = currentCPUPressure;
    }

    public double getCurrentBWPressure() {
        return currentBWPressure;
    }

    public void setCurrentBWPressure(double currentBWPressure) {
        this.currentBWPressure = currentBWPressure;
    }

    public double getCurrentDiskPressure() {
        return currentDiskPressure;
    }

    public void setCurrentDiskPressure(double currentDiskPressure) {
        this.currentDiskPressure = currentDiskPressure;
    }

    public void setMonitorCenter(int monitorCenterID) {
        this.monitorCenter = monitorCenterID;
    }

    public double getCurrentBW(){
        updateUtilization();
        return this.currentBW;
    }

    public int findTopCpuBE(){
        int maxCpuConID = -1;
        double maxCpuUsage = 0.0;
        for (ColocationContainer c: this.containerList) {
            if (c.isLC) {
                continue;
            }
            if (c.isRunning()) {
                int cId = c.getContainerId();
                double usage = c.getCpuUsage();
                if (usage > maxCpuUsage) {
                    maxCpuUsage = usage;
                    maxCpuConID = cId;
                }
            }
        }
        return  maxCpuConID;
    }

    public int findTopMemBE(){
        int maxMemConID = -1;
        double maxMemUsage = 0.0;
        for (ColocationContainer c: this.containerList) {
            if (c.isLC) {
                continue;
            }
            if (c.isRunning()) {
                int cId = c.getContainerId();
                long usage = c.getMemUsage();
                if (usage > maxMemUsage) {
                    maxMemUsage = usage;
                    maxMemConID = cId;
                }
            }
        }
        return  maxMemConID;
    }

    private void killContainer(int containerID) {
        if (containerID < 0 ) {
            return;
        }
        Log.printLine(" EVICTION !!! in host:"+this.getId()+" evict container:"+containerID);
        ColocationContainer targetCon = null;
        for (ColocationContainer c : this.containerList) {
            if ( c.getContainerId() == containerID ) {
                targetCon = c;
                ColocationTask beTask = targetCon.getBeTask();
                deleteBETask(beTask);
                targetCon.evictContainer();
                break;
            }
        }
        this.containerList.remove(targetCon);
    }

    public void updateUtilization() {
        updateAllProgramPerformance();
        // check resource, kill BE Task when resource is not enough
        double totalCpuUsage = 0.0;
        for (ColocationContainer c : this.containerList) {
            totalCpuUsage += c.getCpuUsage();
        }
        this.currCpuUsage = totalCpuUsage;
        if (totalCpuUsage > this.cpuUtilUpper*100) {
            //kill most cpu usage be
            Log.printLine("Host "+this.getId()+" cpu upper level: curr: " + totalCpuUsage+ " upper:" + this.cpuUtilUpper*100 +"on host:"+ this.getId());
            int conID = findTopCpuBE();
            killContainer(conID);
        }

        long totalMemUsage = 0L;
        for (ColocationContainer c : this.containerList) {
            totalMemUsage += c.getMemUsage();
        }
        double totalMemUsageRatio = totalMemUsage/ this.getRamBytes();
        this.currMemUsage = totalMemUsage;
        if (totalMemUsageRatio > this.limitMemRatio) {
            //kill most mem usage be
            Log.printLine("mem upper level: curr: " + totalMemUsageRatio+ " upper:" + this.limitMemRatio +"on host:"+ this.getId());
            int conID = findTopMemBE();
            killContainer(conID);
        }

        // report every container usage
        double hostCpuTotal = 0.0;
        double hostMemTotal = 0.0;
        int brokerID = this.getUserId();
        for (ColocationContainer c: this.containerList) {
            double cpuUsage = c.getCpuUsage();
            double memUsage = c.getMemUsage();
            MonitorData monitorData = new MonitorData(c.getContainerId(), this.getId(), CloudSim.clock());
            monitorData.addMetric(Constants.METRIC_CPU, cpuUsage);
            monitorData.addMetric(Constants.METRIC_MEM, memUsage);
            // send to Broker
            //CloudSim.send(this.getId(), this.monitorCenter, 0, Constants.MONITER, monitorData);
            hostCpuTotal = hostCpuTotal + cpuUsage;
            hostMemTotal = hostMemTotal + memUsage;
        }
        MonitorData hostMonData = new MonitorData(-1, this.getId(), CloudSim.clock());
        hostMonData.addMetric(Constants.METRIC_CPU, hostCpuTotal);
        hostMonData.addMetric(Constants.METRIC_MEM, hostMemTotal);
        //CloudSim.send(this.getId(), this.monitorCenter, 0, Constants.MONITER, hostMonData);

        double lcMemBandwidth = 0.0;
        // update Mem pressure

        for (int i = 0; i < this.lcList.size(); i++) {
            ServiceEntity lcIns = this.lcList.get(i);
            lcMemBandwidth = lcMemBandwidth + lcIns.getCurrMemBW();
        }
        this.lcMemBW = lcMemBandwidth;

        long beMemUsage = 0;
        double beMemBandwidth = 0.0;
        for (int i = 0; i < this.beList.size(); i++) {
            ColocationTask beIns = this.beList.get(i);
            beMemBandwidth = beMemBandwidth + beIns.getMemBWQuota();
            beMemUsage = beMemUsage + beIns.getRamQuota();
        }
        this.beMemBW = beMemBandwidth;
        this.beAllocatedRam = beMemUsage;

        double currMemBW = lcMemBandwidth+beMemBandwidth;

        // normalize bandwith pressure to 0~10
        if (currMemBW > this.totalMemBW) {
            this.currentBWPressure = 10;
            this.currentBW = this.getBw();
        } else {
            this.currentBW = currMemBW;
            this.currentBWPressure = (currMemBW / totalMemBW) * 10;
        }
        //TODO: update cpu pressure
    }

    public void setCurrentBW(double currentBW) {
        this.currentBW = currentBW;
    }

    public double getMinResourceToSLA(){
        double minMemBW = Double.MAX_VALUE;
        for (int i = 0; i < this.containerList.size(); i++) {
            ColocationContainer c = this.containerList.get(i);
            if (c.isLC){
                ServiceEntity se = c.getLcService();
                double cr = se.getContributedResource();
                if (cr < minMemBW){
                    minMemBW = cr;
                }
            } else {
                continue;
            }
        }
        return minMemBW;
    }

    public double getMinSLASlack(){
        double min = Double.MAX_VALUE;
        for(ServiceEntity se: this.lcList){
            double score = 0-se.getPerfLoss();
            if (score < min) {
                min = score;
            }
        }
        return min;
    }

    public double getInterferenceBy(double mbw){
        double max_inter = 0;
        for(ServiceEntity se : this.lcList){
            double ci = se.getSensitiveFun().LossByMem(se.getPeerPressure()+mbw,0);
            if (ci > max_inter) {
                max_inter += ci;
            }
        }
        return max_inter;
    }

    public double getCpuUsagePercent(){
        return this.currCpuUsage;
    }

    public double getAvailableCPU(){
        double available = 100 * this.cpuUtilUpper - currCpuUsage;
        return available;
    }

    public double getMemUsagePercent(){
        return this.currMemUsage;
    }
}
