package org.colocation.trace;

/**
 * Created by wkj on 2019/7/17.
 */
public class DataIDGenerator {
    static long nextDataID = 0;
    static long nextNetComID = 0;
    public static long getNextDataID(){
        nextDataID = nextDataID+1;
        return  nextDataID;
    }
    public static long getNextNetComID(){
        nextNetComID = nextNetComID +1;
        return  nextNetComID;
    }
}
