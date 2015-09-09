package org.jboss.ddoyle.ejbtmbug.reproducer.producer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.naming.Context;
import javax.naming.NamingException;

import org.jboss.ddoyle.ejbtmbug.reproducer.consumer.BatchConsumerEjb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message-Driven Bean implementation class for: BatchProducerMdb
 */
@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "BatchQueue"),
		// @ActivationConfigProperty(propertyName = "connectionParameters", propertyValue = "host=127.0.0.4;port=5445"),
		// @ActivationConfigProperty(propertyName = "connectorClassName", propertyValue =
		// "org.hornetq.core.remoting.impl.netty.NettyConnectorFactory"),
		@ActivationConfigProperty(propertyName = "user", propertyValue = "guest"),
		@ActivationConfigProperty(propertyName = "password", propertyValue = "jboss@01"),
		@ActivationConfigProperty(propertyName = "maxSession", propertyValue = "50"),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") }, mappedName = "BatchQueue")
public class BatchProducerMdb implements MessageListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(BatchProducerMdb.class);

	private static final String BATCH_SIZE_PROPERTY_NAME = "batch.size";

	private static final String DEFAULT_BATCH_SIZE = "100";
	
	private final int batchSize;

	@EJB
	private BatchProducerEjb producer;
	

	/**
	 * Default constructor.
	 */
	public BatchProducerMdb() {
		String configuredBatchSize = System.getProperty(BATCH_SIZE_PROPERTY_NAME);
		if (configuredBatchSize == null || "".equals(configuredBatchSize)) {
			configuredBatchSize = DEFAULT_BATCH_SIZE;
		}
		this.batchSize = Integer.parseInt(configuredBatchSize);
	}

	@PostConstruct
	private void init() {
		LOGGER.info("Initializing EJB.");
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
		for (int i = 0; i < batchSize; i++) {
			String record = "order-" + threadName + "-" + i + "-" + Math.floor((Math.random() * 100000));
			batchSet.add(record);
		}
		LOGGER.info("Sending batch to producer.");
		//Call the local SLSB with the batch to be processed.
		producer.processBatch(batchSet);
		LOGGER.info("Message processed.");
	}

}
