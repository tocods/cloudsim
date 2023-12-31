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
package com.wfc.cloudsim.workflowsim.planning;

import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;
import com.wfc.cloudsim.wfc.core.WFCDatacenter;
import com.wfc.cloudsim.wfc.core.WFCDatacenter;
import com.wfc.cloudsim.workflowsim.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * The base planner has implemented the basic features. Every other planning method
 * should extend from BasePlanningAlgorithm but should not directly use it. 
 *
 * @author Arman Riazi
 * @since WorkflowSim Toolkit 1.0
 * @date Jun 17, 2013
 */
public abstract class BasePlanningAlgorithm implements PlanningAlgorithmInterface {

    /**
     * the task list.
     */
    private List<Task> tasktList;
    /**
     * the vm list.
     */
    private List<? extends ContainerPod> vmList;

    /**
     * the datacenter list
     */
    private List<? extends WFCDatacenter> datacenterList;
    /**
     * Initialize a BaseScheduler
     */
    public BasePlanningAlgorithm() {
    }

    /**
     * Sets the job list.
     *
     * @param list
     */
    @Override
    public void setTaskList(List list) {
        this.tasktList = list;
    }

    /**
     * Sets the vm list
     *
     * @param list
     */
    @Override
    public void setVmList(List list) {
        this.vmList = new ArrayList(list);
    }

    /**
     * Gets the task list.
     *
     * @return the task list
     */
    @Override
    public List<Task> getTaskList() {
        return this.tasktList;
    }

    /**
     * Gets the vm list
     *
     * @return the vm list
     */
    @Override
    public List getVmList() {
        return this.vmList;
    }

    /**
     * Gets the datacenter list
     * @return the datacenter list
     */
    public List getDatacenterList(){
        return this.datacenterList;
    }
    
    /**
     * Sets the datacenter list
     * @param list the datacenter list
     */
    public void setDatacenterList(List list){
        this.datacenterList = list;
    }
    
    /**
     * The main function
     */
    public abstract void run() throws Exception;

    
}
