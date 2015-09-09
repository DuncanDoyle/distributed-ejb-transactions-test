#!/bin/sh
#
# Builds the Docker image.
#
# author: ddoyle@redhat.com
#

# Retrieve the archive we want to deploy via Maven.
mvn -o dependency:copy -Dmdep.useBaseVersion=true -DoutputDirectory=./dockerfile_copy/applications -Dartifact=org.jboss.ddoyle.ejbtmbug.reproducer:BatchProducer:1.0.0-SNAPSHOT:jar
mvn -o dependency:copy -Dmdep.useBaseVersion=true -DoutputDirectory=./dockerfile_copy/applications -Dartifact=org.jboss.ddoyle.ejbtmbug.reproducer:BatchProducerWeb:1.0.0-SNAPSHOT:war

docker build --rm -t ddoyle/jboss-ejb-tm-bug-batch-producer:1.0 .
