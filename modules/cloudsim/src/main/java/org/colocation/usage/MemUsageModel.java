package org.colocation.usage;

/**
 * Created by wkj on 2019/6/5.
 */
public interface MemUsageModel {
    long getMemUsageByRequestNum(long proccessedReqNum, int currReqNum);
}
