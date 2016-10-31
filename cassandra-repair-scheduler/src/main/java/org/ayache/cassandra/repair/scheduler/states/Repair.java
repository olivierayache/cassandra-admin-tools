/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.states;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ayache.automaton.api.IStateRetriever;
import org.ayache.automaton.api.OutGoingTransitions;
import org.ayache.automaton.api.State;
import org.ayache.cassandra.repair.scheduler.NodeReparator;
import org.ayache.cassandra.repair.scheduler.RepairTransition;

/**
 *
 * @author Ayache
 */
@OutGoingTransitions(transitionType = RepairTransition.class, transitions = {"END_REPAIR", "REPAIR_FAILED", "CANCEL"})
public class Repair extends State<RepairContext, Void, RepairInner> {

    private final Lock lock = new ReentrantLock();
    private static final long MAX_TIME_TO_WAIT = TimeUnit.NANOSECONDS.convert(8, TimeUnit.HOURS);

    public Repair(boolean[] accessor) {
        super(accessor);
    }

    @Override
    public void registerNextStates(IStateRetriever retriever) {
        addNextState(retriever.state(Success.class)).whenEND_REPAIR().end();
        addNextState(retriever.state(Failure.class)).whenREPAIR_FAILED().whenEND_REPAIR().end();
        addNextState(retriever.state(Cancelled.class)).whenCANCEL().end();
    }

    @Override
    public boolean shouldExecuteAsync() {
        return true;
    }

    @Override
    public Void execute(RepairContext context) {
        lock.lock();
        try {
            Condition condition = lock.newCondition();
            List<NodeReparator> nodeReparators = new LinkedList<>();
            List<String> keyspacesToRepair = Collections.EMPTY_LIST;
            for (String nodeToRepair : context.getNodesToRepair()) {
                try {
                    NodeReparator nodeProbe = context.getNodeProbe(nodeToRepair);
                    if (keyspacesToRepair.isEmpty()) {
                        keyspacesToRepair = nodeProbe.getKeyspaces();
                    }
                    nodeReparators.add(nodeProbe);
                } catch (IOException ex) {
                    Logger.getLogger(Repair.class.getName()).log(Level.SEVERE, null, ex);
                    context.addStatus(NodeReparator.Status.JMX_ERROR).addMessage(ex.getMessage()).activate(RepairTransition.REPAIR_FAILED);
                    keyspacesToRepair.clear();
                }
            }
            
            long timeout = MAX_TIME_TO_WAIT;
            
            for (String keyspace : keyspacesToRepair) {
                List<NodeReparator> reparatorsToWait = new ArrayList<>();
                for (NodeReparator nodeReparator : nodeReparators) {
                    try {
                        long forceRepairAsync = nodeReparator.forceRepairAsync(context, lock, condition, System.out, keyspace, true, context.repairLocalDCOnly, true);
                        if (forceRepairAsync>0){
                            reparatorsToWait.add(nodeReparator);
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(Repair.class.getName()).log(Level.SEVERE, null, ex);
                        context.error(nodeReparator.host, NodeReparator.Status.JMX_UNKWOWN, ex.getMessage()).activate(RepairTransition.REPAIR_FAILED);
                    }
                }
                
                try {
                    timeout = waitForRepairs(context, condition, reparatorsToWait, timeout);
                    if (timeout<0){
                        break;
                    }
                } catch (InterruptedException ex) {
                    for (NodeReparator nodeReparator : nodeReparators) {
                        nodeReparator.cancel();
                    }
                    context.addMessage("Waiting for repair cancelled").activate(RepairTransition.CANCEL);
                    return null;
                } finally {
                    for (NodeReparator nodeReparator : nodeReparators) {
                        nodeReparator.removeListeners();
                    }
                }
                
            }

            context.activate(RepairTransition.END_REPAIR);
        } finally {
            lock.unlock();
        }
        return null;
    }

    private long waitForRepairs(RepairContext context, Condition condition, List<NodeReparator> nodeReparators, long timeout) throws InterruptedException {
        if (nodeReparators.isEmpty()){
            return timeout;
        }
        long remaining = timeout;
        boolean finished = false;
        while (!finished) {
            if ((remaining = condition.awaitNanos(remaining)) <= 0) { // prevents from waiting indefinitly
                for (NodeReparator nodeReparator : nodeReparators) {
                    if (!nodeReparator.finished()) {
                        nodeReparator.repairTimeout(context);
                    }
                }
                return remaining;
            } else {
                boolean endRepair = true;
                for (NodeReparator nodeReparator : nodeReparators) {
                    nodeReparator.checkForErrors(context);
                    endRepair &= nodeReparator.finished();
                }
                if (endRepair) {
                    finished = true;
                }
               
//                if (errorMessage != null) {
//                    repairContext.error(host, NodeReparator.Status.JMX_UNKWOWN, errorMessage).activate(RepairTransition.REPAIR_FAILED);
//                }
//                if (!finished && success) {
//                    repairContext.error(host, NodeReparator.Status.JMX_UNKWOWN, "Unbelievable, it seems that a spurious wake up occurs!!!").activate(RepairTransition.REPAIR_FAILED);
//                }
            }
        }
        return remaining;
    }

}
