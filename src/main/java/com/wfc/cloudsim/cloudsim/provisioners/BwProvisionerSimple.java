/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.com.wfc.cloudsim/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package com.wfc.cloudsim.cloudsim.provisioners;

import com.wfc.cloudsim.cloudsim.Pod;

import java.util.HashMap;
import java.util.Map;

/**
 * BwProvisionerSimple is an extension of {@link BwProvisioner} which uses a best-effort policy to
 * allocate bandwidth (bw) to VMs: 
 * if there is available bw on the host, it allocates; otherwise, it fails. 
 * Each host has to have its own instance of a RamProvisioner.
 * 
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 1.0
 */
public class BwProvisionerSimple extends BwProvisioner {

	/** The BW map, where each key is a VM id and each value
         * is the amount of BW allocated to that VM. */
	private Map<String, Long> bwTable;

	/**
	 * Instantiates a new bw provisioner simple.
	 * 
	 * @param bw The total bw capacity from the host that the provisioner can allocate to VMs. 
	 */
	public BwProvisionerSimple(long bw) {
		super(bw);
		setBwTable(new HashMap<String, Long>());
	}

	@Override
	public boolean allocateBwForVm(Pod pod, long bw) {
		deallocateBwForVm(pod);

		if (getAvailableBw() >= bw) {
			setAvailableBw(getAvailableBw() - bw);
			getBwTable().put(pod.getUid(), bw);
			pod.setCurrentAllocatedBw(getAllocatedBwForVm(pod));
			return true;
		}

		pod.setCurrentAllocatedBw(getAllocatedBwForVm(pod));
		return false;
	}

	@Override
	public long getAllocatedBwForVm(Pod pod) {
		if (getBwTable().containsKey(pod.getUid())) {
			return getBwTable().get(pod.getUid());
		}
		return 0;
	}

	@Override
	public void deallocateBwForVm(Pod pod) {
		if (getBwTable().containsKey(pod.getUid())) {
			long amountFreed = getBwTable().remove(pod.getUid());
			setAvailableBw(getAvailableBw() + amountFreed);
			pod.setCurrentAllocatedBw(0);
		}
	}

	@Override
	public void deallocateBwForAllVms() {
		super.deallocateBwForAllVms();
		getBwTable().clear();
	}

	@Override
	public boolean isSuitableForVm(Pod pod, long bw) {
		long allocatedBw = getAllocatedBwForVm(pod);
		boolean result = allocateBwForVm(pod, bw);
		deallocateBwForVm(pod);
		if (allocatedBw > 0) {
			allocateBwForVm(pod, allocatedBw);
		}
		return result;
	}

	/**
	 * Gets the map between VMs and allocated bw.
	 * 
	 * @return the bw map
	 */
	protected Map<String, Long> getBwTable() {
		return bwTable;
	}

	/**
	 * Sets the map between VMs and allocated bw.
	 * 
	 * @param bwTable the bw map
	 */
	protected void setBwTable(Map<String, Long> bwTable) {
		this.bwTable = bwTable;
	}

}