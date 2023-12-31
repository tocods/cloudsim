package com.wfc.cloudsim.cloudsim.container.resourceAllocatorMigrationEnabled;

import com.wfc.cloudsim.cloudsim.container.containerSelectionPolicies.PowerContainerSelectionPolicy;
import com.wfc.cloudsim.cloudsim.container.core.*;
import com.wfc.cloudsim.cloudsim.container.hostSelectionPolicies.HostSelectionPolicy;
import com.wfc.cloudsim.cloudsim.container.podSelectionPolicies.PowerContainerPodSelectionPolicy;
import com.wfc.cloudsim.wfc.core.WFCDatacenter;

import java.util.List;

/**
 * Created by sareh on 3/08/15.
 */
public class PowerContainerPodAllocationPolicyMigrationStaticThresholdMC extends PowerContainerPodAllocationPolicyMigrationAbstractContainerHostSelection {
//public class PowerContainerPodAllocationPolicyMigrationStaticThresholdMC extends PowerContainerPodAllocationPolicyMigrationAbstractContainerHostSelectionUnderUtilizedAdded {


    /**
     * The utilization threshold.
     */
    private double utilizationThreshold = 0.9;

    /**
     * Instantiates a new power vm allocation policy migration mad.
     *
     * @param hostList             the host list
     * @param vmSelectionPolicy    the vm selection policy
     * @param utilizationThreshold the utilization threshold
     */
    public PowerContainerPodAllocationPolicyMigrationStaticThresholdMC(
            List<? extends ContainerHost> hostList,
            PowerContainerPodSelectionPolicy vmSelectionPolicy, PowerContainerSelectionPolicy containerSelectionPolicy,
            HostSelectionPolicy hostSelectionPolicy, double utilizationThreshold,
            int numberOfVmTypes, int[] vmPes, float[] vmRam, long vmBw, long vmSize, double[] vmMips) {
        super(hostList, vmSelectionPolicy, containerSelectionPolicy, hostSelectionPolicy,
        		 numberOfVmTypes, vmPes, vmRam, vmBw, vmSize, vmMips);
        setUtilizationThreshold(utilizationThreshold);
    }

    /**
     * Checks if is host over utilized.
     *
     * @param host the _host
     * @return true, if is host over utilized
     */
    @Override
    protected boolean isHostOverUtilized(PowerContainerHost host) {
        addHistoryEntry(host, getUtilizationThreshold());
        double totalRequestedMips = 0;
        for (ContainerPod vm : host.getVmList()) {
            totalRequestedMips += vm.getCurrentRequestedTotalMips();
        }
        double utilization = totalRequestedMips / host.getTotalMips();
        return utilization > getUtilizationThreshold();
    }

    @Override
    protected boolean isHostUnderUtilized(PowerContainerHost host) {
        return false;
    }

    /**
     * Sets the utilization threshold.
     *
     * @param utilizationThreshold the new utilization threshold
     */
    protected void setUtilizationThreshold(double utilizationThreshold) {
        this.utilizationThreshold = utilizationThreshold;
    }

    /**
     * Gets the utilization threshold.
     *
     * @return the utilization threshold
     */
    protected double getUtilizationThreshold() {
        return utilizationThreshold;
    }
    @Override
    public void setDatacenter(WFCDatacenter datacenter) {
        super.setDatacenter(datacenter);
    }

}



