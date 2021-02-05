package org.colocation.engine;

import org.colocation.ColocationContainer;
import org.colocation.ColocationHost;
import org.colocation.Constants;

/**
 * Created by wkj on 2019/5/27.
 */
public class CPUShareExecEngine implements ExecEngine {
    double totalMIPS;
    ColocationHost vm;
    public CPUShareExecEngine(double totalMIPS, ColocationHost vm) {
        this.totalMIPS = totalMIPS;
        this.vm = vm;
    }
    @Override
    public double getExecTime(long insNum, int containerID) {
        // return insNum/(MIPS*cpushare*sensiScore)

        double actualMips = this.getActualMips(containerID);
        return insNum/(actualMips* Constants.MILLION);
    }

    private double getCpuShare(int containerID) {
        int total = 0;
        int weight = 0;
        for (ColocationContainer c: vm.getContainerList()) {
            total += c.getCpuShare();
            if (c.getContainerId() == containerID) {
                weight = c.getCpuShare();
            }
        }
        double cpuShare = (double)weight/total;
        return cpuShare;
    }

    @Override
    public double getActualMips(int containerId) {
        double perf = -1;
        for (ColocationContainer c: vm.getContainerList()) {
            if (c.getId() == containerId) {
                perf = c.getPerfScore();
                break;
            }
        }
        if (perf < 0 ){
            return -1;
        }
        double hostMips = totalMIPS;
        //double cpushare = getCpuShare(containerId);
        double cpushare = 1.0;
        double res =  hostMips * cpushare * perf;
        return res;
    }
}
