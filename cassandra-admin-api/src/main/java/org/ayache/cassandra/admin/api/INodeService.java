/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.admin.api;

import java.util.Collection;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.ayache.cassandra.admin.api.dto.NodeDto;
import org.ayache.cassandra.admin.api.dto.NodeInfoDto;
import org.fusesource.restygwt.client.DirectRestService;

/**
 *
 * @author Ayache
 */
public interface INodeService extends DirectRestService{

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/")
    Collection<NodeDto> describe();
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/infos/{name}")        
    NodeInfoDto getInfos(@PathParam("name") String name);
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/infos") 
    Collection<NodeInfoDto> getInfos();

    @PUT
    @Path("/configure/{name}/{param}/{value}")        
    void configure(@PathParam("name") String name, @PathParam("param") String param, @PathParam("value") String value);
    
    @PUT
    @Path("/configure/{param}/{value}")        
    void configure(@PathParam("param") String param, @PathParam("value") String value);
}
