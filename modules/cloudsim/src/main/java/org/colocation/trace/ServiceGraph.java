package org.colocation.trace;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.colocation.ColocationHost;
import org.colocation.Constants;
import org.colocation.RequestLog;
import org.colocation.ServiceEntity;
import org.colocation.qos.QosCenter;
import org.colocation.scheduler.mcr.ResourceRTFunction;
import org.colocation.scheduler.mcr.ServicePathSlack;

import java.util.*;
import java.util.function.Function;

/**
 * Created by wkj on 2019/3/20.
 */
public class ServiceGraph extends SimEntity {
    private QosCenter qosCenter;
    private Map<String, ServiceEntity> seMap;
    private Map<Integer, RequestGraph> requestGraphMap;

    private Map<Integer, ArrayList> hostGraphMap;

    private Map<Integer, RequestPath> requestPathMap;
    private Map<Integer, ArrayList<Integer>> hostPathMap;

    public ServiceGraph(QosCenter qosCenter){
        super("ServiceGraph");
        this.setQosCenter(qosCenter);
        this.seMap = new HashMap<>();
        this.requestGraphMap = new HashMap<>();
        this.requestPathMap = new HashMap<>();
        this.hostGraphMap = new HashMap<>();
        this.hostPathMap = new HashMap<>();
    }

    public void setQosCenter(QosCenter qosCenter) {
        this.qosCenter = qosCenter;
    }


    public void addServiceEntity(ServiceEntity se) {
        this.seMap.put(se.getName(), se);
    }
    @Override
    public void startEntity() {

    }

    @Override
    public void processEvent(SimEvent ev) {
        // receive request log and update graph
        int tag = ev.getTag();
        switch (tag) {
            case Constants.REQUEST_LOGS:
                List<RequestLog> logs = (List<RequestLog>) ev.getData();
                updateGraph(logs);
        }
    }

    @Override
    public void shutdownEntity() {

    }

    private void updateGraph(List<RequestLog> logs) {
        //step1: build a graph by this log
        //Log.printLine("recv request log, build graph");
        RequestGraph graph = new RequestGraph();
        List<RequestGraphNode> nodeList = new ArrayList<>();
        for (int i = 0; i < logs.size(); i++) {
            RequestLog log = logs.get(i);
            String eventID = log.getEventID();
            String svcName = log.getServiceName();
            int hostID = log.getHostID();
            long dataID = log.getDataId();
            RequestGraphNode node = new RequestGraphNode(eventID, log.getTimestamp(), log.getNetcomId(), dataID, svcName, hostID, 10);
            nodeList.add(node);
            graph.addVertex(node);
            if (i==0){
                graph.setStartNode(node);
            }
        }

        for (int i = 0; i < logs.size(); i++) {
            RequestLog log = logs.get(i);
            RequestGraphNode currNode = nodeList.get(i);
            int parentIndex = findParent(log, logs, i);
            if (parentIndex > 0) {
                //has parent
                RequestGraphNode parent = nodeList.get(parentIndex);

                graph.addEdge(parent, currNode);

            }
            int childIndex = findChild(log, logs, i);
            if (childIndex > 0) {
                //has child
                RequestGraphNode child = nodeList.get(childIndex);
                graph.addEdge(currNode, child);
            }
        }

        // simplify request graph

        //graph.simplify();

        graph.generatePath2();

        ArrayList<RequestPath> paths = graph.getPaths();
        for(RequestPath p : paths) {
            int pid = p.getID();
            if (this.requestPathMap.containsKey(pid)) {
                RequestPath oldPath = this.requestPathMap.get(pid);
                oldPath.mergePath(p);
            } else {
                this.requestPathMap.put(pid, p);
                List<Integer> hostList = p.getContainsHosts();
                for (int h : hostList) {
                    if (this.hostPathMap.containsKey(h)){
                        if (!this.hostPathMap.get(h).contains(pid)) {
                            this.hostPathMap.get(h).add(pid);
                        }
                    } else {
                        this.hostPathMap.put(h,new ArrayList());
                        this.hostPathMap.get(h).add(pid);
                    }
                }
            }
        }

        /**
        //merge to graph pool
        int graphID = graph.getToplogyId();
        if (graphID ==-4316523){
            //Log.printLine();
        }
        if (requestGraphMap.containsKey(graphID)) {
            RequestGraph graphOld = requestGraphMap.get(graphID);
            graphOld.mergeGraph(graph);
        } else {
            graph.generatePath();
            graph.setHistory();
            requestGraphMap.put(graphID, graph);

            //update path information
        }

        //Log.printLine(graphID+" simplified graph: "+ graph.toString());

        ArrayList<Integer> hosts = graph.involveHosts();
        for (int i = 0; i < hosts.size(); i++) {
            int hostID = hosts.get(i);
            if (!hostGraphMap.containsKey(hostID)){
                hostGraphMap.put(hostID, new ArrayList());
            }
            ArrayList arr = hostGraphMap.get(hostID);
            if ( !arr.contains(graphID) ) {
                arr.add(graphID);
            }
        }
        //Log.printLine("graph exec end");
         **/
    }

    private int findParent(RequestLog log, List<RequestLog> logs, int logPos) {
        //recv event: find send event with same dataid
        //other event: find nearest event with same netcomid

        if (logPos >=logs.size()) {
            return -1;
        }
        logPos = logPos -1;
        for (; logPos >=0; logPos--){
            RequestLog currLog = logs.get(logPos);
            if ( currLog.hasChild(log) ) {
                return logPos;
            }
        }
        return -1;
    }

    private int findChild(RequestLog log, List<RequestLog> logs, int logPos) {
        // send event: find recv event with same dataid
        // other event: find nearest event with same netcomid
        if (logPos < 0) {
            return -1;
        }
        logPos = logPos + 1;
        for (; logPos < logs.size() && logPos>=0 ; logPos++){
            RequestLog currLog = logs.get(logPos);
            if ( currLog.hasParent(log) ) {
                return logPos;
            }
        }
        return -1;
    }

    public double getWorstQosIntervalTime(ServiceEntity se) {
        /**
         1.get longest time of path that cross this node
         2.get the worst time of this service
         * **/
        double longestTimeCrossNode = getLongestTimeCrossNode(se);
        double upper = qosCenter.getServiceUpperRT(se.getApp());

        return upper - longestTimeCrossNode;
    }

    public double getLongestTimeCrossNode(ServiceEntity se){
        int[] path1 = {1,2,5};
        int[] path2 = {1,3,5};
        int[] path3 = {1,4,5};
        ArrayList<Double> costList = new ArrayList<>();
        switch (se.getName()){
            case "service1":
                Log.printLine("find service1 longest path");
                costList.add(getPathExecTime(path1));
                costList.add(getPathExecTime(path2));
                costList.add(getPathExecTime(path3));
                break;
            case "service2":
                Log.printLine("find service2 longest path");
                costList.add(getPathExecTime(path1));
                break;
            case "service3":
                Log.printLine("find service3 longest path");
                costList.add(getPathExecTime(path2));
                break;
            case "service4":
                Log.printLine("find service4 longest path");
                costList.add(getPathExecTime(path3));
                break;
            case "service5":
                Log.printLine("find service5 longest path");
                costList.add(getPathExecTime(path1));
                costList.add(getPathExecTime(path2));
                costList.add(getPathExecTime(path3));
                break;
        }
        double max = Collections.max(costList);
        Log.printLine("find longest path. cost:"+max);
        return max;
    }

    private double getPathExecTime(int[] pathSe){
        double cost = 0;
        for (int i = 0; i < pathSe.length; i++) {
            String serviceName = "service"+pathSe[i];
            ServiceEntity se = this.seMap.get(serviceName);
            if (se == null) {
                Log.printLine(serviceName);
            }
            cost = cost + se.getWholeExecTime();
        }
        return cost;
    }
    public double getCurrTimeOfService(ServiceEntity se) {
        return se.getWholeExecTime();
    }

    public ArrayList<ServicePathSlack> getPathAndSlack2(ColocationHost host, double sla){
        int hostID = host.getId();
        ArrayList<ServicePathSlack> res = new ArrayList<>();
        if (!hostPathMap.containsKey(hostID)) {
            //this host dose not contains any service return whole mem bandwidth
            Log.printLine("Waring! this server has no services:"+hostID);
            double totalBW = host.getTotalMemBW();
            ServicePathSlack sps = new ServicePathSlack();
            sps.addFunction( new XEqualY() );
            sps.setSlack(totalBW);
            res.add(sps);
            return res;
        }
        ArrayList<Integer> pathIDs = hostPathMap.get(hostID);

        for (int j = 0; j < pathIDs.size(); j++) {

            RequestPath path = requestPathMap.get(pathIDs.get(j));
            //get upper RT
            String apiName = path.getRootServiceName();
            double bestRT = qosCenter.getBestRTof(apiName);
            double maxRT = bestRT/sla;
            double slack = maxRT - path.pathValue();
            ServicePathSlack sps = new ServicePathSlack();
            sps.setSlack(slack);

            for (int k = 0; k < path.size(); k++) {
                RequestGraphNode pathStep = path.get(k);

                if ( hostID == pathStep.getHostID() ) {
                    sps.addServiceStage(pathStep);
                }
            }
            res.add(sps);
        }

        return res;
    }

    public ArrayList<ServicePathSlack> getPathAndSlack(ColocationHost host){
        int hostID = host.getId();
        ArrayList<ServicePathSlack> res = new ArrayList<>();
        if (!hostGraphMap.containsKey(hostID)) {
            //this host dose not contains any service return whole mem bandwidth
            double totalBW = host.getTotalMemBW();
            ServicePathSlack sps = new ServicePathSlack();
            sps.addFunction( new XEqualY() );
            sps.setSlack(totalBW);
            res.add(sps);
            return res;
        }
        ArrayList<Integer> graphIDs = hostGraphMap.get(hostID);
        for (int i = 0; i < graphIDs.size(); i++) {
            int graphID = graphIDs.get(i);
            RequestGraph graph = requestGraphMap.get(graphID);
            ArrayList<RequestPath> pathsHasHost = graph.pathFilter(hostID);

            for (int j = 0; j < pathsHasHost.size(); j++) {
                RequestPath path = pathsHasHost.get(j);
                //get upper RT
                String apiName = path.getRootServiceName();
                double maxRT = qosCenter.getServiceUpperRT(apiName);

                double slack = maxRT - path.pathValue();
                ServicePathSlack sps = new ServicePathSlack();
                sps.setSlack(slack);
                HashSet<String> visitedSvc = new HashSet<>();
                for (int k = 0; k < path.size(); k++) {
                    RequestGraphNode pathStep = path.get(k);
                    String svcName = pathStep.getSvcName();

                    if ( (hostID == pathStep.getHostID()) && (!visitedSvc.contains(visitedSvc))  ) {
                        ServiceEntity se = (ServiceEntity) CloudSim.getEntity(svcName);
                        double currRT = path.getSvcValue(svcName);
                        sps.addFunction( new ResourceRTFunction(se.getSensitiveFun(), se.getPeerPressure(), se.getTotalBW() , currRT));
                    }
                }
                res.add(sps);
            }
        }
        return res;
    }

    class XEqualY implements Function {

        XEqualY() {
        }

        @Override
        public Object apply(Object o) {
            return o;
        }
    }
}
