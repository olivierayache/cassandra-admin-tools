/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.jaxrs;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ayache.cassandra.admin.api.IClusterService;
import org.ayache.cassandra.admin.api.INodeService;
import org.ayache.cassandra.admin.api.IRepairService;

/**
 *
 * @author Ayache
 */
public class ClusterService implements IClusterService {

    @Override
    public Collection<String> all() {
        return ClusterServiceFactory.getInstance().clusters.keySet();
    }

    @Override
    public String addCluster(String address, int jmxPort) {
        try {
            return ClusterServiceFactory.getInstance().addCluster(address, jmxPort);
        } catch (IOException ex) {
            Logger.getLogger(ClusterService.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    @Override
    public IRepairService getRepairService(String clusterName) {
        return new RepairService(clusterName);
    }

    @Override
    public INodeService getNodeService(String clusterName) {
        return new NodeService(clusterName);
    }

}
