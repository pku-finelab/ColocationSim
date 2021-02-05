package org.colocation.usage;

/**
 * Created by wkj on 2019/6/5.
 */
public interface CpuUsageModel {
    /**
     * Gets the utilization percentage of a given resource.
     *
     * @param requestNum the number that proccessing requests in a service .
     * @return utilization percentage, from [0 to 1]
     */
    double getCpuUsageByRequestNum(int requestNum);
}
