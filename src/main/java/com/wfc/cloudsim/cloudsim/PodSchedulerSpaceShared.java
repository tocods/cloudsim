/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.com.wfc.cloudsim/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package com.wfc.cloudsim.cloudsim;

import java.util.*;

/**
 * PodSchedulerSpaceShared is a VMM allocation policy that allocates one or more PEs from a host to a
 * Virtual Machine Monitor (VMM), and doesn't allow sharing of PEs. 
 * The allocated PEs will be used until the VM finishes running. 
 * If there is no enough free PEs as required by a VM,
 * or whether the available PEs doesn't have enough capacity, the allocation fails. 
 * In the case of fail, no PE is allocated to the requesting VM.
 * 
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 1.0
 */
public class PodSchedulerSpaceShared extends PodScheduler {

	/** A map between each VM and its allocated PEs, where the key is a VM ID and
         * the value a list of PEs allocated to VM. */
	private Map<String, List<Pe>> peAllocationMap;

	/** The list of free PEs yet available in the host. */
	private List<Pe> freePes;

	/**
	 * Instantiates a new vm space-shared scheduler.
	 * 
	 * @param pelist the pelist
	 */
	public PodSchedulerSpaceShared(List<? extends Pe> pelist) {
		super(pelist);
		setPeAllocationMap(new HashMap<String, List<Pe>>());
		setFreePes(new ArrayList<Pe>());
		getFreePes().addAll(pelist);
	}

	@Override
	public boolean allocatePesForVm(Pod pod, List<Double> mipsShare) {
		// if there is no enough free PEs, fails
		if (getFreePes().size() < mipsShare.size()) {
			return false;
		}

		List<Pe> selectedPes = new ArrayList<Pe>();
		Iterator<Pe> peIterator = getFreePes().iterator();
		Pe pe = peIterator.next();
		double totalMips = 0;
		for (Double mips : mipsShare) {
			if (mips <= pe.getMips()) {
				selectedPes.add(pe);
				if (!peIterator.hasNext()) {
					break;
				}
				pe = peIterator.next();
				totalMips += mips;
			}
		}
		if (mipsShare.size() > selectedPes.size()) {
			return false;
		}

		getFreePes().removeAll(selectedPes);

		getPeAllocationMap().put(pod.getUid(), selectedPes);
		getMipsMap().put(pod.getUid(), mipsShare);
		setAvailableMips(getAvailableMips() - totalMips);
		return true;
	}

	@Override
	public void deallocatePesForVm(Pod pod) {
		getFreePes().addAll(getPeAllocationMap().get(pod.getUid()));
		getPeAllocationMap().remove(pod.getUid());

		double totalMips = 0;
		for (double mips : getMipsMap().get(pod.getUid())) {
			totalMips += mips;
		}
		setAvailableMips(getAvailableMips() + totalMips);

		getMipsMap().remove(pod.getUid());
	}

	/**
	 * Sets the pe allocation map.
	 * 
	 * @param peAllocationMap the pe allocation map
	 */
	protected void setPeAllocationMap(Map<String, List<Pe>> peAllocationMap) {
		this.peAllocationMap = peAllocationMap;
	}

	/**
	 * Gets the pe allocation map.
	 * 
	 * @return the pe allocation map
	 */
	protected Map<String, List<Pe>> getPeAllocationMap() {
		return peAllocationMap;
	}

	/**
	 * Sets the free pes list.
	 * 
	 * @param freePes the new free pes list
	 */
	protected void setFreePes(List<Pe> freePes) {
		this.freePes = freePes;
	}

	/**
	 * Gets the free pes list.
	 * 
	 * @return the free pes list
	 */
	protected List<Pe> getFreePes() {
		return freePes;
	}

}
