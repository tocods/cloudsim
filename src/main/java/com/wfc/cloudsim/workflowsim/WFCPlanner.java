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
package com.wfc.cloudsim.workflowsim;

import com.wfc.cloudsim.cloudsim.Log;
import com.wfc.cloudsim.cloudsim.container.core.Container;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;
import com.wfc.cloudsim.cloudsim.core.CloudSimTags;
import com.wfc.cloudsim.cloudsim.core.SimEntity;
import com.wfc.cloudsim.cloudsim.core.SimEvent;
import com.wfc.cloudsim.wfc.core.WFCConstants;
import com.wfc.cloudsim.workflowsim.planning.BasePlanningAlgorithm;
import com.wfc.cloudsim.workflowsim.planning.DHEFTPlanningAlgorithm;
import com.wfc.cloudsim.workflowsim.planning.HEFTPlanningAlgorithm;
import com.wfc.cloudsim.workflowsim.planning.RandomPlanningAlgorithm;
import com.wfc.cloudsim.workflowsim.utils.Parameters;
import com.wfc.cloudsim.workflowsim.utils.Parameters.PlanningAlgorithm;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * WFCPlanner supports dynamic planning. In the future we will have global
 and static algorithm here. The WorkflowSim starts from WFCPlanner. It
 picks up a planning algorithm based on the configuration
 *
 * @author Arman Riazi
 * @since WorkflowSim Toolkit 1.0
 * @date March 29, 2020
 *
 */
public final class WFCPlanner extends SimEntity {

    private Integer dataCenterId;
    /**
     * The task list.
     */
    protected List< Task> taskList;

    private List<Integer> schedulerIds;
    /**
     * The workflow parser.
     */
    protected YamlUtil parser;
    /**
     * The associated clustering engine.
     */
    private int clusteringEngineId;
    private WFCEngineClustering clusteringEngine;

    /**
     * Created a new WorkflowPlanner object.
     *
     * @param name name to be associated with this entity (as required by
     * Sim_entity class from simjava package)
     * @throws Exception the exception
     * @pre name != null
     * @post $none
     */
    public WFCPlanner(String name) throws Exception {
        this(name, 1, 1, new ArrayList<>(), new ArrayList<>());
    }

    public WFCPlanner(String name, int schedulers, Integer id, List<ContainerPod> pods, List<Container> containers) throws Exception {
        super(name);

        setTaskList(new ArrayList<>());
        this.clusteringEngine = new WFCEngineClustering(name + "_Merger_", schedulers, pods, containers);
        this.clusteringEngineId = this.clusteringEngine.getId();
        this.parser = new YamlUtil(getClusteringEngine().getWorkflowEngine().getSchedulerId(0));
        //this.schedulerIds = new ArrayList<>();
        this.schedulerIds = this.clusteringEngine.getSchedulerIds();
        this.dataCenterId = id;
    }

    /**
     * Gets the clustering engine id
     *
     * @return clustering engine id
     */
    public int getClusteringEngineId() {
        return this.clusteringEngineId;
    }

    /**
     * Gets the clustering engine
     *
     * @return the clustering engine
     */
    public WFCEngineClustering getClusteringEngine() {
        return this.clusteringEngine;
    }

    /**
     * Gets the workflow parser
     *
     * @return the workflow parser
     */
    public YamlUtil getWorkflowParser() {
        return this.parser;
    }

    /**
     * Gets the workflow engine id
     *
     * @return the workflow engine id
     */
    public int getWorkflowEngineId() {
        return getClusteringEngine().getWorkflowEngineId();
    }

    /**
     * Gets the workflow engine
     *
     * @return the workflow engine
     */
    public WFCEngine getWorkflowEngine() {
        return getClusteringEngine().getWorkflowEngine();
    }

    /**
     * Processes events available for this Broker.
     *
     * @param ev a SimEvent object
     * @pre ev != null
     * @post $none
     */
    @Override
    public void processEvent(SimEvent ev) {
        if(WFCConstants.CAN_PRINT_SEQ_LOG)
            Log.printLine("WFPlanner=>ProccessEvent()=>ev.getTag():"+ev.getTag());
        switch (ev.getTag()) {
            case WorkflowSimTags.START_SIMULATION:
                getWorkflowParser().parse();
                //if(Parameters.getPlanningAlgorithm().equals(PlanningAlgorithm.INVALID))
                //    sendNow(getClusteringEngineId(), WorkflowSimTags.JOB_SUBMIT, getWorkflowParser().getTaskList());
                List<Task> tasks = getWorkflowParser().getTaskList();

                // pauseContainer
                for(Map.Entry<Integer, Pair<Double, Double>> entry: WFCConstants.pause.entrySet()) {
                    Integer ContainerID = entry.getKey();
                    Double pauseTime = entry.getValue().getValue();
                    if(tasks.get(ContainerID-1) == null) continue;
                    long length = (long)(pauseTime * 1000 * Parameters.getRuntimeScale());

                    tasks.get(ContainerID-1).setCloudletLength(tasks.get(ContainerID-1).getCloudletLength() + length);
                }
                setTaskList(getWorkflowParser().getTaskList());
                processPlanning();
                processImpactFactors(getTaskList());

                sendNow(getClusteringEngineId(), WorkflowSimTags.JOB_SUBMIT, getTaskList());
                sendNow(this.dataCenterId, WorkflowSimTags.JOBS_GIVE, getTaskList());
                for(Integer i: schedulerIds) {
                    sendNow(i, WorkflowSimTags.JOBS_GIVE, getTaskList());
                }
                break;
            case CloudSimTags.END_OF_SIMULATION:
                shutdownEntity();
                break;
            // other unknown tags are processed by this method
            case WorkflowSimTags.CLOUDLET_UPDATE:
                processPlanning();
            default:
                processOtherEvent(ev);
                break;
        }
    }

    private void processPlanning() {
        if (Parameters.getPlanningAlgorithm().equals(PlanningAlgorithm.INVALID)) {
            return;
        }
        BasePlanningAlgorithm planner = getPlanningAlgorithm(Parameters.getPlanningAlgorithm());
        
        planner.setTaskList(getTaskList());
        planner.setVmList(getWorkflowEngine().getAllVmList());
        try {
            Log.printLine("start run planner");
            planner.run();
        } catch (Exception e) {
            Log.printLine("Error in configuring scheduler_method");
            e.printStackTrace();
        }
    }

    /**
     * Switch between multiple planners. Based on planner.method
     *
     * @param name the SCHMethod name
     * @return the scheduler that extends BaseScheduler
     */
    private BasePlanningAlgorithm getPlanningAlgorithm(PlanningAlgorithm name) {
        BasePlanningAlgorithm planner;

        // choose which scheduler to use. Make sure you have add related enum in
        //Parameters.java
        switch (name) {
            //by default it is FCFS_SCH
            case INVALID:
                planner = null;
                break;
            case RANDOM:
                planner = new RandomPlanningAlgorithm();
                break;
            case HEFT:
                planner = new HEFTPlanningAlgorithm();
                break;
            case DHEFT:
                planner = new DHEFTPlanningAlgorithm();
                break;
            default:
                planner = null;
                break;
        }
        return planner;
    }

    /**
     * Add impact factor for each task. This is useful in task balanced
     * clustering algorithm It is for research purpose and thus it is optional.
     *
     * @param taskList all the tasks
     */
    private void processImpactFactors(List<Task> taskList) {
        List<Task> exits = new ArrayList<>();
        for (Task task : taskList) {
            if (task.getChildList().isEmpty()) {
                exits.add(task);
            }
        }
        double avg = 1.0 / exits.size();
        for (Task task : exits) {
            addImpact(task, avg);
        }
    }

    /**
     * Add impact factor for one particular task
     *
     * @param task, the task
     * @param impact , the impact factor
     */
    private void addImpact(Task task, double impact) {

        task.setImpact(task.getImpact() + impact);
        int size = task.getParentList().size();
        if (size > 0) {
            double avg = impact / size;
            for (Task parent : task.getParentList()) {
                addImpact(parent, avg);
            }
        }
    }

    /**
     * Overrides this method when making a new and different type of Broker.
     *
     *
     * @param ev a SimEvent object
     * @pre ev != null
     * @post $none
     */
    protected void processOtherEvent(SimEvent ev) {
        if (ev == null) {
            Log.printLine(getName() + ".processOtherEvent(): " + "Error - an event is null.");
            return;
        }

        Log.printLine(getName() + ".processOtherEvent(): "
                + "Error - event unknown by this DatacenterBroker.");
    }

    /**
     * Send an internal event communicating the end of the simulation.
     *
     * @pre $none
     * @post $none
     */
    protected void finishExecution() {
        //sendNow(getId(), CloudSimTags.END_OF_SIMULATION);
    }

    /*
     * (non-Javadoc)
     * @see cloudsim.core.SimEntity#shutdownEntity()
     */
    @Override
    public void shutdownEntity() {
        Log.printLine(getName() + " is shutting down...");
    }

    /*
     * (non-Javadoc)
     * @see cloudsim.core.SimEntity#startEntity()
     */
    @Override
    public void startEntity() {
        Log.printLine("Starting WorkflowSim " + Parameters.getVersion());
        Log.printLine(getName() + " is starting...");
        schedule(getId(), 0, WorkflowSimTags.START_SIMULATION);
    }

    /**
     * Gets the task list.
     *
     * @return the task list
     */
    @SuppressWarnings("unchecked")
    public List<Task> getTaskList() {
        return (List<Task>) taskList;
    }

    /**
     * Sets the task list.
     *
     * @param taskList
     */
    protected void setTaskList(List<Task> taskList) {
        this.taskList = taskList;
    }
}
