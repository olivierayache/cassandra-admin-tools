/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.jaxrs;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.ayache.cassandra.admin.api.dto.NodeDto;
import org.ayache.cassandra.repair.scheduler.NodeConnector;
import org.ayache.cassandra.repair.scheduler.model.Cluster;
import org.ayache.cassandra.repair.scheduler.states.RepairContext;

/**
 *
 * @author Ayache
 */
public class ClusterServiceFactory {

    final Map<String, Cluster> clusters = new ConcurrentHashMap<>();

    private ClusterServiceFactory() {
    }

    public static ClusterServiceFactory getInstance() {
        return ClusterServiceFactoryHolder.INSTANCE;
    }

    private static class ClusterServiceFactoryHolder {

        private static final ClusterServiceFactory INSTANCE = new ClusterServiceFactory();
    }

    public String addCluster(String address, int port) throws IOException {
        NodeConnector nodeConnector = new NodeConnector(address, port);
        String clusterName = nodeConnector.getSsProxy().getClusterName();
        if (clusters.containsKey(clusterName)) {
            nodeConnector.close();
            throw new IllegalArgumentException("Cluster already exists");
        }
        RepairContext repairContext = new RepairContext(clusterName, port, 21, 2);
        repairContext.addNodeConnector(nodeConnector);
        final Cluster cluster = new Cluster(clusterName, port, repairContext, nodeConnector);
        clusters.putIfAbsent(clusterName, cluster);
        saveCluster(cluster);
        return clusterName;
    }

    public void loadCluster(String json) throws IOException {
        Cluster cluster = new Gson().fromJson(json, Cluster.class);
        NodeConnector nodeConnector = cluster.getNodeConnector();
        nodeConnector.connect();
        cluster.getRepairContext().addNodeConnector(nodeConnector);
        clusters.putIfAbsent(cluster.getName(), cluster);
    }

    public void saveCluster(Cluster cluster) throws IOException {
        String toJson = new Gson().toJson(cluster);
        new File("data").mkdir();
        try (FileOutputStream fileOutputStream = new FileOutputStream("data/"+cluster.getName())) {
            fileOutputStream.write(toJson.getBytes());
        }
    }
    
    public void saveCluster(String clusteName) throws IOException {
        saveCluster(clusters.get(clusteName));
    }

    public RepairContext getRepairContext(String clusterName) {
        return clusters.get(clusterName).getRepairContext();
    }

    public Collection<NodeDto> getNodes(String clusterName) throws IOException {
        return clusters.get(clusterName).getNodeConnector().getNodes().values();
    }

    public NodeConnector getNodeConnector(String clusterName, String hostName) throws IOException {
        return clusters.get(clusterName).getNodeConnector(hostName);
    }

}
