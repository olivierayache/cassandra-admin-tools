/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.admin.api.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 *
 * @author Ayache
 */
@XmlRootElement()
@XmlAccessorType(XmlAccessType.FIELD)
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class NodeDto {

    private String name;
    private String load;
    private String dc;
    private boolean repairInProgress;
    private boolean repairInError;

   public static class NodeDtoBuilder {

        public static NodeDto build(String name, String load, String dc) {
            NodeDto node = new NodeDto();
            node.name = name;
            node.load = load;
            node.dc = dc;
            return node;
        }

    }

    @JsOverlay
    public final String getName() {
        return name;
    }

    @JsOverlay
    public final String getLoad() {
        return load;
    }

    @JsOverlay
    public final boolean isRepairInProgress() {
        return repairInProgress;
    }

    @JsOverlay
    public final void setRepairInProgress(boolean repairInProgress) {
        this.repairInProgress = repairInProgress;
    }

    @JsOverlay
    public final boolean isRepairInError() {
        return repairInError;
    }

    @JsOverlay
    public final void setRepairInError(boolean repairInError) {
        this.repairInError = repairInError;
    }

}
