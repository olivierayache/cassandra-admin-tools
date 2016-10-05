/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.states;

import org.ayache.cassandra.repair.scheduler.NodeReparator;

/**
 *
 * @author Ayache
 */
public interface InfoAggregator {
    
    InfoAggregator addStatus(NodeReparator.Status status);
    
    InfoAggregator addMessage(String message);
    
    InfoAggregator addNodeInError(String node);
}
