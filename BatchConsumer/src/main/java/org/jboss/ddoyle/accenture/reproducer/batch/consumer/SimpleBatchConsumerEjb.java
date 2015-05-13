package org.jboss.ddoyle.accenture.reproducer.batch.consumer;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
@Remote(BatchConsumerEjb.class)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class SimpleBatchConsumerEjb implements BatchConsumerEjb {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleBatchConsumerEjb.class);
	
	@Resource(mappedName="java:/RemoteJmsXA")
	private ConnectionFactory cf;
	
	//Sends the content of the record to HornetQ in a text-message.
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override
	public void processRecord(String record) {
		LOGGER.info("Processing record: " + record);
		Connection connection = null;
		try {
			 connection = cf.createConnection("guest", "jboss@01");
			 Session session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
			 Queue queue = session.createQueue("BatchQueueOutput");
			 MessageProducer producer = session.createProducer(queue);
			 TextMessage message = session.createTextMessage(record);
			 producer.send(message);
			 producer.close();
			 session.close();
		} catch (JMSException jmse) {
			String message = "Error sending JMS message";
			LOGGER.error(message, jmse);
			throw new RuntimeException(message, jmse);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (JMSException jmse) {
					LOGGER.error("Unable to close conection.", jmse);
				}
			}
		}
	}
	
}
