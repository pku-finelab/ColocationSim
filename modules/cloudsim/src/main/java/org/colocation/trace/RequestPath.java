package org.colocation.trace;

import org.cloudbus.cloudsim.core.CloudSim;
import org.colocation.ServiceEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by wkj on 2019/8/3.
 */
public class RequestPath {
    ArrayList<RequestGraphNode> pathNodes;
    HashSet<Integer> hostSet;
    int pid;
    public RequestPath(){
        this.pathNodes = new ArrayList<>();
        this.hostSet = new HashSet<>();
    }
    public RequestPath(RequestGraphNode firstNode) {
        this();
        this.add(firstNode);
    }

    public void add(RequestGraphNode node){
        this.pathNodes.add(node);
        int hostID = node.getHostID();
        if ( !hostSet.contains(hostID) ){
            this.hostSet.add(hostID);
        }
    }

    public RequestPath deepAdd(RequestGraphNode node){
        RequestPath newPath = new RequestPath();
        for (RequestGraphNode n: pathNodes){
            newPath.add(n);
        }
        newPath.add(node);
        return newPath;
    }

    public RequestGraphNode get(int pos){
        return pathNodes.get(pos);
    }

    public int size(){
        return pathNodes.size();
    }

    public RequestGraphNode getLastOne() {
        if ( pathNodes.size() >0 ) {
            return pathNodes.get(pathNodes.size()-1);
        }
        return null;
    }

    public void mergePath(RequestPath another){
        for (int i = 0; i< this.pathNodes.size(); i++) {
            this.pathNodes.get(i).mergeNode(another.get(i));
        }
    }

    public List<Integer> getContainsHosts(){
        ArrayList l = new ArrayList();
        for (RequestGraphNode n: this.pathNodes) {
            l.add(n.getHostID());
        }
        return l;
    }

    public String getRootServiceName(){
        if ( pathNodes.size() >0 ) {
            return pathNodes.get(0).getSvcName();
        }
        return "";
    }

    public boolean hasHost(int hostID){
        return hostSet.contains(hostID);
    }

    public ArrayList getNodeOnHost(int hostID){
        ArrayList res = new ArrayList();
        for (int i = 0; i < pathNodes.size(); i++) {
            RequestGraphNode node = pathNodes.get(i);
            if (node.getHostID() == hostID) {
                res.add(node);
            }
        }
        return res;
    }

    public void genID(){
        String s = "";
        for (RequestGraphNode n: pathNodes) {
            s = s + n.getSvcName();
        }
        this.pid = s.hashCode();
    }

    public int getID() {
        return pid;
    }

    public double pathValue(){
        double pathValue = 0.0;
        for( RequestGraphNode node: pathNodes) {
            String svcName = node.getSvcName();
            ServiceEntity se = (ServiceEntity) CloudSim.getEntity(svcName);
            double bestStageValue = node.getMinCost();
            double perf = se.getPerfLoss();
            double currStageValue = bestStageValue/perf;
            pathValue += currStageValue;
        }
        return pathValue;
        /**
        RequestGraphNode first = pathNodes.get(0);
        double start = first.getStart();
        RequestGraphNode last = pathNodes.get(pathNodes.size()-1);
        double end = last.getEnd();
        double res = end- start;
        return res;
         ***/
    }

    public void simplify() {
        ArrayList<RequestGraphNode> simPath = new ArrayList<>();
        for (RequestGraphNode n : this.pathNodes) {
            if(simPath.isEmpty()) {
                simPath.add(n);
            } else {
                RequestGraphNode privious = simPath.get(simPath.size()-1);
                if (n.getNetcomID() == privious.getNetcomID()){
                    privious.mergeEvent(n);
                } else {
                    simPath.add(n);
                }
            }
        }
        this.pathNodes = simPath;
    }

    public double getSvcValue(String svcName){
        double res = 0.0;
        for (int i = 0; i < pathNodes.size(); i++) {
            RequestGraphNode node = pathNodes.get(i);
            if ( node.getSvcName() == svcName) {
                res = res + node.value();
            }
        }
        return res;
    }

    public double getSvcBestValue(String svcName){
        double res = 0.0;
        for (int i = 0; i < pathNodes.size(); i++) {
            RequestGraphNode node = pathNodes.get(i);
            if ( node.getSvcName() == svcName) {
                res = res + node.getMinCost();
            }
        }
        return res;
    }
}
