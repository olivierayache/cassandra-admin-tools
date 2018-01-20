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
package org.ayache.cassandra.admin;

import io.undertow.Undertow;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.ayache.cassandra.repair.scheduler.jaxrs.ClusterService;
import org.ayache.cassandra.repair.scheduler.jaxrs.ClusterServiceFactory;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;

/**
 *
 * @author Ayache
 */
public class NewMain {

    private static final Options OPTIONS = new Options();

    static {
        OPTIONS.addOption("h", "host", true, "IP address of a Cassandra node accessible via JMX");
        OPTIONS.addOption("p", "port", true, "JMX port of Cassandra node");
        OPTIONS.addOption("jh", "jaxrshost", true, "Listen address of REST server");
        OPTIONS.addOption("jp", "jaxrsport", true, "Listen port of REST server");
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException, ParseException {
        //LogManager.getLogManager().readConfiguration(NewMain.class.getResourceAsStream("/logging.properties"));
        //System.setOut(new PrintStream("repaird.log"));
        HelpFormatter h = new HelpFormatter();
        h.printHelp("main", OPTIONS);
        CommandLineParser lineParser = new DefaultParser();
        final String host = lineParser.parse(OPTIONS, args).getOptionValue("h");
        final String port = lineParser.parse(OPTIONS, args).getOptionValue("p");
        final String jaxhost = lineParser.parse(OPTIONS, args).getOptionValue("jh", "0.0.0.0");
        final String jaxport = lineParser.parse(OPTIONS, args).getOptionValue("jp", "8080");

        ResteasyDeployment rd = new ResteasyDeployment();
        rd.getActualResourceClasses().add(ClusterService.class);
        UndertowJaxrsServer server = new UndertowJaxrsServer();
        server.deploy(server.undertowDeployment(rd, "/admin")
                .setDeploymentName("cassandra-admin")
                .setContextPath("/")
                .addWelcomePage("index.html")
                .setResourceManager(new ClassPathResourceManager(NewMain.class.getClassLoader()))
                .setClassLoader(NewMain.class.getClassLoader()));
        Undertow.Builder undertow = Undertow.builder().addHttpListener(Integer.valueOf(jaxport), jaxhost);
        server.start(undertow);

        if (host != null && port != null) {
            String clusterName = ClusterServiceFactory.getInstance().addCluster(host, Integer.valueOf(port));
            ClusterServiceFactory.getInstance().getRepairContext(clusterName).activate();
        }
        File dir = new File("data");
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
                    ClusterServiceFactory.getInstance().loadCluster(bufferedReader.readLine());
                }
            }
        }
        
        Runtime.getRuntime().addShutdownHook(new Thread("ShutDownHook-Thread"){
            @Override
            public void run() {
                server.stop();
            }
        });
    }

}
