/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler;

import org.ayache.automaton.api.AbstractAutomaton;
import org.ayache.automaton.api.AutomatonTypes;
import org.ayache.cassandra.repair.scheduler.states.Cancelled;
import org.ayache.cassandra.repair.scheduler.states.Failure;
import org.ayache.cassandra.repair.scheduler.states.Init;
import org.ayache.cassandra.repair.scheduler.states.Repair;
import org.ayache.cassandra.repair.scheduler.states.RepairContext;
import org.ayache.cassandra.repair.scheduler.states.RepairReschedule;
import org.ayache.cassandra.repair.scheduler.states.Success;

/**
 *
 * @author Ayache
 */
@AutomatonTypes(transitionsType = RepairTransition.class, stateTypes = {Init.class, Repair.class, RepairReschedule.class, Success.class, Failure.class, Cancelled.class})
public class RepairAutomaton extends AbstractAutomaton<RepairContext,RepairTransition>{
    
}
