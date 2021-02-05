package org.colocation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.colocation.sensitiveFunction.AbstractSensitiveFunction;
import org.colocation.sensitiveFunction.LinearSensitiveFunction;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by wkj on 2019/3/22.
 */
public class ServiceEntityTest {

    @Before
    public void setUp(){
        int num_user = 1;
        Calendar calendar = Calendar.getInstance();
        CloudSim.init(num_user, calendar, false);

    }
    @Test
    public void testServiceEntityBW(){
        int mips = 1000;
        long size = 10000; // image size (MB)
        int ram = 2000; // vm memory (MB)
        long bw = 900;
        int pesNumber = 1; // number of cpus
        String vmm = "Xen"; // VMM name
        double memBandwidth = 1333.0;
        ColocationHost vm = new ColocationHost(0, 0, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared(), 0.5, 0.9, memBandwidth, Constants.CPUSHARE);
        Program program = new Program();
        ArrayList<String> pragram2Dep = new ArrayList<>();
        program.add(new Procedure(1000, pragram2Dep, false, 0));
        AbstractSensitiveFunction fun1 = new LinearSensitiveFunction();
        ServiceEntity se = new ServiceEntity("service1", "web",100, 2.0, "DC", 1, 800, program, fun1, 100);
        vm.setCurrentBW(500);
        se.setVM(vm);
        vm.assignServiceEntity(se);
        double frt = se.getRTByBw(500+277.679);
        System.out.println(frt);
        double interval = se.convertTimeToBW(2, 2.5);
        System.out.println(interval);
    }
}
