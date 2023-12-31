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
import com.wfc.cloudsim.cloudsim.Cloudlet;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;
import com.wfc.cloudsim.workflowsim.FileItem;
import com.wfc.cloudsim.workflowsim.Job;
import com.wfc.cloudsim.workflowsim.WorkflowSimTags;
import com.wfc.cloudsim.workflowsim.utils.WFCReplicaCatalog;

import java.util.List;

/**
 * Data aware algorithm. Schedule a job to a vm that has most input data it requires. 
 * It only works for a local environment. 
 *
 * @author Arman Riazi
 * @since WorkflowSim Toolkit 1.0
 * @date March 29, 2020
 */
public class DataAwareSchedulingAlgorithm extends BaseSchedulingAlgorithm {

    public DataAwareSchedulingAlgorithm() {
        super();
    }

    @Override
    public void run() {
        
        int size = getCloudletList().size();

        for (int i = 0; i < size; i++) {

            Cloudlet cloudlet = (Cloudlet) getCloudletList().get(i);

            int vmSize = getVmList().size();
            ContainerPod closestVm = null;//(CondorPod)getVmList().get(0);
            double minTime = Double.MAX_VALUE;
            for (int j = 0; j < vmSize; j++) {
                ContainerPod vm = (ContainerPod) getVmList().get(j);
                if (vm.getState() == WorkflowSimTags.VM_STATUS_IDLE) {
                    Job job = (Job)cloudlet;
                    double time = dataTransferTime(job.getFileList(), cloudlet, vm.getId());
                    if(time < minTime){
                        minTime = time;
                        closestVm = vm;
                    }
                    
                }
            }

            if(closestVm!=null){
                closestVm.setState(WorkflowSimTags.VM_STATUS_BUSY);
                cloudlet.setVmId(closestVm.getId());
                getScheduledList().add(cloudlet);
            }
        }
    }

    /*
     * Stage in for a single job (both stage-in job and compute job)
     * @param requiredFiles, all files to be stage-in
     * @param cl, the job to be processed
     * @pre  $none
     * @post $none
     */

    protected double dataTransferTime(List<FileItem> requiredFiles, Cloudlet cl, int vmId)  {
        double time = 0.0;

        for (FileItem file : requiredFiles) {
            //The input file is not an output File 
            if (file.isRealInputFile(requiredFiles)) {
                List<String> siteList = WFCReplicaCatalog.getStorageList(file.getName());

                boolean hasFile = false;
                for (String site : siteList) {
                    if(site.equals(Integer.toString(vmId))){
                        hasFile = true;
                        break;
                    }
                }
                if(!hasFile){
                    time += file.getSize() ;
                }
            }
        }
        return time;
    }
}
