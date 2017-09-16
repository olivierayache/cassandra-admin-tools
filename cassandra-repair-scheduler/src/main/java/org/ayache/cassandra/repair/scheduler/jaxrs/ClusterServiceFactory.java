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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.ayache.cassandra.admin.api.dto.NodeDto;
import org.ayache.cassandra.admin.backup.BackupContext;
import org.ayache.cassandra.admin.INodeConnector;
import org.ayache.cassandra.repair.scheduler.NodeConnectorProxy;
import org.ayache.cassandra.repair.scheduler.model.Cluster;
import org.ayache.cassandra.repair.scheduler.states.RepairContext;

/**
 *
 * @author Ayache
 */
public class ClusterServiceFactory {

    final Map<String, Cluster> clusters = new ConcurrentHashMap<>();
    private static final Gson GSON = new Gson();

    private ClusterServiceFactory() {
    }

    public static ClusterServiceFactory getInstance() {
        return ClusterServiceFactoryHolder.INSTANCE;
    }

    private static class ClusterServiceFactoryHolder {

        private static final ClusterServiceFactory INSTANCE = new ClusterServiceFactory();
    }

    public String addCluster(String address, int port) throws IOException {
        NodeConnectorProxy nodeConnector = new NodeConnectorProxy(address, port);
        String clusterName = nodeConnector.getClusterName();
        if (clusters.containsKey(clusterName)) {
            throw new IllegalArgumentException("Cluster already exists");
        }
        final Cluster cluster = new Cluster(clusterName, port, nodeConnector);
        cluster.init();
        clusters.putIfAbsent(clusterName, cluster);
        saveCluster(cluster);
        return clusterName;
    }

    public void loadCluster(String json) {
        Cluster cluster = GSON.fromJson(json, Cluster.class);
        cluster.init();
        clusters.putIfAbsent(cluster.getName(), cluster);
    }

    public synchronized void saveCluster(Cluster cluster) throws IOException {
        String toJson = GSON.toJson(cluster);
        File file = new File("data");
        if (!file.exists()){
            file.mkdir();
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(new File(file, cluster.getName()))) {
            fileOutputStream.write(toJson.getBytes());
        }
    }
    
    public void saveCluster(String clusteName) throws IOException {
        saveCluster(clusters.get(clusteName));
    }

    public RepairContext getRepairContext(String clusterName) {
        return clusters.get(clusterName).getRepairContext();
    }
    
    public BackupContext getBackupContext(String clusterName) {
        return clusters.get(clusterName).getBackupContext();
    }

    public Map<String, NodeDto> getNodes(String clusterName) throws IOException {
        return clusters.get(clusterName).getNodeConnector().getNodes();
    }

    public INodeConnector getNodeConnector(String clusterName, String hostName) throws IOException {
        return clusters.get(clusterName).getNodeConnector(hostName);
    }

}
