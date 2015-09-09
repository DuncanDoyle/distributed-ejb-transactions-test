#!/bin/sh
#
# Builds the Docker container.
#
# author: ddoyle@redhat.com
#
docker build --rm -t ddoyle/jboss-eap-ejb-tm-bug-base:6.1.1 .
