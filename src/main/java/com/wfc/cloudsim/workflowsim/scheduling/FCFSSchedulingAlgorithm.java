/**
 * Copyright 2019-2020 University Of Southern California
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.com.wfc.cloudsim/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.wfc.cloudsim.workflowsim.scheduling;

import com.wfc.cloudsim.cloudsim.Cloudlet;
import com.wfc.cloudsim.cloudsim.container.core.Container;
import com.wfc.cloudsim.cloudsim.container.core.ContainerCloudlet;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;
import com.wfc.cloudsim.workflowsim.WorkflowSimTags;

import java.util.Iterator;

/**
 * The FCFS algorithm. 
 *
 * @author Arman Riazi
 * @since WorkflowSim Toolkit 1.0
 * @date March 29, 2020
 */
public class FCFSSchedulingAlgorithm extends BaseSchedulingAlgorithm {

    /**
     * The main function
     */
    @Override
    public void run() {


        for (Iterator it = getCloudletList().iterator(); it.hasNext();) {
            Cloudlet cloudlet = (Cloudlet) it.next();
            boolean stillHasContainer = false;
            for (Iterator itc = getVmList().iterator(); itc.hasNext();) {

                ContainerPod vm = (ContainerPod) itc.next();
                if (vm.getState() == WorkflowSimTags.VM_STATUS_IDLE) {
                    for(Container c: vm.getContainerList()) {
                        if(c.getState() == WorkflowSimTags.VM_STATUS_IDLE) {
                            stillHasContainer = true;
                            c.setState(WorkflowSimTags.VM_STATUS_BUSY);
                            cloudlet.setVmId(vm.getId());
                            ((ContainerCloudlet)cloudlet).setContainerId(c.getId());
                            getScheduledList().add(cloudlet);
                            break;
                        }
                    }
                }
            }
            //no vm available 
            if (!stillHasContainer) {
                break;
            }
        }
    }
}
