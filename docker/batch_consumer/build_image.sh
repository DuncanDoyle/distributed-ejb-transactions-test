#!/bin/sh
#
# Builds the Docker container.
#
# author: ddoyle@redhat.com
#

# Retrieve the archive we want to deploy via Maven.
mvn -o dependency:copy -Dmdep.useBaseVersion=true -DoutputDirectory=./dockerfile_copy/applications -Dartifact=org.jboss.ddoyle.ejbtmbug.reproducer:BatchConsumer:1.0.0-SNAPSHOT:jar

docker build --rm -t ddoyle/jboss-ejb-tm-bug-batch-consumer:1.0 .
