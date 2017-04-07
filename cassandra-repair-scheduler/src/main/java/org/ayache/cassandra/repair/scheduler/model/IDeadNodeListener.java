/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.model;

/**
 *
 * @author Ayache
 */
public interface IDeadNodeListener {
    
    void onNodeRemoved(String host);
}
