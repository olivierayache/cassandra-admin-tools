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

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import org.ayache.cassandra.repair.scheduler.states.RepairContext;

/**
 *
 * @author Ayache
 */
public interface INodeReparator {
    
    /**
     * 
     * @return host of repaired node 
     */
    public String getHost();
    
    /**
     * 
     * @return the list of keyspaces to repair
     * @throws IOException 
     */
    public List<String> getKeyspaces() throws IOException;

    /**
     * 
     * @param context
     * @param lock
     * @param condition
     * @param out
     * @param keyspace
     * @param isSequential
     * @param primaryRange
     * @param columnFamilies
     * @return
     * @throws IOException 
     */
    public long forceRepairAsync(RepairContext context, Lock lock, Condition condition, PrintStream out, String keyspace, boolean isSequential, boolean primaryRange,  String... columnFamilies) throws IOException;

    /**
     * Cancel current repair
     */
    public void cancel();

    public void removeListeners();

    /**
     * 
     * @return true if repair is finished
     */
    public boolean finished();

    public void repairTimeout(RepairContext context);

    public void checkForErrors(RepairContext context);

    public static enum Status {
        STARTED, SESSION_SUCCESS, SESSION_FAILED, FINISHED, JMX_ERROR, JMX_UNKWOWN
    }
}
