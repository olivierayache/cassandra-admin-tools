/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.states;

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
@OutGoingTransitions(transitionType = RepairTransition.class, transitions = {"NODES_FOUND", "REPAIR_FAILED", "CANCEL"})
public class Init extends State<RepairContext, Void, InitInner> {

    public Init(boolean[] accessor) {
        super(accessor);
    }

    @Override
    public void registerNextStates(IStateRetriever retriever) {
        addNextState(retriever.state(Repair.class)).whenNODES_FOUND().end();
        addNextState(retriever.state(Failure.class)).whenREPAIR_FAILED().end();
        addNextState(retriever.state(Cancelled.class)).whenCANCEL().end();
    }

    @Override
    public Void execute(RepairContext context) {
        try {
            if (!context.initNodesToRepair().isEmpty()) {
                context.activate(RepairTransition.NODES_FOUND);
            }else{
                context.addMessage("No node found").activate(RepairTransition.REPAIR_FAILED);
            }
        } catch (Exception ex) {
            Logger.getLogger(Init.class.getName()).log(Level.SEVERE, null, ex);
            context.addStatus(NodeReparator.Status.JMX_ERROR).activate(RepairTransition.REPAIR_FAILED);
        }
        return null;
    }

}
