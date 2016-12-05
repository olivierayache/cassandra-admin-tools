/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.admin.api;

import com.google.gwt.core.shared.GwtIncompatible;
import java.util.Collection;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.fusesource.restygwt.client.DirectRestService;

/**
 *
 * @author Ayache
 */
@Path("cassandra-cluster")
public interface IClusterService extends DirectRestService {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/")
    Collection<String> all();

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/add/{address}/{port}")
    String addCluster(@PathParam("address") String address, @PathParam("port") int jmxPort);

    @Path("/{id}/repair")
    @GwtIncompatible
    IRepairService getRepairService(@PathParam("id") String clusterName);

    @Path("/{id}/nodes")
    @GwtIncompatible
    INodeService getNodeService(@PathParam("id") String clusterName);
    
    @Path("/{id}/backup")
    @GwtIncompatible
    IBackupService getBackupService(@PathParam("id") String clusterName);

}
