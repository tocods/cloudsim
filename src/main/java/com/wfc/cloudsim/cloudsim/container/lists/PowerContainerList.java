package com.wfc.cloudsim.cloudsim.container.lists;

import com.wfc.cloudsim.cloudsim.container.core.Container;
import com.wfc.cloudsim.cloudsim.core.CloudSim;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by sareh on 31/07/15.
 */
public class PowerContainerList {


    /**
     * Sort by cpu utilization.
     *
     * @param containerList the vm list
     */
    public static <T extends Container> void sortByCpuUtilization(List<T> containerList) {
        Collections.sort(containerList, new Comparator<T>() {

            @Override
            public int compare(T a, T b) throws ClassCastException {
                Double aUtilization = a.getTotalUtilizationOfCpuMips(CloudSim.clock());
                Double bUtilization = b.getTotalUtilizationOfCpuMips(CloudSim.clock());
                return bUtilization.compareTo(aUtilization);
            }
        });
    }

}
