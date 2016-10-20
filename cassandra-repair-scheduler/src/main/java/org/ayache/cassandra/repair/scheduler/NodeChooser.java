/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.cassandra.locator.EndpointSnitchInfoMBean;
import org.apache.cassandra.service.StorageServiceMBean;

/**
 *
 * @author Ayache
 */
public class NodeChooser {

    private final String lastRepairedNode;
    private final boolean supportSimultaneousRepair;
    private final String dc;
    private final EndpointSnitchInfoMBean esMBean;
//    private final int replicates;

    private final StorageServiceMBean serviceMBean;

    /**
     * Construct NodeChooser
     *
     * @param serviceMBean
     * @param esMBean
     * @param dc
     * @param lastRepairedNode
     * @throws java.io.IOException
     */
    public NodeChooser(StorageServiceMBean serviceMBean, EndpointSnitchInfoMBean esMBean, String dc, String lastRepairedNode) throws IOException {
        this.serviceMBean = serviceMBean;
        this.esMBean = esMBean;
        this.dc = dc;
        this.lastRepairedNode = lastRepairedNode;

        List<String> keyspaces = serviceMBean.getKeyspaces();
        Collection<String> nodesFromDC = getNodesFromDC();
        Collection<Float> values = new LinkedList<>();
        float Rf = 0;
        for (String keyspace : keyspaces) {
            values.clear();
            Map<InetAddress, Float> effectiveOwnership = serviceMBean.effectiveOwnership(keyspace);
            for (Map.Entry<InetAddress, Float> entry : effectiveOwnership.entrySet()) {
                for (String node : nodesFromDC) {
                    if (entry.getKey().getHostAddress().equals(node)) {
                        values.add(entry.getValue());
                    }
                }
            }
            float localRf = 0;
            for (Float value : values) {
                localRf += value;
            }
            Rf = (localRf > Rf) ? localRf : Rf;
        }
//        replicates = (int) Rf;

        supportSimultaneousRepair = nodesFromDC.size() >= 3 * Rf;
        Logger.getLogger(NodeChooser.class.getName()).info(serviceMBean.getLoadMap().toString());
        Logger.getLogger(NodeChooser.class.getName()).info("Replicas :" + Rf + " Simultaneous Repair Supported: " + supportSimultaneousRepair);
    }

    private Collection<String> getNodesFromDC() throws IOException {
        Collection<String> result = new LinkedList<>();
        Map<String, String> tokenToEndpointMap = serviceMBean.getTokenToEndpointMap();
        for (String node : tokenToEndpointMap.values()) {
            if (dc.equals(esMBean.getDatacenter(node))) {
                result.add(node);
            }
        }
        return result;
    }

    /**
     * Returns the list of nodes to repair. An empty list is returned if not
     * other node is available for repair
     *
     * @return the list of nodes to repair
     */
    public Collection<String> getNextNodeToRepair() throws IOException {
        boolean found = false;
        Logger.getLogger(NodeChooser.class.getName()).info("Last Repaired Node :" + lastRepairedNode);
        Set<String> result = new HashSet<>();
        Collection<String> nodesFromDC = getNodesFromDC();
        for (String node : nodesFromDC) {
            if (found) {
                if (!serviceMBean.getUnreachableNodes().contains(node)) {
                    result.add(node);
                    break;
                } else {
                    Logger.getLogger(NodeChooser.class.getName()).info(result.toString());
                    return result;
                }
            }
            if (node.equals(lastRepairedNode)) {
                if (!serviceMBean.getUnreachableNodes().contains(node)) {
                    found = true;
                } else {
                    Logger.getLogger(NodeChooser.class.getName()).info(result.toString());
                    return result;
                }
            }
        }

        if (found && result.isEmpty()) {
            for (String node : nodesFromDC) {
                if (!serviceMBean.getUnreachableNodes().contains(node)) {
                    result.add(node);
                    break;
                } else {
                    Logger.getLogger(NodeChooser.class.getName()).info(result.toString());
                    return result;
                }
            }
        }

        //First start lastRepairNode not initialized
        if (!found) {
            String next = nodesFromDC.iterator().next();
            if (!serviceMBean.getUnreachableNodes().contains(next)) {
                result.add(next);
            } else {
                Logger.getLogger(NodeChooser.class.getName()).info(result.toString());
                return result;
            }
        }

        if (supportSimultaneousRepair && !result.isEmpty()) {
            //TODO: optimiser de facon a lancer simultanement les noeuds diametralement opposés
            //int remainingToNext = 2*replicates;
            int remainingToNext = nodesFromDC.size() / 2;
            String firstNodeToRepair = result.iterator().next();
            int index = 0;
            for (String node : nodesFromDC) {
                if (node.equals(firstNodeToRepair)) {
                    break;
                }
                index++;
            }
            int total = nodesFromDC.size();
            String[] nodeArray = nodesFromDC.toArray(new String[0]);
            if (index + remainingToNext < total) {
                result.add(nodeArray[index + remainingToNext]);
            } else {
                result.add(nodeArray[index + remainingToNext - total]);
            }
        }
        Logger.getLogger(NodeChooser.class.getName()).info(result.toString());
        return result;
    }

}
