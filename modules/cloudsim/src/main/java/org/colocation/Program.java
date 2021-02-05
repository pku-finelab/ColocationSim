package org.colocation;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wkj on 2019/5/30.
 */
public class Program {
    private List<Procedure> program;
    public Program(){
        this.program = new ArrayList<>();
    }
    public Program(Procedure p) {
        this();
        this.program.add(p);
    }

    public Program(JSONArray procedureJson){
        this();
        for (int i = 0; i < procedureJson.size(); i++) {
            Procedure p = new Procedure(procedureJson.getJSONObject(i));
            this.program.add(p);
        }
    }

    public void add(Procedure p) {
        this.program.add(p);
    }
    public Procedure get(int n) {
        if ( (n >= 0 ) && (n < this.program.size() )) {
            return this.program.get(n);
        }
        return null;
    }

    public List<Procedure> getAllProcedure() {
        return this.program;
    }

    public long getAllInstructionNum(){
        long total = 0L;
        for (Procedure p : this.program) {
            total = total + p.getInstructionNum();
        }
        return total;
    }
    public int size(){
        return this.program.size();
    }

    public void setServicePrefix(String prefix){
        for (int i = 0; i < this.program.size(); i++) {
            Procedure p = this.program.get(i);
            p.setServicePrefix(prefix);
        }
    }
}
