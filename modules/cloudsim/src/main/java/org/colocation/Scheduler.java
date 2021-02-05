package org.colocation;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.List;

/**
 * Created by wkj on 2019/3/9.
 */
public class Scheduler extends SimEntity {

    private String DCName;

    public Scheduler(String name, String datacenterName){
        super(name);
        this.DCName = datacenterName;
    }

    private void scheduleEntity(List<ServiceEntity> serviceEntities){
        Log.printLine(": received schedule event. service entity list size is "+serviceEntities.size());
        //put the service to some vm
        Datacenter dc = (Datacenter) CloudSim.getEntity(this.DCName);
        List<ColocationHost> vms = dc.getVmList();

        for (int j = 0; j < serviceEntities.size(); j++) {

            for (int i = 0; i < vms.size(); i++) {
                ColocationHost vmi = vms.get(i);
                ServiceEntity se = serviceEntities.get(j);
                int seWant = se.getMemQuota();
                long avaliRam = vmi.getLcAvailableRam();
                int lcInsNum = vmi.getLCNumber();
                if (lcInsNum < 1){
                    //set se to vmi
                    se.setVMId(vmi.getId());
                    int res = vmi.assignServiceEntity(se);
                    if (res > 0) {
                        Log.printLine("schedule success:"+se.getName() +" on VM: "+vmi.getId());
                        break;
                    } else {
                        Log.printLine("schedule failed:"+se.getName());
                    }
                } else {
                    //Log.printLine("lc available not enough: schedule failed:"+se.getName());
                }
            }

        }
    }


    /**
     * Processes events or services that are available for the entity.
     * This method is invoked by the {@link CloudSim} class whenever there is an event in the
     * deferred queue, which needs to be processed by the entity.
     *
     * @param ev information about the event just happened
     * @pre ev != null
     * @post $none
     */
    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()){
            case Constants.SCHEDULE_EVENT:
                scheduleEntity((List<ServiceEntity>)ev.getData());
        }
    }
    /**
     * This method is invoked by the {@link CloudSim} class when the simulation is started.
     * It should be responsible for starting the entity up.
     */
    @Override
    public void startEntity() {

    }


    /**
     * Shuts down the entity.
     * This method is invoked by the {@link CloudSim} before the simulation finishes. If you want
     * to save data in log files this is the method in which the corresponding code would be placed.
     */
    @Override
    public void shutdownEntity() {

    }
}
