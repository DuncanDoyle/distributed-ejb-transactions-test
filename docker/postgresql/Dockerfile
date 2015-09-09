#
# Base image for PostgreSQL 9.3 containers.
#
FROM    centos:centos6

MAINTAINER ddoyle <ddoyle@redhat.com>

# Update the system
RUN yum -y update && yum clean all

RUN echo "root:redhat" | chpasswd

# Expose the postgresql port.
EXPOSE 5432

############################################################################
# Install PostgreSQL 9.3
#
# Code taken from: https://github.com/DataVirtualizationByExample/DVWorkshop
###########################################################################
RUN yum -y install http://yum.postgresql.org/9.3/redhat/rhel-6-x86_64/pgdg-redhat93-9.3-1.noarch.rpm && yum -y install postgresql93-server postgresql93-contrib sudo && yum clean all

# Initialize the DB
RUN service postgresql-9.3 initdb

# Set Postgres environment variables
ENV PGDATA /var/lib/pgsql/9.3/data
ENV PGINST /usr/pgsql-9.3

# Create a homedir for the postgres user.
ENV HOME /home/postgres
RUN usermod -d $HOME postgres

# Set user to postgres.
USER postgres

# Configure max-prepared transactions to allows XA transasctions
RUN sed '119imax_prepared_transactions = 128' /var/lib/pgsql/9.3/data/postgresql.conf > /var/lib/pgsql/9.3/data/postgresql.conf.new && mv /var/lib/pgsql/9.3/data/postgresql.conf.new /var/lib/pgsql/9.3/data/postgresql.conf


############################################################################
# Set the database user and create the tables required for our reproducer.
###########################################################################
RUN $PGINST/bin/pg_ctl start -w -D $PGDATA && \ 
    $PGINST/bin/psql --command "ALTER USER postgres PASSWORD 'postgres';" && \
    $PGINST/bin/psql --command "CREATE DATABASE reproducer_db OWNER postgres;" && \
    $PGINST/bin/psql reproducer_db --command "CREATE TABLE batch_producer (record_id text NOT NULL, CONSTRAINT batch_producer_primary_key PRIMARY KEY (record_id)) WITH (OIDS=FALSE);" && \
    $PGINST/bin/psql reproducer_db --command "ALTER TABLE batch_producer OWNER TO postgres;" && \ 
    $PGINST/bin/psql reproducer_db --command "CREATE TABLE batch_consumer (record_id text NOT NULL, CONSTRAINT batch_consumer_primary_key PRIMARY KEY (record_id)) WITH (OIDS=FALSE);" && \
    $PGINST/bin/psql reproducer_db --command "ALTER TABLE batch_consumer OWNER TO postgres;" 

############################################
# Copy generic artifacts.
# Need to change back to root as COPY currently doesn't set file ownership to USER.
############################################
USER root
COPY dockerfile_copy/pgpass $HOME/.pgpass
COPY dockerfile_copy/pg_hba.conf $PGDATA/pg_hba.conf
RUN chown postgres:postgres $PGDATA/pg_hba.conf && chown postgres:postgres $HOME/.pgpass && chmod 0600 $HOME/.pgpass
USER postgres

# Adjust PostgreSQL configuration so that remote connections to the database are possible. 
RUN echo "listen_addresses='*'" >> $PGDATA/postgresql.conf && \
    echo "" >> $PGDATA/postgresql.conf

#CMD ["/bin/bash","-l"]
CMD /usr/pgsql-9.3/bin/postgres -D /var/lib/pgsql/9.3/data -c config_file=/var/lib/pgsql/9.3/data/postgresql.conf
