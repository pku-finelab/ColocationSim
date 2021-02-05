package org.colocation.lcjob;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.colocation.Program;
import org.colocation.ServiceEntity;
import org.colocation.bestEffort.ColocationJob;
import org.colocation.loadgenerator.BoostLoadGenerator;
import org.colocation.loadgenerator.ConstLoad;
import org.colocation.loadgenerator.LoadGenerator;
import org.colocation.pressureFunction.IMemPressureFunction;
import org.colocation.pressureFunction.LinearPressureFunction;
import org.colocation.qos.EntrypointAPI;
import org.colocation.qos.QoSConfiguration;
import org.colocation.sensitiveFunction.AbstractSensitiveFunction;
import org.colocation.sensitiveFunction.LinearSensitiveFunction;
import org.colocation.sensitiveFunction.SensitiveFunction1;
import org.colocation.sensitiveFunction.SpecificCurveFunction;
import org.colocation.usage.LinearCpuUsage;
import org.colocation.usage.LinearMemUsage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by wkj on 2019/8/30.
 */
public class LcJobBuilder {

    private String jobDescripterPath;
    private String datacenterName;
    private int userID;
    JSONObject jsonObject;
    private HashMap<String, AbstractSensitiveFunction> functionMap;
    private ArrayList<QoSConfiguration> qoSConfigurations;

    public LcJobBuilder(String datacenterName, int userID, String jobDescripterPath) {
        this.jobDescripterPath = jobDescripterPath;
        this.datacenterName = datacenterName;
        this.userID = userID;
        this.functionMap = new HashMap<>();
        this.qoSConfigurations = new ArrayList<>();
        String content = "";
        try
        {
            content = new String ( Files.readAllBytes(Paths.get(jobDescripterPath)) );
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        this.jsonObject = JSONObject.parseObject(content);

        this.registerSensitiveFunctions();
    }

    public List<ServiceEntity> getServices(){
        return getServices(1);
    }

    public List<ServiceEntity> getServices(int copyNum){
        HashMap<String, double[]> qosMap = new HashMap<>();

        //build meta qos info
        JSONArray slaList = this.jsonObject.getJSONArray("sla");
        double pd = this.jsonObject.getDouble("pd");
        for (int i = 0; i < slaList.size(); i++) {
            JSONObject config = slaList.getJSONObject(i);
            String serviceName = config.getString("service");
            double targetRT = config.getDoubleValue("targetRT");
            double rtRaise = config.getDoubleValue("MaxRTRaise");
            double maxRT = targetRT / pd;
            double[] rtAndMaxRT = {targetRT, maxRT};
            qosMap.put(serviceName, rtAndMaxRT);
        }

        ArrayList<ServiceEntity> res = new ArrayList<>();
        for (int copyID = 0; copyID < copyNum; copyID++) {
            String prefix = "Copy."+copyID+"_";
            String appName = this.jsonObject.getString(prefix+"app");
            JSONArray servicesJSON = this.jsonObject.getJSONArray("services");
            for (int i = 0; i < servicesJSON.size(); i++) {
                JSONObject serviceConfig = servicesJSON.getJSONObject(i);
                String serviceName = serviceConfig.getString("name");
                int priority = serviceConfig.getIntValue("priority");
                float cpuQuota = serviceConfig.getFloatValue("cpuQuota");
                int memQuota = serviceConfig.getIntValue("memQuota");
                int cpuShare = serviceConfig.getIntValue("cpuShare");
                Program program = new Program(serviceConfig.getJSONArray("program"));
                String senType = serviceConfig.getString("sensitiveFun");
                AbstractSensitiveFunction senFun = this.functionMap.get(senType);

                LinearCpuUsage cpuUsageModel = new LinearCpuUsage(serviceConfig.getJSONObject("cpuModel"));
                LinearMemUsage memUsageModel = new LinearMemUsage(serviceConfig.getJSONObject("memModel"));
                IMemPressureFunction bwModel = new LinearPressureFunction(serviceConfig.getJSONObject("memBWModel"));

                String serviceFullName = prefix+serviceName;
                ServiceEntity serviceEntity = new ServiceEntity(serviceFullName, appName, priority, cpuQuota, datacenterName, userID, memQuota, program, senFun, cpuShare, cpuUsageModel, memUsageModel);
                serviceEntity.setMemPressureFun(bwModel);

                double[] rtAndMaxRT = qosMap.get(serviceName);
                double targetRT = 0;
                double maxRT = 0;
                if (rtAndMaxRT != null) {
                    targetRT = rtAndMaxRT[0];
                    maxRT = rtAndMaxRT[1];
                    serviceEntity.setMaxRT(maxRT);
                }
                serviceEntity.updateServiceNameInProcedure(prefix);
                res.add(serviceEntity);

                //update qos center
                EntrypointAPI entrypoint = new EntrypointAPI(serviceFullName,serviceFullName);
                QoSConfiguration c = new QoSConfiguration(entrypoint, targetRT, maxRT);
                this.qoSConfigurations.add(c);
            }
        }


        return  res;
    }

    public List<QoSConfiguration> getQosConfigurations(){
        return this.qoSConfigurations;
    }

    public LoadGenerator getLoadGenerator(){
        return getLoadGenerator(1);
    }

    public LoadGenerator getLoadGenerator(int copysNum){
        JSONObject load = jsonObject.getJSONObject("load");
        String loadType = load.getString("type");
        int duration = load.getIntValue("duration");
        double startAt =  load.getDoubleValue("startAt");
        LoadGenerator loader;
        switch (loadType){
            case "const":
                loader = new ConstLoad(startAt, duration, load.getJSONArray("entrypoints"), copysNum);
                break;
            case "boost":
                loader = new BoostLoadGenerator(load.getJSONArray("entrypoints"), copysNum);
                break;
            default:
                loader = new ConstLoad(startAt, duration, load.getJSONArray("entrypoints"), copysNum);
        }
        return loader;
    }

    private void registerSensitiveFunctions(){
        AbstractSensitiveFunction linear = new LinearSensitiveFunction();
        AbstractSensitiveFunction logistic = new SensitiveFunction1();
        this.functionMap.put("linear", linear);
        this.functionMap.put("logistic", logistic);
        JSONObject functions = this.jsonObject.getJSONObject("sensitiveFunctions");
        Set keys = functions.keySet();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()){
            String key = it.next();
            JSONArray data = functions.getJSONArray(key);
            AbstractSensitiveFunction func = new SpecificCurveFunction(data);
            this.functionMap.put(key, func);
        }
    }
}
