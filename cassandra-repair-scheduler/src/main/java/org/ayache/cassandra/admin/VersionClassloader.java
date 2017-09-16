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

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 *
 * @author Ayache
 */
public class VersionClassloader extends URLClassLoader {

    private static final ConcurrentHashMap<String, VersionClassloader> INSTANCE = new ConcurrentHashMap<>();
    static String cassandraLib = System.getProperty("cassandra.lib.directory");

    public static VersionClassloader getClassloader(String version) throws IOException {
        VersionClassloader putIfAbsent = INSTANCE.putIfAbsent(version, new VersionClassloader(version));
        return putIfAbsent == null ? INSTANCE.get(version) : putIfAbsent;
    }

    private VersionClassloader(String releaseVersion) throws IOException {
        super(new URL[]{new URL(URLClassLoader.class.getResource("/config-default.properties").toString().split("config-default.properties")[0]), getPath(releaseVersion).toUri().toURL()});

    }

    private static Path getPath(String releaseVersion) throws IOException {
        try (Stream<Path> list = Files.list(FileSystems.getDefault().getPath(cassandraLib))) {
            return list.filter((Path t) -> {
                String[] split = releaseVersion.split("\\.");
                return t.getFileName().toString().matches(".*" + split[0] + "\\." + split[1] + "\\..*");
            }).findFirst().orElseThrow(() -> new IOException("Library for Cassandra "+releaseVersion + " not present"));
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith("org.ayache.cassandra.repair.scheduler.Node")
                || name.startsWith("org.ayache.cassandra.repair.scheduler.jaxrs.NodeInfo")) {

            Class<?> c = findLoadedClass(name) == null ? findClass(name) : findLoadedClass(name);
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }

        return super.loadClass(name, resolve);

    }

}
