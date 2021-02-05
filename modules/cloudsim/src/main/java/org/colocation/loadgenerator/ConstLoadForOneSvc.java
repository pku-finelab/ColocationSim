package org.colocation.loadgenerator;

/**
 * Created by wkj on 2019/9/2.
 */
public class ConstLoadForOneSvc{
    private String service;
    private int qps;

    public ConstLoadForOneSvc(String service, int qps) {
        this.service = service;
        this.qps = qps;
    }

    public void setService(String service) {
        this.service = service;
    }

    public void setQps(int qps) {
        this.qps = qps;
    }

    public String getService() {
        return service;
    }

    public int getQps() {
        return qps;
    }

    public void addPrefix(String prefix) {
        this.service = prefix+service;
    }
}
