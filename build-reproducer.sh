#!/bin/sh

# Check that JBoss EAP 6.1.1.zip has been copied to the current directory
if [ ! -f jboss-eap-6.1.1.zip ]; then
    echo "File 'jboss-eap-6.1.1.zip' not found. Please copy this file to the current directory."
    exit 1
fi

# Build the applications
echo "Building the reproducer applications."
mvn clean install 

# Build the postgresql module
echo "Building the postgresql module."
pushd postgresql-module
./create-postgresql-module-zip.sh -d ../postgresql-module.zip
popd

# Build the base jboss-eap docker-image
echo "Building the jboss-eap-6.1.1 base image."
echo "Copying JBoss EAP 6.1.1 file."
cp jboss-eap-6.1.1.zip docker/jboss-eap-6.1_base/dockerfile_copy
pushd docker/jboss-eap-6.1_base
echo "Building image."
./build_image.sh
popd

# Build the producer docker image
# First copy the postgresql module
echo "Building the producer Docker image."
echo "Copying the PostgreSQL module."
cp postgresql-module.zip docker/batch_producer/dockerfile_copy
pushd docker/batch_producer
echo "Building image."
./build_image.sh
popd

# Build the consumer docker image
# First copy the postgresql module
echo "Building the producer Docker image."
echo "Copying the PostgreSQL module."
cp postgresql-module.zip docker/batch_consumer/dockerfile_copy
pushd docker/batch_consumer
echo "Building image."
./build_image.sh
popd

#Build the PostgreSQL image
echo "Building the PostgreSQL Docker image."
pushd docker/postgresql
./build_image.sh
popd

echo "Building the HornetQ Docker image."
pushd docker/hornetq
./build_image.sh
popd

echo "Finished building the Docker containers."
echo "The reproducer can be started using the 'docker-compose up' commmand."









echo "Finished setting up the reproducer."
echo "The reproducer can be started by entering the 'docker' directory and executing 'docker-compose up'."

