/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.cassandra.locator.EndpointSnitchInfoMBean;
import org.apache.cassandra.service.StorageServiceMBean;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.Mockito;

/**
 *
 * @author Ayache
 */
public class NodeChooserTest {
    
    public NodeChooserTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getNextNodeToRepair method, of class NodeChooser.
     */
    @Test
    public void testGetNextNodeToRepair() throws Exception {
        System.out.println("getNextNodeToRepair");
        StorageServiceMBean mock = Mockito.mock(StorageServiceMBean.class);
        List<String> nodes = Arrays.asList("127.0.0.1","127.0.0.2","127.0.0.3","127.0.0.4","127.0.0.5","127.0.0.6","127.0.0.7","127.0.0.8","127.0.0.9");
        Map<InetAddress,Float> ownership = new HashMap<>();
        Map<String,String> tokenToEndpoints = new LinkedHashMap<>();
        int Rf = 3;
        for (String node : nodes) {
            ownership.put(InetAddress.getByName(node), 1f/nodes.size()*(float)Rf);
            tokenToEndpoints.put(node, node);
        }
        Mockito.when(mock.getKeyspaces()).thenReturn(Arrays.asList("TestKeyspace"));
        Mockito.when(mock.effectiveOwnership(Mockito.anyString())).thenReturn(ownership);
        Mockito.when(mock.getLiveNodes()).thenReturn(nodes);
        Mockito.when(mock.getTokenToEndpointMap()).thenReturn(tokenToEndpoints);
        NodeChooser instance = new NodeChooser(mock, new EndpointSnitchInfoMBean() {
            @Override
            public String getRack(String host) throws UnknownHostException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public String getDatacenter(String host) throws UnknownHostException {
                return "DC1";
            }

            @Override
            public String getSnitchName() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        }, "DC1", null);
        Collection<String> expResult = new HashSet<>(Arrays.asList("127.0.0.1","127.0.0.5"));
        Collection<String> result = instance.getNextNodeToRepair();
        assertEquals(expResult, result);
        
        instance = new NodeChooser(mock, new EndpointSnitchInfoMBean() {
            @Override
            public String getRack(String host) throws UnknownHostException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public String getDatacenter(String host) throws UnknownHostException {
                return "DC1";
            }

            @Override
            public String getSnitchName() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        }, "DC1", "127.0.0.2");
        expResult = new HashSet<>(Arrays.asList("127.0.0.3","127.0.0.7"));
        result = instance.getNextNodeToRepair();
        assertEquals(expResult, result);
       
                instance = new NodeChooser(mock, new EndpointSnitchInfoMBean() {
            @Override
            public String getRack(String host) throws UnknownHostException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public String getDatacenter(String host) throws UnknownHostException {
                return "DC1";
            }

            @Override
            public String getSnitchName() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        }, "DC1", "127.0.0.8");
        expResult = new HashSet<>(Arrays.asList("127.0.0.9","127.0.0.4"));
        result = instance.getNextNodeToRepair();
        assertEquals(expResult, result);
        
        instance = new NodeChooser(mock, new EndpointSnitchInfoMBean() {
            @Override
            public String getRack(String host) throws UnknownHostException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public String getDatacenter(String host) throws UnknownHostException {
                return "DC1";
            }

            @Override
            public String getSnitchName() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        }, "DC1", "127.0.0.9");
        expResult = new HashSet<>(Arrays.asList("127.0.0.1","127.0.0.5"));
        result = instance.getNextNodeToRepair();
        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }
    
}
