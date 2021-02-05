package org.colocation.scheduler;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.colocation.ColocationHost;
import org.colocation.ServiceEntity;
import org.colocation.bestEffort.ColocationTask;
import org.workflowsim.scheduling.BaseSchedulingAlgorithm;

import java.util.*;

/**
 * Created by wkj on 2019/3/14.
 */
public class CooperAlgorithm extends BaseSchedulingAlgorithm {
    public CooperAlgorithm(double sla){
        super();
        this.setCloudletList(new ArrayList<>());
        Log.printLine("create Least Load scheduler");
        this.sla = 0.0;
    }
    private double sla;
    /**
     * The main function
     *
     * @throws Exception
     */
    @Override
    public void run() throws Exception {
        // score
        // weight
        // remove success scheduled tasks
        List<ColocationHost> vmList = getVmList();
        List<ColocationTask> taskToRemove = new ArrayList<>();
        List<ColocationTask> tasks = this.getCloudletList();
        if (tasks.size() ==0) {
            return;
        }
        int maxIter = 10*tasks.size();
        int iterNum = 0;
        Queue<TaskElement> singleTasks = new LinkedList<>();
        HashMap<ColocationHost, HostElement> chMap = new HashMap<>();
        HashMap<ColocationTask, TaskElement> teMap = new HashMap<>();
        for (ColocationTask t : tasks) {
            TaskElement te = new TaskElement(t);
            te.genScores(vmList);
            singleTasks.add(te);
            teMap.put(t, te);
        }
        for (ColocationHost host: vmList) {
            HostElement he = new HostElement(host);
            he.genScores(tasks);
            chMap.put(host, he);
        }
        while((iterNum < maxIter) && (!singleTasks.isEmpty())) {
            //Log.printLine("iter: "+iterNum);
            iterNum ++;
            TaskElement t = singleTasks.poll();
            ColocationHost h = t.getMostWantedHost();
            if (h == null) {
                singleTasks.add(t);
                continue;
            }
            HostElement he = chMap.get(h);
            if (he.agree(t.getTask())){
                if (he.isSingle()) {
                    he.setTask(t.getTask());
                    t.setHost(h);
                } else {
                    ColocationTask oldTask = he.getPair();
                    TaskElement oldTe = teMap.get(oldTask);
                    singleTasks.add(oldTe);
                    oldTe.setHost(null);
                    he.setTask(t.getTask());
                    t.setHost(h);
                }
                singleTasks.remove(t);
            } else {
                t.addVisited(h);
                singleTasks.add(t);
            }
        }
        Log.printLine("iter: "+iterNum);
        for (ColocationTask t : tasks) {
            TaskElement te = teMap.get(t);
            ColocationHost targetHost = te.getHost();
            if (targetHost!= null) {
                int schRes = t.tryRunningOnHost(targetHost.getId());
                if (schRes > 0) {
                    Log.printLine(CloudSim.clock()+": schedule task #"+t.getTaskFullName()+" to vm:#"+targetHost.getId());
                    taskToRemove.add(t);
                }
            }
        }

        for (ColocationTask t: taskToRemove) {
            tasks.remove(t);
        }
        this.setCloudletList(tasks);
    }

    class TaskElement {
        ColocationTask self;
        ColocationHost host;
        HashMap<ColocationHost, Double> scores;
        List<HostAndValue> sortedScores;
        Set<ColocationHost> vistedHosts;

        public TaskElement(ColocationTask self) {
            this.self = self;
            this.host = null;
            this.scores = new HashMap<>();
            this.sortedScores = new ArrayList<>();
            this.vistedHosts = new HashSet<>();
        }

        public ColocationHost getHost() {
            return host;
        }



        public void genScores(List<ColocationHost> hosts) {
            for (ColocationHost h : hosts) {
                double currPressure = h.getCurrentBWPressure();
                double score = this.self.getSensitiveFunction().LossByMem(currPressure, 0);
                this.scores.put(h, score);
                this.sortedScores.add(new HostAndValue(h, score));
            }
            this.sortedScores.sort(Comparator.reverseOrder());
            //Log.printLine("gen hosts scores end");
        }

        public void setHost(ColocationHost host) {
            this.host = host;
        }

        public ColocationTask getTask() {
            return self;
        }

        public boolean isSingle() {
            if (host == null) {
                return true;
            }
            return false;
        }
        public ColocationHost getMostWantedHost(){
            for (HostAndValue hav : sortedScores) {
                ColocationHost h = hav.getHost();
                if (!vistedHosts.contains(h)) {
                    return h;
                }
            }
            return null;
        }
        public void addVisited(ColocationHost h){
            this.vistedHosts.add(h);
        }
    }

    class HostElement {
        ColocationTask pair;
        ColocationHost self;
        HashMap<ColocationTask, Double> scores ;

        public HostElement(ColocationHost self) {
            this.self = self;
            this.pair = null;
            this.scores = new HashMap<>();
        }
        public boolean isSingle() {
            if (pair == null) {
                return true;
            }
            return false;
        }

        public ColocationTask getPair() {
            return pair;
        }

        public boolean agree(ColocationTask task) {
            double score = this.scores.get(task);
            if (score <0){
                return false;
            }
            if(this.isSingle()){
                Log.printLine("Host #"+this.self.getId()+" accepts "+task.getTaskFullName());
                return true;
            }
            double pairScore = this.scores.get(this.pair);
            if (score > pairScore) {
                return true;
            }
            return false;
        }

        public void setTask(ColocationTask pair) {
            this.pair = pair;
        }

        public ColocationTask getTask() {
            return pair;
        }
        public void genScores(List<ColocationTask> tasks) {
            for(ColocationTask task : tasks) {
                double minScore = Double.MAX_VALUE;
                for (ServiceEntity se : this.self.getLcList()) {
                    double pressure = se.getPeerPressure();
                    double score = se.getSensitiveFun().LossByMem(pressure+task.getMemBWQuota(), 0);
                    if (score> sla) {
                        if (score <minScore) {
                            minScore = score;
                        }
                    } else {
                        minScore = -1;
                    }
                }
                this.scores.put(task, minScore);
            }
            //Log.printLine("gen tasks score end");
        }
    }
}
