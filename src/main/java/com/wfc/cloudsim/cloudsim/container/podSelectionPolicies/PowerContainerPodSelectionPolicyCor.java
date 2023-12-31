package com.wfc.cloudsim.cloudsim.container.podSelectionPolicies;


import com.wfc.cloudsim.cloudsim.Log;
import com.wfc.cloudsim.cloudsim.container.core.*;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;
import com.wfc.cloudsim.cloudsim.container.core.PowerContainerPod;
import com.wfc.cloudsim.cloudsim.container.utils.Correlation;

import java.util.List;

/**
 * Created by sareh on 16/11/15.
 */
public class PowerContainerPodSelectionPolicyCor extends PowerContainerPodSelectionPolicy {


    /**
     * The fallback policy.
     */
    private PowerContainerPodSelectionPolicy fallbackPolicy;

    /**
     * Instantiates a new power vm selection policy maximum correlation.
     *
     * @param fallbackPolicy the fallback policy
     */
    public PowerContainerPodSelectionPolicyCor(final PowerContainerPodSelectionPolicy fallbackPolicy) {
        super();
        setFallbackPolicy(fallbackPolicy);
    }

    /*
    * (non-Javadoc)
    *
    * @see com.wfc.cloudsim.cloudsim.experiments.power.PowerPodSelectionPolicy#
    * getVmsToMigrate(com.wfc.cloudsim .cloudsim.power.PowerHost)
    */
    @Override
    public ContainerPod getVmToMigrate(final PowerContainerHost host) {
        List<PowerContainerPod> migratableVMs = getMigratableVms(host);
        if (migratableVMs.isEmpty()) {
            return null;
        }
        ContainerPod vm = getContainerVM(migratableVMs, host);
        migratableVMs.clear();
        if (vm != null) {
//            Log.printConcatLine("We have to migrate the container with ID", container.getId());
            return vm;
        } else {
            return getFallbackPolicy().getVmToMigrate(host);
        }
    }

    /**
     * Gets the fallback policy.
     *
     * @return the fallback policy
     */
    public PowerContainerPodSelectionPolicy getFallbackPolicy() {
        return fallbackPolicy;
    }


    /**
     * Sets the fallback policy.
     *
     * @param fallbackPolicy the new fallback policy
     */
    public void setFallbackPolicy(final PowerContainerPodSelectionPolicy fallbackPolicy) {
        this.fallbackPolicy = fallbackPolicy;
    }

    public ContainerPod getContainerVM(List<PowerContainerPod> migratableContainerVMs, PowerContainerHost host) {

        double[] corResult = new double[migratableContainerVMs.size()];
        Correlation correlation = new Correlation();
        int i = 0;
        double maxValue = -2;
        int id = -1;
        if (host instanceof PowerContainerHostUtilizationHistory) {

            double[] hostUtilization = ((PowerContainerHostUtilizationHistory) host).getUtilizationHistory();
            for (ContainerPod vm : migratableContainerVMs) {
                double[] containerUtilization = ((PowerContainerPod) vm).getUtilizationHistoryList();

                double cor = correlation.getCor(hostUtilization, containerUtilization);
                if (Double.isNaN(cor)) {
                    cor = -3;
                }
                corResult[i] = cor;
                
                if(corResult[i] > maxValue) {
                	maxValue = corResult[i];
                	id = i;
                }
                
                i++;
            }

        }

        if (id == -1) {
            Log.printConcatLine("Problem with correlation list.");
        }

        return migratableContainerVMs.get(id);

    }


}













