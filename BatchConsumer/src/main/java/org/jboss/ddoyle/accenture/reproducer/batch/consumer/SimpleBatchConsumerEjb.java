package org.jboss.ddoyle.accenture.reproducer.batch.consumer;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.EJBException;
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
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
@Remote(BatchConsumerEjb.class)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class SimpleBatchConsumerEjb implements BatchConsumerEjb {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleBatchConsumerEjb.class);
	
	private static AtomicInteger counter = new AtomicInteger(0);
	
	@Resource(mappedName="java:/jboss/datasources/AccenturePragueDS")
	private DataSource ds;
	
	@Resource(mappedName="java:/RemoteJmsXA")
	private ConnectionFactory cf;
	
	@Resource
	private EJBContext ejbCtx;
	
	//The delay we built in while processing records. We do this to simulate errors.
	private static final long PROCESS_RECORD_DELAY = 2000L;
	
	
	//Lift on the transaction that's passed in
	/*
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override
	public void processRecord(String record) {
		LOGGER.info("Processing record.");
		Connection connection = null;
        try {
        	connection = ds.getConnection();
        	PreparedStatement prepStatement = connection.prepareStatement("INSERT INTO batch_consumer (record_id) VALUES (?)");
        	prepStatement.setString(1, record);
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
					//Not much we can do here. Log the exception and move on.
					LOGGER.warn("Unable to close connection.", sqle);
				}
        	}
        }
	}
	*/
	
	@Override
	public void processRecord(String record) {
		int currentCounter = counter.incrementAndGet();
		/*
		if (currentCounter == 500) {
			 throw new EJBException("Forcing transaction rollback.");
		 }
		 */
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
