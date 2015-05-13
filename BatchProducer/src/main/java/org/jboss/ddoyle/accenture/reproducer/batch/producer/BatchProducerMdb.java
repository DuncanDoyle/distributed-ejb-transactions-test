package org.jboss.ddoyle.accenture.reproducer.batch.producer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jboss.ddoyle.accenture.reproducer.batch.consumer.BatchConsumerEjb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message-Driven Bean implementation class for: BatchProducerMdb
 */
@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "BatchQueue"),
		@ActivationConfigProperty(propertyName = "connectionParameters", propertyValue = "host=127.0.0.4;port=5445"),
		@ActivationConfigProperty(propertyName = "connectorClassName", propertyValue = "org.hornetq.core.remoting.impl.netty.NettyConnectorFactory"),
		@ActivationConfigProperty(propertyName = "user", propertyValue = "guest"),
		@ActivationConfigProperty(propertyName = "password", propertyValue = "jboss@01"),
		@ActivationConfigProperty(propertyName = "maxSession", propertyValue = "12"),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") }, mappedName = "BatchQueue")
public class BatchProducerMdb implements MessageListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(BatchProducerMdb.class);

	private static final String BATCH_CONSUMER_EJB_JNDI_LOOKUP_NAME = "ejb:BatchConsumerEAR-1.0.0-SNAPSHOT/BatchConsumer-1.0.0-SNAPSHOT/SimpleBatchConsumerEjb!org.jboss.ddoyle.accenture.reproducer.batch.consumer.BatchConsumerEjb";

	private static final int RECORDS_IN_BATCH = 15000;

	@Resource(mappedName = "java:/jboss/datasources/AccenturePragueDS")
	private DataSource ds;

	private BatchConsumerEjb consumer;
	
	
	/**
	 * Default constructor.
	 */
	public BatchProducerMdb() {
		// TODO Auto-generated constructor stub
	}

	@PostConstruct
	private void init() {
		LOGGER.info("Initializing EJB.");
		consumer = getBatchConsumerEjbProxy();
	}

	/**
	 * @see MessageListener#onMessage(Message)
	 */
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void onMessage(Message message) {
		LOGGER.info("Processing message.");

		String threadName = Thread.currentThread().getName();

		// Create the batch
		Set<String> batchSet = new HashSet<>();
		for (int i = 0; i < RECORDS_IN_BATCH; i++) {
			String record = "order-" + threadName + "-" + i;
			batchSet.add(record);
		}
		LOGGER.info("Sending batch to producer.");
		processBatch(batchSet);
		LOGGER.info("Message processed.");
	}

	public void processBatch(Set<String> batch) {
		LOGGER.info("Processing batch.");
		for (String nextRecord : batch) {
			// go through the batch, write every record to DB and do a remote call.
			Connection connection = null;
			try {
				connection = ds.getConnection();
				PreparedStatement prepStatement = connection.prepareStatement("INSERT INTO batch_producer (record_id) VALUES (?)");
				prepStatement.setString(1, nextRecord);
				prepStatement.execute();

			} catch (SQLException sqle) {
				String errorMessage = "Error executing SQL prepared statement.";
				LOGGER.error(errorMessage, sqle);
				throw new RuntimeException(errorMessage, sqle);
			} finally {
				if (connection != null) {
					try {
						connection.close();
					} catch (SQLException sqle) {
						// Not much we can do here. Log the exception and move on.
						LOGGER.warn("Unable to close connection.", sqle);
					}
				}
			}
			//Note that we could get somer errors here, but we don't do any exception handling in this simple reproducer.
			consumer.processRecord(nextRecord);
		}
		LOGGER.info("Batch processed.");
	}

	private BatchConsumerEjb getBatchConsumerEjbProxy() {
		LOGGER.info("Retrieving remote EJB proxy.");
		BatchConsumerEjb ejb = null;
		final Hashtable<String, Object> jndiProps = new Hashtable<>();
		// setup the ejb: namespace URL factory

		jndiProps.put("jboss.naming.client.ejb.context", true);
		jndiProps.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");

		// create the InitialContext
		Context context = null;
		try {
			context = new javax.naming.InitialContext(jndiProps);
			ejb = (BatchConsumerEjb) context.lookup(BATCH_CONSUMER_EJB_JNDI_LOOKUP_NAME);
		} catch (NamingException ne) {
			String message = "Error looking up remote EJB.";
			LOGGER.error(message, ne);
			throw new RuntimeException(message, ne);
		} finally {
			if (context != null) {
				try {
					context.close();
				} catch (NamingException e) {
					// Can't do much here, just log an error.
					LOGGER.error("Unable to close context.");
				}
			}
		}
		return ejb;
	}

}
