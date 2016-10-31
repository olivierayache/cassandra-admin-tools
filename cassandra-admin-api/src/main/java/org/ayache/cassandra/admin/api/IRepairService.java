/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.admin.api;

import java.util.Collection;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.ayache.cassandra.admin.api.dto.NodeDto;
import org.ayache.cassandra.admin.api.dto.RepairConfigDto;
import org.fusesource.restygwt.client.DirectRestService;

/**
 *
 * @author Ayache
 */
public interface IRepairService extends DirectRestService {

    @PUT
    @Path("handle")
    void handleErrorNotification(@DefaultValue(value = "true") @QueryParam(value = "restart") boolean restartSession);

    @PUT
    @Path("restart/{id}")
    void restartSessionForNode(@PathParam(value = "id") String id, @QueryParam(value = "checkForRestart") @DefaultValue("false") boolean checkForRestart);
    
    @DELETE
    @Path("errors/{id}")
    void purgeErrorForNode(@PathParam(value = "id") String id, @QueryParam(value = "checkForRestart") @DefaultValue("false") boolean checkForRestart);

    @DELETE
    @Path("cancel")
    void cancelRepair();
    
    @PUT
    @Path("activate")
    void activateRepair();

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("status")
    String status();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("describe")
    Collection<NodeDto> describe();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("history")
    Collection<String> history();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("config")
    RepairConfigDto getConfigurations();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("config")
    void editConfigurations(RepairConfigDto config, @QueryParam("cancel") boolean cancelCurrent);

}
