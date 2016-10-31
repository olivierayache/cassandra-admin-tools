/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.jaxrs;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ayache.cassandra.admin.api.IRepairService;
import org.ayache.cassandra.admin.api.dto.NodeDto;
import org.ayache.cassandra.admin.api.dto.RepairConfigDto;
import org.ayache.cassandra.repair.scheduler.RepairTransition;
import org.ayache.cassandra.repair.scheduler.states.Failure;
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
    public void restartSessionForNode(String id, boolean checkForRestart) {
        RepairContext repairContext = ClusterServiceFactory.getInstance().getRepairContext(clusterName);
        if (repairContext.addNodeToRestart(id) && checkForRestart) {
            repairContext.activate(RepairTransition.ACKNOWLEDGED);
        }
    }

    @Override
    public void purgeErrorForNode(String id, boolean checkForRestart) {
        RepairContext repairContext = ClusterServiceFactory.getInstance().getRepairContext(clusterName);
        repairContext.removeNodeInError(id);
        if (checkForRestart && repairContext.getNodesToRepairInUnknown().isEmpty()){
            repairContext.activate(RepairTransition.IGNORE_FAILURE);
        } else if (checkForRestart && repairContext.getNodesToRepairInUnknown().isEmpty()){
            repairContext.activate(RepairTransition.ACKNOWLEDGED);
        }
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
        return ClusterServiceFactory.getInstance().getRepairContext(clusterName).getState().toString();
    }

    @Override
    public Collection<NodeDto> describe() {
        try {

            RepairContext context = ClusterServiceFactory.getInstance().getRepairContext(clusterName);
            Map<String, NodeDto> nodes = ClusterServiceFactory.getInstance().getNodes(clusterName);

            for (String host : context.getNodesToRepairInUnknown()) {
                if (context.getState() instanceof Failure){
                    nodes.get(host).setRepairInError(true);
                }
            }
            for (String host : context.getNodesToRepair()) {
                nodes.get(host).setRepairInProgress(true);
            }
            return nodes.values();
        } catch (IOException ex) {
            Logger.getLogger(RepairService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Collections.EMPTY_LIST;
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
        }

    }
}
