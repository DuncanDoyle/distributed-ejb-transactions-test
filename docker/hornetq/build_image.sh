#!/bin/sh
#
# Builds the Docker container.
#
# author: ddoyle@redhat.com
#

docker build --rm -t ddoyle/jboss-ejb-tm-bug-hornetq:1.0 .
