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
package com.wfc.cloudsim.workflowsim.failure;

/**
 * Failure Record is a record with all information of a failure
 *
 * @author Arman Riazi
 * @since WorkflowSim Toolkit 1.0
 * @date March 29, 2020
 */
public class FailureRecord {

    /**
     * Length
     */
    public double length;
    /**
     * number of failed tasks.
     */
    public int failedTasksNum;
    /**
     * the depth (also used as type in some cases) of a failure.
     */
    public int depth;//used as type
    /**
     * all the tasks (failed or not).
     */
    public int allTaskNum;
    /**
     * the location.
     */
    public int vmId;
    /**
     * the job id.
     */
    public int jobId;
    /**
     * the workflow id (user id in this version).
     */
    public int workflowId;
    /**
     * delay length.
     */
    public double delayLength;

    /**
     * Initialize a Failure Record
     *
     * @param length, length of this task
     * @param tasks, number of failed tasks
     * @param depth, depth of the failed tasks
     * @param all, all the tasks
     * @param vm, vm id where the failure generates
     * @param job, job id where the failure generates
     * @param workflow , workflow id where the failure generates
     */
    public FailureRecord(double length, int tasks, int depth, int all, int vm, int job, int workflow) {
        this.length = length;
        this.failedTasksNum = tasks;
        this.depth = depth;
        this.allTaskNum = all;
        this.vmId = vm;
        this.jobId = job;
        this.workflowId = workflow;
    }
}
