/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
public class NodeReparator implements NotificationListener, INodeReparator {

    public final String host;
    private final JMXConnector jmxc;
    public NodeConnector.StorageServiceCompatMBean ssProxy;
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
     * @param ssProx
     */
    public NodeReparator(String host, JMXConnector jmxc, StorageServiceMBean ssProx) {
        this.host = host;
        this.jmxc = jmxc;
        try {
            final NodeConnector.StorageServiceCompat bean = (NodeConnector.StorageServiceCompat) Class.forName(NodeConnector.StorageServiceCompat.class.getName()).getConstructor(ssProx.getClass().getInterfaces()[0]).newInstance(ssProx);

            InvocationHandler invocationHandler = new InvocationHandler() {

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    Object ret;
                    try {
                        if (args == null) {
                            ret = method.invoke(ssProx);
                        } else {
                            ret = method.invoke(ssProx, args);
                        }
                        return ret;
                    } catch (Throwable e) {
                        return bean.getClass().getMethod(method.getName(), method.getParameterTypes()).invoke(bean, args);
                    }

                }
            };
            this.ssProxy = (NodeConnector.StorageServiceCompatMBean) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{NodeConnector.StorageServiceCompatMBean.class}, invocationHandler);
        } catch (Exception ex) {
            Logger.getLogger(NodeReparator.class.getName()).log(Level.SEVERE, null, ex);
            this.ssProxy = null;
        }

    }

    public boolean succeed() {
        return success;
    }

    @Override
    public boolean finished() {
        return finished;
    }

    @Override
    public void checkForErrors(RepairContext context) {
        if (errorMessage != null) {
            context.error(host, Status.JMX_UNKWOWN, errorMessage).activate(RepairTransition.REPAIR_FAILED);
        }
    }

    @Override
    public void repairTimeout(RepairContext context) {
        ssProxy.forceTerminateAllRepairSessions();
        context.error(host, NodeReparator.Status.JMX_UNKWOWN, "Repair duration exceeds 8 hours. You should check server log for repair status").activate(RepairTransition.REPAIR_FAILED);
    }

    @Override
    public void cancel() {
        ssProxy.forceTerminateAllRepairSessions();

    }

    @Override
    public void removeListeners() {
        try {
            ssProxy.removeNotificationListener(this);
            jmxc.removeConnectionNotificationListener(this);
        } catch (Exception ex) {
            Logger.getLogger(NodeReparator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public long forceRepairAsync(final RepairContext context, final Lock lock, final Condition condition, final PrintStream out, final String tableName, boolean isSequential, boolean primaryRange, String... columnFamilies) throws IOException {

        this.out = out;
        this.keyspace = tableName;
        this.repairContext = (RepairContext) context;
        this.condition = condition;
        this.lock = lock;
        finished = false;
        success = true;
        errorMessage = null;

        try {
            jmxc.addConnectionNotificationListener(this, null, null);
            ssProxy.addNotificationListener(this, null, null);
            cmd = ssProxy.forceRepairAsync(keyspace, isSequential, repairContext.repairLocalDCOnly, repairContext.repairLocalDCOnly ? false : primaryRange, columnFamilies);
            if (cmd == 0) {
                String message = String.format("[%s] %s Nothing to repair for keyspace '%s'", format.format(System.currentTimeMillis()), host, keyspace);
                out.println(message);
                repairContext.addMessage(message);
            }
            return cmd;
        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public List<String> getKeyspaces() throws IOException {
        return ssProxy.getKeyspaces();
    }

    @Override
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
        } else if (JMXConnectionNotification.FAILED.equals(notification.getType())
                || JMXConnectionNotification.CLOSED.equals(notification.getType())) {
            String message = String.format("[%s] %s JMX connection closed. You should check server log for repair status of keyspace %s"
                    + "(Subsequent keyspaces are not going to be repaired).", format.format(notification.getTimeStamp()), host,
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
