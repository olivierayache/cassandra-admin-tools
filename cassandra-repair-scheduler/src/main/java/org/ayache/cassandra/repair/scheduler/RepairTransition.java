/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler;

import org.ayache.automaton.api.Transition;

/**
 *
 * @author Ayache
 */
@Transition
public enum RepairTransition {
   NODES_FOUND, END_REPAIR, REPAIR_FAILED, WAKE_UP, ACKNOWLEDGED, IGNORE_FAILURE, CONTEXT_CLEARED, CANCEL, ACTIVATE,fefef; 
}
