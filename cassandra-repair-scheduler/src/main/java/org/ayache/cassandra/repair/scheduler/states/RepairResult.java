/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.states;

import java.util.List;

/**
 *
 * @author Ayache
 */
@Deprecated
public class RepairResult {
    
    final List<String> nodesToRepair;

    /**
     * 
     * @param nodesToRepair 
     */
    public RepairResult(List<String> nodesToRepair) {
        this.nodesToRepair = nodesToRepair;
    }
    
    /**
     * 
     * @param params 
     */
    public void fillParams(RepairContext params){
       // params.getNodesToRepair().addAll(nodesToRepair);
    }
    
}
