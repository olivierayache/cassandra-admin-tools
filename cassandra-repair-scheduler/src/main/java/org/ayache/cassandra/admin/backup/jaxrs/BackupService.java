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
package org.ayache.cassandra.admin.backup.jaxrs;

import org.ayache.cassandra.admin.api.IBackupService;
import org.ayache.cassandra.admin.backup.BackupContext;
import org.ayache.cassandra.repair.scheduler.jaxrs.ClusterServiceFactory;

/**
 *
 * @author Ayache
 */
public class BackupService implements IBackupService {

    private final BackupContext backupContext;

    @Override
    public String status() {
        return backupContext.getStatus().toString();
    }

    
    public BackupService(String clusterName) {
        backupContext = ClusterServiceFactory.getInstance().getBackupContext(clusterName);
    }

    @Override
    public void activateSnapshots(String period, long when) {
        switch (period){
            case "MINUTES":
            case "HOURS":
            case "DAYS":
            case "WEEKS":
                break;
            default:
                throw new IllegalArgumentException("period is invalid");
        }
        backupContext.scheduleBackup(period, when);
    }

    @Override
    public void disableSnapshots() {
        backupContext.cancel();
    }

}
