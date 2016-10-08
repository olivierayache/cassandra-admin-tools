/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.states;

import java.util.List;
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
    public Void execute(RepairContext context) {
        if (context.cancel) {
            context.addMessage("Repair aborted").activate(RepairTransition.CANCEL);
            return null;
        }
        for (String nodeToRepair : context.getNodesToRepair()) {
            try {
                NodeReparator nodeProbe = context.getNodeProbe(nodeToRepair);
                List<String> keyspaces = nodeProbe.getKeyspaces();
                for (String keyspace : keyspaces) {
                    nodeProbe.forceRepairAsync(context, System.out, keyspace, true, context.repairLocalDCOnly, true);
                }
            } catch (Exception ex) {
                Logger.getLogger(Repair.class.getName()).log(Level.SEVERE, null, ex);
                context.error(nodeToRepair, NodeReparator.Status.JMX_UNKWOWN, ex.getMessage()).activate(RepairTransition.REPAIR_FAILED);
            }
        }
        if (!context.getNodesToRepairInUnknown().isEmpty()){
            context.activate(RepairTransition.REPAIR_FAILED);
        }
        context.activate(RepairTransition.END_REPAIR);
        return null;
    }

}
