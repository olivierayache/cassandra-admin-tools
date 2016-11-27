/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.states;

import org.ayache.automaton.api.IStateRetriever;
import org.ayache.automaton.api.OutGoingTransitions;
import org.ayache.automaton.api.State;
import org.ayache.cassandra.repair.scheduler.RepairTransition;

/**
 * 
 * @author Ayache
 */
@OutGoingTransitions(transitionType = RepairTransition.class, transitions = {"ACTIVATE"}, init = true)
public class Cancelled extends State<RepairContext, Void, CancelledInner> {

    public Cancelled(boolean[] accessor) {
        super(accessor);
    }

    @Override
    public void registerNextStates(IStateRetriever retriever) {
        addNextState(retriever.state(RepairReschedule.class)).whenACTIVATE().end();
    }

    @Override
    public Void execute(RepairContext p) {       
        p.cancelRepairSessions();        
//        if (p.checkAndApplyChanges()){
//            p.activate(RepairTransition.ACTIVATE);
//        }
        return null;
    }
    
}
