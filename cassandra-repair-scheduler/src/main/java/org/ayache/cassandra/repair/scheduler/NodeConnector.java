/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.apache.cassandra.locator.EndpointSnitchInfoMBean;
import org.apache.cassandra.net.MessagingServiceMBean;
import org.apache.cassandra.service.StorageServiceMBean;
import org.ayache.cassandra.admin.api.dto.NodeDto;

/**
 *
 * @author Ayache
 */
public class NodeConnector {

    private static final String fmtUrl = "service:jmx:rmi:///jndi/rmi://[%s]:%d/jmxrmi";
    private static final String ssObjName = "org.apache.cassandra.db:type=StorageService";
    private static final String esObjName = "org.apache.cassandra.db:type=EndpointSnitchInfo";
    private static final String msObjName = "org.apache.cassandra.net:type=MessagingService";
    private static final int defaultPort = 7199;
    private final String host;
    private final int port;
    private String dc;
    private String username;
    private String password;

    private transient JMXConnector jmxc;
    private transient MBeanServerConnection mbeanServerConn;
    private transient StorageServiceMBean ssProxy;
    private transient MessagingServiceMBean msProxy;
    private transient EndpointSnitchInfoMBean esProxy;

    private transient NodeReparator nodeReparator;
    
    private static final ExecutorService ES = Executors.newCachedThreadPool();
    
    private static final class ReconnectRunnable implements Runnable{

        private final NodeConnector connector;

        public ReconnectRunnable(NodeConnector connector) {
            this.connector = connector;
        }
        
        @Override
        public void run() {
            boolean ok = false;
            while (!ok) {
                try {
                    Thread.sleep(2000);
                    connector.connect();
                    ok = true;
                } catch (Exception ex) {
                    Logger.getLogger(NodeConnector.class.getName()).log(Level.INFO, "Unable to connect via JMX, will retry in 2 seconds", ex.getMessage());
                }
            }
        }
        
    }

    /**
     * Creates a NodeConnector using the specified JMX host and port.
     *
     * @param host hostname or IP address of the JMX agent
     * @param port TCP port of the remote JMX agent
     * @throws IOException on connection failures
     */
    public NodeConnector(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        connect();
    }

    /**
     * Creates a NodeConnector using the specified JMX host, port, username, and
     * password.
     *
     * @param host hostname or IP address of the JMX agent
     * @param port TCP port of the remote JMX agent
     * @throws IOException on connection failures
     */
    public NodeConnector(String host, int port, String username, String password) throws IOException {
        assert username != null && !username.isEmpty() && password != null && !password.isEmpty() : "neither username nor password can be blank";

        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        connect();
    }

    /**
     * Create a connection to the JMX agent and setup the M[X]Bean proxies.
     *
     * @throws IOException on connection failures
     */
    public void connect() throws IOException {
        JMXServiceURL jmxUrl = new JMXServiceURL(String.format(fmtUrl, host, port));
        Map<String, Object> env = new HashMap<String, Object>();
        if (username != null) {
            String[] creds = {username, password};
            env.put(JMXConnector.CREDENTIALS, creds);
        }
        jmxc = JMXConnectorFactory.connect(jmxUrl, env);
        mbeanServerConn = jmxc.getMBeanServerConnection();

        try {
            ObjectName name = new ObjectName(ssObjName);
            ssProxy = JMX.newMBeanProxy(mbeanServerConn, name, StorageServiceMBean.class);
            name = new ObjectName(msObjName);
            msProxy = JMX.newMBeanProxy(mbeanServerConn, name, MessagingServiceMBean.class);
            name = new ObjectName(esObjName);
            esProxy = JMX.newMBeanProxy(mbeanServerConn, name, EndpointSnitchInfoMBean.class);
//            name = new ObjectName(PBSPredictor.MBEAN_NAME);
//            PBSPredictorProxy = JMX.newMBeanProxy(mbeanServerConn, name, PBSPredictorMBean.class);
//            name = new ObjectName(StreamingService.MBEAN_OBJECT_NAME);
//            streamProxy = JMX.newMBeanProxy(mbeanServerConn, name, StreamingServiceMBean.class);
//            name = new ObjectName(CompactionManager.MBEAN_OBJECT_NAME);
//            compactionProxy = JMX.newMBeanProxy(mbeanServerConn, name, CompactionManagerMBean.class);
//            name = new ObjectName(FailureDetector.MBEAN_NAME);
//            fdProxy = JMX.newMBeanProxy(mbeanServerConn, name, FailureDetectorMBean.class);
//            name = new ObjectName(CacheService.MBEAN_NAME);
//            cacheService = JMX.newMBeanProxy(mbeanServerConn, name, CacheServiceMBean.class);
//            name = new ObjectName(StorageProxy.MBEAN_NAME);
//            spProxy = JMX.newMBeanProxy(mbeanServerConn, name, StorageProxyMBean.class);
//            name = new ObjectName(HintedHandOffManager.MBEAN_NAME);
//            hhProxy = JMX.newMBeanProxy(mbeanServerConn, name, HintedHandOffManagerMBean.class);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(
                    "Invalid ObjectName? Please report this as a bug.", e);
        }

        dc = esProxy.getDatacenter(host);
        nodeReparator = new NodeReparator(host, jmxc, ssProxy);

        jmxc.addConnectionNotificationListener(new NotificationListener() {
            @Override
            public void handleNotification(Notification notification, Object handback) {
                if (notification.getType().equals(JMXConnectionNotification.CLOSED) || notification.getType().equals(JMXConnectionNotification.FAILED)) {
                    ES.submit(new ReconnectRunnable(NodeConnector.this));
                }
            }
        }, null, null);

//        memProxy = ManagementFactory.newPlatformMXBeanProxy(mbeanServerConn,
//                ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class);
//        runtimeProxy = ManagementFactory.newPlatformMXBeanProxy(
//                mbeanServerConn, ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
    }

    public void close() throws IOException {
        jmxc.close();
    }

    public StorageServiceMBean getSsProxy() {
        return ssProxy;
    }

    public MessagingServiceMBean getMsProxy() {
        return msProxy;
    }
    
    public EndpointSnitchInfoMBean getEsProxy() {
        return esProxy;
    }

    public NodeReparator getNodeReparator() {
        return nodeReparator;
    }

    public String getDc() {
        return dc;
    }

    public Map<String, NodeDto> getNodes() throws IOException {
        Map<String, NodeDto> nodes = new HashMap<>();
        Map<String, String> loadMap = ssProxy.getLoadMap();
        Map<String, String> tokenToEndpointMap = ssProxy.getTokenToEndpointMap();
        for (Map.Entry<String, String> entry : tokenToEndpointMap.entrySet()) {
            String host = entry.getValue();
            nodes.put(host, NodeDto.NodeDtoBuilder.build(host, loadMap.get(host), esProxy.getDatacenter(host)));
        }
        return nodes;
    }

    public String getHost() {
        return host;
    }
}
