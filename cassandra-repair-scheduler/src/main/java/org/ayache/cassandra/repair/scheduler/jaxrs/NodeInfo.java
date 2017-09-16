/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.jaxrs;

import java.util.List;
import org.apache.cassandra.service.StorageServiceMBean;
import org.ayache.cassandra.admin.api.dto.NodeInfoDto;

/**
 *
 * @author Ayache
 */
public interface NodeInfo extends StorageServiceMBean, NodeInfoDto {

    /**
     * Returns the list of editable attributes of StorageServiceMBean
     * 
     * @return the list of editable attributes
     */
    List<String> getEditables();
}
