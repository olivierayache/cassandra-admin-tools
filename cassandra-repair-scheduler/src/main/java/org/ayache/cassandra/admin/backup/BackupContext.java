/*
 * Copyright (C) 2016 Ayache.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.ayache.cassandra.admin.backup;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ayache.cassandra.repair.scheduler.NodeConnector;
import org.ayache.cassandra.repair.scheduler.jaxrs.ClusterServiceFactory;

/**
 *
 * @author Ayache
 */
public class BackupContext {

    private static final ClusterServiceFactory CLUSTER_SERVICE_FACTORY = ClusterServiceFactory.getInstance();
    private transient final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private final String clusterName;
    private transient ScheduledFuture<?> scheduleAtFixedRate;
    private Status status;

    public static enum Status {
        ACTIVE, INACTIVE, ERROR
    }

    private final class BackupRunnable implements Runnable {

        @Override
        public void run() {
            try {
                for (String host : CLUSTER_SERVICE_FACTORY.getNodes(clusterName).keySet()) {
                    try {
                        ((NodeConnector)CLUSTER_SERVICE_FACTORY.getNodeConnector(clusterName, host)).getSsProxy().takeSnapshot(host + "a");
                        ((NodeConnector)CLUSTER_SERVICE_FACTORY.getNodeConnector(clusterName, host)).getSsProxy().clearSnapshot(host + "b");
                        System.out.println("Last snapshot ok for node " + host);
                    } catch (IOException e) {
                        if (e.getMessage().contains("already exists")) {
                            ((NodeConnector)CLUSTER_SERVICE_FACTORY.getNodeConnector(clusterName, host)).getSsProxy().takeSnapshot(host + "b");
                            ((NodeConnector)CLUSTER_SERVICE_FACTORY.getNodeConnector(clusterName, host)).getSsProxy().clearSnapshot(host + "a");
                            System.out.println("Last snapshot ok for node " + host);
                        } else {
                            throw e;
                        }
                    }
                }
                status = Status.ACTIVE;
            } catch (IOException ex) {
                Logger.getLogger(BackupContext.class.getName()).log(Level.SEVERE, null, ex);
                status = Status.ERROR;
            }
        }

    }

    /**
     * Constructor present for serialization
     */
    public BackupContext() {
        this.clusterName = null;
    }

    public BackupContext(String clusterName) {
        this.clusterName = clusterName;
    }

    public void scheduleBackup(String period, long when) {
        if (scheduleAtFixedRate == null || scheduleAtFixedRate.isCancelled() || scheduleAtFixedRate.isDone()) {
            long periodInSeconds = "WEEKS".equals(period) ? TimeUnit.DAYS.toSeconds(7) : TimeUnit.valueOf(period).toSeconds(1);
            scheduleAtFixedRate = scheduledExecutorService.scheduleAtFixedRate(new BackupRunnable(), when == -1 ? 0 : when, periodInSeconds, TimeUnit.SECONDS);
        }
    }

    public void cancel() {
        if (scheduleAtFixedRate != null) {
            try {
                for (String host : CLUSTER_SERVICE_FACTORY.getNodes(clusterName).keySet()) {
                    ((NodeConnector)CLUSTER_SERVICE_FACTORY.getNodeConnector(clusterName, host)).getSsProxy().clearSnapshot(host + "a");
                    ((NodeConnector)CLUSTER_SERVICE_FACTORY.getNodeConnector(clusterName, host)).getSsProxy().clearSnapshot(host + "b");
                }
            } catch (IOException ex) {
                Logger.getLogger(BackupContext.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (scheduleAtFixedRate.cancel(true)) {
                status = Status.INACTIVE;
            }
        }
    }

    public Status getStatus() {
        return status;
    }

}
