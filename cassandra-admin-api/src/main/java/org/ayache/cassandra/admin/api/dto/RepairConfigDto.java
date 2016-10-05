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
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class RepairConfigDto {

    public int hourToBegin;
    public int minutesToBegin;
    public int lastHourToBegin;
    public int lastMinutesToBegin;
    public boolean simultaneousRepair;
    public boolean repairLocalDCOnly;

    public static class RepairConfigBuilder {

        public static RepairConfigDto build(int hourToBegin, int minutesToBegin, int lastHourToBegin, int lastMinutesToBegin, boolean simultaneousRepair, boolean repairLocalDCOnly) {
            RepairConfigDto repairConfigDto = new RepairConfigDto();
            repairConfigDto.hourToBegin = hourToBegin;
            repairConfigDto.minutesToBegin = minutesToBegin;
            repairConfigDto.lastHourToBegin = lastHourToBegin;
            repairConfigDto.lastMinutesToBegin = lastMinutesToBegin;
            repairConfigDto.simultaneousRepair = simultaneousRepair;
            repairConfigDto.repairLocalDCOnly = repairLocalDCOnly;
            return repairConfigDto;
        }

    }

    @Override
    @JsOverlay
    public final int hashCode() {
        int hash = 7;
        hash = 83 * hash + this.hourToBegin;
        hash = 83 * hash + this.minutesToBegin;
        hash = 83 * hash + this.lastHourToBegin;
        hash = 83 * hash + this.lastMinutesToBegin;
        hash = 83 * hash + (this.simultaneousRepair ? 1 : 0);
        hash = 83 * hash + (this.repairLocalDCOnly ? 1 : 0);
        return hash;
    }

    @Override
    @JsOverlay
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RepairConfigDto other = (RepairConfigDto) obj;
        if (this.hourToBegin != other.hourToBegin) {
            return false;
        }
        if (this.minutesToBegin != other.minutesToBegin) {
            return false;
        }
        if (this.lastHourToBegin != other.lastHourToBegin) {
            return false;
        }
        if (this.lastMinutesToBegin != other.lastMinutesToBegin) {
            return false;
        }
        if (this.simultaneousRepair != other.simultaneousRepair) {
            return false;
        }
        if (this.repairLocalDCOnly != other.repairLocalDCOnly) {
            return false;
        }
        return true;
    }

    @Override
    @JsOverlay
    public final String toString() {
        return "RepairConfigDto{" + "hourToBegin=" + hourToBegin + ", minutesToBegin=" + minutesToBegin + ", lastHourToBegin=" + lastHourToBegin + ", lastMinutesToBegin=" + lastMinutesToBegin + ", simultaneousRepair=" + simultaneousRepair + ", repairLocalDCOnly=" + repairLocalDCOnly + '}';
    }

}
