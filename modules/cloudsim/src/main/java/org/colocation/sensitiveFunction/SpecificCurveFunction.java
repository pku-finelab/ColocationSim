package org.colocation.sensitiveFunction;

import com.alibaba.fastjson.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class SpecificCurveFunction extends AbstractSensitiveFunction {
    private ArrayList<KVPoint> curve;

    public SpecificCurveFunction(JSONArray array) {
        this.curve = new ArrayList();
        for (int i = 0; i < array.size(); i++) {
            JSONArray datapoint = array.getJSONArray(i);
            double key = datapoint.getDouble(0);
            double value = datapoint.getDouble(1);
            KVPoint point = new KVPoint(key, value);
            this.curve.add(point);
        }
        Collections.sort(this.curve);
    }

    private double getPerf(double key){
        KVPoint left = null;
        KVPoint right = null;
        double res = 1;
        for (int i = 0; i < this.curve.size(); i++) {
            KVPoint curr = this.curve.get(i);
            if (curr.x > key) {
                right = curr;
                break;
            }
            if (curr.x <= key) {
                left = curr;
                right = null;
            }
        }
        if (right == null && left!= null) {
            if (left.x >100)
                return 0.1;
            return left.y;
        }
        if (left == null && right!= null) {
            return right.y;
        }
        if ( (left != null) && (right!= null)) {
            return left.y + ( (right.y - left.y)/(right.x- left.x) ) *(key - left.x);
        }
        return -1;
    }

    @Override
    public double LossByMem(double peerBW, double totalBW) {
        // find left and right
        return getPerf(peerBW);
    }

    @Override
    public double getCurrRealPerf(double peerBW) {
        double precision = 0.025;
        //precision = 0;
        Random random = new Random();
        double e = random.nextDouble()*(precision*2)-precision;
        return getPerf(peerBW)+e;
    }


    @Override
    public double rtToCapMemBW(double start, double delta) {
        return 0;
    }

    class KVPoint implements Comparable<KVPoint>{
        double x;
        double y;

        public KVPoint(double key, double value) {
            this.x = key;
            this.y = value;
        }

        @Override
        public int compareTo(KVPoint o) {
            double res = this.x - o.x;
            if (res == 0) {
                return 0;
            } else if (res >0) {
                return 1;
            } else {
                return -1;
            }
    }
    }
}
