package org.colocation.exceptions;

/**
 * Created by wkj on 2019/9/1.
 */
public class ServiceNotStartedException extends Exception {
    public ServiceNotStartedException(String msg){
        super(msg);
    }
}
