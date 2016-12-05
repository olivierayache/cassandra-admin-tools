/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.admin;

import io.undertow.Undertow;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.ayache.cassandra.repair.scheduler.jaxrs.ClusterService;
import org.ayache.cassandra.repair.scheduler.jaxrs.ClusterServiceFactory;
import org.ayache.cassandra.repair.scheduler.states.Repair;
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
        CommandLineParser lineParser = new PosixParser();
        final String host = lineParser.parse(OPTIONS, args).getOptionValue("h");
        final String port = lineParser.parse(OPTIONS, args).getOptionValue("p");
        final String jaxhost = lineParser.parse(OPTIONS, args).getOptionValue("jh", "0.0.0.0");
        final String jaxport = lineParser.parse(OPTIONS, args).getOptionValue("jp", "8080");

        ResteasyDeployment rd = new ResteasyDeployment();
        rd.getActualResourceClasses().add(ClusterService.class);
        UndertowJaxrsServer server = new UndertowJaxrsServer();
        server.deploy(server.undertowDeployment(rd, "/admin").setDeploymentName("cassandra-admin").setContextPath("/").setResourceManager(new ClassPathResourceManager(NewMain.class.getClassLoader())).setClassLoader(NewMain.class.getClassLoader()));
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
    }

}
