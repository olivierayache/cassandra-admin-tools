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
package org.ayache.cassandra.repair.scheduler;

import org.ayache.cassandra.admin.INodeConnector;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.JMX;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.ayache.cassandra.admin.INodeFactory;
import org.ayache.cassandra.admin.VersionClassloader;

/**
 *
 * @author Ayache
 */
public class NodeConnectorProxy implements INodeConnector {

    protected static final String fmtUrl = "service:jmx:rmi:///jndi/rmi://[%s]:%d/jmxrmi";
    protected static final String ssObjName = "org.apache.cassandra.db:type=StorageService";

    protected final String host;
    protected final int port;
    protected String dc;
    protected String username;
    protected String password;
    private String clusterName;
    private transient INodeFactory newInstance;
    private String releaseVersion;

    public NodeConnectorProxy(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        connect();
    }

    public NodeConnectorProxy(String host, int port, String username, String password) throws IOException {
        assert username != null && !username.isEmpty() && password != null && !password.isEmpty() : "neither username nor password can be blank";
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        connect();
    }

    @Override
    public boolean isAlive() {
        return false;
    }

    @Override
    public void connect() throws IOException {
        JMXServiceURL jmxUrl = new JMXServiceURL(String.format(fmtUrl, host, port));
        Map<String, Object> env = new HashMap<String, Object>();
        if (username != null) {
            String[] creds = {username, password};
            env.put(JMXConnector.CREDENTIALS, creds);
        }
        try (JMXConnector connect = JMXConnectorFactory.connect(jmxUrl, env)) {
            MBean newMBeanProxy = JMX.newMBeanProxy(connect.getMBeanServerConnection(), new ObjectName(ssObjName), NodeConnectorProxy.MBean.class);
            releaseVersion = newMBeanProxy.getReleaseVersion();
            System.out.println(releaseVersion);
            clusterName = newMBeanProxy.getClusterName();
            System.out.println(clusterName);
            Constructor<INodeFactory> constructor = (Constructor<INodeFactory>) VersionClassloader.getClassloader(releaseVersion).loadClass("org.ayache.cassandra.repair.scheduler.NodeConnector").getConstructor(String.class, int.class);
            newInstance = constructor.newInstance(host, port);
        } catch (MalformedObjectNameException | ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(NodeConnectorProxy.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public INodeFactory getProxy() {
        return newInstance;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public Object getSsProxy() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static interface MBean {

        /**
         * Fetch a string representation of the Cassandra version.
         *
         * @return A string representation of the Cassandra version.
         */
        public String getReleaseVersion();

        /**
         *
         * @return the name of the cluster
         */
        public String getClusterName();
    }
}
