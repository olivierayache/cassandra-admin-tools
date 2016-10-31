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
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import org.apache.cassandra.service.StorageServiceMBean;
import org.ayache.cassandra.repair.scheduler.states.RepairContext;

/**
 *
 * @author Ayache
 */
public class NodeReparator implements NotificationListener {

    public static enum Status {
        STARTED, SESSION_SUCCESS, SESSION_FAILED, FINISHED, JMX_ERROR, JMX_UNKWOWN
    }

    public final String host;
    private final JMXConnector jmxc;
    public final StorageServiceMBean ssProxy;
    private volatile boolean success = true;
    private volatile boolean finished;
    private volatile String errorMessage;
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
    private volatile Condition condition;
    private volatile Lock lock;
    private PrintStream out;
    private volatile String keyspace;
    private volatile RepairContext repairContext;
    private volatile int cmd;

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

    public boolean succeed() {
        return success;
    }

    public boolean finished() {
        return finished;
    }

    public void checkForErrors(RepairContext context) {
        if (errorMessage != null) {
            context.error(host, Status.JMX_UNKWOWN, errorMessage).activate(RepairTransition.REPAIR_FAILED);
        }
    }

    public void repairTimeout(RepairContext context) {
        ssProxy.forceTerminateAllRepairSessions();
        context.error(host, NodeReparator.Status.JMX_UNKWOWN, "Repair duration exceeds 8 hours. You should check server log for repair status").activate(RepairTransition.REPAIR_FAILED);
    }

    public void cancel() {
        ssProxy.forceTerminateAllRepairSessions();

    }

    public void removeListeners() {
        try {
            ssProxy.removeNotificationListener(this);
            jmxc.removeConnectionNotificationListener(this);
        } catch (Exception ex) {
            Logger.getLogger(NodeReparator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public long forceRepairAsync(final RepairContext context, final Lock lock, final Condition condition, final PrintStream out, final String tableName, boolean isSequential, boolean isLocal, boolean primaryRange, String... columnFamilies) throws IOException {

        this.out = out;
        this.keyspace = tableName;
        this.repairContext = context;
        this.condition = condition;
        this.lock = lock;
        finished = false;
        success = true;

        try {
            jmxc.addConnectionNotificationListener(this, null, null);
            ssProxy.addNotificationListener(this, null, null);
            cmd = ssProxy.forceRepairAsync(keyspace, isSequential, isLocal, isLocal ? false : primaryRange, columnFamilies);
            if (cmd == 0) {
                String message = String.format("[%s] %s Nothing to repair for keyspace '%s'", format.format(System.currentTimeMillis()), host, keyspace);
                out.println(message);
                repairContext.addMessage(message);
            }
            return cmd;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public List<String> getKeyspaces() throws IOException {
        return ssProxy.getKeyspaces();
    }

    public void handleNotification(Notification notification, Object handback) {
        if ("repair".equals(notification.getType())) {
            int[] status = (int[]) notification.getUserData();
            assert status.length == 2;
            if (cmd == status[0]) {
                String message = String.format("[%s] %s %s", format.format(notification.getTimeStamp()), host, notification.getMessage());
                out.println(message);
                repairContext.addMessage(message);
                // repair status is int array with [0] = cmd number, [1] = status
                if (status[1] == Status.SESSION_FAILED.ordinal()) {
                    success = false;
                    repairContext.error(host, Status.SESSION_FAILED, message);
                } else if (status[1] == Status.FINISHED.ordinal()) {
                    if (!success) {
                        repairContext.activate(RepairTransition.REPAIR_FAILED);
                    }
                    finished = true;
                    lock.lock();
                    try {
                        condition.signalAll();
                    } finally {
                        lock.unlock();
                    }

                }
            }
        } else if (JMXConnectionNotification.NOTIFS_LOST.equals(notification.getType())) {
            String message = String.format("[%s] %s Lost notification. You should check server log for repair status of keyspace %s",
                    format.format(notification.getTimeStamp()),host,
                    keyspace);
            out.println(message);
            success = false;
            errorMessage = message;
            finished = true;
            lock.lock();
            try {
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        } else if (JMXConnectionNotification.FAILED.equals(notification.getType())
                || JMXConnectionNotification.CLOSED.equals(notification.getType())) {
            String message = String.format("[%s] %s JMX connection closed. You should check server log for repair status of keyspace %s"
                    + "(Subsequent keyspaces are not going to be repaired).",format.format(notification.getTimeStamp()),host,
                    keyspace);
            out.println(message);
            success = false;
            errorMessage = message;
            finished = true;
            lock.lock();
            try {
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }
}
