/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.jaxrs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ayache.cassandra.admin.api.IRepairService;
import org.ayache.cassandra.admin.api.dto.NodeDto;
import org.ayache.cassandra.admin.api.dto.RepairConfigDto;
import org.ayache.cassandra.repair.scheduler.RepairTransition;
import org.ayache.cassandra.repair.scheduler.states.RepairContext;

/**
 *
 * @author Ayache
 */
public class RepairService implements IRepairService {

    private final String clusterName;

    RepairService(String clusterName) {
        this.clusterName = clusterName;
    }

    @Override
    public void handleErrorNotification(boolean restartSession) {
        final RepairContext context = ClusterServiceFactory.getInstance().getRepairContext(clusterName);
        if (context.getNodesToRepairInUnknown().isEmpty() || !restartSession) {
            context.activate(RepairTransition.IGNORE_FAILURE);
        } else {
            context.activate(RepairTransition.ACKNOWLEDGED);
        }
    }

    @Override
    public void purgeErrorForNode(String id) {
        ClusterServiceFactory.getInstance().getRepairContext(clusterName).removeNodeInError(id);
    }

    @Override
    public void activateRepair() {
        ClusterServiceFactory.getInstance().getRepairContext(clusterName).activate(RepairTransition.ACTIVATE);
    }

    @Override
    public void cancelRepair() {
        ClusterServiceFactory.getInstance().getRepairContext(clusterName).cancelRepairSessions();
    }

    @Override
    public String status() {
        return ClusterServiceFactory.getInstance().getRepairContext(clusterName).getState();
    }

    @Override
    public Collection<NodeDto> describe() {
        try {

            final RepairContext context = ClusterServiceFactory.getInstance().getRepairContext(clusterName);
            Map<String, NodeDto> nodes = context.getNodes();

            for (String host : context.getNodesToRepairInUnknown()) {
                nodes.get(host).setRepairInError(true);
            }
            for (String host : context.getNodesToRepair()) {
                nodes.get(host).setRepairInProgress(true);
            }
            return nodes.values();
        } catch (IOException ex) {
            Logger.getLogger(RepairService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new ArrayList<>();
    }

    @Override
    public Collection<String> history() {
        return ClusterServiceFactory.getInstance().getRepairContext(clusterName).getMessages();
    }

    @Override
    public RepairConfigDto getConfigurations() {
        return ClusterServiceFactory.getInstance().getRepairContext(clusterName).getConfigurations();
    }

    @Override
    public void editConfigurations(RepairConfigDto config, boolean cancelCurrent) {
        RepairContext repairContext = ClusterServiceFactory.getInstance().getRepairContext(clusterName);
        repairContext.editConfigurations(config);
        if (!cancelCurrent) {
            repairContext.cancelRepairSessions();
        } else {
            repairContext.cancelWaiting();
        }

    }
}
