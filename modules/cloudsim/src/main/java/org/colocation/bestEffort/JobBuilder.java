package org.colocation.bestEffort;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.colocation.Constants;
import org.colocation.Procedure;
import org.colocation.Program;
import org.colocation.sensitiveFunction.AbstractSensitiveFunction;
import org.colocation.sensitiveFunction.LinearSensitiveFunction;
import org.colocation.sensitiveFunction.NoneSensitive;
import org.colocation.sensitiveFunction.SensitiveFunction1;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wkj on 2019/6/20.
 */
public class JobBuilder {
    private String jobDescripterPath;
    private String datacenterName;
    private int userID;

    int taskCount = 0;
    int jobCount = 0;

    public JobBuilder(String datacenterName, int userID, String jobDescripterPath) {
        this.datacenterName = datacenterName;
        this.userID = userID;
        this.jobDescripterPath = jobDescripterPath;
    }
    public List<ColocationJob> getJobs(){
        return this.getJobs(0);
    }
    public List<ColocationJob> getJobs(int postfix){
        ArrayList<ColocationJob> jobs = new ArrayList<>();
        String content = "";
        try
        {
            content = new String ( Files.readAllBytes(Paths.get(jobDescripterPath)) );
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        JSONObject jsonObject = JSONObject.parseObject(content);
        JSONArray jobsJson = jsonObject.getJSONArray("jobs");
        for (int i = 0; i < jobsJson.size(); i++) {
            JSONObject jobJson = jobsJson.getJSONObject(i);
            String jobName = jobJson.getString("jobName") +"_" + postfix;
            JSONArray taskList = jobJson.getJSONArray("tasks");
            ColocationJob job = new ColocationJob(jobName, jobCount, 0.0, 0,0.0, this.datacenterName, this.userID);
            jobCount = jobCount + 1;
            // add tasks
            for (int j = 0; j < taskList.size(); j++) {
                JSONObject taskJson = taskList.getJSONObject(j);
                String taskName = taskJson.getString("taskName");
                double cpuQuota = taskJson.getDoubleValue("cpuQuota");
                long ramQuota = taskJson.getIntValue("ramQuotaMB") * Constants.MB;
                int cpuShare = taskJson.getIntValue("cpuShare");
                long instrNum = taskJson.getLongValue("instructionNum");
                double memBW = 10.0;
                if (taskJson.containsKey("memBW")) {
                    memBW = taskJson.getDoubleValue("memBW");
                }
                String fun = taskJson.getString("sensitiveFun");
                AbstractSensitiveFunction sf ;
                switch (fun) {
                    case "linear":
                        sf = new LinearSensitiveFunction();
                        break;
                    case "fun1":
                        sf = new SensitiveFunction1();
                        break;
                    case "best":
                        sf = new NoneSensitive();
                        break;
                    default:
                        sf = new NoneSensitive();
                }
                Program program = new Program(new Procedure(instrNum));

                ColocationTask task = new ColocationTask(jobName, taskName, taskCount, cpuQuota, ramQuota, memBW, this.datacenterName, this.userID, cpuShare, program, sf);
                job.addTask(task);
                taskCount += 1;
            }

            // add links
            for (int j = 0; j < taskList.size(); j++) {
                JSONObject taskJson = taskList.getJSONObject(j);
                String taskName = taskJson.getString("taskName");
                ColocationTask task = job.getTask(taskName);
                JSONArray parents = taskJson.getJSONArray("parents");
                if (parents.size()>0){
                    for (int k = 0; k < parents.size(); k++) {
                        String parentTaskName = parents.getString(k);
                        ColocationTask parentTask = job.getTask(parentTaskName);
                        task.addParent(parentTask);
                        parentTask.addChild(task);
                    }
                } else {
                    job.addRootTask(task);
                }
            }
            jobs.add(job);
        }
        return jobs;
    }

}
