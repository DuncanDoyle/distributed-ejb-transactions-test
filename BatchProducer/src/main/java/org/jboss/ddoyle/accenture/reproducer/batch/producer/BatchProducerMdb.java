package org.jboss.ddoyle.accenture.reproducer.batch.producer;

import java.util.HashSet;
import java.util.Set;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.Message;
import javax.jms.MessageListener;

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

	private static final int RECORDS_IN_BATCH = 15000;
	
	@EJB
	private BatchProducerEjb producer;

	/**
	 * Default constructor.
	 */
	public BatchProducerMdb() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see MessageListener#onMessage(Message)
	 */
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void onMessage(Message message) {
		LOGGER.info("Processing message.");
		
		String threadName = Thread.currentThread().getName();
		
		//Create the batch
		Set<String> batchSet = new HashSet<>();
		for (int i = 0; i < RECORDS_IN_BATCH; i++ ) {
			String record = "order-" + threadName + "-" + i;
			batchSet.add(record);
		}
		LOGGER.info("Sending batch to producer.");
		producer.processBatch(batchSet);
		LOGGER.info("Message processed.");
	}

}
