/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.admin;

import com.google.gwt.editor.client.Editor;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import org.ayache.cassandra.repair.scheduler.INodeReparator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ayache
 */
public class VersionClassloaderTest {

    public VersionClassloaderTest() {
    }

    @BeforeClass
    public static void setUpClass() throws URISyntaxException {
        String directory = VersionClassloader.class.getResource("/config-default.properties").getFile().split("classes")[0] + "cassandralibs/";
        System.setProperty("cassandra.lib.directory", new File(directory).toString());
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
     * Test of getClassloader method, of class VersionClassloader.
     */
    @Test
    public void testGetClassloader() throws Exception {
        System.out.println("getClassloader");
        String version = "2.1.18";
        VersionClassloader result = VersionClassloader.getClassloader(version);
        assertNotNull(result);
        version = "2.8.18";
        try {
            VersionClassloader.getClassloader(version);
            fail("Exception should occur");
        } catch (IOException e) {
        }
    }

    /**
     * Test of loadClass method, of class VersionClassloader.
     */
    @Test
    public void testLoadClass() throws Exception {
        System.out.println("loadClass");
        String name = "org.ayache.cassandra.repair.scheduler.NodeConnector";
        boolean resolve = false;
        VersionClassloader instance = VersionClassloader.getClassloader("2.1.18");
        Class<INodeConnector> result = (Class<INodeConnector>) instance.loadClass(name, resolve);
        assertNotNull(result);
        name = "org.ayache.cassandra.repair.scheduler.NodeReparator";
        resolve = true;
        instance = VersionClassloader.getClassloader("2.1.18");
        Class<INodeReparator> result2 = (Class<INodeReparator>) instance.loadClass(name, resolve);
        assertNotNull(result2);
    }

}
