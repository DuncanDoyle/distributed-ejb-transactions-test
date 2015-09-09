#
# JBoss EAP 6.4.0 base-image.
#
#FROM ddoyle/jboss-eap-ejb-tm-bug:6.4.0
FROM ddoyle/jboss-eap-ejb-tm-bug-base:6.1.1

MAINTAINER ddoyle <ddoyle@redhat.com>

# Add guest user
RUN /opt/jboss/jboss-eap-6/bin/add-user.sh -a -u guest -p jboss@01 -ro guest --silent

# Add installation files to tmp.
RUN mkdir /tmp/dockerfile_copy_2
COPY dockerfile_copy/ /tmp/dockerfile_copy_2

# Copy the launch script
RUN cp /tmp/dockerfile_copy_2/launch.sh /opt/jboss/jboss-eap-6/bin/launch.sh

# Install the PostgreSQL JBoss module.
RUN /tmp/dockerfile_copy_2/install-jboss-module.sh -m /tmp/dockerfile_copy_2/postgresql-module.zip -j /opt/jboss/jboss-eap-6 -l postgresql -e true

RUN cd /tmp/dockerfile_copy_2 && /tmp/dockerfile_copy_2/setup-jboss-eap-profile.sh -j /opt/jboss/jboss-eap-6/ -s standalone-full.xml -t reproducer-standalone-full.xml -c /tmp/dockerfile_copy_2/cli-scripts && rm -rf /opt/jboss/jboss-eap-6/standalone/configuration/standalone_xml_history/


# Clean up. Need to change to root for now because Docker COPY copies files as root (this is likely gonna change in future versions, where Docker COPY will respect USER config.
USER root
RUN rm -rf /tmp/dockerfile_copy_2
USER jboss

# Define the entrypoint and cmd.
# Define the entrypoint and cmd.
ENTRYPOINT ["/opt/jboss/jboss-eap-6.1/bin/launch.sh"]
#CMD ["-b", "0.0.0.0", "-bmanagement", "0.0.0.0", "-c", "reproducer-standalone-full.xml", "-Djboss.node.name=consumer_node"]
CMD ["-c", "reproducer-standalone-full.xml", "-Djboss.node.name=consumer_node"]
