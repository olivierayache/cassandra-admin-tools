/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.states;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import org.ayache.cassandra.admin.api.dto.NodeDto;
import org.ayache.cassandra.admin.api.dto.RepairConfigDto;
import org.ayache.cassandra.repair.scheduler.NodeChooser;
import org.ayache.cassandra.repair.scheduler.NodeConnector;
import org.ayache.cassandra.repair.scheduler.NodeReparator;
import org.ayache.cassandra.repair.scheduler.NodeReparator.Status;
import org.ayache.cassandra.repair.scheduler.RepairAutomaton;
import org.ayache.cassandra.repair.scheduler.RepairTransition;
import org.ayache.cassandra.repair.scheduler.jaxrs.ClusterServiceFactory;

/**
 *
 * @author Ayache
 */
public class RepairContext {

    private final List<String> nodesToRepair = new ArrayList<>();
    private final List<String> aggregatedNodesToRepair = new ArrayList<>();
    private final Set<String> nodesToRepairInError = new ConcurrentSkipListSet<>();
    private final Set<String> nodesToRepairInUnknown = new ConcurrentSkipListSet<>();
    private final Map<String, ErrorInfoAggregator> nodesToRepairInFailure = new ConcurrentHashMap<>();
    private final transient RepairAutomaton automaton = new RepairAutomaton();
    private final List<String> messages = new LinkedList<String>(){
        @Override
        public boolean add(String e) {
            if (size() > 200){
                poll();
            }
            return super.add(e);
        }
        
    };
    public final transient Map<String, NodeConnector> map = new HashMap<>();
    volatile NodeReparator.Status status = Status.STARTED;
    private final String clusterName;
    final int jmxPort;
    int hourToBegin;
    int lastHourToBegin;
    int minutesToBegin;
    int lastMinutesToBegin;
    boolean repairLocalDCOnly;
    private final transient ExecutorService executorService = Executors.newFixedThreadPool(1);
    private volatile transient FutureHolder currentTask;
    private volatile transient Condition waitingCondition;
    private volatile transient Lock waitingLock;
    volatile boolean cancel;
    private RepairConfigDto nextConfig;
    private String lastRepairedNode;

    private RepairContext() {
        this.clusterName = null;
        this.jmxPort = 0;
        automaton.setParams(this);
    }
    
    public RepairContext(String clusterName, int jmxPort, int hourToBegin, int lastHourToBegin) {
        this.clusterName = clusterName;
        this.jmxPort = jmxPort;
        this.hourToBegin = hourToBegin;
        this.lastHourToBegin = lastHourToBegin;
        automaton.setParams(this);
    }

    /**
     * Retrieve list of nodes to repair. If errors occurs on previous sessions the list will contain these nodes.
     * @return list of nodes to repair
     * @throws IOException 
     */
    public Collection<String> initNodesToRepair() throws IOException {
        aggregatedNodesToRepair.clear();
        aggregatedNodesToRepair.addAll(nodesToRepairInError);
        aggregatedNodesToRepair.addAll(nodesToRepairInUnknown);
        if (aggregatedNodesToRepair.isEmpty()) {
            NodeConnector connector = map.values().iterator().next();
            nodesToRepair.addAll(new NodeChooser(connector.getSsProxy(), connector.getEsProxy(), connector.getDc(), lastRepairedNode).getNextNodeToRepair());
            aggregatedNodesToRepair.addAll(nodesToRepair);
        }
        return aggregatedNodesToRepair;
    }

    public Iterable<String> getNodesToRepair() {
        return aggregatedNodesToRepair;
    }

    public Iterable<String> getNodesInError() {
        return nodesToRepairInError;
    }

    /**
     * Clears 
     */
    public void clear() {
        if (!nodesToRepair.isEmpty()) {
            lastRepairedNode = nodesToRepair.get(0);
        }
        nodesToRepair.clear();
        nodesToRepairInError.clear();
        nodesToRepairInUnknown.clear();
    }

    /**
     * Declare node in error
     * @param host the host of node in error 
     * @param status error status (repair seesion failed, jmx problems...)
     * @param messages additional messages on error causes
     * @return the context
     */
    public RepairContext error(String host, NodeReparator.Status status, String... messages) {
        final ErrorInfoAggregator errorInfoAggregator = new ErrorInfoAggregator(host, status, messages);
        nodesToRepairInFailure.put(host, errorInfoAggregator);
        addMessage(errorInfoAggregator.toString());
        return this;
    }

    public Collection<String> getNodesToRepairInUnknown() {
        return nodesToRepairInUnknown;
    }

    public Iterable<ErrorInfoAggregator> getNodesToRepairInFailure() {
        return nodesToRepairInFailure.values();
    }

    public void addNodeInError(String host) {
        nodesToRepairInFailure.remove(host);
        nodesToRepairInError.add(host);
    }

    public void addNodeInUnknownError(String host) {
        nodesToRepairInFailure.remove(host);
        nodesToRepairInUnknown.add(host);
    }

    public void removeNodeInError(String id) {
        nodesToRepairInUnknown.remove(id);
    }

    public RepairContext addMessage(String message) {
        messages.add(message);
        return this;
    }

    public RepairContext addStatus(Status status) {
        this.status = status;
        return this;
    }

    public NodeReparator.Status getStatus() {
        return status;
    }

    public void checkJMXConnections() throws IOException, InterruptedException {
        for (NodeConnector connector : map.values()) {
            connector.getSsProxy().getClusterName();
        }
    }

    public void addNodeConnector(NodeConnector connector) {
        map.put(connector.getHost(), connector);
    }

    public NodeReparator getNodeProbe(String host) throws IOException {
        if (map.containsKey(host)) {
            return map.get(host).getNodeReparator();
        } else {
            NodeConnector nodeConnector = new NodeConnector(host, jmxPort);
            NodeReparator nodeProbe = nodeConnector.getNodeReparator();
            map.put(host, nodeConnector);
            return nodeProbe;
        }
    }

    public void cancelRepairSessions() {
        currentTask.cancel(true);
        cancel = true;
    }

    public void activate(RepairTransition... transitions) {
        ActivatorRunnable activatorRunnable = new ActivatorRunnable(automaton, transitions);
        currentTask = new FutureHolder(executorService.submit(activatorRunnable), activatorRunnable);
    }

    public String getState() {
        return automaton.getCurrentState().toString();
    }

    public Map<String, NodeDto> getNodes() throws IOException {
        if (!map.isEmpty()) {
            return map.values().iterator().next().getNodes();
        } else {
            return new HashMap<>();
        }
    }

    public Collection<String> getMessages() {
        return messages;
    }

    public RepairConfigDto getConfigurations() {
        return RepairConfigDto.RepairConfigBuilder.build(hourToBegin, minutesToBegin, lastHourToBegin, lastMinutesToBegin, true, repairLocalDCOnly);
    }

    public void editConfigurations(RepairConfigDto dto) {
        nextConfig = dto;
    }

    public boolean checkAndApplyChanges() {
        if (nextConfig != null) {
            hourToBegin = nextConfig.hourToBegin;
            minutesToBegin = nextConfig.minutesToBegin;
            lastHourToBegin = nextConfig.lastHourToBegin;
            lastMinutesToBegin = nextConfig.lastMinutesToBegin;
            repairLocalDCOnly = nextConfig.repairLocalDCOnly;
            nextConfig = null;
            return true;
        }
        return false;
    }

    void setWaitingCondition(Lock lock, Condition condition) {
        waitingLock = lock;
        waitingCondition = condition;
        //cancel = false;
    }

    public void cancelWaiting() {
        if (waitingLock != null && waitingLock.tryLock()) {
            try {
                cancel = true;
                waitingCondition.signal();
            } finally {
                waitingLock.unlock();
            }
        }
    }

    void save() throws IOException {
        ClusterServiceFactory.getInstance().saveCluster(clusterName);
    }

    private static class ActivatorRunnable implements Runnable {

        private final RepairTransition[] transitions;
        private final RepairAutomaton automaton;
        private volatile boolean started;

        public ActivatorRunnable(RepairAutomaton automaton, RepairTransition... transitions) {
            this.transitions = transitions;
            this.automaton = automaton;
        }

        @Override
        public void run() {
            started = true;
            automaton.activate(transitions);
            automaton.updateCurrentState();
            automaton.execute();
        }
    }

    private static final class FutureHolder {

        Future task;
        ActivatorRunnable runnable;

        public FutureHolder(Future task, ActivatorRunnable runnable) {
            this.task = task;
            this.runnable = runnable;
        }

        public void cancel(boolean mayInterrupt) {
            if (runnable.started) {
                task.cancel(mayInterrupt);
            }
        }
    }

}
