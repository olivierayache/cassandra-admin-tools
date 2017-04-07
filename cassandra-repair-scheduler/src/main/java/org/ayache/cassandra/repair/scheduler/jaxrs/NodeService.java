/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.jaxrs;

import java.io.IOException;
import java.util.ArrayList;
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
public class NodeService implements INodeService {

    private final String clusterName;
    private static final ClusterServiceFactory CLUSTER_SERVICE_FACTORY = ClusterServiceFactory.getInstance();

    public NodeService(String clusterName) {
        this.clusterName = clusterName;
    }

    @Override
    public Collection<NodeDto> describe() {
        try {

            return CLUSTER_SERVICE_FACTORY.getNodes(clusterName).values();
        } catch (IOException ex) {
            Logger.getLogger(NodeService.class.getName()).log(Level.SEVERE, null, ex);
            throw new ServerErrorException(Response.serverError().build(), ex);
        }
    }

    @Override
    public NodeInfoDto getInfos(String name) {
        try {
            return JMXBeanModelFactory.getModel(NodeInfoDto.class, CLUSTER_SERVICE_FACTORY.getNodeConnector(clusterName, name).getSsProxy(), name);
        } catch (IOException ex) {
            Logger.getLogger(NodeService.class.getName()).log(Level.SEVERE, null, ex);
            throw new ServerErrorException(Response.serverError().build(), ex);
        }
    }
    
    @Override
    public Collection<NodeInfoDto> getInfos() {
        Collection<NodeInfoDto> infoDtos = new ArrayList<>();
        try {
            Collection<NodeDto> nodes = CLUSTER_SERVICE_FACTORY.getNodes(clusterName).values();
            for (NodeDto node : nodes) {
                if (node.isAlive()){
                    infoDtos.add(JMXBeanModelFactory.getModel(NodeInfoDto.class, CLUSTER_SERVICE_FACTORY.getNodeConnector(clusterName, node.getName()).getSsProxy(), node.getName()));
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(NodeService.class.getName()).log(Level.SEVERE, null, ex);
            throw new ServerErrorException(Response.serverError().build(), ex);
        }
        return infoDtos;
    }
    

    @Override
    public void configure(String name, String param, String value) {
        try {
            JMXBeanModelFactory.editBean(CLUSTER_SERVICE_FACTORY.getNodeConnector(clusterName, name).getSsProxy(), param, value);
        } catch (IOException ex) {
            Logger.getLogger(NodeService.class.getName()).log(Level.SEVERE, null, ex);
            throw new ServerErrorException(Response.serverError().build(), ex);
        }
    }
    
    @Override
    public void configure(String param, String value) {
        try {
            Collection<NodeDto> nodes = CLUSTER_SERVICE_FACTORY.getNodes(clusterName).values();
            for (NodeDto node : nodes) {
                if (node.isAlive()){
                    JMXBeanModelFactory.editBean(CLUSTER_SERVICE_FACTORY.getNodeConnector(clusterName, node.getName()).getSsProxy(), param, value);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(NodeService.class.getName()).log(Level.SEVERE, null, ex);
            throw new ServerErrorException(Response.serverError().build(), ex);
        }
    }

}
