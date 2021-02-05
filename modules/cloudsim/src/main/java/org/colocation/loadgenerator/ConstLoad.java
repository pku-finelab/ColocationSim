package org.colocation.loadgenerator;

import com.alibaba.fastjson.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wkj on 2019/9/1.
 */
public class ConstLoad extends LoadGenerator {
    List<ConstLoadForOneSvc> configs;
    public ConstLoad(double startAt, int duration, JSONArray loadConfigs, int copysNum) {
        super(startAt, duration);
        this.configs = new ArrayList<>();
        for (int copyID = 0; copyID < copysNum; copyID ++) {
            for (int i = 0; i < loadConfigs.size(); i++) {
                ConstLoadForOneSvc oneSvc = loadConfigs.getJSONObject(i).toJavaObject(ConstLoadForOneSvc.class);
                oneSvc.addPrefix("Copy." + copyID + "_");
                configs.add(oneSvc);
            }
        }
    }

    @Override
    public void genLoad() {
        for (int i = 0; i < configs.size(); i++) {
            ConstLoadForOneSvc oneSvc = configs.get(i);
            for (int j = 0; j < this.duration; j++) {
                for (int k = 0; k < oneSvc.getQps(); k++) {
                    sendARequest(oneSvc.getService(), oneSvc.getService()+"_tick."+j+"_no."+k, this.startTime+j);
                }
            }
        }

    }
}
