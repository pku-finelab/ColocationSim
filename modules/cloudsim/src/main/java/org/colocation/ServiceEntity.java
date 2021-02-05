package org.colocation;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.colocation.exceptions.ServiceNotStartedException;
import org.colocation.pressureFunction.IMemPressureFunction;
import org.colocation.qos.QosCenter;
import org.colocation.sensitiveFunction.AbstractSensitiveFunction;
import org.colocation.trace.DataIDGenerator;
import org.colocation.usage.CpuUsageModel;
import org.colocation.usage.LinearCpuUsage;
import org.colocation.usage.LinearMemUsage;
import org.colocation.usage.MemUsageModel;

import java.util.*;

/**
 * Created by wkj on 2019/3/7.
 */
public class ServiceEntity extends SimEntity {

    public ServiceEntity(String id, String app, int priority, double cpuQuota, String DCName, int UserID, int memQuota, Program program, AbstractSensitiveFunction sensitiveFun, int cpushare) {
        this(id, app, priority, cpuQuota, DCName, UserID, memQuota, program, sensitiveFun, cpushare, new LinearCpuUsage(1, 1, Math.round(CloudSim.clock()) ), new LinearMemUsage(200*Constants.MB, 10*Constants.KB, 200*Constants.KB));
    }

    public ServiceEntity(String id, String app, int priority, double cpuQuota, String DCName, int UserID, int memQuota, Program program, AbstractSensitiveFunction sensitiveFun, int cpushare, CpuUsageModel cpuUsageModel, MemUsageModel memUsageModel
    ) {
        super(id);
        setUtilCpu(0.0);
        setUtilMem(0.0);
        setUtilMemBW(0.0);
        setCPUQuota(cpuQuota);
        setMemQuota(memQuota);
        this.program = program;
        this.app = app;
        requestContext = new HashMap<>();
        requestWaitGroup = new HashMap<>();
        this.DatacenterName = DCName;
        this.userID = UserID;
        this.sensitiveFun = sensitiveFun;
        this.priority = priority;
        this.cpushare = cpushare;
        this.finishedRequests = new HashSet<>();
        this.cpuUsageModel = cpuUsageModel;
        this.memUsageModel = memUsageModel;
        this.processedRequestsNum = 0;
        this.brokerID = -1;
    }

    private String app;
    private double utilCpu;
    private double utilMem;
    private double utilMemBW;
    private double CPUQuota;
    private int cpushare;
    private int MemQuota;
    private int VMId;
    private String DatacenterName;
    private int userID;
    private AbstractSensitiveFunction sensitiveFun;
    private IMemPressureFunction memPressureFun;
    private ColocationHost vm;
    private double currentRT = -1;
    private double maxRT = -1;

    private QosCenter qosCenter;
    private int containerID;

    private int brokerID;

    // > 50 is lc, <=50 is be
    private int priority;

    private Program program;
    private Map<String, RequestHandleEvent> requestContext;
    private Map<String, Integer> requestWaitGroup;
    private Set<String> finishedRequests;
    private CpuUsageModel cpuUsageModel;
    private MemUsageModel memUsageModel;
    private long processedRequestsNum;

    public int getPriority() {
        return priority;
    }
    public int getCpushare(){
        return this.cpushare;
    }
    public boolean isLc(){
        if (this.priority>50){
            return true;
        }
        return false;
    }

    public void setMaxRT(double maxRT){
        this.maxRT = maxRT;
    }

    public void setBrokerID(int brokerID) {
        this.brokerID = brokerID;
    }

    public Program getProgram() {
        return this.program;
    }

    public int getContainerID() {
        return containerID;
    }

    public void setContainerID(int containerID) {
        this.containerID = containerID;
    }

    public double convertTimeToBW(double startRT, double deltaRT){
        ColocationHost vm = getVM();
        double error = 0.001;
        // find target bw that up to rt=start+delta
        // assume the perf loss function is monotonic function
        double totalBW = vm.getBw();

        double bw1 = binSearch(0, startRT, totalBW, error);
        double bw2 = binSearch(0, startRT+deltaRT, totalBW, error);

        return  bw2 - bw1;
    }

    private double binSearch(double startBW, double targetRT, double endBW, double error) {
        double startRT = this.getRTByBw(startBW);
        double endRT = this.getRTByBw(endBW);
        double middBW= (startBW + endBW)/2;
        double middRT = this.getRTByBw(middBW);
        while (Math.abs(this.getRTByBw(middBW)-targetRT) > error) {
            double interval = middRT - targetRT;
            if ( interval > 0 ) {
                // reduce endbw
                endBW = (endBW + startBW)/2;
                endRT = this.getRTByBw(endBW);
            } else {
                startBW = (endBW + startBW)/2;
                startRT = this.getRTByBw( startBW);
            }
            middBW = (startBW + endBW)/2;
            middRT = this.getRTByBw(middBW);
            if (Math.abs(startRT- endRT) < error)
                break;
        }
        return middBW;
    }

    public double getRTByBw(double bw) {
        //get total instructions
        long allInstr = this.getAllInstructions();
        double timeCost = allInstr/getActualMIPS();
        double perfLoss = this.sensitiveFun.LossByMem(bw, this.vm.getTotalMemBW());
        return timeCost/perfLoss;
    }


    public void setMemPressureFun(IMemPressureFunction memPressureFun) {
        this.memPressureFun = memPressureFun;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getVMId() {
        return VMId;
    }

    public void setVM(ColocationHost vm) {
        this.vm = vm;
        this.setVMId(vm.getId());
    }

    public void setVMId(int VMId) {
        if ((this.vm == null) || (this.vm.getId() != VMId) ) {
            Datacenter dc = (Datacenter) CloudSim.getEntity(this.DatacenterName);
            List<Vm> vms = dc.getVmList();
            for (Vm vm : vms) {
                if (vm.getId() == VMId) {
                    this.vm = (ColocationHost) vm;
                    this.VMId = VMId;
                    return;
                }
            }
        }

        //Log.printLine("VmMIPS Clock"+CloudSim.clock()+", vmid:"+this.VMId);
    }

    public void setQosCenter(QosCenter qosCenter) {
        this.qosCenter = qosCenter;
    }

    public double getCPUQuota() {
        return CPUQuota;
    }

    public void setCPUQuota(double CPUQuota) {
        this.CPUQuota = CPUQuota;
    }

    public int getMemQuota() {
        return MemQuota;
    }

    public void setMemQuota(int memQuota) {
        MemQuota = memQuota;
    }

    public double getUtilCpu() {
        return utilCpu;
    }

    public void setUtilCpu(double utilCpu) {
        this.utilCpu = utilCpu;
    }

    public double getUtilMemBW() {
        return utilMemBW;
    }

    public void setUtilMemBW(double utilMemBW) {
        this.utilMemBW = utilMemBW;
    }

    public double getUtilMem() {
        return utilMem;
    }

    public void setUtilMem(double utilMem) {
        this.utilMem = utilMem;
    }

    public ColocationHost getVm() {
        return vm;
    }

    public String getApp() {
        return app;
    }

    /**
     * This method is invoked by the {@link CloudSim} class when the simulation is started.
     * It should be responsible for starting the entity up.
     */
    @Override
    public void startEntity() {
        Log.printLine("start service: "+ this.getName());
    }

    /**
     * Processes events or services that are available for the entity.
     * This method is invoked by the {@link CloudSim} class whenever there is an event in the
     * deferred queue, which needs to be processed by the entity.
     *
     * @param ev information about the event just happened
     * @pre ev != null
     * @post $none
     */
    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()){
            case Constants.HANDEL_REQUEST:
                processRequest(ev);
                break;
            case Constants.UPDATE_PERFORMANCE:
                updateInterference();
                break;
            case Constants.REQUEST_RETURN:
                processReturnRequest(ev);
                break;
            case Constants.RESTART_CONTAINER:
                restart();
        }
    }

    public void restart(){
        this.processedRequestsNum = 0;
    }

    /**
     * Shuts down the entity.
     * This method is invoked by the {@link CloudSim} before the simulation finishes. If you want
     * to save data in log files this is the method in which the corresponding code would be placed.
     */
    @Override
    public void shutdownEntity() {

    }

    private void log(String log){
        //condition
        double time = CloudSim.clock();
        boolean condition = this.getName().equals("search-render");
        if (condition){
            Log.printLine(log);
        }
    }

    private void processRequest(SimEvent event){
        RequestHandleEvent reqHandEvent = (RequestHandleEvent) event.getData();
        Request request = reqHandEvent.getRequest();
        String reqID = reqHandEvent.requestID;

        if(reqID.equals("readPost_tick.48_no.0")){
            this.log("");
        }

        if (this.finishedRequests.contains(reqID)) {
            return;
        }
        if (!this.requestContext.containsKey(reqID) ) {
            //record request context
            request.setStartTime(CloudSim.clock());
            RequestHandleEvent newOne = new RequestHandleEvent(request,event.getSource(), this.program);

            this.log("handel new request:"+ newOne.requestID);

            //recv event
            long dataID = reqHandEvent.getDataID();
            newOne.setDataID(dataID);

            RequestLog log = this.newLog( dataID, newOne.getNetcomID(), event.getSource(), newOne.getRequest().getId(), Constants.FTYPE_RECV);
            newOne.addLogEvent(log);

            this.requestContext.put(reqID, newOne);
            if (vm == null) {
                Log.printLine();
            }
            this.vm.updateUtilization();

        } else{
            double now = CloudSim.clock();
            double lastEstiTime = reqHandEvent.getLastEstimateFinishClock();
            //Log.printLine(reqHandEvent.requestID +" continue, estimate:"+lastEstiTime);
            if (now < lastEstiTime){
                return;
            }



            //Log.printLine(reqHandEvent.requestID +" step end:" + currStep);
            // performance not degrade, finished on time. move to next step
            if (reqHandEvent.isBlocked()) {
                this.log("waiting: "+ reqID);
                return;
            } else {
                this.log(reqHandEvent.requestID + " move to next step");
                // stage end add exec event
                RequestLog log = this.newLog(reqHandEvent.getDataID(), reqHandEvent.getNetcomID(), this.getId(), reqHandEvent.getRequest().getId(), Constants.FTYPE_EXEC);
                reqHandEvent.addLogEvent(log);
                reqHandEvent.moveToNextStep();
            }
            this.requestContext.put(reqID, reqHandEvent);

        }
        //request.setEndTime(CloudSim.clock());
        reqHandEvent = this.requestContext.get(reqID);
        if (reqHandEvent.isFinished()){
            // request end send to source entity
            int dest = this.requestContext.get(reqID).getFrom();
            request.setEndTime(CloudSim.clock());
            this.requestContext.remove(reqID);
            this.finishedRequests.add(reqID);
            this.log(reqID+ "finished");
            this.vm.updateUtilization();

            //TODO ADD send event log
            long dataID = DataIDGenerator.getNextDataID();
            reqHandEvent.setDataID(dataID);
            RequestLog log = this.newLog( dataID, reqHandEvent.getNetcomID(), dest, reqHandEvent.getRequest().getId(), Constants.FTYPE_SEND);
            reqHandEvent.addLogEvent(log);

            // send to source
            send(dest, 0, Constants.REQUEST_RETURN, reqHandEvent);

            if (dest != this.brokerID) {
                // send to broker
                //send(this.brokerID, 0, Constants.REPORT_RT_TO_BROKER, reqHandEvent);
            }

            // record current response time
            Request req = reqHandEvent.getRequest();
            double cost = req.getEndTime() - req.getStartTime();
            this.currentRT = cost;

            return;
        }

        int stepIndex = reqHandEvent.getCurrStep();
        Procedure p = this.program.get(stepIndex);
        if (p.hasDep) {
            try {

            List<String> deps = p.getDepServices();
            reqHandEvent.setBlocked(true);
            for (int i = 0; i < deps.size(); i++) {
                //new Request
                String depServiceName = deps.get(i);
                double now = CloudSim.clock();
                Request reqNext = new Request(request.getId()+"_"+depServiceName+"_"+now, depServiceName, this.getName(), request);
                this.log("new request:"+ reqNext.getId());
                long dataID = DataIDGenerator.getNextDataID();
                long netcomID = reqHandEvent.getNetcomID();
                ServiceEntity depSe = (ServiceEntity) CloudSim.getEntity(depServiceName);
                RequestHandleEvent requestHandleEvent = new RequestHandleEvent(reqNext, this.getId(), depSe.program);
                requestHandleEvent.setDataID(dataID);
                if ( (null == depSe) || (!depSe.isRunning() )){
                    // target service is not running, throw exception
                    Log.printLine("ERROR: "+depServiceName+" not started OR not exist");
                    throw new ServiceNotStartedException(depServiceName+" not started OR not exist");
                }
                RequestLog log = this.newLog( dataID, netcomID, depSe.getId(), request.getId(), Constants.FTYPE_SEND);
                reqHandEvent.addLogEvent(log);
                CloudSim.send(this.getId(), depSe.getId(), 0, Constants.HANDEL_REQUEST, requestHandleEvent);
            }

            // wait
            requestWaitGroup.put(reqID, p.getWaitNum());
            return;

            } catch (ServiceNotStartedException e) {
                Log.printLine("Error: "+ e.getMessage());
                e.printStackTrace();
                return;
            }
        } else {
            // handle this procedure without dependency service
            // new step
            double mips = getActualMIPS();
            double instructions = (double) p.getInstructionNum();
            double timeCost = instructions/(mips* Constants.MILLION);
            double estimateFinishTime = CloudSim.clock()+timeCost;
            reqHandEvent.setCurrStepInstrFinishedNum(0);
            double perf = this.getPerfLoss();
            this.log(reqID + " step "+ reqHandEvent.getCurrStep()+" first estimate end at: " + estimateFinishTime +" perf:"+perf);
            reqHandEvent.setLastEstimateFinishClock(estimateFinishTime);
            reqHandEvent.setLastExecActualMips(mips);
            reqHandEvent.setLastStartTime(CloudSim.clock());
            reqHandEvent.setCurrStepIsRunning();
            //Log.printLine(reqID+" step "+stepIndex+" estimate end:"+ (CloudSim.clock()+timeCost) );
            this.requestContext.put(reqID, reqHandEvent);
            CloudSim.send( this.getId(), this.getId(), timeCost, Constants.HANDEL_REQUEST, reqHandEvent);
            return;
        }
    }

    public void updateInterference() {
        //update all request finish time in context
        this.log(this.getName() + " updateInterference");
        for( Map.Entry<String, RequestHandleEvent> entry:  this.requestContext.entrySet()) {
            String key = entry.getKey();
            //Log.printLine("updateInterference:"+key);
            double now = CloudSim.clock();
            RequestHandleEvent requestHandleEvent = entry.getValue();
            if (requestHandleEvent.isFinished() || this.finishedRequests.contains(key) || !requestHandleEvent.isCurrStepIsRunning() || requestHandleEvent.isBlocked()) {
                continue;
            }
            if (this.requestWaitGroup.containsKey(requestHandleEvent.requestID)){
                int waitNum = this.requestWaitGroup.get(requestHandleEvent.requestID);
                if (waitNum>0){
                    this.log(requestHandleEvent.requestID+" is blocked, skip perf update.");
                    continue;
                }
            }
            long finishdIns = requestHandleEvent.getCurrStepInstrFinishedNum();
            double lastStartTime = requestHandleEvent.getLastStartTime();
            double lastFinishTime = requestHandleEvent.getLastEstimateFinishClock();
            double lastMips = requestHandleEvent.getLastExecActualMips();
            long execInsNum = Math.round(Math.floor((now - lastStartTime) * lastMips * Constants.MILLION));
            long nowFinishdIns = finishdIns + execInsNum;
            this.log(key+" finished instruction num: "+ nowFinishdIns);
            long totalInsNum = requestHandleEvent.getCurrStepInstrNum();
            if (nowFinishdIns > totalInsNum) {
                Log.printLine("Error: finishedIns < totalInsNum");
            }
            long remaningInsNum = totalInsNum - nowFinishdIns;
            this.log(key+" step "+requestHandleEvent.getCurrStep()+" remaining instruction num: "+ remaningInsNum);
            double aMips = this.getActualMIPS();
            double newEstimateCost = remaningInsNum/(aMips* Constants.MILLION);
            double newEstiFinishTime = now + newEstimateCost;
            if (newEstiFinishTime == lastFinishTime) {
                // no changes
                continue;
            }
            if (newEstiFinishTime<lastFinishTime){
                //Log.printLine("ahead of finish :"+ requestHandleEvent.getRequest().getId());
            }
            this.log(key+" perf loss: "+this.getPerfLoss()+" mips:"+aMips+" need:"+newEstimateCost);
            this.log(key+" lastEstiTime:"+lastFinishTime+" new EstimateFinishTime:"+newEstiFinishTime);
            requestHandleEvent.setLastEstimateFinishClock(newEstiFinishTime);
            requestHandleEvent.setCurrStepInstrFinishedNum(nowFinishdIns);
            requestHandleEvent.setLastStartTime(now);
            requestHandleEvent.setLastExecActualMips(aMips);
            this.requestContext.put(key, requestHandleEvent);
            CloudSim.send(this.getId(), this.getId(), newEstimateCost, Constants.HANDEL_REQUEST, requestHandleEvent);
        }
    }

    private RequestLog newLog(long dataID, long netcomID, int remoteID, String requestID, int functionType){
        RequestLog log = new RequestLog(this.getName(), CloudSim.clock(), dataID, netcomID, this.getId(), remoteID, requestID, functionType, this.getVMId());
        return log;
    }

    public boolean isRunning(){
        if (this.vm == null){
            return false;
        }
        return true;
    }

    private ColocationHost getVM(){
        return this.vm;
    }
    private double getActualMIPS(){
        ColocationHost vm = this.getVM();
        return vm.getActualMips(this.containerID);
    }

    public double getPerfLoss(){
        ColocationHost vm = this.getVM();
        double pressure = vm.getPressure(this.getContainerID());
        double totalBW = vm.getTotalMemBW();
        double perf =  this.sensitiveFun.getCurrRealPerf(pressure);
        if (perf >1){
            return  1.0;
        }
        return perf;
    }

    public double getPeerPressure() {
        double pressure = vm.getPressure(this.getContainerID());
        return pressure;
    }

    public double getTotalBW() {
        return vm.getTotalMemBW();
    }

    public AbstractSensitiveFunction getSensitiveFun() {
        return sensitiveFun;
    }

    private void processReturnRequest(SimEvent event) {
        RequestHandleEvent requestEventReturned = (RequestHandleEvent) event.getData();
        Request requestReturned = requestEventReturned.getRequest();

        String parentRequestID = requestReturned.getParentRequestId();
        if (parentRequestID.equals("")) {
            // error
            Log.printLine("service entity received request that parent is null");
            return;
        }

        if ( !this.requestWaitGroup.containsKey(parentRequestID)) {
            Log.printLine("none");
        }
        this.log(this.getName() +" get returned request:"+requestEventReturned.getRequest().getId());
        int waitNum = this.requestWaitGroup.get(parentRequestID);
        if (waitNum > 0) {
            RequestHandleEvent reqHandEvent = this.requestContext.get(parentRequestID);
            if (reqHandEvent == null ) {
                Log.printLine("reqHandEvent is null");
            }
            reqHandEvent.mergeLogEvent(requestReturned.getRequestLogs());
            // recv event
            long dataID = requestEventReturned.getDataID();
            reqHandEvent.setDataID(dataID);
            RequestLog log = this.newLog(dataID, reqHandEvent.getNetcomID(), event.getSource(), reqHandEvent.getRequest().getId
                    (), Constants.FTYPE_RECV);
            reqHandEvent.addLogEvent(log);
            this.requestContext.put(parentRequestID, reqHandEvent);
        }

        // check if the waitGroup has been finished
        waitNum = waitNum -1;


        if (waitNum == 0 ){
            // return
            this.log(parentRequestID+" waitGroup is zero");
            this.requestWaitGroup.put(parentRequestID, 0);
            RequestHandleEvent context = this.requestContext.get(parentRequestID);
            context.setBlocked(false);
            this.requestContext.put(parentRequestID, context);
            CloudSim.send(this.getId(), this.getId(),0, Constants.HANDEL_REQUEST, context);
            return;
        }
        this.requestWaitGroup.put(parentRequestID, waitNum);
    }


    public double getCurrMemBW(){
        double currBW = this.memPressureFun.getMemBWbyRequestNum(this.requestContext.size());
        return currBW;
    }

    public long getAllInstructions() {
        long total = 0;
        for (Procedure p : this.program.getAllProcedure()) {
            if ( !p.hasDep ) {
                total = total + p.getInstructionNum();
            }
        }
        return total;
    }

    public double getWholeExecTime(){
        long length = getAllInstructions();
        double mips = this.getVM().getMips();
        double perfLoss = this.getPerfLoss();
        return (length/mips)/perfLoss;
    }


    public double getCpuUsage() {
        return this.cpuUsageModel.getCpuUsageByRequestNum(this.requestContext.size());
    }

    public long getMemUsage() {
        long usage = this.memUsageModel.getMemUsageByRequestNum(this.processedRequestsNum, this.requestContext.size());
        this.log(this.getName()+ " mem:"+ usage+" B");
        return usage;
    }

    public void updateServiceNameInProcedure(String prefix) {
        this.program.setServicePrefix(prefix);
    }

    public double getContributedResource(){
        double slack = this.maxRT - this.currentRT;
        if (slack < 0) {
            Log.printLine("WARNING: slack is negative"+this.getName()+" slack:"+slack +" maxRT:"+maxRT+" currRT:"+currentRT);
            return -1;
        }
        double tolerantBW = this.convertTimeToBW(currentRT, slack);
        return tolerantBW;
    }
}


