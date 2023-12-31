/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.com.wfc.cloudsim/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package com.wfc.cloudsim.cloudsim.power;

import com.wfc.cloudsim.cloudsim.Pod;
import com.wfc.cloudsim.cloudsim.core.CloudSim;

import java.util.List;

/**
 * A VM selection policy that selects for migration the VM with Minimum Utilization (MU)
 * of CPU.
 * 
 * <br/>If you are using any algorithms, policies or workload included in the power package please cite
 * the following paper:<br/>
 * 
 * <ul>
 * <li><a href="http://dx.doi.com.wfc.cloudsim/10.1002/cpe.1867">Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012</a>
 * </ul>
 * 
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 3.0
 */
public class PowerPodSelectionPolicyMinimumUtilization extends PowerPodSelectionPolicy {
	@Override
	public Pod getVmToMigrate(PowerHost host) {
		List<PowerPod> migratableVms = getMigratableVms(host);
		if (migratableVms.isEmpty()) {
			return null;
		}
		Pod podToMigrate = null;
		double minMetric = Double.MAX_VALUE;
		for (Pod pod : migratableVms) {
			if (pod.isInMigration()) {
				continue;
			}
			double metric = pod.getTotalUtilizationOfCpuMips(CloudSim.clock()) / pod.getMips();
			if (metric < minMetric) {
				minMetric = metric;
				podToMigrate = pod;
			}
		}
		return podToMigrate;
	}

}
