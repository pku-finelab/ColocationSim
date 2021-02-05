package org.colocation.loadgenerator;

import org.colocation.bestEffort.ColocationJob;
import org.colocation.bestEffort.JobBuilder;

import java.util.List;

public class SimpleBeLoadGenerator extends BeLoadGenerator {
    private int repeatNum;
    private int interval;
    private int jobNumOnce;
    private JobBuilder builder;

    public SimpleBeLoadGenerator(double startTime, int brokerID, int repeatNum, int interval, int jobNumOnce, JobBuilder builder) {
        super(startTime, brokerID);
        this.interval  = interval;
        this.repeatNum = repeatNum;
        this.builder = builder;
        this.jobNumOnce = jobNumOnce;
    }

    @Override
    public void genLoad() {
        int jobPatchCount = 0;
        for (int i = 0; i < this.repeatNum; i++) {
            for (int j = 0; j < this.jobNumOnce; j++){
                List<ColocationJob> jobList = builder.getJobs(jobPatchCount);
                jobPatchCount = jobPatchCount + 1;
                for (int k = 0; k < jobList.size(); k++) {
                    ColocationJob job = jobList.get(k);
                    this.sendJob(job, startTime + i*interval );
                }
            }

        }
    }
}
