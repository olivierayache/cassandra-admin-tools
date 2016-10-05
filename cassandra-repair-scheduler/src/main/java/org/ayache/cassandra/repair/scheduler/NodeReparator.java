/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import org.apache.cassandra.service.StorageServiceMBean;
import org.apache.cassandra.utils.SimpleCondition;
import org.ayache.cassandra.repair.scheduler.states.RepairContext;

/**
 *
 * @author Ayache
 */
public class NodeReparator {

    public static enum Status {
        STARTED, SESSION_SUCCESS, SESSION_FAILED, FINISHED, JMX_ERROR, JMX_UNKWOWN
    }

    private final String host;
    private final JMXConnector jmxc;
    public final StorageServiceMBean ssProxy;

    /**
     * Construct NodeReparator for a specific node
     *
     * @param host
     * @param jmxc
     * @param ssProxy
     */
    public NodeReparator(String host, JMXConnector jmxc, StorageServiceMBean ssProxy) {
        this.host = host;
        this.jmxc = jmxc;
        this.ssProxy = ssProxy;
    }

    public void forceRepairAsync(final RepairContext context, final PrintStream out, final String tableName, boolean isSequential, boolean isLocal, boolean primaryRange, String... columnFamilies) throws IOException {
        RepairRunner runner = new RepairRunner(context, out, tableName, columnFamilies);
        try {
            jmxc.addConnectionNotificationListener(runner, null, null);
            ssProxy.addNotificationListener(runner, null, null);
            //TODO: Make it async without wait
            if (!runner.repairAndWait(ssProxy, isSequential, isLocal, primaryRange)) {
                //failed = true;
            }
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                ssProxy.removeNotificationListener(runner);
                jmxc.removeConnectionNotificationListener(runner);
            } catch (Throwable ignored) {
                Logger.getLogger(NodeReparator.class.getName()).log(Level.SEVERE, null, ignored);
            }
        }
    }

    public List<String> getKeyspaces() throws IOException {
        return ssProxy.getKeyspaces();
    }

    class RepairRunner implements NotificationListener {

        private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        private final Condition condition = new SimpleCondition();
        private final PrintStream out;
        private final String keyspace;
        private final String[] columnFamilies;
        private final RepairContext repairContext;
        private int cmd;
        private volatile boolean success = true;
//        private volatile boolean cancelled = true;
        private volatile Exception error = null;

        RepairRunner(RepairContext repairContext, PrintStream out, String keyspace, String... columnFamilies) {
            this.out = out;
            this.keyspace = keyspace;
            this.columnFamilies = columnFamilies;
            this.repairContext = repairContext;
        }

        public boolean repairAndWait(StorageServiceMBean ssProxy, boolean isSequential, boolean isLocal, boolean primaryRangeOnly) throws Exception {
            cmd = ssProxy.forceRepairAsync(keyspace, isSequential, isLocal, primaryRangeOnly, columnFamilies);
            waitForRepair();
            return success;
        }

        public boolean repairRangeAndWait(StorageServiceMBean ssProxy, boolean isSequential, boolean isLocal, String startToken, String endToken) throws Exception {
            cmd = ssProxy.forceRepairRangeAsync(startToken, endToken, keyspace, isSequential, isLocal, columnFamilies);
            waitForRepair();
            return success;
        }

        private void waitForRepair() throws Exception {
            if (cmd > 0) {
                try {
                    condition.await();
                } catch (InterruptedException exception) {
                    ssProxy.forceTerminateAllRepairSessions();
                    repairContext.addMessage("Waiting for repair cancelled").activate(RepairTransition.CANCEL);
                    throw new IOException(exception);
                }
            } else {
                String message = String.format("[%s] Nothing to repair for keyspace '%s'", format.format(System.currentTimeMillis()), keyspace);
                out.println(message);
                repairContext.addMessage(message);
            }
            if (error != null) {
                throw error;
            }
        }

        public void handleNotification(Notification notification, Object handback) {
            if ("repair".equals(notification.getType())) {
                int[] status = (int[]) notification.getUserData();
                assert status.length == 2;
                if (cmd == status[0]) {
                    String message = String.format("[%s] %s", format.format(notification.getTimeStamp()), notification.getMessage());
                    out.println(message);
                    repairContext.addMessage(message);
                    // repair status is int array with [0] = cmd number, [1] = status
                    if (status[1] == Status.SESSION_FAILED.ordinal()) {
                        success = false;
                        repairContext.error(host, Status.SESSION_FAILED, message);
                    } else if (status[1] == Status.FINISHED.ordinal()) {
                        condition.signalAll();
                        if (!success) {
                            repairContext.activate(RepairTransition.REPAIR_FAILED);
                        }
                    }
                }
            } else if (JMXConnectionNotification.NOTIFS_LOST.equals(notification.getType())) {
                String message = String.format("[%s] Lost notification. You should check server log for repair status of keyspace %s",
                        format.format(notification.getTimeStamp()),
                        keyspace);
                out.println(message);
                repairContext.addMessage(message);
                success = false;
                condition.signalAll();
                repairContext.error(host, Status.JMX_UNKWOWN, message).activate(RepairTransition.REPAIR_FAILED);
            } else if (JMXConnectionNotification.FAILED.equals(notification.getType())
                    || JMXConnectionNotification.CLOSED.equals(notification.getType())) {
                String message = String.format("JMX connection closed. You should check server log for repair status of keyspace %s"
                        + "(Subsequent keyspaces are not going to be repaired).",
                        keyspace);
                error = new IOException(message);
                condition.signalAll();
            }
        }
    }
}
