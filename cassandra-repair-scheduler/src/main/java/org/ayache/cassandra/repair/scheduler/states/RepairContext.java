/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.states;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import org.ayache.automaton.api.IState;
import org.ayache.cassandra.admin.INodeFactory;
import org.ayache.cassandra.admin.api.dto.RepairConfigDto;
import org.ayache.cassandra.repair.scheduler.INodeReparator;
import org.ayache.cassandra.repair.scheduler.INodeReparator.Status;
import org.ayache.cassandra.repair.scheduler.RepairAutomaton;
import org.ayache.cassandra.repair.scheduler.RepairTransition;
import org.ayache.cassandra.repair.scheduler.jaxrs.ClusterServiceFactory;
import org.ayache.cassandra.repair.scheduler.model.INodeConnectorRetriever;

/**
 *
 * @author Ayache
 */
public class RepairContext {

    private final NavigableSet<String> nodesToRepair = new ConcurrentSkipListSet<>();
    private final Set<String> aggregatedNodesToRepair = new ConcurrentSkipListSet<>();
    private final Set<String> nodesToRepairInError = new ConcurrentSkipListSet<>();
    private final Set<String> nodesToRepairInUnknown = new ConcurrentSkipListSet<>();
    private final transient Set<String> nodesToRepairToRestart = new ConcurrentSkipListSet<>();
    private final Map<String, ErrorInfoAggregator> nodesToRepairInFailure = new ConcurrentHashMap<>();
    private final transient RepairAutomaton automaton = new RepairAutomaton();
    private final SizedLinkedList<String> messages = new SizedLinkedList();              

    private static final class SizedLinkedList<T> extends ConcurrentLinkedQueue<T> {

        public boolean add(T e) {
            if (size() > 200) {
                poll();
            }
            return super.add(e);
        }

    };
    private transient INodeConnectorRetriever<INodeFactory> retriever;
    volatile INodeReparator.Status status = Status.STARTED;
    private final String clusterName;
    private final int jmxPort;
    int hourToBegin;
    int lastHourToBegin;
    int minutesToBegin;
    int lastMinutesToBegin;
    public boolean repairLocalDCOnly;
    boolean simult = true;
    private final transient ExecutorService executorService = Executors.newFixedThreadPool(1);
    private volatile transient Future currentTask;
    private volatile transient Condition waitingCondition;
    private volatile transient Lock waitingLock;
//    volatile boolean cancel;
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
            INodeFactory connector = retriever.getNodeConnector();
            nodesToRepair.addAll(connector.getNodeChooser(lastRepairedNode, simult).getNextNodeToRepair());
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
            lastRepairedNode = nodesToRepair.first();
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
    public RepairContext error(String host, INodeReparator.Status status, String... messages) {
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

    public boolean addNodeToRestart(String id) {
        if (nodesToRepairInUnknown.contains(id)){
            nodesToRepairToRestart.add(id);
        }
        return nodesToRepairToRestart.equals(nodesToRepairInUnknown);
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

    public INodeReparator.Status getStatus() {
        return status;
    }

//    public void checkJMXConnections() throws IOException, InterruptedException {
//        for (NodeConnector connector : retriever.iterable()) {
//            connector.getSsProxy().getClusterName();
//        }
//    }

    public void init(INodeConnectorRetriever<INodeFactory> reriever) {
        this.retriever = reriever;
    }

    public INodeReparator getNodeProbe(String host) throws IOException {
        return retriever.getNodeConnector(host).getNodeReparator();
    }

    public void cancelRepairSessions() {
        activate(RepairTransition.CANCEL);
        if (currentTask != null){
            currentTask.cancel(true);
        }
    }

    public void activate(RepairTransition... transitions) {
        ActivatorRunnable activatorRunnable = new ActivatorRunnable(executorService, automaton, transitions) {
            @Override
            public void onTaskStarted(Future task) {
                currentTask = task;
            }
        };
        activatorRunnable.init();
    }

    void onTaskStarted(Future task){
        currentTask = task;
    }
    
    public IState getState() {
        return automaton.getCurrentState();
    }

    public Collection<String> getMessages() {
        return messages;
    }

    public RepairConfigDto getConfigurations() {
        return nextConfig != null ? nextConfig : RepairConfigDto.RepairConfigBuilder.build(hourToBegin, minutesToBegin, lastHourToBegin, lastMinutesToBegin, simult, repairLocalDCOnly);
    }

    public void editConfigurations(RepairConfigDto dto) {
        nextConfig = dto;
        activate(RepairTransition.CONFIG_CHANGED);
    }

    public boolean checkAndApplyChanges() {
        if (nextConfig != null) {
            hourToBegin = nextConfig.hourToBegin;
            minutesToBegin = nextConfig.minutesToBegin;
            lastHourToBegin = nextConfig.lastHourToBegin;
            lastMinutesToBegin = nextConfig.lastMinutesToBegin;
            repairLocalDCOnly = nextConfig.repairLocalDCOnly;
            simult = nextConfig.simultaneousRepair;
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
                waitingCondition.signal();
            } finally {
                waitingLock.unlock();
            }
        }
    }

    public void save() throws IOException {
        ClusterServiceFactory.getInstance().saveCluster(clusterName);
    }

    public Status status() {
        return status;
    }
    
    

    private abstract static class ActivatorRunnable implements Runnable {

        private final RepairTransition[] transitions;
        private final RepairAutomaton automaton;
        private final ExecutorService executorService;
        
        public ActivatorRunnable(ExecutorService executorService, RepairAutomaton automaton, RepairTransition... transitions) {
            this.transitions = transitions;
            this.automaton = automaton;
            this.executorService = executorService;
        }

        public void init() {
            executorService.submit(this);
        }
        
        @Override
        public void run() {
            automaton.activate(transitions);
            automaton.updateCurrentState();
            Future postExecute = automaton.postExecute();
            if (postExecute != null){
                onTaskStarted(postExecute);
            }
        }

        public abstract void onTaskStarted(Future task);
        
    }
//
//    private static final class FutureHolder {
//
//        Future task;
//        ActivatorRunnable runnable;
//        RepairContext context;
//        
//        public FutureHolder(Future task, ActivatorRunnable runnable, RepairContext context) {
//            this.task = task;
//            this.runnable = runnable;
//            this.context = context;
//        }
//        
//        void taskStarted() {
//            context.onTaskStarted(task);
//        }
//        
//        void taskDone() {
//            
//        }
//        
//        public void cancel(boolean mayInterrupt) {
//            if (runnable.started) {
//                task.cancel(mayInterrupt);
//            }
//        }
//    }

}
