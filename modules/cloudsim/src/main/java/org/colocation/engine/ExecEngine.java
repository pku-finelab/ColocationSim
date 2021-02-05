package org.colocation.engine;

/**
 * Created by wkj on 2019/5/27.
 */
public interface ExecEngine {
    double getExecTime(long InsNum, int ContianerID);
    double getActualMips(int containerId);
}
