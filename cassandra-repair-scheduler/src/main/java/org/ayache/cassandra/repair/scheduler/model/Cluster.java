/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.model;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import org.ayache.cassandra.repair.scheduler.NodeConnector;
import org.ayache.cassandra.repair.scheduler.states.RepairContext;

/**
 *
 * @author Ayache
 */
@XmlRootElement()
@XmlAccessorType(XmlAccessType.FIELD)
public class Cluster {

    private final String name;
    private final int port;
    private final RepairContext repairContext;
    private final NodeConnector nodeConnector;
    private transient final Map<String, NodeConnector> map = new ConcurrentHashMap<>();

    /**
     * Constructor present for serialization
     */
    private Cluster() {
        this.name = null;
        this.port = 0;
        this.repairContext = null;
        this.nodeConnector = null;
    }

    /**
     * Construct a cluster
     * @param name
     * @param port
     * @param repairContext
     * @param nodeConnector 
     */
    public Cluster(String name, int port, RepairContext repairContext, NodeConnector nodeConnector) {
        this.name = name;
        this.port = port;
        this.repairContext = repairContext;
        this.nodeConnector = nodeConnector;
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
    public NodeConnector getNodeConnector() {
        return nodeConnector;
    }

    /**
     * 
     * @param hostName
     * @return
     * @throws IOException 
     */
    public NodeConnector getNodeConnector(String hostName) throws IOException {
        NodeConnector get = map.get(hostName);
        NodeConnector connector = get == null ? new NodeConnector(hostName, port) : get;
        if (get == null) {
            map.putIfAbsent(hostName, connector);
        }
        return connector;
    }

}
