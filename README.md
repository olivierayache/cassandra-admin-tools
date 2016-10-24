# cassandra-admin-tools

This project provides tools for Cassandra cluster administration. Multiple clusters and multi data center clusters are supported. The main feature is repair scheduling and monitoring. It simplify errors handling during repair with automatic rescheduling of failed repairs. In order to work properly the tool needs to connect to each node of cluster via JMX (connection can be secured via login/password).

Tools can be accessed via Web UI or directly via REST apis 

Dist directory contains binary distribution of current version. It can be started with the following command:

java -jar cassandra-repair-scheduler-1.0.0-SNAPSHOT.jar. 

Once started Web UI is accessible via http://localhost:8080/index.html
