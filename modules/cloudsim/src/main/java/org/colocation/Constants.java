package org.colocation;

/**
 * Created by wkj on 2019/3/7.
 */
public final class Constants {
    // events
    public static final int BASE = 10000;
    public static final int UPDATE_PERFORMANCE = BASE + 1;
    public static final int HANDEL_REQUEST = BASE + 2;
    public static final int REQUEST_RETURN = BASE + 3;
    public static final int SCHEDULE_EVENT = BASE + 4;
    public static final int START_WORKLOAD = BASE + 5;
    public static final int OPTTYPE_CAL    = BASE + 6;
    public static final int SCHEDULE_BE_JOB= BASE + 7;
    public static final int COLOCATION_BE_RETURN = BASE + 8;
    public static final int VM_UPDATE = BASE + 9;
    public static final int REQUEST_LOGS = BASE + 10;
    public static final int CONTAINER_START = BASE + 11;
    public static final int CONTAINER_SHUTDOWN = BASE + 12;
    public static final int PROGRAM_RUNNING = BASE + 13;
    public static final int PROGRAM_FINISHED = BASE + 14;
    public static final int MONITER = BASE+15;
    public static final int RESTART_CONTAINER = BASE +16;
    public static final int EVICTION = BASE + 17;
    public static final int RUN_SCHEDULE = BASE + 18;
    public static final int COLOCATION_JOB_RETURN = BASE+ 19;
    public static final int MONITOR_EVENT = BASE + 20;
    public static final int REPORT_RT_TO_BROKER = BASE + 21;

    // function type
    public static final int FTYPE_BASE = 20000;
    public static final int FTYPE_SEND = FTYPE_BASE + 1;
    public static final int FTYPE_RECV = FTYPE_BASE + 2;
    public static final int FTYPE_EXEC = FTYPE_BASE + 3;

    public static final String  FCFS = "FCFS";
    public static final String  MINMIN = "MINMIN";
    public static final String  MAXMIN = "MAXMIN";
    public static final String  DIAS = "DIAS";
    public static final String  ROUNDROBIN = "ROUNDROBIN";
    public static final String  PARAGON = "PARAGON";
    public static final String  LL = "LeastLoad";
    public static final String  ParagonNI = "ParagonNI";
    public static final String  BUBBLEUP = "BubbleUp";
    public static final String COOPER = "Cooper";

    public static final String CPUSHARE = "cpushare";

    public static final String METRIC_CPU = "cpu";
    public static final String METRIC_MEM = "mem";

    public static final long KB = 1024;
    public static final long MB = 1024*KB;
    public static final long GB = 1024*MB;

    public static final long MILLION = 1000000;

    public static final int MINUTE = 60;
    public static final int HOUR = 60 * MINUTE;
    public static final int DAY = 24*HOUR;

    public static final String EVENT_TYPE_ADD_CON = "ADD_CON";

}
