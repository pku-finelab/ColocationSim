package org.colocation;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wkj on 2019/3/8.
 */
public class Procedure {

    final long instructionNum;
    boolean hasDep;
    List<String> depServices;
    boolean async = false;
    int waitNum = 1;
    boolean finished = false;

    public Procedure(long insNum) {
        this(insNum, null, false, 0);
    }

    public Procedure(long insNumM, List<String> depServices, boolean async, int waitNum){
        setAsync(async);
        setDepServices(depServices);
        setFinished(false);
        setWaitNum(waitNum);
        instructionNum = insNumM * Constants.MILLION;
        if (depServices != null && depServices.size()>0){
            this.hasDep = true;
        } else {
            hasDep = false;
        }
    }

    public Procedure(JSONObject json) {
        String type = json.getString("type");
        switch (type) {
            case "local":
                long insNum = json.getLongValue("insNum");
                this.instructionNum = insNum * Constants.MILLION;
                this.depServices = null;
                setAsync(false);
                setWaitNum(0);
                break;
            case "remote":
                this.instructionNum = 0;
                this.depServices = new ArrayList<>();
                JSONArray arr =  json.getJSONArray("deps");
                for (int i = 0; i < arr.size(); i++) {
                    this.depServices.add(arr.getString(i));
                }
                setAsync(false);
                this.hasDep = true;
                setWaitNum(arr.size());
                break;
            default:
                this.instructionNum = 0;
        }
    }

    public boolean isHasDep() {
        return hasDep;
    }

    public long getInstructionNum() {
        return instructionNum;
    }

    public List<String> getDepServices() {
        return depServices;
    }

    public void setDepServices(List<String> depServices) {
        this.depServices = depServices;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public int getWaitNum() {
        return waitNum;
    }

    public void setWaitNum(int waitNum) {
        this.waitNum = waitNum;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public void setServicePrefix(String prefix) {
        if (hasDep) {
            for (int i = 0; i < this.depServices.size(); i++) {
                String newDepSvcName = prefix + this.depServices.get(i);
                this.depServices.set(i, newDepSvcName);
            }
        }
    }
}
