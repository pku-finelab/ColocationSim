package org.colocation;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.colocation.bestEffort.ColocationJob;
import org.colocation.loadgenerator.BeLoadGenerator;
import org.colocation.loadgenerator.LoadGenerator;
import org.colocation.monitor.MonitorCenter;
import org.colocation.qos.QosCenter;
import org.colocation.scheduler.DIASAlgorithm;
import org.colocation.trace.ServiceGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wkj on 2019/3/10.
 */
public class ColocationDatacenterBroker extends DatacenterBroker {
    Scheduler scheduler;
    BeScheduler beScheduler;
    List<ServiceEntity> serviceToSchedule;
    List<? extends Cloudlet> beJobsForSchedule;
    List<Request> returnedRequests;
    String dcName;
    int userID;
    ServiceGraph graphservice;
    double beStartAt;
    double jobPerSecond;
    MonitorCenter monitorCenter;
    LoadGenerator lcLoadGenerator;
    BeLoadGenerator beLoadGenerator;
    Analysis analyzer;
    QosCenter qosCenter;

    public ColocationDatacenterBroker(String name, String dcName) throws Exception{
        this(name, dcName, Constants.FCFS, "", -1);
    }

    public ColocationDatacenterBroker(String name, String dcName, String beScheduleAlg ) throws Exception{
        this(name, dcName, beScheduleAlg, "", -1);
    }
    public ColocationDatacenterBroker(String name, String dcName, String beScheduleAlg, String ouppath ) throws Exception{
        this(name, dcName, beScheduleAlg, ouppath, -1);
    }

    public ColocationDatacenterBroker(String name,String dcName, String beScheduleAlg, String outputPath, double sla ) throws Exception{
        super(name);
        this.serviceToSchedule = new ArrayList<>();
        this.beJobsForSchedule = new ArrayList<>();
        this.dcName = dcName;
        this.userID = this.getId();
        this.beScheduler = new BeScheduler("beScheduler", beScheduleAlg, dcName, userID, this.getId(), sla);
        this.beStartAt = 100;
        this.jobPerSecond = 1;
        this.analyzer = new Analysis(outputPath);
        this.monitorCenter = new MonitorCenter();
        this.monitorCenter.setAnalyzer(analyzer);
        this.beScheduler.setAnalysis(analyzer);
        this.returnedRequests = new ArrayList<>();
    }

    public void setBeSchedulerExitNumber(int jobNumber){
        this.beScheduler.setTotalJobNumbers(jobNumber);
    }

    public void setOutput(boolean isOutput){
        this.analyzer.setDisableOutput(isOutput);
    }

    public void setWorkloadConfig(double lcStartTick, double beStartTick, double jobPerSecond) {
        lcLoadGenerator.setStartTime(lcStartTick);
        this.beStartAt = beStartTick;
        this.jobPerSecond = jobPerSecond;
    }

    public void setGraphServiceForScheduler(ServiceGraph graphservice) {
        this.graphservice = graphservice;
        if (this.beScheduler.scheduler.getClass().equals(DIASAlgorithm.class)){
            DIASAlgorithm da = (DIASAlgorithm) beScheduler.scheduler;
            da.setServiceGraph(graphservice);
        }
    }

    public int getBESchedulerID(){
        return beScheduler.getId();
    }

    public void setLcServicesForScheduler(List<ServiceEntity> forScheduler) {
        this.serviceToSchedule = forScheduler;
    }

    public List<? extends Cloudlet> getBeJobsForSchedule() {
        return beJobsForSchedule;
    }

    public void setBeJobsForSchedule(List<? extends Cloudlet> beJobsForSchedule) {
        this.beJobsForSchedule = beJobsForSchedule;
    }

    public void setLcLoadGenerator(LoadGenerator lcLoadGenerator) {
        lcLoadGenerator.setBrokerID(this.getId());
        this.lcLoadGenerator = lcLoadGenerator;
    }

    public void setBeLoadGenerator(BeLoadGenerator beLoadGenerator) {
        beLoadGenerator.setBrokerID(this.getId());
        this.beLoadGenerator = beLoadGenerator;
    }

    @Override
    public void startEntity() {
        Log.printConcatLine(getName(), " is starting...");
        schedule(getId(), 0, CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST);
        schedule(this.scheduler.getId(), 10, Constants.SCHEDULE_EVENT, serviceToSchedule);
        if (this.lcLoadGenerator == null) {
            Log.printLine("lc load generator is null, only be job");
        } else {
            this.lcLoadGenerator.genLoad();
        }

        if (this.beLoadGenerator == null) {
            Log.printLine("be load generator is null, only be job");
        } else {
            this.beLoadGenerator.genLoad();
        }

    }

    @Override
    public void processOtherEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case Constants.REQUEST_RETURN:
                processRequestReturn(ev, true);
                break;
            case Constants.REPORT_RT_TO_BROKER:
                processRequestReturn(ev, false);
                break;
            case Constants.SCHEDULE_BE_JOB:
                addBEJob(ev);
        }
    }

    @Override
    protected void processVmCreate(SimEvent ev){
        super.processVmCreate(ev);

        for (Vm vm : this.vmList) {
            ColocationHost cvm = (ColocationHost) vm;
            cvm.setMonitorCenter(this.monitorCenter.getId());
        }

        //update be scheduler's vm list
        CloudSim.send(this.getId(), beScheduler.getId(), 0, Constants.VM_UPDATE, this.vmList);
    }

    public void setQosCenter(QosCenter qosCenter) {
        this.qosCenter = qosCenter;
    }

    public void processRequestReturn(SimEvent ev, boolean recordREG){
        RequestHandleEvent requestHE = (RequestHandleEvent) ev.getData();
        Request req = requestHE.getRequest();
        this.qosCenter.updateRT(req.getService(), req.getEndTime()-req.getStartTime());
        this.analyzer.addRequest(req.getService(), req.getStartTime(), req.getEndTime()-req.getStartTime());
        if (recordREG) {
            List<RequestLog> requestLogs = req.getRequestLogs();
            if (this.graphservice != null) {
                CloudSim.send(this.getId(), this.graphservice.getId(), 0, Constants.REQUEST_LOGS, requestLogs);
            }
        }
    }

    void addBEJob(SimEvent ev) {
        ColocationJob job = (ColocationJob) ev.getData();
        schedule(this.beScheduler.getId(), 0, Constants.SCHEDULE_BE_JOB, job);
    }

    public List<Request> getReturnedRequests(){
        return this.returnedRequests;
    }

    public MonitorCenter getMonitorCenter(){
        return monitorCenter;
    }

    public void bindScheduler(Scheduler schld) {
        this.scheduler = schld;
    }

    public List<ColocationJob> getFinishedJobs(){
        return beScheduler.getFinishedJobs();
    }

    public void printReport(){
        this.analyzer.printReport();
    }

    @Override
    public void shutdownEntity(){
        super.shutdownEntity();
        this.analyzer.close();
    }
}
