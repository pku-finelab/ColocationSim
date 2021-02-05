package org.colocation.scheduler;

import org.colocation.ColocationHost;

public class HostAndValue implements Comparable<HostAndValue>{
    private ColocationHost host;
    private double d;

    public HostAndValue(ColocationHost host, double d) {
        this.host = host;
        this.d = d;
    }

    public ColocationHost getHost(){
        return host;
    }

    public double getD() {
        return d;
    }
    @Override
    public int compareTo(HostAndValue b){
        double diff = this.d-b.d;
        if ( diff > 0){
            return 1;
        } else if (diff < 0){
            return -1;
        }
        return 0;
    }
}
