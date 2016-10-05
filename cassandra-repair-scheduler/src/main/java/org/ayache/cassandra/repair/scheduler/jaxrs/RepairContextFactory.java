/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.jaxrs;

import org.ayache.cassandra.repair.scheduler.states.RepairContext;

/**
 *
 * @author Ayache
 */
public class RepairContextFactory {
    
    private volatile RepairContext context;
    
    private RepairContextFactory() {
    }
    
    public RepairContext getContext(){
        return context;
    }
    
    public void pushContext(RepairContext context){
        this.context = context;
    }
    
    public static RepairContextFactory getInstance() {
        return RepairContextHolder.INSTANCE;
    }
    
    private static class RepairContextHolder {

        private static final RepairContextFactory INSTANCE = new RepairContextFactory();
    }
}
