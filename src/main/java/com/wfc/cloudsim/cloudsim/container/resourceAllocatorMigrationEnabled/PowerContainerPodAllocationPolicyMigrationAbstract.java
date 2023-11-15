package com.wfc.cloudsim.cloudsim.container.resourceAllocatorMigrationEnabled;

import com.wfc.cloudsim.cloudsim.Log;
import com.wfc.cloudsim.cloudsim.container.core.*;
import com.wfc.cloudsim.cloudsim.container.lists.PowerContainerPodList;
import com.wfc.cloudsim.cloudsim.container.podSelectionPolicies.PowerContainerPodSelectionPolicy;
import com.wfc.cloudsim.cloudsim.container.resourceAllocators.PowerContainerPodAllocationAbstract;
import com.wfc.cloudsim.cloudsim.core.CloudSim;
import com.wfc.cloudsim.cloudsim.util.ExecutionTimeMeasurer;

import java.util.*;

/**
 * Created by sareh on 28/07/15.
 */
public abstract class PowerContainerPodAllocationPolicyMigrationAbstract extends PowerContainerPodAllocationAbstract {

    /**
     * The vm selection policy.
     */
    private PowerContainerPodSelectionPolicy vmSelectionPolicy;

    /**
     * The saved allocation.
     */
    private final List<Map<String, Object>> savedAllocation = new ArrayList<Map<String, Object>>();

    /**
     * The utilization history.
     */
    private final Map<Integer, List<Double>> utilizationHistory = new HashMap<Integer, List<Double>>();

    /**
     * The metric history.
     */
    private final Map<Integer, List<Double>> metricHistory = new HashMap<Integer, List<Double>>();

    /**
     * The time history.
     */
    private final Map<Integer, List<Double>> timeHistory = new HashMap<Integer, List<Double>>();

    /**
     * The execution time history vm selection.
     */
    private final List<Double> executionTimeHistoryVmSelection = new LinkedList<Double>();

    /**
     * The execution time history host selection.
     */
    private final List<Double> executionTimeHistoryHostSelection = new LinkedList<Double>();

    /**
     * The execution time history vm reallocation.
     */
    private final List<Double> executionTimeHistoryVmReallocation = new LinkedList<Double>();

    /**
     * The execution time history total.
     */
    private final List<Double> executionTimeHistoryTotal = new LinkedList<Double>();

    /**
     * Instantiates a new power vm allocation policy migration abstract.
     *
     * @param hostList          the host list
     * @param vmSelectionPolicy the vm selection policy
     */
    public PowerContainerPodAllocationPolicyMigrationAbstract(
            List<? extends ContainerHost> hostList,
            PowerContainerPodSelectionPolicy vmSelectionPolicy) {
        super(hostList);
        setVmSelectionPolicy(vmSelectionPolicy);

    }

    /**
     * Optimize allocation of the VMs according to current utilization.
     *
     * @param vmList the vm list
     * @return the array list< hash map< string, object>>
     */
    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends ContainerPod> vmList) {
        ExecutionTimeMeasurer.start("optimizeAllocationTotal");

        ExecutionTimeMeasurer.start("optimizeAllocationHostSelection");
        List<PowerContainerHostUtilizationHistory> overUtilizedHosts = getOverUtilizedHosts();
        getExecutionTimeHistoryHostSelection().add(
                ExecutionTimeMeasurer.end("optimizeAllocationHostSelection"));

        printOverUtilizedHosts(overUtilizedHosts);

        saveAllocation();
        Log.printLine("optimizeAllocationVmSelection");
        ExecutionTimeMeasurer.start("optimizeAllocationVmSelection");
        List<? extends ContainerPod> vmsToMigrate = getVmsToMigrateFromHosts(overUtilizedHosts);
        getExecutionTimeHistoryVmSelection().add(ExecutionTimeMeasurer.end("optimizeAllocationVmSelection"));

        Log.printLine("Reallocation of VMs from the over-utilized hosts:");
        ExecutionTimeMeasurer.start("optimizeAllocationVmReallocation");
        List<Map<String, Object>> migrationMap = getNewVmPlacement(vmsToMigrate, new HashSet<ContainerHost>(
                overUtilizedHosts));
        getExecutionTimeHistoryVmReallocation().add(
                ExecutionTimeMeasurer.end("optimizeAllocationVmReallocation"));
        Log.printLine();

        migrationMap.addAll(getMigrationMapFromUnderUtilizedHosts(overUtilizedHosts, migrationMap));

        restoreAllocation();

        getExecutionTimeHistoryTotal().add(ExecutionTimeMeasurer.end("optimizeAllocationTotal"));

        return migrationMap;
    }

    /**
     * Gets the migration map from under utilized hosts.
     *
     * @param overUtilizedHosts the over utilized hosts
     * @return the migration map from under utilized hosts
     */
    protected List<Map<String, Object>> getMigrationMapFromUnderUtilizedHosts(
            List<PowerContainerHostUtilizationHistory> overUtilizedHosts, List<Map<String, Object>> previouseMap) {
        List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
        List<PowerContainerHost> switchedOffHosts = getSwitchedOffHosts();

        // over-utilized hosts + hosts that are selected to migrate VMs to from over-utilized hosts
        Set<PowerContainerHost> excludedHostsForFindingUnderUtilizedHost = new HashSet<>();
        excludedHostsForFindingUnderUtilizedHost.addAll(overUtilizedHosts);
        excludedHostsForFindingUnderUtilizedHost.addAll(switchedOffHosts);
        excludedHostsForFindingUnderUtilizedHost.addAll(extractHostListFromMigrationMap(previouseMap));

        // over-utilized + under-utilized hosts
        Set<PowerContainerHost> excludedHostsForFindingNewVmPlacement = new HashSet<>();
        excludedHostsForFindingNewVmPlacement.addAll(overUtilizedHosts);
        excludedHostsForFindingNewVmPlacement.addAll(switchedOffHosts);

        int numberOfHosts = getContainerHostList().size();

        while (true) {
            if (numberOfHosts == excludedHostsForFindingUnderUtilizedHost.size()) {
                break;
            }

            PowerContainerHost underUtilizedHost = getUnderUtilizedHost(excludedHostsForFindingUnderUtilizedHost);
            if (underUtilizedHost == null) {
                break;
            }

            Log.printConcatLine("Under-utilized host: host #", underUtilizedHost.getId() + " " + underUtilizedHost.getUtilizationMips(), "\n");

            excludedHostsForFindingUnderUtilizedHost.add(underUtilizedHost);
            excludedHostsForFindingNewVmPlacement.add(underUtilizedHost);

            List<? extends ContainerPod> vmsToMigrateFromUnderUtilizedHost = getVmsToMigrateFromUnderUtilizedHost(underUtilizedHost);
            if (vmsToMigrateFromUnderUtilizedHost.isEmpty()) {
                continue;
            }

            Log.print("Reallocation of VMs from the under-utilized host: ");
            if (!Log.isDisabled()) {
                for (ContainerPod vm : vmsToMigrateFromUnderUtilizedHost) {
                    Log.print(vm.getId() + " " + vm.getTotalUtilizationOfCpuMips(CloudSim.clock()));
                }
            }
            Log.printLine();

            List<Map<String, Object>> newVmPlacement = getNewVmPlacementFromUnderUtilizedHost(
                    vmsToMigrateFromUnderUtilizedHost,
                    excludedHostsForFindingNewVmPlacement);

            excludedHostsForFindingUnderUtilizedHost.addAll(extractHostListFromMigrationMap(newVmPlacement));

            migrationMap.addAll(newVmPlacement);
            Log.printLine();
        }

        excludedHostsForFindingUnderUtilizedHost.clear();
        excludedHostsForFindingNewVmPlacement.clear();
        return migrationMap;
    }

    /**
     * Prints the over utilized hosts.
     *
     * @param overUtilizedHosts the over utilized hosts
     */
    protected void printOverUtilizedHosts(List<PowerContainerHostUtilizationHistory> overUtilizedHosts) {
        if (!Log.isDisabled()) {
            Log.printLine("Over-utilized hosts:");
            for (PowerContainerHostUtilizationHistory host : overUtilizedHosts) {
                Log.printConcatLine("Host #", host.getId());
            }
            Log.printLine();
        }
    }


    /**
     * Find host for vm.
     *
     * @param vm            the vm
     * @param excludedHosts the excluded hosts
     * @return the power host
     */
    public PowerContainerHost findHostForVm(ContainerPod vm, Set<? extends ContainerHost> excludedHosts) {
        Log.printLine("find Host for Pod");
        double minPower = Double.MAX_VALUE;
        PowerContainerHost allocatedHost = null;

        for (PowerContainerHost host : this.<PowerContainerHost>getContainerHostList()) {
            if (excludedHosts.contains(host)) {
                continue;
            }
            if (host.isSuitableForContainerVm(vm)) {
                if (getUtilizationOfCpuMips(host) != 0 && isHostOverUtilizedAfterAllocation(host, vm)) {
                    Log.printLine("no, can't chose");
                    continue;
                }

                try {
                    double powerAfterAllocation = getPowerAfterAllocation(host, vm);
                    if (powerAfterAllocation != -1) {
                        double powerDiff = powerAfterAllocation - host.getPower();
                        if (powerDiff < minPower) {
                            minPower = powerDiff;
                            allocatedHost = host;
                        }
                    }
                } catch (Exception e) {
                }
            }
            else {
                Log.printLine("host " + host.getId() + " is not suitable");
            }
        }
        return allocatedHost;
    }

    /**
     * Checks if is host over utilized after allocation.
     *
     * @param host the host
     * @param vm   the vm
     * @return true, if is host over utilized after allocation
     */
    protected boolean isHostOverUtilizedAfterAllocation(PowerContainerHost host, ContainerPod vm) {
        boolean isHostOverUtilizedAfterAllocation = true;
        if (host.containerVmCreate(vm)) {
            isHostOverUtilizedAfterAllocation = isHostOverUtilized(host);
            host.containerVmDestroy(vm);
        }
        return isHostOverUtilizedAfterAllocation;
    }

    /**
     * Find host for vm.
     *
     * @param vm the vm
     * @return the power host
     */
    @Override
    public PowerContainerHost findHostForVm(ContainerPod vm) {
        Set<ContainerHost> excludedHosts = new HashSet<>();
        if (vm.getHost() != null) {
            excludedHosts.add(vm.getHost());
        }
        PowerContainerHost hostForVm = findHostForVm(vm, excludedHosts);
        excludedHosts.clear();

        return hostForVm;
    }

    @Override
    public List<PowerContainerHost> findHostForPods(List<ContainerPod> pods) {
        return null;
    }

    /**
     * Extract host list from migration map.
     *
     * @param migrationMap the migration map
     * @return the list
     */
    protected List<PowerContainerHost> extractHostListFromMigrationMap(List<Map<String, Object>> migrationMap) {
        List<PowerContainerHost> hosts = new LinkedList<PowerContainerHost>();
        for (Map<String, Object> map : migrationMap) {
            hosts.add((PowerContainerHost) map.get("host"));
        }

        return hosts;
    }

    /**
     * Gets the new vm placement.
     *
     * @param vmsToMigrate  the vms to migrate
     * @param excludedHosts the excluded hosts
     * @return the new vm placement
     */
    protected List<Map<String, Object>> getNewVmPlacement(
            List<? extends ContainerPod> vmsToMigrate,
            Set<? extends ContainerHost> excludedHosts) {
        List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
        PowerContainerPodList.sortByCpuUtilization(vmsToMigrate);
        for (ContainerPod vm : vmsToMigrate) {
            PowerContainerHost allocatedHost = findHostForVm(vm, excludedHosts);
            if (allocatedHost != null) {
                allocatedHost.containerVmCreate(vm);
                Log.printConcatLine("VM #", vm.getId(), " allocated to host #", allocatedHost.getId());

                Map<String, Object> migrate = new HashMap<String, Object>();
                migrate.put("vm", vm);
                migrate.put("host", allocatedHost);
                migrationMap.add(migrate);
            }
        }
        return migrationMap;
    }

    /**
     * Gets the new vm placement from under utilized host.
     *
     * @param vmsToMigrate  the vms to migrate
     * @param excludedHosts the excluded hosts
     * @return the new vm placement from under utilized host
     */
    protected List<Map<String, Object>> getNewVmPlacementFromUnderUtilizedHost(
            List<? extends ContainerPod> vmsToMigrate,
            Set<? extends ContainerHost> excludedHosts) {
        List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
        PowerContainerPodList.sortByCpuUtilization(vmsToMigrate);
        for (ContainerPod vm : vmsToMigrate) {
            Log.printLine("PowerContainerPodAllocationPolicyMigration: find host for pod" + vm.getId() + " in " + excludedHosts.size() + " hosts");
            PowerContainerHost allocatedHost = findHostForVm(vm, excludedHosts);
            if (allocatedHost != null) {
                allocatedHost.containerVmCreate(vm);
                Log.printConcatLine("VM #", vm.getId(), " allocated to host #", allocatedHost.getId());

                Map<String, Object> migrate = new HashMap<String, Object>();
                migrate.put("vm", vm);
                migrate.put("host", allocatedHost);
                migrationMap.add(migrate);
            } else {
                Log.printLine("PowerContainerPodAllocationPolicyMigration: Not all VMs can be reallocated from the host, reallocation cancelled");
                for (Map<String, Object> map : migrationMap) {
                    ((ContainerHost) map.get("host")).containerVmDestroy((ContainerPod) map.get("vm"));
                }
                migrationMap.clear();
                break;
            }
        }
        return migrationMap;
    }

    /**
     * Gets the vms to migrate from hosts.
     *
     * @param overUtilizedHosts the over utilized hosts
     * @return the vms to migrate from hosts
     */
    protected List<? extends ContainerPod> getVmsToMigrateFromHosts(List<PowerContainerHostUtilizationHistory> overUtilizedHosts) {
        List<ContainerPod> vmsToMigrate = new LinkedList<ContainerPod>();
        for (PowerContainerHostUtilizationHistory host : overUtilizedHosts) {
            while (true) {
                ContainerPod vm = getVmSelectionPolicy().getVmToMigrate(host);

                if (vm == null) {
                    break;
                }
//                Log.printLine("Pod " + vm.getId() + " in Host " + vm.getHost().getId() + " is chosen to be migrated");
                vmsToMigrate.add(vm);
                host.containerVmDestroy(vm);
                if (!isHostOverUtilized(host)) {
                    break;
                }
            }
        }
        return vmsToMigrate;
    }


    /**
     * Gets the vms to migrate from under utilized host.
     *
     * @param host the host
     * @return the vms to migrate from under utilized host
     */
    protected List<? extends ContainerPod> getVmsToMigrateFromUnderUtilizedHost(PowerContainerHost host) {
        List<ContainerPod> vmsToMigrate = new LinkedList<ContainerPod>();
        for (ContainerPod vm : host.getVmList()) {
            if (!vm.isInMigration()) {
                vmsToMigrate.add(vm);
            }
        }
        return vmsToMigrate;
    }

    /**
     * Gets the over utilized hosts.
     *
     * @return the over utilized hosts
     */
    protected List<PowerContainerHostUtilizationHistory> getOverUtilizedHosts() {
        List<PowerContainerHostUtilizationHistory> overUtilizedHosts = new LinkedList<PowerContainerHostUtilizationHistory>();
        for (PowerContainerHostUtilizationHistory host : this.<PowerContainerHostUtilizationHistory>getContainerHostList()) {
            if (isHostOverUtilized(host)) {
                overUtilizedHosts.add(host);
            }
            }
        return overUtilizedHosts;
    }

    /**
     * Gets the switched off host.
     *
     * @return the switched off host
     */
    protected List<PowerContainerHost> getSwitchedOffHosts() {
        List<PowerContainerHost> switchedOffHosts = new LinkedList<PowerContainerHost>();
        for (PowerContainerHost host : this.<PowerContainerHost>getContainerHostList()) {
            if (host.getUtilizationOfCpu() == 0) {
                switchedOffHosts.add(host);
            }
        }
        return switchedOffHosts;
    }

    /**
     * Gets the under utilized host.
     *
     * @param excludedHosts the excluded hosts
     * @return the under utilized host
     */
    protected PowerContainerHost getUnderUtilizedHost(Set<? extends ContainerHost> excludedHosts) {
        double minUtilization = 1;
        PowerContainerHost underUtilizedHost = null;
        for (PowerContainerHost host : this.<PowerContainerHost>getContainerHostList()) {
            if (excludedHosts.contains(host)) {
                continue;
            }

            double utilization = host.getUtilizationOfCpu();
            if (utilization > 0 && utilization < minUtilization
                    && !areAllVmsMigratingOutOrAnyVmMigratingIn(host)&& !areAllContainersMigratingOutOrAnyContainersMigratingIn(host)) {
                minUtilization = utilization;
                underUtilizedHost = host;
            }
        }
        return underUtilizedHost;
    }






    /**
     * Checks whether all vms are in migration.
     *
     * @param host the host
     * @return true, if successful
     */
    protected boolean areAllVmsMigratingOutOrAnyVmMigratingIn(PowerContainerHost host) {
        for (PowerContainerPod vm : host.<PowerContainerPod>getVmList()) {
            if (!vm.isInMigration()) {
                return false;
            }
            if (host.getVmsMigratingIn().contains(vm)) {
                return true;
            }
        }
        return true;
    }

    /**
     * Checks whether all vms are in migration.
     *
     * @param host the host
     * @return true, if successful
     */
    protected boolean areAllContainersMigratingOutOrAnyContainersMigratingIn(PowerContainerHost host) {
        for (PowerContainerPod vm : host.<PowerContainerPod>getVmList()) {
           if(vm.getContainersMigratingIn().size() != 0){
               return true;
           }
            for(Container container:vm.getContainerList()){
                if(!container.isInMigration()){
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if is host over utilized.
     *
     * @param host the host
     * @return true, if is host over utilized
     */
    protected abstract boolean isHostOverUtilized(PowerContainerHost host);


    /**
     * Checks if is host over utilized.
     *
     * @param host the host
     * @return true, if is host over utilized
     */
    protected abstract boolean isHostUnderUtilized(PowerContainerHost host);


    /**
     * Adds the history value.
     *
     * @param host   the host
     * @param metric the metric
     */
    protected void addHistoryEntry(ContainerHostDynamicWorkload host, double metric) {
        int hostId = host.getId();
        if (!getTimeHistory().containsKey(hostId)) {
            getTimeHistory().put(hostId, new LinkedList<Double>());
        }
        if (!getUtilizationHistory().containsKey(hostId)) {
            getUtilizationHistory().put(hostId, new LinkedList<Double>());
        }
        if (!getMetricHistory().containsKey(hostId)) {
            getMetricHistory().put(hostId, new LinkedList<Double>());
        }
        if (!getTimeHistory().get(hostId).contains(CloudSim.clock())) {
            getTimeHistory().get(hostId).add(CloudSim.clock());
            getUtilizationHistory().get(hostId).add(host.getUtilizationOfCpu());
            getMetricHistory().get(hostId).add(metric);
        }
    }

    /**
     * Save allocation.
     */
    protected void saveAllocation() {
        getSavedAllocation().clear();
        for (ContainerHost host : getContainerHostList()) {
            for (ContainerPod vm : host.getVmList()) {
                if (host.getVmsMigratingIn().contains(vm)) {
                    continue;
                }
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("host", host);
                map.put("vm", vm);
                getSavedAllocation().add(map);
            }
        }
    }

    /**
     * Restore allocation.
     */
    protected void restoreAllocation() {
        for (ContainerHost host : getContainerHostList()) {
            host.containerVmDestroyAll();
            host.reallocateMigratingInContainerVms();
        }
        for (Map<String, Object> map : getSavedAllocation()) {
            ContainerPod vm = (ContainerPod) map.get("vm");
            PowerContainerHost host = (PowerContainerHost) map.get("host");
            if (!host.containerVmCreate(vm)) {
                Log.printConcatLine("Couldn't restore VM #", vm.getId(), " on host #", host.getId());
                System.exit(0);
            }
            getVmTable().put(vm.getId(), host);
        }
    }

    /**
     * Gets the power after allocation.
     *
     * @param host the host
     * @param vm   the vm
     * @return the power after allocation
     */
    protected double getPowerAfterAllocation(PowerContainerHost host, ContainerPod vm) {
        Log.printLine("getPowerAfterAllocation");
        double power = 0;
        try {
            power = host.getPowerModel().getPower(getMaxUtilizationAfterAllocation(host, vm));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return power;
    }

    /**
     * Gets the power after allocation. We assume that load is balanced between PEs. The only
     * restriction is: VM's max MIPS < PE's MIPS
     *
     * @param host the host
     * @param vm   the vm
     * @return the power after allocation
     */
    protected double getMaxUtilizationAfterAllocation(PowerContainerHost host, ContainerPod vm) {
        double requestedTotalMips = vm.getCurrentRequestedTotalMips();
        double hostUtilizationMips = getUtilizationOfCpuMips(host);
        double hostPotentialUtilizationMips = hostUtilizationMips + requestedTotalMips;
        double pePotentialUtilization = hostPotentialUtilizationMips / host.getTotalMips();
        return pePotentialUtilization;
    }

    /**
     * Gets the utilization of the CPU in MIPS for the current potentially allocated VMs.
     *
     * @param host the host
     * @return the utilization of the CPU in MIPS
     */
    protected double getUtilizationOfCpuMips(PowerContainerHost host) {
        double hostUtilizationMips = 0;
        for (ContainerPod vm2 : host.getVmList()) {
            if (host.getVmsMigratingIn().contains(vm2)) {
                // calculate additional potential CPU usage of a migrating in VM
                hostUtilizationMips += host.getTotalAllocatedMipsForContainerVm(vm2) * 0.9 / 0.1;
            }
            hostUtilizationMips += host.getTotalAllocatedMipsForContainerVm(vm2);
        }
        return hostUtilizationMips;
    }

    /**
     * Gets the saved allocation.
     *
     * @return the saved allocation
     */
    protected List<Map<String, Object>> getSavedAllocation() {
        return savedAllocation;
    }

    /**
     * Sets the vm selection policy.
     *
     * @param vmSelectionPolicy the new vm selection policy
     */
    protected void setVmSelectionPolicy(PowerContainerPodSelectionPolicy vmSelectionPolicy) {
        this.vmSelectionPolicy = vmSelectionPolicy;
    }

    /**
     * Gets the vm selection policy.
     *
     * @return the vm selection policy
     */
    protected PowerContainerPodSelectionPolicy getVmSelectionPolicy() {
        return vmSelectionPolicy;
    }

    /**
     * Gets the utilization history.
     *
     * @return the utilization history
     */
    public Map<Integer, List<Double>> getUtilizationHistory() {
        return utilizationHistory;
    }

    /**
     * Gets the metric history.
     *
     * @return the metric history
     */
    public Map<Integer, List<Double>> getMetricHistory() {
        return metricHistory;
    }

    /**
     * Gets the time history.
     *
     * @return the time history
     */
    public Map<Integer, List<Double>> getTimeHistory() {
        return timeHistory;
    }

    /**
     * Gets the execution time history vm selection.
     *
     * @return the execution time history vm selection
     */
    public List<Double> getExecutionTimeHistoryVmSelection() {
        return executionTimeHistoryVmSelection;
    }

    /**
     * Gets the execution time history host selection.
     *
     * @return the execution time history host selection
     */
    public List<Double> getExecutionTimeHistoryHostSelection() {
        return executionTimeHistoryHostSelection;
    }

    /**
     * Gets the execution time history vm reallocation.
     *
     * @return the execution time history vm reallocation
     */
    public List<Double> getExecutionTimeHistoryVmReallocation() {
        return executionTimeHistoryVmReallocation;
    }

    /**
     * Gets the execution time history total.
     *
     * @return the execution time history total
     */
    public List<Double> getExecutionTimeHistoryTotal() {
        return executionTimeHistoryTotal;
    }


//    public abstract List<? extends Container> getContainersToMigrateFromHosts(List<PowerContainerHostUtilizationHistory> overUtilizedHosts);
}















