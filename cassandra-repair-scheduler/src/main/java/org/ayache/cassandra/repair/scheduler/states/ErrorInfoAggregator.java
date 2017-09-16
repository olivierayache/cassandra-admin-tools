/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.states;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Objects;
import org.ayache.cassandra.repair.scheduler.INodeReparator;

/**
 *
 * @author Ayache
 */
public class ErrorInfoAggregator {

    private final String host;
    private final INodeReparator.Status status;
    private final String[] message;
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
    private final long timeStamp;

    public ErrorInfoAggregator(String host, INodeReparator.Status status, String... message) {
        this.host = host;
        this.status = status;
        this.message = message;
        timeStamp = System.currentTimeMillis();
    }

    public String getHost() {
        return host;
    }

    public INodeReparator.Status getStatus() {
        return status;
    }

    public Iterable<String> getMessage() {
        return Arrays.asList(message);
    }

    @Override
    public String toString() {
        return String.format("[%s] Node %s in failure (%s): %s", FORMAT.format(timeStamp), host, status, Arrays.toString(message));
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 11 * hash + Objects.hashCode(this.host);
        hash = 11 * hash + Objects.hashCode(this.status);
        hash = 11 * hash + Arrays.deepHashCode(this.message);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ErrorInfoAggregator other = (ErrorInfoAggregator) obj;
        if (!Objects.equals(this.host, other.host)) {
            return false;
        }
        if (this.status != other.status) {
            return false;
        }
        if (!Arrays.deepEquals(this.message, other.message)) {
            return false;
        }
        return true;
    }

   
    
    
}
