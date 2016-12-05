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
package org.ayache.cassandra.admin.api;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import org.fusesource.restygwt.client.DirectRestService;

/**
 *
 * @author Ayache
 */
public interface IBackupService extends DirectRestService{

    long NOW = -1;
    
    /**
     * Activate snapshots of all keyspaces. A snapshot will be taken with a given period. If when != -1, First occurrence will be delayed by when.  
     * @param period frequency of snapshot, can be MINUTES, HOURS, DAYS, WEEKS
     * @param when initial delay
     */
    @GET
    @Path("activate")
    void activateSnapshots(@DefaultValue("DAYS") @QueryParam("period") String period, @DefaultValue("-1") long when);

    /**
     * Deactivate scheduled snapshots for all keyspaces. 
     */
    @GET
    @Path("disable")
    void disableSnapshots();
    
    /**
     * Returns status of snapshot service.
     * @return status of service
     */
    @GET
    @Path("status")
    String status();

}
