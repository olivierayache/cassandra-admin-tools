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
import org.ayache.cassandra.repair.scheduler.RepairTransition;

/**
 *
 * @author Ayache
 */
@OutGoingTransitions(transitionType = RepairTransition.class, transitions = "CONTEXT_CLEARED")
public class Success extends State<RepairContext, Object, SuccessInner>{

    public Success(boolean[] accessor) {
        super(accessor);
    }

    @Override
    public void registerNextStates(IStateRetriever retriever) {
        addNextState(retriever.state(RepairReschedule.class)).whenCONTEXT_CLEARED().end();
    }

    @Override
    public Object execute(RepairContext p) {
        p.clear();
        try {
            p.save();
        } catch (Exception ex) {
            Logger.getLogger(Success.class.getName()).log(Level.SEVERE, null, ex);
        }
        p.activate(RepairTransition.CONTEXT_CLEARED);
        return null;
    }
    
}
