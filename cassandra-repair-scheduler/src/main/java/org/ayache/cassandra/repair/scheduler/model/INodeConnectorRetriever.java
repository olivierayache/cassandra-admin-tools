/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.model;

import java.io.IOException;
import org.ayache.cassandra.admin.INodeConnector;

/**
 *
 * @author Ayache
 */
public interface INodeConnectorRetriever<C extends INodeConnector> {

    /**
     *
     * @return
     */
    C getNodeConnector();

    /**
     *
     * @param hostName
     * @return
     * @throws IOException
     */
    C getNodeConnector(String hostName) throws IOException;

//    /**
//     * 
//     * @return 
//     */
//    Iterable<C> iterable();
}
