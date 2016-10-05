/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.states;

import java.io.IOException;
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
@OutGoingTransitions(transitionType = RepairTransition.class, transitions = {"ACKNOWLEDGED", "IGNORE_FAILURE"})
public class Failure extends State<RepairContext, Object, FailureInner> {

    private static final int RETRY_ON_SESSION_FAILED = 5;
    private int nbRetry = RETRY_ON_SESSION_FAILED;

    public Failure(boolean[] accessor) {
        super(accessor);
    }

    @Override
    public void registerNextStates(IStateRetriever retriever) {
        addNextState(retriever.state(RepairReschedule.class)).whenACKNOWLEDGED().end();
        addNextState(retriever.state(Success.class)).whenIGNORE_FAILURE().end();
    }

    @Override
    public Object execute(RepairContext p) {

        NodeReparator.Status status = p.status;

        //Retrieve nodes in failure
        for (ErrorInfoAggregator aggregator : p.getNodesToRepairInFailure()) {

            switch (aggregator.getStatus()) {
                //Repair will be automatically reschedule later
                case SESSION_FAILED:
                    status = NodeReparator.Status.SESSION_FAILED;
                    if (nbRetry == 0) {
                        p.addNodeInUnknownError(aggregator.getHost());
                    } else {
                        p.addNodeInError(aggregator.getHost());
                    }
                    break;
                //Jmx connection will be automatically reopened
                // Repair won't be reschedule, error should be handled manually
                case JMX_UNKWOWN:
                    p.addNodeInUnknownError(aggregator.getHost());
                    boolean ok = false;
                    while (!ok) {
                        try {
                            Thread.sleep(10000);
                            p.checkJMXConnections();
                            ok = true;
                        } catch (Exception ex) {
                            Logger.getLogger(Failure.class.getName()).log(Level.INFO, "Unable to connect via JMX, will retry in 10 seconds", ex);
                        }
                    }
                    break;
                default:
                    p.addNodeInUnknownError(aggregator.getHost());
            }
        }
        
        try {
            p.save();
        } catch (IOException ex) {
            Logger.getLogger(Failure.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        switch (status) {
            //Node(s) in failure
            //Repair will be automatically reschedule later
            case SESSION_FAILED:
                if (nbRetry == 0) {
                    nbRetry = RETRY_ON_SESSION_FAILED;
                    return null;
                }
                nbRetry--;
                p.activate(RepairTransition.ACKNOWLEDGED);
                break;
            //No node in failure 
            //Jmx connection will be automatically reopened and repair will be automatically reschedule later
            case JMX_ERROR:
                boolean ok = false;
                while (!ok) {
                    try {
                        Thread.sleep(10000);
                        p.checkJMXConnections();
                        ok = true;
                    } catch (Exception ex) {
                        Logger.getLogger(Failure.class.getName()).log(Level.INFO, "Unable to connect via JMX, will retry in 10 seconds", ex.getMessage());
                    }
                }
                p.activate(RepairTransition.ACKNOWLEDGED);
                break;
        }
        return null;
    }

}
