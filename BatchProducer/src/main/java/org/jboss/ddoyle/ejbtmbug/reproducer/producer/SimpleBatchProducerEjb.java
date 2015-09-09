package org.jboss.ddoyle.ejbtmbug.reproducer.producer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.jboss.ddoyle.ejbtmbug.reproducer.consumer.BatchConsumerEjb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Session Bean implementation class SimpleBatchProducerEjb
 */
@Stateless
public class SimpleBatchProducerEjb implements BatchProducerEjb {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleBatchProducerEjb.class);
	
	private static final String REMOTE_EJB_JNDI_NAME = "ejb:/BatchConsumer-1.0.0-SNAPSHOT/SimpleBatchConsumerEjb!org.jboss.ddoyle.ejbtmbug.reproducer.consumer.BatchConsumerEjb";

	private Context ejbRootNamingContext;
	
	private BatchConsumerEjb consumer;
	
	@Resource
	private SessionContext sessionContext;

	@Resource(mappedName = "java:/jboss/datasources/ReproducerDS")
	private DataSource ds;

	
	@Resource
	private EJBContext ejbCtx;
	/**
	 * Default constructor.
	 */
	public SimpleBatchProducerEjb() {
	}
	
	@PostConstruct
	private void init() {
		LOGGER.info("Initializing EJB.");
		consumer = getBatchConsumerEjbProxy();
	}
	
	@PreDestroy
	private void destroy() {
		if (ejbRootNamingContext != null) {
			try {
				ejbRootNamingContext.close();
			} catch (NamingException ne) {
				//Not much we can do here. Just log.
				LOGGER.error("Unable to close EJB Root Naming Context.");
			}
		}
	}

	// Always start a new transaction.
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override
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
			try {
				consumer.processRecord(nextRecord);
			} catch(Exception e) {
				String message = "Received an Exception from the remote side. Marking transaction for rollback.";
				LOGGER.error(message, e);
				sessionContext.setRollbackOnly();
				//Just break out of the loop. Normally we would throw a proper exception, but we don't really care in this reproducer.
				break;
			}
		}
		
		if (!sessionContext.getRollbackOnly()) {
			LOGGER.info("Batch processed succesfully.");
		} else {
			LOGGER.info("Batch not succesfully processed, rolling back transaction.");
		}
	}


	private BatchConsumerEjb getBatchConsumerEjbProxy() {
		LOGGER.info("Retrieving remote EJB proxy.");
		BatchConsumerEjb ejb = null;
		final Hashtable<String, Object> jndiProps = new Hashtable();
		// setup the ejb: namespace URL factory
		jndiProps.put("jboss.naming.client.ejb.context", true);
		jndiProps.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
		
		// create the InitialContext
		Context context = null;
		try {
			context = new javax.naming.InitialContext(jndiProps);
			ejb = (BatchConsumerEjb) context.lookup(REMOTE_EJB_JNDI_NAME);
		} catch (NamingException ne) {
			String message = "Error looking up remote EJB.";
			LOGGER.error(message, ne);
			throw new RuntimeException(message, ne);
		} finally {
			//Close the context once we've got the EJB proxy.
			if (context != null) {
				try {
					context.close();
				} catch (NamingException ne) {
					LOGGER.warn("Error while closing JNDI context.", ne);
				}
			}
		}
		return ejb;
	}
	
	
	
	

}
