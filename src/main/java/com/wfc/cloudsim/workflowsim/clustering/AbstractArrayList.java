/*
 * 
 *   Copyright 2019-2020 University Of Southern California
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 * 
 *   http://www.apache.com.wfc.cloudsim/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */
package com.wfc.cloudsim.workflowsim.clustering;

import java.util.ArrayList;

/**
 * This data structure AbstractArrayList is used in clustering alone It is
 * better than ArrayList since it associates with depth information
 *
 * @author Arman Riazi
 * @since WorkflowSim Toolkit 1.0
 * @date March 29, 2020
 */
public class AbstractArrayList {

    /**
     * The task list.
     */
    private final ArrayList taskList;
    /**
     * The depth of these tasks.
     */
    private final int depth;

    /**
     * Initialize AbstractArrayList
     *
     * @param taskList the task list
     * @param depth the level of these tasks
     */
    public AbstractArrayList(ArrayList taskList, int depth) {
        this.taskList = taskList;
        this.hasChecked = false;
        this.depth = depth;
    }

    /**
     * Gets the task list
     *
     * @return task list
     */
    public ArrayList getArrayList() {
        return this.taskList;
    }

    /**
     * Gets the depth of these tasks
     *
     * @return depth
     */
    public int getDepth() {
        return this.depth;
    }
    /**
     * A check point.
     */
    public boolean hasChecked;
}
