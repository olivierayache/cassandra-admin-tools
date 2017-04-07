/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.model;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import org.ayache.cassandra.admin.api.dto.NodeDto;
import org.ayache.cassandra.admin.backup.BackupContext;
import org.ayache.cassandra.repair.scheduler.NodeConnector;
import org.ayache.cassandra.repair.scheduler.jaxrs.ClusterServiceFactory;
import org.ayache.cassandra.repair.scheduler.states.RepairContext;

/**
 *
 * @author Ayache
 */
@XmlRootElement()
@XmlAccessorType(XmlAccessType.FIELD)
public class Cluster implements IDeadNodeListener, INodeConnectorRetriever{

    private final String name;
    private final int port;
    private final RepairContext repairContext;
    private final BackupContext backupContext;
    private NodeConnector nodeConnector;
    private transient final Map<String, NodeConnector> map = new ConcurrentHashMap<>();

    /**
     * Constructor present for serialization
     */
    private Cluster() {
        this.name = null;
        this.port = 0;
        this.repairContext = null;
        this.backupContext = null;
        this.nodeConnector = null;
    }

    /**
     * Construct a cluster
     * @param name
     * @param port
     * @param nodeConnector 
     */
    public Cluster(String name, int port, NodeConnector nodeConnector) {
        this.name = name;
        this.port = port;
        this.repairContext = new RepairContext(name, port, 21, 2);
        this.backupContext = new BackupContext(name);
        this.nodeConnector = nodeConnector;
        addNodeConnector(nodeConnector);
    }

    private void addNodeConnector(NodeConnector nodeConnector) {
        map.putIfAbsent(nodeConnector.getHost(), nodeConnector);
        nodeConnector.setDeadNodeListener(this);
    }

    /**
     * 
     * @return 
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * @return 
     */
    public RepairContext getRepairContext() {
        return repairContext;
    }

    /**
     * 
     * @return 
     */
    public BackupContext getBackupContext() {
        return backupContext;
    }
    
    /**
     * 
     * @return 
     */
    @Override
    public NodeConnector getNodeConnector() {
        nodeConnector = nodeConnector.isAlive() ? nodeConnector : map.values().stream().filter((NodeConnector t) -> t.isAlive()).findAny().orElse(nodeConnector);
        return nodeConnector;
    }
    
    public Map<String, NodeDto> getNodes() throws IOException {
        return getNodeConnector().getNodes();
    }

    /**
     * 
     * @param hostName
     * @return
     * @throws IOException 
     */
    @Override
    public NodeConnector getNodeConnector(String hostName) throws IOException {
        NodeConnector get = map.get(hostName);
        NodeConnector connector = get == null ? new NodeConnector(hostName, port) : get;
        if (get == null) {
            addNodeConnector(connector);
        }
        return connector;
    }

    @Override
    public Iterable<NodeConnector> iterable() {
        return map.values();
    }

    @Override
    public void onNodeRemoved(String host) {
        map.remove(host);
        try {
            ClusterServiceFactory.getInstance().saveCluster(this);
        } catch (IOException ex) {
            Logger.getLogger(Cluster.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void init() {
        repairContext.init(this);
        if (!nodeConnector.isAlive()) {
            try {
                nodeConnector.connect();
                addNodeConnector(nodeConnector);
            } catch (IOException ex) {
                Logger.getLogger(Cluster.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
