package org.colocation.qos;

import org.cloudbus.cloudsim.core.CloudSim;
import org.colocation.ServiceEntity;

/**
 * Created by wkj on 2019/6/13.
 */
public class EntrypointAPI {
    private String entrypoint;
    private String  APIName;

    public EntrypointAPI( String APIName, String entrypoint) {
        this.APIName = APIName;
        this.entrypoint = entrypoint;
    }

    public String getEntrypoint() {
        return entrypoint;
    }

    public String getAPIName() {
        return APIName;
    }
}
