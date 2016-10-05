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
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;
import org.ayache.cassandra.admin.api.INodeService;
import org.ayache.cassandra.admin.api.dto.NodeDto;
import org.ayache.cassandra.admin.api.dto.NodeInfoDto;
import org.ayache.cassandra.repair.scheduler.model.JMXBeanModelFactory;

/**
 *
 * @author Ayache
 */
public class NodeService implements INodeService{

    private final String clusterName;
    private static final ClusterServiceFactory CLUSTER_SERVICE_FACTORY = ClusterServiceFactory.getInstance();

    public NodeService(String clusterName) {
        this.clusterName = clusterName;
    }
    
    @Override
    public Collection<NodeDto> describe() {
        try {
            
            return CLUSTER_SERVICE_FACTORY.getNodes(clusterName);
        } catch (IOException ex) {
            Logger.getLogger(NodeService.class.getName()).log(Level.SEVERE, null, ex);
            throw new ServerErrorException(Response.serverError().build(), ex);
        }
    }

    @Override
    public NodeInfoDto getInfo(String name) {
        try {
            return JMXBeanModelFactory.getModel(NodeInfoDto.class, CLUSTER_SERVICE_FACTORY.getNodeConnector(clusterName, name).getSsProxy(), name);
        } catch (IOException ex) {
            Logger.getLogger(NodeService.class.getName()).log(Level.SEVERE, null, ex);
            throw new ServerErrorException(Response.serverError().build(), ex);
        }
    }
    
}
