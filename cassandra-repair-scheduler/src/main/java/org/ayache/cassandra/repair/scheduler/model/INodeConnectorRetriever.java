/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.model;

import java.io.IOException;
import org.ayache.cassandra.repair.scheduler.NodeConnector;

/**
 *
 * @author Ayache
 */
public interface INodeConnectorRetriever {

    /**
     *
     * @return
     */
    NodeConnector getNodeConnector();

    /**
     *
     * @param hostName
     * @return
     * @throws IOException
     */
    NodeConnector getNodeConnector(String hostName) throws IOException;

    /**
     * 
     * @return 
     */
    Iterable<NodeConnector> iterable();
}
