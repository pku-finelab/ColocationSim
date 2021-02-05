package org.colocation.bestEffort;

import java.util.Comparator;

/**
 * Created by wkj on 2019/3/20.
 */
public class TaskResourceComparator implements Comparator {
    public TaskResourceComparator(){
        super();
    }
    @Override
    public int compare(Object t1, Object t2) {
        ColocationTask task1 = (ColocationTask) t1;
        ColocationTask task2 = (ColocationTask) t2;
        double res = task1.getMemBWQuota()-task2.getMemBWQuota();
        return (int) res;
    }

}
