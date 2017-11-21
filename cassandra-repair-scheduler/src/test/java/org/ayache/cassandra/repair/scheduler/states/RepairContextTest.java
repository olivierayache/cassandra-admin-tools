/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.states;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.cassandra.locator.EndpointSnitchInfoMBean;
import org.ayache.cassandra.admin.api.dto.RepairConfigDto;
import org.ayache.cassandra.repair.scheduler.INodeChooser;
import org.ayache.cassandra.repair.scheduler.NodeChooser;
import org.ayache.cassandra.repair.scheduler.NodeConnector;
import org.ayache.cassandra.repair.scheduler.NodeReparator;
import org.ayache.cassandra.repair.scheduler.jaxrs.ClusterServiceFactory;
import org.ayache.cassandra.repair.scheduler.model.INodeConnectorRetriever;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 *
 * @author Ayache
 */
public class RepairContextTest {

    public RepairContextTest() {
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
     * Test of initNodesToRepair method, of class RepairContext.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testInitNodesToRepair() throws Exception {
        System.out.println("initNodesToRepair");
        RepairContext instance = new RepairContext("Test", 0, 0, 0);
        final NodeConnector mock = Mockito.mock(NodeConnector.class);
        EndpointSnitchInfoMBean esBean = Mockito.mock(EndpointSnitchInfoMBean.class);
        Mockito.when(esBean.getDatacenter(Mockito.anyString())).thenReturn("DC1");
        Mockito.when(mock.getEsProxy()).thenReturn(esBean);
        NodeConnector.StorageServiceCompatMBean serviceMBean = Mockito.mock(NodeConnector.StorageServiceCompatMBean.class);
        List<String> nodes = Arrays.asList("127.0.0.1", "127.0.0.2", "127.0.0.3", "127.0.0.4", "127.0.0.5", "127.0.0.6", "127.0.0.7", "127.0.0.8", "127.0.0.9");
        Map<InetAddress, Float> ownership = new HashMap<>();
        Map<String, String> tokenToEndpoints = new LinkedHashMap<>();
        int Rf = 3;
        for (String node : nodes) {
            ownership.put(InetAddress.getByName(node), 1f / nodes.size() * (float) Rf);
            tokenToEndpoints.put(node, node);
        }
        Mockito.when(serviceMBean.getKeyspaces()).thenReturn(Arrays.asList("TestKeyspace"));
        Mockito.when(serviceMBean.effectiveOwnership(Mockito.anyString())).thenReturn(ownership);
        Mockito.when(serviceMBean.getLiveNodes()).thenReturn(nodes);
        Mockito.when(serviceMBean.getTokenToEndpointMap()).thenReturn(tokenToEndpoints);
        Mockito.when(mock.getSsProxy()).thenReturn(serviceMBean);
        Mockito.when(mock.getDc()).thenReturn("DC1");
        instance.init(new INodeConnectorRetriever() {
            @Override
            public NodeConnector getNodeConnector() {
                return mock;
            }

            @Override
            public NodeConnector getNodeConnector(String hostName) throws IOException {
                return mock;
            }
        });
        Mockito.when(mock.getNodeChooser(Mockito.any(), Mockito.anyBoolean())).thenAnswer(new Answer<INodeChooser>() {
            @Override
            public INodeChooser answer(InvocationOnMock invocation) throws Throwable {
                return new NodeChooser(serviceMBean, esBean, mock.getDc(), null, true);
            }
        });
        Collection<String> expResult = Arrays.asList("127.0.0.1", "127.0.0.5");
        Collection<String> result = instance.initNodesToRepair();
        assertArrayEquals(expResult.toArray(), result.toArray());
        instance.addNodeInError("127.0.0.8");
        instance.addNodeInUnknownError("127.0.0.9");
        expResult = Arrays.asList("127.0.0.8", "127.0.0.9");
        result = instance.initNodesToRepair();
        assertArrayEquals(expResult.toArray(), result.toArray());
    }
//
//    /**
//     * Test of getNodesToRepair method, of class RepairContext.
//     */
//    @Test
//    public void testGetNodesToRepair() {
//        System.out.println("getNodesToRepair");
//        RepairContext instance = null;
//        Iterable<String> expResult = null;
//        Iterable<String> result = instance.getNodesToRepair();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getNodesInError method, of class RepairContext.
//     */
//    @Test
//    public void testGetNodesInError() {
//        System.out.println("getNodesInError");
//        RepairContext instance = null;
//        Iterable<String> expResult = null;
//        Iterable<String> result = instance.getNodesInError();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//

    /**
     * Test of clear method, of class RepairContext.
     *
     * @throws java.io.IOException
     */
    @Test
    public void testClear() throws IOException {
        System.out.println("clear");
        RepairContext instance = new RepairContext("Test", 0, 0, 0);
        instance.addNodeInError("127.0.0.8");
        instance.addNodeInUnknownError("127.0.0.9");
        instance.error("127.0.0.2", NodeReparator.Status.JMX_UNKWOWN);
        instance.clear();
        assertTrue("List" + instance.getNodesToRepair() + "should be empty", !instance.getNodesToRepair().iterator().hasNext());
        assertTrue("List" + instance.getNodesInError() + "should be empty", !instance.getNodesInError().iterator().hasNext());
        assertTrue("List" + instance.getNodesToRepairInUnknown() + "should be empty", instance.getNodesToRepairInUnknown().isEmpty());
//        assertTrue("List"+instance.getNodesToRepairInFailure()+"should be empty", !instance.getNodesToRepairInFailure().iterator().hasNext());
    }

    /**
     * Test of error method, of class RepairContext.
     */
    @Test
    public void testError() {
        System.out.println("error");
        String host = "127.0.0.1";
        NodeReparator.Status status = NodeReparator.Status.JMX_UNKWOWN;
        String[] messages = null;
        RepairContext instance = new RepairContext("Test", 0, 0, 0);
        RepairContext result = Mockito.spy(instance.error(host, status, messages));
        try {
            Mockito.doNothing().when(result).save();
        } catch (IOException ex) {
            Logger.getLogger(RepairContextTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertEquals(new ErrorInfoAggregator(host, status, messages), instance.getNodesToRepairInFailure().iterator().next());
        new Failure(null).execute(result);
        assertTrue(!instance.getNodesToRepairInFailure().iterator().hasNext());
        assertEquals(host, instance.getNodesToRepairInUnknown().iterator().next());
        status = NodeReparator.Status.SESSION_FAILED;
        instance.error(host, status, messages);
        new Failure(null).execute(result);
        assertTrue(!instance.getNodesToRepairInFailure().iterator().hasNext());
        assertEquals(host, instance.getNodesInError().iterator().next());
    }
//
//    /**
//     * Test of getNodesToRepairInUnknown method, of class RepairContext.
//     */
//    @Test
//    public void testGetNodesToRepairInUnknown() {
//        System.out.println("getNodesToRepairInUnknown");
//        RepairContext instance = null;
//        Collection<String> expResult = null;
//        Collection<String> result = instance.getNodesToRepairInUnknown();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getNodesToRepairInFailure method, of class RepairContext.
//     */
//    @Test
//    public void testGetNodesToRepairInFailure() {
//        System.out.println("getNodesToRepairInFailure");
//        RepairContext instance = null;
//        Iterable<ErrorInfoAggregator> expResult = null;
//        Iterable<ErrorInfoAggregator> result = instance.getNodesToRepairInFailure();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addNodeInError method, of class RepairContext.
//     */
//    @Test
//    public void testAddNodeInError() {
//        System.out.println("addNodeInError");
//        String host = "";
//        RepairContext instance = null;
//        instance.addNodeInError(host);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addNodeInUnknownError method, of class RepairContext.
//     */
//    @Test
//    public void testAddNodeInUnknownError() {
//        System.out.println("addNodeInUnknownError");
//        String host = "";
//        RepairContext instance = null;
//        instance.addNodeInUnknownError(host);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of removeNodeInError method, of class RepairContext.
//     */
//    @Test
//    public void testRemoveNodeInError() {
//        System.out.println("removeNodeInError");
//        String id = "";
//        RepairContext instance = null;
//        instance.removeNodeInError(id);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//

    /**
     * Test of addMessage method, of class RepairContext.
     */
    @Test
    public void testAddMessage() {
        System.out.println("addMessage");
        String message = "Test";
        RepairContext instance = new RepairContext("Test", 0, 0, 0);
        for (int j = 0; j < 10000; j++) {
            instance.addMessage(message + j);
        }
        assertEquals(201, instance.getMessages().size());
    }
//
//    /**
//     * Test of addStatus method, of class RepairContext.
//     */
//    @Test
//    public void testAddStatus() {
//        System.out.println("addStatus");
//        NodeReparator.Status status = null;
//        RepairContext instance = null;
//        RepairContext expResult = null;
//        RepairContext result = instance.addStatus(status);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getStatus method, of class RepairContext.
//     */
//    @Test
//    public void testGetStatus() {
//        System.out.println("getStatus");
//        RepairContext instance = null;
//        NodeReparator.Status expResult = null;
//        NodeReparator.Status result = instance.getStatus();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of checkJMXConnections method, of class RepairContext.
//     */
//    @Test
//    public void testCheckJMXConnections() throws Exception {
//        System.out.println("checkJMXConnections");
//        RepairContext instance = null;
//        instance.checkJMXConnections();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addNodeConnector method, of class RepairContext.
//     */
//    @Test
//    public void testAddNodeConnector() {
//        System.out.println("addNodeConnector");
//        NodeConnector connector = null;
//        RepairContext instance = null;
//        instance.addNodeConnector(connector);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getNodeProbe method, of class RepairContext.
//     */
//    @Test
//    public void testGetNodeProbe() throws Exception {
//        System.out.println("getNodeProbe");
//        String host = "";
//        RepairContext instance = null;
//        NodeReparator expResult = null;
//        NodeReparator result = instance.getNodeProbe(host);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of cancelRepairSessions method, of class RepairContext.
//     */
//    @Test
//    public void testCancelRepairSessions() {
//        System.out.println("cancelRepairSessions");
//        RepairContext instance = null;
//        instance.cancelRepairSessions();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of activate method, of class RepairContext.
//     */
//    @Test
//    public void testActivate() {
//        System.out.println("activate");
//        RepairTransition[] transitions = null;
//        RepairContext instance = null;
//        instance.activate(transitions);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getState method, of class RepairContext.
//     */
//    @Test
//    public void testGetState() {
//        System.out.println("getState");
//        RepairContext instance = null;
//        String expResult = "";
//        String result = instance.getState();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getNodes method, of class RepairContext.
//     */
//    @Test
//    public void testGetNodes() throws Exception {
//        System.out.println("getNodes");
//        RepairContext instance = null;
//        Map<String, NodeDto> expResult = null;
//        Map<String, NodeDto> result = instance.getNodes();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getMessages method, of class RepairContext.
//     */
//    @Test
//    public void testGetMessages() {
//        System.out.println("getMessages");
//        RepairContext instance = null;
//        Collection<String> expResult = null;
//        Collection<String> result = instance.getMessages();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of getConfigurations method, of class RepairContext.
     */
    @Test
    public void testGetConfigurations() {
        System.out.println("getConfigurations");
        RepairContext instance = new RepairContext("Test", 0, 21, 2);
        RepairConfigDto expResult = RepairConfigDto.RepairConfigBuilder.build(21, 0, 2, 0, true, false);
        RepairConfigDto result = instance.getConfigurations();
        assertEquals(expResult, result);
        expResult = RepairConfigDto.RepairConfigBuilder.build(23, 21, 3, 15, true, true);
        instance.editConfigurations(expResult);
        instance.checkAndApplyChanges();
        result = instance.getConfigurations();
        assertEquals(expResult, result);

    }

//    /**
//     * Test of editConfigurations method, of class RepairContext.
//     */
//    @Test
//    public void testEditConfigurations() {
//        System.out.println("editConfigurations");
//        RepairConfigDto dto = null;
//        RepairContext instance = null;
//        instance.editConfigurations(dto);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of checkAndApplyChanges method, of class RepairContext.
//     */
//    @Test
//    public void testCheckAndApplyChanges() {
//        System.out.println("checkAndApplyChanges");
//        RepairContext instance = null;
//        boolean expResult = false;
//        boolean result = instance.checkAndApplyChanges();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setWaitingCondition method, of class RepairContext.
//     */
//    @Test
//    public void testSetWaitingCondition() {
//        System.out.println("setWaitingCondition");
//        Lock lock = null;
//        Condition condition = null;
//        RepairContext instance = null;
//        instance.setWaitingCondition(lock, condition);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of cancelWaiting method, of class RepairContext.
//     */
//    @Test
//    public void testCancelWaiting() {
//        System.out.println("cancelWaiting");
//        RepairContext instance = null;
//        instance.cancelWaiting();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of save method, of class RepairContext.
//     */
//    @Test
//    public void testSave() throws Exception {
//        System.out.println("save");
//        RepairContext instance = null;
//        instance.save();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
}
